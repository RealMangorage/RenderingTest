package org.mangorage.game;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.opengl.GL;
import org.mangorage.game.core.BuiltInRegistries;
import org.mangorage.game.core.Direction;
import org.mangorage.game.core.KeybindRegistry;
import org.mangorage.game.renderer.BlockOutlineRenderer;
import org.mangorage.game.renderer.HudCubeRenderer;
import org.mangorage.game.renderer.TextRenderer;
import org.mangorage.game.util.Cooldown;
import org.mangorage.game.util.supplier.InitializableSupplier;
import org.mangorage.game.world.BlockAction;
import org.mangorage.game.world.BlockHitResult;
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

    // Selected block pos/block
    private BlockHitResult selected = null;
    private Direction selectedFace = null; // <---- HERE IS YOUR SELECTED FACE, FINALLY
    private int selectedBlock = 0;

    // Window itself
    private long window;

    // Various things related to rendering...
    private final Matrix4f view = new Matrix4f();
    private final Matrix4f projection = new Matrix4f();

    private final Vector3f cameraPos = new Vector3f(0f, 20f, 3f);
    private Vector3f cameraFront = new Vector3f(0f, 0f, -1f);
    private final Vector3f cameraUp = new Vector3f(0f, 1f, 0f);

    // Misc
    private float yaw = -90.0f;
    private float pitch = 0.0f;
    private int windowWidth = 800, windowHeight = 600;
    private final Cooldown worldGarbageCollector = new Cooldown(1000 * (60));

    private float lastX = 400f, lastY = 300f;
    private boolean firstMouse = true;

    // Delta/Frame info
    private float deltaTime = 0.0f;
    private float lastFrame = 0.0f;

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

        world.saveAll();
    }

    private void init() {
        if (!glfwInit()) {
            throw new IllegalStateException("GLFW initialization failed");
        }

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_COMPAT_PROFILE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(windowWidth, windowHeight, "Cube Renderer Test", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(0); // Disable v-sync
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        GL.createCapabilities();

        // Setup viewport and projection matrix
        glViewport(0, 0, windowWidth, windowHeight);
        float aspect = ((float) windowWidth) / windowHeight;
        projection.setPerspective((float) Math.toRadians(45.0f), aspect, 0.1f, Float.POSITIVE_INFINITY);

        // Setup callbacks
        glfwSetCursorPosCallback(window, mouseCallback());
        glfwSetKeyCallback(window, keyCallback());

        glfwSetFramebufferSizeCallback(window, (win, width, height) -> {
            glViewport(0, 0, width, height);

            windowWidth = width;
            windowHeight = height;
            float aspectN = ((float) width) / height;
            projection.setPerspective((float) Math.toRadians(45.0f), aspectN, 0.1f, Float.POSITIVE_INFINITY);
            hudCubeRenderer.get().setScreenSize(width, height); // Assuming you have a method like this
        });

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glClearColor(0.2f, 0.3f, 0.3f, 1f);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        BuiltInRegistries.init();
        TextRenderer.init();

        // Init all the rendering side things...
        blockOutlineRenderer.init();
        hudCubeRenderer.init();
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            if (worldGarbageCollector.consume()) {
                world.clearUnusedChunks(cameraPos);
            }

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

            // Get selected block + face every frame, so it’s up to date for clicks and outline render
            selected = getBlockInViewWithFace(16);
            selectedFace = (selected != null) ? selected.getFace() : null;

            world.render(cameraPos, view, projection);

            if (selected != null)
                blockOutlineRenderer.get().render(selected.getPos().toVector3f(), view, projection);

            hudCubeRenderer.get().render(20);
            renderDebugHud(windowWidth, windowHeight);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void renderDebugHud(int windowWidth, int windowHeight) {
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, windowWidth, windowHeight, 0, -1, 1);

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        float fps = deltaTime > 0 ? 1.0f / deltaTime : 0;
        StringBuilder sb = new StringBuilder()
                .append(String.format("FPS: %.0f\n", fps))
                .append(String.format("Pos: (%.2f, %.2f, %.2f)\n", cameraPos.x, cameraPos.y, cameraPos.z))
                .append(String.format("Yaw/Pitch: (%.2f, %.2f)\n", yaw, pitch))
                .append(String.format("Selected Block: %s\n", BuiltInRegistries.BLOCK_REGISTRY.getAll().get(selectedBlock).getName()));

        if (selected != null) {
            sb.append(String.format("Looking at Block Pos: X: %s Y: %s Z: %s\n", selected.getPos().x(), selected.getPos().y(), selected.getPos().z()))
                    .append(String.format("Looking at Block: %s\n", world.getBlock(selected.getPos()).getName()))
                    .append(String.format("Looking at Face: %s\n", selectedFace));
        }

        String direction = getFacingDirection(yaw);
        sb.append(String.format("Facing: %s\n", direction));

        TextRenderer.drawString(sb.toString(), 10, 20);

        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);

        glEnable(GL_DEPTH_TEST);
    }

    private String getFacingDirection(float yaw) {
        yaw = (yaw % 360 + 360) % 360;

        if (yaw >= 45 && yaw < 135)
            return "South";
        else if (yaw >= 135 && yaw < 225)
            return "West";
        else if (yaw >= 225 && yaw < 315)
            return "North";
        else
            return "East";
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
                float yoffset = lastY - (float) ypos;

                lastX = (float) xpos;
                lastY = (float) ypos;

                float sensitivity = 0.1f;
                xoffset *= sensitivity;
                yoffset *= sensitivity;

                yaw += xoffset;
                pitch += yoffset;

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

    public BlockHitResult getBlockInViewWithFace(float maxDistance) {
        Vector3f rayOrigin = new Vector3f(cameraPos);
        Vector3f rayDirection = new Vector3f(cameraFront).normalize();

        Vector3f currentPos = new Vector3f(rayOrigin);
        for (int i = 0; i < maxDistance * 10; i++) {
            currentPos.fma(0.1f, rayDirection);
            int blockX = (int) Math.floor(currentPos.x);
            int blockY = (int) Math.floor(currentPos.y);
            int blockZ = (int) Math.floor(currentPos.z);

            BlockPos pos = new BlockPos(blockX, blockY, blockZ);

            if (!world.getBlock(pos).isAir()) {
                float dx = currentPos.x - blockX - 0.5f;
                float dy = currentPos.y - blockY - 0.5f;
                float dz = currentPos.z - blockZ - 0.5f;

                Direction face;

                float absDx = Math.abs(dx);
                float absDy = Math.abs(dy);
                float absDz = Math.abs(dz);

                if (absDx > absDy && absDx > absDz) {
                    face = dx > 0 ? Direction.EAST : Direction.WEST;
                } else if (absDy > absDx && absDy > absDz) {
                    face = dy > 0 ? Direction.UP : Direction.DOWN;
                } else {
                    face = dz > 0 ? Direction.SOUTH : Direction.NORTH;
                }

                return new BlockHitResult(pos, face);
            }
        }
        return null;
    }

    private void configureKeyBinds() {
        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                if (button == GLFW_MOUSE_BUTTON_RIGHT) {
                    if (selected != null) {
                        BlockPos placePos = selected.getFace().offset(selected.getPos());
                        world.setBlock(BuiltInRegistries.BLOCK_REGISTRY.getAll().get(selectedBlock), placePos, BlockAction.UPDATE);
                    }
                } else if (button == GLFW_MOUSE_BUTTON_LEFT) {
                    if (selected != null) {
                        world.setBlock(null, selected.getPos(), BlockAction.UPDATE);
                    }
                }
            }
        });

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

            float cameraSpeed = 64f * deltaTime;
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


        // Cycle selected block
        keybindRegistry.register((key, scancode, action, mods) -> {
            if ((action == GLFW_PRESS || action == GLFW_REPEAT) && key == GLFW_KEY_M) {
                selectedBlock = (selectedBlock + 1) % BuiltInRegistries.BLOCK_REGISTRY.getAll().size();
                hudCubeRenderer.get().setActiveBlock(BuiltInRegistries.BLOCK_REGISTRY.getAll().get(selectedBlock));
                return true;
            }
            return false;
        }, 0);
    }
}
