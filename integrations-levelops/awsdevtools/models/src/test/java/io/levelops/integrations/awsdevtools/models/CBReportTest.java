package io.levelops.integrations.awsdevtools.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class CBReportTest {

    private static final String RESPONSE_FILE_NAME = "reports.json";

    @Test
    public void deSerialize() throws IOException {
        CBReport response = DefaultObjectMapper.get()
                .readValue(CBReport.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        CBReport.class);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
    }
}
