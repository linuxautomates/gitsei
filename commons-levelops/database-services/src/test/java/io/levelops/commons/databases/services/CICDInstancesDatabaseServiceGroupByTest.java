package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CiCdInstanceConfig;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.filters.CICDInstanceFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static io.levelops.commons.databases.services.CiCdInstanceUtils.generateCiCdInstanceGuids;

@Log4j2
public class CICDInstancesDatabaseServiceGroupByTest {
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static CiCdInstancesDatabaseService dbService;
    private final static String company = "test";
    private static List<String> integrationIds;
    private static List<UUID> uuidsList;

    @BeforeClass
    public static void setup() throws SQLException {
        if (dataSource != null) return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        dbService = new CiCdInstancesDatabaseService(dataSource);
        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        new TagsService(dataSource).ensureTableExistence(company);
        new TagItemDBService(dataSource).ensureTableExistence(company);
        IntegrationService integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        dbService.ensureTableExistence(company);

        integrationIds = new ArrayList<>();
        Integration integration = Integration.builder()
                .name("test-integration-1")
                .url("http")
                .status("good")
                .application("jira")
                .description("desc")
                .satellite(true)
                .build();
        integrationIds.add(integrationService.insert(company, integration));
        Integration integration2 = Integration.builder()
                .name("test-integration-2")
                .url("http")
                .status("good")
                .application("zendesk")
                .description("description")
                .satellite(true)
                .build();
        integrationIds.add(integrationService.insert(company, integration2));

        uuidsList = generateCiCdInstanceGuids(3);
        CICDInstance cicdInstance = CICDInstance.builder()
                .id(uuidsList.get(0))
                .name("name-0")
                .url("url-0")
                .integrationId(integrationIds.get(0))
//                .type("jenkins")
                .lastHeartbeatAt(Instant.now())
                .config(CiCdInstanceConfig.builder()
                        .heartbeatDuration(1)
                        .bullseyeReportPaths("path")
                        .build())
                .build();
        dbService.insert(company, cicdInstance);
        cicdInstance = CICDInstance.builder()
                .id(uuidsList.get(1))
                .name("name-1")
                .url("url-1")
                .integrationId(integrationIds.get(1))
//                .type("jenkins")
                .lastHeartbeatAt(Instant.now())
                .config(CiCdInstanceConfig.builder()
                        .heartbeatDuration(12)
                        .bullseyeReportPaths("path")
                        .build())
                .build();
        dbService.insert(company, cicdInstance);
        cicdInstance = CICDInstance.builder()
                .id(uuidsList.get(2))
                .name("name-2")
                .url("url-1")
                .integrationId(integrationIds.get(1))
//                .type("azure")
                .lastHeartbeatAt(Instant.now())
                .config(CiCdInstanceConfig.builder()
                        .heartbeatDuration(21)
                        .bullseyeReportPaths("path")
                        .build())
                .build();
        dbService.insert(company, cicdInstance);
    }

    @Test
    public void testGroupByAndCalculate() {
        //url without integration_ids
        CICDInstanceFilter filter = CICDInstanceFilter.builder()
                .across(CICDInstanceFilter.DISTINCT.fromString("url"))
                .build();
        DbListResponse<DbAggregationResult> response = dbService.groupByAndCalculate(company, filter);
        Assert.assertNotNull(response);
        Assert.assertEquals(2, response.getRecords().size());
        Set<DbAggregationResult> expectedResults = Set.of(
                DbAggregationResult.builder()
                        .key("url-1")
                        .count(2L)
                        .build(),
                DbAggregationResult.builder()
                        .key("url-0")
                        .count(1L)
                        .build()
        );
        Assert.assertEquals(expectedResults, new HashSet<>(response.getRecords()));

        //url with all integration_ids
        filter = CICDInstanceFilter.builder()
                .across(CICDInstanceFilter.DISTINCT.fromString("url"))
                .integrationIds(integrationIds)
                .build();
        response = dbService.groupByAndCalculate(company, filter);
        Assert.assertNotNull(response);
        Assert.assertEquals(2, response.getRecords().size());
        expectedResults = Set.of(
                DbAggregationResult.builder()
                        .key("url-1")
                        .count(2L)
                        .build(),
                DbAggregationResult.builder()
                        .key("url-0")
                        .count(1L)
                        .build()
        );
        Assert.assertEquals(expectedResults, new HashSet<>(response.getRecords()));

        //url with only one integration_id
        filter = CICDInstanceFilter.builder()
                .across(CICDInstanceFilter.DISTINCT.fromString("url"))
                .integrationIds(List.of(integrationIds.get(1)))
                .build();
        response = dbService.groupByAndCalculate(company, filter);
        Assert.assertNotNull(response);
        Assert.assertEquals(1, response.getRecords().size());
        expectedResults = Set.of(
                DbAggregationResult.builder()
                        .key("url-1")
                        .count(2L)
                        .build()
        );
        Assert.assertEquals(expectedResults, new HashSet<>(response.getRecords()));

        //name without integration_ids
        filter = CICDInstanceFilter.builder()
                .across(CICDInstanceFilter.DISTINCT.fromString("name"))
                .build();
        response = dbService.groupByAndCalculate(company, filter);
        Assert.assertNotNull(response);
        Assert.assertEquals(3, response.getRecords().size());
        expectedResults = Set.of(
                DbAggregationResult.builder()
                        .key("name-0")
                        .count(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("name-1")
                        .count(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("name-2")
                        .count(1L)
                        .build()
        );
        Assert.assertEquals(expectedResults, new HashSet<>(response.getRecords()));

        //name with integration_ids
        filter = CICDInstanceFilter.builder()
                .across(CICDInstanceFilter.DISTINCT.fromString("name"))
                .integrationIds(integrationIds)
                .build();
        response = dbService.groupByAndCalculate(company, filter);
        Assert.assertNotNull(response);
        Assert.assertEquals(3, response.getRecords().size());
        expectedResults = Set.of(
                DbAggregationResult.builder()
                        .key("name-0")
                        .count(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("name-1")
                        .count(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key("name-2")
                        .count(1L)
                        .build()
        );
        Assert.assertEquals(expectedResults, new HashSet<>(response.getRecords()));

        //name with only one integration_id
        filter = CICDInstanceFilter.builder()
                .across(CICDInstanceFilter.DISTINCT.fromString("name"))
                .integrationIds(List.of(integrationIds.get(0)))
                .build();
        response = dbService.groupByAndCalculate(company, filter);
        Assert.assertNotNull(response);
        Assert.assertEquals(1, response.getRecords().size());
        expectedResults = Set.of(
                DbAggregationResult.builder()
                        .key("name-0")
                        .count(1L)
                        .build()
        );
        Assert.assertEquals(expectedResults, new HashSet<>(response.getRecords()));

        //id without integration_ids
        filter = CICDInstanceFilter.builder()
                .across(CICDInstanceFilter.DISTINCT.fromString("id"))
                .build();
        response = dbService.groupByAndCalculate(company, filter);
        Assert.assertNotNull(response);
        Assert.assertEquals(3, response.getRecords().size());
        expectedResults = Set.of(
                DbAggregationResult.builder()
                        .key(uuidsList.get(0).toString())
                        .count(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key(uuidsList.get(1).toString())
                        .count(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key(uuidsList.get(2).toString())
                        .count(1L)
                        .build()
        );
        Assert.assertEquals(expectedResults, new HashSet<>(response.getRecords()));

        //id with integration_ids
        filter = CICDInstanceFilter.builder()
                .across(CICDInstanceFilter.DISTINCT.fromString("id"))
                .integrationIds(integrationIds)
                .build();
        response = dbService.groupByAndCalculate(company, filter);
        Assert.assertNotNull(response);
        Assert.assertEquals(3, response.getRecords().size());
        expectedResults = Set.of(
                DbAggregationResult.builder()
                        .key(uuidsList.get(0).toString())
                        .count(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key(uuidsList.get(1).toString())
                        .count(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key(uuidsList.get(2).toString())
                        .count(1L)
                        .build()
        );
        Assert.assertEquals(expectedResults, new HashSet<>(response.getRecords()));

        //id with only one integration_id
        filter = CICDInstanceFilter.builder()
                .across(CICDInstanceFilter.DISTINCT.fromString("id"))
                .integrationIds(List.of(integrationIds.get(1)))
                .build();
        response = dbService.groupByAndCalculate(company, filter);
        Assert.assertNotNull(response);
        Assert.assertEquals(2, response.getRecords().size());
        expectedResults = Set.of(
                DbAggregationResult.builder()
                        .key(uuidsList.get(1).toString())
                        .count(1L)
                        .build(),
                DbAggregationResult.builder()
                        .key(uuidsList.get(2).toString())
                        .count(1L)
                        .build()
        );
        Assert.assertEquals(expectedResults, new HashSet<>(response.getRecords()));

//        //type without integration_ids
//        filter = CICDInstanceFilter.builder()
//                .across(CICDInstanceFilter.DISTINCT.fromString("type"))
//                .build();
//        response = dbService.groupByAndCalculate(company, filter);
//        Assert.assertNotNull(response);
//        Assert.assertEquals(2, response.getRecords().size());
//        expectedResults = Set.of(
//                DbAggregationResult.builder()
//                        .key("jenkins")
//                        .count(2L)
//                        .build(),
//                DbAggregationResult.builder()
//                        .key("azure")
//                        .count(1L)
//                        .build()
//        );
//        Assert.assertEquals(expectedResults, new HashSet<>(response.getRecords()));
//
//        //type with integration_ids
//        filter = CICDInstanceFilter.builder()
//                .across(CICDInstanceFilter.DISTINCT.fromString("type"))
//                .integrationIds(integrationIds)
//                .build();
//        response = dbService.groupByAndCalculate(company, filter);
//        Assert.assertNotNull(response);
//        Assert.assertEquals(2, response.getRecords().size());
//        expectedResults = Set.of(
//                DbAggregationResult.builder()
//                        .key("jenkins")
//                        .count(2L)
//                        .build(),
//                DbAggregationResult.builder()
//                        .key("azure")
//                        .count(1L)
//                        .build()
//        );
//        Assert.assertEquals(expectedResults, new HashSet<>(response.getRecords()));
//
//        //type with only one integration_id
//        filter = CICDInstanceFilter.builder()
//                .across(CICDInstanceFilter.DISTINCT.fromString("type"))
//                .integrationIds(List.of(integrationIds.get(0)))
//                .build();
//        response = dbService.groupByAndCalculate(company, filter);
//        Assert.assertNotNull(response);
//        Assert.assertEquals(1, response.getRecords().size());
//        expectedResults = Set.of(
//                DbAggregationResult.builder()
//                        .key("jenkins")
//                        .count(1L)
//                        .build()
//        );
//        Assert.assertEquals(expectedResults, new HashSet<>(response.getRecords()));
    }
}
