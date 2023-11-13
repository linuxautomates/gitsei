package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.salesforce.DbSalesforceCase;
import io.levelops.commons.databases.models.database.salesforce.DbSalesforceCaseHistory;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.SalesforceCaseFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
public class SalesforceCaseServiceDateTest {

    public static final long currentTimeInMillis = System.currentTimeMillis();
    private static final String COMPANY = "test";
    private static final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();
    private final Long ingestedAt = 1596326400L;
    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private DataSource dataSource;
    private SalesforceCaseService salesforceCaseService;
    private static final List<DbSalesforceCase> salesForceCases = new ArrayList<>();

    @Before
    public void setup() throws SQLException, IOException {
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        salesforceCaseService = new SalesforceCaseService(dataSource);
        IntegrationService integrationService = new IntegrationService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService.ensureTableExistence(COMPANY);
        integrationService.insert(COMPANY, Integration.builder()
                .application("salesforce")
                .name("salesforce_test")
                .status("enabled")
                .build());
        salesforceCaseService.ensureTableExistence(COMPANY);
        final String caseInput = ResourceUtils.getResourceAsString("json/databases/salesforce_case.json");
        final String caseHistoryInput = ResourceUtils.getResourceAsString("json/databases/salesforce_case_history.json");

        List<DbSalesforceCase> cases = OBJECT_MAPPER.readValue(caseInput, OBJECT_MAPPER.getTypeFactory()
                .constructCollectionType(List.class, DbSalesforceCase.class));
        List<DbSalesforceCaseHistory> caseHistories = OBJECT_MAPPER.readValue(caseHistoryInput, OBJECT_MAPPER.getTypeFactory()
                .constructCollectionType(List.class, DbSalesforceCaseHistory.class));

        boolean setCaseIdle = true;
        for (DbSalesforceCase sfCase : cases) {
            try {
                Date lastModifiedDate;
                if (setCaseIdle) {
                    lastModifiedDate = new Date(currentTimeInMillis - TimeUnit.DAYS.toMillis(45));
                    setCaseIdle = false;
                } else {
                    lastModifiedDate = new Date(currentTimeInMillis - TimeUnit.DAYS.toMillis(15));
                }
                sfCase = sfCase.toBuilder()
                        .ingestedAt(ingestedAt)
                        .createdAt(new Date(currentTimeInMillis - TimeUnit.DAYS.toMillis(20)))
                        .lastModifiedAt(lastModifiedDate)
                        .build();
                salesforceCaseService.insert(COMPANY, sfCase);
                salesForceCases.add(sfCase);
                if (salesforceCaseService.get(COMPANY, sfCase.getCaseId(), sfCase.getIntegrationId(), sfCase.getIngestedAt()).isEmpty()) {
                    throw new RuntimeException("The case must exist");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        caseHistories.forEach(sfCaseHistory ->
                salesforceCaseService.insertCaseHistory(COMPANY, sfCaseHistory, ingestedAt));
    }

    @Test
    public void testGroupBy() throws SQLException {
        List<Long> listOfInputDates = salesForceCases.stream().map(salesforceCase -> salesforceCase.getIngestedAt())
                .collect(Collectors.toList());
        List<String> actualList;
        List<String> expectedList;
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        DbListResponse<DbAggregationResult> result = salesforceCaseService.groupByAndCalculate(COMPANY, SalesforceCaseFilter.builder()
                .ingestedAt(ingestedAt)
                .across(SalesforceCaseFilter.DISTINCT.trend)
                .aggInterval(AGG_INTERVAL.month)
                .calculation(SalesforceCaseFilter.CALCULATION.resolution_time)
                .age(Map.ofEntries(
                        entry("$lt", "30")
                ))
                .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();

        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = salesforceCaseService.groupByAndCalculate(COMPANY, SalesforceCaseFilter.builder()
                .ingestedAt(ingestedAt)
                .across(SalesforceCaseFilter.DISTINCT.trend)
                .aggInterval(AGG_INTERVAL.year)
                .calculation(SalesforceCaseFilter.CALCULATION.resolution_time)
                .age(Map.ofEntries(
                        entry("$lt", "30")
                ))
                .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();

        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.DAY_OF_MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = salesforceCaseService.groupByAndCalculate(COMPANY, SalesforceCaseFilter.builder()
                .ingestedAt(ingestedAt)
                .across(SalesforceCaseFilter.DISTINCT.trend)
                .aggInterval(AGG_INTERVAL.day)
                .calculation(SalesforceCaseFilter.CALCULATION.resolution_time)
                .age(Map.ofEntries(
                        entry("$lt", "30")
                ))
                .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.DAY_OF_MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        Assert.assertNotNull(expectedList);
        Assert.assertNotNull(actualList);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();

        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.DAY_OF_MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = salesforceCaseService.groupByAndCalculate(COMPANY, SalesforceCaseFilter.builder()
                .ingestedAt(ingestedAt)
                .across(SalesforceCaseFilter.DISTINCT.trend)
                .calculation(SalesforceCaseFilter.CALCULATION.resolution_time)
                .age(Map.ofEntries(
                        entry("$lt", "30")
                ))
                .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.DAY_OF_MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        Assert.assertNotNull(expectedList);
        Assert.assertNotNull(actualList);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();

        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.WEEK_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = salesforceCaseService.groupByAndCalculate(COMPANY, SalesforceCaseFilter.builder()
                .ingestedAt(ingestedAt)
                .across(SalesforceCaseFilter.DISTINCT.trend)
                .aggInterval(AGG_INTERVAL.week)
                .calculation(SalesforceCaseFilter.CALCULATION.resolution_time)
                .age(Map.ofEntries(
                        entry("$lt", "30")
                ))
                .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.WEEK_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();

        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = salesforceCaseService.groupByAndCalculate(COMPANY, SalesforceCaseFilter.builder()
                .ingestedAt(ingestedAt)
                .across(SalesforceCaseFilter.DISTINCT.trend)
                .aggInterval(AGG_INTERVAL.quarter)
                .calculation(SalesforceCaseFilter.CALCULATION.resolution_time)
                .age(Map.ofEntries(
                        entry("$lt", "30")
                ))
                .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
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
