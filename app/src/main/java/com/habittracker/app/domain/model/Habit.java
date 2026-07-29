package com.habittracker.app.domain.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Domain model for a habit. This is the clean representation used by the UI and algorithm layers.
 * Maps to/from HabitEntity (Room) and HabitDto (Supabase).
 */
public class Habit {

    private String id;
    private String userId;
    private String name;
    private String icon;
    private String color;
    private String cadence;          // "daily", "weekdays", "custom"
    private List<Integer> cadenceDays; // 1=Mon ... 7=Sun
    private String difficultyTiny;    // User-defined label for easiest version
    private String difficultyNormal;  // User-defined label for normal version
    private String difficultyStretch; // User-defined label for hardest version
    private boolean archived;
    private LocalDate createdAt;

    public Habit() {
        this.id = UUID.randomUUID().toString();
        this.icon = "⭐";
        this.color = "#4CAF50";
        this.cadence = "daily";
        this.cadenceDays = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
        this.archived = false;
        this.createdAt = LocalDate.now();
    }

    public Habit(String name, String icon, String color) {
        this();
        this.name = name;
        this.icon = icon;
        this.color = color;
    }

    // --- Getters ---

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getIcon() { return icon; }
    public String getColor() { return color; }
    public String getCadence() { return cadence; }
    public List<Integer> getCadenceDays() { return cadenceDays; }
    public String getDifficultyTiny() { return difficultyTiny; }
    public String getDifficultyNormal() { return difficultyNormal; }
    public String getDifficultyStretch() { return difficultyStretch; }
    public boolean isArchived() { return archived; }
    public LocalDate getCreatedAt() { return createdAt; }

    // --- Setters ---

    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setName(String name) { this.name = name; }
    public void setIcon(String icon) { this.icon = icon; }
    public void setColor(String color) { this.color = color; }
    public void setCadence(String cadence) { this.cadence = cadence; }
    public void setCadenceDays(List<Integer> cadenceDays) { this.cadenceDays = cadenceDays; }
    public void setDifficultyTiny(String difficultyTiny) { this.difficultyTiny = difficultyTiny; }
    public void setDifficultyNormal(String difficultyNormal) { this.difficultyNormal = difficultyNormal; }
    public void setDifficultyStretch(String difficultyStretch) { this.difficultyStretch = difficultyStretch; }
    public void setArchived(boolean archived) { this.archived = archived; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }

    /**
     * Returns a human-readable difficulty label based on the current difficulty level.
     * Maps the 0.0–1.0 continuous value to one of four tiers.
     */
    public String getDifficultyLabel(double difficultyLevel) {
        if (difficultyLevel < 0.25) {
            return "🌱 Tiny";
        } else if (difficultyLevel < 0.55) {
            return "🌿 Building";
        } else if (difficultyLevel < 0.80) {
            return "💪 Pushing";
        } else {
            return "🔥 Peak";
        }
    }

    /**
     * Returns the user-defined task description for the current difficulty level.
     * Interpolates between tiny/normal/stretch labels.
     */
    public String getTaskDescription(double difficultyLevel) {
        if (difficultyLevel < 0.33) {
            return difficultyTiny != null ? difficultyTiny : "Start small";
        } else if (difficultyLevel < 0.67) {
            return difficultyNormal != null ? difficultyNormal : "Regular effort";
        } else {
            return difficultyStretch != null ? difficultyStretch : "Push yourself";
        }
    }

    /**
     * Checks if this habit is scheduled for the given day of week.
     * @param dayOfWeek 1=Monday ... 7=Sunday (ISO-8601)
     */
    public boolean isScheduledForDay(int dayOfWeek) {
        if ("daily".equals(cadence)) return true;
        if ("weekdays".equals(cadence)) return dayOfWeek >= 1 && dayOfWeek <= 5;
        return cadenceDays != null && cadenceDays.contains(dayOfWeek);
    }
}
