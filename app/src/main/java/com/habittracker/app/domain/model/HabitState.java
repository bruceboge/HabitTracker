package com.habittracker.app.domain.model;

import java.time.LocalDate;

/**
 * Domain model for the algorithm's working state of a habit.
 * This is what the DifficultyEngine reads and writes.
 * Updated daily by the scaling algorithm.
 */
public class HabitState {

    private String habitId;
    private double difficultyLevel;      // 0.0–1.0 continuous difficulty
    private double completionRateEma;    // Exponential moving average of completion
    private double volatility;           // How much completion swings day-to-day
    private int streak;                  // Current unbroken streak
    private int bestStreak;              // All-time best streak
    private int totalCompletions;        // Total times completed
    private int consecutiveMisses;       // Current consecutive misses
    private boolean restartWinPending;   // True if next completion should be celebrated as restart
    private LocalDate lastCompletedAt;
    private LocalDate lastAdjustedAt;

    /** Creates a fresh state for a new habit */
    public HabitState(String habitId) {
        this.habitId = habitId;
        this.difficultyLevel = 0.3;        // Start at 30% — slightly above "tiny"
        this.completionRateEma = 0.5;      // Neutral starting point
        this.volatility = 0.0;
        this.streak = 0;
        this.bestStreak = 0;
        this.totalCompletions = 0;
        this.consecutiveMisses = 0;
        this.restartWinPending = false;
    }

    /** Full constructor for restoring from database */
    public HabitState(String habitId, double difficultyLevel, double completionRateEma,
                      double volatility, int streak, int bestStreak, int totalCompletions,
                      int consecutiveMisses, boolean restartWinPending,
                      LocalDate lastCompletedAt, LocalDate lastAdjustedAt) {
        this.habitId = habitId;
        this.difficultyLevel = difficultyLevel;
        this.completionRateEma = completionRateEma;
        this.volatility = volatility;
        this.streak = streak;
        this.bestStreak = bestStreak;
        this.totalCompletions = totalCompletions;
        this.consecutiveMisses = consecutiveMisses;
        this.restartWinPending = restartWinPending;
        this.lastCompletedAt = lastCompletedAt;
        this.lastAdjustedAt = lastAdjustedAt;
    }

    // --- Getters ---

    public String getHabitId() { return habitId; }
    public double getDifficultyLevel() { return difficultyLevel; }
    public double getCompletionRateEma() { return completionRateEma; }
    public double getVolatility() { return volatility; }
    public int getStreak() { return streak; }
    public int getBestStreak() { return bestStreak; }
    public int getTotalCompletions() { return totalCompletions; }
    public int getConsecutiveMisses() { return consecutiveMisses; }
    public boolean isRestartWinPending() { return restartWinPending; }
    public LocalDate getLastCompletedAt() { return lastCompletedAt; }
    public LocalDate getLastAdjustedAt() { return lastAdjustedAt; }

    // --- Setters ---

    public void setHabitId(String habitId) { this.habitId = habitId; }
    public void setDifficultyLevel(double difficultyLevel) { this.difficultyLevel = difficultyLevel; }
    public void setCompletionRateEma(double completionRateEma) { this.completionRateEma = completionRateEma; }
    public void setVolatility(double volatility) { this.volatility = volatility; }
    public void setStreak(int streak) { this.streak = streak; }
    public void setBestStreak(int bestStreak) { this.bestStreak = bestStreak; }
    public void setTotalCompletions(int totalCompletions) { this.totalCompletions = totalCompletions; }
    public void setConsecutiveMisses(int consecutiveMisses) { this.consecutiveMisses = consecutiveMisses; }
    public void setRestartWinPending(boolean restartWinPending) { this.restartWinPending = restartWinPending; }
    public void setLastCompletedAt(LocalDate lastCompletedAt) { this.lastCompletedAt = lastCompletedAt; }
    public void setLastAdjustedAt(LocalDate lastAdjustedAt) { this.lastAdjustedAt = lastAdjustedAt; }

    /** Creates a deep copy of this state (used by algorithm to avoid mutating the original) */
    public HabitState copy() {
        return new HabitState(
            habitId, difficultyLevel, completionRateEma, volatility,
            streak, bestStreak, totalCompletions, consecutiveMisses,
            restartWinPending, lastCompletedAt, lastAdjustedAt
        );
    }
}
