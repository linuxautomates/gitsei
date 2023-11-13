package io.levelops.aggregations.models.jenkins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class JobAllConfigChangesTest {
    @Test
    public void testSerializeDeSerialize() throws JsonProcessingException {
        JobConfigChangeDetail jobConfigChangeDetail = JobConfigChangeDetail.builder()
                .changeTime(1595811932L).operation("CREATED").userId("viraj").userFullName("Viraj Ajgaonkar")
                .build();
        JobAllConfigChanges jobAllConfigChanges = JobAllConfigChanges.builder()
                .jobName("pipeline-1").jobFullName("pipeline-1/branches/master").jobNormalizedFullName("pipeline-1/master").configChangeDetail(jobConfigChangeDetail).build();
        ObjectMapper objectMapper = new ObjectMapper();
        String serialized = objectMapper.writeValueAsString(jobAllConfigChanges);
        Assert.assertEquals(serialized, "{\"job_name\":\"pipeline-1\",\"job_full_name\":\"pipeline-1/branches/master\",\"job_normalized_full_name\":\"pipeline-1/master\",\"config_change_details\":[{\"change_time\":1595811932,\"operation\":\"CREATED\",\"user_id\":\"viraj\",\"user_full_name\":\"Viraj Ajgaonkar\"}]}");
        JobAllConfigChanges actual = objectMapper.readValue(serialized, JobAllConfigChanges.class);
        Assert.assertEquals(jobAllConfigChanges, actual);
    }

}