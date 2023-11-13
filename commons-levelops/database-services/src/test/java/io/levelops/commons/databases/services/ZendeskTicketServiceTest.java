package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.zendesk.DbZendeskField;
import io.levelops.commons.databases.models.database.zendesk.DbZendeskTicket;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.ZendeskTicketsFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.zendesk.models.Ticket;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
public class ZendeskTicketServiceTest {

    private static final String COMPANY = "test";
    private static final String INTEGRATION_ID = "1";

    private static final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();
    private static final String TEST_JIRA_KEY_1 = "test-jira-key-1";
    private static final String TEST_JIRA_KEY_2 = "test-jira-key-2";
    
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static ZendeskTicketService zendeskTicketService;
    private static Date currentTime;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        long currentTimeMillis = System.currentTimeMillis();
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        ZendeskFieldService zendeskFieldService = new ZendeskFieldService(dataSource);
        IntegrationService integrationService = new IntegrationService(dataSource);
        zendeskTicketService = new ZendeskTicketService(dataSource, integrationService, zendeskFieldService);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService.ensureTableExistence(COMPANY);
        integrationService.insert(COMPANY, Integration.builder()
                .application("zendesk")
                .name("zendesk_test")
                .status("enabled")
                .build());
        List<IntegrationConfig.ConfigEntry> entries = List.of(IntegrationConfig.ConfigEntry.builder()
                        .key("360041814071")
                        .name("TestSample")
                        .build(),
                IntegrationConfig.ConfigEntry.builder()
                        .key("360041814291")
                        .name("SampleTest")
                        .build(),
                IntegrationConfig.ConfigEntry.builder()
                        .key("360041840072")
                        .name("Sample")
                        .build(),
                IntegrationConfig.ConfigEntry.builder()
                        .key("360041814291")
                        .name("Test")
                        .build()
        );
        integrationService.insertConfig(COMPANY, IntegrationConfig.builder()
                .integrationId("1")
                .config(Map.of("agg_custom_fields",
                        entries))
                .build());
        zendeskTicketService.ensureTableExistence(COMPANY);
        zendeskFieldService.ensureTableExistence(COMPANY);
        zendeskFieldService.batchUpsert(COMPANY,
                List.of(DbZendeskField.builder().fieldId(360041814071L).fieldType("multiselect").integrationId("1").id("1").title("Type").build(),
                        DbZendeskField.builder().fieldId(360041814291L).fieldType("multiselect").integrationId("1").id("2").title("Status").build(),
                        DbZendeskField.builder().fieldId(360041840072L).fieldType("subject").integrationId("1").id("3").title("Subject").build(),
                        DbZendeskField.builder().fieldId(360041814291L).fieldType("subject").integrationId("1").id("4").title("Subject").build()));
        List<DbZendeskField> customFields = zendeskFieldService.listByFilter(COMPANY,
                List.of(INTEGRATION_ID),
                null,
                null,
                null,
                null,
                0,
                1000000).getRecords();
        final String input = ResourceUtils.getResourceAsString("json/databases/zendesk-tickets.json");
        PaginatedResponse<Ticket> zendeskTickets = OBJECT_MAPPER.readValue(input, OBJECT_MAPPER.getTypeFactory()
                .constructParametricType(PaginatedResponse.class, Ticket.class));
        currentTime = DateUtils.truncate(new Date(Instant.parse("2021-01-01T00:00:00-08:00").getEpochSecond()), Calendar.DATE);
        List<DbZendeskTicket> tickets = zendeskTickets.getResponse().getRecords().stream()
                .map(ticket -> DbZendeskTicket
                        .fromTicket(ticket, INTEGRATION_ID, currentTime, entries, customFields))
                .collect(Collectors.toList());
        List<DbZendeskTicket> zfTickets = new ArrayList<>();
        long milliSecondsDiff = 100;
        for (DbZendeskTicket ticket : tickets) {
            Date ticketUpdatedAt;
            Date ticketCreatedAt;
            if (ticket.getTicketId() == 51) {
                ticketUpdatedAt = new Date(currentTimeMillis - TimeUnit.DAYS.toMillis(45) - milliSecondsDiff);
            } else {
                ticketUpdatedAt = new Date(currentTimeMillis - TimeUnit.DAYS.toMillis(15) - milliSecondsDiff);
            }
            ticketCreatedAt = new Date(currentTimeMillis - TimeUnit.DAYS.toMillis(25));
            milliSecondsDiff += 100;
            zfTickets.addAll(List.of(
                    ticket.toBuilder().ingestedAt(currentTime).ticketCreatedAt(ticketCreatedAt).ticketUpdatedAt(ticketUpdatedAt).build(),
                    ticket.toBuilder().ingestedAt(randomDaysBefore(currentTime)).ticketCreatedAt(ticketCreatedAt).ticketUpdatedAt(ticketUpdatedAt).build()));
        }
        zfTickets.sort(Comparator.comparing(DbZendeskTicket::getTicketUpdatedAt));
        for (DbZendeskTicket dbZendeskTicket : zfTickets) {
            try {
                final int integrationId = NumberUtils.toInt(dbZendeskTicket.getIntegrationId());
                zendeskTicketService.insert(COMPANY, dbZendeskTicket);
                if (zendeskTicketService.get(COMPANY, dbZendeskTicket.getTicketId(), integrationId,
                        dbZendeskTicket.getIngestedAt()).isEmpty())
                    throw new RuntimeException("The ticket must exist: " + dbZendeskTicket);
            } catch (SQLException e) {
                log.error(e);
            }
        }
    }

    private static Date randomDaysBefore(Date date) {
        return Date.from(date.toInstant().minus(new Random().nextInt(7) + 1, ChronoUnit.DAYS));
    }


    @Test
    public void test() throws SQLException, JsonProcessingException {
        ZendeskTicketsFilter a = ZendeskTicketsFilter.builder()
                .ingestedAt(currentTime.toInstant().getEpochSecond())
                .DISTINCT(ZendeskTicketsFilter.DISTINCT.trend)
                .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                .build();
        ZendeskTicketsFilter b = ZendeskTicketsFilter.builder()
                .ingestedAt(currentTime.toInstant().getEpochSecond())
                .DISTINCT(ZendeskTicketsFilter.DISTINCT.organization)
                .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                .age(Map.ofEntries(
                        entry("$lt", "30")
                ))
                .build();
        assertThat(a.generateCacheHash()).isEqualTo(a.generateCacheHash());
        assertThat(b.generateCacheHash()).isEqualTo(b.generateCacheHash());
        assertThat(a.generateCacheHash()).isNotEqualTo(b.generateCacheHash());
        final int integrationId = NumberUtils.toInt(INTEGRATION_ID);
        assertThat(zendeskTicketService.groupByAndCalculate(COMPANY, ZendeskTicketsFilter.builder()
                .ingestedAt(currentTime.toInstant().getEpochSecond())
                .aggInterval(AGG_INTERVAL.day)
                .DISTINCT(ZendeskTicketsFilter.DISTINCT.trend)
                .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                .build()).getTotalCount()).isEqualTo(8);
        assertThat(zendeskTicketService.get(COMPANY, 19, integrationId, currentTime).isPresent()).isTrue();
        // test hops and bounces computation
        final DbZendeskTicket bouncedTicket = zendeskTicketService.get(COMPANY, 19, integrationId, currentTime)
                .orElse(DbZendeskTicket.builder().build());
        assertThat(bouncedTicket.getHops()).isEqualTo(5);
        assertThat(bouncedTicket.getBounces()).isEqualTo(2);
        // test sorting
        assertThat(zendeskTicketService.list(COMPANY, ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond()).build(),
                Map.of("hops", SortingOrder.DESC), 0, 100).getRecords()
                .get(0).getHops()).isEqualTo(5);
        assertThat(zendeskTicketService.list(COMPANY, ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond()).build(),
                Map.of("bounces", SortingOrder.DESC), 0, 100).getRecords()
                .get(0).getBounces()).isEqualTo(2);
        assertThat(zendeskTicketService.list(COMPANY, ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond()).build(),
                Map.of("hops", SortingOrder.ASC), 0, 100).getRecords()
                .get(0).getHops()).isEqualTo(0);
        assertThat(zendeskTicketService.list(COMPANY, ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond()).build(),
                Map.of("bounces", SortingOrder.ASC), 0, 100).getRecords()
                .get(0).getBounces()).isEqualTo(0);
        // test list
        assertThat(zendeskTicketService.list(COMPANY, ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond()).build(),
                Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(46);
        assertThat(zendeskTicketService.list(COMPANY, ZendeskTicketsFilter.builder()
                .integrationIds(List.of("2"))
                .ingestedAt(currentTime.toInstant().getEpochSecond())
                .build(), Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(0);
        assertThat(zendeskTicketService.list(COMPANY, ZendeskTicketsFilter.builder()
                .ingestedAt(currentTime.toInstant().getEpochSecond())
                .extraCriteria(List.of(ZendeskTicketsFilter.EXTRA_CRITERIA.idle))
                .build(), Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(1);
        assertThat(zendeskTicketService.list(COMPANY, ZendeskTicketsFilter.builder()
                .ingestedAt(currentTime.toInstant().getEpochSecond())
                .extraCriteria(List.of(
                        ZendeskTicketsFilter.EXTRA_CRITERIA.no_assignee,
                        ZendeskTicketsFilter.EXTRA_CRITERIA.no_due_date))
                .build(), Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(25);
        assertThat(zendeskTicketService.list(COMPANY, ZendeskTicketsFilter.builder()
                .ingestedAt(currentTime.toInstant().getEpochSecond())
                .statuses(List.of("NEW", "OPEN"))
                .build(), Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(42);
        assertThat(zendeskTicketService.list(COMPANY, ZendeskTicketsFilter.builder()
                .ingestedAt(currentTime.toInstant().getEpochSecond())
                .priorities(List.of("NORMAL", "LOW"))
                .build(), Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(2);
        assertThat(zendeskTicketService.list(COMPANY, ZendeskTicketsFilter.builder()
                .ingestedAt(currentTime.toInstant().getEpochSecond())
                .brands(List.of("TEST"))
                .build(), Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(0);

        // test aggregations
        assertThat(zendeskTicketService.groupByAndCalculate(COMPANY, ZendeskTicketsFilter.builder()
                .ingestedAt(currentTime.toInstant().getEpochSecond())
                .DISTINCT(ZendeskTicketsFilter.DISTINCT.assignee)
                .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                .build()).getTotalCount()).isEqualTo(3);
        assertThat(zendeskTicketService.groupByAndCalculate(COMPANY, ZendeskTicketsFilter.builder()
                .ingestedAt(currentTime.toInstant().getEpochSecond())
                .DISTINCT(ZendeskTicketsFilter.DISTINCT.priority)
                .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                .build()).getTotalCount()).isEqualTo(2);
        assertThat(zendeskTicketService.groupByAndCalculate(COMPANY, ZendeskTicketsFilter.builder()
                .ingestedAt(currentTime.toInstant().getEpochSecond())
                .DISTINCT(ZendeskTicketsFilter.DISTINCT.status)
                .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                .build()).getTotalCount()).isEqualTo(3);


        // test min-max
        assertThat(zendeskTicketService.groupByAndCalculate(COMPANY, ZendeskTicketsFilter.builder()
                .ingestedAt(currentTime.toInstant().getEpochSecond())
                .DISTINCT(ZendeskTicketsFilter.DISTINCT.assignee)
                .CALCULATION(ZendeskTicketsFilter.CALCULATION.bounces)
                .build()).getCount()).isEqualTo(3);
        assertThat(zendeskTicketService.groupByAndCalculate(COMPANY, ZendeskTicketsFilter.builder()
                .ingestedAt(currentTime.toInstant().getEpochSecond())
                .DISTINCT(ZendeskTicketsFilter.DISTINCT.organization)
                .CALCULATION(ZendeskTicketsFilter.CALCULATION.bounces)
                .build()).getCount()).isEqualTo(2);

        DbListResponse<DbAggregationResult> topTickets30Days = zendeskTicketService.groupByAndCalculate(COMPANY, ZendeskTicketsFilter.builder()
                .ingestedAt(currentTime.toInstant().getEpochSecond())
                .DISTINCT(ZendeskTicketsFilter.DISTINCT.organization)
                .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                .age(Map.ofEntries(
                        entry("$lt", "30")
                ))
                .build());

        assertThat(topTickets30Days.getTotalCount()).isEqualTo(2);
        Map<String, Long> prgranizationToTickets =
                topTickets30Days.getRecords().stream().collect(Collectors.toMap(DbAggregationResult::getKey, DbAggregationResult::getTotalTickets));
        assertThat(prgranizationToTickets.get("_UNASSIGNED_")).isEqualTo(44);
        assertThat(prgranizationToTickets.get("Test Org")).isEqualTo(2);

        DbListResponse<DbAggregationResult> topTickets7Days = zendeskTicketService.groupByAndCalculate(COMPANY, ZendeskTicketsFilter.builder()
                .ingestedAt(currentTime.toInstant().getEpochSecond())
                .DISTINCT(ZendeskTicketsFilter.DISTINCT.organization)
                .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                .age(Map.ofEntries(
                        entry("$lt", "7")
                ))
                .build());

        assertThat(topTickets7Days.getTotalCount()).isEqualTo(0);
        //test jira links
        final int ticketId = 2;
        final DbZendeskTicket ticket = zendeskTicketService.get(COMPANY, ticketId, integrationId, currentTime)
                .orElse(DbZendeskTicket.builder().build());
        zendeskTicketService.insert(COMPANY, ticket.toBuilder()
                .jiraIssueKeys(List.of(TEST_JIRA_KEY_1, TEST_JIRA_KEY_2))
                .build());
        assertThat(zendeskTicketService.get(COMPANY, ticketId, integrationId, currentTime)
                .orElse(DbZendeskTicket.builder().build()).getJiraIssueKeys()).containsExactlyInAnyOrder(TEST_JIRA_KEY_1, TEST_JIRA_KEY_2);
        zendeskTicketService.insert(COMPANY, ticket.toBuilder()
                .jiraIssueKeys(List.of(TEST_JIRA_KEY_1))
                .build());
        assertThat(zendeskTicketService.get(COMPANY, ticketId, integrationId, currentTime)
                .orElse(DbZendeskTicket.builder().build()).getJiraIssueKeys()).containsExactlyInAnyOrder(TEST_JIRA_KEY_1);
        zendeskTicketService.insert(COMPANY, ticket.toBuilder()
                .jiraIssueKeys(List.of(TEST_JIRA_KEY_1, TEST_JIRA_KEY_2))
                .build());
        assertThat(zendeskTicketService.get(COMPANY, ticketId, integrationId, currentTime)
                .orElse(DbZendeskTicket.builder().build()).getJiraIssueKeys()).containsExactlyInAnyOrder(TEST_JIRA_KEY_1, TEST_JIRA_KEY_2);
    }

    @Test
    public void testCustomFieldFilter() throws SQLException {
        assertThat(zendeskTicketService.list(COMPANY, ZendeskTicketsFilter.builder()
                .ingestedAt(currentTime.toInstant().getEpochSecond())
                .customFields(Map.of("360041814391", List.of("3.0013")))
                .build(), Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(0);
        DbListResponse<DbZendeskTicket> includeLists = zendeskTicketService.list(COMPANY, ZendeskTicketsFilter.builder()
                .ingestedAt(currentTime.toInstant().getEpochSecond())
                .customFields(Map.of("360041814291", List.of("3.0013")))
                .build(), Collections.emptyMap(), 0, 100);
        assertThat(includeLists.getTotalCount()).isEqualTo(3);
        assertThat(includeLists.getRecords().stream()
                .map(DbZendeskTicket::getCustomFields)
                .filter(Objects::nonNull)
                .map(customFields -> customFields.get("360041814291"))
                .collect(Collectors.toList())).isEqualTo(List.of("3.0013", "3.0013", "3.0013"));
    }

    @Test
    public void testExcludeCustomFieldFilter() throws SQLException {
        assertThat(zendeskTicketService.list(COMPANY, ZendeskTicketsFilter.builder()
                .ingestedAt(currentTime.toInstant().getEpochSecond())
                .excludeCustomFields(Map.of("36002814291", List.of("3.0013")))
                .build(), Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(46);
        DbListResponse<DbZendeskTicket> excludeLists = zendeskTicketService.list(COMPANY, ZendeskTicketsFilter.builder()
                .ingestedAt(currentTime.toInstant().getEpochSecond())
                .excludeCustomFields(Map.of("360041814291", List.of("3.0013")))
                .build(), Collections.emptyMap(), 0, 100);
        assertThat(excludeLists.getTotalCount()).isEqualTo(43);
        assertThat(excludeLists.getRecords().stream()
                .map(DbZendeskTicket::getCustomFields)
                .filter(Objects::nonNull)
                .map(customFields -> customFields.get("360041814071"))
                .collect(Collectors.toList())).contains(List.of("val1"), List.of("val3"));
    }

    @Test
    public void testCustomAcross() throws SQLException {
        assertThat(zendeskTicketService.stackedGroupBy(COMPANY, ZendeskTicketsFilter.builder()
                .ingestedAt(currentTime.toInstant().getEpochSecond())
                .integrationIds(List.of("1"))
                .DISTINCT(ZendeskTicketsFilter.DISTINCT.custom_field)
                .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                .customAcross("36004214291")
                .build(), List.of()).getCount()).isEqualTo(0);
        DbListResponse<DbAggregationResult> customAcrossResponse = zendeskTicketService.stackedGroupBy(COMPANY, ZendeskTicketsFilter.builder()
                .ingestedAt(currentTime.toInstant().getEpochSecond())
                .integrationIds(List.of("1"))
                .DISTINCT(ZendeskTicketsFilter.DISTINCT.custom_field)
                .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                .customAcross("360041814291")
                .build(), List.of());
        assertThat(customAcrossResponse.getCount()).isEqualTo(3);
        assertThat(customAcrossResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .collect(Collectors.toList()))
                .isEqualTo(List.of("3.0013", "3", "1"));
        assertThat(customAcrossResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getTotalTickets)
                .collect(Collectors.toList()))
                .isEqualTo(List.of(3L, 2L, 1L));
    }

    @Test
    public void testCustomStacks() throws SQLException {
        assertThat(zendeskTicketService.stackedGroupBy(COMPANY, ZendeskTicketsFilter.builder()
                .ingestedAt(currentTime.toInstant().getEpochSecond())
                .integrationIds(List.of("1"))
                .DISTINCT(ZendeskTicketsFilter.DISTINCT.status)
                .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                .customStacks(List.of("36004183291"))
                .build(), List.of(ZendeskTicketsFilter.DISTINCT.custom_field)).getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .size()).isEqualTo(0);
        DbListResponse<DbAggregationResult> customStacksResponse = zendeskTicketService.stackedGroupBy(COMPANY, ZendeskTicketsFilter.builder()
                .ingestedAt(currentTime.toInstant().getEpochSecond())
                .integrationIds(List.of("1"))
                .DISTINCT(ZendeskTicketsFilter.DISTINCT.status)
                .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                .customStacks(List.of("360041814291"))
                .build(), List.of(ZendeskTicketsFilter.DISTINCT.custom_field));
        assertThat(customStacksResponse.getCount()).isEqualTo(3);
        assertThat(customStacksResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).isEqualTo(List.of("NEW", "OPEN", "CLOSED"));
        assertThat(customStacksResponse
                .getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey).collect(Collectors.toList())
        ).isEqualTo(List.of("3.0013", "1", "3"));
        assertThat(customStacksResponse
                .getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getTotalTickets).collect(Collectors.toList())
        ).isEqualTo(List.of(3L, 1L, 1L));
    }

    @Test
    public void testCustomFields() throws SQLException {
        DbListResponse<DbAggregationResult> customResponse = zendeskTicketService.stackedGroupBy(COMPANY, ZendeskTicketsFilter.builder()
                .ingestedAt(currentTime.toInstant().getEpochSecond())
                .integrationIds(List.of("1"))
                .DISTINCT(ZendeskTicketsFilter.DISTINCT.custom_field)
                .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                .customAcross("360041814071")
                .excludeCustomFields(Map.of("360041814291", List.of("3.0013")))
                .customStacks(List.of("360041814291"))
                .build(), List.of(ZendeskTicketsFilter.DISTINCT.custom_field));
        assertThat(customResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .isEqualTo(List.of("val3", "val1"));
        assertThat((int) zendeskTicketService.stackedGroupBy(COMPANY, ZendeskTicketsFilter.builder()
                .ingestedAt(currentTime.toInstant().getEpochSecond())
                .integrationIds(List.of("1"))
                .DISTINCT(ZendeskTicketsFilter.DISTINCT.custom_field)
                .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                .customAcross("360041814071")
                .customFields(Map.of("360041814391", List.of("3")))
                .excludeCustomFields(Map.of("360041814291", List.of("3.0013")))
                .customStacks(List.of("360041814291"))
                .build(), List.of(ZendeskTicketsFilter.DISTINCT.custom_field))
                .getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .count()).isEqualTo(0);
        assertThat(customResponse.getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream()
                .filter(Objects::nonNull)
                .map(DbAggregationResult::getTotalTickets)
                .collect(Collectors.toList())).isEqualTo(List.of(2L));
        assertThat(customResponse
                .getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull)
                .map(DbAggregationResult::getKey)
                .collect(Collectors.toList())
        ).isEqualTo(List.of("3"));
    }
}
