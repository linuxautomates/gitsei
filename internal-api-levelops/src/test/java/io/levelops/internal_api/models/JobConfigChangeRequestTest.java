package io.levelops.internal_api.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class JobConfigChangeRequestTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void test() throws JsonProcessingException {
        JobConfigChangeRequest r = JobConfigChangeRequest.builder()
                .jenkinsInstanceGuid("4297df67-0800-44b8-a857-1d85799bc04f").jenkinsInstanceName("Jenkins Instance").jenkinsInstanceUrl("http://localhost:8080/")
                .jobName("pipeline-1").jobFullName("pipeline-1/branches/master").jobNormalizedFullName("pipeline-1/master").branchName("master").moduleName(null)
                .repoUrl("https://github.com/viraj-levelops/pipeline-1").scmUserId(null)
                .changeType("changed").changeTime(1611792781000l).userId("admin").usersName("admin")
                .build();
        String data = MAPPER.writeValueAsString(r);
        Assert.assertNotNull(data);
    }
}