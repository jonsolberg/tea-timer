package se.jonsolberg.teatimer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        String action = intent.getAction();
        if (TimerController.ACTION_FIRE.equals(action)) {
            if (new TimerStore(c).isRunning()) TimerController.finish(c, true);
        } else if (TimerController.ACTION_CANCEL.equals(action)) {
            TimerController.reset(c);
        }
    }
}
