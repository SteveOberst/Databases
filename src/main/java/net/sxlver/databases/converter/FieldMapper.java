package net.sxlver.databases.converter;

import lombok.NonNull;
import net.sxlver.databases.ClassInfo;
import net.sxlver.databases.filter.FieldFilter;
import net.sxlver.databases.filter.FieldFilters;
import net.sxlver.databases.formatter.FieldNameFormatter;
import net.sxlver.databases.util.Reflection;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import static net.sxlver.databases.util.Validator.*;

public class FieldMapper {
    static void instanceFromMap(final @NonNull Object              inst,
                                final @NonNull Map<String, Object> instMap
    ) {
        final FieldFilter filter = FieldFilters.DEFAULT;
        for (final Field field : filter.filterDeclaredFieldsOf(inst.getClass())) {
            final FieldNameFormatter fnf = selectFormatter(inst.getClass());
            final String fn = fnf.fromFieldName(field.getName());
            final Object mapValue = instMap.get(fn);
            if (mapValue != null) {
                fromConvertedObject(field, mapValue, inst);
            }
        }
    }

    static Map<String, Object> instanceToMap(final @NonNull Object inst) {
        final Map<String, Object> map = new LinkedHashMap<>();
        final FieldFilter filter = FieldFilters.DEFAULT;
        for (final Field field : filter.filterDeclaredFieldsOf(inst.getClass())) {
            final Object val = toConvertibleObject(field, inst);
            final FieldNameFormatter fnf = selectFormatter(inst.getClass());
            final String fn = fnf.fromFieldName(field.getName());
            map.put(fn, val);
        }
        return map;
    }

    private static Object toConvertibleObject(final @NonNull Field field, final @NonNull Object instance) {
        final ConversionContext context = ConversionContext.of(field, instance);
        return Converter.convertTo(context);
    }

    private static void fromConvertedObject(final @NonNull Field  field,
                                            final @NonNull Object mapValue,
                                            final @NonNull Object instance
    ) {
        final ConversionContext context = ConversionContext.of(field, mapValue, instance);
        final Object convert = Converter.convertFrom(context);

        if (convert == null) {
            return;
        }

        if (Reflection.isContainerType(context.getFieldType())) {
            checkFieldTypeAssignableFrom(convert.getClass(), context);
        }

        Reflection.setValue(field, instance, convert);
    }

    static Map<String, Object> toTypeMap(final Object value, final @Nullable String fn) {
        checkIsMap(value, fn);
        checkMapKeysAreStrings((Map<?, ?>) value, fn);

        // The following cast won't fail because we just verified that
        // it's a Map<String, Object>.
        @SuppressWarnings("unchecked")
        final Map<String, Object> map = (Map<String, Object>) value;

        return map;
    }

    static FieldNameFormatter selectFormatter(final Class<?> clazz) {
        return ClassInfo.getFormatter(clazz);
    }
}
