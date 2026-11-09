package com.example.pokeappcesba;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WhosThatPokemonActivity extends AppCompatActivity {

    private PokeApiService apiService;
    private ImageView btnBackWhos, ivPokemonMystery;
    private VideoView vvWhosBackground;
    private TextView tvCurrentStreak, tvCurrentScore, tvClueDisplay;
    private Button btnHintType, btnHintStat, btnHintCry;
    private final Button[] btnOptions = new Button[4];

    private Pokemon correctPokemon;
    private final List<String> optionsNames = new ArrayList<>();
    private int loadedOptions = 0;

    private int score = 0;
    private int streak = 0;
    private int record = 0;
    private boolean isRoundActive = false;

    private SharedPreferences prefs;
    private SharedPreferences prefsGlobales;
    private String userUid = "default";
    
    private MediaPlayer cryPlayer;
    private Handler videoHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whos_that_pokemon);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        prefs = getSharedPreferences("PokeMinijuegos_" + userUid, MODE_PRIVATE);
        prefsGlobales = getSharedPreferences("PokePrefs", MODE_PRIVATE);
        record = prefs.getInt("record_racha", 0);

        apiService = new Retrofit.Builder()
                .baseUrl("https://pokeapi.co/api/v2/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PokeApiService.class);

        btnBackWhos = findViewById(R.id.btnBackWhos);
        ivPokemonMystery = findViewById(R.id.ivPokemonMystery);
        vvWhosBackground = findViewById(R.id.vvWhosBackground);
        
        tvCurrentStreak = findViewById(R.id.tvCurrentStreak);
        tvCurrentScore = findViewById(R.id.tvCurrentScore);
        tvClueDisplay = findViewById(R.id.tvClueDisplay);

        btnHintType = findViewById(R.id.btnHintType);
        btnHintStat = findViewById(R.id.btnHintStat);
        btnHintCry = findViewById(R.id.btnHintCry);

        btnOptions[0] = findViewById(R.id.btnOption1);
        btnOptions[1] = findViewById(R.id.btnOption2);
        btnOptions[2] = findViewById(R.id.btnOption3);
        btnOptions[3] = findViewById(R.id.btnOption4);

        btnBackWhos.setOnClickListener(v -> finish());

        btnHintType.setOnClickListener(v -> useHintType());
        btnHintStat.setOnClickListener(v -> useHintStat());
        btnHintCry.setOnClickListener(v -> useHintCry());

        for (Button btn : btnOptions) {
            btn.setOnClickListener(v -> checkAnswer(btn.getText().toString()));
        }

        setupVideoView();
        updateScoreBoard();
        loadNextRound();
    }

    private void setupVideoView() {
        int videoId = getResources().getIdentifier("whos_that_pokemon_intro", "raw", getPackageName());
        if (videoId != 0) {
            String path = "android.resource://" + getPackageName() + "/" + videoId;
            vvWhosBackground.setVideoURI(Uri.parse(path));
            vvWhosBackground.setOnCompletionListener(mp -> {
                // Video finished, automatically load next round after a brief pause
                videoHandler.postDelayed(this::loadNextRound, 1000);
            });
            // Loop is handled manually to prevent black screens between rounds
        } else {
            vvWhosBackground.setBackgroundResource(R.drawable.bg_dialog_game_over);
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateScoreBoard() {
        tvCurrentScore.setText("Puntos: " + score);
        tvCurrentStreak.setText("Ronda / Racha: " + streak + " | Récord: " + record);
    }

    @SuppressLint("SetTextI18n")
    private void loadNextRound() {
        isRoundActive = false;
        tvClueDisplay.setText("Cargando próximo Pokémon...");
        tvClueDisplay.setTextColor(Color.WHITE);
        
        ivPokemonMystery.setVisibility(View.INVISIBLE);
        ivPokemonMystery.setImageDrawable(null);
        
        optionsNames.clear();
        loadedOptions = 0;
        
        btnHintType.setEnabled(false);
        btnHintStat.setEnabled(false);
        btnHintCry.setEnabled(false);

        for (Button btn : btnOptions) {
            btn.setText("...");
            btn.setEnabled(false);
            btn.setBackgroundResource(R.drawable.btn_retro_gray);
            btn.setBackgroundTintList(null);
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
                                    .into(ivPokemonMystery);
                            // Silhouette black filter
                            ivPokemonMystery.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
                        }
                    }
                    optionsNames.add(p.name.toUpperCase().replace("-", " "));
                    loadedOptions++;
                    if (loadedOptions == 4) {
                        startVideoIntro();
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

    private void startVideoIntro() {
        // Shuffle options and prepare buttons
        Collections.shuffle(optionsNames);
        for (int i = 0; i < 4; i++) {
            btnOptions[i].setText(optionsNames.get(i));
        }

        videoHandler.removeCallbacksAndMessages(null);
        
        vvWhosBackground.seekTo(0);
        vvWhosBackground.start();

        // 1.0s: Reveal silhouette and scale up
        videoHandler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            ivPokemonMystery.setVisibility(View.VISIBLE);
            ivPokemonMystery.setScaleX(0.3f);
            ivPokemonMystery.setScaleY(0.3f);
            ivPokemonMystery.animate().scaleX(1.0f).scaleY(1.0f).setDuration(400).start();
        }, 1000);

        // 3.5s: Pause video before it fades to black to let user answer
        videoHandler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            vvWhosBackground.pause();
            
            // Enable UI
            for (Button btn : btnOptions) {
                btn.setEnabled(true);
            }
            btnHintType.setEnabled(true);
            btnHintStat.setEnabled(true);
            btnHintCry.setEnabled(true);
            tvClueDisplay.setText("¡Es hora de adivinar!");
            tvClueDisplay.setTextColor(Color.parseColor("#FACC15"));
            isRoundActive = true;
        }, 3500);
    }

    @SuppressLint("SetTextI18n")
    private void checkAnswer(String chosen) {
        if (!isRoundActive) return;
        isRoundActive = false;
        
        videoHandler.removeCallbacksAndMessages(null);
        
        String actualName = correctPokemon.name.toUpperCase().replace("-", " ");

        // Color buttons based on correctness
        for (Button btn : btnOptions) {
            btn.setEnabled(false);
            if (btn.getText().toString().equals(actualName)) {
                btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#22C55E"))); // Green
            } else {
                btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EF4444"))); // Red
            }
        }
        
        btnHintType.setEnabled(false);
        btnHintStat.setEnabled(false);
        btnHintCry.setEnabled(false);

        // Resume video for the reveal part (approx 5.5s)
        vvWhosBackground.seekTo(5500);
        vvWhosBackground.start();

        // After 600ms of resuming video, reveal Pokemon color and play cry
        videoHandler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            ivPokemonMystery.clearColorFilter();
            if (correctPokemon.cries != null && correctPokemon.cries.latest != null) {
                playCry(correctPokemon.cries.latest);
            }
        }, 600);

        if (chosen.equals(actualName)) {
            score += 10;
            streak++;
            if (streak > record) {
                record = streak;
                prefs.edit().putInt("record_racha", record).apply();
                prefsGlobales.edit().putInt("record_racha", record).apply();
                
                if (!userUid.equals("default")) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("record_racha", record);
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(userUid)
                            .collection("Records")
                            .document("minijuegos")
                            .set(data, SetOptions.merge());
                }
            }
            tvClueDisplay.setText("¡CORRECTO! +10 pts");
            tvClueDisplay.setTextColor(Color.parseColor("#4ADE80"));
        } else {
            streak = 0;
            tvClueDisplay.setText("¡INCORRECTO! Era " + actualName);
            tvClueDisplay.setTextColor(Color.parseColor("#EF4444"));
        }

        updateScoreBoard();
        
        // Let video run its course, onCompletionListener will trigger loadNextRound
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
        tvClueDisplay.setText("TIPO: " + types.toString().trim());
        btnHintType.setEnabled(false);
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
        tvClueDisplay.setText("STATS -> HP: " + hp + " | ATK: " + atk);
        btnHintStat.setEnabled(false);
    }

    @SuppressLint("SetTextI18n")
    private void useHintCry() {
        if (!isRoundActive || correctPokemon == null) return;
        if (correctPokemon.cries != null && correctPokemon.cries.latest != null) {
            score -= 3;
            updateScoreBoard();
            tvClueDisplay.setText("ESCUCHANDO GRITO...");
            playCry(correctPokemon.cries.latest);
        } else {
            Toast.makeText(this, "Sin grito disponible", Toast.LENGTH_SHORT).show();
        }
        btnHintCry.setEnabled(false);
    }

    private void playCry(String url) {
        try {
            if (cryPlayer != null) {
                try {
                    if (cryPlayer.isPlaying()) cryPlayer.stop();
                } catch (Exception ignored) {}
                cryPlayer.release();
            }
            cryPlayer = new MediaPlayer();
            cryPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            cryPlayer.setDataSource(url);
            cryPlayer.prepareAsync();
            cryPlayer.setOnPreparedListener(mp -> {
                if (!isFinishing() && !isDestroyed()) mp.start();
            });
        } catch (Exception ignored) {}
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (vvWhosBackground != null && vvWhosBackground.isPlaying()) {
            vvWhosBackground.pause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        videoHandler.removeCallbacksAndMessages(null);
        if (vvWhosBackground != null && vvWhosBackground.isPlaying()) {
            vvWhosBackground.stopPlayback();
        }
        if (cryPlayer != null) {
            try {
                if (cryPlayer.isPlaying()) cryPlayer.stop();
                cryPlayer.release();
            } catch (Exception ignored) {}
            cryPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoHandler.removeCallbacksAndMessages(null);
        if (cryPlayer != null) {
            try {
                cryPlayer.release();
            } catch (Exception ignored) {}
            cryPlayer = null;
        }
    }
}
