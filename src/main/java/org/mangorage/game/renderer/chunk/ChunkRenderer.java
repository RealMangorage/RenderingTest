package org.mangorage.game.renderer.chunk;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.mangorage.game.block.Block;
import org.mangorage.game.core.BuiltInRegistries;
import org.mangorage.game.core.Direction;
import org.mangorage.game.util.supplier.InitializableSupplier;
import org.mangorage.game.world.World;
import org.mangorage.game.world.chunk.Chunk;
import org.mangorage.game.world.chunk.ChunkPos;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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

    private final int shaderProgram;
    private final int modelLoc, viewLoc, projLoc, texUniformSampler, texUniform, tintLoc;

    private final Map<String, Integer> textureCache = new HashMap<>();

    ChunkRenderer() {
        shaderProgram = createShaderProgram();

        modelLoc = glGetUniformLocation(shaderProgram, "model");
        viewLoc = glGetUniformLocation(shaderProgram, "view");
        projLoc = glGetUniformLocation(shaderProgram, "projection");
        texUniformSampler = glGetUniformLocation(shaderProgram, "texSampler");

        tintLoc = glGetUniformLocation(shaderProgram, "tint");
        texUniform = glGetUniformLocation(shaderProgram, "tex"); // or whatever your sampler uniform is named
    }

    private int getOrCreateTexture(String resourceName) {
        return textureCache.computeIfAbsent(resourceName, name -> {
            try {
                return loadTextureFromResource(name);
            } catch (Throwable e) {
                return loadTextureFromResource("assets/textures/misc/missing.png");
            }
        });
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

            if (GL.getCapabilities().GL_EXT_texture_filter_anisotropic) {
                float maxAniso = glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
                glTexParameterf(GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, maxAniso);
            }

            stbi_image_free(image);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load texture resource: " + resourceName, e);
        }

        return textureId;
    }

    public ChunkMesh buildMesh(World world, ChunkPos chunkPos, int[][][] blocks) {
        List<Float> vertices = new ArrayList<>();
        List<DrawCommand> drawCommands = new ArrayList<>();

        int width = blocks.length;
        int height = blocks[0].length;
        int depth = blocks[0][0].length;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    Block currentBlock = BuiltInRegistries.BLOCK_REGISTRY.getByInternalId(blocks[x][y][z]);
                    if (currentBlock == null || currentBlock.isAir()) continue;

                    for (Direction dir : Direction.values()) {
                        int nx = x + dir.x;
                        int ny = y + dir.y;
                        int nz = z + dir.z;

                        Block neighborBlock = null;

                        if (nx >= 0 && nx < 16 && nz >= 0 && nz < 16 && ny >= 0 && ny < height) {
                            // Inside current chunk
                            neighborBlock = BuiltInRegistries.BLOCK_REGISTRY.getByInternalId(blocks[nx][ny][nz]);
                        } else if (world != null) {
                            // Outside current chunk — calculate world position
                            int worldX = chunkPos.x() * 16 + nx;
                            int worldY = ny;
                            int worldZ = chunkPos.z() * 16 + nz;

                            int chunkX = (int) Math.floor(worldX / 16.0);
                            int chunkZ = (int) Math.floor(worldZ / 16.0);
                            int localX = Math.floorMod(worldX, 16);
                            int localZ = Math.floorMod(worldZ, 16);

                            ChunkPos neighborChunkPos = new ChunkPos(chunkX, chunkZ);

                            try {
                                Chunk neighborChunk = world.getLoadedChunk(neighborChunkPos);
                                if (neighborChunk != null && worldY >= 0 && worldY < height) {
                                    int[][][] neighborBlocks = neighborChunk.getSaveData();
                                    neighborBlock = BuiltInRegistries.BLOCK_REGISTRY.getByInternalId(neighborBlocks[localX][worldY][localZ]);
                                }
                            } catch (Throwable ignored) {
                                // You screw up? Catch it and move on. Typical.
                            }
                        }

                        boolean shouldRenderFace = neighborBlock == null || neighborBlock.isAir();

                        if (shouldRenderFace) {
                            int vertexStart = vertices.size() / 5;
                            addFaceVertices(vertices, x, y, z, currentBlock, dir);
                            int addedVerts = (vertices.size() / 5) - vertexStart;

                            if (addedVerts > 0) {
                                if (currentBlock != BuiltInRegistries.GRASS_BLOCK) {
                                    int texId = getOrCreateTexture(currentBlock.getBlockInfo().getTexture(dir));
                                    float[] tint = currentBlock.getTint(dir, 1);
                                    drawCommands.add(new DrawCommand(texId, vertexStart, addedVerts, tint, state -> {}));
                                } else {
                                    float[] tint = currentBlock.getTint(dir, 1);
                                    int texId = getOrCreateTexture(currentBlock.getBlockInfo().getTexture(dir));
                                    drawCommands.add(new DrawCommand(texId, vertexStart, addedVerts, tint, state -> {}));

                                    int texOverlay = getOrCreateTexture("assets/textures/blocks/grass_block_side_overlay.png");
                                    if (dir != Direction.UP && dir != Direction.DOWN) {
                                        drawCommands.add(new DrawCommand(texOverlay, vertexStart, addedVerts, currentBlock.getTint(dir, 2), state -> {
                                            if (state) {
                                                glEnable(GL_BLEND);
                                                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                                                glEnable(GL_POLYGON_OFFSET_FILL);
                                                glPolygonOffset(-0.02f, 0f);
                                                glDepthMask(false);
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

        int meshVao = glGenVertexArrays();
        int meshVbo = glGenBuffers();

        glBindVertexArray(meshVao);
        glBindBuffer(GL_ARRAY_BUFFER, meshVbo);

        if (!vertices.isEmpty()) {
            float[] vertexArray = new float[vertices.size()];
            for (int i = 0; i < vertexArray.length; i++) vertexArray[i] = vertices.get(i);

            glBufferData(GL_ARRAY_BUFFER, vertexArray, GL_STATIC_DRAW);

            glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);
            glEnableVertexAttribArray(0);

            glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
            glEnableVertexAttribArray(1);
        } else {
            glBufferData(GL_ARRAY_BUFFER, 0, GL_STATIC_DRAW);
        }

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        return new ChunkMesh(meshVao, meshVbo, drawCommands);
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

    public void render(ChunkMesh chunkMesh, Matrix4f model, Matrix4f view, Matrix4f projection) {
        glUseProgram(shaderProgram);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(modelLoc, false, model.get(stack.mallocFloat(16)));
            glUniformMatrix4fv(viewLoc, false, view.get(stack.mallocFloat(16)));
            glUniformMatrix4fv(projLoc, false, projection.get(stack.mallocFloat(16)));
        }

        glBindVertexArray(chunkMesh.getVao());
        glActiveTexture(GL_TEXTURE0);
        glUniform1i(texUniform, 0);

        int lastTexture = -1;
        float[] lastTint = null;
        Consumer<Boolean> lastExtra = null;

        for (DrawCommand cmd : chunkMesh.drawCommands()) {
            if (cmd.textureId() != lastTexture) {
                glBindTexture(GL_TEXTURE_2D, cmd.textureId());
                lastTexture = cmd.textureId();
            }

            Consumer<Boolean> extra = cmd.extra();
            if (lastExtra != extra) {
                if (lastExtra != null) lastExtra.accept(false);
                if (extra != null) extra.accept(true);
                lastExtra = extra;
            }

            float[] tint = cmd.tint();
            if (lastTint == null || !java.util.Arrays.equals(tint, lastTint)) {
                if (tint == null) {
                    glUniform3f(tintLoc, 1f, 1f, 1f);
                } else {
                    glUniform3f(tintLoc, tint[0], tint[1], tint[2]);
                }
                lastTint = tint;
            }

            glDrawArrays(GL_TRIANGLES, cmd.startIndex(), cmd.vertexCount());
        }

        if (lastExtra != null) lastExtra.accept(false);

        glBindVertexArray(0);
        glUseProgram(0);
    }

    public void dispose() {
        glDeleteProgram(shaderProgram);
        for (int textureId : textureCache.values()) glDeleteTextures(textureId);
        textureCache.clear();
    }
}