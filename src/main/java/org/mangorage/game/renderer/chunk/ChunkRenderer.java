package org.mangorage.game.renderer.chunk;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;
import org.mangorage.game.block.Block;
import org.mangorage.game.core.Blocks;
import org.mangorage.game.core.Direction;
import org.mangorage.game.util.supplier.InitializableSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBImage.*;
import static org.mangorage.game.util.RenderUtil.rotateUVs;

public final class ChunkRenderer {
    private static final InitializableSupplier<ChunkRenderer> INSTANCE = InitializableSupplier.of(ChunkRenderer::new);

    public static ChunkRenderer get() {
        if (!INSTANCE.isLoaded()) {
            INSTANCE.init();
        }
        return INSTANCE.get();
    }

    private final int vao;
    private final int vbo;
    private final int shaderProgram;
    private final int modelLoc, viewLoc, projLoc, texUniform;
    private final Map<String, Integer> textureCache = new HashMap<>();

    public ChunkRenderer() {
        shaderProgram = createShaderProgram();
        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        modelLoc = glGetUniformLocation(shaderProgram, "model");
        viewLoc = glGetUniformLocation(shaderProgram, "view");
        projLoc = glGetUniformLocation(shaderProgram, "projection");
        texUniform = glGetUniformLocation(shaderProgram, "texSampler");
    }

    private int getOrCreateTexture(String resourceName) {
        return textureCache.computeIfAbsent(resourceName, this::loadTextureFromResource);
    }

    private int loadTextureFromResource(String resourceName) {
        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) throw new RuntimeException("Texture resource not found: " + resourceName);
            byte[] imageBytes = in.readAllBytes();
            ByteBuffer imageBuffer = BufferUtils.createByteBuffer(imageBytes.length);
            imageBuffer.put(imageBytes).flip();

            IntBuffer width = BufferUtils.createIntBuffer(1);
            IntBuffer height = BufferUtils.createIntBuffer(1);
            IntBuffer channels = BufferUtils.createIntBuffer(1);

            stbi_set_flip_vertically_on_load(true);
            ByteBuffer image = stbi_load_from_memory(imageBuffer, width, height, channels, 4);
            if (image == null) throw new RuntimeException("Failed to load texture: " + stbi_failure_reason());

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
            glGenerateMipmap(GL_TEXTURE_2D);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            stbi_image_free(image);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load texture resource: " + resourceName, e);
        }

        return textureId;
    }

    public ChunkMesh buildMesh(Block[][][] blocks) {
        List<Float> vertices = new ArrayList<>();
        List<DrawCommand> drawCommands = new ArrayList<>();

        int width = blocks.length;
        int height = blocks[0].length;
        int depth = blocks[0][0].length;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    final var currentBlock = blocks[x][y][z];
                    if (currentBlock == null || currentBlock.isAir()) continue;

                    for (Direction dir : Direction.values()) {
                        int nx = x + dir.x;
                        int ny = y + dir.y;
                        int nz = z + dir.z;

                        boolean isInBounds = !(nx < 0 || ny < 0 || nz < 0 || nx >= width || ny >= height || nz >= depth);
                        boolean shouldRenderFace = !isInBounds && !currentBlock.isAir();

                        if (isInBounds) {
                            Block neighbor = blocks[nx][ny][nz];
                            if (neighbor.isAir())
                                shouldRenderFace = true;
                        }

                        if (shouldRenderFace) {
                            int currentVertexOffset = vertices.size() / 5;
                            addFaceVertices(vertices, x, y, z, currentBlock, dir);
                            int addedVertices = (vertices.size() / 5) - currentVertexOffset;

                            if (addedVertices > 0) {

                                if (currentBlock != Blocks.GRASS_BLOCK) {
                                    int textureId = getOrCreateTexture(currentBlock.getBlockInfo().getTexture(dir));
                                    float[] tint = currentBlock.getTint(dir, 1);
                                    drawCommands.add(new DrawCommand(textureId, currentVertexOffset, addedVertices, tint, state -> {}));
                                }


                                if (currentBlock == Blocks.GRASS_BLOCK) {

                                    float[] tint = currentBlock.getTint(dir, 1);

                                    int textureId = getOrCreateTexture(currentBlock.getBlockInfo().getTexture(dir));
                                    drawCommands.add(new DrawCommand(textureId, currentVertexOffset, addedVertices, tint, state -> {}));


                                    int textureIdOverlay = getOrCreateTexture("assets/textures/blocks/grass_block_side_overlay.png");

                                    if (dir != Direction.UP && dir != Direction.DOWN) {
                                        drawCommands.add(new DrawCommand(textureIdOverlay, currentVertexOffset, addedVertices, currentBlock.getTint(dir, 2), state -> {
                                            if (state) {
                                                glEnable(GL_BLEND);
                                                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

                                                glEnable(GL_POLYGON_OFFSET_FILL);
                                                glPolygonOffset(-0.02f, 0f);

                                                glDepthMask(false);  // disable writing to depth buffer for overlay
                                            } else {
                                                glPolygonOffset(0f, 0f);
                                                glDepthMask(true);
                                                glDisable(GL_POLYGON_OFFSET_FILL);
                                                glDisable(GL_BLEND);
                                            }
                                        }));
                                    }
                                }

                            }
                        }
                    }
                }
            }
        }

        if (!vertices.isEmpty()) {
            float[] vertexArray = new float[vertices.size()];
            for (int i = 0; i < vertexArray.length; i++) vertexArray[i] = vertices.get(i);

            glBindVertexArray(vao);
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, vertexArray, GL_STATIC_DRAW);

            glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);
            glEnableVertexAttribArray(0);

            glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
            glEnableVertexAttribArray(1);

            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);
        } else {
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, 0, GL_STATIC_DRAW);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
        }

        return new ChunkMesh(drawCommands);
    }

    public void render(ChunkMesh chunkMesh, Matrix4f model, Matrix4f view, Matrix4f projection) {
        glUseProgram(shaderProgram);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer modelBuf = stack.mallocFloat(16);
            FloatBuffer viewBuf = stack.mallocFloat(16);
            FloatBuffer projBuf = stack.mallocFloat(16);
            model.get(modelBuf);
            view.get(viewBuf);
            projection.get(projBuf);

            glUniformMatrix4fv(modelLoc, false, modelBuf);
            glUniformMatrix4fv(viewLoc, false, viewBuf);
            glUniformMatrix4fv(projLoc, false, projBuf);
        }

        glBindVertexArray(vao);
        glActiveTexture(GL_TEXTURE0);
        glUniform1i(texUniform, 0);

        final var drawCommands = chunkMesh.drawCommands();

        for (DrawCommand command : drawCommands) {
            glBindTexture(GL_TEXTURE_2D, command.textureId());

            glUseProgram(shaderProgram);

            command.extra().accept(true);

            int tintLoc = glGetUniformLocation(shaderProgram, "tint");

            final float[] tint = command.tint();
            if (tint == null) {
                glUniform3f(tintLoc, 1.0f, 1.0f, 1.0f);  // No tint = white multiplier
            } else {
                glUniform3f(tintLoc, tint[0], tint[1], tint[2]);
            }

            glDrawArrays(GL_TRIANGLES, command.startIndex(), command.vertexCount());
            command.extra().accept(false);
        }

        glBindVertexArray(0);
        glUseProgram(0);
    }

    private void addFaceVertices(List<Float> vertices, int x, int y, int z, Block blockInstance, Direction dir) {
        float[][] face = blockInstance.getShape()[dir.ordinal()];
        float[] texCoords = new float[] { 0,0, 1,0, 1,1, 0,0, 1,1, 0,1 };

        if (dir == Direction.EAST || dir == Direction.NORTH) {
            texCoords = rotateUVs(texCoords, 270);
        }

        for (int i = 0; i < face.length; i++) {
            vertices.add(face[i][0] + x);
            vertices.add(face[i][1] + y);
            vertices.add(face[i][2] + z);
            vertices.add(texCoords[i * 2]);
            vertices.add(texCoords[i * 2 + 1]);
        }
    }

    private int createShaderProgram() {
        String vertexShaderSrc = """
                #version 330 core
                layout(location = 0) in vec3 aPos;
                layout(location = 1) in vec2 aTexCoord;
                out vec2 TexCoord;
                uniform mat4 model;
                uniform mat4 view;
                uniform mat4 projection;
                void main() {
                    gl_Position = projection * view * model * vec4(aPos, 1.0);
                    TexCoord = aTexCoord;
                }
                """;

        String fragmentShaderSrc = """
                #version 330 core
                in vec2 TexCoord;
                out vec4 FragColor;
                uniform sampler2D texSampler;
                uniform vec3 tint;  // The tint color, default is white if no tint
                
                void main() {
                    vec4 texColor = texture(texSampler, TexCoord);
                    FragColor = vec4(texColor.rgb * tint, texColor.a);
                }
                """;

        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, vertexShaderSrc);
        glCompileShader(vertexShader);
        checkShaderCompile(vertexShader);

        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, fragmentShaderSrc);
        glCompileShader(fragmentShader);
        checkShaderCompile(fragmentShader);

        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);
        checkProgramLink(program);

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        return program;
    }

    private void checkShaderCompile(int shader) {
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("Shader compile error: " + glGetShaderInfoLog(shader));
        }
    }

    private void checkProgramLink(int program) {
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Program link error: " + glGetProgramInfoLog(program));
        }
    }

    public void dispose() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        glDeleteProgram(shaderProgram);
        for (int textureId : textureCache.values()) glDeleteTextures(textureId);
        textureCache.clear();
    }
}