package com.example.pokeappcesba;

import android.annotation.SuppressLint;
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
        
        setupMenuButton(R.id.btnMenuEmulator, "Battle Emulator");
        setupMenuButton(R.id.btnMenuVersus, "Battle Versus");
        setupMenuButton(R.id.btnMenuTorre, "Torre Pokémon");
        setupMenuButton(R.id.btnMenuQuienEs, "¿Quién es ese Pokémon?");
        setupMenuButton(R.id.btnMenuSafari, "Safari Pokémon");
        setupMenuButton(R.id.btnMenuMaestro, "Maestro de Tipos");
        setupMenuButton(R.id.btnMenuMemory, "PokéMemory");
        
        // Favoritos Dialog
        findViewById(R.id.btnMenuFavoritos).setOnClickListener(v -> {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_favoritos, null);
            AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
            if(dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialogView.findViewById(R.id.btnCerrarDialog).setOnClickListener(v2 -> dialog.dismiss());
            dialog.show();
        });

        setupMenuButton(R.id.btnMenuHistorial, "Historial");
        setupMenuButton(R.id.btnMenuMedallas, "Medallas");
        
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
}
