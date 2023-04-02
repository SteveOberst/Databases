package net.sxlver.databases.converter;

import lombok.NonNull;
import net.sxlver.databases.annotation.ElementType;
import net.sxlver.databases.util.Reflection;
import net.sxlver.databases.formatter.FieldNameFormatters;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

public class ConversionContextImpl extends ConversionContext {
    private final Class<?> fieldType;
    private final Class<?> elementType;
    private final Object fieldValue;
    private final Object mapValue;
    private final int nestingLevel;
    private int currentNestingLevel;

    ConversionContextImpl(final @NonNull Field   field,
                          final @Nullable Object mapValue,
                          final @NonNull Object  instance
    ) {
        super(field, instance);
        this.mapValue = mapValue;
        this.fieldType = field.getType();
        this.elementType = elementType(field);
        this.fieldValue = Reflection.getValue(field, instance);
        this.nestingLevel = nestingLevel(field);
    }

    private static Class<?> elementType(final Field field) {
        if (field.isAnnotationPresent(ElementType.class)) {
            final ElementType et = field.getAnnotation(ElementType.class);
            return et.value();
        }
        return null;
    }

    private static int nestingLevel(final Field field) {
        if (field.isAnnotationPresent(ElementType.class)) {
            final ElementType et = field.getAnnotation(ElementType.class);
            return et.nestingLevel();
        }
        return -1;
    }

    @Override
    public int getNestingLevel() {
        return nestingLevel;
    }

    @Override
    public int getCurrentNestingLevel() {
        return currentNestingLevel;
    }

    @Override
    public void incCurrentNestingLevel() {
        currentNestingLevel++;
    }

    @Override
    public Class<?> getFieldType() {
        return fieldType;
    }

    @Override
    public Class<?> getElementType() {
        return elementType;
    }

    @Override
    public boolean hasElementType() {
        return elementType != null;
    }

    @Override
    public Object getValue() {
        return fieldValue;
    }

    @Override
    public Object getMapValue() {
        return mapValue;
    }

    @Override
    public Class<?> getValueType() {
        return fieldValue.getClass();
    }
}
