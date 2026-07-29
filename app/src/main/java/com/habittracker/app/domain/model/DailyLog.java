package com.habittracker.app.domain.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Domain model for a single day's log entry for a habit.
 * Records whether the habit was completed and the user's subjective effort.
 */
public class DailyLog {

    /** Effort levels — maps to the check-in UI's Hard/OK/Easy buttons */
    public static final int EFFORT_HARD = 1;
    public static final int EFFORT_OK = 2;
    public static final int EFFORT_EASY = 3;

    private String id;
    private String habitId;
    private LocalDate logDate;
    private boolean completed;
    private int effortLevel;          // 1=hard, 2=ok, 3=easy, 0=not set
    private double difficultyAtTime;  // Snapshot of difficulty level when this was logged
    private boolean synced;           // Whether this has been pushed to Supabase

    public DailyLog() {
        this.id = UUID.randomUUID().toString();
        this.logDate = LocalDate.now();
        this.synced = false;
    }

    public DailyLog(String habitId, boolean completed, int effortLevel, double difficultyAtTime) {
        this();
        this.habitId = habitId;
        this.completed = completed;
        this.effortLevel = effortLevel;
        this.difficultyAtTime = difficultyAtTime;
    }

    /** Full constructor for restoring from database */
    public DailyLog(String id, String habitId, LocalDate logDate, boolean completed,
                    int effortLevel, double difficultyAtTime, boolean synced) {
        this.id = id;
        this.habitId = habitId;
        this.logDate = logDate;
        this.completed = completed;
        this.effortLevel = effortLevel;
        this.difficultyAtTime = difficultyAtTime;
        this.synced = synced;
    }

    // --- Getters ---

    public String getId() { return id; }
    public String getHabitId() { return habitId; }
    public LocalDate getLogDate() { return logDate; }
    public boolean isCompleted() { return completed; }
    public int getEffortLevel() { return effortLevel; }
    public double getDifficultyAtTime() { return difficultyAtTime; }
    public boolean isSynced() { return synced; }

    // --- Setters ---

    public void setId(String id) { this.id = id; }
    public void setHabitId(String habitId) { this.habitId = habitId; }
    public void setLogDate(LocalDate logDate) { this.logDate = logDate; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public void setEffortLevel(int effortLevel) { this.effortLevel = effortLevel; }
    public void setDifficultyAtTime(double difficultyAtTime) { this.difficultyAtTime = difficultyAtTime; }
    public void setSynced(boolean synced) { this.synced = synced; }

    /** Returns a human-readable effort label */
    public String getEffortLabel() {
        switch (effortLevel) {
            case EFFORT_HARD: return "😫 Hard";
            case EFFORT_OK:   return "😐 OK";
            case EFFORT_EASY: return "😊 Easy";
            default:          return "—";
        }
    }
}
