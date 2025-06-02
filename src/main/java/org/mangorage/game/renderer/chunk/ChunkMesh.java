package org.mangorage.game.renderer.chunk;

import java.util.List;

import static org.lwjgl.opengl.ARBVertexArrayObject.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL15C.glDeleteBuffers;

public final class ChunkMesh {
    private final int vao;
    private final int vbo;
    private final List<DrawCommand> drawCommands;

    public ChunkMesh(int vao, int vbo, List<DrawCommand> drawCommands) {
        this.vao = vao;
        this.vbo = vbo;
        this.drawCommands = drawCommands;
    }

    public int getVao() {
        return vao;
    }

    public int getVbo() {
        return vbo;
    }

    public List<DrawCommand> drawCommands() {
        return drawCommands;
    }

    public void dispose() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
}
