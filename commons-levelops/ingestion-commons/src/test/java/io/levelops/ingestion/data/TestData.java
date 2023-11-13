package io.levelops.ingestion.data;

import io.levelops.commons.functional.IngestionFailure;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TestData {

    @Test
    public void testIngestionData() {
        List<IngestionData<Integer>> emptyIngestionData = List.of(IngestionData.<Integer>builder()
                .records(List.of())
                .build());

        assertThat(emptyIngestionData.get(0).isPresent()).isEqualTo(false);

        List<Data<Integer>> ingestionData = List.of(IngestionData.<Integer>builder()
                .records(List.of(10))
                .build());

        assertThat(ingestionData.get(0).isPresent()).isEqualTo(true);

        ingestionData = List.of(IngestionData.<Integer>builder()
                .records(List.of())
                .ingestionFailures(List.of(IngestionFailure.builder().severity(IngestionFailure.Severity.ERROR).build()))
                .build());

        assertThat(ingestionData.get(0).isPresent()).isEqualTo(true);

        ingestionData = List.of(
                IngestionData.of(Integer.class, List.of(10, 20), null),
                IngestionData.of(Integer.class, null, List.of(IngestionFailure.builder().build())),
                IngestionData.of(Integer.class, List.of(30), List.of(IngestionFailure.builder().build()))
        );

        ImmutablePair<List<Integer>, List<IngestionFailure>> ingestionDataPair =
                Data.collectData(ingestionData.stream());

        List<Integer> records = ingestionDataPair.getLeft();
        List<IngestionFailure> failures = ingestionDataPair.getRight();

        assertThat(records.size()).isEqualTo(3);
        assertThat(failures.size()).isEqualTo(2);
    }
}
