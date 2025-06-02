package org.mangorage.game;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.opengl.GL;
import org.mangorage.game.block.Block;
import org.mangorage.game.core.Blocks;
import org.mangorage.game.core.Direction;
import org.mangorage.game.core.KeybindRegistry;
import org.mangorage.game.renderer.BlockOutlineRenderer;
import org.mangorage.game.renderer.HudCubeRenderer;
import org.mangorage.game.util.supplier.InitializableSupplier;
import org.mangorage.game.world.BlockAction;
import org.mangorage.game.world.BlockPos;
import org.mangorage.game.world.World;


import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class Game {

    private final World world = new World();
    private final InitializableSupplier<BlockOutlineRenderer> blockOutlineRenderer = InitializableSupplier.of(BlockOutlineRenderer::new);
    private final InitializableSupplier<HudCubeRenderer> hudCubeRenderer = InitializableSupplier.of(() -> new HudCubeRenderer(800, 600));

    private final KeybindRegistry keybindRegistry = new KeybindRegistry();

    private final Block[] blocks_all = new Block[] {
            Blocks.DIAMOND_BLOCK,
            Blocks.GRASS_BLOCK
    };

    // Selected block pos/block
    private BlockPos selected = null;
    private int selectedBlock = 0;


    // Window itself
    private long window;

    // Various things releated to rendering...
    private final Matrix4f view = new Matrix4f();
    private final Matrix4f projection = new Matrix4f();

    private final Vector3f cameraPos = new Vector3f(0f, 0f, 3f);
    private Vector3f cameraFront = new Vector3f(0f, 0f, -1f);
    private final Vector3f cameraUp = new Vector3f(0f, 1f, 0f);

    // Misc
    private float yaw = -90.0f;
    private float pitch = 0.0f;

    private float lastX = 400f, lastY = 300f;
    private boolean firstMouse = true;

    // Delta/Frame info
    private float deltaTime = 0.0f;
    private float lastFrame = 0.0f;

    // Keys
    private final boolean[] keys = new boolean[1024];

    public void run() {
        // Setup error callback for debugging
        glfwSetErrorCallback((error, description) -> {
            System.err.println("GLFW Error " + error + ": " + nglfwGetError(description));
        });

        init();
        configureKeyBinds();
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
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

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

        glfwSetFramebufferSizeCallback(window, (win, width, height) -> {
            glViewport(0, 0, width, height);
            float aspectN = 800f / 600f;
            projection.setPerspective((float) Math.toRadians(45.0f), aspectN, 0.1f, 100f);
            hudCubeRenderer.get().setScreenSize(width, height); // Assuming you have a method like this
        });

        glEnable(GL_DEPTH_TEST);
        glClearColor(0.2f, 0.3f, 0.3f, 1f);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        Blocks.init();

        // Init all the rendering side things...

        blockOutlineRenderer.init();
        world.init();
        hudCubeRenderer.init();

    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            float currentFrame = (float) glfwGetTime();
            deltaTime = currentFrame - lastFrame;
            lastFrame = currentFrame;

            // Update view matrix with current camera position and orientation
            view.identity()
                    .lookAt(
                            cameraPos,
                            new Vector3f(cameraPos)
                                    .add(cameraFront),
                            cameraUp
                    );

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            this.selected = getBlockInView(10);
            world.render(cameraPos, view, projection);

            if (selected != null)
                blockOutlineRenderer.get().render(selected.toVector3f(), view, projection);

            // In your render loop:
            hudCubeRenderer.get().render(20);

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
                keybindRegistry.consume(key, scancode, action, mods);
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

    public BlockPos getBlockInView(float maxDistance) {
        Vector3f rayOrigin = new Vector3f(cameraPos);
        Vector3f rayDirection = new Vector3f(cameraFront).normalize();

        Vector3f currentPos = new Vector3f(rayOrigin);
        for (int i = 0; i < maxDistance * 10; i++) {
            currentPos.fma(0.1f, rayDirection); // move along the ray in 0.1 increments
            BlockPos pos = new BlockPos((int)Math.floor(currentPos.x),
                    (int)Math.floor(currentPos.y),
                    (int)Math.floor(currentPos.z));
            if (!world.getBlock(pos).isAir()) {
                return pos; // block hit
            }
        }

        return null; // nothing hit
    }

    private void configureKeyBinds() {
        // Escape key - close window
        keybindRegistry.register((key, scancode, action, mods) -> {
            if (action == GLFW_PRESS && key == GLFW_KEY_ESCAPE) {
                glfwSetWindowShouldClose(window, true);
                return true;
            }
            return false;
        }, 250);

        // Movement keys - W A S D
        keybindRegistry.register((key, scancode, action, mods) -> {
            if (action != GLFW_PRESS && action != GLFW_REPEAT) return false;

            float cameraSpeed = 32.5f * deltaTime;
            Vector3f right = new Vector3f();
            cameraFront.cross(cameraUp, right).normalize();

            switch (key) {
                case GLFW_KEY_W -> cameraPos.add(new Vector3f(cameraFront).mul(cameraSpeed));
                case GLFW_KEY_S -> cameraPos.sub(new Vector3f(cameraFront).mul(cameraSpeed));
                case GLFW_KEY_A -> cameraPos.sub(new Vector3f(right).mul(cameraSpeed));
                case GLFW_KEY_D -> cameraPos.add(new Vector3f(right).mul(cameraSpeed));
                default -> {
                    return false;
                }
            }
            return true;
        }, -1);

        // Place grass block at fixed pos
        keybindRegistry.register((key, scancode, action, mods) -> {
            if ((action == GLFW_PRESS || action == GLFW_REPEAT) && key == GLFW_KEY_G) {
                if (world != null) {
                    world.setBlock(Blocks.GRASS_BLOCK, new BlockPos(15, 0, 15), BlockAction.UPDATE);
                }
                return true;
            }
            return false;
        }, 0);

        // Remove selected block
        keybindRegistry.register((key, scancode, action, mods) -> {
            if ((action == GLFW_PRESS || action == GLFW_REPEAT) && key == GLFW_KEY_F) {
                if (selected != null) {
                    world.setBlock(null, selected, BlockAction.UPDATE);
                }
                return true;
            }
            return false;
        }, 0);

        // Place selected block
        keybindRegistry.register((key, scancode, action, mods) -> {
            if ((action == GLFW_PRESS || action == GLFW_REPEAT) && key == GLFW_KEY_H) {
                if (selected != null) {
                    world.setBlock(
                            blocks_all[selectedBlock],
                            Direction.fromFacingVector(cameraFront).getOpposite().offset(selected),
                            BlockAction.UPDATE
                    );
                }
                return true;
            }
            return false;
        }, 0);

        // Cycle selected block
        keybindRegistry.register((key, scancode, action, mods) -> {
            if ((action == GLFW_PRESS || action == GLFW_REPEAT) && key == GLFW_KEY_M) {
                selectedBlock = (selectedBlock + 1) % blocks_all.length;
                hudCubeRenderer.get().setActiveBlock(blocks_all[selectedBlock]);
                return true;
            }
            return false;
        }, 0);
    }
}
