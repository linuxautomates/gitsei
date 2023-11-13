package io.levelops.etl.jobs.testrails;

import io.levelops.aggregations_shared.helpers.TestRailsAggHelperService;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.etl.job_framework.BaseIngestionResultProcessingStage;
import io.levelops.integrations.testrails.models.CaseField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Date;

@Service
public class TestRailsCaseFieldStage extends BaseIngestionResultProcessingStage<CaseField, TestRailsJobState>  {

    TestRailsAggHelperService helper;

    @Autowired
    public TestRailsCaseFieldStage(TestRailsAggHelperService helper) {
        this.helper = helper;
    }

    @Override
    public void process(JobContext context, TestRailsJobState jobState, String ingestionJobId, CaseField entity) throws SQLException {
        helper.processTestRailsCaseFields(entity, context.getTenantId(), context.getIntegrationId(), context.getJobScheduledStartTime());
    }

    @Override
    public String getDataTypeName() {
        return "case_fields";
    }

    @Override
    public boolean onlyProcessLatestIngestionJob() {
        return false;
    }

    @Override
    public String getName() {
        return "TestRails CaseField Stage";
    }

    @Override
    public void preStage(JobContext context, TestRailsJobState jobState) throws SQLException {

    }

    @Override
    public void postStage(JobContext context, TestRailsJobState jobState) throws SQLException {

    }
}
