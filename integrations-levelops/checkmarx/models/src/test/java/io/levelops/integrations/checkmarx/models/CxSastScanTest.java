package io.levelops.integrations.checkmarx.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CxSastScanTest {
    private static final String RESPONSE_FILE_NAME = "scan-response.json";

    @Test
    public void deSerialize() throws IOException {
        ObjectMapper mapper = DefaultObjectMapper.get();
        List<CxSastScan> response = mapper.readValue(CxSastScanTest.class.getClassLoader()
                .getResourceAsStream(RESPONSE_FILE_NAME), mapper.getTypeFactory()
                .constructCollectionType(List.class, CxSastScan.class));
        assertThat(response).isNotNull();
        assertThat(response.get(0).getId()).isNotNull();
    }
}
