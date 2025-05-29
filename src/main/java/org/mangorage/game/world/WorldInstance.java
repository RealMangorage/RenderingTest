package org.mangorage.game.world;

import org.joml.Matrix4f;
import org.mangorage.game.block.Block;
import org.mangorage.game.core.Blocks;
import org.mangorage.game.core.Direction;
import org.mangorage.game.renderer.CubeRenderer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class WorldInstance {
    private final CubeRenderer renderer = new CubeRenderer();
    private final int sX, sY, sZ;
    private final Block[][][] blocks;

    public WorldInstance(int sX, int sY, int sZ) {
        this.sX = sX;
        this.sY = sY;
        this.sZ = sZ;
        this.blocks = new Block[sX][sY][sZ];
    }

    boolean isValid(BlockPos blockPos) {
        return blockPos.x() < sX && blockPos.y() < sY && blockPos.z() < sZ && blockPos.x() >= 0 && blockPos.y() >= 0 && blockPos.z() >= 0;
    }

    public void setBlock(Block block, BlockPos blockPos) {
        if (!isValid(blockPos)) return;
        blocks[blockPos.x()][blockPos.y()][blockPos.z()] = block;
        renderer.buildMesh(blocks);
    }

    public Block getBlock(BlockPos blockPos) {
        if (!isValid(blockPos)) return null;
        return blocks[blockPos.x()][blockPos.y()][blockPos.z()];
    }

    public void removeBlock(BlockPos blockPos) {
        if (!isValid(blockPos)) return;
        blocks[blockPos.x()][blockPos.y()][blockPos.z()] = null;
    }

    public void render(Matrix4f view, Matrix4f projection) {
        renderer.render(new Matrix4f(), view, projection);
    }

    public void init() {
        Block diamond = Blocks.DIAMOND_BLOCK;
        Block grass = Blocks.GRASS_BLOCK;

        for (int x = 0; x < sX; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < sZ; z++) {
                    if (y == 1) {
                        blocks[x][y][z] = grass;
                    } else {
                        blocks[x][y][z] = diamond;
                    }
                }
            }
        }

        renderer.buildMesh(blocks);
    }
}
