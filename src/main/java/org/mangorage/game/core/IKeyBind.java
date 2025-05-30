package org.mangorage.game.core;

@FunctionalInterface
public interface IKeyBind {
    boolean consume(int key, int scancode, int action, int mods);
}
