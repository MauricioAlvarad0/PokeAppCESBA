package com.example.pokeappcesba;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

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

public class TowerActivity extends AppCompatActivity {

    public static final String[] MEDALLAS_TORRE = {
            "Medalla 1", "Medalla 2", "Medalla 3", "Medalla 4", "Medalla 5",
            "Medalla 6", "Medalla 7", "Medalla 8", "Medalla 9", "Medalla 10"
    };

    private static final String[] MOVE_ICONS = {"🔥 ", "⚡ ", "💥 ", "⚔️ "};

    private PokeApiService apiService;
    private View llTeamSelection, clTowerArena, llMovesContainer;
    private GridLayout glPokemonSelection;
    private Button btnStartTower, btnRandomTeam;
    private GridLayout glMoves;
    private final Button[] btnMoves = new Button[4];
    private View viewLogTap;
    private TextView tvSubtitleSelection;

    private TextView tvTowerLevel, tvDocenteName, tvEnemyName, tvEnemyHpText, tvPlayerName, tvPlayerHpText, tvBattleLog, tvAliveCount;
    private TextView tvProjectile, tvFloatingDamage;
    private ImageView ivDocente, ivEnemyPokemon, ivPlayerPokemon;
    private ProgressBar pbEnemyHp, pbPlayerHp;
    private ScrollView svBattleLog;

    // State
    private final List<BattleEngine.BattlePokemon> selectionPool = new ArrayList<>();
    private final List<BattleEngine.BattlePokemon> playerTeam = new ArrayList<>();
    private final List<BattleEngine.BattlePokemon> enemyTeam = new ArrayList<>();

    private int currentLevel = 1; // 1 to 6
    private int currentPlayerIndex = 0;
    private int currentEnemyIndex = 0;

    private SharedPreferences prefsMedallas;
    private SharedPreferences prefsGlobales;
    private String userUid = "default";
    private Typeface pixelFont;

    private boolean battleEnded = false;
    private final android.os.Handler battleHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private MediaPlayer mediaPlayer;
    private MediaPlayer bgmPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tower);

        findViewById(R.id.btnBackTower).setOnClickListener(v -> mostrarConfirmacionSalida());
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                mostrarConfirmacionSalida();
            }
        });

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        prefsMedallas = getSharedPreferences("PokeMedallas_" + userUid, MODE_PRIVATE);
        prefsGlobales = getSharedPreferences("PokePrefs", MODE_PRIVATE);
        pixelFont = ResourcesCompat.getFont(this, R.font.pokemon_pixel);

        apiService = new Retrofit.Builder()
                .baseUrl("https://pokeapi.co/api/v2/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PokeApiService.class);

        initViews();
        loadSelectionPool();
    }

    private void mostrarConfirmacionSalida() {
        if (llTeamSelection.getVisibility() == View.VISIBLE || battleEnded) {
            detenerTodoYSalir();
        } else {
            abrirDialogoRetro("⚠️ ¿ABANDONAR COMBATE?", 
                "¿Estás seguro de que quieres abandonar el combate actual y volver al menú principal? Se perderá el progreso de esta pelea.", 
                "🚪 SÍ, SALIR AL MENÚ", 
                "⚔️ CONTINUAR PELEANDO", 
                this::detenerTodoYSalir, 
                null,
                0);
        }
    }

    private void detenerTodoYSalir() {
        battleEnded = true;
        detenerMusicaBatalla();
        battleHandler.removeCallbacksAndMessages(null);
        if (ivPlayerPokemon != null) ivPlayerPokemon.animate().cancel();
        if (ivEnemyPokemon != null) ivEnemyPokemon.animate().cancel();

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

    private void initViews() {
        llTeamSelection = findViewById(R.id.llTeamSelection);
        clTowerArena = findViewById(R.id.clTowerArena);
        glPokemonSelection = findViewById(R.id.glPokemonSelection);
        btnStartTower = findViewById(R.id.btnStartTower);
        btnRandomTeam = findViewById(R.id.btnRandomTeam);
        tvSubtitleSelection = findViewById(R.id.tvSubtitleSelection);

        tvTowerLevel = findViewById(R.id.tvTowerLevel);
        tvDocenteName = findViewById(R.id.tvDocenteName);
        ivDocente = findViewById(R.id.ivDocente);
        tvAliveCount = findViewById(R.id.tvAliveCount);

        tvEnemyName = findViewById(R.id.tvEnemyName);
        pbEnemyHp = findViewById(R.id.pbEnemyHp);
        ivEnemyPokemon = findViewById(R.id.ivEnemyPokemon);

        tvPlayerName = findViewById(R.id.tvPlayerName);
        tvPlayerHpText = findViewById(R.id.tvPlayerHpText);
        pbPlayerHp = findViewById(R.id.pbPlayerHp);
        ivPlayerPokemon = findViewById(R.id.ivPlayerPokemon);

        tvProjectile = findViewById(R.id.tvProjectile);
        tvFloatingDamage = findViewById(R.id.tvFloatingDamage);

        tvEnemyHpText = findViewById(R.id.tvEnemyHpText);
        findViewById(R.id.btnSwitchTowerPokemon).setOnClickListener(v -> abrirDialogoRelevo());

        tvBattleLog = findViewById(R.id.tvBattleLog);
        svBattleLog = findViewById(R.id.svBattleLog);

        llMovesContainer = findViewById(R.id.llMovesContainer);
        glMoves = findViewById(R.id.glMoves);
        btnMoves[0] = findViewById(R.id.btnMove1);
        btnMoves[1] = findViewById(R.id.btnMove2);
        btnMoves[2] = findViewById(R.id.btnMove3);
        btnMoves[3] = findViewById(R.id.btnMove4);
        viewLogTap = findViewById(R.id.viewLogTap);

        btnStartTower.setOnClickListener(v -> startTower());

        viewLogTap.setOnClickListener(v -> {
            if (llMovesContainer.getVisibility() == View.GONE && btnMoves[0].isEnabled()) {
                svBattleLog.setVisibility(View.GONE);
                llMovesContainer.setVisibility(View.VISIBLE);
                viewLogTap.setVisibility(View.GONE);
            }
        });

        for (int i = 0; i < 4; i++) {
            final int index = i;
            btnMoves[i].setOnClickListener(v -> {
                String rawText = btnMoves[index].getText().toString();
                if (rawText.equals("-")) return;
                // Limpiar icono decorativo del nombre del ataque
                String cleanMove = rawText.replace("🔥 ", "").replace("⚡ ", "").replace("💥 ", "").replace("⚔️ ", "").trim();
                llMovesContainer.setVisibility(View.GONE);
                svBattleLog.setVisibility(View.VISIBLE);
                viewLogTap.setVisibility(View.VISIBLE);
                playTurn(cleanMove);
            });
        }

        View.OnClickListener pickRandomThreeListener = v -> {
            if (selectionPool.size() == 6) {
                playerTeam.clear();
                List<BattleEngine.BattlePokemon> tempPool = new ArrayList<>(selectionPool);
                Collections.shuffle(tempPool);
                for (int i = 0; i < 3; i++) {
                    playerTeam.add(tempPool.get(i));
                }
                updateSelectionUI();
            }
        };

        btnRandomTeam.setOnClickListener(pickRandomThreeListener);
        btnRandomTeam.setOnLongClickListener(v -> {
            loadSelectionPool();
            return true;
        });

        btnRandomTeam.setOnLongClickListener(v -> {
            loadSelectionPool();
            return true;
        });
    }

    private void loadSelectionPool() {
        selectionPool.clear();
        playerTeam.clear();
        glPokemonSelection.removeAllViews();
        btnStartTower.setEnabled(false);
        btnStartTower.setText("🔥 ¡INICIAR TORRE (0/3)! 🔥");
        tvSubtitleSelection.setText("¡ELIGE 3 COMBATIENTES!\n(0/3)");

        Random rand = new Random();
        List<Integer> chosenIds = new ArrayList<>();
        while (chosenIds.size() < 6) {
            int randomId = rand.nextInt(251) + 1;
            if (!chosenIds.contains(randomId)) {
                chosenIds.add(randomId);
            }
        }

        for (int randomId : chosenIds) {
            apiService.getPokemon(String.valueOf(randomId)).enqueue(new Callback<Pokemon>() {
                @Override
                public void onResponse(Call<Pokemon> call, Response<Pokemon> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        selectionPool.add(new BattleEngine.BattlePokemon(response.body()));
                        if (selectionPool.size() == 6) {
                            updateSelectionUI();
                        }
                    }
                }

                @Override
                public void onFailure(Call<Pokemon> call, Throwable t) {}
            });
        }
    }

    private void updateSelectionUI() {
        if (selectionPool.size() < 6) return;

        glPokemonSelection.removeAllViews();
        for (BattleEngine.BattlePokemon bp : selectionPool) {
            boolean isSelected = playerTeam.contains(bp);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setBackgroundResource(isSelected ? R.drawable.bg_gold_frame : R.drawable.bg_input_retro);
            card.setPadding(8, 8, 8, 8);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = 0;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(6, 6, 6, 6);
            card.setLayoutParams(params);

            TextView tvCheck = new TextView(this);
            if (pixelFont != null) tvCheck.setTypeface(pixelFont, Typeface.BOLD);
            int orderIndex = playerTeam.indexOf(bp);
            String label = "";
            if (isSelected) {
                if (orderIndex == 0) label = "⭐ LÍDER (1°)";
                else if (orderIndex == 1) label = "🛡️ 2° EN EQUIPO";
                else label = "⚔️ 3° EN EQUIPO";
            }
            tvCheck.setText(label);
            tvCheck.setTextColor(Color.parseColor("#FACC15"));
            tvCheck.setTextSize(10f);
            tvCheck.setGravity(Gravity.CENTER);
            tvCheck.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);
            card.addView(tvCheck);

            ImageView iv = new ImageView(this);
            LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
            iv.setLayoutParams(imgParams);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            if (bp.pokemon.sprites != null && bp.pokemon.sprites.frontDefault != null) {
                Glide.with(this).load(bp.pokemon.sprites.frontDefault).into(iv);
            }
            card.addView(iv);

            TextView tvName = new TextView(this);
            if (pixelFont != null) tvName.setTypeface(pixelFont, Typeface.BOLD);
            tvName.setText(bp.pokemon.name.toUpperCase());
            tvName.setTextSize(12f);
            tvName.setGravity(Gravity.CENTER);
            tvName.setSingleLine(true);
            tvName.setPadding(6, 4, 6, 4);
            tvName.setBackgroundColor(Color.parseColor("#1E293B"));
            tvName.setTextColor(isSelected ? Color.parseColor("#FACC15") : Color.parseColor("#4ADE80"));
            card.addView(tvName);

            card.setOnClickListener(v -> {
                if (playerTeam.contains(bp)) {
                    playerTeam.remove(bp);
                } else if (playerTeam.size() < 3) {
                    playerTeam.add(bp);
                }
                updateSelectionUI();
            });

            glPokemonSelection.addView(card);
        }

        tvSubtitleSelection.setText("¡ELIGE 3 COMBATIENTES!\n(" + playerTeam.size() + "/3)");
        btnStartTower.setText("🔥 ¡INICIAR TORRE (" + playerTeam.size() + "/3)! 🔥");
        btnStartTower.setEnabled(playerTeam.size() == 3);
    }

    private void startTower() {
        if (playerTeam.size() < 3) return;
        battleEnded = false;
        llTeamSelection.setVisibility(View.GONE);
        clTowerArena.setVisibility(View.VISIBLE);
        currentLevel = 1;
        startLevel();
    }

    private void startLevel() {
        mostrarHistoriaNivel(currentLevel, () -> {
            currentPlayerIndex = 0;
            currentEnemyIndex = 0;
            enemyTeam.clear();

            for (BattleEngine.BattlePokemon bp : playerTeam) {
                bp.currentHp = bp.maxHp;
            }

            if (currentLevel < 6) {
                tvTowerLevel.setText("NIVEL " + currentLevel + " DE 6");
                tvDocenteName.setText("Docente CESBA " + currentLevel);
                int resId = getResources().getIdentifier("docente_cesba_" + currentLevel, "drawable", getPackageName());
                ivDocente.setImageResource(resId != 0 ? resId : R.drawable.ic_mi_perfil);
            } else {
                tvTowerLevel.setText("NIVEL 6 - JEFE FINAL");
                tvDocenteName.setText("Prof. Víctor (Jefe Final)");
                int resId = getResources().getIdentifier("docente_cesba_6", "drawable", getPackageName());
                ivDocente.setImageResource(resId != 0 ? resId : R.drawable.ic_mi_perfil);
            }

            updateAliveCount();
            tvBattleLog.setText("¡Nivel " + currentLevel + " empieza!");
            battleEnded = false;

            iniciarMusicaBatalla();

            int enemyCount = (currentLevel == 6) ? 4 : 3;
            loadEnemyTeam(enemyCount, currentLevel == 6);
        });
    }

    private void mostrarHistoriaNivel(int level, Runnable onStartBattle) {
        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_story_intro);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }

        View llBossCard = dialog.findViewById(R.id.llBossCard);
        TextView tvStoryFloorTitle = dialog.findViewById(R.id.tvStoryFloorTitle);
        ImageView ivStoryTeacher = dialog.findViewById(R.id.ivStoryTeacher);
        TextView tvStoryTeacherName = dialog.findViewById(R.id.tvStoryTeacherName);
        TextView tvStoryBossTeam = dialog.findViewById(R.id.tvStoryBossTeam);
        TextView tvStoryDialogText = dialog.findViewById(R.id.tvStoryDialogText);
        Button btnAcceptMission = dialog.findViewById(R.id.btnAcceptMission);

        int resId = getResources().getIdentifier("docente_cesba_" + level, "drawable", getPackageName());
        ivStoryTeacher.setImageResource(resId != 0 ? resId : R.drawable.ic_mi_perfil);
        
        String fullText = "";

        if (level == 6) {
            tvStoryFloorTitle.setText("⚠️ PISO 6 DE 6 · NÚCLEO DE LA TORRE ⚠️");
            tvStoryTeacherName.setText("PROF. VÍCTOR (JEFE FINAL)");
            tvStoryBossTeam.setText("Equipo Rival: 🔴 🔴 🔴 🔴");
            fullText = "¡Llegaste al núcleo de la Torre CESBA! El sistema invocó a 4 Pokémon Legendarios. ¡Vénceme en esta batalla final para salvar el día y consagrarte Campeón!";
        } else {
            tvStoryTeacherName.setText("DOCENTE CESBA " + level);
            tvStoryBossTeam.setText("Equipo Rival: 🔴 🔴 🔴");
            switch (level) {
                case 1:
                    tvStoryFloorTitle.setText("⚠️ PISO 1 DE 6 · EMERGENCIA CESBA ⚠️");
                    fullText = "¡Emergencia en el Campus CESBA! Un virus descontroló el Laboratorio 1 y bloqueó los accesos. ¡Derrota a mi equipo para restaurar la red y avanzar!";
                    break;
                case 2:
                    tvStoryFloorTitle.setText("⚠️ PISO 2 DE 6 · ATAQUE AL SERVIDOR ⚠️");
                    fullText = "¡El servidor de Base de Datos está bajo ataque! Necesito comprobar que tu equipo tiene la fuerza suficiente para cruzar el cortafuegos del Piso 2.";
                    break;
                case 3:
                    tvStoryFloorTitle.setText("⚠️ PISO 3 DE 6 · CAÍDA DEL SISTEMA ⚠️");
                    fullText = "¡La mitad del campus se quedó sin sistema! Si logras superar mi estrategia en este piso, te daré las credenciales hacia la torre central.";
                    break;
                case 4:
                    tvStoryFloorTitle.setText("⚠️ PISO 4 DE 6 · ANOMALÍA DIGITAL ⚠️");
                    fullText = "¡Cuidado, entrenador! La anomalía digital se fortalece. ¡Vence a mis Pokémon para proteger el código fuente de la universidad!";
                    break;
                case 5:
                    tvStoryFloorTitle.setText("⚠️ PISO 5 DE 6 · LA ÚLTIMA BARRERA ⚠️");
                    fullText = "¡Estás a un paso de la cima! Soy la última barrera antes del núcleo principal. ¡Demuestra que puedes salvar el semestre!";
                    break;
            }
        }
        
        // Animación de entrada
        llBossCard.setScaleX(0.8f);
        llBossCard.setScaleY(0.8f);
        llBossCard.setAlpha(0f);
        llBossCard.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(400).setInterpolator(new android.view.animation.OvershootInterpolator()).start();

        // Efecto Typewriter
        tvStoryDialogText.setText("");
        String finalText = fullText;
        Runnable typeWriter = new Runnable() {
            int index = 0;
            @Override
            public void run() {
                if (index <= finalText.length()) {
                    if (battleEnded || isFinishing() || isDestroyed()) return;
                    tvStoryDialogText.setText(finalText.substring(0, index));
                    index++;
                    battleHandler.postDelayed(this, 30); // 30ms per char
                }
            }
        };
        battleHandler.postDelayed(typeWriter, 400); // Start after card animation

        btnAcceptMission.setOnClickListener(v -> {
            battleHandler.removeCallbacks(typeWriter);
            dialog.dismiss();
            onStartBattle.run();
        });

        dialog.show();
    }

    private void updateAliveCount() {
        int pAlive = 0;
        for (BattleEngine.BattlePokemon p : playerTeam) if (!p.isFainted()) pAlive++;
        int eAlive = 0;
        for (BattleEngine.BattlePokemon e : enemyTeam) if (!e.isFainted()) eAlive++;
        int eTotal = (currentLevel == 6) ? 4 : 3;
        if (enemyTeam.isEmpty()) eAlive = eTotal;
        tvAliveCount.setText("Tú: " + pAlive + " | Ene: " + eAlive);
    }

    private void loadEnemyTeam(int count, boolean isBoss) {
        setAttackButtonsEnabled(false);
        logBattle("Generando equipo enemigo...");
        Random rand = new Random();
        for (int i = 0; i < count; i++) {
            int randomId = rand.nextInt(251) + 1;
            if (isBoss) {
                randomId = BattleEngine.LEGENDARY_IDS[rand.nextInt(BattleEngine.LEGENDARY_IDS.length)];
            }
            apiService.getPokemon(String.valueOf(randomId)).enqueue(new Callback<Pokemon>() {
                @Override
                public void onResponse(Call<Pokemon> call, Response<Pokemon> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        enemyTeam.add(new BattleEngine.BattlePokemon(response.body()));
                        if (enemyTeam.size() == count) {
                            setupArena();
                        }
                    }
                }

                @Override
                public void onFailure(Call<Pokemon> call, Throwable t) {}
            });
        }
    }

    private void setupArena() {
        updateArenaUI();
        updateAliveCount();
        logBattle("¡Enemigo envió a " + getActiveEnemy().pokemon.name.toUpperCase() + "!");
        playCry(getActiveEnemy().pokemon);
        // Mostrar directamente el menú de ataques estilizado para que el jugador elija
        svBattleLog.setVisibility(View.GONE);
        llMovesContainer.setVisibility(View.VISIBLE);
        setAttackButtonsEnabled(true);
    }

    private BattleEngine.BattlePokemon getActivePlayer() {
        return playerTeam.get(currentPlayerIndex);
    }

    private BattleEngine.BattlePokemon getActiveEnemy() {
        return enemyTeam.get(currentEnemyIndex);
    }

    private void updateArenaUI() {
        BattleEngine.BattlePokemon pPlayer = getActivePlayer();
        BattleEngine.BattlePokemon pEnemy = getActiveEnemy();

        tvPlayerName.setText(pPlayer.pokemon.name.toUpperCase());
        pbPlayerHp.setMax(pPlayer.maxHp);
        pbPlayerHp.setProgress(Math.max(0, pPlayer.currentHp));
        tvPlayerHpText.setText("HP: " + Math.max(0, pPlayer.currentHp) + "/" + pPlayer.maxHp);
        if (pPlayer.pokemon.sprites != null) {
            String playerSprite = pPlayer.pokemon.sprites.backDefault != null
                    ? pPlayer.pokemon.sprites.backDefault
                    : pPlayer.pokemon.sprites.frontDefault;
            Glide.with(this).load(playerSprite).into(ivPlayerPokemon);
        }

        tvEnemyName.setText(pEnemy.pokemon.name.toUpperCase());
        pbEnemyHp.setMax(pEnemy.maxHp);
        pbEnemyHp.setProgress(Math.max(0, pEnemy.currentHp));
        if (tvEnemyHpText != null) {
            tvEnemyHpText.setText("HP: " + Math.max(0, pEnemy.currentHp) + "/" + pEnemy.maxHp);
        }
        if (pEnemy.pokemon.sprites != null && pEnemy.pokemon.sprites.frontDefault != null) {
            Glide.with(this).load(pEnemy.pokemon.sprites.frontDefault).into(ivEnemyPokemon);
        }
        updateAliveCount();

        for (int i = 0; i < 4; i++) {
            if (i < pPlayer.moves.size()) {
                btnMoves[i].setText(MOVE_ICONS[i] + pPlayer.moves.get(i).toUpperCase());
                btnMoves[i].setEnabled(true);
            } else {
                btnMoves[i].setText("-");
                btnMoves[i].setEnabled(false);
            }
        }
    }

    private void playTurn(String playerMove) {
        setAttackButtonsEnabled(false);
        BattleEngine.BattlePokemon pPlayer = getActivePlayer();
        BattleEngine.BattlePokemon pEnemy = getActiveEnemy();

        boolean playerGoesFirst = BattleEngine.determineFirstAttacker(pPlayer, pEnemy);

        if (playerGoesFirst) {
            executeAttack(pPlayer, pEnemy, true, playerMove, () -> {
                if (!pEnemy.isFainted()) {
                    executeAttack(pEnemy, pPlayer, false, null, this::checkTurnEnd);
                } else {
                    checkTurnEnd();
                }
            });
        } else {
            executeAttack(pEnemy, pPlayer, false, null, () -> {
                if (!pPlayer.isFainted()) {
                    executeAttack(pPlayer, pEnemy, true, playerMove, this::checkTurnEnd);
                } else {
                    checkTurnEnd();
                }
            });
        }
    }

    private void setAttackButtonsEnabled(boolean enabled) {
        for (Button btn : btnMoves) {
            if (!btn.getText().toString().equals("-")) {
                btn.setEnabled(enabled);
            }
        }
        if (enabled && llMovesContainer.getVisibility() != View.VISIBLE) {
            viewLogTap.setVisibility(View.VISIBLE);
            logBattle("¡Es tu turno! (Toca aquí para elegir ataque)");
        } else {
            viewLogTap.setVisibility(View.GONE);
        }
    }

    private void executeAttack(BattleEngine.BattlePokemon attacker, BattleEngine.BattlePokemon defender, boolean isPlayerAttacking, String moveUsed, Runnable onComplete) {
        if (battleEnded || isFinishing() || isDestroyed()) return;
        String move = moveUsed != null ? moveUsed : BattleEngine.getRandomMove(attacker);
        int dmg = BattleEngine.calculateDamage(attacker.attack, defender.defense);
        defender.currentHp = Math.max(0, defender.currentHp - dmg);

        logBattle(attacker.pokemon.name.toUpperCase() + " usó " + move + ".");
        playCry(attacker.pokemon);

        ImageView attackerView = isPlayerAttacking ? ivPlayerPokemon : ivEnemyPokemon;
        ImageView defenderView = isPlayerAttacking ? ivEnemyPokemon : ivPlayerPokemon;

        // Ejecutar animación completa: Embestida + Lanzamiento de proyectil + Golpe con sacudida
        animateAttackAndImpact(attackerView, defenderView, isPlayerAttacking, dmg, () -> {
            new android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100).startTone(android.media.ToneGenerator.TONE_CDMA_ABBR_ALERT, 150);
            logBattle("¡Causa " + dmg + " puntos de daño!");
            logBattle(defender.pokemon.name.toUpperCase() + " queda con " + defender.currentHp + " HP.");
            updateArenaUI();
            battleHandler.postDelayed(() -> {
                if (!battleEnded && !isFinishing() && !isDestroyed()) onComplete.run();
            }, 550);
        });
    }

    private void animateAttackAndImpact(ImageView attackerView, ImageView defenderView, boolean isPlayerAttacking, int damage, Runnable onAnimationFinished) {
        float lungeX = isPlayerAttacking ? 65f : -65f;
        float lungeY = isPlayerAttacking ? -55f : 55f;

        // 1. Embestida rápida del atacante
        attackerView.animate()
                .translationX(lungeX)
                .translationY(lungeY)
                .scaleX(1.15f)
                .scaleY(1.15f)
                .setDuration(160)
                .withEndAction(() -> {
                    attackerView.animate()
                            .translationX(0f)
                            .translationY(0f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(160)
                            .start();

                    // 2. Lanzar proyectil desde el centro del atacante hasta el centro del defensor
                    float startX = attackerView.getX() + (attackerView.getWidth() / 2f) - 40f;
                    float startY = attackerView.getY() + (attackerView.getHeight() / 2f) - 40f;
                    float endX = defenderView.getX() + (defenderView.getWidth() / 2f) - 40f;
                    float endY = defenderView.getY() + (defenderView.getHeight() / 2f) - 40f;

                    tvProjectile.setText(isPlayerAttacking ? "🔥" : "⚡");
                    tvProjectile.setX(startX);
                    tvProjectile.setY(startY);
                    tvProjectile.setScaleX(0.6f);
                    tvProjectile.setScaleY(0.6f);
                    tvProjectile.setRotation(0f);
                    tvProjectile.setVisibility(View.VISIBLE);

                    tvProjectile.animate()
                            .x(endX)
                            .y(endY)
                            .scaleX(1.4f)
                            .scaleY(1.4f)
                            .rotation(360f)
                            .setDuration(280)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .withEndAction(() -> {
                                // 3. Impacto: Explosión + Destello Rojo + Sacudida (Shake) + Daño Flotante
                                tvProjectile.setText("💥");
                                defenderView.setColorFilter(Color.argb(180, 255, 40, 40));

                                // Mostrar daño flotante "-XX HP" subiendo sobre el rival
                                tvFloatingDamage.setText("-" + damage + " HP");
                                tvFloatingDamage.setX(endX - 20f);
                                tvFloatingDamage.setY(endY - 20f);
                                tvFloatingDamage.setAlpha(1f);
                                tvFloatingDamage.setVisibility(View.VISIBLE);
                                tvFloatingDamage.animate()
                                        .y(endY - 110f)
                                        .alpha(0f)
                                        .setDuration(600)
                                        .withEndAction(() -> tvFloatingDamage.setVisibility(View.GONE))
                                        .start();

                                // Sacudida lateral de golpe en el Pokémon defensor
                                defenderView.animate().translationX(28f).setDuration(50).withEndAction(() ->
                                        defenderView.animate().translationX(-28f).setDuration(50).withEndAction(() ->
                                                defenderView.animate().translationX(18f).setDuration(50).withEndAction(() ->
                                                        defenderView.animate().translationX(-18f).setDuration(50).withEndAction(() ->
                                                                defenderView.animate().translationX(0f).setDuration(50).withEndAction(() -> {
                                                                    defenderView.clearColorFilter();
                                                                    tvProjectile.setVisibility(View.GONE);
                                                                    onAnimationFinished.run();
                                                                }).start()
                                                        ).start()
                                                ).start()
                                        ).start()
                                ).start();
                            })
                            .start();
                })
                .start();
    }

    private void checkTurnEnd() {
        if (getActiveEnemy().isFainted()) {
            logBattle("¡" + getActiveEnemy().pokemon.name.toUpperCase() + " ha caído!");
            currentEnemyIndex++;
            updateAliveCount();
            if (currentEnemyIndex < enemyTeam.size()) {
                logBattle("Enemigo envía a " + getActiveEnemy().pokemon.name.toUpperCase());
                playCry(getActiveEnemy().pokemon);
                updateArenaUI();
                setAttackButtonsEnabled(true);
            } else {
                levelComplete();
            }
        } else if (getActivePlayer().isFainted()) {
            logBattle("¡" + getActivePlayer().pokemon.name.toUpperCase() + " ha caído!");
            currentPlayerIndex++;
            updateAliveCount();
            if (currentPlayerIndex < playerTeam.size()) {
                logBattle("¡Adelante, " + getActivePlayer().pokemon.name.toUpperCase() + "!");
                playCry(getActivePlayer().pokemon);
                updateArenaUI();
                setAttackButtonsEnabled(true);
            } else {
                gameOver();
            }
        } else {
            // Al terminar el intercambio de golpes, volver a mostrar los 4 botones de ataque tras una breve pausa
            battleHandler.postDelayed(() -> {
                if (battleEnded || isFinishing() || isDestroyed()) return;
                if (!getActivePlayer().isFainted() && !getActiveEnemy().isFainted()) {
                    svBattleLog.setVisibility(View.GONE);
                    llMovesContainer.setVisibility(View.VISIBLE);
                    setAttackButtonsEnabled(true);
                }
            }, 900);
        }
    }

    private void levelComplete() {
        if (currentLevel == 6) {
            campaignWon();
        } else {
            int superado = currentLevel;
            currentLevel++;
            int resId = getResources().getIdentifier("docente_cesba_" + superado, "drawable", getPackageName());
            abrirDialogoRetro("🔥 ¡PISO " + superado + " SUPERADO! 🔥",
                "Subiendo al siguiente nivel de la Torre...",
                "⬆️ ¡ASCENDER AL PISO " + currentLevel + "!",
                null,
                this::startLevel,
                null,
                resId != 0 ? resId : R.drawable.ic_mi_perfil);
        }
    }

    private void gameOver() {
        detenerMusicaBatalla();
        abrirDialogoRetro("¡GAME OVER!",
            "Tu equipo ha sido derrotado. La campaña termina aquí.",
            "SALIR AL MENÚ",
            null,
            this::finish,
            null,
            R.drawable.docente_cesba_1); // or any other icon
    }

    private void abrirDialogoRetro(String title, String message, String positiveText, String negativeText, Runnable onPositive, Runnable onNegative, int iconRes) {
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
        } else {
            flIconCont.setVisibility(View.GONE);
        }

        dialog.show();
    }

    private void abrirDialogoRelevo() {
        if (battleEnded || isFinishing() || isDestroyed()) return;
        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_retro_custom);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));

        TextView tvTitle = dialog.findViewById(R.id.tvRetroDialogTitle);
        TextView tvMsg = dialog.findViewById(R.id.tvRetroDialogMessage);
        Button btnPos = dialog.findViewById(R.id.btnRetroPositive);
        Button btnNeg = dialog.findViewById(R.id.btnRetroNegative);
        View flIconCont = dialog.findViewById(R.id.flRetroDialogIconContainer);
        LinearLayout llOptions = dialog.findViewById(R.id.llRetroDialogOptions);

        tvTitle.setText("🔄 CAMBIAR POKÉMON");
        tvMsg.setText("Selecciona el Pokémon que entrará a la batalla:");
        btnPos.setVisibility(View.GONE);
        flIconCont.setVisibility(View.GONE);
        llOptions.setVisibility(View.VISIBLE);
        
        btnNeg.setVisibility(View.VISIBLE);
        btnNeg.setOnClickListener(v -> dialog.dismiss());
        // Si no tiene pokemon vivo actual, ocultar cancelar
        if (getActivePlayer().isFainted()) {
            btnNeg.setVisibility(View.GONE);
            tvMsg.setText("¡Tu Pokémon ha caído! Selecciona tu relevo:");
        }

        for (int i = 0; i < playerTeam.size(); i++) {
            BattleEngine.BattlePokemon bp = playerTeam.get(i);
            if (bp.isFainted() || i == currentPlayerIndex) continue;

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setBackgroundResource(R.drawable.bg_input_retro);
            card.setPadding(16, 16, 16, 16);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 16);
            card.setLayoutParams(lp);
            card.setGravity(Gravity.CENTER_VERTICAL);

            ImageView iv = new ImageView(this);
            iv.setLayoutParams(new LinearLayout.LayoutParams(100, 100));
            iv.setBackgroundResource(R.drawable.bg_gold_frame);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setPadding(8,8,8,8);
            if (bp.pokemon.sprites != null && bp.pokemon.sprites.frontDefault != null) {
                Glide.with(this).load(bp.pokemon.sprites.frontDefault).into(iv);
            }
            card.addView(iv);

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setPadding(16, 0, 0, 0);

            TextView tvName = new TextView(this);
            if (pixelFont != null) tvName.setTypeface(pixelFont, Typeface.BOLD);
            tvName.setText(bp.pokemon.name.toUpperCase());
            tvName.setTextColor(Color.WHITE);
            tvName.setTextSize(16f);

            TextView tvHp = new TextView(this);
            if (pixelFont != null) tvHp.setTypeface(pixelFont);
            tvHp.setText("HP: " + Math.max(0, bp.currentHp) + "/" + bp.maxHp);
            tvHp.setTextColor(Color.parseColor("#4ADE80"));
            tvHp.setTextSize(12f);

            info.addView(tvName);
            info.addView(tvHp);
            card.addView(info);

            final int chosenIndex = i;
            card.setOnClickListener(v -> {
                dialog.dismiss();
                currentPlayerIndex = chosenIndex;
                logBattle("¡Adelante, " + bp.pokemon.name.toUpperCase() + "!");
                playCry(bp.pokemon);
                updateArenaUI();
                setAttackButtonsEnabled(true);
            });

            llOptions.addView(card);
        }
        dialog.show();
    }

    private void campaignWon() {
        detenerMusicaBatalla();
        int medals = prefsMedallas.getInt("medals_count", 0);
        if (medals < 10) {
            medals++;
            prefsMedallas.edit().putInt("medals_count", medals).apply();
            prefsGlobales.edit().putInt("torre_medallas", medals).apply();
        }

        abrirDialogoRetro("🏆 ¡CAMPAÑA COMPLETADA! 🏆",
            "¡Has vencido al Jefe Final y sus 4 Legendarios!\n\n🎖️ Medallas desbloqueadas: " + medals + " / 10",
            "¡ÉPICO!",
            null,
            this::finish,
            null,
            R.drawable.ic_medallas);
    }

    private void logBattle(String text) {
        String currentText = tvBattleLog.getText().toString();
        tvBattleLog.setText(currentText + "\n- " + text);
        svBattleLog.post(() -> svBattleLog.fullScroll(View.FOCUS_DOWN));
    }

    private void playCry(Pokemon pokemon) {
        if (pokemon != null && pokemon.cries != null && pokemon.cries.latest != null) {
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
                mediaPlayer.setDataSource(pokemon.cries.latest);
                mediaPlayer.prepareAsync();
                mediaPlayer.setOnPreparedListener(mp -> {
                    if (!battleEnded && !isFinishing() && !isDestroyed()) mp.start();
                });
            } catch (Exception e) {}
        }
    }

    private void iniciarMusicaBatalla() {
        boolean soundEnabled = prefsGlobales.getBoolean("sound_enabled", true);
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