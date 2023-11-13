package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsCaseField;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.testrails.models.CaseField;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
public class TestRailsCustomCaseFieldConditionsBuilderTest {

    private static final String COMPANY = "test";
    private static final String INTEGRATION_ID = "1";
    private static final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();
    private TestRailsCustomCaseFieldConditionsBuilder testRailsCustomCaseFieldConditionsBuilder;
    private TestRailsCaseFieldDatabaseService caseFieldDatabaseService;
    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private DataSource dataSource;
    private Date currentTime;

    @Before
    public void setUp() throws SQLException, IOException {
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        IntegrationService integrationService = new IntegrationService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService.ensureTableExistence(COMPANY);
        integrationService.insert(COMPANY, Integration.builder()
                .application("testrails")
                .name("testrails_test")
                .status("enabled")
                .build());
        caseFieldDatabaseService = new TestRailsCaseFieldDatabaseService(dataSource);
        caseFieldDatabaseService.ensureTableExistence(COMPANY);

        currentTime = DateUtils.truncate(new Date(), Calendar.DATE);

        String caseFieldsString = ResourceUtils.getResourceAsString("json/databases/testrails-case-fields.json");
        PaginatedResponse<CaseField> caseFields = OBJECT_MAPPER.readValue(caseFieldsString,
                OBJECT_MAPPER.getTypeFactory().constructParametricType(PaginatedResponse.class, CaseField.class));
        final List<DbTestRailsCaseField> dbTestRailsCaseFields = caseFields.getResponse().getRecords().stream()
                .map(caseField -> DbTestRailsCaseField.fromCaseField(caseField, INTEGRATION_ID))
                .collect(Collectors.toList());
        dbTestRailsCaseFields.forEach(dbCaseField -> {
            try {
                caseFieldDatabaseService.insert(COMPANY, dbCaseField);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        testRailsCustomCaseFieldConditionsBuilder = new TestRailsCustomCaseFieldConditionsBuilder(caseFieldDatabaseService);
    }

    @Test
    public void testCreateCustomCaseFieldConditionsEquals() {
        Map<String, Object> params = new HashMap<>();
        Map<String, Object> customFields = new HashMap<>();
        customFields.put("custom_integer_field", List.of("1", "2"));
        customFields.put("custom_date_field", Map.of("$gt", "1680307200", "$lt", "1685577599"));
        customFields.put("custom_text_field", List.of("text"));
        customFields.put("custom_checkbox_field", "true");
        customFields.put("custom_multi_select_field", List.of("option0Esha", "option2Esha"));
        customFields.put("custom_project_admin_field", List.of("esha.shah@harness.io"));
        customFields.put("custom_milestone_field", List.of("1"));
        List<String> criteriaConditions = new ArrayList<>();
        testRailsCustomCaseFieldConditionsBuilder.createCustomCaseFieldConditions(COMPANY,
                params, "", List.of(INTEGRATION_ID), customFields, criteriaConditions, true);
        Assert.assertNotNull(criteriaConditions);
        Assert.assertTrue(criteriaConditions.size() == 8);
        Assert.assertTrue(criteriaConditions.stream().anyMatch(cond -> cond.equals("(custom_case_fields @> :custom4_intVal0::jsonb OR custom_case_fields @> :custom4_intVal0_intVal1::jsonb)")));
        Assert.assertTrue(criteriaConditions.stream().anyMatch(cond -> cond.equals("TO_DATE(custom_case_fields->>'custom_date_field', 'MM/DD/YYYY') >= to_timestamp(:custom3_start)")));
        Assert.assertTrue(criteriaConditions.stream().anyMatch(cond -> cond.equals("TO_DATE(custom_case_fields->>'custom_date_field', 'MM/DD/YYYY') <= to_timestamp(:custom3_start_end)")));
        Assert.assertTrue(criteriaConditions.stream().anyMatch(cond -> cond.equals("(custom_case_fields @> :custom5_val0::jsonb)")));
        Assert.assertTrue(criteriaConditions.stream().anyMatch(cond -> cond.equals("(custom_case_fields @> :custom6_boolean6 :: jsonb)")));
        Assert.assertTrue(criteriaConditions.stream().anyMatch(cond -> cond.equals("(custom_case_fields->'custom_multi_select_field' @> ANY(ARRAY[ :custom0_val0 ]::jsonb[]))")));
        Assert.assertTrue(criteriaConditions.stream().anyMatch(cond -> cond.equals("(custom_case_fields @> :custom1_val0::jsonb)")));
        Assert.assertTrue(criteriaConditions.stream().anyMatch(cond -> cond.equals("(custom_case_fields @> :custom2_intVal0::jsonb)")));
    }


}
