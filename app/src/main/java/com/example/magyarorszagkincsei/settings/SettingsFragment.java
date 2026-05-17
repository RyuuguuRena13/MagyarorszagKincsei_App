package com.example.magyarorszagkincsei.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.example.magyarorszagkincsei.main.MainActivity; // Helyes import
import com.example.magyarorszagkincsei.R; // HOZZÁADVA

import java.util.Locale;

/**
 * A beállításokért felelős Fragment.
 * Itt a felhasználó kiválaszthatja a nyelvet, amit az app eltárol.
 */
public class SettingsFragment extends Fragment {

    private Spinner languageSpinner;
    private Button saveBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupUI(view);
        setupSpinner();

        saveBtn.setOnClickListener(v -> {
            String selectedLang = languageSpinner.getSelectedItem().toString();
            String langCode = selectedLang.equals("English") ? "en" : "hu";
            setLocale(langCode);
        });
    }

    private void setupUI(View view) {
        languageSpinner = view.findViewById(R.id.languageSpinner);
        saveBtn = view.findViewById(R.id.saveSettingsBtn);

        Toolbar toolbar = view.findViewById(R.id.toolbar_settings);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if (((AppCompatActivity) requireActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle(R.string.settings_title);
            ((AppCompatActivity) requireActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
    }

    private void setupSpinner() {
        String[] languages = {"Magyar", "English"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, languages);
        languageSpinner.setAdapter(adapter);

        SharedPreferences prefs = requireActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String currentLang = prefs.getString("My_Lang", "hu");
        if (currentLang.equals("en")) {
            languageSpinner.setSelection(1);
        } else {
            languageSpinner.setSelection(0);
        }
    }

    private void setLocale(String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Resources resources = requireContext().getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        SharedPreferences.Editor editor = requireActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE).edit();
        editor.putString("My_Lang", langCode);
        editor.apply();

        Toast.makeText(requireContext(), R.string.language_changed, Toast.LENGTH_SHORT).show();

        // Minden ablak bezárása és a MainActivity újraindítása friss nyelven
        Intent i = new Intent(requireActivity(), MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        requireActivity().finish();
    }
}