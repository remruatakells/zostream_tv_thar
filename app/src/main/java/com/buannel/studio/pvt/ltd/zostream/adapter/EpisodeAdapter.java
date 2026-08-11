package com.buannel.studio.pvt.ltd.zostream.adapter;

import android.graphics.Color;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.buannel.studio.pvt.ltd.zostream.R;
import com.buannel.studio.pvt.ltd.zostream.model.Episode;
import com.bumptech.glide.Glide;

import java.util.List;

public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.ViewHolder> {

    private List<Episode> list;
    private OnEpisodeClick listener;

    public interface OnEpisodeClick {
        void onClick(Episode episode);
    }

    public EpisodeAdapter(List<Episode> list) {
        this(list, null);
    }

    public EpisodeAdapter(List<Episode> list, OnEpisodeClick listener) {
        this.list = list;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_episode, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        Episode ep = list.get(position);

        holder.txtEpisode.setText("E" + ep.episodeNumber + " " + ep.title);

        Glide.with(holder.imgEpisode.getContext())
                .load(ep.thumbnail)
                .into(holder.imgEpisode);

        boolean hasAction = listener != null;
        holder.itemView.setFocusable(hasAction);
        holder.itemView.setFocusableInTouchMode(hasAction);
        holder.itemView.setClickable(hasAction);
        holder.itemView.setOnClickListener(hasAction ? v -> listener.onClick(ep) : null);
        holder.itemView.setOnKeyListener(hasAction ? (v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_UP
                    && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                    || keyCode == KeyEvent.KEYCODE_ENTER
                    || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                v.performClick();
                return true;
            }
            return false;
        } : null);

        // TV Focus animation
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            v.setScaleX(hasFocus ? 1.1f : 1f);
            v.setScaleY(hasFocus ? 1.1f : 1f);
            v.setAlpha(hasFocus ? 1f : 0.8f);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imgEpisode;
        TextView txtEpisode;

        public ViewHolder(View itemView) {
            super(itemView);
            imgEpisode = itemView.findViewById(R.id.imgEpisode);
            txtEpisode = itemView.findViewById(R.id.txtEpisode);
        }
    }
}
