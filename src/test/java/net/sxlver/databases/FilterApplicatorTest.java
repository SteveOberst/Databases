package net.sxlver.databases;

import net.sxlver.databases.impl.DatabaseEntryIdentifier;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

public class FilterApplicatorTest {
    @Test
    void testFilterApplicator() {
        final DatabaseEntryIdentifier identifier = DatabaseEntryIdentifier.of("Sxlver");
        final Predicate<String> filterApplicator = identifier.getFilterApplicator();
        final String uniqueId = "f4434f79-a872-48fa-a981-c299ef2f2392_Sxlver_1";
        MatcherAssert.assertThat(
                "Could not verify unique id to match filter.",
                filterApplicator.test(uniqueId)
        );
    }
}
