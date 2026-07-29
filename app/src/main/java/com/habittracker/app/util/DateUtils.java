package com.habittracker.app.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Date utility methods used across the app.
 * All date logic uses java.time (API 26+) — no legacy Calendar/Date usage.
 */
public final class DateUtils {

    private DateUtils() {} // Prevent instantiation

    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter SHORT_FORMAT = DateTimeFormatter.ofPattern("MMM d");

    /** Format a LocalDate for display: "Jul 29, 2026" */
    public static String formatDisplay(LocalDate date) {
        return date.format(DISPLAY_FORMAT);
    }

    /** Format a LocalDate short: "Jul 29" */
    public static String formatShort(LocalDate date) {
        return date.format(SHORT_FORMAT);
    }

    /** Format a LocalDate as ISO: "2026-07-29" (for database/API) */
    public static String formatIso(LocalDate date) {
        return date.format(ISO_FORMAT);
    }

    /** Parse an ISO date string: "2026-07-29" → LocalDate */
    public static LocalDate parseIso(String dateString) {
        return LocalDate.parse(dateString, ISO_FORMAT);
    }

    /**
     * Get the start date for the heatmap grid (91 days ago from today).
     * The grid shows the last 13 weeks (91 days).
     */
    public static LocalDate getHeatmapStartDate() {
        return LocalDate.now().minusDays(Constants.HEATMAP_TOTAL_DAYS - 1);
    }

    /**
     * Convert a LocalDate to its position in the heatmap grid.
     * Returns [column, row] where column 0 = oldest week, row 0 = Monday.
     * Returns null if the date is outside the grid range.
     */
    public static int[] dateToGridPosition(LocalDate date, LocalDate gridStartDate) {
        long daysSinceStart = ChronoUnit.DAYS.between(gridStartDate, date);
        if (daysSinceStart < 0 || daysSinceStart >= Constants.HEATMAP_TOTAL_DAYS) {
            return null; // Outside grid range
        }

        // Adjust so Monday = row 0, Sunday = row 6
        int dayOfWeek = date.getDayOfWeek().getValue() - 1; // Monday=0, Sunday=6
        int weekColumn = (int) (daysSinceStart / 7);

        return new int[] { weekColumn, dayOfWeek };
    }

    /**
     * Get the day-of-week as an integer compatible with our cadence system.
     * Monday = 1, Sunday = 7 (ISO-8601 standard).
     */
    public static int getDayOfWeekIso(LocalDate date) {
        return date.getDayOfWeek().getValue();
    }

    /** Get abbreviated day name: "Mon", "Tue", etc. */
    public static String getDayAbbreviation(int isoDayOfWeek) {
        return DayOfWeek.of(isoDayOfWeek).getDisplayName(TextStyle.SHORT, Locale.getDefault());
    }

    /** Get days between two dates */
    public static long daysBetween(LocalDate start, LocalDate end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    /** Check if a date is today */
    public static boolean isToday(LocalDate date) {
        return date.equals(LocalDate.now());
    }

    /** Get a friendly greeting based on time of day */
    public static String getGreeting() {
        int hour = java.time.LocalTime.now().getHour();
        if (hour < 12) return "Good morning";
        if (hour < 17) return "Good afternoon";
        return "Good evening";
    }

    /**
     * Get a human-readable "time ago" string for streak display.
     * "Today", "Yesterday", "3 days ago", etc.
     */
    public static String getRelativeDay(LocalDate date) {
        long daysAgo = ChronoUnit.DAYS.between(date, LocalDate.now());
        if (daysAgo == 0) return "Today";
        if (daysAgo == 1) return "Yesterday";
        if (daysAgo < 7) return daysAgo + " days ago";
        if (daysAgo < 30) return (daysAgo / 7) + " weeks ago";
        return formatShort(date);
    }
}
