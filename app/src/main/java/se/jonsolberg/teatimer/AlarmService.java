package se.jonsolberg.teatimer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

/**
 * Rings like an alarm clock when the tea is ready: loops the alarm sound on the
 * alarm volume (so it also sounds when the phone is on silent), vibrates and shows
 * a full-screen "Tea is ready" screen until stopped. Gives up after a few minutes.
 */
public class AlarmService extends Service {
    static final String ACTION_RING = "se.jonsolberg.teatimer.RING";
    static final String ACTION_STOP = "se.jonsolberg.teatimer.STOP";
    private static final long MAX_RING_MS = 3 * 60 * 1000;

    private static volatile boolean ringing;

    private MediaPlayer player;
    private Vibrator vibrator;
    private PowerManager.WakeLock wakeLock;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable timeout = () -> stopRinging(true);

    static boolean isRinging() {
        return ringing;
    }

    static void ring(Context c) {
        Intent i = new Intent(c, AlarmService.class).setAction(ACTION_RING);
        c.startForegroundService(i);
    }

    static void stop(Context c) {
        if (!ringing) return;
        c.startService(new Intent(c, AlarmService.class).setAction(ACTION_STOP));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_STOP.equals(action)) {
            stopRinging(false);
            return START_NOT_STICKY;
        }

        Tea tea = new TimerStore(this).tea();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(Notifications.ID_READY, Notifications.buildAlarm(this, tea),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(Notifications.ID_READY, Notifications.buildAlarm(this, tea));
        }
        if (!ringing) {
            ringing = true;
            acquireWakeLock();
            startSound();
            startVibration();
            handler.postDelayed(timeout, MAX_RING_MS);
        }
        return START_NOT_STICKY;
    }

    private void startSound() {
        Uri uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);
        if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        try {
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            player.setDataSource(this, uri);
            player.setLooping(true);
            player.prepare();
            player.start();
        } catch (Exception e) {
            if (player != null) player.release();
            player = null;
        }
    }

    private void startVibration() {
        if (Build.VERSION.SDK_INT >= 31) {
            vibrator = getSystemService(VibratorManager.class).getDefaultVibrator();
        } else {
            vibrator = getSystemService(Vibrator.class);
        }
        if (vibrator == null || !vibrator.hasVibrator()) return;
        long[] pattern = {0, 600, 400, 600, 400, 600, 1200};
        VibrationEffect effect = VibrationEffect.createWaveform(pattern, 0);
        if (Build.VERSION.SDK_INT >= 33) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM));
        } else {
            vibrateLegacy(effect);
        }
    }

    @SuppressWarnings("deprecation")
    private void vibrateLegacy(VibrationEffect effect) {
        vibrator.vibrate(effect, new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM).build());
    }

    private void acquireWakeLock() {
        PowerManager pm = getSystemService(PowerManager.class);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TeaTimer:alarm");
        wakeLock.acquire(MAX_RING_MS + 10_000);
    }

    /** @param timedOut true when nobody stopped it: leave a quiet "tea is ready" notice. */
    private void stopRinging(boolean timedOut) {
        handler.removeCallbacks(timeout);
        ringing = false;
        if (player != null) {
            try {
                player.stop();
            } catch (IllegalStateException ignored) {
            }
            player.release();
            player = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        wakeLock = null;
        stopForeground(STOP_FOREGROUND_REMOVE);
        if (timedOut) Notifications.showReadyQuiet(this, new TimerStore(this).tea());
        sendBroadcast(new Intent(AlarmActivity.ACTION_CLOSE).setPackage(getPackageName()));
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (ringing) stopRinging(false);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
