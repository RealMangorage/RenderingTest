package org.mangorage.game.renderer.block;

import org.mangorage.game.block.Block;
import org.mangorage.game.core.Direction;
import org.mangorage.game.renderer.chunk.DrawCommand;

import java.util.EnumMap;
import java.util.List;

public abstract class BlockRenderer {
    abstract public void render(List<DrawCommand> drawCommands, List<Float> vertices, Block block, int x, int y, int z, EnumMap<Direction, Block> neighbors, AssetLoader assetLoader);
}
