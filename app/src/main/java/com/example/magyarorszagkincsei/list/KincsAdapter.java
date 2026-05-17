package com.example.magyarorszagkincsei.list;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.magyarorszagkincsei.data.Kincs;
import com.example.magyarorszagkincsei.R; // HOZZÁADVA
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Frissített adapter, amely már kezeli a kattintást, a törlést és a több képet is.
 */
public class KincsAdapter extends RecyclerView.Adapter<KincsAdapter.KincsViewHolder> {

    private List<Kincs> kincsLista;
    private OnKincsClickListener listener;

    public interface OnKincsClickListener {
        void onKincsClick(Kincs kincs);
    }

    public KincsAdapter(List<Kincs> kincsLista, OnKincsClickListener listener) {
        this.kincsLista = kincsLista;
        this.listener = listener;
    }

    @NonNull
    @Override
    public KincsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_kincs, parent, false);
        return new KincsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull KincsViewHolder holder, int position) {
        Kincs kincs = kincsLista.get(position);

        holder.tvNev.setText(kincs.getNev());
        holder.tvFelfedezo.setText(kincs.getFelfedezo());

        // Az első képet mutatjuk a listában index képnek
        Glide.with(holder.itemView.getContext())
                .load(kincs.getKepUrl())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.ivKep);

        // KATTINTÁS: Megnyitja a kincs adatlapját minden adattal a Navigation Componenttel
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onKincsClick(kincs);
            }
        });

        // HOSSZÚ KATTINTÁS A TÖRLÉSHEZ
        holder.itemView.setOnLongClickListener(v -> {
            String aktualisEmail = "";
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                aktualisEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            }

            if (aktualisEmail != null && aktualisEmail.equalsIgnoreCase(kincs.getFelfedezo())) {
                new AlertDialog.Builder(v.getContext())
                        .setTitle(R.string.delete_treasure)
                        .setMessage(R.string.delete_confirm)
                        .setPositiveButton(R.string.delete_yes, (dialog, which) -> {
                            torlesFirestorebol(kincs, position, v);
                        })
                        .setNegativeButton(R.string.delete_no, null)
                        .show();
            } else {
                Toast.makeText(v.getContext(), R.string.no_permission_delete, Toast.LENGTH_SHORT).show();
            }
            return true;
        });
    }

    private void torlesFirestorebol(Kincs kincs, int position, View v) {
        String docId = kincs.getId();
        if (docId == null) return;

        FirebaseFirestore.getInstance().collection("kincsek").document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    kincsLista.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, kincsLista.size());
                });
    }

    @Override
    public int getItemCount() {
        return kincsLista != null ? kincsLista.size() : 0;
    }

    public static class KincsViewHolder extends RecyclerView.ViewHolder {
        TextView tvNev, tvFelfedezo;
        ImageView ivKep;
        public KincsViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNev = itemView.findViewById(R.id.itemNev);
            tvFelfedezo = itemView.findViewById(R.id.itemFelfedezo);
            ivKep = itemView.findViewById(R.id.itemKep);
        }
    }
}