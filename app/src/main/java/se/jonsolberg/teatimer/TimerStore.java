package se.jonsolberg.teatimer;

import android.content.Context;
import android.content.SharedPreferences;

/** Timer state, kept in SharedPreferences so it survives the app being closed. */
final class TimerStore {
    private final SharedPreferences p;

    TimerStore(Context c) {
        p = c.getSharedPreferences("timer", Context.MODE_PRIVATE);
    }

    String teaId() {
        return p.getString("tea", "black");
    }

    Tea tea() {
        return Tea.byId(teaId());
    }

    long durationMs() {
        return p.getLong("duration", secondsFor(tea()) * 1000L);
    }

    boolean isRunning() {
        return p.getBoolean("running", false);
    }

    boolean isPaused() {
        return p.getBoolean("paused", false);
    }

    long endAt() {
        return p.getLong("endAt", 0);
    }

    long finishedAt() {
        return p.getLong("finishedAt", 0);
    }

    long pausedRemaining() {
        return p.getLong("remaining", durationMs());
    }

    long remaining(long now) {
        return isRunning() ? Math.max(0, endAt() - now) : pausedRemaining();
    }

    // Per-tea steep time (user override or default)

    int secondsFor(Tea t) {
        return p.getInt("custom_" + t.id, t.defaultSeconds);
    }

    boolean hasCustom(Tea t) {
        return p.contains("custom_" + t.id);
    }

    void setCustomSeconds(Tea t, int seconds) {
        p.edit().putInt("custom_" + t.id, seconds).apply();
    }

    void clearCustom(Tea t) {
        p.edit().remove("custom_" + t.id).apply();
    }

    // State transitions

    void select(Tea t) {
        long d = secondsFor(t) * 1000L;
        p.edit().putString("tea", t.id).putLong("duration", d).putLong("remaining", d)
                .putBoolean("running", false).putBoolean("paused", false)
                .putLong("finishedAt", 0).apply();
    }

    void setRunning(long endAt) {
        p.edit().putBoolean("running", true).putBoolean("paused", false)
                .putLong("endAt", endAt).putLong("finishedAt", 0).apply();
    }

    void setPaused(long remaining) {
        p.edit().putBoolean("running", false).putBoolean("paused", true)
                .putLong("remaining", remaining).apply();
    }

    void setIdleRemaining(long remaining) {
        p.edit().putBoolean("running", false).putBoolean("paused", false)
                .putLong("remaining", remaining).putLong("finishedAt", 0).apply();
    }

    void reset() {
        p.edit().putBoolean("running", false).putBoolean("paused", false)
                .putLong("remaining", durationMs()).putLong("finishedAt", 0).apply();
    }

    void finished(long now) {
        p.edit().putBoolean("running", false).putBoolean("paused", false)
                .putLong("remaining", durationMs()).putLong("finishedAt", now).apply();
    }
}
