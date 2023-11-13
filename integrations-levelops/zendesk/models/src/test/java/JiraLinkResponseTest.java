import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.zendesk.models.GetJiraLinkResponse;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraLinkResponseTest {

    private static final String RESPONSE_FILE_NAME = "jira-link.json";

    @Test
    public void deSerialise() throws IOException {
        GetJiraLinkResponse response = DefaultObjectMapper.get()
                .readValue(JiraLinkResponseTest.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        GetJiraLinkResponse.class);
        assertThat(response).isNotNull();
        assertThat(response.getLinks()).isNotNull();
        assertThat(response.getLinks()).hasSize(1);
        assertThat(response.getTotal()).isEqualTo(1);
    }
}
