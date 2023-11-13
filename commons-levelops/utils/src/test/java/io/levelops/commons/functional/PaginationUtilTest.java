package io.levelops.commons.functional;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.assertThat;

public class PaginationUtilTest {
    @Test
    public void testStreamData() {
        Function<Integer, IngestionResult<Integer>> identity = val -> {
            if(val == 2)
                return null;
            else {
                return IngestionResult.<Integer>builder().data(List.of(1)).ingestionFailures(null).build();
            }
        };

        Stream<IngestionResult<Integer>> ingestionResultStream =
                PaginationUtils.streamData(1, 1, identity);

        List<Integer> result = new ArrayList<>();
        ingestionResultStream.forEach(ingestionResult -> {
            if(CollectionUtils.isNotEmpty(ingestionResult.getData()))
                result.addAll(ingestionResult.getData());
        });

        assertThat(result.size()).isEqualTo(1);

    }
}
