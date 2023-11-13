package io.levelops.integrations.gerrit.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class ChangesInfoTest {

    private static final String RESPONSE_FILE_NAME = "changes.json";
    private static final int EXPECTED_NUM_CHANGES = 4;

    @Test
    public void deSerialize() throws IOException {
        List<ChangeInfo> response = DefaultObjectMapper.get()
                .readValue(DateFormatter.formatDates(new String(ChangesInfoTest.class.getClassLoader()
                                .getResourceAsStream(RESPONSE_FILE_NAME).readAllBytes(), UTF_8)),
                        DefaultObjectMapper.get().getTypeFactory().constructParametricType(List.class, ChangeInfo.class));
        assertThat(response).isNotNull();
        assertThat(response).hasSize(EXPECTED_NUM_CHANGES);
    }


}
