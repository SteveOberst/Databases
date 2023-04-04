package net.sxlver.databases.util;

import net.sxlver.databases.annotation.UniqueIdentifier;
import net.sxlver.databases.converter.ConversionContext;
import net.sxlver.databases.exception.DatabaseException;
import net.sxlver.databases.impl.mysql.annotation.AutoIncrement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class Validator {
    public static void checkFieldTypeAssignableFrom(final Class<?> type, final ConversionContext info) {
        final Class<?> fieldType = info.getFieldType();
        if (!fieldType.isAssignableFrom(type)) {
            final String message = "Can not set field '" + info.getFieldName() + "' with " +
                    "type '" + getClsName(fieldType) + "' to '" +
                    getClsName(type) + "'.";
            throw new DatabaseException(message);
        }
    }

    public static void checkIsMap(final Object value, final String fn) {
        final Class<?> cls = value.getClass();
        if (!Map.class.isAssignableFrom(cls)) {
            final String message = "Initializing field '" + fn + "' requires a " +
                    "Map<String, Object> but the given object is not a map.\n" +
                    "Type: '" + cls.getSimpleName() + "'\tValue: '" + value + "'";
            throw new DatabaseException(message);
        }
    }

    public static void checkMapKeysAreStrings(final Map<?, ?> map, final String fn) {
        for (final Map.Entry<?, ?> entry : map.entrySet()) {
            final Object key = entry.getKey();
            if ((key == null) || (key.getClass() != String.class)) {
                final String message = "Initializing field '" + fn + "' requires a " +
                        "Map<String, Object> but the given map contains " +
                        "non-string keys.\nAll entries: " + map;
                throw new DatabaseException(message);
            }
        }
    }

    public static void checkAutoIncrementFieldIsInteger(final Field field) {
        final boolean isInteger = field.getType() == Integer.class || field.getType() == int.class;
        if(!isInteger) {
            throw new DatabaseException("Field annotated with " + AutoIncrement.class + " must be of the type integer.");
        }
    }

    public static void checkValidTableStructure(final Class<?> type) {
        final List<Annotation> annotations = Reflection.getClsAnnotations(type);
        final long autoIncrementFields = annotations.stream()
                .filter(annotation -> annotation.annotationType() == AutoIncrement.class)
                .count();

        if(autoIncrementFields > 1) {
            throw new DatabaseException("Table cannot have more than 1 auto increment field.");
        }

        final long uniqueIdCount = annotations.stream()
                .filter(annotation -> annotation.annotationType() == UniqueIdentifier.class)
                .count();

        if(uniqueIdCount < 1) {
            throw new DatabaseException("Table must have at least one unique identifier.");
        }
    }

    public static void checkCurrentLevelSameAsExpectedRequiresMapOrString(final boolean currentLevelSameAsExpected,
                                                                          final Object element,
                                                                          final ConversionContext context
    ) {
        final boolean isMapOrString = (element instanceof Map<?, ?>) ||
                (element instanceof String);
        if (currentLevelSameAsExpected && !isMapOrString) {
            final Class<?> clazz = context.getInstance().getClass();
            final String message = "Field '" + context.getFieldName() + "' of class '" +
                    getClsName(clazz) + "' has a nesting level" +
                    " of " + context.getNestingLevel() + " but element '" + element +
                    "' of type '" + getClsName(element.getClass()) + "' cannot be " +
                    "converted to '" + getClsName(context.getElementType()) + "'.";
            throw new DatabaseException(message);
        }
    }

    public static void checkNestingLevel(final Object element, final ConversionContext info) {
        if (!Reflection.isContainerType(element.getClass())) {
            if (info.getNestingLevel() != info.getCurrentNestingLevel()) {
                final String message = "Field '" + info.getFieldName() + "' of class " +
                        "'" + getClsName(info.getInstance().getClass()) + "' " +
                        "has a nesting level of " + info.getNestingLevel() +
                        " but the first object of type '" +
                        getClsName(info.getElementType()) + "' was found on " +
                        "level " + info.getCurrentNestingLevel() + ".";
                throw new DatabaseException(message);
            }
        }
    }

    public static void checkConverterHasNoArgsConstructor(final Class<?> converterClass, final String fn) {
        if (!Reflection.hasNoArgConstructor(converterClass)) {
            final String message = "Converter '" + converterClass.getSimpleName() + "' used " +
                    "on field '" + fn + "' doesn't have a no-args constructor.";
            throw new DatabaseException(message);
        }
    }

    private static String getClsName(final Class<?> cls) {
        String clsName = cls.getSimpleName();
        if (clsName.equals("")) {
            clsName = cls.getName();
        }
        return clsName;
    }
}
