package com.example.magyarorszagkincsei.details;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;

import com.example.magyarorszagkincsei.data.Kincs;
import com.example.magyarorszagkincsei.details.ImagePagerAdapter;
import com.example.magyarorszagkincsei.R; // HOZZÁADVA
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Kibővített adatlap fragment több kép kezelésével, leírással és értékelési rendszerrel.
 */
public class KincsReszletekFragment extends Fragment {

    private String docId, felfedezoString;
    private List<String> kepUrlLista;
    private float atlagErtekeles;
    private int ertekelesekSzama;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_kincs_reszletek, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // UI elemek
        TextView tvNev = view.findViewById(R.id.reszletNev);
        TextView tvFelfedezo = view.findViewById(R.id.reszletFelfedezo);
        TextView tvLeiras = view.findViewById(R.id.reszletLeiras);
        TextView tvAtlagText = view.findViewById(R.id.tvAtlagErtekeles);
        TextView tvSzamlalo = view.findViewById(R.id.tvKepSzamlalo);
        ViewPager2 viewPager = view.findViewById(R.id.viewPagerKepek);
        
        RatingBar rbAtlag = view.findViewById(R.id.ratingBarAtlag);
        RatingBar rbUser = view.findViewById(R.id.ratingBarUser);
        Button btnVissza = view.findViewById(R.id.visszaGomb);
        Button btnTorles = view.findViewById(R.id.torlesGomb);

        // Adatok átvétele az argumentumokból
        if (getArguments() != null) {
            docId = getArguments().getString("ID");
            String nev = getArguments().getString("NEV");
            String leiras = getArguments().getString("LEIRAS");
            felfedezoString = getArguments().getString("FELFEDEZO");
            kepUrlLista = getArguments().getStringArrayList("KEPEK");
            atlagErtekeles = getArguments().getFloat("ATLAG", 0f);
            ertekelesekSzama = getArguments().getInt("SZAM", 0);

            if (kepUrlLista == null) kepUrlLista = new ArrayList<>();

            // Adatok megjelenítése
            tvNev.setText(nev);
            tvFelfedezo.setText(felfedezoString);
            tvLeiras.setText(leiras != null && !leiras.isEmpty() ? leiras : "");
            
            tvAtlagText.setText(getString(R.string.rating_title) + String.format(Locale.getDefault(), "%.1f", atlagErtekeles) + " (" + ertekelesekSzama + ")");
            rbAtlag.setRating(atlagErtekeles);

            // ViewPager2 beállítása a képekhez
            ImagePagerAdapter pagerAdapter = new ImagePagerAdapter(kepUrlLista);
            viewPager.setAdapter(pagerAdapter);

            // Kép számláló frissítése lapozáskor
            tvSzamlalo.setText("1 / " + kepUrlLista.size());
            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    tvSzamlalo.setText((position + 1) + " / " + kepUrlLista.size());
                }
            });
        }

        // --- ÉRTÉKELÉS LOGIKA ---
        if (mAuth.getCurrentUser() == null) {
            rbUser.setIsIndicator(true);
            view.findViewById(R.id.tvRatePrompt).setVisibility(View.GONE);
            TextView tvLoginPrompt = new TextView(requireContext());
            tvLoginPrompt.setText(R.string.login_to_rate);
            tvLoginPrompt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            ((LinearLayout)rbUser.getParent()).addView(tvLoginPrompt, 8);
        }

        rbUser.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (fromUser) {
                if (mAuth.getCurrentUser() != null) {
                    mentesErtekeles(rating);
                    rbUser.setIsIndicator(true);
                } else {
                    Toast.makeText(requireContext(), R.string.login_to_rate, Toast.LENGTH_SHORT).show();
                    rbUser.setRating(0);
                }
            }
        });

        btnVissza.setOnClickListener(v -> {
            // Vissza a térképre (vagy az előző fragmentre)
            Navigation.findNavController(v).navigate(R.id.nav_map);
        });

        btnTorles.setOnClickListener(v -> ellenorzesEsTorles());
    }

    private void mentesErtekeles(float ujRating) {
        if (docId == null) return;

        float ujAtlag = ((atlagErtekeles * ertekelesekSzama) + ujRating) / (ertekelesekSzama + 1);
        int ujSzam = ertekelesekSzama + 1;

        Map<String, Object> update = new HashMap<>();
        update.put("atlagErtekeles", ujAtlag);
        update.put("ertekelesekSzama", ujSzam);

        db.collection("kincsek").document(docId).update(update)
                .addOnSuccessListener(aVoid -> {
                    atlagErtekeles = ujAtlag;
                    ertekelesekSzama = ujSzam;
                    ((TextView)requireView().findViewById(R.id.tvAtlagErtekeles)).setText(getString(R.string.rating_title) + String.format(Locale.getDefault(), "%.1f", ujAtlag) + " (" + ujSzam + ")");
                    ((RatingBar)requireView().findViewById(R.id.ratingBarAtlag)).setRating(ujAtlag);
                    Toast.makeText(requireContext(), R.string.rating_saved, Toast.LENGTH_SHORT).show();
                });
    }

    private void ellenorzesEsTorles() {
        String currentUserEmail = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getEmail() : "";
        String prefix = getString(R.string.felfedezo_prefix);
        String tulajdonosEmail = felfedezoString != null ? felfedezoString.replace(prefix, "").trim() : "";

        if (currentUserEmail != null && currentUserEmail.equalsIgnoreCase(tulajdonosEmail)) {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.delete_treasure)
                    .setMessage(R.string.delete_confirm)
                    .setPositiveButton(R.string.delete_yes, (dialog, which) -> veglegesTorlesFolyamat())
                    .setNegativeButton(R.string.delete_no, null)
                    .show();
        } else {
            Toast.makeText(requireContext(), R.string.no_permission_delete, Toast.LENGTH_LONG).show();
        }
    }

    private void veglegesTorlesFolyamat() {
        if (kepUrlLista != null) {
            for (String url : kepUrlLista) {
                try {
                    FirebaseStorage.getInstance().getReferenceFromUrl(url).delete();
                } catch (Exception e) { /* ignore */ }
            }
        }
        
        if (docId != null) {
            db.collection("kincsek").document(docId).delete().addOnSuccessListener(aVoid -> {
                // Vissza a térképre sikeres törlés után
                Navigation.findNavController(requireView()).navigate(R.id.nav_map);
            });
        }
    }
}