package com.habittracker.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.habittracker.app.data.local.entity.HabitEntity;

import java.util.List;

/**
 * Data Access Object for habits table.
 * LiveData queries auto-update the UI when data changes.
 */
@Dao
public interface HabitDao {

    @Query("SELECT * FROM habits WHERE is_archived = 0 ORDER BY created_at ASC")
    LiveData<List<HabitEntity>> getActiveHabits();

    @Query("SELECT * FROM habits WHERE is_archived = 0 ORDER BY created_at ASC")
    List<HabitEntity> getActiveHabitsSync();

    @Query("SELECT * FROM habits WHERE id = :habitId")
    LiveData<HabitEntity> getById(String habitId);

    @Query("SELECT * FROM habits WHERE id = :habitId")
    HabitEntity getByIdSync(String habitId);

    @Query("SELECT * FROM habits ORDER BY created_at ASC")
    List<HabitEntity> getAllSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(HabitEntity habit);

    @Update
    void update(HabitEntity habit);

    @Query("UPDATE habits SET is_archived = 1, synced = 0 WHERE id = :habitId")
    void archive(String habitId);

    @Query("DELETE FROM habits WHERE id = :habitId")
    void delete(String habitId);

    @Query("SELECT * FROM habits WHERE synced = 0")
    List<HabitEntity> getUnsynced();

    @Query("UPDATE habits SET synced = 1 WHERE id = :habitId")
    void markSynced(String habitId);

    @Query("SELECT COUNT(*) FROM habits WHERE is_archived = 0")
    LiveData<Integer> getActiveHabitCount();
}
