package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.NotificationMode;
import io.levelops.commons.databases.models.database.Questionnaire;
import io.levelops.commons.databases.models.database.QuestionnaireNotification;
import io.levelops.commons.databases.models.database.QuestionnaireTemplate;
import io.levelops.commons.databases.models.database.Severity;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class QuestionnaireNotificationsDatabaseServiceTest {
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
    private static QuestionnaireDBService questionnaireDBService;
    private static QuestionnaireNotificationsDatabaseService questionnaireNotificationsDatabaseService;

    @Before
    public void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        userService = new UserService(dataSource, DefaultObjectMapper.get());
        productService = new ProductService(dataSource);
        stateDBService = new StateDBService(dataSource);
        integrationService = new IntegrationService(dataSource);
        bestPracticesService = new BestPracticesService(dataSource);
        questionnaireTemplateDBService = new QuestionnaireTemplateDBService(dataSource);
        ticketTemplateDBService = new TicketTemplateDBService(dataSource, MAPPER);
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        ciCdJobRunsDatabaseService = new CiCdJobRunsDatabaseService(DefaultObjectMapper.get(), dataSource);
        jobRunStageDatabaseService = new CiCdJobRunStageDatabaseService(dataSource, MAPPER);
        workItemDBService = new WorkItemDBService(dataSource, MAPPER, productService, stateDBService, false, 1000);
        questionnaireDBService = new QuestionnaireDBService(dataSource);
        questionnaireNotificationsDatabaseService = new QuestionnaireNotificationsDatabaseService(dataSource);


        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        userService.ensureTableExistence(company);
        integrationService.ensureTableExistence(company);
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
        questionnaireDBService.ensureTableExistence(company);
        questionnaireNotificationsDatabaseService.ensureTableExistence(company);
    }

    private void verifyRecord(QuestionnaireNotification a, QuestionnaireNotification e){
        Assert.assertEquals(a.getId(), e.getId());
        Assert.assertEquals(a.getQuestionnaireId(), e.getQuestionnaireId());
        Assert.assertEquals(a.getMode(), e.getMode());
        Assert.assertEquals(a.getReferenceId(), e.getReferenceId());
        Assert.assertEquals(a.getChannelId(), e.getChannelId());
        Assert.assertEquals(a.getRecipient(), e.getRecipient());
        Assert.assertEquals(a.getUrl(), e.getUrl());
        Assert.assertNotNull(a.getCreatedAt());
    }
    private void verifyRecords(List<QuestionnaireNotification> a, List<QuestionnaireNotification> e){
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if(CollectionUtils.isEmpty(a)){
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<UUID, QuestionnaireNotification> actualMap = a.stream().collect(Collectors.toMap(QuestionnaireNotification::getId, x -> x));
        Map<UUID, QuestionnaireNotification> expectedMap = e.stream().collect(Collectors.toMap(QuestionnaireNotification::getId, x -> x));

        for(UUID key : actualMap.keySet()){
            verifyRecord(actualMap.get(key), expectedMap.get(key));
        }
    }

    private void testGet(QuestionnaireNotification questionnaireNotification) throws SQLException {
        Optional<QuestionnaireNotification> optional = questionnaireNotificationsDatabaseService.get(company, questionnaireNotification.getId().toString());
        Assert.assertTrue(optional.isPresent());
        verifyRecord(optional.get(), questionnaireNotification);
    }

    private void testGets(List<QuestionnaireNotification> questionnaireNotifications) throws SQLException {
        for(QuestionnaireNotification questionnaireNotification : questionnaireNotifications) {
            testGet(questionnaireNotification);
        }
    }

    private List<QuestionnaireNotification> testInserts(List<Questionnaire> questionnaires) throws SQLException {
        List<QuestionnaireNotification> retVal = new ArrayList<>();
        for(int i=0; i< questionnaires.size(); i++) {
            Questionnaire questionnaire = questionnaires.get(i);
            QuestionnaireNotification questionnaireNotification = QuestionnaireNotification.builder()
                    .questionnaireId(UUID.fromString(questionnaire.getId()))
                    .mode(NotificationMode.SLACK)
                    .referenceId(UUID.randomUUID().toString())
                    .channelId(UUID.randomUUID().toString())
                    .recipient(RECIPIENT + i)
                    .url(PERMA_LINK + i)
                    .build();
            String id = questionnaireNotificationsDatabaseService.insert(company, questionnaireNotification);
            Assert.assertNotNull(id);
            questionnaireNotification = questionnaireNotification.toBuilder().id(UUID.fromString(id)).build();
            testGet(questionnaireNotification);
            retVal.add(questionnaireNotification);
        }
        return retVal;
    }

    private void testList(List<QuestionnaireNotification> expected) throws SQLException {
        DbListResponse<QuestionnaireNotification> dbListResponse = questionnaireNotificationsDatabaseService.list(company, 0, 300);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(expected.size(), dbListResponse.getCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getRecords().size());
        verifyRecords(dbListResponse.getRecords(), expected);
    }
    private void testListByFilterByIds(List<QuestionnaireNotification> expected) throws SQLException {
        for(QuestionnaireNotification questionnaireNotification : expected) {
            DbListResponse<QuestionnaireNotification> dbListResponse = questionnaireNotificationsDatabaseService.listByFilter(company, 0, 300, List.of(questionnaireNotification.getId()), null, null, null, null);
            Assert.assertNotNull(dbListResponse);
            Assert.assertEquals(1, dbListResponse.getCount().intValue());
            Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
            Assert.assertEquals(1, dbListResponse.getRecords().size());
            verifyRecord(dbListResponse.getRecords().get(0), questionnaireNotification);
        }
        List<UUID> ids = expected.stream().map(QuestionnaireNotification::getId).collect(Collectors.toList());
        DbListResponse<QuestionnaireNotification> dbListResponse = questionnaireNotificationsDatabaseService.listByFilter(company, 0, 300, ids, null, null, null, null);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(expected.size(), dbListResponse.getCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getRecords().size());
        verifyRecords(dbListResponse.getRecords(), expected);
    }
    private void testListByFilterByQuestionnaireIds(List<QuestionnaireNotification> expected) throws SQLException {
        for(QuestionnaireNotification questionnaireNotification : expected) {
            DbListResponse<QuestionnaireNotification> dbListResponse = questionnaireNotificationsDatabaseService.listByFilter(company, 0, 300, null, List.of(questionnaireNotification.getQuestionnaireId()), null, null, null);
            Assert.assertNotNull(dbListResponse);
            Assert.assertEquals(1, dbListResponse.getCount().intValue());
            Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
            Assert.assertEquals(1, dbListResponse.getRecords().size());
            verifyRecord(dbListResponse.getRecords().get(0), questionnaireNotification);
        }
        List<UUID> questionnaireIds = expected.stream().map(QuestionnaireNotification::getQuestionnaireId).collect(Collectors.toList());
        DbListResponse<QuestionnaireNotification> dbListResponse = questionnaireNotificationsDatabaseService.listByFilter(company, 0, 300, null, questionnaireIds, null, null, null);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(expected.size(), dbListResponse.getCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getRecords().size());
        verifyRecords(dbListResponse.getRecords(), expected);
    }
    private void testListByFilterByReferenceIds(List<QuestionnaireNotification> expected) throws SQLException {
        for(QuestionnaireNotification questionnaireNotification : expected) {
            DbListResponse<QuestionnaireNotification> dbListResponse = questionnaireNotificationsDatabaseService.listByFilter(company, 0, 300, null, null, List.of(questionnaireNotification.getReferenceId()), null, null);
            Assert.assertNotNull(dbListResponse);
            Assert.assertEquals(1, dbListResponse.getCount().intValue());
            Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
            Assert.assertEquals(1, dbListResponse.getRecords().size());
            verifyRecord(dbListResponse.getRecords().get(0), questionnaireNotification);
        }
        List<String> referenceIds = expected.stream().map(QuestionnaireNotification::getReferenceId).collect(Collectors.toList());
        DbListResponse<QuestionnaireNotification> dbListResponse = questionnaireNotificationsDatabaseService.listByFilter(company, 0, 300, null, null, referenceIds, null, null);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(expected.size(), dbListResponse.getCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getRecords().size());
        verifyRecords(dbListResponse.getRecords(), expected);
    }

    private void testListByFilterByChannelIds(List<QuestionnaireNotification> expected) throws SQLException {
        for(QuestionnaireNotification questionnaireNotification : expected) {
            DbListResponse<QuestionnaireNotification> dbListResponse = questionnaireNotificationsDatabaseService.listByFilter(company, 0, 300, null, null, null, List.of(questionnaireNotification.getChannelId()), null);
            Assert.assertNotNull(dbListResponse);
            Assert.assertEquals(1, dbListResponse.getCount().intValue());
            Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
            Assert.assertEquals(1, dbListResponse.getRecords().size());
            verifyRecord(dbListResponse.getRecords().get(0), questionnaireNotification);
        }
        List<String> channelIds = expected.stream().map(QuestionnaireNotification::getChannelId).collect(Collectors.toList());
        DbListResponse<QuestionnaireNotification> dbListResponse = questionnaireNotificationsDatabaseService.listByFilter(company, 0, 300, null, null, null, channelIds, null);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(expected.size(), dbListResponse.getCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getRecords().size());
        verifyRecords(dbListResponse.getRecords(), expected);
    }

    private void testListByFilterByModes(List<QuestionnaireNotification> expected) throws SQLException {
        DbListResponse<QuestionnaireNotification> dbListResponse = questionnaireNotificationsDatabaseService.listByFilter(company, 0, 300, null, null, null, null, List.of(expected.get(0).getMode().toString()));
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(expected.size(), dbListResponse.getCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getRecords().size());
        verifyRecords(dbListResponse.getRecords(), expected);
    }

    private void testListByFilterByReferenceIdChannelIdAndMode(List<QuestionnaireNotification> expected) throws SQLException {
        for(QuestionnaireNotification questionnaireNotification : expected) {
            DbListResponse<QuestionnaireNotification> dbListResponse = questionnaireNotificationsDatabaseService.listByFilter(company, 0, 300, null, null, List.of(questionnaireNotification.getReferenceId()),List.of(questionnaireNotification.getChannelId()), List.of(questionnaireNotification.getMode().toString()));
            Assert.assertNotNull(dbListResponse);
            Assert.assertEquals(1, dbListResponse.getCount().intValue());
            Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
            Assert.assertEquals(1, dbListResponse.getRecords().size());
            verifyRecord(dbListResponse.getRecords().get(0), questionnaireNotification);
        }
        List<String> referenceIds = expected.stream().map(QuestionnaireNotification::getReferenceId).collect(Collectors.toList());
        List<String> channelIds = expected.stream().map(QuestionnaireNotification::getChannelId).collect(Collectors.toList());
        DbListResponse<QuestionnaireNotification> dbListResponse = questionnaireNotificationsDatabaseService.listByFilter(company, 0, 300, null, null, referenceIds, channelIds, List.of(expected.get(0).getMode().toString()));
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(expected.size(), dbListResponse.getCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), dbListResponse.getRecords().size());
        verifyRecords(dbListResponse.getRecords(), expected);
    }

    private void testAllLists(List<QuestionnaireNotification> expected) throws SQLException {
        testList(expected);
        testListByFilterByIds(expected);
        testListByFilterByQuestionnaireIds(expected);
        testListByFilterByReferenceIds(expected);
        testListByFilterByChannelIds(expected);
        testListByFilterByModes(expected);
        testListByFilterByReferenceIdChannelIdAndMode(expected);
    }

    private List<QuestionnaireNotification> testUpdate(List<QuestionnaireNotification> expected) throws SQLException {
        List<QuestionnaireNotification> updated = new ArrayList<>();
        for(int i=0; i< expected.size(); i++) {
            QuestionnaireNotification current = expected.get(i);
            current = current.toBuilder().referenceId(UUID.randomUUID().toString()).mode(NotificationMode.EMAIL).recipient(RECIPIENT + i + i).url(PERMA_LINK + i + i).build();
            Boolean success = questionnaireNotificationsDatabaseService.update(company, current);
            Assert.assertTrue(success);
            testGet(current);
            updated.add(current);
        }
        return updated;
    }

    private void testValidDelete(List<QuestionnaireNotification> expected) throws SQLException {
        for(int i=0; i< expected.size(); i++){
            QuestionnaireNotification current = expected.get(0);
            Boolean success = questionnaireNotificationsDatabaseService.delete(company, current.getId().toString());
            Assert.assertTrue(success);
            expected.remove(0);
            testAllLists(expected);
        }
        testAllLists(expected);
    }

    private void testInvalidDelete() throws SQLException {
        Boolean success = questionnaireNotificationsDatabaseService.delete(company, UUID.randomUUID().toString());
        Assert.assertFalse(success);
    }

    private void testDelete(List<QuestionnaireNotification> expected) throws SQLException {
        testInvalidDelete();
        testValidDelete(expected);
    }

    @Test
    public void test() throws SQLException {
        int n = 5;
        List<QuestionnaireTemplate> qTemplates = QuestionnaireTemplateTestUtils.createQuestionnaireTemplates(questionnaireTemplateDBService, company, n, Collections.emptyList());
        List<Questionnaire> questionnaires = QuestionnaireTestUtils.createQuestionnaires(questionnaireDBService, company, null, qTemplates, null, Severity.HIGH);
        List<QuestionnaireNotification> expected = testInserts(questionnaires);
        testGets(expected);
        testAllLists(expected);

        expected = testUpdate(expected);
        testGets(expected);
        testAllLists(expected);

        testDelete(expected);
    }
}