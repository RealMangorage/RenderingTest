package org.mangorage.game.renderer.block;

import org.mangorage.game.block.Block;
import org.mangorage.game.core.Direction;
import org.mangorage.game.renderer.chunk.DrawCommand;

import java.util.EnumMap;
import java.util.List;

import static org.mangorage.game.util.RenderUtil.rotateUVs;

public final class SlabBlockRenderer extends BlockRenderer {

    @Override
    public void render(List<DrawCommand> drawCommands, List<Float> vertices, Block block, int x, int y, int z, EnumMap<Direction, Block> neighbors, AssetLoader assetLoader) {
        for (Direction dir : Direction.values()) {
            // Only render relevant slab faces
            if (dir == Direction.UP || dir == Direction.DOWN || dir == Direction.NORTH ||
                    dir == Direction.SOUTH || dir == Direction.EAST || dir == Direction.WEST) {

                Block neighborBlock = neighbors.get(dir);
                boolean shouldRenderFace = neighborBlock == null || !neighborBlock.isSolid() || neighborBlock.isAir() || dir == Direction.UP;

                if (shouldRenderFace) {
                    int vertexStart = vertices.size() / 5;
                    addSlabFaceVertices(vertices, x, y, z, block, dir);

                    int addedVerts = (vertices.size() / 5) - vertexStart;
                    if (addedVerts > 0) {
                        int texId = assetLoader.getOrCreateTexture(block.getBlockInfo().getTexture(dir));
                        float[] tint = block.getTint(dir, 1);
                        drawCommands.add(new DrawCommand(texId, vertexStart, addedVerts, tint, state -> {}));
                    }
                }
            }
        }
    }

    private void addSlabFaceVertices(List<Float> vertices, int x, int y, int z, Block blockInstance, Direction dir) {
        float[][] fullFace = blockInstance.getShape()[dir.ordinal()];
        float[] texCoords = new float[] { 0,0, 1,0, 1,1, 0,0, 1,1, 0,1 };

        if (dir == Direction.EAST || dir == Direction.NORTH) {
            texCoords = rotateUVs(texCoords, 270);
        }

        for (int i = 0; i < fullFace.length; i++) {
            float vx = fullFace[i][0] + x;
            float vy = fullFace[i][1] + y;
            float vz = fullFace[i][2] + z;

            // Clamp Y between 0 and 0.5 for bottom slab
            vy = Math.min(0.5f + y, vy);

            vertices.add(vx);
            vertices.add(vy);
            vertices.add(vz);
            vertices.add(texCoords[i * 2]);
            vertices.add(texCoords[i * 2 + 1]);
        }
    }
}

