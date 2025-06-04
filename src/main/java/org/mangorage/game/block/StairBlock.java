package org.mangorage.game.block;

import org.mangorage.game.renderer.block.BlockRenderer;
import org.mangorage.game.renderer.block.StairBlockRenderer;

public final class StairBlock extends Block{
    public static final BlockRenderer stairBlockRenderer = new StairBlockRenderer();
    // Stair shape with 7 faces: bottom, top (sloped), north, south (step front), east, west, step vertical face
    // Each face is 6 vertices (2 triangles), each vertex is {x, y, z}
    private static final float[][][] STAIR_SHAPE = new float[][][] {
            // 0 - BOTTOM
            {
                    {0,0,0}, {1,0,0}, {1,0,1},
                    {0,0,0}, {1,0,1}, {0,0,1}
            },
            // 1 - TOP (sloped)
            {
                    {0,1,0}, {1,1,0}, {1,0.5f,0.5f},
                    {0,1,0}, {1,0.5f,0.5f}, {0,0.5f,0.5f}
            },
            // 2 - NORTH (back)
            {
                    {0,0,0}, {1,0,0}, {1,1,0},
                    {0,0,0}, {1,1,0}, {0,1,0}
            },
            // 3 - SOUTH (step front lower)
            {
                    {0,0,1}, {1,0,1}, {1,0.5f,0.5f},
                    {0,0,1}, {1,0.5f,0.5f}, {0,0.5f,0.5f}
            },
            // 4 - EAST (side step face)
            {
                    {1,0,0}, {1,0,1}, {1,0.5f,0.5f},
                    {1,0,0}, {1,0.5f,0.5f}, {1,1,0.5f}
            },
            // 5 - WEST (side step face)
            {
                    {0,0,0}, {0,0,1}, {0,0.5f,0.5f},
                    {0,0,0}, {0,0.5f,0.5f}, {0,1,0.5f}
            },
            // 6 - FRONT VERTICAL STEP (vertical face on step front)
            {
                    {0,0.5f,0.5f}, {1,0.5f,0.5f}, {1,1,0.5f},
                    {0,0.5f,0.5f}, {1,1,0.5f}, {0,1,0.5f}
            }
    };

    @Override
    public float[][][] getShape() {
        return STAIR_SHAPE;
    }

    @Override
    public BlockRenderer getRenderer() {
        return stairBlockRenderer;
    }

    @Override
    public boolean isSolid() {
        return false;
    }
}
