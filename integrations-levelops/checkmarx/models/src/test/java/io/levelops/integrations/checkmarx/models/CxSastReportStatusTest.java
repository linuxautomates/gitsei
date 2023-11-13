package io.levelops.integrations.checkmarx.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CxSastReportStatusTest {
    private static final String RESPONSE_FILE_NAME = "scan-status-response.json";

    @Test
    public void deSerialize() throws IOException {
        ObjectMapper mapper = DefaultObjectMapper.get();
        CxSastReportStatus response = mapper.readValue(CxSastReportStatusTest.class.getClassLoader()
                .getResourceAsStream(RESPONSE_FILE_NAME), CxSastReportStatus.class);
        assertThat(response).isNotNull();
    }
}
