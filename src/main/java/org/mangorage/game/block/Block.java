package org.mangorage.game.block;

import org.mangorage.game.core.BuiltInRegistries;
import org.mangorage.game.core.Direction;
import org.mangorage.game.core.data.blockinfo.BlockInfo;
import org.mangorage.game.renderer.block.BlockRenderer;
import org.mangorage.game.renderer.block.SimpleBlockRenderer;
import org.mangorage.game.util.RenderUtil;
import org.mangorage.game.util.supplier.InitializableSupplier;

public class Block {
    private static final SimpleBlockRenderer simpleBlockRenderer = new SimpleBlockRenderer();

    private static final float[] DEFAULT_TINT = new float[]{1f, 1f, 1f};;

    // The default cube shape with interleaved position (xyz) and texture coordinates (uv)
    // Each face is composed of two triangles, forming a quad.
    // Vertices are defined in counter-clockwise order when looking at the face from the outside.
    private static final float[][][] DEFAULT_SHAPE = new float[][][] {
            // UP (Y=1) - Direction.UP.ordinal() = 0
            // Corners: P2(0,1,0), P3(1,1,0), P6(0,1,1), P7(1,1,1)
            {
                    {0, 1, 0}, {0, 1, 1}, {1, 1, 1},  // Triangle 1: P2, P6, P7 (Top-Back-Left, Top-Front-Left, Top-Front-Right)
                    {0, 1, 0}, {1, 1, 1}, {1, 1, 0}   // Triangle 2: P2, P7, P3 (Top-Back-Left, Top-Front-Right, Top-Back-Right)
            },
            // DOWN (Y=0) - Direction.DOWN.ordinal() = 1
            // Corners: P0(0,0,0), P1(1,0,0), P4(0,0,1), P5(1,0,1)
            // Winding order must be CW from top perspective to be CCW from bottom perspective
            {
                    {0, 0, 0}, {1, 0, 0}, {1, 0, 1},  // Triangle 1: P0, P1, P5 (Bottom-Back-Left, Bottom-Back-Right, Bottom-Front-Right)
                    {0, 0, 0}, {1, 0, 1}, {0, 0, 1}   // Triangle 2: P0, P5, P4 (Bottom-Back-Left, Bottom-Front-Right, Bottom-Front-Left)
            },
            // NORTH (Z=0) - Direction.NORTH.ordinal() = 2 (The "back" face, looking from positive Z towards negative Z)
            // Corners: P0(0,0,0), P1(1,0,0), P2(0,1,0), P3(1,1,0)
            {
                    {0, 0, 0}, {0, 1, 0}, {1, 1, 0},  // Triangle 1: P0, P2, P3 (Bottom-Back-Left, Top-Back-Left, Top-Back-Right)
                    {0, 0, 0}, {1, 1, 0}, {1, 0, 0}   // Triangle 2: P0, P3, P1 (Bottom-Back-Left, Top-Back-Right, Bottom-Back-Right)
            },
            // SOUTH (Z=1) - Direction.SOUTH.ordinal() = 3 (The "front" face, looking from negative Z towards positive Z)
            // Corners: P4(0,0,1), P5(1,0,1), P6(0,1,1), P7(1,1,1)
            {
                    {0, 0, 1}, {1, 0, 1}, {1, 1, 1},  // Triangle 1: P4, P5, P7 (Bottom-Front-Left, Bottom-Front-Right, Top-Front-Right)
                    {0, 0, 1}, {1, 1, 1}, {0, 1, 1}   // Triangle 2: P4, P7, P6 (Bottom-Front-Left, Top-Front-Right, Top-Front-Left)
            },
            // WEST (X=0) - Direction.WEST.ordinal() = 5 (The "left" face, looking from positive X towards negative X)
            // Corners: P0(0,0,0), P2(0,1,0), P4(0,0,1), P6(0,1,1)
            {
                    {0, 0, 0}, {0, 0, 1}, {0, 1, 1},  // Triangle 1: P0, P4, P6 (Bottom-Back-Left, Bottom-Front-Left, Top-Front-Left)
                    {0, 0, 0}, {0, 1, 1}, {0, 1, 0}   // Triangle 2: P0, P6, P2 (Bottom-Back-Left, Top-Front-Left, Top-Back-Left)
            },
            // EAST (X=1) - Direction.EAST.ordinal() = 4 (The "right" face, looking from negative X towards positive X)
            // Corners: P1(1,0,0), P3(1,1,0), P5(1,0,1), P7(1,1,1)
            {
                    {1, 0, 0}, {1, 1, 0}, {1, 1, 1},  // Triangle 1: P1, P3, P7 (Bottom-Back-Right, Top-Back-Right, Top-Front-Right)
                    {1, 0, 0}, {1, 1, 1}, {1, 0, 1}   // Triangle 2: P1, P7, P5 (Bottom-Back-Right, Top-Front-Right, Bottom-Front-Right)
            },
    };

    private final InitializableSupplier<String> name = InitializableSupplier.ofAuto(() -> BuiltInRegistries.BLOCK_REGISTRY.getId(this));
    private final InitializableSupplier<BlockInfo> blockInfo = InitializableSupplier.ofAuto(() -> BlockInfo.load(getName()));

    public Block() {
    }

    public final String getName() {
        return name.get();
    }

    public final BlockInfo getBlockInfo() {
        return blockInfo.get();
    }

    public boolean isSolid() {
        return true;
    }

    public boolean isAir() {
        return false;
    }

    public float[][][] getShape() {
        return DEFAULT_SHAPE;
    }

    public float[] getTint(Direction face, int layer) {
        return RenderUtil.adjustForBrightness(DEFAULT_TINT, face);
    }

    public BlockRenderer getRenderer() {
        return simpleBlockRenderer;
    }
}
