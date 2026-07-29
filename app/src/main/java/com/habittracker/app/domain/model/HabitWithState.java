package com.habittracker.app.domain.model;

import java.util.List;

/**
 * Combined view of a Habit and its current algorithm state.
 * Used by the UI to render habit cards with live difficulty/streak info.
 */
public class HabitWithState {

    private Habit habit;
    private HabitState state;
    private List<DailyLog> recentLogs; // Last 91 days for heatmap

    public HabitWithState(Habit habit, HabitState state) {
        this.habit = habit;
        this.state = state;
    }

    public HabitWithState(Habit habit, HabitState state, List<DailyLog> recentLogs) {
        this.habit = habit;
        this.state = state;
        this.recentLogs = recentLogs;
    }

    public Habit getHabit() { return habit; }
    public HabitState getState() { return state; }
    public List<DailyLog> getRecentLogs() { return recentLogs; }

    public void setHabit(Habit habit) { this.habit = habit; }
    public void setState(HabitState state) { this.state = state; }
    public void setRecentLogs(List<DailyLog> recentLogs) { this.recentLogs = recentLogs; }

    /** Convenience: get the current difficulty label from the habit */
    public String getDifficultyLabel() {
        return habit.getDifficultyLabel(state.getDifficultyLevel());
    }

    /** Convenience: get today's task description */
    public String getTaskDescription() {
        return habit.getTaskDescription(state.getDifficultyLevel());
    }

    /** Convenience: check if habit was completed today */
    public boolean isCompletedToday() {
        if (recentLogs == null || recentLogs.isEmpty()) return false;
        DailyLog latest = recentLogs.get(recentLogs.size() - 1);
        return latest.getLogDate().equals(java.time.LocalDate.now()) && latest.isCompleted();
    }
}
