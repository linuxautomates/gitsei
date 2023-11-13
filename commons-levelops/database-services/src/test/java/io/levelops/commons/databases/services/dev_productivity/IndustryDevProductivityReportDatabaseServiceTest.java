package io.levelops.commons.databases.services.dev_productivity;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityResponse;
import io.levelops.commons.databases.models.database.dev_productivity.IndustryDevProductivityReport;
import io.levelops.commons.databases.models.database.dev_productivity.OrgDevProductivityReport;
import io.levelops.commons.databases.models.database.dev_productivity.ReportIntervalType;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class IndustryDevProductivityReportDatabaseServiceTest {
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    private static final String LEVELOPS_INVENTORY_SCHEMA ="_levelops";

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    String company = LEVELOPS_INVENTORY_SCHEMA;
    IndustryDevProductivityReportDatabaseService industryDevProductivityReportDatabaseService;


    @Before
    public void setUp() throws Exception {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        industryDevProductivityReportDatabaseService = new IndustryDevProductivityReportDatabaseService(dataSource, DefaultObjectMapper.get());

        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(LEVELOPS_INVENTORY_SCHEMA);
        industryDevProductivityReportDatabaseService.ensureTableExistence(LEVELOPS_INVENTORY_SCHEMA);

    }

    @Test
    public void test() throws SQLException {

        IndustryDevProductivityReport report1 = IndustryDevProductivityReport.builder()
                .interval(ReportIntervalType.MONTH_JAN)
                .score(10)
                .report(DevProductivityResponse.builder().orgUserId(UUID.randomUUID()).orgName(LEVELOPS_INVENTORY_SCHEMA)
                        .email("satish@levelops.io").fullName("Satish Kumar Singh").build())
                .build();

        String id2 = industryDevProductivityReportDatabaseService.insert(LEVELOPS_INVENTORY_SCHEMA, report1);
        report1 = report1.toBuilder().id(UUID.fromString(id2)).build();

        IndustryDevProductivityReport report1Read = industryDevProductivityReportDatabaseService.get(LEVELOPS_INVENTORY_SCHEMA, id2).orElse(null);
        assertThat(report1Read.getScore()).isEqualTo(10);
        IndustryDevProductivityReport report1Updated = report1.toBuilder().id(report1.getId())
                .score(20)
                .report(report1.getReport()).build();
        industryDevProductivityReportDatabaseService.update(LEVELOPS_INVENTORY_SCHEMA, report1Updated);
        IndustryDevProductivityReport report1UpdatedRead = industryDevProductivityReportDatabaseService.get(LEVELOPS_INVENTORY_SCHEMA, report1.getId().toString()).orElse(null);
        assertThat(report1UpdatedRead.getScore()).isEqualTo(20);
        assertThat(report1UpdatedRead.getReport().getEmail()).isEqualTo("satish@levelops.io");
        assertThat(industryDevProductivityReportDatabaseService.list(LEVELOPS_INVENTORY_SCHEMA, 0, 5).getRecords().size()).isGreaterThanOrEqualTo(1);
        int sizeBeforeUpsert = industryDevProductivityReportDatabaseService.list(company, 0, 1000).getRecords().size();
        //Test Upsert
        IndustryDevProductivityReport report = IndustryDevProductivityReport.builder()
                .interval(ReportIntervalType.PAST_YEAR)
                .score(10).report(DevProductivityResponse.builder().fullName("Satish Singh").email("satish@propello.ai").build())
                .build();
        String id = industryDevProductivityReportDatabaseService.upsert(company, report);
        report = report.toBuilder().id(UUID.fromString(id)).build();
        int sizeAfterUpsert1 = industryDevProductivityReportDatabaseService.list(company, 0, 1000).getRecords().size();
        Assert.assertTrue(sizeAfterUpsert1 == sizeBeforeUpsert + 1);
        verifyRecord(report, industryDevProductivityReportDatabaseService.get(company, id).get());

        //Test List By Filters
        DbListResponse<IndustryDevProductivityReport> dbListResponse = industryDevProductivityReportDatabaseService.listByFilter(company, 0, 100, List.of(UUID.fromString(id)), null, null);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(report, dbListResponse.getRecords().get(0));

        dbListResponse = industryDevProductivityReportDatabaseService.listByFilter(company, 0, 100, List.of(UUID.fromString(id2)), null, null);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(report1Updated, dbListResponse.getRecords().get(0));

        IndustryDevProductivityReport updatedReport = report.toBuilder()
                .score(20).report(DevProductivityResponse.builder().build())
                .build();
        id = industryDevProductivityReportDatabaseService.upsert(company, updatedReport);
        int sizeAfterUpsert2 = industryDevProductivityReportDatabaseService.list(company, 0, 1000).getRecords().size();
        Assert.assertTrue(sizeAfterUpsert2 == sizeAfterUpsert1);
        verifyRecord(updatedReport, industryDevProductivityReportDatabaseService.get(company, id).get());
    }

    private void testListByFiltersIntervals(List<IndustryDevProductivityReport> allExpected) throws SQLException {
        Map<ReportIntervalType, List<IndustryDevProductivityReport>> map = allExpected.stream().collect(Collectors.groupingBy(IndustryDevProductivityReport::getInterval));
        for (ReportIntervalType interval : map.keySet()) {
            List<IndustryDevProductivityReport> expected = map.get(interval);
            DbListResponse<IndustryDevProductivityReport> result = industryDevProductivityReportDatabaseService.listByFilter(company, 0, 100, null, null,null);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(result.getRecords(), expected);
        }
        List<ReportIntervalType> allIds = allExpected.stream().map(IndustryDevProductivityReport::getInterval).distinct().collect(Collectors.toList());
        DbListResponse<IndustryDevProductivityReport> result = industryDevProductivityReportDatabaseService.listByFilter(company, 0, 100, null, null, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), allExpected);
    }
    private void testListByFiltersIds(List<IndustryDevProductivityReport> allExpected) throws SQLException {
        Map<UUID, List<IndustryDevProductivityReport>> map = allExpected.stream().collect(Collectors.groupingBy(IndustryDevProductivityReport::getId));
        for (UUID id : map.keySet()) {
            List<IndustryDevProductivityReport> expected = map.get(id);
            DbListResponse<IndustryDevProductivityReport> result = industryDevProductivityReportDatabaseService.listByFilter(company, 0, 100, List.of(id), null, null);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(result.getRecords(), expected);
        }
        List<UUID> allIds = allExpected.stream().map(IndustryDevProductivityReport::getId).distinct().collect(Collectors.toList());
        DbListResponse<IndustryDevProductivityReport> result = industryDevProductivityReportDatabaseService.listByFilter(company, 0, 100, allIds, null, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), allExpected);

        result = industryDevProductivityReportDatabaseService.listByFilter(company, 0, 100, List.of(UUID.randomUUID()), null, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertTrue(CollectionUtils.isEmpty(result.getRecords()));
    }
    private void testAllListByFilters(List<IndustryDevProductivityReport> allExpected, Map<Integer, List<OrgDevProductivityReport>> ouRefIdToReportMap) throws SQLException {
        testListByFiltersIds(allExpected);
        testListByFiltersIntervals(allExpected);
    }

    private void verifyRecords(List<IndustryDevProductivityReport> a, List<IndustryDevProductivityReport> e) {
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if (CollectionUtils.isEmpty(a)) {
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<UUID, IndustryDevProductivityReport> actualMap = a.stream().collect(Collectors.toMap(IndustryDevProductivityReport::getId, x -> x));
        Map<UUID, IndustryDevProductivityReport> expectedMap = e.stream().collect(Collectors.toMap(IndustryDevProductivityReport::getId, x -> x));

        for (UUID key : actualMap.keySet()) {
            verifyRecord(actualMap.get(key), expectedMap.get(key));
        }
    }
    private void verifyRecord(IndustryDevProductivityReport expected, IndustryDevProductivityReport actual) {
        Assert.assertEquals(expected.getInterval(), actual.getInterval());
        Assert.assertEquals(expected.getScore(), actual.getScore());
        Assert.assertEquals(expected.getReport(), actual.getReport());
    }
    
    @Test
    public void testData() throws SQLException {

        String id2 = industryDevProductivityReportDatabaseService.insert(LEVELOPS_INVENTORY_SCHEMA, IndustryDevProductivityReport.builder()
                .interval(ReportIntervalType.LAST_MONTH)
                .score(10)
                .report(DevProductivityResponse.builder().orgUserId(UUID.randomUUID()).orgName(LEVELOPS_INVENTORY_SCHEMA)
                        .email("satish@levelops.io").fullName("Satish Kumar Singh").build())
                .build());
        DbListResponse<IndustryDevProductivityReport> output = industryDevProductivityReportDatabaseService.list(LEVELOPS_INVENTORY_SCHEMA, 0, 20);
        industryDevProductivityReportDatabaseService.update(LEVELOPS_INVENTORY_SCHEMA, output.getRecords().get(0));
        IndustryDevProductivityReport output2 = industryDevProductivityReportDatabaseService.get(LEVELOPS_INVENTORY_SCHEMA, id2).orElse(null);
        assertThat(output.getRecords().size()).isGreaterThanOrEqualTo(1);
        assertThat(output2.getScore()).isEqualTo(10);
        assertThat(industryDevProductivityReportDatabaseService.list(LEVELOPS_INVENTORY_SCHEMA, 0, 5).getRecords().size()).isGreaterThanOrEqualTo(1);
        industryDevProductivityReportDatabaseService.delete(LEVELOPS_INVENTORY_SCHEMA, id2);
        IndustryDevProductivityReport output3 = industryDevProductivityReportDatabaseService.get(LEVELOPS_INVENTORY_SCHEMA, id2).orElse(null);
        assertThat(output3).isEqualTo(null);
    }
}
