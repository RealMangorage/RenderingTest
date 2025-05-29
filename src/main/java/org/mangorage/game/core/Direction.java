package org.mangorage.game.core;

import org.joml.Vector3f;
import org.mangorage.game.world.BlockPos;

public enum Direction {
    UP(0, 1, 0, 4),
    DOWN(0, -1, 0, 5),
    NORTH(0, 0, -1, 1),
    SOUTH(0, 0, 1, 0),
    WEST(-1, 0, 0, 2),
    EAST(1, 0, 0, 3);

    public final int x, y, z, faceIndex;

    Direction(int x, int y, int z, int faceIndex) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.faceIndex = faceIndex;
    }

    public int getFaceIndex() {
        return faceIndex;
    }

    public BlockPos offset(BlockPos pos) {
        return new BlockPos(pos.x() + x, pos.y() + y, pos.z() + z);
    }

    public Direction getOpposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST -> WEST;
            case WEST -> EAST;
            case UP -> DOWN;
            case DOWN -> UP;
        };
    }

    public static Direction fromFacingVector(Vector3f facing) {
        float x = facing.x;
        float y = facing.y;
        float z = facing.z;

        float absX = Math.abs(x);
        float absY = Math.abs(y);
        float absZ = Math.abs(z);

        if (absX > absY && absX > absZ) {
            // Dominant axis is X
            return x > 0 ? Direction.EAST : Direction.WEST;
        } else if (absZ > absX && absZ > absY) {
            // Dominant axis is Z
            return z > 0 ? Direction.SOUTH : Direction.NORTH;
        } else {
            // Dominant axis is Y (looking up or down)
            return y > 0 ? Direction.UP : Direction.DOWN;
        }
    }
}