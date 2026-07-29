package com.habittracker.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.habittracker.app.R;
import com.habittracker.app.data.local.AppDatabase;
import com.habittracker.app.ui.MainActivity;
import com.habittracker.app.util.TokenManager;

/**
 * Splash screen — routes user to the correct destination:
 * - Has valid session → Home
 * - Has local data (no account) → Home (local-only mode)
 * - Nothing → Login
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Short delay for splash branding, then route
        new Handler(Looper.getMainLooper()).postDelayed(this::routeUser, 1000);
    }

    private void routeUser() {
        TokenManager tokenManager = new TokenManager(this);

        if (tokenManager.hasTokens()) {
            // Has valid session → go to Home
            navigateToMain();
        } else {
            // Check if there's local data (delayed signup scenario)
            AppDatabase db = AppDatabase.getInstance(this);
            new Thread(() -> {
                // Check if user has any habits locally
                int habitCount = db.habitDao().getActiveHabitsSync().size();
                runOnUiThread(() -> {
                    if (habitCount > 0) {
                        // Has local data — continue without account
                        navigateToMain();
                    } else {
                        // Fresh install — show login
                        navigateToLogin();
                    }
                });
            }).start();
        }
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void navigateToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
