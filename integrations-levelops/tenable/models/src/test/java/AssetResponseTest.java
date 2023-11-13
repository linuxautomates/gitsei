import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.tenable.models.Asset;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This unit test case ensures that asset response is deserializable in {@link Asset}.
 */
public class AssetResponseTest {
    private static final String RESPONSE_FILE_NAME = "asset.json";

    @Test
    public void deSerialize() throws IOException {
        Asset response = DefaultObjectMapper.get()
                .readValue(AssetResponseTest.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        Asset.class);
        assertThat(response).isNotNull();
    }
}
