package com.habittracker.app.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.habittracker.app.R;
import com.habittracker.app.data.sync.SyncWorker;
import com.habittracker.app.util.TokenManager;

/**
 * Main activity hosting the bottom navigation and fragment container.
 * Schedules the periodic sync worker on first launch.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup bottom navigation with NavController
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
            NavigationUI.setupWithNavController(bottomNav, navController);
        }

        // Schedule periodic sync
        SyncWorker.schedule(this);

        // Record first use for delayed signup tracking
        TokenManager tokenManager = new TokenManager(this);
        tokenManager.recordFirstUse();
    }
}
