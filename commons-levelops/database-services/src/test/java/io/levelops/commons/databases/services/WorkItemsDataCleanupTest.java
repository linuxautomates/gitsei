package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.azureDevops.models.WorkItemField;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import junit.framework.TestCase;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
public class WorkItemsDataCleanupTest {
    public static final String company = "test";
    private static WorkItemsService workItemService;
    private static WorkItemsReportService workItemsReportService;
    private static WorkItemsStageTimesReportService workItemsStageTimesReportService;
    private static WorkItemsAgeReportService workItemsAgeReportService;
    private static WorkItemsResolutionTimeReportService workItemsResolutionTimeReportService;
    private static WorkItemsResponseTimeReportService workItemsResponseTimeReportService;
    private static WorkItemsPrioritySLAService workItemsPrioritySLAService;
    private static IntegrationService integrationService;
    private static DataSource dataSource;
    private static final ObjectMapper m = DefaultObjectMapper.get();
    private static List<DbWorkItem> dbWorkItems = new ArrayList<>();
    private static Date currentTime;
    private static Long ingestedAt;
    private static UserIdentityService userIdentityService;

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        WorkItemFieldsMetaService workItemFieldsMetaService = new WorkItemFieldsMetaService(dataSource);
        workItemsReportService = new WorkItemsReportService(dataSource, workItemFieldsMetaService);
        workItemsStageTimesReportService = new WorkItemsStageTimesReportService(dataSource, workItemFieldsMetaService);
        integrationService = new IntegrationService(dataSource);
        workItemsAgeReportService = new WorkItemsAgeReportService(dataSource, workItemFieldsMetaService);
        workItemsResolutionTimeReportService = new WorkItemsResolutionTimeReportService(dataSource, workItemFieldsMetaService);
        workItemsResponseTimeReportService = new WorkItemsResponseTimeReportService(dataSource, workItemFieldsMetaService);
        workItemsPrioritySLAService = new WorkItemsPrioritySLAService(dataSource);
        WorkItemsMetadataService workItemsMetadataService = new WorkItemsMetadataService(dataSource);
        WorkItemsBouncesReportService workItemsBouncesReportService = new WorkItemsBouncesReportService(dataSource, workItemFieldsMetaService);
        WorkItemsHopsReportService workItemsHopsReportService = new WorkItemsHopsReportService(dataSource, workItemFieldsMetaService);
        WorkItemsFirstAssigneeReportService workItemsFirstAssigneeReportService = new WorkItemsFirstAssigneeReportService(dataSource, workItemFieldsMetaService);
        workItemService = new WorkItemsService(dataSource, workItemsReportService, workItemsStageTimesReportService,
                workItemsAgeReportService, workItemsResolutionTimeReportService, workItemsResponseTimeReportService,
                null, null, workItemsPrioritySLAService,
                workItemsBouncesReportService, workItemsHopsReportService, workItemsFirstAssigneeReportService, workItemFieldsMetaService, null);
        integrationService.ensureTableExistence(company);
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);
        workItemService.ensureTableExistence(company);
        workItemsPrioritySLAService.ensureTableExistence(company);
        workItemsMetadataService.ensureTableExistence(company);
        WorkItemTimelineService workItemTimelineService = new WorkItemTimelineService(dataSource);
        workItemTimelineService.ensureTableExistence(company);

        integrationService.insert(company, Integration.builder()
                .application("issue_mgmt")
                .name("issue mgmt test1")
                .status("enabled")
                .build());
    }

    @Test
    public void testCleanUpData() throws SQLException {
        Long ingestedMoreThan90DaysBackOn1st = LocalDate.now().minusDays(100).with(ChronoField.DAY_OF_MONTH, 1)
                .toEpochSecond(LocalTime.NOON, ZoneOffset.UTC);
        Long ingestedMoreThan90DaysBackOn2nd = LocalDate.now().minusDays(100).with(ChronoField.DAY_OF_MONTH, 2)
                .toEpochSecond(LocalTime.NOON, ZoneOffset.UTC);
        Long ingestedMoreThan90DaysBackOn3rd = LocalDate.now().minusDays(100).with(ChronoField.DAY_OF_MONTH, 3)
                .toEpochSecond(LocalTime.NOON, ZoneOffset.UTC);
        Long ingestedMoreThan90DaysBackOn4th = LocalDate.now().minusDays(100).with(ChronoField.DAY_OF_MONTH, 4)
                .toEpochSecond(LocalTime.NOON, ZoneOffset.UTC);
        Long ingestedToday = LocalDate.now()
                .toEpochSecond(LocalTime.NOON, ZoneOffset.UTC);
        Long ingestedYesterday = LocalDate.now().minusDays(1)
                .toEpochSecond(LocalTime.NOON, ZoneOffset.UTC);
        workItemService.insert(company, DbWorkItem.builder().ingestedAt(ingestedMoreThan90DaysBackOn2nd).integrationId("1").workItemId("LEV_999").project("test").summary("asc").descSize(12)
                .priority("high").reporter("as").status("qwe").workItemType("wer").hops(1).bounces(1).numAttachments(1).workItemCreatedAt(new Timestamp(1604385440L)).workItemUpdatedAt(new Timestamp(1604385440L)).build());
        workItemService.insert(company, DbWorkItem.builder().ingestedAt(ingestedMoreThan90DaysBackOn3rd).integrationId("1").workItemId("LEV_999").project("test").summary("asc").descSize(12)
                .priority("high").reporter("as").status("qwe").workItemType("wer").hops(1).bounces(1).numAttachments(1).workItemCreatedAt(new Timestamp(1604385440L)).workItemUpdatedAt(new Timestamp(1604385440L)).build());
        workItemService.insert(company, DbWorkItem.builder().ingestedAt(ingestedMoreThan90DaysBackOn4th).integrationId("1").workItemId("LEV_999").project("test").summary("asc").descSize(12)
                .priority("high").reporter("as").status("qwe").workItemType("wer").hops(1).bounces(1).numAttachments(1).workItemCreatedAt(new Timestamp(1604385440L)).workItemUpdatedAt(new Timestamp(1604385440L)).build());
        workItemService.insert(company, DbWorkItem.builder().ingestedAt(ingestedMoreThan90DaysBackOn1st).integrationId("1").workItemId("LEV_999").project("test").summary("asc").descSize(12)
                .priority("high").reporter("as").status("qwe").workItemType("wer").hops(1).bounces(1).numAttachments(1).workItemCreatedAt(new Timestamp(1604385440L)).workItemUpdatedAt(new Timestamp(1604385440L)).build());
        workItemService.insert(company, DbWorkItem.builder().ingestedAt(ingestedToday).integrationId("1").workItemId("LEV_999").project("test").summary("asc").descSize(12)
                .priority("high").reporter("as").status("qwe").workItemType("wer").hops(1).bounces(1).numAttachments(1).workItemCreatedAt(new Timestamp(1604385440L)).workItemUpdatedAt(new Timestamp(1604385440L)).build());
        workItemService.insert(company, DbWorkItem.builder().ingestedAt(ingestedYesterday).integrationId("1").workItemId("LEV_999").project("test").summary("asc").descSize(12)
                .priority("high").reporter("as").status("qwe").workItemType("wer").hops(1).bounces(1).numAttachments(1).workItemCreatedAt(new Timestamp(1604385440L)).workItemUpdatedAt(new Timestamp(1604385440L)).build());

        int deletedValues = workItemService.cleanUpOldData(company,
                new Date().toInstant().getEpochSecond(),
                86400 * 91L);
        AssertionsForClassTypes.assertThat(deletedValues).isEqualTo(3);
        AssertionsForClassTypes.assertThat(workItemService.get(company, "1", "LEV_999", ingestedMoreThan90DaysBackOn1st)).isNotNull();
        AssertionsForClassTypes.assertThat(workItemService.get(company, "1", "LEV_999", ingestedToday)).isNotNull();
        AssertionsForClassTypes.assertThat(workItemService.get(company, "1", "LEV_999", ingestedYesterday)).isNotNull();
    }
}