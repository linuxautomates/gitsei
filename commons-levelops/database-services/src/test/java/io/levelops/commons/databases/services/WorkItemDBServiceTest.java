package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.BestPracticesItem;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.KvData;
import io.levelops.commons.databases.models.database.Product;
import io.levelops.commons.databases.models.database.Questionnaire;
import io.levelops.commons.databases.models.database.QuestionnaireTemplate;
import io.levelops.commons.databases.models.database.Severity;
import io.levelops.commons.databases.models.database.State;
import io.levelops.commons.databases.models.database.Tag;
import io.levelops.commons.databases.models.database.TicketData;
import io.levelops.commons.databases.models.database.TicketTemplate;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.WorkItem;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.models.database.cicd.JobRunStage;
import io.levelops.commons.databases.models.database.mappings.TagItemMapping;
import io.levelops.commons.databases.models.filters.WorkItemFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.ProductUtils.createProducts;
import static io.levelops.commons.databases.services.QuestionnaireTemplateTestUtils.buildQuestionnaireTemplate;
import static io.levelops.commons.databases.services.QuestionnaireTestUtils.buildQuestionnaire;
import static io.levelops.commons.databases.services.TicketTemplateUtils.buildTicketTemplate;
import static org.assertj.core.api.Assertions.assertThat;

public class WorkItemDBServiceTest {
    private final static Pattern VANITY_ID_PATTERN = Pattern.compile("^(?<key>.*)-(?<seq>.*)$");
    // region Private Data Members
    private final static List<TicketTemplate> EMPTY_TICKET_TEMPLATES = Arrays.asList(null, null, null);
    private final static ObjectMapper mapper = DefaultObjectMapper.get();
    private final static String company = "ploop";
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource ds;
    private static NamedParameterJdbcTemplate template;
    private static IntegrationService its;
    private static UserService userService;
    private static OrganizationService os;
    private static TagsService tagsService;
    private static TagItemDBService tagItemDBService;
    private static BestPracticesService bestPracticesService;
    private static QuestionnaireTemplateDBService questionnaireTemplateDBService;

    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private static CiCdJobRunStageDatabaseService jobRunStageDatabaseService;

    private static WorkItemDBService workItemDBService;
    private static QuestionnaireDBService qs;
    private static TicketTemplateDBService ticketTemplateDBService;
    private static StateDBService stateDBService;
    private static ProductService productService;
    private static List<BestPracticesItem> kbs = null;
    private static List<UUID> kbIds = null;

    private static State state;
    private static String productId;
    private static List<Product> products;
    private static Integration integration;
    private static User user;
    private static CICDInstance cicdInstance;
    private static CICDJob cicdJob;
    private static CICDJobRun cicdJobRun1;
    private static JobRunStage cicdJobRunStage1;
    private static CICDJobRun cicdJobRun2;
    private static CICDJobRun cicdJobRun3;
    private static JobRunStage cicdJobRunStage3;
    // endregion

    // region Verify
    private static void verifyTicketAttachment(WorkItem.Attachment a, WorkItem.Attachment e) {
        Assert.assertEquals(a.getWorkItemId(), e.getWorkItemId());
        Assert.assertEquals(a.getFileName(), e.getFileName());
        Assert.assertEquals(a.getComment(), e.getComment());
        Assert.assertEquals(a.getUploadId(), e.getUploadId());
    }

    private static void verifyTicketAttachments(List<WorkItem.Attachment> a, List<WorkItem.Attachment> e) {
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if (CollectionUtils.isEmpty(a)) {
            return;
        }
        Map<String, WorkItem.Attachment> am = a.stream().collect(Collectors.toMap(WorkItem.Attachment::getComment, v -> v));
        Map<String, WorkItem.Attachment> em = e.stream().collect(Collectors.toMap(WorkItem.Attachment::getComment, v -> v));
        for (String k : am.keySet()) {
            verifyTicketAttachment(am.get(k), em.get(k));
        }
    }

    private static void verifyTicketData(WorkItem a, WorkItem e) {
        Assert.assertEquals(a.getTicketDataValues().size(), e.getTicketDataValues().size());
        for (int i = 0; i < e.getTicketDataValues().size(); ++i) {
            TicketData aData = a.getTicketDataValues().get(i);
            TicketData eData = e.getTicketDataValues().get(i);

            Assert.assertEquals(aData.getData().getKey(), eData.getData().getKey());
            Assert.assertEquals(aData.getWorkItemId(), e.getId());
            Assert.assertEquals(aData.getTicketFieldId(), eData.getTicketFieldId());
            Assert.assertEquals(aData.getData().getValues().size(), eData.getData().getValues().size());
            for (int j = 0; j < eData.getData().getValues().size(); j++) {
                Assert.assertEquals(aData.getData().getValues().get(j).getValue(), eData.getData().getValues().get(j).getValue());
                Assert.assertEquals(aData.getData().getValues().get(j).getType(), eData.getData().getValues().get(j).getType());
            }
            Assert.assertEquals(aData.getData().getType(), eData.getData().getType());
        }
    }

    private static void verifyAssignees(List<WorkItem.Assignee> a, List<WorkItem.Assignee> e) {
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if (CollectionUtils.isEmpty(a)) {
            return;
        }
        Set<String> aIds = a.stream().map(t -> t.getUserId()).collect(Collectors.toSet());
        Set<String> eIds = e.stream().map(t -> t.getUserId()).collect(Collectors.toSet());
        Assert.assertEquals(aIds, eIds);
    }

    private static void verifyChildIds(List<String> a, List<String> e) {
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if (CollectionUtils.isEmpty(a)) {
            return;
        }
        Set<String> aIds = a.stream().collect(Collectors.toSet());
        Set<String> eIds = e.stream().collect(Collectors.toSet());
        Assert.assertEquals(aIds, eIds);
    }

    private static void verifyCiCdMappings(List<WorkItem.CICDMapping> a, List<WorkItem.CICDMapping> e) {
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if (CollectionUtils.isEmpty(a)) {
            return;
        }
        Assert.assertEquals(a.stream().collect(Collectors.toSet()), e.stream().collect(Collectors.toSet()));
    }

    private static void verifyWorkItem(WorkItem a, WorkItem e) {
        Assert.assertEquals(a.getId(), e.getId());
        Assert.assertEquals(a.getType(), e.getType());
        Assert.assertEquals(a.getTicketType(), e.getTicketType());
        Assert.assertEquals(a.getCloudOwner(), e.getCloudOwner());
        Assert.assertEquals(a.getArtifactTitle(), e.getArtifactTitle());
        Assert.assertEquals(a.getDueAt(), e.getDueAt());
        Assert.assertEquals(a.getNotify(), e.getNotify());
        Assert.assertEquals(a.getReason(), e.getReason());
        Assert.assertEquals(a.getProductId(), e.getProductId());
        Assert.assertEquals(a.getIntegrationId(), e.getIntegrationId());
        Assert.assertEquals(a.getArtifact(), e.getArtifact());
        Assert.assertNotNull(a.getCreatedAt());
        Assert.assertNotNull(a.getUpdatedAt());
        verifyAssignees(a.getAssignees(), e.getAssignees());
        Assert.assertEquals(a.getTicketTemplateId(), e.getTicketTemplateId());
        verifyTicketData(a, e);
        Assert.assertEquals(a.getTagIds().stream().collect(Collectors.toSet()), e.getTagIds().stream().collect(Collectors.toSet()));
        Assert.assertEquals(a.getStateId(), e.getStateId());
        Assert.assertEquals(a.getStatus(), e.getStatus());
        verifyTicketAttachments(a.getAttachments(), e.getAttachments());
        Assert.assertEquals(a.getTitle(), e.getTitle());
        Assert.assertEquals(a.getDescription(), e.getDescription());
        Assert.assertEquals(a.getReporter(), e.getReporter());
        Assert.assertTrue(org.apache.commons.lang3.StringUtils.isNoneBlank(a.getVanityId()));
        Assert.assertEquals(a.getParentId(), e.getParentId());
        verifyChildIds(a.getChildIds(), e.getChildIds());
        verifyCiCdMappings(a.getCicdMappings(), e.getCicdMappings());
    }

    public static void verifyWorkItemsList(List<WorkItem> a, List<WorkItem> e) {
        Assert.assertEquals(a.size(), e.size());
        Map<String, WorkItem> am = a.stream().collect(Collectors.toMap(WorkItem::getId, v -> v));
        Map<String, WorkItem> em = e.stream().collect(Collectors.toMap(WorkItem::getId, v -> v));
        for (WorkItem w : e) {
            verifyWorkItem(am.get(w.getId()), em.get(w.getId()));
        }
    }

    private static List<WorkItem.Attachment> createAttachments(int n) {
        List<WorkItem.Attachment> attachments = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            WorkItem.Attachment attachment = WorkItem.Attachment.builder()
                    .uploadId(UUID.randomUUID().toString())
                    .fileName("fileName-" + 1)
                    .comment("comment-" + i)
                    .build();
            attachments.add(attachment);
        }
        return attachments;
    }
    // endregion

    // region Setup
    @BeforeClass
    public static void setup() throws SQLException {
        ds = pg.getEmbeddedPostgres().getPostgresDatabase();
        ds.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        its = new IntegrationService(ds);
        userService = new UserService(ds, mapper);
        os = new OrganizationService(ds);
        tagsService = new TagsService(ds);
        tagItemDBService = new TagItemDBService(ds);
        bestPracticesService = new BestPracticesService(ds);
        questionnaireTemplateDBService = new QuestionnaireTemplateDBService(ds);
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(ds);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(ds);
        ciCdJobRunsDatabaseService = new CiCdJobRunsDatabaseService(DefaultObjectMapper.get(), ds);
        jobRunStageDatabaseService = new CiCdJobRunStageDatabaseService(ds, mapper);
        workItemDBService = new WorkItemDBService(ds, mapper, productService, stateDBService, true, 100);
        qs = new QuestionnaireDBService(ds);
        ticketTemplateDBService = new TicketTemplateDBService(ds, mapper);
        stateDBService = new StateDBService(ds);
        productService = new ProductService(ds);

        workItemDBService.ensureSchemaExistence(company);
        userService.ensureTableExistence(company);
        its.ensureTableExistence(company);
        productService.ensureTableExistence(company);
        os.ensureTableExistence(company);
        tagsService.ensureTableExistence(company);
        tagItemDBService.ensureTableExistence(company);
        bestPracticesService.ensureTableExistence(company);
        questionnaireTemplateDBService.ensureTableExistence(company);
        stateDBService.ensureTableExistence(company);
        ticketTemplateDBService.ensureTableExistence(company);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        ciCdJobRunsDatabaseService.ensureTableExistence(company);
        jobRunStageDatabaseService.ensureTableExistence(company);
        workItemDBService.ensureTableExistence(company);
        qs.ensureTableExistence(company);

        kbs = BestPracticesServiceUtils.createBestPracticesItems(bestPracticesService, company, 2);
        kbIds = kbs.stream().map(x -> x.getId()).collect(Collectors.toList());

        template = new NamedParameterJdbcTemplate(pg.getEmbeddedPostgres().getPostgresDatabase());

        state = createState();
        products = createProducts(productService, company, 2);
        productId = products.get(0).getId();
        integration = testInsertIntegration();
        user = testInsertUser();
        cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, 0);
        cicdJob = CiCdJobUtils.createCICDJob(ciCdJobsDatabaseService, company, 0, cicdInstance);
        cicdJobRun1 = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, cicdJob, company, 0, Instant.now(), 123, null, null);
        cicdJobRunStage1 = JobRunStageUtils.createJobRunStage(jobRunStageDatabaseService, cicdJobRun1, company, 0);
        cicdJobRun2 = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, cicdJob, company, 1, Instant.now(), 456, null, null);
        cicdJobRun3 = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, cicdJob, company, 2, Instant.now(), 789, null, null);
        cicdJobRunStage3 = JobRunStageUtils.createJobRunStage(jobRunStageDatabaseService, cicdJobRun3, company, 2);
    }

    @Before
    public void cleanup() {
        template.update("DELETE FROM " + company + ".workitems", Map.of());
    }

    // region Test Supporting
    private static Integration testInsertIntegration() throws SQLException {
        Integration it = Integration.builder()
                .name("asd")
                .status("OPEN")
                .application("github")
                .url("asda")
                .build();
        String itid = its.insert(company, it);
        return it.toBuilder().id(itid).build();
    }

    private static User testInsertUser() throws SQLException {
        return testInsertUser("asd");
    }

    private static User testInsertUser(String email) throws SQLException {
        User us = User.builder()
                .bcryptPassword("asd")
                .firstName("asd")
                .email(email)
                .lastName("asd")
                .passwordAuthEnabled(Boolean.TRUE)
                .samlAuthEnabled(Boolean.FALSE)
                .userType(RoleType.ADMIN)
                .build();
        String userId = userService.insert(company, us);
        return us.toBuilder().id(userId).build();
    }

    private static State createState() throws SQLException {
        return createState("Open");
    }

    private static State createState(String name) throws SQLException {
        State st = State.builder().name(name).build();
        String stateId = stateDBService.insert(company, st);
        return st.toBuilder().id(Integer.parseInt(stateId)).build();
    }

    TicketTemplate createTicketTemplate(int n, List<String> questionnaireTemplateIds) throws SQLException {
        TicketTemplate tt1 = buildTicketTemplate(n, questionnaireTemplateIds);
        String ticketTemplateId1 = ticketTemplateDBService.insert(company, tt1);
        Assert.assertTrue(StringUtils.isNotBlank(ticketTemplateId1));
        return ticketTemplateDBService.get(company, ticketTemplateId1).get();
    }
    // endregion

    private List<String> createQuestionnaireTemplates() throws SQLException {
        List<String> questionnaireTemplateIds = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            QuestionnaireTemplate qt = buildQuestionnaireTemplate(i + 1, kbIds);
            String qtId = questionnaireTemplateDBService.insert(company, qt);
            Assert.assertTrue(StringUtils.isNotBlank(qtId));
            questionnaireTemplateIds.add(qtId);
        }
        return questionnaireTemplateIds;
    }

    // region Test Utils
    private void testGet(List<WorkItem> workItems) throws SQLException {
        for (WorkItem wi : workItems) {
            Optional<WorkItem> opt = workItemDBService.get(company, wi.getId());
            Assert.assertNotNull(opt);
            Assert.assertTrue(opt.isPresent());
            verifyWorkItem(opt.get(), wi);
        }
    }

    private void testUpdate(List<WorkItem> workItems) throws SQLException {
        int newIndex = workItems.size();
        int index = workItems.size() - 1;
        WorkItem wi = workItems.get(index);
        WorkItem wiUpdated = wi.toBuilder()
                .reason("abcd")
                .ticketType(WorkItem.TicketType.STORY)
                .build();
        Boolean success = workItemDBService.update(company, wiUpdated);
        Assert.assertTrue(success);
        workItems.remove(index);
        workItems.add(index, wiUpdated);
        return;
    }

    private List<String> createTags(int n, String workItemId) throws SQLException {
        List<String> tagIds = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Tag t = Tag.builder()
                    .name("tag-value-" + workItemId + "-" + (i + 1))
                    .build();
            String tagId = tagsService.insert(company, t);
            Assert.assertTrue(StringUtils.isNotBlank(tagId));
            TagItemMapping tagItemMapping = TagItemMapping.builder()
                    .itemId(workItemId).tagId(tagId).tagItemType(WorkItem.ITEM_TYPE)
                    .build();
            String tagItemMappingId = tagItemDBService.insert(company, tagItemMapping);
            Assert.assertTrue(StringUtils.isNotBlank(tagItemMappingId));
            tagIds.add(tagId);
        }
        return tagIds;
    }

    private WorkItem testInsert(int n, WorkItem.ItemType itemType, String productId, String integrationId, String userId, TicketTemplate ticketTemplate, State state, String parentId, List<WorkItem.CICDMapping> ciCdMappings) throws SQLException {
        WorkItem.WorkItemBuilder bldr = WorkItem.builder()
                .reason("asd" + n)
                .title("title" + n)
                //.artifact("asd")
                //.integrationId(integrationId)
                //.assignee(WorkItem.Assignee.builder().userId(Integer.parseInt(userId)).build())
                .type(itemType)
                .notify(Boolean.FALSE)
                .title("title" + n)
                .description("description" + n)
                .reporter("abc" + n + "@test.com")
                .productId(productId)
                .stateId(state.getId())
                .status(state.getName())
                .ticketType(WorkItem.TicketType.WORK_ITEM)
                .attachments(createAttachments(n))
                .dueAt(System.currentTimeMillis())
                .artifactTitle("artifactTitle" + n)
                .cloudOwner("cloudOwner" + n);

        if (ticketTemplate != null) {
            String ticketTemplateId = ticketTemplate.getId();
            bldr.ticketTemplateId(ticketTemplateId);
            for (int i = 0; i < 2; i++) {
                KvData.Value v1 = KvData.Value.builder().value("abc").type("t1").build();
                bldr.ticketDataValue(TicketData.builder()
                        .ticketFieldId(ticketTemplate.getTicketFields().get(i).getId())
                        .data(KvData.builder().values(List.of(v1)).build())
                        .build());
            }
        }
        if (org.apache.commons.lang3.StringUtils.isNotBlank(parentId)) {
            bldr.parentId(parentId);
        }
        if ((n % 2) == 0) {
            bldr.artifact("asd" + n);
            bldr.integrationId(integrationId);
            bldr.assignee(WorkItem.Assignee.builder().userId(userId).build());
        }

        if (CollectionUtils.isNotEmpty(ciCdMappings)) {
            bldr.cicdMappings(ciCdMappings);
        }

        WorkItem wi = bldr.build();
        String workItemId = workItemDBService.insert(company, wi);
        Assert.assertNotNull(workItemId);
        List<String> tagIds = createTags(2, workItemId);
        List<WorkItem.Attachment> attachments = wi.getAttachments().stream().map(a -> a.toBuilder().workItemId(workItemId).build()).collect(Collectors.toList());
        return wi.toBuilder().id(workItemId).clearAttachments().attachments(attachments).tagIds(tagIds).build();
    }

    private List<WorkItem> testInserts(WorkItem.ItemType itemType, String productId, String integrationId, String userId, int n, List<TicketTemplate> ticketTemplates, State state, List<WorkItem.CICDMapping> ciCdMappings, boolean testChildWorkItems) throws SQLException {
        List<WorkItem> workItems = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            WorkItem wi = testInsert(i, itemType, productId, integrationId, userId, ticketTemplates.get(i), state, null, ciCdMappings);
            workItems.add(wi);
            testGet(workItems);
        }
        for (int i = 0; i < n; i++) {
            WorkItem parent = workItems.get(i);
            WorkItem wi = testInsert(n + i, itemType, productId, integrationId, userId, ticketTemplates.get(i), state, parent.getId(), ciCdMappings);
            workItems.add(wi);
            WorkItem updatedParent = parent.toBuilder().childId(wi.getId()).build();
            workItems.set(i, updatedParent);
            testGet(workItems);
        }
        return workItems;
    }

    private List<WorkItem> testInserts(WorkItem.ItemType itemType, String productId, String integrationId, String userId, int n, List<TicketTemplate> ticketTemplates, State state, List<WorkItem.CICDMapping> ciCdMappings) throws SQLException {
        return testInserts(itemType, productId, integrationId, userId, n, ticketTemplates, state, ciCdMappings, false);
    }
    // endregion

    // region Test List By Filter
    private void testListByFilterIds(List<WorkItem> workItems) throws SQLException {
        List<UUID> ids = workItems.stream().map(w -> UUID.fromString(w.getId())).collect(Collectors.toList());
        DbListResponse<WorkItem> response = workItemDBService.listByFilter(company, 0, 100, ids, null,
                false, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null);
        Assert.assertNotNull(response);
        Assert.assertEquals(workItems.size(), response.getTotalCount().intValue());
        Assert.assertEquals(workItems.size(), response.getCount().intValue());
        verifyWorkItemsList(response.getRecords(), workItems);
    }

    private void testListByFilterReporter(List<WorkItem> workItems) throws SQLException {
        for (WorkItem w : workItems) {
            DbListResponse<WorkItem> response = workItemDBService.listByFilter(company, 0, 50, null, null,
                    false, null, null, null, null, Set.of(w.getReporter()), null,
                    null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null);
            Assert.assertNotNull(response);
            Assert.assertEquals(1, response.getTotalCount().intValue());
            Assert.assertEquals(1, response.getCount().intValue());
            verifyWorkItem(response.getRecords().get(0), w);
        }
    }

    private void testListByFilterTitle(List<WorkItem> workItems) throws SQLException {
        for (WorkItem w : workItems) {
            DbListResponse<WorkItem> response = workItemDBService.listByFilter(company, 0, 100, null, w.getTitle(),
                    false, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null);
            Assert.assertNotNull(response);
            Assert.assertEquals(1, response.getTotalCount().intValue());
            Assert.assertEquals(1, response.getCount().intValue());
            verifyWorkItem(response.getRecords().get(0), w);
        }
        DbListResponse<WorkItem> response = workItemDBService.listByFilter(company, 0, 100, null, "title",
                false, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null,null, null,  null, null, null, null, null);
        Assert.assertNotNull(response);
        Assert.assertEquals(workItems.size(), response.getTotalCount().intValue());
        Assert.assertEquals(workItems.size(), response.getCount().intValue());
        verifyWorkItemsList(response.getRecords(), workItems);
    }

    private void testListByFilterUnAssigned(List<WorkItem> workItems) throws SQLException {
        List<WorkItem> expected = workItems.stream().filter(w -> CollectionUtils.isEmpty(w.getAssignees())).collect(Collectors.toList());
        DbListResponse<WorkItem> response = workItemDBService.listByFilter(company, 0, 100, null, null,
                true, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null,null, null,  null, null, null, null, null);
        Assert.assertNotNull(response);
        Assert.assertEquals(expected.size(), response.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), response.getCount().intValue());
        verifyWorkItemsList(response.getRecords(), expected);
    }

    private void testListByFilterAssigned(List<WorkItem> workItems) throws SQLException {
        List<WorkItem> expected = workItems.stream().filter(w -> CollectionUtils.isNotEmpty(w.getAssignees())).collect(Collectors.toList());
        List<Integer> assigneeUserIds = expected.stream().map(w -> w.getAssignees()).flatMap(List::stream).map(a -> a.getUserId()).map(i -> Integer.parseInt(i)).collect(Collectors.toList());
        DbListResponse<WorkItem> response = workItemDBService.listByFilter(company, 0, 100, null, null,
                false, assigneeUserIds, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null,null, null,  null, null, null, null, null);
        Assert.assertNotNull(response);
        Assert.assertEquals(expected.size(), response.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), response.getCount().intValue());
        verifyWorkItemsList(response.getRecords(), expected);
    }

    private void testListByFilterTicketTempateId(List<WorkItem> workItems, List<String> ticketTemplateIds) throws SQLException {
        if (CollectionUtils.isEmpty(ticketTemplateIds)) {
            return;
        }
        List<UUID> qtIds = ticketTemplateIds.stream().map(i -> UUID.fromString(i)).collect(Collectors.toList());
        DbListResponse<WorkItem> response = workItemDBService.listByFilter(company, 0, 100, null, null,
                false, null, qtIds, null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null);
        Assert.assertNotNull(response);
        Assert.assertEquals(2, response.getTotalCount().intValue());
        Assert.assertEquals(2, response.getCount().intValue());
        verifyWorkItem(response.getRecords().get(0), workItems.stream()
                .filter(a -> a.getId().equals(response.getRecords().get(0).getId())).findFirst().get());
        verifyWorkItem(response.getRecords().get(1), workItems.stream()
                .filter(a -> a.getId().equals(response.getRecords().get(1).getId())).findFirst().get());
    }

    private void testListByFilterTagId(List<WorkItem> workItems) throws SQLException {
        DbListResponse<WorkItem> dbListResponse = null;
        for (WorkItem w : workItems) {
            for (String tagId : w.getTagIds()) {
                dbListResponse = workItemDBService.listByFilter(company, 0, 100, null, null,
                        false, null, null, Arrays.asList(tagId), null,
                        null, null, null, null, null, null, null, null, null, null,
                        null,null, null,  null, null, null, null, null);
                Assert.assertTrue(1 == dbListResponse.getTotalCount());
                Assert.assertEquals(1, dbListResponse.getRecords().size());
                verifyWorkItemsList(dbListResponse.getRecords(), Arrays.asList(w));
            }
            dbListResponse = workItemDBService.listByFilter(company, 0, 100, null, null,
                    false, null, null, w.getTagIds(), null, null,
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null);
            Assert.assertTrue(1 == dbListResponse.getTotalCount());
            Assert.assertEquals(1, dbListResponse.getRecords().size());
            verifyWorkItemsList(dbListResponse.getRecords(), Arrays.asList(w));
        }
    }

    private void testListByFilterProductId(List<WorkItem> workItems) throws SQLException {
        List<Integer> productIds = workItems.stream().map(w -> Integer.parseInt(w.getProductId())).distinct().collect(Collectors.toList());
        DbListResponse<WorkItem> dbListResponse = workItemDBService.listByFilter(company, 0, 100, null, null,
                false, null, null, null, null, null, productIds,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null);
        Assert.assertTrue(workItems.size() == dbListResponse.getTotalCount());
        Assert.assertEquals(workItems.size(), dbListResponse.getRecords().size());
        verifyWorkItemsList(dbListResponse.getRecords(), workItems);
    }

    private void testListByFilterType(List<WorkItem> workItems) throws SQLException {
        Map<WorkItem.ItemType, List<WorkItem>> map = new HashMap<>();
        for (WorkItem w : workItems) {
            WorkItem.ItemType type = w.getType();
            if (type == null) {
                continue;
            }
            if (!map.containsKey(type)) {
                map.put(type, new ArrayList<>());
            }
            map.get(type).add(w);
        }
        for (WorkItem.ItemType type : map.keySet()) {
            DbListResponse<WorkItem> response = workItemDBService.listByFilter(company, 0, 100, null, null,
                    false, null, null, null, null, null,
                    null, type.toString(), null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null);
            Assert.assertNotNull(response);
            Assert.assertEquals(map.get(type).size(), response.getTotalCount().intValue());
            Assert.assertEquals(map.get(type).size(), response.getCount().intValue());
            verifyWorkItemsList(response.getRecords(), map.get(type));
        }
    }

    private void testListByFilterStatus(List<WorkItem> workItems) throws SQLException {
//        Map<WorkItem.ItemStatus, List<WorkItem>> map  = new HashMap<>();
//        for (WorkItem w : workItems){
//            WorkItem.ItemStatus s = w.getStatus();
//            if(s == null){
//                continue;
//            }
//            if (!map.containsKey(s)){
//                map.put(s, new ArrayList<>());
//            }
//            map.get(s).add(w);
//        }
//        for(WorkItem.ItemStatus s : map.keySet()){
//            DbListResponse<WorkItem> response = workItemDBService.listByFilter(company,0,100, null, null,
//                    false, null, null, null, null, null, null, s.toString(), null, null,
//                    null, null, null, null, null, null);
//            Assert.assertNotNull(response);
//            Assert.assertEquals(map.get(s).size(), response.getTotalCount().intValue());
//            Assert.assertEquals(map.get(s).size(), response.getCount().intValue());
//            verifyWorkItemsList(response.getRecords(), map.get(s));
//        }
    }

    private void testListByFilterPriority(List<WorkItem> workItems) throws SQLException {
        Map<Severity, List<WorkItem>> map = new HashMap<>();
        for (WorkItem w : workItems) {
            Severity p = w.getPriority();
            if (p == null) {
                continue;
            }
            if (!map.containsKey(p)) {
                map.put(p, new ArrayList<>());
            }
            map.get(p).add(w);
        }
        for (Severity s : map.keySet()) {
            DbListResponse<WorkItem> response = workItemDBService.listByFilter(company, 0, 100,
                    null, null, false, null, null, null, null,
                    null, null, null, null, s, null, null, null,
                    null, null, null,null, null, null, null,
                    null, null, null);
            Assert.assertNotNull(response);
            Assert.assertEquals(map.get(s).size(), response.getTotalCount().intValue());
            Assert.assertEquals(map.get(s).size(), response.getCount().intValue());
            verifyWorkItemsList(response.getRecords(), map.get(s));
        }
    }

    private void testListByFilterArtifact(List<WorkItem> workItems) throws SQLException {
        for (WorkItem w : workItems) {
            if (org.apache.commons.lang3.StringUtils.isBlank(w.getArtifact())) {
                continue;
            }
            DbListResponse<WorkItem> response = workItemDBService.listByFilter(company, 0, 100, null, null,
                    false, null, null, null, null, null, null,
                    null, null, null,  w.getArtifact(), null, null, null, null, null, null,
                    null, null, null, null, null, null);
            Assert.assertNotNull(response);
            Assert.assertEquals(1, response.getTotalCount().intValue());
            Assert.assertEquals(1, response.getCount().intValue());
            verifyWorkItem(response.getRecords().get(0), w);
        }
    }

    private void testListByFilterArtifactTitle(List<WorkItem> workItems) throws SQLException {
        for (WorkItem w : workItems) {
            if (org.apache.commons.lang3.StringUtils.isBlank(w.getArtifact())) {
                continue;
            }
            DbListResponse<WorkItem> response = workItemDBService.listByFilter(company, 0, 100, null, null,
                    false, null, null, null, null, null, null,
                    null, null, null, null, w.getArtifactTitle(), null, null,
                    null, null, null, null, null, null, null,
                    null, null);
            Assert.assertNotNull(response);
            Assert.assertEquals(1, response.getTotalCount().intValue());
            Assert.assertEquals(1, response.getCount().intValue());
            verifyWorkItem(response.getRecords().get(0), w);
        }
    }

    // endregion
    private void testListByFilter(List<WorkItem> workItems, List<String> ticketTemplateId) throws SQLException {
        testListByFilterIds(workItems);
        testListByFilterTitle(workItems);
        testListByFilterUnAssigned(workItems);
        testListByFilterAssigned(workItems);
        testListByFilterTicketTempateId(workItems, ticketTemplateId);
        testListByFilterTagId(workItems);
        testListByFilterProductId(workItems);
        testListByFilterType(workItems);
        testListByFilterStatus(workItems);
        testListByFilterPriority(workItems);
        testListByFilterArtifact(workItems);
        testListByFilterArtifactTitle(workItems);
        testListByFilterReporter(workItems);
    }

    @Test
    public void testBatchInsert() throws SQLException {
        // State state = createState();
        // String productId = createProducts(productService, company, 1).get(0).getId();
        // Integration integration = testInsertIntegration();
        // User user = testInsertUser();
        // String policyId = testInsertPolicy();

        List<WorkItem.CICDMapping> ciCdMappings1 = List.of(WorkItem.CICDMapping.builder().cicdJobRunId(cicdJobRun1.getId()).cicdJobRunStageId(cicdJobRunStage1.getId()).build(),
                WorkItem.CICDMapping.builder().cicdJobRunId(cicdJobRun2.getId()).build());
        List<WorkItem.CICDMapping> ciCdMappings2 = List.of(WorkItem.CICDMapping.builder().cicdJobRunId(cicdJobRun3.getId()).cicdJobRunStageId(cicdJobRunStage3.getId()).build());
        List<WorkItem> workItems = new ArrayList<>();
        workItems.add(testInsert(0, WorkItem.ItemType.AUTOMATED, productId, integration.getId(), user.getId(), null, state, null, ciCdMappings1));
        workItems.add(testInsert(1, WorkItem.ItemType.AUTOMATED, productId, integration.getId(), user.getId(), null, state, null, ciCdMappings2));
        workItems.add(testInsert(2, WorkItem.ItemType.AUTOMATED, productId, integration.getId(), user.getId(), null, state, null, null));

        testUpdate(workItems);
        testListByFilter(workItems, null);

        workItemDBService.listByFilter(company, 0, 100, null, "", false, null, null, null, null, null, null,
                "AUTOMATED", "", Severity.fromIntValue(1), null, null, null, null,null, null,  null, null, null, null, null, null, null);
        workItemDBService.listByFilter(company, 0, 100, null, null, false, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, 1, null, null, null);
        workItemDBService.listByFilter(company, 0, 100, Collections.singletonList(UUID.fromString(workItems.get(0).getId())), null, false, Collections.singletonList(1),
                null, null, null, null, null, null, "OPEN", null,
                null, null, null, null, null, null, null, null, null, null, null, null, 1);

        DbListResponse<WorkItem> dbListResponse = workItemDBService.listByFilter(company, 0, 100, null, null, false, null, null, null,
                 "", null, null, null, null, null, null, null, List.of(cicdJobRun1.getId()), null, null, null, null, null, null, null, null, null, null);
        Assert.assertEquals(1, dbListResponse.getCount().intValue());
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        verifyWorkItem(dbListResponse.getRecords().get(0), workItems.get(0));

        dbListResponse = workItemDBService.listByFilter(company, 0, 100, null, null, false, null, null, null, null, null, null,
                null, "", null, null, null, List.of(cicdJobRun2.getId()), null, null, null, null, null, null, null, null, null, null);
        Assert.assertEquals(1, dbListResponse.getCount().intValue());
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        verifyWorkItem(dbListResponse.getRecords().get(0), workItems.get(0));
 
        dbListResponse = workItemDBService.listByFilter(company, 0, 100, null, null, false, null, null, null, null, null,
                null, "", null, null, null, null, List.of(cicdJobRun3.getId()), null, null, null, null, null, null, null, null, null, null);
        Assert.assertEquals(1, dbListResponse.getCount().intValue());
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        verifyWorkItem(dbListResponse.getRecords().get(0), workItems.get(1));

        dbListResponse = workItemDBService.listByFilter(company, 0, 100, null, null, false, null, null, null, null, null,
                null, "", null, null, null, null, List.of(cicdJobRun1.getId(), cicdJobRun2.getId()), null, null, null, null, null, null, null, null, null, null);
        Assert.assertEquals(1, dbListResponse.getCount().intValue());
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        verifyWorkItem(dbListResponse.getRecords().get(0), workItems.get(0));

        dbListResponse = workItemDBService.listByFilter(company, 0, 100, null, null, false, null, null, null, null, null,
                null, "", null, null, null, null, List.of(cicdJobRun1.getId(), cicdJobRun3.getId()), null, null, null, null, null, null, null, null, null, null);
        Assert.assertEquals(2, dbListResponse.getCount().intValue());
        Assert.assertEquals(2, dbListResponse.getTotalCount().intValue());
        verifyWorkItemsList(dbListResponse.getRecords(), List.of(workItems.get(0), workItems.get(1)));

        dbListResponse = workItemDBService.listByFilter(company, 0, 100, null, null, false, null, null, null, null, null,
                null, "", null, null, null, null, List.of(cicdJobRun1.getId(), cicdJobRun2.getId(), cicdJobRun3.getId()), null, null, null, null, null, null, null, null, null, null);
        Assert.assertEquals(2, dbListResponse.getCount().intValue());
        Assert.assertEquals(2, dbListResponse.getTotalCount().intValue());
        verifyWorkItemsList(dbListResponse.getRecords(), List.of(workItems.get(0), workItems.get(1)));

        dbListResponse = workItemDBService.listByFilter(company, 0, 100, null, null, false, null, null, null, null, null,
                null, "", null, null, null, null, null, List.of(cicdJobRunStage1.getId()), null, null,  null, null, null, null, null, null, null);
        Assert.assertEquals(1, dbListResponse.getCount().intValue());
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        verifyWorkItem(dbListResponse.getRecords().get(0), workItems.get(0));

        dbListResponse = workItemDBService.listByFilter(company, 0, 100, null, null, false, null, null, null, null, null,
                null, "", null, null, null, null, null, List.of(cicdJobRunStage3.getId()), null, null, null, null, null, null, null, null, null);
        Assert.assertEquals(1, dbListResponse.getCount().intValue());
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        verifyWorkItem(dbListResponse.getRecords().get(0), workItems.get(1));

        dbListResponse = workItemDBService.listByFilter(company, 0, 100, null, null, false, null, null, null, null, null,
                null, "", null, null, null, null, null, List.of(cicdJobRunStage1.getId(), cicdJobRunStage3.getId()), null, null, null, null, null, null, null, null, null);
        Assert.assertEquals(2, dbListResponse.getCount().intValue());
        Assert.assertEquals(2, dbListResponse.getTotalCount().intValue());
        verifyWorkItemsList(dbListResponse.getRecords(), List.of(workItems.get(0), workItems.get(1)));

        // no exceptions is valid test..
    }

    @Test
    public void testBatchInsertManualWI() throws SQLException {
        // State state = createState();
        // String productId = createProducts(productService, company, 1).get(0).getId();
        // Integration integration = testInsertIntegration();
        // User user = testInsertUser();
        // String policyId = testInsertPolicy();

        List<WorkItem> workItems = testInserts(WorkItem.ItemType.MANUAL, productId, integration.getId(), user.getId(), 2, EMPTY_TICKET_TEMPLATES, state, null);
        testUpdate(workItems);
        testListByFilter(workItems, null);

        workItemDBService.listByFilter(company, 0, 100, null, "", false, null, null, null, null, null, null,
                "AUTOMATED", "", Severity.fromIntValue(1), null, null, null, null, null, null, null, null, null, null, null, null, null);
        workItemDBService.listByFilter(company, 0, 100, null, null, false, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,null, null,  null, null, 1, null, null, null);
        workItemDBService.listByFilter(company, 0, 100, Collections.singletonList(UUID.fromString(workItems.get(0).getId())), null, false, Collections.singletonList(1),
                null, null, null, null, null, null,
                "OPEN", null, null, null, null, null, null, null, null, null, null, null, null, null, 1);
        // no exceptions is valid test..
    }

    private void testGetByVanitySequenceNumber(Map<String, WorkItem> vanityIdWorkItemMap) throws SQLException {
        for (String vanityId : vanityIdWorkItemMap.keySet()) {
            Matcher matcher = VANITY_ID_PATTERN.matcher(vanityId);
            Assert.assertTrue(matcher.matches());
            String key = matcher.group("key");
            Long seqNumber = Long.parseLong(matcher.group("seq"));
            Optional<WorkItem> opt = workItemDBService.getByVanitySequenceNumber(company, key, seqNumber);
            Assert.assertNotNull(opt);
            Assert.assertTrue(opt.isPresent());
            verifyWorkItem(opt.get(), vanityIdWorkItemMap.get(vanityId));
            Assert.assertEquals(opt.get().getVanityId().toUpperCase(), vanityId.toUpperCase());
            opt = workItemDBService.getByVanitySequenceNumber(company, key, seqNumber * 8);
            Assert.assertNotNull(opt);
            Assert.assertTrue(opt.isEmpty());
        }
    }

    private List<WorkItem> testMoveProducts(List<WorkItem> workItems, Product newProduct, Map<String, WorkItem> vanityIdWorkItemMap) throws SQLException {
        List<WorkItem> movedWorkItems = new ArrayList<>();
        for (WorkItem w : workItems) {
            /*
            if(newW.getParentId() != null){
                movedWorkItems.add(newW);
                continue;
            }*/
            Long seqNumber = workItemDBService.updateProductId(company, UUID.fromString(w.getId()), newProduct.getId());
            String vanityId = newProduct.getKey() + "-" + seqNumber;
            var newW = w.toBuilder().productId(newProduct.getId()).build();
            vanityIdWorkItemMap.put(vanityId, newW);
            movedWorkItems.add(newW);
            testGetByVanitySequenceNumber(vanityIdWorkItemMap);
        }
        return movedWorkItems;
    }

    @Test
    public void testBatchInsertWithTicketId() throws SQLException {

        List<String> questionnaireTemplateIds = createQuestionnaireTemplates();
        TicketTemplate tt1 = createTicketTemplate(1, questionnaireTemplateIds);

        List<String> questionnaireIds = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Questionnaire q = buildQuestionnaire(1, null, questionnaireTemplateIds.get(i), kbIds);
            String qId = qs.insert(company, q);
            Assert.assertTrue(StringUtils.isNotBlank(qId));
            questionnaireIds.add(qId);
        }

        Map<String, WorkItem> vanityIdWorkItemMap = new HashMap<>();

        List<WorkItem> workItems = testInserts(WorkItem.ItemType.MANUAL, products.get(0).getId(), integration.getId(), user.getId(), 2, Arrays.asList(null, tt1), state, null);
        testListByFilter(workItems, List.of(tt1.getId()));
        testUpdate(workItems);
        workItems = testMoveProducts(workItems, products.get(1), vanityIdWorkItemMap);
        testListByFilter(workItems, List.of(tt1.getId()));

        workItemDBService.listByFilter(company, 0, 100, null, "", false, null, null, null, null, null, null,
                "AUTOMATED", "", Severity.fromIntValue(1), null, null, null, null, null, null, null, null, null, null, null, null, null);
        workItemDBService.listByFilter(company, 0, 100, null, null, false, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,null, null,  null, null, 1, null, null, null);
        workItemDBService.listByFilter(company, 0, 100, Collections.singletonList(UUID.fromString(workItems.get(0).getId())), null, false, Collections.singletonList(1),
                null, null, null, null, null, null,
                "OPEN", null, null, null, null, null, null,null, null,
                null, null, null, null, null, 1);
    }

    @Test
    public void testTicketMoveProductsNonRecursively() throws SQLException {

        List<String> questionnaireTemplateIds = createQuestionnaireTemplates();
        TicketTemplate tt1 = createTicketTemplate(1, questionnaireTemplateIds);

        List<String> questionnaireIds = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Questionnaire q = buildQuestionnaire(1, null, questionnaireTemplateIds.get(i), kbIds);
            String qId = qs.insert(company, q);
            Assert.assertTrue(StringUtils.isNotBlank(qId));
            questionnaireIds.add(qId);
        }

        List<WorkItem> workItems = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            String parentId = null;
            if (i > 0) {
                parentId = workItems.get(i - 1).getId();
            }
            WorkItem w = testInsert(2, WorkItem.ItemType.MANUAL, products.get(0).getId(), integration.getId(), user.getId(), tt1, state, parentId, null);
            workItems.add(w);
            if (i > 0) {
                WorkItem prev = workItems.get(i - 1);
                WorkItem prevWithChild = prev.toBuilder().childId(w.getId()).build();
                workItems.set(i - 1, prevWithChild);
            }
        }
        Pattern productPattern1 = Pattern.compile("^" + products.get(0).getKey().toUpperCase() + "-\\d*$");
        for (int i = 0; i < 8; i++) {
            WorkItem w = workItems.get(i);
            Optional<WorkItem> opt = workItemDBService.get(company, w.getId());
            Assert.assertNotNull(opt);
            Assert.assertTrue(opt.isPresent());
            WorkItem actual = opt.get();
            verifyWorkItem(actual, w);
            Matcher matcher = productPattern1.matcher(actual.getVanityId());
            Assert.assertTrue(matcher.matches());
        }

        int itemsChanged = 1;
        for (int i = 0; i < itemsChanged; i++) {
            WorkItem a = workItems.get(i);
            WorkItem m = a.toBuilder().productId(products.get(1).getId()).build();
            workItems.set(i, m);
        }

        workItemDBService.updateProductId(company, UUID.fromString(workItems.get(0).getId()), workItems.get(0).getProductId());

        Pattern productPattern2 = Pattern.compile("^" + products.get(1).getKey().toUpperCase() + "-\\d*$");
        for (int i = 0; i < 8; i++) {
            WorkItem w = workItems.get(i);
            Optional<WorkItem> opt = workItemDBService.get(company, w.getId());
            Assert.assertNotNull(opt);
            Assert.assertTrue(opt.isPresent());
            WorkItem actual = opt.get();
            verifyWorkItem(actual, w);
            Matcher matcher = null;
            if (i < itemsChanged) {
                matcher = productPattern2.matcher(actual.getVanityId());
            } else {
                matcher = productPattern1.matcher(actual.getVanityId());
            }
            Assert.assertTrue(matcher.matches());
        }
    }

    @Test
    public void testTicketMoveProductCircular() throws SQLException {
        // State state = createState();
        // List<Product> products = createProducts(productService, company, 2);
        // Integration integration = testInsertIntegration();
        // User user = testInsertUser();
        // String policyId = testInsertPolicy();

        List<String> questionnaireTemplateIds = createQuestionnaireTemplates();
        TicketTemplate tt1 = createTicketTemplate(2, questionnaireTemplateIds);

        List<String> questionnaireIds = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Questionnaire q = buildQuestionnaire(1, null, questionnaireTemplateIds.get(i), kbIds);
            String qId = qs.insert(company, q);
            Assert.assertTrue(StringUtils.isNotBlank(qId));
            questionnaireIds.add(qId);
        }

        Pattern productPattern0 = Pattern.compile("^" + products.get(0).getKey().toUpperCase() + "-\\d*$");
        Pattern productPattern1 = Pattern.compile("^" + products.get(1).getKey().toUpperCase() + "-\\d*$");

        String vanityId1 = null;
        String vanityId2 = null;

        WorkItem w = testInsert(2, WorkItem.ItemType.MANUAL, products.get(0).getId(), integration.getId(), user.getId(), tt1, state, null, null);
        Optional<WorkItem> opt = workItemDBService.get(company, w.getId());
        Assert.assertNotNull(opt);
        Assert.assertTrue(opt.isPresent());
        WorkItem actual = opt.get();
        Matcher matcher = productPattern0.matcher(actual.getVanityId());
        Assert.assertTrue(matcher.matches());
        vanityId1 = actual.getVanityId();

        w = w.toBuilder().productId(products.get(1).getId()).build();
        workItemDBService.updateProductId(company, UUID.fromString(w.getId()), products.get(1).getId());
        opt = workItemDBService.get(company, w.getId());
        Assert.assertNotNull(opt);
        Assert.assertTrue(opt.isPresent());
        actual = opt.get();
        matcher = productPattern1.matcher(actual.getVanityId());
        Assert.assertTrue(matcher.matches());


        w = w.toBuilder().productId(products.get(0).getId()).build();
        workItemDBService.updateProductId(company, UUID.fromString(w.getId()), products.get(0).getId());
        opt = workItemDBService.get(company, w.getId());
        Assert.assertNotNull(opt);
        Assert.assertTrue(opt.isPresent());
        actual = opt.get();
        matcher = productPattern0.matcher(actual.getVanityId());
        Assert.assertTrue(matcher.matches());
        vanityId2 = actual.getVanityId();

        Assert.assertEquals(vanityId1, vanityId2);

        WorkItem w2 = testInsert(2, WorkItem.ItemType.MANUAL, products.get(0).getId(), integration.getId(), user.getId(), tt1, state, null, null);
        opt = workItemDBService.get(company, w2.getId());
        Assert.assertNotNull(opt);
        Assert.assertTrue(opt.isPresent());
        actual = opt.get();
        matcher = productPattern0.matcher(actual.getVanityId());
        Assert.assertTrue(matcher.matches());
        String vanityId3 = actual.getVanityId();
    }

//    automated ticket uniqueness is not required anymore.
//    @Test
//    public void testTicketAutomatedWIUpdate() throws SQLException {
//        // State state = createState();
//        // List<Product> products = createProducts(productService, company, 2);
//        // Integration integration = testInsertIntegration();
//        // User user = testInsertUser();
//        // String policyId = testInsertPolicy();
//
//        List<String> questionnaireTemplateIds = createQuestionnaireTemplates();
//        TicketTemplate tt1 = createTicketTemplate(2, questionnaireTemplateIds);
//
//        List<String> questionnaireIds = new ArrayList<>();
//        for (int i = 0; i < 2; i++) {
//            Questionnaire q = buildQuestionnaire(1, null, questionnaireTemplateIds.get(i), kbIds);
//            String qId = qs.insert(company, q);
//            Assert.assertTrue(StringUtils.isNotBlank(qId));
//            questionnaireIds.add(qId);
//        }
//
//        WorkItem w = testInsert(0, WorkItem.ItemType.AUTOMATED, products.get(0).getId(), integration.getId(), user.getId(), tt1, state, null, null);
//        testGet(List.of(w));
//
//        WorkItem w2 = w.toBuilder().artifactTitle("artifactTitle2").cloudOwner("cloudOwner2").build();
//        String workItemId2 = workItemDBService.insert(company, w2);
//        Assert.assertNotNull(workItemId2);
//        Assert.assertEquals(w.getId(), workItemId2);
//        w2 = w2.toBuilder().id(workItemId2).build();
//        testGet(List.of(w2));
//    }

    @Test
    public void testTicketDataUpdate() throws SQLException {
        // State state = createState();
        // List<Product> products = createProducts(productService, company, 2);
        // Integration integration = testInsertIntegration();
        // User user = testInsertUser();
        // String policyId = testInsertPolicy();

        List<String> questionnaireTemplateIds = createQuestionnaireTemplates();
        TicketTemplate tt1 = createTicketTemplate(2, questionnaireTemplateIds);

        WorkItem w = testInsert(0, WorkItem.ItemType.MANUAL, products.get(0).getId(), integration.getId(), user.getId(), tt1, state, null, null);
        w = workItemDBService.get(company, w.getId()).get();

        List<TicketData> dataValues = w.getTicketDataValues();
        List<TicketData> newDataValues = new ArrayList<>();
        KvData kvData = KvData.builder().values(Arrays.asList(KvData.Value.builder().value("v1").type("t1").build())).build();
        TicketData newTicketData = TicketData.builder().workItemId(w.getId()).ticketFieldId(tt1.getTicketFields().get(2).getId()).data(kvData).build();

        newDataValues.add(dataValues.get(0));
        newDataValues.add(newTicketData);

        w = w.toBuilder().clearTicketDataValues().ticketDataValues(newDataValues).build();

        workItemDBService.update(company, w);
        WorkItem actual = workItemDBService.get(company, w.getId()).get();
        verifyWorkItem(actual, w);
    }

    // region AGGS

    @Test
    public void testCountAggs() throws SQLException, JsonProcessingException {
        // -- SETUP
        State stateA = createState("a");
        State stateB = createState("b");
        State stateC = createState("c");
        String userA = testInsertUser("userA").getId();
        String userB = testInsertUser("userB").getId();
        String tagA = createTag();
        String tagB = createTag();
        String tagC = createTag();
        long d1 = DateUtils.toEpochSecond(Instant.parse("2020-01-02T00:00:00Z"));
        long d2 = DateUtils.toEpochSecond(Instant.parse("2020-01-02T00:01:00Z"));
        long d3 = DateUtils.toEpochSecond(Instant.parse("2020-01-03T00:05:00Z"));
        long d4 = DateUtils.toEpochSecond(Instant.parse("2020-01-04T00:05:00Z"));
        String wi1 = insertWorkItem(productId, "reporterA", userA, stateA, d1, d1);
        String wi2 = insertWorkItem(productId, "reporterB", userB, stateA, d1, d2);
        String wi3 = insertWorkItem(productId, "reporterC", userA, stateB, d1, d3);
        String wi4 = insertWorkItem(productId, null, userB, stateC, d1, d4);
        tagWorkItem(wi1, tagA);
        tagWorkItem(wi2, tagA);
        tagWorkItem(wi3, tagB);
        tagWorkItem(wi4, tagB);

        // TEST no filters
        var aggResult = workItemDBService.aggregate(company, WorkItemFilter.builder()
                .across(WorkItemFilter.Distinct.state)
                .calculation(WorkItemFilter.Calculation.count)
                .build());
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                stateA.getId().toString(), 2,
                stateB.getId().toString(), 1,
                stateC.getId().toString(), 1
        ));

        // State filter
        aggResult = workItemDBService.aggregate(company, WorkItemFilter.builder()
                .across(WorkItemFilter.Distinct.state)
                .calculation(WorkItemFilter.Calculation.count)
                .states(List.of(stateB.getId().toString(), stateC.getId().toString()))
                .build());
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                stateB.getId().toString(), 1,
                stateC.getId().toString(), 1
        ));

        // Assignee filter
        aggResult = workItemDBService.aggregate(company, WorkItemFilter.builder()
                .across(WorkItemFilter.Distinct.state)
                .calculation(WorkItemFilter.Calculation.count)
                .assignees(List.of(userA))
                .build());
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                stateA.getId().toString(), 1,
                stateB.getId().toString(), 1
        ));
        aggResult = workItemDBService.aggregate(company, WorkItemFilter.builder()
                .across(WorkItemFilter.Distinct.state)
                .calculation(WorkItemFilter.Calculation.count)
                .assignees(List.of(user.getId()))
                .build());
        assertThat(aggResult.getRecords()).isEmpty();
        ;

        // reporter filter
        aggResult = workItemDBService.aggregate(company, WorkItemFilter.builder()
                .across(WorkItemFilter.Distinct.state)
                .calculation(WorkItemFilter.Calculation.count)
                .reporters(List.of("reporterB", "reporterC"))
                .build());
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                stateA.getId().toString(), 1,
                stateB.getId().toString(), 1
        ));

        // tags filter
        aggResult = workItemDBService.aggregate(company, WorkItemFilter.builder()
                .across(WorkItemFilter.Distinct.state)
                .calculation(WorkItemFilter.Calculation.count)
                .tags(List.of(tagA))
                .build());
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                stateA.getId().toString(), 2
        ));
        aggResult = workItemDBService.aggregate(company, WorkItemFilter.builder()
                .across(WorkItemFilter.Distinct.state)
                .calculation(WorkItemFilter.Calculation.count)
                .tags(List.of(tagB, tagC))
                .build());
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                stateB.getId().toString(), 1,
                stateC.getId().toString(), 1
        ));

        // product filter
        aggResult = workItemDBService.aggregate(company, WorkItemFilter.builder()
                .across(WorkItemFilter.Distinct.state)
                .calculation(WorkItemFilter.Calculation.count)
                .products(List.of(productId, "xyz"))
                .build());
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                stateA.getId().toString(), 2,
                stateB.getId().toString(), 1,
                stateC.getId().toString(), 1
        ));

        // updatedat filter
        aggResult = workItemDBService.aggregate(company, WorkItemFilter.builder()
                .across(WorkItemFilter.Distinct.state)
                .calculation(WorkItemFilter.Calculation.count)
                .updatedAt(WorkItemFilter.UpdatedAtFilter.builder()
                        .gt(d2)
                        .lt(d4)
                        .build())
                .build());
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                stateB.getId().toString(), 1
        ));

        // ---- ACROSS ASSIGNEE ----
        aggResult = workItemDBService.aggregate(company, WorkItemFilter.builder()
                .across(WorkItemFilter.Distinct.assignee)
                .calculation(WorkItemFilter.Calculation.count)
                .build());
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                userA, 2,
                userB, 2
        ));

        //tag-user combo test due to new EXISTS statement introduction.
        aggResult = workItemDBService.aggregate(company, WorkItemFilter.builder()
                .across(WorkItemFilter.Distinct.tag)
                .assignees(List.of(userA, userB))
                .calculation(WorkItemFilter.Calculation.count)
                .build());
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                tagA, 2,
                tagB, 2
        ));

        // ---- ACROSS reporter ----
        aggResult = workItemDBService.aggregate(company, WorkItemFilter.builder()
                .across(WorkItemFilter.Distinct.reporter)
                .calculation(WorkItemFilter.Calculation.count)
                .build());
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                "reporterA", 1,
                "reporterB", 1,
                "reporterC", 1,
                "unknown", 1
        ));

        // ---- ACROSS tag ----
        aggResult = workItemDBService.aggregate(company, WorkItemFilter.builder()
                .across(WorkItemFilter.Distinct.tag)
                .calculation(WorkItemFilter.Calculation.count)
                .build());
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                tagA, 2,
                tagB, 2
        ));

        // ---- ACROSS product ----
        aggResult = workItemDBService.aggregate(company, WorkItemFilter.builder()
                .across(WorkItemFilter.Distinct.product)
                .calculation(WorkItemFilter.Calculation.count)
                .build());
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                productId, 4
        ));

        // ---- ACROSS created ----
        aggResult = workItemDBService.aggregate(company, WorkItemFilter.builder()
                .across(WorkItemFilter.Distinct.created)
                .calculation(WorkItemFilter.Calculation.count)
                .build());
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                String.valueOf(d1), 4
        ));

        // ---- ACROSS updated ----
        aggResult = workItemDBService.aggregate(company, WorkItemFilter.builder()
                .across(WorkItemFilter.Distinct.updated)
                .calculation(WorkItemFilter.Calculation.count)
                .build());
        DefaultObjectMapper.prettyPrint(aggResult);
        assertCountAggResults(aggResult, Map.of(
                String.valueOf(d1), 1,
                String.valueOf(d2), 1,
                String.valueOf(d3), 1,
                String.valueOf(d4), 1
        ));

        // ---- ACROSS trend ----
        aggResult = workItemDBService.aggregate(company, WorkItemFilter.builder()
                .across(WorkItemFilter.Distinct.trend)
                .calculation(WorkItemFilter.Calculation.count)
                .build());
        DefaultObjectMapper.prettyPrint(aggResult);
        // assertCountAggResults(aggResult, Map.of(
        //         "1577865600", 2,
        //         "1577952000", 3,
        //         "1578038400", 4
        // ));

        // ---- STACK ----
        aggResult = workItemDBService.stackedAggregate(company, WorkItemFilter.builder()
                        .across(WorkItemFilter.Distinct.assignee)
                        .calculation(WorkItemFilter.Calculation.count)
                        .build(),
                List.of(WorkItemFilter.Distinct.state));
        DefaultObjectMapper.prettyPrint(aggResult);
        assertThat(aggResult.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder()
                        .key(userA)
                        .total(2L)
                        .stacks(List.of(
                                DbAggregationResult.builder()
                                        .key(stateA.getId().toString())
                                        .total(1L)
                                        .build(),
                                DbAggregationResult.builder()
                                        .key(stateB.getId().toString())
                                        .total(1L)
                                        .build()
                        ))
                        .build(),
                DbAggregationResult.builder()
                        .key(userB)
                        .total(2L)
                        .stacks(List.of(
                                DbAggregationResult.builder()
                                        .key(stateA.getId().toString())
                                        .total(1L)
                                        .build(),
                                DbAggregationResult.builder()
                                        .key(stateC.getId().toString())
                                        .total(1L)
                                        .build()
                        ))
                        .build()
        );

        // STACK reporter state
        aggResult = workItemDBService.stackedAggregate(company, WorkItemFilter.builder()
                        .across(WorkItemFilter.Distinct.reporter)
                        .calculation(WorkItemFilter.Calculation.count)
                        .build(),
                List.of(WorkItemFilter.Distinct.state));
        DefaultObjectMapper.prettyPrint(aggResult);
        assertThat(aggResult.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder()
                        .key("reporterA")
                        .total(1L)
                        .stacks(List.of(
                                DbAggregationResult.builder()
                                        .key(stateA.getId().toString())
                                        .total(1L)
                                        .build()
                        ))
                        .build(),
                DbAggregationResult.builder()
                        .key("reporterB")
                        .total(1L)
                        .stacks(List.of(
                                DbAggregationResult.builder()
                                        .key(stateA.getId().toString())
                                        .total(1L)
                                        .build()
                        ))
                        .build(),
                DbAggregationResult.builder()
                        .key("reporterC")
                        .total(1L)
                        .stacks(List.of(
                                DbAggregationResult.builder()
                                        .key(stateB.getId().toString())
                                        .total(1L)
                                        .build()
                        ))
                        .build(),
                DbAggregationResult.builder()
                        .key("unknown")
                        .total(1L)
                        .stacks(List.of(
                                DbAggregationResult.builder()
                                        .key(stateC.getId().toString())
                                        .total(1L)
                                        .build()
                        ))
                        .build()
        );

    }

    private String insertWorkItem(String productId, String reporter, String userId, State state, long createdAt, long updatedAt) throws SQLException {
        UUID uuid = UUID.randomUUID();
        WorkItem.WorkItemBuilder bldr = WorkItem.builder()
                .reason("reason-" + uuid)
                .title("title-" + uuid)
                .type(WorkItem.ItemType.MANUAL)
                .notify(Boolean.FALSE)
                .description("description" + uuid)
                .reporter(reporter)
                .productId(productId)
                .stateId(state.getId())
                .status(state.getName())
                .ticketType(WorkItem.TicketType.WORK_ITEM)
                .dueAt(System.currentTimeMillis())
                .artifactTitle("artifactTitle-" + uuid)
                .cloudOwner("cloudOwner-" + uuid);
        if (userId != null) {
            bldr.assignee(WorkItem.Assignee.builder().userId(userId).build());
        }
        WorkItem wi = bldr.build();
        String id = workItemDBService.insert(company, wi);
        template.update("update " + company + ".workitems " +
                        " SET updatedat = :updatedAt," +
                        "     createdat = :createdat " +
                        " WHERE id = :id::uuid",
                Map.of("id", id,
                        "updatedAt", updatedAt,
                        "createdat", createdAt));
        return id;
    }

    private String createTag() throws SQLException {
        return tagsService.insert(company, Tag.builder()
                .name("tag-" + UUID.randomUUID())
                .build());
    }

    private void tagWorkItem(String workItemId, String tagId) throws SQLException {
        tagItemDBService.insert(company, TagItemMapping.builder()
                .itemId(workItemId)
                .tagId(tagId)
                .tagItemType(WorkItem.ITEM_TYPE)
                .build());
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

    // endregion
}
