package io.levelops.utils;

import io.levelops.etl.job_framework.BaseIngestionResultProcessingStage;
import io.levelops.aggregations_shared.models.JobContext;
import lombok.Builder;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

import java.sql.SQLException;

@Log4j2
public class SampleJobStage extends BaseIngestionResultProcessingStage<SampleJobStage.ExampleSerialized, TestEtlProcessor.TestJobState> {
    public SampleJobStage() {
    }

    @Override
    public String getName() {
        return "test";
    }

    @Override
    public void preStage(JobContext context, TestEtlProcessor.TestJobState jobState) throws SQLException {
        jobState.getListPopulatedBeforeJob().add(1);
    }

    @Override
    public void postStage(JobContext context, TestEtlProcessor.TestJobState jobState) {
        jobState.getListPopulatedBeforeJob().add(3);
    }

    @Override
    public void process(JobContext context, TestEtlProcessor.TestJobState jobState, String ingestionJobId, SampleJobStage.ExampleSerialized entity) throws SQLException {
        log.info("Running job processing stage");
        jobState.setName("testing testing");
        jobState.getListPopulatedBeforeJob().add(2);
    }

    @Override
    public String getDataTypeName() {
        return "jira";
    }

    @Override
    public boolean onlyProcessLatestIngestionJob() {
        return false;
    }

    @Builder
    @Data
    public static class ExampleSerialized {
        String name;
    }
}