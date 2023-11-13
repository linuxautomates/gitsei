package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.BestPracticesItem;
import io.levelops.commons.databases.models.database.Questionnaire;
import io.levelops.commons.databases.models.database.Questionnaire.State;
import io.levelops.commons.databases.models.database.Severity;
import io.levelops.commons.databases.models.database.Tag;
import io.levelops.commons.databases.models.database.TicketTemplate;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.WorkItem;
import io.levelops.commons.databases.models.database.WorkItem.ItemType;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.models.database.mappings.TagItemMapping;
import io.levelops.commons.databases.models.filters.DateFilter;
import io.levelops.commons.databases.models.filters.QuestionnaireAggFilter;
import io.levelops.commons.databases.models.filters.QuestionnaireFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.QuestionnaireDetails;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.UUIDUtils;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.QuestionnaireTemplateTestUtils.buildQuestionnaireTemplate;
import static io.levelops.commons.databases.services.QuestionnaireTestUtils.buildQuestionnaire;
import static io.levelops.commons.databases.services.TicketTemplateUtils.buildTicketTemplate;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings({"unused", "deprecation"})
public class QuestionnaireDBServiceTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    private static final String company = "test";
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static QuestionnaireDBService questionnaireDBService;
    private static BestPracticesService bestPracticesService;
    private static QuestionnaireTemplateDBService questionnaireTemplateDBService;
    private static TagsService tagsService;
    private static TagItemDBService tagItemDBService;
    private static TicketTemplateDBService ticketTemplateDBService;
    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private static CiCdJobRunStageDatabaseService jobRunStageDatabaseService;
    private static ProductService productService;
    private static WorkItemDBService wItemService;
    private static String productId = null;
    private static Integer stateId = null;
    private static List<BestPracticesItem> kbs = null;
    private static List<UUID> kbIds = null;
    private static  UserService userService;
    private static final Integer pageNumber = 0;
    private static final Integer pageSize = 100;

    private static NamedParameterJdbcTemplate template;

    @BeforeClass
    public static void setup() throws SQLException {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        template = new NamedParameterJdbcTemplate(dataSource);
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        dataSource.getConnection().prepareStatement("DROP SCHEMA IF EXISTS " + company + " CASCADE").execute();

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        new UserService(dataSource, DefaultObjectMapper.get()).ensureTableExistence(company);
        new IntegrationService(dataSource).ensureTableExistence(company);
        new ProductService(dataSource).ensureTableExistence(company);
        new TagsService(dataSource).ensureTableExistence(company);
        new TagItemDBService(dataSource).ensureTableExistence(company);
        new OrganizationService(dataSource).ensureTableExistence(company);

        bestPracticesService = new BestPracticesService(dataSource);
        questionnaireTemplateDBService = new QuestionnaireTemplateDBService(dataSource);
        tagsService = new TagsService(dataSource);
        tagItemDBService = new TagItemDBService(dataSource);
        ticketTemplateDBService = new TicketTemplateDBService(dataSource, MAPPER);
        StateDBService stateDBService = new StateDBService(dataSource);
        productService = new ProductService(dataSource);
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        ciCdJobRunsDatabaseService = new CiCdJobRunsDatabaseService(DefaultObjectMapper.get(), dataSource);
        jobRunStageDatabaseService = new CiCdJobRunStageDatabaseService(dataSource, MAPPER);
        wItemService = new WorkItemDBService(dataSource, MAPPER, productService, stateDBService, true, 100);
        questionnaireDBService = new QuestionnaireDBService(dataSource);
        userService = new UserService(dataSource, DefaultObjectMapper.get());

        bestPracticesService.ensureTableExistence(company);
        questionnaireTemplateDBService.ensureTableExistence(company);
        tagsService.ensureTableExistence(company);
        tagItemDBService.ensureTableExistence(company);
        ticketTemplateDBService.ensureTableExistence(company);
        stateDBService.ensureTableExistence(company);
        productService.ensureTableExistence(company);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        ciCdJobRunsDatabaseService.ensureTableExistence(company);
        jobRunStageDatabaseService.ensureTableExistence(company);
        wItemService.ensureTableExistence(company);

        questionnaireDBService.ensureTableExistence(company);
        productId = ProductUtils.createProducts(productService, company, 1).get(0).getId();
        stateId = stateDBService.getStateByName(company, WorkItem.ItemStatus.OPEN.toString()).getId();
        kbs = BestPracticesServiceUtils.createBestPracticesItems(bestPracticesService, company, 2);
        kbIds = kbs.stream().map(x -> x.getId()).collect(Collectors.toList());
    }

    @Before
    public void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("DELETE FROM " + company + ".questionnaires").execute();
        dataSource.getConnection().prepareStatement("DELETE FROM " + company + ".users").execute();
        dataSource.getConnection().prepareStatement("DELETE FROM " + company + ".tags").execute();
        dataSource.getConnection().prepareStatement("DELETE FROM " + company + ".questionnaire_bpracticesitem_mappings").execute();
        dataSource.getConnection().prepareStatement("DELETE FROM " + company + ".workitems").execute();
        dataSource.getConnection().prepareStatement("DELETE FROM " + company + ".tagitems").execute();
//        dataSource.getConnection().prepareStatement("DELETE FROM " + company + ".questionnaire_templates").execute();
    }

    @Test
    public void testCreate() throws SQLException {
        String qTemplateId = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));

        String wItemId = wItemService.insert(company, WorkItem.builder()
                .type(ItemType.AUTOMATED)
                .ticketType(WorkItem.TicketType.WORK_ITEM)
                .productId(productId)
                .stateId(stateId)
                .build());

        String id = questionnaireDBService.insert(company, buildQuestionnaire(1, wItemId, qTemplateId, kbIds));

        var questionnaire = questionnaireDBService.get(company, id).get();
        Assert.assertEquals(id, questionnaire.getId());
        Assert.assertEquals("bucketname1", questionnaire.getBucketName());
        Assert.assertEquals("bucketpath1", questionnaire.getBucketPath());
        Assert.assertEquals(State.CREATED, questionnaire.getState());
    }

    private void verifyQuestionnaire(Questionnaire a, Questionnaire e) {
        Assert.assertEquals(a.getId(), e.getId());
        Assert.assertEquals(a.getQuestionnaireTemplateId(), e.getQuestionnaireTemplateId());
        Assert.assertEquals(a.getWorkItemId(), e.getWorkItemId());
        Assert.assertEquals(a.getProductId(), e.getProductId());
        Assert.assertEquals(a.getAnswered(), e.getAnswered());
        Assert.assertEquals(a.getTotalQuestions(), e.getTotalQuestions());
        Assert.assertEquals(a.getTotalPossibleScore(), e.getTotalPossibleScore());
        Assert.assertEquals(a.getTargetEmail(), e.getTargetEmail());
        Assert.assertEquals(a.getSenderEmail(), e.getSenderEmail());
        Assert.assertEquals(a.getBucketName(), e.getBucketName());
        Assert.assertEquals(a.getBucketPath(), e.getBucketPath());
        Assert.assertEquals(a.getScore(), e.getScore());
        Assert.assertEquals(a.getPriority(), e.getPriority());
        Assert.assertEquals(a.getState(), e.getState());
        Assert.assertEquals(a.getMain(), e.getMain());
        Assert.assertEquals(a.getCompletedAt(), e.getCompletedAt());
        Assert.assertNotNull(e.getUpdatedAt());
        Assert.assertNotNull(e.getCreatedAt());
        Assert.assertEquals(a.getMessageSent(), e.getMessageSent());
        Assert.assertEquals(CollectionUtils.isEmpty(e.getKbIds()), CollectionUtils.isEmpty(a.getKbIds()));
        if (CollectionUtils.isNotEmpty(e.getKbIds())) {
            Assert.assertEquals(e.getKbIds(), a.getKbIds());
        }
    }

    @Test
    public void testGet() throws SQLException {
        String qTemplateId = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String wItemId = wItemService.insert(company, WorkItem.builder()
                .type(ItemType.AUTOMATED)
                .ticketType(WorkItem.TicketType.WORK_ITEM)
                .productId(productId)
                .stateId(stateId)
                .notify(false)
                .build());
        Questionnaire expected = buildQuestionnaire(1, wItemId, qTemplateId, kbIds);
        String id = questionnaireDBService.insert(company, expected);
        expected = expected.toBuilder().id(id).build();
        // Verify record
        var actual = questionnaireDBService.get(company, id).get();
        verifyQuestionnaire(actual, expected);
    }

    @Test
    public void testUpdate() throws SQLException {
        String qTemplateId = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));

        String wItemId = wItemService.insert(company, WorkItem.builder()
                .type(ItemType.AUTOMATED)
                .ticketType(WorkItem.TicketType.WORK_ITEM)
                .productId(productId)
                .stateId(stateId)
                .build());

        String id = questionnaireDBService.insert(company, buildQuestionnaire(1, wItemId, qTemplateId, kbIds));

        // Update state
        questionnaireDBService.update(company, Questionnaire.builder().id(id).state(State.INCOMPLETE).build());
        var questionnaire = questionnaireDBService.get(company, id).get();
        Assert.assertEquals("sender1@test.test", questionnaire.getSenderEmail());
        Assert.assertEquals("target1@test.test", questionnaire.getTargetEmail());
        Assert.assertEquals(State.INCOMPLETE, questionnaire.getState());
        Assert.assertNull(questionnaire.getCompletedAt());

        // Sender email is not allowed to be updated so, if set, it gets ignored
        questionnaireDBService.update(company, Questionnaire.builder().id(id).senderEmail("sender2@test.test").build());
        questionnaire = questionnaireDBService.get(company, id).get();
        Assert.assertEquals("sender1@test.test", questionnaire.getSenderEmail());
        Assert.assertEquals("target1@test.test", questionnaire.getTargetEmail());
        Assert.assertEquals(State.INCOMPLETE, questionnaire.getState());
        Assert.assertNull(questionnaire.getCompletedAt());

        // Update target
        questionnaireDBService.update(company, Questionnaire.builder().id(id).targetEmail("target2@test.test").build());
        questionnaire = questionnaireDBService.get(company, id).get();
        Assert.assertEquals("sender1@test.test", questionnaire.getSenderEmail());
        Assert.assertEquals("target2@test.test", questionnaire.getTargetEmail());
        Assert.assertEquals(State.INCOMPLETE, questionnaire.getState());
        Assert.assertNull(questionnaire.getCompletedAt());

        // Update state
        questionnaireDBService.update(company, Questionnaire.builder().id(id).state(State.COMPLETED).build());
        questionnaire = questionnaireDBService.get(company, id).get();
        Assert.assertEquals("sender1@test.test", questionnaire.getSenderEmail());
        Assert.assertEquals("target2@test.test", questionnaire.getTargetEmail());
        Assert.assertEquals(State.COMPLETED, questionnaire.getState());
        Assert.assertNotNull(questionnaire.getCompletedAt());
    }

    @Test
    public void testGetDetails() throws SQLException {
        String qTemplateId = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));

        String wItemId = wItemService.insert(company, WorkItem.builder()
                .reason("reason1")
                .type(ItemType.AUTOMATED)
                .ticketType(WorkItem.TicketType.WORK_ITEM)
                .productId(productId)
                .stateId(stateId)
                .build());

        Questionnaire expected = buildQuestionnaire(1, wItemId, qTemplateId, kbIds);
        String id = questionnaireDBService.insert(company, expected);
        expected = expected.toBuilder().id(id).build();
        // Verify record
        var details = questionnaireDBService.getDetails(company, id).get();
        Assert.assertEquals("sender1@test.test", details.getSenderEmail());
        Assert.assertEquals("target1@test.test", details.getTargetEmail());
        Assert.assertEquals("reason1", details.getReason());
        Assert.assertEquals(State.CREATED, details.getState());
        Assert.assertNull(details.getCompletedAt());
        verifyQuestionnaire(details, expected);
    }

    @Test
    public void testList() throws SQLException {
        String qTemplateId = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String wItemId = wItemService.insert(company, WorkItem.builder()
                .reason("reason list1")
                .type(ItemType.AUTOMATED)
                .ticketType(WorkItem.TicketType.WORK_ITEM)
                .productId(productId)
                .stateId(stateId)
                .build());

        var questionnaires = questionnaireDBService.list(company, 0, 10);
        var initialCount = questionnaires.getCount();
        String id1 = questionnaireDBService.insert(company, buildQuestionnaire(1, wItemId, qTemplateId, kbIds));

        String id2 = questionnaireDBService.insert(company, buildQuestionnaire(2, wItemId, qTemplateId, kbIds));
        // Verify record
        questionnaires = questionnaireDBService.list(company, 0, 10);
        Assertions.assertThat(Integer.valueOf(2)).isEqualTo(questionnaires.getCount() - initialCount);
        questionnaires.getRecords().forEach(questionnaire -> {
            Assert.assertEquals(State.CREATED, questionnaire.getState());
            Assert.assertNull(questionnaire.getCompletedAt());
        });

        Assertions.assertThat(questionnaires.getRecords().stream().map(Questionnaire::getId).collect(Collectors.toSet()))
                .containsAll(Set.of(id1, id2));
        // Assert.assertEquals(Set.of(id1, id2), questionnaires.getRecords().stream().map(Questionnaire::getId).collect(Collectors.toSet()));
    }

    @Test
    public void testListByWorkItemId() throws SQLException {
        String qTemplateId = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String wItemId = wItemService.insert(company, WorkItem.builder()
                .reason("reason list2")
                .type(ItemType.AUTOMATED)
                .ticketType(WorkItem.TicketType.WORK_ITEM)
                .productId(productId)
                .stateId(stateId)
                .build());

        String id1 = questionnaireDBService.insert(company, buildQuestionnaire(1, wItemId, qTemplateId, kbIds));

        String id2 = questionnaireDBService.insert(company, buildQuestionnaire(2, wItemId, qTemplateId, kbIds));
        // Verify record
        var questionnaires = questionnaireDBService.listByWorkItemId(company, wItemId, 0, 10);
        Assert.assertEquals(Integer.valueOf(2), questionnaires.getCount());
        questionnaires.getRecords().forEach(questionnaire -> {
            Assert.assertEquals(State.CREATED, questionnaire.getState());
            Assert.assertNull(questionnaire.getCompletedAt());
        });
        Assert.assertEquals(Set.of(id1, id2), questionnaires.getRecords().stream().map(Questionnaire::getId).collect(Collectors.toSet()));
    }

    @Test
    public void testListByFilters() throws SQLException {
        String qTemplateId = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));

        String wItemId = wItemService.insert(company, WorkItem.builder()
                .reason("reason1")
                .type(ItemType.AUTOMATED)
                .ticketType(WorkItem.TicketType.WORK_ITEM)
                .productId(productId)
                .stateId(stateId)
                .build());
        var initalCount = questionnaireDBService.listByFilters(company, 0, 10, Map.of("priority", Severity.HIGH.getValue())).getCount();
        String id1 = questionnaireDBService.insert(company, buildQuestionnaire(101, wItemId, qTemplateId, kbIds));

        String id2 = questionnaireDBService.insert(company, buildQuestionnaire(102, wItemId, qTemplateId, kbIds));
        // Verify record
        Map<String, Object> filters = Map.of("bucketname", "bucketname102");
        var questionnaires = questionnaireDBService.listByFilters(company, 0, 10, filters);
        Assert.assertEquals(Integer.valueOf(1), questionnaires.getCount());
        questionnaires.getRecords().forEach(questionnaire -> {
            Assert.assertEquals(State.CREATED, questionnaire.getState());
            Assert.assertNull(questionnaire.getCompletedAt());
        });
        Assert.assertEquals(id2, questionnaires.getRecords().get(0).getId());

        filters = Map.of("priority", Severity.HIGH.getValue());
        questionnaires = questionnaireDBService.listByFilters(company, 0, 10, filters);
        Assertions.assertThat(Integer.valueOf(2)).isEqualTo(questionnaires.getCount() - initalCount);
        questionnaires.getRecords().forEach(questionnaire -> {
            Assert.assertEquals(State.CREATED, questionnaire.getState());
            Assert.assertNull(questionnaire.getCompletedAt());
        });
        Assertions.assertThat(questionnaires.getRecords().stream().map(Questionnaire::getId).collect(Collectors.toSet()))
                .containsAll(Set.of(id1, id2));
    }

    @Test
    @Ignore // FIXME need better data clean up
    public void testListDetailsByFilters() throws SQLException {

        var initialMain = questionnaireDBService.listQuestionnaireDetailsByFilters(company, 0, 10, null, null, null, null, null, null, null, true, null, null, null, null, null, null);
        var initialCountMain = initialMain.getCount();
        var initialTotalCountMain = initialMain.getTotalCount();

        var initialNotMain = questionnaireDBService.listQuestionnaireDetailsByFilters(company, 0, 10, null, null, null, null, null, null, null, false, null, null, null, null, null, null);
        var initialCountNotMain = initialNotMain.getCount();
        var initialTotalCountNotMain = initialNotMain.getTotalCount();

        String qTemplateId = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));

        List<String> tagIds = new ArrayList<>();
        List<String> tagItemIds = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Tag tag = Tag.builder().name("tag-value" + i).build();
            String tagId = tagsService.insert(company, tag);
            Assert.assertNotNull(tagId);
            tagIds.add(tagId);
            TagItemMapping ti = TagItemMapping.builder().itemId(qTemplateId).tagItemType(TagItemMapping.TagItemType.QUESTIONNAIRE_TEMPLATE).tagId(tagId).build();
            String tagItemId = tagItemDBService.batchInsert(company, Collections.singletonList(ti)).get(0);
            Assert.assertNotNull(tagItemId);
            tagItemIds.add(tagItemId);
        }
        Set<String> tagIdsSet = new HashSet<>(tagItemIds);

        TicketTemplate ticketTemplate = buildTicketTemplate(1, Collections.singletonList(qTemplateId));
        String ticketTemplateId = ticketTemplateDBService.insert(company, ticketTemplate);

        String userA = buildAndInsertUser("userA");

        String wItemId = wItemService.insert(company, WorkItem.builder()
                .reason("reason401")
                .type(ItemType.AUTOMATED)
                .ticketType(WorkItem.TicketType.WORK_ITEM)
                .productId(productId)
                .stateId(stateId)
                .assignees(List.of(WorkItem.Assignee.builder()
                        .userId(userA)
                        .build()))
                .build());

        String id1 = questionnaireDBService.insert(company, QuestionnaireTestUtils.buildQuestionnaire(401, wItemId, qTemplateId, kbIds, Severity.HIGH));
        String id2 = questionnaireDBService.insert(company, QuestionnaireTestUtils.buildQuestionnaire(402, wItemId, qTemplateId, kbIds, Severity.LOW));
        String id3 = questionnaireDBService.insert(company, QuestionnaireTestUtils.buildQuestionnaire(403, null, qTemplateId, kbIds, Severity.MEDIUM));
        String id4 = questionnaireDBService.insert(company, QuestionnaireTestUtils.buildQuestionnaire(404, null, qTemplateId, kbIds, Severity.MEDIUM));
        List<String> questionnaireIds = List.of(id1, id2, id3, id4);
        Set<String> questionnaireIdsSet = new HashSet<>(questionnaireIds);

        for (String id : questionnaireIds) {
            Optional<QuestionnaireDetails> optionalQuestionnaireDetails = questionnaireDBService.getDetails(company, id);
            Assert.assertNotNull(optionalQuestionnaireDetails);
            Assert.assertTrue(optionalQuestionnaireDetails.isPresent());
            QuestionnaireDetails qd = optionalQuestionnaireDetails.get();
            Assert.assertEquals(id, qd.getId());
            Assert.assertEquals(tagIdsSet, new HashSet<>(qd.getTagIds()));
        }

        // Verify record
        var questionnaires = questionnaireDBService.listQuestionnaireDetailsByFilters(company, 0, 10, null, null, null, Collections.singletonList(UUID.fromString(qTemplateId)), null, null, null, null, null, null, null, null, null, null);
        Assert.assertEquals(Integer.valueOf(4), questionnaires.getCount());
        questionnaires.getRecords().forEach(questionnaire -> {
            Assert.assertEquals(State.CREATED, questionnaire.getState());
            Assert.assertNull(questionnaire.getCompletedAt());
            Assert.assertEquals(tagIdsSet, new HashSet<>(questionnaire.getTagIds()));
        });
        Assert.assertEquals(Set.of(id1, id2, id3, id4), questionnaires.getRecords().stream().map(Questionnaire::getId).collect(Collectors.toSet()));

        questionnaires = questionnaireDBService.listQuestionnaireDetailsByFilters(company, 0, 10, null, Severity.HIGH.toString(), null, null, null, null, "target401@test.test", null, null, null, null, null, null, null);
        Assert.assertEquals(Integer.valueOf(1), questionnaires.getCount());
        Assert.assertEquals(State.CREATED, questionnaires.getRecords().get(0).getState());
        Assert.assertNull(questionnaires.getRecords().get(0).getCompletedAt());
        Assert.assertEquals(id1, questionnaires.getRecords().get(0).getId());
        Assert.assertEquals("target401@test.test", questionnaires.getRecords().get(0).getTargetEmail());
        Assert.assertEquals(tagIdsSet, new HashSet<>(questionnaires.getRecords().get(0).getTagIds()));

        questionnaires = questionnaireDBService.listQuestionnaireDetailsByFilters(company, 0, 10, null, Severity.LOW.toString(), null, null, null, null, null, null, null, null, null, null, null, null);
        Assert.assertEquals(Integer.valueOf(1), questionnaires.getCount());
        Assert.assertEquals(State.CREATED, questionnaires.getRecords().get(0).getState());
        Assert.assertNull(questionnaires.getRecords().get(0).getCompletedAt());
        Assert.assertEquals(id2, questionnaires.getRecords().get(0).getId());
        Assert.assertEquals(tagIdsSet, new HashSet<>(questionnaires.getRecords().get(0).getTagIds()));

        questionnaires = questionnaireDBService.listQuestionnaireDetailsByFilters(company, 0, 10, null, null, List.of(UUID.fromString(wItemId)), null, null, null, null, null, null, null, null, null, null, null);
        Assert.assertEquals(Integer.valueOf(2), questionnaires.getCount());
        questionnaires.getRecords().forEach(questionnaire -> {
            Assert.assertEquals(State.CREATED, questionnaire.getState());
            Assert.assertNull(questionnaire.getCompletedAt());
            Assert.assertEquals(tagIdsSet, new HashSet<>(questionnaire.getTagIds()));
        });
        Assert.assertEquals(Set.of(id1, id2), questionnaires.getRecords().stream().map(item -> item.getId().toString()).collect(Collectors.toSet()));

        questionnaires = questionnaireDBService.listQuestionnaireDetailsByFilters(company, 0, 10, null, null, null, null, UUIDUtils.fromStringsList(Arrays.asList(id1, id3)), null, null, null, null, null, null, null, null, null);
        Assert.assertEquals(Integer.valueOf(2), questionnaires.getCount());
        questionnaires.getRecords().forEach(questionnaire -> {
            Assert.assertEquals(State.CREATED, questionnaire.getState());
            Assert.assertNull(questionnaire.getCompletedAt());
            Assert.assertEquals(tagIdsSet, new HashSet<>(questionnaire.getTagIds()));
        });
        Assert.assertEquals(Set.of(id1, id3), questionnaires.getRecords().stream().map(item -> item.getId().toString()).collect(Collectors.toSet()));

        for (String tagId : tagIds) {
            questionnaires = questionnaireDBService.listQuestionnaireDetailsByFilters(company, 0, 10, null, null, null, null, null, Collections.singletonList(tagId), null, null, null, null, null, null, null, null);
            Assert.assertEquals(Integer.valueOf(4), questionnaires.getCount());
            questionnaires.getRecords().forEach(questionnaire -> {
                Assert.assertEquals(State.CREATED, questionnaire.getState());
                Assert.assertNull(questionnaire.getCompletedAt());
                Assert.assertEquals(tagIdsSet, new HashSet<>(questionnaire.getTagIds()));
            });
            Assert.assertEquals(questionnaireIdsSet, questionnaires.getRecords().stream().map(item -> item.getId().toString()).collect(Collectors.toSet()));
        }
        questionnaires = questionnaireDBService.listQuestionnaireDetailsByFilters(company, 0, 10, null, null, null, null, null, tagIds, null, null, null, null, null, null, null, null);
        Assert.assertEquals(Integer.valueOf(4), questionnaires.getCount());
        questionnaires.getRecords().forEach(questionnaire -> {
            Assert.assertEquals(State.CREATED, questionnaire.getState());
            Assert.assertNull(questionnaire.getCompletedAt());
            Assert.assertEquals(tagIdsSet, new HashSet<>(questionnaire.getTagIds()));
        });
        Assert.assertEquals(questionnaireIdsSet, questionnaires.getRecords().stream().map(Questionnaire::getId).collect(Collectors.toSet()));
        questionnaires = questionnaireDBService.listQuestionnaireDetailsByFilters(company, 0, 1, null, null, null, null, null, tagIds, null, null, null, null, null, null, null, null);
        Assert.assertEquals(Integer.valueOf(4), questionnaires.getTotalCount());

        questionnaires = questionnaireDBService.listQuestionnaireDetailsByFilters(company, 0, 10, null, null, null, null, null, null, null, true, null, null, null, null, null, null);
        Assertions.assertThat(Integer.valueOf(4)).isEqualTo(questionnaires.getCount() - initialCountMain);
        questionnaires.getRecords().stream().filter(questionnaire -> questionnaireIds.contains(questionnaire.getId())).forEach(questionnaire -> {
            Assert.assertEquals(State.CREATED, questionnaire.getState());
            Assert.assertNull(questionnaire.getCompletedAt());
            Assertions.assertThat(new HashSet<>(questionnaire.getTagIds())).containsAll(tagIdsSet);
            Assert.assertTrue(questionnaire.getMain());
        });
        Assertions.assertThat(questionnaires.getRecords().stream().map(Questionnaire::getId).collect(Collectors.toSet())).containsAll(questionnaireIdsSet);

        questionnaires = questionnaireDBService.listQuestionnaireDetailsByFilters(company, 0, 10, null, null, null, null, null, null, null, false, null, null, null, null, null, null);
        Assertions.assertThat(Integer.valueOf(0)).isEqualTo(questionnaires.getCount() - initialCountNotMain);
        Assertions.assertThat(Integer.valueOf(0)).isEqualTo(questionnaires.getTotalCount() - initialTotalCountNotMain);

        questionnaires = questionnaireDBService.listQuestionnaireDetailsByFilters(company, 0, 10, null, null, List.of(UUID.fromString(wItemId)), null, null, tagIds, null, true, State.COMPLETED, null, null, null, null, null);
        Assert.assertEquals(Integer.valueOf(0), questionnaires.getCount());
        Assert.assertEquals(Integer.valueOf(0), questionnaires.getTotalCount());

        questionnaires = questionnaireDBService.listQuestionnaireDetailsByFilters(company, 0, 10, null, null, List.of(UUID.fromString(wItemId)), null, null, tagIds, null, true, null, null, null, null, null, null);
        Assert.assertEquals(Integer.valueOf(2), questionnaires.getCount());
        questionnaires.getRecords().forEach(questionnaire -> {
            Assert.assertEquals(State.CREATED, questionnaire.getState());
            Assert.assertNull(questionnaire.getCompletedAt());
            Assert.assertEquals(tagIdsSet, new HashSet<>(questionnaire.getTagIds()));
        });
        Assert.assertEquals(Set.of(id1, id2), questionnaires.getRecords().stream().map(Questionnaire::getId).collect(Collectors.toSet()));

        assertThat(questionnaireDBService.listQuestionnaireDetailsByFilters(company, 0, 10, QuestionnaireFilter.builder()
                .assigneeUserIds(List.of(userA))
                .build()
        ).getRecords().stream().map(Questionnaire::getId)).contains(id1, id2);

        assertThat(questionnaireDBService.listQuestionnaireDetailsByFilters(company, 0, 10, QuestionnaireFilter.builder()
                .assigneeUserIds(List.of("unassigned"))
                .build()
        ).getRecords().stream().map(Questionnaire::getId)).contains(id3, id4);

        assertThat(questionnaireDBService.listQuestionnaireDetailsByFilters(company, 0, 10, QuestionnaireFilter.builder()
                .assigneeUserIds(List.of(userA, "unassigned"))
                .build()
        ).getRecords().stream().map(Questionnaire::getId)).contains(id1, id2, id3, id4);
    }

    @Test
    public void testCountAggAcrossTemplate() throws SQLException, JsonProcessingException {
        // -- SETUP
        String qTemplateId1 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String qTemplateId2 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String qTemplateId3 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String tagA = tagsService.insert(company, List.of("tagA")).get(0);
        String tagB = tagsService.insert(company, List.of("tagB")).get(0);
        String tagC = tagsService.insert(company, List.of("tagC")).get(0);
        tagQuestionnaireTemplate(qTemplateId1, tagA);
        tagQuestionnaireTemplate(qTemplateId1, tagB);
        tagQuestionnaireTemplate(qTemplateId2, tagA);
        String userA = buildAndInsertUser("userA");
        String userB = buildAndInsertUser("userB");
        String userC = buildAndInsertUser("userC");
        String wi1 = buildAndInsertWorkItem(List.of(userA, userB));
        String wi2 = buildAndInsertWorkItem(List.of(userA));
        String wi3 = buildAndInsertWorkItem(List.of());
        tagWorkItem(wi1, tagA);
        tagWorkItem(wi3, tagB);

        buildAndInsertQuestionnaire(qTemplateId1, null, false, true, 0, 0);
        buildAndInsertQuestionnaire(qTemplateId1, wi1, false, false, 0, 10);
        buildAndInsertQuestionnaire(qTemplateId1, wi2, false, false, 0, 20);
        buildAndInsertQuestionnaire(qTemplateId2, wi3, false, true, 0, 30);
        buildAndInsertQuestionnaire(qTemplateId2, wi1, false, true, 0, 40);
        buildAndInsertQuestionnaire(qTemplateId3, wi2, false, false, 0, 50);


        // TEST no filters
        var aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.questionnaire_template_id)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .build(), pageNumber, pageSize);

        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                qTemplateId1, 3,
                qTemplateId2, 2,
                qTemplateId3, 1
        ));

        // TEST template filter
        aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.questionnaire_template_id)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .questionnaireTemplateId(List.of(qTemplateId2, qTemplateId3))
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                qTemplateId2, 2,
                qTemplateId3, 1
        ));

        // --- TEST COMPLETED ---

        // TEST completed = true filter
        aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.questionnaire_template_id)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .completed(true)
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                qTemplateId1, 1,
                qTemplateId2, 2
        ));

        // TEST completed = false filter
        aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.questionnaire_template_id)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .completed(false)
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                qTemplateId1, 2,
                qTemplateId3, 1
        ));

        // --- TEST ASSIGNEES ---

        aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.questionnaire_template_id)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .assignees(List.of(userA))
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                qTemplateId1, 2,
                qTemplateId2, 1,
                qTemplateId3, 1
        ));

        aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.questionnaire_template_id)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .assignees(List.of(userB))
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                qTemplateId1, 1,
                qTemplateId2, 1
        ));

        aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.questionnaire_template_id)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .assignees(List.of(userA, userB, userC))
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                qTemplateId1, 2,
                qTemplateId2, 1,
                qTemplateId3, 1
        ));

        aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.questionnaire_template_id)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .assignees(List.of(userC))
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of());

        // --- TEST UNASSIGNED ---
        aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.questionnaire_template_id)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .unassigned(true)
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of());

        aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.questionnaire_template_id)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .unassigned(false)
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                qTemplateId1, 2,
                qTemplateId2, 1,
                qTemplateId3, 1
        ));

        // --- TEST TAGS ---

        // TEST tags filter A
        aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.questionnaire_template_id)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .tags(List.of(tagA))
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                qTemplateId1, 3,
                qTemplateId2, 2
        ));

        // TEST tags filter B
        aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.questionnaire_template_id)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .tags(List.of(tagB))
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                qTemplateId1, 3
        ));

        // TEST tags filter dummy
        aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.questionnaire_template_id)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .tags(List.of(tagC))
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertThat(aggResult.getCount()).isEqualTo(0);
        assertCountAggResults(aggResult, Map.of());

        // --- TEST UPDATED AT  ---

        aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.questionnaire_template_id)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .updatedAt(DateFilter.builder()
                                .gt(99L)
                                .build())
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of());

        aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.questionnaire_template_id)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .updatedAt(DateFilter.builder()
                                .gt(35L)
                                .build())
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                qTemplateId2, 1,
                qTemplateId3, 1
        ));

        aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.questionnaire_template_id)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .updatedAt(DateFilter.builder()
                                .lt(25L)
                                .build())
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                qTemplateId1, 3));

        // TEST created at
        aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.questionnaire_template_id)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .createdAt(DateFilter.builder()
                                .gt(1L)
                                .build())
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of());

        // TEST tags work item tags A
        aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.questionnaire_template_id)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .workItemTags(List.of(tagA))
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                qTemplateId1, 1,
                qTemplateId2, 1
        ));

        // TEST tags work item tags B
        aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.questionnaire_template_id)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .workItemTags(List.of(tagB))
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                qTemplateId2, 1
        ));

        // TEST tags work item tags A or B
        aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.questionnaire_template_id)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .workItemTags(List.of(tagA, tagB))
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                qTemplateId1, 1,
                qTemplateId2, 2
        ));
    }

    @Test
    public void testCountAggAcrossCompleted() throws SQLException, JsonProcessingException {
        // -- SETUP
        String qTemplateId1 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String qTemplateId2 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));

        buildAndInsertQuestionnaire(qTemplateId1, null, false, true, 0, 0);
        buildAndInsertQuestionnaire(qTemplateId1, null, false, false, 0, 10);
        buildAndInsertQuestionnaire(qTemplateId1, null, false, false, 0, 20);
        buildAndInsertQuestionnaire(qTemplateId2, null, false, true, 0, 30);
        buildAndInsertQuestionnaire(qTemplateId2, null, false, true, 0, 40);


        // TEST no filters
        var aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.completed)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                "true", 3,
                "false", 2));

        // TEST filters
        aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.completed)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .questionnaireTemplateId(List.of(qTemplateId1))
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                "true", 1,
                "false", 2));
    }

    @Test
    public void testCountAggAcrossSubmitted() throws SQLException, JsonProcessingException {
        // -- SETUP
        String qTemplateId1 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String qTemplateId2 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));

        String id1 = buildAndInsertQuestionnaire(qTemplateId1, null, false, true, 0, 0);
        String id2 = buildAndInsertQuestionnaire(qTemplateId1, null, false, false, 0, 10);
        String id3 = buildAndInsertQuestionnaire(qTemplateId1, null, false, false, 0, 20);
        String id4 = buildAndInsertQuestionnaire(qTemplateId2, null, false, true, 0, 30);
        String id5 = buildAndInsertQuestionnaire(qTemplateId2, null, false, true, 0, 40);
        updateQuestionnaireState(id1, State.COMPLETED);
        updateQuestionnaireState(id2, State.COMPLETED);

        // TEST no filters
        var aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.submitted)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                "true", 2,
                "false", 3));

        // TEST filters
        aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.completed)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .questionnaireTemplateId(List.of(qTemplateId1))
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                "true", 1,
                "false", 2));
    }

    @Test
    public void testCountAggAcrossAssignee() throws SQLException, JsonProcessingException {
        // -- SETUP
        String qTemplateId1 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String qTemplateId2 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));

        String userA = buildAndInsertUser("userA");
        String userB = buildAndInsertUser("userB");
        String userC = buildAndInsertUser("userC");
        String wi1 = buildAndInsertWorkItem(List.of(userA, userB));
        String wi2 = buildAndInsertWorkItem(List.of(userA));
        String wi3 = buildAndInsertWorkItem(List.of());

        buildAndInsertQuestionnaire(qTemplateId1, null, false, true, 0, 0);
        buildAndInsertQuestionnaire(qTemplateId1, wi1, false, false, 0, 10);
        buildAndInsertQuestionnaire(qTemplateId1, wi2, false, false, 0, 20);
        buildAndInsertQuestionnaire(qTemplateId2, wi3, false, true, 0, 30);
        buildAndInsertQuestionnaire(qTemplateId2, wi2, false, true, 0, 40);


        // TEST no filters
        var aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.assignee)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                userA, 3,
                userB, 1,
                "unassigned", 2));

        // TEST filters
        aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.assignee)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .questionnaireTemplateId(List.of(qTemplateId1))
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                userA, 2,
                userB, 1,
                "unassigned", 1));
    }

    @Test
    public void testCountAggAcrossTag() throws SQLException, JsonProcessingException {
        // -- SETUP
        String qTemplateId1 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String qTemplateId2 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String qTemplateId3 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String tagA = tagsService.insert(company, List.of("tagA")).get(0);
        String tagB = tagsService.insert(company, List.of("tagB")).get(0);
        String tagC = tagsService.insert(company, List.of("tagC")).get(0);
        tagQuestionnaireTemplate(qTemplateId1, tagA);
        tagQuestionnaireTemplate(qTemplateId1, tagB);
        tagQuestionnaireTemplate(qTemplateId2, tagA);

        String id1 = buildAndInsertQuestionnaire(qTemplateId1, null, false, true, 0, 0);
        String id2 = buildAndInsertQuestionnaire(qTemplateId1, null, false, false, 0, 10);
        String id3 = buildAndInsertQuestionnaire(qTemplateId1, null, false, false, 0, 20);
        String id4 = buildAndInsertQuestionnaire(qTemplateId2, null, false, true, 0, 30);
        String id5 = buildAndInsertQuestionnaire(qTemplateId2, null, false, true, 0, 40);
        String id6 = buildAndInsertQuestionnaire(qTemplateId3, null, false, false, 0, 50);


        // TEST no filters
        var aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.tag)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                tagA, 5,
                tagB, 3
        ));

        // TEST filters
        aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.tag)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .questionnaireTemplateId(List.of(qTemplateId1))
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                tagA, 3,
                tagB, 3
        ));

        aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.tag)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .questionnaireTemplateId(List.of(qTemplateId1))
                        .tags(List.of(tagB))
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                tagB, 3
        ));
    }

    @Test
    public void testCountAggAcrossTrend() throws SQLException, JsonProcessingException {
        // -- SETUP
        String qTemplateId1 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String qTemplateId2 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String qTemplateId3 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));

        String id1 = buildAndInsertQuestionnaire(qTemplateId1, null, false, true, 0, DateUtils.toEpochSecond(Instant.parse("2020-01-02T00:00:00Z")));
        String id2 = buildAndInsertQuestionnaire(qTemplateId1, null, false, false, 0, DateUtils.toEpochSecond(Instant.parse("2020-01-02T00:00:00Z")));
        String id3 = buildAndInsertQuestionnaire(qTemplateId1, null, false, false, 0, DateUtils.toEpochSecond(Instant.parse("2020-02-02T00:00:00Z")));
        String id4 = buildAndInsertQuestionnaire(qTemplateId2, null, false, true, 0, DateUtils.toEpochSecond(Instant.parse("2020-02-02T00:00:00Z")));
        String id5 = buildAndInsertQuestionnaire(qTemplateId2, null, false, true, 0, DateUtils.toEpochSecond(Instant.parse("2020-02-02T00:00:00Z")));
        String id6 = buildAndInsertQuestionnaire(qTemplateId3, null, false, false, 0, DateUtils.toEpochSecond(Instant.parse("2020-03-02T00:00:00Z")));


        // TEST no filters
        var aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.trend)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                "1577865600", 2,
                "1580544000", 5,
                "1583049600", 6
        ));

        // TEST filters
        aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.trend)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .questionnaireTemplateId(List.of(qTemplateId1))
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                "1577865600", 2,
                "1580544000", 3
        ));

    }

    @Test
    public void testCountAggAcrossCreated() throws SQLException, JsonProcessingException {
        // -- SETUP
        String qTemplateId1 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String qTemplateId2 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String qTemplateId3 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));

        Long d1 = DateUtils.toEpochSecond(Instant.parse("2020-01-02T00:00:00Z"));
        Long d2 = DateUtils.toEpochSecond(Instant.parse("2020-01-02T00:01:00Z"));
        Long d3 = DateUtils.toEpochSecond(Instant.parse("2020-01-02T00:05:00Z"));
        String id1 = buildAndInsertQuestionnaire(qTemplateId1, null, false, false, d1, d1);
        String id2 = buildAndInsertQuestionnaire(qTemplateId1, null, true, false, d1, d2);
        String id3 = buildAndInsertQuestionnaire(qTemplateId1, null, false, true, d1, d3);
        String id4 = buildAndInsertQuestionnaire(qTemplateId2, null, false, true, d1, d2);
        String id5 = buildAndInsertQuestionnaire(qTemplateId2, null, true, false, d1, d2);
        String id6 = buildAndInsertQuestionnaire(qTemplateId3, null, false, false, d1, d1);


        // TEST no filters
        var aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.created)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                String.valueOf(d1), 6
        ));
    }

    @Test
    public void testCountAggAcrossUpdated() throws SQLException, JsonProcessingException {
        // -- SETUP
        String qTemplateId1 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String qTemplateId2 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String qTemplateId3 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));

        Long d1 = DateUtils.toEpochSecond(Instant.parse("2020-01-02T00:00:00Z"));
        Long d2 = DateUtils.toEpochSecond(Instant.parse("2020-01-02T00:01:00Z"));
        Long d3 = DateUtils.toEpochSecond(Instant.parse("2020-01-02T00:05:00Z"));
        String id1 = buildAndInsertQuestionnaire(qTemplateId1, null, false, false, d1, d1);
        String id2 = buildAndInsertQuestionnaire(qTemplateId1, null, true, false, d1, d2);
        String id3 = buildAndInsertQuestionnaire(qTemplateId1, null, false, true, d1, d3);
        String id4 = buildAndInsertQuestionnaire(qTemplateId2, null, false, true, d1, d2);
        String id5 = buildAndInsertQuestionnaire(qTemplateId2, null, true, false, d1, d2);
        String id6 = buildAndInsertQuestionnaire(qTemplateId3, null, false, false, d1, d1);


        // TEST no filters
        var aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.updated)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                String.valueOf(d1), 2,
                String.valueOf(d2), 3,
                String.valueOf(d3), 1
        ));
    }

    @Test
    public void testCountAggAcrossWiTag() throws SQLException, JsonProcessingException {
        // -- SETUP
        String qTemplateId1 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String qTemplateId2 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String qTemplateId3 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String tagA = tagsService.insert(company, List.of("tagA")).get(0);
        String tagB = tagsService.insert(company, List.of("tagB")).get(0);
        String tagC = tagsService.insert(company, List.of("tagC")).get(0);
        tagQuestionnaireTemplate(qTemplateId1, tagA);
        tagQuestionnaireTemplate(qTemplateId1, tagB);
        tagQuestionnaireTemplate(qTemplateId2, tagA);
        String userA = buildAndInsertUser("userA");
        String userB = buildAndInsertUser("userB");
        String userC = buildAndInsertUser("userC");
        String wi1 = buildAndInsertWorkItem(List.of(userA, userB));
        String wi2 = buildAndInsertWorkItem(List.of(userA));
        String wi3 = buildAndInsertWorkItem(List.of());
        tagWorkItem(wi1, tagA);
        tagWorkItem(wi3, tagB);

        String id1 = buildAndInsertQuestionnaire(qTemplateId1, null, false, true, 0, 0);
        String id2 = buildAndInsertQuestionnaire(qTemplateId1, wi1, false, false, 0, 10);
        String id3 = buildAndInsertQuestionnaire(qTemplateId1, wi2, false, false, 0, 20);
        String id4 = buildAndInsertQuestionnaire(qTemplateId2, wi3, false, true, 0, 30);
        String id5 = buildAndInsertQuestionnaire(qTemplateId2, wi1, false, true, 0, 40);
        String id6 = buildAndInsertQuestionnaire(qTemplateId3, wi2, false, false, 0, 50);

        // TEST no filters
        var aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.work_item_tag)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                tagA, 2,
                tagB, 1
        ));
    }

    @Test
    public void testCountAggAcrossState() throws SQLException, JsonProcessingException {
        String qTemplateId1 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String qTemplateId2 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));

        String userA = buildAndInsertUser("userA");
        String userB = buildAndInsertUser("userB");
        String userC = buildAndInsertUser("userC");
        String wi1 = buildAndInsertWorkItem(List.of(userA, userB));
        String wi2 = buildAndInsertWorkItem(List.of(userA));
        String wi3 = buildAndInsertWorkItem(List.of());

        String id1 = buildAndInsertQuestionnaire(qTemplateId1, null, false, true, 0, 0);
        String id2 = buildAndInsertQuestionnaire(qTemplateId1, wi1, false, false, 0, 10);
        String id3 = buildAndInsertQuestionnaire(qTemplateId1, wi2, false, false, 0, 20);
        String id4 = buildAndInsertQuestionnaire(qTemplateId2, wi3, false, true, 0, 30);
        String id5 = buildAndInsertQuestionnaire(qTemplateId2, wi2, false, true, 0, 40);

        // TEST no filters
        var aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.state)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                String.valueOf(stateId), 4,
                "unknown", 1));

        // TEST filter
        aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.state)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .states(List.of(stateId.toString()))
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                String.valueOf(stateId), 4));

    }

    @Test
    public void testCountAggAcrossProduct() throws SQLException, JsonProcessingException {
        String qTemplateId1 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String qTemplateId2 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));

        String userA = buildAndInsertUser("userA");
        String userB = buildAndInsertUser("userB");
        String userC = buildAndInsertUser("userC");
        String wi1 = buildAndInsertWorkItem(List.of(userA, userB));
        String wi2 = buildAndInsertWorkItem(List.of(userA));
        String wi3 = buildAndInsertWorkItem(List.of());

        String id1 = buildAndInsertQuestionnaire(qTemplateId1, wi1, false, true, 0, 0);
        String id2 = buildAndInsertQuestionnaire(qTemplateId1, wi1, false, false, 0, 10);
        String id3 = buildAndInsertQuestionnaire(qTemplateId1, wi2, false, false, 0, 20);
        String id4 = buildAndInsertQuestionnaire(qTemplateId2, wi3, false, true, 0, 30);
        String id5 = buildAndInsertQuestionnaire(qTemplateId2, wi2, false, true, 0, 40);

        String productId2 = ProductUtils.createProduct(productService, company, 1, null).getId();
        setWorkItemProductId(wi1, productId2);

        var aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.work_item_product)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                productId, 3,
                productId2, 2));

        // test filter
        aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.work_item_product)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .workItemProductIds(List.of(productId2))
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                productId2, 2));

    }
    @Test
    public void testPagination() throws SQLException{
        // -- SETUP
        String qTemplateId1 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String qTemplateId2 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String qTemplateId3 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));

        Long d1 = DateUtils.toEpochSecond(Instant.parse("2020-01-02T00:00:00Z"));
        Long d2 = DateUtils.toEpochSecond(Instant.parse("2020-01-02T00:01:00Z"));
        Long d3 = DateUtils.toEpochSecond(Instant.parse("2020-01-02T00:05:00Z"));
        String id1 = buildAndInsertQuestionnaire(qTemplateId1, null, false, false, d1, d1);
        String id2 = buildAndInsertQuestionnaire(qTemplateId1, null, true, false, d1, d2);
        String id3 = buildAndInsertQuestionnaire(qTemplateId1, null, false, true, d1, d3);
        String id4 = buildAndInsertQuestionnaire(qTemplateId2, null, false, true, d1, d2);
        String id5 = buildAndInsertQuestionnaire(qTemplateId2, null, true, false, d1, d2);
        String id6 = buildAndInsertQuestionnaire(qTemplateId3, null, false, false, d1, d1);


        // TEST no filters
        var aggResult = questionnaireDBService.stackedAggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.questionnaire_template_id)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .build(),
                List.of(QuestionnaireAggFilter.Distinct.completed), pageNumber, 2);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertThat(aggResult.getCount()).isEqualTo(2);
    }

    @Test
    public void testSlaAggAcrossTemplate() throws SQLException, JsonProcessingException {
        // -- SETUP
        String qTemplateId1 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String qTemplateId2 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String qTemplateId3 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));

        Long d1 = DateUtils.toEpochSecond(Instant.parse("2020-01-02T00:00:00Z"));
        Long d2 = DateUtils.toEpochSecond(Instant.parse("2020-01-02T00:01:00Z"));
        Long d3 = DateUtils.toEpochSecond(Instant.parse("2020-01-02T00:05:00Z"));
        String id1 = buildAndInsertQuestionnaire(qTemplateId1, null, true, true, d1, d1);
        String id2 = buildAndInsertQuestionnaire(qTemplateId1, null, true, false, d1, d2);
        String id3 = buildAndInsertQuestionnaire(qTemplateId1, null, true, false, d1, d3);
        String id4 = buildAndInsertQuestionnaire(qTemplateId2, null, true, true, d1, d2);
        String id5 = buildAndInsertQuestionnaire(qTemplateId2, null, true, true, d1, d2);
        String id6 = buildAndInsertQuestionnaire(qTemplateId3, null, true, false, d1, d1);

        // TEST no filters
        var aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.questionnaire_template_id)
                        .calculation(QuestionnaireAggFilter.Calculation.response_time)
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertSlaAggResults(aggResult, Map.of(
                qTemplateId1, List.of(60, 0, 300, 3), // med min max total
                qTemplateId2, List.of(60, 60, 60, 2),
                qTemplateId3, List.of(0, 0, 0, 1)
        ));

        // TEST filters
        aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.questionnaire_template_id)
                        .calculation(QuestionnaireAggFilter.Calculation.response_time)
                        .questionnaireTemplateId(List.of(qTemplateId1))
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertSlaAggResults(aggResult, Map.of(
                qTemplateId1, List.of(60, 0, 300, 3) // med min max total
        ));
    }

    @Test
    public void testSlaAggAcrossAssignee() throws SQLException, JsonProcessingException {
        // -- SETUP
        String qTemplateId1 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String qTemplateId2 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String qTemplateId3 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));

        String userA = buildAndInsertUser("userA");
        String userB = buildAndInsertUser("userB");
        String userC = buildAndInsertUser("userC");
        String wi1 = buildAndInsertWorkItem(List.of(userA, userB));
        String wi2 = buildAndInsertWorkItem(List.of(userA));
        String wi3 = buildAndInsertWorkItem(List.of());

        Long d1 = DateUtils.toEpochSecond(Instant.parse("2020-01-02T00:00:00Z"));
        Long d2 = DateUtils.toEpochSecond(Instant.parse("2020-01-02T00:01:00Z"));
        Long d3 = DateUtils.toEpochSecond(Instant.parse("2020-01-02T00:05:00Z"));
        String id1 = buildAndInsertQuestionnaire(qTemplateId1, null, true, true, d1, d1);
        String id2 = buildAndInsertQuestionnaire(qTemplateId1, wi1, true, false, d1, d2);
        String id3 = buildAndInsertQuestionnaire(qTemplateId1, wi2, true, false, d1, d3);
        String id4 = buildAndInsertQuestionnaire(qTemplateId2, wi3, true, true, d1, d2);
        String id5 = buildAndInsertQuestionnaire(qTemplateId2, wi1, true, true, d1, d2);
        String id6 = buildAndInsertQuestionnaire(qTemplateId3, wi2, true, false, d1, d1);


        // TEST no filters
        var aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.assignee)
                        .calculation(QuestionnaireAggFilter.Calculation.response_time)
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertSlaAggResults(aggResult, Map.of(
                userA, List.of(60, 0, 300, 4), // med min max total
                userB, List.of(60, 60, 60, 2),
                "unassigned", List.of(0, 0, 60, 2)
        ));
    }

    @Test
    public void testSlaAggAcrossCompleted() throws SQLException, JsonProcessingException {
        // -- SETUP
        String qTemplateId1 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String qTemplateId2 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String qTemplateId3 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));

        Long d1 = DateUtils.toEpochSecond(Instant.parse("2020-01-02T00:00:00Z"));
        Long d2 = DateUtils.toEpochSecond(Instant.parse("2020-01-02T00:01:00Z"));
        Long d3 = DateUtils.toEpochSecond(Instant.parse("2020-01-02T00:05:00Z"));
        buildAndInsertQuestionnaire(qTemplateId1, null, false, false, d1, d1);
        buildAndInsertQuestionnaire(qTemplateId1, null, true, false, d1, d2);
        buildAndInsertQuestionnaire(qTemplateId1, null, false, true, d1, d3);
        buildAndInsertQuestionnaire(qTemplateId2, null, false, true, d1, d2);
        buildAndInsertQuestionnaire(qTemplateId2, null, true, false, d1, d2);
        buildAndInsertQuestionnaire(qTemplateId3, null, false, false, d1, d1);


        // TEST no filters
        var aggResult = questionnaireDBService.aggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.completed)
                        .calculation(QuestionnaireAggFilter.Calculation.response_time)
                        .build(), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertThat(aggResult.getRecords()).hasSize(2);
        Map<String, DbAggregationResult> map = aggResult.getRecords().stream().collect(Collectors.toMap(x -> x.getKey(), x -> x));
        assertThat(map.get("false").getMedian()).isEqualTo(60);
        assertThat(map.get("false").getMin()).isEqualTo(60);
        assertThat(map.get("false").getMax()).isGreaterThan(1000); // because of incomplete
        assertThat(map.get("false").getTotal()).isEqualTo(4);

        assertThat(map.get("true").getMedian()).isEqualTo(60);
        assertThat(map.get("true").getMin()).isEqualTo(60);
        assertThat(map.get("true").getMax()).isEqualTo(300);
        assertThat(map.get("true").getTotal()).isEqualTo(2);
    }

    @Test
    public void testStackedAgg() throws SQLException, JsonProcessingException {
        // -- SETUP
        String qTemplateId1 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String qTemplateId2 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));
        String qTemplateId3 = questionnaireTemplateDBService.insert(company, buildQuestionnaireTemplate(1, kbIds));

        Long d1 = DateUtils.toEpochSecond(Instant.parse("2020-01-02T00:00:00Z"));
        Long d2 = DateUtils.toEpochSecond(Instant.parse("2020-01-02T00:01:00Z"));
        Long d3 = DateUtils.toEpochSecond(Instant.parse("2020-01-02T00:05:00Z"));
        String id1 = buildAndInsertQuestionnaire(qTemplateId1, null, false, false, d1, d1);
        String id2 = buildAndInsertQuestionnaire(qTemplateId1, null, true, false, d1, d2);
        String id3 = buildAndInsertQuestionnaire(qTemplateId1, null, false, true, d1, d3);
        String id4 = buildAndInsertQuestionnaire(qTemplateId2, null, false, true, d1, d2);
        String id5 = buildAndInsertQuestionnaire(qTemplateId2, null, true, false, d1, d2);
        String id6 = buildAndInsertQuestionnaire(qTemplateId3, null, false, false, d1, d1);


        // TEST no filters
        var aggResult = questionnaireDBService.stackedAggregate(company,
                QuestionnaireAggFilter.builder()
                        .across(QuestionnaireAggFilter.Distinct.questionnaire_template_id)
                        .calculation(QuestionnaireAggFilter.Calculation.count)
                        .build(),
                List.of(QuestionnaireAggFilter.Distinct.completed), pageNumber, pageSize);
        DefaultObjectMapper.prettyPrint(aggResult);
        assertThat(aggResult.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder()
                        .key(qTemplateId1)
                        .total(3L)
                        .stacks(List.of(
                                DbAggregationResult.builder()
                                        .key("false")
                                        .total(2L)
                                        .build(),
                                DbAggregationResult.builder()
                                        .key("true")
                                        .total(1L)
                                        .build()
                        ))
                        .build(),
                DbAggregationResult.builder()
                        .key(qTemplateId2)
                        .total(2L)
                        .stacks(List.of(
                                DbAggregationResult.builder()
                                        .key("false")
                                        .total(1L)
                                        .build(),
                                DbAggregationResult.builder()
                                        .key("true")
                                        .total(1L)
                                        .build()
                        ))
                        .build(),
                DbAggregationResult.builder()
                        .key(qTemplateId3)
                        .total(1L)
                        .stacks(List.of(
                                DbAggregationResult.builder()
                                        .key("false")
                                        .total(1L)
                                        .build()
                        ))
                        .build()
        );
    }


    // region UTILS
    private String buildAndInsertQuestionnaire(String qTemplateId, String wi, boolean partiallyCompleted, boolean completed, long createdAt, long updatedAt) throws SQLException {
        var q = Questionnaire.builder()
                .questionnaireTemplateId(qTemplateId)
                .state(State.CREATED).bucketName("a").bucketPath("b")
                .totalQuestions(4)
                .answered((completed || partiallyCompleted) ? (completed ? 4 : 3) : 0)
                .workItemId(wi)
                .build();
        String id = questionnaireDBService.insert(company, q);
        template.update("update " + company + ".questionnaires " +
                        " SET updatedat = :updatedAt," +
                        "     createdat = :createdat " +
                        " WHERE id = :id::uuid",
                Map.of("id", id,
                        "updatedAt", updatedAt,
                        "createdat", createdAt));
        return id;
    }

    private void updateQuestionnaireState(String id, State state) throws SQLException {
        Questionnaire q = questionnaireDBService.get(company, id).orElseThrow();
        questionnaireDBService.update(company, q.toBuilder()
                .state(state)
                .build());
    }

    private void tagQuestionnaireTemplate(String qTemplateId, String tagId) throws SQLException {
        TagItemMapping ti = TagItemMapping.builder()
                .itemId(qTemplateId)
                .tagItemType(TagItemMapping.TagItemType.QUESTIONNAIRE_TEMPLATE)
                .tagId(tagId)
                .build();
        tagItemDBService.batchInsert(company, Collections.singletonList(ti));
    }

    private String buildAndInsertWorkItem(List<String> userIds) throws SQLException {
        return wItemService.insert(company,
                WorkItem.builder()
                        .reason("reason " + System.currentTimeMillis())
                        .type(ItemType.AUTOMATED)
                        .ticketType(WorkItem.TicketType.WORK_ITEM)
                        .productId(productId)
                        .stateId(stateId)
                        .assignees(userIds.stream()
                                .map(userId -> WorkItem.Assignee.builder().userId(userId).build())
                                .collect(Collectors.toList())).build());
    }

    private String buildAndInsertUser(String e) throws SQLException {
        return userService.insert(company, User.builder()
                .firstName(e)
                .lastName(e)
                .email(e)
                .bcryptPassword(e)
                .userType(RoleType.ADMIN)
                .passwordAuthEnabled(true)
                .samlAuthEnabled(false)
                .build());
    }

    private void setQuestionnaireProductId(String questionnaireId, String productId) throws SQLException {
        Questionnaire questionnaire = questionnaireDBService.get(company, questionnaireId).orElseThrow();
        questionnaireDBService.update(company, questionnaire.toBuilder()
                .productId(productId)
                .build());
    }

    private void tagWorkItem(String workItemId, String tagId) throws SQLException {
        tagItemDBService.insert(company, TagItemMapping.builder()
                .itemId(workItemId)
                .tagId(tagId)
                .tagItemType(WorkItem.ITEM_TYPE)
                .build());
    }

    private void setWorkItemProductId(String workItemId, String productId) throws SQLException {
        wItemService.updateProductId(company, UUID.fromString(workItemId), productId);
    }

    private static void assertCountAggResults(DbListResponse<DbAggregationResult> aggResult,
                                              Map<String, Integer> expectedTotals) {

        assertThat(aggResult.getRecords()).containsExactlyInAnyOrderElementsOf(
                expectedTotals.entrySet().stream()
                        .map(kv -> DbAggregationResult.builder()
                                .key(kv.getKey())
                                .total(kv.getValue().longValue())
                                .build())
                        .collect(Collectors.toList()));
        assertThat(aggResult.getCount()).isEqualTo(expectedTotals.size());
    }

    private static void assertSlaAggResults(DbListResponse<DbAggregationResult> aggResult,
                                            Map<String, List<Integer>> expectedTotals) {

        assertThat(aggResult.getRecords()).containsExactlyInAnyOrderElementsOf(
                expectedTotals.entrySet().stream()
                        .map(kv -> DbAggregationResult.builder()
                                .key(kv.getKey())
                                .median(kv.getValue().get(0).longValue())
                                .min(kv.getValue().get(1).longValue())
                                .max(kv.getValue().get(2).longValue())
                                .total(kv.getValue().get(3).longValue())
                                .build())
                        .collect(Collectors.toList()));
        assertThat(aggResult.getCount()).isEqualTo(expectedTotals.size());
    }

    // endregion
}