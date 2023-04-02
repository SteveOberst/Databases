package net.sxlver.databases.impl.mysql;

import lombok.NonNull;
import lombok.SneakyThrows;
import net.sxlver.databases.*;
import net.sxlver.databases.exception.DatabaseException;
import net.sxlver.databases.formatter.FieldNameFormatter;
import net.sxlver.databases.formatter.FieldNameFormatters;
import net.sxlver.databases.impl.DatabaseEntryIdentifier;
import net.sxlver.databases.impl.mysql.type.TypeMappings;
import net.sxlver.databases.util.Reflection;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static net.sxlver.databases.util.Validator.checkValidTableStructure;

public class MySQLDatabase<T> extends AbstractDatabase<T> {
    private final DatabaseSettings settings;

    private final MySQLDatabaseHandler<T> databaseHandler;

    private final FieldNameFormatter formatter;
    private final ClassInfo classInfo;
    private final String tableName;

    private Connection connection;

    /**
     * Constructs the MySQLDatabase and automatically opens a connection
     * given the information provided by the {@link DatabaseSettings}
     *
     * @param type the object the database is dealing with
     * @param settings the database settings
     */
    public MySQLDatabase(final @NonNull Class<?> type, final @NonNull DatabaseSettings settings
    ) {
        this(type, settings, FieldNameFormatters.LOWER_SNAKE);
    }

    /**
     * Constructs the MySQLDatabase and automatically opens a connection
     * given the information provided by the {@link DatabaseSettings}
     *
     * @param type the object the database is dealing with
     * @param settings the database settings
     * @param formatter the formatter converting the field names
     */
    public MySQLDatabase(final @NonNull Class<?>           type,
                         final @NonNull DatabaseSettings   settings,
                         final @NonNull FieldNameFormatter formatter
    ) {
        this(type, settings, false, null, formatter);
    }

    /**
     * Constructs the MySQLDatabase and automatically opens a connection
     * given the information provided by the {@link DatabaseSettings}
     *
     * @param type the object the database is dealing with
     * @param settings the database settings
     * @param catchMainThread whether to throw an exception if executed on the defined thread
     * @param thread thread db operations should not be allowed on
     */
    public MySQLDatabase(final @NonNull Class<?>           type,
                         final @NonNull DatabaseSettings   settings,
                         final boolean                     catchMainThread,
                         final @Nullable Thread            thread,
                         final @NonNull FieldNameFormatter formatter
    ) {
        super(type, catchMainThread, thread);
        this.settings = settings;
        this.classInfo = ClassInfo.ofClass(type, formatter);
        this.databaseHandler = new MySQLDatabaseHandler<>(this, (DatabaseConverter<T, Map<String, Object>>) classInfo.getConverter());
        this.formatter = formatter;
        this.tableName = formatter.apply(Reflection.getTableName(type));
        connect(new MySQLDatabaseAuthentication(settings));
        super.init();
    }

    /**
     * Serializes an object using it's corresponding {@link DatabaseConverter}
     * and inserts it into or updates it's state in the database
     *
     * @param object the object to write to the database
     */
    @Override
    protected void write(final @NonNull T object) {
        databaseHandler.save(object);
    }

    /**
     * Gets a object from the database by either a unique identifier or
     * a custom {@link SQLQuery} that can be passed to this method.
     *
     * @param query an instance of {@link SQLQuery} or any unique identifier.
     * @return the fetched object or {@code null} if the operation wasn't successful;
     */
    @Override
    protected T read(final @NonNull Object query) {
        if(query instanceof SQLQuery) {
            return getFromQuery((SQLQuery) query);
        }
        return getFromUniqueIdentifier(query.toString());
    }

    /**
     * Retrieves data from the db using the provided custom query
     * and returns the deserialized data as an instance of {@link T}.
     *
     * @param query the query to run on the database
     * @return the deserialized object
     */
    private T getFromQuery(final @NonNull SQLQuery query) {
        return databaseHandler.get(query);
    }

    /**
     * Retrieves data from the db that match the provided unique id
     * and returns the deserialized data as an instance of {@link T}.
     *
     * @param uniqueId the unique identifier for the object
     * @return the deserialized object
     */
    private T getFromUniqueIdentifier(final @NonNull Object uniqueId) {
        return databaseHandler.get(uniqueId.toString().split(DatabaseEntryIdentifier.DELIMITER));
    }


    /**
     * Gets all objects from the database that match either a unique identifier or
     * a custom {@link SQLQuery} that can be passed to this method.
     *
     * @param query an instance of {@link SQLQuery} or any unique identifier.
     * @return the fetched object or {@code null} if the operation wasn't successful;
     */
    @Override
    @NonNull
    public Collection<T> readAll(final @NonNull Object query) {
        if(query instanceof SQLQuery) {
            return getAllFromQuery((SQLQuery) query);
        }
        return getAllFromUniqueIdentifier(query);
    }

    /**
     * Retrieves all the data from the db that match the provided custom query and returns
     * the deserialized data in a {@link Collection} as an instance of {@link T}.
     *
     * @param query the query to run on the database
     * @return A {@link Collection} of deserialized data matching the provided query.
     */
    @NonNull
    private Collection<T> getAllFromQuery(final @NonNull SQLQuery query) {
        return databaseHandler.getAll(query);
    }

    /**
     * Retrieves all the data from the db that match the provided unique id and returns
     * the deserialized data in a {@link Collection} as an instance of {@link T}.
     *
     * @param uniqueId the unique identifier(s) for the objects
     * @return A {@link Collection} of deserialized data matching the provided unique id.
     */
    @NonNull
    private Collection<T> getAllFromUniqueIdentifier(final @NonNull Object uniqueId) {
        return databaseHandler.getAll(uniqueId.toString().split(DatabaseEntryIdentifier.DELIMITER));
    }

    /**
     * Reads all data from the db, deserializes them to {@link T} and
     * returns them in a {@link Collection}
     *
     * @return All data stored in the db in a {@link Collection} of {@link T}s
     */
    @Override
    protected Collection<T> readAll() {
        return databaseHandler.getAll();
    }

    @Override
    protected void delete(final @NonNull Object query) {
        if(query instanceof SQLQuery) {
            ((SQLQuery) query).update();
        }
        databaseHandler.remove(query.toString().split(DatabaseEntryIdentifier.DELIMITER));
    }

    /**
     * Fetches the next available id for an auto increment field
     * from the database.
     *
     * @return The next available id.
     */
    @Override
    @SneakyThrows
    protected int fetchMaxId() {
        final Field autoIncrementField = Reflection.getAutoIncrementField(type, classInfo);
        if(autoIncrementField != null) {
            final ClassInfo.FieldInfo fieldInfo = classInfo.getInfo(autoIncrementField);
            final SQLQuery query = newStatementBuilder().selectMaxId(getTable(), fieldInfo.getFormattedName()).createQuery();
            final ResultSet result = query.execute();
            if(result.next()) {
                return result.getInt(fieldInfo.getFormattedName());
            }
        }
        return 0;
    }

    public static <T> MySQLDatabase<T> of(final Class<T> type, final DatabaseSettings settings) {
        return of(type, settings, false, null);
    }

    public static <T> MySQLDatabase<T> of(final Class<T>         type,
                                          final DatabaseSettings settings,
                                          final boolean          catchMainThread,
                                          final Thread           thread
    ) {
        return new MySQLDatabase<>(type, settings, catchMainThread, thread, FieldNameFormatters.LOWER_SNAKE);
    }

    public String getSchema() {
        return Reflection.getTableSchema(type, formatter, TypeMappings.DEFAULT);
    }

    SQLQuery newQuery(final String query) {
        return new SQLQuery(connection, query);
    }

    public SQLBuilder newStatementBuilder() {
        return new SQLBuilder(this);
    }

    @Override
    public boolean isConnected() {
        return connection != null;
    }

    protected ClassInfo getClassInfo() {
        return classInfo;
    }

    public String getTable() {
        return tableName;
    }

    @Override
    public void connect(final @NonNull DatabaseAuth auth) {
        final MySQLDatabaseAuthentication mySQLAuth = (MySQLDatabaseAuthentication) auth;
        try {
            connection = DriverManager.getConnection(
                    "jdbc:mysql://" +
                            mySQLAuth.getHost() + ":" +
                            mySQLAuth.getPort() + "/" +
                            mySQLAuth.getDatabase() + "?autoReconnect=true&characterEncoding=utf8&serverTimezone=UTC&useSSL=" +
                            mySQLAuth.isUseSSL(),
                    mySQLAuth.getUsername(),
                    mySQLAuth.getPassword()
            );
            createTable();
        } catch(final Exception exception) {
            throw new DatabaseException("Unable to open connection to MySQL database: " + exception.getMessage());
        }
    }

    private void createTable() {
        checkValidTableStructure(type);
        if(Reflection.hasCustomQuery(type)) {
            final String query = Reflection.getQuery(type);
            newQuery(query).execute();
        }
        final SQLQuery tableCreateQuery = newStatementBuilder()
                .createIfNotExists(
                        getTable(),
                        getSchema(),
                        getUniqueIdStructure()
                ).createQuery();

        tableCreateQuery.update();
    }

    private String getUniqueIdStructure() {
        final Field[] uniqueIds = getClassInfo().getUniqueIdentifiers();
        if (uniqueIds.length > 1) {
            return SQLBuilder.uniqueConstraint(
                    "identifier_constraint",
                    Arrays.stream(uniqueIds).map(Field::getName).toArray(String[]::new)
            );
        }
        return SQLBuilder.uniqueKey(formatter.apply(uniqueIds[0].getName()));
    }
}
