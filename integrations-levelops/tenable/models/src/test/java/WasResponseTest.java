import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.tenable.models.WASResponse;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This unit test case ensures that web application scanning vulnerability response is deserializable in {@link WASResponse}.
 */
public class WasResponseTest {
    private static final String RESPONSE_FILE_NAME = "was-vuln.json";
    private static final int EXPECTED_DATA = 1;

    @Test
    public void deSerialize() throws IOException {
        WASResponse response = DefaultObjectMapper.get()
                .readValue(WasResponseTest.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        WASResponse.class);
        assertThat(response).isNotNull();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData()).hasSize(EXPECTED_DATA);
    }
}
