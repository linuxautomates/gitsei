package io.levelops.ingestion.exceptions;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ResumableIngestExceptionTest {

    @Test
    public void test() {
        assertThatThrownBy(() -> {throw ResumableIngestException.builder()
                .customMessage("test")
                .build(); })
                .hasMessage("test (result=false, intermediate_state=false)");

        assertThatThrownBy(() -> {throw ResumableIngestException.builder()
                .build(); })
                .hasMessage("Resumable error (result=false, intermediate_state=false)");
    }
}