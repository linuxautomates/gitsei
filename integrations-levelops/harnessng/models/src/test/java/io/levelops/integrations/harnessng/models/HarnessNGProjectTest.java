package io.levelops.integrations.harnessng.models;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class HarnessNGProjectTest {

    @Test
    public void testDeserialize() throws IOException {

        ObjectMapper mapper = DefaultObjectMapper.get();
        HarnessNGAPIResponse<HarnessNGListAPIResponse<HarnessNGProject>> response = mapper.readValue(
                ResourceUtils.getResourceAsString("harnessng/harnessng_api_projects.json"),
                mapper.constructType(new TypeReference<HarnessNGAPIResponse<HarnessNGListAPIResponse<HarnessNGProject>>>() {})
        );
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getData());
        Assert.assertEquals(2, response.getData().getContent().size());

        HarnessNGProject apiRes = HarnessNGProject.builder()
                .project(HarnessNGProject.Project.builder()
                        .identifier("Test_Project")
                        .name("Test Project")
                        .orgIdentifier("test")
                        .build())
                .build();
        Assert.assertEquals(response.getData().getContent().get(0), apiRes);
    }
}
