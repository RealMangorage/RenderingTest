package org.mangorage.game.block;

public final class AirBlock extends Block {
    public AirBlock(String name) {
        super(name);
    }

    @Override
    public boolean isAir() {
        return true;
    }
}
