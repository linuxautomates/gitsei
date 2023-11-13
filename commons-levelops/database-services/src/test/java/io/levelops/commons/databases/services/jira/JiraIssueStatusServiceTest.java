package io.levelops.commons.databases.services.jira;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.CICDJobConfigChange;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraProject;
import io.levelops.commons.databases.models.database.jira.DbJiraSprint;
import io.levelops.commons.databases.models.database.jira.DbJiraStatus;
import io.levelops.commons.databases.models.database.jira.DbJiraUser;
import io.levelops.commons.databases.models.database.jira.DbJiraVersion;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationUtils;
import io.levelops.commons.databases.services.JiraFieldService;
import io.levelops.commons.databases.services.JiraProjectService;
import io.levelops.commons.databases.services.TagItemDBService;
import io.levelops.commons.databases.services.TagsService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.jira.conditions.JiraConditionsBuilder;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.jira.models.JiraField;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.jira.models.JiraProject;
import io.levelops.integrations.jira.models.JiraUser;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import junit.framework.TestCase;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.jira.JiraIssueWriteService.UNDEFINED_STATUS_ID;

public class JiraIssueStatusServiceTest {
    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static DatabaseTestUtils.JiraTestDbs jiraTestDbs;
    private static JiraIssueStatusService jiraIssueStatusService;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        if (dataSource != null)
            return;

        dataSource = DatabaseTestUtils.setUpDataSource(pg, company);
        jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, company);
        jiraIssueStatusService = new JiraIssueStatusService(dataSource, null);
    }

    @Test
    public void test() throws SQLException {
        IntegrationUtils.createIntegration(jiraTestDbs.getIntegrationService(), company, 8);
        List<DbJiraStatus> jiraStatuses1_1 = List.of(
                DbJiraStatus.builder()
                        .issueKey("I9P-16887").integrationId("1")
                        .statusId(UNDEFINED_STATUS_ID).status("TO DO").startTime(1660908877l).endTime(1661176314l)
                        .build(),
                DbJiraStatus.builder()
                        .issueKey("I9P-16887").integrationId("1")
                        .statusId(UNDEFINED_STATUS_ID).status("IN PROGRESS").startTime(1661176314l).endTime(1661433478l)
                        .build(),
                DbJiraStatus.builder()
                        .issueKey("I9P-16887").integrationId("1")
                        .statusId(UNDEFINED_STATUS_ID).status("CODE REVIEW").startTime(1661433478l).endTime(1662132033l)
                        .build(),
                DbJiraStatus.builder()
                        .issueKey("I9P-16887").integrationId("1")
                        .statusId(UNDEFINED_STATUS_ID).status("READY FOR TEST").startTime(1662132033l).endTime(1662549681l)
                        .build(),
                DbJiraStatus.builder()
                        .issueKey("I9P-16887").integrationId("1")
                        .statusId(UNDEFINED_STATUS_ID).status("DONE").startTime(1662549681l).endTime(1679615999l)
                        .build()
        );
        List<DbJiraStatus> jiraStatuses1_2 = List.of(
                DbJiraStatus.builder()
                        .issueKey("I9P-16887").integrationId("1")
                        .statusId("1").status("TO DO").startTime(1660908877l).endTime(1661176314l)
                        .build(),
                DbJiraStatus.builder()
                        .issueKey("I9P-16887").integrationId("1")
                        .statusId("2").status("IN PROGRESS").startTime(1661176314l).endTime(1661433478l)
                        .build(),
                DbJiraStatus.builder()
                        .issueKey("I9P-16887").integrationId("1")
                        .statusId("3").status("CODE REVIEW").startTime(1661433478l).endTime(1662132033l)
                        .build(),
                DbJiraStatus.builder()
                        .issueKey("I9P-16887").integrationId("1")
                        .statusId("4").status("READY FOR TEST").startTime(1662132033l).endTime(1662549681l)
                        .build(),
                DbJiraStatus.builder()
                        .issueKey("I9P-16887").integrationId("1")
                        .statusId("5").status("DONE").startTime(1662549681l).endTime(1679615999l)
                        .build()
        );
        DbJiraIssue jiraIssue1 = DbJiraIssue.builder()
                .key("I9P-16887")
                .integrationId("1")
                .project("I9P").summary("Anywhere Plus: I-9 Settings Page - Redesign the configuration settings layout")
                .status("DONE")
                .isActive(true).issueType("STORY")
                .priority("HIGH")
                .assignee("rama.vemuri")//.assigneeId("95aa425a-3f0a-4e5b-9626-9047aca5c1ec")
                .reporter("anil.venuthurupalli")//.reporterId("95385354-f564-4e9c-94e4-7856c600d63b")
                .epic("I9P-16197").parentKey("I9P-16197")
                .descSize(618).hops(2).bounces(0).numAttachments(39)
                .firstAttachmentAt(1660909382l)
                .issueCreatedAt(1660908877l).issueUpdatedAt(1670263974l).issueResolvedAt(1662549681l)
                .issueDueAt(0l)
                .originalEstimate(0l)
                .firstCommentAt(1661436636l).firstAssignedAt(1660908877l)
                .firstAssignee("Jianjie Gao")//.firstAssigneeId("7c8f0ced-b990-48ed-8ad0-e11de5a1404a")
                .ingestedAt(1679788800l)
                .statuses(jiraStatuses1_1)
                .build();

        JiraIssueWriteService jiraIssueWriteService = new JiraIssueWriteService(dataSource, m, null, null);
        jiraIssueWriteService.insert(company, jiraIssue1);
        jiraIssueWriteService.insert(company, jiraIssue1);

        jiraIssue1 = jiraIssue1.toBuilder().statuses(jiraStatuses1_2).build();
        jiraIssueWriteService.insert(company, jiraIssue1);
        jiraIssueWriteService.insert(company, jiraIssue1);

        Map<Pair<String, String>, List<DbJiraStatus>> actual = jiraIssueStatusService.getStatusesForIssues(company, List.of(jiraIssue1));
        Assert.assertEquals(1, actual.size());
        verifyRecords(jiraStatuses1_2, actual.getOrDefault(Pair.of(jiraIssue1.getIntegrationId(), jiraIssue1.getKey()), null));
    }

    private void verifyRecord(DbJiraStatus e, DbJiraStatus a){
        Assert.assertEquals(e.getStatus(), a.getStatus());
        Assert.assertEquals(e.getStatusId(), a.getStatusId());
        Assert.assertEquals(e.getIssueKey(), a.getIssueKey());
        Assert.assertEquals(e.getIntegrationId(), a.getIntegrationId());
        Assert.assertEquals(e.getStartTime(), a.getStartTime());
        Assert.assertEquals(e.getEndTime(), a.getEndTime());
    }
    private void verifyRecords(List<DbJiraStatus> e, List<DbJiraStatus> a){
        Assert.assertEquals(CollectionUtils.isEmpty(e), CollectionUtils.isEmpty(a));
        if(CollectionUtils.isEmpty(e)){
            return;
        }
        Assert.assertEquals(e.size(), a.size());
        Map<Long, DbJiraStatus> expectedMap = e.stream().collect(Collectors.toMap(DbJiraStatus::getStartTime, x -> x));
        Map<Long, DbJiraStatus> actualMap = a.stream().collect(Collectors.toMap(DbJiraStatus::getStartTime, x -> x));


        for(Long key : actualMap.keySet()){
            verifyRecord(actualMap.get(key), expectedMap.get(key));
        }
    }
}