package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.azureDevops.models.WorkItemField;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class WorkItemFieldsMetaServiceTest {

    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static WorkItemFieldsMetaService workItemFieldsMetaService;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        workItemFieldsMetaService = new WorkItemFieldsMetaService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        IntegrationService integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        workItemFieldsMetaService.ensureTableExistence(company);

        integrationService.insert(company, Integration.builder()
                .application("issue_mgmt")
                .name("issue mgmt test")
                .status("enabled")
                .build());

        String input = ResourceUtils.getResourceAsString("json/databases/azure_devops_work_items_fields.json");
        List<WorkItemField> workItemFields = m.readValue(input,
                m.getTypeFactory().constructParametricType(List.class, WorkItemField.class));
        List<DbWorkItemField> dbWorkItemFields = workItemFields.stream()
                .map(workItemField -> DbWorkItemField.fromAzureDevopsWorkItemField("1", workItemField))
                .collect(Collectors.toList());
        workItemFieldsMetaService.batchUpsert(company, dbWorkItemFields);
    }

    @Test
    public void listTest() throws SQLException, IllegalArgumentException {
        List<DbWorkItemField> dbWorkItemFields = workItemFieldsMetaService.list(company, List.of("1"), true).getRecords();
        Assertions.assertNotNull(dbWorkItemFields);
        Assertions.assertNotNull(dbWorkItemFields.get(0));

        dbWorkItemFields.forEach(dbWorkItemField -> {
            assertThat(dbWorkItemField.getIntegrationId()).isEqualTo("1");
            assertThat(dbWorkItemField.getCustom()).isEqualTo(true);
        });
    }

    @Test
    public void testListByFilter() throws SQLException {
        assertThat(workItemFieldsMetaService.listByFilter(company,
                        List.of(), null, null, null, List.of(),
                        List.of(), List.of(), 0, 100)
                .getTotalCount()).isEqualTo(4);
        assertThat(workItemFieldsMetaService.listByFilter(company,
                        List.of(), null, null, null, null,
                        null, null, 0, 100)
                .getTotalCount()).isEqualTo(4);
        assertThat(workItemFieldsMetaService.listByFilter(company,
                        List.of("1"), null, null, null, null,
                        null, null, 0, 100)
                .getTotalCount()).isEqualTo(4);
        assertThat(workItemFieldsMetaService.listByFilter(company,
                        List.of("2"), null, null, null, null,
                        null, null, 0, 100)
                .getTotalCount()).isEqualTo(0);
        assertThat(workItemFieldsMetaService.listByFilter(company,
                        List.of("1"), null, "Work Item Field 1", null, null,
                        null, null, 0, 100)
                .getTotalCount()).isEqualTo(1);
        assertThat(workItemFieldsMetaService.listByFilter(company,
                        List.of("1"), null, "Field not present", null, null,
                        null, null, 0, 100)
                .getTotalCount()).isEqualTo(0);
        assertThat(workItemFieldsMetaService.listByFilter(company,
                        List.of("1"), null, null, "Work Item Field", null,
                        null, null, 0, 100)
                .getTotalCount()).isEqualTo(4);
        assertThat(workItemFieldsMetaService.listByFilter(company,
                        List.of("1"), null, null, "No Prefix", null,
                        null, null, 0, 100)
                .getTotalCount()).isEqualTo(0);
        assertThat(workItemFieldsMetaService.listByFilter(company,
                        List.of("1"), null, null, null,
                        List.of("Custom.customfield_10031", "System.systemfield_10032"),
                        null, null, 0, 100)
                .getTotalCount()).isEqualTo(2);
        assertThat(workItemFieldsMetaService.listByFilter(company,
                        List.of("1"), null, null, null,
                        List.of("customfield_10"),
                        null, null, 0, 100)
                .getTotalCount()).isEqualTo(0);
        assertThat(workItemFieldsMetaService.listByFilter(company,
                        List.of("1"), null, null, null, null,
                        List.of("date", "datetime"), null, 0, 100)
                .getTotalCount()).isEqualTo(2);
        assertThat(workItemFieldsMetaService.listByFilter(company,
                        List.of("1"), null, null, null, null,
                        List.of("This Type is not Present"), null, 0, 100)
                .getTotalCount()).isEqualTo(0);
        assertThat(workItemFieldsMetaService.listByFilter(company,
                        List.of(), true, null, null, null,
                        null, null, 0, 100)
                .getTotalCount()).isEqualTo(2);
        assertThat(workItemFieldsMetaService.listByFilter(company,
                        List.of(), false, null, null, null,
                        null, null, 0, 100)
                .getTotalCount()).isEqualTo(2);
    }
}
