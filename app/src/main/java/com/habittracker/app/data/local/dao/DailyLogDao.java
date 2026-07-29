package com.habittracker.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.habittracker.app.data.local.entity.DailyLogEntity;

import java.util.List;

/**
 * Data Access Object for daily_logs table.
 * Provides queries optimized for the heatmap (date-range), algorithm (recent N days),
 * and sync (unsynced records).
 */
@Dao
public interface DailyLogDao {

    /**
     * Get logs for a habit within a date range (for heatmap rendering).
     * Returns in ascending date order so index 0 = oldest.
     */
    @Query("SELECT * FROM daily_logs WHERE habit_id = :habitId " +
           "AND log_date >= :startDate AND log_date <= :endDate " +
           "ORDER BY log_date ASC")
    LiveData<List<DailyLogEntity>> getLogsForHabit(String habitId, String startDate, String endDate);

    /** Synchronous version for algorithm/sync use */
    @Query("SELECT * FROM daily_logs WHERE habit_id = :habitId " +
           "AND log_date >= :startDate AND log_date <= :endDate " +
           "ORDER BY log_date ASC")
    List<DailyLogEntity> getLogsForHabitSync(String habitId, String startDate, String endDate);

    /** Get the most recent N logs for a habit (for algorithm calculation) */
    @Query("SELECT * FROM daily_logs WHERE habit_id = :habitId " +
           "ORDER BY log_date DESC LIMIT :limit")
    List<DailyLogEntity> getRecentLogs(String habitId, int limit);

    /** Get today's log for a specific habit */
    @Query("SELECT * FROM daily_logs WHERE habit_id = :habitId AND log_date = :date LIMIT 1")
    DailyLogEntity getLogForDate(String habitId, String date);

    /** Check if a log exists for today */
    @Query("SELECT COUNT(*) FROM daily_logs WHERE habit_id = :habitId AND log_date = :date")
    int hasLogForDate(String habitId, String date);

    /** Insert or replace (for same-day re-submissions) */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(DailyLogEntity log);

    /** Get all unsynced logs for push to Supabase */
    @Query("SELECT * FROM daily_logs WHERE synced = 0 ORDER BY log_date ASC")
    List<DailyLogEntity> getUnsynced();

    /** Mark a log as synced */
    @Query("UPDATE daily_logs SET synced = 1 WHERE id = :logId")
    void markSynced(String logId);

    /** Mark all logs for a habit as synced */
    @Query("UPDATE daily_logs SET synced = 1 WHERE habit_id = :habitId AND synced = 0")
    void markAllSyncedForHabit(String habitId);

    /** Count completions for a habit (for stats) */
    @Query("SELECT COUNT(*) FROM daily_logs WHERE habit_id = :habitId AND completed = 1")
    int getCompletionCount(String habitId);

    /** Count total logged days for a habit */
    @Query("SELECT COUNT(*) FROM daily_logs WHERE habit_id = :habitId")
    int getTotalLogCount(String habitId);

    /** Get completion count for today across all habits (for dashboard progress bar) */
    @Query("SELECT COUNT(*) FROM daily_logs WHERE log_date = :today AND completed = 1")
    LiveData<Integer> getCompletedTodayCount(String today);

    /** Delete all logs for a habit */
    @Query("DELETE FROM daily_logs WHERE habit_id = :habitId")
    void deleteForHabit(String habitId);
}
