package io.levelops.commons.functional;

import org.junit.Test;

import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

public class IngestionResultTest {
    @Test
    public void testIngestionResult() {
        IngestionResult<Integer> results = IngestionResult.<Integer>builder()
                .data(List.of(1)).ingestionFailures(null).build();

        assertThat(results.getData()).isEqualTo(List.of(1));
        assertThat(results.getIngestionFailures()).isEqualTo(null);

        results = new IngestionResult<>(List.of(1), List.of(IngestionFailure.builder().build()));

        assertThat(results.getData()).isEqualTo(List.of(1));
        assertThat(results.getIngestionFailures().size()).isEqualTo(1);

        results = new IngestionResult<>(List.of(1),
                List.of(IngestionFailure.builder().message("Error while loading data")
                        .url("http://localhost/api")
                        .severity(IngestionFailure.Severity.ERROR).build()));

        assertThat(results.getData()).isEqualTo(List.of(1));
        assertThat(results.getIngestionFailures().get(0).getMessage()).isEqualTo("Error while loading data");
        assertThat(results.getIngestionFailures().get(0).getUrl()).isEqualTo("http://localhost/api");
        assertThat(results.getIngestionFailures().get(0).getSeverity()).isEqualTo(IngestionFailure.Severity.ERROR);
    }

}
