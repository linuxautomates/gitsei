package io.levelops.integrations.awsdevtools.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class CBBuildBatchTest {

    private static final String RESPONSE_FILE_NAME = "build_batches.json";

    @Test
    public void deSerialize() throws IOException {
        CBBuildBatch response = DefaultObjectMapper.get()
                .readValue(CBBuildBatch.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        CBBuildBatch.class);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
    }
}
