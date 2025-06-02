package org.mangorage.game.world;

import org.joml.Matrix4f;
import org.mangorage.game.block.Block;
import org.mangorage.game.core.Blocks;
import org.mangorage.game.world.chunk.Chunk;

public final class World {
    private final Chunk chunk = new Chunk(64);

    public Chunk getChunk() {
        return chunk;
    }

    public void setBlock(Block block, BlockPos blockPos) { // Needs to be relative here...
        chunk.setBlock(block, blockPos);
    }

    public Block getBlock(BlockPos blockPos) {
        return chunk.getBlock(blockPos);
    }

    public void render(Matrix4f view, Matrix4f projection) {
        getChunk().render(view, projection);
    }

    public void init() {
        chunk.init();
        setBlock(Blocks.GRASS_BLOCK, new BlockPos(0, 0, 0));
        chunk.refreshChunkMesh();
    }
}
