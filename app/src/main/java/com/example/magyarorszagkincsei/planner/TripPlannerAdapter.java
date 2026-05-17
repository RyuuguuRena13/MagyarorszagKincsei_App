package com.example.magyarorszagkincsei.planner;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.magyarorszagkincsei.data.Kincs;
import com.example.magyarorszagkincsei.R; // HOZZÁADVA

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter a kirándulás tervezőhöz.
 */
public class TripPlannerAdapter extends RecyclerView.Adapter<TripPlannerAdapter.TripViewHolder> {

    private List<Kincs> mindenKincs;
    private List<Kincs> szurtKincs;
    private OnKincsClickListener listener;

    public interface OnKincsClickListener {
        void onAddClick(Kincs kincs);
    }

    public TripPlannerAdapter(List<Kincs> kincsek, OnKincsClickListener listener) {
        this.mindenKincs = kincsek;
        this.szurtKincs = new ArrayList<>(kincsek);
        this.listener = listener;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_kincs_planner, parent, false);
        return new TripViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Kincs k = szurtKincs.get(position);
        holder.tvNev.setText(k.getNev());
        holder.tvFelfedezo.setText(k.getFelfedezo());

        Glide.with(holder.itemView.getContext())
                .load(k.getKepUrl())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.ivKep);

        holder.btnAdd.setOnClickListener(v -> listener.onAddClick(k));
    }

    @Override
    public int getItemCount() {
        return szurtKincs.size();
    }

    public void filter(String text) {
        szurtKincs.clear();
        if (text.isEmpty()) {
            szurtKincs.addAll(mindenKincs);
        } else {
            text = text.toLowerCase();
            for (Kincs k : mindenKincs) {
                if (k.getNev().toLowerCase().contains(text)) {
                    szurtKincs.add(k);
                }
            }
        }
        notifyDataSetChanged();
    }

    public static class TripViewHolder extends RecyclerView.ViewHolder {
        TextView tvNev, tvFelfedezo;
        ImageView ivKep;
        Button btnAdd;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNev = itemView.findViewById(R.id.itemNevPlanner);
            tvFelfedezo = itemView.findViewById(R.id.itemFelfedezoPlanner);
            ivKep = itemView.findViewById(R.id.itemKepPlanner);
            btnAdd = itemView.findViewById(R.id.btnAddTrip);
        }
    }
}