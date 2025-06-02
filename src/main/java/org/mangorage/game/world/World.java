package org.mangorage.game.world;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.mangorage.game.block.Block;
import org.mangorage.game.core.Blocks;
import org.mangorage.game.world.chunk.Chunk;
import org.mangorage.game.world.chunk.ChunkPos;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class World {
    private static final int RENDER_DISTANCE = 2;

    private final Map<ChunkPos, Chunk> chunks = new ConcurrentHashMap<>();

    public Chunk getChunk(ChunkPos chunkPos) {
        return chunks.computeIfAbsent(chunkPos, this::generateChunk);
    }

    public Chunk getChunk(BlockPos blockPos) {
        int chunkX = Math.floorDiv(blockPos.x(), 16);
        int chunkZ = Math.floorDiv(blockPos.z(), 16);
        return getChunk(new ChunkPos(chunkX, chunkZ));
    }

    public void setBlock(Block block, BlockPos blockPos, BlockAction blockAction) {
        Chunk chunk = getChunk(blockPos);
        if (chunk == null) return;
        BlockPos localPos = new BlockPos(
                Math.floorMod(blockPos.x(), 16),
                blockPos.y(),
                Math.floorMod(blockPos.z(), 16)
        );
        chunk.setBlock(block, localPos, blockAction);
    }

    public Block getBlock(BlockPos blockPos) {
        Chunk chunk = getChunk(blockPos);
        if (chunk == null) return Blocks.AIR_BLOCK;
        BlockPos localPos = new BlockPos(
                Math.floorMod(blockPos.x(), 16),
                blockPos.y(),
                Math.floorMod(blockPos.z(), 16)
        );
        return chunk.getBlock(localPos);
    }

    public void render(Vector3f cameraPos, Matrix4f view, Matrix4f projection) {
        int cameraChunkX = Math.floorDiv((int) cameraPos.x, 16);
        int cameraChunkZ = Math.floorDiv((int) cameraPos.z, 16);

        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                int chunkX = cameraChunkX + dx;
                int chunkZ = cameraChunkZ + dz;
                ChunkPos pos = new ChunkPos(chunkX, chunkZ);

                Chunk chunk = getChunk(pos);
                if (chunk == null) continue;

                Matrix4f model = new Matrix4f()
                        .translate(chunkX * 16.0f, 0.0f, chunkZ * 16.0f);

                chunk.render(model, view, projection);
            }
        }
    }

    public Chunk generateChunk(ChunkPos chunkPos) {
        Chunk chunk = new Chunk(255); // Chunk Height

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 16; y++) {
                    Block block = (x == 0 || x == 15 || z == 0 || z == 15)
                            ? Blocks.DIAMOND_BLOCK     // Edge block
                            : Blocks.GRASS_BLOCK; // Inner block

                    BlockPos blockPos = new BlockPos(x, y, z);
                    chunk.setBlock(block, blockPos, BlockAction.NONE);
                }
            }
        }

        chunk.updateMesh();

        return chunk;
    }
}