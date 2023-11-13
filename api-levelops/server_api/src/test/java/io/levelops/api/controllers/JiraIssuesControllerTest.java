package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.api.config.DefaultApiTestConfiguration;
import io.levelops.api.services.JiraIssueApiService;
import io.levelops.api.services.JiraSprintMetricsService;
import io.levelops.api.services.JiraSprintMetricsServiceLegacy;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.databases.services.jira.VelocityStageTimesReportPrecalculateWidgetService;
import io.levelops.commons.databases.services.jira.utils.JiraFilterParser;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.services.velocity_productivity.services.VelocityAggsService;
import io.levelops.faceted_search.services.workitems.EsJiraIssueQueryService;
import io.levelops.ingestion.models.IntegrationType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {DefaultApiTestConfiguration.class})
public class JiraIssuesControllerTest {

    private static final String company = "test";
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    JiraIssueService issueService;
    @Autowired
    ConfigTableHelper configTableHelper;
    @Autowired
    JiraFilterParser jiraFilterParser;
    @Autowired
    AggCacheService cacheService;
    @Autowired
    JiraSprintMetricsServiceLegacy jiraSprintMetricsServiceLegacy;
    @Autowired
    JiraSprintMetricsService jiraSprintMetricsService;
    @Autowired
    OrgUnitHelper orgUnitHelper;
    @Autowired
    EsJiraIssueQueryService esJiraIssueQueryService;
    @Autowired
    Executor dbValuesTaskExecutor;
    @Autowired
    VelocityAggsService velocityAggsService;
    @Autowired
    OrgUsersDatabaseService orgUsersDatabaseService;
    @Autowired
    VelocityStageTimesReportPrecalculateWidgetService velocityStageTimesReportPrecalculateWidgetService;

    @Autowired
    JiraIssueApiService jiraIssueApiService;

    private JiraIssuesController jiraIssuesController;

    @Before()
    public void setup() {
        jiraIssuesController = new JiraIssuesController(
                issueService,
                objectMapper,
                configTableHelper,
                jiraFilterParser,
                jiraIssueApiService,
                cacheService,
                jiraSprintMetricsServiceLegacy,
                jiraSprintMetricsService,
                orgUnitHelper,
                esJiraIssueQueryService,
                dbValuesTaskExecutor,
                velocityAggsService,
                orgUsersDatabaseService,
                velocityStageTimesReportPrecalculateWidgetService, null);
        mvc = MockMvcBuilders.standaloneSetup(jiraIssuesController).build();
    }

    @Test
    public void testGetTimeAcrossStagesLeadReport() throws Exception {
        // configure
        String originalFilter = "{\n" +
                "    \"filter\": {\n" +
                "        \"issue_resolved_at\": {\n" +
                "            \"$gt\": \"1663823600\",\n" +
                "            \"$lt\": \"1684908000\"\n" +
                "        },\n" +
                "        \"velocity_config_id\": \"a3f5ac61-b9f0-4e11-85ee-574793cecbb5\",\n" +
                "        \"integration_ids\": [\n" +
                "            \"4399\"\n" +
                "        ],\n" +
                "        \"excludeVelocityStages\": [\n" +
                "            \"DONE\"\n" +
                "        ],\n" +
                "        \"calculateSingleState\": true,\n" +
                "        \"work_items_type\": \"jira\"\n" +
                "    },\n" +
                "    \"ou_ids\": [\n" +
                "        \"32895\"\n" +
                "    ],\n" +
                "    \"ou_user_filter_designation\": {\n" +
                "        \"sprint\": [\n" +
                "            \"customfield_10103\"\n" +
                "        ]\n" +
                "    },\n" +
                "    \"widget_id\": \"d764c590-f475-11ed-93ec-79e7b0f80b0f\"\n" +
                "}";

        OUConfiguration ouConfig = OUConfiguration.builder()
                .ouRefId(33217)
                .ouId(UUID.fromString("1ecd3075-7ee9-49a7-811c-6667cf7ef778"))
                .request(
                        DefaultListRequest.builder()
                                .pageSize(10)
                                .page(0)
                                .filter(
                                        Map.of(
                                                "released_in", Map.of("$gt", 1683071000, "$lt", 1683244800),
                                                "issue_resolved_at", Map.of("$gt", 1681084800, "$lt", 1683244799),
                                                "velocity_config_id", "0d52bd99-67cb-4e4d-9b88-a894dbf1111b",
                                                "work_items_type", "jira"
                                        )
                                )
                                .build()
                )
                .build();
        when(orgUnitHelper.getOuConfigurationFromRequest(eq(company), eq(IntegrationType.JIRA), any()))
                .thenReturn(ouConfig);
        VelocityConfigDTO velocityConfigDTO = VelocityConfigDTO.builder()
                .preDevelopmentCustomStages(List.of(
                                VelocityConfigDTO.Stage.builder()
                                        .name("TODO")
                                        .order(1)
                                        .event(VelocityConfigDTO.Event.builder()
                                                .type(VelocityConfigDTO.EventType.JIRA_STATUS)
                                                .values(List.of("TODO", "TO-DO")).build()
                                        ).build(),
                                VelocityConfigDTO.Stage.builder()
                                        .name("In PROGRESS")
                                        .order(2)
                                        .event(VelocityConfigDTO.Event.builder()
                                                .type(VelocityConfigDTO.EventType.JIRA_STATUS)
                                                .values(List.of("IN PROGRESS", "DEV IN PROGRESS")).build()
                                        ).build(),
                                VelocityConfigDTO.Stage.builder()
                                        .name("DONE")
                                        .order(3)
                                        .event(VelocityConfigDTO.Event.builder()
                                                .type(VelocityConfigDTO.EventType.JIRA_STATUS)
                                                .values(List.of("DONE", "COMPLETED")).build()
                                        ).build(),
                                VelocityConfigDTO.Stage.builder()
                                        .name("In PROGRESS")
                                        .order(4)
                                        .event(VelocityConfigDTO.Event.builder()
                                                .type(VelocityConfigDTO.EventType.JIRA_RELEASE)
                                                .params(Map.of("prefer_release", List.of("min"))
                                                ).build()
                                        ).build()
                        )
                )
                .isJiraOnly(true)
                .isNew(false)
                .build();
        when(velocityAggsService.getVelocityConfig(eq(company), any())).thenReturn(velocityConfigDTO);
        JiraIssuesFilter issuesFilter = JiraIssuesFilter.builder().build();
        when(jiraFilterParser.createFilter(eq(company), any(), any(), any(), any(), any(), eq(false), eq(false)))
                .thenReturn(issuesFilter);
        when(issueService.stackedGroupBy(eq(company), any(),any(), eq(null), any(), eq(velocityConfigDTO), any()))
                .thenReturn(DbListResponse.of(List.of(), 0));

        // execute & assert
        mvc.perform(post("/v1/jira_issues/velocity_stage_times_report")
                        .sessionAttr("company", company)
                        .requestAttr("there_is_no_cache", false)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(originalFilter))
                .andExpect(status().is(200))
                .andReturn();

        mvc.perform(post("/v1/jira_issues/release_table_report")
                        .sessionAttr("company", company)
                        .requestAttr("there_is_no_cache", false)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(originalFilter))
                .andExpect(status().is(200))
                .andReturn();

        mvc.perform(post("/v1/jira_issues/release_table_report/list")
                        .sessionAttr("company", company)
                        .requestAttr("there_is_no_cache", false)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(originalFilter))
                .andExpect(status().is(200))
                .andReturn();
    }
}