package net.sxlver.databases.impl.mysql;

import net.sxlver.databases.exception.DatabaseException;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQLQuery {
    private final Connection connection;
    private final String query;

    SQLQuery(final Connection connection, final String query) {
        this.connection = connection;
        this.query = query;
    }

    public int update(final Object... parameter) {
        try {
            final PreparedStatement statement = connection.prepareStatement(query);
            for (int i = 0; i < parameter.length; i++) {
                statement.setObject(i + 1, parameter[i]);
            }
            return statement.executeUpdate();
        }catch(final SQLException exception) {
            final String message = "whilst executing update query '" + query + "'";
            throw new DatabaseException(message, exception);
        }
    }

    public ResultSet execute(final Object... parameter) {
        try {
            String query = this.query;
            for (int i = 0; i < parameter.length; i++) {
                query = query.replaceFirst("\\?", parameter[i].toString());
            }
            final PreparedStatement statement = connection.prepareStatement(query);
            return statement.executeQuery();
        }catch(final SQLException exception) {
            final String message = "whilst executing query '" + query + "'";
            throw new DatabaseException(message, exception);
        }
    }

    public String getQuery() {
        return query;
    }
}
