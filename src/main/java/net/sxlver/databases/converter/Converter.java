package net.sxlver.databases.converter;

import lombok.NonNull;
import net.sxlver.databases.DatabaseConverter;
import net.sxlver.databases.annotation.CustomConverter;
import net.sxlver.databases.exception.DatabaseException;
import net.sxlver.databases.util.Reflection;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.*;
import static net.sxlver.databases.util.Validator.*;

/*
 * mostly taken over from config lib project - https://github.com/Sxlver/ConfigLib
 */
public final class Converter {

    private static final Map<Class<? extends DatabaseConverter<?, ?>>, DatabaseConverter<?, ?>> cache = new WeakHashMap<>();

    private static final ReflectiveObjectConverter REFLECTIVE_OBJECT_CONVERTER = new ReflectiveObjectConverter();

    private static final SimpleTypeConverter SIMPLE_TYPE_CONVERTER = new SimpleTypeConverter();
    private static final EnumConverter ENUM_CONVERTER = new EnumConverter();
    private static final IdentityConverter IDENTITY_CONVERTER = new IdentityConverter();
    private static final ListConverter LIST_CONVERTER = new ListConverter();
    private static final SetConverter SET_CONVERTER = new SetConverter();
    private static final MapConverter MAP_CONVERTER = new MapConverter();
    private static final SimpleListConverter SIMPLE_LIST_CONVERTER = new SimpleListConverter();
    private static final SimpleSetConverter SIMPLE_SET_CONVERTER = new SimpleSetConverter();
    private static final SimpleMapConverter SIMPLE_MAP_CONVERTER = new SimpleMapConverter();

    public static final class SimpleTypeConverter implements DatabaseConverter<Object, Object> {
        @Override
        public Object deserialize(final @NonNull Object toDeserialize, final @NonNull ConversionContext context) {
            return toDeserialize;
        }

        @Override
        public Object serialize(final @NonNull Object toSerialize, final @NonNull ConversionContext context) {
            if (toSerialize instanceof Number) {
                return convertNumber(context.getFieldType(), (Number) toSerialize);
            }
            return toSerialize;
        }

        private Object convertNumber(final @NonNull Class<?> target, final @NonNull Number value) {
            if (target == byte.class || target == Byte.class) {
                return value.byteValue();
            } else if (target == short.class || target == Short.class) {
                return value.shortValue();
            } else if (target == int.class || target == Integer.class) {
                return value.intValue();
            } else if (target == long.class || target == Long.class) {
                return value.longValue();
            } else if (target == float.class || target == Float.class) {
                return value.floatValue();
            } else if (target == double.class || target == Double.class) {
                return value.doubleValue();
            } else {
                final String message = "Number '" + value + "' cannot be converted " + "to type '" + target + "'";
                throw new IllegalArgumentException(message);
            }
        }

        private Object convertString(final String string) {
            final int length = string.length();
            if (length == 0) {
                final String message = "An empty string cannot be converted to a character.";
                throw new IllegalArgumentException(message);
            }
            if (length > 1) {
                final String message = "String '" + string + "' is too long to " + "be converted to a character";
                throw new IllegalArgumentException(message);
            }
            return string.charAt(0);
        }
    }

    public static final class EnumConverter implements DatabaseConverter<Enum<?>, String> {

        @Override
        public String serialize(final Enum<?> element, final ConversionContext context) {
            return element.toString();
        }

        @Override
        public Enum<?> deserialize(final String element, final ConversionContext context) {
            final Class<? extends Enum> clazz = getEnumClass(context);
            try {
                @SuppressWarnings("unchecked")
                final Enum<?> enm = Enum.valueOf(clazz, element);
                return enm;
            } catch (IllegalArgumentException e) {
                final String in = selectWord(context);
                final String msg = "Cannot initialize " + in + " because there is no " +
                        "enum constant '" + element + "'.\nValid constants are: " +
                        Arrays.toString(clazz.getEnumConstants());
                throw new IllegalArgumentException(msg, e);
            }
        }

        private String selectWord(final ConversionContext context) {
            final String fn = context.getFieldName();
            if (Reflection.isContainerType(context.getFieldType())) {
                final String w = selectContainerName(context.getValueType());
                return "an enum element of " + w + " '" + fn + "'";
            }
            return "enum '" + fn + "' ";
        }
    }

    public static final class ReflectiveObjectConverter implements DatabaseConverter<Object, Object> {

        @Override
        public Object deserialize(final Object toDeserialize, final ConversionContext context) {
            final Object newInstance = Reflection.newInstance(context.getValueType());
            final Map<String, Object> typeMap = FieldMapper.toTypeMap(toDeserialize, null);
            for (Map.Entry<String, Object> entry : typeMap.entrySet()) {
            }
            FieldMapper.instanceFromMap(newInstance, typeMap);
            return newInstance;
        }

        @Override
        public Object serialize(final Object toSerialize, final @Nullable ConversionContext context) {
            return FieldMapper.instanceToMap(toSerialize);
        }
    }

    private static final class IdentityConverter implements DatabaseConverter<Object, Object> {

        @Override
        public Object deserialize(final Object toDeserialize, final ConversionContext context) {
            return toDeserialize;
        }

        @Override
        public Object serialize(final Object toSerialize, final ConversionContext context) {
            return toSerialize;
        }
    }

    private static final class ListConverter implements DatabaseConverter<List<?>, List<?>> {

        @Override
        public List<?> deserialize(final List<?> toDeserialize, final ConversionContext context) {
            if (toDeserialize.isEmpty()) {
                return toDeserialize;
            }
            final Object o = toDeserialize.get(0);
            final Function<Object, ?> f = createToConversionFunction(o, context);
            return toDeserialize.stream().map(f).collect(toList());
        }

        @Override
        public List<?> serialize(final List<?> toSerialize, final ConversionContext context) {
            if (toSerialize.isEmpty()) {
                return toSerialize;
            }
            final Object o = toSerialize.get(0);
            final Function<Object, ?> f = createFromConversionFunction(o, context);
            return toSerialize.stream().map(f).collect(toList());
        }
    }

    private static final class SetConverter implements DatabaseConverter<Set<?>, Set<?>> {

        @Override
        public Set<?> deserialize(final Set<?> toDeserialize, final ConversionContext context) {
            if (toDeserialize.isEmpty()) {
                return toDeserialize;
            }
            final Object o = toDeserialize.iterator().next();
            final Function<Object, ?> f = createToConversionFunction(o, context);
            return toDeserialize.stream().map(f).collect(toSet());
        }

        @Override
        public Set<?> serialize(final Set<?> toSerialize, final ConversionContext context) {
            if (toSerialize.isEmpty()) {
                return toSerialize;
            }
            final Object o = toSerialize.iterator().next();
            final Function<Object, ?> f = createFromConversionFunction(o, context);
            return toSerialize.stream().map(f).collect(toSet());
        }
    }

    private static final class MapConverter implements DatabaseConverter<Map<?, ?>, Map<?, ?>> {

        @Override
        public Map<?, ?> deserialize(final Map<?, ?> toDeserialize, final ConversionContext context) {
            if (toDeserialize.isEmpty()) {
                return toDeserialize;
            }
            final Object o = toDeserialize.values().iterator().next();
            final Function<Object, ?> cf = createToConversionFunction(o, context);
            final Function<Map.Entry<?, ?>, ?> f = e -> cf.apply(e.getValue());
            return new LinkedHashMap<>(toDeserialize.entrySet().stream().collect(toMap(Map.Entry::getKey, f)));
        }

        @Override
        public Map<?, ?> serialize(final Map<?, ?> toSerialize, final ConversionContext context) {
            if (toSerialize.isEmpty()) {
                return toSerialize;
            }
            final Object o = toSerialize.values().iterator().next();
            final Function<Object, ?> cf = createFromConversionFunction(o, context);
            final Function<Map.Entry<?, ?>, ?> f = e -> cf.apply(e.getValue());
            return new LinkedHashMap<>(toSerialize.entrySet().stream().collect(toMap(Map.Entry::getKey, f)));
        }
    }

    private static Function<Object, ?> createToConversionFunction(
            final Object element, final ConversionContext context
    ) {
        checkNestingLevel(element, context);
        if (Reflection.isContainerType(element.getClass())) {
            context.incCurrentNestingLevel();
        }
        final DatabaseConverter<Object, ?> converter = selectNonSimpleConverter(
                element.getClass(), context
        );
        return o -> converter.serialize(o, context);
    }

    private static Function<Object, ?> createFromConversionFunction(
            final Object element, final ConversionContext context
    ) {
        final boolean currentLevelSameAsExpected = context.getNestingLevel() == context.getCurrentNestingLevel();
        checkCurrentLevelSameAsExpectedRequiresMapOrString(
                currentLevelSameAsExpected, element, context
        );
        if ((element instanceof Map<?, ?>) && currentLevelSameAsExpected) {
            return o -> {
                final Map<String, Object> map = toTypeMap(o, null);
                final Object inst = Reflection.newInstance(context.getElementType());
                FieldMapper.instanceFromMap(inst, map);
                return inst;
            };
        } else if ((element instanceof String) && currentLevelSameAsExpected) {
            return createNonSimpleConverter(element, context);
        } else {
            context.incCurrentNestingLevel();
            return createNonSimpleConverter(element, context);
        }
    }

    private static Function<Object, ?> createNonSimpleConverter(final Object element, final ConversionContext context) {
        final DatabaseConverter<?, Object> converter = selectNonSimpleConverter(
                element.getClass(), context
        );
        return o -> converter.deserialize(o, context);
    }

    private static Map<String, Object> toTypeMap(final Object value, final String fn) {
        checkIsMap(value, fn);
        checkMapKeysAreStrings((Map<?, ?>) value, fn);

        // The following cast won't fail because we just verified that
        // it's a Map<String, Object>.
        @SuppressWarnings("unchecked")
        final Map<String, Object> map = (Map<String, Object>) value;

        return map;
    }

    private static final class SimpleListConverter implements DatabaseConverter<List<?>, List<?>> {

        @Override
        public List<?> deserialize(final List<?> toDeserialize, final ConversionContext context) {
            return toDeserialize;
        }

        @Override
        public List<?> serialize(final List<?> toSerialize, final ConversionContext context) {
            return toSerialize;
        }
    }

    private static final class SimpleSetConverter implements DatabaseConverter<Set<?>, Set<?>> {
        @Override
        public Set<?> deserialize(final Set<?> toDeserialize, final ConversionContext context) {
            return toDeserialize;
        }

        @Override
        public Set<?> serialize(final Set<?> toSerialize, final ConversionContext context) {
            return toSerialize;
        }
    }

    private static final class SimpleMapConverter implements DatabaseConverter<Map<?, ?>, Map<?, ?>> {

        @Override
        public Map<?, ?> deserialize(final Map<?, ?> toDeserialize, final ConversionContext context) {
            return toDeserialize;
        }

        @Override
        public Map<?, ?> serialize(final Map<?, ?> toSerialize, final ConversionContext context) {
            return toSerialize;
        }
    }

    static String selectContainerName(final @NonNull Class<?> containerType) {
        return selector("list", "set", "map").apply(containerType);
    }

    static <R> Function<Class<?>, R> selector(final @NonNull R listValue,
                                              final @NonNull R setValue,
                                              final @NonNull R mapValue
    ) {
        return containerClass -> {
            if (List.class.isAssignableFrom(containerClass)) {
                return listValue;
            } else if (Set.class.isAssignableFrom(containerClass)) {
                return setValue;
            } else {
                return mapValue;
            }
        };
    }

    private static DatabaseConverter<Object, Object> selectConverter(final @NonNull Class<?>          valueType,
                                                                     final @NonNull ConversionContext context
    ) {
        final DatabaseConverter<?, ?> converter;
        if (Reflection.shouldIgnore(context.getField())) {
            converter = IDENTITY_CONVERTER;
        } else if (Reflection.hasConverter(context.getField())) {
            converter = instantiateConverter(context.getField());
        } else if (Reflection.isSimpleType(valueType)) {
            converter = SIMPLE_TYPE_CONVERTER;
        } else {
            converter = selectNonSimpleConverter(valueType, context);
        }
        return toObjectConverter(converter);
    }

    private static DatabaseConverter<Object, Object> selectNonSimpleConverter(final @NonNull Class<?>          valueType,
                                                                              final @NonNull ConversionContext context
    ) {
        final DatabaseConverter<?, ?> converter;
        if (Reflection.isEnumType(valueType) || valueType == String.class) {
            converter = ENUM_CONVERTER;
        } else if (Reflection.isContainerType(valueType)) {
            converter = selectContainerConverter(valueType, context);
        } else {
            converter = REFLECTIVE_OBJECT_CONVERTER;
        }
        return toObjectConverter(converter);
    }

    private static DatabaseConverter<?, ?> selectContainerConverter(final @NonNull Class<?>          valueType,
                                                                    final @NonNull ConversionContext context
    ) {
        if (context.hasElementType()) {
            return selectElementTypeContainerConverter(valueType);
        } else {
            return selectSimpleContainerConverter(valueType);
        }
    }

    private static DatabaseConverter<?, ?> selectElementTypeContainerConverter(final @NonNull Class<?> valueType) {
        return selector(
                LIST_CONVERTER, SET_CONVERTER, MAP_CONVERTER
        ).apply(valueType);
    }

    private static DatabaseConverter<?, ?> selectSimpleContainerConverter(final @NonNull Class<?> valueType) {
        return selector(
                SIMPLE_LIST_CONVERTER, SIMPLE_SET_CONVERTER, SIMPLE_MAP_CONVERTER
        ).apply(valueType);
    }

    static Object convertTo(final @NonNull ConversionContext context) {
        final DatabaseConverter<Object, Object> converter = selectConverter(
                context.getValueType(), context
        );
        return tryConvertTo(converter, context);
    }

    static Object convertFrom(final @NonNull ConversionContext context) {
        final DatabaseConverter<Object, Object> converter = selectConverter(
                context.getValueType(), context
        );
        return tryConvertFrom(converter, context);
    }

    static DatabaseConverter<Object, Object> toObjectConverter(final @NonNull DatabaseConverter<?, ?> converter) {
        /* This cast may result in a ClassCastException when converting objects
         * back to their original representation. This happens if the type of the
         * converted object has changed for some reason (e.g. by a configuration
         * mistake). However, the ClassCastException is later caught and translated
         * to a ConfigurationException to give additional information about what
         * happened. */
        @SuppressWarnings("unchecked")
        final DatabaseConverter<Object, Object> c = (DatabaseConverter<Object, Object>) converter;
        return c;
    }

    private static Object tryConvertTo(final @NonNull DatabaseConverter<Object, Object> converter,
                                       final @NonNull ConversionContext                 context
    ) {
        try {
            return converter.serialize(context.getValue(), context);
        } catch (final ClassCastException exception) {
            final String message = "Converter '" + converter.getClass().getSimpleName() + "'" +
                    " cannot convert value '" + context.getValue() + "' of field '" +
                    context.getFieldName() + "' because it expects a different type.";
            throw new DatabaseException(message, exception);
        }
    }

    private static Object tryConvertFrom(
            final @NonNull DatabaseConverter<Object, Object> converter,
            final @NonNull ConversionContext                 context
    ) {
        try {
            return converter.deserialize(context.getMapValue(), context);
        } catch (final ClassCastException | IllegalArgumentException exception) {
            final String message = "The value for field '" + context.getFieldName() + "' with " +
                    "type '" + getClsName(context.getFieldType()) + "' cannot " +
                    "be converted back to its original representation because a " +
                    "type mismatch occurred.";
            throw new DatabaseException(message, exception);
        }
    }

    static DatabaseConverter<?, ?> instantiateConverter(final Field field) {
        final CustomConverter convert = field.getAnnotation(CustomConverter.class);
        return cache.computeIfAbsent(convert.value(), cls -> {
            checkConverterHasNoArgsConstructor(cls, field.getName());
            return Reflection.newInstance(cls);
        });
    }

    private static String getClsName(final Class<?> clazz) {
        return clazz.getSimpleName();
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Enum> getEnumClass(final ConversionContext context) {
        return (Class<? extends Enum>) context.getValue().getClass();
    }
}
