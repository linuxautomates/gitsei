package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Set;

public class CICDJobTriggerTest {

    @Test
    public void test() throws JsonMappingException, JsonProcessingException {
        var triggerJson = "{ \"id\":\"Test2\",  \"job_run_number\":\"25\", \"type\":\"UpstreamCause\", \"direct_parents\": [{ \"id\":\"F2/jobs/Test2.2\",  \"job_run_number\":\"14\", \"type\":\"UpstreamCause\", \"direct_parents\": [{ \"id\":\"anonymous\", \"type\":\"UserIdCause\"}]}]}";
        var triggersJson = "[{ \"id\":\"Test2\",  \"job_run_number\":\"25\", \"type\":\"UpstreamCause\", \"direct_parents\": [{ \"id\":\"F2/jobs/Test2.2\",  \"job_run_number\":\"14\", \"type\":\"UpstreamCause\", \"direct_parents\": [{ \"id\":\"anonymous\", \"type\":\"UserIdCause\"}]}]}]";
        var trigger = DefaultObjectMapper.get().readValue(triggerJson, CICDJobTrigger.class);
        Set<CICDJobTrigger> triggers = DefaultObjectMapper.get().readValue(triggersJson, DefaultObjectMapper.get().getTypeFactory().constructCollectionType(Set.class, CICDJobTrigger.class));

        Assertions.assertThat(triggers.size()).isEqualTo(1);
        Assertions.assertThat(trigger.getId()).isEqualTo("Test2");
        Assertions.assertThat(DefaultObjectMapper.get().readValue("{ \"id\":\"Build-Server-API\",  \"job_run_number\":\"1012\", \"type\":\"UpstreamCause\", \"direct_parents\": [{ \"id\":\"unknown\", \"type\":\"GitHubPushCause\"}]}", CICDJobTrigger.class).getId()).isEqualTo("Build-Server-API");
    }
}