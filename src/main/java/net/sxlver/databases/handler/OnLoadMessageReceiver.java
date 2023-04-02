package net.sxlver.databases.handler;

import lombok.NonNull;

import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;

public interface OnLoadMessageReceiver extends Function<Object, Boolean> {

    boolean onLoad(final @NonNull Object object);

    @Override
    default Boolean apply(final @NonNull Object object) {
        return onLoad(object);
    }
}
