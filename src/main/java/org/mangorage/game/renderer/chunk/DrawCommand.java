package org.mangorage.game.renderer.chunk;

import java.util.function.Consumer;

public record DrawCommand(int textureId, int startIndex, int vertexCount, float[] tint, Consumer<Boolean> extra) { }
