package org.mangorage.game.renderer;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.mangorage.game.block.Block;
import org.mangorage.game.core.BuiltInRegistries;
import org.mangorage.game.renderer.chunk.ChunkMesh;

import org.mangorage.game.renderer.chunk.ChunkRenderer;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;

public final class HudCubeRenderer {
    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f view = new Matrix4f();
    private volatile ChunkMesh chunkMesh = null;

    public HudCubeRenderer(int windowWidth, int windowHeight) {
        setScreenSize(windowWidth, windowHeight);
        setActiveBlock(BuiltInRegistries.DIAMOND_BLOCK);
    }

    public void setActiveBlock(Block block) {
        int[][][] blocks = new int[1][1][1];
        blocks[0][0][0] = BuiltInRegistries.BLOCK_REGISTRY.getInternalId(block);
        final var oldMesh = this.chunkMesh;
        this.chunkMesh = null;
        if (oldMesh != null) {
            oldMesh.dispose();
        }
        this.chunkMesh = ChunkRenderer.get().buildMesh(blocks);
    }

    public void render(float size) {
        // Save current viewport
        IntBuffer viewport = BufferUtils.createIntBuffer(4);
        glGetIntegerv(GL_VIEWPORT, viewport);

        int fullWidth = viewport.get(2);
        int fullHeight = viewport.get(3);

        int tinyWidth = fullWidth / 6;
        int tinyHeight = fullHeight / 6;

        // Set viewport to bottom-left tiny area
        glViewport(0, 0, tinyWidth, tinyHeight);
        glClear(GL_DEPTH_BUFFER_BIT);

        Matrix4f model = new Matrix4f()
                .translate(0f, 0f, 0f)
                .scale(size, size, size);

        ChunkRenderer.get().render(chunkMesh, model, view, projection);

        // Restore full viewport
        glViewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
    }

    public void setScreenSize(int width, int height) {
        float aspectRatio = (float) width / height;
        projection.identity()
                .perspective((float) Math.toRadians(45.0), aspectRatio, 0.1f, 100f);

        view.identity()
                .lookAt(
                        50f, 50f, 50f,
                        0f, 0f, 0f,
                        0f, 1f, 0f
                );
    }
}

