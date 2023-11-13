package io.levelops.etl.jobs.testrails;

import io.levelops.aggregations_shared.helpers.TestRailsAggHelperService;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.etl.job_framework.BaseIngestionResultProcessingStage;
import io.levelops.integrations.testrails.models.TestPlan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Date;

@Service
public class TestRailsTestPlanStage extends BaseIngestionResultProcessingStage<TestPlan, TestRailsJobState> {

    TestRailsAggHelperService helper;

    @Autowired
    public TestRailsTestPlanStage(TestRailsAggHelperService helper) {
        this.helper = helper;
    }

    @Override
    public void process(JobContext context, TestRailsJobState jobState, String ingestionJobId, TestPlan entity) throws SQLException {
        Date currentTime = new Date();
        helper.processTestRailsTestPlans(entity, context.getTenantId(), context.getIntegrationId(), context.getJobScheduledStartTime());
    }

    @Override
    public String getDataTypeName() {
        return "test_plans";
    }

    @Override
    public boolean onlyProcessLatestIngestionJob() {
        return false;
    }

    @Override
    public String getName() {
        return "TestRails TestPlan Stage";
    }

    @Override
    public void preStage(JobContext context, TestRailsJobState jobState) throws SQLException {

    }

    @Override
    public void postStage(JobContext context, TestRailsJobState jobState) throws SQLException {

    }
}
