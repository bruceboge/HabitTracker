package com.habittracker.app.data.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.habittracker.app.data.local.AppDatabase;
import com.habittracker.app.data.remote.SupabaseClient;
import com.habittracker.app.data.repository.SyncManager;
import com.habittracker.app.util.Constants;
import com.habittracker.app.util.TokenManager;

import java.util.concurrent.TimeUnit;

/**
 * WorkManager Worker that runs periodic background sync.
 * 
 * Runs every 15 minutes when the device has network connectivity.
 * WorkManager handles scheduling, retry, and respects battery/Doze.
 */
public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";
    public static final String WORK_NAME = "habit_sync_worker";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting sync work");

        try {
            Context context = getApplicationContext();
            AppDatabase db = AppDatabase.getInstance(context);
            TokenManager tokenManager = new TokenManager(context);
            SupabaseClient client = SupabaseClient.getInstance(tokenManager);
            SyncManager syncManager = new SyncManager(db, client, tokenManager);

            boolean success = syncManager.syncToRemote();
            if (success) {
                Log.d(TAG, "Sync work completed successfully");
                return Result.success();
            } else {
                Log.w(TAG, "Sync work completed with failures — will retry");
                return Result.retry();
            }
        } catch (Exception e) {
            Log.e(TAG, "Sync work failed", e);
            return Result.retry();
        }
    }

    /**
     * Schedule the periodic sync worker.
     * Call this once at app startup (idempotent — KEEP policy means it won't re-create if exists).
     */
    public static void schedule(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncWork = new PeriodicWorkRequest.Builder(
                SyncWorker.class,
                Constants.SYNC_INTERVAL_MINUTES,
                TimeUnit.MINUTES
        )
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.MINUTES) // Don't sync immediately on first launch
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Don't replace existing schedule
                syncWork
        );

        Log.d(TAG, "Sync worker scheduled (every " + Constants.SYNC_INTERVAL_MINUTES + " min)");
    }

    /**
     * Trigger an immediate one-shot sync (e.g., after a check-in).
     */
    public static void syncNow(Context context) {
        androidx.work.OneTimeWorkRequest syncWork = new androidx.work.OneTimeWorkRequest.Builder(
                SyncWorker.class
        )
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .build();

        WorkManager.getInstance(context).enqueue(syncWork);
        Log.d(TAG, "One-shot sync enqueued");
    }
}
