package io.levelops.aggregations.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.issue_management.DbWorkItemHistory;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.WorkItemTimelineService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.azureDevops.models.EnrichedProjectData;
import io.levelops.integrations.azureDevops.models.Project;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
public class WorkItemsTimelineTest {

    private static final String COMPANY = "test";
    private static final int INTEGRATION_ID = 1;

    private static WorkItemTimelineService workItemTimelineService;
    private static DataSource dataSource;
    private static final ObjectMapper m = DefaultObjectMapper.get();

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static IntegrationService integrationService;

    @BeforeClass
    public static void setupAndPopulateTable() throws IOException, SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService = new IntegrationService(dataSource);
        workItemTimelineService = new WorkItemTimelineService(dataSource);
        integrationService.ensureTableExistence(COMPANY);
        integrationService.insert(COMPANY, Integration.builder()
                .application("azure_devops")
                .name("azd")
                .status("enabled")
                .build());
        workItemTimelineService.ensureTableExistence(COMPANY);
        String input = ResourceUtils.getResourceAsString("azuredevops/azure_devops_workitems_timeline.json");
        PaginatedResponse<EnrichedProjectData> enrichedProjectDataPaginatedResponse = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, EnrichedProjectData.class));
        enrichedProjectDataPaginatedResponse.getResponse().getRecords().forEach(
                enrichedProjectData -> {
                    Project project = enrichedProjectData.getProject();
                    enrichedProjectData.getWorkItemHistories().stream()
                            .filter(workItemHistory -> workItemHistory.getFields() != null && workItemHistory.getFields().getChangedDate() != null).forEach(
                            workItemHistory -> {
                                List<DbWorkItemHistory> dbWorkItemHistories = DbWorkItemHistory
                                        .fromAzureDevopsWorkItemHistories(String.valueOf(INTEGRATION_ID), workItemHistory, new Date());
                                dbWorkItemHistories.forEach(dbWorkItemHistory -> {
                                    try {
                                        Optional<DbWorkItemHistory> lastEvent = workItemTimelineService
                                                .getLastEvent(COMPANY, INTEGRATION_ID, dbWorkItemHistory.getFieldType(),
                                                        dbWorkItemHistory.getWorkItemId());
                                        if (lastEvent.isPresent()) {
                                            String changedDate = workItemHistory.getFields().getChangedDate().getNewValue();
                                            DbWorkItemHistory lastEventUpdatedHistory = lastEvent.get().toBuilder()
                                                    .endDate(Timestamp.from(DateUtils.parseDateTime(changedDate)))
                                                    .build();

                                            workItemTimelineService.updateEndDate(COMPANY, lastEventUpdatedHistory);
                                        }
                                        workItemTimelineService.insert(COMPANY, dbWorkItemHistory);
                                    } catch (Exception ex) {
                                        log.warn("setupAzureDevopsWorkItems: error inserting project: " + workItemHistory.getId()
                                                + " for project id: " + project.getId(), ex);
                                    }
                                });
                            });
                });
    }

    @Test
    public void testWorkitemsTimelines() {
        //insert
        String FETCH_NUMBER_OF_ROWS = "select count(*) from " + COMPANY + ".issue_mgmt_workitems_timeline;";
        int countOfRows = 0;
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(FETCH_NUMBER_OF_ROWS)) {
            if (rs.next()) {
                countOfRows = rs.getInt(1);
            }
        } catch (SQLException e) {
            log.warn("testWorkitemsTimelines: error inserting workitemhistories: " + e);
        }
        assertThat(countOfRows).isEqualTo(15);
    }

    @Test
    public void checkEndDates() {
        Timestamp endDate = Timestamp.valueOf("9999-01-01 05:30:00.0");
        String FETCH_END_DATES = "select end_date from " + COMPANY + ".issue_mgmt_workitems_timeline;";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(FETCH_END_DATES)) {
            while (rs.next()) {
                System.out.println(rs.getTimestamp(1));
                Timestamp actualDate = rs.getTimestamp(1);
//                assertThat(actualDate).isEqualTo(endDate);
                rs.next();
            }
        } catch (SQLException e) {
            log.warn("checkEndDates: error inserting dates " + e);
        }
    }

    @Test
    public void checkAssignees() {
        String FETCH_END_DATES = "select field_type,field_value,start_date,end_date from " + COMPANY +
                ".issue_mgmt_workitems_timeline where field_type='assignee' order by field_value;";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(FETCH_END_DATES)) {
            DbWorkItemHistory dbWorkItemHistory = null;
            while (rs.next()) {
                dbWorkItemHistory = DbWorkItemHistory.builder()
                        .fieldType(rs.getString("field_type"))
                        .fieldValue(rs.getString("field_value"))
                        .build();
            }
            assertThat(dbWorkItemHistory.getFieldValue()).isEqualTo("xyz@gmail.com");
        } catch (SQLException e) {
            log.warn("checkEndDates: error inserting dates " + e);
        }

    }
}
