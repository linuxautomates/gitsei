package io.levelops.ingestion.sinks;

import io.levelops.ingestion.data.BasicData;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestSinkTest {

    @Test
    public void test() {
        TestSink<Integer> sink = TestSink.forClass(Integer.class);
        sink.pushOne(null);
        sink.pushOne(BasicData.<Integer>builder()
                .dataClass(Integer.class)
                .payload(100)
                .build());
        assertThat(sink.getCapturedData()).hasSize(2);
    }
}