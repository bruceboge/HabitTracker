package com.habittracker.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room entity for habits table.
 * Maps to the Supabase 'habits' table for sync.
 */
@Entity(tableName = "habits")
public class HabitEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    @ColumnInfo(name = "user_id")
    public String userId;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "icon")
    public String icon;

    @ColumnInfo(name = "color")
    public String color;

    @ColumnInfo(name = "cadence")
    public String cadence;

    @ColumnInfo(name = "cadence_days")
    public String cadenceDays; // Stored as comma-separated: "1,2,3,4,5,6,7"

    @ColumnInfo(name = "difficulty_tiny")
    public String difficultyTiny;

    @ColumnInfo(name = "difficulty_normal")
    public String difficultyNormal;

    @ColumnInfo(name = "difficulty_stretch")
    public String difficultyStretch;

    @ColumnInfo(name = "is_archived")
    public boolean isArchived;

    @ColumnInfo(name = "created_at")
    public String createdAt; // ISO date string

    @ColumnInfo(name = "synced")
    public boolean synced;

    @ColumnInfo(name = "updated_at")
    public long updatedAt; // Epoch millis for conflict resolution

    public HabitEntity() {
        this.synced = false;
        this.updatedAt = System.currentTimeMillis();
    }
}
