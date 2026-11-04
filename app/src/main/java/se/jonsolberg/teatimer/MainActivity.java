package se.jonsolberg.teatimer;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {
    private TimerStore store;
    private TextView teaName, teaTemp, timeText, status;
    private Button startPause, resetButton;
    private LinearLayout teaList;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            render();
            handler.postDelayed(this, 200);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        store = new TimerStore(this);
        Notifications.ensureChannels(this);

        teaName = findViewById(R.id.teaName);
        teaTemp = findViewById(R.id.teaTemp);
        timeText = findViewById(R.id.time);
        status = findViewById(R.id.status);
        startPause = findViewById(R.id.startPause);
        resetButton = findViewById(R.id.reset);
        teaList = findViewById(R.id.teaList);

        startPause.setOnClickListener(v -> {
            if (store.isRunning()) {
                TimerController.pause(this);
            } else {
                TimerController.start(this);
            }
            render();
        });
        findViewById(R.id.minus).setOnClickListener(v -> {
            TimerController.adjust(this, -30_000);
            render();
        });
        findViewById(R.id.plus).setOnClickListener(v -> {
            TimerController.adjust(this, 30_000);
            render();
        });
        resetButton.setOnClickListener(v -> {
            TimerController.reset(this);
            render();
        });

        askForNotificationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        buildTeaList();
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    private void askForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    private void render() {
        long now = System.currentTimeMillis();
        Tea tea = store.tea();
        teaName.setText(tea.nameRes);
        teaTemp.setText(tea.temp.isEmpty() ? "" : getString(R.string.water_temp, tea.temp));
        teaTemp.setVisibility(tea.temp.isEmpty() ? View.GONE : View.VISIBLE);

        boolean running = store.isRunning();
        long rem = store.remaining(now);

        // Safety net in case the alarm was lost (e.g. after a reboot).
        if (running && rem == 0 && now - store.endAt() > 3000) {
            TimerController.finish(this, false);
            running = false;
        }

        boolean ready = !running && store.finishedAt() > 0;
        boolean paused = !running && store.isPaused();
        if (!running) rem = store.pausedRemaining();

        timeText.setText(format(ready ? 0 : rem));
        timeText.setTextColor(getColor(ready || (running && rem == 0) ? R.color.accent : R.color.text));

        if (running) {
            status.setText(rem == 0 ? R.string.ready_status : R.string.steeping);
        } else if (ready) {
            status.setText(R.string.ready_status);
        } else if (paused) {
            status.setText(R.string.paused);
        } else {
            status.setText("");
        }

        startPause.setText(running ? R.string.pause : paused ? R.string.resume : R.string.start);
        resetButton.setVisibility(running || paused || ready || rem != store.durationMs()
                ? View.VISIBLE : View.INVISIBLE);

        if (running) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void buildTeaList() {
        teaList.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        String selected = store.teaId();
        for (Tea t : Tea.ALL) {
            View row = inflater.inflate(R.layout.item_tea, teaList, false);
            ((TextView) row.findViewById(R.id.name)).setText(t.nameRes);
            ((TextView) row.findViewById(R.id.temp)).setText(t.temp);
            TextView time = row.findViewById(R.id.time);
            time.setText(format(store.secondsFor(t) * 1000L));
            if (store.hasCustom(t) && !t.isCustom()) time.setTextColor(getColor(R.color.accent));
            row.setBackgroundResource(t.id.equals(selected)
                    ? R.drawable.tea_item_selected : R.drawable.tea_item);
            row.setOnClickListener(v -> onTeaTapped(t));
            row.setOnLongClickListener(v -> {
                editTime(t);
                return true;
            });
            teaList.addView(row);
        }
    }

    private void onTeaTapped(Tea t) {
        if (store.isRunning()) {
            Toast.makeText(this, R.string.switch_blocked, Toast.LENGTH_SHORT).show();
            return;
        }
        if (t.isCustom()) {
            editTime(t);
            return;
        }
        TimerController.select(this, t);
        buildTeaList();
        render();
    }

    private void editTime(Tea t) {
        if (store.isRunning() && t.id.equals(store.teaId())) {
            Toast.makeText(this, R.string.switch_blocked, Toast.LENGTH_SHORT).show();
            return;
        }
        int current = store.secondsFor(t);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER);
        int pad = Math.round(16 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad, pad, 0);

        NumberPicker minutes = new NumberPicker(this);
        minutes.setMinValue(0);
        minutes.setMaxValue(30);
        minutes.setValue(Math.min(30, current / 60));

        String[] secLabels = new String[12];
        for (int i = 0; i < secLabels.length; i++) {
            secLabels[i] = String.format(Locale.ROOT, "%02d", i * 5);
        }
        NumberPicker seconds = new NumberPicker(this);
        seconds.setMinValue(0);
        seconds.setMaxValue(secLabels.length - 1);
        seconds.setDisplayedValues(secLabels);
        seconds.setValue((current % 60) / 5);

        TextView colon = new TextView(this);
        colon.setText(":");
        colon.setTextSize(28);
        colon.setPadding(pad, 0, pad, 0);

        box.addView(minutes);
        box.addView(colon);
        box.addView(seconds);

        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.set_time_for, getString(t.nameRes)))
                .setView(box)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    int secs = Math.max(5, minutes.getValue() * 60 + seconds.getValue() * 5);
                    store.setCustomSeconds(t, secs);
                    if (!store.isRunning() && (t.isCustom() || t.id.equals(store.teaId()))) {
                        TimerController.select(this, t);
                    }
                    buildTeaList();
                    render();
                })
                .setNegativeButton(android.R.string.cancel, null);

        if (!t.isCustom() && store.hasCustom(t)) {
            b.setNeutralButton(R.string.restore_default, (d, w) -> {
                store.clearCustom(t);
                if (!store.isRunning() && t.id.equals(store.teaId())) {
                    TimerController.select(this, t);
                }
                buildTeaList();
                render();
            });
        }
        b.show();
    }

    private static String format(long ms) {
        long s = (ms + 999) / 1000;
        return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }
}
