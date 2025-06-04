package org.mangorage.game.block;

public final class AirBlock extends Block {

    @Override
    public boolean isAir() {
        return true;
    }

    @Override
    public boolean isSolid() {
        return false;
    }
}
