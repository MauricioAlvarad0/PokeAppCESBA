package com.example.pokeappcesba;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class BattleVersusActivity extends AppCompatActivity {

    private PokeApiService apiService;

    private View llVersusSelection;
    private ConstraintLayout clVersusArena;
    
    // Player 1 UI
    private EditText etP1Slot1, etP1Slot2, etP1Slot3;
    private ImageView ivP1Slot1, ivP1Slot2, ivP1Slot3;
    private List<BattleEngine.BattlePokemon> p1Team = new ArrayList<>();
    
    // Player 2 UI
    private EditText etP2Slot1, etP2Slot2, etP2Slot3;
    private ImageView ivP2Slot1, ivP2Slot2, ivP2Slot3;
    private List<BattleEngine.BattlePokemon> p2Team = new ArrayList<>();
    
    private Button btnVersusRandom, btnVersusStart;

    // Arena UI
    private TextView tvVersusTurn, tvVersusAvailable;
    private ImageView ivP1Sprite, ivP2Sprite;
    private TextView tvP1Name, tvP2Name, tvP1HpText, tvP2HpText;
    private ProgressBar pbP1Hp, pbP2Hp;
    private TextView tvVersusProjectile, tvVersusFloatingDamage;

    // Bottom Controls
    private ScrollView svVersusLog;
    private TextView tvVersusLog;
    private GridLayout glVersusMoves;
    private Button[] btnVersusMoves = new Button[4];
    private View viewVersusLogTap;

    // Battle State
    private int currentP1Index = 0;
    private int currentP2Index = 0;
    private int p1Loaded = 0;
    private int p2Loaded = 0;
    private boolean isP1Turn = true; // Turn taking phase
    private String p1SelectedMove = null;
    private String p2SelectedMove = null;

    private SharedPreferences prefsGlobales;
    private SharedPreferences prefsPerfil;
    private String userUid = "default";

    private boolean battleEnded = false;
    private final android.os.Handler battleHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private MediaPlayer mediaPlayer;
    private MediaPlayer bgmPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battle_versus);

        findViewById(R.id.btnBackVersus).setOnClickListener(v -> mostrarConfirmacionSalida());
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                mostrarConfirmacionSalida();
            }
        });

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        prefsGlobales = getSharedPreferences("PokePrefs", MODE_PRIVATE);
        prefsPerfil = getSharedPreferences("PokePerfil_" + userUid, MODE_PRIVATE);

        apiService = new Retrofit.Builder()
                .baseUrl("https://pokeapi.co/api/v2/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PokeApiService.class);

        initViews();
        setupListeners();
    }

    private void mostrarConfirmacionSalida() {
        if (llVersusSelection.getVisibility() == View.VISIBLE || battleEnded) {
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

    private void initViews() {
        llVersusSelection = findViewById(R.id.llVersusSelection);
        clVersusArena = findViewById(R.id.clVersusArena);

        etP1Slot1 = findViewById(R.id.etP1Slot1);
        etP1Slot2 = findViewById(R.id.etP1Slot2);
        etP1Slot3 = findViewById(R.id.etP1Slot3);
        ivP1Slot1 = findViewById(R.id.ivP1Slot1);
        ivP1Slot2 = findViewById(R.id.ivP1Slot2);
        ivP1Slot3 = findViewById(R.id.ivP1Slot3);

        etP2Slot1 = findViewById(R.id.etP2Slot1);
        etP2Slot2 = findViewById(R.id.etP2Slot2);
        etP2Slot3 = findViewById(R.id.etP2Slot3);
        ivP2Slot1 = findViewById(R.id.ivP2Slot1);
        ivP2Slot2 = findViewById(R.id.ivP2Slot2);
        ivP2Slot3 = findViewById(R.id.ivP2Slot3);

        btnVersusRandom = findViewById(R.id.btnVersusRandom);
        btnVersusStart = findViewById(R.id.btnVersusStart);

        tvVersusTurn = findViewById(R.id.tvVersusTurn);
        tvVersusAvailable = findViewById(R.id.tvVersusAvailable);

        ivP1Sprite = findViewById(R.id.ivP1Sprite);
        ivP2Sprite = findViewById(R.id.ivP2Sprite);
        tvP1Name = findViewById(R.id.tvP1Name);
        tvP2Name = findViewById(R.id.tvP2Name);
        tvP1HpText = findViewById(R.id.tvP1HpText);
        tvP2HpText = findViewById(R.id.tvP2HpText);
        pbP1Hp = findViewById(R.id.pbP1Hp);
        pbP2Hp = findViewById(R.id.pbP2Hp);

        tvVersusProjectile = findViewById(R.id.tvVersusProjectile);
        tvVersusFloatingDamage = findViewById(R.id.tvVersusFloatingDamage);

        svVersusLog = findViewById(R.id.svVersusLog);
        tvVersusLog = findViewById(R.id.tvVersusLog);
        glVersusMoves = findViewById(R.id.glVersusMoves);
        btnVersusMoves[0] = findViewById(R.id.btnVersusMove1);
        btnVersusMoves[1] = findViewById(R.id.btnVersusMove2);
        btnVersusMoves[2] = findViewById(R.id.btnVersusMove3);
        btnVersusMoves[3] = findViewById(R.id.btnVersusMove4);
        viewVersusLogTap = findViewById(R.id.viewVersusLogTap);
    }

    private void setupListeners() {
        btnVersusRandom.setOnClickListener(v -> fillRandomTeams());
        btnVersusStart.setOnClickListener(v -> startVersusBattle());

        findViewById(R.id.btnP1Slot1Rand).setOnClickListener(v -> loadRandomPokemonForSlot(new Random().nextInt(251) + 1, 1, 0));
        findViewById(R.id.btnP1Slot2Rand).setOnClickListener(v -> loadRandomPokemonForSlot(new Random().nextInt(251) + 1, 1, 1));
        findViewById(R.id.btnP1Slot3Rand).setOnClickListener(v -> loadRandomPokemonForSlot(new Random().nextInt(251) + 1, 1, 2));

        findViewById(R.id.btnP2Slot1Rand).setOnClickListener(v -> loadRandomPokemonForSlot(new Random().nextInt(251) + 1, 2, 0));
        findViewById(R.id.btnP2Slot2Rand).setOnClickListener(v -> loadRandomPokemonForSlot(new Random().nextInt(251) + 1, 2, 1));
        findViewById(R.id.btnP2Slot3Rand).setOnClickListener(v -> loadRandomPokemonForSlot(new Random().nextInt(251) + 1, 2, 2));

        viewVersusLogTap.setOnClickListener(v -> {
            if (glVersusMoves.getVisibility() == View.GONE && btnVersusMoves[0].isEnabled()) {
                svVersusLog.setVisibility(View.GONE);
                glVersusMoves.setVisibility(View.VISIBLE);
                viewVersusLogTap.setVisibility(View.GONE);
            }
        });

        for (int i = 0; i < 4; i++) {
            final int index = i;
            btnVersusMoves[i].setOnClickListener(v -> {
                String rawText = btnVersusMoves[index].getText().toString();
                if (rawText.equals("-")) return;
                String cleanMove = rawText.replace("🔥 ", "").replace("⚡ ", "").replace("💥 ", "").replace("⚔️ ", "").trim();
                
                glVersusMoves.setVisibility(View.GONE);
                svVersusLog.setVisibility(View.VISIBLE);
                viewVersusLogTap.setVisibility(View.VISIBLE);
                
                handleMoveSelection(cleanMove);
            });
        }
    }

    private void fillRandomTeams() {
        btnVersusStart.setEnabled(false);
        p1Team.clear();
        p2Team.clear();
        p1Loaded = 0;
        p2Loaded = 0;
        Random r = new Random();
        for (int i = 0; i < 3; i++) {
            loadRandomPokemonForSlot(r.nextInt(251) + 1, 1, i);
            loadRandomPokemonForSlot(r.nextInt(251) + 1, 2, i);
        }
    }

    private void loadRandomPokemonForSlot(int id, int player, int slotIndex) {
        apiService.getPokemon(String.valueOf(id)).enqueue(new Callback<Pokemon>() {
            @Override
            public void onResponse(Call<Pokemon> call, Response<Pokemon> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BattleEngine.BattlePokemon bp = new BattleEngine.BattlePokemon(response.body());
                    if (player == 1) {
                        if (p1Team.size() > slotIndex) {
                            p1Team.set(slotIndex, bp);
                        } else {
                            while(p1Team.size() < slotIndex) p1Team.add(null);
                            p1Team.add(bp);
                            p1Loaded++;
                        }
                        updateSlotUI(1, slotIndex, bp);
                    } else {
                        if (p2Team.size() > slotIndex) {
                            p2Team.set(slotIndex, bp);
                        } else {
                            while(p2Team.size() < slotIndex) p2Team.add(null);
                            p2Team.add(bp);
                            p2Loaded++;
                        }
                        updateSlotUI(2, slotIndex, bp);
                    }
                    if (p1Loaded >= 3 && p2Loaded >= 3) {
                        boolean allValid = true;
                        for(int i=0;i<3;i++) if (p1Team.size()<=i || p1Team.get(i)==null || p2Team.size()<=i || p2Team.get(i)==null) allValid = false;
                        if(allValid) btnVersusStart.setEnabled(true);
                    }
                }
            }
            @Override
            public void onFailure(Call<Pokemon> call, Throwable t) {}
        });
    }

    private void updateSlotUI(int player, int index, BattleEngine.BattlePokemon bp) {
        String name = bp.pokemon.name.toUpperCase();
        String sprite = bp.pokemon.sprites != null ? bp.pokemon.sprites.frontDefault : null;
        if (player == 1) {
            if (index == 0) { etP1Slot1.setText(name); if(sprite!=null) Glide.with(this).load(sprite).into(ivP1Slot1); }
            if (index == 1) { etP1Slot2.setText(name); if(sprite!=null) Glide.with(this).load(sprite).into(ivP1Slot2); }
            if (index == 2) { etP1Slot3.setText(name); if(sprite!=null) Glide.with(this).load(sprite).into(ivP1Slot3); }
        } else {
            if (index == 0) { etP2Slot1.setText(name); if(sprite!=null) Glide.with(this).load(sprite).into(ivP2Slot1); }
            if (index == 1) { etP2Slot2.setText(name); if(sprite!=null) Glide.with(this).load(sprite).into(ivP2Slot2); }
            if (index == 2) { etP2Slot3.setText(name); if(sprite!=null) Glide.with(this).load(sprite).into(ivP2Slot3); }
        }
    }

    private void startVersusBattle() {
        if (p1Team.size() < 3 || p2Team.size() < 3) {
            Toast.makeText(this, "Debe cargar todos los Pokémon", Toast.LENGTH_SHORT).show();
            return;
        }
        
        llVersusSelection.setVisibility(View.GONE);
        clVersusArena.setVisibility(View.VISIBLE);
        
        currentP1Index = 0;
        currentP2Index = 0;
        
        for (BattleEngine.BattlePokemon bp : p1Team) bp.currentHp = bp.maxHp;
        for (BattleEngine.BattlePokemon bp : p2Team) bp.currentHp = bp.maxHp;

        tvVersusLog.setText("¡Duelo Versus Inicia!\n");
        isP1Turn = true;
        battleEnded = false;
        p1SelectedMove = null;
        p2SelectedMove = null;

        iniciarMusicaBatalla();
        
        setupTurnUI();
    }

    private void updateArenaUI() {
        BattleEngine.BattlePokemon p1 = p1Team.get(currentP1Index);
        BattleEngine.BattlePokemon p2 = p2Team.get(currentP2Index);

        tvP1Name.setText(p1.pokemon.name.toUpperCase());
        pbP1Hp.setMax(p1.maxHp);
        pbP1Hp.setProgress(Math.max(0, p1.currentHp));
        tvP1HpText.setText("HP: " + Math.max(0, p1.currentHp) + "/" + p1.maxHp);
        if (p1.pokemon.sprites != null && p1.pokemon.sprites.backDefault != null) {
            Glide.with(this).load(p1.pokemon.sprites.backDefault).into(ivP1Sprite);
        }

        tvP2Name.setText(p2.pokemon.name.toUpperCase());
        pbP2Hp.setMax(p2.maxHp);
        pbP2Hp.setProgress(Math.max(0, p2.currentHp));
        tvP2HpText.setText("HP: " + Math.max(0, p2.currentHp) + "/" + p2.maxHp);
        if (p2.pokemon.sprites != null && p2.pokemon.sprites.frontDefault != null) {
            Glide.with(this).load(p2.pokemon.sprites.frontDefault).into(ivP2Sprite);
        }
        
        int p1Alive = 0;
        for (BattleEngine.BattlePokemon p : p1Team) if (!p.isFainted()) p1Alive++;
        int p2Alive = 0;
        for (BattleEngine.BattlePokemon p : p2Team) if (!p.isFainted()) p2Alive++;
        tvVersusAvailable.setText("J1: " + p1Alive + "/3 | J2: " + p2Alive + "/3");
    }

    private void setupTurnUI() {
        updateArenaUI();
        if (isP1Turn) {
            tvVersusTurn.setText("🔵 TURNO DE: JUGADOR 1");
            tvVersusTurn.setTextColor(Color.parseColor("#38BDF8"));
            loadMovesFor(p1Team.get(currentP1Index));
            logBattle("Jugador 1, elige tu ataque. (Toca aquí)");
        } else {
            tvVersusTurn.setText("🔴 TURNO DE: JUGADOR 2");
            tvVersusTurn.setTextColor(Color.parseColor("#EF4444"));
            loadMovesFor(p2Team.get(currentP2Index));
            logBattle("Jugador 2, elige tu ataque. (Toca aquí)");
        }
    }

    private void loadMovesFor(BattleEngine.BattlePokemon bp) {
        for (int i = 0; i < 4; i++) {
            if (i < bp.moves.size()) {
                btnVersusMoves[i].setText(bp.moves.get(i).toUpperCase());
                btnVersusMoves[i].setEnabled(true);
            } else {
                btnVersusMoves[i].setText("-");
                btnVersusMoves[i].setEnabled(false);
            }
        }
        viewVersusLogTap.setVisibility(View.VISIBLE);
    }

    private void handleMoveSelection(String move) {
        if (isP1Turn) {
            p1SelectedMove = move;
            isP1Turn = false;
            setupTurnUI();
        } else {
            p2SelectedMove = move;
            viewVersusLogTap.setVisibility(View.GONE);
            resolveTurn();
        }
    }

    private void resolveTurn() {
        if (battleEnded || isFinishing() || isDestroyed()) return;
        BattleEngine.BattlePokemon p1 = p1Team.get(currentP1Index);
        BattleEngine.BattlePokemon p2 = p2Team.get(currentP2Index);

        boolean p1GoesFirst = BattleEngine.determineFirstAttacker(p1, p2);

        if (p1GoesFirst) {
            executeAttack(p1, p2, 1, p1SelectedMove, () -> {
                if (!p2.isFainted()) {
                    executeAttack(p2, p1, 2, p2SelectedMove, this::checkFaint);
                } else {
                    checkFaint();
                }
            });
        } else {
            executeAttack(p2, p1, 2, p2SelectedMove, () -> {
                if (!p1.isFainted()) {
                    executeAttack(p1, p2, 1, p1SelectedMove, this::checkFaint);
                } else {
                    checkFaint();
                }
            });
        }
    }

    private void executeAttack(BattleEngine.BattlePokemon attacker, BattleEngine.BattlePokemon defender, int attackerId, String move, Runnable onComplete) {
        if (battleEnded || isFinishing() || isDestroyed()) return;
        int dmg = BattleEngine.calculateDamage(attacker.attack, defender.defense);
        defender.currentHp -= dmg;
        
        logBattle(attacker.pokemon.name.toUpperCase() + " usó " + move + ".");
        
        ImageView targetView = (attackerId == 1) ? ivP2Sprite : ivP1Sprite;
        
        tvVersusProjectile.setText(new Random().nextBoolean() ? "🔥" : "⚡");
        tvVersusProjectile.setVisibility(View.VISIBLE);
        
        battleHandler.postDelayed(() -> {
            if (battleEnded || isFinishing() || isDestroyed()) return;
            tvVersusProjectile.setVisibility(View.INVISIBLE);
            targetView.setColorFilter(Color.RED);
            
            tvVersusFloatingDamage.setText("-" + dmg + " HP");
            tvVersusFloatingDamage.setVisibility(View.VISIBLE);
            new android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100).startTone(android.media.ToneGenerator.TONE_CDMA_ABBR_ALERT, 150);
            
            logBattle("¡Causa " + dmg + " puntos de daño!");
            if (defender.currentHp < 0) defender.currentHp = 0;
            logBattle(defender.pokemon.name.toUpperCase() + " queda con " + defender.currentHp + " HP.");
            playCry(attacker.pokemon);
            updateArenaUI();
            
            battleHandler.postDelayed(() -> {
                if (battleEnded || isFinishing() || isDestroyed()) return;
                targetView.clearColorFilter();
                tvVersusFloatingDamage.setVisibility(View.INVISIBLE);
                battleHandler.postDelayed(() -> {
                    if (!battleEnded && !isFinishing() && !isDestroyed()) onComplete.run();
                }, 500);
            }, 300);
        }, 300);
    }

    private void checkFaint() {
        BattleEngine.BattlePokemon p1 = p1Team.get(currentP1Index);
        BattleEngine.BattlePokemon p2 = p2Team.get(currentP2Index);

        if (p1.isFainted() && p2.isFainted()) {
            handleFaint(1, () -> handleFaint(2, this::nextTurn));
        } else if (p1.isFainted()) {
            handleFaint(1, this::nextTurn);
        } else if (p2.isFainted()) {
            handleFaint(2, this::nextTurn);
        } else {
            nextTurn();
        }
    }

    private void handleFaint(int player, Runnable onResolved) {
        if (player == 1) {
            logBattle("¡" + p1Team.get(currentP1Index).pokemon.name.toUpperCase() + " de J1 ha caído!");
            if (hasAlive(p1Team)) {
                promptSwitch(1, onResolved);
            } else {
                endGame(2);
            }
        } else {
            logBattle("¡" + p2Team.get(currentP2Index).pokemon.name.toUpperCase() + " de J2 ha caído!");
            if (hasAlive(p2Team)) {
                promptSwitch(2, onResolved);
            } else {
                endGame(1);
            }
        }
    }

    private void nextTurn() {
        isP1Turn = true;
        p1SelectedMove = null;
        p2SelectedMove = null;
        setupTurnUI();
    }

    private boolean hasAlive(List<BattleEngine.BattlePokemon> team) {
        for (BattleEngine.BattlePokemon bp : team) {
            if (!bp.isFainted()) return true;
        }
        return false;
    }

    private void promptSwitch(int player, Runnable onResolved) {
        if (battleEnded || isFinishing() || isDestroyed()) return;
        List<BattleEngine.BattlePokemon> team = player == 1 ? p1Team : p2Team;

        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_retro_custom);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));

        TextView tvTitle = dialog.findViewById(R.id.tvRetroDialogTitle);
        TextView tvMsg = dialog.findViewById(R.id.tvRetroDialogMessage);
        Button btnPos = dialog.findViewById(R.id.btnRetroPositive);
        Button btnNeg = dialog.findViewById(R.id.btnRetroNegative);
        View flIconCont = dialog.findViewById(R.id.flRetroDialogIconContainer);
        android.widget.LinearLayout llOptions = dialog.findViewById(R.id.llRetroDialogOptions);

        tvTitle.setText("🔄 RELEVO - JUGADOR " + player);
        tvMsg.setText("Jugador " + player + ", elige tu relevo:");
        btnPos.setVisibility(View.GONE);
        flIconCont.setVisibility(View.GONE);
        llOptions.setVisibility(View.VISIBLE);
        btnNeg.setVisibility(View.GONE);

        for (int i = 0; i < team.size(); i++) {
            BattleEngine.BattlePokemon bp = team.get(i);
            if (bp.isFainted()) continue;

            android.widget.LinearLayout card = new android.widget.LinearLayout(this);
            card.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            card.setBackgroundResource(R.drawable.bg_input_retro);
            card.setPadding(16, 16, 16, 16);
            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 16);
            card.setLayoutParams(lp);
            card.setGravity(android.view.Gravity.CENTER_VERTICAL);

            ImageView iv = new ImageView(this);
            iv.setLayoutParams(new android.widget.LinearLayout.LayoutParams(100, 100));
            iv.setBackgroundResource(R.drawable.bg_gold_frame);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setPadding(8,8,8,8);
            if (bp.pokemon.sprites != null && bp.pokemon.sprites.frontDefault != null) {
                Glide.with(this).load(bp.pokemon.sprites.frontDefault).into(iv);
            }
            card.addView(iv);

            android.widget.LinearLayout info = new android.widget.LinearLayout(this);
            info.setOrientation(android.widget.LinearLayout.VERTICAL);
            info.setPadding(16, 0, 0, 0);

            TextView tvName = new TextView(this);
            tvName.setText(bp.pokemon.name.toUpperCase());
            tvName.setTextColor(Color.WHITE);
            tvName.setTextSize(16f);
            tvName.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.pokemon_pixel), android.graphics.Typeface.BOLD);
            info.addView(tvName);

            TextView tvHp = new TextView(this);
            tvHp.setText("HP: " + Math.max(0, bp.currentHp) + "/" + bp.maxHp);
            tvHp.setTextColor(Color.parseColor("#4ADE80"));
            tvHp.setTextSize(14f);
            tvHp.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.pokemon_pixel));
            info.addView(tvHp);

            card.addView(info);

            final int chosenIndex = i;
            card.setOnClickListener(v -> {
                dialog.dismiss();
                if (player == 1) {
                    currentP1Index = chosenIndex;
                    logBattle("J1 envía a " + p1Team.get(currentP1Index).pokemon.name.toUpperCase());
                    playCry(p1Team.get(currentP1Index).pokemon);
                } else {
                    currentP2Index = chosenIndex;
                    logBattle("J2 envía a " + p2Team.get(currentP2Index).pokemon.name.toUpperCase());
                    playCry(p2Team.get(currentP2Index).pokemon);
                }
                updateArenaUI();
                onResolved.run();
            });

            llOptions.addView(card);
        }

        dialog.setCancelable(false);
        dialog.show();
    }

    private void endGame(int winnerPlayer) {
        int vic = prefsGlobales.getInt("versus_victorias", 0);
        int der = prefsGlobales.getInt("versus_derrotas", 0);
        
        if (winnerPlayer == 1) {
            prefsGlobales.edit().putInt("versus_victorias", vic + 1).apply();
            prefsPerfil.edit().putInt("versus_victorias", vic + 1).apply();
        } else {
            prefsGlobales.edit().putInt("versus_derrotas", der + 1).apply();
            prefsPerfil.edit().putInt("versus_derrotas", der + 1).apply();
        }

        detenerMusicaBatalla();

        abrirDialogoRetro("🏆 ¡JUGADOR " + winnerPlayer + " GANA! 🏆", 
            "La batalla ha terminado.", 
            "SALIR AL MENÚ", 
            null, 
            this::finish, 
            null, 
            R.drawable.ic_medallas);
    }

    private void logBattle(String text) {
        String currentText = tvVersusLog.getText().toString();
        tvVersusLog.setText(currentText + "\n- " + text);
        svVersusLog.post(() -> svVersusLog.fullScroll(View.FOCUS_DOWN));
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
            } catch (Exception ignored) {}
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
}
