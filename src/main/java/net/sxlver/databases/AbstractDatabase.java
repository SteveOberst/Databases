package net.sxlver.databases;

import com.google.common.collect.Maps;
import com.google.gson.*;
import lombok.NonNull;
import net.sxlver.databases.adapter.strategy.CustomToNumberPolicy;
import net.sxlver.databases.impl.CustomTypeAdapterFactory;
import net.sxlver.databases.util.Reflection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static net.sxlver.databases.util.Validator.checkAutoIncrementFieldIsInteger;

public abstract class AbstractDatabase<T> implements Database<T> {

    @Nullable
    private final Thread thread;
    private final boolean catchMainThread;

    protected final Class<?> type;
    private final GsonBuilder gsonBuilder;
    private Gson gson;

    protected static final Map<Database<?>, Integer> ID_MAP = Maps.newConcurrentMap();

    public AbstractDatabase(final Class<?> type, final boolean catchMainThread, final @Nullable Thread thread) {
        this.type = type;
        this.catchMainThread = catchMainThread;
        this.thread = thread;

        this.gsonBuilder = new GsonBuilder()
                .enableComplexMapKeySerialization()
                .disableInnerClassSerialization()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .setObjectToNumberStrategy(CustomToNumberPolicy.INT_LONG_DOUBLE)
                .registerTypeAdapterFactory(new CustomTypeAdapterFactory());

        this.gson = gsonBuilder.create();

    }

    protected void init() {
        ID_MAP.put(this, nextId(true) - 1);
    }

    @Override
    public @Nullable T get(final @NonNull Object query) {
        if(isCatchMainThread()) {
            catchMainThread();
        }
        return read(query);
    }

    @Override
    public void save(final @NonNull T object) {
        if(isCatchMainThread()) {
            catchMainThread();
        }
        applyAttributes(object);
        write(object);
    }

    @Override
    public @NonNull Collection<T> getAll(final @NonNull Object query) {
        if(isCatchMainThread()) {
            catchMainThread();
        }
        return readAll(query);
    }

    @Override
    public @NonNull Collection<T> getAll() {
        if(isCatchMainThread()) {
            catchMainThread();
        }
        return readAll();
    }

    @Override
    public @NotNull CompletableFuture<Void> saveAsync(final @NotNull T object) {
        return CompletableFuture.runAsync(() -> {
            save(object);
        });
    }

    @Override
    public void remove(final @NonNull Object query) {
        if(isCatchMainThread()) {
            catchMainThread();
        }
        delete(query);
    }

    protected abstract T read(final Object query);

    protected abstract void write(final T Object);

    protected abstract Collection<T> readAll(final Object query);

    protected abstract Collection<T> readAll();

    protected abstract void delete(final Object query);

    protected abstract void connect(final @NonNull DatabaseAuth auth);

    protected abstract ClassInfo getClassInfo();

    public <T> void registerTypeAdapter(final Type type, final TypeAdapter<T> adapter) {
        gsonBuilder.registerTypeAdapter(type, adapter);
        updateGson();
    }

    protected int nextId(final boolean fetch) {
        int nextId = getCurrentId() + 1;
        if(fetch) {
            nextId = fetchMaxId() + 1;
        }
        ID_MAP.put(this, nextId);
        return nextId;
    }

    protected void applyAttributes(final T instance) {
        final Field field = Reflection.getAutoIncrementField(type, getClassInfo());
        if(field != null) {
            checkAutoIncrementFieldIsInteger(field);
            final Object value = Reflection.getValue(field, instance);
            if ((Integer) value == 0) {
                final int nextId = nextId(false);
                Reflection.setValue(field, instance, nextId);
            }
        }
    }

    protected int getCurrentId() {
        return ID_MAP.computeIfAbsent(this, database -> fetchMaxId());
    }

    protected abstract int fetchMaxId();

    protected void updateGson() {
        this.gson = gsonBuilder.create();
    }

    @Nullable
    public Thread getCatchingThread() {
        return thread;
    }

    public boolean isCatchMainThread() {
        return catchMainThread;
    }

    protected void catchMainThread() {
        if(isPrimaryThread()) {
            throw new IllegalStateException("Synchronous database access.");
        }
    }

    protected Gson getGson() {
        return gson;
    }

    private boolean isPrimaryThread() {
        return Thread.currentThread().equals(thread);
    }

    public Class<?> getType() {
        return type;
    }
}
