package se.jonsolberg.teatimer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.RingtoneManager;

import java.util.Arrays;

final class Notifications {
    private static final String CH_RUNNING = "running";
    private static final String CH_READY = "ready";
    private static final int ID_RUNNING = 1;
    private static final int ID_READY = 2;

    private Notifications() {
    }

    static void ensureChannels(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);

        NotificationChannel running = new NotificationChannel(CH_RUNNING,
                c.getString(R.string.channel_running), NotificationManager.IMPORTANCE_LOW);
        running.setShowBadge(false);

        NotificationChannel ready = new NotificationChannel(CH_READY,
                c.getString(R.string.channel_ready), NotificationManager.IMPORTANCE_HIGH);
        ready.enableVibration(true);
        ready.setVibrationPattern(new long[]{0, 400, 200, 400, 200, 400});
        AudioAttributes audio = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        ready.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audio);

        nm.createNotificationChannels(Arrays.asList(running, ready));
    }

    static void showRunning(Context c, Tea tea, long endAt) {
        ensureChannels(c);
        Notification.Action cancel = new Notification.Action.Builder(
                Icon.createWithResource(c, R.drawable.ic_stop),
                c.getString(R.string.cancel),
                TimerController.cancelPI(c)).build();

        Notification n = new Notification.Builder(c, CH_RUNNING)
                .setSmallIcon(R.drawable.ic_stat_tea)
                .setColor(c.getColor(R.color.accent))
                .setContentTitle(c.getString(tea.nameRes))
                .setContentText(c.getString(R.string.steeping))
                .setWhen(endAt)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(TimerController.openPI(c))
                .addAction(cancel)
                .build();
        c.getSystemService(NotificationManager.class).notify(ID_RUNNING, n);
    }

    static void showReady(Context c, Tea tea) {
        ensureChannels(c);
        Notification n = new Notification.Builder(c, CH_READY)
                .setSmallIcon(R.drawable.ic_stat_tea)
                .setColor(c.getColor(R.color.accent))
                .setContentTitle(c.getString(R.string.ready_title))
                .setContentText(c.getString(R.string.ready_text, c.getString(tea.nameRes)))
                .setCategory(Notification.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(TimerController.openPI(c))
                .build();
        c.getSystemService(NotificationManager.class).notify(ID_READY, n);
    }

    static void cancelRunning(Context c) {
        c.getSystemService(NotificationManager.class).cancel(ID_RUNNING);
    }

    static void cancelReady(Context c) {
        c.getSystemService(NotificationManager.class).cancel(ID_READY);
    }
}
