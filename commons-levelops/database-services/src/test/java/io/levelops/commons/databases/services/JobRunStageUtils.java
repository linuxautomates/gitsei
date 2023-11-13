package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.cicd.JobRunStage;
import io.levelops.commons.databases.models.database.cicd.PathSegment;
import io.levelops.commons.databases.models.database.cicd.SegmentType;
import org.junit.Assert;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class JobRunStageUtils {
    public static JobRunStage createJobRunStage(CiCdJobRunStageDatabaseService jobRunStageDatabaseService, CICDJobRun cicdJobRun, String company, int i) throws SQLException {
        JobRunStage jobRunStage = JobRunStage.builder()
                .ciCdJobRunId(cicdJobRun.getId())
                .stageId("stage-id-" + i)
                .name("name-" + i)
                .description("description-" + i)
                .result("result-" + i)
                .state("state-" + i)
                .duration(1234)
                .logs("log-" + i)
                .startTime(Instant.now())
                .url("https://jenkins.dev.levelops.io/" + i)
                .fullPath(Set.of(PathSegment.builder().id("id").name("name").position(1).type(SegmentType.CICD_STAGE).build()))
                .childJobRuns(Set.of(UUID.randomUUID()))
                .build();
        String id = jobRunStageDatabaseService.insert(company, jobRunStage);
        Assert.assertNotNull(id);
        return jobRunStage.toBuilder().id(UUID.fromString(id)).build();
    }

    public static List<JobRunStage> createJobRunStages(CiCdJobRunStageDatabaseService jobRunStageDatabaseService, CICDJobRun cicdJobRun, String company, int n) throws SQLException {
        List<JobRunStage> jobRunStages = new ArrayList<>();
        for(int i=0; i<n; i++) {
            JobRunStage jobRunStage = createJobRunStage(jobRunStageDatabaseService, cicdJobRun, company, i);
            jobRunStages.add(jobRunStage);
        }
        return jobRunStages;
    }
}
