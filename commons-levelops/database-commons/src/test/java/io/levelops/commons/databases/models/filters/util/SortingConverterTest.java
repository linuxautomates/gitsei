package io.levelops.commons.databases.models.filters.util;

import io.levelops.commons.databases.models.database.SortingOrder;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class SortingConverterTest {
    @Test
    public void testCon(){
        var actual = SortingConverter.fromFilter(List.of(Map.of("id", "created_at", "desc", true)));
        var expected = Map.of("created_at", SortingOrder.DESC);
        Assertions.assertThat(actual).isEqualTo(expected);

        actual = SortingConverter.fromFilter(List.of(Map.of("id", "created_at", "desc", false), Map.of("id", "updated_at", "desc", true), Map.of("id", "id", "asc", true)));
        expected = Map.of("created_at", SortingOrder.ASC, "updated_at", SortingOrder.DESC, "id", SortingOrder.DESC);
        Assertions.assertThat(actual).isEqualTo(expected);

        actual = SortingConverter.fromFilter(null);
        expected = Map.of();
        Assertions.assertThat(actual).isEqualTo(expected);

        actual = SortingConverter.fromFilter(List.of());
        expected = Map.of();
        Assertions.assertThat(actual).isEqualTo(expected);

        actual = SortingConverter.fromFilter(List.of(Map.of()));
        expected = Map.of();
        Assertions.assertThat(actual).isEqualTo(expected);
    }

}