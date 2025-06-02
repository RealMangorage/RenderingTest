package org.mangorage.game.util.supplier;

import java.util.function.Supplier;

public interface InitializableSupplier<T> extends Supplier<T> {
    static <T> InitializableSupplier<T> of(Supplier<T> supplier) {
        return new DefaultInitializableSupplier<>(supplier);
    }

    void init();
    boolean isLoaded();
}
