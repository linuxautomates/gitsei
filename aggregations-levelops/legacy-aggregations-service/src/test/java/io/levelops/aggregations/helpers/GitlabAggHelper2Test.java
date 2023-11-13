package io.levelops.aggregations.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregations.services.GitlabPipelineService;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmTag;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunStageDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunStageStepsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunTestDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.commons.databases.services.CiCdPipelinesAggsService;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.gitlab.models.GitlabProject;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

@Log4j2
public class GitlabAggHelper2Test {
    private static final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();
    private static final String company = "test";

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static DataSource dataSource;
    private static UserIdentityService userIdentityService;
    private static ScmAggService scmAggService;

    @Before
    public void setup() throws IOException, URISyntaxException, SQLException {
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);

        userIdentityService = new UserIdentityService(dataSource);
        UserService userService = new UserService(dataSource, OBJECT_MAPPER);
        IntegrationService integrationService = new IntegrationService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        integrationService.insert(company, Integration.builder()
                .id("1")
                .application("gitlab")
                .name("gitlab-integ")
                .status("enabled")
                .build());
        userIdentityService.ensureTableExistence(company);
        userService.ensureTableExistence(company);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        scmAggService.ensureTableExistence(company);

        MockitoAnnotations.initMocks(this);
    }
    @Test
    public void test() throws IOException, SQLException {
        String integrationId = "1";
        GitlabProject project = OBJECT_MAPPER.readValue(ResourceUtils.getResourceAsString("gitlab/gitlab_prs.json"), GitlabProject.class);
        DbScmPullRequest gitlabMergeRequest = DbScmPullRequest.fromGitlabMergeRequest(
                project.getMergeRequests().get(0), project.getPathWithNamespace(), integrationId,null);
        Assert.assertNotNull(gitlabMergeRequest);
        String id = scmAggService.insert(company, gitlabMergeRequest);
        Assert.assertNotNull(id);
    }
}