package io.levelops.etl.jobs.gitlab;

import com.google.common.collect.Lists;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.etl.job_framework.BaseIngestionResultProcessingStage;
import io.levelops.integrations.gitlab.models.GitlabUser;
import io.micrometer.core.instrument.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GitlabUserStage extends BaseIngestionResultProcessingStage<GitlabUser, GitlabState> {
    private final UserIdentityService userIdentityService;

    @Autowired
    public GitlabUserStage(UserIdentityService userIdentityService) {
        this.userIdentityService = userIdentityService;
    }

    @Override
    public void process(JobContext context, GitlabState jobState, String ingestionJobId, GitlabUser entity) throws SQLException {
        Set<String> emails = new HashSet<>();
        if (StringUtils.isNotBlank(entity.getEmail())) {
            emails.add(entity.getEmail());
        }
        if (StringUtils.isNotBlank(entity.getPublicEmail())) {
            emails.add(entity.getPublicEmail());
        }

        var originalUser = userIdentityService
                .getUserByCloudId(context.getTenantId(), context.getIntegrationId(), entity.getUsername());

        if (originalUser.isPresent() && originalUser.get().getEmails() != null) {
            emails.addAll(originalUser.get().getEmails());
        }

        DbScmUser user = DbScmUser.builder()
                .integrationId(context.getIntegrationId())
                .cloudId(entity.getUsername())
                .displayName(entity.getName())
                .originalDisplayName(entity.getName())
                .emails(emails.stream().toList())
                .build();
        jobState.getUsers().add(user);
    }

    @Override
    public String getDataTypeName() {
        return "users";
    }

    @Override
    public boolean onlyProcessLatestIngestionJob() {
        return false;
    }

    @Override
    public String getName() {
        return "Gitlab Users Stage";
    }

    @Override
    public void preStage(JobContext context, GitlabState jobState) throws SQLException {

    }

    @Override
    public void postStage(JobContext context, GitlabState jobState) throws SQLException {
        // Write 200 users at a time
        for (List<DbScmUser> users : Lists.partition(jobState.getUsers(), 200)) {
            userIdentityService.batchUpsert(context.getTenantId(), users);
        }
    }
}
