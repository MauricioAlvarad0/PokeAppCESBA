package com.example.pokeappcesba;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class PerfilActivity extends AppCompatActivity {

    private ImageView btnBackPerfil, ivPerfilAvatar;
    private TextView tvPerfilTrainerName, tvPerfilEmail;
    private TextView tvStatMedals, tvStatVersus, tvStatFlappy, tvStatWhos, tvStatTypeMaster, tvStatMemory, tvStatSafari, tvStatFavorites;
    private Button btnEditTrainerName;

    private SharedPreferences prefsGlobales;
    private SharedPreferences prefsMedallas;
    private SharedPreferences prefsMinijuegos;
    private SharedPreferences prefsFavoritos;
    private SharedPreferences prefsSafari;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        String userUid = "default";
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userUid = user.getUid();
        }

        prefsGlobales = getSharedPreferences("PokePrefs", MODE_PRIVATE);
        prefsMedallas = getSharedPreferences("PokeMedallas_" + userUid, MODE_PRIVATE);
        prefsMinijuegos = getSharedPreferences("PokeMinijuegos_" + userUid, MODE_PRIVATE);
        prefsFavoritos = getSharedPreferences("PokeFavoritos", MODE_PRIVATE);
        prefsSafari = getSharedPreferences("PokeSafari_" + userUid, MODE_PRIVATE);

        btnBackPerfil = findViewById(R.id.btnBackPerfil);
        ivPerfilAvatar = findViewById(R.id.ivPerfilAvatar);
        tvPerfilTrainerName = findViewById(R.id.tvPerfilTrainerName);
        tvPerfilEmail = findViewById(R.id.tvPerfilEmail);

        tvStatMedals = findViewById(R.id.tvStatMedals);
        tvStatVersus = findViewById(R.id.tvStatVersus);
        tvStatFlappy = findViewById(R.id.tvStatFlappy);
        tvStatWhos = findViewById(R.id.tvStatWhos);
        tvStatTypeMaster = findViewById(R.id.tvStatTypeMaster);
        tvStatMemory = findViewById(R.id.tvStatMemory);
        tvStatSafari = findViewById(R.id.tvStatSafari);
        tvStatFavorites = findViewById(R.id.tvStatFavorites);

        btnEditTrainerName = findViewById(R.id.btnEditTrainerName);

        btnBackPerfil.setOnClickListener(v -> finish());
        btnEditTrainerName.setOnClickListener(v -> editNameDialog());

        loadProfileData();
    }

    @SuppressLint("SetTextI18n")
    private void loadProfileData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String currentName = prefsGlobales.getString("trainer_name", "");
        if (currentName.isEmpty() && user != null && user.getEmail() != null) {
            currentName = user.getEmail().split("@")[0];
        }

        tvPerfilTrainerName.setText("Entrenador: " + (currentName.isEmpty() ? "Mauricio" : currentName));
        tvPerfilEmail.setText(user != null && user.getEmail() != null ? user.getEmail() : "entrenador@pokeapp.com");

        int medals = prefsMedallas.getInt("medals_count", 0);
        int vic = prefsGlobales.getInt("versus_victorias", 0);
        int der = prefsGlobales.getInt("versus_derrotas", 0);

        int flappyRecord = prefsMinijuegos.getInt("flappy_record", 0);
        int whosRecord = prefsMinijuegos.getInt("record_racha", 0);
        int maestroRecord = prefsMinijuegos.getInt("maestro_record", 0);
        int memoryRecord = prefsMinijuegos.getInt("memory_record", 999);
        int safariCount = prefsSafari.getAll().size();
        int favCount = prefsFavoritos.getAll().size();

        tvStatMedals.setText("🎖️ Medallas de la Torre: " + medals + " / 10");
        tvStatVersus.setText("⚔️ Duelos Versus: " + vic + " Victorias | " + der + " Derrotas");
        tvStatFlappy.setText("🦅 Flappy Poké Récord: " + flappyRecord + " Puntos");
        tvStatWhos.setText("❓ ¿Quién es ese Pokémon?: " + whosRecord + " Racha");
        tvStatTypeMaster.setText("⚡ Maestro de Tipos: " + maestroRecord + " Racha");
        tvStatMemory.setText("🧠 PokéMemory: " + (memoryRecord == 999 ? "--" : memoryRecord + " Movimientos"));
        tvStatSafari.setText("🌾 Safari Capturados: " + safariCount + " Pokémon");
        tvStatFavorites.setText("⭐ Pokémon Favoritos: " + favCount + " Guardados");
    }

    private void editNameDialog() {
        android.widget.EditText etInput = new android.widget.EditText(this);
        etInput.setHint("Nuevo Nombre");
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Editar Nombre de Entrenador")
                .setView(etInput)
                .setPositiveButton("Guardar", (d, w) -> {
                    String newName = etInput.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        prefsGlobales.edit().putString("trainer_name", newName).apply();
                        loadProfileData();
                        Toast.makeText(this, "Nombre actualizado", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
