package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CiCdInstanceConfig;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.models.BulkUpdateResponse;
import io.levelops.commons.models.CICDInstanceIntegAssignment;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static io.levelops.commons.databases.services.CiCdInstanceUtils.generateCiCdInstanceGuids;

@Log4j2
public class CICDInstancesDatabaseServiceAssignTest {
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
        Integration integration3 = Integration.builder()
                .name("test-integration-3")
                .url("http")
                .status("good")
                .application("zendesk")
                .description("description")
                .satellite(true)
                .build();
        integrationIds.add(integrationService.insert(company, integration3));
        Integration integration4 = Integration.builder()
                .name("test-integration-4")
                .url("http")
                .status("good")
                .application("zendesk")
                .description("description")
                .satellite(true)
                .build();
        integrationIds.add(integrationService.insert(company, integration4));

        uuidsList = generateCiCdInstanceGuids(3);
        CICDInstance cicdInstance = CICDInstance.builder()
                .id(uuidsList.get(0))
                .name("name-0")
                .url("url-0")
                .integrationId(integrationIds.get(0))
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
                .url("url-2")
                .integrationId(integrationIds.get(2))
                .lastHeartbeatAt(Instant.now())
                .config(CiCdInstanceConfig.builder()
                        .heartbeatDuration(21)
                        .bullseyeReportPaths("path")
                        .build())
                .build();
        dbService.insert(company, cicdInstance);

    }

    @Test
    public void testAssignIntegrationId() throws SQLException, JsonProcessingException {
        CICDInstanceIntegAssignment request = CICDInstanceIntegAssignment.builder()
                .integrationId(integrationIds.get(3))
                .addIds(Set.of(uuidsList.get(0).toString(), uuidsList.get(1).toString()))
                .removeIds(Set.of(uuidsList.get(2).toString()))
                .build();
        BulkUpdateResponse response = dbService.assignIntegrationId(company, request);
        Assert.assertNotNull(response);
        Assert.assertEquals(request.getAddIds().size() + request.getRemoveIds().size(),
                response.getRecords().size());
        Assert.assertEquals(request.getAddIds().size() + request.getRemoveIds().size(),
                response.getCount());

        List<String> actualIntegrationsId = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            dbService.get(company, uuidsList.get(i).toString())
                    .ifPresent(instance -> actualIntegrationsId.add(instance.getIntegrationId()));
        }
        List<String> expectedIntegrationsId = List.of(integrationIds.get(3), integrationIds.get(3));
        Assert.assertEquals(expectedIntegrationsId, actualIntegrationsId);
        Optional<CICDInstance> instance = dbService.get(company, uuidsList.get(2).toString());
        Assert.assertTrue(instance.isPresent());
        Assert.assertEquals(integrationIds.get(2), instance.get().getIntegrationId());

        request = CICDInstanceIntegAssignment.builder()
                .integrationId(integrationIds.get(3))
                .addIds(Set.of())
                .removeIds(Set.of())
                .build();
        response = dbService.assignIntegrationId(company, request);
        Assert.assertNotNull(response);
        Assert.assertEquals(0, response.getRecords().size());
        Assert.assertEquals(0, response.getCount());

        request = CICDInstanceIntegAssignment.builder()
                .integrationId("1234")
                .addIds(Set.of())
                .removeIds(Set.of())
                .build();
        response = dbService.assignIntegrationId(company, request);
        Assert.assertNotNull(response);
        Assert.assertEquals(0, response.getRecords().size());
        Assert.assertEquals(0, response.getCount());

        request = CICDInstanceIntegAssignment.builder()
                .removeIds(Set.of(uuidsList.get(0).toString()))
                .build();
        response = dbService.assignIntegrationId(company, request);
        Assert.assertNotNull(response);
        Assert.assertEquals(1, response.getRecords().size());
        Assert.assertEquals(1, response.getCount());

        request = CICDInstanceIntegAssignment.builder()
                .addIds(Set.of(uuidsList.get(0).toString(), uuidsList.get(2).toString()))
                .removeIds(Set.of(uuidsList.get(1).toString()))
                .build();
        response = dbService.assignIntegrationId(company, request);
        Assert.assertNotNull(response);
        Assert.assertEquals(3, response.getRecords().size());
        Assert.assertEquals(3, response.getCount());
    }
}
