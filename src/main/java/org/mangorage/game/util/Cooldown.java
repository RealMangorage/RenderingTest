package org.mangorage.game.util;

public final class Cooldown {
    private final long duration;
    private long time = System.currentTimeMillis();


    public Cooldown(long duration) {
        this.duration = duration;
    }

    public boolean consume() {
        if (System.currentTimeMillis() - time > duration) {
            time = System.currentTimeMillis();
            return true;
        } else {
            return false;
        }
    }
}
