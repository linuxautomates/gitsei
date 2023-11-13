package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.KvData;
import io.levelops.commons.databases.models.database.Product;
import io.levelops.commons.databases.models.database.State;
import io.levelops.commons.databases.models.database.TicketData;
import io.levelops.commons.databases.models.database.TicketTemplate;
import io.levelops.commons.databases.models.database.TriageRule;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.WorkItem;
import io.levelops.commons.databases.models.database.cicd.JobRunStage;
import io.levelops.commons.databases.models.database.cicd.JobRunStageStep;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class WorkItemFailureTriageViewServiceTest {
    private static final Random RANDOM = new Random(System.currentTimeMillis());
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    private final static String company = "test";
    private static StateDBService stateDBService;
    private static IntegrationService its;
    private static OrganizationService os;
    private static TagsService tagsService;
    private static TagItemDBService tagItemDBService;
    private static BestPracticesService bestPracticesService;
    private static QuestionnaireTemplateDBService questionnaireTemplateDBService;
    private static WorkItemDBService workItemDBService;
    private static QuestionnaireDBService qs;
    private static TicketTemplateDBService ticketTemplateDBService;
    private static TriageRulesService triageRulesService;
    private static TriageRuleHitsService triageRuleHitsService;
    private static WorkItemFailureTriageViewService workItemFailureTriageViewService;
    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private DataSource dataSource;
    private UserService userService;
    private ProductService productService;
    private CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private CiCdJobRunStageDatabaseService jobRunStageDatabaseService;
    private static CiCdJobRunStageStepsDatabaseService ciCdJobRunStageStepsDatabaseService;

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

    private static State createState() throws SQLException {
        State st = State.builder().name("Open").build();
        String stateId = stateDBService.insert(company, st);
        return st.toBuilder().id(Integer.parseInt(stateId)).build();
    }

    @Before
    public void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        userService = new UserService(dataSource, DefaultObjectMapper.get());
        stateDBService = new StateDBService(dataSource);
        productService = new ProductService(dataSource);

        its = new IntegrationService(dataSource);
        userService = new UserService(dataSource, MAPPER);
        os = new OrganizationService(dataSource);
        tagsService = new TagsService(dataSource);
        tagItemDBService = new TagItemDBService(dataSource);
        bestPracticesService = new BestPracticesService(dataSource);
        questionnaireTemplateDBService = new QuestionnaireTemplateDBService(dataSource);

        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        ciCdJobRunsDatabaseService = new CiCdJobRunsDatabaseService(MAPPER, dataSource);
        jobRunStageDatabaseService = new CiCdJobRunStageDatabaseService(dataSource, MAPPER);

        workItemDBService = new WorkItemDBService(dataSource, MAPPER, productService, stateDBService, true, 100);
        qs = new QuestionnaireDBService(dataSource);
        ticketTemplateDBService = new TicketTemplateDBService(dataSource, MAPPER);

        triageRulesService = new TriageRulesService(dataSource);
        triageRuleHitsService = new TriageRuleHitsService(dataSource, MAPPER);

        workItemFailureTriageViewService = new WorkItemFailureTriageViewService(dataSource);

        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        userService.ensureTableExistence(company);
        stateDBService.ensureTableExistence(company);
        its.ensureTableExistence(company);
        productService.ensureTableExistence(company);

        os.ensureTableExistence(company);
        tagsService.ensureTableExistence(company);
        tagItemDBService.ensureTableExistence(company);
        bestPracticesService.ensureTableExistence(company);
        questionnaireTemplateDBService.ensureTableExistence(company);
        ticketTemplateDBService.ensureTableExistence(company);

        ciCdInstancesDatabaseService.ensureTableExistence(company);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        ciCdJobRunsDatabaseService.ensureTableExistence(company);
        jobRunStageDatabaseService.ensureTableExistence(company);

        workItemDBService.ensureTableExistence(company);
        qs.ensureTableExistence(company);
        ciCdJobRunStageStepsDatabaseService = new CiCdJobRunStageStepsDatabaseService(dataSource);
        ciCdJobRunStageStepsDatabaseService.ensureTableExistence(company);
        triageRulesService.ensureTableExistence(company);
        triageRuleHitsService.ensureTableExistence(company);

        workItemFailureTriageViewService.ensureTableExistence(company);
    }

    private WorkItem createWorkItem(int n, WorkItem.ItemType itemType, String productId, String integrationId, String userId, TicketTemplate
            ticketTemplate, State state, String parentId, List<WorkItem.CICDMapping> ciCdMappings) throws SQLException {
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
        return wi.toBuilder().id(workItemId).build();
    }

    @Test
    public void test() throws SQLException, JsonProcessingException {
        State state = createState();
        Integration it = testInsertIntegration();
        User user = UserUtils.createUser(userService, company, 0);

        Product product = ProductUtils.createProduct(productService, company, 0, user);
        CICDInstance cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, 0);

        CICDJob cicdJob1 = CiCdJobUtils.createCICDJob(ciCdJobsDatabaseService, company, 0, cicdInstance);
        CICDJobRun cicdJobRuns1 = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, cicdJob1, company, 0, Instant.now(), 1234, null, null);
        List<JobRunStage> jobRunStages1 = JobRunStageUtils.createJobRunStages(jobRunStageDatabaseService, cicdJobRuns1, company, 4);

        CICDJob cicdJob2 = CiCdJobUtils.createCICDJob(ciCdJobsDatabaseService, company, 1, cicdInstance);
        CICDJobRun cicdJobRuns2 = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, cicdJob2, company, 0, Instant.now(), 1234, null, null);
        List<JobRunStage> jobRunStages2 = JobRunStageUtils.createJobRunStages(jobRunStageDatabaseService, cicdJobRuns2, company, 3);

        CICDJob cicdJob3 = CiCdJobUtils.createCICDJob(ciCdJobsDatabaseService, company, 2, cicdInstance);
        CICDJobRun cicdJobRuns3 = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, cicdJob3, company, 0, Instant.now(), 1234, null, null);

        List<WorkItem.CICDMapping> ciCdMappings = List.of(
                WorkItem.CICDMapping.builder().cicdJobRunId(cicdJobRuns1.getId()).cicdJobRunStageId(jobRunStages1.get(0).getId()).build(),
                WorkItem.CICDMapping.builder().cicdJobRunId(cicdJobRuns1.getId()).cicdJobRunStageId(jobRunStages1.get(1).getId()).build(),
                WorkItem.CICDMapping.builder().cicdJobRunId(cicdJobRuns1.getId()).cicdJobRunStageId(jobRunStages1.get(2).getId()).build(),
                WorkItem.CICDMapping.builder().cicdJobRunId(cicdJobRuns2.getId()).cicdJobRunStageId(jobRunStages2.get(0).getId()).build(),
                WorkItem.CICDMapping.builder().cicdJobRunId(cicdJobRuns2.getId()).cicdJobRunStageId(jobRunStages2.get(1).getId()).build(),
                WorkItem.CICDMapping.builder().cicdJobRunId(cicdJobRuns3.getId()).build()
        );

        var stepId = ciCdJobRunStageStepsDatabaseService.insert(company, JobRunStageStep.builder()
                .stepId("1")
                .cicdJobRunStageId(jobRunStages1.get(0).getId())
                .displayName("test")
                .displayDescription("testing")
                .result("success")
                .state("success")
                .duration(100)
                .startTime(Instant.now())
                .gcsPath("test")
                .build());

        WorkItem workItem = createWorkItem(0, WorkItem.ItemType.AUTOMATED, product.getId(), it.getId(), user.getId(),
                null, state, null, ciCdMappings);

        TriageRule triageRule1 = TriageRuleUtils.createTriageRule(triageRulesService, company, 0);
        TriageRule triageRule2 = TriageRuleUtils.createTriageRule(triageRulesService, company, 1);

        TriageRuleHitUtils.createTriageRuleHit(triageRuleHitsService, company, 0, cicdJobRuns1.getId(), jobRunStages1.get(0).getId(), UUID.fromString(stepId), triageRule1);
        TriageRuleHitUtils.createTriageRuleHit(triageRuleHitsService, company, 1, cicdJobRuns1.getId(), jobRunStages1.get(0).getId(), UUID.fromString(stepId), triageRule2);
        TriageRuleHitUtils.createTriageRuleHit(triageRuleHitsService, company, 2, cicdJobRuns1.getId(), jobRunStages1.get(1).getId(), UUID.fromString(stepId), triageRule1);
        TriageRuleHitUtils.createTriageRuleHit(triageRuleHitsService, company, 3, cicdJobRuns1.getId(), jobRunStages1.get(2).getId(), UUID.fromString(stepId), triageRule1);
        TriageRuleHitUtils.createTriageRuleHit(triageRuleHitsService, company, 4, cicdJobRuns2.getId(), jobRunStages2.get(0).getId(), UUID.fromString(stepId), triageRule1);
        TriageRuleHitUtils.createTriageRuleHit(triageRuleHitsService, company, 5, cicdJobRuns2.getId(), jobRunStages2.get(0).getId(), UUID.fromString(stepId), triageRule2);
        TriageRuleHitUtils.createTriageRuleHit(triageRuleHitsService, company, 6, cicdJobRuns2.getId(), jobRunStages2.get(1).getId(), UUID.fromString(stepId), triageRule1);
        TriageRuleHitUtils.createTriageRuleHit(triageRuleHitsService, company, 7, cicdJobRuns3.getId(), null, UUID.fromString(stepId), triageRule1);


        List<WorkItemFailureTriageViewService.WIFailureTriageView> result = workItemFailureTriageViewService.getFailureTriageForWorkItem(company, UUID.fromString(workItem.getId()));
        Assert.assertNotNull(result);
        Assert.assertEquals(8, result.size());
        String serialized = MAPPER.writeValueAsString(result);
        Assert.assertNotNull(serialized);
        List<WorkItemFailureTriageViewService.WIFailureTriageView> views = MAPPER.readValue(serialized, MAPPER.getTypeFactory().constructCollectionType(List.class, WorkItemFailureTriageViewService.WIFailureTriageView.class));
        Assert.assertNotNull(views);
    }

    @Test
    public void test2() throws SQLException, JsonProcessingException {
        State state = createState();
        Integration it = testInsertIntegration();
        User user = UserUtils.createUser(userService, company, 0);

        Product product = ProductUtils.createProduct(productService, company, 0, user);
        CICDInstance cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, 0);

        CICDJob cicdJob1 = CiCdJobUtils.createCICDJob(ciCdJobsDatabaseService, company, 0, cicdInstance);
        CICDJobRun cicdJobRuns1 = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, cicdJob1, company, 0, Instant.now(), 1234, null, null);
        List<JobRunStage> jobRunStages1 = JobRunStageUtils.createJobRunStages(jobRunStageDatabaseService, cicdJobRuns1, company, 4);

        CICDJob cicdJob2 = CiCdJobUtils.createCICDJob(ciCdJobsDatabaseService, company, 1, cicdInstance);
        CICDJobRun cicdJobRuns2 = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, cicdJob2, company, 0, Instant.now(), 1234, null, null);
        List<JobRunStage> jobRunStages2 = JobRunStageUtils.createJobRunStages(jobRunStageDatabaseService, cicdJobRuns2, company, 3);

        CICDJob cicdJob3 = CiCdJobUtils.createCICDJob(ciCdJobsDatabaseService, company, 2, cicdInstance);
        CICDJobRun cicdJobRuns3 = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, cicdJob3, company, 0, Instant.now(), 1234, null, null);
        List<JobRunStage> jobRunStages3 = JobRunStageUtils.createJobRunStages(jobRunStageDatabaseService, cicdJobRuns3, company, 2);

        CICDJob cicdJob4 = CiCdJobUtils.createCICDJob(ciCdJobsDatabaseService, company, 3, cicdInstance);
        CICDJobRun cicdJobRuns4 = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, cicdJob4, company, 0, Instant.now(), 1234, null, null);

        List<WorkItem.CICDMapping> ciCdMappings1 = List.of(
                WorkItem.CICDMapping.builder().cicdJobRunId(cicdJobRuns1.getId()).cicdJobRunStageId(jobRunStages1.get(0).getId()).build(),
                WorkItem.CICDMapping.builder().cicdJobRunId(cicdJobRuns1.getId()).cicdJobRunStageId(jobRunStages1.get(1).getId()).build(),
                WorkItem.CICDMapping.builder().cicdJobRunId(cicdJobRuns1.getId()).cicdJobRunStageId(jobRunStages1.get(2).getId()).build()
        );
        WorkItem workItem1 = createWorkItem(0, WorkItem.ItemType.AUTOMATED, product.getId(), it.getId(), user.getId(),
                null, state, null, ciCdMappings1);

        List<WorkItem.CICDMapping> ciCdMappings2 = List.of(
                WorkItem.CICDMapping.builder().cicdJobRunId(cicdJobRuns2.getId()).cicdJobRunStageId(jobRunStages2.get(0).getId()).build()
        );
        WorkItem workItem2 = createWorkItem(0, WorkItem.ItemType.AUTOMATED, product.getId(), it.getId(), user.getId(),
                null, state, null, ciCdMappings2);

        List<WorkItem.CICDMapping> ciCdMappings3 = List.of(
                WorkItem.CICDMapping.builder().cicdJobRunId(cicdJobRuns3.getId()).build()
        );
        WorkItem workItem3 = createWorkItem(0, WorkItem.ItemType.AUTOMATED, product.getId(), it.getId(), user.getId(),
                null, state, null, ciCdMappings3);

        List<WorkItem.CICDMapping> ciCdMappings4 = List.of(
                WorkItem.CICDMapping.builder().cicdJobRunId(cicdJobRuns4.getId()).build()
        );
        WorkItem workItem4 = createWorkItem(0, WorkItem.ItemType.AUTOMATED, product.getId(), it.getId(), user.getId(),
                null, state, null, ciCdMappings4);

        var stepId = ciCdJobRunStageStepsDatabaseService.insert(company, JobRunStageStep.builder()
                .stepId("1")
                .cicdJobRunStageId(jobRunStages1.get(0).getId())
                .displayName("test")
                .displayDescription("testing")
                .result("success")
                .state("success")
                .duration(100)
                .startTime(Instant.now())
                .gcsPath("test")
                .build());
        TriageRule triageRule1 = TriageRuleUtils.createTriageRule(triageRulesService, company, 0);
        TriageRule triageRule2 = TriageRuleUtils.createTriageRule(triageRulesService, company, 1);

        TriageRuleHitUtils.createTriageRuleHit(triageRuleHitsService, company, 0, cicdJobRuns1.getId(), jobRunStages1.get(0).getId(), UUID.fromString(stepId),  triageRule1);
        TriageRuleHitUtils.createTriageRuleHit(triageRuleHitsService, company, 1, cicdJobRuns1.getId(), jobRunStages1.get(0).getId(), UUID.fromString(stepId), triageRule2);
        TriageRuleHitUtils.createTriageRuleHit(triageRuleHitsService, company, 2, cicdJobRuns1.getId(), jobRunStages1.get(1).getId(), UUID.fromString(stepId), triageRule1);
        TriageRuleHitUtils.createTriageRuleHit(triageRuleHitsService, company, 3, cicdJobRuns1.getId(), jobRunStages1.get(2).getId(), UUID.fromString(stepId), triageRule1);
        TriageRuleHitUtils.createTriageRuleHit(triageRuleHitsService, company, 4, cicdJobRuns1.getId(), jobRunStages1.get(3).getId(), UUID.fromString(stepId), triageRule1);
        TriageRuleHitUtils.createTriageRuleHit(triageRuleHitsService, company, 5, cicdJobRuns2.getId(), jobRunStages2.get(0).getId(), UUID.fromString(stepId), triageRule1);
        TriageRuleHitUtils.createTriageRuleHit(triageRuleHitsService, company, 6, cicdJobRuns2.getId(), jobRunStages2.get(0).getId(), UUID.fromString(stepId), triageRule2);
        TriageRuleHitUtils.createTriageRuleHit(triageRuleHitsService, company, 7, cicdJobRuns2.getId(), jobRunStages2.get(1).getId(), UUID.fromString(stepId), triageRule1);
        TriageRuleHitUtils.createTriageRuleHit(triageRuleHitsService, company, 8, cicdJobRuns2.getId(), jobRunStages2.get(2).getId(), UUID.fromString(stepId), triageRule1);
        TriageRuleHitUtils.createTriageRuleHit(triageRuleHitsService, company, 9, cicdJobRuns3.getId(), jobRunStages3.get(0).getId(), UUID.fromString(stepId), triageRule1);
        TriageRuleHitUtils.createTriageRuleHit(triageRuleHitsService, company, 10, cicdJobRuns3.getId(), jobRunStages3.get(0).getId(), UUID.fromString(stepId), triageRule2);
        TriageRuleHitUtils.createTriageRuleHit(triageRuleHitsService, company, 11, cicdJobRuns3.getId(), jobRunStages3.get(1).getId(), UUID.fromString(stepId), triageRule1);
        TriageRuleHitUtils.createTriageRuleHit(triageRuleHitsService, company, 12, cicdJobRuns4.getId(), null, UUID.fromString(stepId), triageRule1);

        /*
        Test setup:
        r1 -> 4 stages
        r2 -> 3 stages
        r3 -> 2 stages
        r4 -> no stages
        
        w1 -> r1 -> stages 0,1,2 -> rule hits = 2 + 1 + 1 = 4
        w2 -> r2 -> stages 0 -> rule hits = 2
        w3 -> r3 -> All stages -> rule hits = 3 = 2 +1
        w4 -> r4 -> no stages -> rule hits = 1
         */

        List<WorkItemFailureTriageViewService.WIFailureTriageView> result = workItemFailureTriageViewService.getFailureTriageForWorkItem(company, UUID.fromString(workItem1.getId()));
        Assert.assertNotNull(result);
        Assert.assertEquals(4, result.size());
        String serialized = MAPPER.writeValueAsString(result);
        Assert.assertNotNull(serialized);
        List<WorkItemFailureTriageViewService.WIFailureTriageView> views = MAPPER.readValue(serialized, MAPPER.getTypeFactory().constructCollectionType(List.class, WorkItemFailureTriageViewService.WIFailureTriageView.class));
        Assert.assertNotNull(views);

        result = workItemFailureTriageViewService.getFailureTriageForWorkItem(company, UUID.fromString(workItem2.getId()));
        Assert.assertNotNull(result);
        System.out.println(DefaultObjectMapper.writeAsPrettyJson(result));
        Assert.assertEquals(2, result.size());
        serialized = MAPPER.writeValueAsString(result);
        Assert.assertNotNull(serialized);
        views = MAPPER.readValue(serialized, MAPPER.getTypeFactory().constructCollectionType(List.class, WorkItemFailureTriageViewService.WIFailureTriageView.class));
        Assert.assertNotNull(views);

        result = workItemFailureTriageViewService.getFailureTriageForWorkItem(company, UUID.fromString(workItem3.getId()));
        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.size());
        serialized = MAPPER.writeValueAsString(result);
        Assert.assertNotNull(serialized);
        views = MAPPER.readValue(serialized, MAPPER.getTypeFactory().constructCollectionType(List.class, WorkItemFailureTriageViewService.WIFailureTriageView.class));
        Assert.assertNotNull(views);

        result = workItemFailureTriageViewService.getFailureTriageForWorkItem(company, UUID.fromString(workItem4.getId()));
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        serialized = MAPPER.writeValueAsString(result);
        Assert.assertNotNull(serialized);
        views = MAPPER.readValue(serialized, MAPPER.getTypeFactory().constructCollectionType(List.class, WorkItemFailureTriageViewService.WIFailureTriageView.class));
        Assert.assertNotNull(views);

    }
}