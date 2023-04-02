package net.sxlver.databases.impl.mysql;

import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SQLBuilder {
    private StringBuilder statement = new StringBuilder();

    private final List<String> replaceOnCreate = Lists.newArrayList();

    private static final String createTableQuery = " CREATE TABLE IF NOT EXISTS %s (%s) ";
    private static final String createTableQueryUniqueId = " CREATE TABLE IF NOT EXISTS %s (%s, %s) ";
    private static final String insertQuery = " INSERT INTO %s ";
    private static final String valuesQuery = " VALUES (%s) ";
    private static final String selectQuery = " SELECT %s FROM %s ";
    private static final String deleteQuery = " DELETE FROM %s ";
    private static final String whereQuery = " WHERE %s = '?' ";
    private static final String andQuery = " AND %s = ? ";
    private static final String uniqueKey = " UNIQUE KEY (%s)";
    private static final String uniqueKeyConstraint = " CONSTRAINT %s UNIQUE(%s) ";
    private static final String updateQuery = " UPDATE %s SET %s";
    private static final String onDuplicateKey = " ON DUPLICATE KEY ";
    private static final String onDuplicateKeyUpdate = onDuplicateKey + " UPDATE %s";
    private static final String selectMaxIdQuery = "SELECT %s FROM %s ORDER BY %s DESC LIMIT 1";

    private final MySQLDatabase<?> database;

    SQLBuilder(final MySQLDatabase<?> database) {
        this.database = database;
    }

    public SQLBuilder createIfNotExists(final String tableName, final String schema) {
        statement.append(String.format(createTableQuery, tableName, schema));
        return this;
    }

    public SQLBuilder createIfNotExists(final String tableName, final String schema, final String uniqueKey) {
        statement.append(String.format(createTableQueryUniqueId, tableName, schema, uniqueKey));
        return this;
    }

    public SQLBuilder select(final String table, final String columns) {
        statement.append(String.format(selectQuery, columns, table));
        return this;
    }

    public SQLBuilder where(final String expression) {
        statement.append(String.format(whereQuery, expression));
        return this;
    }

    public SQLBuilder where(final String... expressions) {
        statement.append(String.format(whereQuery, expressions[0]));
        for (int i = 1; i < expressions.length; i++) {
            statement.append(String.format(andQuery, expressions[i]));
        }
        return this;
    }

    public SQLBuilder whereAnd(final String... expressions) {
        statement.append(String.format(whereQuery, expressions[0]));
        for (int i = 1; i < expressions.length; i++) {
            statement.append(String.format(andQuery, expressions[i]));
        }
        return this;
    }

    public SQLBuilder insert(final String table) {
        statement.append(String.format(insertQuery, table));
        return this;
    }

    public SQLBuilder update(final String table, final String columns) {
        statement.append(String.format(updateQuery, table, columns));
        return this;
    }

    public SQLBuilder updateOnDuplicateKey(final String columns) {
        statement.append(String.format(onDuplicateKeyUpdate, columns));
        return this;
    }

    public SQLBuilder delete(final String table) {
        statement.append(String.format(deleteQuery, table));
        return this;
    }

    public SQLBuilder insert(final String table, final String columns) {
        statement.append(String.format(insertQuery, table));
        if (!columns.isBlank()) {
            statement.append(String.format(valuesQuery, columns));
        }
        return this;
    }

    public SQLBuilder values(final String values) {
        statement.append(String.format(valuesQuery, values));
        return this;
    }

    public SQLBuilder nextString(final String value) {
        replaceOnCreate.add(value);
        return this;
    }

    public SQLBuilder replaceOnCreate(final String... values) {
        replaceOnCreate.addAll(Arrays.stream(values).collect(Collectors.toList()));
        return this;
    }

    SQLBuilder selectMaxId(final String table, final String column) {
        statement.append(String.format(selectMaxIdQuery, column, table, column));
        return this;
    }

    public static String uniqueKey(final String column) {
        return String.format(uniqueKey, column);
    }

    public static String uniqueConstraint(final String constraint, final String... columns) {
        return String.format(uniqueKeyConstraint, constraint, formatStringArray(columns));
    }

    private static String formatStringArray(final String[] array) {
        final StringBuilder sb = new StringBuilder();
        for (final String string : array) {
            sb.append(string).append(",");
        }
        return sb.substring(0, Math.max(sb.length()-1, 0));
    }

    public SQLQuery createQuery() {
        return database.newQuery(replaceOnCreate(statement.toString()));
    }

    private String replaceOnCreate(final String statement) {
        String replace = statement;
        for (final String string : replaceOnCreate) {
            replace = statement.replaceFirst("\\'\\?\\'", "'" + string + "'");
        }
        return replace;
    }

    public String getQuery() {
        return statement.toString();
    }
}
