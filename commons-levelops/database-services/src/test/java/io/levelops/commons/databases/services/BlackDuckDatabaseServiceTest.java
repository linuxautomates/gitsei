package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.converters.DbBlackDuckConvertors;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.blackduck.DbBlackDuckIssue;
import io.levelops.commons.databases.models.database.blackduck.DbBlackDuckProject;
import io.levelops.commons.databases.models.database.blackduck.DbBlackDuckProjectVersion;
import io.levelops.commons.databases.models.database.blackduck.DbBlackDuckVersion;
import io.levelops.commons.databases.models.filters.BlackDuckIssueFilter;
import io.levelops.commons.databases.models.filters.BlackDuckProjectFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.blackduck.BlackDuckDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.blackduck.models.EnrichedProjectData;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Log4j2
@SuppressWarnings("unused")
public class BlackDuckDatabaseServiceTest {

    private static final String COMPANY = "test";
    private static final String INTEGRATION_ID = "1";
    private static final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();
    private static String inputJsonString = null;
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static Date currentTime;
    private static UUID instanceId;
    private static BlackDuckDatabaseService service;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        IntegrationService integrationService = new IntegrationService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService.ensureTableExistence(COMPANY);
        integrationService.insert(COMPANY, Integration.builder()
                .id("1")
                .application("blackduck")
                .name("black-duck")
                .status("enabled")
                .build());
        currentTime = DateUtils.truncate(new Date(), Calendar.DATE);
        service = new BlackDuckDatabaseService(dataSource);
        service.ensureTableExistence(COMPANY);
        String resourcePath = "json/databases/blackduck.json";
        inputJsonString = ResourceUtils.getResourceAsString(resourcePath);
        PaginatedResponse<EnrichedProjectData> projects = OBJECT_MAPPER.readValue(inputJsonString, OBJECT_MAPPER.getTypeFactory()
                .constructParametricType(PaginatedResponse.class, EnrichedProjectData.class));
        String insertId = service.insert(COMPANY, DbBlackDuckConvertors.fromProject(projects.getResponse()
                .getRecords().get(0).getProject(), INTEGRATION_ID));
        String versionId = service.insertVersions(COMPANY, DbBlackDuckConvertors.fromVersion(projects.getResponse()
                .getRecords().get(0).getVersion(), insertId));
        List<String> insertIssues = projects.getResponse().getRecords().get(0).getIssues().stream().
                map(issue -> service.insertIssues(COMPANY,DbBlackDuckConvertors.fromIssue(issue, versionId)))
                .collect(Collectors.toList());

    }

    @Test
    public void test() throws IOException, SQLException {
        PaginatedResponse<EnrichedProjectData> projects = OBJECT_MAPPER.readValue(inputJsonString, OBJECT_MAPPER.getTypeFactory()
                .constructParametricType(PaginatedResponse.class, EnrichedProjectData.class));
        DbBlackDuckProject dbBlackDuckProject = DbBlackDuckConvertors.fromProject(projects.getResponse().getRecords().get(0).getProject(),
                INTEGRATION_ID);
        String projectId = service.insert(COMPANY, dbBlackDuckProject);
        assertThat(projectId).isNotNull();
        DbBlackDuckVersion dbBlackDuckVersion = DbBlackDuckConvertors.fromVersion(projects.getResponse().getRecords().get(0).getVersion(), projectId);
        String versionId = service.insertVersions(COMPANY, dbBlackDuckVersion);
        assertThat(versionId).isNotNull();
        DbBlackDuckIssue dbBlackDuckIssue = DbBlackDuckConvertors.fromIssue(projects.getResponse().getRecords().get(0).getIssues().get(0), versionId);
        String insertIssues = service.insertIssues(COMPANY, dbBlackDuckIssue);
        assertThat(insertIssues).isNotNull();
    }

    @Test
    public void testListAndGroupByProjects() {
        DbListResponse<DbBlackDuckProjectVersion> listResponse = service.listProjects(COMPANY,
                BlackDuckProjectFilter.builder().build(), 0, 100);
        assertThat(listResponse).isNotNull();
        assertThat(listResponse.getTotalCount()).isEqualTo(1);
        assertThat(listResponse.getRecords().get(0).getName()).isEqualTo("weather-vue");

        listResponse = service.listProjects(COMPANY,
                BlackDuckProjectFilter.builder().projects(List.of("weather-vue")).build(), 0, 100);
        assertThat(listResponse).isNotNull();
        assertThat(listResponse.getTotalCount()).isEqualTo(1);
        assertThat(listResponse.getRecords().get(0).getName()).isEqualTo("weather-vue");

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse =
                service.groupByAndCalculateIssues(COMPANY, BlackDuckIssueFilter.builder().across(BlackDuckIssueFilter.DISTINCT.vulnerability)
                        .calculation(BlackDuckIssueFilter.CALCULATION.overall_score).build(), null);
        assertThat(dbAggregationResultDbListResponse).isNotNull();
        assertThat(dbAggregationResultDbListResponse.getTotalCount()).isEqualTo(4);
        assertThat(dbAggregationResultDbListResponse.getRecords().get(0).getKey()).isEqualTo("CVE-2020-15133");

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse1 =
                service.groupByAndCalculateIssues(COMPANY, BlackDuckIssueFilter.builder().across(BlackDuckIssueFilter.DISTINCT.version)
                        .calculation(BlackDuckIssueFilter.CALCULATION.count).build(), null);
        assertThat(dbAggregationResultDbListResponse1).isNotNull();
        assertThat(dbAggregationResultDbListResponse1.getTotalCount()).isEqualTo(1);
        assertThat(dbAggregationResultDbListResponse1.getRecords().get(0).getKey()).isEqualTo("0.1.0");


        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse2 =
                service.groupByAndCalculateIssues(COMPANY, BlackDuckIssueFilter.builder().across(BlackDuckIssueFilter.DISTINCT.phase)
                        .calculation(BlackDuckIssueFilter.CALCULATION.count).build(), null);
        assertThat(dbAggregationResultDbListResponse2).isNotNull();
        assertThat(dbAggregationResultDbListResponse2.getTotalCount()).isEqualTo(1);
        assertThat(dbAggregationResultDbListResponse2.getRecords().get(0).getKey()).isEqualTo("DEVELOPMENT");
    }
}
