package org.mangorage.game.util.supplier;

import java.util.function.Supplier;

final class DefaultInitializableSupplier<T> implements InitializableSupplier<T> {
    private final Supplier<T> supplier;
    private final boolean auto;
    private volatile T value;

    DefaultInitializableSupplier(Supplier<T> supplier, boolean auto) {
        this.supplier = supplier;
        this.auto = auto;
    }

    @Override
    public void init() {
        if (this.value != null) return;
        this.value = supplier.get();
    }

    @Override
    public boolean isLoaded() {
        return value != null;
    }

    @Override
    public T get() {
        if (auto && !isLoaded()) init();
        return value;
    }
}
