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
    private ImageView btnBackSafari, ivWildSprite, ivThrownPokeball, ivCaughtPokeball;
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
    private MediaPlayer bgmPlayer;

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
        ivThrownPokeball = findViewById(R.id.ivThrownPokeball);
        ivCaughtPokeball = findViewById(R.id.ivCaughtPokeball);
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
        tvSafariCaught.setOnClickListener(v -> showCaughtPokemon());

        btnPokeball.setOnClickListener(v -> selectBall(40, 0, btnPokeball));
        btnSuperball.setOnClickListener(v -> selectBall(65, 10, btnSuperball));
        btnUltraball.setOnClickListener(v -> selectBall(85, 25, btnUltraball));
        btnMasterball.setOnClickListener(v -> selectBall(100, 50, btnMasterball));

        btnCatch.setOnClickListener(v -> attemptCatch());
        btnRun.setOnClickListener(v -> findWildPokemon());

        updateUI();
        findWildPokemon();
        iniciarMusicaBatalla();
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

        // Configuración inicial de la Pokéball
        ivThrownPokeball.setVisibility(android.view.View.VISIBLE);
        ivCaughtPokeball.setVisibility(android.view.View.INVISIBLE);
        ivThrownPokeball.setTranslationY(0f);
        ivThrownPokeball.setRotation(0f);
        ivThrownPokeball.setTranslationX(0f);
        
        // Efecto de sonido (opcional)
        new android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100).startTone(android.media.ToneGenerator.TONE_CDMA_ABBR_ALERT, 150);

        // a) y b) Anima ivThrownPokeball
        ivThrownPokeball.animate()
            .translationY(-400f) // Ajustar según el layout para que caiga sobre el pokemon
            .rotation(360f)
            .setDuration(600)
            .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
            .withEndAction(() -> {
                // c) Oculta al Pokémon salvaje y la bola que vuela, muestra la bola en el centro
                ivWildSprite.setVisibility(android.view.View.INVISIBLE);
                ivThrownPokeball.setVisibility(android.view.View.INVISIBLE);
                ivCaughtPokeball.setVisibility(android.view.View.VISIBLE);
                tvWildStatus.setText("Tic...");
                
                // d) Secuencia matemática de temblor
                ivCaughtPokeball.animate().rotation(15f).setDuration(300).withEndAction(() -> {
                    tvWildStatus.setText("Tic... Tac...");
                    ivCaughtPokeball.animate().rotation(-15f).setDuration(300).withEndAction(() -> {
                        tvWildStatus.setText("Tic... Tac... Tic...");
                        ivCaughtPokeball.animate().rotation(15f).setDuration(300).withEndAction(() -> {
                            ivCaughtPokeball.animate().rotation(0f).setDuration(150).withEndAction(() -> {
                                
                                // e) Decide la captura
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
                    
                                    // Cambiar color de la ball para indicar captura
                                    ivCaughtPokeball.setColorFilter(Color.parseColor("#80FF0000")); // Tinte rojizo/brillo

                                    // Limpiar y buscar el siguiente
                                    isBusy = false;
                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        ivCaughtPokeball.clearColorFilter();
                                        ivCaughtPokeball.setVisibility(android.view.View.INVISIBLE);
                                        ivWildSprite.setVisibility(android.view.View.VISIBLE);
                                        findWildPokemon();
                                    }, 2000);
                                } else {
                                    // Fallado (se rompe la bola)
                                    ivCaughtPokeball.setVisibility(android.view.View.INVISIBLE);
                                    ivWildSprite.setVisibility(android.view.View.VISIBLE);
                                    System.out.println("❌ Falló la captura del Pokémon.");
                                    
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
                            }).start();
                        }).start();
                    }).start();
                }).start();
            }).start();
    }

    private void showCaughtPokemon() {
        if (isBusy) return;

        java.util.Map<String, ?> todosLosCapturados = prefsSafari.getAll();

        if (todosLosCapturados.isEmpty()) {
            Toast.makeText(this, "Aún no has capturado a ningún Pokémon en el Safari", Toast.LENGTH_SHORT).show();
            return;
        }

        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_favoritos, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this).setView(dialogView).create();
        if(dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));

        android.widget.GridLayout llContainer = dialogView.findViewById(R.id.llFavoritosContainer);
        
        for (String nombrePokemon : todosLosCapturados.keySet()) {
            android.view.View itemFav = getLayoutInflater().inflate(R.layout.item_favorito, llContainer, false);
            ImageView ivPokemonFavo = itemFav.findViewById(R.id.ivPokemonFavo);
            TextView tvPokemonNombreFavo = itemFav.findViewById(R.id.tvPokemonNombreFavo);
            
            tvPokemonNombreFavo.setText(nombrePokemon.toUpperCase());
            
            apiService.getPokemon(nombrePokemon.toLowerCase()).enqueue(new Callback<Pokemon>() {
                @Override
                public void onResponse(@NonNull Call<Pokemon> call, @NonNull Response<Pokemon> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String spriteUrl = "";
                        if (response.body().sprites != null) {
                            if (response.body().sprites.frontDefault != null) {
                                spriteUrl = response.body().sprites.frontDefault;
                            }
                        }
                        if (!spriteUrl.isEmpty()) {
                            Glide.with(SafariActivity.this).load(spriteUrl).into(ivPokemonFavo);
                        }
                    }
                }
                @Override
                public void onFailure(@NonNull Call<Pokemon> call, @NonNull Throwable t) {}
            });
            
            llContainer.addView(itemFav);
        }

        Button btnCerrar = dialogView.findViewById(R.id.btnCerrarDialog);
        btnCerrar.setText("CERRAR SAFARI (CAPTURADOS: " + caughtCount + ")");
        btnCerrar.setOnClickListener(v2 -> dialog.dismiss());
        
        dialog.show();
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
        if (mediaPlayer != null) {
            try { if (mediaPlayer.isPlaying()) mediaPlayer.stop(); mediaPlayer.release(); } catch (Exception ignored) {}
            mediaPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        detenerMusicaBatalla();
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
