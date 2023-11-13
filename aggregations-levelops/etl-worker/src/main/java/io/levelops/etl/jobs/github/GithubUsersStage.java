package io.levelops.etl.jobs.github;

import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.etl.job_framework.BaseIngestionResultProcessingStage;
import io.levelops.integrations.github.models.GithubUser;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

@Service
@Log4j2
public class GithubUsersStage extends BaseIngestionResultProcessingStage<GithubUser, GithubJobState> {
    private final UserIdentityService userIdentityService;
    private final MeterRegistry meterRegistry;

    public GithubUsersStage(
            UserIdentityService userIdentityService,
            MeterRegistry meterRegistry
    ) {
        this.userIdentityService = userIdentityService;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void process(JobContext context, GithubJobState jobState, String ingestionJobId, GithubUser entity) throws SQLException {
        DbScmUser dbScmUser = DbScmUser.fromGithubUser(entity, context.getIntegrationId());
        String userId = userIdentityService.insert(context.getTenantId(), dbScmUser);
        if (userId != null) {
            meterRegistry.counter("etl.worker.github.user.upserted").increment();
        }
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
        return "Github Users Stage";
    }

    @Override
    public void preStage(JobContext context, GithubJobState jobState) throws SQLException {

    }

    @Override
    public void postStage(JobContext context, GithubJobState jobState) throws SQLException {

    }
}
