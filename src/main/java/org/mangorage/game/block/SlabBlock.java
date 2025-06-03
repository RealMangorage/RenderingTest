package org.mangorage.game.block;

import org.mangorage.game.renderer.block.BlockRenderer;
import org.mangorage.game.renderer.block.SlabBlockRenderer;

public final class SlabBlock extends Block {
    private static final BlockRenderer slabBlockRenderer = new SlabBlockRenderer();
    private static final float[][][] HALF_SLAB_SHAPE = new float[][][] {
            // UP (Y=0.5)
            {
                    {0, 0.5f, 0}, {0, 0.5f, 1}, {1, 0.5f, 1},
                    {0, 0.5f, 0}, {1, 0.5f, 1}, {1, 0.5f, 0}
            },
            // DOWN (Y=0)
            {
                    {0, 0, 0}, {1, 0, 0}, {1, 0, 1},
                    {0, 0, 0}, {1, 0, 1}, {0, 0, 1}
            },
            // NORTH (Z=0)
            {
                    {0, 0, 0}, {0, 0.5f, 0}, {1, 0.5f, 0},
                    {0, 0, 0}, {1, 0.5f, 0}, {1, 0, 0}
            },
            // SOUTH (Z=1)
            {
                    {0, 0, 1}, {1, 0, 1}, {1, 0.5f, 1},
                    {0, 0, 1}, {1, 0.5f, 1}, {0, 0.5f, 1}
            },
            // WEST (X=0)
            {
                    {0, 0, 0}, {0, 0, 1}, {0, 0.5f, 1},
                    {0, 0, 0}, {0, 0.5f, 1}, {0, 0.5f, 0}
            },
            // EAST (X=1)
            {
                    {1, 0, 0}, {1, 0.5f, 0}, {1, 0.5f, 1},
                    {1, 0, 0}, {1, 0.5f, 1}, {1, 0, 1}
            },
    };


    @Override
    public float[][][] getShape() {
        return HALF_SLAB_SHAPE;
    }

    @Override
    public BlockRenderer getRenderer() {
        return slabBlockRenderer;
    }

    @Override
    public boolean isSolid() {
        return false;
    }
}
