package com.example.magyarorszagkincsei.details;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.magyarorszagkincsei.R; // HOZZÁADVA

import java.util.List;

/**
 * Adapter a ViewPager2 számára, amely a kincshez tartozó képeket jeleníti meg.
 */
public class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder> {

    private List<String> kepUrlLista;

    public ImagePagerAdapter(List<String> kepUrlLista) {
        this.kepUrlLista = kepUrlLista;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_pager, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String url = kepUrlLista.get(position);
        Glide.with(holder.itemView.getContext())
                .load(url)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return kepUrlLista != null ? kepUrlLista.size() : 0;
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewPager);
        }
    }
}