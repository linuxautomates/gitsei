package io.levelops.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.JiraReleaseResponse;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.VelocityFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.services.jira.JiraIssueReleaseService;
import io.levelops.commons.databases.services.jira.JiraIssueReleaseWidgetService;
import io.levelops.commons.databases.services.jira.conditions.JiraConditionsBuilder;
import io.levelops.commons.databases.services.jira.conditions.JiraCustomFieldConditionsBuilder;
import io.levelops.commons.databases.services.jira.utils.JiraIssueQueryBuilder;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Date;
import java.util.UUID;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class JiraIssueApiServiceTest {
    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;

    private static JiraIssueApiService jiraIssueApiService;
    final Date currentTime = new Date();
    @Mock
    static JiraCustomFieldConditionsBuilder customFieldConditionsBuilder;
    private static JiraConditionsBuilder jiraConditionsBuilder;
    private static JiraIssueQueryBuilder queryBuilder;
    private static NamedParameterJdbcTemplate template;

    private static JiraIssueReleaseService jiraIssueReleaseService;
    private static JiraIssueReleaseWidgetService jiraIssueReleaseWidgetService;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        if (dataSource != null) {
            return;
        }
        jiraConditionsBuilder = Mockito.mock(JiraConditionsBuilder.class);
        queryBuilder = Mockito.mock(JiraIssueQueryBuilder.class);
        jiraIssueReleaseService = Mockito.mock(JiraIssueReleaseService.class);
        jiraIssueReleaseWidgetService = Mockito.mock(JiraIssueReleaseWidgetService.class);
        jiraIssueApiService = new JiraIssueApiService(jiraIssueReleaseService, jiraIssueReleaseWidgetService);
    }

    @Test
    public void testGetListOfRelease() throws Exception {
        // configure

        Map<String, Object> filter = new HashMap<>();
        filter.put("velocity_config_id", "72c6eb79-ad4f-484e-a294-7449e6bba32a");
        List<String> list = new ArrayList<>();
        list.add("4399");
        filter.put("integration_ids", list);

        DefaultListRequest listRequest = DefaultListRequest.builder()
                .across(VelocityFilter.DISTINCT.velocity.toString())
                .filter(filter)
                .build();

        String originalFilter = "{\n" +
                "    \"filter\": {\n" +
                "        \"issue_resolved_at\": {\n" +
                "            \"$gt\": \"1651536000\",\n" +
                "            \"$lt\": \"1684908000\"\n" +
                "        },\n" +
                "         \"released_in\": {\n" +
                "            \"$gt\": \"1651536000\",\n" +
                "            \"$lt\": \"1687305599\"\n" +
                "        },\n" +
                "        \"velocity_config_id\": \"72c6eb79-ad4f-484e-a294-7449e6bba32a\",\n" +
                "        \"integration_ids\": [\n" +
                "            \"4399\"\n" +
                "        ],\n" +
                "        \"work_items_type\": \"jira\"\n" +
                "    },\n" +
                "    \"ou_ids\": [\n" +
                "        \"33308\"\n" +
                "    ],\n" +
                "    \"widget_id\": \"daaf8460-0537-11ee-a421-55df7dabe7ce\"\n" +
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
                                        .name("$$RELEASE$$")
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

        List<String> stages = new ArrayList<>();
        stages.add("RELEASE");
        JiraIssuesFilter issuesFilter = JiraIssuesFilter.builder().excludeVelocityStages(stages).build();

        DbListResponse<JiraReleaseResponse> dbListResponse = DbListResponse.of(
                List.of(
                        JiraReleaseResponse.builder()
                                .issueCount(3)
                                .name("fix_version1")
                                .project("PROJ")
                                .releaseEndTime(1684886400L)
                                .averageTime(4081669L)
                                .build(),
                        JiraReleaseResponse.builder()
                                .issueCount(2)
                                .name("fix_version2")
                                .project("PROJ")
                                .releaseEndTime(1283886401L)
                                .averageTime(4084659L)
                                .build()
                ),
                2
        );


        DbListResponse<DbJiraIssue> drillDownResponse = DbListResponse.of(
                List.of(DbJiraIssue.builder()
                        .id("24ba0e55-a81e-4ec0-898c-397decebde0b")
                        .key("COM-19")
                        .integrationId("4399")
                        .project("PROJ")
                        .summary("Testing Jira Release 7")
                        .status("DONE")
                        .isActive(true)
                        .issueType("STORY")
                        .priority("MEDIUM")
                        .reporter("XYZ ABC")
                        .hops(0)
                        .bounces(0)
                        .build()),
                1
        );

        when(jiraIssueReleaseWidgetService.jiraReleaseTableReport(anyString(), any(), anyBoolean())).thenReturn(dbListResponse);
        when(jiraIssueReleaseWidgetService.drilldownListReport(anyString(), any(), anyBoolean())).thenReturn(drillDownResponse);
        DbListResponse<JiraReleaseResponse> result = jiraIssueApiService.getListOfRelease(company, listRequest, issuesFilter, velocityConfigDTO, false);
        DbListResponse<DbJiraIssue> drilldownResult = jiraIssueApiService.getListOfReleaseForDrillDown(company, listRequest, issuesFilter, velocityConfigDTO, false);

        Assert.assertEquals(result.getRecords().size(), 2);
        Assert.assertEquals(drilldownResult.getRecords().size(), 1);
    }
}