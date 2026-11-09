package com.example.pokeappcesba;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.media.MediaPlayer;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.GridLayout;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class BattleEmulatorActivity extends AppCompatActivity {

    private LinearLayout llSelection;
    private ConstraintLayout clBattleArena;
    private EditText etP1, etP2;
    private Button btnPrepareBattle;

    private TextView tvP1Name, tvP2Name, tvP1Hp, tvP2Hp, tvBattleLog;
    private ProgressBar pbP1Hp, pbP2Hp;
    private ImageView ivP1Sprite, ivP2Sprite;
    private ImageView ivPreviewP1, ivPreviewP2;
    private ScrollView svLog;
    private PokeApiService apiService;
    private Pokemon p1, p2;
    private MediaPlayer mediaPlayer;
    private MediaPlayer bgmPlayer;
    
    private int p1MaxHp, p2MaxHp;
    private int p1CurrentHp, p2CurrentHp;
    private int turnNumber = 1;
    private boolean isP1Turn;
    private StringBuilder battleLogBuilder = new StringBuilder();
    private boolean battleEnded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battle_emulator);

        mediaPlayer = new MediaPlayer();
        apiService = new Retrofit.Builder().baseUrl("https://pokeapi.co/api/v2/").addConverterFactory(GsonConverterFactory.create()).build().create(PokeApiService.class);

        findViewById(R.id.btnBack).setOnClickListener(v -> mostrarConfirmacionSalida());
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                mostrarConfirmacionSalida();
            }
        });

        llSelection = findViewById(R.id.llSelection);
        clBattleArena = findViewById(R.id.clBattleArena);
        etP1 = findViewById(R.id.etP1);
        etP2 = findViewById(R.id.etP2);
        ivPreviewP1 = findViewById(R.id.ivPreviewP1);
        ivPreviewP2 = findViewById(R.id.ivPreviewP2);
        btnPrepareBattle = findViewById(R.id.btnPrepareBattle);

        tvP1Name = findViewById(R.id.tvP1Name);
        tvP2Name = findViewById(R.id.tvP2Name);
        tvP1Hp = findViewById(R.id.tvP1Hp);
        tvP2Hp = findViewById(R.id.tvP2Hp);
        tvBattleLog = findViewById(R.id.tvBattleLog);
        pbP1Hp = findViewById(R.id.pbP1Hp);
        pbP2Hp = findViewById(R.id.pbP2Hp);
        ivP1Sprite = findViewById(R.id.ivP1Sprite);
        ivP2Sprite = findViewById(R.id.ivP2Sprite);
        svLog = findViewById(R.id.svLog);

        setupPreviewListeners();

        btnPrepareBattle.setOnClickListener(v -> validateAndFetch());
    }

    private void mostrarConfirmacionSalida() {
        if (llSelection.getVisibility() == View.VISIBLE || battleEnded) {
            detenerTodoYSalir();
        } else {
            abrirDialogoRetro("⚠️ ¿ABANDONAR COMBATE?", 
                "¿Estás seguro de que quieres abandonar el combate actual y volver al menú principal? Se perderá el progreso de esta pelea.", 
                "🚪 SÍ, SALIR AL MENÚ", 
                "⚔️ CONTINUAR PELEANDO", 
                this::detenerTodoYSalir, 
                null, 
                0, null);
        }
    }

    private void detenerTodoYSalir() {
        battleEnded = true;
        detenerMusicaBatalla();
        if (ivP1Sprite != null) ivP1Sprite.animate().cancel();
        if (ivP2Sprite != null) ivP2Sprite.animate().cancel();
        
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.reset();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        detenerMusicaBatalla();
    }

    @Override
    protected void onStop() {
        super.onStop();
        detenerMusicaBatalla();
        if (isFinishing()) {
            detenerTodoYSalir();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        detenerMusicaBatalla();
        detenerTodoYSalir();
    }

    private void setupPreviewListeners() {
        Handler handler = new Handler();
        etP1.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(() -> loadPreview(s.toString().trim().toLowerCase(), ivPreviewP1), 800);
            }
        });
        etP2.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(() -> loadPreview(s.toString().trim().toLowerCase(), ivPreviewP2), 800);
            }
        });
    }

    private void loadPreview(String query, ImageView target) {
        if(query.isEmpty()) {
            target.setImageDrawable(null);
            return;
        }
        apiService.getPokemon(query).enqueue(new Callback<Pokemon>() {
            @Override
            public void onResponse(Call<Pokemon> call, Response<Pokemon> response) {
                if(response.isSuccessful() && response.body() != null) {
                    Glide.with(BattleEmulatorActivity.this).load(response.body().sprites.frontDefault).into(target);
                } else {
                    target.setImageDrawable(null);
                }
            }
            @Override
            public void onFailure(Call<Pokemon> call, Throwable t) { target.setImageDrawable(null); }
        });
    }

    private void validateAndFetch() {
        String q1 = etP1.getText().toString().trim().toLowerCase();
        String q2 = etP2.getText().toString().trim().toLowerCase();

        if (q1.isEmpty() || q2.isEmpty()) {
            Toast.makeText(this, "No dejes campos vacíos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (q1.equals(q2)) {
            Toast.makeText(this, "No puedes elegir el mismo Pokémon para pelear", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPrepareBattle.setEnabled(false);
        btnPrepareBattle.setText("Buscando Pokémon...");

        // Fetch P1
        apiService.getPokemon(q1).enqueue(new Callback<Pokemon>() {
            @Override
            public void onResponse(Call<Pokemon> call, Response<Pokemon> response) {
                if (response.isSuccessful() && response.body() != null) {
                    p1 = response.body();
                    // Fetch P2
                    apiService.getPokemon(q2).enqueue(new Callback<Pokemon>() {
                        @Override
                        public void onResponse(Call<Pokemon> call2, Response<Pokemon> response2) {
                            if (response2.isSuccessful() && response2.body() != null) {
                                p2 = response2.body();
                                prepareBattleArena();
                            } else {
                                handleError("El Pokémon 2 no existe.");
                            }
                        }
                        @Override
                        public void onFailure(Call<Pokemon> call2, Throwable t) { handleError("Error de red."); }
                    });
                } else {
                    handleError("El Pokémon 1 no existe.");
                }
            }
            @Override
            public void onFailure(Call<Pokemon> call, Throwable t) { handleError("Error de red."); }
        });
    }

    private void handleError(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        btnPrepareBattle.setEnabled(true);
        btnPrepareBattle.setText("⚔️ PREPARAR BATALLA ⚔️");
    }

    @SuppressLint("SetTextI18n")
    private void prepareBattleArena() {
        llSelection.setVisibility(View.GONE);
        clBattleArena.setVisibility(View.VISIBLE);
        
        btnPrepareBattle.setEnabled(true);
        btnPrepareBattle.setText("⚔️ PREPARAR BATALLA ⚔️");

        // Calculate HP from base stats (Base stat * 3 for a longer battle)
        p1MaxHp = getStat(p1, "hp") * 3;
        p2MaxHp = getStat(p2, "hp") * 3;
        
        tvP1Name.setText(p1.name);
        tvP2Name.setText(p2.name);

        String p1Sprite = (p1.sprites.backDefault != null) ? p1.sprites.backDefault : p1.sprites.frontDefault;
        String p2Sprite = p2.sprites.frontDefault;

        Glide.with(this).load(p1Sprite).into(ivP1Sprite);
        Glide.with(this).load(p2Sprite).into(ivP2Sprite);
        
        iniciarMusicaBatalla();

        startMatch();
    }

    private void startMatch() {
        p1CurrentHp = p1MaxHp;
        p2CurrentHp = p2MaxHp;
        turnNumber = 1;
        battleEnded = false;
        battleLogBuilder.setLength(0); // Clear log

        updateHpBars();

        int p1Speed = getStat(p1, "speed");
        int p2Speed = getStat(p2, "speed");

        isP1Turn = p1Speed >= p2Speed;

        String starter = isP1Turn ? p1.name.toUpperCase() : p2.name.toUpperCase();
        logMessage("¡Empieza la batalla!\nPor su mayor velocidad, " + starter + " ataca primero.\n");
        setupTurnUI();
    }

    private void setupTurnUI() {
        if (battleEnded) return;

        // Turno Automático después de 1.5 segundos (Emulador Automático)
        new Handler().postDelayed(() -> {
            if (!battleEnded) {
                Pokemon attacker = isP1Turn ? p1 : p2;
                String moveName = "Ataque Básico";
                if (attacker.moves != null && !attacker.moves.isEmpty()) {
                    int randomIndex = (int) (Math.random() * Math.min(4, attacker.moves.size()));
                    moveName = attacker.moves.get(randomIndex).move.name.toUpperCase();
                }
                executeTurn(moveName);
            }
        }, 1500);
    }

        // Quitar la función de setupMovesForP1 ya que todo es automático y los botones ya no existen.

    private void executeTurn(String moveName) {
        if (battleEnded) return;
        
        Pokemon attacker = isP1Turn ? p1 : p2;
        Pokemon defender = isP1Turn ? p2 : p1;
        
        // Reproducir Grito al Atacar
        if (attacker.cries != null && attacker.cries.latest != null) {
            reproducirSonido(attacker.cries.latest);
        }

        // Calculo de daño: Daño = (Ataque * 15 / Defensa) + aleatorio(1-6)
        int atk = getStat(attacker, "attack");
        int def = getStat(defender, "defense");
        int damage = Math.max(1, (atk * 15 / def) + (int)(Math.random() * 6));

        // Animación de ataque
        ImageView attackerImg = isP1Turn ? ivP1Sprite : ivP2Sprite;
        ImageView defenderImg = isP1Turn ? ivP2Sprite : ivP1Sprite;

        int moveX = isP1Turn ? 100 : -100;
        int moveY = isP1Turn ? -100 : 100;

        ViewPropertyAnimator anim = attackerImg.animate().translationX(moveX).translationY(moveY).setDuration(150);
        anim.withEndAction(() -> {
            attackerImg.animate().translationX(0).translationY(0).setDuration(150).start();
            
            // Impacto
            defenderImg.setColorFilter(Color.parseColor("#80FF0000")); // Rojo transparente
            defenderImg.postDelayed(defenderImg::clearColorFilter, 250);
            
            // Efecto de sonido de impacto
            new android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100).startTone(android.media.ToneGenerator.TONE_CDMA_ABBR_ALERT, 150);

            // Reducir HP
            if (isP1Turn) {
                p2CurrentHp = Math.max(0, p2CurrentHp - damage);
            } else {
                p1CurrentHp = Math.max(0, p1CurrentHp - damage);
            }

            logMessage("● Turno " + turnNumber);
            logMessage(attacker.name.toUpperCase() + " usó " + moveName + ".");
            logMessage("¡Causa " + damage + " puntos de daño!");
            
            int hpLeft = isP1Turn ? p2CurrentHp : p1CurrentHp;
            logMessage(defender.name.toUpperCase() + " queda con " + hpLeft + " HP.\n");

            updateHpBars();

            if (p1CurrentHp == 0 || p2CurrentHp == 0) {
                battleEnded = true;
                
                // Mostrar el modal de victoria directamente
                showWinnerDialog(p1CurrentHp > 0 ? p1 : p2, p1CurrentHp > 0 ? p1CurrentHp : p2CurrentHp);
            } else {
                turnNumber++;
                isP1Turn = !isP1Turn;
                setupTurnUI();
            }
        }).start();
    }


    private void updateHpBars() {
        pbP1Hp.setMax(p1MaxHp);
        pbP1Hp.setProgress(p1CurrentHp);
        tvP1Hp.setText("HP: " + p1CurrentHp + "/" + p1MaxHp);

        pbP2Hp.setMax(p2MaxHp);
        pbP2Hp.setProgress(p2CurrentHp);
        tvP2Hp.setText("HP: " + p2CurrentHp + "/" + p2MaxHp);
        
        // Cambiar color de barras si tienen poca vida
        pbP1Hp.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor(getHpColor(p1CurrentHp, p1MaxHp))));
        pbP2Hp.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor(getHpColor(p2CurrentHp, p2MaxHp))));
    }

    private String getHpColor(int current, int max) {
        float pct = (float) current / max;
        if (pct > 0.5f) return "#10B981"; // Verde
        if (pct > 0.2f) return "#F59E0B"; // Amarillo
        return "#EF4444"; // Rojo
    }

    private void logMessage(String msg) {
        battleLogBuilder.append(msg).append("\n");
        tvBattleLog.setText(battleLogBuilder.toString());
        svLog.post(() -> svLog.fullScroll(View.FOCUS_DOWN)); // Auto-scroll
    }

    private void showWinnerDialog(Pokemon winner, int hpLeft) {
        String fullLog = battleLogBuilder.toString();
        String[] logs = fullLog.split("●");
        String lastLog = "Batalla finalizada.";
        if(logs.length > 0) {
            lastLog = "Último turno:\n●" + logs[logs.length - 1].trim();
        }

        abrirDialogoRetro("🏆 ¡DUELO TERMINADO! 🏆", 
            "¡" + winner.name.toUpperCase() + " ha ganado el combate!\nTurnos totales: " + turnNumber + "\n\n" + lastLog, 
            "NUEVO COMBATE", 
            "SALIR AL MENÚ", 
            () -> {
                clBattleArena.setVisibility(View.GONE);
                llSelection.setVisibility(View.VISIBLE);
                etP1.setText("");
                etP2.setText("");
                detenerMusicaBatalla();
            }, 
            () -> {
                detenerMusicaBatalla();
                finish();
            }, 
            0, 
            winner);
            
        saveBattleHistory(winner.name.toUpperCase(), winner.name.equals(p1.name) ? p2.name.toUpperCase() : p1.name.toUpperCase());
    }

    private void saveBattleHistory(String winnerName, String loserName) {
        SharedPreferences prefsHistorial = getSharedPreferences("PokeHistorial", MODE_PRIVATE);
        String currentHistorial = prefsHistorial.getString("historial_batallas", "");
        String newRecord = "⚔️ " + winnerName + " ganó en " + turnNumber + " turnos vs " + loserName;
        prefsHistorial.edit().putString("historial_batallas", currentHistorial + newRecord + "\n").apply();
    }

    private int getStat(Pokemon p, String statName) {
        if (p.stats == null) return 50;
        for (Pokemon.StatSlot s : p.stats) {
            if (s.stat.name.equals(statName)) {
                return s.baseStat;
            }
        }
        return 50;
    }

    private void reproducirSonido(String url) {
        try {
            if (mediaPlayer != null) {
                try {
                    if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                } catch (Exception ignored) {}
                mediaPlayer.release();
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_GAME)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                if (!battleEnded && !isFinishing() && !isDestroyed()) mp.start();
            });
        } catch (Exception ignored) {}
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

    private void abrirDialogoRetro(String title, String message, String positiveText, String negativeText, Runnable onPositive, Runnable onNegative, int iconRes, Pokemon winnerPoke) {
        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_retro_custom);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));

        TextView tvTitle = dialog.findViewById(R.id.tvRetroDialogTitle);
        TextView tvMsg = dialog.findViewById(R.id.tvRetroDialogMessage);
        Button btnPos = dialog.findViewById(R.id.btnRetroPositive);
        Button btnNeg = dialog.findViewById(R.id.btnRetroNegative);
        ImageView ivIcon = dialog.findViewById(R.id.ivRetroDialogIcon);
        View flIconCont = dialog.findViewById(R.id.flRetroDialogIconContainer);

        tvTitle.setText(title);
        tvMsg.setText(message);
        btnPos.setText(positiveText);
        btnPos.setOnClickListener(v -> {
            dialog.dismiss();
            if (onPositive != null) onPositive.run();
        });

        if (negativeText != null) {
            btnNeg.setVisibility(View.VISIBLE);
            btnNeg.setText(negativeText);
            btnNeg.setOnClickListener(v -> {
                dialog.dismiss();
                if (onNegative != null) onNegative.run();
            });
        }

        if (iconRes != 0) {
            ivIcon.setImageResource(iconRes);
        } else if (winnerPoke != null && winnerPoke.sprites != null && winnerPoke.sprites.frontDefault != null) {
            Glide.with(this).load(winnerPoke.sprites.frontDefault).into(ivIcon);
        } else {
            flIconCont.setVisibility(View.GONE);
        }

        dialog.show();
    }
}