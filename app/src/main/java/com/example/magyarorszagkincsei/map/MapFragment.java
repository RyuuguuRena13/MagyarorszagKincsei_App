package com.example.magyarorszagkincsei.map;

import android.Manifest;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.magyarorszagkincsei.data.Kincs; // Kincs osztály importja az új helyéről
import com.example.magyarorszagkincsei.R; // R osztály importja a fő csomagból
import com.example.magyarorszagkincsei.auth.AuthActivity; // AuthActivity importja
import com.example.magyarorszagkincsei.main.MainActivity; // MainActivity importja
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Térkép fragment: Kezeli a térképet, a kincsek mentését és a navigációt.
 */
public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private static final int PICK_IMAGE_REQUEST = 1;

    private List<Marker> markerLista = new ArrayList<>();
    private LatLng tempLatLng;
    private String tempNev;
    private String tempLeiras;
    private List<Uri> selectedImageUris = new ArrayList<>();

    private AlertDialog progressDialog;

    // Alapértelmezett helyszín és zoom szint
    private static final LatLng DEFAULT_HUNGARY_LOCATION = new LatLng(47.4979, 19.0402); // Budapest, Magyarország
    private static final float DEFAULT_ZOOM_LEVEL = 7.0f; // Országos szintű nézet

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupMap();
        setupSearchView(view);
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupSearchView(View view) {
        SearchView searchView = view.findViewById(R.id.search_view);
        searchView.setQueryHint(getString(R.string.search_hint));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                kereses(query);
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) { return false; }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setPadding(0, 150, 0, 0);

        try {
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style));
        } catch (Exception e) { e.printStackTrace(); }

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(true);

        // Alapértelmezett kameranézet beállítása
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_HUNGARY_LOCATION, DEFAULT_ZOOM_LEVEL));

        checkLocationPermission();
        kincsekBetoltese();

        mMap.setOnInfoWindowClickListener(marker -> {
            if (marker.getTag() != null) {
                Kincs k = (Kincs) marker.getTag();
                NavController navController = Navigation.findNavController(requireView());
                Bundle bundle = new Bundle();
                bundle.putString("ID", k.getId());
                bundle.putString("NEV", k.getNev());
                bundle.putString("LEIRAS", k.getLeiras());
                bundle.putString("FELFEDEZO", getString(R.string.felfedezo_prefix) + k.getFelfedezo());
                bundle.putStringArrayList("KEPEK", new ArrayList<>(k.getKepUrlLista()));
                bundle.putFloat("ATLAG", k.getAtlagErtekeles());
                bundle.putInt("SZAM", k.getErtekelesekSzama());
                navController.navigate(R.id.nav_details, bundle);
            }
        });

        mMap.setOnMapLongClickListener(this::kincsMentese);

        mMap.setOnPoiClickListener(poi -> {
            Marker p = mMap.addMarker(new MarkerOptions().position(poi.latLng).title(poi.name)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            if (p != null) p.showInfoWindow();
        });
    }

    private void kereses(String szoveg) {
        if (szoveg == null || szoveg.isEmpty()) return;
        boolean talalt = false;
        for (Marker m : markerLista) {
            if (m.getTitle().toLowerCase().contains(szoveg.toLowerCase())) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(m.getPosition(), 15));
                m.showInfoWindow();
                talalt = true;
                break;
            }
        }
        if (!talalt) {
            Geocoder geocoder = new Geocoder(requireContext());
            try {
                List<Address> cimek = geocoder.getFromLocationName(szoveg, 1);
                if (cimek != null && !cimek.isEmpty()) {
                    LatLng loc = new LatLng(cimek.get(0).getLatitude(), cimek.get(0).getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 13));
                }
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void kincsekBetoltese() {
        db.collection("kincsek").get().addOnSuccessListener(snapshots -> {
            mMap.clear();
            markerLista.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                Kincs k = doc.toObject(Kincs.class);
                k.setId(doc.getId());
                Marker m = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(k.getLat(), k.getLng()))
                        .title(k.getNev())
                        .snippet(getString(R.string.felfedezo_prefix) + k.getFelfedezo())
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                if (m != null) {
                    m.setTag(k);
                    markerLista.add(m);
                }
            }
        });
    }

    private void kincsMentese(LatLng latLng) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), R.string.login_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.new_treasure_title);

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 0);

        final EditText nameInput = new EditText(requireContext());
        nameInput.setHint(R.string.treasure_name_hint);
        layout.addView(nameInput);

        final EditText descInput = new EditText(requireContext());
        descInput.setHint(R.string.treasure_desc_hint);
        layout.addView(descInput);

        builder.setView(layout);

        builder.setPositiveButton(R.string.select_photo, (dialog, which) -> {
            tempNev = nameInput.getText().toString();
            tempLeiras = descInput.getText().toString();
            if (!tempNev.isEmpty()) {
                tempLatLng = latLng;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
            }
        });
        builder.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == requireActivity().RESULT_OK && data != null) {
            selectedImageUris.clear();
            if (data.getClipData() != null) {
                ClipData mClipData = data.getClipData();
                for (int i = 0; i < mClipData.getItemCount(); i++) {
                    selectedImageUris.add(mClipData.getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                selectedImageUris.add(data.getData());
            }
            feltoltesEsMentes();
        }
    }

    private void feltoltesEsMentes() {
        if (selectedImageUris.isEmpty()) return;
        showProgressDialog();

        List<String> feltoltottUrlLista = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger(0);
        int totalImages = selectedImageUris.size();

        for (Uri uri : selectedImageUris) {
            StorageReference ref = FirebaseStorage.getInstance().getReference().child("kepek/" + UUID.randomUUID().toString() + ".jpg");
            ref.putFile(uri).addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                feltoltottUrlLista.add(downloadUri.toString());
                if (counter.incrementAndGet() == totalImages) {
                    mentesFirestoreba(feltoltottUrlLista);
                }
            })).addOnFailureListener(e -> {
                if (counter.incrementAndGet() == totalImages) {
                    if (feltoltottUrlLista.isEmpty()) {
                        hideProgressDialog();
                        Toast.makeText(requireContext(), "Upload failed", Toast.LENGTH_SHORT).show();
                    } else {
                        // Ha volt már feltöltött kép, de ez sikertelen, akkor is mentsük el a meglévőket.
                        mentesFirestoreba(feltoltottUrlLista);
                    }
                }
            });
        }
    }

    private void mentesFirestoreba(List<String> urlLista) {
        Kincs ujKincs = new Kincs(tempNev, tempLeiras, mAuth.getCurrentUser().getEmail(), urlLista, tempLatLng.latitude, tempLatLng.longitude);

        db.collection("kincsek").add(ujKincs).addOnSuccessListener(d -> {
            hideProgressDialog();
            kincsekBetoltese();
            Toast.makeText(requireContext(), R.string.save_success, Toast.LENGTH_SHORT).show();
        });
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        }
    }

    private void showProgressDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(requireContext());
        b.setView(R.layout.progress_layout);
        b.setCancelable(false);
        progressDialog = b.create();
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMap != null) kincsekBetoltese();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        hideProgressDialog();
    }
}