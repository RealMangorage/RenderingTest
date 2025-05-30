package org.mangorage.game.core;

import org.mangorage.game.util.Cooldown;

import java.util.ArrayList;
import java.util.List;

public final class KeybindRegistry {


    record RegisteredKeyBind(IKeyBind keyBind, Cooldown cooldown) {
        RegisteredKeyBind(IKeyBind keyBind) {
            this(keyBind, new Cooldown(250));
        }
    }

    private final List<RegisteredKeyBind> keyBinds = new ArrayList<>();

    public void register(IKeyBind keyBind, long duration) {
        keyBinds.add(new RegisteredKeyBind(keyBind, duration == -1 ? null : new Cooldown(duration)));
    }

    public void consume(int key, int scancode, int action, int mods) {
        for (RegisteredKeyBind keyBind : keyBinds) {
            if (keyBind.cooldown() != null && keyBind.cooldown().isActive()) continue;
            if (keyBind.keyBind().consume(key, scancode, action, mods)) {
                if (keyBind.cooldown() != null)
                    keyBind.cooldown().consume();
                break;
            }
        }
    }

}
