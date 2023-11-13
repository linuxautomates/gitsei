package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.salesforce.DbSalesforceCase;
import io.levelops.commons.databases.models.database.salesforce.DbSalesforceCaseHistory;
import io.levelops.commons.databases.models.filters.SalesforceCaseFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.sql.DataSource;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

public class SalesforceCaseServiceTest {

    public static final long currentTimeInMillis = System.currentTimeMillis();
    private static final String COMPANY = "test";
    private static final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();
    private final Long ingestedAt = 1596326400L;
    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private DataSource dataSource;
    private SalesforceCaseService salesforceCaseService;

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
    public void test() throws SQLException, JsonProcessingException {
        SalesforceCaseFilter a = SalesforceCaseFilter.builder()
                .ingestedAt(ingestedAt)
                .across(SalesforceCaseFilter.DISTINCT.priority)
                .calculation(SalesforceCaseFilter.CALCULATION.case_count)
                .build();
        SalesforceCaseFilter b = SalesforceCaseFilter.builder()
                .ingestedAt(ingestedAt)
                .across(SalesforceCaseFilter.DISTINCT.account_name)
                .calculation(SalesforceCaseFilter.CALCULATION.case_count)
                .age(Map.ofEntries(
                        entry("$lt", "7")
                ))
                .build();
        assertThat(a.generateCacheHash()).isEqualTo(a.generateCacheHash());
        assertThat(b.generateCacheHash()).isEqualTo(b.generateCacheHash());
        assertThat(a.generateCacheHash()).isNotEqualTo(b.generateCacheHash());
        // test sorting
        assertThat(salesforceCaseService.list(COMPANY, SalesforceCaseFilter.builder()
                        .ingestedAt(ingestedAt).build(),
                Map.of("hops", SortingOrder.DESC), 0, 100).getRecords()
                .get(0).getHops()).isEqualTo(12);
        assertThat(salesforceCaseService.list(COMPANY, SalesforceCaseFilter.builder()
                        .ingestedAt(ingestedAt).build(),
                Map.of("bounces", SortingOrder.DESC), 0, 100).getRecords()
                .get(0).getBounces()).isEqualTo(9);
        assertThat(salesforceCaseService.list(COMPANY, SalesforceCaseFilter.builder()
                        .ingestedAt(ingestedAt).build(),
                Map.of("hops", SortingOrder.ASC), 0, 100).getRecords()
                .get(0).getHops()).isEqualTo(1);
        assertThat(salesforceCaseService.list(COMPANY, SalesforceCaseFilter.builder()
                        .ingestedAt(ingestedAt).build(),
                Map.of("bounces", SortingOrder.ASC), 0, 100).getRecords()
                .get(0).getBounces()).isEqualTo(0);

        // test list
        assertThat(salesforceCaseService.list(COMPANY, SalesforceCaseFilter.builder()
                        .ingestedAt(ingestedAt).build(),
                Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(10);
        assertThat(salesforceCaseService.list(COMPANY, SalesforceCaseFilter.builder()
                        .caseNumbers(List.of("00001010", "00001014"))
                        .ingestedAt(ingestedAt).build(),
                Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(2);
        assertThat(salesforceCaseService.list(COMPANY, SalesforceCaseFilter.builder()
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("1"))
                .build(), Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(10);
        assertThat(salesforceCaseService.list(COMPANY, SalesforceCaseFilter.builder()
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("2"))
                .build(), Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(0);
        assertThat(salesforceCaseService.list(COMPANY, SalesforceCaseFilter.builder()
                .ingestedAt(ingestedAt)
                .extraCriteria(List.of(SalesforceCaseFilter.EXTRA_CRITERIA.idle))
                .build(), Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(1);
        assertThat(salesforceCaseService.list(COMPANY, SalesforceCaseFilter.builder()
                .ingestedAt(ingestedAt)
                .extraCriteria(List.of(
                        SalesforceCaseFilter.EXTRA_CRITERIA.no_contact))
                .build(), Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(1);
        assertThat(salesforceCaseService.list(COMPANY, SalesforceCaseFilter.builder()
                .ingestedAt(ingestedAt)
                .statuses(List.of("PENDING", "ESCALATED"))
                .build(), Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(5);
        assertThat(salesforceCaseService.list(COMPANY, SalesforceCaseFilter.builder()
                .ingestedAt(ingestedAt)
                .priorities(List.of("MEDIUM", "LOW"))
                .build(), Collections.emptyMap(), 0, 100).getTotalCount()).isEqualTo(9);
        // test aggregations
        assertThat(salesforceCaseService.groupByAndCalculate(COMPANY, SalesforceCaseFilter.builder()
                .ingestedAt(ingestedAt)
                .across(SalesforceCaseFilter.DISTINCT.contact)
                .calculation(SalesforceCaseFilter.CALCULATION.case_count)
                .build(), null).getTotalCount()).isEqualTo(4);
        assertThat(salesforceCaseService.groupByAndCalculate(COMPANY, SalesforceCaseFilter.builder()
                .ingestedAt(ingestedAt)
                .across(SalesforceCaseFilter.DISTINCT.priority)
                .calculation(SalesforceCaseFilter.CALCULATION.case_count)
                .build(), null).getTotalCount()).isEqualTo(2);
        assertThat(salesforceCaseService.groupByAndCalculate(COMPANY, SalesforceCaseFilter.builder()
                .ingestedAt(ingestedAt)
                .across(SalesforceCaseFilter.DISTINCT.status)
                .calculation(SalesforceCaseFilter.CALCULATION.case_count)
                .build(), null).getTotalCount()).isEqualTo(5);
        assertThat(salesforceCaseService.groupByAndCalculate(COMPANY, SalesforceCaseFilter.builder()
                .ingestedAt(ingestedAt)
                .across(SalesforceCaseFilter.DISTINCT.contact)
                .calculation(SalesforceCaseFilter.CALCULATION.resolution_time)
                .build(), null).getTotalCount()).isEqualTo(4);
        // test min-max
        assertThat(salesforceCaseService.groupByAndCalculate(COMPANY, SalesforceCaseFilter.builder()
                .ingestedAt(ingestedAt)
                .across(SalesforceCaseFilter.DISTINCT.contact)
                .calculation(SalesforceCaseFilter.CALCULATION.bounces)
                .build(), null).getCount()).isEqualTo(4);
        assertThat(salesforceCaseService.groupByAndCalculate(COMPANY, SalesforceCaseFilter.builder()
                .ingestedAt(ingestedAt)
                .across(SalesforceCaseFilter.DISTINCT.account_name)
                .calculation(SalesforceCaseFilter.CALCULATION.bounces)
                .build(), null).getCount()).isEqualTo(4);

        DbListResponse<DbAggregationResult> topAccount30Days = salesforceCaseService.groupByAndCalculate(COMPANY, SalesforceCaseFilter.builder()
                .ingestedAt(ingestedAt)
                .across(SalesforceCaseFilter.DISTINCT.account_name)
                .calculation(SalesforceCaseFilter.CALCULATION.case_count)
                .age(Map.ofEntries(
                        entry("$lt", "30")
                ))
                .build(), null);
        assertThat(topAccount30Days.getTotalCount()).isEqualTo(4);
        Map<String, Long> accountToTotalCases =
                topAccount30Days.getRecords().stream().collect(Collectors.toMap(DbAggregationResult::getKey, DbAggregationResult::getTotalCases));
        assertThat(accountToTotalCases.get("TestAcc1")).isEqualTo(5);
        assertThat(accountToTotalCases.get("Rivendell")).isEqualTo(2);
        assertThat(accountToTotalCases.get("Shire")).isEqualTo(2);
        assertThat(accountToTotalCases.get("Gondor")).isEqualTo(1);

        DbListResponse<DbAggregationResult> topAccount7Days = salesforceCaseService.groupByAndCalculate(COMPANY, SalesforceCaseFilter.builder()
                .ingestedAt(ingestedAt)
                .across(SalesforceCaseFilter.DISTINCT.account_name)
                .calculation(SalesforceCaseFilter.CALCULATION.case_count)
                .age(Map.ofEntries(
                        entry("$lt", "7")
                ))
                .build(), null);
        assertThat(topAccount7Days.getTotalCount()).isEqualTo(0);

        DbListResponse<DbSalesforceCase> salesforceCasesList = salesforceCaseService.list(COMPANY, SalesforceCaseFilter.builder()
                .ingestedAt(ingestedAt)
                .statuses(List.of("PENDING", "ESCALATED"))
                .build(), Collections.emptyMap(), 0, 100);
        assertThat(salesforceCasesList.getTotalCount()).isEqualTo(5);

        DbListResponse<DbAggregationResult> salesforceCasesGroup = salesforceCaseService.groupByAndCalculate(COMPANY, SalesforceCaseFilter.builder()
                .ingestedAt(ingestedAt)
                .across(SalesforceCaseFilter.DISTINCT.none)
                .calculation(SalesforceCaseFilter.CALCULATION.resolution_time)
                .build(), "Custom row key");

        assertThat(salesforceCasesGroup.getTotalCount()).isEqualTo(1);

        assertThat(salesforceCasesGroup.getRecords().get(0).getKey()).isEqualTo("Custom row key");
    }
}
