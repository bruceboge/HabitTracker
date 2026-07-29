package com.habittracker.app.ui.settings;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.habittracker.app.BuildConfig;
import com.habittracker.app.R;
import com.habittracker.app.data.local.AppDatabase;
import com.habittracker.app.data.repository.HabitRepository;
import com.habittracker.app.ui.auth.LoginActivity;
import com.habittracker.app.util.TokenManager;

/**
 * Settings fragment with account info, notification controls,
 * data export, and sign out.
 */
public class SettingsFragment extends Fragment {

    private TokenManager tokenManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tokenManager = new TokenManager(requireContext());

        // Account info
        TextView accountEmail = view.findViewById(R.id.account_email);
        TextView accountLevel = view.findViewById(R.id.account_level);

        if (tokenManager.hasTokens()) {
            accountEmail.setText(tokenManager.getUserEmail());
        } else {
            accountEmail.setText("Local mode (no account)");
        }

        // Level from profile
        AppDatabase db = AppDatabase.getInstance(requireContext());
        HabitRepository repo = new HabitRepository(db);
        repo.getProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                accountLevel.setText(String.format("Level %d • %d XP • %d points",
                        profile.level, profile.xp, profile.points));
            } else {
                accountLevel.setText("Level 1 • 0 XP");
            }
        });

        // Notification switch
        SwitchMaterial notifSwitch = view.findViewById(R.id.notification_switch);
        notifSwitch.setChecked(tokenManager.isNotificationEnabled());
        notifSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            tokenManager.saveNotificationSettings(
                    isChecked, tokenManager.getNotificationHour(), tokenManager.getNotificationMinute()
            );
        });

        // Reminder time
        TextView reminderTimeValue = view.findViewById(R.id.reminder_time_value);
        updateTimeDisplay(reminderTimeValue);

        view.findViewById(R.id.reminder_time_row).setOnClickListener(v -> {
            new TimePickerDialog(requireContext(), (tp, hour, minute) -> {
                tokenManager.saveNotificationSettings(
                        tokenManager.isNotificationEnabled(), hour, minute
                );
                updateTimeDisplay(reminderTimeValue);
            }, tokenManager.getNotificationHour(), tokenManager.getNotificationMinute(), false).show();
        });

        // Export data
        view.findViewById(R.id.btn_export).setOnClickListener(v -> {
            // TODO: Implement JSON export
            android.widget.Toast.makeText(requireContext(),
                    "Export coming soon!", android.widget.Toast.LENGTH_SHORT).show();
        });

        // Sign out
        view.findViewById(R.id.btn_sign_out).setOnClickListener(v -> {
            tokenManager.logout();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // Version
        TextView versionText = view.findViewById(R.id.version_text);
        versionText.setText(getString(R.string.version, BuildConfig.VERSION_NAME));
    }

    private void updateTimeDisplay(TextView view) {
        int hour = tokenManager.getNotificationHour();
        int minute = tokenManager.getNotificationMinute();
        String amPm = hour >= 12 ? "PM" : "AM";
        int displayHour = hour % 12;
        if (displayHour == 0) displayHour = 12;
        view.setText(String.format("%d:%02d %s", displayHour, minute, amPm));
    }
}
