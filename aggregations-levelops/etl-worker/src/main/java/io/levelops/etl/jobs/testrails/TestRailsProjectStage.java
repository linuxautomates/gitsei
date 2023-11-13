package io.levelops.etl.jobs.testrails;

import io.levelops.aggregations_shared.helpers.TestRailsAggHelperService;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.etl.job_framework.BaseIngestionResultProcessingStage;
import io.levelops.integrations.testrails.models.Project;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;

@Service
public class TestRailsProjectStage extends BaseIngestionResultProcessingStage<Project, TestRailsJobState> {

    TestRailsAggHelperService helper;

    @Autowired
    public TestRailsProjectStage(TestRailsAggHelperService helper) {
        this.helper = helper;
    }

    @Override
    public void process(JobContext context, TestRailsJobState jobState, String ingestionJobId, Project entity) throws SQLException {
        helper.processTestRailsProjects(entity, context.getTenantId(), context.getIntegrationId());
    }

    @Override
    public String getDataTypeName() {
        return "projects";
    }

    @Override
    public boolean onlyProcessLatestIngestionJob() {
        return false;
    }

    @Override
    public String getName() {
        return "TestRails Project Stage";
    }

    @Override
    public void preStage(JobContext context, TestRailsJobState jobState) throws SQLException {

    }

    @Override
    public void postStage(JobContext context, TestRailsJobState jobState) throws SQLException {

    }
}
