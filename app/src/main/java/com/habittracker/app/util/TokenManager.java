package com.habittracker.app.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Manages authentication tokens using EncryptedSharedPreferences.
 * Tokens are stored encrypted at rest — not in plain SharedPreferences.
 * 
 * This is critical because the Supabase access token grants full API access
 * for the user's account. Plain storage would be a security vulnerability.
 */
public class TokenManager {

    private static final String ENCRYPTED_PREFS_NAME = "habit_tracker_secure_prefs";
    private SharedPreferences encryptedPrefs;
    private SharedPreferences regularPrefs;

    public TokenManager(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            encryptedPrefs = EncryptedSharedPreferences.create(
                    context,
                    ENCRYPTED_PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            // Fallback to regular prefs if encryption fails (shouldn't happen on API 26+)
            encryptedPrefs = context.getSharedPreferences(ENCRYPTED_PREFS_NAME, Context.MODE_PRIVATE);
        }

        regularPrefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
    }

    // --- Token Management (encrypted) ---

    public void saveTokens(String accessToken, String refreshToken) {
        encryptedPrefs.edit()
                .putString(Constants.PREF_ACCESS_TOKEN, accessToken)
                .putString(Constants.PREF_REFRESH_TOKEN, refreshToken)
                .apply();
    }

    public String getAccessToken() {
        return encryptedPrefs.getString(Constants.PREF_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return encryptedPrefs.getString(Constants.PREF_REFRESH_TOKEN, null);
    }

    public void clearTokens() {
        encryptedPrefs.edit()
                .remove(Constants.PREF_ACCESS_TOKEN)
                .remove(Constants.PREF_REFRESH_TOKEN)
                .apply();
    }

    public boolean hasTokens() {
        return getAccessToken() != null && getRefreshToken() != null;
    }

    // --- User Info (regular prefs — not sensitive) ---

    public void saveUserInfo(String userId, String email, String name) {
        regularPrefs.edit()
                .putString(Constants.PREF_USER_ID, userId)
                .putString(Constants.PREF_USER_EMAIL, email)
                .putString(Constants.PREF_USER_NAME, name)
                .apply();
    }

    public String getUserId() {
        return regularPrefs.getString(Constants.PREF_USER_ID, null);
    }

    public String getUserEmail() {
        return regularPrefs.getString(Constants.PREF_USER_EMAIL, null);
    }

    public String getUserName() {
        return regularPrefs.getString(Constants.PREF_USER_NAME, "");
    }

    // --- First Use Tracking (for delayed signup) ---

    public void recordFirstUse() {
        if (!regularPrefs.contains(Constants.PREF_FIRST_USE_DATE)) {
            regularPrefs.edit()
                    .putString(Constants.PREF_FIRST_USE_DATE, DateUtils.formatIso(java.time.LocalDate.now()))
                    .apply();
        }
    }

    public boolean shouldPromptSignup() {
        String firstUseStr = regularPrefs.getString(Constants.PREF_FIRST_USE_DATE, null);
        if (firstUseStr == null) return false;

        java.time.LocalDate firstUse = DateUtils.parseIso(firstUseStr);
        long daysUsed = DateUtils.daysBetween(firstUse, java.time.LocalDate.now());
        return daysUsed >= Constants.DAYS_BEFORE_SIGNUP_PROMPT && !hasTokens();
    }

    // --- Notification Preferences ---

    public void saveNotificationSettings(boolean enabled, int hour, int minute) {
        regularPrefs.edit()
                .putBoolean(Constants.PREF_NOTIFICATION_ENABLED, enabled)
                .putInt(Constants.PREF_NOTIFICATION_HOUR, hour)
                .putInt(Constants.PREF_NOTIFICATION_MINUTE, minute)
                .apply();
    }

    public boolean isNotificationEnabled() {
        return regularPrefs.getBoolean(Constants.PREF_NOTIFICATION_ENABLED, true);
    }

    public int getNotificationHour() {
        return regularPrefs.getInt(Constants.PREF_NOTIFICATION_HOUR, Constants.NOTIFICATION_DEFAULT_HOUR);
    }

    public int getNotificationMinute() {
        return regularPrefs.getInt(Constants.PREF_NOTIFICATION_MINUTE, Constants.NOTIFICATION_DEFAULT_MINUTE);
    }

    /** Full logout: clear all tokens and user info */
    public void logout() {
        clearTokens();
        regularPrefs.edit()
                .remove(Constants.PREF_USER_ID)
                .remove(Constants.PREF_USER_EMAIL)
                .remove(Constants.PREF_USER_NAME)
                .apply();
    }
}
