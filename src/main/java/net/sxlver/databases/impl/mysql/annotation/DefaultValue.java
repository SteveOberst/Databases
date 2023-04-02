package net.sxlver.databases.impl.mysql.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Currently only simple types (strings, primitives) can be used as default value.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@ColumnAttribute("DEFAULT %s")
//TODO: apply default value on instantiation of class
public @interface DefaultValue {
    String attributeValue();
}
