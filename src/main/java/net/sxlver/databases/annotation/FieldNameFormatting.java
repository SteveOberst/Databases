package net.sxlver.databases.annotation;

import net.sxlver.databases.formatter.FieldNameFormatter;
import net.sxlver.databases.formatter.FieldNameFormatters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldNameFormatting {
    FieldNameFormatters formatter() default FieldNameFormatters.LOWER_SNAKE;
}
