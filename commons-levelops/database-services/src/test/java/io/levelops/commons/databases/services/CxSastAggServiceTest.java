package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.checkmarx.DbCxSastProject;
import io.levelops.commons.databases.models.database.checkmarx.DbCxSastQuery;
import io.levelops.commons.databases.models.database.checkmarx.DbCxSastScan;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.checkmarx.models.CxSastProject;
import io.levelops.integrations.checkmarx.models.CxSastScan;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class CxSastAggServiceTest {

    private static final String COMPANY = "test";
    private static final String INTEGRATION_ID = "1";

    private static final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private DataSource dataSource;
    private CxSastAggService aggService;
    private Date currentTime;
    private String queryId;

    @Before
    public void setup() throws SQLException, IOException {
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        aggService = new CxSastAggService(dataSource);
        currentTime = new Date();
        IntegrationService integrationService = new IntegrationService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService.ensureTableExistence(COMPANY);
        integrationService.insert(COMPANY, Integration.builder()
                .id(INTEGRATION_ID)
                .application("cxsast")
                .name("cxsast_test")
                .status("enabled")
                .build());
        aggService.ensureTableExistence(COMPANY);

        setupProjects();
        setupScans();
    }

    private void setupProjects() throws IOException, SQLException {
        final String cxSastProjects = ResourceUtils.getResourceAsString("json/databases/cx_sast_projects.json");
        final PaginatedResponse<CxSastProject> projects = OBJECT_MAPPER.readValue(cxSastProjects,
                OBJECT_MAPPER.getTypeFactory().constructParametricType(PaginatedResponse.class, CxSastProject.class));
        List<DbCxSastProject> dbCxSastProjects = projects.getResponse().getRecords().stream()
                .map(cxSastProject -> DbCxSastProject.fromProject(cxSastProject, INTEGRATION_ID))
                .collect(Collectors.toList());
        for (DbCxSastProject dbCxSastProject : dbCxSastProjects) {
            String id = aggService.insert(COMPANY, dbCxSastProject);
            if (StringUtils.isEmpty(id)) {
                throw new RuntimeException("The project must exist: " + dbCxSastProject);
            }
        }
    }

    private void setupScans() throws IOException {
        final String cxSastScans = ResourceUtils.getResourceAsString("json/databases/cx_sast_scans.json");
        final PaginatedResponse<CxSastScan> scans = OBJECT_MAPPER.readValue(cxSastScans,
                OBJECT_MAPPER.getTypeFactory().constructParametricType(PaginatedResponse.class, CxSastScan.class));
        scans.getResponse().getRecords().forEach(cxSastScan -> {
            DbCxSastScan dbCxSastScan = DbCxSastScan.fromScan(cxSastScan, INTEGRATION_ID);
            aggService.insertScan(COMPANY, dbCxSastScan);
            cxSastScan.getReport().getQueries()
                    .forEach(cxQuery -> queryId = aggService.insertQuery(COMPANY, DbCxSastQuery.fromQuery(cxQuery,
                            INTEGRATION_ID, currentTime), cxSastScan.getId(),
                            cxSastScan.getProject().getId()));
        });
    }

    @Test
    public void testGetIssue() {
        var issue = aggService.getIssue(COMPANY, Integer.parseInt(INTEGRATION_ID), queryId, "10000000009");
        Assertions.assertThat(issue).isNotEmpty();
        Assertions.assertThat(issue.get()).isNotNull();
    }
}
