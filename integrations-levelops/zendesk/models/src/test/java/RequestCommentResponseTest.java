import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.zendesk.models.RequestCommentResponse;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class RequestCommentResponseTest {

    private static final String RESPONSE_FILE_NAME = "request-comments.json";

    @Test
    public void deSerialize() throws IOException {
        RequestCommentResponse response = DefaultObjectMapper.get()
                .readValue(RequestCommentResponseTest.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        RequestCommentResponse.class);
        assertThat(response).isNotNull();
        assertThat(response.getComments()).isNotNull();
        assertThat(response.getComments()).hasSize(1);
    }
}
