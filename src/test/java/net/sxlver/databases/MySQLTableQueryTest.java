package net.sxlver.databases;

import net.sxlver.databases.formatter.FieldNameFormatters;
import net.sxlver.databases.impl.mysql.annotation.DataType;
import net.sxlver.databases.impl.mysql.annotation.TableQuery;
import net.sxlver.databases.impl.mysql.type.TypeMappings;
import net.sxlver.databases.util.Reflection;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class MySQLTableQueryTest {
    @Test
    void queryMatches() {
        final String schema = "test_string VARCHAR(50),test_decimal DECIMAL,test BOOLEAN";

        @TableQuery("CREATE TABLE IF NOT EXISTS test (" + schema + ")")
        class TestObject {
            private String testString = "test";
            private BigDecimal testDecimal = new BigDecimal(0);
            private boolean test = true;
        }
        org.hamcrest.MatcherAssert.assertThat(
                "Reflected table query doesn't match the expected result.",
                schema.equals(Reflection.getTableSchema(TestObject.class, FieldNameFormatters.LOWER_SNAKE, TypeMappings.DEFAULT))
        );
    }

    @Test
    void schemaMatches() {
        final String expectedSchema = "test_string TEXT,test_decimal DOUBLE,test SMALLINT";

        class TestObject {
            @DataType("TEXT")
            private String testString = "test";

            @DataType("DOUBLE")
            private BigDecimal testDecimal = new BigDecimal(0);

            @DataType("SMALLINT")
            private boolean test = true;
        }
        org.hamcrest.MatcherAssert.assertThat(
                "Reflected table query doesn't match the expected result.",
                expectedSchema.equals(Reflection.getTableSchema(TestObject.class, FieldNameFormatters.LOWER_SNAKE, TypeMappings.DEFAULT))
        );
    }
}
