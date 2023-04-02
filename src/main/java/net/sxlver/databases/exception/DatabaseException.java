package net.sxlver.databases.exception;

import lombok.NonNull;

public class DatabaseException extends RuntimeException {
    public DatabaseException(final @NonNull String message) {
        super(message);
    }

    public DatabaseException(final @NonNull String message, final @NonNull Throwable cause) {
        super(message, cause);
    }
}
