package net.sxlver.databases.formatter;

import java.util.function.Function;

@FunctionalInterface
public interface FieldNameFormatter extends Function<String, String> {
    String fromFieldName(final String fieldName);

    @Override
    default String apply(final String string) {
        return fromFieldName(string);
    }
}