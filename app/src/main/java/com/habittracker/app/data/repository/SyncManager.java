package com.habittracker.app.data.repository;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.habittracker.app.data.local.AppDatabase;
import com.habittracker.app.data.local.entity.DailyLogEntity;
import com.habittracker.app.data.local.entity.HabitEntity;
import com.habittracker.app.data.local.entity.HabitStateEntity;
import com.habittracker.app.data.remote.SupabaseClient;
import com.habittracker.app.data.remote.api.DailyLogsApi;
import com.habittracker.app.data.remote.api.HabitsApi;
import com.habittracker.app.util.TokenManager;

import java.util.List;

import retrofit2.Response;

/**
 * Handles syncing local Room data to Supabase.
 * 
 * Strategy: push unsynced records to Supabase, then mark them as synced locally.
 * Uses last-write-wins for conflict resolution (good enough for solo MVP).
 */
public class SyncManager {

    private static final String TAG = "SyncManager";

    private final AppDatabase db;
    private final SupabaseClient supabaseClient;
    private final TokenManager tokenManager;
    private final Gson gson = new Gson();

    public SyncManager(AppDatabase db, SupabaseClient supabaseClient, TokenManager tokenManager) {
        this.db = db;
        this.supabaseClient = supabaseClient;
        this.tokenManager = tokenManager;
    }

    /**
     * Push all unsynced data to Supabase.
     * Called by SyncWorker (periodic) and after each check-in.
     * 
     * @return true if all syncs succeeded, false if any failed
     */
    public boolean syncToRemote() {
        if (!tokenManager.hasTokens()) {
            Log.d(TAG, "No auth tokens — skipping sync (local-only mode)");
            return true; // Not an error, just nothing to do
        }

        boolean allSuccess = true;

        try {
            allSuccess &= syncHabits();
            allSuccess &= syncHabitStates();
            allSuccess &= syncDailyLogs();
        } catch (Exception e) {
            Log.e(TAG, "Sync failed with exception", e);
            return false;
        }

        if (allSuccess) {
            Log.d(TAG, "Sync completed successfully");
        } else {
            Log.w(TAG, "Sync completed with some failures");
        }

        return allSuccess;
    }

    /** Sync unsynced habits to Supabase */
    private boolean syncHabits() {
        List<HabitEntity> unsynced = db.habitDao().getUnsynced();
        if (unsynced.isEmpty()) return true;

        Log.d(TAG, "Syncing " + unsynced.size() + " habits");
        HabitsApi api = supabaseClient.getHabitsApi();
        boolean allSuccess = true;

        for (HabitEntity habit : unsynced) {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("id", habit.id);
                json.addProperty("user_id", tokenManager.getUserId());
                json.addProperty("name", habit.name);
                json.addProperty("icon", habit.icon);
                json.addProperty("color", habit.color);
                json.addProperty("cadence", habit.cadence);
                json.addProperty("cadence_days", habit.cadenceDays);
                json.addProperty("difficulty_tiny", habit.difficultyTiny);
                json.addProperty("difficulty_normal", habit.difficultyNormal);
                json.addProperty("difficulty_stretch", habit.difficultyStretch);
                json.addProperty("is_archived", habit.isArchived);
                json.addProperty("created_at", habit.createdAt);

                Response<List<JsonObject>> response = api.insertHabit(json).execute();
                if (response.isSuccessful()) {
                    db.habitDao().markSynced(habit.id);
                    Log.d(TAG, "Synced habit: " + habit.name);
                } else if (response.code() == 409) {
                    // Conflict — record already exists, try update instead
                    Response<List<JsonObject>> updateResponse =
                            api.updateHabit("eq." + habit.id, json).execute();
                    if (updateResponse.isSuccessful()) {
                        db.habitDao().markSynced(habit.id);
                    } else {
                        allSuccess = false;
                    }
                } else {
                    Log.e(TAG, "Failed to sync habit " + habit.id + ": " + response.code());
                    allSuccess = false;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error syncing habit " + habit.id, e);
                allSuccess = false;
            }
        }

        return allSuccess;
    }

    /** Sync unsynced habit states to Supabase */
    private boolean syncHabitStates() {
        List<HabitStateEntity> unsynced = db.habitStateDao().getUnsynced();
        if (unsynced.isEmpty()) return true;

        Log.d(TAG, "Syncing " + unsynced.size() + " habit states");
        HabitsApi api = supabaseClient.getHabitsApi();
        boolean allSuccess = true;

        for (HabitStateEntity state : unsynced) {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("habit_id", state.habitId);
                json.addProperty("difficulty_level", state.difficultyLevel);
                json.addProperty("completion_rate_ema", state.completionRateEma);
                json.addProperty("volatility", state.volatility);
                json.addProperty("streak", state.streak);
                json.addProperty("best_streak", state.bestStreak);
                json.addProperty("total_completions", state.totalCompletions);
                json.addProperty("consecutive_misses", state.consecutiveMisses);
                json.addProperty("restart_win_pending", state.restartWinPending);
                if (state.lastCompletedAt != null) {
                    json.addProperty("last_completed_at", state.lastCompletedAt);
                }
                if (state.lastAdjustedAt != null) {
                    json.addProperty("last_adjusted_at", state.lastAdjustedAt);
                }

                Response<List<JsonObject>> response = api.insertHabitState(json).execute();
                if (response.isSuccessful()) {
                    db.habitStateDao().markSynced(state.habitId);
                } else if (response.code() == 409) {
                    Response<List<JsonObject>> updateResponse =
                            api.updateHabitState("eq." + state.habitId, json).execute();
                    if (updateResponse.isSuccessful()) {
                        db.habitStateDao().markSynced(state.habitId);
                    } else {
                        allSuccess = false;
                    }
                } else {
                    allSuccess = false;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error syncing habit state " + state.habitId, e);
                allSuccess = false;
            }
        }

        return allSuccess;
    }

    /** Sync unsynced daily logs to Supabase */
    private boolean syncDailyLogs() {
        List<DailyLogEntity> unsynced = db.dailyLogDao().getUnsynced();
        if (unsynced.isEmpty()) return true;

        Log.d(TAG, "Syncing " + unsynced.size() + " daily logs");
        DailyLogsApi api = supabaseClient.getDailyLogsApi();
        boolean allSuccess = true;

        for (DailyLogEntity log : unsynced) {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("id", log.id);
                json.addProperty("habit_id", log.habitId);
                json.addProperty("log_date", log.logDate);
                json.addProperty("completed", log.completed);
                json.addProperty("effort_level", log.effortLevel);
                json.addProperty("difficulty_at_time", log.difficultyAtTime);

                // Use upsert to handle re-submissions for the same day
                Response<List<JsonObject>> response =
                        api.upsertLog(json, "habit_id,log_date").execute();

                if (response.isSuccessful()) {
                    db.dailyLogDao().markSynced(log.id);
                } else {
                    Log.e(TAG, "Failed to sync log " + log.id + ": " + response.code());
                    allSuccess = false;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error syncing log " + log.id, e);
                allSuccess = false;
            }
        }

        return allSuccess;
    }
}
