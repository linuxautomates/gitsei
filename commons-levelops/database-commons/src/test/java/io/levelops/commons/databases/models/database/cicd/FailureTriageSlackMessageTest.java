package io.levelops.commons.databases.models.database.cicd;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;

import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("unused")
public class FailureTriageSlackMessageTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testSerialize() throws JsonProcessingException {
        UUID jobRunId1 = UUID.randomUUID();
        UUID jobRunId2 = UUID.randomUUID();
        UUID ruleId1 = UUID.randomUUID();
        UUID ruleId2 = UUID.randomUUID();
        FailureTriageSlackMessage.RuleHit ruleHit1 = FailureTriageSlackMessage.RuleHit.builder()
                .ruleId(ruleId1)
                .rule("rule-1")
                .matchesCount(1).snippet("aaa").build();
        FailureTriageSlackMessage.RuleHit ruleHit2 = FailureTriageSlackMessage.RuleHit.builder()
                .ruleId(ruleId1)
                .rule("rule-2")
                .matchesCount(2).snippet("aaa").build();

        FailureTriageSlackMessage.JobRun jobRun1 = FailureTriageSlackMessage.JobRun.builder()
                .jenkinsInstanceName("instance-1")
                .jobRunId(jobRunId1)
                .jobName("job-1")
                .jobRunNumber(1L)
                .ruleHits(List.of(ruleHit1))
                .runStartTime(Instant.now())
                .build();

        FailureTriageSlackMessage msg = FailureTriageSlackMessage.builder()
                .jobRuns(List.of(jobRun1))
                .build();

        FailureTriageSlackMessage.JobRun jobRun2 = FailureTriageSlackMessage.JobRun.builder()
                .jenkinsInstanceName("instance-1")
                .jobRunId(jobRunId1)
                .jobName("job-2")
                .jobRunNumber(2L)
                .ruleHits(List.of(ruleHit1))
                .runStartTime(Instant.now())
                .build();

        List<FailureTriageSlackMessage.JobRun> jobRuns = new ArrayList<>();
        jobRuns.add(jobRun1);
        jobRuns.add(jobRun2);

        msg = msg.toBuilder().jobRuns(jobRuns).build();

        String serialized = MAPPER.writeValueAsString(msg);
        Assert.assertNotNull(serialized);
    }
}