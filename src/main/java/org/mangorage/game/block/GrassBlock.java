package org.mangorage.game.block;

import org.mangorage.game.core.Direction;
import org.mangorage.game.util.RenderUtil;

public final class GrassBlock extends Block {

    private static final float[] TINT_TOP = new float[]{0.0f, 1.0f, 0.0f};

    public GrassBlock(String name) {
        super(name);
    }

    @Override
    public float[] getTint(Direction face, int layer) {
        if (face != Direction.DOWN && layer != 1) {
            return RenderUtil.adjustForBrightness(TINT_TOP, face);
        } else if (face == Direction.UP) {
            return RenderUtil.adjustForBrightness(TINT_TOP, face);
        }
        return super.getTint(face, layer);
    }
}
