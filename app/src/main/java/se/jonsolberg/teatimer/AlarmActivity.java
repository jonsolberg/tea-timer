package se.jonsolberg.teatimer;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.TextView;

/** Full-screen "Tea is ready" screen, shown over the lock screen while the alarm rings. */
public class AlarmActivity extends Activity {
    static final String ACTION_CLOSE = "se.jonsolberg.teatimer.CLOSE_ALARM";

    private final BroadcastReceiver closer = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_alarm);

        Tea tea = new TimerStore(this).tea();
        ((TextView) findViewById(R.id.alarmTea)).setText(
                getString(R.string.ready_text, getString(tea.nameRes)));

        ScaleAnimation pulse = new ScaleAnimation(1f, 1.12f, 1f, 1.12f,
                Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, .5f);
        pulse.setDuration(600);
        pulse.setRepeatMode(Animation.REVERSE);
        pulse.setRepeatCount(Animation.INFINITE);
        findViewById(R.id.alarmIcon).startAnimation(pulse);

        findViewById(R.id.alarmStop).setOnClickListener(v -> stopAndClose(false));
        findViewById(R.id.alarmOpen).setOnClickListener(v -> stopAndClose(true));

        registerReceiver(closer, new IntentFilter(ACTION_CLOSE), RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!AlarmService.isRinging()) finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Volume buttons silence the alarm, like on an alarm clock.
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            stopAndClose(false);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void stopAndClose(boolean openApp) {
        AlarmService.stop(this);
        if (openApp) {
            KeyguardManager km = getSystemService(KeyguardManager.class);
            if (km != null && km.isKeyguardLocked()) {
                km.requestDismissKeyguard(this, null);
            }
            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(closer);
        super.onDestroy();
    }
}
