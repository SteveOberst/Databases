package net.sxlver.databases;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.NonNull;
import net.sxlver.databases.annotation.CustomConverter;
import net.sxlver.databases.annotation.FieldNameFormatting;
import net.sxlver.databases.annotation.Ignore;
import net.sxlver.databases.annotation.UniqueIdentifier;
import net.sxlver.databases.converter.Converter;
import net.sxlver.databases.exception.DatabaseException;
import net.sxlver.databases.filter.FieldFilter;
import net.sxlver.databases.filter.FieldFilters;
import net.sxlver.databases.formatter.FieldNameFormatter;
import net.sxlver.databases.formatter.FieldNameFormatters;
import net.sxlver.databases.impl.mysql.annotation.AutoIncrement;
import net.sxlver.databases.util.Reflection;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassInfo {

    private static final Cache<Class<?>, ClassInfo> classInfoCache = CacheBuilder.newBuilder().build();

    private final Map<Field, FieldInfo> fieldInfo;
    private final Class<?> clazz;
    private final DatabaseConverter<?, ?> converter;
    private final Field[] uniqueIdentifiers;

    ClassInfo(final @NonNull Class<?>                clazz,
              final @NonNull Map<Field, FieldInfo>   fieldInfo,
              final @NonNull DatabaseConverter<?, ?> converter,
              final @NonNull Field[]                 uniqueIdentifiers
    ) {
        this.clazz = clazz;
        this.fieldInfo = fieldInfo;
        this.converter = converter;
        this.uniqueIdentifiers = uniqueIdentifiers;
    }

    /**
     * Returns the name of the class the info corresponds to
     *
     * @return the name of the class
     */
    public String getClassName() {
        return clazz.getName();
    }

    /**
     * returns whether a field should be excluded from serialization
     *
     * @deprecated replaced with {@link FieldFilter}
     *
     * @param field the field to check
     * @return whether the field should be ignored
     */
    @Deprecated
    static boolean shouldSkip(final @NonNull Field field) {
        return field.isAnnotationPresent(Ignore.class);
    }

    /**
     * Returns whether a custom converter was defined for the field.
     *
     * @param clazz the class to check for
     * @return whether the field has a custom converter defined
     */
    static boolean hasCustomConverter(final Class<?> clazz) {
        return clazz.isAnnotationPresent(CustomConverter.class);
    }

    /**
     * Returns whether a custom {@link FieldNameFormatter} was defined for the class.
     *
     * @param clazz the class to check for
     * @return whether the class has a custom formatter defined
     */
    static boolean hasCustomFieldNameFormatter(final Class<?> clazz) {
        return clazz.isAnnotationPresent(FieldNameFormatting.class);
    }

    /**
     * Returns the {@link DatabaseConverter} defined or the default converter
     * if no custom converter has been defined.
     *
     * @param clazz the clazz to parse the converter from
     * @return the appropriate converter
     */
    static DatabaseConverter<?, ?> getConverter(final Class<?> clazz) {
        if(!hasCustomConverter(clazz)) {
            return new Converter.ReflectiveObjectConverter();
        }
        final CustomConverter annotation = clazz.getAnnotation(CustomConverter.class);
        return Reflection.newInstance(annotation.value());
    }

    /**
     * Returns the {@link FieldNameFormatter} defined or the default formatter
     * if no custom formatter has been defined.
     *
     * @param clazz the clazz to parse the formatter from
     * @return the appropriate formatter
     */
    public static FieldNameFormatter getFormatter(final Class<?> clazz) {
        if(!hasCustomConverter(clazz)) {
            return FieldNameFormatters.LOWER_SNAKE;
        }
        final FieldNameFormatting annotation = clazz.getAnnotation(FieldNameFormatting.class);
        return annotation.formatter();
    }

    /**
     * Returns the {@link FieldInfo} corresponding to the field param.
     *
     * @param field the field to retrieve the FieldInfo from
     * @return the FieldInfo or {@code null}
     */
    public FieldInfo getInfo(final @NonNull Field field) {
        return fieldInfo.get(field);
    }

    /**
     * Returns the defined converter for the class. If no converter
     * is defined through the {@link CustomConverter} annotation, the
     * framework will use the default converter.
     *
     * @return the converter handling serialization of the class
     */
    public DatabaseConverter<?, ?> getConverter() {
        return converter;
    }

    /**
     * Finds all fields annotated with {@link UniqueIdentifier} and returns
     * them as array
     *
     * @param clazz class to scan for unique identifiers
     * @return fields defined as unique identifiers
     */
    static Field[] findUniqueIdentifiers(final Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields()).filter(FieldInfo::isUniqueIdentifier).toArray(Field[]::new);
    }

    /**
     * Returns the appropriately formatted class name.
     *
     * @return the formatted class name
     */
    public String getFormattedClassName() {
        return getFormatter(clazz).apply(clazz.getSimpleName());
    }

    /**
     * returns the fields that have been defined as unique identifiers.
     *
     * @return fields declared as unique identifier.
     */
    public Field[] getUniqueIdentifiers() {
        return uniqueIdentifiers;
    }

    /**
     * Generates a {@link ClassInfo} instance from a class.
     *
     * @param clazz class to parse information from
     * @param formatter field name formatter
     * @return the generated {@link ClassInfo}
     */
    public static ClassInfo ofClass(final @NonNull Class<?> clazz, final @NonNull FieldNameFormatter formatter) {
        if(classInfoCache.getIfPresent(clazz) != null) {
            return classInfoCache.getIfPresent(clazz);
        }

        final List<? extends Field> declaredFields = FieldFilters.DEFAULT.filterDeclaredFieldsOf(clazz);
        final Map<Field, FieldInfo> fieldInfo = new HashMap<>();
        final DatabaseConverter<?, ?> converter = getConverter(clazz);
        final Field[] uniqueIdentifiers = findUniqueIdentifiers(clazz);
        if(uniqueIdentifiers.length == 0) {
            final String message = "no unique identifier(s) defined in " + clazz;
            throw new DatabaseException(message);
        }
        for (final Field field : declaredFields) {
            final FieldInfo info = new FieldInfo(field, formatter);
            fieldInfo.put(field, info);
        }
        final ClassInfo info = new ClassInfo(clazz, fieldInfo, converter, uniqueIdentifiers);
        classInfoCache.put(clazz, info);
        return info;
    }

    public static final class FieldInfo {
        private final Field field;
        private final Class<?> fieldType;
        private final boolean uniqueIdentifier;
        private final boolean autoIncrement;
        private final String formattedName;

        /**
         * Constructs the field info to the corresponding field
         * given the information provided.
         *
         * @param field the field to parse the information from
         * @param formatter formatter used to format the field name
         */
        public FieldInfo(final @NonNull Field              field,
                         final @NonNull FieldNameFormatter formatter) {
            this.field = field;
            this.fieldType = field.getType();
            this.uniqueIdentifier = isUniqueIdentifier(field);
            this.autoIncrement = isAutoIncrement(field);
            this.formattedName = formatter.apply(field.getName());
        }

        /**
         * Returns whether the field is annotated with the
         * {@link UniqueIdentifier} annotation.
         *
         * @param field field to check, not null
         * @return whether the field is marked with the UniqueIdentifier annotation
         */
        static boolean isUniqueIdentifier(final Field field) {
            return field.isAnnotationPresent(UniqueIdentifier.class);
        }

        /**
         * Returns whether the field is annotated with the
         * {@link AutoIncrement} annotation.
         *
         * @param field field to check, not null
         * @return whether the field is marked with the AutoIncrement annotation
         */
        static boolean isAutoIncrement(final Field field) {
            return field.isAnnotationPresent(AutoIncrement.class);
        }

        /**
         * Returns whether a custom converter was defined for the field.
         *
         * @param field the field to check for
         * @return whether the field has a custom converter defined
         */
        static boolean hasCustomConverter(final Field field) {
            return field.isAnnotationPresent(CustomConverter.class);
        }

        /**
         * Returns the field the other information correspond to.
         *
         * @return the field
         */
        public Field getField() {
            return field;
        }

        /**
         * Returns the type of the field passed to this class
         * on initialization.
         *
         * @return type of the field
         */
        public Class<?> getFieldType() {
            return fieldType;
        }

        /**
         * Returns whether the field is declared as unique identifier.
         *
         * @return whether the field is a unique identifier
         */
        public boolean isUniqueIdentifier() {
            return uniqueIdentifier;
        }

        /**
         * Returns true if the field has the auto increment attribute.
         *
         * @return whether the field is annotated with AutoIncrement
         */
        public boolean isAutoIncrement() {
            return autoIncrement;
        }

        /**
         * Returns the field name formatted by it's corresponding
         * {@link FieldNameFormatter}.
         *
         * @return formatted field name
         */
        public String getFormattedName() {
            return formattedName;
        }
    }
}
