package org.mangorage.game.world;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.mangorage.game.block.Block;
import org.mangorage.game.core.BuiltInRegistries;
import org.mangorage.game.core.Direction;
import org.mangorage.game.world.chunk.Chunk;
import org.mangorage.game.world.chunk.ChunkPos;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class World {
    private static final int RENDER_DISTANCE = 8;

    private final Map<ChunkPos, Chunk> chunks = new ConcurrentHashMap<>();

    public Chunk getLoadedChunk(ChunkPos chunkPos) {
        return chunks.get(chunkPos);
    }

    public Chunk getChunk(ChunkPos chunkPos) {
        return chunks.computeIfAbsent(chunkPos, this::loadChunk);
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
        if (chunk == null) return BuiltInRegistries.AIR_BLOCK;
        BlockPos localPos = new BlockPos(
                Math.floorMod(blockPos.x(), 16),
                blockPos.y(),
                Math.floorMod(blockPos.z(), 16)
        );
        return chunk.getBlock(localPos);
    }

    public void render(Vector3f cameraPos, Matrix4f view, Matrix4f projection) {
        chunks.forEach((d, c) -> c.updateMesh());
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

    public void clearUnusedChunks(Vector3f cameraPos) {
        int cameraChunkX = Math.floorDiv((int) cameraPos.x, 16);
        int cameraChunkZ = Math.floorDiv((int) cameraPos.z, 16);

        chunks.entrySet().removeIf(entry -> {
            ChunkPos pos = entry.getKey();
            Chunk chunk = entry.getValue();

            int dx = pos.x() - cameraChunkX;
            int dz = pos.z() - cameraChunkZ;

            boolean outside = dx < -RENDER_DISTANCE || dx > RENDER_DISTANCE
                    || dz < -RENDER_DISTANCE || dz > RENDER_DISTANCE;

            if (outside) {
                // YOUR EXTRA LOGIC HERE: e.g. save chunk data, log info, free resources
                onChunkRemoved(chunk, pos);
            }

            return outside;
        });
    }

    private void onChunkRemoved(Chunk chunk, ChunkPos pos) {
        // Example of extra logic:
        System.out.println("Removing chunk at " + pos);
        chunk.dispose();
        saveChunk(chunk, pos);
    }


    public Chunk loadChunk(ChunkPos chunkPos) {
        Path worldFolder = Path.of("world");
        if (!Files.exists(worldFolder)) return generateChunk(chunkPos);
        Path chunkFile = worldFolder.resolve("chk-%s-%s.chk".formatted(chunkPos.x(), chunkPos.z()));
        if (!Files.exists(chunkFile)) return generateChunk(chunkPos);

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(chunkFile.toFile())))) {
            // Read dimensions
            int x = in.readInt();
            int y = in.readInt();
            int z = in.readInt();

            Chunk chunk = new Chunk(255, this, chunkPos);

            // Read the data
            for (int i = 0; i < x; i++) {
                for (int j = 0; j < y; j++) {
                    for (int k = 0; k < z; k++) {
                        chunk.setBlock(
                                BuiltInRegistries.BLOCK_REGISTRY.getByInternalId(in.readInt()),
                                new BlockPos(i, j, k),
                                BlockAction.NONE
                        );
                    }
                }
            }

            return chunk;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveChunk(Chunk chunk, ChunkPos chunkPos) {

        Path worldFolder = Path.of("world");
        try {
            if (!Files.exists(worldFolder))
                Files.createDirectory(worldFolder);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Path chunkFile = worldFolder.resolve("chk-%s-%s.chk".formatted(chunkPos.x(), chunkPos.z()));

        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(chunkFile.toFile())))) {
            // Write the two integers

            final var data = chunk.getSaveData();
            // Write dimensions
            int x = data.length;
            int y = data[0].length;
            int z = data[0][0].length;
            out.writeInt(x);
            out.writeInt(y);
            out.writeInt(z);

            // Write the data
            for (int[][] slice : data) {
                for (int[] row : slice) {
                    for (int val : row) {
                        out.writeInt(val);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Chunk generateChunk(ChunkPos chunkPos) {
        Chunk chunk = new Chunk(4, this, chunkPos); // Chunk Height

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 16; y++) {
                    Block block = (x == 0 || x == 15 || z == 0 || z == 15)
                            ? BuiltInRegistries.DIAMOND_BLOCK     // Edge block
                            : BuiltInRegistries.GRASS_BLOCK; // Inner block

                    BlockPos blockPos = new BlockPos(x, y, z);
                    chunk.setBlock(block, blockPos, BlockAction.NONE);
                }
            }
        }

        chunk.updateMesh();

        return chunk;
    }

    public void saveAll() {
        chunks.forEach(((chunkPos, chunk) -> saveChunk(chunk, chunkPos)));
    }
}