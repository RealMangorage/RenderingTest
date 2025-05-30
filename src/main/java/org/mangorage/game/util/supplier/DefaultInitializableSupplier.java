package org.mangorage.game.util.supplier;

import java.util.function.Supplier;

final class DefaultInitializableSupplier<T> implements InitializableSupplier<T> {
    private final Supplier<T> supplier;
    private volatile T value;

    DefaultInitializableSupplier(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public void init() {
        if (this.value != null) return;
        this.value = supplier.get();
    }

    @Override
    public T get() {
        return value;
    }
}
