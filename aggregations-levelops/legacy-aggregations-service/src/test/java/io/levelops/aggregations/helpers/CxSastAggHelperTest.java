package io.levelops.aggregations.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregations.parsers.JobDtoParser;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.checkmarx.DbCxSastIssue;
import io.levelops.commons.databases.models.database.checkmarx.DbCxSastProject;
import io.levelops.commons.databases.models.database.checkmarx.DbCxSastQuery;
import io.levelops.commons.databases.models.database.checkmarx.DbCxSastScan;
import io.levelops.commons.databases.services.CxSastAggService;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.events.clients.EventsClient;
import io.levelops.events.models.EventsClientException;
import io.levelops.integrations.checkmarx.models.CxSastProject;
import io.levelops.integrations.checkmarx.models.CxSastScan;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class CxSastAggHelperTest {

    private static final String COMPANY = "test";
    private static final String INTEGRATION_ID = "1";

    private static final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private DataSource dataSource;
    private CxSastAggService aggService;
    private Date currentTime;

    @Mock
    JobDtoParser jobDtoParser;
    @Mock
    IntegrationTrackingService trackingService;
    @Mock
    EventsClient eventsClient;

    CxSastAggHelper aggHelper;

    @Before
    public void setup() throws SQLException {
        MockitoAnnotations.initMocks(this);
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        aggService = new CxSastAggService(dataSource);
        currentTime = new Date();
        aggHelper = new CxSastAggHelper(jobDtoParser, aggService, trackingService, eventsClient);
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
    }

    @Test
    public void test() throws SQLException, IOException, EventsClientException {
        setupProjects();
        setupScans();
        verify(eventsClient, times(9)).emitEvent(eq(COMPANY), eq(EventType.CHECKMARX_SAST_NEW_ISSUE), anyMap());
    }

    private void setupProjects() throws IOException, SQLException {
        final String cxSastProjects = ResourceUtils.getResourceAsString("cxsast/cx_sast_projects.json");
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
        final String cxSastScans = ResourceUtils.getResourceAsString("cxsast/cx_sast_scans.json");
        final PaginatedResponse<CxSastScan> scans = OBJECT_MAPPER.readValue(cxSastScans,
                OBJECT_MAPPER.getTypeFactory().constructParametricType(PaginatedResponse.class, CxSastScan.class));
        scans.getResponse().getRecords().forEach(cxSastScan -> {
            DbCxSastScan dbCxSastScan = DbCxSastScan.fromScan(cxSastScan, INTEGRATION_ID);
            String scanUUID = aggService.insertScan(COMPANY, dbCxSastScan);
            cxSastScan.getReport().getQueries()
                    .forEach(cxQuery -> {
                        Optional<String> query = aggService.getQuery(COMPANY, cxQuery.getId(), scanUUID,
                                Integer.parseInt(INTEGRATION_ID));
                        List<DbCxSastIssue> dbCxSastIssues = DbCxSastIssue.fromQuery(cxQuery, INTEGRATION_ID, currentTime);
                        dbCxSastIssues.forEach(dbCxSastIssue -> {
                            aggHelper.checkAndGenerateEventForIssue(COMPANY, INTEGRATION_ID, false, query, dbCxSastIssue, dbCxSastScan);
                        });
                        aggService.insertQuery(COMPANY, DbCxSastQuery.fromQuery(cxQuery,
                                INTEGRATION_ID, currentTime), cxSastScan.getId(),
                                cxSastScan.getProject().getId());
                    });
        });
    }
}
