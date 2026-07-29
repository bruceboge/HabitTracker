package com.habittracker.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.habittracker.app.data.local.entity.ProfileEntity;

/**
 * Data Access Object for profiles table.
 * Single-row table (one profile per device for MVP).
 */
@Dao
public interface ProfileDao {

    @Query("SELECT * FROM profiles LIMIT 1")
    LiveData<ProfileEntity> getProfile();

    @Query("SELECT * FROM profiles LIMIT 1")
    ProfileEntity getProfileSync();

    @Query("SELECT * FROM profiles WHERE user_id = :userId")
    ProfileEntity getByUserId(String userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(ProfileEntity profile);

    @Update
    void update(ProfileEntity profile);

    @Query("UPDATE profiles SET xp = xp + :amount WHERE user_id = :userId")
    void addXp(String userId, int amount);

    @Query("UPDATE profiles SET points = points + :amount WHERE user_id = :userId")
    void addPoints(String userId, int amount);

    @Query("UPDATE profiles SET level = :level WHERE user_id = :userId")
    void updateLevel(String userId, int level);

    @Query("DELETE FROM profiles")
    void deleteAll();
}
