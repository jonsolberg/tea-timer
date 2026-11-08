package se.jonsolberg.teatimer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.RingtoneManager;

import java.util.Arrays;

final class Notifications {
    private static final String CH_RUNNING = "running";
    /** Fallback alert with its own sound, used only if the alarm service can't start. */
    private static final String CH_READY = "ready";
    /** The alarm: silent channel, because AlarmService plays the sound and vibration itself. */
    private static final String CH_ALARM = "alarm";
    private static final int ID_RUNNING = 1;
    static final int ID_READY = 2;

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
        ready.setVibrationPattern(new long[]{0, 600, 400, 600, 400, 600});
        AudioAttributes audio = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        ready.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), audio);

        NotificationChannel alarm = new NotificationChannel(CH_ALARM,
                c.getString(R.string.channel_alarm), NotificationManager.IMPORTANCE_HIGH);
        alarm.setSound(null, null);
        alarm.enableVibration(false);
        alarm.setBypassDnd(true);
        alarm.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        nm.createNotificationChannels(Arrays.asList(running, ready, alarm));
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

    /** The ringing alarm's notification, with a full-screen "Tea is ready" screen. */
    static Notification buildAlarm(Context c, Tea tea) {
        ensureChannels(c);
        Intent full = new Intent(c, AlarmActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        PendingIntent fullPI = PendingIntent.getActivity(c, 3, full,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stopPI = PendingIntent.getService(c, 4,
                new Intent(c, AlarmService.class).setAction(AlarmService.ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Action stop = new Notification.Action.Builder(
                Icon.createWithResource(c, R.drawable.ic_stop),
                c.getString(R.string.stop_alarm), stopPI).build();

        return new Notification.Builder(c, CH_ALARM)
                .setSmallIcon(R.drawable.ic_stat_tea)
                .setColor(c.getColor(R.color.accent))
                .setColorized(true)
                .setContentTitle(c.getString(R.string.ready_title))
                .setContentText(c.getString(R.string.ready_text, c.getString(tea.nameRes)))
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setFullScreenIntent(fullPI, true)
                .setContentIntent(fullPI)
                .setDeleteIntent(stopPI)
                .addAction(stop)
                .build();
    }

    /** Loud fallback if the alarm service can't be started: repeats until opened or dismissed. */
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
        n.flags |= Notification.FLAG_INSISTENT;
        c.getSystemService(NotificationManager.class).notify(ID_READY, n);
    }

    /** Silent reminder left behind when the alarm rang out without being stopped. */
    static void showReadyQuiet(Context c, Tea tea) {
        ensureChannels(c);
        Notification n = new Notification.Builder(c, CH_ALARM)
                .setSmallIcon(R.drawable.ic_stat_tea)
                .setColor(c.getColor(R.color.accent))
                .setContentTitle(c.getString(R.string.ready_title))
                .setContentText(c.getString(R.string.ready_text, c.getString(tea.nameRes)))
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
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
