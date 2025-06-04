package org.mangorage.game.renderer.block;

import org.mangorage.game.block.Block;
import org.mangorage.game.core.Direction;
import org.mangorage.game.renderer.chunk.DrawCommand;

import java.util.EnumMap;
import java.util.List;

public final class StairBlockRenderer extends BlockRenderer {

    private static final float[][][] STAIR_SHAPE = new float[][][]{
            // BOTTOM
            {
                    {0, 0, 1}, {1, 0, 0}, {1, 0, 1},  // First triangle reversed
                    {0, 0, 1}, {0, 0, 0}, {1, 0, 0}   // Second triangle reversed
            },
            // TOP
            {
                    {0, 0.5f, 0.5f}, {1, 0.5f, 0.5f}, {1, 0.5f, 0},
                    {0, 0.5f, 0.5f}, {1, 0.5f, 0}, {0, 0.5f, 0},
                    {0, 1.0f, 1.0f}, {1, 1.0f, 1.0f}, {1, 1.0f, 0.5f},
                    {0, 1.0f, 1.0f}, {1, 1.0f, 0.5f}, {0, 1.0f, 0.5f}
            },
            // NORTH
            {
                    {0, 0, 0}, {0, 0.5f, 0}, {1, 0.5f, 0},
                    {0, 0, 0}, {1, 0.5f, 0}, {1, 0, 0}
            },
            // SOUTH
            {
                    {0, 0, 1}, {1, 0, 1}, {1, 1, 1},
                    {0, 0, 1}, {1, 1, 1}, {0, 1, 1}
            },
            // EAST
            {
                    {1, 0, 0}, {1, 0.5f, 0}, {1, 0.5f, 0.5f},
                    {1, 0, 0}, {1, 0.5f, 0.5f}, {1, 0, 0.5f},
                    {1, 0, 0.5f}, {1, 1, 0.5f}, {1, 1, 1},
                    {1, 0, 0.5f}, {1, 1, 1}, {1, 0, 1}
            },
            // WEST
            {
                    {0, 0, 0.5f}, {0, 0.5f, 0.5f}, {0, 0.5f, 0},
                    {0, 0, 0.5f}, {0, 0.5f, 0}, {0, 0, 0},
                    {0, 0, 1}, {0, 1, 1}, {0, 1, 0.5f},
                    {0, 0, 1}, {0, 1, 0.5f}, {0, 0, 0.5f}
            },
            // STEP
            {
                    {0, 0.5f, 0.5f}, {0, 1, 0.5f}, {1, 1, 0.5f},
                    {0, 0.5f, 0.5f}, {1, 1, 0.5f}, {1, 0.5f, 0.5f}
            }
    };

    private static final int[] VERT_COUNTS = {6, 12, 6, 6, 12, 12, 6};

    @Override
    public void render(List<DrawCommand> drawCommands, List<Float> vertices, Block block, int x, int y, int z, EnumMap<Direction, Block> neighbors, AssetLoader assetLoader) {
        for (int faceIndex = 0; faceIndex < STAIR_SHAPE.length; faceIndex++) {
            if (!shouldRenderFace(faceIndex, neighbors)) continue;

            int vertexStart = vertices.size() / 5;
            float[][] face = STAIR_SHAPE[faceIndex];
            float[] texCoords = generateUV(face, getTextureDirection(faceIndex));

            for (int i = 0; i < face.length; i++) {
                vertices.add(face[i][0] + x);
                vertices.add(face[i][1] + y);
                vertices.add(face[i][2] + z);
                vertices.add(texCoords[i * 2]);
                vertices.add(texCoords[i * 2 + 1]);
            }

            int addedVerts = (vertices.size() / 5) - vertexStart;
            if (addedVerts > 0) {
                int texId = assetLoader.getOrCreateTexture("assets/textures/blocks/stone_block.png");
                float[] tint = block.getTint(getTextureDirection(faceIndex), 1);
                // Brighten the RGB components
                tint[0] = Math.min(1.0f, tint[0] * 1.5f); // Red
                tint[1] = Math.min(1.0f, tint[1] * 1.5f); // Green
                tint[2] = Math.min(1.0f, tint[2] * 1.5f); // Blue
                drawCommands.add(new DrawCommand(texId, vertexStart, addedVerts, tint, s -> {}));
            }
        }
    }

    private boolean shouldRenderFace(int i, EnumMap<Direction, Block> n) {
        switch (i) {
            case 0: return n.get(Direction.DOWN) == null || !n.get(Direction.DOWN).isSolid();
            case 1: return n.get(Direction.UP) == null || !n.get(Direction.UP).isSolid();
            case 2: return n.get(Direction.NORTH) == null || !n.get(Direction.NORTH).isSolid();
            case 3: return n.get(Direction.SOUTH) == null || !n.get(Direction.SOUTH).isSolid();
            case 4: return n.get(Direction.EAST) == null || !n.get(Direction.EAST).isSolid();
            case 5: return n.get(Direction.WEST) == null || !n.get(Direction.WEST).isSolid();
            case 6: return n.get(Direction.NORTH) == null || !n.get(Direction.NORTH).isSolid();  // THIS IS THE FIX
            default: return true;
        }
    }

    private Direction getTextureDirection(int i) {
        return (i == 6) ? Direction.NORTH : (i < 6 ? Direction.values()[i] : Direction.UP);
    }


    private float[] generateUV(float[][] face, Direction dir) {
        int uAxis = 0, vAxis = 1;
        switch (dir) {
            case UP: case DOWN: uAxis = 0; vAxis = 2; break;
            case NORTH: case SOUTH: uAxis = 0; vAxis = 1; break;
            case EAST: case WEST: uAxis = 2; vAxis = 1; break;
        }

        float minU = Float.MAX_VALUE, maxU = Float.MIN_VALUE;
        float minV = Float.MAX_VALUE, maxV = Float.MIN_VALUE;

        for (float[] v : face) {
            minU = Math.min(minU, v[uAxis]);
            maxU = Math.max(maxU, v[uAxis]);
            minV = Math.min(minV, v[vAxis]);
            maxV = Math.max(maxV, v[vAxis]);
        }

        float dU = maxU - minU;
        float dV = maxV - minV;
        float[] tex = new float[face.length * 2];

        // Use actual face dimensions for UV mapping to prevent squishing
        for (int i = 0; i < face.length; i++) {
            tex[i * 2] = (face[i][uAxis] - minU); // Use actual size, not normalized
            tex[i * 2 + 1] = 1.0f - (face[i][vAxis] - minV); // Use actual size, not normalized
        }

        return tex;
    }
}