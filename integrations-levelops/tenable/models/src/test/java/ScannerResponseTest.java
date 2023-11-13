import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.tenable.models.ScannerResponse;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This unit test case ensures that scanner response is deserializable in {@link ScannerResponse}.
 */
public class ScannerResponseTest {
    private static final String RESPONSE_FILE_NAME = "scanner-response.json";
    private static final int EXPECTED_SCANNERS = 3;

    @Test
    public void deSerialize() throws IOException {
        ScannerResponse response = DefaultObjectMapper.get()
                .readValue(ScannerResponseTest.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        ScannerResponse.class);
        assertThat(response).isNotNull();
        assertThat(response.getScanners()).isNotNull();
        assertThat(response.getScanners()).hasSize(EXPECTED_SCANNERS);
    }
}
