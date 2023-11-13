package io.levelops.aggregations.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunStageDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunStageStepsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.circleci.models.CircleCIBuild;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CircleCIBuildServiceTest {

    private static final String COMPANY = "circlecitest";

    private static CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private static CircleCIBuildService circleCIBuildService;

    private static final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();
    private static UUID instanceId;

    private static CiCdJobRunStageStepsDatabaseService ciCdJobRunStageStepsDatabaseService;

    private static CiCdJobRunStageDatabaseService ciCdJobRunStageDatabaseService;

    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;

    private static final String CIRCLECI_BUILDS_JSON = "circleci/circleci_builds.json";
    private final String CIRCLECI_REPO_JSON = "circleci/circleci_builds_updated.json";

    @BeforeClass
    public static void setup() throws IOException, SQLException {
        if (dataSource != null) {
            return;
        }
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);

        CiCdInstancesDatabaseService ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdJobRunStageStepsDatabaseService = new CiCdJobRunStageStepsDatabaseService(dataSource);
        ciCdJobRunsDatabaseService = new CiCdJobRunsDatabaseService(OBJECT_MAPPER, dataSource);
        ciCdJobRunStageDatabaseService = new CiCdJobRunStageDatabaseService(dataSource, OBJECT_MAPPER);
        circleCIBuildService = new CircleCIBuildService(ciCdJobsDatabaseService,
                ciCdJobRunsDatabaseService, ciCdJobRunStageDatabaseService,
                ciCdJobRunStageStepsDatabaseService);

        IntegrationService integrationService = new IntegrationService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService.ensureTableExistence(COMPANY);
        integrationService.insert(COMPANY, Integration.builder()
                .id("1")
                .application("circleci")
                .name("circleci-integ")
                .status("enabled")
                .build());

        ciCdInstancesDatabaseService.ensureTableExistence(COMPANY);
        ciCdJobsDatabaseService.ensureTableExistence(COMPANY);
        ciCdJobRunsDatabaseService.ensureTableExistence(COMPANY);
        ciCdJobRunStageDatabaseService.ensureTableExistence(COMPANY);
        ciCdJobRunStageStepsDatabaseService.ensureTableExistence(COMPANY);

        instanceId = UUID.randomUUID();
        ciCdInstancesDatabaseService.insert(COMPANY, CICDInstance.builder()
                .id(instanceId)
                .integrationId("1")
                .name("circleci-integration")
                .type(CICD_TYPE.circleci.toString())
                .build());
        insertDataIntoTable(CIRCLECI_BUILDS_JSON);
    }

    private static void insertDataIntoTable(String resourcePath) throws IOException {
        String projectsInput = ResourceUtils.getResourceAsString(resourcePath);
        PaginatedResponse<CircleCIBuild> repos = OBJECT_MAPPER.readValue(projectsInput, OBJECT_MAPPER.getTypeFactory()
                .constructParametricType(PaginatedResponse.class, CircleCIBuild.class));
        CircleCIBuild circleCIBuild = repos.getResponse().getRecords().get(0);
        String id = circleCIBuildService.insert(COMPANY, instanceId, circleCIBuild);
        if (id == null) {
            throw new RuntimeException("Failed to insert build of repo" + circleCIBuild.getRepoName());
        }
    }

    @Test
    public void testCircleCIDuplicateInsertion() throws IOException, SQLException {
        insertDataIntoTable(CIRCLECI_BUILDS_JSON);
        var listRepoResponse = ciCdJobsDatabaseService.list(COMPANY, 0, 10);
        var listBuildResponse = ciCdJobRunsDatabaseService.list(COMPANY, 0, 10);
        var listStepResponse = ciCdJobRunStageStepsDatabaseService.list(COMPANY, 0, 10);

        Assert.assertEquals(1, listRepoResponse.getRecords().size());
        Assert.assertEquals(1, listBuildResponse.getRecords().size());
        Assert.assertEquals(3, listStepResponse.getRecords().size());
    }

    @Test
    public void testCircleCIRepoDetails() throws SQLException {
        var listResponse = ciCdJobsDatabaseService.list(COMPANY, 0, 10);
        Assert.assertEquals(1, listResponse.getRecords().size());
        Assert.assertEquals("github/test/circleciRepo", listResponse.getRecords().get(0).getProjectName());
        Assert.assertEquals("job_name", listResponse.getRecords().get(0).getJobName());
        Assert.assertEquals("github/test/circleciRepo/job_name", listResponse.getRecords().get(0).getJobFullName());
        Assert.assertEquals("github/test/circleciRepo/job_name", listResponse.getRecords().get(0).getJobNormalizedFullName());
        Assert.assertEquals("main", listResponse.getRecords().get(0).getBranchName());
        Assert.assertEquals("test", listResponse.getRecords().get(0).getScmUserId());
    }

    @Test
    public void testCircleCIBuildDetails() throws SQLException {
        var listResponse = ciCdJobRunsDatabaseService.list(COMPANY, 0, 10);
        Assert.assertEquals(1, listResponse.getRecords().size());
        Assert.assertEquals(Long.valueOf(13), listResponse.getRecords().get(0).getJobRunNumber());
        Assert.assertEquals("success", listResponse.getRecords().get(0).getStatus());
        Assert.assertEquals("test", listResponse.getRecords().get(0).getCicdUserId());
        Assert.assertEquals(Integer.valueOf(18),listResponse.getRecords().get(0).getDuration());
        Assert.assertEquals(List.of("6432e0bc7134f1fd70f2b07621547fbc67aad822"), listResponse.getRecords().get(0).getScmCommitIds());
    }

    @Test
    public void testCircleCIStepDetails() throws SQLException {
        var listResponse = ciCdJobRunStageStepsDatabaseService.list(COMPANY, 0, 10);
        Assert.assertEquals(3, listResponse.getRecords().size());
        Assert.assertEquals(String.valueOf(101), listResponse.getRecords().get(0).getStepId());
        Assert.assertEquals("echo \"Say hi to YAML!\"", listResponse.getRecords().get(0).getDisplayName());
        Assert.assertEquals("success", listResponse.getRecords().get(0).getResult());
        Assert.assertEquals("success", listResponse.getRecords().get(0).getState());
    }

    @Test
    public void testCircleCIUpdatedRepos() throws SQLException, IOException {
        insertDataIntoTable(CIRCLECI_REPO_JSON);
        var listResponse = ciCdJobsDatabaseService.list(COMPANY, 0, 10);
        Assert.assertEquals(1, listResponse.getRecords().size());
    }

    @Test
    public void testCircleCIUpdatedSteps() throws SQLException, IOException {
        insertDataIntoTable(CIRCLECI_REPO_JSON);
        var listResponse = ciCdJobRunStageStepsDatabaseService.list(COMPANY, 0, 10);
        Assert.assertEquals(3, listResponse.getRecords().size());
        Assert.assertEquals("echo \"Say hi to YAML!\" 1", listResponse.getRecords().get(0).getDisplayName());
        Assert.assertEquals("success", listResponse.getRecords().get(0).getState());
        Assert.assertEquals("success", listResponse.getRecords().get(0).getResult());
    }
}
