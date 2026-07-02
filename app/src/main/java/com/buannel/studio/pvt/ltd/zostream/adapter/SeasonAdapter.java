package com.buannel.studio.pvt.ltd.zostream.adapter;

import android.annotation.SuppressLint;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import com.buannel.studio.pvt.ltd.zostream.R;
import com.buannel.studio.pvt.ltd.zostream.model.Season;

import java.util.List;

public class SeasonAdapter extends RecyclerView.Adapter<SeasonAdapter.ViewHolder> {

    private List<Season> list;
    private int selected = 0;
    private OnSeasonClick listener;

    public interface OnSeasonClick {
        void onClick(Season season, int position);
    }

    public SeasonAdapter(List<Season> list, OnSeasonClick listener) {
        this.list = list;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_season, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        Season season = list.get(position);

        holder.txtSeason.setText("Season " + season.seasonNumber);

        holder.itemView.setOnClickListener(v -> {
            selected = position;
            notifyDataSetChanged();
            listener.onClick(season, position);
        });

        // Focus animation
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            v.setScaleX(hasFocus ? 1.15f : 1f);
            v.setScaleY(hasFocus ? 1.15f : 1f);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtSeason;

        public ViewHolder(View itemView) {
            super(itemView);
            txtSeason = itemView.findViewById(R.id.txtSeason);
        }
    }
}