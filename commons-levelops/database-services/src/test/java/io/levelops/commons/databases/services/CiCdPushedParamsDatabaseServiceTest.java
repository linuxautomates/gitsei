package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.cicd.DbCiCdPushedArtifact;
import io.levelops.commons.databases.models.database.cicd.DbCiCdPushedJobRunParam;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class CiCdPushedParamsDatabaseServiceTest {
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static ObjectMapper objectmapper = DefaultObjectMapper.get();
    private static DataSource dataSource;
    private static CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private static CiCdPushedParamsDatabaseService ciCdPushedParamsDatabaseService;
    private static String company = "test";
    private static ObjectMapper objectMapper;
    private static IntegrationService integrationService;
    private static Integration integration;
    private static NamedParameterJdbcTemplate template;
    private static CICDInstance cicdInstance;
    private static List<DbCiCdPushedJobRunParam> dbCiCdPushedJobRunParams;

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
        ciCdPushedParamsDatabaseService = new CiCdPushedParamsDatabaseService(dataSource, objectMapper);
        ciCdPushedParamsDatabaseService.ensureTableExistence(company);

        String dbCiCdPushedParamsString = ResourceUtils.getResourceAsString("json/databases/db_cicd_pushed_params.json");
        dbCiCdPushedJobRunParams = objectMapper.readValue(dbCiCdPushedParamsString, objectMapper.getTypeFactory()
                .constructParametricType(List.class, DbCiCdPushedJobRunParam.class));
        for (var dbCiCdPushedJobRunParam: dbCiCdPushedJobRunParams){
            ciCdPushedParamsDatabaseService.insertPushedJobRunParams(company, dbCiCdPushedJobRunParam);
        }
    }

    @Test
    public void testInsertPushedJobRunParams() {
        List<DbCiCdPushedJobRunParam> dbCiCdPushedJobRunParamList = ciCdPushedParamsDatabaseService.filterPushedParams(company, List.of("action_workflow"), null, List.of("test-repo"), List.of(1));
        DbCiCdPushedJobRunParam dbCiCdPushedParam = dbCiCdPushedJobRunParamList.get(0);
        DbCiCdPushedJobRunParam expectedPushedParam = dbCiCdPushedJobRunParams.get(0);
        Assert.assertEquals(2, dbCiCdPushedJobRunParamList.size());
        Assert.assertEquals(expectedPushedParam.getJobName(), dbCiCdPushedParam.getJobName());
        Assert.assertEquals(expectedPushedParam.getJobRunNumber(), dbCiCdPushedParam.getJobRunNumber());
        Assert.assertEquals(expectedPushedParam.getRepository(), dbCiCdPushedParam.getRepository());
        Assert.assertEquals(expectedPushedParam.getJobRunParams().get(0).getName(), dbCiCdPushedParam.getJobRunParams().get(0).getName());
        Assert.assertEquals(expectedPushedParam.getJobRunParams().get(0).getType(), dbCiCdPushedParam.getJobRunParams().get(0).getType());
        Assert.assertEquals(expectedPushedParam.getJobRunParams().get(0).getValue(), dbCiCdPushedParam.getJobRunParams().get(0).getValue());
    }

    @Test
    public void testInsertCiCdJobRunParamsFromPushedParams() throws SQLException {
        DbCiCdPushedJobRunParam dbCiCdPushedJobRunParam = dbCiCdPushedJobRunParams.get(0);
        CICDJob cicdJob = CICDJob.builder()
                .projectName(dbCiCdPushedJobRunParam.getRepository())
                .jobName(dbCiCdPushedJobRunParam.getJobName())
                .jobFullName(dbCiCdPushedJobRunParam.getRepository()+ "/" + dbCiCdPushedJobRunParam.getJobName())
                .jobNormalizedFullName(dbCiCdPushedJobRunParam.getRepository()+ "/" + dbCiCdPushedJobRunParam.getJobName())
                .cicdInstanceId(cicdInstance.getId())
                .build();
        String cicdJobId = ciCdJobsDatabaseService.insert(company, cicdJob);

        CICDJobRun cicdJobRun = CICDJobRun.builder()
                .cicdJobId(UUID.fromString(cicdJobId))
                .jobRunNumber(dbCiCdPushedJobRunParam.getJobRunNumber())
                .build();
        String cicdJobRunId = ciCdJobRunsDatabaseService.insert(company, cicdJobRun);
        List<String> insertedParamIds = ciCdPushedParamsDatabaseService.insertCiCdJobRunParamsFromPushedParams(company, integration.getId(),
                UUID.fromString(cicdJobRunId), dbCiCdPushedJobRunParam.getJobName(), dbCiCdPushedJobRunParam.getJobRunNumber(), dbCiCdPushedJobRunParam.getRepository());
        Map<UUID, List<CICDJobRun.JobRunParam>> jobRunParams = ciCdJobRunsDatabaseService.getJobRunParams(company, List.of(UUID.fromString(cicdJobRunId)));
        List<CICDJobRun.JobRunParam> params = jobRunParams.get(UUID.fromString(cicdJobRunId));
        DbCiCdPushedJobRunParam dbCiCdPushedJobRunParam1 = dbCiCdPushedJobRunParams.stream().filter(jobRunParam -> jobRunParam.getJobRunNumber() == 1).collect(Collectors.toList()).get(0);
        CICDJobRun.JobRunParam param = params.get(0);
        Assert.assertEquals(dbCiCdPushedJobRunParam1.getJobRunParams().get(0).getName(), param.getName());
        Assert.assertEquals(dbCiCdPushedJobRunParam1.getJobRunParams().get(0).getType(), param.getType());
        Assert.assertEquals(dbCiCdPushedJobRunParam1.getJobRunParams().get(0).getValue(), param.getValue());
        Assert.assertEquals(1, insertedParamIds.size());
    }
}
