package net.sxlver.databases.formatter;

/*
 * copied over from config lib project - https://github.com/Sxlver/ConfigLib
 */
public enum FieldNameFormatters implements FieldNameFormatter {
    /**
     * Represents a {@code FieldNameFormatter} that doesn't actually format the
     * field name but instead returns it.
     */
    IDENTITY {
        @Override
        public String fromFieldName(final String fieldName) {
            return fieldName;
        }
    },
    /**
     * Represents a {@code FieldNameFormatter} that transforms <i>camelCase</i> to
     * <i>lower_underscore</i>.
     * <p>
     * For example, <i>myPrivateField</i> becomes <i>my_private_field</i>.
     */
    LOWER_SNAKE {
        @Override
        public String fromFieldName(final String fieldName) {
            final StringBuilder builder = new StringBuilder(fieldName.length());
            int iterations = 0;
            for (char c : fieldName.toCharArray()) {
                if (Character.isLowerCase(c)) {
                    builder.append(c);
                } else if (Character.isUpperCase(c)) {
                    c = Character.toLowerCase(c);
                    if(iterations != 0) {
                        builder.append('_');
                    }
                    builder.append(c);
                }
                iterations++;
            }
            return builder.toString();
        }
    },
    /**
     * Represents a {@code FieldNameFormatter} that transforms <i>camelCase</i> to
     * <i>UPPER_UNDERSCORE</i>.
     * <p>
     * For example, <i>myPrivateField</i> becomes <i>MY_PRIVATE_FIELD</i>.
     */
    UPPER_SNAKE {
        @Override
        public String fromFieldName(final String fieldName) {
            final StringBuilder builder = new StringBuilder(fieldName.length());
            int iterations = 0;
            for (final char c : fieldName.toCharArray()) {
                if (Character.isLowerCase(c)) {
                    builder.append(Character.toUpperCase(c));
                } else if (Character.isUpperCase(c)) {
                    if(iterations != 0) {
                        builder.append('_');
                    }
                    builder.append(c);
                }
                iterations++;
            }
            return builder.toString();
        }
    },
    /**
     * A more YAML-friendly case pattern.
     *
     * Represents a {@code FieldNameFormatter} that transforms <i>camelCase</i> to
     * <i>LOWER_KEBAB</i>.
     * <p>
     * For example, <i>myPrivateField</i> becomes <i>my-private-field</i>.
     */
    LOWER_KEBAB {
        @Override
        public String fromFieldName(final String fieldName) {
            final StringBuilder builder = new StringBuilder(fieldName.length());
            int iterations = 0;
            for (char c : fieldName.toCharArray()) {
                if (Character.isLowerCase(c)) {
                    builder.append(c);
                } else if (Character.isUpperCase(c)) {
                    c = Character.toLowerCase(c);
                    if(iterations != 0) {
                        builder.append('-');
                    }
                    builder.append(c);
                }
                iterations++;
            }
            return builder.toString();
        }
    }
}
