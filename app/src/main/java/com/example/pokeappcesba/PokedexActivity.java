package com.example.pokeappcesba;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PokedexActivity extends AppCompatActivity {
    private EditText etBusqueda;
    private MaterialCardView cardPokemon;
    private ImageView ivSprite, ivBack, ivShiny, ivShinyBack;
    private TextView tvNombre, tvId, tvInfoGral;
    private ProgressBar pbHp, pbAtk, pbDef, pbSpAtk, pbSpDef, pbSpd;
    private TextView tvHp, tvAtk, tvDef, tvSpAtk, tvSpDef, tvSpd;
    private Button btnCry, btnVerFavoritos;
    private CheckBox btnFavorito;

    private PokeApiService apiService;
    private MediaPlayer mediaPlayer;
    private String currentCryUrl = "";
    private SharedPreferences prefsFavoritos;
    private String currentPokemonName = "";

    // Variables para guardar las URLs de las formas
    private String urlFront = "", urlBack = "", urlShiny = "", urlShinyBack = "";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pokedex);

        prefsFavoritos = getSharedPreferences("PokeFavoritos", MODE_PRIVATE);

        etBusqueda = findViewById(R.id.etBusqueda);
        View btnBuscar = findViewById(R.id.btnBuscar);
        btnVerFavoritos = findViewById(R.id.btnVerFavoritos);
        cardPokemon = findViewById(R.id.cardPokemon);

        ivSprite = findViewById(R.id.ivSprite);
        ivBack = findViewById(R.id.ivBack);
        ivShiny = findViewById(R.id.ivShiny);
        ivShinyBack = findViewById(R.id.ivShinyBack);

        tvNombre = findViewById(R.id.tvNombre);
        tvId = findViewById(R.id.tvId);
        tvInfoGral = findViewById(R.id.tvInfoGral);
        btnCry = findViewById(R.id.btnCry);
        btnFavorito = findViewById(R.id.btnFavorito);

        pbHp = findViewById(R.id.pbHp); tvHp = findViewById(R.id.tvHp);
        pbAtk = findViewById(R.id.pbAtk); tvAtk = findViewById(R.id.tvAtk);
        pbDef = findViewById(R.id.pbDef); tvDef = findViewById(R.id.tvDef);
        pbSpAtk = findViewById(R.id.pbSpAtk); tvSpAtk = findViewById(R.id.tvSpAtk);
        pbSpDef = findViewById(R.id.pbSpDef); tvSpDef = findViewById(R.id.tvSpDef);
        pbSpd = findViewById(R.id.pbSpd); tvSpd = findViewById(R.id.tvSpd);

        mediaPlayer = new MediaPlayer();
        apiService = new Retrofit.Builder().baseUrl("https://pokeapi.co/api/v2/").addConverterFactory(GsonConverterFactory.create()).build().create(PokeApiService.class);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Cargar un Pokémon por defecto al entrar para que no se vea vacío
        buscarPokemon("pikachu");

        btnBuscar.setOnClickListener(v -> {
            String q = etBusqueda.getText().toString().trim().toLowerCase();
            if (!q.isEmpty()) {
                buscarPokemon(q);
                ocultarTeclado();
            }
        });

        btnCry.setOnClickListener(v -> {
            if (!currentCryUrl.isEmpty()) reproducirSonido(currentCryUrl);
        });

        btnFavorito.setOnClickListener(v -> {
            if (currentPokemonName.isEmpty()) return;
            if (btnFavorito.isChecked()) {
                prefsFavoritos.edit().putString(currentPokemonName, currentPokemonName).apply();
                Toast.makeText(this, currentPokemonName.toUpperCase() + " guardado en Favoritos", Toast.LENGTH_SHORT).show();
            } else {
                prefsFavoritos.edit().remove(currentPokemonName).apply();
                Toast.makeText(this, currentPokemonName.toUpperCase() + " eliminado de Favoritos", Toast.LENGTH_SHORT).show();
            }
        });

        btnVerFavoritos.setOnClickListener(v -> mostrarMenuFavoritosEstilizado());

        // --- INTERACTIVIDAD DEL CARRUSEL ---
        // Al tocar la imagen principal, vuelve a la normalidad
        ivSprite.setOnClickListener(v -> { if(!urlFront.isEmpty()) Glide.with(this).load(urlFront).into(ivSprite); });
        // Al tocar un círculo, carga esa URL en la imagen grande
        ivBack.setOnClickListener(v -> { if(!urlBack.isEmpty()) Glide.with(this).load(urlBack).into(ivSprite); });
        ivShiny.setOnClickListener(v -> { if(!urlShiny.isEmpty()) Glide.with(this).load(urlShiny).into(ivSprite); });
        ivShinyBack.setOnClickListener(v -> { if(!urlShinyBack.isEmpty()) Glide.with(this).load(urlShinyBack).into(ivSprite); });
    }

    private void buscarPokemon(String query) {
        apiService.getPokemon(query).enqueue(new Callback<Pokemon>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(Call<Pokemon> call, Response<Pokemon> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Pokemon p = response.body();
                    currentPokemonName = p.name;

                    tvNombre.setText(p.name);
                    tvId.setText(String.format("#%03d", p.id));
                    btnFavorito.setChecked(prefsFavoritos.contains(p.name));

                    List<String> tipos = new ArrayList<>();
                    if (p.types != null) for (Pokemon.TypeSlot ts : p.types) tipos.add(ts.type.name.toUpperCase());

                    tvInfoGral.setText("TIPO: " + String.join(", ", tipos) + "\nALTURA: " + (p.height/10.0) + "m | PESO: " + (p.weight/10.0) + "kg");

                    if (p.sprites != null) {
                        urlFront = p.sprites.frontDefault != null ? p.sprites.frontDefault : "";
                        urlBack = p.sprites.backDefault != null ? p.sprites.backDefault : "";
                        urlShiny = p.sprites.frontShiny != null ? p.sprites.frontShiny : "";
                        urlShinyBack = p.sprites.backShiny != null ? p.sprites.backShiny : "";

                        if (!urlFront.isEmpty()) Glide.with(PokedexActivity.this).load(urlFront).into(ivSprite);
                        if (!urlBack.isEmpty()) Glide.with(PokedexActivity.this).load(urlBack).into(ivBack);
                        if (!urlShiny.isEmpty()) Glide.with(PokedexActivity.this).load(urlShiny).into(ivShiny);
                        if (!urlShinyBack.isEmpty()) Glide.with(PokedexActivity.this).load(urlShinyBack).into(ivShinyBack);
                    }

                    if (p.stats != null) {
                        for (Pokemon.StatSlot s : p.stats) {
                            switch (s.stat.name) {
                                case "hp": animarBarra(pbHp, tvHp, s.baseStat); break;
                                case "attack": animarBarra(pbAtk, tvAtk, s.baseStat); break;
                                case "defense": animarBarra(pbDef, tvDef, s.baseStat); break;
                                case "special-attack": animarBarra(pbSpAtk, tvSpAtk, s.baseStat); break;
                                case "special-defense": animarBarra(pbSpDef, tvSpDef, s.baseStat); break;
                                case "speed": animarBarra(pbSpd, tvSpd, s.baseStat); break;
                            }
                        }
                    }

                    if (p.cries != null && p.cries.latest != null) {
                        currentCryUrl = p.cries.latest;
                        btnCry.setEnabled(true);
                    } else {
                        btnCry.setEnabled(false);
                    }
                    cardPokemon.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(PokedexActivity.this, "Pokémon no encontrado", Toast.LENGTH_SHORT).show();
                    cardPokemon.setVisibility(View.GONE);
                }
            }
            @Override
            public void onFailure(Call<Pokemon> call, Throwable t) {
                Toast.makeText(PokedexActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarMenuFavoritosEstilizado() {
        Map<String, ?> todosLosFavoritos = prefsFavoritos.getAll();
        if (todosLosFavoritos.isEmpty()) {
            Toast.makeText(this, "Aún no tienes favoritos guardados", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_favoritos);
        if(dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        LinearLayout llContainer = dialog.findViewById(R.id.llFavoritosContainer);
        Button btnCerrar = dialog.findViewById(R.id.btnCerrarDialog);

        for (String nombre : todosLosFavoritos.keySet()) {
            Button btnFav = new Button(this);
            btnFav.setText(nombre.toUpperCase());
            btnFav.setBackgroundColor(Color.parseColor("#313131"));
            btnFav.setTextColor(Color.WHITE);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 16);
            btnFav.setLayoutParams(params);

            btnFav.setOnClickListener(v -> {
                etBusqueda.setText(nombre);
                buscarPokemon(nombre);
                dialog.dismiss();
            });
            llContainer.addView(btnFav);
        }

        btnCerrar.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void animarBarra(ProgressBar pb, TextView tv, int valor) {
        tv.setText(String.valueOf(valor));
        ObjectAnimator.ofInt(pb, "progress", 0, valor).setDuration(800).start();
    }

    private void reproducirSonido(String url) {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void ocultarTeclado() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}