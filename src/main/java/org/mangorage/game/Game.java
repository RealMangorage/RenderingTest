package org.mangorage.game;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.opengl.GL;
import org.mangorage.game.core.Blocks;
import org.mangorage.game.renderer.CubeRenderer;
import org.mangorage.game.world.WorldInstance;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class Game {

    private final WorldInstance worldInstance = new WorldInstance();

    private long window;

    private final Matrix4f view = new Matrix4f();
    private final Matrix4f projection = new Matrix4f();

    private final Vector3f cameraPos = new Vector3f(0f, 0f, 3f);
    private Vector3f cameraFront = new Vector3f(0f, 0f, -1f);
    private final Vector3f cameraUp = new Vector3f(0f, 1f, 0f);

    private float yaw = -90.0f;
    private float pitch = 0.0f;

    private float lastX = 400f, lastY = 300f;
    private boolean firstMouse = true;

    private float deltaTime = 0.0f;
    private float lastFrame = 0.0f;

    private final boolean[] keys = new boolean[1024];

    public static void main(String[] args) {
        new Game().run();
    }

    public void run() {
        // Setup error callback for debugging
        glfwSetErrorCallback((error, description) -> {
            System.err.println("GLFW Error " + error + ": " + nglfwGetError(description));
            //            textures[i] = loadTextureFromResource("assets/textures/blocks/diamond_block/side.png");
        });

        init();
        loop();

        // Destroy callbacks and window properly
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private void init() {
        if (!glfwInit()) {
            throw new IllegalStateException("GLFW initialization failed");
        }


        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);

        window = glfwCreateWindow(800, 600, "Cube Renderer Test", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); // Enable v-sync
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        GL.createCapabilities();

        // Setup viewport and projection matrix
        glViewport(0, 0, 800, 600);
        float aspect = 800f / 600f;
        projection.setPerspective((float) Math.toRadians(45.0f), aspect, 0.1f, 100f);

        // Setup callbacks
        glfwSetCursorPosCallback(window, mouseCallback());
        glfwSetKeyCallback(window, keyCallback());

        glEnable(GL_DEPTH_TEST);
        glClearColor(0.2f, 0.3f, 0.3f, 1f);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        Blocks.init();

        final var blocks = CubeRenderer.makeTestBlockArray(128, 2, 128, (x, y, z) -> {
            if (y == 1)
                return Blocks.GRASS_BLOCK;
            return Blocks.DIAMOND_BLOCK;
        });

        Blocks.GRASS_BLOCK.getRenderer().buildMesh(blocks);
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            float currentFrame = (float) glfwGetTime();
            deltaTime = currentFrame - lastFrame;
            lastFrame = currentFrame;

            processInput();

            // Update view matrix with current camera position and orientation
            view.identity().lookAt(cameraPos,
                    new Vector3f(cameraPos).add(cameraFront),
                    cameraUp);

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            Blocks.DIAMOND_BLOCK.getRenderer().render(new Matrix4f(), view, projection);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private GLFWCursorPosCallback mouseCallback() {
        return new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                if (firstMouse) {
                    lastX = (float) xpos;
                    lastY = (float) ypos;
                    firstMouse = false;
                }

                float xoffset = (float) xpos - lastX;
                float yoffset = lastY - (float) ypos; // reversed since y-coordinates go from top to bottom

                lastX = (float) xpos;
                lastY = (float) ypos;

                float sensitivity = 0.1f;
                xoffset *= sensitivity;
                yoffset *= sensitivity;

                yaw += xoffset;
                pitch += yoffset;

                // Clamp the pitch angle
                if (pitch > 89.0f) pitch = 89.0f;
                if (pitch < -89.0f) pitch = -89.0f;

                updateCameraFront();
            }
        };
    }

    private GLFWKeyCallback keyCallback() {
        return new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
                    glfwSetWindowShouldClose(window, true);
                }

                if (key >= 0 && key < 1024) {
                    if (action == GLFW_PRESS) {
                        keys[key] = true;
                    } else if (action == GLFW_RELEASE) {
                        keys[key] = false;
                    }
                }
            }
        };
    }

    private void updateCameraFront() {
        Vector3f front = new Vector3f();
        front.x = (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        front.y = (float) Math.sin(Math.toRadians(pitch));
        front.z = (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        cameraFront = front.normalize();
    }

    private void processInput() {
        float cameraSpeed = 2.5f * deltaTime;
        Vector3f right = new Vector3f();
        cameraFront.cross(cameraUp, right).normalize();

        if (keys[GLFW_KEY_W]) {
            cameraPos.add(new Vector3f(cameraFront).mul(cameraSpeed));
        }
        if (keys[GLFW_KEY_S]) {
            cameraPos.sub(new Vector3f(cameraFront).mul(cameraSpeed));
        }
        if (keys[GLFW_KEY_A]) {
            cameraPos.sub(new Vector3f(right).mul(cameraSpeed));
        }
        if (keys[GLFW_KEY_D]) {
            cameraPos.add(new Vector3f(right).mul(cameraSpeed));
        }
    }
}
