package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.converters.CICDInstanceConverters;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CiCdInstanceConfig;
import io.levelops.commons.databases.models.database.CiCdInstanceDetails;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Integration.Authentication;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.CICDInstanceIntegAssignment;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.collections4.CollectionUtils;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.CiCdInstanceUtils.generateCiCdInstanceGuids;

@Log4j2
public class CiCdInstancesDatabaseServiceTest {
    private static final Boolean UPSERT = Boolean.TRUE;
    private static final Boolean INSERT_ONLY = Boolean.FALSE;

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static DataSource dataSource;
    private static CiCdInstancesDatabaseService dbService;
    private static IntegrationService integrationService;
    private static TagItemDBService tagItemService;
    private static TagsService tagsService;

    private static String company = "test";
    private static String integrationId;
    private static ObjectMapper objectMapper;
    private static Integration integration;
    private static String integrationId1;
    private static String integrationId2;

    
    @BeforeClass
    public static void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        dbService = new CiCdInstancesDatabaseService(dataSource);
        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        dbService.ensureTableExistence(company);

        tagsService = new TagsService(dataSource);
        tagsService.ensureTableExistence(company);

        tagItemService = new TagItemDBService(dataSource);
        tagItemService.ensureTableExistence(company);
        Integration integration = Integration.builder()
                .name("integration")
                .url("http")
                .status("good")
                .application("jenkins")
                .description("desc")
                .satellite(true)
                .build();
        integrationId = integrationService.insert(company, integration);
        objectMapper = DefaultObjectMapper.get();


        integration = Integration.builder()
            .name("name for jenkins1")
            .satellite(false)
            .application("jenkins")
            .authentication(Authentication.API_KEY)
            .status("active")
            .build();
        integrationId1 = integrationService.insert(company, integration);
        integrationId2 = integrationService.insert(company, integration.toBuilder().name("name for jenkins2").build());
    }

    private void verifyRecord(CICDInstance a, CICDInstance e){
        Assert.assertEquals(a.getId(), e.getId());
        Assert.assertEquals(a.getName(), e.getName());
        Assert.assertEquals(a.getUrl(), e.getUrl());
        Assert.assertNotNull(a.getCreatedAt());
        Assert.assertNotNull(a.getUpdatedAt());
    }
    private void verifyRecords(List<CICDInstance> a, List<CICDInstance> e){
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if(CollectionUtils.isEmpty(a)){
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<UUID, CICDInstance> actualMap = a.stream().collect(Collectors.toMap(CICDInstance::getId, x -> x));
        Map<UUID, CICDInstance> expectedMap = e.stream().collect(Collectors.toMap(CICDInstance::getId, x -> x));

        for(UUID key : actualMap.keySet()){
            verifyRecord(actualMap.get(key), expectedMap.get(key));
        }
    }

    private void testGet(CICDInstance expected) throws SQLException {
        CICDInstance actual = dbService.get(company, expected.getId().toString()).get();
        verifyRecord(actual,expected);
    }

    private CICDInstance testInsert(int i, UUID instanceGuid, boolean upsert) throws SQLException {
        CICDInstance.CICDInstanceBuilder bldr = CICDInstance.builder()
                .id(instanceGuid)
                .lastHeartbeatAt(Instant.now())
                .type("jenkins")
                .integrationId(integrationId);
        if(upsert) {
            bldr.name("name-" + i + "-" + i);
            bldr.url("url-" + i + "-" + i);
        } else {
            bldr.name("name-" + i);
            bldr.url("url-" + i);
        }

        CICDInstance cicdInstance = bldr.build();
        String id = dbService.insert(company, cicdInstance);
        Assert.assertEquals(id, cicdInstance.getId().toString());
        Assert.assertNotNull(id);
        CICDInstance expected = cicdInstance.toBuilder().id(UUID.fromString(id)).build();
        testGet(expected);
        return expected;
    }

    private List<CICDInstance> testInserts(List<UUID> jenkinsInstanceGuids) throws SQLException {
        List<CICDInstance> cicdInstances = new ArrayList<>();
        for(int i=0; i< jenkinsInstanceGuids.size(); i++){
            CICDInstance cicdInstance = testInsert(i, jenkinsInstanceGuids.get(i), INSERT_ONLY);
            cicdInstances.add(cicdInstance);
        }
        return cicdInstances;
    }

    @Test
    public void testUpsertJenkins() throws SQLException {
        var cicdInstance = CICDInstance.builder()
            .integrationId(integrationId1)
            .name("upsert test")
            .url("url")
            .lastHeartbeatAt(Instant.parse("2021-05-05T13:00:00-08:00"))
            .updatedAt(Instant.parse("2021-05-05T13:00:00-08:00"))
            .configUpdatedAt(Instant.parse("2021-05-05T13:00:00-08:00"))
            .type("jenkins")
            .id(UUID.randomUUID())
            .build();

        var id = dbService.insert(company, cicdInstance);

        var result = dbService.get(company, id).get();
        Assertions.assertThat(result.getIntegrationId()).isEqualTo(integrationId1);
        
        dbService.upsert(company, cicdInstance.toBuilder().integrationId(integrationId2).build());
        result = dbService.get(company, id).get();
        Assertions.assertThat(result.getIntegrationId()).isEqualTo(integrationId1);

        dbService.upsert(company, cicdInstance.toBuilder().integrationId("").build());
        result = dbService.get(company, id).get();
        Assertions.assertThat(result.getIntegrationId()).isEqualTo(integrationId1);

        dbService.upsert(company, cicdInstance.toBuilder().integrationId(null).build());
        result = dbService.get(company, id).get();
        Assertions.assertThat(result.getIntegrationId()).isEqualTo(integrationId1);
    }

    @Test
    public void testUpdate() throws SQLException, JsonProcessingException {
        var cicdInstance = CICDInstance.builder()
            .integrationId(integrationId1)
            .name("upsert test")
            .url("url")
            .lastHeartbeatAt(Instant.parse("2021-05-05T13:00:00-08:00"))
            .updatedAt(Instant.parse("2021-05-05T13:00:00-08:00"))
            .configUpdatedAt(Instant.parse("2021-05-05T13:00:00-08:00"))
            .type("jenkins")
            .id(UUID.randomUUID())
            .build();

        var id = dbService.insert(company, cicdInstance);

        var result = dbService.get(company, id).get();
        Assertions.assertThat(result.getIntegrationId()).isEqualTo(integrationId1);
        
        var update = CICDInstanceIntegAssignment.builder()
            .integrationId(integrationId2)
            .addIds(Set.of(id))
            .build();
        dbService.assignIntegrationId(company, update);
        result = dbService.get(company, id).get();
        Assertions.assertThat(result.getIntegrationId()).isEqualTo(integrationId2);

        dbService.assignIntegrationId(company, update.toBuilder().integrationId("").build());
        result = dbService.get(company, id).get();
        Assertions.assertThat(result.getIntegrationId()).isEqualTo(integrationId2);

        dbService.assignIntegrationId(company, update.toBuilder().integrationId(null).build());
        result = dbService.get(company, id).get();
        Assertions.assertThat(result.getIntegrationId()).isEqualTo(integrationId2);
    }

    private List<CICDInstance> testUpserts(List<CICDInstance> expected) throws SQLException {
        List<CICDInstance> updatedInstances = new ArrayList<>();
        for(int i=0; i< expected.size(); i++){
            CICDInstance currentCiCdInstance = expected.get(i);
            CICDInstance cicdInstance = testInsert(i, currentCiCdInstance.getId(), UPSERT);
            updatedInstances.add(cicdInstance);
        }

        Assert.assertEquals(expected.stream().map(CICDInstance::getId).collect(Collectors.toSet()),updatedInstances.stream().map(CICDInstance::getId).collect(Collectors.toSet()));
        testList(updatedInstances);
        testListByFilter(updatedInstances);
        return updatedInstances;
    }


    private void testList(List<CICDInstance> expected) throws SQLException {
        DbListResponse<CICDInstance> result = dbService.list(company, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), expected);
    }

    private void testListByFilterIds(List<CICDInstance> allExpected) throws SQLException {
        List<UUID> allIds = allExpected.stream().map(CICDInstance::getId).collect(Collectors.toList());
        DbListResponse<CICDInstance> result = dbService.listByFilter(company, 0, 100, allIds);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), allExpected);
    }

    private void testListByFilterPageSize(List<CICDInstance> allExpected) throws SQLException {
        List<UUID> allIds = allExpected.stream().map(CICDInstance::getId).collect(Collectors.toList());
        DbListResponse<CICDInstance> result = dbService.listByFilter(company, 0, 2, allIds);
        List<CICDInstance> expected = getLatestCiCdInstance(allExpected, 2);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getTotalCount().intValue(), allExpected.size());
        Assert.assertEquals(result.getCount().intValue(), expected.size());
        verifyRecords(result.getRecords(),expected);
    }

    private List<CICDInstance> getLatestCiCdInstance(List<CICDInstance> expected, int n){
        if(CollectionUtils.isEmpty(expected)){
            return Collections.emptyList();
        }
        List<CICDInstance> reverse = new ArrayList<>();
        for(int i= expected.size()-1; i>=0; i--){
            reverse.add(expected.get(i));
        }
        int j=1;
        List<CICDInstance> result = new ArrayList<>();
        for(int i=0; i<reverse.size(); i++){
            if (j > n) {
                return result;
            } else {
                result.add(reverse.get(i));
                j++;
            }
        }
        return result;
    }
    private void testListByFilter(List<CICDInstance> expected) throws SQLException {
        testListByFilterIds(expected);
        testListByFilterPageSize(expected);
    }

    private void testDelete(List<CICDInstance> expected) throws SQLException {
        int size = expected.size();
        for(int i=0; i < size; i++){
            CICDInstance current = expected.get(0);
            Boolean success = dbService.delete(company, current.getId().toString());
            Assert.assertTrue(success);
            expected.remove(0);
            testList(expected);
        }
        testList(expected);
        testListByFilter(expected);
        UUID jenkinsInstanceIdDoesNotExist = UUID.randomUUID();
        Boolean success = dbService.delete(company, jenkinsInstanceIdDoesNotExist.toString());
        Assert.assertFalse(success);
    }

    private void testUpdate(List<CICDInstance> expected) throws SQLException {
        for (CICDInstance current : expected) {
            UUID currentInstanceId = current.getId();
            Instant lastHbTime  = Instant.now();
            CiCdInstanceDetails ciCdInstanceDetails = CiCdInstanceDetails.builder()
                    .jenkinsVersion("2.1")
                    .build();
            CICDInstance updated = current.toBuilder()
                    .id(currentInstanceId)
                    .lastHeartbeatAt(lastHbTime)
                    .details(ciCdInstanceDetails)
                    .build();
            Boolean success = dbService.update(company, updated);
            CICDInstance cicdInstance = dbService.get(company, updated.getId().toString()).get();
            Assert.assertEquals(lastHbTime, cicdInstance.getLastHeartbeatAt());
            Assert.assertEquals(updated.getDetails(), cicdInstance.getDetails());
            Assert.assertTrue(success);
        }
    }

    @Test
    public void test() throws SQLException {
        int n = 5;
        List<UUID> jenkinsInstanceGuids = generateCiCdInstanceGuids(n);
        List<CICDInstance> expected = testInserts(jenkinsInstanceGuids);
        testList(expected);
        testListByFilter(expected);
        expected = testUpserts(expected);
        testUpdate(expected);
        testDelete(expected);
    }

    @Test
    public void testConfig() throws SQLException, JsonProcessingException {
        CICDInstance instance = CICDInstance.builder()
                .id(UUID.randomUUID())
                .name("name")
                .url("url")
                .type("jenkins")
                .integrationId(integrationId)
                .lastHeartbeatAt(Instant.now())
                .config(CiCdInstanceConfig.builder()
                        .heartbeatDuration(2)
                        .bullseyeReportPaths("path")
                        .build())
                .build();
        String instanceId = dbService.insert(company, instance);
        CiCdInstanceConfig config = dbService.getConfig(company, instanceId);
        Assert.assertNotNull(config);
        CiCdInstanceConfig updateConfig = CiCdInstanceConfig.builder()
                .heartbeatDuration(1)
                .bullseyeReportPaths("new_path")
                .build();
        Boolean configUpdated = dbService.updateConfig(company, CICDInstance.builder().config(updateConfig).build(), instanceId);
        Assert.assertTrue(configUpdated);
        CiCdInstanceConfig newConfig = dbService.getConfig(company, instanceId);
        Assert.assertEquals(updateConfig, newConfig);
    }

    @Test
    public void testParseConfig() throws JsonProcessingException {
        CiCdInstanceConfig expectedInstanceConfig = CiCdInstanceConfig.builder()
                .heartbeatDuration(72)
                .bullseyeReportPaths("this/is/a/sample/path")
                .build();
        CiCdInstanceConfig actualInstanceConfig = CICDInstanceConverters.parseConfig(objectMapper, objectMapper.writeValueAsString(expectedInstanceConfig));
        Assert.assertNotNull(actualInstanceConfig);
        Assert.assertEquals(expectedInstanceConfig, actualInstanceConfig);
        actualInstanceConfig = CICDInstanceConverters.parseConfig(objectMapper, null);
        Assert.assertNotNull(actualInstanceConfig);
        Assert.assertEquals(CiCdInstanceConfig.builder().build(), actualInstanceConfig);
        actualInstanceConfig = CICDInstanceConverters.parseConfig(objectMapper, "");
        Assert.assertNotNull(actualInstanceConfig);
        Assert.assertEquals(CiCdInstanceConfig.builder().build(), actualInstanceConfig);
    }

    @Test
    public void testParseDetails() throws JsonProcessingException {
        CiCdInstanceDetails expectedInstanceDetails = CiCdInstanceDetails.builder()
                .jenkinsVersion("2.1")
                .pluginVersion("3.1")
                .build();
        CiCdInstanceDetails actualCiCdInstanceDetails = dbService.parseInstDetails(company, UUID.randomUUID(),
                objectMapper, objectMapper.writeValueAsString(expectedInstanceDetails));
        Assert.assertNotNull(actualCiCdInstanceDetails);
        Assert.assertEquals(expectedInstanceDetails, actualCiCdInstanceDetails);
        actualCiCdInstanceDetails = dbService.parseInstDetails(company, UUID.randomUUID(), objectMapper, null);
        Assert.assertNotNull(actualCiCdInstanceDetails);
        Assert.assertEquals(CiCdInstanceDetails.builder().build(), actualCiCdInstanceDetails);
        actualCiCdInstanceDetails = dbService.parseInstDetails(company, UUID.randomUUID(), objectMapper, "");
        Assert.assertNotNull(actualCiCdInstanceDetails);
        Assert.assertEquals(CiCdInstanceDetails.builder().build(), actualCiCdInstanceDetails);
    }

    @Test
    public void testUpsert() throws SQLException {
        UUID uuid = UUID.randomUUID();
        String INTEGRATION_ID = "1";
        CICDInstance cicdInstance = CICDInstance.builder()
                .id(uuid)
                .name("azure-integration"+INTEGRATION_ID)
                .type("azure")
                .integrationId(INTEGRATION_ID)
                .build();
        dbService.upsert(company,cicdInstance);
        Optional<CICDInstance> actualCicdInstance = dbService.get(company, uuid.toString());
        actualCicdInstance.ifPresent(instance ->{
            Assert.assertNotNull(instance);
            Assert.assertEquals(cicdInstance.getName(),instance.getName());
            Assert.assertEquals(cicdInstance.getType(),instance.getType());
            Assert.assertEquals(cicdInstance.getId(),instance.getId());
        } );
    }

    // @Test(expected = DataIntegrityViolationException.class)
    public void testUpsertFail() throws SQLException {
        UUID uuid = UUID.randomUUID();
        String INTEGRATION_ID = "1";
        CICDInstance cicdInstance = CICDInstance.builder()
                .id(uuid)
                .name("azure-integration"+INTEGRATION_ID)
                .type("azure")
                .build();
        dbService.upsert(company,cicdInstance);
    }
}