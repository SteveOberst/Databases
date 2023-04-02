package net.sxlver.databases;

import com.google.gson.TypeAdapter;
import net.sxlver.databases.converter.ConversionContext;
import net.sxlver.databases.exception.DatabaseException;

import java.lang.reflect.Type;

/**
 * Implementations of this interface convert types and field values to
 * objects that can be handled by the underlying database/serializer,
 * and vice versa.
 *
 * <p>Implementations must have a no-args constructor.
 *
 * @param <A> the source type
 * @param <B> the target type
 */
public interface DatabaseConverter<A, B> {
    /**
     * Deserializes {@code B} back to it's original type.
     *
     * <p>If this method returns null and the deserialized type is to
     * be assigned to a field, the field's default value will be kept,
     * if a "Object itself" is to be deserialized a {@code DatabaseException}
     * will be thrown.
     *
     * @param toDeserialize object that should be converted back
     * @param context the ConversionContext for the given object
     * @return the object deserialized to it's original type
     */
    A deserialize(final B toDeserialize, final ConversionContext context);

    /**
     * Converts {@code A} to any object that can natively be stored or serialized
     * by the underlying database/serializer.
     *
     * <p>The required return value of this method depends on the implementation
     * of {@link Database} and on the database that is used itself. To ensure
     * compatibility with every default implementation, the returned value
     * should be of a type that <i>MySQL</i> and <i>Gson</i> can handle by
     * default. If you want to ensure compatibility over any implementation
     * that exists and might be added in the future, returning a primitive
     * will likely always be supported by the implementation.
     *
     * <p>More complex data types, non primitives that may be returned must be
     * supported by the underlying database or serializer (or a type adapter must
     * be created, see {@link AbstractDatabase#registerTypeAdapter(Type, TypeAdapter)}
     * the object might be passed to before actually being written to the database.
     * An example for this would be gson, which will convert the value returned by
     * this method to it's proper json representation before passing it to the database
     *
     * <p>An example for mysql mostly being:
     * <li>Any primitive value</li>
     * <li>java.lang.String</li>
     * <li>java.io.InputStream</li>
     * <li>java.io.Reader</li>
     * <li>java.sql.Blob</li>
     * <li>java.sql.Clob</li>
     * <li>java.sql.Timestamp</li>
     * <li>java.sql.Date</li>
     * <li>java.sql.BigDecimal</li>
     *
     * <p> more info about mysql type conversion can be found here:
     *
     * <a href="http://https://dev.mysql.com/doc/connector-j/5.1/en/connector-j-reference-type-conversions.html">
     * MySQL Supported java types
     * </a>
     *
     * @throws DatabaseException if null is returned
     * @param toSerialize instance of the object ({@code A}) that is being serialized
     * @param context the ConversionContext for the given object
     * @return A object that can be handled by the underlying database/serializer
     */
    B serialize(final A toSerialize, final ConversionContext context);
}
