package com.example.pokeappcesba;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SafariActivity extends AppCompatActivity {

    private PokeApiService apiService;
    private ImageView btnBackSafari, ivWildSprite;
    private TextView tvSafariCaught, tvSafariCoins, tvWildStatus, tvWildName;
    private Button btnPokeball, btnSuperball, btnUltraball, btnMasterball;
    private Button btnCatch, btnRun;

    private Pokemon currentWild;
    private int selectedBallRate = 40; // 40, 65, 85, 100
    private int selectedBallCost = 0;
    private int safariCoins = 100;
    private int caughtCount = 0;
    private boolean isBusy = false;

    private SharedPreferences prefsSafari;
    private SharedPreferences prefsGlobales;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safari);

        String userUid = "default";
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        prefsSafari = getSharedPreferences("PokeSafari_" + userUid, MODE_PRIVATE);
        prefsGlobales = getSharedPreferences("PokePrefs", MODE_PRIVATE);

        caughtCount = prefsSafari.getAll().size();

        apiService = new Retrofit.Builder()
                .baseUrl("https://pokeapi.co/api/v2/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PokeApiService.class);

        btnBackSafari = findViewById(R.id.btnBackSafari);
        ivWildSprite = findViewById(R.id.ivWildSprite);
        tvSafariCaught = findViewById(R.id.tvSafariCaught);
        tvSafariCoins = findViewById(R.id.tvSafariCoins);
        tvWildStatus = findViewById(R.id.tvWildStatus);
        tvWildName = findViewById(R.id.tvWildName);

        btnPokeball = findViewById(R.id.btnPokeball);
        btnSuperball = findViewById(R.id.btnSuperball);
        btnUltraball = findViewById(R.id.btnUltraball);
        btnMasterball = findViewById(R.id.btnMasterball);

        btnCatch = findViewById(R.id.btnCatch);
        btnRun = findViewById(R.id.btnRun);

        btnBackSafari.setOnClickListener(v -> finish());

        btnPokeball.setOnClickListener(v -> selectBall(40, 0, btnPokeball));
        btnSuperball.setOnClickListener(v -> selectBall(65, 10, btnSuperball));
        btnUltraball.setOnClickListener(v -> selectBall(85, 25, btnUltraball));
        btnMasterball.setOnClickListener(v -> selectBall(100, 50, btnMasterball));

        btnCatch.setOnClickListener(v -> attemptCatch());
        btnRun.setOnClickListener(v -> findWildPokemon());

        updateUI();
        findWildPokemon();
    }

    @SuppressLint("SetTextI18n")
    private void updateUI() {
        tvSafariCaught.setText("CAPTURADOS: " + caughtCount);
        tvSafariCoins.setText("SAFARI MONEDAS: " + safariCoins);
    }

    private void selectBall(int rate, int cost, Button selected) {
        if (isBusy) return;
        this.selectedBallRate = rate;
        this.selectedBallCost = cost;

        btnPokeball.setBackgroundResource(R.drawable.bg_button_retro);
        btnSuperball.setBackgroundResource(R.drawable.bg_button_retro);
        btnUltraball.setBackgroundResource(R.drawable.bg_button_retro);
        btnMasterball.setBackgroundResource(R.drawable.bg_button_retro);

        selected.setBackgroundResource(R.drawable.btn_retro_red);
    }

    @SuppressLint("SetTextI18n")
    private void findWildPokemon() {
        if (isBusy) return;
        isBusy = true;
        btnCatch.setEnabled(false);
        btnRun.setEnabled(false);

        tvWildStatus.setText("¡Buscando Pokémon en la hierba alta...");
        tvWildStatus.setTextColor(Color.parseColor("#FACC15"));
        tvWildName.setText("...");
        ivWildSprite.setImageDrawable(null);

        int randomId = new Random().nextInt(251) + 1;
        apiService.getPokemon(String.valueOf(randomId)).enqueue(new Callback<Pokemon>() {
            @Override
            public void onResponse(@NonNull Call<Pokemon> call, @NonNull Response<Pokemon> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentWild = response.body();
                    tvWildName.setText(currentWild.name.toUpperCase());
                    tvWildStatus.setText("¡Un " + currentWild.name.toUpperCase() + " salvaje apareció!");

                    if (currentWild.sprites != null && currentWild.sprites.frontDefault != null) {
                        Glide.with(SafariActivity.this).load(currentWild.sprites.frontDefault).into(ivWildSprite);
                    }
                    if (currentWild.cries != null && currentWild.cries.latest != null) {
                        playCry(currentWild.cries.latest);
                    }
                    btnCatch.setEnabled(true);
                    btnRun.setEnabled(true);
                    isBusy = false;
                } else {
                    isBusy = false;
                    findWildPokemon();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Pokemon> call, @NonNull Throwable t) {
                isBusy = false;
                Toast.makeText(SafariActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void attemptCatch() {
        if (isBusy || currentWild == null) return;

        if (safariCoins < selectedBallCost) {
            Toast.makeText(this, "¡No tienes suficientes Safari Monedas!", Toast.LENGTH_SHORT).show();
            return;
        }

        isBusy = true;
        safariCoins -= selectedBallCost;
        updateUI();

        btnCatch.setEnabled(false);
        btnRun.setEnabled(false);
        tvWildStatus.setText("Lanzando Pokéball... 🔴");

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            int roll = new Random().nextInt(100) + 1;
            if (roll <= selectedBallRate) {
                // Capturado!
                caughtCount++;
                prefsSafari.edit().putString(currentWild.name.toLowerCase(), currentWild.name.toUpperCase()).apply();
                prefsGlobales.edit().putInt("safari_caught", caughtCount).apply();
                safariCoins += 30; // Recompensa
                updateUI();

                tvWildStatus.setText("🎉 ¡" + currentWild.name.toUpperCase() + " CAPTURADO!");
                tvWildStatus.setTextColor(Color.parseColor("#4ADE80"));
                new android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100).startTone(android.media.ToneGenerator.TONE_PROP_BEEP2, 250);

                new Handler(Looper.getMainLooper()).postDelayed(this::findWildPokemon, 2000);
            } else {
                // Fallado
                tvWildStatus.setText("❌ ¡El Pokémon se escapó de la Ball!");
                tvWildStatus.setTextColor(Color.parseColor("#EF4444"));

                // Chance de huir (30%)
                if (new Random().nextInt(100) < 30) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        tvWildStatus.setText("💨 ¡" + currentWild.name.toUpperCase() + " ha huido!");
                        new Handler(Looper.getMainLooper()).postDelayed(this::findWildPokemon, 1500);
                    }, 1000);
                } else {
                    btnCatch.setEnabled(true);
                    btnRun.setEnabled(true);
                    isBusy = false;
                }
            }
        }, 1200);
    }

    private void playCry(String url) {
        try {
            if (mediaPlayer != null) {
                try { if (mediaPlayer.isPlaying()) mediaPlayer.stop(); } catch (Exception ignored) {}
                mediaPlayer.release();
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                if (!isFinishing() && !isDestroyed()) mp.start();
            });
        } catch (Exception ignored) {}
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mediaPlayer != null) {
            try { if (mediaPlayer.isPlaying()) mediaPlayer.stop(); mediaPlayer.release(); } catch (Exception ignored) {}
            mediaPlayer = null;
        }
    }
}
