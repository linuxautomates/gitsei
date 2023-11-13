//package io.levelops.aggregations.helpers;
//
//import com.google.common.cache.LoadingCache;
//import io.levelops.aggregations.parsers.JobDtoParser;
//import io.levelops.aggregations.services.AutomationRulesEngine;
//import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
//import io.levelops.commons.databases.models.database.jira.DbJiraIssue.JiraIssueSprintEvent;
//import io.levelops.commons.databases.models.database.jira.DbJiraIssue.JiraIssueSprintEvent.JiraSprintEventType;
//import io.levelops.commons.databases.models.database.jira.DbJiraIssueSprintMapping;
//import io.levelops.commons.databases.models.database.jira.DbJiraSprint;
//import io.levelops.commons.databases.models.database.jira.DbJiraStatus;
//import io.levelops.commons.databases.models.database.jira.DbJiraStoryPoints;
//import io.levelops.commons.databases.services.*;
//import io.levelops.commons.inventory.InventoryService;
//import io.levelops.commons.jackson.DefaultObjectMapper;
//import io.levelops.events.clients.EventsClient;
//import org.assertj.core.api.Assertions;
//import org.junit.Before;
//import org.junit.Test;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//
//import javax.sql.DataSource;
//import java.sql.SQLException;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//import java.util.concurrent.ExecutionException;
//
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//public class JiraAggHelperTest {
//
//    @Mock
//    JobDtoParser jobDtoParser;
//    @Mock
//    JiraIssueService jiraIssueService;
//    @Mock
//    IntegrationTrackingService trackingService;
//    @Mock
//    InventoryService inventoryService;
//    @Mock
//    AutomationRulesEngine automationRulesEngine;
//    @Mock
//    EventsClient eventsClient;
//    @Mock
//    JiraIssueStoryPointsDatabaseService storyPointsDatabaseService;
//    @Mock
//    JiraIssueSprintMappingDatabaseService sprintMappingDatabaseService;
//    @Mock
//    JiraStatusMetadataDatabaseService statusMetadataDatabaseService;
//    @Mock
//    LoadingCache<String, Optional<String>> statusIdToStatusCategoryCache;
//    @Mock
//    LoadingCache<String, Optional<DbJiraSprint>> dbSprintLoadingCache;
//    @Mock
//    DataSource dataSource;
//    @Mock
//    UserIdentityMaskingService userIdentityMaskingService;
//
//    JiraAggHelper jiraAggHelper;
//
//    @Before
//    public void setUp() throws Exception {
//        MockitoAnnotations.initMocks(this);
//
//        when(statusIdToStatusCategoryCache.get(anyString()))
//                .thenAnswer(ans -> Optional.of(ans.getArgument(0, String.class).equalsIgnoreCase("id2") ? "DONE" : "TO DO"));
//
//        jiraAggHelper = new JiraAggHelper(dataSource, jobDtoParser, jiraIssueService, trackingService, DefaultObjectMapper.get(), inventoryService, automationRulesEngine, eventsClient, storyPointsDatabaseService, sprintMappingDatabaseService, statusMetadataDatabaseService, false, userIdentityMaskingService);
//    }
//
//    @Test
//    public void testSprintMappings() throws SQLException, ExecutionException {
//        DbJiraIssue issue = DbJiraIssue.builder()
//                .sprintEvents(Map.of("67", List.of(JiraIssueSprintEvent.builder()
//                                        .sprintId("67")
//                                        .startTime(123L)
//                                        .endTime(124L)
//                                        .eventType(JiraSprintEventType.ADDED)
//                                        .build(),
//                                JiraIssueSprintEvent.builder()
//                                        .sprintId("67")
//                                        .startTime(124L)
//                                        .endTime(199L)
//                                        .eventType(JiraSprintEventType.REMOVED)
//                                        .build(),
//                                JiraIssueSprintEvent.builder()
//                                        .sprintId("67")
//                                        .startTime(199L)
//                                        .endTime(999999L)
//                                        .eventType(JiraSprintEventType.ADDED)
//                                        .build()),
//                        "74", List.of(JiraIssueSprintEvent.builder()
//                                .sprintId("74")
//                                .startTime(456L)
//                                .endTime(999999L)
//                                .eventType(JiraSprintEventType.ADDED)
//                                .build())))
//                .statuses(List.of(
//                        DbJiraStatus.builder()
//                                .status("Not fixed")
//                                .statusId("id1")
//                                .startTime(100L)
//                                .endTime(250L)
//                                .build(),
//                        DbJiraStatus.builder()
//                                .status("Fixed")
//                                .statusId("id2")
//                                .startTime(250L)
//                                .endTime(450L)
//                                .build(),
//                        DbJiraStatus.builder()
//                                .status("ReOpened")
//                                .statusId("id3")
//                                .startTime(450L)
//                                .endTime(650L)
//                                .build()))
//                .storyPointsLogs(List.of(
//                        DbJiraStoryPoints.builder()
//                                .storyPoints(5)
//                                .startTime(100L)
//                                .endTime(250L)
//                                .build(),
//                        DbJiraStoryPoints.builder()
//                                .storyPoints(12)
//                                .startTime(250L)
//                                .endTime(600L)
//                                .build()
//                ))
//                .build();
//        when(dbSprintLoadingCache.get(eq("67"))).thenReturn(Optional.of(
//                DbJiraSprint.builder()
//                        .startDate(200L)
//                        .completedDate(300L)
//                        .build()));
//        when(dbSprintLoadingCache.get(eq("74"))).thenReturn(Optional.of(
//                DbJiraSprint.builder()
//                        .startDate(400L)
//                        .completedDate(500L)
//                        .build()));
//
//        List<DbJiraIssueSprintMapping> sprintMappings = jiraAggHelper.generateIssueSprintMappingsFromEvents("test", "1", "DEVTEST-1", issue.getSprintEvents(), dbSprintLoadingCache);
//        DefaultObjectMapper.prettyPrint(sprintMappings);
//        jiraAggHelper.handleSprintMappings("test", issue, sprintMappings, dbSprintLoadingCache, statusIdToStatusCategoryCache);
//
//        ArgumentCaptor<DbJiraIssueSprintMapping> argCaptor = ArgumentCaptor.forClass(DbJiraIssueSprintMapping.class);
//        verify(sprintMappingDatabaseService, times(2)).upsert(eq("test"), argCaptor.capture());
//        DefaultObjectMapper.prettyPrint(argCaptor.getAllValues());
//        Assertions.assertThat(argCaptor.getAllValues()).containsExactlyInAnyOrder(
//                DbJiraIssueSprintMapping.builder()
//                        .integrationId("1")
//                        .issueKey("DEVTEST-1")
//                        .sprintId("67")
//                        .addedAt(199L)
//                        .planned(true)
//                        .delivered(true)
//                        .outsideOfSprint(false)
//                        .ignorableIssueType(false)
//                        .storyPointsPlanned(5)
//                        .storyPointsDelivered(12)
//                        .build(),
//                DbJiraIssueSprintMapping.builder()
//                        .integrationId("1")
//                        .issueKey("DEVTEST-1")
//                        .sprintId("74")
//                        .addedAt(456L)
//                        .planned(false)
//                        .delivered(false)
//                        .outsideOfSprint(false)
//                        .ignorableIssueType(false)
//                        .storyPointsPlanned(12)
//                        .storyPointsDelivered(12)
//                        .build()
//        );
//    }
//
//    @Test
//    // For LEV-3977: Completed in Sprint Calculation
//    public void testCompletedTicketsAddedToSprint() throws SQLException, ExecutionException {
//        DbJiraIssue issue = DbJiraIssue.builder()
//                .issueResolvedAt(199L) // issue completed before sprint start
//                .sprintEvents(Map.of("67", List.of(JiraIssueSprintEvent.builder()
//                        .sprintId("67")
//                        .startTime(123L)
//                        .endTime(201L)
//                        .eventType(JiraSprintEventType.ADDED)
//                        .build())
//                ))
//                .statuses(List.of(
//                        DbJiraStatus.builder()
//                                .status("Fixed")
//                                .statusId("id2")
//                                .startTime(120L)
//                                .endTime(121L)
//                                .build()))
//                .storyPointsLogs(List.of(
//                        DbJiraStoryPoints.builder()
//                                .storyPoints(5)
//                                .startTime(100L)
//                                .endTime(250L)
//                                .build(),
//                        DbJiraStoryPoints.builder()
//                                .storyPoints(12)
//                                .startTime(250L)
//                                .endTime(600L)
//                                .build()
//                ))
//                .build();
//        when(dbSprintLoadingCache.get(eq("67"))).thenReturn(Optional.of(
//                DbJiraSprint.builder()
//                        .startDate(200L)
//                        .completedDate(300L)
//                        .build()));
//
//        List<DbJiraIssueSprintMapping> sprintMappings = jiraAggHelper.generateIssueSprintMappingsFromEvents(
//                "test", "1", "DEVTEST-1", issue.getSprintEvents(), dbSprintLoadingCache);
//        DefaultObjectMapper.prettyPrint(sprintMappings);
//        jiraAggHelper.handleSprintMappings("test", issue, sprintMappings, dbSprintLoadingCache, statusIdToStatusCategoryCache);
//
//        ArgumentCaptor<DbJiraIssueSprintMapping> argCaptor = ArgumentCaptor.forClass(DbJiraIssueSprintMapping.class);
//        verify(sprintMappingDatabaseService, times(1)).upsert(eq("test"), argCaptor.capture());
//        DefaultObjectMapper.prettyPrint(argCaptor.getAllValues());
//        Assertions.assertThat(argCaptor.getAllValues()).containsExactlyInAnyOrder(
//                DbJiraIssueSprintMapping.builder()
//                        .integrationId("1")
//                        .issueKey("DEVTEST-1")
//                        .sprintId("67")
//                        .addedAt(123L)
//                        .planned(false) // should be false
//                        .delivered(false) // should be false
//                        .outsideOfSprint(true) // should be true
//                        .ignorableIssueType(false)
//                        .storyPointsPlanned(5)
//                        .storyPointsDelivered(12)
//                        .build()
//        );
//    }
//}