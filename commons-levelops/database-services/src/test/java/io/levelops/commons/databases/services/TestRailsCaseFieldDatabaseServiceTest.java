package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsCaseField;
import io.levelops.commons.databases.models.filters.TestRailsCaseFieldFilter;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.testrails.models.CaseField;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRailsCaseFieldDatabaseServiceTest {
    private static final String COMPANY = "test";
    private static final String INTEGRATION_ID = "1";

    private static final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private DataSource dataSource;
    private TestRailsCaseFieldDatabaseService testRailsCaseFieldDatabaseService;
    private NamedParameterJdbcTemplate template;
    private Date currentTime;

    @Before
    public void setup() throws SQLException, IOException {
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        template = new NamedParameterJdbcTemplate(dataSource);
        testRailsCaseFieldDatabaseService = new TestRailsCaseFieldDatabaseService(dataSource);
        IntegrationService integrationService = new IntegrationService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService.ensureTableExistence(COMPANY);
        integrationService.insert(COMPANY, Integration.builder()
                .application("testrails")
                .name("testrails_test")
                .status("enabled")
                .build());
        currentTime = DateUtils.truncate(new Date(), Calendar.DATE);
        testRailsCaseFieldDatabaseService.ensureTableExistence(COMPANY);
    }

    @Test
    public void testInsert() throws SQLException, IOException{
        final String testrailsCaseFields = ResourceUtils.getResourceAsString("json/databases/testrails-case-fields.json");
        final PaginatedResponse<CaseField> caseFields = OBJECT_MAPPER.readValue(testrailsCaseFields,
                OBJECT_MAPPER.getTypeFactory().constructParametricType(PaginatedResponse.class, CaseField.class));
        final List<DbTestRailsCaseField> dbTestRailsCaseFields = caseFields.getResponse().getRecords().stream()
                .map(caseField -> DbTestRailsCaseField.fromCaseField(caseField, INTEGRATION_ID))
                .collect(Collectors.toList());
        List<String> caseFieldIds = new ArrayList<>();
        for (DbTestRailsCaseField dbTestRailsCaseField : dbTestRailsCaseFields) {
            String id = testRailsCaseFieldDatabaseService.insert(COMPANY, dbTestRailsCaseField);
            assertThat(id).isNotNull();
            caseFieldIds.add(id);
        }
        List<DbTestRailsCaseField> insertedRecords = testRailsCaseFieldDatabaseService.getByIds(COMPANY, caseFieldIds);
        assertThat(insertedRecords.size()).isEqualTo(dbTestRailsCaseFields.size());
        for (var caseField: insertedRecords){
            assertThat(caseField.getName()).isNotNull();
            assertThat(caseField.getLabel()).isNotNull();
            assertThat(caseField.getSystemName()).isNotNull();
            assertThat(caseField.getType()).isNotNull();
            assertThat(caseField.getIsActive()).isNotNull();
        }
        testRailsCaseFieldDatabaseService.cleanUpRecordsByIds(COMPANY, caseFieldIds);
    }

    @Test
    public void testListByFilter() throws IOException, SQLException {
        final String testrailsCaseFields = ResourceUtils.getResourceAsString("json/databases/testrails-case-fields.json");
        final PaginatedResponse<CaseField> caseFields = OBJECT_MAPPER.readValue(testrailsCaseFields,
                OBJECT_MAPPER.getTypeFactory().constructParametricType(PaginatedResponse.class, CaseField.class));
        final List<DbTestRailsCaseField> dbTestRailsCaseFields = caseFields.getResponse().getRecords().stream()
                .map(caseField -> DbTestRailsCaseField.fromCaseField(caseField, INTEGRATION_ID))
                .collect(Collectors.toList());
        List<String> caseFieldIds = new ArrayList<>();
        for (DbTestRailsCaseField dbTestRailsCaseField : dbTestRailsCaseFields) {
            String id = testRailsCaseFieldDatabaseService.insert(COMPANY, dbTestRailsCaseField);
            assertThat(id).isNotNull();
            caseFieldIds.add(id);
        }
        DbListResponse<DbTestRailsCaseField> caseFieldList = testRailsCaseFieldDatabaseService.listByFilter(COMPANY, TestRailsCaseFieldFilter.builder().integrationIds(List.of(INTEGRATION_ID)).ingestedAt(currentTime.toInstant().getEpochSecond()).isActive(true).needAssignedFieldsOnly(true).build(), 0,100);
        assertThat(caseFieldList.getCount()).isEqualTo(19);
    }

}
