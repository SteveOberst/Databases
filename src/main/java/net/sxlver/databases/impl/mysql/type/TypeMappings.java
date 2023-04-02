package net.sxlver.databases.impl.mysql.type;

import com.google.common.collect.Lists;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

public enum TypeMappings implements TypeMapping {
    DEFAULT(
            new TypeMapper("VARCHAR(255)", String.class),
            new TypeMapper("VARCHAR(50)", UUID.class),
            new TypeMapper("BOOLEAN", Boolean.class, boolean.class),
            new TypeMapper("DOUBLE PRECISION", Double.class, double.class),
            new TypeMapper("SMALLFLOAT", Float.class, float.class),
            new TypeMapper("INT", Integer.class, int.class),
            new TypeMapper("INT8", Long.class, long.class),
            new TypeMapper("SMALLINT", Short.class, short.class),
            new TypeMapper("DATETIME", Time.class, Timestamp.class),
            new TypeMapper("DATE", Date.class),
            new TypeMapper("DECIMAL", BigDecimal.class),
            new TypeMapper("BLOB", byte[].class),
            new TypeMapper("TEXT", List.class, Map.class, Set.class),
            new TypeMapper("VARCHAR(64)", Enum.class)
    );

    private final List<TypeMapper> typeMappers = Lists.newArrayList();

    TypeMappings(final TypeMapper... typeMappers) {
        this.typeMappers.addAll(Arrays.stream(typeMappers).collect(Collectors.toList()));
    }

    @Override
    public String getType(final Class<?> type) {
        return typeMappers.stream().filter(typeMapper -> typeMapper.matches(type))
                .map(typeMapper -> typeMapper.getSqlType(type))
                .findFirst()
                .orElse("");
    }
}
