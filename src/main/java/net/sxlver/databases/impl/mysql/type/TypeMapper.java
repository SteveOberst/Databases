package net.sxlver.databases.impl.mysql.type;

import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TypeMapper {
    private final List<Class<?>> javaTypes = Lists.newArrayList();
    private final String sqlType;

    public TypeMapper(final String sqlType, final Class<?>... javaTypes) {
        this.sqlType = sqlType;
        this.javaTypes.addAll(Arrays.stream(javaTypes).collect(Collectors.toList()));
    }

    public TypeMapper(final String sqlType, final List<Class<?>> javaTypes) {
        this.sqlType = sqlType;
        this.javaTypes.addAll(javaTypes);
    }

    public boolean matches(final Class<?> clazz) {
        boolean match = false;
        for (final Class<?> javaType : javaTypes) {
            match = javaType.isAssignableFrom(clazz);
            if(match) {
                break;
            }
        }
        return match;
    }

    public String getSqlType(final Class<?> clazz) {
        return sqlType;
    }
}
