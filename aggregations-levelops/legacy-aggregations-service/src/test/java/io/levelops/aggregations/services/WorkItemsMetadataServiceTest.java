package io.levelops.aggregations.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.issue_management.DbIssueStatusMetadata;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.WorkItemsMetadataService;
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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
public class WorkItemsMetadataServiceTest {

    public static final String COMPANY = "test";
    private static final int INTEGRATION_ID = 1;

    private static WorkItemsMetadataService workItemsMetadataService;
    private static DataSource dataSource;
    private static final ObjectMapper m = DefaultObjectMapper.get();

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static IntegrationService integrationService;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService = new IntegrationService(dataSource);

        workItemsMetadataService = new WorkItemsMetadataService(dataSource);
        integrationService.ensureTableExistence(COMPANY);
        integrationService.insert(COMPANY, Integration.builder()
                .application("azure_devops")
                .name("azd")
                .status("enabled")
                .build());
        workItemsMetadataService.ensureTableExistence(COMPANY);

        //read json
        String input = ResourceUtils.getResourceAsString("azuredevops/azure_devops_metadata.json");
        PaginatedResponse<EnrichedProjectData> enrichedProjectDataPaginatedResponse = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, EnrichedProjectData.class));
        List<DbIssueStatusMetadata> dbIssueStatusMetadata = new ArrayList<>();
        enrichedProjectDataPaginatedResponse.getResponse().getRecords().forEach(
                enrichedProjectData -> {
                    Project project = enrichedProjectData.getProject();
                    dbIssueStatusMetadata.addAll(DbIssueStatusMetadata.fromAzureDevopsWorkItemMetadata(
                            String.valueOf(INTEGRATION_ID), project, enrichedProjectData.getMetadata()));
                });
        dbIssueStatusMetadata.forEach(
                issueStatusMetadata -> {
                    try {
                        workItemsMetadataService.insert(COMPANY, issueStatusMetadata);
                    } catch (SQLException e) {
                        log.warn("setupAzureDevopsWorkItemsMetadata: error inserting project: " + issueStatusMetadata.getProjectId()
                                + " for project id: " + issueStatusMetadata.getProjectId(), e);
                    }
                }
        );
    }

    @Test
    public void testWorkitemsMetadata() {
        String FETCH_NUMBER_OF_ROWS = "select count(*) from " + COMPANY + ".issue_mgmt_status_metadata;";
        int countOfRows = 0;
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(FETCH_NUMBER_OF_ROWS)) {
            if (rs.next()) {
                countOfRows = rs.getInt(1);
            }
        } catch (SQLException e) {
            log.warn("testWorkitemsMetadata: error inserting metadata: " + e);
        }
        assertThat(countOfRows).isEqualTo(14);
    }
}