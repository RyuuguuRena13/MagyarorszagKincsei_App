package com.example.magyarorszagkincsei.list;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.magyarorszagkincsei.data.Kincs;
import com.example.magyarorszagkincsei.list.KincsAdapter;
import com.example.magyarorszagkincsei.R; // HOZZÁADVA
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ez a Fragment felelős a már felfedezett kincsek listaszerű megjelenítéséért.
 */
public class KincsListaFragment extends Fragment implements KincsAdapter.OnKincsClickListener {

    private RecyclerView recyclerView;
    private KincsAdapter adapter;
    private List<Kincs> kincsLista;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_kincs_lista, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        setupToolbar(view);
        setupRecyclerView(view);

        betoltesFirebasebol();
    }

    private void setupToolbar(View view) {
        Toolbar toolbar = view.findViewById(R.id.toolbar_lista);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);

        if (((AppCompatActivity) requireActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) requireActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            ((AppCompatActivity) requireActivity()).getSupportActionBar().setDisplayShowHomeEnabled(true);
            ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle(R.string.menu_list);
        }
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());
    }

    private void setupRecyclerView(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewKincsek);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        kincsLista = new ArrayList<>();
        adapter = new KincsAdapter(kincsLista, this);
        recyclerView.setAdapter(adapter);
    }

    private void betoltesFirebasebol() {
        db.collection("kincsek")
                .get()
                .addOnSuccessListener(snapshots -> {
                    kincsLista.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Kincs kincs = doc.toObject(Kincs.class);
                        kincs.setId(doc.getId());
                        kincsLista.add(kincs);
                    }
                    adapter.notifyDataSetChanged();

                    if (kincsLista.isEmpty()) {
                        Toast.makeText(requireContext(), R.string.empty_fields, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Hiba az adatok lekérésekor: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onKincsClick(Kincs kincs) {
        NavController navController = Navigation.findNavController(requireView());
        Bundle bundle = new Bundle();
        bundle.putString("ID", kincs.getId());
        bundle.putString("NEV", kincs.getNev());
        bundle.putString("LEIRAS", kincs.getLeiras());
        bundle.putString("FELFEDEZO", getString(R.string.felfedezo_prefix) + kincs.getFelfedezo());
        bundle.putStringArrayList("KEPEK", new ArrayList<>(kincs.getKepUrlLista()));
        bundle.putFloat("ATLAG", kincs.getAtlagErtekeles());
        bundle.putInt("SZAM", kincs.getErtekelesekSzama());
        navController.navigate(R.id.nav_details, bundle);
    }

    @Override
    public void onResume() {
        super.onResume();
        betoltesFirebasebol();
    }
}