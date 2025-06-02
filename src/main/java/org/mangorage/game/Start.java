package org.mangorage.game;

public final class Start {
    public static void main(String[] args) {
        try {
            new Game().run();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
