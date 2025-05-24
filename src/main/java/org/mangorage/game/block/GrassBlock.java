package org.mangorage.game.block;

import org.mangorage.game.core.Direction;

public final class GrassBlock extends Block {

    private static final float[] TINT_TOP = new float[]{0.0f, 1.0f, 0.0f};

    public GrassBlock(String name) {
        super(name);
    }

    @Override
    public float[] getTint(Direction face) {
        if (face == Direction.UP) {
            return TINT_TOP;
        }
        return null;
    }
}
