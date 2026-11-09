package com.example.pokeappcesba;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class TypeMasterActivity extends AppCompatActivity {

    private ImageView btnBackType;
    private TextView tvTypeScore, tvTypeStreak, tvTypeRecord, tvQuestionText;
    private ProgressBar pbTimerType;
    private Button[] btnOptions = new Button[4];

    private int score = 0;
    private int streak = 0;
    private int record = 0;
    private String correctAnswer = "";
    private CountDownTimer countDownTimer;
    private boolean isAnswering = false;

    private SharedPreferences prefs;
    private MediaPlayer bgmPlayer;

    // Type relationship data
    private static final String[][] TYPE_MATCHUPS = {
            {"FUEGO", "AGUA"},       // Fuego es débil a Agua
            {"AGUA", "PLANTA"},      // Agua es débil a Planta
            {"PLANTA", "FUEGO"},     // Planta es débil a Fuego
            {"ELÉCTRICO", "TIERRA"},  // Eléctrico es débil a Tierra
            {"ROCA", "LUCHA"},       // Roca es débil a Lucha
            {"HIELO", "FUEGO"},      // Hielo es débil a Fuego
            {"VENENO", "PSIQUICO"},  // Veneno es débil a Psíquico
            {"FANTASMA", "FANTASMA"},// Fantasma es débil a Fantasma
            {"DRAGÓN", "HIELO"},     // Dragón es débil a Hielo
            {"VOLADOR", "ELÉCTRICO"},// Volador es débil a Eléctrico
            {"BICHO", "VOLADOR"}     // Bicho es débil a Volador
    };

    private static final String[] ALL_TYPES = {
            "FUEGO", "AGUA", "PLANTA", "ELÉCTRICO", "ROCA", "TIERRA", 
            "HIELO", "VENENO", "PSIQUICO", "VOLADOR", "BICHO", "LUCHA", "FANTASMA", "DRAGÓN"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_type_master);

        String userUid = "default";
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        prefs = getSharedPreferences("PokeMinijuegos_" + userUid, MODE_PRIVATE);
        record = prefs.getInt("maestro_record", 0);

        btnBackType = findViewById(R.id.btnBackType);
        tvTypeScore = findViewById(R.id.tvTypeScore);
        tvTypeStreak = findViewById(R.id.tvTypeStreak);
        tvTypeRecord = findViewById(R.id.tvTypeRecord);
        tvQuestionText = findViewById(R.id.tvQuestionText);
        pbTimerType = findViewById(R.id.pbTimerType);

        btnOptions[0] = findViewById(R.id.btnTypeOptA);
        btnOptions[1] = findViewById(R.id.btnTypeOptB);
        btnOptions[2] = findViewById(R.id.btnTypeOptC);
        btnOptions[3] = findViewById(R.id.btnTypeOptD);

        btnBackType.setOnClickListener(v -> finish());

        for (Button btn : btnOptions) {
            btn.setOnClickListener(v -> checkAnswer(btn.getText().toString()));
        }

        updateScoreBoard();
        loadNextQuestion();
        iniciarMusicaBatalla();
    }

    @SuppressLint("SetTextI18n")
    private void updateScoreBoard() {
        tvTypeScore.setText("PUNTOS: " + score);
        tvTypeStreak.setText("RACHA: " + streak);
        tvTypeRecord.setText("RÉCORD: " + record);
    }

    @SuppressLint("SetTextI18n")
    private void loadNextQuestion() {
        if (countDownTimer != null) countDownTimer.cancel();
        isAnswering = true;

        Random rand = new Random();
        int matchupIndex = rand.nextInt(TYPE_MATCHUPS.length);
        String targetType = TYPE_MATCHUPS[matchupIndex][0];
        correctAnswer = TYPE_MATCHUPS[matchupIndex][1];

        tvQuestionText.setText("¿Qué tipo es SÚPER EFECTIVO contra el tipo " + targetType + "?");

        List<String> options = new ArrayList<>();
        options.add(correctAnswer);

        while (options.size() < 4) {
            String randomType = ALL_TYPES[rand.nextInt(ALL_TYPES.length)];
            if (!options.contains(randomType)) {
                options.add(randomType);
            }
        }
        Collections.shuffle(options);

        for (int i = 0; i < 4; i++) {
            btnOptions[i].setText(options.get(i));
            btnOptions[i].setEnabled(true);
            btnOptions[i].setBackgroundResource(R.drawable.bg_button_retro);
            btnOptions[i].setBackgroundTintList(null);
        }

        pbTimerType.setProgress(100);
        countDownTimer = new CountDownTimer(10000, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                int progress = (int) (millisUntilFinished / 100);
                pbTimerType.setProgress(progress);
            }

            @Override
            public void onFinish() {
                if (isAnswering) {
                    onTimeOut();
                }
            }
        }.start();
    }

    private void checkAnswer(String chosen) {
        if (!isAnswering) return;
        isAnswering = false;
        if (countDownTimer != null) countDownTimer.cancel();

        for (Button btn : btnOptions) {
            btn.setEnabled(false);
            if (btn.getText().toString().equals(correctAnswer)) {
                btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#22C55E")));
            } else {
                btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#EF4444")));
            }
        }

        if (chosen.equals(correctAnswer)) {
            score += 15;
            streak++;
            if (streak > record) {
                record = streak;
                prefs.edit().putInt("maestro_record", record).apply();
            }
            new android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100).startTone(android.media.ToneGenerator.TONE_PROP_BEEP2, 150);
        } else {
            streak = 0;
            new android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100).startTone(android.media.ToneGenerator.TONE_CDMA_ABBR_ALERT, 200);
        }

        updateScoreBoard();
        pbTimerType.postDelayed(this::loadNextQuestion, 1500);
    }

    private void onTimeOut() {
        isAnswering = false;
        streak = 0;
        updateScoreBoard();
        new android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100).startTone(android.media.ToneGenerator.TONE_CDMA_ABBR_ALERT, 200);
        pbTimerType.postDelayed(this::loadNextQuestion, 1200);
    }

    @Override
    protected void onPause() {
        super.onPause();
        detenerMusicaBatalla();
    }

    @Override
    protected void onResume() {
        super.onResume();
        iniciarMusicaBatalla();
    }

    @Override
    protected void onStop() {
        super.onStop();
        detenerMusicaBatalla();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        detenerMusicaBatalla();
        if (countDownTimer != null) countDownTimer.cancel();
    }

    private void iniciarMusicaBatalla() {
        SharedPreferences prefs = getSharedPreferences("PokePrefs", MODE_PRIVATE);
        boolean soundEnabled = prefs.getBoolean("sound_enabled", true);
        if (soundEnabled) {
            try {
                if (bgmPlayer == null) {
                    int rawId = getResources().getIdentifier("bgm_wild_battle", "raw", getPackageName());
                    if (rawId != 0) {
                        bgmPlayer = MediaPlayer.create(this, rawId);
                        if (bgmPlayer != null) {
                            bgmPlayer.setLooping(true);
                            bgmPlayer.setVolume(0.35f, 0.35f);
                            bgmPlayer.start();
                        }
                    }
                } else if (!bgmPlayer.isPlaying()) {
                    bgmPlayer.start();
                }
            } catch (Exception ignored) {}
        }
    }

    private void detenerMusicaBatalla() {
        if (bgmPlayer != null) {
            try {
                if (bgmPlayer.isPlaying()) {
                    bgmPlayer.stop();
                }
                bgmPlayer.release();
            } catch (Exception ignored) {}
            bgmPlayer = null;
        }
    }
}
