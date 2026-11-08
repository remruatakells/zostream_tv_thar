package com.buannel.studio.pvt.ltd.zostream.adapter;

import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.buannel.studio.pvt.ltd.zostream.model.Movie;
import com.bumptech.glide.Glide;

import java.util.List;

public class AlsoLikeAdapter extends RecyclerView.Adapter<AlsoLikeAdapter.ViewHolder> {

    private List<Movie> list;
    private OnMovieClick listener;

    public interface OnMovieClick {
        void onClick(Movie movie);
    }

    public AlsoLikeAdapter(List<Movie> list) {
        this(list, null);
    }

    public AlsoLikeAdapter(List<Movie> list, OnMovieClick listener) {
        this.list = list;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        ImageView img = new ImageView(parent.getContext());
        img.setLayoutParams(new ViewGroup.LayoutParams(250, 350));
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);

        return new ViewHolder(img);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Movie movie = list.get(position);

        Glide.with(holder.itemView.getContext())
                .load(movie.poster)
                .into((ImageView) holder.itemView);

        boolean hasAction = listener != null;
        holder.itemView.setFocusable(hasAction);
        holder.itemView.setFocusableInTouchMode(hasAction);
        holder.itemView.setClickable(hasAction);
        holder.itemView.setOnClickListener(hasAction ? v -> listener.onClick(movie) : null);
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

        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            v.setScaleX(hasFocus ? 1.1f : 1f);
            v.setScaleY(hasFocus ? 1.1f : 1f);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
