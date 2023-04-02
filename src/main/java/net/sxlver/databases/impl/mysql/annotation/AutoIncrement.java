package net.sxlver.databases.impl.mysql.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@ColumnAttribute(value = "AUTO_INCREMENT", requiredTableAttribute = "PRIMARY KEY (%s)")
public @interface AutoIncrement {
}
