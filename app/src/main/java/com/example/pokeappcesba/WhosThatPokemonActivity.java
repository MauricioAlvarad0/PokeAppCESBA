package com.example.pokeappcesba;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WhosThatPokemonActivity extends AppCompatActivity {

    private PokeApiService apiService;
    private ImageView btnBackWT, ivMysterySprite;
    private TextView tvScoreWT, tvStreakWT, tvRecordWT, tvResultBanner, tvHintLogWT;
    private Button btnHintTypeWT, btnHintStatWT, btnHintCryWT;
    private final Button[] btnOptions = new Button[4];

    private Pokemon correctPokemon;
    private final List<String> optionsNames = new ArrayList<>();
    private int loadedOptions = 0;

    private int score = 0;
    private int streak = 0;
    private int record = 0;
    private boolean isRoundActive = false;

    private SharedPreferences prefs;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whos_that_pokemon);

        String userUid = "default";
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        prefs = getSharedPreferences("PokeMinijuegos_" + userUid, MODE_PRIVATE);
        record = prefs.getInt("record_racha", 0);

        apiService = new Retrofit.Builder()
                .baseUrl("https://pokeapi.co/api/v2/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PokeApiService.class);

        btnBackWT = findViewById(R.id.btnBackWT);
        ivMysterySprite = findViewById(R.id.ivMysterySprite);
        tvScoreWT = findViewById(R.id.tvScoreWT);
        tvStreakWT = findViewById(R.id.tvStreakWT);
        tvRecordWT = findViewById(R.id.tvRecordWT);
        tvResultBanner = findViewById(R.id.tvResultBanner);
        tvHintLogWT = findViewById(R.id.tvHintLogWT);

        btnHintTypeWT = findViewById(R.id.btnHintTypeWT);
        btnHintStatWT = findViewById(R.id.btnHintStatWT);
        btnHintCryWT = findViewById(R.id.btnHintCryWT);

        btnOptions[0] = findViewById(R.id.btnOptionA);
        btnOptions[1] = findViewById(R.id.btnOptionB);
        btnOptions[2] = findViewById(R.id.btnOptionC);
        btnOptions[3] = findViewById(R.id.btnOptionD);

        btnBackWT.setOnClickListener(v -> finish());

        btnHintTypeWT.setOnClickListener(v -> useHintType());
        btnHintStatWT.setOnClickListener(v -> useHintStat());
        btnHintCryWT.setOnClickListener(v -> useHintCry());

        for (Button btn : btnOptions) {
            btn.setOnClickListener(v -> checkAnswer(btn.getText().toString()));
        }

        updateScoreBoard();
        loadRound();
    }

    @SuppressLint("SetTextI18n")
    private void updateScoreBoard() {
        tvScoreWT.setText("PUNTOS: " + score);
        tvStreakWT.setText("RACHA: " + streak);
        tvRecordWT.setText("RÉCORD: " + record);
    }

    @SuppressLint("SetTextI18n")
    private void loadRound() {
        isRoundActive = false;
        tvResultBanner.setText("CARGANDO...");
        tvResultBanner.setTextColor(Color.WHITE);
        tvHintLogWT.setText("¿Podrás adivinarlo?");
        ivMysterySprite.setImageDrawable(null);
        optionsNames.clear();
        loadedOptions = 0;
        
        btnHintTypeWT.setEnabled(false);
        btnHintStatWT.setEnabled(false);
        btnHintCryWT.setEnabled(false);

        for (Button btn : btnOptions) {
            btn.setText("...");
            btn.setEnabled(false);
            btn.setBackgroundResource(R.drawable.bg_button_retro);
        }

        Random rand = new Random();
        int correctId = rand.nextInt(251) + 1;
        int wrong1 = rand.nextInt(251) + 1;
        int wrong2 = rand.nextInt(251) + 1;
        int wrong3 = rand.nextInt(251) + 1;

        fetchPokemon(correctId, true);
        fetchPokemon(wrong1, false);
        fetchPokemon(wrong2, false);
        fetchPokemon(wrong3, false);
    }

    private void fetchPokemon(int id, boolean isCorrect) {
        apiService.getPokemon(String.valueOf(id)).enqueue(new Callback<Pokemon>() {
            @Override
            public void onResponse(@NonNull Call<Pokemon> call, @NonNull Response<Pokemon> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Pokemon p = response.body();
                    if (isCorrect) {
                        correctPokemon = p;
                        if (p.sprites != null && p.sprites.frontDefault != null) {
                            Glide.with(WhosThatPokemonActivity.this)
                                    .load(p.sprites.frontDefault)
                                    .into(ivMysterySprite);
                            ivMysterySprite.setColorFilter(Color.parseColor("#FF0F172A"), PorterDuff.Mode.SRC_IN);
                        }
                    }
                    optionsNames.add(p.name.toUpperCase());
                    loadedOptions++;
                    if (loadedOptions == 4) {
                        setupOptions();
                    }
                } else {
                    fetchPokemon(new Random().nextInt(251) + 1, isCorrect);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Pokemon> call, @NonNull Throwable t) {
                fetchPokemon(new Random().nextInt(251) + 1, isCorrect);
            }
        });
    }

    private void setupOptions() {
        Collections.shuffle(optionsNames);
        for (int i = 0; i < 4; i++) {
            btnOptions[i].setText(optionsNames.get(i));
            btnOptions[i].setEnabled(true);
        }
        btnHintTypeWT.setEnabled(true);
        btnHintStatWT.setEnabled(true);
        btnHintCryWT.setEnabled(true);
        tvResultBanner.setText("");
        isRoundActive = true;
    }

    @SuppressLint("SetTextI18n")
    private void checkAnswer(String chosen) {
        if (!isRoundActive) return;
        isRoundActive = false;

        ivMysterySprite.clearColorFilter();
        
        for (Button btn : btnOptions) {
            btn.setEnabled(false);
            if (btn.getText().toString().equalsIgnoreCase(correctPokemon.name)) {
                btn.setBackgroundResource(R.drawable.btn_retro_red);
            }
        }
        btnHintTypeWT.setEnabled(false);
        btnHintStatWT.setEnabled(false);
        btnHintCryWT.setEnabled(false);

        if (chosen.equalsIgnoreCase(correctPokemon.name)) {
            score += 10;
            streak++;
            if (streak > record) {
                record = streak;
                prefs.edit().putInt("record_racha", record).apply();
            }
            tvResultBanner.setText("¡CORRECTO! +10");
            tvResultBanner.setTextColor(Color.parseColor("#4ADE80"));
            if (correctPokemon.cries != null && correctPokemon.cries.latest != null) {
                playCry(correctPokemon.cries.latest);
            }
        } else {
            streak = 0;
            tvResultBanner.setText("¡INCORRECTO! Era " + correctPokemon.name.toUpperCase());
            tvResultBanner.setTextColor(Color.parseColor("#EF4444"));
        }

        updateScoreBoard();
        new Handler(Looper.getMainLooper()).postDelayed(this::loadRound, 2200);
    }

    @SuppressLint("SetTextI18n")
    private void useHintType() {
        if (!isRoundActive || correctPokemon == null) return;
        score -= 2;
        updateScoreBoard();
        StringBuilder types = new StringBuilder();
        if (correctPokemon.types != null) {
            for (Pokemon.TypeSlot ts : correctPokemon.types) {
                types.append(ts.type.name.toUpperCase()).append(" ");
            }
        }
        tvHintLogWT.setText("TIPO: " + types.toString().trim());
        btnHintTypeWT.setEnabled(false);
    }

    @SuppressLint("SetTextI18n")
    private void useHintStat() {
        if (!isRoundActive || correctPokemon == null || correctPokemon.stats == null) return;
        score -= 3;
        updateScoreBoard();
        int hp = 0, atk = 0;
        for (Pokemon.StatSlot s : correctPokemon.stats) {
            if (s.stat.name.equals("hp")) hp = s.baseStat;
            if (s.stat.name.equals("attack")) atk = s.baseStat;
        }
        tvHintLogWT.setText("STATS -> HP: " + hp + " | ATK: " + atk);
        btnHintStatWT.setEnabled(false);
    }

    @SuppressLint("SetTextI18n")
    private void useHintCry() {
        if (!isRoundActive || correctPokemon == null) return;
        if (correctPokemon.cries != null && correctPokemon.cries.latest != null) {
            score -= 3;
            updateScoreBoard();
            tvHintLogWT.setText("ESCUCHANDO GRITO...");
            playCry(correctPokemon.cries.latest);
        } else {
            Toast.makeText(this, "Sin grito disponible", Toast.LENGTH_SHORT).show();
        }
        btnHintCryWT.setEnabled(false);
    }

    private void playCry(String url) {
        try {
            if (mediaPlayer != null) {
                try {
                    if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                } catch (Exception ignored) {}
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
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
    }
}
