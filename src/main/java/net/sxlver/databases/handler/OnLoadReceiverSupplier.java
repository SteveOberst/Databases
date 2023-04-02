package net.sxlver.databases.handler;

import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public interface OnLoadReceiverSupplier<T extends OnLoadMessageReceiver> extends Supplier<T> {

    T getMessageHandler();

    @Override
    default T get() {
        return getMessageHandler();
    }
}
