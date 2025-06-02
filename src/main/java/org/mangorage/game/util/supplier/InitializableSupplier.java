package org.mangorage.game.util.supplier;

import java.util.function.Supplier;

public interface InitializableSupplier<T> extends Supplier<T> {
    static <T> InitializableSupplier<T> of(Supplier<T> supplier) {
        return new DefaultInitializableSupplier<>(supplier, false);
    }

    static <T> InitializableSupplier<T> ofAuto(Supplier<T> supplier) {
        return new DefaultInitializableSupplier<>(supplier, true);
    }

    void init();
    boolean isLoaded();
}
