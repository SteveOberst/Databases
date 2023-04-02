package net.sxlver.databases;

import lombok.NonNull;

public abstract class DatabaseAuth {
    private final String username;
    private final String password;

    public DatabaseAuth(final @NonNull String username, final @NonNull String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
