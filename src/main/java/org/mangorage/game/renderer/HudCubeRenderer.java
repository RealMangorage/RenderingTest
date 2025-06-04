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
    private final IntBuffer viewport = BufferUtils.createIntBuffer(4);

    public HudCubeRenderer(int windowWidth, int windowHeight) {
        setScreenSize(windowWidth, windowHeight);
        setActiveBlock(BuiltInRegistries.DIAMOND_BLOCK);
        update();
    }

    public void update() {
        glGetIntegerv(GL_VIEWPORT, viewport);
    }

    public void setActiveBlock(Block block) {
        int[][][] blocks = new int[16][16][16];
        blocks[0][0][0] = BuiltInRegistries.BLOCK_REGISTRY.getInternalId(block);
        final var oldMesh = this.chunkMesh;
        this.chunkMesh = null;
        if (oldMesh != null) {
            oldMesh.dispose();
        }
        this.chunkMesh = ChunkRenderer.get().buildMesh(null, null, blocks);
    }

    public void render(float size) {

        int fullWidth = viewport.get(2);
        int fullHeight = viewport.get(3);

        // Make viewport larger for bigger HUD cube
        int tinyWidth = fullWidth / 4;   // Changed from /6 to /4
        int tinyHeight = fullHeight / 4; // Changed from /6 to /4

        // Set viewport to bottom-left area
        glViewport(0, 0, tinyWidth, tinyHeight);
        glClear(GL_DEPTH_BUFFER_BIT);

        // Create separate close-up view just for HUD to make cube appear larger
        Matrix4f hudView = new Matrix4f()
                .lookAt(
                        2f, 2f, 2f,    // Very close camera for large appearance
                        0f, 0f, 0f,
                        0f, 1f, 0f
                );

        Matrix4f model = new Matrix4f()
                .translate(0.3f, -0.2f, -0.5f)        // Offset positioning
                .rotateY((float) Math.toRadians(185))  // Your preferred rotation
                .rotateX((float) Math.toRadians(-5))    // No X tilt
                .rotateZ((float) Math.toRadians(0))    // No roll
                .scale(size, size, size);

        ChunkRenderer.get().render(chunkMesh, model, hudView, projection);

        // Restore full viewport
        glViewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
    }

    public void setScreenSize(int width, int height) {
        float aspectRatio = (float) width / height;
        projection.identity()
                .perspective((float) Math.toRadians(45.0), aspectRatio, 0.1f, 100f);

        view.identity()
                .lookAt(
                        40f, 40f, 40f,  // Main game camera stays the same
                        0f, 0f, 0f,
                        0f, 1f, 0f
                );
        update();
    }
}