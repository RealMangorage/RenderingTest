package org.mangorage.game.core;

import org.mangorage.game.block.AirBlock;
import org.mangorage.game.block.Block;
import org.mangorage.game.block.GrassBlock;

public final class Blocks {
    public static final Block AIR_BLOCK = new AirBlock("air");
    public static final Block DIAMOND_BLOCK = new Block("diamond_block");
    public static final Block GRASS_BLOCK = new GrassBlock("grass_block");

    public static void init() {}
}
