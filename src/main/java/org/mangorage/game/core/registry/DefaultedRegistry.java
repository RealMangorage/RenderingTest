package org.mangorage.game.core.registry;

import org.mangorage.game.block.Block;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DefaultedRegistry<T> implements Registry<T> {
    private final String defaultId;

    private final Map<String, T> entries = new HashMap<>();
    private final Map<T, String> entries_reverse = new HashMap<>();
    private final Map<String, Integer> entries_byId = new HashMap<>();
    private final Map<Integer, T> entries_byId_reverse = new HashMap<>();

    private volatile boolean frozen = false;

    public DefaultedRegistry(String id) {
        this.defaultId = id;
    }

    @Override
    public void freeze() {
        frozen = true;
    }

    void check() {
        if (frozen)
            throw new IllegalStateException("Cant register to a frozen registry...");
    }

    @Override
    public T register(String id, T object) {
        check();
        final int internalId = entries_byId.size() + 1;
        entries_byId.put(id, internalId);
        entries_byId_reverse.put(internalId, object);
        entries_reverse.put(object, id);
        entries.put(id, object);
        return object;
    }

    @Override
    public T get(String id) {
        return entries
                .getOrDefault(
                        id,
                        entries.get(defaultId)
                );
    }

    @Override
    public String getId(T value) {
        return entries_reverse.getOrDefault(value, defaultId);
    }

    @Override
    public int getInternalId(String id) {
        return entries_byId.get(id);
    }

    @Override
    public int getInternalId(T value) {
        return getInternalId(getId(value));
    }

    @Override
    public int getDefaultInternalId() {
        return entries_byId.get(defaultId);
    }

    @Override
    public T getByInternalId(int internalId) {
        return entries_byId_reverse.get(internalId);
    }

    @Override
    public List<T> getAll() {
        return entries.values().stream().toList();
    }
}
