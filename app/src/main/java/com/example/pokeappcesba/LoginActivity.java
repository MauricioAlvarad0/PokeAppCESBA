package com.example.pokeappcesba;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import android.content.SharedPreferences;
import android.widget.EditText;
import android.text.InputType;
import android.view.View;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    private EditText etEmail, etPassword, etName;
    private View layoutName;
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

        // Corregido: Ahora btnLogin es un TextView para coincidir con tu XML retro
        TextView btnLogin = findViewById(R.id.btnLogin);
        TextView btnRegister = findViewById(R.id.btnRegister);
        TextView tvOlvidePassword = findViewById(R.id.tvOlvidePassword);
        ImageView ivLoginPokemon = findViewById(R.id.ivLoginPokemon);

        // Corregido: Ahora cardLogin es un LinearLayout
        LinearLayout cardLogin = findViewById(R.id.cardLogin);

        ImageView ivTogglePass = findViewById(R.id.ivTogglePass);
        if (ivTogglePass != null) {
            ivTogglePass.setOnClickListener(v -> {
                if(etPassword.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                    etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    ivTogglePass.setColorFilter(android.graphics.Color.parseColor("#38BDF8"));
                } else {
                    etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    ivTogglePass.setColorFilter(android.graphics.Color.parseColor("#94A3B8"));
                }
                if(etPassword.getText() != null) etPassword.setSelection(etPassword.getText().length());
            });
        }

        // Cargar imagen de Pikachu con Glide
        Glide.with(this)
                .load("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png")
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