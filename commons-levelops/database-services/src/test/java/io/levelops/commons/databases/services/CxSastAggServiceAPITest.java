package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.checkmarx.DbCxSastProject;
import io.levelops.commons.databases.models.database.checkmarx.DbCxSastQuery;
import io.levelops.commons.databases.models.database.checkmarx.DbCxSastScan;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.CxSastIssueFilter;
import io.levelops.commons.databases.models.filters.CxSastScanFilter;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.checkmarx.models.CxSastProject;
import io.levelops.integrations.checkmarx.models.CxSastScan;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Log4j2
public class CxSastAggServiceAPITest {
    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static CxSastAggService aggService;
    private static String integrationId;
    private static Date currentTime;
    
    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;

        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        IntegrationService integrationService = new IntegrationService(dataSource);
        aggService = new CxSastAggService(dataSource);

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        integrationId = integrationService.insert(company, Integration.builder()
                .application("cxsast")
                .name("cxsast test")
                .status("enabled")
                .build());
        aggService.ensureTableExistence(company);
        currentTime = DateUtils.truncate(new Date(Instant.parse("2021-06-17T15:00:00.00Z").getEpochSecond() * 1000), Calendar.DATE);
        String projects = ResourceUtils.getResourceAsString("json/databases/cxsast_projects.json");
        PaginatedResponse<CxSastProject> cxprojects = m.readValue(projects,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, CxSastProject.class));
        cxprojects.getResponse().getRecords().forEach(project -> {
            DbCxSastProject tmp = DbCxSastProject.fromProject(project, integrationId);
            try {
                aggService.insert(company, tmp);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        String scans = ResourceUtils.getResourceAsString("json/databases/cxsast_scans.json");
        PaginatedResponse<CxSastScan> cxscans = m.readValue(scans,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, CxSastScan.class));
        cxscans.getResponse().getRecords().forEach(scan -> {
            DbCxSastScan tmp = DbCxSastScan.fromScan(scan, integrationId);
            aggService.insertScan(company, tmp);
            scan.getReport().getQueries()
                    .forEach(cxQuery -> aggService.insertQuery(company, DbCxSastQuery.fromQuery(cxQuery,
                            integrationId, currentTime), scan.getId(),
                            scan.getProject().getId()));
        });
    }

    @Test
    public void testScanFilters() throws SQLException {
        Assertions.assertThat(aggService.listScans(company,
                CxSastScanFilter.builder().build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(4);
        Assertions.assertThat(aggService.listScans(company,
                CxSastScanFilter
                        .builder()
                        .projectNames(List.of("Tic - Tac - Toe"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(2);
        Assertions.assertThat(aggService.listScans(company,
                CxSastScanFilter
                        .builder()
                        .projectNames(List.of("Tic - Tac - Toe", "smart-inspector"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(4);
        Assertions.assertThat(aggService.listScans(company,
                CxSastScanFilter
                        .builder()
                        .projectNames(List.of("Unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listScans(company,
                CxSastScanFilter
                        .builder()
                        .initiatorNames(List.of("Srinath Chandrashekhar"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(4);
        Assertions.assertThat(aggService.listScans(company,
                CxSastScanFilter
                        .builder()
                        .initiatorNames(List.of("Srinath Chandrashekhar", "Unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(4);
        Assertions.assertThat(aggService.listScans(company,
                CxSastScanFilter
                        .builder()
                        .initiatorNames(List.of("Unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listScans(company,
                CxSastScanFilter
                        .builder()
                        .integrationIds(List.of(integrationId))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(4);
        Assertions.assertThat(aggService.listScans(company,
                CxSastScanFilter
                        .builder()
                        .integrationIds(List.of(integrationId, "unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(4);
        Assertions.assertThat(aggService.listScans(company,
                CxSastScanFilter
                        .builder()
                        .integrationIds(List.of("Unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listScans(company,
                CxSastScanFilter
                        .builder()
                        .isPublic(true)
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(4);
        Assertions.assertThat(aggService.listScans(company,
                CxSastScanFilter
                        .builder()
                        .isPublic(false)
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listScans(company,
                CxSastScanFilter
                        .builder()
                        .languages(List.of("Common"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(2);
        Assertions.assertThat(aggService.listScans(company,
                CxSastScanFilter
                        .builder()
                        .languages(List.of("Common", "Groovy", "Java"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(2);
        Assertions.assertThat(aggService.listScans(company,
                CxSastScanFilter
                        .builder()
                        .languages(List.of("Java"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(2);
        Assertions.assertThat(aggService.listScans(company,
                CxSastScanFilter
                        .builder()
                        .languages(List.of("Unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listScans(company,
                CxSastScanFilter
                        .builder()
                        .owners(List.of("srinathc"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(4);
        Assertions.assertThat(aggService.listScans(company,
                CxSastScanFilter
                        .builder()
                        .owners(List.of("srinathc", "Unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(4);
        Assertions.assertThat(aggService.listScans(company,
                CxSastScanFilter
                        .builder()
                        .owners(List.of("Unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listScans(company,
                CxSastScanFilter
                        .builder()
                        .scanIds(List.of("1000000"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(aggService.listScans(company,
                CxSastScanFilter
                        .builder()
                        .scanIds(List.of("1000000", "1000003"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(2);
        Assertions.assertThat(aggService.listScans(company,
                CxSastScanFilter
                        .builder()
                        .scanIds(List.of("1000001"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listScans(company,
                CxSastScanFilter
                        .builder()
                        .scanPaths(List.of(" N/A (Zip File)"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(4);
        Assertions.assertThat(aggService.listScans(company,
                CxSastScanFilter
                        .builder()
                        .scanPaths(List.of(" N/A (Zip File)", "Unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(4);
        Assertions.assertThat(aggService.listScans(company,
                CxSastScanFilter
                        .builder()
                        .scanPaths(List.of("Unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listScans(company,
                CxSastScanFilter
                        .builder()
                        .scanTypes(List.of("Regular"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(4);
        Assertions.assertThat(aggService.listScans(company,
                CxSastScanFilter
                        .builder()
                        .scanTypes(List.of("Regular", "Full Scan"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(4);
        Assertions.assertThat(aggService.listScans(company,
                CxSastScanFilter
                        .builder()
                        .scanTypes(List.of("Unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listScans(company,
                CxSastScanFilter
                        .builder()
                        .statuses(List.of("Finished"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(4);
        Assertions.assertThat(aggService.listScans(company,
                CxSastScanFilter
                        .builder()
                        .statuses(List.of("Finished", "Ongoing"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(4);
        Assertions.assertThat(aggService.listScans(company,
                CxSastScanFilter
                        .builder()
                        .statuses(List.of("Unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
    }

    @Test
    public void testScanGroupBy() {
        Assertions.assertThat(aggService.groupByAndCalculateScan(company,
                CxSastScanFilter
                        .builder()
                        .across(CxSastScanFilter.DISTINCT.initiator_name)
                        .build(),
                null).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(aggService.groupByAndCalculateScan(company,
                CxSastScanFilter
                        .builder()
                        .across(CxSastScanFilter.DISTINCT.initiator_name)
                        .build(),
                null).getRecords().get(0).getCount()).isEqualTo(4);
        Assertions.assertThat(aggService.groupByAndCalculateScan(company,
                CxSastScanFilter
                        .builder()
                        .across(CxSastScanFilter.DISTINCT.initiator_name)
                        .build(),
                null).getRecords().get(0).getKey()).isEqualTo("Srinath Chandrashekhar");
        Assertions.assertThat(aggService.groupByAndCalculateScan(company,
                CxSastScanFilter
                        .builder()
                        .across(CxSastScanFilter.DISTINCT.scan_path)
                        .build(),
                null).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(aggService.groupByAndCalculateScan(company,
                CxSastScanFilter
                        .builder()
                        .across(CxSastScanFilter.DISTINCT.scan_path)
                        .build(),
                null).getRecords().get(0).getCount()).isEqualTo(4);
        Assertions.assertThat(aggService.groupByAndCalculateScan(company,
                CxSastScanFilter
                        .builder()
                        .across(CxSastScanFilter.DISTINCT.scan_path)
                        .build(),
                null).getRecords().get(0).getKey()).isEqualTo(" N/A (Zip File)");
        Assertions.assertThat(aggService.groupByAndCalculateScan(company,
                CxSastScanFilter
                        .builder()
                        .across(CxSastScanFilter.DISTINCT.scan_type)
                        .build(),
                null).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(aggService.groupByAndCalculateScan(company,
                CxSastScanFilter
                        .builder()
                        .across(CxSastScanFilter.DISTINCT.scan_type)
                        .build(),
                null).getRecords().get(0).getCount()).isEqualTo(4);
        Assertions.assertThat(aggService.groupByAndCalculateScan(company,
                CxSastScanFilter
                        .builder()
                        .across(CxSastScanFilter.DISTINCT.scan_type)
                        .build(),
                null).getRecords().get(0).getKey()).isEqualTo("Regular");
        Assertions.assertThat(aggService.groupByAndCalculateScan(company,
                CxSastScanFilter
                        .builder()
                        .across(CxSastScanFilter.DISTINCT.owner)
                        .build(),
                null).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(aggService.groupByAndCalculateScan(company,
                CxSastScanFilter
                        .builder()
                        .across(CxSastScanFilter.DISTINCT.owner)
                        .build(),
                null).getRecords().get(0).getCount()).isEqualTo(4);
        Assertions.assertThat(aggService.groupByAndCalculateScan(company,
                CxSastScanFilter
                        .builder()
                        .across(CxSastScanFilter.DISTINCT.owner)
                        .build(),
                null).getRecords().get(0).getKey()).isEqualTo("srinathc");
        Assertions.assertThat(aggService.groupByAndCalculateScan(company,
                CxSastScanFilter
                        .builder()
                        .across(CxSastScanFilter.DISTINCT.status)
                        .build(),
                null).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(aggService.groupByAndCalculateScan(company,
                CxSastScanFilter
                        .builder()
                        .across(CxSastScanFilter.DISTINCT.status)
                        .build(),
                null).getRecords().get(0).getCount()).isEqualTo(4);
        Assertions.assertThat(aggService.groupByAndCalculateScan(company,
                CxSastScanFilter
                        .builder()
                        .across(CxSastScanFilter.DISTINCT.status)
                        .build(),
                null).getRecords().get(0).getKey()).isEqualTo("Finished");
        Assertions.assertThat(aggService.groupByAndCalculateScan(company,
                CxSastScanFilter
                        .builder()
                        .across(CxSastScanFilter.DISTINCT.none)
                        .build(),
                null).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(aggService.groupByAndCalculateScan(company,
                CxSastScanFilter
                        .builder()
                        .across(CxSastScanFilter.DISTINCT.none)
                        .build(),
                null).getRecords().get(0).getCount()).isEqualTo(4);
        Assertions.assertThat(aggService.groupByAndCalculateScan(company,
                CxSastScanFilter
                        .builder()
                        .across(CxSastScanFilter.DISTINCT.none)
                        .build(),
                null).getRecords().get(0).getKey()).isNull();
        Assertions.assertThat(aggService.groupByAndCalculateScan(company,
                CxSastScanFilter
                        .builder()
                        .across(CxSastScanFilter.DISTINCT.language)
                        .build(),
                null).getTotalCount()).isEqualTo(4);
        Assertions.assertThat(aggService.groupByAndCalculateScan(company,
                CxSastScanFilter
                        .builder()
                        .across(CxSastScanFilter.DISTINCT.language)
                        .build(),
                null).getRecords().get(0).getCount()).isEqualTo(2);
        Assertions.assertThat(aggService.groupByAndCalculateScan(company,
                CxSastScanFilter
                        .builder()
                        .across(CxSastScanFilter.DISTINCT.language)
                        .acrossLimit(2)
                        .build(),
                null).getTotalCount()).isEqualTo(2);
        Assertions.assertThat(aggService.groupByAndCalculateScan(company,
                CxSastScanFilter
                        .builder()
                        .across(CxSastScanFilter.DISTINCT.language)
                        .acrossLimit(5)
                        .build(),
                null).getTotalCount()).isEqualTo(4);
        Assertions.assertThat(aggService.groupByAndCalculateScan(company,
                CxSastScanFilter
                        .builder()
                        .across(CxSastScanFilter.DISTINCT.language)
                        .acrossLimit(0)
                        .build(),
                null).getTotalCount()).isEqualTo(4);
        Assertions.assertThat(aggService.groupByAndCalculateScan(company,
                CxSastScanFilter
                        .builder()
                        .across(CxSastScanFilter.DISTINCT.language)
                        .acrossLimit(1)
                        .build(),
                null).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(aggService.groupByAndCalculateScan(company,
                CxSastScanFilter
                        .builder()
                        .across(CxSastScanFilter.DISTINCT.language)
                        .acrossLimit(1)
                        .aggInterval(AGG_INTERVAL.day)
                        .build(),
                null).getTotalCount()).isEqualTo(1);
    }

    @Test
    public void testIssueFilter() {
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(18);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .integrationIds(List.of(integrationId))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(18);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .integrationIds(List.of(integrationId, "unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(18);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .integrationIds(List.of("unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .assignees(List.of(""))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(18);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .assignees(List.of("", "unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(18);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .assignees(List.of("unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .categories(List.of("OWASP Top 10 2013"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(8);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .categories(List.of("OWASP Top 10 2013", "ASD STIG 4.10"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(8);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .categories(List.of("unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .scanIds(List.of("1000000"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(9);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .scanIds(List.of("1000001"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .scanIds(List.of("1000000", "1000001"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(9);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .scanIds(List.of("unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .falsePositive(true)
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .falsePositive(false)
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(18);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .files(List.of("tictactoe-master/android/app/src/main/java/com/tictactoe/MainApplication.java"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(8);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .files(List.of("tictactoe-master/android/app/src/main/java/com/tictactoe/MainApplication.java",
                                "tictactoe-master/App.js"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(10);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .files(List.of("unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .issueGroups(List.of("Java_Low_Visibility"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(8);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .issueGroups(List.of("Java_Low_Visibility", "JavaScript_ReactNative"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(10);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .issueGroups(List.of("JavaScript_ReactNative"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(2);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .issueGroups(List.of("unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .issueNames(List.of("Information_Exposure_Through_an_Error_Message"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(8);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .issueNames(List.of("Information_Exposure_Through_an_Error_Message",
                                "Missing_Root_Or_Jailbreak_Check"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(10);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .issueNames(List.of("Missing_Root_Or_Jailbreak_Check"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(2);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .issueNames(List.of("unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .languages(List.of("JavaScript"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(2);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .languages(List.of("JavaScript", "Groovy"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(10);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .languages(List.of("unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .projects(List.of("Tic - Tac - Toe"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(18);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .projects(List.of("Tic - Tac - Toe", "smart-inspector"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(18);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .projects(List.of("unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .severities(List.of("Low"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(10);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .severities(List.of("Information"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(8);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .severities(List.of("Information", "Low"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(18);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .severities(List.of("Unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .states(List.of("0"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(18);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .states(List.of("0", "1"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(18);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .states(List.of("1"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .statuses(List.of("New"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(9);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .statuses(List.of("New", "Recurrent"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(18);
        Assertions.assertThat(aggService.listIssues(company,
                CxSastIssueFilter
                        .builder()
                        .statuses(List.of("Unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
    }

    @Test
    public void testIssueGroupBy() {
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.project)
                        .build(),
                null).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.project)
                        .build(),
                null).getRecords().get(0).getCount()).isEqualTo(18);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.project)
                        .projects(List.of("Tic - Tac - Toe"))
                        .build(),
                null).getRecords().get(0).getCount()).isEqualTo(18);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.project)
                        .projects(List.of("unknown"))
                        .build(),
                null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.issue_group)
                        .projects(List.of("Tic - Tac - Toe"))
                        .build(),
                null).getTotalCount()).isEqualTo(3);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.issue_group)
                        .build(),
                null).getRecords().get(0).getCount()).isEqualTo(8);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.issue_group)
                        .issueGroups(List.of("JavaScript_ReactNative"))
                        .build(),
                null).getRecords().get(0).getCount()).isEqualTo(2);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.issue_group)
                        .projects(List.of("unknown"))
                        .build(),
                null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.issue_name)
                        .build(),
                null).getTotalCount()).isEqualTo(3);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.issue_name)
                        .build(),
                null).getRecords().get(0).getCount()).isEqualTo(8);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.issue_name)
                        .issueNames(List.of("Missing_Root_Or_Jailbreak_Check"))
                        .build(),
                null).getRecords().get(0).getCount()).isEqualTo(2);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.issue_name)
                        .issueNames(List.of("Exposure_of_Resource_to_Wrong_Sphere"))
                        .build(),
                null).getRecords().get(0).getCount()).isEqualTo(8);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.file)
                        .build(),
                null).getTotalCount()).isEqualTo(3);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.file)
                        .build(),
                null).getRecords().get(0).getCount()).isEqualTo(8);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.assignee)
                        .build(),
                null).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.assignee)
                        .build(),
                null).getRecords().get(0).getCount()).isEqualTo(18);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.language)
                        .build(),
                null).getTotalCount()).isEqualTo(3);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.language)
                        .languages(List.of("Groovy"))
                        .build(),
                null).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.language)
                        .languages(List.of("Groovy"))
                        .build(),
                null).getRecords().get(0).getCount()).isEqualTo(8);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.language)
                        .languages(List.of("Groovy", "Java"))
                        .build(),
                null).getTotalCount()).isEqualTo(2);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.language)
                        .build(),
                null).getRecords().get(0).getCount()).isEqualTo(8);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.language)
                        .severities(List.of("Low"))
                        .build(),
                null).getRecords().get(0).getCount()).isEqualTo(8);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.language)
                        .severities(List.of("Information"))
                        .build(),
                null).getRecords().get(0).getCount()).isEqualTo(8);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.language)
                        .scanIds(List.of("1000000"))
                        .build(),
                null).getRecords().get(0).getCount()).isEqualTo(4);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.language)
                        .scanIds(List.of("1000000"))
                        .build(),
                null).getTotalCount()).isEqualTo(3);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.severity)
                        .build(),
                null).getTotalCount()).isEqualTo(2);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.severity)
                        .build(),
                null).getRecords().get(0).getCount()).isEqualTo(10);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.state)
                        .build(),
                null).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.state)
                        .build(),
                null).getRecords().get(0).getCount()).isEqualTo(18);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.status)
                        .build(),
                null).getTotalCount()).isEqualTo(2);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.status)
                        .build(),
                null).getRecords().get(0).getCount()).isEqualTo(9);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.none)
                        .build(),
                null).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.none)
                        .build(),
                null).getRecords().get(0).getCount()).isEqualTo(18);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.none)
                        .build(),
                null).getRecords().get(0).getKey()).isNull();
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.trend)
                        .build(),
                null).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.trend)
                        .build(),
                null).getRecords().get(0).getCount()).isEqualTo(18);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.trend)
                        .build(),
                null).getRecords().get(0).getAdditionalKey()).isEqualTo("17-6-2021");
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.trend)
                        .aggInterval(AGG_INTERVAL.day)
                        .build(),
                null).getRecords().get(0).getAdditionalKey()).isEqualTo("17-6-2021");
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.trend)
                        .aggInterval(AGG_INTERVAL.month)
                        .build(),
                null).getRecords().get(0).getAdditionalKey()).isEqualTo("6-2021");
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.trend)
                        .aggInterval(AGG_INTERVAL.week)
                        .build(),
                null).getRecords().get(0).getAdditionalKey()).isEqualTo("24-2021");
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.trend)
                        .aggInterval(AGG_INTERVAL.quarter)
                        .build(),
                null).getRecords().get(0).getAdditionalKey()).isEqualTo("Q2-2021");
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.trend)
                        .aggInterval(AGG_INTERVAL.year)
                        .build(),
                null).getRecords().get(0).getAdditionalKey()).isEqualTo("2021");
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.language)
                        .acrossLimit(2)
                        .build(),
                null).getTotalCount()).isEqualTo(2);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.language)
                        .acrossLimit(4)
                        .build(),
                null).getTotalCount()).isEqualTo(3);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.language)
                        .acrossLimit(3)
                        .build(),
                null).getTotalCount()).isEqualTo(3);
        Assertions.assertThat(aggService.groupByAndCalculateIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.language)
                        .acrossLimit(0)
                        .build(),
                null).getTotalCount()).isEqualTo(3);
    }

    @Test
    public void testFilesFilter() {
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(18);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .integrationIds(List.of(integrationId))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(18);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .integrationIds(List.of(integrationId, "unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(18);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .integrationIds(List.of("unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .assignees(List.of(""))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(18);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .assignees(List.of("", "unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(18);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .assignees(List.of("unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .categories(List.of("OWASP Top 10 2013"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(8);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .categories(List.of("OWASP Top 10 2013", "ASD STIG 4.10"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(8);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .categories(List.of("unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .scanIds(List.of("1000000"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(9);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .scanIds(List.of("1000001"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .scanIds(List.of("1000000", "1000001"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(9);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .scanIds(List.of("unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .falsePositive(true)
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .falsePositive(false)
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(18);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .files(List.of("tictactoe-master/android/app/src/main/java/com/tictactoe/MainApplication.java"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(8);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .files(List.of("tictactoe-master/android/app/src/main/java/com/tictactoe/MainApplication.java",
                                "tictactoe-master/App.js"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(10);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .files(List.of("unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .issueGroups(List.of("Java_Low_Visibility"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(8);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .issueGroups(List.of("Java_Low_Visibility", "JavaScript_ReactNative"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(10);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .issueGroups(List.of("JavaScript_ReactNative"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(2);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .issueGroups(List.of("unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .issueNames(List.of("Information_Exposure_Through_an_Error_Message"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(8);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .issueNames(List.of("Information_Exposure_Through_an_Error_Message",
                                "Missing_Root_Or_Jailbreak_Check"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(10);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .issueNames(List.of("Missing_Root_Or_Jailbreak_Check"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(2);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .issueNames(List.of("unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .languages(List.of("JavaScript"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(2);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .languages(List.of("JavaScript", "Groovy"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(10);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .languages(List.of("unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .projects(List.of("Tic - Tac - Toe"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(18);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .projects(List.of("Tic - Tac - Toe", "smart-inspector"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(18);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .projects(List.of("unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .severities(List.of("Low"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(10);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .severities(List.of("Information"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(8);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .severities(List.of("Information", "Low"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(18);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .severities(List.of("Unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .states(List.of("0"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(18);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .states(List.of("0", "1"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(18);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .states(List.of("1"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .statuses(List.of("New"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(9);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .statuses(List.of("New", "Recurrent"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(18);
        Assertions.assertThat(aggService.listFiles(company,
                CxSastIssueFilter
                        .builder()
                        .statuses(List.of("Unknown"))
                        .build(),
                Map.of(), 0, 10000).getTotalCount()).isEqualTo(0);
    }

    @Test
    public void testIssueStacks() throws SQLException {
        Assertions.assertThat(aggService.stackedGroupByIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.language)
                        .build(),
                List.of(CxSastIssueFilter.DISTINCT.assignee), null).getTotalCount()).isEqualTo(3);
        Assertions.assertThat(aggService.stackedGroupByIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.language)
                        .build(),
                List.of(CxSastIssueFilter.DISTINCT.severity), null).getTotalCount()).isEqualTo(3);
        Assertions.assertThat(aggService.stackedGroupByIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.language)
                        .build(),
                List.of(CxSastIssueFilter.DISTINCT.state), null).getTotalCount()).isEqualTo(3);
        Assertions.assertThat(aggService.stackedGroupByIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.language)
                        .build(),
                List.of(CxSastIssueFilter.DISTINCT.status), null).getTotalCount()).isEqualTo(3);
        Assertions.assertThat(aggService.stackedGroupByIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.language)
                        .build(),
                List.of(CxSastIssueFilter.DISTINCT.file), null).getTotalCount()).isEqualTo(3);
        Assertions.assertThat(aggService.stackedGroupByIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.language)
                        .build(),
                List.of(CxSastIssueFilter.DISTINCT.issue_name), null).getTotalCount()).isEqualTo(3);
        Assertions.assertThat(aggService.stackedGroupByIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.language)
                        .build(),
                List.of(CxSastIssueFilter.DISTINCT.project), null).getTotalCount()).isEqualTo(3);
        Assertions.assertThat(aggService.stackedGroupByIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.language)
                        .build(),
                List.of(CxSastIssueFilter.DISTINCT.language), null).getTotalCount()).isEqualTo(3);
        Assertions.assertThat(aggService.stackedGroupByIssue(company,
                CxSastIssueFilter
                        .builder()
                        .across(CxSastIssueFilter.DISTINCT.language)
                        .build(),
                List.of(CxSastIssueFilter.DISTINCT.issue_group), null).getTotalCount()).isEqualTo(3);
    }
}
