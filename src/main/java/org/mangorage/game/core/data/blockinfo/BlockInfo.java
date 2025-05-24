package org.mangorage.game.core.data;

import com.google.gson.Gson;
import org.mangorage.game.core.Side;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public sealed class BlockInfo permits MissingBlockInfo {
    private static final Gson GSON = new Gson();

    private static final BlockInfo MISSING = new BlockInfo();

    public static BlockInfo load(String name) {
        try (var is = BlockInfo.class.getClassLoader().getResourceAsStream("assets/models/" + name + ".json")) {
            if (is == null)
                return MISSING;
            return GSON.fromJson(
                    new InputStreamReader(is),
                    BlockInfo.class
            );
        } catch (IOException ignored) {
            return MISSING;
        }
    }

    private final String allTexture = null;
    private final Map<Side, String> textures = new HashMap<>();

    public String getTexture(Side side) {
        if (allTexture != null)
            return "assets/textures/blocks/" + allTexture;
        if (textures.containsKey(side))
            return "assets/textures/blocks/" + textures.get(side);
        return "assets/textures/misc/missing.png";
    }
}
