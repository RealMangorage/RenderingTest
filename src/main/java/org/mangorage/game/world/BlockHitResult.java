package org.mangorage.game.world;

import org.mangorage.game.core.Direction;

public final class BlockHitResult {
    public final BlockPos pos;
    public final Direction face;

    public BlockHitResult(BlockPos pos, Direction face) {
        this.pos = pos;
        this.face = face;
    }

    public BlockPos getPos() {
        return pos;
    }

    public Direction getFace() {
        return face;
    }
}