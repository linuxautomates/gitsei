package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.zendesk.DbZendeskField;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
public class ZendeskFieldServiceTest {
    private static final String COMPANY = "test";
    private String id1;
    private String id2;

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static ZendeskFieldService zendeskFieldService;

    @Before
    public void setup() throws SQLException, IOException {
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        zendeskFieldService = new ZendeskFieldService(dataSource);
        IntegrationService integrationService = new IntegrationService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService.ensureTableExistence(COMPANY);
        id1 = integrationService.insert(COMPANY, Integration.builder()
                .application("zendesk")
                .name("zendesk_test")
                .status("enabled")
                .build());
        id2 = integrationService.insert(COMPANY, Integration.builder()
                .application("zendesk")
                .name("zendesk_test1")
                .status("enabled")
                .build());
        zendeskFieldService.ensureTableExistence(COMPANY);
        zendeskFieldService.batchUpsert(COMPANY,
                List.of(DbZendeskField.builder().fieldId(360041814071L).fieldType("multiselect").integrationId(id1).id("1").title("Type").build(),
                        DbZendeskField.builder().fieldId(360041814391L).fieldType("multiselect").integrationId(id1).id("2").title("Status").build(),
                        DbZendeskField.builder().fieldId(360041840072L).fieldType("subject").integrationId(id1).id("3").title("Subject").build(),
                        DbZendeskField.builder().fieldId(360041814291L).fieldType("integer").integrationId(id1).id("4").title("Assignee").build(),
                        DbZendeskField.builder().fieldId(360041817891L).fieldType("group").integrationId(id1).id("5").title("NewField6").build(),
                        DbZendeskField.builder().fieldId(360041814081L).fieldType("text").integrationId(id1).id("6").title("NewField4").build(),
                        DbZendeskField.builder().fieldId(360041834091L).fieldType("subject").integrationId(id2).id("7").title("subject").build(),
                        DbZendeskField.builder().fieldId(360041811091L).fieldType("group").integrationId(id2).id("8").title("NewField5").build(),
                        DbZendeskField.builder().fieldId(360041818591L).fieldType("checkbox").integrationId(id1).id("9").title("NewField3").build()));
    }

    @Test
    public void testPartialMatch() throws SQLException {
        DbListResponse<DbZendeskField> zendeskFieldList;

        // test partial match
        zendeskFieldList = zendeskFieldService.listByFilter(COMPANY,
                List.of(id1), "ty", null, null, null, 0, 100);
        assertThat(zendeskFieldList.getCount()).isEqualTo(1);
        assertThat(zendeskFieldList.getRecords().stream().map(DbZendeskField::getTitle).collect(Collectors.toList()))
                .isEqualTo(List.of("Type"));

        zendeskFieldList = zendeskFieldService.listByFilter(COMPANY,
                List.of(id1, id2), "s", null, null, null, 0, 100);
        assertThat(zendeskFieldList.getCount()).isEqualTo(3);
        assertThat(zendeskFieldList.getRecords().stream().map(DbZendeskField::getTitle).collect(Collectors.toList()))
                .isEqualTo(List.of("Status", "Subject", "subject"));

        zendeskFieldList = zendeskFieldService.listByFilter(COMPANY,
                List.of(id1), "newfield", null, null, null, 0, 100);
        assertThat(zendeskFieldList.getCount()).isEqualTo(3);
        assertThat((int) zendeskFieldList.getRecords().stream().filter(field -> field.getTitle().contains("newfield")).count()).isEqualTo(0);
        assertThat(zendeskFieldList.getRecords().stream().map(DbZendeskField::getTitle).collect(Collectors.toList()))
                .isEqualTo(List.of("NewField6", "NewField4", "NewField3"));

        zendeskFieldList = zendeskFieldService.listByFilter(COMPANY,
                List.of(id1, id2), "newField", null, null, null, 0, 100);
        assertThat(zendeskFieldList.getCount()).isEqualTo(4);
        assertThat((int) zendeskFieldList.getRecords().stream().filter(field -> field.getTitle().contains("NewField")).count()).isEqualTo(4);
        assertThat(zendeskFieldList.getRecords().stream().map(DbZendeskField::getTitle).collect(Collectors.toList()))
                .isEqualTo(List.of("NewField6", "NewField4", "NewField5", "NewField3"));

        //test exact match
        zendeskFieldList = zendeskFieldService.listByFilter(COMPANY,
                List.of(id1), null, "Assignee", null, null, 0, 100);
        assertThat(zendeskFieldList.getCount()).isEqualTo(1);
        assertThat((int) zendeskFieldList.getRecords().stream().filter(field -> field.getTitle().contains("assignee")).count()).isEqualTo(0);

        zendeskFieldList = zendeskFieldService.listByFilter(COMPANY,
                List.of(id1), null, "assignee", null, null, 0, 100);
        assertThat(zendeskFieldList.getCount()).isEqualTo(0);

        // test partial match with empty partial match field
        zendeskFieldList = zendeskFieldService.listByFilter(COMPANY,
                List.of(id1), "", null, null, null, 0, 100);
        assertThat(zendeskFieldList.getCount()).isEqualTo(7);
        assertThat((int) zendeskFieldList.getRecords().stream().map(DbZendeskField::getTitle).count()).isEqualTo(7);

        // test partial match with empty exact match field
        zendeskFieldList = zendeskFieldService.listByFilter(COMPANY,
                List.of(id2), null, "", null, null, 0, 100);
        assertThat(zendeskFieldList.getCount()).isEqualTo(2);
        assertThat((int) zendeskFieldList.getRecords().stream().map(DbZendeskField::getTitle).count()).isEqualTo(2);

    }
}
