package io.levelops.commons.databases.services.pagerduty;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Service;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.pagerduty.DbPDIncident;
import io.levelops.commons.databases.models.database.pagerduty.DbPagerDutyAlert;
import io.levelops.commons.databases.models.database.pagerduty.DbPagerDutyIncident;
import io.levelops.commons.databases.models.database.pagerduty.DbPagerDutyService;
import io.levelops.commons.databases.models.database.pagerduty.DbPagerDutyStatus;
import io.levelops.commons.databases.models.database.pagerduty.DbPagerDutyUser;
import io.levelops.commons.databases.models.database.pagerduty.DbPdAlert;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.PagerDutyFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ProductService;
import io.levelops.commons.databases.services.ServicesDatabaseService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.web.exceptions.BadRequestException;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class PagerDutyIncidentsDatabaseService2Test {

    private static final String company = "test";
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;

    private static NamedParameterJdbcTemplate template;

    private static ServicesDatabaseService servicesDbService;
    private static PagerDutyServicesDatabaseService pdServicesDbService;
    private static PagerDutyIncidentsDatabaseService pdIncidentsDbService;
    private static PagerDutyAlertsDatabaseService pdAlertsDbService;
    private static CiCdJobsDatabaseService cicdJobsDbService;
    private static CiCdInstancesDatabaseService cicdInstancesDatabaseService;
    private static UserService userService;
    private static ProductService productService;
    private static IntegrationService integrationsService;
    private static UserIdentityService userIdentityService;
    private static TeamMembersDatabaseService teamMembersDatabaseService;
    private static ProductsDatabaseService productsDatabaseService;

    private static UUID pdServiceId1;
    private static UUID pdServiceId2;
    private static List<DbPagerDutyIncident> incidents = new ArrayList<>();
    private static List<DbPagerDutyAlert> alerts = new ArrayList<>();

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        template = new NamedParameterJdbcTemplate(dataSource);
        servicesDbService = new ServicesDatabaseService(DefaultObjectMapper.get(), dataSource);
        pdServicesDbService = new PagerDutyServicesDatabaseService(DefaultObjectMapper.get(), dataSource);
        pdIncidentsDbService = new PagerDutyIncidentsDatabaseService(mapper, dataSource);
        pdAlertsDbService = new PagerDutyAlertsDatabaseService(mapper, dataSource);
        userService = new UserService(dataSource, mapper);
        productService = new ProductService(dataSource);
        productsDatabaseService = new ProductsDatabaseService(dataSource, mapper);
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, mapper);
        userIdentityService = new UserIdentityService(dataSource);
        cicdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        cicdJobsDbService = new CiCdJobsDatabaseService(dataSource);
        integrationsService = new IntegrationService(dataSource);
        List.of(
                "CREATE SCHEMA IF NOT EXISTS " + company,
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";"
        )
                .forEach(template.getJdbcTemplate()::execute);
        integrationsService.ensureTableExistence(company);
        userIdentityService.ensureTableExistence(company);
        productsDatabaseService.ensureTableExistence(company);
        servicesDbService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);
        pdServicesDbService.ensureTableExistence(company);
        pdIncidentsDbService.ensureTableExistence(company);
        pdAlertsDbService.ensureTableExistence(company);
        userService.ensureTableExistence(company);
        productService.ensureTableExistence(company);
        cicdInstancesDatabaseService.ensureTableExistence(company);
        cicdJobsDbService.ensureTableExistence(company);
        var serviceId1 = UUID.randomUUID();
        var service = Service.builder()
                .id(serviceId1)
                .type(Service.Type.PAGERDUTY)
                .name("My service1")
                .createdAt(Instant.ofEpochSecond(1600637801L))
                .build();
        servicesDbService.insert(company, service);

        var integration = Integration.builder()
                .description("description")
                .name("Integration 1")
                .satellite(false)
                .application("pagerduty")
                .status("ok")
                .build();
        var integrationId = integrationsService.insert(company, integration);

        var pdService = DbPagerDutyService.builder()
                .name("My Service 1")
                .pdId("PDU1")
                .integrationId(1)
                .serviceId(serviceId1)
                .createdAt(Instant.ofEpochSecond(1600647692))
                .build();
        pdServiceId1 = UUID.fromString(pdServicesDbService.insert(company, pdService));

        var pdService2 = DbPagerDutyService.builder()
                .name("My Service 2")
                .pdId("PDU2")
                .integrationId(1)
                .serviceId(serviceId1)
                .createdAt(Instant.ofEpochSecond(1600647692))
                .build();
        pdServiceId2 = UUID.fromString(pdServicesDbService.insert(company, pdService2));


        var pdIncident = DbPagerDutyIncident.builder()
                .pdId("PDU2")
                .pdServiceId(pdServiceId1)
                .priority("P0")
                .status("high")
                .summary("summary")
                .urgency("high")
                .createdAt(Instant.ofEpochSecond(1637388504))
                .updatedAt(Instant.ofEpochSecond(1637561336))
                .lastStatusAt(Instant.ofEpochSecond(1637474904))
                .statuses(Set.of(DbPagerDutyStatus.builder()
                                .status("acknowledged")
                                .timestamp(Instant.ofEpochSecond(1637474904))
                                .user(DbPagerDutyUser.builder()
                                        .pdId("PDU1")
                                        .name("name")
                                        .email("sample@email")
                                        .build())
                                .build(),
                        DbPagerDutyStatus.builder()
                                .status("triggered")
                                .timestamp(Instant.ofEpochSecond(1637402904))
                                .user(DbPagerDutyUser.builder()
                                        .pdId("PDU2")
                                        .name("name2")
                                        .email("sample2@email")
                                        .build())
                                .build()))
                .build();
        var pdIncidentId1 = pdIncidentsDbService.insert(company, pdIncident);
        incidents.add(pdIncident);
        var pdIncident2 = DbPagerDutyIncident.builder()
                .pdId("PDU1")
                .pdServiceId(pdServiceId2)
                .priority("P1")
                .status("high")
                .summary("summary2")
                .urgency("low")
                .createdAt(Instant.ofEpochSecond(1637230080))
                .updatedAt(Instant.ofEpochSecond(1637561940))
                .lastStatusAt(Instant.ofEpochSecond(1637230085))
                .statuses(Set.of(DbPagerDutyStatus.builder()
                        .status("acknowledged")
                        .timestamp(Instant.ofEpochSecond(1637402945))
                        .user(DbPagerDutyUser.builder()
                                .pdId("PDU1")
                                .name("srinath")
                                .email("srinath@emai.com")
                                .build())
                        .build()))
                .build();
        var pdIncidentId2 = pdIncidentsDbService.insert(company, pdIncident2);
        incidents.add(pdIncident2);
        var pdAlert1 = DbPagerDutyAlert.builder()
                .incidentId(UUID.fromString(pdIncidentId1))
                .severity("low")
                .pdId("pdId")
                .pdServiceId(pdServiceId1)
                .summary("summary")
                .status("acknowledged")
                .createdAt(Instant.ofEpochSecond(1637388504))
                .updatedAt(Instant.ofEpochSecond(1637388504))
                .lastStatusAt(Instant.ofEpochSecond(1637474904))
                .build();
        var pdAlertId1 = pdAlertsDbService.insert(company, pdAlert1);

        var pdAlert2 = DbPagerDutyAlert.builder()
                .incidentId(UUID.fromString(pdIncidentId2))
                .severity("high")
                .pdId("pdId2")
                .pdServiceId(pdServiceId1)
                .summary("summary")
                .status("triggered")
                .createdAt(Instant.ofEpochSecond(1637388504))
                .updatedAt(Instant.ofEpochSecond(1637388504))
                .lastStatusAt(Instant.ofEpochSecond(1637474999))
                .build();
        var pdAlertId2 = pdAlertsDbService.insert(company, pdAlert2);
        alerts.add(pdAlert1);
        alerts.add(pdAlert2);
        var pdAlert3 = DbPagerDutyAlert.builder()
                .incidentId(UUID.fromString(pdIncidentId2))
                .severity("high")
                .pdId("pdId2")
                .pdServiceId(pdServiceId1)
                .summary("summary")
                .status("triggered")
                .createdAt(Instant.ofEpochSecond(1637388504))
                .updatedAt(Instant.ofEpochSecond(1637388504))
                .lastStatusAt(Instant.ofEpochSecond(1637474999))
                .build();
        var pdAlertId3 = pdAlertsDbService.insert(company, pdAlert3);
        alerts.add(pdAlert3);
    }


    @Test
    public void testList() throws SQLException, BadRequestException {
        DbListResponse<DbAggregationResult> dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.user_id)
                        .issueType("incident")
                        .calculation(PagerDutyFilter.CALCULATION.response_time)
                        .build(), null, OUConfiguration.builder().build(),
                Map.of());
        Assertions.assertThat(dbListResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(dbListResponse.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("srinath", "name2");

        String userId = dbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()).get(0);
        DbListResponse<DbPDIncident> listResponse = pdIncidentsDbService.list(company, PagerDutyFilter.builder()
                        .issueType("incident")
                        .userIds(List.of(userId))
                        .build(), Map.of(),
                OUConfiguration.builder().build(), Set.of(), 0, 10000);
        Assertions.assertThat(listResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(listResponse.getTotalCount()).isEqualTo(2);

        userId = dbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()).get(1);
        listResponse = pdIncidentsDbService.list(company, PagerDutyFilter.builder()
                        .issueType("incident")
                        .userIds(List.of(userId))
                        .build(), Map.of(),
                OUConfiguration.builder().build(), Set.of(), 0, 10000);
        Assertions.assertThat(listResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(listResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(listResponse.getRecords().stream().map(DbPDIncident::getServiceName).collect(Collectors.toList()))
        .containsExactly("My Service 1");

        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.incident_priority)
                        .issueType("incident")
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(),
                Map.of());
        Assertions.assertThat(listResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(listResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("P1", "P0");
        listResponse = pdIncidentsDbService.list(company, PagerDutyFilter.builder()
                        .issueType("incident")
                        .incidentPriorities(List.of("P1"))
                        .build(), Map.of(),
                OUConfiguration.builder().build(), Set.of(), 0, 10000);
        Assertions.assertThat(listResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(listResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(listResponse.getRecords().stream().map(DbPDIncident::getPriority).collect(Collectors.toList()))
                .containsExactly("P1");

        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.status)
                        .issueType("incident")
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(),
                Map.of());
        Assertions.assertThat(listResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(listResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("high");

        listResponse = pdIncidentsDbService.list(company, PagerDutyFilter.builder()
                        .issueType("incident")
                        .incidentStatuses(List.of("high"))
                        .build(), Map.of(),
                OUConfiguration.builder().build(), Set.of(), 0, 10000);
        Assertions.assertThat(listResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(listResponse.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(listResponse.getRecords().stream().map(DbPDIncident::getStatus).collect(Collectors.toList()))
                .containsExactly("high", "high");

        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.alert_severity)
                        .issueType("alert")
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(),
                Map.of());
        Assertions.assertThat(listResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(listResponse.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("high", "low");
        DbListResponse<DbPdAlert> list = pdAlertsDbService.list(company, PagerDutyFilter.builder()
                        .issueType("alert")
                        .alertSeverities(List.of("high"))
                        .build(), Map.of(), Set.of()
                , 0, 10000, OUConfiguration.builder().build());
        Assertions.assertThat(list.getRecords()).isNotEmpty();
        Assertions.assertThat(list.getTotalCount()).isEqualTo(1);

    }

    @Test
    public void testGroupBy() throws SQLException {
        DbListResponse<DbAggregationResult> dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.status)
                        .issueType("incident")
                        .calculation(PagerDutyFilter.CALCULATION.response_time)
                        .build(), null, OUConfiguration.builder().build(),
                Map.of());
        DefaultObjectMapper.prettyPrint(dbListResponse);
        assertThat(dbListResponse.getRecords()).isNotEmpty();
        assertThat(dbListResponse.getTotalCount()).isEqualTo(1);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .isEqualTo(List.of(2L));
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getMean).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getP90).collect(Collectors.toList()))
                .isNotEmpty();

        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.alert_severity)
                        .issueType("incident")
                        .calculation(PagerDutyFilter.CALCULATION.response_time)
                        .build(), null, OUConfiguration.builder().build(),
                Map.of());
        DefaultObjectMapper.prettyPrint(dbListResponse);
        assertThat(dbListResponse.getRecords()).isNotEmpty();
        assertThat(dbListResponse.getTotalCount()).isEqualTo(2);
        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.alert_created_at)
                        .issueType("incident")
                        .calculation(PagerDutyFilter.CALCULATION.response_time)
                        .build(), null, OUConfiguration.builder().build(),
                Map.of());
        DefaultObjectMapper.prettyPrint(dbListResponse);
        assertThat(dbListResponse.getRecords()).isNotEmpty();
        assertThat(dbListResponse.getTotalCount()).isEqualTo(1);
        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.alert_resolved_at)
                        .issueType("incident")
                        .calculation(PagerDutyFilter.CALCULATION.response_time)
                        .build(), null, OUConfiguration.builder().build(),
                Map.of());
        DefaultObjectMapper.prettyPrint(dbListResponse);
        assertThat(dbListResponse.getRecords()).isNotEmpty();
        assertThat(dbListResponse.getTotalCount()).isEqualTo(1);
        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.status)
                        .incidentStatuses(List.of("low"))
                        .issueType("incident")
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        DefaultObjectMapper.prettyPrint(dbListResponse);
        assertThat(dbListResponse.getRecords()).isEmpty();
        assertThat(dbListResponse.getTotalCount()).isEqualTo(0);

        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.status)
                        .incidentStatuses(List.of("high"))
                        .issueType("incident")
                        .calculation(PagerDutyFilter.CALCULATION.response_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        DefaultObjectMapper.prettyPrint(dbListResponse);
        assertThat(dbListResponse.getRecords()).isNotEmpty();
        assertThat(dbListResponse.getTotalCount()).isEqualTo(1);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .isEqualTo(List.of(2L));
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getMean).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getP90).collect(Collectors.toList()))
                .isNotEmpty();

        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.incident_priority)
                        .issueType("incident")
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        DefaultObjectMapper.prettyPrint(dbListResponse);
        assertThat(dbListResponse.getRecords()).isNotEmpty();
        assertThat(dbListResponse.getTotalCount()).isEqualTo(2);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(1L, 1L);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getMean).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getP90).collect(Collectors.toList()))
                .isNotEmpty();
        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.incident_priority)
                        .issueType("incident")
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        DefaultObjectMapper.prettyPrint(dbListResponse);
        assertThat(dbListResponse.getRecords()).isNotEmpty();
        assertThat(dbListResponse.getTotalCount()).isEqualTo(2);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(1L, 1L);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getMean).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getP90).collect(Collectors.toList()))
                .isNotEmpty();

        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.incident_priority)
                        .issueType("incident")
                        .incidentPriorities(List.of("P0"))
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        assertThat(dbListResponse.getRecords()).isNotEmpty();
        assertThat(dbListResponse.getTotalCount()).isEqualTo(1);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(1L);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("P0");
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getMean).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getP90).collect(Collectors.toList()))
                .isNotEmpty();

        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.incident_priority)
                        .issueType("incident")
                        .incidentPriorities(List.of("P1"))
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        assertThat(dbListResponse.getRecords()).isNotEmpty();
        assertThat(dbListResponse.getTotalCount()).isEqualTo(1);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(1L);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("P1");
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getMean).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getP90).collect(Collectors.toList()))
                .isNotEmpty();
        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.user_id)
                        .issueType("incident")
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        assertThat(dbListResponse.getRecords()).isNotEmpty();
        assertThat(dbListResponse.getTotalCount()).isEqualTo(2);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(2L, 1L);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getMean).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getP90).collect(Collectors.toList()))
                .isNotEmpty();
        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.incident_resolved_at)
                        .issueType("incident")
                        .calculation(PagerDutyFilter.CALCULATION.response_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        assertThat(dbListResponse.getRecords()).isEmpty();
        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.alert_resolved_at)
                        .issueType("alert")
                        .aggInterval(AGG_INTERVAL.month)
                        .calculation(PagerDutyFilter.CALCULATION.response_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        assertThat(dbListResponse.getRecords()).isNotEmpty();
        assertThat(dbListResponse.getTotalCount()).isEqualTo(1);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getP90).collect(Collectors.toList()))
                .isNotEmpty();

        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.incident_priority)
                        .issueType("incident")
                        .alertStatuses(List.of("acknowledged"))
                        .calculation(PagerDutyFilter.CALCULATION.response_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        assertThat(dbListResponse.getRecords()).isNotEmpty();
        assertThat(dbListResponse.getTotalCount()).isEqualTo(1);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getP90).collect(Collectors.toList()))
                .isNotEmpty();
        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.incident_priority)
                        .issueType("incident")
                        .missingFields(Map.of("priority", true))
                        .calculation(PagerDutyFilter.CALCULATION.response_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        assertThat(dbListResponse.getRecords()).isEmpty();
        assertThat(dbListResponse.getTotalCount()).isEqualTo(0);
        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.user_id)
                        .issueType("incident")
                        .missingFields(Map.of("user_id", true))
                        .calculation(PagerDutyFilter.CALCULATION.response_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        assertThat(dbListResponse.getRecords()).isEmpty();
        assertThat(dbListResponse.getTotalCount()).isEqualTo(0);
    }

    @Test
    public void testStacks() throws SQLException {
        DbListResponse<DbAggregationResult> dbListResponse = pdIncidentsDbService.stackedGroupBy(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.user_id)
                        .issueType("incident")
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, List.of(PagerDutyFilter.DISTINCT.incident_priority), OUConfiguration.builder().build(), Map.of());
        assertThat(dbListResponse.getTotalCount()).isEqualTo(2);
        assertThat(dbListResponse.getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .size()).isBetween(1, 2);
        assertThat(dbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).isNotEmpty();
        assertThat(dbListResponse
                .getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey).collect(Collectors.toList())
        ).containsAnyOf("P0", "P1");
        dbListResponse = pdIncidentsDbService.stackedGroupBy(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.user_id)
                        .issueType("incident")
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, List.of(PagerDutyFilter.DISTINCT.pd_service), OUConfiguration.builder().build(), Map.of());
        assertThat(dbListResponse.getTotalCount()).isEqualTo(2);
        assertThat(dbListResponse.getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .size()).isBetween(1, 2);
        assertThat(dbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).isNotEmpty();
        dbListResponse = pdIncidentsDbService.stackedGroupBy(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.user_id)
                        .issueType("incident")
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, List.of(PagerDutyFilter.DISTINCT.pd_service), OUConfiguration.builder().build(), Map.of());
        assertThat(dbListResponse.getTotalCount()).isEqualTo(2);
        assertThat(dbListResponse.getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .size()).isBetween(1, 2);
        assertThat(dbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).isNotEmpty();
        assertThat(dbListResponse
                .getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey).collect(Collectors.toList())
        ).isNotEmpty();

        dbListResponse = pdIncidentsDbService.stackedGroupBy(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.pd_service)
                        .issueType("incident")
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, List.of(PagerDutyFilter.DISTINCT.user_id), OUConfiguration.builder().build(), Map.of());
        assertThat(dbListResponse.getTotalCount()).isEqualTo(2);
        assertThat(dbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).isNotEmpty();
        dbListResponse = pdIncidentsDbService.stackedGroupBy(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.status)
                        .issueType("alert")
                        .aggInterval(AGG_INTERVAL.day)
                        .calculation(PagerDutyFilter.CALCULATION.response_time)
                        .build(), null, List.of(PagerDutyFilter.DISTINCT.alert_severity), OUConfiguration.builder().build(), Map.of());
        assertThat(dbListResponse.getTotalCount()).isEqualTo(2);
        assertThat(dbListResponse.getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .size()).isBetween(1, 2);
        assertThat(dbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).containsAnyOf("acknowledged", "triggered");
        assertThat(dbListResponse
                .getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey).collect(Collectors.toList())
        ).containsAnyOf("low", "high");
    }

    @Test
    public void testDateGroupBy() throws SQLException {
        List<Long> listOfInputDates = incidents.stream().map(DbPagerDutyIncident::getCreatedAt).map(Instant::getEpochSecond).collect(Collectors.toList());
        List<String> actualList;
        List<String> expectedList;
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)), Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        DbListResponse<DbAggregationResult> dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.incident_created_at)
                        .issueType("incident")
                        .aggInterval(AGG_INTERVAL.month)
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        DefaultObjectMapper.prettyPrint(dbListResponse);
        actualList = dbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(dbListResponse);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();

        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)), Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.incident_created_at)
                        .issueType("incident")
                        .aggInterval(AGG_INTERVAL.year)
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        DefaultObjectMapper.prettyPrint(dbListResponse);
        actualList = dbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(dbListResponse);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();

        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)), Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.incident_created_at)
                        .issueType("incident")
                        .aggInterval(AGG_INTERVAL.quarter)
                        .calculation(PagerDutyFilter.CALCULATION.response_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        DefaultObjectMapper.prettyPrint(dbListResponse);
        actualList = dbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(dbListResponse);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();

        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)), Calendar.WEEK_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.incident_created_at)
                        .issueType("incident")
                        .aggInterval(AGG_INTERVAL.week)
                        .calculation(PagerDutyFilter.CALCULATION.response_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        DefaultObjectMapper.prettyPrint(dbListResponse);
        actualList = dbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.WEEK_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(dbListResponse);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();

        listOfInputDates = alerts.stream().map(DbPagerDutyAlert::getCreatedAt).map(Instant::getEpochSecond).collect(Collectors.toList());
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)), Calendar.WEEK_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.alert_created_at)
                        .issueType("alert")
                        .aggInterval(AGG_INTERVAL.week)
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        DefaultObjectMapper.prettyPrint(dbListResponse);
        actualList = dbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.WEEK_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(dbListResponse);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();


        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)), Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.alert_created_at)
                        .issueType("alert")
                        .aggInterval(AGG_INTERVAL.month)
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        DefaultObjectMapper.prettyPrint(dbListResponse);
        actualList = dbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(dbListResponse);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();

        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)), Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.alert_created_at)
                        .issueType("alert")
                        .aggInterval(AGG_INTERVAL.year)
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        DefaultObjectMapper.prettyPrint(dbListResponse);
        actualList = dbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(dbListResponse);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();

        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)), Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.alert_created_at)
                        .issueType("alert")
                        .aggInterval(AGG_INTERVAL.quarter)
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        DefaultObjectMapper.prettyPrint(dbListResponse);
        actualList = dbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(dbListResponse);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
    }

    @Test
    public void testGroupByAlerts() throws SQLException {
        DbListResponse<DbAggregationResult> dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.status)
                        .issueType("alert")
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        DefaultObjectMapper.prettyPrint(dbListResponse);
        assertThat(dbListResponse.getRecords()).isNotEmpty();
        assertThat(dbListResponse.getTotalCount()).isEqualTo(2);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(1L, 1L);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("acknowledged", "triggered");
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getMean).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getP90).collect(Collectors.toList()))
                .isNotEmpty();
        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.pd_service)
                        .issueType("alert")
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        DefaultObjectMapper.prettyPrint(dbListResponse);
        assertThat(dbListResponse.getRecords()).isNotEmpty();
        assertThat(dbListResponse.getTotalCount()).isEqualTo(1);
        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.pd_service)
                        .issueType("alert")
                        .incidentStatuses(List.of("high"))
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        DefaultObjectMapper.prettyPrint(dbListResponse);
        assertThat(dbListResponse.getRecords()).isNotEmpty();
        assertThat(dbListResponse.getTotalCount()).isEqualTo(1);

        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.status)
                        .incidentAcknowledgedAt(ImmutablePair.of(555L,444L))
                        .issueType("alert")
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        assertThat(dbListResponse.getRecords()).isEmpty();
        dbListResponse =  pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.pd_service)
                        .issueType("alert")
                        .incidentStatuses(List.of("high"))
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        DefaultObjectMapper.prettyPrint(dbListResponse);
        assertThat(dbListResponse.getRecords()).isNotEmpty();
        assertThat(dbListResponse.getTotalCount()).isEqualTo(1);
        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.alert_created_at)
                        .aggInterval(AGG_INTERVAL.month)
                        .issueType("alert")
                        .calculation(PagerDutyFilter.CALCULATION.response_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        assertThat(dbListResponse.getRecords()).isNotEmpty();
        assertThat(dbListResponse.getTotalCount()).isEqualTo(1);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(2L);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getMean).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList()))
                .isNotEmpty();
        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.alert_severity)
                        .issueType("alert")
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        DefaultObjectMapper.prettyPrint(dbListResponse);
        assertThat(dbListResponse.getRecords()).isNotEmpty();
        assertThat(dbListResponse.getTotalCount()).isEqualTo(2);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(1L, 1L);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("high", "low");
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getMean).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getP90).collect(Collectors.toList()))
                .isNotEmpty();

        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.alert_severity)
                        .issueType("alert")
                        .alertSeverities(List.of("high"))
                        .calculation(PagerDutyFilter.CALCULATION.response_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        DefaultObjectMapper.prettyPrint(dbListResponse);
        assertThat(dbListResponse.getRecords()).isNotEmpty();
        assertThat(dbListResponse.getTotalCount()).isEqualTo(1);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(1L);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("high");
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getMean).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getP90).collect(Collectors.toList()))
                .isNotEmpty();
        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.alert_severity)
                        .issueType("alert")
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        assertThat(dbListResponse.getRecords()).isNotEmpty();
        assertThat(dbListResponse.getTotalCount()).isEqualTo(2);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(1L, 1L);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("high", "low");
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getMean).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getP90).collect(Collectors.toList()))
                .isNotEmpty();

        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.alert_severity)
                        .issueType("alert")
                        .alertSeverities(List.of("low"))
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        assertThat(dbListResponse.getRecords()).isNotEmpty();
        assertThat(dbListResponse.getTotalCount()).isEqualTo(1);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(1L);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("low");
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getMean).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getP90).collect(Collectors.toList()))
                .isNotEmpty();

        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.incident_priority)
                        .officeHoursFrom(1637388504L)
                        .OfficeHoursTo(1637474904L)
                        .officeHours(ImmutablePair.of("09:00", "14:00"))
                        .issueType("incident")
                        .alertSeverities(List.of("low"))
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        assertThat(dbListResponse.getRecords()).isNotEmpty();
        assertThat(dbListResponse.getTotalCount()).isEqualTo(1);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(1L);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("P0");
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getMean).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList()))
                .isNotEmpty();
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getP90).collect(Collectors.toList()))
                .isNotEmpty();

    }

    @Test
    public void testListIncidents() throws SQLException {
        DbListResponse<DbPDIncident> listResponse = pdIncidentsDbService.list(company, PagerDutyFilter.builder()
                        .issueType("incident")
                        .build(), Map.of(),
                OUConfiguration.builder().build(), Set.of(), 0, 10000);
        Assertions.assertThat(listResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(listResponse.getTotalCount()).isEqualTo(2);
        listResponse = pdIncidentsDbService.list(company, PagerDutyFilter.builder()
                        .issueType("incident")
                        .incidentUrgencies(List.of("low"))
                        .incidentPriorities(List.of())
                        .build(), Map.of(),
                OUConfiguration.builder().build(), Set.of(), 0, 10000);
        Assertions.assertThat(listResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(listResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(listResponse.getRecords().stream().map(DbPDIncident::getUrgency).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("low");
        listResponse = pdIncidentsDbService.list(company, PagerDutyFilter.builder()
                        .issueType("incident")
                        .alertSeverities(List.of("low"))
                        .incidentPriorities(List.of())
                        .build(), Map.of(),
                OUConfiguration.builder().build(), Set.of(), 0, 10000);
        Assertions.assertThat(listResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(listResponse.getTotalCount()).isEqualTo(1);
        listResponse = pdIncidentsDbService.list(company, PagerDutyFilter.builder()
                        .issueType("incident")
                        .incidentStatuses(List.of("low"))
                        .incidentPriorities(List.of())
                        .build(), Map.of(),
                OUConfiguration.builder().build(), Set.of(), 0, 10000);
        Assertions.assertThat(listResponse.getRecords()).isEmpty();
        Assertions.assertThat(listResponse.getTotalCount()).isEqualTo(0);
        listResponse = pdIncidentsDbService.list(company, PagerDutyFilter.builder()
                        .issueType("incident")
                        .incidentStatuses(List.of("high"))
                        .incidentPriorities(List.of())
                        .build(), Map.of("incident_created_at", SortingOrder.DESC),
                OUConfiguration.builder().build(), Set.of(), 0, 10000);
        Assertions.assertThat(listResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(listResponse.getTotalCount()).isEqualTo(2);
    }

    @Test
    public void testAlertsList() throws SQLException {
        DbListResponse<DbPdAlert> dbListResponse = pdAlertsDbService.list(company, PagerDutyFilter.builder()
                        .issueType("alert")
                        .build(), Map.of(), Set.of(), 0, 10000, null);
        Assertions.assertThat(dbListResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(dbListResponse.getTotalCount()).isEqualTo(2);

        dbListResponse = pdAlertsDbService.list(company, PagerDutyFilter.builder()
                        .issueType("alert")
                        .build(), Map.of(), Set.of(), 0, 10000, null);
        Assertions.assertThat(dbListResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(dbListResponse.getTotalCount()).isEqualTo(2);
        dbListResponse = pdAlertsDbService.list(company, PagerDutyFilter.builder()
                        .issueType("incident")
                        .alertSeverities(List.of("high"))
                        .build(), Map.of(), Set.of(), 0, 10000, null);
        Assertions.assertThat(dbListResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(dbListResponse.getTotalCount()).isEqualTo(1);
        dbListResponse = pdAlertsDbService.list(company, PagerDutyFilter.builder()
                        .issueType("incident")
                        .alertStatuses(List.of("acknowledged"))
                        .build(), Map.of(), Set.of(), 0, 10000, null);
        Assertions.assertThat(dbListResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(dbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(dbListResponse.getRecords().stream().map(DbPdAlert::getStatus).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("acknowledged");
        dbListResponse = pdAlertsDbService.list(company, PagerDutyFilter.builder()
                        .issueType("incident")
                        .build(), Map.of("alert_updated_at", SortingOrder.DESC), Set.of(), 0, 10000, null);
        Assertions.assertThat(dbListResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(dbListResponse.getTotalCount()).isEqualTo(2);
        dbListResponse = pdAlertsDbService.list(company, PagerDutyFilter.builder()
                        .issueType("alert")
                        .incidentPriorities(List.of("P0"))
                        .build(), Map.of("alert_updated_at", SortingOrder.DESC), Set.of(), 0, 10000, null);
        Assertions.assertThat(dbListResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(dbListResponse.getTotalCount()).isEqualTo(1);
        dbListResponse = pdAlertsDbService.list(company, PagerDutyFilter.builder()
                        .issueType("alert")
                        .incidentPriorities(List.of("P0"))
                        .incidentResolvedAt(ImmutablePair.of(0L,999999999L))
                        .build(), Map.of("alert_updated_at", SortingOrder.DESC), Set.of(), 0, 10000, null);
        Assertions.assertThat(dbListResponse.getRecords()).isEmpty();
    }

    @Test
    public void testDrilldownGroupByCounts() throws SQLException {
        DbListResponse<DbAggregationResult> dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.incident_priority)
                        .calculation(PagerDutyFilter.CALCULATION.response_time)
                        .incidentPriorities(List.of("P0"))
                        .issueType("incident")
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        Assertions.assertThat(dbListResponse.getTotalCount()).isEqualTo(1);
        DbListResponse<DbPDIncident> listResponse = pdIncidentsDbService.list(company, PagerDutyFilter.builder()
                        .issueType("incident")
                        .incidentPriorities(List.of("P0"))
                        .build(), Map.of(),
                OUConfiguration.builder().build(), Set.of(), 0, 10000);
        Assertions.assertThat(Long.valueOf(listResponse.getTotalCount())).isEqualTo(dbListResponse.getRecords()
                .stream().map(DbAggregationResult::getCount).collect(Collectors.toList()).get(0));
        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.status)
                        .alertStatuses(List.of("triggered"))
                        .issueType("alert")
                        .calculation(PagerDutyFilter.CALCULATION.response_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        Assertions.assertThat(dbListResponse.getTotalCount()).isEqualTo(1);
        DbListResponse<DbPdAlert> response = pdAlertsDbService.list(company, PagerDutyFilter.builder()
                        .issueType("alert")
                        .alertStatuses(List.of("triggered"))
                        .build(), Map.of(), Set.of(), 0, 10000, null);
        Assertions.assertThat(Long.valueOf(response.getTotalCount())).isEqualTo(dbListResponse.getRecords()
                .stream().map(DbAggregationResult::getCount).collect(Collectors.toList()).get(0));

    }

    @Test
    public void testSort() throws SQLException {
        DbListResponse<DbAggregationResult> dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.status)
                        .issueType("incident")
                        .calculation(PagerDutyFilter.CALCULATION.response_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of("status", SortingOrder.ASC));
        List<Long> result4 = dbListResponse.getRecords().stream().map(DbAggregationResult::getMax).collect(Collectors.toList());
        assertThat(dbListResponse.getTotalCount()).isEqualTo(1);
        assertThat(result4).isSorted();
        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.incident_created_at)
                        .issueType("incident")
                        .calculation(PagerDutyFilter.CALCULATION.response_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of("incident_created_at", SortingOrder.DESC));
        result4 = dbListResponse.getRecords().stream().map(DbAggregationResult::getKey).map(Long::valueOf).collect(Collectors.toList());
        Collections.reverse(result4);
        assertThat(result4).isSorted();
        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.incident_created_at)
                        .issueType("incident")
                        .calculation(PagerDutyFilter.CALCULATION.response_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of("incident_created_at", SortingOrder.ASC));
        result4 = dbListResponse.getRecords().stream().map(DbAggregationResult::getKey).map(Long::valueOf).collect(Collectors.toList());
        assertThat(result4).isSorted();
        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.pd_service)
                        .issueType("incident")
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        result4 = dbListResponse.getRecords().stream().map(DbAggregationResult::getMax).collect(Collectors.toList());
        Collections.reverse(result4);
        assertThat(result4).isSorted();
        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.alert_created_at)
                        .issueType("alert")
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of("alert_created_at", SortingOrder.ASC));
        result4 = dbListResponse.getRecords().stream().map(DbAggregationResult::getMax).collect(Collectors.toList());
        Collections.reverse(result4);
        assertThat(result4).isSorted();

        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.alert_severity)
                        .issueType("alert")
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of("alert_severity", SortingOrder.ASC));
        List<String> stringList = dbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList());
        assertThat(stringList).isSorted();

        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.pd_service)
                        .issueType("incident")
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of("pd_service", SortingOrder.DESC));
        stringList = dbListResponse.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList());
        assertThat(stringList).containsExactly("My Service 2", "My Service 1");

        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.pd_service)
                        .issueType("incident")
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of("pd_service", SortingOrder.ASC));
        stringList = dbListResponse.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList());
        assertThat(stringList).containsExactly("My Service 1", "My Service 2");
        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.user_id)
                        .issueType("incident")
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of("user_id", SortingOrder.ASC));
        stringList = dbListResponse.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList());
        assertThat(stringList).containsExactly("name2", "srinath");
        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.user_id)
                        .issueType("incident")
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of("resolution_time", SortingOrder.ASC));
        Assertions.assertThat(dbListResponse.getTotalCount()).isEqualTo(2);
        result4 = dbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList());
        assertThat(result4).isSorted();
        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.user_id)
                        .issueType("incident")
                        .calculation(PagerDutyFilter.CALCULATION.response_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of("response_time", SortingOrder.ASC));
        Assertions.assertThat(dbListResponse.getTotalCount()).isEqualTo(2);
        result4 = dbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList());
        assertThat(result4).isSorted();
        dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.user_id)
                        .issueType("incident")
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of("user_id", SortingOrder.DESC));
        stringList = dbListResponse.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList());
        assertThat(stringList).containsExactly("srinath", "name2");
        DbListResponse<DbPDIncident> listResponse = pdIncidentsDbService.list(company, PagerDutyFilter.builder()
                        .issueType("incident")
                        .build(),  Map.of("incident_created_at", SortingOrder.ASC),
                OUConfiguration.builder().build(), Set.of(), 0, 10000);
        List<Instant> instantList = listResponse.getRecords().stream().map(DbPDIncident::getCreatedAt).collect(Collectors.toList());
        assertThat(instantList).isSorted();

        listResponse = pdIncidentsDbService.list(company, PagerDutyFilter.builder()
                        .issueType("incident")
                        .build(),  Map.of("incident_created_at", SortingOrder.DESC),
                OUConfiguration.builder().build(), Set.of(), 0, 10000);
        instantList = listResponse.getRecords().stream().map(DbPDIncident::getCreatedAt).collect(Collectors.toList());
        Collections.reverse(instantList);
        assertThat(instantList).isSorted();
    }

    @Test
    public void testMissingFilters() throws SQLException {
        DbListResponse<DbAggregationResult> dbListResponse = pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.alert_severity)
                        .issueType("incident")
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of("alert_severity", SortingOrder.DESC));
        Assertions.assertThat(dbListResponse.getTotalCount()).isEqualTo(2);
        DbListResponse<DbPDIncident> listResponse = pdIncidentsDbService.list(company, PagerDutyFilter.builder()
                        .issueType("incident")
                        .missingFields(Map.of("alert_severity", false))
                        .build(), Map.of("incident_created_at", SortingOrder.DESC),
                OUConfiguration.builder().build(), Set.of(), 0, 10000);
        Assertions.assertThat(listResponse.getTotalCount()).isEqualTo(2);

        dbListResponse =  pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.status)
                        .issueType("incident")
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        Assertions.assertThat(dbListResponse.getTotalCount()).isEqualTo(1);
        listResponse = pdIncidentsDbService.list(company, PagerDutyFilter.builder()
                        .issueType("incident")
                        .missingFields(Map.of("incident_status", false))
                        .build(), Map.of("incident_created_at", SortingOrder.DESC),
                OUConfiguration.builder().build(), Set.of(), 0, 10000);
        Assertions.assertThat(listResponse.getTotalCount()).isEqualTo(2);

        dbListResponse =  pdIncidentsDbService.groupByAndCalculate(company,
                PagerDutyFilter.builder()
                        .across(PagerDutyFilter.DISTINCT.status)
                        .issueType("alert")
                        .calculation(PagerDutyFilter.CALCULATION.resolution_time)
                        .build(), null, OUConfiguration.builder().build(), Map.of());
        Assertions.assertThat(dbListResponse.getTotalCount()).isEqualTo(2);
        listResponse = pdIncidentsDbService.list(company, PagerDutyFilter.builder()
                        .issueType("incident")
                        .missingFields(Map.of("alert_status", false))
                        .build(), Map.of("incident_created_at", SortingOrder.DESC),
                OUConfiguration.builder().build(), Set.of(), 0, 10000);
        Assertions.assertThat(listResponse.getTotalCount()).isEqualTo(2);

        listResponse = pdIncidentsDbService.list(company, PagerDutyFilter.builder()
                        .issueType("incident")
                        .missingFields(Map.of("alert_status", true))
                        .build(), Map.of("incident_created_at", SortingOrder.DESC),
                OUConfiguration.builder().build(), Set.of(), 0, 10000);
        Assertions.assertThat(listResponse.getTotalCount()).isEqualTo(0);


        listResponse = pdIncidentsDbService.list(company, PagerDutyFilter.builder()
                        .issueType("incident")
                        .missingFields(Map.of("incident_status", true))
                        .build(), Map.of("incident_created_at", SortingOrder.DESC),
                OUConfiguration.builder().build(), Set.of(), 0, 10000);
        Assertions.assertThat(listResponse.getTotalCount()).isEqualTo(0);
    }

    private int extractDataComponentForDbResults(Date date, int dateComponent, boolean isIntervalQuarter) {
        Calendar calendar = getPGCompatibleCalendar();
        calendar.setTime(date);
        if (isIntervalQuarter) {
            return (calendar.get(dateComponent) / 3) + 1;
        }
        return calendar.get(dateComponent);
    }

    /**
     * By definition, ISO weeks start on Mondays and the first week of a year contains January 4 of that year.
     * In other words, the first Thursday of a year is in week 1 of that year.
     * {@see https://tapoueh.org/blog/2017/06/postgresql-and-the-calendar/}
     */
    @NotNull
    private Calendar getPGCompatibleCalendar() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(4);
        return calendar;
    }
}
