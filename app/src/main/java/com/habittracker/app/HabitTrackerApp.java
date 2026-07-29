package com.habittracker.app;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import com.habittracker.app.util.Constants;

/**
 * Application class — initializes notification channels.
 */
public class HabitTrackerApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Daily habit reminders");

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}
