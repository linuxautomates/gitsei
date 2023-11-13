package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.NotificationMode;
import io.levelops.commons.databases.models.database.Product;
import io.levelops.commons.databases.models.database.State;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.WorkItem;
import io.levelops.commons.databases.models.database.WorkItemNotification;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class WorkItemNotificationsDatabaseServiceTest {
    public static ObjectMapper MAPPER = DefaultObjectMapper.get();
    private static final String RECIPIENT = "recipient-";
    private static final String PERMA_LINK = "https://levelopsworkspace.slack.com/archives/G01C14VKRJP/p160259215700010";

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private String company = "test";

    private DataSource dataSource;
    private static UserService userService;
    private static ProductService productService;
    private static StateDBService stateDBService;
    private static IntegrationService integrationService;
    private static BestPracticesService bestPracticesService;
    private static QuestionnaireTemplateDBService questionnaireTemplateDBService;
    private static TicketTemplateDBService ticketTemplateDBService;
    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private static CiCdJobRunStageDatabaseService jobRunStageDatabaseService;
    private static WorkItemDBService workItemDBService;
    private static WorkItemNotificationsDatabaseService workItemNotificationsDatabaseService;
    private static UserIdentityService userIdentityService;

    @Before
    public void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);
        userService = new UserService(dataSource, DefaultObjectMapper.get());
        productService = new ProductService(dataSource);
        stateDBService = new StateDBService(dataSource);
        bestPracticesService = new BestPracticesService(dataSource);
        questionnaireTemplateDBService = new QuestionnaireTemplateDBService(dataSource);
        ticketTemplateDBService = new TicketTemplateDBService(dataSource, MAPPER);
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        ciCdJobRunsDatabaseService = new CiCdJobRunsDatabaseService(DefaultObjectMapper.get(), dataSource);
        jobRunStageDatabaseService = new CiCdJobRunStageDatabaseService(dataSource, MAPPER);
        workItemDBService = new WorkItemDBService(dataSource, MAPPER, productService, stateDBService, false, 1000);
        workItemNotificationsDatabaseService = new WorkItemNotificationsDatabaseService(dataSource);


        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        userService.ensureTableExistence(company);
        productService.ensureTableExistence(company);
        stateDBService.ensureTableExistence(company);
        bestPracticesService.ensureTableExistence(company);
        questionnaireTemplateDBService.ensureTableExistence(company);
        ticketTemplateDBService.ensureTableExistence(company);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        ciCdJobRunsDatabaseService.ensureTableExistence(company);
        jobRunStageDatabaseService.ensureTableExistence(company);
        workItemDBService.ensureTableExistence(company);
        workItemNotificationsDatabaseService.ensureTableExistence(company);
    }

    private void verifyRecord(WorkItemNotification a, WorkItemNotification e){
        Assert.assertEquals(a.getId(), e.getId());
        Assert.assertEquals(a.getWorkItemId(), e.getWorkItemId());
        Assert.assertEquals(a.getMode(), e.getMode());
        Assert.assertEquals(a.getReferenceId(), e.getReferenceId());
        Assert.assertEquals(a.getChannelId(), e.getChannelId());
        Assert.assertEquals(a.getRecipient(), e.getRecipient());
        Assert.assertEquals(a.getUrl(), e.getUrl());
        Assert.assertNotNull(a.getCreatedAt());
    }
    private void verifyRecords(List<WorkItemNotification> a, List<WorkItemNotification> e){
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if(CollectionUtils.isEmpty(a)){
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<UUID, WorkItemNotification> actualMap = a.stream().collect(Collectors.toMap(WorkItemNotification::getId, x -> x));
        Map<UUID, WorkItemNotification> expectedMap = e.stream().collect(Collectors.toMap(WorkItemNotification::getId, x -> x));

        for(UUID key : actualMap.keySet()){
            verifyRecord(actualMap.get(key), expectedMap.get(key));
        }
    }

    private void testGet(WorkItemNotification workItemNotification) throws SQLException {
        Optional<WorkItemNotification> optional = workItemNotificationsDatabaseService.get(company, workItemNotification.getId().toString());
        Assert.assertTrue(optional.isPresent());
        verifyRecord(optional.get(), workItemNotification);
    }

    private void testGets(List<WorkItemNotification> workItemNotifications) throws SQLException {
        for(WorkItemNotification workItemNotification : workItemNotifications) {
            testGet(workItemNotification);
        }
    }

    private List<WorkItemNotification> testInserts(List<WorkItem> workItems) throws SQLException {
        List<WorkItemNotification> retVal = new ArrayList<>();
        for(int i=0; i< workItems.size(); i++) {
            WorkItem workItem = workItems.get(i);
            WorkItemNotification workItemNotification = WorkItemNotification.builder()
                    .workItemId(UUID.fromString(workItem.getId()))
                    .mode(NotificationMode.SLACK)
                    .referenceId(UUID.randomUUID().toString())
                    .channelId(UUID.randomUUID().toString())
                    .recipient(RECIPIENT + i)
                    .url(PERMA_LINK + i)
                    .build();
            String id = workItemNotificationsDatabaseService.insert(company, workItemNotification);
            Assert.assertNotNull(id);
            workItemNotification = workItemNotification.toBuilder().id(UUID.fromString(id)).build();
            testGet(workItemNotification);
            retVal.add(workItemNotification);
        }
        return retVal;
    }

    private void testList(List<WorkItemNotification> expected) throws SQLException {
        DbListResponse<WorkItemNotification> dbListResponse = workItemNotificationsDatabaseService.list(company, 0, 300);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(expected.size(), dbListResponse.getCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getRecords().size());
        verifyRecords(dbListResponse.getRecords(), expected);
    }
    private void testListByFilterByIds(List<WorkItemNotification> expected) throws SQLException {
        for(WorkItemNotification workItemNotification : expected) {
            DbListResponse<WorkItemNotification> dbListResponse = workItemNotificationsDatabaseService.listByFilter(company, 0, 300, List.of(workItemNotification.getId()), null, null, null, null);
            Assert.assertNotNull(dbListResponse);
            Assert.assertEquals(1, dbListResponse.getCount().intValue());
            Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
            Assert.assertEquals(1, dbListResponse.getRecords().size());
            verifyRecord(dbListResponse.getRecords().get(0), workItemNotification);
        }
        List<UUID> ids = expected.stream().map(WorkItemNotification::getId).collect(Collectors.toList());
        DbListResponse<WorkItemNotification> dbListResponse = workItemNotificationsDatabaseService.listByFilter(company, 0, 300, ids, null, null, null, null);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(expected.size(), dbListResponse.getCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getRecords().size());
        verifyRecords(dbListResponse.getRecords(), expected);
    }
    private void testListByFilterByWorkItemIds(List<WorkItemNotification> expected) throws SQLException {
        for(WorkItemNotification workItemNotification : expected) {
            DbListResponse<WorkItemNotification> dbListResponse = workItemNotificationsDatabaseService.listByFilter(company, 0, 300, null, List.of(workItemNotification.getWorkItemId()), null, null, null);
            Assert.assertNotNull(dbListResponse);
            Assert.assertEquals(1, dbListResponse.getCount().intValue());
            Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
            Assert.assertEquals(1, dbListResponse.getRecords().size());
            verifyRecord(dbListResponse.getRecords().get(0), workItemNotification);
        }
        List<UUID> workItemIds = expected.stream().map(WorkItemNotification::getWorkItemId).collect(Collectors.toList());
        DbListResponse<WorkItemNotification> dbListResponse = workItemNotificationsDatabaseService.listByFilter(company, 0, 300, null, workItemIds, null, null, null);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(expected.size(), dbListResponse.getCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getRecords().size());
        verifyRecords(dbListResponse.getRecords(), expected);
    }
    private void testListByFilterByReferenceIds(List<WorkItemNotification> expected) throws SQLException {
        for(WorkItemNotification workItemNotification : expected) {
            DbListResponse<WorkItemNotification> dbListResponse = workItemNotificationsDatabaseService.listByFilter(company, 0, 300, null, null, List.of(workItemNotification.getReferenceId()), null, null);
            Assert.assertNotNull(dbListResponse);
            Assert.assertEquals(1, dbListResponse.getCount().intValue());
            Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
            Assert.assertEquals(1, dbListResponse.getRecords().size());
            verifyRecord(dbListResponse.getRecords().get(0), workItemNotification);
        }
        List<String> referenceIds = expected.stream().map(WorkItemNotification::getReferenceId).collect(Collectors.toList());
        DbListResponse<WorkItemNotification> dbListResponse = workItemNotificationsDatabaseService.listByFilter(company, 0, 300, null, null, referenceIds, null, null);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(expected.size(), dbListResponse.getCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getRecords().size());
        verifyRecords(dbListResponse.getRecords(), expected);
    }

    private void testListByFilterByChannelIds(List<WorkItemNotification> expected) throws SQLException {
        for(WorkItemNotification workItemNotification : expected) {
            DbListResponse<WorkItemNotification> dbListResponse = workItemNotificationsDatabaseService.listByFilter(company, 0, 300, null, null, null, List.of(workItemNotification.getChannelId()), null);
            Assert.assertNotNull(dbListResponse);
            Assert.assertEquals(1, dbListResponse.getCount().intValue());
            Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
            Assert.assertEquals(1, dbListResponse.getRecords().size());
            verifyRecord(dbListResponse.getRecords().get(0), workItemNotification);
        }
        List<String> channelIds = expected.stream().map(WorkItemNotification::getChannelId).collect(Collectors.toList());
        DbListResponse<WorkItemNotification> dbListResponse = workItemNotificationsDatabaseService.listByFilter(company, 0, 300, null, null, null, channelIds, null);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(expected.size(), dbListResponse.getCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getRecords().size());
        verifyRecords(dbListResponse.getRecords(), expected);
    }

    private void testListByFilterByModes(List<WorkItemNotification> expected) throws SQLException {
        DbListResponse<WorkItemNotification> dbListResponse = workItemNotificationsDatabaseService.listByFilter(company, 0, 300, null, null, null, null, List.of(expected.get(0).getMode().toString()));
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(expected.size(), dbListResponse.getCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getRecords().size());
        verifyRecords(dbListResponse.getRecords(), expected);
    }

    private void testListByFilterByReferenceIdChannelIdAndMode(List<WorkItemNotification> expected) throws SQLException {
        for(WorkItemNotification workItemNotification : expected) {
            DbListResponse<WorkItemNotification> dbListResponse = workItemNotificationsDatabaseService.listByFilter(company, 0, 300, null, null, List.of(workItemNotification.getReferenceId()),List.of(workItemNotification.getChannelId()), List.of(workItemNotification.getMode().toString()));
            Assert.assertNotNull(dbListResponse);
            Assert.assertEquals(1, dbListResponse.getCount().intValue());
            Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
            Assert.assertEquals(1, dbListResponse.getRecords().size());
            verifyRecord(dbListResponse.getRecords().get(0), workItemNotification);
        }
        List<String> referenceIds = expected.stream().map(WorkItemNotification::getReferenceId).collect(Collectors.toList());
        List<String> channelIds = expected.stream().map(WorkItemNotification::getChannelId).collect(Collectors.toList());
        DbListResponse<WorkItemNotification> dbListResponse = workItemNotificationsDatabaseService.listByFilter(company, 0, 300, null, null, referenceIds, channelIds, List.of(expected.get(0).getMode().toString()));
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(expected.size(), dbListResponse.getCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getRecords().size());
        verifyRecords(dbListResponse.getRecords(), expected);
    }

    private void testAllLists(List<WorkItemNotification> expected) throws SQLException {
        testList(expected);
        testListByFilterByIds(expected);
        testListByFilterByWorkItemIds(expected);
        testListByFilterByReferenceIds(expected);
        testListByFilterByChannelIds(expected);
        testListByFilterByModes(expected);
        testListByFilterByReferenceIdChannelIdAndMode(expected);
    }

    private List<WorkItemNotification> testUpdate(List<WorkItemNotification> expected) throws SQLException {
        List<WorkItemNotification> updated = new ArrayList<>();
        for(int i=0; i< expected.size(); i++) {
            WorkItemNotification current = expected.get(i);
            current = current.toBuilder().referenceId(UUID.randomUUID().toString()).mode(NotificationMode.EMAIL).recipient(RECIPIENT + i + i).url(PERMA_LINK + i + i).build();
            Boolean success = workItemNotificationsDatabaseService.update(company, current);
            Assert.assertTrue(success);
            testGet(current);
            updated.add(current);
        }
        return updated;
    }

    private void testValidDelete(List<WorkItemNotification> expected) throws SQLException {
        for(int i=0; i< expected.size(); i++){
            WorkItemNotification current = expected.get(0);
            Boolean success = workItemNotificationsDatabaseService.delete(company, current.getId().toString());
            Assert.assertTrue(success);
            expected.remove(0);
            testAllLists(expected);
        }
        testAllLists(expected);
    }

    private void testInvalidDelete() throws SQLException {
        Boolean success = workItemNotificationsDatabaseService.delete(company, UUID.randomUUID().toString());
        Assert.assertFalse(success);
    }

    private void testDelete(List<WorkItemNotification> expected) throws SQLException {
        testInvalidDelete();
        testValidDelete(expected);
    }

    @Test
    public void test() throws SQLException {
        User user = UserUtils.createUser(userService, company, 0);
        Product product = ProductUtils.createProduct(productService, company, 0, user);
        State state = StateUtils.createOpenState(stateDBService, company);

        int n = 5;
        List<WorkItem> workItems = WorkItemTestUtils.createWorkItems(workItemDBService, company, n, product.getId(), state, user.getId());
        List<WorkItemNotification> expected = testInserts(workItems);
        testGets(expected);
        testAllLists(expected);

        expected = testUpdate(expected);
        testGets(expected);
        testAllLists(expected);

        testDelete(expected);
    }
}