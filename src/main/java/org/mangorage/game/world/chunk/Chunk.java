package org.mangorage.game.world.chunk;

import org.joml.Matrix4f;
import org.mangorage.game.block.Block;
import org.mangorage.game.core.BuiltInRegistries;
import org.mangorage.game.renderer.chunk.ChunkMesh;
import org.mangorage.game.renderer.chunk.ChunkRenderer;
import org.mangorage.game.world.BlockAction;
import org.mangorage.game.world.BlockPos;
import org.mangorage.game.world.World;

import java.util.Arrays;

public final class Chunk {
    private final int sY;
    private final int[][][] blocks;
    private final World world;
    private final ChunkPos chunkPos;

    private volatile boolean dirty = true;
    private volatile ChunkMesh chunkMesh = null;



    public Chunk(final int sY, World world, ChunkPos chunkPos) {
        this.sY = sY;
        this.world = world;
        this.chunkPos = chunkPos;

        this.blocks = new int[16][sY][16];
        for (int[][] block : blocks) {
            for (int[] blocks : block) {
                Arrays.fill(blocks, BuiltInRegistries.BLOCK_REGISTRY.getInternalId(BuiltInRegistries.AIR_BLOCK));
            }
        }
    }

    public boolean isValid(BlockPos blockPos) {
        return blockPos.x() < 16 && blockPos.y() < sY && blockPos.z() < 16 && blockPos.x() >= 0 && blockPos.y() >= 0 && blockPos.z() >= 0;
    }

    public void setBlock(Block block, BlockPos blockPos, BlockAction blockAction) { // Needs to be relative here...
        if (!isValid(blockPos)) return;
        blocks[blockPos.x()][blockPos.y()][blockPos.z()] = block == null ? BuiltInRegistries.BLOCK_REGISTRY.getDefaultInternalId() : BuiltInRegistries.BLOCK_REGISTRY.getInternalId(block);
        if (blockAction == BlockAction.UPDATE) {
            updateMesh();
        }
        dirty = true;
    }

    public void updateMesh() {
        if (!dirty) return;
        dirty = false;
        final var oldMesh = chunkMesh;
        this.chunkMesh = null;
        this.chunkMesh = ChunkRenderer.get().buildMesh(world, chunkPos, blocks);
        if (oldMesh != null) oldMesh.dispose();
    }

    public void dispose() {
        if (this.chunkMesh != null) {
            this.chunkMesh.dispose();
        }
    }

    public Block getBlock(BlockPos blockPos) {
        if (!isValid(blockPos)) return BuiltInRegistries.AIR_BLOCK;
        return BuiltInRegistries.BLOCK_REGISTRY.getByInternalId(blocks[blockPos.x()][blockPos.y()][blockPos.z()]);
    }

    public int[][][] getSaveData() {
        return blocks;
    }

    public void render(Matrix4f model, Matrix4f view, Matrix4f projection) {
        // ... Should never be null... but we check anyways...
        if (chunkMesh == null) return; // Cant render, we don't have a mesh yet!
        ChunkRenderer.get().render(chunkMesh, model, view, projection);
    }

    public int getHeight() {
        return sY;
    }

    public ChunkPos getChunkPos() {
        return chunkPos;
    }
}
