package io.levelops.etl.jobs.jira;

import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.commons.databases.models.database.jira.DbJiraStatusMetadata;
import io.levelops.commons.databases.services.JiraStatusMetadataDatabaseService;
import io.levelops.etl.job_framework.BaseIngestionResultProcessingStage;
import io.levelops.integrations.jira.models.JiraIssueFields;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

@Log4j2
@Service
public class JiraStatusStage extends BaseIngestionResultProcessingStage<JiraIssueFields.JiraStatus, JiraJobState> {
    private final JiraStatusMetadataDatabaseService statusMetadataDatabaseService;

    protected JiraStatusStage(JiraStatusMetadataDatabaseService statusMetadataDatabaseService) {
        this.statusMetadataDatabaseService = statusMetadataDatabaseService;
    }

    @Override
    public String getName() {
        return "Jira Status Stage";
    }

    @Override
    public void preStage(JobContext context, JiraJobState jobState) throws SQLException {
    }

    @Override
    public void process(JobContext context, JiraJobState jobState, String ingestionJobId, JiraIssueFields.JiraStatus entity) throws SQLException {
        DbJiraStatusMetadata metadata = DbJiraStatusMetadata.fromJiraStatus(context.getIntegrationId(), entity);
        statusMetadataDatabaseService.upsert(context.getTenantId(), metadata);
    }

    @Override
    public void postStage(JobContext context, JiraJobState jobState) {
    }

    @Override
    public String getDataTypeName() {
        return "statuses";
    }

    @Override
    public boolean onlyProcessLatestIngestionJob() {
        return true;
    }
}
