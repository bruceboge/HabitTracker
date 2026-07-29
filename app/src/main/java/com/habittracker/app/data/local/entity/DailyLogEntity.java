package com.habittracker.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room entity for daily habit log entries.
 * Each row = one day's record for one habit.
 * Composite unique constraint on (habit_id, log_date) prevents duplicate entries.
 */
@Entity(tableName = "daily_logs",
        foreignKeys = @ForeignKey(
                entity = HabitEntity.class,
                parentColumns = "id",
                childColumns = "habit_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index(value = {"habit_id", "log_date"}, unique = true),
                @Index(value = {"habit_id"}),
                @Index(value = {"synced"})
        })
public class DailyLogEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    @ColumnInfo(name = "habit_id")
    public String habitId;

    @ColumnInfo(name = "log_date")
    public String logDate; // ISO date string: "2026-07-29"

    @ColumnInfo(name = "completed")
    public boolean completed;

    @ColumnInfo(name = "effort_level")
    public int effortLevel; // 0=unset, 1=hard, 2=ok, 3=easy

    @ColumnInfo(name = "difficulty_at_time")
    public double difficultyAtTime; // Snapshot of difficulty when logged

    @ColumnInfo(name = "synced")
    public boolean synced;

    @ColumnInfo(name = "created_at")
    public long createdAt; // Epoch millis

    public DailyLogEntity() {
        this.synced = false;
        this.createdAt = System.currentTimeMillis();
    }
}
