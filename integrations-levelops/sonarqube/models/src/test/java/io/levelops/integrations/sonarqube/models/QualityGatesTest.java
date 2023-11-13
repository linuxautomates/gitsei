package io.levelops.integrations.sonarqube.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class QualityGatesTest {

    private static final String RESPONSE_FILE_NAME = "quality-gate-response.json";

    @Test
    public void deSerialize() throws IOException {
        QualityGateResponse response = DefaultObjectMapper.get()
                .readValue(QualityGatesTest.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        QualityGateResponse.class);
        assertThat(response).isNotNull();
        assertThat(response.getQualitygates()).isNotNull();
    }
}