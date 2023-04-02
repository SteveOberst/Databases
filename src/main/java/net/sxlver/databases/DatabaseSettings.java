package net.sxlver.databases;

public interface DatabaseSettings {

    /**
     * Returns the type of database to be used.
     *
     * @return the type of database
     */
    DatabaseTypes getDatabaseType();

    /**
     * Returns the username used for authentication
     *
     * @return the username
     */
    String getUsername();

    /**
     * Returns the password used for authentication
     *
     * @return the password
     */
    String getPassword();

    /**
     * Returns the username used for authentication
     *
     * @return
     */
    String getDatabase();

    /**
     * Returns the address of the host the database is running on
     *
     * <p>The address returned must be a valid ipv4 address or
     * 'localhost' for a locally hosted database.
     *
     * @return
     */
    String getHost();

    /**
     * Returns whether ssl should be used for a SQL connection.
     *
     * @return
     */
    boolean isUseSsl();

    /**
     * Returns the port the database is running on
     *
     * @return
     */
    int getPort();

    /**
     * Returns the path for a flat file storage databases
     *
     * @return
     */
    String getDatabasePath();

    /**
     * Returns the connection uri mongodb uses to connect
     * to the database
     *
     * @return
     */
    String getConnectionUri();
}
