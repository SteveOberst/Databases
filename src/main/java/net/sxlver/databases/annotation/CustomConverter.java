package net.sxlver.databases.annotation;

import net.sxlver.databases.DatabaseConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomConverter {
    /**
     * Defines a custom {@link DatabaseConverter} for a field. The
     * provided converter will be used to serialize and deserialize
     * the annotated field.
     *
     * @return the class of the converter
     */
    Class<? extends DatabaseConverter<?, ?>> value();
}
