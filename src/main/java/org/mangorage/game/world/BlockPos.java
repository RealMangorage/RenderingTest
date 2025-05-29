package org.mangorage.game.world;

import org.joml.Vector3f;

public record BlockPos(int x, int y, int z) {
    public Vector3f toVector3f() {
        return new Vector3f(x, y, z);
    }
}
