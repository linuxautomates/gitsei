import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.zendesk.models.Ticket;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class RequestAttributeTest {

    private static final String RESPONSE_FILE_NAME = "request-attributes.json";

    @Test
    public void deSerialize() throws IOException {
        Ticket.RequestAttributes requestAttributes = DefaultObjectMapper.get()
                .readValue(RequestAttributeTest.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        Ticket.RequestAttributes.class);
        assertThat(requestAttributes).isNotNull();
        assertThat(requestAttributes.getCanBeSolvedByMe()).isNull();
    }
}
