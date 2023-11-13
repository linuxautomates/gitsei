package io.levelops.commons.databases.models.database;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Map;

public class QueryFilterTest {
    @Test
    public void test(){
        var partial = Map.of("description", "p");
        var strict = Map.of("id", 12, "name", "name1");

        var filter = QueryFilter.fromRequestFilters(Map.of("id", 12, "name", "name1", "partial", partial));
        Assertions.assertThat(filter.getPartialMatches()).as("The partial match do not match (.filters.partial)").containsAllEntriesOf(partial);
        Assertions.assertThat(filter.getStrictMatches()).as("The partial match do not match (.filters.(*, !partial))").containsAllEntriesOf(strict);
    }
}