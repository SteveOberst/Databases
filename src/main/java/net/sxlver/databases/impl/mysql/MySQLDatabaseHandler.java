package net.sxlver.databases.impl.mysql;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.sxlver.databases.ClassInfo;
import net.sxlver.databases.DatabaseConverter;
import net.sxlver.databases.impl.mysql.context.MySQLConversionContext;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class MySQLDatabaseHandler<T> {
    private final MySQLDatabase<T> database;
    private final DatabaseConverter<T, Map<String, Object>> converter;

    MySQLDatabaseHandler(final @NonNull MySQLDatabase<T> database,
                         final @NonNull DatabaseConverter<T, Map<String, Object>> converter
    ) {
        this.database = database;
        this.converter = converter;
    }

    /**
     * Retrieves data from the db that match the provided unique id
     * and returns the deserialized data as an instance of {@code T}.
     *
     * @param uniqueIds the unique identifier(s) for the object
     * @return the deserialized object, null if the requested data does not exist
     */
    @SneakyThrows
    public T get(final String... uniqueIds) {
        final Field[] uniqueIdentifiers = database.getClassInfo().getUniqueIdentifiers();
        final ResultSet result = database.newStatementBuilder()
                .select(database.getTable(), "*")
                .where(convertUniqueIds(uniqueIdentifiers, uniqueIds.length))
                .createQuery()
                .execute((Object[]) uniqueIds);

        if(!result.next())
            return null;

        final Map<String, Object> resultMap = mapFromResult(result);
        final T object = converter.deserialize(resultMap, MySQLConversionContext.of(database.getType()));
        database.callMessageReceiver(object);
        return object;
    }

    /**
     * Retrieves data from the db using the provided custom query
     * and returns the deserialized data as an instance of {@code T}.
     *
     * @param query the query to run on the database, not null
     * @return the deserialized object, null if the requested data does not exist
     */
    @SneakyThrows
    public T get(final SQLQuery query) {
        final ResultSet result = query.execute();
        if(!result.next())
            return null;

        final Map<String, Object> resultMap = mapFromResult(result);
        final T object = converter.deserialize(resultMap, MySQLConversionContext.of(database.getType()));
        database.callMessageReceiver(object);
        return object;
    }

    @SneakyThrows
    public Collection<T> getAll(final String... uniqueIds) {
        final ResultSet result = database.newStatementBuilder()
                .select(database.getTable(), "*")
                .where(convertUniqueIds(database.getClassInfo().getUniqueIdentifiers(), uniqueIds.length))
                .createQuery()
                .execute((Object[]) uniqueIds);

        final List<Map<String, Object>> mappedResults = Lists.newArrayList();
        while(result.next()) {
            mappedResults.add(mapFromResult(result));
        }
        final Collection<T> objects = mappedResults.stream().map(map
                -> converter.deserialize(map, MySQLConversionContext.of(database.getType())))
                .collect(Collectors.toList());

        for (final T object : objects) {
            database.callMessageReceiver(object);
        }
        return objects;
    }

    @SneakyThrows
    public Collection<T> getAll() {
        final ResultSet result = database.newStatementBuilder()
                .select(database.getTable(), "*")
                .createQuery()
                .execute();

        final List<Map<String, Object>> mappedResults = Lists.newArrayList();
        while(result.next()) {
            mappedResults.add(mapFromResult(result));
        }
        final Collection<T> objects = mappedResults.stream().map(map
                -> converter.deserialize(map, MySQLConversionContext.of(database.getType())))
                .collect(Collectors.toList());

        for (final T object : objects) {
            database.callMessageReceiver(object);
        }
        return objects;
    }

    /**
     * Gets all the data from the db that match the given query and deserializes them.
     *
     * @param query to execute on the database
     * @return All data that match the given query in a {@code Collection} of {@code T}s
     */
    @SneakyThrows
    public Collection<T> getAll(final SQLQuery query) {
        final ResultSet result = query.execute();
        final List<Map<String, Object>> mappedResults = Lists.newArrayList();
        while(result.next()) {
            mappedResults.add(mapFromResult(result));
        }
        final Collection<T> objects = mappedResults.stream().map(map
                -> converter.deserialize(map, MySQLConversionContext.of(database.getType())))
                .collect(Collectors.toList());

        for (final T object : objects) {
            database.callMessageReceiver(object);
        }
        return objects;
    }

    /**
     * Serializes an instance of {@code T} and writes it to the database.
     *
     * @param object object instance to write to the database, not null
     */
    public void save(final T object) {
        final Map<String, Object> serialized = converter.serialize(object, null);
        final SQLQuery query = database.newStatementBuilder()
                .insert(database.getTable(), mapToColumns(serialized))
                .updateOnDuplicateKey(mapToColumnsUpdate(serialized))
                .createQuery();

        // Duplicate the array due to the values being required once by
        // the INSERT and once again by the ON DUPLICATE KEY UPDATE statement
        final Object[] values = serialized.values().toArray();
        query.update(Stream.concat(Arrays.stream(values), Arrays.stream(values)).toArray(Object[]::new));
    }

    /**
     * Removes every object that matches the given unique identifier(s)
     * from the database.
     *
     * @param uniqueIds unique ids of the object(s) that should be removed
     */
    public void remove(final String... uniqueIds) {
        final Field[] uniqueIdentifiers = database.getClassInfo().getUniqueIdentifiers();
        final SQLQuery deleteQuery = database.newStatementBuilder()
                .delete(database.getTable())
                .where(convertUniqueIds(uniqueIdentifiers, uniqueIds.length))
                .replaceOnCreate(uniqueIds)
                .createQuery();

        deleteQuery.update();
    }

    /**
     * Parses a {@code ResultSet} to a {@code Map<String, Object>}
     *
     * @param result the result to parse
     * @return the result parsed to a map
     * @throws SQLException if a error occurs whilst parsing the results
     */
    private Map<String, Object> mapFromResult(final ResultSet result) throws SQLException{
        final Map<String, Object> map = new HashMap<>();
        final ResultSetMetaData metadata = result.getMetaData();
        for (int i = 0; i < metadata.getColumnCount(); i++) {
            map.put(metadata.getColumnLabel(i+1), result.getObject(i+1));
        }
        return map;
    }

    /**
     * Converts the fields marked as unique ids to a string array
     * containing their formatted names
     *
     * @param uniqueIds the field(s) defined as unique id(s)
     * @return the converted unique id(s)
     */
    private String[] convertUniqueIds(final Field[] uniqueIds, final int max) {
        final String[] formattedUniqueIds = new String[max];
        for (int i = 0; i < uniqueIds.length && i < max; i++) {
            final Field field = uniqueIds[i];
            final ClassInfo.FieldInfo fieldInfo = database.getClassInfo().getInfo(field);
            formattedUniqueIds[i] = fieldInfo.getFormattedName();
        }
        return formattedUniqueIds;
    }

    /**
     * Takes a {@code Map} as argument and returns a string
     * that can be put in the VALUES sql query.
     *
     * @param map map to parse from
     * @return a valid string passed to the VALUES sql query
     */
    private String mapToColumns(final Map<String, Object> map) {
        final Set<String> keySet = map.keySet();
        final String valueQuery = Strings.repeat("?,", keySet.size());
        return valueQuery.substring(0, valueQuery.length()-1);
    }

    /**
     * Takes a {@code Map} as argument and returns a string
     * that can be put in the UPDATE sql query.
     *
     * @param map map to parse from
     * @return a valid string passed to the UPDATE sql query
     */
    private String mapToColumnsUpdate(final Map<String, Object> map) {
        final StringBuilder valueQuery = new StringBuilder();
        for (final Map.Entry<String, Object> entry : map.entrySet()) {
            final String field = entry.getKey();
            valueQuery.append(field).append(" = ?,");
        }
        return valueQuery.substring(0, valueQuery.length()-1);
    }
}
