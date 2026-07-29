package com.habittracker.app.notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.habittracker.app.R;
import com.habittracker.app.ui.auth.SplashActivity;
import com.habittracker.app.util.Constants;
import com.habittracker.app.util.TokenManager;

import java.util.Calendar;

/**
 * Manages daily habit reminder notifications.
 * Uses AlarmManager for exact-time daily alarms.
 * The notification opens the app to the home screen.
 */
public class HabitReminderManager extends BroadcastReceiver {

    private static final int ALARM_REQUEST_CODE = 1001;
    private static final int NOTIFICATION_ID = 2001;

    @Override
    public void onReceive(Context context, Intent intent) {
        showReminderNotification(context);
    }

    /**
     * Schedule the daily reminder alarm.
     * Should be called after notification settings change and on app start.
     */
    public static void scheduleReminder(Context context) {
        TokenManager tokenManager = new TokenManager(context);

        if (!tokenManager.isNotificationEnabled()) {
            cancelReminder(context);
            return;
        }

        int hour = tokenManager.getNotificationHour();
        int minute = tokenManager.getNotificationMinute();

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, HabitReminderManager.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, ALARM_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Set alarm for next occurrence of the specified time
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // If the time has already passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Repeating daily alarm
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );
    }

    /** Cancel the reminder alarm */
    public static void cancelReminder(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, HabitReminderManager.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, ALARM_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);
    }

    /** Show the reminder notification */
    private static void showReminderNotification(Context context) {
        Intent tapIntent = new Intent(context, SplashActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingTap = PendingIntent.getActivity(
                context, 0, tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, Constants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("Time for your habits! 🎯")
                .setContentText("Tap to check in and keep your streak going")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingTap)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException ignored) {
            // Notification permission not granted — silently fail
        }
    }
}
