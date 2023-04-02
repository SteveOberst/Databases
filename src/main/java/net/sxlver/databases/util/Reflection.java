package net.sxlver.databases.util;

import com.google.common.collect.Lists;
import net.sxlver.databases.ClassInfo;
import net.sxlver.databases.annotation.CustomConverter;
import net.sxlver.databases.annotation.Ignore;
import net.sxlver.databases.annotation.TableName;
import net.sxlver.databases.annotation.UniqueIdentifier;
import net.sxlver.databases.filter.FieldFilter;
import net.sxlver.databases.filter.FieldFilters;
import net.sxlver.databases.formatter.FieldNameFormatter;
import net.sxlver.databases.impl.DatabaseEntryIdentifier;
import net.sxlver.databases.impl.mysql.annotation.ColumnAttribute;
import net.sxlver.databases.impl.mysql.annotation.DataType;
import net.sxlver.databases.impl.mysql.annotation.TableQuery;
import net.sxlver.databases.exception.DatabaseException;
import net.sxlver.databases.impl.mysql.type.TypeMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Reflection {
    private static final Set<Class<?>> SIMPLE_TYPES = new HashSet<>(Arrays.asList(
            Boolean.class,
            Byte.class,
            Character.class,
            Short.class,
            Integer.class,
            Long.class,
            Float.class,
            Double.class,
            String.class
    ));

    public static boolean isSimpleType(final Class<?> clazz) {
        return clazz.isPrimitive() || SIMPLE_TYPES.contains(clazz);
    }

    public static boolean isEnumType(final Class<?> cls) {
        return cls.isEnum();
    }

    public static Object getValue(final Field field, final Object inst) {
        try {
            field.setAccessible(true);
            return field.get(inst);
        } catch (final IllegalAccessException exception) {
            final String message = "Illegal access of field '" + field + "' " + "on object " + inst + ".";
            throw new DatabaseException(message, exception);
        }
    }

    public static String getTableName(final Class<?> clazz) {
        if(clazz.isAnnotationPresent(TableName.class)) {
            final TableName annotation = clazz.getAnnotation(TableName.class);
            return annotation.value();
        }
        return clazz.getSimpleName();
    }

    public static void setValue(final Field field, final Object inst, final Object value) {
        try {
            field.setAccessible(true);
            field.set(inst, value);
        } catch (IllegalAccessException e) {
            final String message = "Illegal access of field '" + field + "' " + "on object " + inst + ".";
            throw new DatabaseException(message, e);
        }
    }

    public static boolean isContainerType(final Class<?> clazz) {
        return List.class.isAssignableFrom(clazz) ||
                Set.class.isAssignableFrom(clazz) ||
                Map.class.isAssignableFrom(clazz);
    }

    static boolean hasNoArgConstructor(Class<?> cls) {
        return Arrays.stream(cls.getDeclaredConstructors())
                .anyMatch(c -> c.getParameterCount() == 0);
    }

    public static List<Annotation> getClsAnnotations(final Class<?> clazz) {
        final Stream<Annotation[]> stream = Arrays.stream(clazz.getDeclaredFields()).map(Field::getAnnotations);
        final List<Annotation> annotations = Lists.newArrayList();
        stream.forEach(fieldAnnotations -> annotations.addAll(Arrays.asList(fieldAnnotations)));
        return annotations;
    }

    public static <T> DatabaseEntryIdentifier getUniqueIdentifier(final T instance) {
        final Class<?> clazz = instance.getClass();
        final Predicate<Field> uniqueIdFilter = field -> field.isAnnotationPresent(UniqueIdentifier.class);
        final List<Field> uniqueIdentifiers = FieldFilters.DEFAULT.filterDeclaredFieldsOfStreaming(clazz)
                .filter(uniqueIdFilter)
                .collect(Collectors.toList());

        final String[] uniqueIdValues = uniqueIdentifiers.stream()
                .map(field -> getValue(field, instance))
                .map(Object::toString)
                .toArray(String[]::new);

        return DatabaseEntryIdentifier.of(uniqueIdValues);
    }

    public static Field getAutoIncrementField(final Class<?> clazz, final ClassInfo classInfo) {
        return FieldFilters.DEFAULT.filterDeclaredFieldsOf(clazz).stream()
                .map(classInfo::getInfo)
                .filter(ClassInfo.FieldInfo::isAutoIncrement)
                .map(ClassInfo.FieldInfo::getField)
                .findFirst()
                .orElse(null);
    }

    public static <T> T newInstance(final Class<T> cls) {
        try {
            final Constructor<T> constructor = cls.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (final NoSuchMethodException exception) {
            final String msg = "Class " + cls.getSimpleName() + " doesn't have a " + "no-args constructor.";
            throw new DatabaseException(msg, exception);
        } catch (final IllegalAccessException exception) {
            /* This exception should not be thrown because
             * we set the field to be accessible. */
            final String msg = "No-args constructor of class " + cls.getSimpleName() + " not accessible.";
            throw new DatabaseException(msg, exception);
        } catch (final InstantiationException exception) {
            final String msg = "Class " + cls.getSimpleName() + " not instantiable.";
            throw new DatabaseException(msg, exception);
        } catch (final InvocationTargetException exception) {
            final String msg = "Constructor of class " + cls.getSimpleName() + " has thrown an exception.";
            throw new DatabaseException(msg, exception);
        }
    }

    public static String getQuery(final AnnotatedElement element) {
        return element.getAnnotation(TableQuery.class).value();
    }

    public static boolean hasCustomQuery(final Class<?> clazz) {
        return clazz.isAnnotationPresent(TableQuery.class);
    }

    public static boolean hasConverter(final Field field) {
        return field.isAnnotationPresent(CustomConverter.class);
    }

    public static boolean hasSQLType(final AnnotatedElement element) {
        return element.isAnnotationPresent(DataType.class);
    }

    public static String getDefinedSQLType(final AnnotatedElement element) {
        if(!hasSQLType(element)) {
            return "";
        }
        return element.getAnnotation(DataType.class).value();
    }

    public static boolean shouldIgnore(final Field field) {
        return field.isAnnotationPresent(Ignore.class);
    }

    public static String getTableSchema(final Class<?> type, final FieldNameFormatter formatter, final TypeMapping typeMapping) {
        final StringBuilder sb = new StringBuilder();
        final FieldFilter filter = FieldFilters.DEFAULT;
        for (final Field field : filter.filterDeclaredFieldsOf(type)) {
            sb.append(formatter.apply(field.getName())).append(" ")
                    .append(getSQLType(field, typeMapping)).append(" ")
                    .append(getColumnModifiers(field)).append(",")
                    .append(getRequiredTableAttributes(field, formatter));
        }
        return sb.substring(0, Math.max(sb.length()-1, 0));
    }

    public static String getColumnModifiers(final Field field) {
        final StringBuilder sb = new StringBuilder();
        for (final Annotation annotation : field.getDeclaredAnnotations()) {
            if(annotation.annotationType().isAnnotationPresent(ColumnAttribute.class)) {
                final ColumnAttribute attribute = annotation.annotationType().getAnnotation(ColumnAttribute.class);
                final String attributeValue = getColumnAttributeValue(annotation);
                sb.append(String.format(attribute.value(), attributeValue)).append(" ");
            }
        }
        return sb.toString();
    }

    private static String getRequiredTableAttributes(final Field field, final FieldNameFormatter formatter) {
        final StringBuilder sb = new StringBuilder();
        for (final Annotation annotation : field.getDeclaredAnnotations()) {
            if(annotation.annotationType().isAnnotationPresent(ColumnAttribute.class)) {
                final ColumnAttribute attribute = annotation.annotationType().getAnnotation(ColumnAttribute.class);
                if(!attribute.requiredTableAttribute().isBlank()) {
                    sb.append(String.format(attribute.requiredTableAttribute(), formatter.apply(field.getName()))).append(",");
                }
            }
        }
        return sb.toString();
    }

    private static String getColumnAttributeValue(final Annotation annotation) {
        try {
            final Method attributeValueMethod = annotation.annotationType().getDeclaredMethod("attributeValue");
            attributeValueMethod.setAccessible(true);
            return (String) attributeValueMethod.invoke(annotation);
        } catch (final Exception exception) {
            return "";
        }
    }

    public static String getSQLType(final Field field, final TypeMapping typeMapping) {
        if(hasSQLType(field)) {
            return getDefinedSQLType(field);
        }
        return typeMapping.getType(field.getType());
    }
}
