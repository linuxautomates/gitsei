package io.levelops.etl.jobs.jira;

import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.commons.databases.models.database.jira.DbJiraUser;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.databases.services.UserIdentityMaskingService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.etl.job_framework.BaseIngestionResultProcessingStage;
import io.levelops.integrations.jira.models.JiraUser;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

@Log4j2
@Service
public class JiraUsersStage extends BaseIngestionResultProcessingStage<JiraUser, JiraJobState> {
    private final JiraIssueService jiraIssueService;
    private final UserIdentityService userIdentityService;
    private final UserIdentityMaskingService userIdentityMaskingService;

    protected JiraUsersStage(JiraIssueService jiraIssueService,
                             UserIdentityService userIdentityService,
                             UserIdentityMaskingService userIdentityMaskingService) {
        this.jiraIssueService = jiraIssueService;
        this.userIdentityService = userIdentityService;
        this.userIdentityMaskingService = userIdentityMaskingService;
    }

    @Override
    public String getName() {
        return "Jira Users Stage";
    }

    @Override
    public void preStage(JobContext context, JiraJobState jobState) throws SQLException {

    }

    @Override
    public void process(JobContext context, JiraJobState jobState, String ingestionJobId, JiraUser entity) throws SQLException {
        DbJiraUser user = DbJiraUser.fromJiraUser(entity, context.getIntegrationId());
        jiraIssueService.insertJiraUser(context.getTenantId(), user);
        if (user.getDisplayName() != null) {
            try {
                String maskedUser = null;
                boolean isMasked = userIdentityMaskingService.isMasking(context.getTenantId(), context.getIntegrationId(), user.getJiraId(), user.getDisplayName());
                if (isMasked) {
                    maskedUser = userIdentityMaskingService.maskedUser(context.getTenantId());
                } else {
                    maskedUser = user.getDisplayName();
                }
                userIdentityService.batchUpsertIgnoreEmail(context.getTenantId(),
                        List.of(DbScmUser.builder()
                                .integrationId(user.getIntegrationId())
                                .cloudId(user.getJiraId())
                                .displayName(maskedUser)
                                .originalDisplayName(user.getDisplayName())
                                .build()));
            } catch (SQLException throwables) {
                log.error("Failed to insert into integration_users with display name: " + user.getDisplayName() + " , company: " + context.getTenantId() + ", integration id:" + context.getIntegrationId());
            }
        }
    }

    @Override
    public void postStage(JobContext context, JiraJobState jobState) {

    }

    @Override
    public String getDataTypeName() {
        return "users";
    }

    @Override
    public boolean onlyProcessLatestIngestionJob() {
        return true;
    }
}
