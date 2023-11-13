package io.levelops.etl.jobs.testrails;

import io.levelops.aggregations_shared.helpers.TestRailsAggHelperService;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.etl.job_framework.BaseIngestionResultProcessingStage;
import io.levelops.integrations.testrails.models.TestRailsTestSuite;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

@Log4j2
@Service
public class TestRailsTestCaseStage extends BaseIngestionResultProcessingStage<TestRailsTestSuite, TestRailsJobState>  {

    TestRailsAggHelperService helper;

    @Autowired
    public TestRailsTestCaseStage(TestRailsAggHelperService helper) {
        this.helper = helper;
    }

    @Override
    public void process(JobContext context, TestRailsJobState jobState, String ingestionJobId, TestRailsTestSuite entity) throws SQLException {
        helper.processTestRailsTestCases(entity, context.getTenantId(), context.getIntegrationId(), context.getJobScheduledStartTime());
    }

    @Override
    public String getDataTypeName() {
        return "test_suites";
    }

    @Override
    public boolean onlyProcessLatestIngestionJob() {
        return false;
    }

    @Override
    public String getName() {
        return "TestRails TestCase Stage";
    }

    @Override
    public void preStage(JobContext context, TestRailsJobState jobState) throws SQLException {
    }

    @Override
    public void postStage(JobContext context, TestRailsJobState jobState) throws SQLException {
    }
}
