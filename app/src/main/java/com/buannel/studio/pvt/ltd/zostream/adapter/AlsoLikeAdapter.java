package com.buannel.studio.pvt.ltd.zostream.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.buannel.studio.pvt.ltd.zostream.model.Movie;
import com.bumptech.glide.Glide;

import java.util.List;

public class AlsoLikeAdapter extends RecyclerView.Adapter<AlsoLikeAdapter.ViewHolder> {

    private List<Movie> list;

    public AlsoLikeAdapter(List<Movie> list) {
        this.list = list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        ImageView img = new ImageView(parent.getContext());
        img.setLayoutParams(new ViewGroup.LayoutParams(250, 350));
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setFocusable(true);
        img.setFocusableInTouchMode(true);

        return new ViewHolder(img);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Movie movie = list.get(position);

        Glide.with(holder.itemView.getContext())
                .load(movie.poster)
                .into((ImageView) holder.itemView);

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