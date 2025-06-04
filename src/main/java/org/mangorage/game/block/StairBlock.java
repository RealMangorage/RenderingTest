package org.mangorage.game.block;

import org.mangorage.game.renderer.block.BlockRenderer;
import org.mangorage.game.renderer.block.StairBlockRenderer;

public final class StairBlock extends Block{
    public static final BlockRenderer stairBlockRenderer = new StairBlockRenderer();
    // Stair shape with 7 faces: bottom, top (sloped), north, south (step front), east, west, step vertical face
    // Each face is 6 vertices (2 triangles), each vertex is {x, y, z}

    private static final float[][] STAIR_OUTLINE = new float[][] {
            // Bottom face edges (full bottom)
            {0, 0, 0}, {1, 0, 0},  // Bottom back edge
            {1, 0, 0}, {1, 0, 1},  // Bottom right edge
            {1, 0, 1}, {0, 0, 1},  // Bottom front edge
            {0, 0, 1}, {0, 0, 0},  // Bottom left edge

            // Lower step top face edges (back half at height 0.5)
            {0, 0.5f, 0}, {1, 0.5f, 0},      // Lower step back edge
            {1, 0.5f, 0}, {1, 0.5f, 0.5f},   // Lower step right edge
            {1, 0.5f, 0.5f}, {0, 0.5f, 0.5f}, // Lower step front edge
            {0, 0.5f, 0.5f}, {0, 0.5f, 0},   // Lower step left edge

            // Upper step top face edges (front half at height 1.0)
            {0, 1, 0.5f}, {1, 1, 0.5f},      // Upper step back edge
            {1, 1, 0.5f}, {1, 1, 1},         // Upper step right edge
            {1, 1, 1}, {0, 1, 1},            // Upper step front edge
            {0, 1, 1}, {0, 1, 0.5f},         // Upper step left edge

            // Vertical edges - back corners
            {0, 0, 0}, {0, 0.5f, 0},         // Back left vertical (bottom to lower step)
            {1, 0, 0}, {1, 0.5f, 0},         // Back right vertical (bottom to lower step)

            // Vertical edges - middle transition
            {0, 0.5f, 0.5f}, {0, 1, 0.5f},   // Mid left vertical (lower to upper step)
            {1, 0.5f, 0.5f}, {1, 1, 0.5f},   // Mid right vertical (lower to upper step)

            // Vertical edges - front corners (from bottom to upper step)
            {0, 0, 1}, {0, 1, 1},            // Front left vertical (bottom to top)
            {1, 0, 1}, {1, 1, 1},            // Front right vertical (bottom to top)
    };

    @Override
    public BlockRenderer getRenderer() {
        return stairBlockRenderer;
    }

    @Override
    public float[][] getOutline() {
        return STAIR_OUTLINE;
    }

    @Override
    public boolean isSolid() {
        return false;
    }
}
