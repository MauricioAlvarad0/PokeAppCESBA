package com.example.pokeappcesba;

import android.annotation.SuppressLint;
import java.util.Map;
import android.widget.LinearLayout;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tvSaludo = findViewById(R.id.tvSaludo);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        SharedPreferences prefs = getSharedPreferences("PokePrefs", MODE_PRIVATE);
        String savedTrainerName = prefs.getString("trainer_name", "");

        if (!savedTrainerName.isEmpty()) {
            tvSaludo.setText("Entrenador: " + savedTrainerName);
        } else if(user != null && user.getEmail() != null) {
            String emailPrefix = user.getEmail().split("@")[0];
            tvSaludo.setText("Entrenador: " + emailPrefix);
        }

        // Top Bar Buttons
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnOptions).setOnClickListener(v -> mostrarDialogoConfiguracion());

        // Grid Menu Buttons
        findViewById(R.id.btnMenuPokedex).setOnClickListener(v -> startActivity(new Intent(MainActivity.this, PokedexActivity.class)));
        findViewById(R.id.btnMenuEmulator).setOnClickListener(v -> startActivity(new Intent(MainActivity.this, BattleEmulatorActivity.class)));
        
        setupMenuButton(R.id.btnMenuVersus, "Battle Versus");
        findViewById(R.id.btnMenuTorre).setOnClickListener(v -> startActivity(new Intent(MainActivity.this, TowerActivity.class)));
        setupMenuButton(R.id.btnMenuQuienEs, "¿Quién es ese Pokémon?");
        setupMenuButton(R.id.btnMenuSafari, "Safari Pokémon");
        setupMenuButton(R.id.btnMenuMaestro, "Maestro de Tipos");
        setupMenuButton(R.id.btnMenuMemory, "PokéMemory");
        
        // Favoritos Dialog
        findViewById(R.id.btnMenuFavoritos).setOnClickListener(v -> {
            SharedPreferences prefsFavoritos = getSharedPreferences("PokeFavoritos", MODE_PRIVATE);
            Map<String, ?> todosLosFavoritos = prefsFavoritos.getAll();

            if (todosLosFavoritos.isEmpty()) {
                Toast.makeText(this, "Aún no tienes favoritos guardados", Toast.LENGTH_SHORT).show();
                return;
            }

            View dialogView = getLayoutInflater().inflate(R.layout.dialog_favoritos, null);
            AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();
            if(dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            android.widget.GridLayout llContainer = dialogView.findViewById(R.id.llFavoritosContainer);
            
            PokeApiService apiService = new retrofit2.Retrofit.Builder()
                    .baseUrl("https://pokeapi.co/api/v2/")
                    .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                    .build()
                    .create(PokeApiService.class);

            for (String nombre : todosLosFavoritos.keySet()) {
                View itemFav = getLayoutInflater().inflate(R.layout.item_favorito, llContainer, false);
                android.widget.ImageView ivPokemonFavo = itemFav.findViewById(R.id.ivPokemonFavo);
                android.widget.TextView tvPokemonNombreFavo = itemFav.findViewById(R.id.tvPokemonNombreFavo);
                
                tvPokemonNombreFavo.setText(nombre);
                
                itemFav.setOnClickListener(v2 -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Para ver a " + nombre.toUpperCase() + " ve a la Pokédex.", Toast.LENGTH_SHORT).show();
                });

                apiService.getPokemon(nombre).enqueue(new retrofit2.Callback<Pokemon>() {
                    @Override
                    public void onResponse(retrofit2.Call<Pokemon> call, retrofit2.Response<Pokemon> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String spriteUrl = "";
                            if (response.body().sprites != null) {
                                if (response.body().sprites.other != null && response.body().sprites.other.showdown != null && response.body().sprites.other.showdown.frontDefault != null) {
                                    spriteUrl = response.body().sprites.other.showdown.frontDefault;
                                } else if (response.body().sprites.frontDefault != null) {
                                    spriteUrl = response.body().sprites.frontDefault;
                                }
                            }
                            if (!spriteUrl.isEmpty()) {
                                com.bumptech.glide.Glide.with(MainActivity.this).load(spriteUrl).into(ivPokemonFavo);
                            }
                        }
                    }
                    @Override
                    public void onFailure(retrofit2.Call<Pokemon> call, Throwable t) {}
                });
                
                llContainer.addView(itemFav);
            }

            dialogView.findViewById(R.id.btnCerrarDialog).setOnClickListener(v2 -> dialog.dismiss());
            dialog.show();
        });

        setupMenuButton(R.id.btnMenuHistorial, "Historial");
        findViewById(R.id.btnMenuMedallas).setOnClickListener(v -> mostrarDialogoMedallas());
        
        // Mi Perfil y Configuración abren el diálogo de configuración del entrenador
        View.OnClickListener configListener = v -> mostrarDialogoConfiguracion();
        View btnPerfil = findViewById(R.id.btnMenuPerfil);
        View btnConfig = findViewById(R.id.btnMenuConfiguracion);
        if(btnPerfil != null) btnPerfil.setOnClickListener(configListener);
        if(btnConfig != null) btnConfig.setOnClickListener(configListener);

        // Bottom Navigation Buttons
        findViewById(R.id.ivNavHome).setOnClickListener(v -> Toast.makeText(this, "Estás en Inicio", Toast.LENGTH_SHORT).show());
        findViewById(R.id.ivNavPokedex).setOnClickListener(v -> startActivity(new Intent(MainActivity.this, PokedexActivity.class)));
        findViewById(R.id.ivNavAdd).setOnClickListener(v -> Toast.makeText(this, "Acción rápida de Pokémon", Toast.LENGTH_SHORT).show());
        findViewById(R.id.ivNavCompass).setOnClickListener(v -> Toast.makeText(this, "Radar y Exploración", Toast.LENGTH_SHORT).show());
        
        // Cerrar Sesión y volver al Login
        findViewById(R.id.btnCerrarSesion).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void mostrarDialogoConfiguracion() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_configuracion, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        if(dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        EditText etTrainerName = dialogView.findViewById(R.id.etTrainerName);
        Button btnSave = dialogView.findViewById(R.id.btnSaveTrainerName);
        Button btnClose = dialogView.findViewById(R.id.btnCerrarConfig);

        SharedPreferences prefs = getSharedPreferences("PokePrefs", MODE_PRIVATE);
        String currentName = prefs.getString("trainer_name", "");
        if (currentName.isEmpty()) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getEmail() != null) {
                currentName = user.getEmail().split("@")[0];
            }
        }
        etTrainerName.setText(currentName);
        etTrainerName.setSelection(etTrainerName.getText().length());
        
        String userUid = "default";
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        SharedPreferences prefsMedallas = getSharedPreferences("PokeMedallas_" + userUid, MODE_PRIVATE);
        int medals = prefsMedallas.getInt("medals_count", 0);
        
        TextView tvStats = new TextView(this);
        tvStats.setText("🏆 Medallas de la Torre: " + medals + "/10");
        tvStats.setTextColor(Color.WHITE);
        tvStats.setTextSize(16f);
        tvStats.setPadding(0, 16, 0, 16);
        tvStats.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        ((LinearLayout)etTrainerName.getParent()).addView(tvStats, 1);

        btnSave.setOnClickListener(v -> {
            String newName = etTrainerName.getText().toString().trim();
            if (!newName.isEmpty()) {
                prefs.edit().putString("trainer_name", newName).apply();
                TextView tvSaludo = findViewById(R.id.tvSaludo);
                tvSaludo.setText("Entrenador: " + newName);
                Toast.makeText(this, "¡Nombre de Entrenador actualizado!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
            }
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void setupMenuButton(int viewId, String title) {
        View view = findViewById(viewId);
        if (view != null) {
            view.setOnClickListener(v -> Toast.makeText(this, "Sección: " + title, Toast.LENGTH_SHORT).show());
        }
    }

    private void mostrarDialogoMedallas() {
        String userUid = "default";
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        SharedPreferences prefsMedallas = getSharedPreferences("PokeMedallas_" + userUid, MODE_PRIVATE);
        int medals = prefsMedallas.getInt("medals_count", 0);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_favoritos, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();
        if(dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        android.widget.GridLayout llContainer = dialogView.findViewById(R.id.llFavoritosContainer);
        llContainer.removeAllViews();
        llContainer.setColumnCount(2);

        for (int i = 1; i <= 10; i++) {
            View itemCard = getLayoutInflater().inflate(R.layout.item_favorito, llContainer, false);
            android.widget.ImageView ivMedal = itemCard.findViewById(R.id.ivPokemonFavo);
            android.widget.TextView tvTitle = itemCard.findViewById(R.id.tvPokemonNombreFavo);
            
            android.widget.GridLayout.LayoutParams params = new android.widget.GridLayout.LayoutParams();
            params.width = android.widget.GridLayout.LayoutParams.WRAP_CONTENT;
            params.height = android.widget.GridLayout.LayoutParams.WRAP_CONTENT;
            params.setMargins(16, 16, 16, 16);
            itemCard.setLayoutParams(params);
            
            if (i <= medals) {
                ivMedal.setImageResource(R.drawable.ic_medallas);
                tvTitle.setText(TowerActivity.MEDALLAS_TORRE[i-1] + "\n🎖️ DESBLOQUEADA");
                tvTitle.setTextColor(Color.parseColor("#FFCC00"));
            } else {
                ivMedal.setImageResource(R.drawable.ic_medallas);
                ivMedal.setColorFilter(Color.GRAY);
                ivMedal.setAlpha(0.3f);
                tvTitle.setText("Medalla " + i + "\n🔒 Bloqueada");
                tvTitle.setTextColor(Color.GRAY);
            }
            llContainer.addView(itemCard);
        }

        Button btnCerrar = dialogView.findViewById(R.id.btnCerrarDialog);
        btnCerrar.setText("CERRAR MEDALLAS (" + medals + "/10)");
        btnCerrar.setOnClickListener(v2 -> dialog.dismiss());
        
        dialog.show();
    }
}
