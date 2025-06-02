package org.mangorage.game.world;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.mangorage.game.block.Block;
import org.mangorage.game.core.Blocks;
import org.mangorage.game.world.chunk.Chunk;
import org.mangorage.game.world.chunk.ChunkPos;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class World {
    private final Map<ChunkPos, Chunk> chunks = new ConcurrentHashMap<>();

    public Chunk getChunk(ChunkPos chunkPos) {
        if (chunkPos.x() < 0 || chunkPos.z() < 0) return null;
        var chk = chunks.get(chunkPos);
        if (chk == null ) {
            try {
                chk = generateChunk(chunkPos);
            } catch (Throwable e) {
                e.printStackTrace();
            }
            chunks.put(chunkPos, chk);
            return chk;
        }
        return chk;
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

    public Block getBlock(BlockPos globalPos) {
        Chunk chunk = getChunk(globalPos);
        if (chunk == null) return Blocks.AIR_BLOCK;
        BlockPos localPos = new BlockPos(
                Math.floorMod(globalPos.x(), 16),
                globalPos.y(),
                Math.floorMod(globalPos.z(), 16)
        );
        return chunk.getBlock(localPos);
    }

    public void render(Vector3f cameraPos, Matrix4f view, Matrix4f projection) {
        int cameraChunkX = Math.floorDiv((int) cameraPos.x, 16);
        int cameraChunkZ = Math.floorDiv((int) cameraPos.z, 16);

        for (int dx = -2; dx <= 1; dx++) {
            for (int dz = -2; dz <= 1; dz++) {
                int chunkX = cameraChunkX + dx;
                int chunkZ = cameraChunkZ + dz;
                ChunkPos pos = new ChunkPos(chunkX, chunkZ);

                Chunk chunk = chunks.get(pos);
                if (chunk == null) continue; // Skip empty air like your thoughts

                Matrix4f model = new Matrix4f()
                        .translate(chunkX * 16.0f, 0.0f, chunkZ * 16.0f);

                try {
                    chunk.render(model, view, projection);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Chunk generateChunk(ChunkPos chunkPos) {
        Chunk chunk = new Chunk(16);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                Block block = (x == 0 || x == 15 || z == 0 || z == 15)
                        ? Blocks.DIAMOND_BLOCK     // Edge block
                        : Blocks.GRASS_BLOCK; // Inner block

                BlockPos blockPos = new BlockPos(x, 0, z);
                chunk.setBlock(block, blockPos, BlockAction.NONE);
            }
        }

        chunk.updateMesh();
        return chunk;
    }



    public void init() {


        for (Chunk chunk : chunks.values()) {
            chunk.updateMesh();
        }
    }
}