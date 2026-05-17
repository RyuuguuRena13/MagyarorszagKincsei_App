package com.example.magyarorszagkincsei.planner;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.magyarorszagkincsei.data.Kincs;
import com.example.magyarorszagkincsei.planner.TripPlannerAdapter;
import com.example.magyarorszagkincsei.R; // HOZZÁADVA
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Kirándulás tervező fragment: Saját kincsek és tetszőleges Google helyszínek keresése és hozzáadása.
 */
public class TripPlannerFragment extends Fragment {

    private RecyclerView recyclerView;
    private TripPlannerAdapter adapter;
    private List<Kincs> mindenKincs = new ArrayList<>();
    private List<Kincs> turaLista = new ArrayList<>();
    private TextView tvCount;
    private Button btnPlan;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trip_planner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        setupUI(view);
        betoltesFirebasebol();
    }

    private void setupUI(View view) {
        Toolbar toolbar = view.findViewById(R.id.toolbar_planner);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if (((AppCompatActivity) requireActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) requireActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        // A vissza gomb működését a NavController kezeli majd
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        tvCount = view.findViewById(R.id.tv_trip_count);
        btnPlan = view.findViewById(R.id.btn_plan_route);
        recyclerView = view.findViewById(R.id.recycler_available_kincsek);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        SearchView searchView = view.findViewById(R.id.planner_search);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                keresesGoogleHelyszinkent(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) adapter.filter(newText);
                return true;
            }
        });

        btnPlan.setOnClickListener(v -> inditGoogleMaps());
    }

    private void keresesGoogleHelyszinkent(String query) {
        if (query == null || query.isEmpty()) return;

        new Thread(() -> {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocationName(query, 5);
                requireActivity().runOnUiThread(() -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        boolean ujTalalat = false;
                        for (Address addr : addresses) {
                            String nev = addr.getFeatureName() != null ? addr.getFeatureName() : query;
                            String leiras = addr.getAddressLine(0);
                            
                            Kincs googleHely = new Kincs(nev, leiras, "Google Maps", new ArrayList<>(), addr.getLatitude(), addr.getLongitude());
                            
                            boolean marLetezik = false;
                            for (Kincs k : mindenKincs) {
                                if (Math.abs(k.getLat() - googleHely.getLat()) < 0.0001 && 
                                    Math.abs(k.getLng() - googleHely.getLng()) < 0.0001) {
                                    marLetezik = true;
                                    break;
                                }
                            }

                            if (!marLetezik) {
                                mindenKincs.add(0, googleHely);
                                ujTalalat = true;
                            }
                        }

                        if (ujTalalat) {
                            adapter.filter("");
                            Toast.makeText(requireContext(), "Google találatok hozzáadva a listához!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), "Ezek a helyszínek már szerepelnek a listában.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(requireContext(), "Nincs Google Térkép találat erre: " + query, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Hálózati hiba a Google keresés közben.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void betoltesFirebasebol() {
        db.collection("kincsek").get().addOnSuccessListener(snapshots -> {
            mindenKincs.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                Kincs k = doc.toObject(Kincs.class);
                k.setId(doc.getId());
                mindenKincs.add(k);
            }
            frissitAdapter();
        });
    }

    private void frissitAdapter() {
        adapter = new TripPlannerAdapter(mindenKincs, kincs -> {
            boolean marBenneVan = false;
            for (Kincs t : turaLista) {
                if (Math.abs(t.getLat() - kincs.getLat()) < 0.0001 && Math.abs(t.getLng() - kincs.getLng()) < 0.0001) {
                    marBenneVan = true;
                    break;
                }
            }
            if (!marBenneVan) {
                turaLista.add(kincs);
                frissitUI();
                Toast.makeText(requireContext(), R.string.item_added, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Ez már szerepel a tervben!", Toast.LENGTH_SHORT).show();
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void frissitUI() {
        if (turaLista.isEmpty()) {
            tvCount.setText(R.string.no_items_in_trip);
            btnPlan.setEnabled(false);
        } else {
            tvCount.setText(turaLista.size() + " " + getString(R.string.menu_list));
            btnPlan.setEnabled(true);
        }
    }

    private void inditGoogleMaps() {
        if (turaLista.isEmpty()) return;

        StringBuilder url = new StringBuilder("https://www.google.com/maps/dir/?api=1&origin=My+Location");
        
        Kincs utolso = turaLista.get(turaLista.size() - 1);
        url.append("&destination=").append(utolso.getLat()).append(",").append(utolso.getLng());

        if (turaLista.size() > 1) {
            url.append("&waypoints=");
            for (int i = 0; i < turaLista.size() - 1; i++) {
                Kincs k = turaLista.get(i);
                url.append(k.getLat()).append(",").append(k.getLng());
                if (i < turaLista.size() - 2) {
                    url.append("|");
                }
            }
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url.toString()));
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url.toString())));
        }
    }
}