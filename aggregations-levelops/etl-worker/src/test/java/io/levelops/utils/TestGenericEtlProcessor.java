package io.levelops.utils;

import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.etl.job_framework.BaseGenericEtlProcessor;
import io.levelops.etl.job_framework.GenericJobProcessingStage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;


@Log4j2
public class TestGenericEtlProcessor extends BaseGenericEtlProcessor<TestGenericEtlProcessor.TestJobState> {
    List<GenericJobProcessingStage<TestJobState>> stages;

    public TestGenericEtlProcessor(List<GenericJobProcessingStage<TestGenericEtlProcessor.TestJobState>> stages) {
        super(TestGenericEtlProcessor.TestJobState.class);
        this.stages = stages;
    }

    @Override
    public void preProcess(JobContext context, TestGenericEtlProcessor.TestJobState jobState) {
        log.info("Pre-processing");
        jobState.setListPopulatedBeforeJob(new ArrayList<>());
    }

    @Override
    public void postProcess(JobContext context, TestGenericEtlProcessor.TestJobState jobState) {
        log.info("Post-processing");
        jobState.getListPopulatedBeforeJob().add(4);
    }

    @Override
    public List<GenericJobProcessingStage<TestJobState>> getGenericJobProcessingStages() {
        return stages;
    }

    @Override
    public TestGenericEtlProcessor.TestJobState createState(JobContext context) {
        return new TestGenericEtlProcessor.TestJobState();
    }

    @Getter
    @Setter
    public static class TestJobState {
        private List<Integer> listPopulatedBeforeJob;
        private String name;
    }
}


