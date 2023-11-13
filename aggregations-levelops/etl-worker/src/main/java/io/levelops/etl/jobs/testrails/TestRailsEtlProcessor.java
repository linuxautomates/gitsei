package io.levelops.etl.jobs.testrails;

import io.levelops.aggregations_shared.helpers.TestRailsAggHelperService;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.etl.job_framework.BaseIngestionResultEtlProcessor;
import io.levelops.etl.job_framework.IngestionResultProcessingStage;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

import java.util.Date;
import java.util.List;
@Log4j2
@Service
public class TestRailsEtlProcessor extends BaseIngestionResultEtlProcessor<TestRailsJobState> {

    private final TestRailsTestCaseStage testRailsTestCaseStage;
    private final TestRailsCaseFieldStage testRailsCaseFieldStage;
    private final TestRailsTestPlanStage testRailsTestPlanStage;
    private final TestRailsTestRunStage testRailsTestRunStage;
    private final TestRailsProjectStage testRailsProjectStage;
    private final TestRailsAggHelperService helper;

    @Autowired
    public TestRailsEtlProcessor(TestRailsTestCaseStage testRailsTestCaseStage,
                                 TestRailsCaseFieldStage testRailsCaseFieldStage,
                                 TestRailsTestPlanStage testRailsTestPlanStage,
                                 TestRailsTestRunStage testRailsTestRunStage,
                                 TestRailsProjectStage testRailsProjectStage,
                                 TestRailsAggHelperService helper) {
        super(TestRailsJobState.class);
        this.testRailsTestCaseStage = testRailsTestCaseStage;
        this.testRailsCaseFieldStage = testRailsCaseFieldStage;
        this.testRailsTestPlanStage = testRailsTestPlanStage;
        this.testRailsTestRunStage = testRailsTestRunStage;
        this.testRailsProjectStage = testRailsProjectStage;
        this.helper = helper;
    }
    @Override
    public List<IngestionResultProcessingStage<?, TestRailsJobState>> getIngestionProcessingJobStages() {
        return List.of(testRailsTestCaseStage, testRailsCaseFieldStage, testRailsTestPlanStage, testRailsTestRunStage, testRailsProjectStage);
    }
    @Override
    public void preProcess(JobContext context, TestRailsJobState jobState) {
        try {
            helper.createTempTable(context.getTenantId(), context.getIntegrationId(), context.getJobScheduledStartTime());
        } catch (RuntimeException e) {
            log.error("preProcess: Failed to create table for tenant: " + context.getTenantId()
                    + "integration_id: " + context.getIntegrationId());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void postProcess(JobContext context, TestRailsJobState jobState) {
        try {
            int noOfRecords = helper.deleteTestCaseRecords(context.getTenantId(), context.getIntegrationId(), context.getJobScheduledStartTime());
            log.debug("Successfully pruned " + noOfRecords + " no of test cases for tenant: " + context.getTenantId() + " integration_id: " + context.getIntegrationId());
            noOfRecords = helper.deleteTestRecords(context.getTenantId(), context.getIntegrationId(), context.getJobScheduledStartTime());
            log.debug("Successfully pruned " + noOfRecords + " no of tests for tenant: " + context.getTenantId() + " integration_id: " + context.getIntegrationId());
            noOfRecords = helper.deleteCaseFieldRecords(context.getTenantId(), context.getIntegrationId(), context.getJobScheduledStartTime());
            log.debug("Successfully pruned " + noOfRecords + " no of case fields for tenant: " + context.getTenantId() + " integration_id: " + context.getIntegrationId());
        } catch (RuntimeException e) {
            log.error("postProcess: Failed to delete records of test cases for tenant: " + context.getTenantId()
                    + "integration_id: " + context.getIntegrationId(), e);
        }
        finally{
            helper.dropTempTable(context.getTenantId(), context.getIntegrationId(), context.getJobScheduledStartTime());
        }
    }

    @Override
    public TestRailsJobState createState(JobContext context) {
        return new TestRailsJobState();
    }
}
