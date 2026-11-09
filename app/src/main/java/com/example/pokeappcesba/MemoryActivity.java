package com.example.pokeappcesba;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MemoryActivity extends AppCompatActivity {

    private PokeApiService apiService;
    private ImageView btnBackMemory;
    private TextView tvMemoryMoves, tvMemoryPairs, tvMemoryRecord;
    private GridLayout glMemoryGrid;
    private Button btnRestartMemory;

    private List<MemoryCard> cards = new ArrayList<>();
    private MemoryCard firstFlipped = null;
    private MemoryCard secondFlipped = null;
    private boolean isBusy = false;

    private int moves = 0;
    private int pairsFound = 0;
    private int bestMoves = 999;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory);

        String userUid = "default";
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        prefs = getSharedPreferences("PokeMinijuegos_" + userUid, MODE_PRIVATE);
        bestMoves = prefs.getInt("memory_record", 999);

        apiService = new Retrofit.Builder()
                .baseUrl("https://pokeapi.co/api/v2/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PokeApiService.class);

        btnBackMemory = findViewById(R.id.btnBackMemory);
        tvMemoryMoves = findViewById(R.id.tvMemoryMoves);
        tvMemoryPairs = findViewById(R.id.tvMemoryPairs);
        tvMemoryRecord = findViewById(R.id.tvMemoryRecord);
        glMemoryGrid = findViewById(R.id.glMemoryGrid);
        btnRestartMemory = findViewById(R.id.btnRestartMemory);

        btnBackMemory.setOnClickListener(v -> finish());
        btnRestartMemory.setOnClickListener(v -> startNewGame());

        updateScoreBoard();
        startNewGame();
    }

    @SuppressLint("SetTextI18n")
    private void updateScoreBoard() {
        tvMemoryMoves.setText("MOVIMIENTOS: " + moves);
        tvMemoryPairs.setText("PAREJAS: " + pairsFound + "/6");
        tvMemoryRecord.setText("MEJOR: " + (bestMoves == 999 ? "--" : bestMoves + " movs"));
    }

    private void startNewGame() {
        if (isBusy) return;
        isBusy = true;
        moves = 0;
        pairsFound = 0;
        firstFlipped = null;
        secondFlipped = null;
        cards.clear();
        glMemoryGrid.removeAllViews();
        updateScoreBoard();

        // Fetch 6 random Pokemon
        Random rand = new Random();
        List<Integer> ids = new ArrayList<>();
        while (ids.size() < 6) {
            int randomId = rand.nextInt(251) + 1;
            if (!ids.contains(randomId)) ids.add(randomId);
        }

        List<String> spritesList = new ArrayList<>();
        int[] loadedCount = {0};

        for (int id : ids) {
            apiService.getPokemon(String.valueOf(id)).enqueue(new Callback<Pokemon>() {
                @Override
                public void onResponse(@NonNull Call<Pokemon> call, @NonNull Response<Pokemon> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().sprites != null) {
                        String sprite = response.body().sprites.frontDefault;
                        if (sprite != null) {
                            spritesList.add(sprite);
                            spritesList.add(sprite); // Double for pair
                        }
                    }
                    loadedCount[0]++;
                    if (loadedCount[0] == 6) {
                        setupGrid(spritesList);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Pokemon> call, @NonNull Throwable t) {
                    loadedCount[0]++;
                    if (loadedCount[0] == 6) {
                        setupGrid(spritesList);
                    }
                }
            });
        }
    }

    private void setupGrid(List<String> sprites) {
        if (sprites.size() < 12) {
            isBusy = false;
            startNewGame();
            return;
        }

        Collections.shuffle(sprites);

        for (int i = 0; i < 12; i++) {
            final MemoryCard card = new MemoryCard(i, sprites.get(i));
            cards.add(card);

            LinearLayout cardView = new LinearLayout(this);
            cardView.setOrientation(LinearLayout.VERTICAL);
            cardView.setGravity(Gravity.CENTER);
            cardView.setBackgroundResource(R.drawable.bg_gold_frame);
            cardView.setPadding(6, 6, 6, 6);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = 0;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(6, 6, 6, 6);
            cardView.setLayoutParams(params);

            ImageView iv = new ImageView(this);
            iv.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iv.setImageResource(R.drawable.ic_poke_memory);
            cardView.addView(iv);

            card.view = cardView;
            card.imageView = iv;

            cardView.setOnClickListener(v -> onCardClicked(card));

            glMemoryGrid.addView(cardView);
        }

        isBusy = false;
    }

    private void onCardClicked(MemoryCard card) {
        if (isBusy || card.isFlipped || card.isMatched) return;

        // Flip card
        card.isFlipped = true;
        Glide.with(this).load(card.spriteUrl).into(card.imageView);
        card.view.setBackgroundResource(R.drawable.bg_battle_text);

        new android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100).startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 50);

        if (firstFlipped == null) {
            firstFlipped = card;
        } else {
            secondFlipped = card;
            moves++;
            updateScoreBoard();
            checkMatch();
        }
    }

    private void checkMatch() {
        isBusy = true;
        if (firstFlipped.spriteUrl.equals(secondFlipped.spriteUrl)) {
            // Match!
            firstFlipped.isMatched = true;
            secondFlipped.isMatched = true;
            pairsFound++;
            updateScoreBoard();
            new android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100).startTone(android.media.ToneGenerator.TONE_PROP_BEEP2, 150);

            firstFlipped = null;
            secondFlipped = null;
            isBusy = false;

            if (pairsFound == 6) {
                onGameWon();
            }
        } else {
            // Mismatch
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                firstFlipped.isFlipped = false;
                secondFlipped.isFlipped = false;
                firstFlipped.imageView.setImageResource(R.drawable.ic_poke_memory);
                secondFlipped.imageView.setImageResource(R.drawable.ic_poke_memory);
                firstFlipped.view.setBackgroundResource(R.drawable.bg_gold_frame);
                secondFlipped.view.setBackgroundResource(R.drawable.bg_gold_frame);

                firstFlipped = null;
                secondFlipped = null;
                isBusy = false;
            }, 800);
        }
    }

    private void onGameWon() {
        if (moves < bestMoves) {
            bestMoves = moves;
            prefs.edit().putInt("memory_record", bestMoves).apply();
            updateScoreBoard();
        }

        abrirDialogoRetro("🎉 ¡FELICIDADES! 🎉", 
                "Has encontrado todas las parejas en " + moves + " movimientos.", 
                "REINICIAR", 
                "SALIR", 
                this::startNewGame, 
                this::finish, 
                R.drawable.ic_poke_memory);
    }

    private void abrirDialogoRetro(String title, String message, String positiveText, String negativeText, Runnable onPositive, Runnable onNegative, int iconRes) {
        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_retro_custom);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        TextView tvTitle = dialog.findViewById(R.id.tvRetroDialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvRetroDialogMessage);
        Button btnPos = dialog.findViewById(R.id.btnRetroPositive);
        Button btnNeg = dialog.findViewById(R.id.btnRetroNegative);
        ImageView ivIcon = dialog.findViewById(R.id.ivRetroDialogIcon);
        View flIconCont = dialog.findViewById(R.id.flRetroDialogIconContainer);

        tvTitle.setText(title);
        tvMessage.setText(message);
        btnPos.setText(positiveText);

        if (iconRes != 0) {
            ivIcon.setImageResource(iconRes);
        } else {
            flIconCont.setVisibility(View.GONE);
        }

        btnPos.setOnClickListener(v -> {
            dialog.dismiss();
            if (onPositive != null) onPositive.run();
        });

        if (negativeText != null && !negativeText.isEmpty()) {
            btnNeg.setText(negativeText);
            btnNeg.setVisibility(View.VISIBLE);
            btnNeg.setOnClickListener(v -> {
                dialog.dismiss();
                if (onNegative != null) onNegative.run();
            });
        } else {
            btnNeg.setVisibility(View.GONE);
        }

        dialog.setCancelable(false);
        dialog.show();
    }

    private static class MemoryCard {
        int id;
        String spriteUrl;
        boolean isFlipped = false;
        boolean isMatched = false;
        LinearLayout view;
        ImageView imageView;

        MemoryCard(int id, String spriteUrl) {
            this.id = id;
            this.spriteUrl = spriteUrl;
        }
    }
}
