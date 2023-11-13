import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.tenable.models.NetworkResponse;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This unit test case ensures that network response response is deserializable in {@link NetworkResponse}.
 */
public class NetworkResponseTest {
    private static final String RESPONSE_FILE_NAME = "network-response.json";
    private static final int EXPECTED_NETWORKS = 1;

    @Test
    public void deSerialize() throws IOException {
        NetworkResponse response = DefaultObjectMapper.get()
                .readValue(NetworkResponseTest.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        NetworkResponse.class);
        assertThat(response).isNotNull();
        assertThat(response.getNetworks()).isNotNull();
        assertThat(response.getNetworks()).hasSize(EXPECTED_NETWORKS);
    }
}
