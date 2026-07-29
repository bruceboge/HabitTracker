package com.habittracker.app.util;

/**
 * App-wide constants. Supabase config comes from BuildConfig (set in build.gradle).
 */
public final class Constants {

    private Constants() {} // Prevent instantiation

    // Shared Preferences
    public static final String PREFS_NAME = "habit_tracker_prefs";
    public static final String PREF_ACCESS_TOKEN = "access_token";
    public static final String PREF_REFRESH_TOKEN = "refresh_token";
    public static final String PREF_USER_ID = "user_id";
    public static final String PREF_USER_EMAIL = "user_email";
    public static final String PREF_USER_NAME = "user_name";
    public static final String PREF_FIRST_USE_DATE = "first_use_date";
    public static final String PREF_NOTIFICATION_ENABLED = "notification_enabled";
    public static final String PREF_NOTIFICATION_HOUR = "notification_hour";
    public static final String PREF_NOTIFICATION_MINUTE = "notification_minute";

    // Delayed signup: days before prompting account creation
    public static final int DAYS_BEFORE_SIGNUP_PROMPT = 3;

    // Heatmap grid dimensions
    public static final int HEATMAP_COLUMNS = 13;   // ~13 weeks = ~91 days
    public static final int HEATMAP_ROWS = 7;       // Days per week (Mon–Sun)
    public static final int HEATMAP_TOTAL_DAYS = HEATMAP_COLUMNS * HEATMAP_ROWS; // 91

    // Sync intervals
    public static final int SYNC_INTERVAL_MINUTES = 15;

    // Points economy
    public static final int POINTS_BASE_COMPLETION = 10;
    public static final int POINTS_STREAK_BONUS_PER_DAY = 2;  // Starting at day 3
    public static final int POINTS_STREAK_BONUS_CAP = 20;
    public static final int POINTS_RESTART_WIN = 15;
    public static final int POINTS_DIFFICULTY_UPGRADE = 25;
    public static final int STREAK_BONUS_START_DAY = 3;

    // XP per level (simple linear progression)
    public static final int XP_PER_LEVEL = 100;

    // Notification
    public static final String NOTIFICATION_CHANNEL_ID = "habit_reminders";
    public static final String NOTIFICATION_CHANNEL_NAME = "Habit Reminders";
    public static final int NOTIFICATION_DEFAULT_HOUR = 8;
    public static final int NOTIFICATION_DEFAULT_MINUTE = 0;

    // Habit colors palette (8 curated colors)
    public static final String[] HABIT_COLORS = {
        "#4CAF50",  // Green
        "#7C4DFF",  // Purple
        "#FF5252",  // Red
        "#FF9800",  // Orange
        "#2196F3",  // Blue
        "#00BCD4",  // Teal
        "#E91E63",  // Pink
        "#FFC107"   // Amber
    };

    // Habit icon options
    public static final String[] HABIT_ICONS = {
        "🏃", "📖", "🧘", "💪", "🎨", "✍️", "🎵", "🧹",
        "💧", "🥗", "😴", "📱", "🧠", "🌱", "⭐", "🎯",
        "🏋️", "🚴", "🧑‍💻", "📝", "🎸", "🍎", "☕", "🌅"
    };

    // Intent extras
    public static final String EXTRA_HABIT_ID = "extra_habit_id";
    public static final String EXTRA_HABIT_NAME = "extra_habit_name";
}
