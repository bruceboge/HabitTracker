package com.habittracker.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

/**
 * Room entity for the algorithm's working state of each habit.
 * One-to-one relationship with HabitEntity.
 * Updated by DifficultyEngine after each check-in and by the nightly server job.
 */
@Entity(tableName = "habit_state",
        foreignKeys = @ForeignKey(
                entity = HabitEntity.class,
                parentColumns = "id",
                childColumns = "habit_id",
                onDelete = ForeignKey.CASCADE
        ))
public class HabitStateEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "habit_id")
    public String habitId;

    @ColumnInfo(name = "difficulty_level")
    public double difficultyLevel;

    @ColumnInfo(name = "completion_rate_ema")
    public double completionRateEma;

    @ColumnInfo(name = "volatility")
    public double volatility;

    @ColumnInfo(name = "streak")
    public int streak;

    @ColumnInfo(name = "best_streak")
    public int bestStreak;

    @ColumnInfo(name = "total_completions")
    public int totalCompletions;

    @ColumnInfo(name = "consecutive_misses")
    public int consecutiveMisses;

    @ColumnInfo(name = "restart_win_pending")
    public boolean restartWinPending;

    @ColumnInfo(name = "last_completed_at")
    public String lastCompletedAt; // ISO date string, nullable

    @ColumnInfo(name = "last_adjusted_at")
    public String lastAdjustedAt; // ISO date string, nullable

    @ColumnInfo(name = "synced")
    public boolean synced;

    public HabitStateEntity() {
        this.difficultyLevel = 0.3;
        this.completionRateEma = 0.5;
        this.volatility = 0.0;
        this.streak = 0;
        this.bestStreak = 0;
        this.totalCompletions = 0;
        this.consecutiveMisses = 0;
        this.restartWinPending = false;
        this.synced = false;
    }
}
