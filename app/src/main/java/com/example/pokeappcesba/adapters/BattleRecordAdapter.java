package com.example.pokeappcesba.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pokeappcesba.R;
import com.example.pokeappcesba.models.BattleRecord;

import java.util.List;

public class BattleRecordAdapter extends RecyclerView.Adapter<BattleRecordAdapter.ViewHolder> {

    private final List<BattleRecord> records;

    public BattleRecordAdapter(List<BattleRecord> records) {
        this.records = records;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_battle_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BattleRecord record = records.get(position);

        holder.tvDate.setText(record.date);
        holder.tvWinnerBadge.setText("👑 GANADOR: " + record.winnerName);
        holder.tvP1Name.setText(record.p1Name);
        holder.tvP2Name.setText(record.p2Name);

        Glide.with(holder.itemView.getContext())
                .load(record.p1SpriteUrl)
                .into(holder.ivP1Sprite);

        Glide.with(holder.itemView.getContext())
                .load(record.p2SpriteUrl)
                .into(holder.ivP2Sprite);
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvWinnerBadge, tvP1Name, tvP2Name;
        ImageView ivP1Sprite, ivP2Sprite;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvWinnerBadge = itemView.findViewById(R.id.tvWinnerBadge);
            tvP1Name = itemView.findViewById(R.id.tvP1Name);
            tvP2Name = itemView.findViewById(R.id.tvP2Name);
            ivP1Sprite = itemView.findViewById(R.id.ivP1Sprite);
            ivP2Sprite = itemView.findViewById(R.id.ivP2Sprite);
        }
    }
}