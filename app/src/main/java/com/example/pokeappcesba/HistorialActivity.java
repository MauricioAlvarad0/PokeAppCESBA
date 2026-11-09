package com.example.pokeappcesba;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.example.pokeappcesba.adapters.BattleRecordAdapter;
import com.example.pokeappcesba.models.BattleRecord;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistorialActivity extends AppCompatActivity {

    private ImageView btnBackHistorial;
    private TextView tvHistorialSummary;
    private Button btnClearHistorial;
    private RecyclerView rvHistorial;

    private SharedPreferences prefsHistorial;
    private SharedPreferences prefsGlobales;
    private BattleRecordAdapter adapter;
    private List<BattleRecord> recordList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial);

        prefsHistorial = getSharedPreferences("PokeHistorial", MODE_PRIVATE);
        prefsGlobales = getSharedPreferences("PokePrefs", MODE_PRIVATE);

        btnBackHistorial = findViewById(R.id.btnBackHistorial);
        tvHistorialSummary = findViewById(R.id.tvHistorialSummary);
        btnClearHistorial = findViewById(R.id.btnClearHistorial);
        rvHistorial = findViewById(R.id.rvHistorial);

        rvHistorial.setLayoutManager(new LinearLayoutManager(this));

        btnBackHistorial.setOnClickListener(v -> finish());
        btnClearHistorial.setOnClickListener(v -> clearHistory());

        loadHistory();
    }

    @SuppressLint("SetTextI18n")
    private void loadHistory() {
        int vic = prefsGlobales.getInt("versus_victorias", 0);
        int der = prefsGlobales.getInt("versus_derrotas", 0);

        tvHistorialSummary.setText("VS LOCAL: " + vic + " VICTORIAS | " + der + " DERROTAS");

        String json = prefsHistorial.getString("historial_batallas_json", "[]");
        Gson gson = new Gson();
        Type type = new TypeToken<List<BattleRecord>>() {}.getType();
        List<BattleRecord> loadedRecords = gson.fromJson(json, type);

        if (loadedRecords == null) {
            loadedRecords = new ArrayList<>();
        }

        // Reverse to show newest first
        Collections.reverse(loadedRecords);
        
        recordList.clear();
        recordList.addAll(loadedRecords);

        if (adapter == null) {
            adapter = new BattleRecordAdapter(recordList);
            rvHistorial.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    private void clearHistory() {
        prefsHistorial.edit().remove("historial_batallas_json").apply();
        loadHistory();
    }
}
