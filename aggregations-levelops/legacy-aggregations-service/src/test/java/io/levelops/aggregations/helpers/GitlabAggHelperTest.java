package io.levelops.aggregations.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregations.parsers.JobDtoParser;
import io.levelops.aggregations.services.GitlabPipelineService;
import io.levelops.commons.databases.models.database.Integration;
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
import io.levelops.commons.models.DbListResponse;
import io.levelops.integrations.gitlab.models.GitlabProject;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
public class GitlabAggHelperTest {

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    @Mock
    JobDtoParser jobDtoParser;
    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private static GitlabPipelineService gitlabPipelineService;
    private static IntegrationService integrationService;
    private static DataSource dataSource;
    private static ScmAggService scmAggService;
    private static UserIdentityService userIdentityService;
    private static IntegrationTrackingService integrationTrackingService;
    private static GitlabAggHelper helper;
    private static final String company = "test";
    private static final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();

    @Before
    public void setup() throws IOException, URISyntaxException, SQLException {
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService = new IntegrationService(dataSource);
        integrationTrackingService = new IntegrationTrackingService(dataSource);
        CiCdJobsDatabaseService ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService = new CiCdJobRunsDatabaseService(OBJECT_MAPPER, dataSource);
        CiCdPipelinesAggsService ciCdPipelinesAggsService = new CiCdPipelinesAggsService(dataSource, ciCdJobRunsDatabaseService);
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        userIdentityService = new UserIdentityService(dataSource);
        CiCdJobRunStageDatabaseService ciCdJobRunStageDatabaseService = new CiCdJobRunStageDatabaseService(dataSource, OBJECT_MAPPER);
        CiCdJobRunStageStepsDatabaseService ciCdJobRunStageStepsDatabaseService = new CiCdJobRunStageStepsDatabaseService(dataSource);
        CiCdJobRunTestDatabaseService ciCdJobRunTestDatabaseService = new CiCdJobRunTestDatabaseService(dataSource);
        gitlabPipelineService = new GitlabPipelineService(dataSource, ciCdJobsDatabaseService,
                ciCdJobRunsDatabaseService, ciCdJobRunStageDatabaseService,
                ciCdJobRunStageStepsDatabaseService, ciCdJobRunTestDatabaseService);
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
        ciCdPipelinesAggsService.ensureTableExistence(company);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        ciCdJobRunsDatabaseService.ensureTableExistence(company);
        ciCdJobRunStageDatabaseService.ensureTableExistence(company);
        ciCdJobRunStageStepsDatabaseService.ensureTableExistence(company);
        ciCdJobRunTestDatabaseService.ensureTableExistence(company);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        scmAggService.ensureTableExistence(company);

        MockitoAnnotations.initMocks(this);
        helper = new GitlabAggHelper(jobDtoParser, scmAggService, gitlabPipelineService, userIdentityService, ciCdInstancesDatabaseService,integrationService);
        File testFile = new File(Objects.requireNonNull(this.getClass().getClassLoader().
                getResource("gitlab/gitlab_tags.json")).toURI());
        ObjectMapper mapper = DefaultObjectMapper.get();
        List<GitlabProject> projects = mapper.readValue(testFile,
                mapper.getTypeFactory().constructCollectionType(List.class, GitlabProject.class));
        projects.get(0).getTags()
                .stream()
                .filter(Objects::nonNull)
                .forEach(tag -> {
                    DbScmTag dbScmTag = DbScmTag.fromGitLabTag(tag, projects.get(0).getName(), "1");
                    try {
                        scmAggService.insertTag(company, dbScmTag);
                    } catch (SQLException e) {
                        log.error("Failed to insert the tag for customer:{}, integrationId:{}, repo:{}, tag:{}", company, 1, projects.get(0).getId(), tag.getName());
                    }
                });
    }

    @Test
    public void test() throws SQLException {
        DbListResponse<DbScmTag> dbScmTags = scmAggService.listTag(company, 0, 10);
        DefaultObjectMapper.prettyPrint(dbScmTags);
        Assert.assertNotNull(dbScmTags);
        Assert.assertEquals(3, dbScmTags.getRecords().size());
        List<String> tags = List.of("FirstTag", "SecondTag", "ThirdTag");
        assertThat(dbScmTags.getRecords().stream()
                .map(DbScmTag::getTag)
                .distinct()
                .collect(Collectors.toList())).containsExactlyInAnyOrder(tags.toArray(String[]::new));
    }
}