package com.example.magyarorszagkincsei.main;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.magyarorszagkincsei.auth.AuthActivity;
import com.example.magyarorszagkincsei.R; // HOZZÁADVA
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;

/**
 * Fő Activity: Navigációs konténer a fragmenteknek, kezeli a Navigation Drawert és a nyelvi beállításokat.
 */
public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private NavController navController;
    private FirebaseAuth mAuth;
    private DrawerLayout drawer;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Nyelv betöltése még a setContentView előtt
        loadLocale();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // AppBarConfiguration beállítása a Navigation Drawer-hez
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_map, R.id.nav_list, R.id.nav_planner, R.id.nav_settings)
                .setOpenableLayout(drawer)
                .build();
        
        // Helyes mód a NavController beszerzésére NavHostFragment-ből
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_main);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        if (navController != null) {
            NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
            NavigationUI.setupWithNavController(navigationView, navController);
        }

        updateNavigationMenu();

        // Navigation Item kiválasztásának kezelése (pl. kijelentkezés)
        navigationView.setNavigationItemSelectedListener(this::onNavigationItemSelected);
    }

    private void loadLocale() {
        SharedPreferences prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String language = prefs.getString("My_Lang", "");
        if (!language.isEmpty()) {
            setLocale(language);
        }
    }

    private void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Resources res = getResources();
        Configuration config = res.getConfiguration();
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_logout) {
            mAuth.signOut();
            // Visszatérés az AuthActivity-re
            Intent intent = new Intent(this, AuthActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            drawer.closeDrawers();
            return true;
        } else if (id == R.id.nav_login) {
            // Ha vendégként van, de rányom a bejelentkezésre a menüben
            Intent intent = new Intent(this, AuthActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            drawer.closeDrawers();
            return true;
        }

        // A többi menüpontot a NavigationUI kezeli
        boolean handled = false;
        if (navController != null) {
             handled = NavigationUI.onNavDestinationSelected(item, navController);
        }
        drawer.closeDrawers();
        return handled;
    }

    private void updateNavigationMenu() {
        View headerView = navigationView.getHeaderView(0);
        TextView emailDisplay = headerView.findViewById(R.id.userEmailDisplay);

        boolean isUserLoggedIn = mAuth.getCurrentUser() != null;

        if (isUserLoggedIn) {
            emailDisplay.setText(mAuth.getCurrentUser().getEmail());
            navigationView.getMenu().findItem(R.id.nav_logout).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_login).setVisible(false);
        } else {
            emailDisplay.setText(getString(R.string.nav_header_guest));
            navigationView.getMenu().findItem(R.id.nav_logout).setVisible(false);
            navigationView.getMenu().findItem(R.id.nav_login).setVisible(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (navController != null) {
            return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
        }
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Frissítjük a menüt, ha visszatérünk az Activity-re (pl. kijelentkezés után)
        updateNavigationMenu();
    }
}