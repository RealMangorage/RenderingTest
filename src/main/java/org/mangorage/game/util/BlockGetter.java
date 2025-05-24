package org.mangorage.game.util;

import org.mangorage.game.block.Block;

@FunctionalInterface
public interface BlockGetter {
    Block getBlock(int x, int y, int z);
}
