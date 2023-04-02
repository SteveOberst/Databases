package net.sxlver.databases.impl;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Wraps unique identifier(s) for database entries
 */
public class DatabaseEntryIdentifier {
    private final String[] uniqueIds;
    public static String DELIMITER = "_";

    public DatabaseEntryIdentifier(final String... uniqueIds) {
        this.uniqueIds = uniqueIds;
    }

    public static DatabaseEntryIdentifier of(final String... uniqueIds) {
        return new DatabaseEntryIdentifier(uniqueIds);
    }

    public Predicate<String> getFilterApplicator() {
        final String uniqueId = toString();
        return id -> {
            if(id.equals(toString())) {
                return true;
            }
            final String[] splitUniqueId = id.split(DELIMITER);
            boolean result = false;
            for (final String string : splitUniqueId) {
                result = string.equals(uniqueId);
                if(result) {
                    break;
                }
            }
            return result || id.contains(DELIMITER + uniqueId) || id.contains(uniqueId + DELIMITER);
        };
    }

    @Override
    public String toString() {
        return String.join(DELIMITER, uniqueIds);
    }
}
