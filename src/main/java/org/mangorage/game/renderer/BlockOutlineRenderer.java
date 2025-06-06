
package org.mangorage.game.renderer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public final class BlockOutlineRenderer {

    private final int vaoId;
    private int vboId;
    private final int shaderProgram;

    private final int uniformProjection;
    private final int uniformView;
    private final int uniformModel;

    private float[] currentOutlineVertices;
    private int currentVertexCount;

    private float lineWidth = 12.0f; // Default thickness

    public BlockOutlineRenderer() {
        vaoId = glGenVertexArrays();
        vboId = glGenBuffers();

        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);

        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        shaderProgram = createShaderProgram();
        uniformProjection = glGetUniformLocation(shaderProgram, "projection");
        uniformView = glGetUniformLocation(shaderProgram, "view");
        uniformModel = glGetUniformLocation(shaderProgram, "model");
    }

    private int createShaderProgram() {
        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader,
                "#version 330 core\n" +
                        "layout(location = 0) in vec3 aPos;\n" +
                        "uniform mat4 projection;\n" +
                        "uniform mat4 view;\n" +
                        "uniform mat4 model;\n" +
                        "void main() {\n" +
                        "  gl_Position = projection * view * model * vec4(aPos, 1.0);\n" +
                        "}"
        );
        glCompileShader(vertexShader);
        checkCompileErrors(vertexShader, "VERTEX");

        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader,
                "#version 330 core\n" +
                        "out vec4 FragColor;\n" +
                        "void main() {\n" +
                        "  FragColor = vec4(0.2, 0.6, 1.0, 1.0);\n" + // Blue color
                        "}"
        );
        glCompileShader(fragmentShader);
        checkCompileErrors(fragmentShader, "FRAGMENT");

        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);
        checkCompileErrors(program, "PROGRAM");

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        return program;
    }

    private void checkCompileErrors(int shader, String type) {
        int success;
        if (type.equals("PROGRAM")) {
            success = glGetProgrami(shader, GL_LINK_STATUS);
            if (success == GL_FALSE) {
                String infoLog = glGetProgramInfoLog(shader);
                throw new RuntimeException("ERROR::SHADER_PROGRAM_LINKING_ERROR\n" + infoLog);
            }
        } else {
            success = glGetShaderi(shader, GL_COMPILE_STATUS);
            if (success == GL_FALSE) {
                String infoLog = glGetShaderInfoLog(shader);
                throw new RuntimeException("ERROR::SHADER_COMPILATION_ERROR of type: " + type + "\n" + infoLog);
            }
        }
    }

    /**
     * Set the line width for the outline rendering
     * @param width Line width in pixels (default is 3.0f)
     */
    public void setLineWidth(float width) {
        this.lineWidth = Math.max(1.0f, width); // Ensure minimum width of 1.0
    }

    /**
     * Update the outline vertices directly from a block's outline definition.
     * Much simpler and more efficient than extracting from geometry.
     *
     * @param outline Array of vertices forming line segments [start1, end1, start2, end2, ...]
     */
    public void updateOutline(float[][] outline) {
        if (outline == null || outline.length == 0) {
            currentOutlineVertices = new float[0];
            currentVertexCount = 0;
            return;
        }

        // The outline array already contains the vertices in the correct format
        currentOutlineVertices = new float[outline.length * 3];
        for (int i = 0; i < outline.length; i++) {
            currentOutlineVertices[i * 3] = outline[i][0];     // x
            currentOutlineVertices[i * 3 + 1] = outline[i][1]; // y
            currentOutlineVertices[i * 3 + 2] = outline[i][2]; // z
        }
        currentVertexCount = outline.length;

        // Update VBO with new data
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(currentOutlineVertices.length);
        vertexBuffer.put(currentOutlineVertices).flip();
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    /**
     * Render the block outline wireframe at given position.
     *
     * @param position Position in world coordinates where outline should be drawn
     * @param view The current view matrix
     * @param projection The current projection matrix
     */
    public void render(Vector3f position, Matrix4f view, Matrix4f projection) {
        if (currentOutlineVertices == null || currentVertexCount == 0) {
            return; // Nothing to render
        }

        glUseProgram(shaderProgram);

        // Set line width
        glLineWidth(lineWidth);

        // Enable line smoothing for better appearance (optional)
        glEnable(GL_LINE_SMOOTH);
        glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);

        // Prepare model matrix translating to the correct block position
        Matrix4f model = new Matrix4f().translation(position);

        // Upload uniforms
        FloatBuffer fb = BufferUtils.createFloatBuffer(16);
        projection.get(fb);
        glUniformMatrix4fv(uniformProjection, false, fb);

        view.get(fb);
        glUniformMatrix4fv(uniformView, false, fb);

        model.get(fb);
        glUniformMatrix4fv(uniformModel, false, fb);

        // Draw wireframe
        glBindVertexArray(vaoId);
        glDrawArrays(GL_LINES, 0, currentVertexCount);
        glBindVertexArray(0);

        // Restore default line width and disable smoothing
        glLineWidth(1.0f);
        glDisable(GL_LINE_SMOOTH);

        glUseProgram(0);
    }

    public void cleanup() {
        glDeleteVertexArrays(vaoId);
        glDeleteBuffers(vboId);
        glDeleteProgram(shaderProgram);
    }
}