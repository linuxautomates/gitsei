package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ResourceUtils;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class CICDJobRunUnitTest {
    private final ObjectMapper MAPPER = DefaultObjectMapper.get();


    @Test
    public void testSerializeDeserialize() throws JsonProcessingException {
        List<CICDJobRun.JobRunParam> jobRunParams = new ArrayList<>();
        jobRunParams.add(
                CICDJobRun.JobRunParam.builder().type("StringParameterValue").name("env_name").value("UAT").build());
        jobRunParams.add(CICDJobRun.JobRunParam.builder().type("TextParameterValue").name("boker_ids")
                .value("broker1\\nbroker2\\nbroker3").build());
        CICDJobRun expected = CICDJobRun.builder().id(UUID.randomUUID()).cicdJobId(UUID.randomUUID()).jobRunNumber(113L)
                .status("SUCCESS").duration(12687).cicdUserId("viraj-jenkins")
                .scmCommitIds(Arrays.asList("d4f4f4de56368133cfb172eb74c8c400b3dc474e",
                        "9db5603eff6b99811650e4f24714697170bb1ea7", "72609d35a811e3b8b261f57c7c3fc99271405ebf"))
                .source(CICDJobRun.Source.ANALYTICS_PERIODIC_PUSH)
                .referenceId(UUID.randomUUID().toString())
                .logGcspath("somepath")
                .params(jobRunParams).build();
        String serialized = MAPPER.writeValueAsString(expected);
        Assert.assertNotNull(serialized);
        CICDJobRun actual = MAPPER.readValue(serialized, CICDJobRun.class);
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void tes() throws IOException {
        DbListResponse<CICDJob> results = ResourceUtils.getResourceAsObject(
            "samples/database/cicd_job_run_list.json",
            DefaultObjectMapper.get().getTypeFactory().constructParametricType(DbListResponse.class, CICDJobRun.class));
        Assertions.assertThat(results.getTotalCount()).isEqualTo(51);
    }

    @Test
    public void testSource() {
        for(CICDJobRun.Source source : CICDJobRun.Source.values()) {
            Assert.assertEquals(source, CICDJobRun.Source.fromString(source.toString()));
        }
        Assert.assertEquals(null, CICDJobRun.Source.fromString(null));
        Assert.assertEquals(null, CICDJobRun.Source.fromString(""));
        Assert.assertEquals(null, CICDJobRun.Source.fromString("does-not-exist"));
    }
}