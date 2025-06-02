package org.mangorage.game.world.chunk;

import org.joml.Matrix4f;
import org.mangorage.game.block.Block;
import org.mangorage.game.core.Blocks;
import org.mangorage.game.renderer.chunk.ChunkMesh;
import org.mangorage.game.renderer.chunk.ChunkRenderer;
import org.mangorage.game.util.supplier.InitializableSupplier;
import org.mangorage.game.world.BlockAction;
import org.mangorage.game.world.BlockPos;

import java.util.Arrays;

public final class Chunk {
    private final int sX, sY, sZ;
    private final Block[][][] blocks;
    private volatile ChunkMesh chunkMesh = null;

    public Chunk(final int sY) {
        this.sX = 16;
        this.sY = sY;
        this.sZ = 16;
        this.blocks = new Block[sX][sY][sZ];
        for (Block[][] block : blocks) {
            for (Block[] blocks : block) {
                Arrays.fill(blocks, Blocks.AIR_BLOCK);
            }
        }
    }

    public boolean isValid(BlockPos blockPos) {
        return blockPos.x() < sX && blockPos.y() < sY && blockPos.z() < sZ && blockPos.x() >= 0 && blockPos.y() >= 0 && blockPos.z() >= 0;
    }

    public void setBlock(Block block, BlockPos blockPos, BlockAction blockAction) { // Needs to be relative here...
        if (!isValid(blockPos)) return;
        blocks[blockPos.x()][blockPos.y()][blockPos.z()] = block == null ? Blocks.AIR_BLOCK : block;
        if (blockAction == BlockAction.UPDATE) {
            updateMesh();
        }
    }

    public void updateMesh() {
        this.chunkMesh = ChunkRenderer.get().buildMesh(blocks);
        var a = 1;
    }

    public Block getBlock(BlockPos blockPos) {
        if (!isValid(blockPos)) return Blocks.AIR_BLOCK;
        return blocks[blockPos.x()][blockPos.y()][blockPos.z()];
    }

    public void render(Matrix4f model, Matrix4f view, Matrix4f projection) {
        // ... Should never be null... but we check anyways...
        if (chunkMesh == null) return; // Cant render, we don't have a mesh yet!
        ChunkRenderer.get().render(chunkMesh, model, view, projection);
    }
}
