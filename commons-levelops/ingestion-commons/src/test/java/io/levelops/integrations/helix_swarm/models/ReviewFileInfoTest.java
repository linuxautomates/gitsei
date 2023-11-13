package io.levelops.integrations.helix_swarm.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class ReviewFileInfoTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testSerializationDeserialization() throws JsonProcessingException {
        ReviewFileInfo expected = ReviewFileInfo.builder()
                .depotFile("//local/broadcom/test.txt")
                .action("add")
                .type("text")
                .rev("1")
                .fileSize("21")
                .digest("62659BDD975C7E7A0857D69DEC1E42FE")
                .build();
        String content = MAPPER.writeValueAsString(expected);
        ReviewFileInfo actual = MAPPER.readValue(content, ReviewFileInfo.class);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testDeserialization() throws IOException {
        String content = ResourceUtils.getResourceAsString("integrations/helix/helix_swarm_review_file_info_1.json");
        ReviewFileInfo actual = MAPPER.readValue(content, ReviewFileInfo.class);

        ReviewFileInfo expected = ReviewFileInfo.builder()
                .depotFile("//local/broadcom/test.txt")
                .action("add")
                .type("text")
                .rev("1")
                .fileSize("21")
                .digest("62659BDD975C7E7A0857D69DEC1E42FE")
                .build();

        Assert.assertEquals(expected, actual);
    }
}