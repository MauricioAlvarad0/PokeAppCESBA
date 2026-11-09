package com.example.pokeappcesba;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FlappyPokemonActivity extends AppCompatActivity implements FlappyGameView.GameListener {

    private PokeApiService apiService;
    private FlappyGameView flappyGameView;
    private TextView tvFlappyScore, tvFlappyRecord, tvFlyerName;
    private ImageView ivFlyerPreview, btnBackFlappy;
    private Button btnChangeFlyer, btnStartFlappy;
    private LinearLayout llStartOverlay;

    private int currentScore = 0;
    private int bestRecord = 0;
    private SharedPreferences prefs;
    private String userUid = "default";
    private Pokemon currentFlyer;
    private CustomTarget<Bitmap> glideTarget;

    // Flying Pokemon IDs: Charizard, Butterfree, Pidgeot, Fearow, Golbat, Aerodactyl, Articuno, Zapdos, Moltres, Dragonite, Crobat, Lugia, Ho-Oh, Rayquaza
    private final int[] flyingPokemonIds = {6, 12, 18, 22, 42, 142, 144, 145, 146, 149, 169, 249, 250, 384};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flappy_pokemon);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        prefs = getSharedPreferences("PokeMinijuegos_" + userUid, MODE_PRIVATE);
        bestRecord = prefs.getInt("flappy_record", 0);

        apiService = new Retrofit.Builder()
                .baseUrl("https://pokeapi.co/api/v2/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PokeApiService.class);

        flappyGameView = findViewById(R.id.flappyGameView);
        flappyGameView.setGameListener(this);

        tvFlappyScore = findViewById(R.id.tvFlappyScore);
        tvFlappyRecord = findViewById(R.id.tvFlappyRecord);
        tvFlyerName = findViewById(R.id.tvFlyerName);
        ivFlyerPreview = findViewById(R.id.ivFlyerPreview);
        btnBackFlappy = findViewById(R.id.btnBackFlappy);
        btnChangeFlyer = findViewById(R.id.btnChangeFlyer);
        btnStartFlappy = findViewById(R.id.btnStartFlappy);
        llStartOverlay = findViewById(R.id.llStartOverlay);

        tvFlappyRecord.setText("RÉCORD: " + bestRecord);

        btnBackFlappy.setOnClickListener(v -> finish());
        btnChangeFlyer.setOnClickListener(v -> loadRandomFlyer());
        btnStartFlappy.setOnClickListener(v -> startFlappy());

        loadRandomFlyer();
    }

    private void loadRandomFlyer() {
        btnStartFlappy.setEnabled(false);
        tvFlyerName.setText("CARGANDO...");
        ivFlyerPreview.setImageDrawable(null);
        flappyGameView.setPokemonBitmap(null);

        int randomId = flyingPokemonIds[new Random().nextInt(flyingPokemonIds.length)];

        apiService.getPokemon(String.valueOf(randomId)).enqueue(new Callback<Pokemon>() {
            @Override
            public void onResponse(@NonNull Call<Pokemon> call, @NonNull Response<Pokemon> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentFlyer = response.body();
                    tvFlyerName.setText(currentFlyer.name.toUpperCase());
                    if (currentFlyer.sprites != null && currentFlyer.sprites.frontDefault != null) {
                        
                        glideTarget = new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                ivFlyerPreview.setImageBitmap(resource);
                                flappyGameView.setPokemonBitmap(resource);
                                btnStartFlappy.setEnabled(true);
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {
                            }
                        };
                        
                        Glide.with(FlappyPokemonActivity.this)
                                .asBitmap()
                                .load(currentFlyer.sprites.frontDefault)
                                .into(glideTarget);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Pokemon> call, @NonNull Throwable t) {
                Toast.makeText(FlappyPokemonActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startFlappy() {
        llStartOverlay.setVisibility(View.GONE);
        btnChangeFlyer.setEnabled(false);
        btnBackFlappy.setEnabled(false);
        flappyGameView.startGame();
    }

    @Override
    public void onScoreChanged(int score) {
        currentScore = score;
        tvFlappyScore.setText("PUNTOS: " + score);
    }

    @Override
    public void onGameOver(int score) {
        if (score > bestRecord) {
            bestRecord = score;
            prefs.edit().putInt("flappy_record", bestRecord).apply();
            getSharedPreferences("PokePrefs", MODE_PRIVATE).edit().putInt("flappy_record", bestRecord).apply();
            tvFlappyRecord.setText("RÉCORD: " + bestRecord);

            // Sincronizar récord en Firebase Firestore
            if (!userUid.equals("default")) {
                Map<String, Object> data = new HashMap<>();
                data.put("flappy_record", bestRecord);

                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userUid)
                        .collection("Records")
                        .document("minijuegos")
                        .set(data, SetOptions.merge());
            }
        }

        btnChangeFlyer.setEnabled(true);
        btnBackFlappy.setEnabled(true);
        showGameOverDialog(score);
    }

    private void showGameOverDialog(int finalScore) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_retro_custom);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView tvTitle = dialog.findViewById(R.id.tvRetroDialogTitle);
        TextView tvMsg = dialog.findViewById(R.id.tvRetroDialogMessage);
        Button btnPos = dialog.findViewById(R.id.btnRetroPositive);
        Button btnNeg = dialog.findViewById(R.id.btnRetroNegative);
        ImageView ivIcon = dialog.findViewById(R.id.ivRetroDialogIcon);

        tvTitle.setText("¡GAME OVER!");
        tvTitle.setTextColor(Color.parseColor("#EF4444"));
        tvMsg.setText("PUNTUACIÓN OBTENIDA: " + finalScore + "\nRÉCORD MAX: " + bestRecord);
        
        btnPos.setText("🔥 REINTENTAR 🔥");
        btnNeg.setVisibility(View.VISIBLE);
        btnNeg.setText("🚪 SALIR AL MENÚ");

        if (currentFlyer != null && currentFlyer.sprites != null && currentFlyer.sprites.frontDefault != null) {
            Glide.with(this).load(currentFlyer.sprites.frontDefault).into(ivIcon);
        }

        btnPos.setOnClickListener(v -> {
            dialog.dismiss();
            startFlappy();
        });

        btnNeg.setOnClickListener(v -> {
            dialog.dismiss();
            llStartOverlay.setVisibility(View.VISIBLE);
        });

        dialog.setCancelable(false);
        dialog.show();
    }
}
