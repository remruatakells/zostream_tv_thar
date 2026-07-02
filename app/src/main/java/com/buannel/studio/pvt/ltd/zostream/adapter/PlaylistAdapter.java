package com.buannel.studio.pvt.ltd.zostream.adapter;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.buannel.studio.pvt.ltd.zostream.R;
import com.buannel.studio.pvt.ltd.zostream.model.Episode;
import com.buannel.studio.pvt.ltd.zostream.model.Movie;
import com.bumptech.glide.Glide;

import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<?> items;
    private final OnItemClick listener;
    private final OnNavigateUp navigateUpListener;

    public interface OnItemClick {
        void onClick(Object item);
    }

    public interface OnNavigateUp {
        boolean onUp();
    }

    public PlaylistAdapter(List<?> items, OnItemClick listener) {
        this(items, listener, null);
    }

    public PlaylistAdapter(List<?> items, OnItemClick listener, OnNavigateUp navigateUpListener) {
        this.items = items;
        this.listener = listener;
        this.navigateUpListener = navigateUpListener;
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_movie_cover, parent, false);
        return new MovieVH(v);

    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        Object item = items.get(position);

        MovieVH vh = (MovieVH) holder;

        if (item instanceof Episode) {
            Episode episode = (Episode) item;
            vh.title.setText("E" + episode.episodeNumber + " " + episode.title);

            Glide.with(holder.itemView.getContext())
                    .load(episode.thumbnail)
                    .into(vh.cover);
        } else {
            Movie movie = (Movie) item;
            vh.title.setText(movie.title);

            Glide.with(holder.itemView.getContext())
                    .load(movie.coverImg)
                    .into(vh.cover);
        }

        // 🔥 CLICK
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(item);
        });

        // 🔥 ENABLE FOCUS
        holder.itemView.setFocusable(true);
        holder.itemView.setFocusableInTouchMode(true);
        holder.itemView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN
                    && keyCode == KeyEvent.KEYCODE_DPAD_UP
                    && navigateUpListener != null) {
                return navigateUpListener.onUp();
            }
            return false;
        });

        // 🔥 FOCUS LISTENER
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {

            if (hasFocus) {
                vh.title.setTextColor(Color.WHITE);

            } else {
                vh.title.setTextColor(Color.LTGRAY);
            }
        });
    }

    static class MovieVH extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView title;

        MovieVH(View v) {
            super(v);
            cover = v.findViewById(R.id.cover);
            title = v.findViewById(R.id.title);
        }
    }
}
