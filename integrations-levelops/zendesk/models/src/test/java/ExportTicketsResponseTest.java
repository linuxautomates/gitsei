import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.zendesk.models.ExportTicketsResponse;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class ExportTicketsResponseTest {

    private static final String RESPONSE_FILE_NAME = "export-tickets.json";
    private static final String CUSTOM_CURSOR = "customCursor";
    private static final int EXPECTED_NUM_TICKETS = 2;

    @Test
    public void deSerialize() throws IOException {
        ExportTicketsResponse response = DefaultObjectMapper.get()
                .readValue(ExportTicketsResponseTest.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        ExportTicketsResponse.class);
        assertThat(response).isNotNull();
        assertThat(response.getTickets()).isNotNull();
        assertThat(response.getTickets()).hasSize(EXPECTED_NUM_TICKETS);
        assertThat(response.getAfterCursor()).isEqualTo(CUSTOM_CURSOR);
    }
}
