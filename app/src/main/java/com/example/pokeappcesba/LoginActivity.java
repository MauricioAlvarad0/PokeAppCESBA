package com.example.pokeappcesba;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import android.content.SharedPreferences;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import android.view.View;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText etEmail, etPassword, etName;
    private TextInputLayout layoutName;
    private boolean isRegisterMode = false;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etName = findViewById(R.id.etName);
        layoutName = findViewById(R.id.layoutName);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnRegister = findViewById(R.id.btnRegister);
        TextView tvOlvidePassword = findViewById(R.id.tvOlvidePassword);
        ImageView ivLoginPokemon = findViewById(R.id.ivLoginPokemon);
        MaterialCardView cardLogin = findViewById(R.id.cardLogin);

        // Cargar imagen de Pikachu con Glide
        Glide.with(this)
                .load("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/25.png")
                .into(ivLoginPokemon);

        // --- ANIMACIÓN DE ENTRADA ---
        cardLogin.setAlpha(0f);
        cardLogin.setTranslationY(150f);
        cardLogin.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(800)
                .setInterpolator(new DecelerateInterpolator())
                .setStartDelay(200)
                .start();

        btnLogin.setOnClickListener(v -> {
            if (isRegisterMode) {
                // Volver a modo login
                isRegisterMode = false;
                layoutName.setVisibility(View.GONE);
                btnLogin.setText("INICIAR SESIÓN");
                btnRegister.setText("CREAR UNA CUENTA NUEVA");
                tvOlvidePassword.setVisibility(View.VISIBLE);
                return;
            }

            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String pass = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
            if(!email.isEmpty() && !pass.isEmpty()) {
                mAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getLocalizedMessage() : "Revisa tus credenciales";
                        Toast.makeText(this, "Error: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Toast.makeText(this, "Llena todos los campos", Toast.LENGTH_SHORT).show();
            }
        });

        btnRegister.setOnClickListener(v -> {
            if (!isRegisterMode) {
                // Cambiar a modo registro
                isRegisterMode = true;
                layoutName.setVisibility(View.VISIBLE);
                btnLogin.setText("VOLVER AL LOGIN");
                btnRegister.setText("REGISTRARME AHORA");
                tvOlvidePassword.setVisibility(View.GONE);
                return;
            }

            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String pass = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
            
            if(name.isEmpty()) {
                Toast.makeText(this, "Por favor ingresa tu nombre de Entrenador", Toast.LENGTH_SHORT).show();
                return;
            }

            if(!email.isEmpty() && pass.length() >= 6) {
                mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        // Guardar nombre en SharedPreferences
                        SharedPreferences prefs = getSharedPreferences("PokePrefs", MODE_PRIVATE);
                        prefs.edit().putString("trainer_name", name).apply();

                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getLocalizedMessage() : "Error desconocido";
                        Toast.makeText(this, "Registro falló: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Toast.makeText(this, "Correo inválido o contraseña menor a 6 caracteres", Toast.LENGTH_SHORT).show();
            }
        });

        tvOlvidePassword.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if(!email.isEmpty()) {
                mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        Toast.makeText(this, "Correo de recuperación enviado", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "No se pudo enviar el correo", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "Ingresa tu correo primero", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
