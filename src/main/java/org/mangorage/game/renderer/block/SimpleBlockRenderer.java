package org.mangorage.game.renderer.block;

import org.mangorage.game.block.Block;
import org.mangorage.game.core.BuiltInRegistries;
import org.mangorage.game.core.Direction;
import org.mangorage.game.renderer.chunk.DrawCommand;

import java.util.EnumMap;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.mangorage.game.util.RenderUtil.rotateUVs;

public final class SimpleBlockRenderer extends BlockRenderer {

    @Override
    public void render(List<DrawCommand> drawCommands, List<Float> vertices, Block block, int x, int y, int z, EnumMap<Direction, Block> neighbors, AssetLoader assetLoader) {
        for (Direction dir : Direction.values()) {
            Block neighborBlock = neighbors.get(dir);
            boolean shouldRenderFace = neighborBlock == null || !neighborBlock.isSolid() || neighborBlock.isAir();

            if (shouldRenderFace) {
                int vertexStart = vertices.size() / 5;
                addFaceVertices(vertices, x, y, z, block, dir); // Assuming rendering at origin

                int addedVerts = (vertices.size() / 5) - vertexStart;
                if (addedVerts > 0) {
                    if (block != BuiltInRegistries.GRASS_BLOCK) {
                        int texId = assetLoader.getOrCreateTexture(block.getBlockInfo().getTexture(dir));
                        float[] tint = block.getTint(dir, 1);
                        drawCommands.add(new DrawCommand(texId, vertexStart, addedVerts, tint, state -> {}));
                    } else {
                        // Grass block special sauce
                        float[] tint = block.getTint(dir, 1);
                        int texId = assetLoader.getOrCreateTexture(block.getBlockInfo().getTexture(dir));
                        drawCommands.add(new DrawCommand(texId, vertexStart, addedVerts, tint, state -> {}));

                        // Overlay for side grass
                        if (dir != Direction.UP && dir != Direction.DOWN) {
                            int texOverlay = assetLoader.getOrCreateTexture("assets/textures/blocks/grass_block_side_overlay.png");
                            float[] overlayTint = block.getTint(dir, 2);
                            drawCommands.add(new DrawCommand(texOverlay, vertexStart, addedVerts, overlayTint, state -> {
                                if (state) {
                                    glEnable(GL_BLEND);
                                    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                                    glEnable(GL_POLYGON_OFFSET_FILL);
                                    glPolygonOffset(-0.02f, 0f);
                                    glDepthMask(false);
                                } else {
                                    glPolygonOffset(0f, 0f);
                                    glDepthMask(true);
                                    glDisable(GL_POLYGON_OFFSET_FILL);
                                    glDisable(GL_BLEND);
                                }
                            }));
                        }
                    }
                }
            }
        }
    }

    private void addFaceVertices(List<Float> vertices, int x, int y, int z, Block blockInstance, Direction dir) {
        float[][] face = blockInstance.getShape()[dir.ordinal()];
        float[] texCoords = new float[] { 0,0, 1,0, 1,1, 0,0, 1,1, 0,1 };

        if (dir == Direction.EAST || dir == Direction.NORTH) {
            texCoords = rotateUVs(texCoords, 270);
        }

        for (int i = 0; i < face.length; i++) {
            vertices.add(face[i][0] + x);
            vertices.add(face[i][1] + y);
            vertices.add(face[i][2] + z);
            vertices.add(texCoords[i * 2]);
            vertices.add(texCoords[i * 2 + 1]);
        }
    }
}
