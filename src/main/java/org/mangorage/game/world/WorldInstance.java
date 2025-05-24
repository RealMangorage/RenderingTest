package org.mangorage.game.world;

import org.joml.Matrix4f;
import org.mangorage.game.block.Block;
import org.mangorage.game.core.Direction;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class WorldInstance {
    private final Map<BlockPos, Block> blocks = new HashMap<>();
    private final Map<BlockPos, Block> blocks_view = Collections.unmodifiableMap(blocks);

    public void setBlock(Block block, BlockPos blockPos) {
        blocks.put(blockPos, block);
    }

    public Block getBlock(BlockPos blockPos) {
        return blocks.get(blockPos);
    }

    public void removeBlock(BlockPos pos) {
        blocks.remove(pos);
    }

    public void render(Matrix4f view, Matrix4f projection) {
        blocks_view.forEach((pos, block) -> {
            boolean[] visibleFaces = new boolean[6];
            int facesHidden = 0;

            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = dir.offset(pos);
                Block neighbor = getBlock(neighborPos);

                visibleFaces[dir.getFaceIndex()] = (neighbor == null || !neighbor.isSolid());
                if (!visibleFaces[dir.getFaceIndex()]) {
                    facesHidden++;
                }
            }

            if (facesHidden != 6) {
//                Matrix4f model = new Matrix4f().translate(pos.x(), pos.y(), pos.z());
//                block.getRenderer().render(model, view, projection);
            }
        });
    }
}
