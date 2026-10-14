package com.example.pokeappcesba;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

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

public class TowerActivity extends AppCompatActivity {

    public static final String[] MEDALLAS_TORRE = {
            "Medalla 1", "Medalla 2", "Medalla 3", "Medalla 4", "Medalla 5",
            "Medalla 6", "Medalla 7", "Medalla 8", "Medalla 9", "Medalla 10"
    };

    private PokeApiService apiService;
    private View llTeamSelection, clTowerArena;
    private GridLayout glPokemonSelection;
    private Button btnStartTower, btnRandomTeam;
    private GridLayout glMoves;
    private Button[] btnMoves = new Button[4];
    private View viewLogTap;
    private TextView tvSubtitleSelection;
    
    private TextView tvTowerLevel, tvDocenteName, tvEnemyName, tvPlayerName, tvPlayerHpText, tvBattleLog, tvAliveCount;
    private ImageView ivDocente, ivEnemyPokemon, ivPlayerPokemon;
    private ProgressBar pbEnemyHp, pbPlayerHp;
    private ScrollView svBattleLog;
    
    // State
    private List<BattleEngine.BattlePokemon> selectionPool = new ArrayList<>();
    private List<BattleEngine.BattlePokemon> playerTeam = new ArrayList<>();
    private List<BattleEngine.BattlePokemon> enemyTeam = new ArrayList<>();
    
    private int currentLevel = 1; // 1 to 6
    private int currentPlayerIndex = 0;
    private int currentEnemyIndex = 0;
    
    private SharedPreferences prefsMedallas;
    private SharedPreferences prefsGlobales;
    private String userUid = "default";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tower);
        
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        prefsMedallas = getSharedPreferences("PokeMedallas_" + userUid, MODE_PRIVATE);
        prefsGlobales = getSharedPreferences("PokePrefs", MODE_PRIVATE);
        
        apiService = new Retrofit.Builder()
                .baseUrl("https://pokeapi.co/api/v2/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PokeApiService.class);
                
        initViews();
        loadSelectionPool();
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
        
        tvBattleLog = findViewById(R.id.tvBattleLog);
        svBattleLog = findViewById(R.id.svBattleLog);
        
        glMoves = findViewById(R.id.glMoves);
        btnMoves[0] = findViewById(R.id.btnMove1);
        btnMoves[1] = findViewById(R.id.btnMove2);
        btnMoves[2] = findViewById(R.id.btnMove3);
        btnMoves[3] = findViewById(R.id.btnMove4);
        viewLogTap = findViewById(R.id.viewLogTap);
        
        btnStartTower.setOnClickListener(v -> startTower());
        
        viewLogTap.setOnClickListener(v -> {
            if (glMoves.getVisibility() == View.GONE && btnMoves[0].isEnabled()) {
                svBattleLog.setVisibility(View.GONE);
                glMoves.setVisibility(View.VISIBLE);
                viewLogTap.setVisibility(View.GONE);
            }
        });

        for (int i = 0; i < 4; i++) {
            final int index = i;
            btnMoves[i].setOnClickListener(v -> {
                String moveName = btnMoves[index].getText().toString();
                if (moveName.equals("-")) return;
                glMoves.setVisibility(View.GONE);
                svBattleLog.setVisibility(View.VISIBLE);
                viewLogTap.setVisibility(View.VISIBLE);
                playTurn(moveName);
            });
        }
        
        btnRandomTeam.setOnClickListener(v -> {
            if (selectionPool.size() == 6) {
                playerTeam.clear();
                List<BattleEngine.BattlePokemon> tempPool = new ArrayList<>(selectionPool);
                Collections.shuffle(tempPool);
                for(int i=0; i<3; i++){
                    playerTeam.add(tempPool.get(i));
                }
                updateSelectionUI();
            }
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
        tvSubtitleSelection.setText("¡ELIGE 3 COMBATIENTES! (0/3)");
        
        Random rand = new Random();
        for (int i = 0; i < 6; i++) {
            int randomId = rand.nextInt(251) + 1;
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
            View card = getLayoutInflater().inflate(R.layout.item_favorito, glPokemonSelection, false);
            ImageView iv = card.findViewById(R.id.ivPokemonFavo);
            TextView tv = card.findViewById(R.id.tvPokemonNombreFavo);
            tv.setText(bp.pokemon.name.toUpperCase() + "\nHP:" + bp.maxHp + " ATK:" + bp.attack);
            tv.setTextSize(10f);
            
            if (bp.pokemon.sprites != null && bp.pokemon.sprites.frontDefault != null) {
                Glide.with(this).load(bp.pokemon.sprites.frontDefault).into(iv);
            }
            
            boolean isSelected = playerTeam.contains(bp);
            card.setBackgroundResource(isSelected ? R.drawable.bg_gold_frame : R.drawable.bg_input_retro);
            if (isSelected) {
                TextView check = new TextView(this);
                check.setText("CHECK 🎖️");
                check.setTextColor(Color.YELLOW);
                check.setTextSize(12f);
                check.setPadding(4,4,4,4);
                LinearLayout innerLayout = (LinearLayout) ((android.view.ViewGroup) card).getChildAt(0);
                innerLayout.addView(check, 0);
            }
            
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
        
        tvSubtitleSelection.setText("¡ELIGE 3 COMBATIENTES! (" + playerTeam.size() + "/3)");
        btnStartTower.setText("🔥 ¡INICIAR TORRE (" + playerTeam.size() + "/3)! 🔥");
        btnStartTower.setEnabled(playerTeam.size() == 3);
    }
    
    private void startTower() {
        llTeamSelection.setVisibility(View.GONE);
        clTowerArena.setVisibility(View.VISIBLE);
        currentLevel = 1;
        startLevel();
    }
    
    private void startLevel() {
        currentPlayerIndex = 0;
        currentEnemyIndex = 0;
        enemyTeam.clear();
        
        // Heal player team completely for each level
        for (BattleEngine.BattlePokemon bp : playerTeam) {
            bp.currentHp = bp.maxHp;
        }
        
        // Setup Banner
        if (currentLevel < 6) {
            tvTowerLevel.setText("NIVEL " + currentLevel + " DE 6");
            tvDocenteName.setText("Docente CESBA " + currentLevel);
            int resId = getResources().getIdentifier("docente_cesba_" + currentLevel, "drawable", getPackageName());
            if(resId != 0) {
                ivDocente.setImageResource(resId);
            } else {
                ivDocente.setImageResource(R.drawable.ic_mi_perfil);
            }
        } else {
            tvTowerLevel.setText("NIVEL 6 - JEFE FINAL");
            tvDocenteName.setText("Docente CESBA 6 / Jefe Final");
            int resId = getResources().getIdentifier("docente_cesba_6", "drawable", getPackageName());
            if(resId != 0) {
                ivDocente.setImageResource(resId);
            } else {
                ivDocente.setImageResource(R.drawable.ic_mi_perfil);
            }
        }
        
        updateAliveCount();
        tvBattleLog.setText("¡Nivel " + currentLevel + " empieza!");
        
        int enemyCount = (currentLevel == 6) ? 4 : 3;
        loadEnemyTeam(enemyCount, currentLevel == 6);
    }
    
    private void updateAliveCount() {
        int pAlive = 0;
        for (BattleEngine.BattlePokemon p : playerTeam) if (!p.isFainted()) pAlive++;
        int eAlive = 0;
        for (BattleEngine.BattlePokemon e : enemyTeam) if (!e.isFainted()) eAlive++;
        int eTotal = (currentLevel == 6) ? 4 : 3;
        if (enemyTeam.isEmpty()) eAlive = eTotal; // Before loading enemies
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
        setAttackButtonsEnabled(true);
        updateArenaUI();
        updateAliveCount();
        logBattle("¡Enemigo envió a " + getActiveEnemy().pokemon.name.toUpperCase() + "!");
        playCry(getActiveEnemy().pokemon);
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
        if (pPlayer.pokemon.sprites != null && pPlayer.pokemon.sprites.backDefault != null) {
            Glide.with(this).load(pPlayer.pokemon.sprites.backDefault).into(ivPlayerPokemon);
        }
        
        tvEnemyName.setText(pEnemy.pokemon.name.toUpperCase());
        pbEnemyHp.setMax(pEnemy.maxHp);
        pbEnemyHp.setProgress(Math.max(0, pEnemy.currentHp));
        if (pEnemy.pokemon.sprites != null && pEnemy.pokemon.sprites.frontDefault != null) {
            Glide.with(this).load(pEnemy.pokemon.sprites.frontDefault).into(ivEnemyPokemon);
        }
        updateAliveCount();
        
        for (int i = 0; i < 4; i++) {
            if (i < pPlayer.moves.size()) {
                btnMoves[i].setText(pPlayer.moves.get(i).toUpperCase());
                btnMoves[i].setEnabled(true);
            } else {
                btnMoves[i].setText("-");
                btnMoves[i].setEnabled(false);
            }
        }
    }
    
    private void playTurn(String playerMove) {
        // Disable attack buttons during turn
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
        if (enabled && glMoves.getVisibility() != View.VISIBLE) {
            viewLogTap.setVisibility(View.VISIBLE);
            logBattle("¡Es tu turno! (Toca aquí para atacar)");
        } else {
            viewLogTap.setVisibility(View.GONE);
        }
    }
    
    private void executeAttack(BattleEngine.BattlePokemon attacker, BattleEngine.BattlePokemon defender, boolean isPlayerAttacking, String moveUsed, Runnable onComplete) {
        String move = moveUsed != null ? moveUsed : BattleEngine.getRandomMove(attacker);
        int dmg = BattleEngine.calculateDamage(attacker.attack, defender.defense);
        defender.currentHp -= dmg;
        
        logBattle(attacker.pokemon.name.toUpperCase() + " usó " + move + ".");
        logBattle("¡Causa " + dmg + " puntos de daño!");
        if (defender.currentHp < 0) defender.currentHp = 0;
        logBattle(defender.pokemon.name.toUpperCase() + " queda con " + defender.currentHp + " HP.");

        playCry(attacker.pokemon);
        
        ImageView targetView = isPlayerAttacking ? ivEnemyPokemon : ivPlayerPokemon;
        targetView.setColorFilter(Color.RED);
        targetView.postDelayed(() -> targetView.clearColorFilter(), 200);
        
        updateArenaUI();
        
        targetView.postDelayed(onComplete, 1000);
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
            setAttackButtonsEnabled(true);
        }
    }
    
    private void levelComplete() {
        if (currentLevel == 6) {
            campaignWon();
        } else {
            currentLevel++;
            new AlertDialog.Builder(this)
                .setTitle("¡Piso superado!")
                .setMessage("Subiendo al siguiente nivel de la Torre...")
                .setPositiveButton("Continuar", (d, w) -> startLevel())
                .setCancelable(false)
                .show();
        }
    }
    
    private void gameOver() {
        new AlertDialog.Builder(this)
            .setTitle("¡Has Perdido!")
            .setMessage("Tu equipo ha sido derrotado. La campaña termina aquí.")
            .setPositiveButton("Salir", (d, w) -> finish())
            .setCancelable(false)
            .show();
    }
    
    private void campaignWon() {
        int medals = prefsMedallas.getInt("medals_count", 0);
        if (medals < 10) {
            medals++;
            prefsMedallas.edit().putInt("medals_count", medals).apply();
            // Sync with PokePrefs
            prefsGlobales.edit().putInt("torre_medallas", medals).apply();
        }
        
        new AlertDialog.Builder(this)
            .setTitle("¡CAMPAÑA COMPLETADA!")
            .setMessage("¡Has vencido al Jefe Final y dominado la Torre!\nMedallas de la Torre: " + medals + "/10")
            .setPositiveButton("¡ÉPICO!", (d, w) -> finish())
            .setCancelable(false)
            .show();
    }
    
    private void logBattle(String text) {
        String currentText = tvBattleLog.getText().toString();
        tvBattleLog.setText(currentText + "\n- " + text);
        svBattleLog.post(() -> svBattleLog.fullScroll(View.FOCUS_DOWN));
    }
    
    private void playCry(Pokemon pokemon) {
        if (pokemon != null && pokemon.cries != null && pokemon.cries.latest != null) {
            try {
                MediaPlayer player = new MediaPlayer();
                player.setDataSource(pokemon.cries.latest);
                player.prepareAsync();
                player.setOnPreparedListener(MediaPlayer::start);
                player.setOnCompletionListener(MediaPlayer::release);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
