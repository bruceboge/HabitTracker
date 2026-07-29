package com.habittracker.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.habittracker.app.data.local.entity.HabitStateEntity;

import java.util.List;

/**
 * Data Access Object for habit_state table.
 * The algorithm reads and writes state through this DAO.
 */
@Dao
public interface HabitStateDao {

    @Query("SELECT * FROM habit_state WHERE habit_id = :habitId")
    LiveData<HabitStateEntity> getByHabitId(String habitId);

    @Query("SELECT * FROM habit_state WHERE habit_id = :habitId")
    HabitStateEntity getByHabitIdSync(String habitId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(HabitStateEntity state);

    @Update
    void update(HabitStateEntity state);

    @Query("SELECT * FROM habit_state")
    List<HabitStateEntity> getAllSync();

    @Query("SELECT * FROM habit_state WHERE synced = 0")
    List<HabitStateEntity> getUnsynced();

    @Query("UPDATE habit_state SET synced = 1 WHERE habit_id = :habitId")
    void markSynced(String habitId);

    @Query("DELETE FROM habit_state WHERE habit_id = :habitId")
    void delete(String habitId);
}
