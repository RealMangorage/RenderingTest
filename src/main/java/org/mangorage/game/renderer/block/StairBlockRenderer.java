 package org.mangorage.game.renderer.block;

import org.mangorage.game.block.Block;
import org.mangorage.game.renderer.chunk.DrawCommand;
import org.mangorage.game.core.Direction;

import java.util.EnumMap;
import java.util.List;

public final class StairBlockRenderer extends BlockRenderer {

    // Minecraft-style stair geometry (assuming stairs face NORTH by default)
    // Stairs consist of: bottom slab (full), top slab (half), and step face
    // All faces use counter-clockwise winding for outward-facing normals
    private static final float[][][] STAIR_SHAPE = new float[][][] {
            // 0 - BOTTOM face (full bottom) - facing DOWN
            {
                    {0, 0, 1}, {1, 0, 1}, {1, 0, 0},
                    {0, 0, 1}, {1, 0, 0}, {0, 0, 0}
            },
            // 1 - TOP faces (lower step top + upper step top) - facing UP
            {
                    // Lower step top (front half, at y=0.5)
                    {0, 0.5f, 0.5f}, {1, 0.5f, 0.5f}, {1, 0.5f, 0},
                    {0, 0.5f, 0.5f}, {1, 0.5f, 0}, {0, 0.5f, 0},
                    // Upper step top (back half, at y=1.0)
                    {0, 1.0f, 1.0f}, {1, 1.0f, 1.0f}, {1, 1.0f, 0.5f},
                    {0, 1.0f, 1.0f}, {1, 1.0f, 0.5f}, {0, 1.0f, 0.5f}
            },
            // 2 - NORTH face (front, only lower step) - facing NORTH (outward)
            {
                    {0, 0, 0}, {0, 0.5f, 0}, {1, 0.5f, 0},
                    {0, 0, 0}, {1, 0.5f, 0}, {1, 0, 0}
            },
            // 3 - SOUTH face (back, full height) - facing SOUTH (outward)
            {
                    {0, 0, 1}, {1, 0, 1}, {1, 1, 1},
                    {0, 0, 1}, {1, 1, 1}, {0, 1, 1}
            },
            // 4 - EAST face (right side, L-shaped) - facing EAST (outward)
            {
                    // Lower front section (0 to 0.5 in Z, 0 to 0.5 in Y)
                    {1, 0, 0}, {1, 0.5f, 0}, {1, 0.5f, 0.5f},
                    {1, 0, 0}, {1, 0.5f, 0.5f}, {1, 0, 0.5f},
                    // Upper back section (0.5 to 1 in Z, 0 to 1 in Y)
                    {1, 0, 0.5f}, {1, 1, 0.5f}, {1, 1, 1},
                    {1, 0, 0.5f}, {1, 1, 1}, {1, 0, 1}
            },
            // 5 - WEST face (left side, L-shaped) - facing WEST (outward)
            {
                    // Lower front section (0 to 0.5 in Z, 0 to 0.5 in Y)
                    {0, 0, 0.5f}, {0, 0.5f, 0.5f}, {0, 0.5f, 0},
                    {0, 0, 0.5f}, {0, 0.5f, 0}, {0, 0, 0},
                    // Upper back section (0.5 to 1 in Z, 0 to 1 in Y)
                    {0, 0, 1}, {0, 1, 1}, {0, 1, 0.5f},
                    {0, 0, 1}, {0, 1, 0.5f}, {0, 0, 0.5f}
            },
            // 6 - STEP face (vertical connection between steps) - facing SOUTH (toward back)
            {
                    {0, 0.5f, 0.5f}, {0, 1, 0.5f}, {1, 1, 0.5f},
                    {0, 0.5f, 0.5f}, {1, 1, 0.5f}, {1, 0.5f, 0.5f}
            }
    };

    // Number of vertices per face for proper texture coordinate assignment
    private static final int[] FACE_VERTEX_COUNTS = {6, 12, 6, 6, 12, 12, 6};

    @Override
    public void render(List<DrawCommand> drawCommands, List<Float> vertices, Block block, int x, int y, int z,
                       EnumMap<Direction, Block> neighbors, AssetLoader assetLoader) {

        for (int faceIndex = 0; faceIndex < STAIR_SHAPE.length; faceIndex++) {
            boolean shouldRender = true;

            if (!shouldRender) continue;

            int vertexStart = vertices.size() / 5;
            addStairFaceVertices(vertices, x, y, z, faceIndex);

            int addedVerts = (vertices.size() / 5) - vertexStart;
            if (addedVerts > 0) {
                Direction textureDirection = getTextureDirection(faceIndex);
                int texId = assetLoader.getOrCreateTexture(block.getBlockInfo().getTexture(textureDirection));
                float[] tint = block.getTint(textureDirection, 1);

                drawCommands.add(new DrawCommand(texId, vertexStart, addedVerts, tint, state -> {
                }));
            }
        }
    }

    private boolean shouldRenderFace(int faceIndex, EnumMap<Direction, Block> neighbors) {
        // Always render internal step face (index 6)
        if (faceIndex == 6) return true;

        // For main faces, check neighbors
        if (faceIndex < 6) {
            Direction dir = Direction.values()[faceIndex];
            Block neighbor = neighbors.get(dir);
            return (neighbor == null) || !neighbor.isSolid() || neighbor.isAir();
        }

        return true;
    }

    private Direction getTextureDirection(int faceIndex) {
        if (faceIndex < 6) {
            return Direction.values()[faceIndex];
        }
        // Step face uses UP texture
        return Direction.UP;
    }

    private void addStairFaceVertices(List<Float> vertices, int x, int y, int z, int faceIndex) {
        float[][] face = STAIR_SHAPE[faceIndex];
        int vertexCount = FACE_VERTEX_COUNTS[faceIndex];

        // Generate texture coordinates based on vertex count
        float[] texCoords = generateTexCoords(vertexCount);

        for (int i = 0; i < face.length; i++) {
            float vx = face[i][0] + x;
            float vy = face[i][1] + y;
            float vz = face[i][2] + z;

            vertices.add(vx);
            vertices.add(vy);
            vertices.add(vz);
            vertices.add(texCoords[i * 2]);
            vertices.add(texCoords[i * 2 + 1]);
        }
    }

    private float[] generateTexCoords(int vertexCount) {
        if (vertexCount == 6) {
            // Single quad (2 triangles)
            return new float[]{0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1};
        } else if (vertexCount == 12) {
            // Two quads (4 triangles)
            return new float[]{
                    0, 0, 1, 0, 1, 0.5f, 0, 0, 1, 0.5f, 0, 0.5f,  // First quad
                    0, 0.5f, 1, 0.5f, 1, 1, 0, 0.5f, 1, 1, 0, 1   // Second quad
            };
        }
        // Fallback
        return new float[]{0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1};
    }
}