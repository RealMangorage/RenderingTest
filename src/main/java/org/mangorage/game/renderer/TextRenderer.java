package org.mangorage.game.renderer;

import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.glActiveTexture;
import static org.lwjgl.stb.STBTruetype.*;

public final class TextRenderer {

    private static int fontTexture;
    private static STBTTBakedChar.Buffer charData;

    public static void init() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer fontBuffer = ioResourceToByteBuffer("fonts/Roboto-Regular.ttf", 512 * 1024);

            charData = STBTTBakedChar.malloc(96);

            ByteBuffer bitmap = ByteBuffer.allocateDirect(512 * 512);

            int bakeResult = stbtt_BakeFontBitmap(fontBuffer, 24, bitmap, 512, 512, 32, charData);
            if (bakeResult <= 0) {
                throw new RuntimeException("Failed to bake font bitmap");
            }

            fontTexture = glGenTextures();
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, fontTexture);
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_ALPHA, 512, 512, 0, GL_ALPHA, GL_UNSIGNED_BYTE, bitmap);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load font", e);
        }
    }

    public static void drawString(String text, float x, float y) {
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, fontTexture);

        glPushMatrix();
        glTranslatef(x, y, 0);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            float[] xPos = {0f};
            float[] yPos = {0f};
            STBTTAlignedQuad quad = STBTTAlignedQuad.mallocStack(stack);

            glBegin(GL_QUADS);

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);

                if (c == '\n') {
                    // Move to the start of next line:
                    xPos[0] = 0f;
                    yPos[0] += 24;  // 24 is your font height, adjust as needed
                    continue;
                }

                if (c < 32 || c >= 128) continue;

                stbtt_GetBakedQuad(charData, 512, 512, c - 32, xPos, yPos, quad, true);


                glTexCoord2f(quad.s0(), quad.t1());
                glVertex2f(quad.x0(), quad.y1());


                glTexCoord2f(quad.s1(), quad.t1());
                glVertex2f(quad.x1(), quad.y1());

                glTexCoord2f(quad.s1(), quad.t0());
                glVertex2f(quad.x1(), quad.y0());


                glTexCoord2f(quad.s0(), quad.t0());
                glVertex2f(quad.x0(), quad.y0());
                
            }

            glEnd();
        }

        glPopMatrix();
    }

    private static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
        try (InputStream source = TextRenderer.class.getClassLoader().getResourceAsStream(resource)) {
            if (source == null)
                throw new IOException("Resource not found: " + resource);

            byte[] bytes = source.readAllBytes();
            ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            return buffer;
        }
    }
}
