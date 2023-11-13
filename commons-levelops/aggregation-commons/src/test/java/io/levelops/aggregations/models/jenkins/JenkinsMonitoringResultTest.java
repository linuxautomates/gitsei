package io.levelops.aggregations.models.jenkins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class JenkinsMonitoringResultTest {
    @Test
    public void testSerializeDeSerialize() throws JsonProcessingException {
        JobConfigChangeDetail jobConfigChangeDetail = JobConfigChangeDetail.builder()
                .changeTime(Instant.now().getEpochSecond()).operation("CREATED").userId("viraj").userFullName("Viraj Ajgaonkar")
                .build();
        JobAllConfigChanges jobAllConfigChanges = JobAllConfigChanges.builder()
                .jobName("Pipe 1").configChangeDetail(jobConfigChangeDetail).build();
        JobRunParam jobRunParam = JobRunParam.builder()
                .name("abc").type("pqr").value("xyz")
                .build();
        JobRunDetails jobRunDetails = JobRunDetails.builder()
                .number(10L).status("SUCCESS").startTime(Instant.now().getEpochSecond()).duration(100L).userId("viraj").commitIds(List.of("commitId1", "commitId2")).param(jobRunParam).build();
        JobAllRuns jobAllRuns = JobAllRuns.builder()
                .jobName("pipeline-1").jobFullName("pipeline-1/branches/master").jobNormalizedFullName("pipeline-1/master").scmUrl("scmUrl1").scmUserId("scmUserId").run(jobRunDetails).build();

        JenkinsMonitoringResult jenkinsMonitoringResult = JenkinsMonitoringResult.builder()
                .jenkinsInstanceGuid(UUID.randomUUID())
                .jenkinsInstanceName("Jenkins Instance US")
                .jenkinsInstanceUrl("https://jenkins.dev.levelops.io/")
                .configChange(jobAllConfigChanges)
                .jobRun(jobAllRuns)
                .build();

        ObjectMapper objectMapper = new ObjectMapper();
        String serialized = objectMapper.writeValueAsString(jenkinsMonitoringResult);
        JenkinsMonitoringResult actual = objectMapper.readValue(serialized, JenkinsMonitoringResult.class);
        Assert.assertEquals(jenkinsMonitoringResult, actual);
    }
}