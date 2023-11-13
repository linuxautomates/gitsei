package io.levelops.commons.databases.services;

import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.issue_management.DbWorkItemHistory;
import io.levelops.commons.databases.models.database.Integration;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class WorkItemTimelineServiceTest {

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    String company = "test";
    String integrationId = "";

    WorkItemTimelineService workItemTimelineService;
    WorkItemsService workItemsService;

    @Before
    public void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        dataSource.getConnection().prepareStatement("DROP SCHEMA IF EXISTS " + company + " CASCADE").execute();

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);

        WorkItemTestUtils.TestDbs testDbs = WorkItemTestUtils.initDbServices(dataSource, company);
        integrationId = testDbs.getIntegrationService().insert(company, Integration.builder()
                .application("awsdevtools")
                .name("awsdevtools1_test")
                .status("enabled")
                .build());

        workItemTimelineService = new WorkItemTimelineService(dataSource);
        workItemTimelineService.ensureTableExistence(company);

        workItemsService = testDbs.getWorkItemsService();
    }

    @Test
    public void testUpsert() throws SQLException {
        workItemTimelineService.upsert(company, DbWorkItemHistory.builder()
                .integrationId(integrationId)
                .workItemId("1")
                .fieldType("sprint")
                .fieldValue("s1")
                .startDate(new Timestamp(10))
                .endDate(new Timestamp(20))
                .build());
        List<DbWorkItemHistory> events = workItemTimelineService.getEvents(company, Integer.valueOf(integrationId), "sprint", "1");
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEndDate()).isEqualTo(new Timestamp(20));

        workItemTimelineService.upsert(company, DbWorkItemHistory.builder()
                .integrationId(integrationId)
                .workItemId("1")
                .fieldType("sprint")
                .fieldValue("s1")
                .startDate(new Timestamp(10))
                .endDate(new Timestamp(30))
                .build());

        events = workItemTimelineService.getEvents(company, Integer.valueOf(integrationId), "sprint", "1");
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEndDate()).isEqualTo(new Timestamp(30));
    }

    @Test
    public void listByWorkItemAndIntegrationTest() throws SQLException {
        workItemTimelineService.upsert(company, DbWorkItemHistory.builder()
                .integrationId(integrationId)
                .workItemId("1")
                .fieldType("status")
                .fieldValue("s1")
                .startDate(new Timestamp(0))
                .endDate(new Timestamp(20))
                .build());
        // check start=0
        List<DbWorkItemHistory> events = workItemTimelineService.getEvents(company, Integer.valueOf(integrationId), "status", "1");
        assertThat(events).hasSize(1);
        workItemTimelineService.upsert(company, DbWorkItemHistory.builder()
                .integrationId(integrationId)
                .workItemId("2")
                .fieldType("story_points")
                .fieldValue("s1")
                .startDate(new Timestamp(0))
                .endDate(new Timestamp(20))
                .build());
        workItemTimelineService.upsert(company, DbWorkItemHistory.builder()
                .integrationId(integrationId)
                .workItemId("3")
                .fieldType("assignee")
                .fieldValue("s1")
                .startDate(new Timestamp(0))
                .endDate(new Timestamp(20))
                .build());

        List<DbWorkItemHistory> workItemHistories = workItemTimelineService.listByFilter(company,
                List.of("1", "2","3", "4"), List.of(integrationId,integrationId,integrationId,integrationId), List.of("story_points", "assignee", "status")).getRecords();
        assertThat(workItemHistories.size()).isEqualTo(3);

        List<String> acutalfieldTypes = new ArrayList<>();
        workItemHistories.forEach(dbWorkItemHistory -> {
            acutalfieldTypes.add(dbWorkItemHistory.getFieldType());
        });
        assertThat(acutalfieldTypes).containsExactlyInAnyOrder("assignee", "status", "story_points");
    }

    @Test
    public void testUpdateZeroStartDates() throws SQLException {
        // insert with start=0
        workItemTimelineService.upsert(company, DbWorkItemHistory.builder()
                .integrationId(integrationId)
                .workItemId("1")
                .fieldType("sprint")
                .fieldValue("s1")
                .startDate(new Timestamp(0))
                .endDate(new Timestamp(20))
                .build());
        // check start=0
        List<DbWorkItemHistory> events = workItemTimelineService.getEvents(company, Integer.valueOf(integrationId), "sprint", "1");
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getStartDate()).isEqualTo(new Timestamp(0));

        // work item with createdAt=42
        workItemsService.insert(company, DbWorkItem.builder()
                .workItemId("1")
                .workItemCreatedAt(new Timestamp(42))
                .integrationId(integrationId)
                .ingestedAt(1234L)
                .build());

        // update should replace start date with 42
        workItemTimelineService.updateAllZeroStartDates(company, integrationId, 1234L);

        // check start=42
        events = workItemTimelineService.getEvents(company, Integer.valueOf(integrationId), "sprint", "1");
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getStartDate()).isEqualTo(new Timestamp(42));
    }
}