package net.sxlver.databases.impl.mysql;

import lombok.NonNull;
import net.sxlver.databases.DatabaseAuth;
import net.sxlver.databases.DatabaseSettings;

public class MySQLDatabaseAuthentication extends DatabaseAuth {

    private final String host;
    private final int port;
    private final String database;
    private final boolean useSSL;

    public MySQLDatabaseAuthentication(final @NonNull DatabaseSettings settings) {
        super(settings.getUsername(), settings.getPassword());
        this.host = settings.getHost();
        this.port = settings.getPort();
        this.database = settings.getDatabase();
        this.useSSL = settings.isUseSsl();
    }

    public MySQLDatabaseAuthentication(final @NonNull String username,
                                       final @NonNull String password,
                                       final @NonNull String host,
                                       final int             port,
                                       final @NonNull String database,
                                       final boolean         useSSL
    ) {
        super(username, password);
        this.host = host;
        this.port = port;
        this.database = database;
        this.useSSL = useSSL;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public boolean isUseSSL() {
        return useSSL;
    }
}
