package io.levelops.aggregations.models.messages;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class JenkinsPluginJobRunCompleteMessageTest {
    public final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void test() throws JsonProcessingException {
        JenkinsPluginJobRunCompleteMessage expected = JenkinsPluginJobRunCompleteMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .customer("test")
                .outputBucket("bucket_name")
                .jsonFilePath("json_path")
                .resultFilePath("result_path").build();

        String serialized = MAPPER.writeValueAsString(expected);

        JenkinsPluginJobRunCompleteMessage actual = MAPPER.readValue(serialized, JenkinsPluginJobRunCompleteMessage.class);
        Assert.assertEquals(expected, actual);
    }

}