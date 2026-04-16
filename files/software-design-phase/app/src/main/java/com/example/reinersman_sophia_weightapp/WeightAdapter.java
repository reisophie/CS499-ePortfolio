package com.example.reinersman_sophia_weightapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class WeightAdapter extends RecyclerView.Adapter<WeightAdapter.WeightViewHolder> {

    public interface OnDeleteClickListener {
        void onDelete(WeightEntry entry);
    }

    public interface OnRowClickListener {
        void onRowClick(WeightEntry entry);
    }

    private final List<WeightEntry> items;
    private final OnDeleteClickListener deleteListener;
    private final OnRowClickListener rowClickListener;

    public WeightAdapter(List<WeightEntry> items,
                         OnDeleteClickListener deleteListener,
                         OnRowClickListener rowClickListener) {
        this.items = items;
        this.deleteListener = deleteListener;
        this.rowClickListener = rowClickListener;
    }

    @NonNull
    @Override
    public WeightViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_weight_entry, parent, false);
        return new WeightViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull WeightViewHolder holder, int position) {
        WeightEntry entry = items.get(position);

        holder.tvDate.setText(entry.date);
        holder.tvWeight.setText(String.valueOf(entry.weight));

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onDelete(entry);
        });

        holder.itemView.setOnClickListener(v -> {
            if (rowClickListener != null) rowClickListener.onRowClick(entry);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class WeightViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvWeight;
        ImageButton btnDelete;

        WeightViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvWeight = itemView.findViewById(R.id.tvWeight);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}