package io.levelops.aggregations_shared.helpers;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.aggregations_shared.helpers.JiraAggHelperService.ProcessingStatus;
import io.levelops.aggregations_shared.models.SnapshottingSettings;
import io.levelops.aggregations_shared.services.AutomationRulesEngine;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue.JiraIssueSprintEvent;
import io.levelops.commons.databases.models.database.jira.DbJiraIssueSprintMapping;
import io.levelops.commons.databases.models.database.jira.DbJiraSprint;
import io.levelops.commons.databases.models.database.jira.DbJiraStatus;
import io.levelops.commons.databases.models.database.jira.DbJiraStoryPoints;
import io.levelops.commons.databases.models.database.jira.parsers.JiraIssueParser;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.databases.services.JiraIssueSprintMappingDatabaseService;
import io.levelops.commons.databases.services.JiraIssueStoryPointsDatabaseService;
import io.levelops.commons.databases.services.JiraStatusMetadataDatabaseService;
import io.levelops.commons.databases.services.UserIdentityMaskingService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.events.clients.EventsClient;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.integrations.jira.models.JiraIssue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static io.levelops.commons.databases.services.JiraIssueSprintMappingDatabaseService.JiraIssueSprintMappingFilter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JiraAggHelperServiceTest {

    @Mock
    JiraIssueService jiraIssueService;
    @Mock
    IntegrationTrackingService trackingService;
    @Mock
    InventoryService inventoryService;
    @Mock
    AutomationRulesEngine automationRulesEngine;
    @Mock
    EventsClient eventsClient;
    @Mock
    JiraIssueStoryPointsDatabaseService storyPointsDatabaseService;
    @Mock
    JiraIssueSprintMappingDatabaseService sprintMappingDatabaseService;
    @Mock
    JiraStatusMetadataDatabaseService statusMetadataDatabaseService;
    @Mock
    UserIdentityService userIdentityService;
    @Mock
    UserIdentityMaskingService userIdentityMaskingService;
    @Mock
    LoadingCache<String, Optional<String>> statusIdToStatusCategoryCache;
    @Mock
    LoadingCache<String, Optional<DbJiraSprint>> dbSprintLoadingCache;


    JiraAggHelperService jiraAggHelperService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        when(statusIdToStatusCategoryCache.get(anyString()))
                .thenAnswer(ans -> Optional.of(ans.getArgument(0, String.class).equalsIgnoreCase("id2") ? "DONE" : "TO DO"));

        SnapshottingSettings snapshottingSettings = SnapshottingSettings.builder()
                .disableSnapshottingForTenants(Set.of("snapshotting-disabled"))
                .build();

        jiraAggHelperService = new JiraAggHelperService(DefaultObjectMapper.get(), jiraIssueService, inventoryService,
                automationRulesEngine, eventsClient, storyPointsDatabaseService, sprintMappingDatabaseService, statusMetadataDatabaseService,
                userIdentityService, userIdentityMaskingService, false, "test", "test",
                snapshottingSettings);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void processJiraIssue() throws IOException, ExecutionException, SQLException, InventoryException {
        // -- mock
        JiraIssue issue = ResourceUtils.getResourceAsObject("jira/jira_issue.json", JiraIssue.class);
        long issueUpdatedAt = issue.getFields().getUpdated().getTime() / 1000;
        JiraIssueParser.JiraParserConfig parserConfig = JiraIssueParser.JiraParserConfig.builder().build();
        JobDTO jobDTO = JobDTO.builder().query(Map.of("from", 123, "to", 456)).build();

        LoadingCache<String, Optional<String>> emptyCache = Mockito.mock(LoadingCache.class);
        when(emptyCache.get(anyString())).thenReturn(Optional.empty());

        when(userIdentityService.upsertIgnoreEmail(anyString(), any())).thenReturn(null);

        when(inventoryService.listConfigs(anyString(), any(), eq(0), eq(1)))
                .thenReturn(DbListResponse.of(List.of(
                        IntegrationConfig.builder()
                                .metadata(IntegrationConfig.Metadata.builder().configUpdatedAt(123L).build())
                                .build()), 1));

        when(jiraIssueService.list(anyString(), any(), any(), any(), any(), any(), any())).thenReturn(DbListResponse.of(List.of(), 0));

        long currentConfigVersion = 123L;
        Date fetchTime = new Date(1000L);

        // ############ SNAPSHOTTING ENABLED ############

        // -- db issue is missing so cloud issue is new
        when(jiraIssueService.get(eq("test"), eq("LEV-123"), eq("1"), any())).thenReturn(Optional.empty());
        ProcessingStatus output = jiraAggHelperService.processJiraIssue("test", "1", jobDTO, issue, fetchTime, parserConfig, currentConfigVersion, false, null, null, null, emptyCache, emptyCache, emptyCache, null);
        assertThat(output.isSuccess()).isTrue();
        assertThat(output.isShouldInsert()).isTrue();
        assertThat(output.isTodayIssueIsNew()).isTrue();
        assertThat(output.isIssueIsActuallyNewOrUpdated()).isTrue();
        assertThat(output.isIssueNeedsReprocessing()).isFalse();
        assertThat(output.isEventSent()).isTrue();

        ArgumentCaptor<DbJiraIssue> issueArgCaptor = ArgumentCaptor.forClass(DbJiraIssue.class);
        verify(jiraIssueService, times(1)).insert(eq("test"), issueArgCaptor.capture());
        assertThat(issueArgCaptor.getValue().getConfigVersion()).isEqualTo(currentConfigVersion);
        Mockito.clearInvocations(jiraIssueService);

        // -- db issue is out of date
        when(jiraIssueService.get(eq("test"), eq("LEV-123"), eq("2"), any())).thenReturn(Optional.of(DbJiraIssue.builder().issueUpdatedAt(issueUpdatedAt - 10).build()));
        output = jiraAggHelperService.processJiraIssue("test", "2", jobDTO, issue, fetchTime, parserConfig, currentConfigVersion, false, null, null, null, emptyCache, emptyCache, emptyCache, null);
        assertThat(output.isSuccess()).isTrue();
        assertThat(output.isShouldInsert()).isTrue();
        assertThat(output.isTodayIssueIsNew()).isFalse();
        assertThat(output.isIssueIsActuallyNewOrUpdated()).isTrue();
        assertThat(output.isIssueNeedsReprocessing()).isFalse();
        assertThat(output.isEventSent()).isFalse();
        verify(jiraIssueService, times(1)).insert(eq("test"), any());
        Mockito.clearInvocations(jiraIssueService);


        // -- cloud issue is older
        when(jiraIssueService.get(eq("test"), eq("LEV-123"), eq("3"), any())).thenReturn(Optional.of(DbJiraIssue.builder().issueUpdatedAt(issueUpdatedAt + 10).build()));
        output = jiraAggHelperService.processJiraIssue("test", "3", jobDTO, issue, fetchTime, parserConfig, currentConfigVersion, false, null, null, null, emptyCache, emptyCache, emptyCache, null);
        assertThat(output.isSuccess()).isTrue();
        assertThat(output.isShouldInsert()).isFalse();
        assertThat(output.isTodayIssueIsNew()).isFalse();
        assertThat(output.isIssueIsActuallyNewOrUpdated()).isFalse();
        assertThat(output.isIssueNeedsReprocessing()).isFalse();
        assertThat(output.isEventSent()).isFalse();
        verify(jiraIssueService, never()).insert(eq("test"), any());
        Mockito.clearInvocations(jiraIssueService);

        // ############ SNAPSHOTTING DISABLED ############
        // -- cloud issue is older but db issue has no config version -> needs reprocessing
        when(jiraIssueService.get(eq("snapshotting-disabled"), eq("LEV-123"), eq("1"), any())).thenReturn(Optional.of(DbJiraIssue.builder().issueUpdatedAt(issueUpdatedAt + 10).build()));
        output = jiraAggHelperService.processJiraIssue("snapshotting-disabled", "1", jobDTO, issue, fetchTime, parserConfig, currentConfigVersion, false, null, null, null, emptyCache, emptyCache, emptyCache, null);
        assertThat(output.isSuccess()).isTrue();
        assertThat(output.isShouldInsert()).isTrue();
        assertThat(output.isTodayIssueIsNew()).isFalse();
        assertThat(output.isIssueIsActuallyNewOrUpdated()).isFalse();
        assertThat(output.isIssueNeedsReprocessing()).isTrue();
        verify(jiraIssueService, times(1)).insert(eq("snapshotting-disabled"), any());
        Mockito.clearInvocations(jiraIssueService);

        // -- cloud issue is older and db issue's config version is out of date -> needs reprocessing
        when(jiraIssueService.get(eq("snapshotting-disabled"), eq("LEV-123"), eq("1"), any())).thenReturn(Optional.of(DbJiraIssue.builder().issueUpdatedAt(issueUpdatedAt + 10).configVersion(currentConfigVersion - 10).build()));
        output = jiraAggHelperService.processJiraIssue("snapshotting-disabled", "1", jobDTO, issue, fetchTime, parserConfig, currentConfigVersion, false, null, null, null, emptyCache, emptyCache, emptyCache, null);
        assertThat(output.isSuccess()).isTrue();
        assertThat(output.isShouldInsert()).isTrue();
        assertThat(output.isTodayIssueIsNew()).isFalse();
        assertThat(output.isIssueIsActuallyNewOrUpdated()).isFalse();
        assertThat(output.isIssueNeedsReprocessing()).isTrue();
        verify(jiraIssueService, times(1)).insert(eq("snapshotting-disabled"), issueArgCaptor.capture());
        assertThat(issueArgCaptor.getValue().getConfigVersion()).isEqualTo(currentConfigVersion);
        Mockito.clearInvocations(jiraIssueService);

        // -- cloud issue is older and db issue's config version is up-to-date -> no need for reprocessing
        when(jiraIssueService.get(eq("snapshotting-disabled"), eq("LEV-123"), eq("1"), any())).thenReturn(Optional.of(DbJiraIssue.builder().issueUpdatedAt(issueUpdatedAt + 10).configVersion(currentConfigVersion).build()));
        output = jiraAggHelperService.processJiraIssue("snapshotting-disabled", "1", jobDTO, issue, fetchTime, parserConfig, currentConfigVersion, false, null, null, null, emptyCache, emptyCache, emptyCache, null);
        assertThat(output.isSuccess()).isTrue();
        assertThat(output.isShouldInsert()).isFalse();
        assertThat(output.isTodayIssueIsNew()).isFalse();
        assertThat(output.isIssueIsActuallyNewOrUpdated()).isFalse();
        assertThat(output.isIssueNeedsReprocessing()).isFalse();
        verify(jiraIssueService, never()).insert(eq("snapshotting-disabled"), any());
        Mockito.clearInvocations(jiraIssueService);


    }

    @Test
    public void testProcessJiraIssueToVerifyAddAndRemoveEventTogether() throws IOException, ExecutionException, InventoryException, SQLException {
        // configuration
        JiraIssue issue = ResourceUtils.getResourceAsObject("jira/jira_issue_with_change_logs_add_remove.json", JiraIssue.class);
        JobDTO jobDTO = JobDTO.builder().query(Map.of("from", 123, "to", 456)).build();

        long currentConfigVersion = 123L;
        Date fetchTime = new Date(1000L);

        JiraIssueParser.JiraParserConfig parserConfig = JiraIssueParser.JiraParserConfig.builder()
                .sprintFieldKey("customfield_10103")
                .build();
        LoadingCache<String, Optional<String>> emptyCache = Mockito.mock(LoadingCache.class);
        when(emptyCache.get(anyString())).thenReturn(Optional.empty());

        LoadingCache<String, Optional<DbJiraSprint>> dbJiraSprintCache = CacheBuilder.newBuilder().build(
                new CacheLoader<>() {
                    @Override
                    public Optional<DbJiraSprint> load(String key) throws Exception {
                        return Optional.of(DbJiraSprint.builder()
                                .sprintId(3230)
                                .startDate(1695207313L)
                                .endDate(1696071313L)
                                .build());
                    }
                }
        );

        when(inventoryService.listConfigs(anyString(), any(), eq(0), eq(1)))
                .thenReturn(DbListResponse.of(List.of(
                        IntegrationConfig.builder()
                                .metadata(IntegrationConfig.Metadata.builder().configUpdatedAt(123L).build())
                                .build()), 1));

        when(jiraIssueService.list(anyString(), any(), any(), any(), any(), any(), any())).thenReturn(DbListResponse.of(List.of(), 0));

        // execution
        ProcessingStatus output = jiraAggHelperService.processJiraIssue("snapshotting-disabled", "1", jobDTO, issue, fetchTime, parserConfig, currentConfigVersion, false, null, null, dbJiraSprintCache, emptyCache, emptyCache, emptyCache, null);

        // assert
        assertThat(output.isSuccess()).isTrue();
        verify(sprintMappingDatabaseService).upsert("snapshotting-disabled",
                DbJiraIssueSprintMapping.builder()
                        .integrationId("1")
                        .issueKey("SSCA-659")
                        .sprintId("3245")
                        .addedAt(1695631873L)
                        .planned(false)
                        .delivered(false)
                        .outsideOfSprint(false)
                        .ignorableIssueType(false)
                        .storyPointsPlanned(0)
                        .storyPointsDelivered(0)
                        .removedMidSprint(false)
                        .build()
        );
        verify(sprintMappingDatabaseService).upsert("snapshotting-disabled",
                DbJiraIssueSprintMapping.builder()
                        .integrationId("1")
                        .issueKey("SSCA-659")
                        .sprintId("3230")
                        .addedAt(1694976592L)
                        .planned(true)
                        .delivered(false)
                        .outsideOfSprint(false)
                        .ignorableIssueType(false)
                        .storyPointsPlanned(0)
                        .storyPointsDelivered(0)
                        .removedMidSprint(true)
                        .build()
        );
    }


    @Test
    public void testProcessJiraIssueForRemovedFromSprintCase() throws IOException, InventoryException, SQLException, ExecutionException {

        // configuration
        JiraIssue issue = ResourceUtils.getResourceAsObject("jira/jira_issue.json", JiraIssue.class);
        JobDTO jobDTO = JobDTO.builder().query(Map.of("from", 123, "to", 456)).build();
        long currentConfigVersion = 123L;
        Date fetchTime = new Date(1000L);

        JiraIssueParser.JiraParserConfig parserConfig = JiraIssueParser.JiraParserConfig.builder()
                .sprintFieldKey("customfield_10103")
                .build();
        LoadingCache<String, Optional<String>> emptyCache = Mockito.mock(LoadingCache.class);
        when(emptyCache.get(anyString())).thenReturn(Optional.empty());
        LoadingCache<String, Optional<DbJiraSprint>> dbJiraSprintCache = CacheBuilder.newBuilder().build(
                new CacheLoader<>() {
                    @Override
                    public Optional<DbJiraSprint> load(String key) throws Exception {
                        return Optional.of(DbJiraSprint.builder()
                                .sprintId(3230)
                                .startDate(1695207313L)
                                .endDate(1696071313L)
                                .build());
                    }
                }
        );

        when(inventoryService.listConfigs(anyString(), any(), eq(0), eq(1)))
                .thenReturn(DbListResponse.of(List.of(
                        IntegrationConfig.builder()
                                .metadata(IntegrationConfig.Metadata.builder().configUpdatedAt(123L).build())
                                .build()), 1));

        when(jiraIssueService.list(anyString(), any(), any(), any(), any(), any(), any())).thenReturn(DbListResponse.of(List.of(), 0));

        // execution
        ProcessingStatus output = jiraAggHelperService.processJiraIssue("snapshotting-disabled", "1", jobDTO, issue, fetchTime, parserConfig, currentConfigVersion, false, null, null, dbJiraSprintCache, emptyCache, emptyCache, emptyCache, null);

        // assert
        assertThat(output.isSuccess()).isTrue();
        verify(sprintMappingDatabaseService).upsert("snapshotting-disabled",
                DbJiraIssueSprintMapping.builder()
                        .integrationId("1")
                        .issueKey("LEV-123")
                        .sprintId("3230")
                        .addedAt(1626307108L)
                        .planned(true)
                        .delivered(false)
                        .outsideOfSprint(false)
                        .ignorableIssueType(false)
                        .storyPointsPlanned(0)
                        .storyPointsDelivered(0)
                        .removedMidSprint(true)
                        .build()
        );
    }

    @Test
    public void testSprintMappings() throws SQLException, ExecutionException {
        DbJiraIssue.DbJiraIssueBuilder builder = DbJiraIssue.builder();
        builder.sprintEvents(Map.of("67", List.of(JiraIssueSprintEvent.builder()
                                .sprintId("67")
                                .startTime(123L)
                                .endTime(124L)
                                .eventType(JiraIssueSprintEvent.JiraSprintEventType.ADDED)
                                .build(),
                        JiraIssueSprintEvent.builder()
                                .sprintId("67")
                                .startTime(124L)
                                .endTime(199L)
                                .eventType(JiraIssueSprintEvent.JiraSprintEventType.REMOVED)
                                .build(),
                        JiraIssueSprintEvent.builder()
                                .sprintId("67")
                                .startTime(199L)
                                .endTime(999999L)
                                .eventType(JiraIssueSprintEvent.JiraSprintEventType.ADDED)
                                .build()),
                "74", List.of(JiraIssueSprintEvent.builder()
                        .sprintId("74")
                        .startTime(456L)
                        .endTime(999999L)
                        .eventType(JiraIssueSprintEvent.JiraSprintEventType.ADDED)
                        .build())));
        builder.statuses(List.of(
                DbJiraStatus.builder()
                        .status("Not fixed")
                        .statusId("id1")
                        .startTime(100L)
                        .endTime(250L)
                        .build(),
                DbJiraStatus.builder()
                        .status("Fixed")
                        .statusId("id2")
                        .startTime(250L)
                        .endTime(450L)
                        .build(),
                DbJiraStatus.builder()
                        .status("ReOpened")
                        .statusId("id3")
                        .startTime(450L)
                        .endTime(650L)
                        .build()));
        builder.storyPointsLogs(List.of(
                DbJiraStoryPoints.builder()
                        .storyPoints(5)
                        .startTime(100L)
                        .endTime(250L)
                        .build(),
                DbJiraStoryPoints.builder()
                        .storyPoints(12)
                        .startTime(250L)
                        .endTime(600L)
                        .build()
        ));
        DbJiraIssue issue = builder
                .build();
        when(dbSprintLoadingCache.get(eq("67"))).thenReturn(Optional.of(
                DbJiraSprint.builder()
                        .startDate(200L)
                        .completedDate(300L)
                        .build()));
        when(dbSprintLoadingCache.get(eq("74"))).thenReturn(Optional.of(
                DbJiraSprint.builder()
                        .startDate(400L)
                        .completedDate(500L)
                        .build()));

        JiraAggHelperService.SprintMappingEvents sprintMappingEvents = jiraAggHelperService.generateIssueSprintMappingsFromEvents("test", "1", "DEVTEST-1", issue.getSprintEvents(), dbSprintLoadingCache);
        DefaultObjectMapper.prettyPrint(sprintMappingEvents);
        jiraAggHelperService.handleSprintMappings("test", issue, sprintMappingEvents, dbSprintLoadingCache, statusIdToStatusCategoryCache);

        ArgumentCaptor<DbJiraIssueSprintMapping> argCaptor = ArgumentCaptor.forClass(DbJiraIssueSprintMapping.class);
        verify(sprintMappingDatabaseService, times(2)).upsert(eq("test"), argCaptor.capture());
        DefaultObjectMapper.prettyPrint(argCaptor.getAllValues());
        assertThat(argCaptor.getAllValues()).containsExactlyInAnyOrder(
                DbJiraIssueSprintMapping.builder()
                        .integrationId("1")
                        .issueKey("DEVTEST-1")
                        .sprintId("67")
                        .addedAt(199L)
                        .planned(true)
                        .delivered(true)
                        .outsideOfSprint(false)
                        .ignorableIssueType(false)
                        .storyPointsPlanned(5)
                        .storyPointsDelivered(12)
                        .removedMidSprint(false)
                        .build(),
                DbJiraIssueSprintMapping.builder()
                        .integrationId("1")
                        .issueKey("DEVTEST-1")
                        .sprintId("74")
                        .addedAt(456L)
                        .planned(false)
                        .delivered(false)
                        .outsideOfSprint(false)
                        .ignorableIssueType(false)
                        .storyPointsPlanned(12)
                        .storyPointsDelivered(12)
                        .removedMidSprint(false)
                        .build()
        );
    }

    @Test
    public void testSprintMappingsCleanUp() throws SQLException, ExecutionException {
        DbJiraIssue.DbJiraIssueBuilder builder = DbJiraIssue.builder();
        builder.sprintEvents(Map.of("67", List.of(JiraIssueSprintEvent.builder()
                                .sprintId("67")
                                .startTime(123L)
                                .endTime(124L)
                                .eventType(JiraIssueSprintEvent.JiraSprintEventType.ADDED)
                                .build(),
                        JiraIssueSprintEvent.builder()
                                .sprintId("67")
                                .startTime(124L)
                                .endTime(199L)
                                .eventType(JiraIssueSprintEvent.JiraSprintEventType.REMOVED)
                                .build()),
                "74", List.of(JiraIssueSprintEvent.builder()
                        .sprintId("74")
                        .startTime(456L)
                        .endTime(999999L)
                        .eventType(JiraIssueSprintEvent.JiraSprintEventType.ADDED)
                        .build())));
        DbJiraIssue issue = builder
                .integrationId("1")
                .key("DEVTEST-1")
                .build();
        when(dbSprintLoadingCache.get(eq("67"))).thenReturn(Optional.of(
                DbJiraSprint.builder()
                        .startDate(150L)
                        .completedDate(300L)
                        .build()));
        when(dbSprintLoadingCache.get(eq("74"))).thenReturn(Optional.of(
                DbJiraSprint.builder()
                        .startDate(400L)
                        .completedDate(500L)
                        .build()));

        // -- TEST SPRINT EVENTS

        JiraAggHelperService.SprintMappingEvents sprintMappingEvents = jiraAggHelperService.generateIssueSprintMappingsFromEvents("test", "1", issue.getKey(), issue.getSprintEvents(), dbSprintLoadingCache);

        // -- VERIFY
        assertThat(sprintMappingEvents.getIssueSprintMappings()).containsExactly(DbJiraIssueSprintMapping.builder()
                .integrationId("1")
                .issueKey("DEVTEST-1")
                .sprintId("74")
                .addedAt(456L)
                .removedMidSprint(false)
                .build());
        assertThat(sprintMappingEvents.getSprintIdsToExcludeFromSprintMetrics()).containsExactly("67");

        // -- TEST HANDLE SPRINT MAPPINGS

        when(sprintMappingDatabaseService.stream(eq("test"), eq((JiraIssueSprintMappingFilter.builder()
                .integrationIds(List.of("1"))
                .issueKey("DEVTEST-1")
                .sprintIds(List.of("67"))
                .build()))))
                .thenReturn(Stream.of(
                        DbJiraIssueSprintMapping.builder()
                                .id("id1")
                                .build(),
                        DbJiraIssueSprintMapping.builder()
                                .id("id2")
                                .build()));

        jiraAggHelperService.handleSprintMappings("test", issue, sprintMappingEvents, dbSprintLoadingCache, statusIdToStatusCategoryCache);

        // -- VERIFY

        var idArgCaptor = ArgumentCaptor.forClass(String.class);
        verify(sprintMappingDatabaseService, times(2)).delete(eq("test"), idArgCaptor.capture());
        assertThat(idArgCaptor.getAllValues()).containsExactlyInAnyOrder("id1", "id2");

        ArgumentCaptor<DbJiraIssueSprintMapping> argCaptor = ArgumentCaptor.forClass(DbJiraIssueSprintMapping.class);
        verify(sprintMappingDatabaseService, times(1)).upsert(eq("test"), argCaptor.capture());
        DefaultObjectMapper.prettyPrint(argCaptor.getAllValues());
        assertThat(argCaptor.getAllValues()).containsExactlyInAnyOrder(
                DbJiraIssueSprintMapping.builder()
                        .integrationId("1")
                        .issueKey("DEVTEST-1")
                        .sprintId("74")
                        .addedAt(456L)
                        .planned(false)
                        .delivered(false)
                        .outsideOfSprint(false)
                        .ignorableIssueType(false)
                        .storyPointsPlanned(0)
                        .storyPointsDelivered(0)
                        .removedMidSprint(false)
                        .build()
        );
    }

    @Test
    // For LEV-3977: Completed in Sprint Calculation
    public void testCompletedTicketsAddedToSprint() throws SQLException, ExecutionException {
        DbJiraIssue issue = DbJiraIssue.builder()
                .issueResolvedAt(199L) // issue completed before sprint start
                .sprintEvents(Map.of("67", List.of(JiraIssueSprintEvent.builder()
                        .sprintId("67")
                        .startTime(123L)
                        .endTime(201L)
                        .eventType(JiraIssueSprintEvent.JiraSprintEventType.ADDED)
                        .build())
                ))
                .statuses(List.of(
                        DbJiraStatus.builder()
                                .status("Fixed")
                                .statusId("id2")
                                .startTime(120L)
                                .endTime(121L)
                                .build()))
                .storyPointsLogs(List.of(
                        DbJiraStoryPoints.builder()
                                .storyPoints(5)
                                .startTime(100L)
                                .endTime(250L)
                                .build(),
                        DbJiraStoryPoints.builder()
                                .storyPoints(12)
                                .startTime(250L)
                                .endTime(600L)
                                .build()
                ))
                .build();
        when(dbSprintLoadingCache.get(eq("67"))).thenReturn(Optional.of(
                DbJiraSprint.builder()
                        .startDate(200L)
                        .completedDate(300L)
                        .build()));

        JiraAggHelperService.SprintMappingEvents sprintMappings = jiraAggHelperService.generateIssueSprintMappingsFromEvents(
                "test", "1", "DEVTEST-1", issue.getSprintEvents(), dbSprintLoadingCache);
        DefaultObjectMapper.prettyPrint(sprintMappings);
        jiraAggHelperService.handleSprintMappings("test", issue, sprintMappings, dbSprintLoadingCache, statusIdToStatusCategoryCache);

        ArgumentCaptor<DbJiraIssueSprintMapping> argCaptor = ArgumentCaptor.forClass(DbJiraIssueSprintMapping.class);
        verify(sprintMappingDatabaseService, times(1)).upsert(eq("test"), argCaptor.capture());
        DefaultObjectMapper.prettyPrint(argCaptor.getAllValues());
        assertThat(argCaptor.getAllValues()).containsExactlyInAnyOrder(
                DbJiraIssueSprintMapping.builder()
                        .integrationId("1")
                        .issueKey("DEVTEST-1")
                        .sprintId("67")
                        .addedAt(123L)
                        .planned(false) // should be false
                        .delivered(false) // should be false
                        .outsideOfSprint(true) // should be true
                        .ignorableIssueType(false)
                        .removedMidSprint(false)
                        .storyPointsPlanned(5)
                        .storyPointsDelivered(12)
                        .build()
        );
    }


    @Test
    public void findSprintAddedAtEventTime() throws IOException, ExecutionException {
        List<JiraIssueSprintEvent> sprintEvents = List.of(
                JiraIssueSprintEvent.builder()
                        .sprintId("338")
                        .eventType(JiraIssueSprintEvent.JiraSprintEventType.ADDED)
                        .startTime(1667443943L)
                        .endTime(1667843677L)
                        .build(),
                JiraIssueSprintEvent.builder()
                        .sprintId("338")
                        .startTime(1667843677L)
                        .endTime(1674761404L)
                        .eventType(JiraIssueSprintEvent.JiraSprintEventType.REMOVED)
                        .build()
        );

        when(dbSprintLoadingCache.get(eq("338"))).thenReturn(Optional.of(
                DbJiraSprint.builder()
                        .startDate(1667822400L) // 2022-11-07T12:00:00.000Z
                        .completedDate(2667852590L)
                        .build()));

        Long addedAt = JiraAggHelperService.findSprintAddedAtEventTime("test", "1", "PROP-1", "338", sprintEvents, dbSprintLoadingCache);
        assertThat(addedAt).isEqualTo(1667443943L);

        when(dbSprintLoadingCache.get(eq("338"))).thenReturn(Optional.of(
                DbJiraSprint.builder()
                        .startDate(1667852590L) // 2022-11-07 20:23:10
                        .completedDate(2667852590L)
                        .build()));

        addedAt = JiraAggHelperService.findSprintAddedAtEventTime("test", "1", "PROP-1", "338", sprintEvents, dbSprintLoadingCache);
        assertThat(addedAt).isNull();
    }

    @Test
    public void testIsRemovedFromActiveSprintIssueRemovedFromActiveSprintCase() throws ExecutionException {

        // configure
        List<JiraIssueSprintEvent> sprintEvents = List.of(
                JiraIssueSprintEvent.builder()
                        .sprintId("338")
                        .eventType(JiraIssueSprintEvent.JiraSprintEventType.ADDED)
                        .startTime(1667443943L)
                        .endTime(1667843677L)
                        .build(),
                JiraIssueSprintEvent.builder()
                        .sprintId("338")
                        .startTime(1667843898L)
                        .endTime(1674761404L)
                        .eventType(JiraIssueSprintEvent.JiraSprintEventType.REMOVED)
                        .build()
        );

        when(dbSprintLoadingCache.get(eq("338"))).thenReturn(Optional.of(
                DbJiraSprint.builder()
                        .startDate(1667390400L) // 2022-11-02T12:00:00.000Z
                        .endDate(1668976200L)   // 2022-11-20T20:30:00.000Z
                        .completedDate(1668976200L)
                        .build())
        );

        // execute
        boolean isRemovedFromActiveSprint = JiraAggHelperService.isRemovedFromActiveSprint("test", "1", "PROP-1", "338", sprintEvents, dbSprintLoadingCache);

        // assert
        Assert.assertTrue(isRemovedFromActiveSprint);
    }

    @Test
    public void testisRemovedFromActiveSprintIssueRemovedAfterSprintEndCase() throws ExecutionException {

        // configure
        List<JiraIssueSprintEvent> sprintEvents = List.of(
                JiraIssueSprintEvent.builder()
                        .sprintId("338")
                        .eventType(JiraIssueSprintEvent.JiraSprintEventType.ADDED)
                        .startTime(1667443943L)
                        .endTime(1667843677L)
                        .build(),
                JiraIssueSprintEvent.builder()
                        .sprintId("338")
                        .startTime(1669270500L)
                        .endTime(1669319100L)
                        .eventType(JiraIssueSprintEvent.JiraSprintEventType.REMOVED)
                        .build()
        );

        when(dbSprintLoadingCache.get(eq("338"))).thenReturn(Optional.of(
                DbJiraSprint.builder()
                        .startDate(1667390400L) // 2022-11-02T12:00:00.000Z
                        .endDate(1668976200L)   // 2022-11-20T20:30:00.000Z
                        .completedDate(1668976200L)
                        .build())
        );

        // execute
        boolean isRemovedFromActiveSprint = JiraAggHelperService.isRemovedFromActiveSprint("test", "1", "PROP-1", "338", sprintEvents, dbSprintLoadingCache);

        // assert
        Assert.assertFalse(isRemovedFromActiveSprint);
    }

    @Test
    public void testisRemovedFromActiveSprintIssueRemovedBeforeSprintEndCase() throws ExecutionException {

        // configure
        List<JiraIssueSprintEvent> sprintEvents = List.of(
                JiraIssueSprintEvent.builder()
                        .sprintId("338")
                        .eventType(JiraIssueSprintEvent.JiraSprintEventType.ADDED)
                        .startTime(1666738810L)
                        .endTime(1666785645L)
                        .build(),
                JiraIssueSprintEvent.builder()
                        .sprintId("338")
                        .startTime(1667331900L)
                        .endTime(1667343610L)
                        .eventType(JiraIssueSprintEvent.JiraSprintEventType.REMOVED)
                        .build()
        );

        when(dbSprintLoadingCache.get(eq("338"))).thenReturn(Optional.of(
                DbJiraSprint.builder()
                        .startDate(1667390400L) // 2022-11-02T12:00:00.000Z
                        .endDate(1668976200L)   // 2022-11-20T20:30:00.000Z
                        .completedDate(1668976200L)
                        .build())
        );

        // execute
        boolean isRemovedFromActiveSprint = JiraAggHelperService.isRemovedFromActiveSprint("test", "1", "PROP-1", "338", sprintEvents, dbSprintLoadingCache);

        // assert
        Assert.assertFalse(isRemovedFromActiveSprint);
    }

    @Test
    public void testLogIfRelevant() {
        jiraAggHelperService.logInfoIfRelevant("cdkglobal", DbJiraIssue.builder().key("FLXPCKMIGR-2360").build(), "{} {}! {}", "hello", "world", 123);
    }

    @Test
    public void testGetInheritedParentLabels() throws SQLException {
        when(jiraIssueService.get(eq("test"), eq("PARENT"), eq("1"), eq(0L)))
                .thenReturn(Optional.of(DbJiraIssue.builder().labels(List.of("a", "b", "c")).build()));

        // parent is an epic - should inherit
        List<String> inheritedParentLabels = jiraAggHelperService.getInheritedParentLabels("test", "1", DbJiraIssue.builder()
                .key("CHILD-1")
                .parentKey("PARENT")
                .parentIssueType("EPIC")
                .ingestedAt(0L)
                .build());
        assertThat(inheritedParentLabels).containsExactly("a", "b", "c");

        // parent is an epic (but through epic link, not parent link) - should inherit
        List<String> inheritedParentLabels2 = jiraAggHelperService.getInheritedParentLabels("test", "1", DbJiraIssue.builder()
                .key("CHILD-1")
                .epic("PARENT")
                .ingestedAt(0L)
                .build());
        assertThat(inheritedParentLabels2).containsExactly("a", "b", "c");

        // parent is not an epic
        inheritedParentLabels = jiraAggHelperService.getInheritedParentLabels("test", "1", DbJiraIssue.builder()
                .key("CHILD-1")
                .parentKey("PARENT")
                .parentIssueType("NOT AN EPIC")
                .ingestedAt(0L)
                .build());
        assertThat(inheritedParentLabels).isEmpty();

        // no parent
        inheritedParentLabels = jiraAggHelperService.getInheritedParentLabels("test", "1", DbJiraIssue.builder()
                .key("CHILD-1")
                .ingestedAt(0L)
                .build());
        assertThat(inheritedParentLabels).isEmpty();
    }

    @Test
    public void testBequeathLabelsToChildren() throws SQLException {
        // -- issue is not an epic
        jiraAggHelperService.bequeathLabelsToChildren("test", "1", DbJiraIssue.builder()
                .key("NOT EPIC")
                .ingestedAt(0L)
                .labels(List.of("a", "b", "c"))
                .build());
        verify(jiraIssueService, never()).insert(eq("test"), any());

        // -- epic is not a parent
        jiraAggHelperService.bequeathLabelsToChildren("test", "1", DbJiraIssue.builder()
                .key("NOT PARENT")
                .issueType("EPIC")
                .ingestedAt(0L)
                .labels(List.of("a", "b", "c"))
                .build());
        verify(jiraIssueService, never()).insert(eq("test"), any());

        // -- epic is parent of 3 children
        when(jiraIssueService.stream(
                eq("test"),
                eq(JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(0L)
                        .parentKeys(List.of("PARENT"))
                        .build()),
                isNull(), isNull(), isNull(), isNull(), isNull())
        ).thenReturn(Stream.of(
                DbJiraIssue.builder().key("CHILD-1").build(),
                DbJiraIssue.builder().key("CHILD-2").build(),
                DbJiraIssue.builder().key("CHILD-3").build()
        ));
        when(jiraIssueService.stream(
                eq("test"),
                eq(JiraIssuesFilter.builder()
                        .integrationIds(List.of("1"))
                        .ingestedAt(0L)
                        .epics(List.of("PARENT"))
                        .build()),
                isNull(), isNull(), isNull(), isNull(), isNull())
        ).thenReturn(Stream.of(
                DbJiraIssue.builder().key("CHILD-2").build(),
                DbJiraIssue.builder().key("CHILD-3").build(),
                DbJiraIssue.builder().key("CHILD-4").build()
        ));
        jiraAggHelperService.bequeathLabelsToChildren("test", "1", DbJiraIssue.builder()
                .key("PARENT")
                .issueType("EPIC")
                .ingestedAt(0L)
                .labels(List.of("a", "b", "c"))
                .build());

        ArgumentCaptor<DbJiraIssue> dbJiraIssueArgumentCaptor = ArgumentCaptor.forClass(DbJiraIssue.class);
        verify(jiraIssueService, times(4)).insert(eq("test"), dbJiraIssueArgumentCaptor.capture());
        assertThat(dbJiraIssueArgumentCaptor.getAllValues()).containsExactlyInAnyOrder(
                DbJiraIssue.builder().key("CHILD-1").parentLabels(List.of("a", "b", "c")).build(),
                DbJiraIssue.builder().key("CHILD-2").parentLabels(List.of("a", "b", "c")).build(),
                DbJiraIssue.builder().key("CHILD-3").parentLabels(List.of("a", "b", "c")).build(),
                DbJiraIssue.builder().key("CHILD-4").parentLabels(List.of("a", "b", "c")).build()
        );
    }

    @Test
    public void isReprocessingNeeded() {
        assertThat(JiraAggHelperService.isReprocessingNeeded(null, null)).isEqualTo(false);
        assertThat(JiraAggHelperService.isReprocessingNeeded(null, 123L)).isEqualTo(true);
        assertThat(JiraAggHelperService.isReprocessingNeeded(DbJiraIssue.builder().build(), 123L)).isEqualTo(true);
        assertThat(JiraAggHelperService.isReprocessingNeeded(DbJiraIssue.builder().configVersion( 100L).build(), 123L)).isEqualTo(true);
        assertThat(JiraAggHelperService.isReprocessingNeeded(DbJiraIssue.builder().configVersion( 123L).build(), 123L)).isEqualTo(false);
        assertThat(JiraAggHelperService.isReprocessingNeeded(DbJiraIssue.builder().configVersion( 456L).build(), 123L)).isEqualTo(false);
    }

    @Test
    public void testJiraVersionAggregation() throws IOException, ExecutionException, SQLException, InventoryException {
        JiraIssue issue = ResourceUtils.getResourceAsObject("jira/jira_issue.json", JiraIssue.class);
        JiraIssueParser.JiraParserConfig parserConfig = JiraIssueParser.JiraParserConfig.builder().build();
        JobDTO jobDTO = JobDTO.builder().query(Map.of("from", 123, "to", 456)).build();

        LoadingCache<String, Optional<String>> emptyCache = Mockito.mock(LoadingCache.class);
        when(emptyCache.get(anyString())).thenReturn(Optional.empty());
        when(userIdentityService.upsertIgnoreEmail(anyString(), any())).thenReturn(null);
        when(inventoryService.listConfigs(anyString(), any(), eq(0), eq(1)))
                .thenReturn(DbListResponse.of(List.of(
                        IntegrationConfig.builder()
                                .metadata(IntegrationConfig.Metadata.builder().configUpdatedAt(123L).build())
                                .build()), 1));

        when(jiraIssueService.list(anyString(), any(), any(), any(), any(), any(), any())).thenReturn(DbListResponse.of(List.of(), 0));
        long currentConfigVersion = 123L;
        Date fetchTime = new Date(1000L);

        ProcessingStatus output = jiraAggHelperService.processJiraIssue("test", "1", jobDTO, issue, fetchTime, parserConfig, currentConfigVersion, false, null, null, null, emptyCache, emptyCache, emptyCache, null);
        assertThat(output.isSuccess()).isTrue();
        assertThat(output.isShouldInsert()).isTrue();
        verify(jiraIssueService, never()).insertJiraVersion(anyString(), any());
    }
}