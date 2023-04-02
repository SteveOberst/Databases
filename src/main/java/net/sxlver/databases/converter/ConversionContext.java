package net.sxlver.databases.converter;

import java.lang.reflect.Field;

public abstract class ConversionContext {
    private final Field field;
    private final Object instance;

    public ConversionContext(final Field field, final Object instance) {
        this.field = field;
        this.instance = instance;
    }

    public Field getField() {
        return field;
    }

    public Object getInstance() {
        return instance;
    }

    public String getFieldName() { return field.getName(); }

    public abstract Object getValue();

    public abstract Object getMapValue();

    public abstract Class<?> getValueType();

    public abstract Class<?> getFieldType();

    public abstract Class<?> getElementType();

    public abstract boolean hasElementType();

    public abstract int getNestingLevel();

    public abstract int getCurrentNestingLevel();

    public abstract void incCurrentNestingLevel();

    public static ConversionContext of(final Field field, final Object instance) {
        return new ConversionContextImpl(field, null, instance);
    }

    public static ConversionContext of(final Field field, final Object mapValue, final Object instance) {
        return new ConversionContextImpl(field, mapValue, instance);
    }
}
