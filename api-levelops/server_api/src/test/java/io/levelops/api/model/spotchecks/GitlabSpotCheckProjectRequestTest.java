package io.levelops.api.model.spotchecks;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class GitlabSpotCheckProjectRequestTest {
    private final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testSerialize() throws JsonProcessingException {
        GitlabSpotCheckProjectRequest expected = GitlabSpotCheckProjectRequest.builder()
                .integrationId(4327).projectName("juhi-test")
                .from("05/01/2023").to("05/31/2023")
                .limit(10)
                .build();
        String data = MAPPER.writeValueAsString(expected);
        GitlabSpotCheckProjectRequest actual = MAPPER.readValue(data, GitlabSpotCheckProjectRequest.class);
        Assert.assertEquals(actual, expected);
    }
}