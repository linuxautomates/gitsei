package io.levelops.utils;

import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.etl.job_framework.GenericJobProcessingStage;
import lombok.extern.log4j.Log4j2;

import java.sql.SQLException;

@Log4j2
public class SampleGenericJobStage implements GenericJobProcessingStage<TestGenericEtlProcessor.TestJobState> {
    public SampleGenericJobStage() {
    }

    @Override
    public String getName() {
        return "test";
    }

    @Override
    public void preStage(JobContext context, TestGenericEtlProcessor.TestJobState jobState) throws SQLException {
        jobState.getListPopulatedBeforeJob().add(1);
    }

    @Override
    public void postStage(JobContext context, TestGenericEtlProcessor.TestJobState jobState) throws SQLException {
        jobState.getListPopulatedBeforeJob().add(3);
    }

    @Override
    public void process(JobContext context, TestGenericEtlProcessor.TestJobState jobState) {
        log.info("Running job processing stage");
        jobState.setName("testing testing");
        jobState.getListPopulatedBeforeJob().add(2);
    }
}

