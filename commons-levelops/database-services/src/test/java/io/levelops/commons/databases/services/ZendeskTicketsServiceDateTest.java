package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
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
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
public class ZendeskTicketsServiceDateTest {
    private static final String COMPANY = "test";
    private static final String INTEGRATION_ID = "1";

    private static final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static ZendeskTicketService zendeskTicketService;
    private static Date currentTime;
    private static List<DbZendeskTicket> zfTickets;

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
        zendeskTicketService.ensureTableExistence(COMPANY);
        final String input = ResourceUtils.getResourceAsString("json/databases/zendesk-tickets.json");
        PaginatedResponse<Ticket> zendeskTickets = OBJECT_MAPPER.readValue(input, OBJECT_MAPPER.getTypeFactory()
                .constructParametricType(PaginatedResponse.class, Ticket.class));
        currentTime = DateUtils.truncate(new Date(), Calendar.DATE);
        List<DbZendeskTicket> tickets = zendeskTickets.getResponse().getRecords().stream()
                .map(ticket -> DbZendeskTicket
                        .fromTicket(ticket, INTEGRATION_ID, currentTime, List.of(), List.of()))
                .collect(Collectors.toList());
        zfTickets = new ArrayList<>();
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
                    ticket.toBuilder().ingestedAt(currentTime).ticketCreatedAt(ticketCreatedAt).ticketUpdatedAt(ticketUpdatedAt).build()
                    , ticket.toBuilder().ingestedAt(currentTime).ticketCreatedAt(ticketCreatedAt).ticketUpdatedAt(ticketUpdatedAt).build()));
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

    @Test
    public void testStackedGroupByTicketIngestedAt() throws SQLException {
        List<Date> listOfInputDates = zfTickets.stream().map(DbZendeskTicket::getIngestedAt).collect(Collectors.toList());
        List<String> actualList;
        List<String> expectedList;
        expectedList = listOfInputDates.stream()
                .map(date -> extractDataComponentForDbResults(date, Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        DbListResponse<DbAggregationResult> result = zendeskTicketService.groupByAndCalculate(COMPANY,
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .DISTINCT(ZendeskTicketsFilter.DISTINCT.fromString("trend"))
                        .aggInterval(AGG_INTERVAL.year)
                        .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                        .build());
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        expectedList = listOfInputDates.stream()
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = zendeskTicketService.groupByAndCalculate(COMPANY,
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .DISTINCT(ZendeskTicketsFilter.DISTINCT.fromString("trend"))
                        .aggInterval(AGG_INTERVAL.quarter)
                        .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                        .build());
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);

        expectedList = listOfInputDates.stream()
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = zendeskTicketService.groupByAndCalculate(COMPANY,
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .DISTINCT(ZendeskTicketsFilter.DISTINCT.fromString("trend"))
                        .aggInterval(AGG_INTERVAL.month)
                        .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                        .build());
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);

        expectedList = listOfInputDates.stream()
                .map(date -> extractDataComponentForDbResults(date, Calendar.WEEK_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = zendeskTicketService.groupByAndCalculate(COMPANY,
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .DISTINCT(ZendeskTicketsFilter.DISTINCT.fromString("trend"))
                        .aggInterval(AGG_INTERVAL.week)
                        .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                        .build());
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.WEEK_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);

        expectedList = listOfInputDates.stream()
                .map(date -> extractDataComponentForDbResults(date, Calendar.DAY_OF_MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = zendeskTicketService.groupByAndCalculate(COMPANY,
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .DISTINCT(ZendeskTicketsFilter.DISTINCT.fromString("trend"))
                        .aggInterval(AGG_INTERVAL.day)
                        .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                        .build());
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.DAY_OF_MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);

    }

    @Test
    public void testStackedGroupedByTicketCreatedAt() throws SQLException {
        List<Date> listOfInputDates = zfTickets.stream().map(DbZendeskTicket::getTicketCreatedAt).collect(Collectors.toList());
        List<String> actualList;
        List<String> expectedList;
        expectedList = listOfInputDates.stream()
                .map(date -> extractDataComponentForDbResults(date, Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        DbListResponse<DbAggregationResult> result = zendeskTicketService.groupByAndCalculate(COMPANY,
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .DISTINCT(ZendeskTicketsFilter.DISTINCT.fromString("ticket_created"))
                        .aggInterval(AGG_INTERVAL.year)
                        .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                        .build());
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);

        expectedList = listOfInputDates.stream()
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = zendeskTicketService.groupByAndCalculate(COMPANY,
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .DISTINCT(ZendeskTicketsFilter.DISTINCT.fromString("ticket_created"))
                        .aggInterval(AGG_INTERVAL.quarter)
                        .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                        .build());
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);

        expectedList = listOfInputDates.stream()
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = zendeskTicketService.groupByAndCalculate(COMPANY,
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .DISTINCT(ZendeskTicketsFilter.DISTINCT.fromString("ticket_created"))
                        .aggInterval(AGG_INTERVAL.month)
                        .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                        .build());
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);

        expectedList = listOfInputDates.stream()
                .map(date -> extractDataComponentForDbResults(date, Calendar.WEEK_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = zendeskTicketService.groupByAndCalculate(COMPANY,
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .DISTINCT(ZendeskTicketsFilter.DISTINCT.fromString("ticket_created"))
                        .aggInterval(AGG_INTERVAL.week)
                        .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                        .build());
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.WEEK_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);

        expectedList = listOfInputDates.stream()
                .map(date -> extractDataComponentForDbResults(date, Calendar.DAY_OF_MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = zendeskTicketService.groupByAndCalculate(COMPANY,
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .DISTINCT(ZendeskTicketsFilter.DISTINCT.fromString("ticket_created"))
                        .aggInterval(AGG_INTERVAL.day)
                        .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                        .build());
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.DAY_OF_MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());

        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);

        result = zendeskTicketService.stackedGroupBy(COMPANY,
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .DISTINCT(ZendeskTicketsFilter.DISTINCT.fromString("ticket_created"))
                        .aggInterval(AGG_INTERVAL.day)
                        .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                        .build(), List.of(ZendeskTicketsFilter.DISTINCT.assignee));
        Assert.assertNotNull(result);
        assertThat(result.getRecords().stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).isEqualTo(List.of("_UNASSIGNED_", "srinath@cognitree.com", "harsh@levelops.io"));

    }

    @Test
    public void testStacksByMonthInterval() throws SQLException {
        DbListResponse<DbAggregationResult> ticketsGroupedByMonth = zendeskTicketService.stackedGroupBy(COMPANY,
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .DISTINCT(ZendeskTicketsFilter.DISTINCT.fromString("ticket_created"))
                        .aggInterval(AGG_INTERVAL.month)
                        .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                        .build(), List.of(ZendeskTicketsFilter.DISTINCT.brand));
        assertThat(ticketsGroupedByMonth).isNotNull();
        assertThat(ticketsGroupedByMonth.getRecords().stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).isEqualTo(List.of("Levelops"));
    }

    @Test
    public void testStackedGroupByTicketUpdatedAt() throws SQLException {
        List<Date> listOfInputDates = zfTickets.stream().map(DbZendeskTicket::getTicketUpdatedAt).collect(Collectors.toList());
        List<String> actualList;
        List<String> expectedList;
        expectedList = listOfInputDates.stream()
                .map(date -> extractDataComponentForDbResults(date, Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        DbListResponse<DbAggregationResult> result = zendeskTicketService.groupByAndCalculate(COMPANY,
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .DISTINCT(ZendeskTicketsFilter.DISTINCT.fromString("ticket_updated"))
                        .aggInterval(AGG_INTERVAL.year)
                        .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                        .build());
        Assert.assertNotNull(result);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);

        expectedList = listOfInputDates.stream()
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = zendeskTicketService.groupByAndCalculate(COMPANY,
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .DISTINCT(ZendeskTicketsFilter.DISTINCT.fromString("ticket_updated"))
                        .aggInterval(AGG_INTERVAL.quarter)
                        .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                        .build());
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        expectedList = listOfInputDates.stream()
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = zendeskTicketService.groupByAndCalculate(COMPANY,
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .DISTINCT(ZendeskTicketsFilter.DISTINCT.fromString("ticket_updated"))
                        .aggInterval(AGG_INTERVAL.month)
                        .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                        .build());
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);

        expectedList = listOfInputDates.stream()
                .map(date -> extractDataComponentForDbResults(date, Calendar.WEEK_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = zendeskTicketService.groupByAndCalculate(COMPANY,
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .DISTINCT(ZendeskTicketsFilter.DISTINCT.fromString("ticket_updated"))
                        .aggInterval(AGG_INTERVAL.week)
                        .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                        .build());
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.WEEK_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);

        expectedList = listOfInputDates.stream()
                .map(date -> extractDataComponentForDbResults(date, Calendar.DAY_OF_MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = zendeskTicketService.groupByAndCalculate(COMPANY,
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .DISTINCT(ZendeskTicketsFilter.DISTINCT.fromString("ticket_updated"))
                        .aggInterval(AGG_INTERVAL.day)
                        .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                        .build());
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.DAY_OF_MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);

    }

    @Test
    public void testStacksByQuarterInterval() throws SQLException {
        DbListResponse<DbAggregationResult> ticketsGroupedByQuarter = zendeskTicketService.stackedGroupBy(COMPANY,
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .DISTINCT(ZendeskTicketsFilter.DISTINCT.fromString("ticket_created"))
                        .aggInterval(AGG_INTERVAL.quarter)
                        .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                        .build(), List.of(ZendeskTicketsFilter.DISTINCT.status));
        assertThat(ticketsGroupedByQuarter).isNotNull();
        assertThat(ticketsGroupedByQuarter.getRecords().stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).isEqualTo(List.of("NEW", "OPEN", "CLOSED"));
    }

    @Test
    public void testStacksByYearInterval() throws SQLException {
        DbListResponse<DbAggregationResult> ticketsGroupedByYear = zendeskTicketService.stackedGroupBy(COMPANY,
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .DISTINCT(ZendeskTicketsFilter.DISTINCT.fromString("ticket_created"))
                        .aggInterval(AGG_INTERVAL.year)
                        .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                        .build(), List.of(ZendeskTicketsFilter.DISTINCT.assignee));
        assertThat(ticketsGroupedByYear).isNotNull();
        assertThat(ticketsGroupedByYear.getRecords().stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).isEqualTo(List.of("_UNASSIGNED_", "srinath@cognitree.com", "harsh@levelops.io"));
    }

    @Test
    public void testStacksByWeekInterval() throws SQLException {
        DbListResponse<DbAggregationResult> stackedGroupByBrand = zendeskTicketService.stackedGroupBy(COMPANY,
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .DISTINCT(ZendeskTicketsFilter.DISTINCT.fromString("ticket_created"))
                        .aggInterval(AGG_INTERVAL.week)
                        .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                        .build(), List.of(ZendeskTicketsFilter.DISTINCT.brand));
        assertThat(stackedGroupByBrand.getCount()).isEqualTo(1);
        assertThat(stackedGroupByBrand.getRecords().stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).isEqualTo(List.of("Levelops"));
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
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(4);
        return calendar;
    }
}
