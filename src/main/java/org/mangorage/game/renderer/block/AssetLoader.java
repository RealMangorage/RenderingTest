package org.mangorage.game.renderer.block;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.GL;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.stb.STBImage.stbi_image_free;

public final class AssetLoader {
    private final Map<String, Integer> cache = new HashMap<>();


    public int getOrCreateTexture(String resourceName) {
        return cache.computeIfAbsent(resourceName, name -> {
            try {
                return loadTextureFromResource(name);
            } catch (Throwable e) {
                return loadTextureFromResource("assets/textures/misc/missing.png");
            }
        });
    }

    public void dispose() {
        for (int textureId : cache.values()) glDeleteTextures(textureId);
        cache.clear();
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
}
