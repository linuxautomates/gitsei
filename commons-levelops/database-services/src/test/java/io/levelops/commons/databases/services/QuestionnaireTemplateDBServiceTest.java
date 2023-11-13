package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.BestPracticesItem;
import io.levelops.commons.databases.models.database.Questionnaire;
import io.levelops.commons.databases.models.database.Questionnaire.State;
import io.levelops.commons.databases.models.database.QuestionnaireTemplate;
import io.levelops.commons.databases.models.database.Severity;
import io.levelops.commons.databases.models.database.Tag;
import io.levelops.commons.databases.models.database.WorkItem;
import io.levelops.commons.databases.models.database.WorkItem.ItemType;
import io.levelops.commons.databases.models.database.mappings.TagItemMapping;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.sql.DataSource;

public class QuestionnaireTemplateDBServiceTest {
    private final ObjectMapper MAPPER = DefaultObjectMapper.get();
    private static final String QUESTIONNAIRE_TEMPLATE_NAME_PREFIX = "qtemplate";
    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private DataSource dataSource;
    private ProductService productService;
    private TagsService tagsService;
    private TagItemDBService tagItemDBService;
    private BestPracticesService bestPracticesService;
    private QuestionnaireTemplateDBService questionnaireTemplateDBService;
    private TicketTemplateDBService ticketTemplateDBService;
    private StateDBService stateDBService;
    private CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private CiCdJobRunStageDatabaseService jobRunStageDatabaseService;
    private WorkItemDBService wItemService;
    private QuestionnaireDBService questionnaireDBService;
    private String company = "test";

    @Before
    public void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        productService = new ProductService(dataSource);
        new UserService(dataSource, MAPPER).ensureTableExistence(company);
        new OrganizationService(dataSource).ensureTableExistence(company);
        new IntegrationService(dataSource).ensureTableExistence(company);

        tagsService = new TagsService(dataSource);
        tagItemDBService = new TagItemDBService(dataSource);
        bestPracticesService = new BestPracticesService(dataSource);
        questionnaireTemplateDBService = new QuestionnaireTemplateDBService(dataSource);
        ticketTemplateDBService = new TicketTemplateDBService(dataSource, MAPPER);
        stateDBService = new StateDBService(dataSource);
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        ciCdJobRunsDatabaseService = new CiCdJobRunsDatabaseService(DefaultObjectMapper.get(), dataSource);
        jobRunStageDatabaseService = new CiCdJobRunStageDatabaseService(dataSource, MAPPER);
        wItemService = new WorkItemDBService(dataSource, DefaultObjectMapper.get(), productService, stateDBService, true, 100);
        questionnaireDBService = new QuestionnaireDBService(dataSource);

        productService.ensureTableExistence(company);
        tagsService.ensureTableExistence(company);
        tagItemDBService.ensureTableExistence(company);
        bestPracticesService.ensureTableExistence(company);
        questionnaireTemplateDBService.ensureTableExistence(company);
        ticketTemplateDBService.ensureTableExistence(company);
        stateDBService.ensureTableExistence(company);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        ciCdJobRunsDatabaseService.ensureTableExistence(company);
        jobRunStageDatabaseService.ensureTableExistence(company);
        wItemService.ensureTableExistence(company);
        questionnaireDBService.ensureTableExistence(company);
    }

    private void verifyRecord(QuestionnaireTemplate e, QuestionnaireTemplate a) {
        Assert.assertEquals(e.getId(), a.getId());
        Assert.assertEquals(e.getName(), a.getName());
        Assert.assertEquals(e.getLowRiskBoundary(), a.getLowRiskBoundary());
        Assert.assertEquals(e.getMidRiskBoundary(), a.getMidRiskBoundary());
        Assert.assertEquals(e.getSections(), a.getSections());
        Assert.assertEquals(e.getRiskEnabled(), a.getRiskEnabled());
        Assert.assertEquals(CollectionUtils.isEmpty(e.getTagIds()), CollectionUtils.isEmpty(a.getTagIds()));
        if (CollectionUtils.isNotEmpty(e.getTagIds())) {
            Assert.assertEquals(e.getTagIds(), a.getTagIds());
        }
        Assert.assertEquals(CollectionUtils.isEmpty(e.getKbIds()), CollectionUtils.isEmpty(a.getKbIds()));
        if (CollectionUtils.isNotEmpty(e.getKbIds())) {
            Assert.assertEquals(e.getKbIds(), a.getKbIds());
        }
        Assert.assertNotNull(a.getCreatedAt());
        Assert.assertNotNull(a.getUpdatedAt());
    }

    @Test
    public void test() throws SQLException {
        Integer stateId = Integer.parseInt(stateDBService.insert(company, io.levelops.commons.databases.models.database.State.builder().name("Open").build()));
        String productId = ProductUtils.createProducts(productService, company, 1).get(0).getId();
        List<BestPracticesItem> bestPracticesItems = BestPracticesServiceUtils.createBestPracticesItems(bestPracticesService, company, 2);
        List<UUID> kbIds = bestPracticesItems.stream().map(x -> x.getId()).collect(Collectors.toList());

        List<QuestionnaireTemplate> qTemplates = IntStream.range(1,4)
                .mapToObj(i -> QuestionnaireTemplate.builder().name(QUESTIONNAIRE_TEMPLATE_NAME_PREFIX +i)
                        // .createdAt(Instant.now().toEpochMilli())
                        .lowRiskBoundary(1)
                        .midRiskBoundary(2)
                        .riskEnabled(true).kbIds(kbIds).build())
                .collect(Collectors.toList());

        Map<String, QuestionnaireTemplate> questionnaireTemplates = new HashMap<>();
        List<String> expectedIds = new ArrayList<>();
        for (QuestionnaireTemplate qTemplate:qTemplates){
            String id = questionnaireTemplateDBService.insert(company, qTemplate);
            expectedIds.add(id);
            questionnaireTemplates.put(id, qTemplate.toBuilder().id(id).build());
        }

        //Test Get - Also to check KB Ids
        for(String currentId: expectedIds) {
            QuestionnaireTemplate actual = questionnaireTemplateDBService.get(company, currentId).get();
            verifyRecord(questionnaireTemplates.get(currentId), actual);
        }

        //Test Update - Mainly for KB Ids
        QuestionnaireTemplate current = questionnaireTemplates.get(expectedIds.get(0));
        QuestionnaireTemplate updated = current.toBuilder().clearKbIds().build();
        Assert.assertTrue(questionnaireTemplateDBService.update(company, updated));
        questionnaireTemplates.put(expectedIds.get(0), updated);
        QuestionnaireTemplate actual = questionnaireTemplateDBService.get(company, expectedIds.get(0)).get();
        verifyRecord(questionnaireTemplates.get(expectedIds.get(0)), actual);

        List<String> tagIds = new ArrayList<>();
        List<String> tagItemIds = new ArrayList<>();
        for(int i=0; i < 2; i++){
            Tag tag = Tag.builder().name("tag-value" + i).build();
            String tagId = tagsService.insert(company, tag);
            Assert.assertNotNull(tagId);
            tagIds.add(tagId);
            TagItemMapping ti = TagItemMapping.builder()
                    .itemId(expectedIds.get(i))
                    .tagItemType(TagItemMapping.TagItemType.QUESTIONNAIRE_TEMPLATE)
                    .tagId(tagId)
                    .build();
            questionnaireTemplates.put(expectedIds.get(i), questionnaireTemplates.get(expectedIds.get(i)).toBuilder().tagId(tagId).build());
            String tagItemId = tagItemDBService.batchInsert(company, Arrays.asList(ti)).get(0);
            Assert.assertNotNull(tagItemId);
            tagItemIds.add(tagItemId);
        }

        for(int i=0; i < 2; i++){
            DbListResponse<QuestionnaireTemplate> response = questionnaireTemplateDBService.listByFilter(company, 0, 50, Set.of(), null, List.of(tagIds.get(i)));
            Assert.assertEquals(Integer.valueOf(1), response.getCount());
            Assert.assertEquals(expectedIds.get(i), response.getRecords().get(0).getId());
            Assert.assertEquals(1, response.getRecords().get(0).getTagIds().size());
            Assert.assertEquals(tagIds.get(i), response.getRecords().get(0).getTagIds().get(0));
        }
        DbListResponse<QuestionnaireTemplate> response = questionnaireTemplateDBService.listByFilter(company, 0, 50, Set.of(), null,tagIds);
        Assert.assertEquals(Integer.valueOf(2), response.getCount());
        Assert.assertEquals(Set.of(expectedIds.get(0), expectedIds.get(1)), response.getRecords().stream().map(r -> r.getId()).collect(Collectors.toSet()));

        response = questionnaireTemplateDBService.listByFilter(company, 0, 50, Set.of(), expectedIds.stream().map(UUID::fromString).collect(Collectors.toList()), null);
        Assert.assertEquals(Integer.valueOf(3), response.getCount());
        Assert.assertEquals(expectedIds.stream().collect(Collectors.toSet()), response.getRecords().stream().map(r -> r.getId()).collect(Collectors.toSet()));

        for(int i=0; i <expectedIds.size(); i++) {
            response = questionnaireTemplateDBService.listByFilter(company, 0, 50, QUESTIONNAIRE_TEMPLATE_NAME_PREFIX + (i+1), null,null);
            Assert.assertEquals(Integer.valueOf(1), response.getCount());
            Assert.assertEquals(expectedIds.get(i), response.getRecords().get(0).getId());
        }

        for(int i=0; i <expectedIds.size(); i++) {
            Optional<QuestionnaireTemplate> optionalQt = questionnaireTemplateDBService.get(company, expectedIds.get(i));
            Assert.assertNotNull(optionalQt);
            Assert.assertTrue(optionalQt.isPresent());
            QuestionnaireTemplate qt = optionalQt.get();
            Assert.assertEquals(expectedIds.get(i), qt.getId());
            Assert.assertEquals(QUESTIONNAIRE_TEMPLATE_NAME_PREFIX + (i+1), qt.getName());
            Assert.assertEquals(1, qt.getLowRiskBoundary().intValue());
            Assert.assertEquals(2, qt.getMidRiskBoundary().intValue());
            Assert.assertEquals(true, qt.getRiskEnabled());
        }

        response = questionnaireTemplateDBService.list(company, 0, 10);
        Assert.assertEquals(Integer.valueOf(3), response.getCount());
        List<String> responseIds = response.getRecords().stream().map(q -> q.getId()).collect(Collectors.toList());
        Assertions.assertThat(responseIds).containsAll(expectedIds);
        String id = expectedIds.iterator().next();
        Assert.assertFalse(questionnaireTemplateDBService.checkIfUsedInQuestionnaires(company, id).isPresent());

        WorkItem workItem = WorkItem.builder()
                .type(ItemType.AUTOMATED)
                .ticketType(WorkItem.TicketType.WORK_ITEM)
                .productId(productId)
                .notify(false)
                .stateId(stateId)
                .build();
                    
        String wId = wItemService.insert(company, workItem);

        Questionnaire questionnaire = Questionnaire.builder()
            .questionnaireTemplateId(id)
            .workItemId(wId)
            .targetEmail("test@test.io")
            .senderEmail("test@test.io")
            .bucketName("test")
            .bucketPath("test/test")
            .score(0)
            .answered(0)
            .state(State.CREATED)
            .priority(Severity.MEDIUM)
            .messageSent(false)
            .totalQuestions(0)
            .totalPossibleScore(10)
            .createdAt(Instant.now().toEpochMilli())
            .build();
        String questionnaireId = questionnaireDBService.insert(company, questionnaire);
        Assert.assertEquals(true, questionnaireTemplateDBService.checkIfUsedInQuestionnaires(company, id).isPresent());
        questionnaireDBService.deleteAndReturn(company, questionnaireId);
        Assert.assertFalse(questionnaireTemplateDBService.checkIfUsedInQuestionnaires(company, id).isPresent());



        var results = questionnaireTemplateDBService.listByFilter(company, 0, 10, Set.of(QUESTIONNAIRE_TEMPLATE_NAME_PREFIX+1, QUESTIONNAIRE_TEMPLATE_NAME_PREFIX+2), null, null);
        Assertions.assertThat(results.getCount()).isEqualTo(2);
        Assertions.assertThat(results.getRecords().stream().map(item -> item.toBuilder().createdAt(null).updatedAt(null).build()).collect(Collectors.<QuestionnaireTemplate>toList()))
            .containsExactlyInAnyOrderElementsOf(questionnaireTemplates.entrySet().stream()
                .filter(entry -> entry.getValue().getName().equalsIgnoreCase(QUESTIONNAIRE_TEMPLATE_NAME_PREFIX+1) 
                            || entry.getValue().getName().equalsIgnoreCase(QUESTIONNAIRE_TEMPLATE_NAME_PREFIX+2))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList()));
    }

    @Test
    public void testBulkDelete() throws SQLException {
        QuestionnaireTemplate template1 = QuestionnaireTemplate.builder()
                .name("test1")
                .build();
        QuestionnaireTemplate template2 = QuestionnaireTemplate.builder()
                .name("test2")
                .build();
        String id1 = questionnaireTemplateDBService.insert(company, template1);
        String id2 = questionnaireTemplateDBService.insert(company, template2);
        questionnaireTemplateDBService.bulkDeleteAndReturn(company, List.of(id1, id2));
        Assertions.assertThat(questionnaireTemplateDBService.get(company, id1)).isEmpty();
        Assertions.assertThat(questionnaireTemplateDBService.get(company, id2)).isEmpty();
    }
}