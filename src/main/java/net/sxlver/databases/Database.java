package net.sxlver.databases;

import lombok.NonNull;
import net.sxlver.databases.exception.DatabaseException;
import net.sxlver.databases.handler.OnLoadMessageReceiver;
import net.sxlver.databases.handler.OnLoadReceiverSupplier;
import net.sxlver.databases.impl.json.JSONDatabase;
import net.sxlver.databases.impl.mongodb.MongoDBDatabase;
import net.sxlver.databases.impl.mysql.MySQLDatabase;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * An abstraction of the underlying database logic that can have very
 * different behaviors.
 *
 * <p>It is advised to use this class as interface when interacting with
 * it's underlying implementations. Instead of instantiating them directly,
 * use the {@code ofType()} method provided by this class.
 *
 * <p>If writing an implementation for this class, it is advised
 * to extend {@link AbstractDatabase} instead of implementing
 * this interface directly.
 *
 * @param <T> describes the type of object the database will work with
 * @author Sxlver
 */
public interface Database<T> {

    /**
     * Takes an instance of {@code T} and attempts to write it to the database.
     *
     * <p>Note that underlying implementations (including the default implementations)
     * may require database queries to be ran in an async context and could throw
     * and exception when being invoked from the main-thread.
     *
     * <p>Invoking the method with a {@code null} parameter will throw a NullPointerException.
     *
     * @param object the object that should be written to the database, not null
     * @throws NullPointerException if the object parameter is null
     */
    void save(final @NonNull T object);

    /**
     * Takes an instance of {@code T} and attempts to write it to the database
     * in an async context
     *
     * <p>The default implementation uses the same logic as {@link #save(Object)}
     * to save the object and shares a lot of it's behavior besides being ran
     * in an async context.
     *
     * The state of the operation (whether the operation was completed) can be checked
     * via {@link CompletableFuture#isDone()} if necessary.
     *
     * @param object the object that should be written to the database, not null
     * @return the CompletableFuture instance used to save the object async, never null
     */
    @NonNull
    CompletableFuture<Void> saveAsync(final @NonNull T object);

    /**
     * Gets data from the database matching the provided query and deserializes
     * them to {@code T}
     *
     * <p>The type of query the underlying implementation accepts and works with
     * can vary from implementation to implementation. Generally speaking, a
     * {@code String} unique id that can be any unique string is accepted by
     * every default implementation. Some implementations may as well accept
     * other types of queries.
     *
     * <p>Should only be used if {@code T} only has one unique identifier or all unique
     * identifiers are provided as otherwise implementations may encounter errors.
     *
     * <p>
     * May return {@code null} if no data matching the given query exist in the db.
     *
     * @param query the query identifying the data to retrieve, not null
     * @return the deserialized object matching the given query or null
     *         if no data matching the query could be fetched
     */
    @Nullable
    T get(final @NonNull Object query);

    /**
     * Gets all data from the database that matches the given query, deserializes
     * them to {@code T} and returns them stored in any {@code Collection}.
     *
     * <p>For more information on queries supported by default implementations
     * see the doc of {@link #get(Object)}.
     *
     * <p>Can be used to fetch multiple database entries that match a given query
     * if entries are identified by multiple unique ids. For example, when a
     * class model has multiple fields annotated with {@code @UniqueIdentifier}
     *
     * <p>Will never return null, if no data matching the given query can be found
     * an empty Collection will be returned.
     *
     * @param query the query identifying the data to retrieve, not null
     * @return the deserialized object(s) matching the given query stored in
     *         a collection that will never be null
     */
    @NonNull
    Collection<T> getAll(final @NonNull Object query);

    /**
     * Gets all data from the database deserializes them to {@code T} and
     * returns them stored in any {@code Collection}.
     *
     * @return the deserialized object(s), never null
     */
    @NonNull
    Collection<T> getAll();

    /**
     * Removes a database entry matching the given query from the database.
     *
     * @apiNote It is important that all the unique identifiers identifying
     *          the particular object you want to remove from the database
     *          are provided as otherwise, depending on the implementation
     *          other data might be deleted as well.
     *
     * @param query the query identifying the data to remove, not null
     */
    void remove(final @NonNull Object query);

    /**
     * This method returns whether a connection to the database has been
     * successfully established.
     *
     * <p>Note that depending on the implementation, it might always return a constant,
     * value. For example there will be no connection required for flat file storages
     * which is why the return value of this method strongly depends on it's underlying
     * implementation.
     *
     * <p>As for the default implementations, this method can be used to determine whether
     * a connection to the databases of the type {@link DatabaseTypes#MONGODB} or
     * {@link DatabaseTypes#MYSQL} has successfully been opened.
     *
     * @return whether a open connection to the database exists
     */
    boolean isConnected();

    /**
     * Instantiates a implementation of this class depending on the return
     * value of the getType() method in the provided DatabaseSettings.
     *
     * <p>Returns the implementation corresponding to the given {@link DatabaseTypes}
     * and throws a {@code DatabaseException} if the DatabaseType doesn't have an
     * existing implementation.
     *
     * @param classType the parameter type of {@code T}, not null
     * @param settings the database settings to parse connection information from, not null
     * @return the implementation of this class for the given DatabaseType, never null
     */
    static <T> Database<T> ofType(final Class<T> classType, final DatabaseSettings settings) {
        return ofType(classType, settings, false, null);
    }

    /**
     * Instantiates a implementation of this class depending on the return
     * value of the getType() method in the provided DatabaseSettings.
     *
     * <p>The only big difference to {@link #ofType(Class, DatabaseSettings)}
     * is that this method lets you define a Thread, which will represent a
     * thread database operations will be disallowed on, and a boolean which
     * will determine whether this particular features is enabled or not.
     *
     * @param classType the parameter type of {@code T}, not null
     * @param settings the database settings to parse connection information from, not null
     * @param catchMainThread whether database operations on the give thread should be allowed
     * @param thread the thread that database operations will be cancelled on, not null
     * @return the implementation of this class for the given DatabaseType, never null
     * @see #ofType(Class, DatabaseSettings)
     */
    static <T> Database<T> ofType(final Class<T>         classType,
                                  final DatabaseSettings settings,
                                  final boolean          catchMainThread,
                                  final Thread           thread
    ) {
        final DatabaseTypes type = settings.getDatabaseType();
        switch(type) {
            case MYSQL: return MySQLDatabase.of(classType, settings, catchMainThread, thread);
            case JSON: return JSONDatabase.of(classType, settings, catchMainThread, thread);
            case MONGODB: return MongoDBDatabase.of(classType, settings, catchMainThread, thread);
        }
        throw new DatabaseException("No database exists for type " + type);
    }

    /**
     * Calls a {@code OnLoadMessageReceiver} when data that belong to him have been loaded.
     *
     * <p>the {@link OnLoadMessageReceiver} is supplied by the {@link OnLoadReceiverSupplier}
     * and will be invoked on instantiation of the supplier.
     *
     * @param object the message receiver, not null
     */
    default void callMessageReceiver(final T object) {
        if(object instanceof OnLoadReceiverSupplier) {
            final OnLoadReceiverSupplier<OnLoadMessageReceiver> messageSupplier = (OnLoadReceiverSupplier<OnLoadMessageReceiver>) object;
            final OnLoadMessageReceiver messageReceiver = messageSupplier.getMessageHandler();
            if(messageReceiver != null) {
                messageReceiver.onLoad(object);
            }
        }
    }
}
