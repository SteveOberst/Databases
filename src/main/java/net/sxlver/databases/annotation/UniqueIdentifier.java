package net.sxlver.databases.annotation;

import net.sxlver.databases.DatabaseConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the field as a unique identifier for this object and
 * will be used in queries to uniquely identify the object the
 * field corresponds to across the different databases.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueIdentifier {
}
