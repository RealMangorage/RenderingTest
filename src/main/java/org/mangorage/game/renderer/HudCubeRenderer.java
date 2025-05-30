package org.mangorage.game.renderer;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.mangorage.game.block.Block;
import org.mangorage.game.core.Blocks;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;

public class HudCubeRenderer {
    private final CubeRenderer cubeRenderer;
    private final Matrix4f projection;
    private final Matrix4f view;

    public HudCubeRenderer(int windowWidth, int windowHeight) {
        cubeRenderer = new CubeRenderer();

        float aspectRatio = (float) windowWidth / windowHeight;
        projection = new Matrix4f()
                .perspective((float) Math.toRadians(45.0), aspectRatio, 0.1f, 100f);

        view = new Matrix4f()
                .identity()
                .lookAt(
                        50f, 50f, 50f,  // Camera waaaaay out there
                        0f, 0f, 0f,
                        0f, 1f, 0f
                );

        setActiveBlock(Blocks.DIAMOND_BLOCK);
    }

    public void setActiveBlock(Block block) {
        Block[][][] blocks = new Block[1][1][1];
        blocks[0][0][0] = block;
        cubeRenderer.buildMesh(blocks);
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

        cubeRenderer.render(model, view, projection);

        // Restore full viewport
        glViewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
    }

    public void dispose() {
        cubeRenderer.dispose();
    }
}

