package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.snyk.DbSnykIssue;
import io.levelops.commons.databases.models.database.snyk.DbSnykIssues;
import io.levelops.commons.databases.models.filters.SnykIssuesFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.snyk.SnykDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.snyk.models.SnykIssues;
import io.levelops.integrations.snyk.models.SnykVulnerability;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class SnykDatabaseServiceTest {

    private static final String COMPANY = "test";
    private static final String INTEGRATION_ID = "1";
    private static final String INTEGRATION_SERVICE_APPLICATION = "snyk";
    private static final String INTEGRATION_SERVICE_NAME = "snyk_test";
    private static final String INTEGRATION_SERVICE_STATUS = "enabled";
    private static final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();
    private static Date currentTime;

    private static final List<String> types = List.of("VULN", "LICENSE");
    private static final List<String> severities = List.of("low", "medium", "high");
    private static final List<String> languages = List.of("js");
    private static final List<String> packageManagers = List.of("npm");
    private static final List<String> exploitMaturities = List.of("no-known-exploit", "mature", "proof-of-concept");
    private static final List<String> upgradable = List.of("true", "false");
    private static final List<String> patchable = List.of("true", "false");
    private static final List<String> pinnable = List.of("true", "false");
    private static final List<String> ignored = List.of("true", "false");
    private static final List<String> patched = List.of("true", "false");


    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static DataSource dataSource;
    private static SnykDatabaseService snykDatabaseService;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        snykDatabaseService = new SnykDatabaseService(dataSource);
        IntegrationService integrationService = new IntegrationService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService.ensureTableExistence(COMPANY);
        integrationService.insert(COMPANY, Integration.builder()
                .application(INTEGRATION_SERVICE_APPLICATION)
                .name(INTEGRATION_SERVICE_NAME)
                .status(INTEGRATION_SERVICE_STATUS)
                .build());
        snykDatabaseService.ensureTableExistence(COMPANY);
        currentTime = DateUtils.truncate(new Date(), Calendar.DATE);
        // A sample of how it is ingested in GCS should come over from here
        final String issuesPath = ResourceUtils.getResourceAsString("json/databases/snyk_issues.json");
        final PaginatedResponse<SnykIssues> issues = OBJECT_MAPPER.readValue(issuesPath,
                OBJECT_MAPPER.getTypeFactory().constructParametricType(PaginatedResponse.class, SnykIssues.class));
        singleInsert(issues);
    }

    @Test
    public void testListIssues() {
        // list builds
        DbListResponse<DbSnykIssue> response = snykDatabaseService.listIssues(COMPANY,
                SnykIssuesFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .build(), 0, 100);
        assertThat(response).isNotNull();
        response.getRecords().forEach(issue -> Assertions.assertThat(issue.getIssueId()).isNotBlank());

        // list builds with a types filter
        List<String> expectedValues = types;
        response = snykDatabaseService.listIssues(COMPANY,
                SnykIssuesFilter.builder()
                        .types(types.stream()
                                .map(String::toLowerCase)
                                .collect(Collectors.toList()))
                        .build(), 0, 100);
        assertThat(response.getRecords().stream()
                .map(DbSnykIssue::getType)
                .distinct()
                .map(String::toUpperCase)
                .collect(Collectors.toList())).containsExactlyInAnyOrder(expectedValues.toArray(String[]::new));

        // list builds with a severity filter
        expectedValues = severities;
        response = snykDatabaseService.listIssues(COMPANY,
                SnykIssuesFilter.builder()
                        .severities(severities)
                        .build(), 0, 100);
        assertThat(response.getRecords().stream()
                .map(DbSnykIssue::getSeverity)
                .distinct()
                .collect(Collectors.toList())).containsExactlyInAnyOrder(expectedValues.toArray(String[]::new));

        // list builds with a language filter
        expectedValues = languages;
        response = snykDatabaseService.listIssues(COMPANY,
                SnykIssuesFilter.builder()
                        .languages(languages)
                        .build(), 0, 100);
        assertThat(response.getRecords().stream()
                .map(DbSnykIssue::getLanguage)
                .distinct()
                .collect(Collectors.toList())).containsExactlyInAnyOrder(expectedValues.toArray(String[]::new));

        // list builds with a package manager filter
        expectedValues = packageManagers;
        response = snykDatabaseService.listIssues(COMPANY,
                SnykIssuesFilter.builder()
                        .packageManagers(packageManagers)
                        .build(), 0, 100);
        assertThat(response.getRecords().stream()
                .map(DbSnykIssue::getPackageManager)
                .distinct()
                .collect(Collectors.toList())).containsExactlyInAnyOrder(expectedValues.toArray(String[]::new));

        // list builds with a exploit maturity filter
        expectedValues = exploitMaturities;
        response = snykDatabaseService.listIssues(COMPANY,
                SnykIssuesFilter.builder()
                        .exploitMaturities(exploitMaturities)
                        .build(), 0, 100);
        assertThat(response.getRecords().stream()
                .map(DbSnykIssue::getExploitMaturity)
                .distinct()
                .filter(Objects::nonNull)
                .collect(Collectors.toList())).containsExactlyInAnyOrder(expectedValues.toArray(String[]::new));

        // list builds with a upgradable filter
        expectedValues = upgradable;
        for (String value: expectedValues) {
            response = snykDatabaseService.listIssues(COMPANY,
                    SnykIssuesFilter.builder()
                            .upgradable(value)
                            .build(),
                    0, 100);
            ListUtils.emptyIfNull(response.getRecords()).forEach(record -> assertThat(
                    record.getUpgradable().equals(BooleanUtils.toBoolean(value))).isTrue());
        }

        // list builds with a patchable filter
        expectedValues = patchable;
        for (String value: expectedValues) {
            response = snykDatabaseService.listIssues(COMPANY,
                    SnykIssuesFilter.builder()
                            .patchable(value)
                            .build(),
                    0, 100);
            ListUtils.emptyIfNull(response.getRecords()).forEach(record -> assertThat(
                    record.getPatchable().equals(BooleanUtils.toBoolean(value))).isTrue());
        }

        // list builds with a ignored filter
        expectedValues = ignored;
        for (String value: expectedValues) {
            response = snykDatabaseService.listIssues(COMPANY,
                    SnykIssuesFilter.builder()
                            .ignored(value)
                            .build(),
                    0, 100);
            ListUtils.emptyIfNull(response.getRecords()).forEach(record -> assertThat(
                    record.getIgnored().equals(BooleanUtils.toBoolean(value))).isTrue());
        }

        // list builds with a pinnable filter
        expectedValues = pinnable;
        for (String value: expectedValues) {
            response = snykDatabaseService.listIssues(COMPANY,
                    SnykIssuesFilter.builder()
                            .pinnable(value)
                            .build(),
                    0, 100);
            ListUtils.emptyIfNull(response.getRecords()).forEach(record -> assertThat(
                    record.getPinnable().equals(BooleanUtils.toBoolean(value))).isTrue());
        }

        // list builds with a patched filter
        expectedValues = patched;
        for (String value: expectedValues) {
            response = snykDatabaseService.listIssues(COMPANY,
                    SnykIssuesFilter.builder()
                            .patched(value)
                            .build(),
                    0, 100);
            ListUtils.emptyIfNull(response.getRecords()).forEach(record -> assertThat(
                    record.getPatched().equals(BooleanUtils.toBoolean(value))).isTrue());
        }

        // list builds with a score range filter
        response = snykDatabaseService.listIssues(COMPANY,
                SnykIssuesFilter.builder()
                        .scoreRange(Map.of("$gt", "2", "$lt", "3"))
                        .build(),
                0, 100);
        ListUtils.emptyIfNull(response.getRecords()).forEach(record -> {
                assertThat(record.getCvssScore()).isGreaterThan(2);
                assertThat(record.getCvssScore()).isLessThan(3);
            }
        );
    }

    @Test
    public void testListPatches() {

        // list builds
        DbListResponse<SnykVulnerability.Patch> response = snykDatabaseService.listPatches(COMPANY,
                SnykIssuesFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID))
                        .build(), 0, 200);
        assertThat(response).isNotNull();
        assertThat(response.getRecords().size()).isEqualTo(21);


        // list builds with a types filter
        response = snykDatabaseService.listPatches(COMPANY,
                SnykIssuesFilter.builder()
                        .types(types.stream()
                                .map(String::toLowerCase)
                                .collect(Collectors.toList()))
                        .build(), 0, 200);
        assertThat(response).isNotNull();
        assertThat(response.getRecords().size()).isEqualTo(21);

        // list builds with a severity filter
        response = snykDatabaseService.listPatches(COMPANY,
                SnykIssuesFilter.builder()
                        .severities(severities)
                        .build(), 0, 200);
        assertThat(response).isNotNull();
        assertThat(response.getRecords().size()).isEqualTo(21);

        // list builds with a language filter
        response = snykDatabaseService.listPatches(COMPANY,
                SnykIssuesFilter.builder()
                        .languages(languages)
                        .build(), 0, 200);
        assertThat(response).isNotNull();
        assertThat(response.getRecords().size()).isEqualTo(21);

        // list builds with a package manager filter
        response = snykDatabaseService.listPatches(COMPANY,
                SnykIssuesFilter.builder()
                        .packageManagers(packageManagers)
                        .build(), 0, 200);
        assertThat(response).isNotNull();
        assertThat(response.getRecords().size()).isEqualTo(21);

        // list builds with a exploit maturity filter
        response = snykDatabaseService.listPatches(COMPANY,
                SnykIssuesFilter.builder()
                        .exploitMaturities(exploitMaturities)
                        .build(), 0, 200);
        assertThat(response).isNotNull();
        assertThat(response.getRecords().size()).isEqualTo(21);

        // list builds with a upgradable filter
        response = snykDatabaseService.listPatches(COMPANY,
                SnykIssuesFilter.builder()
                        .upgradable("true")
                        .build(), 0, 200);
        assertThat(response).isNotNull();
        assertThat(response.getRecords().size()).isEqualTo(21);

        // list builds with a patchable filter
        response = snykDatabaseService.listPatches(COMPANY,
                SnykIssuesFilter.builder()
                        .patchable("true")
                        .build(), 0, 200);
        assertThat(response).isNotNull();
        assertThat(response.getRecords().size()).isEqualTo(13);

        // list builds with a pinnable filter
        response = snykDatabaseService.listPatches(COMPANY,
                SnykIssuesFilter.builder()
                        .pinnable("false")
                        .build(), 0, 200);
        assertThat(response).isNotNull();
        assertThat(response.getRecords().size()).isEqualTo(21);

        // list builds with a ignored filter
        response = snykDatabaseService.listPatches(COMPANY,
                SnykIssuesFilter.builder()
                        .ignored("false")
                        .build(), 0, 200);
        assertThat(response).isNotNull();
        assertThat(response.getRecords().size()).isEqualTo(21);

        // list builds with a patched filter
        response = snykDatabaseService.listPatches(COMPANY,
                SnykIssuesFilter.builder()
                        .patched("false")
                        .build(), 0, 200);
        assertThat(response).isNotNull();
        assertThat(response.getRecords().size()).isEqualTo(21);
    }

    @Test
    public void testGroupByIssues() {
        // group by builds
        testGroupByIssuesUtil("total_issues", "type", List.of("vuln", "license"));
        testGroupByIssuesUtil("total_issues", "severity", List.of("low", "medium", "high"));
        testGroupByIssuesUtil("total_issues", "language", List.of("js", "ruby", "java"));
        testGroupByIssuesUtil("total_issues", "package_manager", List.of("maven", "rubygems", "npm"));
        testGroupByIssuesUtil("total_issues", "exploit_maturity", List.of("no-known-exploit",
                "proof-of-concept", "mature"));
    }

    private void testGroupByIssuesUtil(String calculation, String distinct, List<String> values) {
        DbListResponse<DbAggregationResult> response = snykDatabaseService.groupByAndCalculateIssues(COMPANY,
                SnykIssuesFilter.builder()
                        .calculation(SnykIssuesFilter.Calculation.fromString(calculation))
                        .across(SnykIssuesFilter.Distinct.fromString(distinct))
                        .build(), null);
        List<DbAggregationResult> records = response.getRecords();
        records = records.stream()
                .filter(record -> StringUtils.isNotEmpty(record.getKey()))
                .collect(Collectors.toList());
        assertThat(records.size()).isEqualTo(values.size());
        List<String> keyList = response.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList());
        for (String key: keyList)
            assertThat(keyList).contains(key);
    }

    private static void singleInsert(PaginatedResponse<SnykIssues> issues) {
        issues.getResponse().getRecords()
                .forEach(issue -> {
                    List<DbSnykIssue> dbSnykIssues = DbSnykIssue.fromIssues(issue, INTEGRATION_ID, currentTime);
                    dbSnykIssues.forEach(dbIssue -> snykDatabaseService.insert(COMPANY, dbIssue));
                });
    }

    private void batchInsert(PaginatedResponse<SnykIssues> issues) {
        final List<DbSnykIssues> dbSnykIssues = issues.getResponse().getRecords().stream()
                .map(issue -> DbSnykIssues.fromIssues(issue, INTEGRATION_ID, currentTime))
                .collect(Collectors.toList());
        // insert all the db objects
        for (DbSnykIssues snykIssues : dbSnykIssues) {
            snykDatabaseService.batchInsert(COMPANY, snykIssues);
        }
    }
}
