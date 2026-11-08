package se.jonsolberg.teatimer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Starts, pauses and stops the timer. The finish is delivered by an exact alarm,
 * so the tea alert arrives even if the app is closed or the phone is asleep.
 */
final class TimerController {
    static final String ACTION_FIRE = "se.jonsolberg.teatimer.FIRE";
    static final String ACTION_CANCEL = "se.jonsolberg.teatimer.CANCEL";

    private TimerController() {
    }

    static void select(Context c, Tea t) {
        cancelAlarm(c);
        AlarmService.stop(c);
        Notifications.cancelRunning(c);
        Notifications.cancelReady(c);
        new TimerStore(c).select(t);
    }

    static void start(Context c) {
        TimerStore s = new TimerStore(c);
        if (s.isRunning()) return;
        long rem = s.pausedRemaining();
        if (rem <= 0 || s.finishedAt() > 0) rem = s.durationMs();
        long end = System.currentTimeMillis() + rem;
        s.setRunning(end);
        schedule(c, end);
        AlarmService.stop(c);
        Notifications.cancelReady(c);
        Notifications.showRunning(c, s.tea(), end);
    }

    static void pause(Context c) {
        TimerStore s = new TimerStore(c);
        if (!s.isRunning()) return;
        long rem = Math.max(0, s.endAt() - System.currentTimeMillis());
        cancelAlarm(c);
        s.setPaused(rem);
        Notifications.cancelRunning(c);
    }

    static void reset(Context c) {
        cancelAlarm(c);
        AlarmService.stop(c);
        new TimerStore(c).reset();
        Notifications.cancelRunning(c);
        Notifications.cancelReady(c);
    }

    /** Add or remove time from the current brew (doesn't change the saved steep time). */
    static void adjust(Context c, long deltaMs) {
        TimerStore s = new TimerStore(c);
        long now = System.currentTimeMillis();
        if (s.isRunning()) {
            long end = Math.max(now + 1000, s.endAt() + deltaMs);
            s.setRunning(end);
            schedule(c, end);
            Notifications.showRunning(c, s.tea(), end);
        } else {
            long base = s.finishedAt() > 0 ? s.durationMs() : s.pausedRemaining();
            long rem = Math.max(5000, base + deltaMs);
            if (s.isPaused()) {
                s.setPaused(rem);
            } else {
                s.setIdleRemaining(rem);
            }
            Notifications.cancelReady(c);
        }
    }

    static void finish(Context c, boolean notify) {
        TimerStore s = new TimerStore(c);
        s.finished(System.currentTimeMillis());
        Notifications.cancelRunning(c);
        if (!notify) return;
        try {
            AlarmService.ring(c);
        } catch (RuntimeException e) {
            // Android refused to start the alarm service; fall back to a loud notification.
            Notifications.showReady(c, s.tea());
        }
    }

    private static void schedule(Context c, long end) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        PendingIntent pi = firePI(c);
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, pi);
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, pi);
        }
    }

    private static void cancelAlarm(Context c) {
        c.getSystemService(AlarmManager.class).cancel(firePI(c));
    }

    private static PendingIntent firePI(Context c) {
        Intent i = new Intent(c, AlarmReceiver.class).setAction(ACTION_FIRE);
        return PendingIntent.getBroadcast(c, 1, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static PendingIntent cancelPI(Context c) {
        Intent i = new Intent(c, AlarmReceiver.class).setAction(ACTION_CANCEL);
        return PendingIntent.getBroadcast(c, 2, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static PendingIntent openPI(Context c) {
        Intent i = new Intent(c, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(c, 0, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
