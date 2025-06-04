package org.mangorage.game.core;

import org.mangorage.game.block.AirBlock;
import org.mangorage.game.block.Block;
import org.mangorage.game.block.GrassBlock;
import org.mangorage.game.block.SlabBlock;
import org.mangorage.game.block.StairBlock;
import org.mangorage.game.core.registry.DefaultedRegistry;
import org.mangorage.game.core.registry.Registry;

public final class BuiltInRegistries {
    public static final Registry<Block> BLOCK_REGISTRY = new DefaultedRegistry<>("air");

    public static final Block AIR_BLOCK = BLOCK_REGISTRY.register("air", new AirBlock());

    public static final Block DIAMOND_BLOCK = BLOCK_REGISTRY.register("diamond_block", new Block());
    public static final Block GRASS_BLOCK = BLOCK_REGISTRY.register("grass_block", new GrassBlock());
    public static final Block DIRT_BLOCK = BLOCK_REGISTRY.register("dirt_block", new Block());
    public static final Block STONE_BLOCK = BLOCK_REGISTRY.register("stone_block", new Block());

    public static final Block SLAB_BLOCK = BLOCK_REGISTRY.register("dirt_slab_block", new SlabBlock());
    public static final Block STAIR_BLOCK = BLOCK_REGISTRY.register("diamond_stair_block", new StairBlock());

    public static void init() {}
}
