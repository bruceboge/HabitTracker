package com.habittracker.app.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.habittracker.app.data.local.dao.DailyLogDao;
import com.habittracker.app.data.local.dao.HabitDao;
import com.habittracker.app.data.local.dao.HabitStateDao;
import com.habittracker.app.data.local.dao.ProfileDao;
import com.habittracker.app.data.local.entity.DailyLogEntity;
import com.habittracker.app.data.local.entity.HabitEntity;
import com.habittracker.app.data.local.entity.HabitStateEntity;
import com.habittracker.app.data.local.entity.ProfileEntity;

/**
 * Room database — the single source of truth for all local data.
 * Offline-first architecture: all UI reads come from here, sync pushes to Supabase.
 */
@Database(
        entities = {
                HabitEntity.class,
                HabitStateEntity.class,
                DailyLogEntity.class,
                ProfileEntity.class
        },
        version = 1,
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract HabitDao habitDao();
    public abstract HabitStateDao habitStateDao();
    public abstract DailyLogDao dailyLogDao();
    public abstract ProfileDao profileDao();

    /**
     * Thread-safe singleton accessor.
     * Uses double-checked locking to avoid synchronization overhead after initialization.
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "habit_tracker.db"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
