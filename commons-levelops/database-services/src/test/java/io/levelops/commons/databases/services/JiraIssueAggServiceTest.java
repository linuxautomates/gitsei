package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.jira.DbJiraStatusMetadata;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.JiraOrFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.jira.JiraIssueAggService;
import io.levelops.commons.databases.services.jira.conditions.JiraConditionsBuilder;
import io.levelops.commons.databases.services.jira.conditions.JiraCustomFieldConditionsBuilder;
import io.levelops.commons.databases.services.jira.utils.JiraIssueQueryBuilder;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DefaultListRequest;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import org.apache.commons.lang3.tuple.ImmutablePair;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
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

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


public class JiraIssueAggServiceTest {
    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static DatabaseTestUtils.JiraTestDbs jiraTestDbs;
    private static JiraIssueAggService jiraIssueAggService;
    final Date currentTime = new Date();
    @Mock
    static JiraCustomFieldConditionsBuilder customFieldConditionsBuilder;
    private static JiraConditionsBuilder jiraConditionsBuilder;
    private static JiraStatusMetadataDatabaseService jiraStatusMetadataDatabaseService;
    private static JiraIssueQueryBuilder queryBuilder;
    private static NamedParameterJdbcTemplate template;


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
        jiraIssueAggService = new JiraIssueAggService(dataSource, null, customFieldConditionsBuilder, jiraConditionsBuilder, queryBuilder, jiraStatusMetadataDatabaseService, null);
        template = Mockito.mock(NamedParameterJdbcTemplate.class);
        ReflectionTestUtils.setField(jiraIssueAggService, "template", template, NamedParameterJdbcTemplate.class);
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
                .issueResolutionRange(ImmutablePair.of(1663823600L, 1685016880L))
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
        VelocityConfigDTO.Stage e1 = VelocityConfigDTO.Stage.builder().name("TO DO").order(1).event(event1).build();
        VelocityConfigDTO.Stage e2 = VelocityConfigDTO.Stage.builder().name("IN PROGRESS").order(2).event(event3).build();
        VelocityConfigDTO.Stage e3 = VelocityConfigDTO.Stage.builder().name("DONE").order(3).event(event2).build();
        VelocityConfigDTO.Stage e4 = VelocityConfigDTO.Stage.builder().name("RELEASE").order(3).event(event4).build();
        stages.add(e1);
        stages.add(e2);
        stages.add(e3);
        stages.add(e4);
        VelocityConfigDTO velocityConfigDTO = VelocityConfigDTO.builder()
                .preDevelopmentCustomStages(stages)
                .build();

        List<DbAggregationResult> result = new ArrayList<>();

        DbAggregationResult data1 = DbAggregationResult.builder().key("Done").median(3067602L).max(3067602L).build();
        DbAggregationResult data2 = DbAggregationResult.builder().key("Other").median(9l).max(9l).build();
        result.add(data1);
        result.add(data2);
        DbAggregationResult res = DbAggregationResult.builder().data(result).build();
        result.add(res);

        String initialSql = "SELECT velocity_stage,MIN(total_time_spent) AS mn, MAX(total_time_spent) AS mx,COUNT(DISTINCT id) AS ct, AVG(total_time_spent) AS mean_time, PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY total_time_spent) AS median,  PERCENTILE_CONT(0.90) WITHIN GROUP(ORDER BY total_time_spent) AS p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY total_time_spent) AS p95 FROM (WITH issues AS ( SELECT *  FROM test.jira_issues AS issues WHERE (is_active = :is_active OR is_active IS NULL) AND integration_id IN (:jira_integration_ids) AND issue_resolved_at > :issue_resolved_start AND issue_resolved_at < :issue_resolved_end AND ((ingested_at = :jira_ingested_at_4399 AND integration_id = '4399') ) )SELECT id,velocity_stage, SUM(time_spent) AS total_time_spent FROM ( SELECT *, (CASE  WHEN ( state IN (:velocity_stage_0_)) THEN 'TO-DO'  WHEN ( state IN (:velocity_stage_1_)) THEN 'In Progressss'  WHEN ( state IN (:velocity_stage_2_)) THEN 'Done'  WHEN ( state IN (:status_category_4399_) AND integration_id = :integ_id_4399) THEN 'Ignore_Terminal_Stage'  ELSE 'Other'  END) as velocity_stage  FROM issues  INNER JOIN ( SELECT integration_id as integ_id, issue_key,end_time-start_time as time_spent,status as state from test.jira_issue_statuses ) s ON s.integ_id=issues.integration_id AND s.issue_key=issues.key  ) as finaltable WHERE velocity_stage NOT IN (:not_jira_velocity_stages) GROUP BY id,velocity_stage UNION SELECT id,  'RELEASE' as velocity_stage,  GREATEST(min(release_end_time) - MAX(last_stage_start_time),0) as total_time_spent FROM issues INNER JOIN ( SELECT integration_id as integ_id, issue_key, status as state, start_time AS last_stage_start_time from test.jira_issue_statuses WHERE status IN ('DONE','DONE','DONE','DONE') ) s ON s.integ_id=issues.integration_id AND s.issue_key=issues.key  INNER JOIN (SELECT integration_id AS integ_id, name AS fix_version ,extract(epoch from (end_date)) AS release_end_time FROM test.jira_issue_versions WHERE integration_id IN (:jira_integration_ids) AND extract( epoch from (end_date) ) > :end_date_start AND extract( epoch from (end_date) ) < :end_date_end AND released=true) versions ON versions.integ_id=issues.integration_id AND versions.fix_version=ANY(issues.fix_versions) GROUP BY id,velocity_stage) AS final_table_1 GROUP BY velocity_stage ORDER BY mean_time DESC LIMIT 90";
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        when(template.query(sqlCaptor.capture(), (Map<String, ?>) any(), (RowMapper<DbAggregationResult>) any())).thenReturn(result);

        // execute
        jiraIssueAggService.groupByAndCalculate(company, filter, false, null, null, velocityStageStatusesMap, velocityConfigDTO);

        // assert
        Assert.assertEquals(initialSql, sqlCaptor.getAllValues().get(0));
    }



    @Test
    public void testForReleasedInFilter() throws SQLException {
        // configure
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
        VelocityConfigDTO.Stage e1 = VelocityConfigDTO.Stage.builder().name("TO DO").order(1).event(event1).build();
        VelocityConfigDTO.Stage e2 = VelocityConfigDTO.Stage.builder().name("IN PROGRESS").order(2).event(event3).build();
        VelocityConfigDTO.Stage e3 = VelocityConfigDTO.Stage.builder().name("DONE").order(3).event(event2).build();
        VelocityConfigDTO.Stage e4 = VelocityConfigDTO.Stage.builder().name("RELEASE").order(3).event(event4).build();
        stages.add(e1);
        stages.add(e2);
        stages.add(e3);
        stages.add(e4);
        VelocityConfigDTO velocityConfigDTO = VelocityConfigDTO.builder()
                .preDevelopmentCustomStages(stages)
                .build();

        List<DbAggregationResult> result = new ArrayList<>();

        DbAggregationResult data1 = DbAggregationResult.builder().key("Done").median(3067602L).max(3067602L).build();
        DbAggregationResult data2 = DbAggregationResult.builder().key("Other").median(9l).max(9l).build();
        result.add(data1);
        result.add(data2);
        DbAggregationResult res = DbAggregationResult.builder().data(result).build();
        result.add(res);

        String initialSql = "SELECT velocity_stage,MIN(total_time_spent) AS mn, MAX(total_time_spent) AS mx,COUNT(DISTINCT id) AS ct, AVG(total_time_spent) AS mean_time, PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY total_time_spent) AS median,  PERCENTILE_CONT(0.90) WITHIN GROUP(ORDER BY total_time_spent) AS p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY total_time_spent) AS p95 FROM (WITH issues AS ( SELECT distinct(key) as jira_issue_key, issues.*  FROM test.jira_issues AS issuesnull WHERE (is_active = :is_active OR is_active IS NULL) AND integration_id IN (:jira_integration_ids) AND issue_resolved_at > :issue_resolved_start AND issue_resolved_at < :issue_resolved_end AND ((ingested_at = :jira_ingested_at_4399 AND integration_id = '4399') ) )SELECT id,velocity_stage, SUM(time_spent) AS total_time_spent FROM ( SELECT *, (CASE  WHEN ( state IN (:velocity_stage_0_)) THEN 'TO-DO'  WHEN ( state IN (:velocity_stage_1_)) THEN 'In Progressss'  WHEN ( state IN (:velocity_stage_2_)) THEN 'Done'  WHEN ( state IN (:status_category_4399_) AND integration_id = :integ_id_4399) THEN 'Ignore_Terminal_Stage'  ELSE 'Other'  END) as velocity_stage  FROM issues  INNER JOIN ( SELECT integration_id as integ_id, issue_key,end_time-start_time as time_spent,status as state from test.jira_issue_statuses ) s ON s.integ_id=issues.integration_id AND s.issue_key=issues.key  ) as finaltable WHERE velocity_stage NOT IN (:not_jira_velocity_stages) GROUP BY id,velocity_stage UNION SELECT id,  'RELEASE' as velocity_stage,  GREATEST(min(release_end_time) - MAX(last_stage_start_time),0) as total_time_spent FROM issues INNER JOIN ( SELECT integration_id as integ_id, issue_key, status as state, start_time AS last_stage_start_time from test.jira_issue_statuses WHERE status IN ('DONE','DONE','DONE','DONE') ) s ON s.integ_id=issues.integration_id AND s.issue_key=issues.key  INNER JOIN (SELECT integration_id AS integ_id, name AS fix_version ,extract(epoch from (end_date)) AS release_end_time FROM test.jira_issue_versions WHERE integration_id IN (:jira_integration_ids) AND extract( epoch from (end_date) ) > :end_date_start AND extract( epoch from (end_date) ) < :end_date_end AND released=true) versions ON versions.integ_id=issues.integration_id AND versions.fix_version=ANY(issues.fix_versions) GROUP BY id,velocity_stage) AS final_table_1 GROUP BY velocity_stage ORDER BY mean_time DESC LIMIT 90";
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        when(template.query(sqlCaptor.capture(), (Map<String, ?>) any(), (RowMapper<DbAggregationResult>) any())).thenReturn(result);

        // execute
        jiraIssueAggService.groupByAndCalculate(company, filter, false, null, null, velocityStageStatusesMap, velocityConfigDTO);

        // assert
        Assert.assertEquals(initialSql, sqlCaptor.getAllValues().get(0));
    }

    @Test
    public void testStacking() throws SQLException {
        // configure
        IntegrationUtils.createIntegration(jiraTestDbs.getIntegrationService(), company, 3);
        JiraOrFilter orFilter = JiraOrFilter.builder().issueReleasedRange(ImmutablePair.of(1663823600L, 1685016880L))
                .build();
        List<String>  projectList=new ArrayList<>();
        projectList.add("SEI");
        List<String>  linkprojectList=new ArrayList<>();
        linkprojectList.add("PROP");
        List<String> link=new ArrayList<>();
        link.add("relates to");

        Map<String, SortingOrder> sort=new HashMap<>();
        sort.put("assignee",SortingOrder.ASC);

        List<String> statuslist=new ArrayList<>();
        statuslist.add("IN PROGRESS");
        statuslist.add("TO DO");

        JiraIssuesFilter newfilter = JiraIssuesFilter.builder()
                .integrationIds(List.of("12"))
                .across(JiraIssuesFilter.DISTINCT.assignee)
                .links(link)
                .projects(linkprojectList)
                .statuses(statuslist)
                .ingestedAt(1688947200l)
                .hygieneCriteriaSpecs(Map.of())
                .sort(sort)
                .orFilter(orFilter)
                .build();
        JiraIssuesFilter filter = JiraIssuesFilter.builder()
                .integrationIds(List.of("12"))
                .projects(projectList)
                .across(JiraIssuesFilter.DISTINCT.project)
                .hygieneCriteriaSpecs(Map.of())
                .orFilter(orFilter)
                .build();


        Map<String, Object> params = new HashMap<>();

        Map<String, List<String>> conditions = new HashMap<>();
        List<String> emptyList = new ArrayList<>();
        conditions.put("final_table", emptyList);
        conditions.put("jira_users", emptyList);
        conditions.put("jira_issue_sprints", emptyList);
        conditions.put("jira_issue_versions", emptyList);

        List<String> statusList = new ArrayList<>();
        conditions.put("jira_issue_statuses", statusList);

        List<String> jiraIssueslist = new ArrayList<>();
        jiraIssueslist.add("(is_active = :is_active OR is_active IS NULL)");
        jiraIssueslist.add("integration_id IN (:jira_integration_ids)");
        jiraIssueslist.add("status IN (:jira_statuses)");
        jiraIssueslist.add("project IN (:jira_projects)");
        jiraIssueslist.add("((ingested_at = :jira_ingested_at_12 AND integration_id = '12') )");
        conditions.put("jira_issues", jiraIssueslist);

        List<String> issueLinkList = new ArrayList<>();
        issueLinkList.add("integration_id IN (:jira_integration_ids)");
        issueLinkList.add("relation IN (:links)");
        conditions.put("jira_issue_links", issueLinkList);


        Mockito.when(jiraConditionsBuilder.createWhereClauseAndUpdateParams(anyString(), eq(params), any(), anyLong(), any(), any())).thenReturn(conditions);

        String initialSql = "WITH issues AS ( SELECT *  FROM test.jira_issues AS issues WHERE (is_active = :is_active OR is_active IS NULL) AND integration_id IN (:jira_integration_ids) AND status IN (:jira_statuses) AND project IN (:jira_projects) AND ((ingested_at = :jira_ingested_at_12 AND integration_id = '12') ) )SELECT * FROM (SELECT I.assignee_id , I.assignee,COUNT(DISTINCT I.id) as ct, sum(COALESCE(I.story_points,0)) as total_story_points, avg(COALESCE(I.story_points,0)) as mean_story_points FROM test.jira_issue_links L INNER JOIN ISSUES ON ISSUES.KEY = L.FROM_ISSUE_KEY  AND L.integration_id IN (:jira_integration_ids) AND L.relation IN (:links) INNER JOIN test.jira_issues I ON I.KEY = L.TO_ISSUE_KEY  AND I.integration_id IN (:jira_linked_issues_integration_ids) AND I.ingested_at = :jira_linked_issues_ingested_at AND I.project IN (:jira_issue_project) GROUP BY I.assignee_id , I.assignee) sort ORDER BY LOWER(assignee) ASC NULLS LAST";
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        List<DbAggregationResult> result = new ArrayList<>();
        when(template.query(sqlCaptor.capture(), (Map<String, ?>) any(), (RowMapper<DbAggregationResult>) any())).thenReturn(result);
        // execute
        jiraIssueAggService.groupByAndCalculate(company, filter, true, newfilter, false, null, null, null, null);
        // asserting query
        Assert.assertEquals(initialSql, sqlCaptor.getAllValues().get(0));
    }

    @Test
    public void testResolutionReportByValue() throws SQLException {
        long currentTime = Instant.now().getEpochSecond();
        IntegrationUtils.createIntegration(jiraTestDbs.getIntegrationService(), company, 5);
        List<String> metricList = new ArrayList<>();
        metricList.add("number_of_tickets_closed");
        Map<String, SortingOrder> sort=new HashMap<>();
        sort.put("assignee",SortingOrder.ASC);

        Map<String, Object> filterMap = new HashMap<>();
        filterMap.put("metric", metricList);
        filterMap.put("sort_xaxis", "label_high-low");

        OUConfiguration ouConfig = OUConfiguration.builder()
                .request(
                        DefaultListRequest.builder()
                                .filter(
                                        filterMap
                                )
                                .build()
                )
                .build();

        JiraIssuesFilter filter = JiraIssuesFilter.builder()
                .integrationIds(List.of("12"))

                .calculation(JiraIssuesFilter.CALCULATION.resolution_time)
                .across(JiraIssuesFilter.DISTINCT.assignee)
                .hygieneCriteriaSpecs(Map.of())
                .sort(sort)
                .acrossLimit(20)
                .build();

        Map<String, Object> params = new HashMap<>();

        Map<String, List<String>> conditions = new HashMap<>();
        List<String> emptyList = new ArrayList<>();
        conditions.put("final_table", emptyList);
        conditions.put("jira_users", emptyList);
        conditions.put("jira_issue_sprints", emptyList);
        conditions.put("jira_issue_versions", emptyList);

        List<String> issueLinkList = new ArrayList<>();
        conditions.put("jira_issue_links", issueLinkList);

        List<String> statusList = new ArrayList<>();
        conditions.put("jira_issue_statuses", statusList);

        List<String> jiraIssueslist = new ArrayList<>();
        jiraIssueslist.add("(is_active = :is_active OR is_active IS NULL)");
        jiraIssueslist.add("integration_id IN (:jira_integration_ids)");
        jiraIssueslist.add("((ingested_at = :jira_ingested_at_12 AND integration_id = '12') )");
        conditions.put("jira_issues", jiraIssueslist);

        Mockito.when(jiraConditionsBuilder.createWhereClauseAndUpdateParams(anyString(), eq(params), any(), anyLong(), any(), any())).thenReturn(conditions);

        String initialSql = "WITH issues AS ( SELECT *  FROM test.jira_issues AS issues WHERE (is_active = :is_active OR is_active IS NULL) AND integration_id IN (:jira_integration_ids) AND ((ingested_at = :jira_ingested_at_12 AND integration_id = '12') ) )SELECT * FROM (SELECT assignee_id , assignee,MIN(solve_time) AS mn,MAX(solve_time) AS mx,COUNT(DISTINCT id) AS ct,PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY solve_time) AS median, PERCENTILE_DISC(0.9) WITHIN GROUP(ORDER BY solve_time) AS p90, AVG(solve_time) AS mean FROM ( SELECT issco.*,p.* FROM ( SELECT *,(COALESCE(first_comment_at,"+currentTime+")-issue_created_at) AS resp_time,(COALESCE(issue_resolved_at,"+currentTime+")-issue_created_at) AS solve_time FROM issues  ) AS issco LEFT OUTER JOIN ( SELECT solve_sla,resp_sla,project AS proj,task_type AS ttype,priority AS prio,integration_id AS integid FROM test.jira_issue_priorities_sla ) AS p ON p.proj = issco.project AND p.prio = issco.priority AND p.integid = issco.integration_id AND p.ttype = issco.issue_type ) as finaltable GROUP BY assignee_id , assignee) sort ORDER BY LOWER(assignee) ASC NULLS LAST LIMIT 20";
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        List<DbAggregationResult> result = new ArrayList<>();
        when(template.query(sqlCaptor.capture(), (Map<String, ?>) any(), (RowMapper<DbAggregationResult>) any())).thenReturn(result);
        // execute
        jiraIssueAggService.groupByAndCalculate(company, null, false, filter, false, null, ouConfig, null, null);
        // asserting query
        Assert.assertEquals(initialSql, sqlCaptor.getAllValues().get(0));
    }

    @Test
    public void testResolutionReportByLabel() throws SQLException {
        long currentTime = Instant.now().getEpochSecond();
        IntegrationUtils.createIntegration(jiraTestDbs.getIntegrationService(), company, 4);
        List<String> metricList = new ArrayList<>();
        metricList.add("number_of_tickets_closed");
        Map<String, SortingOrder> sort=new HashMap<>();
        sort.put("resolution_time",SortingOrder.ASC);
        Map<String, Object> filterMap = new HashMap<>();
        filterMap.put("metric", metricList);
        filterMap.put("sort_xaxis", "label_high-low");

        OUConfiguration ouConfig = OUConfiguration.builder()
                .request(
                        DefaultListRequest.builder()
                                .filter(
                                        filterMap
                                )
                                .build()
                )
                .build();


        JiraIssuesFilter filter = JiraIssuesFilter.builder()
                .integrationIds(List.of("12"))
                .calculation(JiraIssuesFilter.CALCULATION.resolution_time)
                .across(JiraIssuesFilter.DISTINCT.assignee)
                .hygieneCriteriaSpecs(Map.of())
                .sort(sort)
                .acrossLimit(20)
                .build();

        Map<String, Object> params = new HashMap<>();

        Map<String, List<String>> conditions = new HashMap<>();
        List<String> emptyList = new ArrayList<>();
        conditions.put("final_table", emptyList);
        conditions.put("jira_users", emptyList);
        conditions.put("jira_issue_sprints", emptyList);
        conditions.put("jira_issue_versions", emptyList);

        List<String> issueLinkList = new ArrayList<>();
        conditions.put("jira_issue_links", issueLinkList);

        List<String> statusList = new ArrayList<>();
        conditions.put("jira_issue_statuses", statusList);

        List<String> jiraIssueslist = new ArrayList<>();
        jiraIssueslist.add("(is_active = :is_active OR is_active IS NULL)");
        jiraIssueslist.add("integration_id IN (:jira_integration_ids)");
        jiraIssueslist.add("((ingested_at = :jira_ingested_at_12 AND integration_id = '12') )");
        conditions.put("jira_issues", jiraIssueslist);

        Mockito.when(jiraConditionsBuilder.createWhereClauseAndUpdateParams(anyString(), eq(params), any(), anyLong(), any(), any())).thenReturn(conditions);

        String initialSql = "WITH issues AS ( SELECT *  FROM test.jira_issues AS issues WHERE (is_active = :is_active OR is_active IS NULL) AND integration_id IN (:jira_integration_ids) AND ((ingested_at = :jira_ingested_at_12 AND integration_id = '12') ) )SELECT assignee_id , assignee,MIN(solve_time) AS mn,MAX(solve_time) AS mx,COUNT(DISTINCT id) AS ct,PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY solve_time) AS median, PERCENTILE_DISC(0.9) WITHIN GROUP(ORDER BY solve_time) AS p90, AVG(solve_time) AS mean FROM ( SELECT issco.*,p.* FROM ( SELECT *,(COALESCE(first_comment_at,"+currentTime+")-issue_created_at) AS resp_time,(COALESCE(issue_resolved_at,"+currentTime+")-issue_created_at) AS solve_time FROM issues  ) AS issco LEFT OUTER JOIN ( SELECT solve_sla,resp_sla,project AS proj,task_type AS ttype,priority AS prio,integration_id AS integid FROM test.jira_issue_priorities_sla ) AS p ON p.proj = issco.project AND p.prio = issco.priority AND p.integid = issco.integration_id AND p.ttype = issco.issue_type ) as finaltable GROUP BY assignee_id , assignee ORDER BY ct ASC NULLS LAST LIMIT 20";
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        List<DbAggregationResult> result = new ArrayList<>();
        when(template.query(sqlCaptor.capture(), (Map<String, ?>) any(), (RowMapper<DbAggregationResult>) any())).thenReturn(result);
        // execute
        jiraIssueAggService.groupByAndCalculate(company, null, false, filter, false, null, ouConfig, null, null);
        // asserting query
        Assert.assertEquals(initialSql, sqlCaptor.getAllValues().get(0));
    }

}