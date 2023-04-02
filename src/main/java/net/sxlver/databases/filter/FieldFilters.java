package net.sxlver.databases.filter;

import net.sxlver.databases.annotation.Ignore;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public enum FieldFilters implements FieldFilter {
    DEFAULT {
        @Override
        public boolean test(final Field field) {
            if (field.isSynthetic()) {
                return false;
            }
            final int mods = field.getModifiers();
            return !(Modifier.isFinal(mods) ||
                    Modifier.isStatic(mods) ||
                    Modifier.isTransient(mods) ||
                    field.isAnnotationPresent(Ignore.class));
        }
    }
}
