package io.levelops.commons.databases.services.jira;

import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.jira.*;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.JiraOrFilter;
import io.levelops.commons.databases.services.IntegrationUtils;
import io.levelops.commons.databases.services.JiraFieldService;
import io.levelops.commons.databases.services.JiraStatusMetadataDatabaseService;
import io.levelops.commons.databases.services.jira.conditions.JiraConditionsBuilder;

import io.levelops.commons.databases.services.jira.utils.JiraIssueQueryBuilder;

import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import org.apache.commons.lang3.tuple.ImmutablePair;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.List;
import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyInt;


public class JiraIssueReleaseServiceTest {
    private static final String company = "test";
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static DatabaseTestUtils.JiraTestDbs jiraTestDbs;
    private static JiraIssueReleaseService jiraIssueReleaseService;
    final Date currentTime = new Date();
    private static JiraConditionsBuilder jiraConditionsBuilder;
    private static JiraStatusMetadataDatabaseService jiraStatusMetadataDatabaseService;
    private static JiraIssueQueryBuilder queryBuilder;
    @Mock
    private static NamedParameterJdbcTemplate template;
    private static JiraIssueStatusService statusService;
    private static JiraIssueSprintService sprintService;
    private static JiraIssueReadService jiraIssueReadService;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        if (dataSource != null) {
            return;
        }
        dataSource = DatabaseTestUtils.setUpDataSource(pg, company);
        jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, company);
        JiraFieldService jiraFieldService = new JiraFieldService(dataSource);
        jiraFieldService.ensureTableExistence(company);
        jiraConditionsBuilder = Mockito.mock(JiraConditionsBuilder.class);
        queryBuilder = Mockito.mock(JiraIssueQueryBuilder.class);
        jiraStatusMetadataDatabaseService = Mockito.mock(JiraStatusMetadataDatabaseService.class);
        statusService = Mockito.mock(JiraIssueStatusService.class);
        sprintService = Mockito.mock(JiraIssueSprintService.class);
        jiraIssueReadService = Mockito.mock(JiraIssueReadService.class);
        jiraIssueReleaseService = new JiraIssueReleaseService(dataSource, sprintService, statusService, jiraConditionsBuilder, queryBuilder, jiraStatusMetadataDatabaseService);
        template = Mockito.mock(NamedParameterJdbcTemplate.class);
        ReflectionTestUtils.setField(jiraIssueReleaseService, "template", template, NamedParameterJdbcTemplate.class);
    }

    @Test
    public void test() throws SQLException {
        // configure
        IntegrationUtils.createIntegration(jiraTestDbs.getIntegrationService(), company, 8);
        JiraOrFilter orFilter = JiraOrFilter.builder().issueReleasedRange(ImmutablePair.of(1663823600L, 1685016880L))
                .build();

        JiraIssuesFilter filter = JiraIssuesFilter.builder()
                .integrationIds(List.of("4399"))
                .ingestedAt(currentTime.toInstant().getEpochSecond())
                .issueCreatedRange(ImmutablePair.of(1663823600L, 1685016880L))
                .issueReleasedRange(ImmutablePair.of(1663823600L, 1685016880L))
                .calculation(JiraIssuesFilter.CALCULATION.velocity_stage_times_report)
                .across(JiraIssuesFilter.DISTINCT.velocity_stage)
                .hygieneCriteriaSpecs(Map.of())
                .acrossLimit(90)
                .orFilter(orFilter)
                .build();

        Map<String, List<String>> conditions = new HashMap<>();
        Map<String, List<String>> velocityStageStatusesMap = new LinkedHashMap<>();
        List<String> releaseStageList = new ArrayList<>();
        releaseStageList.add("$$RELEASE$$");
        velocityStageStatusesMap.put("RELEASE", releaseStageList);
        List<String> doneStageList = new ArrayList<>();
        doneStageList.add("DONE");
        doneStageList.add("Done");
        doneStageList.add("Complete");
        velocityStageStatusesMap.put("DONE", doneStageList);
        List<String> finalTablelist = new ArrayList<>();
        finalTablelist.add("velocity_stage NOT IN (:not_jira_velocity_stages)");
        conditions.put("final_table", finalTablelist);

        List<String> versionTablelist = new ArrayList<>();
        versionTablelist.add("integration_id IN (:jira_integration_ids)");
        versionTablelist.add("extract( epoch from (end_date) ) > :end_date_start");
        versionTablelist.add("extract( epoch from (end_date) ) < :end_date_end");
        conditions.put("jira_issue_versions", versionTablelist);

        List<String> jiraIssueslist = new ArrayList<>();
        jiraIssueslist.add("(is_active = :is_active OR is_active IS NULL)");
        jiraIssueslist.add("integration_id IN (:jira_integration_ids)");
        jiraIssueslist.add("issue_resolved_at > :issue_resolved_start");
        jiraIssueslist.add("issue_resolved_at < :issue_resolved_end");
        jiraIssueslist.add("((ingested_at = :jira_ingested_at_4399 AND integration_id = '4399') )");
        conditions.put("jira_issues", jiraIssueslist);

        List<String> emptylist = new ArrayList<>();
        conditions.put("jira_issue_statuses", emptylist);
        conditions.put("jira_users", emptylist);
        conditions.put("jira_issue_sprints", emptylist);
        conditions.put("jira_issue_links", emptylist);

        List<DbJiraStatusMetadata.IntegStatusCategoryMetadata> metaDataconditions = new ArrayList<>();
        List<String> todoList = new ArrayList<>();
        todoList.add("TO DO");
        todoList.add("TO DO");
        todoList.add("BACKLOG");
        todoList.add("SELECTED FOR DEVELOPMENT");
        todoList.add("TO DO");
        List<String> doneList = new ArrayList<>();
        doneList.add("DONE");
        doneList.add("DONE");
        doneList.add("DONE");
        doneList.add("DONE");
        List<String> inprogressList = new ArrayList<>();
        inprogressList.add("IN PROGRESS");
        inprogressList.add("DEV IN PROGRESS");
        inprogressList.add("PROGRESS");
        DbJiraStatusMetadata.IntegStatusCategoryMetadata todoData = DbJiraStatusMetadata.IntegStatusCategoryMetadata.builder()
                .integrationId("4399")
                .statusCategory("TO DO")
                .statuses(todoList)
                .build();
        DbJiraStatusMetadata.IntegStatusCategoryMetadata doneData = DbJiraStatusMetadata.IntegStatusCategoryMetadata.builder()
                .integrationId("4399")
                .statusCategory("DONE")
                .statuses(doneList)
                .build();
        DbJiraStatusMetadata.IntegStatusCategoryMetadata inprogresssData = DbJiraStatusMetadata.IntegStatusCategoryMetadata.builder()
                .integrationId("4399")
                .statusCategory("IN PROGRESS")
                .statuses(inprogressList)
                .build();

        metaDataconditions.add(todoData);
        metaDataconditions.add(doneData);
        metaDataconditions.add(inprogresssData);

        JiraFieldService jiraFieldService = new JiraFieldService(dataSource);
        jiraFieldService.ensureTableExistence(company);
        Map<String, Object> params = new HashMap<>();
        Mockito.when(jiraConditionsBuilder.createWhereClauseAndUpdateParams(eq(company), eq(params), any(), anyLong(), anyLong(), any())).thenReturn(conditions);
        Mockito.when(jiraStatusMetadataDatabaseService.getIntegStatusCategoryMetadata(eq(company), any())).thenReturn(metaDataconditions);
        String sql = "CASE  WHEN ( state IN (:velocity_stage_0_)) THEN 'TO-DO'  WHEN ( state IN (:velocity_stage_1_)) THEN 'In Progressss'  WHEN ( state IN (:velocity_stage_2_)) THEN 'Done'  WHEN ( state IN (:status_category_4399_) AND integration_id = :integ_id_4399) THEN 'Ignore_Terminal_Stage'  ELSE 'Other'  END";
        Mockito.when(queryBuilder.generateVelocityStageSql(any(), any(), any())).thenReturn(sql);


        List<VelocityConfigDTO.Stage> stages = new ArrayList<>();
        VelocityConfigDTO.Event event1 = VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.JIRA_STATUS).values(todoList).build();
        VelocityConfigDTO.Event event2 = VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.JIRA_STATUS).values(doneList).build();
        VelocityConfigDTO.Event event3 = VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.JIRA_STATUS).values(inprogressList).build();
        VelocityConfigDTO.Event event4 = VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.JIRA_RELEASE).build();

        VelocityConfigDTO.Stage e1 = VelocityConfigDTO.Stage.builder().name("TO DO").order(1).event(event1)
                .lowerLimitValue(100l).lowerLimitUnit(TimeUnit.SECONDS)
                .upperLimitValue(200l).upperLimitUnit(TimeUnit.SECONDS).build();
        VelocityConfigDTO.Stage e2 = VelocityConfigDTO.Stage.builder().name("IN PROGRESS").order(2).event(event3)
                .lowerLimitValue(100l).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(200l).upperLimitUnit(TimeUnit.SECONDS).build();
        VelocityConfigDTO.Stage e3 = VelocityConfigDTO.Stage.builder().name("DONE").order(3).event(event2)
                .lowerLimitValue(100l).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(200l).upperLimitUnit(TimeUnit.SECONDS).build();
        VelocityConfigDTO.Stage e4 = VelocityConfigDTO.Stage.builder().name("RELEASE").order(3).event(event4)
                .lowerLimitValue(100l).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(200l).upperLimitUnit(TimeUnit.SECONDS).build();
        stages.add(e1);
        stages.add(e2);
        stages.add(e3);
        stages.add(e4);
        VelocityConfigDTO velocityConfigDTO = VelocityConfigDTO.builder().preDevelopmentCustomStages(stages).build();

        List<DbJiraIssue> result = new ArrayList<DbJiraIssue>();
        List<DbJiraStatus> statuslist = new ArrayList<>();
        DbJiraStatus status = DbJiraStatus.builder().status("DONE").startTime(1699899866l).endTime(1699899877l).build();
        statuslist.add(status);
        DbJiraIssue issue = DbJiraIssue.builder().fixVersion("JiraRelease")
                .key("COM-11")
                .project("COM")
                .releaseEndTime(1699899898l)
                .releaseTime(1699899898l)
                .velocityStage("RELEASE")
                .asOfStatus("DONE")
                .status("DONE")
                .statuses(statuslist)
                .integrationId(String.valueOf(4399))
                .build();
        DbJiraIssue issue1 = DbJiraIssue.builder().fixVersion("JiraTestRelease")
                .key("COM-13")
                .project("COM")
                .releaseEndTime(1699899899l)
                .releaseTime(1699899899l)
                .velocityStage("RELEASE")
                .asOfStatus("DONE")
                .status("DONE")
                .statuses(statuslist)
                .integrationId(String.valueOf(4399))
                .build();
        result.add(issue);
        result.add(issue1);

        String initialSql = "WITH issues AS ( SELECT issues.*  FROM test.jira_issues AS issues WHERE (is_active = :is_active OR is_active IS NULL) AND integration_id IN (:jira_integration_ids) AND issue_resolved_at > :issue_resolved_start AND issue_resolved_at < :issue_resolved_end AND ((ingested_at = :jira_ingested_at_4399 AND integration_id = '4399') ) AND array_length(fix_versions, 1) > 0  ) SELECT * FROM ( SELECT distinct (issues.id), issues.* , ( 'RELEASE' ) as velocity_stage  , Greatest(release_end_time - last_stage_start_time, 0) as release_time , release_end_time, fix_version  FROM issues INNER JOIN ( SELECT integration_id as integ_id, issue_key, status as state, max(start_time) as last_stage_start_time from test.jira_issue_statuses WHERE status IN ('DONE','DONE','DONE','DONE') group by integration_id, issue_key, status) s ON s.integ_id=issues.integration_id AND s.issue_key=issues.key  INNER JOIN (SELECT integration_id AS integ_id, name AS fix_version ,extract(epoch from (end_date)) AS release_end_time FROM test.jira_issue_versions WHERE integration_id IN (:jira_integration_ids) AND extract( epoch from (end_date) ) > :end_date_start AND extract( epoch from (end_date) ) < :end_date_end AND released ) versions ON versions.integ_id=issues.integration_id AND versions.fix_version=ANY(issues.fix_versions)  WHERE (is_active = :is_active OR is_active IS NULL) AND integration_id IN (:jira_integration_ids) AND issue_resolved_at > :issue_resolved_start AND issue_resolved_at < :issue_resolved_end AND ((ingested_at = :jira_ingested_at_4399 AND integration_id = '4399') ) ) as tbl WHERE velocity_stage NOT IN (:not_jira_velocity_stages)";
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        when(template.query(sqlCaptor.capture(), (Map<String, ?>) any(), (RowMapper<DbJiraIssue>) any())).thenReturn(result);

        Map<Pair<String, String>, List<DbJiraStatus>> jiraStatuses = new HashMap<>();
        Pair<String, String> pair1 = Pair.of("4399", "COM-13");
        Pair<String, String> pair2 = Pair.of("4399", "COM-11");
        jiraStatuses.put(pair1, statuslist);
        jiraStatuses.put(pair2, statuslist);
        when(statusService.getStatusesForIssues(any(), any(), any())).thenReturn(jiraStatuses);

        DbJiraSprint sprintResult = DbJiraSprint.builder().completedDate(1699899888l).build();
        DbListResponse<DbJiraSprint> sprintResultList = new DbListResponse<>();
        // sprintResultList=DbListResponse.of(Collections.singletonList(sprintResult), 0);
        when(sprintService.filterSprints(any(), anyInt(), anyInt(), any())).thenReturn(sprintResultList);

        Pair<String, String> pair = Pair.of("4399", "COM-13");
        Map<Pair<String, String>, List<DbJiraAssignee>> jiraAssignees = new HashMap<>();
        List<DbJiraAssignee> jiraAssigneeList = Collections.singletonList(DbJiraAssignee.builder().issueKey("COM-13").build());
        jiraAssignees.put(pair, jiraAssigneeList);
        when(jiraIssueReadService.getAssigneesForIssues(any(), anyList())).thenReturn(jiraAssignees);

        DbListResponse<JiraReleaseResponse> testData = jiraIssueReleaseService.jiraReleaseTableReport(company, filter, null, null, velocityConfigDTO, 1, 1);
        Assert.assertTrue(testData.getRecords().size() > 0);
        Assert.assertEquals(initialSql, sqlCaptor.getAllValues().get(0));
    }
}