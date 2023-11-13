package io.levelops.utils;

import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.etl.job_framework.BaseIngestionResultEtlProcessor;
import io.levelops.etl.job_framework.IngestionResultProcessingStage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class TestEtlProcessor extends BaseIngestionResultEtlProcessor<TestEtlProcessor.TestJobState> {
    List<IngestionResultProcessingStage<?, TestJobState>> stages;

    public TestEtlProcessor(List<IngestionResultProcessingStage<?, TestJobState>> stages) {
        super(TestEtlProcessor.TestJobState.class);
        this.stages = stages;
    }

    @Override
    public void preProcess(JobContext context, TestEtlProcessor.TestJobState jobState) {
        log.info("Pre-processing");
        jobState.setListPopulatedBeforeJob(new ArrayList<>());
    }

    @Override
    public void postProcess(JobContext context, TestEtlProcessor.TestJobState jobState) {
        log.info("Post-processing");
        jobState.getListPopulatedBeforeJob().add(4);
    }

    @Override
    public List<IngestionResultProcessingStage<?, TestJobState>> getIngestionProcessingJobStages() {
        return this.stages;
    }

    @Override
    public TestEtlProcessor.TestJobState createState(JobContext context) {
        return new TestEtlProcessor.TestJobState();
    }

    @Getter
    @Setter
    public static class TestJobState {
        private List<Integer> listPopulatedBeforeJob;
        private String name;
    }
}

