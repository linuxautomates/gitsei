package io.levelops.integrations.harnessng.models;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

public class HarnessNGPipelineExecutionsTest {

    @Test
    public void testDeserialize() throws IOException {

        ObjectMapper mapper = DefaultObjectMapper.get();
        HarnessNGAPIResponse<HarnessNGListAPIResponse<HarnessNGPipeline>> response = mapper.readValue(
                ResourceUtils.getResourceAsString("harnessng/harnessng_api_pipeline_summary.json"),
                mapper.constructType(new TypeReference<HarnessNGAPIResponse<HarnessNGListAPIResponse<HarnessNGPipeline>>>() {})
        );


        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getData());
        Assert.assertEquals(5, response.getData().getContent().size());
        HarnessNGPipeline pipeline = HarnessNGPipeline.builder()
                .identifier("Messy_Pipeline")
                .executionId("bEdpBLFcR5iVx6Up4C9uDw")
                .name("Messy Pipeline")
                .status("Failed")
                .runSequence(3L)
                .executionTriggerInfo(HarnessNGPipeline.ExecutionTriggerInfo.builder()
                        .triggeredByUser(HarnessNGPipeline.ExecutionTriggerInfo.TriggeredUser.builder()
                                .uuid("q27LHq2XSSam3H09Rrqevg")
                                .identifier("Meetrajsinh Solanki")
                                .extraInfo(Map.of("email", "meetrajsinh.solanki@crestdatasys.com"))
                                .build())
                        .build())
                .startTs(1672649303018L)
                .endTs(1672649305628L)
                .createdAt(1672649303214L)
                .build();
        Assert.assertEquals(response.getData().getContent().get(0).getIdentifier(), pipeline.getIdentifier());
        Assert.assertEquals(response.getData().getContent().get(0).getStatus(), pipeline.getStatus());
        Assert.assertEquals(response.getData().getContent().get(0).getRunSequence(), pipeline.getRunSequence());
        Assert.assertEquals(response.getData().getContent().get(0).getExecutionTriggerInfo(), pipeline.getExecutionTriggerInfo());
    }
}
