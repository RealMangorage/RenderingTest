package org.mangorage.game.core.registry;

import org.mangorage.game.block.Block;

import java.util.List;

public sealed interface Registry<T> permits DefaultedRegistry {
    void freeze();
    T register(String id, T object);

    T get(String id);

    String getId(T value);

    int getInternalId(String id);
    int getInternalId(T value);
    int getDefaultInternalId();

    T getByInternalId(int internalId);

    List<T> getAll();
}
