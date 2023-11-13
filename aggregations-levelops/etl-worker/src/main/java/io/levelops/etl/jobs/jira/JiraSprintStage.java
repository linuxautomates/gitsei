package io.levelops.etl.jobs.jira;

import io.levelops.aggregations_shared.helpers.JiraAggHelperService;
import io.levelops.etl.job_framework.BaseIngestionResultProcessingStage;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.integrations.jira.models.JiraSprint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

import static io.levelops.aggregations_shared.helpers.JiraAggHelperService.DEFAULT_SPRINTS_DATATYPE;

@Service
public class JiraSprintStage extends BaseIngestionResultProcessingStage<JiraSprint, JiraJobState> {
    private final JiraAggHelperService jiraAggHelper;
    private final ControlPlaneService controlPlaneService;

    @Autowired
    public JiraSprintStage(JiraAggHelperService jiraAggHelper,
                           ControlPlaneService controlPlaneService) {
        this.jiraAggHelper = jiraAggHelper;
        this.controlPlaneService = controlPlaneService;
    }

    @Override
    public String getName() {
        return "Jira Sprint Stage";
    }

    @Override
    public void preStage(JobContext context, JiraJobState jobState) throws SQLException {

    }

    @Override
    public void process(JobContext context, JiraJobState state, String ingestionJobId, JiraSprint entity) {
        JobDTO ingestionJobDto = null;
        try {
            ingestionJobDto = context.getIngestionJobDto(ingestionJobId, controlPlaneService);
        } catch (IngestionServiceException e) {
            throw new RuntimeException(e);
        }
        jiraAggHelper.processJiraSprint(context.getTenantId(), context.getIntegrationId(), entity, ingestionJobDto.getCreatedAt());
    }

    @Override
    public void postStage(JobContext context, JiraJobState jobState) {

    }

    @Override
    public String getDataTypeName() {
        return DEFAULT_SPRINTS_DATATYPE;
    }

    @Override
    public boolean allowFailure() {
        return true;
    }

    @Override
    public boolean onlyProcessLatestIngestionJob() {
        return false;
    }
}
