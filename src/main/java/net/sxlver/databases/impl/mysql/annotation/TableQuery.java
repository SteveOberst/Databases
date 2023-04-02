package net.sxlver.databases.impl.mysql.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Can be used to define a custom query for table creation instead of
 * the default one. Note that if you provide a custom query, you need
 * to make sure it's compatible with the class structure.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TableQuery {
    String value();
}
