package org.mangorage.game.world;

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
}