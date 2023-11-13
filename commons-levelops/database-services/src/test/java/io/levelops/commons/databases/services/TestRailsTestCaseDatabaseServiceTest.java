package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsTestCase;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.testrails.models.TestRailsTestCase;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class TestRailsTestCaseDatabaseServiceTest {

    private static final String COMPANY = "test";
    private static final String INTEGRATION_ID = "1";

    private static final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private DataSource dataSource;
    private TestRailsTestCaseDatabaseService testRailsTestCaseDatabaseService;
    private NamedParameterJdbcTemplate template;
    private Date currentTime;

    @Before
    public void setup() throws SQLException, IOException {
        MockitoAnnotations.initMocks(this);
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        template = new NamedParameterJdbcTemplate(dataSource);
        IntegrationService integrationService = new IntegrationService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService.ensureTableExistence(COMPANY);
        integrationService.insert(COMPANY, Integration.builder()
                .application("testrails")
                .name("testrails_testcase")
                .status("enabled")
                .build());
        testRailsTestCaseDatabaseService = new TestRailsTestCaseDatabaseService(DefaultObjectMapper.get(), dataSource);
        testRailsTestCaseDatabaseService.ensureTableExistence(COMPANY);
        currentTime = DateUtils.truncate(new Date(), Calendar.DATE);
    }

    @Test
    public void testInsertBatch() throws IOException, SQLException {
        final PaginatedResponse<TestRailsTestCase> testCases = ResourceUtils.getResourceAsObject("json/databases/testrails-test-cases.json",
                OBJECT_MAPPER.getTypeFactory().constructParametricType(PaginatedResponse.class, TestRailsTestCase.class));
        final List<DbTestRailsTestCase> dbTestRailsTestCases = testCases.getResponse().getRecords().stream()
                .map(testCase -> DbTestRailsTestCase.fromTestCase(testCase, INTEGRATION_ID))
                .collect(Collectors.toList());
        testRailsTestCaseDatabaseService.insertBatch(COMPANY, dbTestRailsTestCases);
        DbListResponse<DbTestRailsTestCase> dbListResponse = testRailsTestCaseDatabaseService.listByFilter(COMPANY, 0, 10, List.of(33418));
        List<DbTestRailsTestCase> dbTestRailsTestCaseList = dbListResponse.getRecords();
        Assert.assertEquals(1, dbTestRailsTestCaseList.size());
        Assert.assertEquals(Instant.ofEpochSecond(1690934400), dbTestRailsTestCaseList.get(0).getCreatedOn());
        Assert.assertEquals(Instant.ofEpochSecond(1690934400), dbTestRailsTestCaseList.get(0).getUpdatedOn());
        Assert.assertEquals("new", dbTestRailsTestCaseList.get(0).getCustomCaseFields().get("custom_string_field"));
        Assert.assertEquals("option2", dbTestRailsTestCaseList.get(0).getCustomCaseFields().get("custom_dropdown_field"));
        Assert.assertEquals(12, dbTestRailsTestCaseList.get(0).getCustomCaseFields().get("custom_integer_field"));
        Assert.assertEquals("option2", dbTestRailsTestCaseList.get(0).getCustomCaseFields().get("custom_dropdown_field"));

        // Test for onConflict for project_id & suite_id
        DbTestRailsTestCase dbTestCase = dbTestRailsTestCases.stream()
                .filter(dbTestRailsTestCase -> dbTestRailsTestCase.getCaseId() == 33418)
                .findFirst()
                .get()
                .toBuilder().projectId(2).suiteId(3).build();
        testRailsTestCaseDatabaseService.insertBatch(COMPANY, List.of(dbTestCase));
        dbListResponse = testRailsTestCaseDatabaseService.listByFilter(COMPANY, 0, 10, List.of(33418));
        dbTestRailsTestCaseList = dbListResponse.getRecords();
        Assert.assertEquals(1, dbTestRailsTestCaseList.size());
        Assert.assertEquals(2, dbTestRailsTestCaseList.get(0).getProjectId().intValue());
        Assert.assertEquals(3, dbTestRailsTestCaseList.get(0).getSuiteId().intValue());
    }
}