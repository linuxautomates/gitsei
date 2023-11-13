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
import io.levelops.integrations.droneci.models.DroneCIEnrichRepoData;
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
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DroneCIBuildServiceTest {

    private static final String COMPANY = "dronecitest";

    private static CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private static DroneCIBuildService droneCIBuildService;

    private static final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();
    private static UUID instanceId;

    private static CiCdJobRunStageStepsDatabaseService ciCdJobRunStageStepsDatabaseService;

    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;

    private static final String DRONECI_BUILDS_JSON = "droneci/droneci_builds.json";
    private final String DRONECI_REPO_JSON = "droneci/droneci_repo_updated.json";

    @BeforeClass
    public static void setup() throws IOException, SQLException {
        if (dataSource != null) {
            return;
        }
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);

        CiCdInstancesDatabaseService ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        CiCdJobRunStageDatabaseService ciCdJobRunStageDatabaseService = new CiCdJobRunStageDatabaseService(dataSource, OBJECT_MAPPER);
        ciCdJobRunStageStepsDatabaseService = new CiCdJobRunStageStepsDatabaseService(dataSource);
        ciCdJobRunsDatabaseService = new CiCdJobRunsDatabaseService(OBJECT_MAPPER, dataSource);
        droneCIBuildService = new DroneCIBuildService(ciCdJobsDatabaseService,
                ciCdJobRunsDatabaseService, ciCdJobRunStageDatabaseService,
                ciCdJobRunStageStepsDatabaseService);

        IntegrationService integrationService = new IntegrationService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService.ensureTableExistence(COMPANY);
        integrationService.insert(COMPANY, Integration.builder()
                .id("1")
                .application("droneci")
                .name("droneci-integ")
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
                .name("droneci-integration")
                .type(CICD_TYPE.droneci.toString())
                .build());
        insertDataIntoTable(DRONECI_BUILDS_JSON);
    }

    private static void insertDataIntoTable(String resourcePath) throws IOException {
        String projectsInput = ResourceUtils.getResourceAsString(resourcePath);
        PaginatedResponse<DroneCIEnrichRepoData> repos = OBJECT_MAPPER.readValue(projectsInput, OBJECT_MAPPER.getTypeFactory()
                .constructParametricType(PaginatedResponse.class, DroneCIEnrichRepoData.class));
        DroneCIEnrichRepoData droneCIEnrichRepoData = repos.getResponse().getRecords().get(0);
        String id = droneCIBuildService.insert(COMPANY, instanceId, droneCIEnrichRepoData);
        if (id == null) {
            throw new RuntimeException("Failed to insert build with id" + droneCIEnrichRepoData.getBuilds().get(0).getId());
        }
    }

    @Test
    public void testDroneCIDuplicateInsertion() throws IOException, SQLException {
        insertDataIntoTable(DRONECI_BUILDS_JSON);
        var listRepoResponse = ciCdJobsDatabaseService.list(COMPANY, 0, 10);
        var listBuildResponse = ciCdJobRunsDatabaseService.list(COMPANY, 0, 10);
        var listStepResponse = ciCdJobRunStageStepsDatabaseService.list(COMPANY, 0, 10);

        Assert.assertEquals(1, listRepoResponse.getRecords().size());
        Assert.assertEquals(1, listBuildResponse.getRecords().size());
        Assert.assertEquals(4, listStepResponse.getRecords().size());
    }

    @Test
    public void testDroneCIRepoDetails() throws SQLException {
        var listResponse = ciCdJobsDatabaseService.list(COMPANY, 0, 10);
        Assert.assertEquals(1, listResponse.getRecords().size());
        Assert.assertEquals("test", listResponse.getRecords().get(0).getProjectName());
        Assert.assertEquals("test", listResponse.getRecords().get(0).getJobName());
        Assert.assertEquals("testuser/test", listResponse.getRecords().get(0).getJobFullName());
        Assert.assertEquals("test", listResponse.getRecords().get(0).getJobNormalizedFullName());
        Assert.assertEquals("main", listResponse.getRecords().get(0).getBranchName());
        Assert.assertEquals("https://github.com/testuser/test", listResponse.getRecords().get(0).getScmUrl());
        Assert.assertEquals("testuser", listResponse.getRecords().get(0).getScmUserId());
    }

    @Test
    public void testDroneCIBuildDetails() throws SQLException {
        var listResponse = ciCdJobRunsDatabaseService.list(COMPANY, 0, 10);
        Assert.assertEquals(1, listResponse.getRecords().size());
        Assert.assertEquals(Long.valueOf(20), listResponse.getRecords().get(0).getJobRunNumber());
        Assert.assertEquals("success", listResponse.getRecords().get(0).getStatus());
        Assert.assertEquals(Instant.ofEpochSecond(1653303407), listResponse.getRecords().get(0).getStartTime());
        Assert.assertEquals(Instant.ofEpochSecond(1653303420), listResponse.getRecords().get(0).getEndTime());
        Assert.assertEquals((Integer.valueOf(1653303420 - 1653303407)), listResponse.getRecords().get(0).getDuration());
        Assert.assertEquals("testuser", listResponse.getRecords().get(0).getCicdUserId());
        Assert.assertEquals(List.of("2a999d9c8e2128ed6140c0b506cf86dd6cdb4523"), listResponse.getRecords().get(0).getScmCommitIds());
    }

    @Test
    public void testDroneCIStepDetails() throws SQLException {
        var listResponse = ciCdJobRunStageStepsDatabaseService.list(COMPANY, 0, 10);
        Assert.assertEquals(4, listResponse.getRecords().size());
        Assert.assertEquals(String.valueOf(4), listResponse.getRecords().get(0).getStepId());
        Assert.assertEquals("update", listResponse.getRecords().get(0).getDisplayName());
        Assert.assertEquals("success", listResponse.getRecords().get(0).getResult());
        Assert.assertEquals("success", listResponse.getRecords().get(0).getState());
    }

    @Test
    public void testDroneCIUpdatedRepos() throws SQLException, IOException {
        insertDataIntoTable(DRONECI_REPO_JSON);
        var listResponse = ciCdJobsDatabaseService.list(COMPANY, 0, 10);
        Assert.assertEquals(1, listResponse.getRecords().size());
        Assert.assertEquals("https://github.com/testuser1/test1", listResponse.getRecords().get(0).getScmUrl());
        Assert.assertEquals("testuser1", listResponse.getRecords().get(0).getScmUserId());
        Assert.assertEquals("test1", listResponse.getRecords().get(0).getJobNormalizedFullName());
        Assert.assertEquals("test1", listResponse.getRecords().get(0).getProjectName());
        Assert.assertNotEquals(Instant.ofEpochSecond(1653287236), listResponse.getRecords().get(0).getUpdatedAt());
    }

    @Test
    public void testDroneCIUpdatedSteps() throws SQLException, IOException {
        insertDataIntoTable(DRONECI_REPO_JSON);
        var listResponse = ciCdJobRunStageStepsDatabaseService.list(COMPANY, 0, 10);
        Assert.assertEquals(4, listResponse.getRecords().size());
        Assert.assertEquals("update 1", listResponse.getRecords().get(0).getDisplayName());
        Assert.assertEquals(Instant.ofEpochSecond(1653303411), listResponse.getRecords().get(0).getStartTime());
        Assert.assertEquals("success", listResponse.getRecords().get(0).getState());
        Assert.assertEquals("success", listResponse.getRecords().get(0).getResult());
        Assert.assertEquals((1653303412 - 1653303411), listResponse.getRecords().get(0).getDuration().intValue());
    }

}
