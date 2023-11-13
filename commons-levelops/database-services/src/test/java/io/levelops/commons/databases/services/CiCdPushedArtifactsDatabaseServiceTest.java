package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.cicd.CiCdJobRunArtifact;
import io.levelops.commons.databases.models.database.cicd.DbCiCdPushedArtifact;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CiCdPushedArtifactsDatabaseServiceTest {
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static ObjectMapper objectmapper = DefaultObjectMapper.get();
    private static DataSource dataSource;
    private static CiCdJobRunArtifactsDatabaseService ciCdJobRunArtifactsDatabaseService;
    private static CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private static CiCdPushedArtifactsDatabaseService ciCdPushedArtifactsDatabaseService;
    private static String company = "test";
    private static ObjectMapper objectMapper;
    private static IntegrationService integrationService;
    private static Integration integration;
    private static NamedParameterJdbcTemplate template;
    private static CICDInstance cicdInstance;
    private static List<DbCiCdPushedArtifact> dbCiCdPushedArtifacts;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        template = new NamedParameterJdbcTemplate(dataSource);
        List.of("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").forEach(template.getJdbcTemplate()::execute);
        objectMapper = DefaultObjectMapper.get();
        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);

        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        integration = Integration.builder()
                .id("1")
                .name("name")
                .url("http")
                .status("good")
                .application("CiCd-Pushed-Artifacts")
                .description("desc")
                .satellite(true)
                .build();
        integrationService.insert(company, integration);
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, 0);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        ciCdJobRunsDatabaseService = new CiCdJobRunsDatabaseService(objectmapper, dataSource);
        ciCdJobRunsDatabaseService.ensureTableExistence(company);
        ciCdJobRunArtifactsDatabaseService = new CiCdJobRunArtifactsDatabaseService(dataSource, objectmapper);
        ciCdJobRunArtifactsDatabaseService.ensureTableExistence(company);
        ciCdPushedArtifactsDatabaseService = new CiCdPushedArtifactsDatabaseService(dataSource, objectmapper,
                ciCdJobRunArtifactsDatabaseService);
        ciCdPushedArtifactsDatabaseService.ensureTableExistence(company);

        String dbCiCdPushedArtifactsString = ResourceUtils.getResourceAsString("json/databases/db_cicd_pushed_artifacts.json");
        dbCiCdPushedArtifacts = objectMapper.readValue(dbCiCdPushedArtifactsString, objectMapper.getTypeFactory()
                .constructParametricType(List.class, DbCiCdPushedArtifact.class));
        for(var dbCiCdPushedArtifact : dbCiCdPushedArtifacts){
            ciCdPushedArtifactsDatabaseService.insertPushedArtifacts(company, dbCiCdPushedArtifact);
        }
    }

    @Test
    public void testInsertPushedArtifacts() {
        List<DbCiCdPushedArtifact> dbCiCdPushedArtifactList = ciCdPushedArtifactsDatabaseService.filterPushedArtifacts(company, List.of("action_workflow"), null, List.of("test-repo"), List.of(1));
        DbCiCdPushedArtifact dbCiCdPushedArtifact = dbCiCdPushedArtifactList.get(0);
        DbCiCdPushedArtifact expectedPushedArtifact = dbCiCdPushedArtifacts.get(0);
        Assert.assertEquals(2, dbCiCdPushedArtifactList.size());
        Assert.assertEquals(expectedPushedArtifact.getJobName(), dbCiCdPushedArtifact.getJobName());
        Assert.assertEquals(expectedPushedArtifact.getJobRunNumber(), dbCiCdPushedArtifact.getJobRunNumber());
        Assert.assertEquals(expectedPushedArtifact.getRepository(), dbCiCdPushedArtifact.getRepository());
        Assert.assertEquals(expectedPushedArtifact.getArtifacts().get(0).getName(), dbCiCdPushedArtifact.getArtifacts().get(0).getName());
        Assert.assertEquals(expectedPushedArtifact.getArtifacts().get(0).getLocation(), dbCiCdPushedArtifact.getArtifacts().get(0).getLocation());
        Assert.assertEquals(expectedPushedArtifact.getArtifacts().get(0).getTag(), dbCiCdPushedArtifact.getArtifacts().get(0).getTag());
        Assert.assertEquals(expectedPushedArtifact.getArtifacts().get(0).getType(), dbCiCdPushedArtifact.getArtifacts().get(0).getType());
        Assert.assertEquals(expectedPushedArtifact.getArtifacts().get(0).getDigest(), dbCiCdPushedArtifact.getArtifacts().get(0).getDigest());
        Assert.assertEquals(expectedPushedArtifact.getArtifacts().get(0).getArtifactCreatedAt(), dbCiCdPushedArtifact.getArtifacts().get(0).getArtifactCreatedAt());
    }

    @Test
    public void testInsertCiCdJobRunArtifactsFromPushedArtifacts() throws SQLException {
        DbCiCdPushedArtifact dbCiCdPushedArtifact = dbCiCdPushedArtifacts.get(0);
        CICDJob cicdJob = CICDJob.builder()
                .projectName(dbCiCdPushedArtifact.getRepository())
                .jobName(dbCiCdPushedArtifact.getJobName())
                .jobFullName(dbCiCdPushedArtifact.getRepository()+ "/" + dbCiCdPushedArtifact.getJobName())
                .jobNormalizedFullName(dbCiCdPushedArtifact.getRepository()+ "/" + dbCiCdPushedArtifact.getJobName())
                .cicdInstanceId(cicdInstance.getId())
                .build();
        String cicdJobId = ciCdJobsDatabaseService.insert(company, cicdJob);

        CICDJobRun cicdJobRun = CICDJobRun.builder()
                .cicdJobId(UUID.fromString(cicdJobId))
                .jobRunNumber(dbCiCdPushedArtifact.getJobRunNumber())
                .build();
        String cicdJobRunId = ciCdJobRunsDatabaseService.insert(company, cicdJobRun);
        List<String> pushedArtifactIds = new ArrayList<>();
        List<String> insertedArtifactIds = ciCdPushedArtifactsDatabaseService.insertCiCdJobRunArtifactsFromPushedArtifacts(company, integration.getId(),
                UUID.fromString(cicdJobRunId), dbCiCdPushedArtifact.getJobName(), dbCiCdPushedArtifact.getJobRunNumber(), dbCiCdPushedArtifact.getRepository(), pushedArtifactIds);
        List<CiCdJobRunArtifact> artifacts = ciCdJobRunArtifactsDatabaseService.filter(company, CiCdJobRunArtifactsDatabaseService.CiCdJobRunArtifactFilter.builder().build(), 0, 10).getRecords();
        DbCiCdPushedArtifact dbCiCdPushedArtifact1 = dbCiCdPushedArtifacts.stream().filter(artifact -> artifact.getArtifacts().get(0).getType() == null).collect(Collectors.toList()).get(0);
        CiCdJobRunArtifact ciCdJobRunArtifact1 = artifacts.get(0);
        Assert.assertEquals(dbCiCdPushedArtifact1.getArtifacts().get(0).getName(), ciCdJobRunArtifact1.getName());
        Assert.assertEquals(dbCiCdPushedArtifact1.getArtifacts().get(0).getLocation(), ciCdJobRunArtifact1.getLocation());
        Assert.assertEquals(dbCiCdPushedArtifact1.getArtifacts().get(0).getTag(), ciCdJobRunArtifact1.getQualifier());
        Assert.assertEquals(dbCiCdPushedArtifact1.getArtifacts().get(0).getDigest(), ciCdJobRunArtifact1.getHash());
        Assert.assertEquals("unknown", ciCdJobRunArtifact1.getType());
        Assert.assertEquals(1, pushedArtifactIds.size());
        Assert.assertEquals(2, insertedArtifactIds.size());
    }
}
