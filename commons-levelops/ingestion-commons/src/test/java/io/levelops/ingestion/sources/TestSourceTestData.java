package io.levelops.ingestion.sources;

import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestSourceTestData {

    @Test
    public void test() {
        TestDataSource<Integer> source = TestDataSource.<Integer>builder()
                .dataClass(Integer.class)
                .dataEntry(BasicData.<Integer>builder()
                        .dataClass(Integer.class)
                        .payload(10)
                        .build())
                .dataEntry(BasicData.<Integer>builder()
                        .dataClass(Integer.class)
                        .payload(20)
                        .build())
                .dataEntry(BasicData.<Integer>builder()
                        .dataClass(Integer.class)
                        .payload(30)
                        .build())
                .build();
        assertThat(source.fetchOne(null).getPayload()).isEqualTo(10);
        assertThat(source.fetchOne(null).getPayload()).isEqualTo(20);
        assertThat(source.fetchOne(null).getPayload()).isEqualTo(30);
        assertThat(source.fetchOne(null).getPayload()).isEqualTo(10);
        assertThat(source.fetchMany(null).map(Data::getPayload)).containsExactly(10, 20, 30);

    }
}