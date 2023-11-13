package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.Component;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.KvField;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.ComponentType;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class EventTypesDatabaseServiceTest {

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private EventTypesDatabaseService eventTypesService;
    private ComponentsDatabaseService componentsService;

    private String company = "test";

    @Before
    public void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        componentsService = new ComponentsDatabaseService(dataSource);
        eventTypesService = new EventTypesDatabaseService(dataSource, DefaultObjectMapper.get(), componentsService);
        new JdbcTemplate(dataSource).execute("DROP SCHEMA IF EXISTS test CASCADE; CREATE SCHEMA test;");
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        componentsService.ensureTableExistence(company);
        eventTypesService.ensureTableExistence(company);
    }

    @Test
    public void testDoubleEnsure() throws SQLException {
        eventTypesService.ensureTableExistence(company);
    }

    @Test
    public void test() throws SQLException {
        Component component = Component.builder()
                .type(ComponentType.INTEGRATION)
                .name("test")
                .build();
        String componentId = componentsService.insert(company, component);

        EventType eventType = EventType.builder()
                .type(EventType.PRAETORIAN_REPORT_CREATED.toString())
                .component(component.toBuilder().id(UUID.fromString(componentId)).build())
                .description("Praetorian Event")
                .data(Map.of("component_id", KvField.builder().key("component_id").required(true).type("text").validation("uuid").build()))
                .build();
        String id = eventTypesService.insert(company, eventType);

        var dbRecord = eventTypesService.get(company, EventType.PRAETORIAN_REPORT_CREATED.toString());
        var dbRecordUpper = eventTypesService.get(company, EventType.PRAETORIAN_REPORT_CREATED.toString().toUpperCase());
        // var dbRecordById = eventTypesService.getById(company, UUID.fromString(id));

        assertThat(dbRecord.get()).as("EventType by type should be equals to EventType by id").isEqualTo(eventType);
        assertThat(dbRecord.get()).as("EventType by type should be equals to EventType in upper cases").isEqualTo(dbRecordUpper.get());
    }

    @Test
    public void testList() throws SQLException {
        List<EventType> stream = PaginationUtils.stream(0, 1, page -> {
            try {
                return eventTypesService.list(company, page, 50).getRecords();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }).peek(e -> {
            System.out.println(" > " + e.toString());
            System.out.println("   Component type: " + e.getComponent().getType());
            System.out.println("   Component name: " + e.getComponent().getName());
            System.out.println("   Description:    " + e.getDescription());
            System.out.println("   Data:           " + e.getData());
            System.out.println();
        }).collect(Collectors.toList());

        assertThat(stream).hasSize(32);
    }
}