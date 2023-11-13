package io.levelops.commons.service.dora;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.JiraReleaseResponse;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.dora.DoraResponseDTO;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.JiraSprintFilter;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.JiraFieldService;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.databases.services.TagItemDBService;
import io.levelops.commons.databases.services.TagsService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.services.jira.utils.JiraFilterParser;
import io.levelops.commons.databases.services.jira.conditions.JiraConditionsBuilder;
import io.levelops.commons.databases.services.jira.conditions.JiraCustomFieldConditionsBuilder;
import io.levelops.commons.databases.services.jira.conditions.JiraFieldConditionsBuilder;
import io.levelops.commons.databases.services.jira.conditions.JiraPartialMatchConditionsBuilder;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.web.exceptions.BadRequestException;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Log4j2
public class JiraDoraServiceTest {

    private static final String company = "test";
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static JiraDoraService jiraDoraService;
    private static JiraConditionsBuilder jiraConditionsBuilder;

    private static JiraIssueService jiraIssueService;

    private static JiraFieldConditionsBuilder jiraFieldConditionsBuilder;
    private static JiraFieldService jiraFieldService;
    private static JiraCustomFieldConditionsBuilder jiraCustomFieldConditionsBuilder;
    private static IntegrationService integrationService;
    private static JiraPartialMatchConditionsBuilder jiraPartialMatchConditionsBuilder;
    private static JiraFilterParser jiraFilterParser;
    private static IntegrationTrackingService integrationTrackingService;
    private static TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;
    private static NamedParameterJdbcTemplate template;

    @Before
    public void setup() throws SQLException, IOException {

        DataSource dataSource = DatabaseTestUtils.setUpDataSource(pg, company);
        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, company);

        // jira issue service
        jiraIssueService = jiraTestDbs.getJiraIssueService();
        jiraIssueService.ensureTableExistence(company);

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        new TagsService(dataSource).ensureTableExistence(company);
        new TagItemDBService(dataSource).ensureTableExistence(company);

        // integration service
        integrationService = jiraTestDbs.getIntegrationService();
        integrationService.ensureTableExistence(company);
        integrationService.insert(company, Integration.builder()
                .application("jira")
                .name("jira test")
                .status("enabled")
                .build());
        integrationService.insertConfig(company, IntegrationConfig.builder()
                .integrationId("1")
                .config(Map.of("agg_custom_fields",
                        List.of(IntegrationConfig.ConfigEntry.builder()
                                .key("customfield_20001")
                                .name("hello")
                                .delimiter(",")
                                .build())))
                .build());

        // jira dora service
        jiraConditionsBuilder = jiraTestDbs.getJiraConditionsBuilder();
        jiraDoraService = new JiraDoraService(dataSource, jiraConditionsBuilder);

        // jira issues insertion
        String issues = ResourceUtils.getResourceAsString("velocity/jira_issues.json");
        PaginatedResponse<DbJiraIssue> jiraIssues = mapper.readValue(issues,
                mapper.getTypeFactory().constructParametricType(PaginatedResponse.class, DbJiraIssue.class));

        jiraIssues.getResponse().getRecords().forEach(issue -> {
            try {
                jiraIssueService.insert(company, issue);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        // jira filter parser
        integrationTrackingService = new IntegrationTrackingService(dataSource);
        integrationTrackingService.ensureTableExistence(company);
        ticketCategorizationSchemeDatabaseService = new TicketCategorizationSchemeDatabaseService(dataSource, mapper);
        ticketCategorizationSchemeDatabaseService.ensureTableExistence(company);
        jiraFilterParser = new JiraFilterParser(jiraFieldConditionsBuilder, integrationService,
                integrationTrackingService, ticketCategorizationSchemeDatabaseService);
    }

    @Test
    public void testGetTimeSeriesDataForDeployment() throws SQLException, BadRequestException, IOException {

        String workflowProfile = ResourceUtils.getResourceAsString("velocity/new_workflow_profile_im.json");
        VelocityConfigDTO velocityConfigDTO = mapper.readValue(workflowProfile, VelocityConfigDTO.class);

        JiraIssuesFilter velocityFilters = jiraFilterParser.createFilter(company, DefaultListRequest.builder()
                        .filter(velocityConfigDTO.getDeploymentFrequency()
                                .getVelocityConfigFilters().getDeploymentFrequency().getFilter())
                        .build(),
                JiraIssuesFilter.CALCULATION.ticket_count, JiraIssuesFilter.DISTINCT.none,null,
                null, false, false);

        // 15th Step - 10th Nov
        JiraIssuesFilter requestFilters = JiraIssuesFilter.builder()
                .issueResolutionRange(ImmutablePair.of(1663064804L, 1669373499L))
                .build();

        DoraResponseDTO doraResponseDTO = jiraDoraService.getTimeSeriesDataForDeployment(
                company, velocityFilters, requestFilters, velocityConfigDTO.getDeploymentFrequency().getCalculationField().toString(), null
        );

        Assert.assertEquals(74, doraResponseDTO.getTimeSeries().getDay().size());
        Assert.assertEquals(11, doraResponseDTO.getTimeSeries().getWeek().size());
        Assert.assertEquals(3, doraResponseDTO.getTimeSeries().getMonth().size());
    }

    @Test
    public void testGetCountForDeployment() throws IOException, SQLException, BadRequestException {

        String workflowProfile = ResourceUtils.getResourceAsString("velocity/new_workflow_profile_im.json");
        VelocityConfigDTO velocityConfigDTO = mapper.readValue(workflowProfile, VelocityConfigDTO.class);

        JiraIssuesFilter velocityFilters = jiraFilterParser.createFilter(company, DefaultListRequest.builder()
                        .filter(velocityConfigDTO.getDeploymentFrequency()
                                .getVelocityConfigFilters().getDeploymentFrequency().getFilter())
                        .build(),
                JiraIssuesFilter.CALCULATION.ticket_count, JiraIssuesFilter.DISTINCT.none,null,
                null, false, false);

        JiraIssuesFilter requestFilters = JiraIssuesFilter.builder()
                .issueResolutionRange(ImmutablePair.of(1663064804L, 1669373499L))
                .build();

        Long deploymentCount = jiraDoraService.getCountForDeployment(company, velocityFilters, requestFilters);

        Assert.assertEquals(3, (long) deploymentCount);
    }

    @Test
    public void testGetTimeSeriesDataForDeploymentIssueReleasedIn() throws SQLException, BadRequestException, IOException {
        String workflowProfile = ResourceUtils.getResourceAsString("velocity/new_workflow_profile_im.json");
        VelocityConfigDTO velocityConfigDTO = mapper.readValue(workflowProfile, VelocityConfigDTO.class);
        velocityConfigDTO = velocityConfigDTO.toBuilder()
                .deploymentFrequency(
                        velocityConfigDTO.getDeploymentFrequency().toBuilder()
                                .velocityConfigFilters(
                                        velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().toBuilder()
                                                .build()
                                )
                                .calculationField(VelocityConfigDTO.CalculationField.released_in)
                                .build()
                )
                .build();

        JiraIssuesFilter velocityFilters = jiraFilterParser.createFilter(company, DefaultListRequest.builder()
                        .filter(velocityConfigDTO.getDeploymentFrequency()
                                .getVelocityConfigFilters().getDeploymentFrequency().getFilter())
                        .build(),
                JiraIssuesFilter.CALCULATION.ticket_count, JiraIssuesFilter.DISTINCT.none,null,
                null, false, false);

        // 15th Step - 10th Nov
        JiraIssuesFilter requestFilters = JiraIssuesFilter.builder()
                .issueReleasedRange(ImmutablePair.of(1663064804L, 1669373499L))
                .build();

        DoraResponseDTO doraResponseDTO = jiraDoraService.getTimeSeriesDataForDeployment(
                company, velocityFilters, requestFilters, velocityConfigDTO.getDeploymentFrequency().getCalculationField().toString(), null
        );

        Assert.assertEquals(74, doraResponseDTO.getTimeSeries().getDay().size());
        Assert.assertEquals(11, doraResponseDTO.getTimeSeries().getWeek().size());
        Assert.assertEquals(3, doraResponseDTO.getTimeSeries().getMonth().size());
    }

    @Test
    public void testGetTimeSeriesDataForDeploymentIssueUpdatedAt() throws SQLException, BadRequestException, IOException {
        String workflowProfile = ResourceUtils.getResourceAsString("velocity/new_workflow_profile_im.json");
        VelocityConfigDTO velocityConfigDTO = mapper.readValue(workflowProfile, VelocityConfigDTO.class);
        velocityConfigDTO = velocityConfigDTO.toBuilder()
                .deploymentFrequency(
                        velocityConfigDTO.getDeploymentFrequency().toBuilder()
                                .velocityConfigFilters(
                                        velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().toBuilder()
                                                .build()
                                )
                                .calculationField(VelocityConfigDTO.CalculationField.issue_updated_at)
                                .build()
                )
                .build();

        JiraIssuesFilter velocityFilters = jiraFilterParser.createFilter(company, DefaultListRequest.builder()
                        .filter(velocityConfigDTO.getDeploymentFrequency()
                                .getVelocityConfigFilters().getDeploymentFrequency().getFilter())
                        .build(),
                JiraIssuesFilter.CALCULATION.ticket_count, JiraIssuesFilter.DISTINCT.none,null,
                null, false, false);

        // 15th Step - 10th Nov
        JiraIssuesFilter requestFilters = JiraIssuesFilter.builder()
                .issueUpdatedRange(ImmutablePair.of(1663064804L, 1669373499L))
                .build();

        DoraResponseDTO doraResponseDTO = jiraDoraService.getTimeSeriesDataForDeployment(
                company, velocityFilters, requestFilters, velocityConfigDTO.getDeploymentFrequency().getCalculationField().toString(), null
        );

        Assert.assertEquals(74, doraResponseDTO.getTimeSeries().getDay().size());
        Assert.assertEquals(11, doraResponseDTO.getTimeSeries().getWeek().size());
        Assert.assertEquals(3, doraResponseDTO.getTimeSeries().getMonth().size());
    }

    @Test
    public void testGetDeploymentForJiraRelease() throws IOException, SQLException, BadRequestException {
        String workflowProfile = ResourceUtils.getResourceAsString("velocity/new_workflow_profile_im.json");
        VelocityConfigDTO velocityConfigDTO = mapper.readValue(workflowProfile, VelocityConfigDTO.class);
        velocityConfigDTO = velocityConfigDTO.toBuilder()
                .deploymentFrequency(
                        velocityConfigDTO.getDeploymentFrequency().toBuilder()
                                .velocityConfigFilters(
                                        velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().toBuilder()
                                                .build()
                                )
                                .calculationField(VelocityConfigDTO.CalculationField.released_in)
                                .build()
                )
                .build();
        JiraIssuesFilter filter = jiraFilterParser.createFilter(company, DefaultListRequest.builder()
                        .filter(velocityConfigDTO.getDeploymentFrequency()
                                .getVelocityConfigFilters().getDeploymentFrequency().getFilter())
                        .build(),
                JiraIssuesFilter.CALCULATION.ticket_count, JiraIssuesFilter.DISTINCT.none, null,
                null, false, false);
        // 15th Step - 10th Nov
        JiraIssuesFilter requestFilter = JiraIssuesFilter.builder()
                .issueReleasedRange(ImmutablePair.of(1663064804L, 1669373499L))
                .build();


        List<JiraReleaseResponse> queryResult = List.of(
                JiraReleaseResponse.builder()
                        .name("fix-version-1")
                        .issueCount(10)
                        .project("COM")
                        .releaseEndTime(1663064804L)
                        .build(),
                JiraReleaseResponse.builder()
                        .name("fix-version-2")
                        .issueCount(4)
                        .project("COM")
                        .releaseEndTime(1660164304L)
                        .build()
        );
        template = Mockito.mock(NamedParameterJdbcTemplate.class);
        ReflectionTestUtils.setField(jiraDoraService, "template", template, NamedParameterJdbcTemplate.class);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        when(template.query(sqlCaptor.capture(), (Map<String, ?>) any(), (RowMapper<JiraReleaseResponse>) any()))
                .thenReturn(queryResult);
        when(template.queryForObject(anyString(), (Map<String, ?>) any(), (RowMapper<Integer>) any()))
                .thenReturn(2);

        DbListResponse<JiraReleaseResponse> result = jiraDoraService.getDeploymentForJiraRelease(
                company, filter, requestFilter, null, 1, 10
        );

        String expectedSql = "SELECT fix_version, STRING_AGG(result_tbl.project, ', ') as project, sum(count) as count, "
                + "released_date FROM (SELECT release.name AS fix_version, count(issue.key), issue.project,  extract( "
                + "epoch from (end_date) ) AS released_date  FROM test.jira_issues issue INNER JOIN ( SELECT name, "
                + "integration_id AS intg_id,  end_date FROM test.jira_issue_versions WHERE released ) release ON "
                + "release.intg_id = issue.integration_id AND release.name = ANY(fix_versions)  AND (is_active = "
                + ":is_active OR is_active IS NULL) AND ((ingested_at = :jira_ingested_at_1 AND integration_id = '1') ) "
                + "AND (is_active = :req_is_active OR is_active IS NULL) AND end_date >= to_timestamp(:req_end_date_start) "
                + "AND end_date <= to_timestamp(:req_end_date_end) GROUP BY fix_version, project, released_date LIMIT 10 OFFSET 10 )"
                + " result_tbl GROUP BY fix_version, released_date";
        Assert.assertEquals(expectedSql, sqlCaptor.getAllValues().get(0));
        Assert.assertEquals(queryResult, result.getRecords());
        Assert.assertEquals(Optional.of(2), Optional.of(result.getCount()));
    }

    @Test
    public void testGetDrillDownData() throws SQLException, BadRequestException, IOException {
        String workflowProfile = ResourceUtils.getResourceAsString("velocity/new_workflow_profile_im.json");
        VelocityConfigDTO velocityConfigDTO = mapper.readValue(workflowProfile, VelocityConfigDTO.class);

        Map<String, Object> filters = Map.of("statuses", List.of("DONE"));
        JiraIssuesFilter velocityFilters = jiraFilterParser.createFilter(company, DefaultListRequest.builder()
                        .filter(filters)
                        .build(),
                JiraIssuesFilter.CALCULATION.ticket_count, JiraIssuesFilter.DISTINCT.none,null,
                null, false, false);
        velocityFilters = velocityFilters.toBuilder().ingestedAtByIntegrationId(Map.of("1", 1663200000L)).build();
        JiraIssuesFilter requestFilters = JiraIssuesFilter.builder()
                .issueResolutionRange(ImmutablePair.of(1663027200L, 1663113599L))
                .build();

        DbListResponse<DbJiraIssue> records = jiraDoraService.getDrillDownData(company, JiraSprintFilter.builder().build(), requestFilters, Optional.empty(), JiraSprintFilter.builder().build(), velocityFilters, Optional.empty(), null, Map.of(), 0, 10);
        Assert.assertEquals(2, records.getRecords().size());
    }
}

