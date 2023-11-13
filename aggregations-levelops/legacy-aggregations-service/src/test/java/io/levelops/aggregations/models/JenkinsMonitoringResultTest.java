package io.levelops.aggregations.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.levelops.aggregations.models.jenkins.JenkinsMonitoringResult;
import io.levelops.aggregations.models.jenkins.JobAllConfigChanges;
import io.levelops.aggregations.models.jenkins.JobAllRuns;
import io.levelops.aggregations.models.jenkins.JobConfigChangeDetail;
import io.levelops.aggregations.models.jenkins.JobRunDetails;
import io.levelops.aggregations.models.jenkins.JobRunParam;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;

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
                .number(10L).status("SUCCESS").startTime(Instant.now().getEpochSecond()).duration(100L).userId("viraj").param(jobRunParam).build();
        JobAllRuns jobAllRuns = JobAllRuns.builder()
                .jobName("Pipe 1").run(jobRunDetails).build();

        JenkinsMonitoringResult jenkinsMonitoringResult = JenkinsMonitoringResult.builder()
                .configChange(jobAllConfigChanges)
                .jobRun(jobAllRuns)
                .build();

        ObjectMapper objectMapper = new ObjectMapper();
        String serialized = objectMapper.writeValueAsString(jenkinsMonitoringResult);
        JenkinsMonitoringResult actual = objectMapper.readValue(serialized, JenkinsMonitoringResult.class);
        Assert.assertEquals(jenkinsMonitoringResult, actual);
    }
}