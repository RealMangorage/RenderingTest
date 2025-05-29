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
    private final int vboId;
    private final int shaderProgram;

    private final int uniformProjection;
    private final int uniformView;
    private final int uniformModel;

    // Wireframe edges of a cube from (0,0,0) to (1,1,1)
    private static final float[] CUBE_OUTLINE_VERTICES = {
            0, 0, 0,  1, 0, 0,
            1, 0, 0,  1, 1, 0,
            1, 1, 0,  0, 1, 0,
            0, 1, 0,  0, 0, 0,

            0, 0, 1,  1, 0, 1,
            1, 0, 1,  1, 1, 1,
            1, 1, 1,  0, 1, 1,
            0, 1, 1,  0, 0, 1,

            0, 0, 0,  0, 0, 1,
            1, 0, 0,  1, 0, 1,
            1, 1, 0,  1, 1, 1,
            0, 1, 0,  0, 1, 1,
    };

    public BlockOutlineRenderer() {
        vaoId = glGenVertexArrays();
        vboId = glGenBuffers();

        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);

        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(CUBE_OUTLINE_VERTICES.length);
        vertexBuffer.put(CUBE_OUTLINE_VERTICES).flip();

        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

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
                        "  FragColor = vec4(1.0, 1.0, 0.0, 1.0);\n" + // Yellow color
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
     * Render the block outline wireframe cube at given position.
     *
     * @param position Position in world coordinates where cube outline should be drawn
     * @param view The current view matrix
     * @param projection The current projection matrix
     */
    public void render(Vector3f position, Matrix4f view, Matrix4f projection) {
        glUseProgram(shaderProgram);

        // Prepare model matrix translating cube to the correct block position
        Matrix4f model = new Matrix4f().translation(position);

        // Upload uniforms
        FloatBuffer fb = BufferUtils.createFloatBuffer(16);
        projection.get(fb);
        glUniformMatrix4fv(uniformProjection, false, fb);

        view.get(fb);
        glUniformMatrix4fv(uniformView, false, fb);

        model.get(fb);
        glUniformMatrix4fv(uniformModel, false, fb);

        // Draw wireframe cube
        glBindVertexArray(vaoId);
        glDrawArrays(GL_LINES, 0, CUBE_OUTLINE_VERTICES.length / 3);
        glBindVertexArray(0);

        glUseProgram(0);
    }

    public void cleanup() {
        glDeleteVertexArrays(vaoId);
        glDeleteBuffers(vboId);
        glDeleteProgram(shaderProgram);
    }
}