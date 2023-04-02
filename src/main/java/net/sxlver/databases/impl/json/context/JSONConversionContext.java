package net.sxlver.databases.impl.json.context;

import net.sxlver.databases.converter.ConversionContext;

public class JSONConversionContext extends ConversionContext {
    private final Class<?> valueType;

    JSONConversionContext(final Class<?> valueType) {
        super(null, null);
        this.valueType = valueType;
    }

    @Override
    public Class<?> getValueType() {
        return valueType;
    }

    @Override
    public Object getValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getMapValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class<?> getFieldType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class<?> getElementType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasElementType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getNestingLevel() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getCurrentNestingLevel() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void incCurrentNestingLevel() {
        throw new UnsupportedOperationException();
    }

    public static JSONConversionContext of(final Class<?> valueType) {
        return new JSONConversionContext(valueType);
    }
}
