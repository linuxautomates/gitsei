package io.levelops.commons.databases.services.velocity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.CiCdJobRunsFilter;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.filters.VelocityFilter;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.services.CiCdMetadataConditionBuilder;
import io.levelops.commons.databases.services.CiCdPartialMatchConditionBuilder;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.jira.conditions.JiraConditionsBuilder;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.web.exceptions.BadRequestException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;

public class VelocityAggsQueryBuilderTest {
    private static final Long LOWER_LIMIT = TimeUnit.DAYS.toSeconds(10);
    private static final Long UPPER_LIMIT = TimeUnit.DAYS.toSeconds(30);
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    private static final WorkItemsType WORK_ITEMS_TYPE_JIRA = WorkItemsType.JIRA;
    private static final WorkItemsType WORK_ITEMS_TYPE_WI = WorkItemsType.WORK_ITEM;
    private static final JiraIssuesFilter JIRA_ISSUES_FILTER_NULL = null;
    private static final JiraIssuesFilter JIRA_ISSUES_FILTER = JiraIssuesFilter.builder().integrationIds(List.of("1", "2", "3")).ingestedAt(1619654400l).build();
    private static final JiraIssuesFilter JIRA_ISSUES_FILTER_ACTIVE_SPRINT = JIRA_ISSUES_FILTER.toBuilder().sprintStates(List.of("ACTIVE")).build();
    private static final WorkItemsFilter WORK_ITEM_FILTER_NULL = null;
    private static final WorkItemsFilter WORK_ITEM_FILTER = WorkItemsFilter.builder().integrationIds(List.of("1", "2", "3")).ingestedAt(1619654400l).build();
    private static final WorkItemsMilestoneFilter WORKITEMS_MILESTONE_FILTER_EMPTY = WorkItemsMilestoneFilter.builder().build();
    private static final ScmPrFilter SCM_PR_FILTER = ScmPrFilter.builder().integrationIds(List.of("1", "2", "3")).build();
    private static final ScmCommitFilter SCM_COMMIT_FILTER = ScmCommitFilter.builder().integrationIds(List.of("1", "2", "3")).committedAtRange(ImmutablePair.of(1592814148L, 1624350148L)).build();
    private static final CiCdJobRunsFilter CICD_JOB_RUNS_FILTER_EMPTY = CiCdJobRunsFilter.builder().integrationIds(List.of("3", "4", "5")).build();
    private static final CiCdJobRunsFilter CICD_JOB_RUNS_FILTER = CiCdJobRunsFilter.builder().endTimeRange(ImmutablePair.of(1592814148L, 1624350148L)).build();
    private static final List<DbWorkItemField> WORKITEM_CUSTOM_FIELDS = List.of();
    private static final ScmPrFilter SCM_PR_FILTER_OU = ScmPrFilter.builder().integrationIds(List.of("1", "2", "3")).isApplyOuOnVelocityReport(true).repoIds(List.of("abc","def")).projects(List.of("Levelops","Propelo")).states(List.of("Pending","Approved")).labels(List.of("LEV","POP")).creators(List.of("3bd4f27b-39ac-4cee-9546-5af53ba87fac","096272fb-070c-45c8-b3cf-6d0989552479")).reviewers(List.of("02b8321b-424c-40ce-a408-e9cb3b1732e9","edbcdfaa-8cba-4dbf-827e-ad19efe0815b")).approvers(List.of("1fe8570a-76c8-4611-bb9f-bc336cd95b5f","f2b4067d-75fc-45b5-8071-a6992832be2b")).build();
    private static final ScmCommitFilter SCM_COMMIT_FILTER_OU = ScmCommitFilter.builder().integrationIds(List.of("1", "2", "3")).committedAtRange(ImmutablePair.of(1592814148L, 1624350148L)).isApplyOuOnVelocityReport(true).repoIds(List.of("abc","def")).projects(List.of("levelops","propelo")).authors(List.of("1fe8570a-76c8-4611-bb9f-bc336cd95b5f","f2b4067d-75fc-45b5-8071-a6992832be2b")).committers(List.of("3bd4f27b-39ac-4cee-9546-5af53ba87fac","096272fb-070c-45c8-b3cf-6d0989552479")).build();
    private static final ScmCommitFilter SCM_COMMIT_FILTER_OU_FALSE = ScmCommitFilter.builder().integrationIds(List.of("1", "2", "3")).committedAtRange(ImmutablePair.of(1592814148L, 1624350148L)).isApplyOuOnVelocityReport(false).repoIds(List.of("abc","def")).projects(List.of("levelops","propelo")).authors(List.of("1fe8570a-76c8-4611-bb9f-bc336cd95b5f","f2b4067d-75fc-45b5-8071-a6992832be2b")).committers(List.of("3bd4f27b-39ac-4cee-9546-5af53ba87fac","096272fb-070c-45c8-b3cf-6d0989552479")).build();
    private static final ScmPrFilter SCM_PR_FILTER_OU_FALSE = ScmPrFilter.builder().integrationIds(List.of("1", "2", "3")).isApplyOuOnVelocityReport(false).repoIds(List.of("abc","def")).states(List.of("Pending","Approved")).labels(List.of("LEV","POP")).creators(List.of("3bd4f27b-39ac-4cee-9546-5af53ba87fac","096272fb-070c-45c8-b3cf-6d0989552479")).reviewers(List.of("02b8321b-424c-40ce-a408-e9cb3b1732e9","edbcdfaa-8cba-4dbf-827e-ad19efe0815b")).approvers(List.of("1fe8570a-76c8-4611-bb9f-bc336cd95b5f","f2b4067d-75fc-45b5-8071-a6992832be2b")).build();
    private static final ScmCommitFilter SCM_COMMIT_FILTER_OU_NULL = ScmCommitFilter.builder().integrationIds(List.of("1", "2", "3")).committedAtRange(ImmutablePair.of(1592814148L, 1624350148L)).repoIds(List.of("abc","def")).projects(List.of("levelops","propelo")).authors(List.of("1fe8570a-76c8-4611-bb9f-bc336cd95b5f","f2b4067d-75fc-45b5-8071-a6992832be2b")).committers(List.of("3bd4f27b-39ac-4cee-9546-5af53ba87fac","096272fb-070c-45c8-b3cf-6d0989552479")).build();
    private static final ScmPrFilter SCM_PR_FILTER_OU_NULL = ScmPrFilter.builder().integrationIds(List.of("1", "2", "3")).repoIds(List.of("abc","def")).projects(List.of("Levelops","Propelo")).states(List.of("Pending","Approved")).labels(List.of("LEV","POP")).creators(List.of("3bd4f27b-39ac-4cee-9546-5af53ba87fac","096272fb-070c-45c8-b3cf-6d0989552479")).reviewers(List.of("02b8321b-424c-40ce-a408-e9cb3b1732e9","edbcdfaa-8cba-4dbf-827e-ad19efe0815b")).approvers(List.of("1fe8570a-76c8-4611-bb9f-bc336cd95b5f","f2b4067d-75fc-45b5-8071-a6992832be2b")).build();
    @Mock
    private JiraConditionsBuilder jiraIssueService;

    @Mock
    private DataSource dataSource;

    @Mock
    private ScmAggService scmAggService;

    private CiCdMetadataConditionBuilder ciCdMetadataConditionBuilder;

    private CiCdPartialMatchConditionBuilder ciCdPartialMatchConditionBuilder;
    
    private JiraConditionsBuilder realJiraConditionsBuilder;

    private ScmAggService realScmAggService;
    @Mock
    private UserIdentityService userIdentityService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(jiraIssueService.createWhereClauseAndUpdateParams(anyString(),any(), anyString(), any(), anyLong(), anyLong(), anyString(), any(OUConfiguration.class))).thenReturn(Map.of());
        Mockito.when(scmAggService.createPrWhereClauseAndUpdateParams(anyString(), any(), any(), any(), anyString(), any())).thenReturn(Map.of());
        Mockito.when(scmAggService.createCommitsWhereClauseAndUpdateParams(anyString(), any(), any(), any(), anyString(), anyBoolean(), any())).thenReturn(Map.of());
        realJiraConditionsBuilder = new JiraConditionsBuilder(dataSource, null, null, null, true);
        realScmAggService= new ScmAggService(dataSource,userIdentityService);
        ciCdMetadataConditionBuilder = new CiCdMetadataConditionBuilder();
        ciCdPartialMatchConditionBuilder = new CiCdPartialMatchConditionBuilder();
    }

    //region Setup
    private static List<VelocityConfigDTO.Stage> buildPreCustomStages() {
        return List.of(
                VelocityConfigDTO.Stage.builder()
                        .name("Backlog Time").description("stage description").order(0).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                        .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.JIRA_STATUS).values(List.of("IN_PROGRESS")).build())
                        .build()
        );
    }
    private static List<VelocityConfigDTO.Stage> buildFixedStages(boolean startIsPRCreated) {
        List<VelocityConfigDTO.Stage> stages = new ArrayList<>();
        if(!startIsPRCreated) {
            stages.add(VelocityConfigDTO.Stage.builder()
                    .name("Lead time to first commit").description("stage description").order(0).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                    .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.SCM_COMMIT_CREATED).build())
                    .build());
        }
        stages.addAll(List.of(
                VelocityConfigDTO.Stage.builder()
                        .name("Dev Time - PR Creared").description("stage description").order(1).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                        .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.SCM_PR_CREATED).build())
                        .build(),
                VelocityConfigDTO.Stage.builder()
                        .name("Dev Time - PR Label Added - Any").description("stage description").order(2).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                        .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.SCM_PR_LABEL_ADDED).params(Map.of("any_label_added", List.of("true"))).build())
                        .build(),
                VelocityConfigDTO.Stage.builder()
                        .name("Dev Time - PR Label Added - Specific").description("stage description").order(3).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                        .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.SCM_PR_LABEL_ADDED).values(List.of("lbl_1", "lbl_c")).build())
                        .build(),
                VelocityConfigDTO.Stage.builder()
                        .name("Lead time to review").description("stage description").order(4).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                        .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.SCM_PR_REVIEW_STARTED).build())
                        .build(),
                VelocityConfigDTO.Stage.builder()
                        .name("Review Time").description("stage description").order(5).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                        .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.SCM_PR_APPROVED).params(Map.of("last_approval", List.of("true"))).build())
                        .build(),
                VelocityConfigDTO.Stage.builder()
                        .name("Merge Time").description("stage description").order(6).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                        .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.SCM_PR_MERGED).build())
                        .build()
        ));
        return stages;
    }

    private static List<VelocityConfigDTO.Stage> buildFixedStagesWithBranchFilter() {
        return new ArrayList<>(List.of(
                VelocityConfigDTO.Stage.builder()
                        .name("Dev Time - PR Creared").description("stage description").order(1).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                        .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.SCM_PR_CREATED).params(Map.of("source_branches", List.of("PRE"), "target_branches", List.of("Main"))).build())
                        .build(),
                VelocityConfigDTO.Stage.builder()
                        .name("Lead time to review").description("stage description").order(2).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                        .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.SCM_PR_REVIEW_STARTED).build())
                        .build(),
                VelocityConfigDTO.Stage.builder()
                        .name("Review Time").description("stage description").order(3).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                        .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.SCM_PR_APPROVED).params(Map.of("last_approval", List.of("true"))).build())
                        .build(),
                VelocityConfigDTO.Stage.builder()
                        .name("Merge Time").description("stage description").order(4).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                        .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.SCM_PR_MERGED).build())
                        .build()
        ));
    }


    private static List<VelocityConfigDTO.Stage> buildPostCustomStages() {
        return List.of(
                VelocityConfigDTO.Stage.builder()
                        .name("Deploy to Staging").description("stage description").order(0).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                        .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.CICD_JOB_RUN).values(List.of("c67fb552-c8dd-463e-9ec9-7173e54f7ebd","01743634-d322-4f05-b765-db8902d8f7a0")).params(Map.of("branch", List.of("dev","staging"))).build())
                        .build(),
                VelocityConfigDTO.Stage.builder()
                        .name("QA").description("stage description").order(1).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                        .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.JIRA_STATUS).values(List.of("QA", "Testing")).build())
                        .build(),
                VelocityConfigDTO.Stage.builder()
                        .name("Deploy to Prod").description("stage description").order(2).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                        .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.CICD_JOB_RUN).values(List.of("c67fb552-c8dd-463e-9ec9-7173e54f7ebd","01743634-d322-4f05-b765-db8902d8f7a0")).params(Map.of("branch", List.of("main","master"))).build())
                        .build()
        );
    }
    private static List<VelocityConfigDTO.Stage> buildPostCustomStagesWithDeployJob() {
        return List.of(
                VelocityConfigDTO.Stage.builder()
                        .name("QA").description("stage description").order(1).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                        .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.JIRA_STATUS).values(List.of("QA", "Testing")).build())
                        .build(),
                VelocityConfigDTO.Stage.builder()
                        .name("Deploy to Prod").description("stage description").order(2).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                        .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.DEPLOYMENT_JOB_RUN).values(List.of("c67fb552-c8dd-463e-9ec9-7173e54f7ebd","01743634-d322-4f05-b765-db8902d8f7a0")).params(Map.of("deploy_job_marker", List.of("deploy-marker"), "branch", List.of("main","master"))).build())
                        .build()
        );
    }

    private static List<VelocityConfigDTO.Stage>  buildPostCustomStagesWithoutCiCdJobsAndParams() {
        return List.of(
                VelocityConfigDTO.Stage.builder()
                        .name("Deploy to Staging").description("stage description").order(0).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                        .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.CICD_JOB_RUN).build())
                        .build(),
                VelocityConfigDTO.Stage.builder()
                        .name("QA").description("stage description").order(1).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                        .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.JIRA_STATUS).values(List.of("QA", "Testing")).build())
                        .build(),
                VelocityConfigDTO.Stage.builder()
                        .name("Deploy to Prod").description("stage description").order(2).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                        .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.CICD_JOB_RUN).build())
                        .build()
        );
    }
    private static VelocityConfigDTO createVelocityConfigDTO() {
        VelocityConfigDTO velocityConfigDTO = VelocityConfigDTO.builder()
                .id(UUID.fromString("d67fb552-c8dd-463e-9ec9-7173e54f7ebc"))
                .name("Default Velocity").description("description").defaultConfig(true)
                //.createdAt(now).updatedAt(now)
                .preDevelopmentCustomStages(buildPreCustomStages())
                .fixedStages(buildFixedStages(false))
                .postDevelopmentCustomStages(buildPostCustomStages())
                .build();
        return velocityConfigDTO;
    }

    private static VelocityConfigDTO addApprovalTimeFlag(VelocityConfigDTO velocityConfigDTO, String aggregateFunction) {
        List<VelocityConfigDTO.Stage> fixedStages = velocityConfigDTO.getFixedStages();
        VelocityConfigDTO.Stage approvalStage = fixedStages.stream()
                .filter(stage -> stage.getEvent().getType() == VelocityConfigDTO.EventType.SCM_PR_APPROVED)
                .findFirst()
                .get();

        fixedStages.removeIf(stage -> stage.getEvent().getType() == VelocityConfigDTO.EventType.SCM_PR_APPROVED);

        Map<String, List<String>> params = new HashMap<>(approvalStage.getEvent().getParams());
        params.put("approval", List.of(aggregateFunction));
        approvalStage = approvalStage.toBuilder()
                .event(approvalStage.getEvent().toBuilder().params(params).build())
                .build();
        fixedStages.add(fixedStages.size()-1, approvalStage);

        velocityConfigDTO = velocityConfigDTO.toBuilder()
                .fixedStages(fixedStages)
                .build();

        return velocityConfigDTO;
    }

    private static VelocityConfigDTO createVelocityConfigDTOWithoutCiCdJobsAndParams() {
        VelocityConfigDTO velocityConfigDTO = VelocityConfigDTO.builder()
                .id(UUID.fromString("d67fb552-c8dd-463e-9ec9-7173e54f7ebc"))
                .name("Default Velocity").description("description").defaultConfig(true)
                //.createdAt(now).updatedAt(now)
                .preDevelopmentCustomStages(buildPreCustomStages())
                .fixedStages(buildFixedStages(false))
                .postDevelopmentCustomStages(buildPostCustomStagesWithoutCiCdJobsAndParams())
                .build();
        return velocityConfigDTO;
    }
    private static VelocityConfigDTO createVelocityConfigDTOStartIsPRCreated() {
        VelocityConfigDTO velocityConfigDTO = VelocityConfigDTO.builder()
                .id(UUID.fromString("d67fb552-c8dd-463e-9ec9-7173e54f7ebc"))
                .name("Default Velocity").description("description").defaultConfig(true)
                //.createdAt(now).updatedAt(now)
                .preDevelopmentCustomStages(List.of())
                .fixedStages(buildFixedStages(true))
                .postDevelopmentCustomStages(buildPostCustomStages())
                .startingEventIsCommitCreated(true)
                .build();
        return velocityConfigDTO;
    }
    private static VelocityConfigDTO createVelocityConfigDTOPRBranchFilter() {
        return VelocityConfigDTO.builder()
                .id(UUID.fromString("d67fb552-c8dd-463e-9ec9-7173e54f7edc"))
                .name("Default Velocity").description("description").defaultConfig(true)
                .preDevelopmentCustomStages(List.of())
                .fixedStages(buildFixedStagesWithBranchFilter())
                .postDevelopmentCustomStages(List.of())
                .startingEventIsCommitCreated(true)
                .build();
    }
    private static VelocityConfigDTO createVelocityConfigDTOStartIsPRDeployed() {
        VelocityConfigDTO velocityConfigDTO = VelocityConfigDTO.builder()
                .id(UUID.fromString("d67fb552-c8dd-463e-9ec9-7173e54f7ebc"))
                .name("Default Velocity").description("description").defaultConfig(true)
                //.createdAt(now).updatedAt(now)
                .preDevelopmentCustomStages(List.of())
                .fixedStages(buildFixedStages(true))
                .postDevelopmentCustomStages(buildPostCustomStagesWithDeployJob())
                .startingEventIsCommitCreated(true)
                .build();
        return velocityConfigDTO;
    }
    //endregion

    //region Use All Data
    //region Jira Velocity
    @Test
    public void testJiraVelocity() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
                "               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND s10.end_time > to_timestamp(:s10_job_run_end_start) AND s10.end_time < to_timestamp(:s10_job_run_end_end) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "  \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s10_job_run_end_start\":1592814148,\"s10_job_run_end_end\":1624350148,\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion
    @Test
    public void testJiraVelocityWithOU() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, realScmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER_OU, SCM_COMMIT_FILTER_OU, CICD_JOB_RUNS_FILTER, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
                "               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where (pr.id is NULL OR pr.repo_id && ARRAY[ :repo_ids ]::varchar[] ) AND (pr.id is NULL OR pr.project IN (:projects) ) AND (pr.id is NULL OR pr.state IN (:states) ) AND (pr.id is NULL OR pr.labels && ARRAY[ :labels ]::varchar[] ) AND (pr.id is NULL OR pr.creator_id IN (:pr.creator_id)) AND (prr.id is NULL OR prr.reviewer_id IN (:pr.reviewer_id)) AND (prr.id is NULL OR prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) AND (prr.id is NULL OR prr.reviewer_id IN (:pr.approver_id) ) AND (c.id is NULL OR c.repo_id && ARRAY[:repo_ids ]::varchar[] ) AND (c.id is NULL OR c.project IN (:projects) ) AND c.committed_at > TO_TIMESTAMP(1592814148) AND c.committed_at < TO_TIMESTAMP(1624350148) AND (c.id is NULL OR c.author_id IN (:c.author_id)) AND (c.id is NULL OR c.committer_id IN (:c.committer_id)) AND im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND s10.end_time > to_timestamp(:s10_job_run_end_start) AND s10.end_time < to_timestamp(:s10_job_run_end_end) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "  \n" +
                "  \n", sql);
        Assert.assertEquals("{\"c.committer_id\":[\"3bd4f27b-39ac-4cee-9546-5af53ba87fac\",\"096272fb-070c-45c8-b3cf-6d0989552479\"],\"projects\":[\"levelops\",\"propelo\"],\"pr.reviewer_id\":[\"02b8321b-424c-40ce-a408-e9cb3b1732e9\",\"edbcdfaa-8cba-4dbf-827e-ad19efe0815b\"],\"pr.creator_id\":[\"3bd4f27b-39ac-4cee-9546-5af53ba87fac\",\"096272fb-070c-45c8-b3cf-6d0989552479\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"repo_ids\":[\"abc\",\"def\"],\"s10_value_0\":[\"main\",\"master\"],\"pr.approver_id\":[\"1fe8570a-76c8-4611-bb9f-bc336cd95b5f\",\"f2b4067d-75fc-45b5-8071-a6992832be2b\"],\"s9_value\":[\"QA\",\"Testing\"],\"states\":[\"Pending\",\"Approved\"],\"labels\":[\"LEV\",\"POP\"],\"integration_ids\":[1,2,3],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"c.author_id\":[\"1fe8570a-76c8-4611-bb9f-bc336cd95b5f\",\"f2b4067d-75fc-45b5-8071-a6992832be2b\"],\"s0_value\":[\"IN_PROGRESS\"],\"s10_job_run_end_start\":1592814148,\"s10_job_run_end_end\":1624350148,\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }

    @Test
    public void testJiraVelocityWithLatestApprovalTime() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        velocityConfigDTO = addApprovalTimeFlag(velocityConfigDTO, "MIN");
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, realScmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER_OU, SCM_COMMIT_FILTER_OU, CICD_JOB_RUNS_FILTER, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
                "               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where (pr.id is NULL OR pr.repo_id && ARRAY[ :repo_ids ]::varchar[] ) AND (pr.id is NULL OR pr.project IN (:projects) ) AND (pr.id is NULL OR pr.state IN (:states) ) AND (pr.id is NULL OR pr.labels && ARRAY[ :labels ]::varchar[] ) AND (pr.id is NULL OR pr.creator_id IN (:pr.creator_id)) AND (prr.id is NULL OR prr.reviewer_id IN (:pr.reviewer_id)) AND (prr.id is NULL OR prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) AND (prr.id is NULL OR prr.reviewer_id IN (:pr.approver_id) ) AND (c.id is NULL OR c.repo_id && ARRAY[:repo_ids ]::varchar[] ) AND (c.id is NULL OR c.project IN (:projects) ) AND c.committed_at > TO_TIMESTAMP(1592814148) AND c.committed_at < TO_TIMESTAMP(1624350148) AND (c.id is NULL OR c.author_id IN (:c.author_id)) AND (c.id is NULL OR c.committer_id IN (:c.committer_id)) AND im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND s10.end_time > to_timestamp(:s10_job_run_end_start) AND s10.end_time < to_timestamp(:s10_job_run_end_end) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "  \n" +
                "  \n", sql);
        Assert.assertEquals("{\"c.committer_id\":[\"3bd4f27b-39ac-4cee-9546-5af53ba87fac\",\"096272fb-070c-45c8-b3cf-6d0989552479\"],\"projects\":[\"levelops\",\"propelo\"],\"pr.reviewer_id\":[\"02b8321b-424c-40ce-a408-e9cb3b1732e9\",\"edbcdfaa-8cba-4dbf-827e-ad19efe0815b\"],\"pr.creator_id\":[\"3bd4f27b-39ac-4cee-9546-5af53ba87fac\",\"096272fb-070c-45c8-b3cf-6d0989552479\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"repo_ids\":[\"abc\",\"def\"],\"s10_value_0\":[\"main\",\"master\"],\"pr.approver_id\":[\"1fe8570a-76c8-4611-bb9f-bc336cd95b5f\",\"f2b4067d-75fc-45b5-8071-a6992832be2b\"],\"s9_value\":[\"QA\",\"Testing\"],\"states\":[\"Pending\",\"Approved\"],\"labels\":[\"LEV\",\"POP\"],\"integration_ids\":[1,2,3],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"c.author_id\":[\"1fe8570a-76c8-4611-bb9f-bc336cd95b5f\",\"f2b4067d-75fc-45b5-8071-a6992832be2b\"],\"s0_value\":[\"IN_PROGRESS\"],\"s10_job_run_end_start\":1592814148,\"s10_job_run_end_end\":1624350148,\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }

    @Test
    public void testJiraVelocityWithOUFalse() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, realScmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER_OU_FALSE, SCM_COMMIT_FILTER_OU_FALSE, CICD_JOB_RUNS_FILTER, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
                "               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where pr.repo_id && ARRAY[ :repo_ids ]::varchar[] AND pr.creator_id IN (:pr.creator_id) AND pr.state IN (:states) AND pr.labels && ARRAY[ :labels ]::varchar[] AND prr.reviewer_id IN (:pr.reviewer_id) AND prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) AND prr.reviewer_id IN (:pr.approver_id) AND c.repo_id && ARRAY[:repo_ids ]::varchar[] AND c.project IN (:projects) AND c.committer_id IN (:c.committer_id) AND c.author_id IN (:c.author_id) AND c.committed_at > TO_TIMESTAMP(1592814148) AND c.committed_at < TO_TIMESTAMP(1624350148) AND im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND s10.end_time > to_timestamp(:s10_job_run_end_start) AND s10.end_time < to_timestamp(:s10_job_run_end_end) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "  \n" +
                "  \n", sql);
        Assert.assertEquals("{\"c.committer_id\":[\"3bd4f27b-39ac-4cee-9546-5af53ba87fac\",\"096272fb-070c-45c8-b3cf-6d0989552479\"],\"projects\":[\"levelops\",\"propelo\"],\"pr.reviewer_id\":[\"02b8321b-424c-40ce-a408-e9cb3b1732e9\",\"edbcdfaa-8cba-4dbf-827e-ad19efe0815b\"],\"pr.creator_id\":[\"3bd4f27b-39ac-4cee-9546-5af53ba87fac\",\"096272fb-070c-45c8-b3cf-6d0989552479\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"repo_ids\":[\"abc\",\"def\"],\"s10_value_0\":[\"main\",\"master\"],\"pr.approver_id\":[\"1fe8570a-76c8-4611-bb9f-bc336cd95b5f\",\"f2b4067d-75fc-45b5-8071-a6992832be2b\"],\"s9_value\":[\"QA\",\"Testing\"],\"states\":[\"Pending\",\"Approved\"],\"labels\":[\"LEV\",\"POP\"],\"integration_ids\":[1,2,3],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"c.author_id\":[\"1fe8570a-76c8-4611-bb9f-bc336cd95b5f\",\"f2b4067d-75fc-45b5-8071-a6992832be2b\"],\"s0_value\":[\"IN_PROGRESS\"],\"s10_job_run_end_start\":1592814148,\"s10_job_run_end_end\":1624350148,\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    @Test
    public void testJiraVelocityWithOUNull() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, realScmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER_OU_NULL, SCM_COMMIT_FILTER_OU_NULL, CICD_JOB_RUNS_FILTER, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
                "               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where (pr.id is NULL OR pr.repo_id && ARRAY[ :repo_ids ]::varchar[] ) AND (pr.id is NULL OR pr.project IN (:projects) ) AND (pr.id is NULL OR pr.state IN (:states) ) AND (pr.id is NULL OR pr.labels && ARRAY[ :labels ]::varchar[] ) AND (pr.id is NULL OR pr.creator_id IN (:pr.creator_id)) AND (prr.id is NULL OR prr.reviewer_id IN (:pr.reviewer_id)) AND (prr.id is NULL OR prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) AND (prr.id is NULL OR prr.reviewer_id IN (:pr.approver_id) ) AND (c.id is NULL OR c.repo_id && ARRAY[:repo_ids ]::varchar[] ) AND (c.id is NULL OR c.project IN (:projects) ) AND c.committed_at > TO_TIMESTAMP(1592814148) AND c.committed_at < TO_TIMESTAMP(1624350148) AND (c.id is NULL OR c.author_id IN (:c.author_id)) AND (c.id is NULL OR c.committer_id IN (:c.committer_id)) AND im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND s10.end_time > to_timestamp(:s10_job_run_end_start) AND s10.end_time < to_timestamp(:s10_job_run_end_end) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "  \n" +
                "  \n", sql);
        Assert.assertEquals("{\"c.committer_id\":[\"3bd4f27b-39ac-4cee-9546-5af53ba87fac\",\"096272fb-070c-45c8-b3cf-6d0989552479\"],\"projects\":[\"levelops\",\"propelo\"],\"pr.reviewer_id\":[\"02b8321b-424c-40ce-a408-e9cb3b1732e9\",\"edbcdfaa-8cba-4dbf-827e-ad19efe0815b\"],\"pr.creator_id\":[\"3bd4f27b-39ac-4cee-9546-5af53ba87fac\",\"096272fb-070c-45c8-b3cf-6d0989552479\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"repo_ids\":[\"abc\",\"def\"],\"s10_value_0\":[\"main\",\"master\"],\"pr.approver_id\":[\"1fe8570a-76c8-4611-bb9f-bc336cd95b5f\",\"f2b4067d-75fc-45b5-8071-a6992832be2b\"],\"s9_value\":[\"QA\",\"Testing\"],\"states\":[\"Pending\",\"Approved\"],\"labels\":[\"LEV\",\"POP\"],\"integration_ids\":[1,2,3],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"c.author_id\":[\"1fe8570a-76c8-4611-bb9f-bc336cd95b5f\",\"f2b4067d-75fc-45b5-8071-a6992832be2b\"],\"s0_value\":[\"IN_PROGRESS\"],\"s10_job_run_end_start\":1592814148,\"s10_job_run_end_end\":1624350148,\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //region Jira Empty Job Ids And Params Velocity
    @Test
    public void testJiraVelocityEmptyJobIdsAndParams() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTOWithoutCiCdJobsAndParams();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "  \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s9_value\":[\"QA\",\"Testing\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region Jira Velocity Trend
    @Test
    public void testJiraVelocityTrend() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.trend).calculation(VelocityFilter.CALCULATION.ticket_velocity).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select trend,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,trend,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,trend,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,trend,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,im.ingested_at as trend,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,trend \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "group by trend \n" +
                "order by trend \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region Jira Empty Job Ids And Params Velocity Trend
    @Test
    public void testJiraVelocityEmptyJobIdsAndParamsTrend() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTOWithoutCiCdJobsAndParams();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.trend).calculation(VelocityFilter.CALCULATION.ticket_velocity).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select trend,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,trend,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,trend,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,trend,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,im.ingested_at as trend,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,trend \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "group by trend \n" +
                "order by trend \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s9_value\":[\"QA\",\"Testing\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region Jira Velocity Stack Issue Type
    @Test
    public void testJiraVelocityStackIssueType() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).stacks(List.of(VelocityFilter.STACK.issue_type)).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select stack,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,stack,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,stack,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,stack,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,im.issue_type as stack,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,stack \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "group by stack \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region Jira Empty Job Ids And Params Velocity Stack Issue Type
    @Test
    public void testJiraVelocityEmptyJobIdsAndParamsStackIssueType() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTOWithoutCiCdJobsAndParams();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).stacks(List.of(VelocityFilter.STACK.issue_type)).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select stack,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,stack,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,stack,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,stack,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,im.issue_type as stack,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,stack \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "group by stack \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s9_value\":[\"QA\",\"Testing\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region WI Velocity
    @Test
    public void testWIVelocity() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).build(),
                WORK_ITEMS_TYPE_WI, JIRA_ISSUES_FILTER_NULL, WORK_ITEM_FILTER, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.workitem_id as key,im.summary as title,im.attributes ->> 'organization' as org,im.project as project,NULL as repo_id,im.workitem_created_at as estart,CASE WHEN (ims.field_type = 'status' AND ims.field_value in (:s0_value) ) THEN CASE WHEN ims.start_date IS NOT NULL THEN COALESCE(ims.end_date,now()) ELSE ims.end_date END ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.field_type = 'status' AND ims.field_value in (:s9_value) ) THEN CASE WHEN ims.start_date IS NOT NULL THEN COALESCE(ims.end_date,now()) ELSE ims.end_date END ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.issue_mgmt_workitems as im \n" +
                "               left join (select m.workitem_id, c.id, c.committed_at, c.integration_id, c.commit_sha, c.repo_id, c.committer_id, c.author_id from ${company}.scm_commit_workitem_mappings m \n" +
                "               inner join ${company}.scm_commits c on c.commit_sha = m.commit_sha and  c.integration_id = m.scm_integration_id \n" +
                "               and c.integration_id IN (:integration_ids) ) AS c on c.workitem_id = im.workitem_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_workitem_mappings as prjm on prjm.workitem_id = im.workitem_id and prjm.project = im.project \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.issue_mgmt_workitems_timeline as ims on ims.integration_id = im.integration_id AND ims.workitem_id=im.workitem_id \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND s10.end_time > to_timestamp(:s10_job_run_end_start) AND s10.end_time < to_timestamp(:s10_job_run_end_end) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "  \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s10_job_run_end_start\":1592814148,\"s10_job_run_end_end\":1624350148,\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    /*
    //region Jira Empty Job Ids And Params Velocity
    @Test
    public void testJiraVelocityEmptyJobIdsAndParams() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTOWithoutCiCdJobsAndParams();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER, SCM_PR_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,pr_link,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,pr_link,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,to_timestamp(s0.end_time) as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,to_timestamp(s9.end_time) as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integration_id \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +

                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as s0 on s0.integration_id = im.integration_id AND s0.issue_key=im.key AND s0.status in (:s0_value) \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ${company}.jira_issue_statuses as s9 on s9.integration_id = im.integration_id AND s9.issue_key=im.key AND s9.status in (:s9_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,pr_link \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "  \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s9_value\":[\"QA\",\"Testing\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion
    */

    //region WI Velocity Trend
    @Test
    public void testWIVelocityTrend() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.trend).calculation(VelocityFilter.CALCULATION.ticket_velocity).build(),
                WORK_ITEMS_TYPE_WI, JIRA_ISSUES_FILTER_NULL, WORK_ITEM_FILTER, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select trend,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,trend,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,trend,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,trend,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.workitem_id as key,im.summary as title,im.attributes ->> 'organization' as org,im.project as project,NULL as repo_id,im.workitem_created_at as estart,im.ingested_at as trend,CASE WHEN (ims.field_type = 'status' AND ims.field_value in (:s0_value) ) THEN CASE WHEN ims.start_date IS NOT NULL THEN COALESCE(ims.end_date,now()) ELSE ims.end_date END ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.field_type = 'status' AND ims.field_value in (:s9_value) ) THEN CASE WHEN ims.start_date IS NOT NULL THEN COALESCE(ims.end_date,now()) ELSE ims.end_date END ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.issue_mgmt_workitems as im \n" +
                "               left join (select m.workitem_id, c.id, c.committed_at, c.integration_id, c.commit_sha, c.repo_id, c.committer_id, c.author_id from ${company}.scm_commit_workitem_mappings m \n" +
                "               inner join ${company}.scm_commits c on c.commit_sha = m.commit_sha and  c.integration_id = m.scm_integration_id \n" +
                "               and c.integration_id IN (:integration_ids) ) AS c on c.workitem_id = im.workitem_id \n" +
                "               left join ${company}.scm_pullrequests_workitem_mappings as prjm on prjm.workitem_id = im.workitem_id and prjm.project = im.project \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.issue_mgmt_workitems_timeline as ims on ims.integration_id = im.integration_id AND ims.workitem_id=im.workitem_id \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,trend \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "group by trend \n" +
                "order by trend \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    /*
    //region Jira Empty Job Ids And Params Velocity Trend
    @Test
    public void testJiraVelocityEmptyJobIdsAndParamsTrend() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTOWithoutCiCdJobsAndParams();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.trend).calculation(VelocityFilter.CALCULATION.ticket_velocity).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER, SCM_PR_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select trend,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,pr_link,trend,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,pr_link,trend,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,estart,trend,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,im.ingested_at as trend,to_timestamp(s0.end_time) as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,to_timestamp(s9.end_time) as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integration_id \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +

                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as s0 on s0.integration_id = im.integration_id AND s0.issue_key=im.key AND s0.status in (:s0_value) \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ${company}.jira_issue_statuses as s9 on s9.integration_id = im.integration_id AND s9.issue_key=im.key AND s9.status in (:s9_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,pr_link,trend \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "group by trend \n" +
                "order by trend \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s9_value\":[\"QA\",\"Testing\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion
    */

    //region WI Velocity Stack Issue Type
    @Test
    public void testWIVelocityStackIssueType() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).stacks(List.of(VelocityFilter.STACK.issue_type)).build(),
                WORK_ITEMS_TYPE_WI, JIRA_ISSUES_FILTER_NULL, WORK_ITEM_FILTER, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select stack,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,stack,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,stack,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,stack,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.workitem_id as key,im.summary as title,im.attributes ->> 'organization' as org,im.project as project,NULL as repo_id,im.workitem_created_at as estart,im.workitem_type as stack,CASE WHEN (ims.field_type = 'status' AND ims.field_value in (:s0_value) ) THEN CASE WHEN ims.start_date IS NOT NULL THEN COALESCE(ims.end_date,now()) ELSE ims.end_date END ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.field_type = 'status' AND ims.field_value in (:s9_value) ) THEN CASE WHEN ims.start_date IS NOT NULL THEN COALESCE(ims.end_date,now()) ELSE ims.end_date END ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.issue_mgmt_workitems as im \n" +
                "               left join (select m.workitem_id, c.id, c.committed_at, c.integration_id, c.commit_sha, c.repo_id, c.committer_id, c.author_id from ${company}.scm_commit_workitem_mappings m \n" +
                "               inner join ${company}.scm_commits c on c.commit_sha = m.commit_sha and  c.integration_id = m.scm_integration_id \n" +
                "               and c.integration_id IN (:integration_ids) ) AS c on c.workitem_id = im.workitem_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_workitem_mappings as prjm on prjm.workitem_id = im.workitem_id and prjm.project = im.project \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.issue_mgmt_workitems_timeline as ims on ims.integration_id = im.integration_id AND ims.workitem_id=im.workitem_id \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,stack \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "group by stack \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    /*
    //region Jira Empty Job Ids And Params Velocity Stack Issue Type
    @Test
    public void testJiraVelocityEmptyJobIdsAndParamsStackIssueType() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTOWithoutCiCdJobsAndParams();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).stacks(List.of(VelocityFilter.STACK.stack)).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER, SCM_PR_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select stack,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,pr_link,stack,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,pr_link,stack,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,stack,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,im.issue_type as stack,to_timestamp(s0.end_time) as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,to_timestamp(s9.end_time) as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integration_id \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +

                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as s0 on s0.integration_id = im.integration_id AND s0.issue_key=im.key AND s0.status in (:s0_value) \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ${company}.jira_issue_statuses as s9 on s9.integration_id = im.integration_id AND s9.issue_key=im.key AND s9.status in (:s9_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,pr_link,stack \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "group by stack \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s9_value\":[\"QA\",\"Testing\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion
     */

    //region PRs Velocity
    @Test
    public void testPRsVelocityStartIsPRCreated() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTOStartIsPRCreated();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.pr_velocity).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,pr_link,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,pr_link,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,pr_link,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MAX(e5) as e5,MIN(e6) as e6,MIN(e7) as e7,MIN(e8) as e8 from ( \n" +
                "               select pr.id AS u_id,pr.integration_id as integration_id,pr.number AS key,pr.title AS title,NULL as org,pr.project as project,pr.repo_id AS repo_id,pr.metadata->>'pr_link' AS pr_link,c.committed_at as estart,pr.pr_created_at as e0,s1.label_added_at as e1,s2.label_added_at as e2,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e3,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e4,pr.pr_merged_at as e5,s6.end_time AS e6,CASE WHEN (ims.status in (:s7_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e7,s8.end_time AS e8 \n" +
                "               from ${company}.scm_pullrequests AS pr \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings AS cprm ON cprm.scm_pullrequest_id = pr.id \n" +
"               left join ${company}.scm_commits AS c ON c.id = cprm.scm_commit_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings AS prjm ON (pr.id=prjm.pr_uuid) \n" +
                "               left join ${company}.scm_commit_jira_mappings AS m ON m.commit_sha = c.commit_sha \n" +
                "               left join ( select im.* from ${company}.jira_issues AS im join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at ) as im ON (im.KEY = m.issue_key) OR (prjm.issue_key = im.key) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s1 on s1.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s2 on s2.scm_pullrequest_id = pr.id AND s2.name in (:s2_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s6_value) AND ( (jrp.name = :s6_name_0 AND jrp.value IN (:s6_value_0)) ) ) as s6 on s6.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where pr.integration_id IN (:integration_ids) AND ( (im.integration_id IN (:jira_integration_ids)) OR (im.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,pr_link \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "  \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s7_value\":[\"QA\",\"Testing\"],\"s6_name_0\":\"branch\",\"s6_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s2_value\":[\"lbl_1\",\"lbl_c\"],\"s8_name_0\":\"branch\",\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"main\",\"master\"],\"s6_value_0\":[\"dev\",\"staging\"]}", params);
        Assert.assertEquals("{\"0\":\"Dev Time - PR Creared\",\"1\":\"Dev Time - PR Label Added - Any\",\"2\":\"Dev Time - PR Label Added - Specific\",\"3\":\"Lead time to review\",\"4\":\"Review Time\",\"5\":\"Merge Time\",\"6\":\"Deploy to Staging\",\"7\":\"QA\",\"8\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    @Test
    public void testPRsVelocityStartIsPRBranchFilter() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTOPRBranchFilter();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.pr_velocity).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        String expectedSql = "select PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,pr_link,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,pr_link,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,pr_link,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MAX(e3) as e3 from ( \n" +
                "               select pr.id AS u_id,pr.integration_id as integration_id,pr.number AS key,pr.title AS title,NULL as org,pr.project as project,pr.repo_id AS repo_id,pr.metadata->>'pr_link' AS pr_link,c.committed_at as estart,pr.pr_created_at as e0,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e1,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e2,pr.pr_merged_at as e3 \n" +
                "               from ${company}.scm_pullrequests AS pr \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings AS cprm ON cprm.scm_pullrequest_id = pr.id \n" +
"               left join ${company}.scm_commits AS c ON c.id = cprm.scm_commit_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings AS prjm ON (pr.id=prjm.pr_uuid) \n" +
                "               left join ${company}.scm_commit_jira_mappings AS m ON m.commit_sha = c.commit_sha \n" +
                "               left join ( select im.* from ${company}.jira_issues AS im join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at ) as im ON (im.KEY = m.issue_key) OR (prjm.issue_key = im.key) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n \n" +
                "               where pr.integration_id IN (:integration_ids) AND ( (im.integration_id IN (:jira_integration_ids)) OR (im.integration_id IS NULL)) AND pr.source_branch in (:s0_value_0) \n" +
                " AND pr.target_branch in (:s0_value_1) \n \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,pr_link \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "  \n" +
                "  \n";
        Assert.assertEquals(expectedSql, sql);
        Assert.assertEquals("{\"s0_value_0\":[\"PRE\"],\"s0_value_1\":[\"Main\"]}", params);
        Assert.assertEquals("{\"0\":\"Dev Time - PR Creared\",\"1\":\"Lead time to review\",\"2\":\"Review Time\",\"3\":\"Merge Time\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }

    @Test
    public void testPRsVelocityStartIsPRBranchFilterWithLatestApprovalTime() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTOPRBranchFilter();
        velocityConfigDTO = addApprovalTimeFlag(velocityConfigDTO, "MIN");
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.pr_velocity).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        String expectedSql = "select PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,pr_link,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,pr_link,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,pr_link,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MAX(e3) as e3 from ( \n" +
                "               select pr.id AS u_id,pr.integration_id as integration_id,pr.number AS key,pr.title AS title,NULL as org,pr.project as project,pr.repo_id AS repo_id,pr.metadata->>'pr_link' AS pr_link,c.committed_at as estart,pr.pr_created_at as e0,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e1,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e2,pr.pr_merged_at as e3 \n" +
                "               from ${company}.scm_pullrequests AS pr \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings AS cprm ON cprm.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_commits AS c ON c.id = cprm.scm_commit_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings AS prjm ON (pr.id=prjm.pr_uuid) \n" +
                "               left join ${company}.scm_commit_jira_mappings AS m ON m.commit_sha = c.commit_sha \n" +
                "               left join ( select im.* from ${company}.jira_issues AS im join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at ) as im ON (im.KEY = m.issue_key) OR (prjm.issue_key = im.key) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n \n" +
                "               where pr.integration_id IN (:integration_ids) AND ( (im.integration_id IN (:jira_integration_ids)) OR (im.integration_id IS NULL)) AND pr.source_branch in (:s0_value_0) \n" +
                " AND pr.target_branch in (:s0_value_1) \n \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,pr_link \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "  \n" +
                "  \n";
        Assert.assertEquals(expectedSql, sql);
        Assert.assertEquals("{\"s0_value_0\":[\"PRE\"],\"s0_value_1\":[\"Main\"]}", params);
        Assert.assertEquals("{\"0\":\"Dev Time - PR Creared\",\"1\":\"Lead time to review\",\"2\":\"Review Time\",\"3\":\"Merge Time\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    // region PRs velocity with deployment Job
    @Test
    public void testPRsVelocityWithDeployJob() throws BadRequestException, JsonProcessingException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTOStartIsPRDeployed();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.pr_velocity).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,pr_link,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,pr_link,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,pr_link,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MAX(e5) as e5,MIN(e6) as e6,MIN(e7) as e7 from ( \n" +
                "               select pr.id AS u_id,pr.integration_id as integration_id,pr.number AS key,pr.title AS title,NULL as org,pr.project as project,pr.repo_id AS repo_id,pr.metadata->>'pr_link' AS pr_link,c.committed_at as estart,pr.pr_created_at as e0,s1.label_added_at as e1,s2.label_added_at as e2,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e3,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e4,pr.pr_merged_at as e5,CASE WHEN (ims.status in (:s6_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e6,s7.end_time AS e7 \n" +
                "               from ${company}.scm_pullrequests AS pr \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings AS cprm ON cprm.scm_pullrequest_id = pr.id \n" +
"               left join ${company}.scm_commits AS c ON c.id = cprm.scm_commit_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings AS prjm ON (pr.id=prjm.pr_uuid) \n" +
                "               left join ${company}.scm_commit_jira_mappings AS m ON m.commit_sha = c.commit_sha \n" +
                "               left join ( select im.* from ${company}.jira_issues AS im join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at ) as im ON (im.KEY = m.issue_key) OR (prjm.issue_key = im.key) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s1 on s1.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s2 on s2.scm_pullrequest_id = pr.id AND s2.name in (:s2_value) \n" +
                "                   left join (  select build_run.id, deploy_run.end_time from  ( select jr.id, jr.end_time, jrp.value from ${company}.cicd_job_runs as jr  join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s7_value) AND ( (jrp.name = :s7_name_0 AND jrp.value IN (:s7_value_0)) ) AND jrp.name = :marker_name ) deploy_run  inner join  ( select jr.id, jr.end_time, jrp.value from ${company}.cicd_job_runs as jr  join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where  ) build_run  on deploy_run.value = build_run.id ) as s7 on s7.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where pr.integration_id IN (:integration_ids) AND ( (im.integration_id IN (:jira_integration_ids)) OR (im.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,pr_link \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "  \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s7_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s6_value\":[\"QA\",\"Testing\"],\"s2_value\":[\"lbl_1\",\"lbl_c\"],\"marker_name\":\"deploy-marker\",\"s7_value_0\":[\"main\",\"master\"],\"s7_name_0\":\"branch\"}", params);
        Assert.assertEquals("{\"0\":\"Dev Time - PR Creared\",\"1\":\"Dev Time - PR Label Added - Any\",\"2\":\"Dev Time - PR Label Added - Specific\",\"3\":\"Lead time to review\",\"4\":\"Review Time\",\"5\":\"Merge Time\",\"6\":\"QA\",\"7\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }

    //region PRs Velocity
    @Test
    public void testPRsVelocity() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.pr_velocity).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,pr_link,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,pr_link,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,pr_link,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select pr.id AS u_id,pr.integration_id as integration_id,pr.number AS key,pr.title AS title,NULL as org,pr.project as project,pr.repo_id AS repo_id,pr.metadata->>'pr_link' AS pr_link,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.scm_pullrequests AS pr \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings AS cprm ON cprm.scm_pullrequest_id = pr.id \n" +
"               left join ${company}.scm_commits AS c ON c.id = cprm.scm_commit_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings AS prjm ON (pr.id=prjm.pr_uuid) \n" +
                "               left join ${company}.scm_commit_jira_mappings AS m ON m.commit_sha = c.commit_sha \n" +
                "               left join ( select im.* from ${company}.jira_issues AS im join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at ) as im ON (im.KEY = m.issue_key) OR (prjm.issue_key = im.key) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where pr.integration_id IN (:integration_ids) AND ( (im.integration_id IN (:jira_integration_ids)) OR (im.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,pr_link \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "  \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }

    @Test
    public void testPRsVelocityWithLatestApprovalTime() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        velocityConfigDTO = addApprovalTimeFlag(velocityConfigDTO, "MAX");
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.pr_velocity).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,pr_link,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,pr_link,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,pr_link,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MAX(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select pr.id AS u_id,pr.integration_id as integration_id,pr.number AS key,pr.title AS title,NULL as org,pr.project as project,pr.repo_id AS repo_id,pr.metadata->>'pr_link' AS pr_link,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.scm_pullrequests AS pr \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings AS cprm ON cprm.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_commits AS c ON c.id = cprm.scm_commit_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings AS prjm ON (pr.id=prjm.pr_uuid) \n" +
                "               left join ${company}.scm_commit_jira_mappings AS m ON m.commit_sha = c.commit_sha \n" +
                "               left join ( select im.* from ${company}.jira_issues AS im join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at ) as im ON (im.KEY = m.issue_key) OR (prjm.issue_key = im.key) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where pr.integration_id IN (:integration_ids) AND ( (im.integration_id IN (:jira_integration_ids)) OR (im.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,pr_link \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "  \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region PRs Empty Job Ids And Params Velocity
    @Test
    public void testJiraPRsEmptyJobIdsAndParams() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTOWithoutCiCdJobsAndParams();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.pr_velocity).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,pr_link,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,pr_link,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,pr_link,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select pr.id AS u_id,pr.integration_id as integration_id,pr.number AS key,pr.title AS title,NULL as org,pr.project as project,pr.repo_id AS repo_id,pr.metadata->>'pr_link' AS pr_link,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.scm_pullrequests AS pr \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings AS cprm ON cprm.scm_pullrequest_id = pr.id \n" +
"               left join ${company}.scm_commits AS c ON c.id = cprm.scm_commit_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings AS prjm ON (pr.id=prjm.pr_uuid) \n" +
                "               left join ${company}.scm_commit_jira_mappings AS m ON m.commit_sha = c.commit_sha \n" +
                "               left join ( select im.* from ${company}.jira_issues AS im join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at ) as im ON (im.KEY = m.issue_key) OR (prjm.issue_key = im.key) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where pr.integration_id IN (:integration_ids) AND ( (im.integration_id IN (:jira_integration_ids)) OR (im.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,pr_link \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "  \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s9_value\":[\"QA\",\"Testing\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region PRs Velocity Trend
    @Test
    public void testPRsVelocityTrend() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.trend).calculation(VelocityFilter.CALCULATION.pr_velocity).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select trend,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,pr_link,trend,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,pr_link,trend,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,pr_link,MIN(estart) as estart,trend,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select pr.id AS u_id,pr.integration_id as integration_id,pr.number AS key,pr.title AS title,NULL as org,pr.project as project,pr.repo_id AS repo_id,pr.metadata->>'pr_link' AS pr_link,to_timestamp(im.issue_created_at) as estart,EXTRACT(EPOCH FROM (pr.pr_created_at::date)) as trend,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.scm_pullrequests AS pr \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings AS cprm ON cprm.scm_pullrequest_id = pr.id \n" +
"               left join ${company}.scm_commits AS c ON c.id = cprm.scm_commit_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings AS prjm ON (pr.id=prjm.pr_uuid) \n" +
                "               left join ${company}.scm_commit_jira_mappings AS m ON m.commit_sha = c.commit_sha \n" +
                "               left join ( select im.* from ${company}.jira_issues AS im join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at ) as im ON (im.KEY = m.issue_key) OR (prjm.issue_key = im.key) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where pr.integration_id IN (:integration_ids) AND ( (im.integration_id IN (:jira_integration_ids)) OR (im.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,pr_link,trend \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "group by trend \n" +
                "order by trend \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region PRs Empty Job Ids And Params Velocity Trend
    @Test
    public void testPRsVelocityEmptyJobIdsAndParamsTrend() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTOWithoutCiCdJobsAndParams();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.trend).calculation(VelocityFilter.CALCULATION.pr_velocity).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select trend,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,pr_link,trend,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,pr_link,trend,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,pr_link,MIN(estart) as estart,trend,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select pr.id AS u_id,pr.integration_id as integration_id,pr.number AS key,pr.title AS title,NULL as org,pr.project as project,pr.repo_id AS repo_id,pr.metadata->>'pr_link' AS pr_link,to_timestamp(im.issue_created_at) as estart,EXTRACT(EPOCH FROM (pr.pr_created_at::date)) as trend,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.scm_pullrequests AS pr \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings AS cprm ON cprm.scm_pullrequest_id = pr.id \n" +
"               left join ${company}.scm_commits AS c ON c.id = cprm.scm_commit_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings AS prjm ON (pr.id=prjm.pr_uuid) \n" +
                "               left join ${company}.scm_commit_jira_mappings AS m ON m.commit_sha = c.commit_sha \n" +
                "               left join ( select im.* from ${company}.jira_issues AS im join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at ) as im ON (im.KEY = m.issue_key) OR (prjm.issue_key = im.key) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where pr.integration_id IN (:integration_ids) AND ( (im.integration_id IN (:jira_integration_ids)) OR (im.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,pr_link,trend \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "group by trend \n" +
                "order by trend \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s9_value\":[\"QA\",\"Testing\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region Jira Values
    @Test
    public void testJiraValues() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        Map<String, SortingOrder> sorts = velocityConfigDTO.getFixedStages().stream().map(VelocityConfigDTO.Stage::getName).limit(2)
                .map(s-> Map.entry(s, SortingOrder.DESC))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.values).calculation(VelocityFilter.CALCULATION.ticket_velocity).sort(sorts).pageSize(100).page(0).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY s1 DESC NULLS LAST,s2 DESC NULLS LAST \n" +
                "   LIMIT 100 OFFSET 0  \n", sql);
        Assert.assertEquals("SELECT COUNT(*) FROM (   select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY s1 DESC NULLS LAST,s2 DESC NULLS LAST \n" +
                "    \n" +
                " ) AS counted", actual.getCountSql());
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region Jira Values - Default Sort
    @Test
    public void testJiraValuesDefaultSort() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.values).calculation(VelocityFilter.CALCULATION.ticket_velocity).pageSize(100).page(0).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY t DESC NULLS LAST,u_id DESC \n" +
                "   LIMIT 100 OFFSET 0  \n", sql);
        Assert.assertEquals("SELECT COUNT(*) FROM (   select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY t DESC NULLS LAST,u_id DESC \n" +
                "    \n" +
                " ) AS counted", actual.getCountSql());
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region Jira Values - With Trend Key
    @Test
    public void testJiraValuesWithTrendKeys() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        Map<String, SortingOrder> sorts = velocityConfigDTO.getFixedStages().stream().map(VelocityConfigDTO.Stage::getName).limit(2)
                .map(s-> Map.entry(s, SortingOrder.DESC))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.values).calculation(VelocityFilter.CALCULATION.ticket_velocity).sort(sorts).valueTrendKeys(List.of(1612137600L)).pageSize(100).page(0).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND im.ingested_at IN (:value_trend_keys) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY s1 DESC NULLS LAST,s2 DESC NULLS LAST \n" +
                "   LIMIT 100 OFFSET 0  \n", sql);
        Assert.assertEquals("SELECT COUNT(*) FROM (   select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND im.ingested_at IN (:value_trend_keys) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY s1 DESC NULLS LAST,s2 DESC NULLS LAST \n" +
                "    \n" +
                " ) AS counted", actual.getCountSql());
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"value_trend_keys\":[1612137600],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region Jira Stack By Issue Type Values
    @Test
    public void testJiraStackByIssueTypeValues() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        Map<String, SortingOrder> sorts = velocityConfigDTO.getFixedStages().stream().map(VelocityConfigDTO.Stage::getName).limit(2)
                .map(s-> Map.entry(s, SortingOrder.DESC))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.values).calculation(VelocityFilter.CALCULATION.ticket_velocity)
                        .sort(sorts).stacks(List.of(VelocityFilter.STACK.issue_type)).valueStacks(List.of("NEW FEATURE", "BUG"))
                        .pageSize(100).page(0).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND im.issue_type IN (:value_stacks) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY s1 DESC NULLS LAST,s2 DESC NULLS LAST \n" +
                "   LIMIT 100 OFFSET 0  \n", sql);
        Assert.assertEquals("SELECT COUNT(*) FROM (   select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND im.issue_type IN (:value_stacks) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY s1 DESC NULLS LAST,s2 DESC NULLS LAST \n" +
                "    \n" +
                " ) AS counted", actual.getCountSql());
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"value_stacks\":[\"NEW FEATURE\",\"BUG\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region PRs Values
    @Test
    public void testPRsValues() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        Map<String, SortingOrder> sorts = velocityConfigDTO.getFixedStages().stream().map(VelocityConfigDTO.Stage::getName).limit(2)
                .map(s-> Map.entry(s, SortingOrder.DESC))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.values).calculation(VelocityFilter.CALCULATION.pr_velocity).sort(sorts).valueTrendKeys(List.of(1612137600L)).pageSize(100).page(0).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("  select u_id,integration_id,key,title,org,project,repo_id,pr_link,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,pr_link,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,pr_link,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select pr.id AS u_id,pr.integration_id as integration_id,pr.number AS key,pr.title AS title,NULL as org,pr.project as project,pr.repo_id AS repo_id,pr.metadata->>'pr_link' AS pr_link,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.scm_pullrequests AS pr \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings AS cprm ON cprm.scm_pullrequest_id = pr.id \n" +
"               left join ${company}.scm_commits AS c ON c.id = cprm.scm_commit_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings AS prjm ON (pr.id=prjm.pr_uuid) \n" +
                "               left join ${company}.scm_commit_jira_mappings AS m ON m.commit_sha = c.commit_sha \n" +
                "               left join ( select im.* from ${company}.jira_issues AS im join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at ) as im ON (im.KEY = m.issue_key) OR (prjm.issue_key = im.key) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where pr.integration_id IN (:integration_ids) AND ( (im.integration_id IN (:jira_integration_ids)) OR (im.integration_id IS NULL)) AND EXTRACT(EPOCH FROM (pr.pr_created_at::date)) IN (:value_trend_keys) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,pr_link \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY s1 DESC NULLS LAST,s2 DESC NULLS LAST \n" +
                "   LIMIT 100 OFFSET 0  \n", sql);
        Assert.assertEquals("SELECT COUNT(*) FROM (   select u_id,integration_id,key,title,org,project,repo_id,pr_link,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,pr_link,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,pr_link,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select pr.id AS u_id,pr.integration_id as integration_id,pr.number AS key,pr.title AS title,NULL as org,pr.project as project,pr.repo_id AS repo_id,pr.metadata->>'pr_link' AS pr_link,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.scm_pullrequests AS pr \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings AS cprm ON cprm.scm_pullrequest_id = pr.id \n" +
"               left join ${company}.scm_commits AS c ON c.id = cprm.scm_commit_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings AS prjm ON (pr.id=prjm.pr_uuid) \n" +
                "               left join ${company}.scm_commit_jira_mappings AS m ON m.commit_sha = c.commit_sha \n" +
                "               left join ( select im.* from ${company}.jira_issues AS im join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at ) as im ON (im.KEY = m.issue_key) OR (prjm.issue_key = im.key) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where pr.integration_id IN (:integration_ids) AND ( (im.integration_id IN (:jira_integration_ids)) OR (im.integration_id IS NULL)) AND EXTRACT(EPOCH FROM (pr.pr_created_at::date)) IN (:value_trend_keys) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,pr_link \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY s1 DESC NULLS LAST,s2 DESC NULLS LAST \n" +
                "    \n" +
                " ) AS counted", actual.getCountSql());
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"value_trend_keys\":[1612137600],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region Jira Histogram
    @Test
    public void testJiraHistogram() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.histogram).calculation(VelocityFilter.CALCULATION.ticket_velocity).histogramStageName("Dev Time - PR Creared").histogramBucketsCount(5).limitToOnlyApplicableData(true).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("SELECT MIN(s2) AS min, MAX(s2) AS max, WIDTH_BUCKET(s2, 0, 2592001, 5) AS bucket, COUNT(*) AS cnt FROM (\n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND s10.end_time > to_timestamp(:s10_job_run_end_start) AND s10.end_time < to_timestamp(:s10_job_run_end_end) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as histo\n" +
                "GROUP BY bucket\n" +
                "ORDER BY bucket;", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s10_job_run_end_start\":1592814148,\"s10_job_run_end_end\":1624350148,\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region Jira Histogram - With Trend Keys
    @Test
    public void testJiraHistogramWithTrendKeys() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.histogram).calculation(VelocityFilter.CALCULATION.ticket_velocity).valueTrendKeys(List.of(1612137600L)).histogramStageName("Dev Time - PR Creared").histogramBucketsCount(8).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("SELECT MIN(s2) AS min, MAX(s2) AS max, WIDTH_BUCKET(s2, 0, 2592001, 8) AS bucket, COUNT(*) AS cnt FROM (\n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND im.ingested_at IN (:value_trend_keys) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as histo\n" +
                "GROUP BY bucket\n" +
                "ORDER BY bucket;", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"value_trend_keys\":[1612137600],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region Jira Histogram Stack By Issue Type
    @Test
    public void testJiraHistogramStackByIssueType() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.histogram).calculation(VelocityFilter.CALCULATION.ticket_velocity)
                        .stacks(List.of(VelocityFilter.STACK.issue_type)).valueStacks(List.of("NEW FEATURE", "BUG"))
                        .histogramStageName("Dev Time - PR Creared").histogramBucketsCount(8)
                        .build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("SELECT MIN(s2) AS min, MAX(s2) AS max, WIDTH_BUCKET(s2, 0, 2592001, 8) AS bucket, COUNT(*) AS cnt FROM (\n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND im.issue_type IN (:value_stacks) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as histo\n" +
                "GROUP BY bucket\n" +
                "ORDER BY bucket;", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"value_stacks\":[\"NEW FEATURE\",\"BUG\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region PRs Histogram
    @Test
    public void testPRsHistogram() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.histogram).calculation(VelocityFilter.CALCULATION.pr_velocity).valueTrendKeys(List.of(1612137600L))
                        .histogramStageName("Dev Time - PR Creared").histogramBucketsCount(8).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("SELECT MIN(s2) AS min, MAX(s2) AS max, WIDTH_BUCKET(s2, 0, 2592001, 8) AS bucket, COUNT(*) AS cnt FROM (\n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,pr_link,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,pr_link,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,pr_link,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select pr.id AS u_id,pr.integration_id as integration_id,pr.number AS key,pr.title AS title,NULL as org,pr.project as project,pr.repo_id AS repo_id,pr.metadata->>'pr_link' AS pr_link,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.scm_pullrequests AS pr \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings AS cprm ON cprm.scm_pullrequest_id = pr.id \n" +
"               left join ${company}.scm_commits AS c ON c.id = cprm.scm_commit_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings AS prjm ON (pr.id=prjm.pr_uuid) \n" +
                "               left join ${company}.scm_commit_jira_mappings AS m ON m.commit_sha = c.commit_sha \n" +
                "               left join ( select im.* from ${company}.jira_issues AS im join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at ) as im ON (im.KEY = m.issue_key) OR (prjm.issue_key = im.key) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where pr.integration_id IN (:integration_ids) AND ( (im.integration_id IN (:jira_integration_ids)) OR (im.integration_id IS NULL)) AND EXTRACT(EPOCH FROM (pr.pr_created_at::date)) IN (:value_trend_keys) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,pr_link \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as histo\n" +
                "GROUP BY bucket\n" +
                "ORDER BY bucket;", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"value_trend_keys\":[1612137600],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion
    //endregion

    //region Limit To Only Applicable Data
    //region Jira Velocity
    @Test
    public void testJiraVelocityApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).limitToOnlyApplicableData(true).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "  \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region Jira Empty Job Ids And Params Velocity
    @Test
    public void testJiraVelocityEmptyJobIdsAndParamsApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTOWithoutCiCdJobsAndParams();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).limitToOnlyApplicableData(true).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "  \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s9_value\":[\"QA\",\"Testing\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region Jira Velocity Trend
    @Test
    public void testJiraVelocityTrendApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.trend).calculation(VelocityFilter.CALCULATION.ticket_velocity).limitToOnlyApplicableData(true).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select trend,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,trend,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,trend,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,trend,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,im.ingested_at as trend,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,trend \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "group by trend \n" +
                "order by trend \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }

    @Test
    public void testJiraVelocityTrendApplicableDataWithLatestApprovalTime() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        velocityConfigDTO = addApprovalTimeFlag(velocityConfigDTO, "MAX");
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.trend).calculation(VelocityFilter.CALCULATION.ticket_velocity).limitToOnlyApplicableData(true).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select trend,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,trend,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,trend,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,trend,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MAX(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,im.ingested_at as trend,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
                "               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,trend \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "group by trend \n" +
                "order by trend \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region Jira Empty Job Ids And Params Velocity Trend
    @Test
    public void testJiraVelocityEmptyJobIdsAndParamsTrendApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTOWithoutCiCdJobsAndParams();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.trend).calculation(VelocityFilter.CALCULATION.ticket_velocity).limitToOnlyApplicableData(true).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select trend,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,trend,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,trend,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,trend,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,im.ingested_at as trend,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,trend \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "group by trend \n" +
                "order by trend \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s9_value\":[\"QA\",\"Testing\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region Jira Velocity Stack Issue Type
    @Test
    public void testJiraVelocityStackIssueTypeApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).stacks(List.of(VelocityFilter.STACK.issue_type)).limitToOnlyApplicableData(true).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select stack,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,stack,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,stack,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,stack,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,im.issue_type as stack,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,stack \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "group by stack \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region Jira Empty Job Ids And Params Velocity Stack Issue Type
    @Test
    public void testJiraVelocityEmptyJobIdsAndParamsStackIssueTypeApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTOWithoutCiCdJobsAndParams();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).stacks(List.of(VelocityFilter.STACK.issue_type)).limitToOnlyApplicableData(true).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select stack,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,stack,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,stack,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,stack,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,im.issue_type as stack,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,stack \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "group by stack \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s9_value\":[\"QA\",\"Testing\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region PRs Velocity
    @Test
    public void testPRsVelocityApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.pr_velocity).limitToOnlyApplicableData(true).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,pr_link,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,pr_link,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,pr_link,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select pr.id AS u_id,pr.integration_id as integration_id,pr.number AS key,pr.title AS title,NULL as org,pr.project as project,pr.repo_id AS repo_id,pr.metadata->>'pr_link' AS pr_link,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.scm_pullrequests AS pr \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings AS cprm ON cprm.scm_pullrequest_id = pr.id \n" +
"               left join ${company}.scm_commits AS c ON c.id = cprm.scm_commit_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings AS prjm ON (pr.id=prjm.pr_uuid) \n" +
                "               left join ${company}.scm_commit_jira_mappings AS m ON m.commit_sha = c.commit_sha \n" +
                "               left join ( select im.* from ${company}.jira_issues AS im join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at ) as im ON (im.KEY = m.issue_key) OR (prjm.issue_key = im.key) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where pr.integration_id IN (:integration_ids) AND ( (im.integration_id IN (:jira_integration_ids)) OR (im.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,pr_link \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "  \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region PRs Empty Job Ids And Params Velocity
    @Test
    public void testJiraPRsEmptyJobIdsAndParamsApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTOWithoutCiCdJobsAndParams();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.pr_velocity).limitToOnlyApplicableData(true).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,pr_link,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,pr_link,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,pr_link,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select pr.id AS u_id,pr.integration_id as integration_id,pr.number AS key,pr.title AS title,NULL as org,pr.project as project,pr.repo_id AS repo_id,pr.metadata->>'pr_link' AS pr_link,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.scm_pullrequests AS pr \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings AS cprm ON cprm.scm_pullrequest_id = pr.id \n" +
"               left join ${company}.scm_commits AS c ON c.id = cprm.scm_commit_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings AS prjm ON (pr.id=prjm.pr_uuid) \n" +
                "               left join ${company}.scm_commit_jira_mappings AS m ON m.commit_sha = c.commit_sha \n" +
                "               left join ( select im.* from ${company}.jira_issues AS im join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at ) as im ON (im.KEY = m.issue_key) OR (prjm.issue_key = im.key) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where pr.integration_id IN (:integration_ids) AND ( (im.integration_id IN (:jira_integration_ids)) OR (im.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,pr_link \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "  \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s9_value\":[\"QA\",\"Testing\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region PRs Velocity Trend
    @Test
    public void testPRsVelocityTrendApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.trend).calculation(VelocityFilter.CALCULATION.pr_velocity).limitToOnlyApplicableData(true).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select trend,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,pr_link,trend,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,pr_link,trend,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,pr_link,MIN(estart) as estart,trend,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select pr.id AS u_id,pr.integration_id as integration_id,pr.number AS key,pr.title AS title,NULL as org,pr.project as project,pr.repo_id AS repo_id,pr.metadata->>'pr_link' AS pr_link,to_timestamp(im.issue_created_at) as estart,EXTRACT(EPOCH FROM (pr.pr_created_at::date)) as trend,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.scm_pullrequests AS pr \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings AS cprm ON cprm.scm_pullrequest_id = pr.id \n" +
"               left join ${company}.scm_commits AS c ON c.id = cprm.scm_commit_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings AS prjm ON (pr.id=prjm.pr_uuid) \n" +
                "               left join ${company}.scm_commit_jira_mappings AS m ON m.commit_sha = c.commit_sha \n" +
                "               left join ( select im.* from ${company}.jira_issues AS im join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at ) as im ON (im.KEY = m.issue_key) OR (prjm.issue_key = im.key) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where pr.integration_id IN (:integration_ids) AND ( (im.integration_id IN (:jira_integration_ids)) OR (im.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,pr_link,trend \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "group by trend \n" +
                "order by trend \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region PRs Empty Job Ids And Params Velocity Trend
    @Test
    public void testPRsVelocityEmptyJobIdsAndParamsTrendApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTOWithoutCiCdJobsAndParams();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.trend).calculation(VelocityFilter.CALCULATION.pr_velocity).limitToOnlyApplicableData(true).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select trend,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,pr_link,trend,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,pr_link,trend,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,pr_link,MIN(estart) as estart,trend,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select pr.id AS u_id,pr.integration_id as integration_id,pr.number AS key,pr.title AS title,NULL as org,pr.project as project,pr.repo_id AS repo_id,pr.metadata->>'pr_link' AS pr_link,to_timestamp(im.issue_created_at) as estart,EXTRACT(EPOCH FROM (pr.pr_created_at::date)) as trend,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.scm_pullrequests AS pr \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings AS cprm ON cprm.scm_pullrequest_id = pr.id \n" +
"               left join ${company}.scm_commits AS c ON c.id = cprm.scm_commit_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings AS prjm ON (pr.id=prjm.pr_uuid) \n" +
                "               left join ${company}.scm_commit_jira_mappings AS m ON m.commit_sha = c.commit_sha \n" +
                "               left join ( select im.* from ${company}.jira_issues AS im join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at ) as im ON (im.KEY = m.issue_key) OR (prjm.issue_key = im.key) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where pr.integration_id IN (:integration_ids) AND ( (im.integration_id IN (:jira_integration_ids)) OR (im.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,pr_link,trend \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "group by trend \n" +
                "order by trend \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s9_value\":[\"QA\",\"Testing\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region Jira Values
    @Test
    public void testJiraValuesApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        Map<String, SortingOrder> sorts = velocityConfigDTO.getFixedStages().stream().map(VelocityConfigDTO.Stage::getName).limit(2)
                .map(s-> Map.entry(s, SortingOrder.DESC))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.values).calculation(VelocityFilter.CALCULATION.ticket_velocity).sort(sorts).pageSize(100).page(0).limitToOnlyApplicableData(true).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY s1 DESC NULLS LAST,s2 DESC NULLS LAST \n" +
                "   LIMIT 100 OFFSET 0  \n", sql);
        Assert.assertEquals("SELECT COUNT(*) FROM (   select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY s1 DESC NULLS LAST,s2 DESC NULLS LAST \n" +
                "    \n" +
                " ) AS counted", actual.getCountSql());
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region Jira Values - With Trend Keys
    @Test
    public void testJiraValuesApplicableDataWithTrendKeys() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        Map<String, SortingOrder> sorts = velocityConfigDTO.getFixedStages().stream().map(VelocityConfigDTO.Stage::getName).limit(2)
                .map(s-> Map.entry(s, SortingOrder.DESC))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.values).calculation(VelocityFilter.CALCULATION.ticket_velocity).sort(sorts).valueTrendKeys(List.of(1612137600L)).pageSize(100).page(0).limitToOnlyApplicableData(true).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND im.ingested_at IN (:value_trend_keys) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY s1 DESC NULLS LAST,s2 DESC NULLS LAST \n" +
                "   LIMIT 100 OFFSET 0  \n", sql);
        Assert.assertEquals("SELECT COUNT(*) FROM (   select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND im.ingested_at IN (:value_trend_keys) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY s1 DESC NULLS LAST,s2 DESC NULLS LAST \n" +
                "    \n" +
                " ) AS counted", actual.getCountSql());
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"value_trend_keys\":[1612137600],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region Jira Stack By Issue Type Values
    @Test
    public void testJiraStackByIssueTypeValuesApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        Map<String, SortingOrder> sorts = velocityConfigDTO.getFixedStages().stream().map(VelocityConfigDTO.Stage::getName).limit(2)
                .map(s-> Map.entry(s, SortingOrder.DESC))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.values).calculation(VelocityFilter.CALCULATION.ticket_velocity)
                        .sort(sorts).limitToOnlyApplicableData(true).stacks(List.of(VelocityFilter.STACK.issue_type)).valueStacks(List.of("NEW FEATURE", "BUG"))
                        .pageSize(100).page(0).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND im.issue_type IN (:value_stacks) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY s1 DESC NULLS LAST,s2 DESC NULLS LAST \n" +
                "   LIMIT 100 OFFSET 0  \n", sql);
        Assert.assertEquals("SELECT COUNT(*) FROM (   select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND im.issue_type IN (:value_stacks) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY s1 DESC NULLS LAST,s2 DESC NULLS LAST \n" +
                "    \n" +
                " ) AS counted", actual.getCountSql());
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"value_stacks\":[\"NEW FEATURE\",\"BUG\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region PRs Values
    @Test
    public void testPRsValuesApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        Map<String, SortingOrder> sorts = velocityConfigDTO.getFixedStages().stream().map(VelocityConfigDTO.Stage::getName).limit(2)
                .map(s-> Map.entry(s, SortingOrder.DESC))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.values).calculation(VelocityFilter.CALCULATION.pr_velocity).sort(sorts).valueTrendKeys(List.of(1612137600L)).pageSize(100).page(0).limitToOnlyApplicableData(true).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("  select u_id,integration_id,key,title,org,project,repo_id,pr_link,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,pr_link,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,pr_link,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select pr.id AS u_id,pr.integration_id as integration_id,pr.number AS key,pr.title AS title,NULL as org,pr.project as project,pr.repo_id AS repo_id,pr.metadata->>'pr_link' AS pr_link,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.scm_pullrequests AS pr \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings AS cprm ON cprm.scm_pullrequest_id = pr.id \n" +
"               left join ${company}.scm_commits AS c ON c.id = cprm.scm_commit_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings AS prjm ON (pr.id=prjm.pr_uuid) \n" +
                "               left join ${company}.scm_commit_jira_mappings AS m ON m.commit_sha = c.commit_sha \n" +
                "               left join ( select im.* from ${company}.jira_issues AS im join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at ) as im ON (im.KEY = m.issue_key) OR (prjm.issue_key = im.key) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where pr.integration_id IN (:integration_ids) AND ( (im.integration_id IN (:jira_integration_ids)) OR (im.integration_id IS NULL)) AND EXTRACT(EPOCH FROM (pr.pr_created_at::date)) IN (:value_trend_keys) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,pr_link \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY s1 DESC NULLS LAST,s2 DESC NULLS LAST \n" +
                "   LIMIT 100 OFFSET 0  \n", sql);
        Assert.assertEquals("SELECT COUNT(*) FROM (   select u_id,integration_id,key,title,org,project,repo_id,pr_link,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,pr_link,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,pr_link,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select pr.id AS u_id,pr.integration_id as integration_id,pr.number AS key,pr.title AS title,NULL as org,pr.project as project,pr.repo_id AS repo_id,pr.metadata->>'pr_link' AS pr_link,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.scm_pullrequests AS pr \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings AS cprm ON cprm.scm_pullrequest_id = pr.id \n" +
"               left join ${company}.scm_commits AS c ON c.id = cprm.scm_commit_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings AS prjm ON (pr.id=prjm.pr_uuid) \n" +
                "               left join ${company}.scm_commit_jira_mappings AS m ON m.commit_sha = c.commit_sha \n" +
                "               left join ( select im.* from ${company}.jira_issues AS im join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at ) as im ON (im.KEY = m.issue_key) OR (prjm.issue_key = im.key) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where pr.integration_id IN (:integration_ids) AND ( (im.integration_id IN (:jira_integration_ids)) OR (im.integration_id IS NULL)) AND EXTRACT(EPOCH FROM (pr.pr_created_at::date)) IN (:value_trend_keys) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,pr_link \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY s1 DESC NULLS LAST,s2 DESC NULLS LAST \n" +
                "    \n" +
                " ) AS counted", actual.getCountSql());
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"value_trend_keys\":[1612137600],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region Jira Histogram
    @Test
    public void testJiraHistogramApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.histogram).calculation(VelocityFilter.CALCULATION.ticket_velocity).histogramStageName("Dev Time - PR Creared").histogramBucketsCount(5).limitToOnlyApplicableData(true).limitToOnlyApplicableData(true).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("SELECT MIN(s2) AS min, MAX(s2) AS max, WIDTH_BUCKET(s2, 0, 2592001, 5) AS bucket, COUNT(*) AS cnt FROM (\n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND s10.end_time > to_timestamp(:s10_job_run_end_start) AND s10.end_time < to_timestamp(:s10_job_run_end_end) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as histo\n" +
                "GROUP BY bucket\n" +
                "ORDER BY bucket;", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s10_job_run_end_start\":1592814148,\"s10_job_run_end_end\":1624350148,\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region Jira Histogram - With Trend Keys
    @Test
    public void testJiraHistogramApplicableDataWithTrendKeys() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.histogram).calculation(VelocityFilter.CALCULATION.ticket_velocity).valueTrendKeys(List.of(1612137600L)).limitToOnlyApplicableData(true).histogramStageName("Dev Time - PR Creared").histogramBucketsCount(8).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("SELECT MIN(s2) AS min, MAX(s2) AS max, WIDTH_BUCKET(s2, 0, 2592001, 8) AS bucket, COUNT(*) AS cnt FROM (\n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND im.ingested_at IN (:value_trend_keys) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as histo\n" +
                "GROUP BY bucket\n" +
                "ORDER BY bucket;", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"value_trend_keys\":[1612137600],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region Jira Histogram Stack By Issue Type
    @Test
    public void testJiraHistogramStackByIssueTypeApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.histogram).calculation(VelocityFilter.CALCULATION.ticket_velocity)
                        .limitToOnlyApplicableData(true).stacks(List.of(VelocityFilter.STACK.issue_type)).valueStacks(List.of("NEW FEATURE", "BUG"))
                        .histogramStageName("Dev Time - PR Creared").histogramBucketsCount(8)
                        .build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("SELECT MIN(s2) AS min, MAX(s2) AS max, WIDTH_BUCKET(s2, 0, 2592001, 8) AS bucket, COUNT(*) AS cnt FROM (\n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND im.issue_type IN (:value_stacks) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as histo\n" +
                "GROUP BY bucket\n" +
                "ORDER BY bucket;", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"value_stacks\":[\"NEW FEATURE\",\"BUG\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region PRs Histogram
    @Test
    public void testPRsHistogramApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.histogram).calculation(VelocityFilter.CALCULATION.pr_velocity).valueTrendKeys(List.of(1612137600L)).limitToOnlyApplicableData(true)
                        .histogramStageName("Dev Time - PR Creared").histogramBucketsCount(8).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("SELECT MIN(s2) AS min, MAX(s2) AS max, WIDTH_BUCKET(s2, 0, 2592001, 8) AS bucket, COUNT(*) AS cnt FROM (\n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,pr_link,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,pr_link,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,pr_link,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select pr.id AS u_id,pr.integration_id as integration_id,pr.number AS key,pr.title AS title,NULL as org,pr.project as project,pr.repo_id AS repo_id,pr.metadata->>'pr_link' AS pr_link,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.scm_pullrequests AS pr \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings AS cprm ON cprm.scm_pullrequest_id = pr.id \n" +
"               left join ${company}.scm_commits AS c ON c.id = cprm.scm_commit_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings AS prjm ON (pr.id=prjm.pr_uuid) \n" +
                "               left join ${company}.scm_commit_jira_mappings AS m ON m.commit_sha = c.commit_sha \n" +
                "               left join ( select im.* from ${company}.jira_issues AS im join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at ) as im ON (im.KEY = m.issue_key) OR (prjm.issue_key = im.key) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where pr.integration_id IN (:integration_ids) AND ( (im.integration_id IN (:jira_integration_ids)) OR (im.integration_id IS NULL)) AND EXTRACT(EPOCH FROM (pr.pr_created_at::date)) IN (:value_trend_keys) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,pr_link \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as histo\n" +
                "GROUP BY bucket\n" +
                "ORDER BY bucket;", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"value_trend_keys\":[1612137600],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region Jira Velocity Stack Issue Priority
    @Test
    public void testJiraVelocityStackIssuePriorityApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).stacks(List.of(VelocityFilter.STACK.issue_priority)).limitToOnlyApplicableData(true).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select stack,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,stack,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,stack,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,stack,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,im.priority as stack,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,stack \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "group by stack \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region WI Velocity Stack Issue Priority
    @Test
    public void testWIVelocityStackIssuePriorityApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).stacks(List.of(VelocityFilter.STACK.issue_priority)).build(),
                WORK_ITEMS_TYPE_WI, JIRA_ISSUES_FILTER_NULL, WORK_ITEM_FILTER, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select stack,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,stack,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,stack,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,stack,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.workitem_id as key,im.summary as title,im.attributes ->> 'organization' as org,im.project as project,NULL as repo_id,im.workitem_created_at as estart,im.priority as stack,CASE WHEN (ims.field_type = 'status' AND ims.field_value in (:s0_value) ) THEN CASE WHEN ims.start_date IS NOT NULL THEN COALESCE(ims.end_date,now()) ELSE ims.end_date END ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.field_type = 'status' AND ims.field_value in (:s9_value) ) THEN CASE WHEN ims.start_date IS NOT NULL THEN COALESCE(ims.end_date,now()) ELSE ims.end_date END ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.issue_mgmt_workitems as im \n" +
                "               left join (select m.workitem_id, c.id, c.committed_at, c.integration_id, c.commit_sha, c.repo_id, c.committer_id, c.author_id from ${company}.scm_commit_workitem_mappings m \n" +
                "               inner join ${company}.scm_commits c on c.commit_sha = m.commit_sha and  c.integration_id = m.scm_integration_id \n" +
                "               and c.integration_id IN (:integration_ids) ) AS c on c.workitem_id = im.workitem_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_workitem_mappings as prjm on prjm.workitem_id = im.workitem_id and prjm.project = im.project \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.issue_mgmt_workitems_timeline as ims on ims.integration_id = im.integration_id AND ims.workitem_id=im.workitem_id \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,stack \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "group by stack \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
//endregion

    //region Jira Stack By Issue Priority Values
    @Test
    public void testJiraStackByIssuePriorityValuesApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        Map<String, SortingOrder> sorts = velocityConfigDTO.getFixedStages().stream().map(VelocityConfigDTO.Stage::getName).limit(2)
                .map(s-> Map.entry(s, SortingOrder.DESC))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.values).calculation(VelocityFilter.CALCULATION.ticket_velocity)
                        .sort(sorts).limitToOnlyApplicableData(true).stacks(List.of(VelocityFilter.STACK.issue_priority)).valueStacks(List.of("NEW FEATURE", "BUG"))
                        .pageSize(100).page(0).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND im.priority IN (:value_stacks) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY s1 DESC NULLS LAST,s2 DESC NULLS LAST \n" +
                "   LIMIT 100 OFFSET 0  \n", sql);
        Assert.assertEquals("SELECT COUNT(*) FROM (   select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND im.priority IN (:value_stacks) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY s1 DESC NULLS LAST,s2 DESC NULLS LAST \n" +
                "    \n" +
                " ) AS counted", actual.getCountSql());
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"value_stacks\":[\"NEW FEATURE\",\"BUG\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
//endregion

    //region Jira Histogram Stack By Issue Priority
    @Test
    public void testJiraHistogramStackByIssuePriorityApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.histogram).calculation(VelocityFilter.CALCULATION.ticket_velocity)
                        .limitToOnlyApplicableData(true).stacks(List.of(VelocityFilter.STACK.issue_priority)).valueStacks(List.of("NEW FEATURE", "BUG"))
                        .histogramStageName("Dev Time - PR Creared").histogramBucketsCount(8)
                        .build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("SELECT MIN(s2) AS min, MAX(s2) AS max, WIDTH_BUCKET(s2, 0, 2592001, 8) AS bucket, COUNT(*) AS cnt FROM (\n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND im.priority IN (:value_stacks) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as histo\n" +
                "GROUP BY bucket\n" +
                "ORDER BY bucket;", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"value_stacks\":[\"NEW FEATURE\",\"BUG\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
//endregion

    //region Jira Velocity Stack Issue Project
    @Test
    public void testJiraVelocityStackIssueProjectApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).stacks(List.of(VelocityFilter.STACK.issue_project)).limitToOnlyApplicableData(true).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select stack,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,stack,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,stack,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,stack,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,im.project as stack,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,stack \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "group by stack \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
//endregion

    //region WI Velocity Stack Issue Project
    @Test
    public void testWIVelocityStackIssueProjectApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).stacks(List.of(VelocityFilter.STACK.issue_project)).build(),
                WORK_ITEMS_TYPE_WI, JIRA_ISSUES_FILTER_NULL, WORK_ITEM_FILTER, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select stack,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,stack,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,stack,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,stack,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.workitem_id as key,im.summary as title,im.attributes ->> 'organization' as org,im.project as project,NULL as repo_id,im.workitem_created_at as estart,im.project as stack,CASE WHEN (ims.field_type = 'status' AND ims.field_value in (:s0_value) ) THEN CASE WHEN ims.start_date IS NOT NULL THEN COALESCE(ims.end_date,now()) ELSE ims.end_date END ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.field_type = 'status' AND ims.field_value in (:s9_value) ) THEN CASE WHEN ims.start_date IS NOT NULL THEN COALESCE(ims.end_date,now()) ELSE ims.end_date END ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.issue_mgmt_workitems as im \n" +
                "               left join (select m.workitem_id, c.id, c.committed_at, c.integration_id, c.commit_sha, c.repo_id, c.committer_id, c.author_id from ${company}.scm_commit_workitem_mappings m \n" +
                "               inner join ${company}.scm_commits c on c.commit_sha = m.commit_sha and  c.integration_id = m.scm_integration_id \n" +
                "               and c.integration_id IN (:integration_ids) ) AS c on c.workitem_id = im.workitem_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_workitem_mappings as prjm on prjm.workitem_id = im.workitem_id and prjm.project = im.project \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.issue_mgmt_workitems_timeline as ims on ims.integration_id = im.integration_id AND ims.workitem_id=im.workitem_id \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,stack \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "group by stack \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
//endregion

    //region Jira Stack By Issue Project Values
    @Test
    public void testJiraStackByIssueProjectValuesApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        Map<String, SortingOrder> sorts = velocityConfigDTO.getFixedStages().stream().map(VelocityConfigDTO.Stage::getName).limit(2)
                .map(s-> Map.entry(s, SortingOrder.DESC))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.values).calculation(VelocityFilter.CALCULATION.ticket_velocity)
                        .sort(sorts).limitToOnlyApplicableData(true).stacks(List.of(VelocityFilter.STACK.issue_project)).valueStacks(List.of("NEW FEATURE", "BUG"))
                        .pageSize(100).page(0).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND im.project IN (:value_stacks) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY s1 DESC NULLS LAST,s2 DESC NULLS LAST \n" +
                "   LIMIT 100 OFFSET 0  \n", sql);
        Assert.assertEquals("SELECT COUNT(*) FROM (   select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND im.project IN (:value_stacks) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY s1 DESC NULLS LAST,s2 DESC NULLS LAST \n" +
                "    \n" +
                " ) AS counted", actual.getCountSql());
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"value_stacks\":[\"NEW FEATURE\",\"BUG\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
//endregion

    //region Jira Histogram Stack By Issue Project
    @Test
    public void testJiraHistogramStackByIssueProjectApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.histogram).calculation(VelocityFilter.CALCULATION.ticket_velocity)
                        .limitToOnlyApplicableData(true).stacks(List.of(VelocityFilter.STACK.issue_project)).valueStacks(List.of("NEW FEATURE", "BUG"))
                        .histogramStageName("Dev Time - PR Creared").histogramBucketsCount(8)
                        .build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("SELECT MIN(s2) AS min, MAX(s2) AS max, WIDTH_BUCKET(s2, 0, 2592001, 8) AS bucket, COUNT(*) AS cnt FROM (\n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND im.project IN (:value_stacks) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as histo\n" +
                "GROUP BY bucket\n" +
                "ORDER BY bucket;", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"value_stacks\":[\"NEW FEATURE\",\"BUG\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
//endregion

    //region Jira Velocity Stack Issue Epic
    @Test
    public void testJiraVelocityStackIssueEpicApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).stacks(List.of(VelocityFilter.STACK.issue_epic)).limitToOnlyApplicableData(true).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select stack,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,stack,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,stack,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,stack,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,im.epic as stack,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,stack \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "group by stack \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
//endregion

    //region WI Velocity Stack Issue Epic
    @Test
    public void testWIVelocityStackIssueEpicApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).stacks(List.of(VelocityFilter.STACK.issue_epic)).build(),
                WORK_ITEMS_TYPE_WI, JIRA_ISSUES_FILTER_NULL, WORK_ITEM_FILTER, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select stack,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,stack,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,stack,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,stack,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.workitem_id as key,im.summary as title,im.attributes ->> 'organization' as org,im.project as project,NULL as repo_id,im.workitem_created_at as estart,im.epic as stack,CASE WHEN (ims.field_type = 'status' AND ims.field_value in (:s0_value) ) THEN CASE WHEN ims.start_date IS NOT NULL THEN COALESCE(ims.end_date,now()) ELSE ims.end_date END ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.field_type = 'status' AND ims.field_value in (:s9_value) ) THEN CASE WHEN ims.start_date IS NOT NULL THEN COALESCE(ims.end_date,now()) ELSE ims.end_date END ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.issue_mgmt_workitems as im \n" +
                "               left join (select m.workitem_id, c.id, c.committed_at, c.integration_id, c.commit_sha, c.repo_id, c.committer_id, c.author_id from ${company}.scm_commit_workitem_mappings m \n" +
                "               inner join ${company}.scm_commits c on c.commit_sha = m.commit_sha and  c.integration_id = m.scm_integration_id \n" +
                "               and c.integration_id IN (:integration_ids) ) AS c on c.workitem_id = im.workitem_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_workitem_mappings as prjm on prjm.workitem_id = im.workitem_id and prjm.project = im.project \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.issue_mgmt_workitems_timeline as ims on ims.integration_id = im.integration_id AND ims.workitem_id=im.workitem_id \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,stack \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "group by stack \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
//endregion

    //region Jira Stack By Issue Epic Values
    @Test
    public void testJiraStackByIssueEpicValuesApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        Map<String, SortingOrder> sorts = velocityConfigDTO.getFixedStages().stream().map(VelocityConfigDTO.Stage::getName).limit(2)
                .map(s-> Map.entry(s, SortingOrder.DESC))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.values).calculation(VelocityFilter.CALCULATION.ticket_velocity)
                        .sort(sorts).limitToOnlyApplicableData(true).stacks(List.of(VelocityFilter.STACK.issue_epic)).valueStacks(List.of("NEW FEATURE", "BUG"))
                        .pageSize(100).page(0).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND im.epic IN (:value_stacks) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY s1 DESC NULLS LAST,s2 DESC NULLS LAST \n" +
                "   LIMIT 100 OFFSET 0  \n", sql);
        Assert.assertEquals("SELECT COUNT(*) FROM (   select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND im.epic IN (:value_stacks) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY s1 DESC NULLS LAST,s2 DESC NULLS LAST \n" +
                "    \n" +
                " ) AS counted", actual.getCountSql());
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"value_stacks\":[\"NEW FEATURE\",\"BUG\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
//endregion

    //region Jira Histogram Stack By Issue Epic
    @Test
    public void testJiraHistogramStackByIssueEpicApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.histogram).calculation(VelocityFilter.CALCULATION.ticket_velocity)
                        .limitToOnlyApplicableData(true).stacks(List.of(VelocityFilter.STACK.issue_epic)).valueStacks(List.of("NEW FEATURE", "BUG"))
                        .histogramStageName("Dev Time - PR Creared").histogramBucketsCount(8)
                        .build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("SELECT MIN(s2) AS min, MAX(s2) AS max, WIDTH_BUCKET(s2, 0, 2592001, 8) AS bucket, COUNT(*) AS cnt FROM (\n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND im.epic IN (:value_stacks) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as histo\n" +
                "GROUP BY bucket\n" +
                "ORDER BY bucket;", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"value_stacks\":[\"NEW FEATURE\",\"BUG\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
//endregion

    //region Jira Velocity Stack Issue Component
    @Test
    public void testJiraVelocityStackIssueComponentApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).stacks(List.of(VelocityFilter.STACK.issue_component)).limitToOnlyApplicableData(true).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select stack,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,stack,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,stack,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,stack,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,UNNEST(im.components) as stack,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,stack \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "group by stack \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
//endregion

    //region WI Velocity Stack Issue Component
    @Test
    public void testWIVelocityStackIssueComponentApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).stacks(List.of(VelocityFilter.STACK.issue_component)).build(),
                WORK_ITEMS_TYPE_WI, JIRA_ISSUES_FILTER_NULL, WORK_ITEM_FILTER, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select stack,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,stack,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,stack,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,stack,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.workitem_id as key,im.summary as title,im.attributes ->> 'organization' as org,im.project as project,NULL as repo_id,im.workitem_created_at as estart,UNNEST(im.components) as stack,CASE WHEN (ims.field_type = 'status' AND ims.field_value in (:s0_value) ) THEN CASE WHEN ims.start_date IS NOT NULL THEN COALESCE(ims.end_date,now()) ELSE ims.end_date END ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.field_type = 'status' AND ims.field_value in (:s9_value) ) THEN CASE WHEN ims.start_date IS NOT NULL THEN COALESCE(ims.end_date,now()) ELSE ims.end_date END ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.issue_mgmt_workitems as im \n" +
                "               left join (select m.workitem_id, c.id, c.committed_at, c.integration_id, c.commit_sha, c.repo_id, c.committer_id, c.author_id from ${company}.scm_commit_workitem_mappings m \n" +
                "               inner join ${company}.scm_commits c on c.commit_sha = m.commit_sha and  c.integration_id = m.scm_integration_id \n" +
                "               and c.integration_id IN (:integration_ids) ) AS c on c.workitem_id = im.workitem_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_workitem_mappings as prjm on prjm.workitem_id = im.workitem_id and prjm.project = im.project \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.issue_mgmt_workitems_timeline as ims on ims.integration_id = im.integration_id AND ims.workitem_id=im.workitem_id \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,stack \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "group by stack \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
//endregion

    //region Jira Stack By Issue Component Values
    @Test
    public void testJiraStackByIssueComponentValuesApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        Map<String, SortingOrder> sorts = velocityConfigDTO.getFixedStages().stream().map(VelocityConfigDTO.Stage::getName).limit(2)
                .map(s-> Map.entry(s, SortingOrder.DESC))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.values).calculation(VelocityFilter.CALCULATION.ticket_velocity)
                        .sort(sorts).limitToOnlyApplicableData(true).stacks(List.of(VelocityFilter.STACK.issue_component)).valueStacks(List.of("NEW FEATURE", "BUG"))
                        .pageSize(100).page(0).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND im.components && :value_stacks \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY s1 DESC NULLS LAST,s2 DESC NULLS LAST \n" +
                "   LIMIT 100 OFFSET 0  \n", sql);
        Assert.assertEquals("SELECT COUNT(*) FROM (   select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND im.components && :value_stacks \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY s1 DESC NULLS LAST,s2 DESC NULLS LAST \n" +
                "    \n" +
                " ) AS counted", actual.getCountSql());
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"value_stacks\":[\"NEW FEATURE\",\"BUG\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
//endregion

    //region Jira Histogram Stack By Issue Component
    @Test
    public void testJiraHistogramStackByIssueComponentApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.histogram).calculation(VelocityFilter.CALCULATION.ticket_velocity)
                        .limitToOnlyApplicableData(true).stacks(List.of(VelocityFilter.STACK.issue_component)).valueStacks(List.of("NEW FEATURE", "BUG"))
                        .histogramStageName("Dev Time - PR Creared").histogramBucketsCount(8)
                        .build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("SELECT MIN(s2) AS min, MAX(s2) AS max, WIDTH_BUCKET(s2, 0, 2592001, 8) AS bucket, COUNT(*) AS cnt FROM (\n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND im.components && :value_stacks \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as histo\n" +
                "GROUP BY bucket\n" +
                "ORDER BY bucket;", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"value_stacks\":[\"NEW FEATURE\",\"BUG\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
//endregion

    //region Jira Velocity Stack Issue Labels
    @Test
    public void testJiraVelocityStackIssueLabelsApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).stacks(List.of(VelocityFilter.STACK.issue_label)).limitToOnlyApplicableData(true).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select stack,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,stack,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,stack,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,stack,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,UNNEST(im.labels) as stack,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,stack \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "group by stack \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
//endregion

    //region WI Velocity Stack Issue Labels
    @Test
    public void testWIVelocityStackIssueLabelsApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).stacks(List.of(VelocityFilter.STACK.issue_label)).build(),
                WORK_ITEMS_TYPE_WI, JIRA_ISSUES_FILTER_NULL, WORK_ITEM_FILTER, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select stack,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,stack,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,stack,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,stack,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.workitem_id as key,im.summary as title,im.attributes ->> 'organization' as org,im.project as project,NULL as repo_id,im.workitem_created_at as estart,UNNEST(im.labels) as stack,CASE WHEN (ims.field_type = 'status' AND ims.field_value in (:s0_value) ) THEN CASE WHEN ims.start_date IS NOT NULL THEN COALESCE(ims.end_date,now()) ELSE ims.end_date END ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.field_type = 'status' AND ims.field_value in (:s9_value) ) THEN CASE WHEN ims.start_date IS NOT NULL THEN COALESCE(ims.end_date,now()) ELSE ims.end_date END ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.issue_mgmt_workitems as im \n" +
                "               left join (select m.workitem_id, c.id, c.committed_at, c.integration_id, c.commit_sha, c.repo_id, c.committer_id, c.author_id from ${company}.scm_commit_workitem_mappings m \n" +
                "               inner join ${company}.scm_commits c on c.commit_sha = m.commit_sha and  c.integration_id = m.scm_integration_id \n" +
                "               and c.integration_id IN (:integration_ids) ) AS c on c.workitem_id = im.workitem_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_workitem_mappings as prjm on prjm.workitem_id = im.workitem_id and prjm.project = im.project \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.issue_mgmt_workitems_timeline as ims on ims.integration_id = im.integration_id AND ims.workitem_id=im.workitem_id \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,stack \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "group by stack \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
//endregion

    //region Jira Stack By Issue Labels Values
    @Test
    public void testJiraStackByIssueLabelsValuesApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        Map<String, SortingOrder> sorts = velocityConfigDTO.getFixedStages().stream().map(VelocityConfigDTO.Stage::getName).limit(2)
                .map(s-> Map.entry(s, SortingOrder.DESC))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.values).calculation(VelocityFilter.CALCULATION.ticket_velocity)
                        .sort(sorts).limitToOnlyApplicableData(true).stacks(List.of(VelocityFilter.STACK.issue_label)).valueStacks(List.of("NEW FEATURE", "BUG"))
                        .pageSize(100).page(0).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND im.labels && :value_stacks \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY s1 DESC NULLS LAST,s2 DESC NULLS LAST \n" +
                "   LIMIT 100 OFFSET 0  \n", sql);
        Assert.assertEquals("SELECT COUNT(*) FROM (   select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND im.labels && :value_stacks \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY s1 DESC NULLS LAST,s2 DESC NULLS LAST \n" +
                "    \n" +
                " ) AS counted", actual.getCountSql());
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"value_stacks\":[\"NEW FEATURE\",\"BUG\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
//endregion

    //region Jira Histogram Stack By Issue Labels
    @Test
    public void testJiraHistogramStackByIssueLabelsApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.histogram).calculation(VelocityFilter.CALCULATION.ticket_velocity)
                        .limitToOnlyApplicableData(true).stacks(List.of(VelocityFilter.STACK.issue_label)).valueStacks(List.of("NEW FEATURE", "BUG"))
                        .histogramStageName("Dev Time - PR Creared").histogramBucketsCount(8)
                        .build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("SELECT MIN(s2) AS min, MAX(s2) AS max, WIDTH_BUCKET(s2, 0, 2592001, 8) AS bucket, COUNT(*) AS cnt FROM (\n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND im.labels && :value_stacks \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as histo\n" +
                "GROUP BY bucket\n" +
                "ORDER BY bucket;", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"value_stacks\":[\"NEW FEATURE\",\"BUG\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
//endregion

    //region Jira Velocity Filter Active Sprint
    @Test
    public void testJiraVelocityFilterActiveSprintsApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(realJiraConditionsBuilder, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).limitToOnlyApplicableData(true).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER_ACTIVE_SPRINT, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               INNER JOIN (select integration_id, sprint_id FROM ${company}.jira_issue_sprints  WHERE UPPER(state) IN (:sprint_states) AND integration_id IN (:jira_integration_ids) ) as sprints ON sprints.integration_id=im.integration_id AND sprints.sprint_id=ANY(im.sprint_ids) \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where (im.is_active = :is_active OR im.is_active IS NULL) AND im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "  \n" +
                "  \n", sql);
        Assert.assertEquals("{\"jira_integration_ids\":[1,2,3],\"is_active\":true,\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_value_0\":[\"main\",\"master\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s0_value\":[\"IN_PROGRESS\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"sprint_states\":[\"ACTIVE\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region Jira Velocity Trend Filter Active Sprint
    @Test
    public void testJiraVelocityTrendFilterActiveSprintsApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(realJiraConditionsBuilder, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.trend).calculation(VelocityFilter.CALCULATION.ticket_velocity).limitToOnlyApplicableData(true).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER_ACTIVE_SPRINT, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select trend,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,trend,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,trend,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,trend,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,im.ingested_at as trend,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               INNER JOIN (select integration_id, sprint_id FROM ${company}.jira_issue_sprints  WHERE UPPER(state) IN (:sprint_states) AND integration_id IN (:jira_integration_ids) ) as sprints ON sprints.integration_id=im.integration_id AND sprints.sprint_id=ANY(im.sprint_ids) \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where (im.is_active = :is_active OR im.is_active IS NULL) AND im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,trend \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "group by trend \n" +
                "order by trend \n", sql);
        Assert.assertEquals("{\"jira_integration_ids\":[1,2,3],\"is_active\":true,\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_value_0\":[\"main\",\"master\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s0_value\":[\"IN_PROGRESS\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"sprint_states\":[\"ACTIVE\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region Jira Values Filter Active Sprint
    @Test
    public void testJiraValuesFilterActiveSprintsApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        Map<String, SortingOrder> sorts = velocityConfigDTO.getFixedStages().stream().map(VelocityConfigDTO.Stage::getName).limit(2)
                .map(s-> Map.entry(s, SortingOrder.DESC))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(realJiraConditionsBuilder, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.values).calculation(VelocityFilter.CALCULATION.ticket_velocity).sort(sorts).pageSize(100).page(0).limitToOnlyApplicableData(true).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER_ACTIVE_SPRINT, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               INNER JOIN (select integration_id, sprint_id FROM ${company}.jira_issue_sprints  WHERE UPPER(state) IN (:sprint_states) AND integration_id IN (:jira_integration_ids) ) as sprints ON sprints.integration_id=im.integration_id AND sprints.sprint_id=ANY(im.sprint_ids) \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where (im.is_active = :is_active OR im.is_active IS NULL) AND im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY s1 DESC NULLS LAST,s2 DESC NULLS LAST \n" +
                "   LIMIT 100 OFFSET 0  \n", sql);
        Assert.assertEquals("SELECT COUNT(*) FROM (   select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               INNER JOIN (select integration_id, sprint_id FROM ${company}.jira_issue_sprints  WHERE UPPER(state) IN (:sprint_states) AND integration_id IN (:jira_integration_ids) ) as sprints ON sprints.integration_id=im.integration_id AND sprints.sprint_id=ANY(im.sprint_ids) \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where (im.is_active = :is_active OR im.is_active IS NULL) AND im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY s1 DESC NULLS LAST,s2 DESC NULLS LAST \n" +
                "    \n" +
                " ) AS counted", actual.getCountSql());
        Assert.assertEquals("{\"jira_integration_ids\":[1,2,3],\"is_active\":true,\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_value_0\":[\"main\",\"master\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s0_value\":[\"IN_PROGRESS\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"sprint_states\":[\"ACTIVE\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region Jira Histogram Filter Active Sprint
    @Test
    public void testJiraHistogramFilterActiveSprintsApplicableData() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(realJiraConditionsBuilder, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.histogram).calculation(VelocityFilter.CALCULATION.ticket_velocity).histogramStageName("Dev Time - PR Creared").histogramBucketsCount(5).limitToOnlyApplicableData(true).limitToOnlyApplicableData(true).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER_ACTIVE_SPRINT, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("SELECT MIN(s2) AS min, MAX(s2) AS max, WIDTH_BUCKET(s2, 0, 2592001, 5) AS bucket, COUNT(*) AS cnt FROM (\n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               INNER JOIN (select integration_id, sprint_id FROM ${company}.jira_issue_sprints  WHERE UPPER(state) IN (:sprint_states) AND integration_id IN (:jira_integration_ids) ) as sprints ON sprints.integration_id=im.integration_id AND sprints.sprint_id=ANY(im.sprint_ids) \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where (im.is_active = :is_active OR im.is_active IS NULL) AND im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND s10.end_time > to_timestamp(:s10_job_run_end_start) AND s10.end_time < to_timestamp(:s10_job_run_end_end) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as histo\n" +
                "GROUP BY bucket\n" +
                "ORDER BY bucket;", sql);
        Assert.assertEquals("{\"jira_integration_ids\":[1,2,3],\"is_active\":true,\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_value_0\":[\"main\",\"master\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s0_value\":[\"IN_PROGRESS\"],\"s10_job_run_end_start\":1592814148,\"s10_job_run_end_end\":1624350148,\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"sprint_states\":[\"ACTIVE\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //endregion

    //region Generic Event
    //region Jira Velocity
    @Test
    public void testJiraVelocityGenericEvent() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        velocityConfigDTO = velocityConfigDTO.toBuilder().startingEventIsGenericEvent(true).build();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,COALESCE(ge.event_time, to_timestamp(im.issue_created_at)) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.generic_events as ge on ge.component = 'jira' and ge.key = im.key \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
                "               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND s10.end_time > to_timestamp(:s10_job_run_end_start) AND s10.end_time < to_timestamp(:s10_job_run_end_end) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "  \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s10_job_run_end_start\":1592814148,\"s10_job_run_end_end\":1624350148,\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }

    @Test
    public void testJiraVelocityGenericEventWithLatestApprovalTime() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        velocityConfigDTO = addApprovalTimeFlag(velocityConfigDTO, "MAX");
        velocityConfigDTO = velocityConfigDTO.toBuilder().startingEventIsGenericEvent(true).build();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MAX(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,COALESCE(ge.event_time, to_timestamp(im.issue_created_at)) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.generic_events as ge on ge.component = 'jira' and ge.key = im.key \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
                "               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND s10.end_time > to_timestamp(:s10_job_run_end_start) AND s10.end_time < to_timestamp(:s10_job_run_end_end) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "  \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s10_job_run_end_start\":1592814148,\"s10_job_run_end_end\":1624350148,\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }

    @Test
    public void testJiraVelocityGenericEventAndEventType() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        velocityConfigDTO = velocityConfigDTO.toBuilder().startingEventIsGenericEvent(true).startingGenericEventTypes(List.of("incident_1", "incident_2")).build();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,COALESCE(ge.event_time, to_timestamp(im.issue_created_at)) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.generic_events as ge on ge.component = 'jira' and ge.key = im.key and ge.event_type in (:starting_generic_event_types) \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
                "               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND s10.end_time > to_timestamp(:s10_job_run_end_start) AND s10.end_time < to_timestamp(:s10_job_run_end_end) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "  \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_value_0\":[\"main\",\"master\"],\"s9_value\":[\"QA\",\"Testing\"],\"starting_generic_event_types\":[\"incident_1\",\"incident_2\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s0_value\":[\"IN_PROGRESS\"],\"s10_job_run_end_start\":1592814148,\"s10_job_run_end_end\":1624350148,\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion

    //region WI Velocity
    @Test
    public void testWIVelocityGenericEvent() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        velocityConfigDTO = velocityConfigDTO.toBuilder().startingEventIsGenericEvent(true).build();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).build(),
                WORK_ITEMS_TYPE_WI, JIRA_ISSUES_FILTER_NULL, WORK_ITEM_FILTER, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.workitem_id as key,im.summary as title,im.attributes ->> 'organization' as org,im.project as project,NULL as repo_id,COALESCE(ge.event_time, im.workitem_created_at) as estart,CASE WHEN (ims.field_type = 'status' AND ims.field_value in (:s0_value) ) THEN CASE WHEN ims.start_date IS NOT NULL THEN COALESCE(ims.end_date,now()) ELSE ims.end_date END ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.field_type = 'status' AND ims.field_value in (:s9_value) ) THEN CASE WHEN ims.start_date IS NOT NULL THEN COALESCE(ims.end_date,now()) ELSE ims.end_date END ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.issue_mgmt_workitems as im \n" +
                "               left join (select m.workitem_id, c.id, c.committed_at, c.integration_id, c.commit_sha, c.repo_id, c.committer_id, c.author_id from ${company}.scm_commit_workitem_mappings m \n" +
                "               inner join ${company}.scm_commits c on c.commit_sha = m.commit_sha and  c.integration_id = m.scm_integration_id \n" +
                "               and c.integration_id IN (:integration_ids) ) AS c on c.workitem_id = im.workitem_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.generic_events as ge on ge.component = 'work_item' and ge.key = im.workitem_id \n" +
                "               left join ${company}.scm_pullrequests_workitem_mappings as prjm on prjm.workitem_id = im.workitem_id and prjm.project = im.project \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
                "               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.issue_mgmt_workitems_timeline as ims on ims.integration_id = im.integration_id AND ims.workitem_id=im.workitem_id \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND s10.end_time > to_timestamp(:s10_job_run_end_start) AND s10.end_time < to_timestamp(:s10_job_run_end_end) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "  \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s10_job_run_end_start\":1592814148,\"s10_job_run_end_end\":1624350148,\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    @Test
    public void testWIVelocityGenericEventAndEventType() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        velocityConfigDTO = velocityConfigDTO.toBuilder().startingEventIsGenericEvent(true).startingGenericEventTypes(List.of("incident_1", "incident_2")).build();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.velocity).calculation(VelocityFilter.CALCULATION.ticket_velocity).build(),
                WORK_ITEMS_TYPE_WI, JIRA_ISSUES_FILTER_NULL, WORK_ITEM_FILTER, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("select PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.workitem_id as key,im.summary as title,im.attributes ->> 'organization' as org,im.project as project,NULL as repo_id,COALESCE(ge.event_time, im.workitem_created_at) as estart,CASE WHEN (ims.field_type = 'status' AND ims.field_value in (:s0_value) ) THEN CASE WHEN ims.start_date IS NOT NULL THEN COALESCE(ims.end_date,now()) ELSE ims.end_date END ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.field_type = 'status' AND ims.field_value in (:s9_value) ) THEN CASE WHEN ims.start_date IS NOT NULL THEN COALESCE(ims.end_date,now()) ELSE ims.end_date END ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.issue_mgmt_workitems as im \n" +
                "               left join (select m.workitem_id, c.id, c.committed_at, c.integration_id, c.commit_sha, c.repo_id, c.committer_id, c.author_id from ${company}.scm_commit_workitem_mappings m \n" +
                "               inner join ${company}.scm_commits c on c.commit_sha = m.commit_sha and  c.integration_id = m.scm_integration_id \n" +
                "               and c.integration_id IN (:integration_ids) ) AS c on c.workitem_id = im.workitem_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.generic_events as ge on ge.component = 'work_item' and ge.key = im.workitem_id and ge.event_type in (:starting_generic_event_types) \n" +
                "               left join ${company}.scm_pullrequests_workitem_mappings as prjm on prjm.workitem_id = im.workitem_id and prjm.project = im.project \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
                "               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.issue_mgmt_workitems_timeline as ims on ims.integration_id = im.integration_id AND ims.workitem_id=im.workitem_id \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND s10.end_time > to_timestamp(:s10_job_run_end_start) AND s10.end_time < to_timestamp(:s10_job_run_end_end) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "  \n" +
                "  \n", sql);
        Assert.assertEquals("{\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_value_0\":[\"main\",\"master\"],\"s9_value\":[\"QA\",\"Testing\"],\"starting_generic_event_types\":[\"incident_1\",\"incident_2\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s0_value\":[\"IN_PROGRESS\"],\"s10_job_run_end_start\":1592814148,\"s10_job_run_end_end\":1624350148,\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //endregion
    //endregion

    @Test
    public void testVelocityRequestApplicableData() throws IOException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTOWithoutCiCdJobsAndParams();
        JiraIssuesFilter jiraIssuesFilter = JiraIssuesFilter.builder().integrationIds(List.of("1", "2", "3"))
                .statuses(List.of("SELECTED FOR DEVELOPMENT")).issueResolutionRange(ImmutablePair.of(1612137600l, 1621593257L))
                .customFields(Map.of("customfield_10030", List.of("1.0"))).build();

        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.trend).calculation(VelocityFilter.CALCULATION.pr_velocity).build(),
                WORK_ITEMS_TYPE_JIRA, jiraIssuesFilter, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertEquals("select trend,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,pr_link,trend,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,pr_link,trend,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,pr_link,MIN(estart) as estart,trend,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select pr.id AS u_id,pr.integration_id as integration_id,pr.number AS key,pr.title AS title,NULL as org,pr.project as project,pr.repo_id AS repo_id,pr.metadata->>'pr_link' AS pr_link,to_timestamp(im.issue_created_at) as estart,EXTRACT(EPOCH FROM (pr.pr_created_at::date)) as trend,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.scm_pullrequests AS pr \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings AS cprm ON cprm.scm_pullrequest_id = pr.id \n" +
"               left join ${company}.scm_commits AS c ON c.id = cprm.scm_commit_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings AS prjm ON (pr.id=prjm.pr_uuid) \n" +
                "               left join ${company}.scm_commit_jira_mappings AS m ON m.commit_sha = c.commit_sha \n" +
                "               left join ( select im.* from ${company}.jira_issues AS im join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at ) as im ON (im.KEY = m.issue_key) OR (prjm.issue_key = im.key) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where pr.integration_id IN (:integration_ids) AND ( (im.integration_id IN (:jira_integration_ids)) OR (im.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,pr_link,trend \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "group by trend \n" +
                "order by trend \n", actual.getSql());
        Assert.assertEquals("SELECT COUNT(*) FROM ( select trend,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s0) as s0_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s0) as s0_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s0) as s0_p95,AVG(s0) as s0_mean,count(s0) as s0_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s1) as s1_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s1) as s1_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s1) as s1_p95,AVG(s1) as s1_mean,count(s1) as s1_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s2) as s2_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s2) as s2_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s2) as s2_p95,AVG(s2) as s2_mean,count(s2) as s2_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s3) as s3_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s3) as s3_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s3) as s3_p95,AVG(s3) as s3_mean,count(s3) as s3_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s4) as s4_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s4) as s4_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s4) as s4_p95,AVG(s4) as s4_mean,count(s4) as s4_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s5) as s5_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s5) as s5_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s5) as s5_p95,AVG(s5) as s5_mean,count(s5) as s5_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s6) as s6_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s6) as s6_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s6) as s6_p95,AVG(s6) as s6_mean,count(s6) as s6_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s7) as s7_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s7) as s7_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s7) as s7_p95,AVG(s7) as s7_mean,count(s7) as s7_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s8) as s8_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s8) as s8_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s8) as s8_p95,AVG(s8) as s8_mean,count(s8) as s8_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s9) as s9_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s9) as s9_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s9) as s9_p95,AVG(s9) as s9_mean,count(s9) as s9_count,PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s10) as s10_median,PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s10) as s10_p90,PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s10) as s10_p95,AVG(s10) as s10_mean,count(s10) as s10_count from ( \n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,pr_link,trend,greatest(s0,0) as s0,greatest(s1,0) as s1,greatest(s2,0) as s2,greatest(s3,0) as s3,greatest(s4,0) as s4,greatest(s5,0) as s5,greatest(s6,0) as s6,greatest(s7,0) as s7,greatest(s8,0) as s8,greatest(s9,0) as s9,greatest(s10,0) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,pr_link,trend,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,pr_link,MIN(estart) as estart,trend,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select pr.id AS u_id,pr.integration_id as integration_id,pr.number AS key,pr.title AS title,NULL as org,pr.project as project,pr.repo_id AS repo_id,pr.metadata->>'pr_link' AS pr_link,to_timestamp(im.issue_created_at) as estart,EXTRACT(EPOCH FROM (pr.pr_created_at::date)) as trend,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.scm_pullrequests AS pr \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings AS cprm ON cprm.scm_pullrequest_id = pr.id \n" +
"               left join ${company}.scm_commits AS c ON c.id = cprm.scm_commit_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings AS prjm ON (pr.id=prjm.pr_uuid) \n" +
                "               left join ${company}.scm_commit_jira_mappings AS m ON m.commit_sha = c.commit_sha \n" +
                "               left join ( select im.* from ${company}.jira_issues AS im join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at ) as im ON (im.KEY = m.issue_key) OR (prjm.issue_key = im.key) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where pr.integration_id IN (:integration_ids) AND ( (im.integration_id IN (:jira_integration_ids)) OR (im.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,pr_link,trend \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as median \n" +
                "group by trend \n" +
                "order by trend \n" +
                " ) AS counted", actual.getCountSql());
        Assert.assertNotNull(actual);
    }

    //region Jira Rating
    @Test
    public void testJiraRating() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.rating).calculation(VelocityFilter.CALCULATION.ticket_velocity).histogramStageName("Dev Time - PR Creared").limitToOnlyApplicableData(true).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("SELECT case WHEN s2 IS NULL OR s2 = 0 THEN 'missing' WHEN s2 > 0 AND s2 <= 863999 THEN 'good' WHEN s2 > 863999 AND s2 <= 2592001 THEN 'Needs_Attention' ELSE 'slow' END AS rating, COUNT(*) AS cnt FROM (\n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND s10.end_time > to_timestamp(:s10_job_run_end_start) AND s10.end_time < to_timestamp(:s10_job_run_end_end) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as histo\n" +
                "GROUP BY rating;", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s10_job_run_end_start\":1592814148,\"s10_job_run_end_end\":1624350148,\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }
    //end region jira rating

    @Test
    public void testPRsRating() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.rating).calculation(VelocityFilter.CALCULATION.pr_velocity).valueTrendKeys(List.of(1612137600L))
                        .histogramStageName("Dev Time - PR Creared").histogramBucketsCount(8).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER_EMPTY, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("SELECT case WHEN s2 IS NULL OR s2 = 0 THEN 'missing' WHEN s2 > 0 AND s2 <= 863999 THEN 'good' WHEN s2 > 863999 AND s2 <= 2592001 THEN 'Needs_Attention' ELSE 'slow' END AS rating, COUNT(*) AS cnt FROM (\n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,pr_link,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,pr_link,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,pr_link,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select pr.id AS u_id,pr.integration_id as integration_id,pr.number AS key,pr.title AS title,NULL as org,pr.project as project,pr.repo_id AS repo_id,pr.metadata->>'pr_link' AS pr_link,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.scm_pullrequests AS pr \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings AS cprm ON cprm.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_commits AS c ON c.id = cprm.scm_commit_id \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings AS prjm ON (pr.id=prjm.pr_uuid) \n" +
                "               left join ${company}.scm_commit_jira_mappings AS m ON m.commit_sha = c.commit_sha \n" +
                "               left join ( select im.* from ${company}.jira_issues AS im join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at ) as im ON (im.KEY = m.issue_key) OR (prjm.issue_key = im.key) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where pr.integration_id IN (:integration_ids) AND ( (im.integration_id IN (:jira_integration_ids)) OR (im.integration_id IS NULL)) AND EXTRACT(EPOCH FROM (pr.pr_created_at::date)) IN (:value_trend_keys) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id,pr_link \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "     \n" +
                "    \n" +
                ") as histo\n" +
                "GROUP BY rating;", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"value_trend_keys\":[1612137600],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }

    @Test
    public void testValuesWithRatingFilter() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.values).calculation(VelocityFilter.CALCULATION.ticket_velocity)
                        .ratings(List.of(VelocityConfigDTO.Rating.GOOD, VelocityConfigDTO.Rating.SLOW)).limitToOnlyApplicableData(true)
                        .page(0).pageSize(10).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("SELECT * FROM (\n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND s10.end_time > to_timestamp(:s10_job_run_end_start) AND s10.end_time < to_timestamp(:s10_job_run_end_end) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY t DESC NULLS LAST,u_id DESC \n" +
                ") as histo\n" +
                "where (s0 <= 863999) OR (s0 > 2592001) OR (s1 <= 863999) OR (s1 > 2592001) OR (s2 <= 863999) OR (s2 > 2592001) OR (s3 <= 863999) OR (s3 > 2592001) OR (s4 <= 863999) OR (s4 > 2592001) OR (s5 <= 863999) OR (s5 > 2592001) OR (s6 <= 863999) OR (s6 > 2592001) OR (s7 <= 863999) OR (s7 > 2592001) OR (s8 <= 863999) OR (s8 > 2592001) OR (s9 <= 863999) OR (s9 > 2592001) OR (s10 <= 863999) OR (s10 > 2592001)\n"+
                "   LIMIT 10 OFFSET 0 ", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s10_job_run_end_start\":1592814148,\"s10_job_run_end_end\":1624350148,\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }

    @Test
    public void testValuesWithRatingFilterSingleStage() throws JsonProcessingException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.values).histogramStageName("Dev Time - PR Creared").calculation(VelocityFilter.CALCULATION.ticket_velocity)
                        .ratings(List.of(VelocityConfigDTO.Rating.GOOD, VelocityConfigDTO.Rating.SLOW)).limitToOnlyApplicableData(true)
                        .page(0).pageSize(10).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("SELECT * FROM (\n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,greatest((s9 + 0) - 0, (s9 + 0) - s9) as s9,greatest((s10 + 0) - 0, (s10 + 0) - s10) as s10,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)+greatest(s9,0)+greatest(s10,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8,EXTRACT(EPOCH FROM (e9 - COALESCE(e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s9,EXTRACT(EPOCH FROM (e10 - COALESCE(e9,e8,e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s10 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MIN(e6) as e6,MAX(e7) as e7,MIN(e8) as e8,MIN(e9) as e9,MIN(e10) as e10 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,c.committed_at as e1,pr.pr_created_at as e2,s3.label_added_at as e3,s4.label_added_at as e4,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e5,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e6,pr.pr_merged_at as e7,s8.end_time AS e8,CASE WHEN (ims.status in (:s9_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e9,s10.end_time AS e10 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
"               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s4 on s4.scm_pullrequest_id = pr.id AND s4.name in (:s4_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s8_value) AND ( (jrp.name = :s8_name_0 AND jrp.value IN (:s8_value_0)) ) ) as s8 on s8.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where jr.cicd_job_id in (:s10_value) AND ( (jrp.name = :s10_name_0 AND jrp.value IN (:s10_value_0)) ) ) as s10 on s10.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND s10.end_time > to_timestamp(:s10_job_run_end_start) AND s10.end_time < to_timestamp(:s10_job_run_end_end) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY t DESC NULLS LAST,u_id DESC \n" +
                ") as histo\n" +
                "where (s2 <= 863999) OR (s2 > 2592001)\n" +
                "   LIMIT 10 OFFSET 0 ", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s10_job_run_end_start\":1592814148,\"s10_job_run_end_end\":1624350148,\"s4_value\":[\"lbl_1\",\"lbl_c\"],\"s10_name_0\":\"branch\",\"s8_name_0\":\"branch\",\"s10_value_0\":[\"main\",\"master\"],\"s8_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"],\"s8_value_0\":[\"dev\",\"staging\"],\"s9_value\":[\"QA\",\"Testing\"],\"s10_value\":[\"c67fb552-c8dd-463e-9ec9-7173e54f7ebd\",\"01743634-d322-4f05-b765-db8902d8f7a0\"]}", params);
        Assert.assertEquals("{\"0\":\"Backlog Time\",\"1\":\"Lead time to first commit\",\"2\":\"Dev Time - PR Creared\",\"3\":\"Dev Time - PR Label Added - Any\",\"4\":\"Dev Time - PR Label Added - Specific\",\"5\":\"Lead time to review\",\"6\":\"Review Time\",\"7\":\"Merge Time\",\"8\":\"Deploy to Staging\",\"9\":\"QA\",\"10\":\"Deploy to Prod\"}", MAPPER.writeValueAsString(actual.getOffsetStageMap().entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e -> e.getValue().getName()))));
    }

    @Test
    public void testCiCdWithFilters() throws BadRequestException, JsonProcessingException {
        VelocityConfigDTO velocityConfigDTO = createVelocityDTOforCICDFilters(List.of(cicdStageWithFilters()));
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.values).calculation(VelocityFilter.CALCULATION.ticket_velocity)
                        .ratings(List.of(VelocityConfigDTO.Rating.GOOD, VelocityConfigDTO.Rating.SLOW)).limitToOnlyApplicableData(true)
                        .page(0).pageSize(10).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("SELECT * FROM (\n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MAX(e6) as e6,MIN(e7) as e7 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,pr.pr_created_at as e1,s2.label_added_at as e2,s3.label_added_at as e3,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e4,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e5,pr.pr_merged_at as e6,s7.end_time AS e7 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
                "               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s2 on s2.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id AND s3.name in (:s3_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id where jr.cicd_user_id IN (:jr.cicd_user_id__7__7) AND j.project_name NOT IN (:excl_projects__7) AND jr.status IN (:job_statuses__7) ) as s7 on s7.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) AND s7.end_time > to_timestamp(:s7_job_run_end_start) AND s7.end_time < to_timestamp(:s7_job_run_end_end) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY t DESC NULLS LAST,u_id DESC \n" +
                ") as histo\n" +
                "where (s0 <= 863999) OR (s0 > 2592001) OR (s1 <= 863999) OR (s1 > 2592001) OR (s2 <= 863999) OR (s2 > 2592001) OR (s3 <= 863999) OR (s3 > 2592001) OR (s4 <= 863999) OR (s4 > 2592001) OR (s5 <= 863999) OR (s5 > 2592001) OR (s6 <= 863999) OR (s6 > 2592001) OR (s7 <= 863999) OR (s7 > 2592001)\n" +
                "   LIMIT 10 OFFSET 0 ", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s7_job_run_end_start\":1592814148,\"jr.cicd_user_id__7__7\":[\"94d0a517-4c4b-4537-aa5d-5ca315df0017\"],\"job_statuses__7\":[\"Failed\",\"Success\"],\"excl_projects__7\":[\"project1\"],\"s3_value\":[\"lbl_1\",\"lbl_c\"],\"s7_job_run_end_end\":1624350148}", params);
    }

    @Test
    public void testHarnessCIStage() throws BadRequestException, JsonProcessingException {
        VelocityConfigDTO velocityConfigDTO = createVelocityDTOforCICDFilters(List.of(harnessCIStageWithFilters()));
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.values).calculation(VelocityFilter.CALCULATION.ticket_velocity)
                        .ratings(List.of(VelocityConfigDTO.Rating.GOOD, VelocityConfigDTO.Rating.SLOW)).limitToOnlyApplicableData(true)
                        .page(0).pageSize(10).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("SELECT * FROM (\n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MAX(e6) as e6,MIN(e7) as e7 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,pr.pr_created_at as e1,s2.label_added_at as e2,s3.label_added_at as e3,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e4,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e5,pr.pr_merged_at as e6,s7.end_time AS e7 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
                "               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s2 on s2.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id AND s3.name in (:s3_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr where jr.cicd_user_id IN (:jr.cicd_user_id__7__7) AND jr.status NOT IN (:excl_job_statuses__7) AND jr.ci = :is_ci__7 AND (  metadata->'env_ids' @> ANY(ARRAY[ :7_metadatafield0_val0 ]::jsonb[]))  AND (  metadata->'service_types' @> ANY(ARRAY[ :7_metadatafield1_val0 ]::jsonb[]))  AND (  metadata->'service_ids' @> ANY(ARRAY[ :7_metadatafield2_val0 ]::jsonb[]))  AND (metadata @> :7_metadatafield3_val0::jsonb OR metadata @> :7_metadatafield3_val0_val1::jsonb) AND metadata->>'repo_url' SIMILAR TO :metadatarepo_url_begins__7 ) as s7 on s7.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY t DESC NULLS LAST,u_id DESC \n" +
                ") as histo\n" +
                "where (s0 <= 863999) OR (s0 > 2592001) OR (s1 <= 863999) OR (s1 > 2592001) OR (s2 <= 863999) OR (s2 > 2592001) OR (s3 <= 863999) OR (s3 > 2592001) OR (s4 <= 863999) OR (s4 > 2592001) OR (s5 <= 863999) OR (s5 > 2592001) OR (s6 <= 863999) OR (s6 > 2592001) OR (s7 <= 863999) OR (s7 > 2592001)\n" +
                "   LIMIT 10 OFFSET 0 ", sql);
                Assert.assertEquals("{\"metadatarepo_url_begins__7\":\"repo%\",\"s0_value\":[\"IN_PROGRESS\"],\"is_ci__7\":true,\"7_metadatafield0_val0\":[\"[\\\"env1\\\"]\",\"[\\\"env2\\\"]\"],\"7_metadatafield3_val0_val1\":\"{\\\"branch\\\":\\\"branch2\\\"}\",\"excl_job_statuses__7\":[\"Failed\"],\"jr.cicd_user_id__7__7\":[\"94d0a517-4c4b-4537-aa5d-5ca315df0017\"],\"7_metadatafield3_val0\":\"{\\\"branch\\\":\\\"branch1\\\"}\",\"7_metadatafield1_val0\":[\"[\\\"type1\\\"]\",\"[\\\"type2\\\"]\"],\"s3_value\":[\"lbl_1\",\"lbl_c\"],\"7_metadatafield2_val0\":[\"[\\\"service1\\\"]\"]}", params);
    }

    @Test
    public void testHarnessCDStage() throws BadRequestException, JsonProcessingException {
        VelocityConfigDTO velocityConfigDTO = createVelocityDTOforCICDFilters(List.of(harnessCIStageWithFilters(), harnessCDStageWithFilters()));
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.values).calculation(VelocityFilter.CALCULATION.ticket_velocity)
                        .ratings(List.of(VelocityConfigDTO.Rating.GOOD, VelocityConfigDTO.Rating.SLOW)).limitToOnlyApplicableData(true)
                        .page(0).pageSize(10).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("SELECT * FROM (\n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MAX(e6) as e6,MIN(e7) as e7,MIN(e8) as e8 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,pr.pr_created_at as e1,s2.label_added_at as e2,s3.label_added_at as e3,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e4,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e5,pr.pr_merged_at as e6,s7.end_time AS e7,s8.end_time AS e8 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
                "               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s2 on s2.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id AND s3.name in (:s3_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr where jr.cicd_user_id IN (:jr.cicd_user_id__7__7) AND jr.status NOT IN (:excl_job_statuses__7) AND jr.ci = :is_ci__7 AND (  metadata->'env_ids' @> ANY(ARRAY[ :7_metadatafield0_val0 ]::jsonb[]))  AND (  metadata->'service_types' @> ANY(ARRAY[ :7_metadatafield1_val0 ]::jsonb[]))  AND (  metadata->'service_ids' @> ANY(ARRAY[ :7_metadatafield2_val0 ]::jsonb[]))  AND (metadata @> :7_metadatafield3_val0::jsonb OR metadata @> :7_metadatafield3_val0_val1::jsonb) AND metadata->>'repo_url' SIMILAR TO :metadatarepo_url_begins__7 ) as s7 on s7.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time,jram.cicd_job_run_id1 as ci_job_run_id from ${company}.cicd_job_runs as jr join ${company}.cicd_job_run_artifact_mappings as jram on jram.cicd_job_run_id2 = jr.id where jr.cicd_user_id IN (:jr.cicd_user_id__8__8) AND jr.status NOT IN (:excl_job_statuses__8) AND jr.cd = :is_cd__8 AND (  metadata->'env_ids' @> ANY(ARRAY[ :8_metadatafield0_val0 ]::jsonb[]))  AND (  metadata->'service_types' @> ANY(ARRAY[ :8_metadatafield1_val0 ]::jsonb[]))  AND (  metadata->'service_ids' @> ANY(ARRAY[ :8_metadatafield2_val0 ]::jsonb[]))  AND (metadata @> :8_metadatafield3_val0::jsonb OR metadata @> :8_metadatafield3_val0_val1::jsonb) AND metadata->>'repo_url' SIMILAR TO :metadatarepo_url_begins__8 ) as s8 on s8.ci_job_run_id = s7.id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY t DESC NULLS LAST,u_id DESC \n" +
                ") as histo\n" +
                "where (s0 <= 863999) OR (s0 > 2592001) OR (s1 <= 863999) OR (s1 > 2592001) OR (s2 <= 863999) OR (s2 > 2592001) OR (s3 <= 863999) OR (s3 > 2592001) OR (s4 <= 863999) OR (s4 > 2592001) OR (s5 <= 863999) OR (s5 > 2592001) OR (s6 <= 863999) OR (s6 > 2592001) OR (s7 <= 863999) OR (s7 > 2592001) OR (s8 <= 863999) OR (s8 > 2592001)\n" +
                "   LIMIT 10 OFFSET 0 ", sql);
                Assert.assertEquals("{\"metadatarepo_url_begins__7\":\"repo%\",\"8_metadatafield3_val0_val1\":\"{\\\"branch\\\":\\\"cd_branch2\\\"}\",\"is_cd__8\":true,\"8_metadatafield1_val0\":[\"[\\\"cd_ype1\\\"]\",\"[\\\"cd_type2\\\"]\"],\"7_metadatafield3_val0_val1\":\"{\\\"branch\\\":\\\"branch2\\\"}\",\"excl_job_statuses__8\":[\"Failed\"],\"excl_job_statuses__7\":[\"Failed\"],\"7_metadatafield3_val0\":\"{\\\"branch\\\":\\\"branch1\\\"}\",\"7_metadatafield1_val0\":[\"[\\\"type1\\\"]\",\"[\\\"type2\\\"]\"],\"8_metadatafield3_val0\":\"{\\\"branch\\\":\\\"cd_branch1\\\"}\",\"8_metadatafield0_val0\":[\"[\\\"cd_env1\\\"]\",\"[\\\"cd_env2\\\"]\"],\"s0_value\":[\"IN_PROGRESS\"],\"is_ci__7\":true,\"7_metadatafield0_val0\":[\"[\\\"env1\\\"]\",\"[\\\"env2\\\"]\"],\"8_metadatafield2_val0\":[\"[\\\"cd_service1\\\"]\"],\"jr.cicd_user_id__8__8\":[\"94d0a517-4c4b-4537-aa5d-5ca315df0017\"],\"jr.cicd_user_id__7__7\":[\"94d0a517-4c4b-4537-aa5d-5ca315df0017\"],\"s3_value\":[\"lbl_1\",\"lbl_c\"],\"7_metadatafield2_val0\":[\"[\\\"service1\\\"]\"],\"metadatarepo_url_begins__8\":\"cd_repo%\"}", params);
    }

    @Test
    public void testGithubActionsCIHarnessCDStage() throws BadRequestException, JsonProcessingException {
        VelocityConfigDTO velocityConfigDTO = createVelocityDTOforCICDFilters(List.of(githubActionCIStageWithFilters(), harnessCDStageWithFilters()));
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.values).calculation(VelocityFilter.CALCULATION.ticket_velocity)
                        .ratings(List.of(VelocityConfigDTO.Rating.GOOD, VelocityConfigDTO.Rating.SLOW)).limitToOnlyApplicableData(true)
                        .page(0).pageSize(10).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("SELECT * FROM (\n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,greatest((s8 + 0) - 0, (s8 + 0) - s8) as s8,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)+greatest(s8,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7,EXTRACT(EPOCH FROM (e8 - COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart))) as s8 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MAX(e6) as e6,MIN(e7) as e7,MIN(e8) as e8 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,pr.pr_created_at as e1,s2.label_added_at as e2,s3.label_added_at as e3,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e4,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e5,pr.pr_merged_at as e6,s7.end_time AS e7,s8.end_time AS e8 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
                "               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s2 on s2.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id AND s3.name in (:s3_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id ) as s7 on s7.id = csm.cicd_job_run_id \n" +
                "               left join ( select jr.id,jr.end_time,jram.cicd_job_run_id1 as ci_job_run_id from ${company}.cicd_job_runs as jr join ${company}.cicd_job_run_artifact_mappings as jram on jram.cicd_job_run_id2 = jr.id where jr.cicd_user_id IN (:jr.cicd_user_id__8__8) AND jr.status NOT IN (:excl_job_statuses__8) AND jr.cd = :is_cd__8 AND (  metadata->'env_ids' @> ANY(ARRAY[ :8_metadatafield0_val0 ]::jsonb[]))  AND (  metadata->'service_types' @> ANY(ARRAY[ :8_metadatafield1_val0 ]::jsonb[]))  AND (  metadata->'service_ids' @> ANY(ARRAY[ :8_metadatafield2_val0 ]::jsonb[]))  AND (metadata @> :8_metadatafield3_val0::jsonb OR metadata @> :8_metadatafield3_val0_val1::jsonb) AND metadata->>'repo_url' SIMILAR TO :metadatarepo_url_begins__8 ) as s8 on s8.ci_job_run_id = s7.id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY t DESC NULLS LAST,u_id DESC \n" +
                ") as histo\n" +
                "where (s0 <= 863999) OR (s0 > 2592001) OR (s1 <= 863999) OR (s1 > 2592001) OR (s2 <= 863999) OR (s2 > 2592001) OR (s3 <= 863999) OR (s3 > 2592001) OR (s4 <= 863999) OR (s4 > 2592001) OR (s5 <= 863999) OR (s5 > 2592001) OR (s6 <= 863999) OR (s6 > 2592001) OR (s7 <= 863999) OR (s7 > 2592001) OR (s8 <= 863999) OR (s8 > 2592001)\n" +
                "   LIMIT 10 OFFSET 0 ", sql);
        Assert.assertEquals("{\"8_metadatafield3_val0_val1\":\"{\\\"branch\\\":\\\"cd_branch2\\\"}\",\"8_metadatafield3_val0\":\"{\\\"branch\\\":\\\"cd_branch1\\\"}\",\"8_metadatafield0_val0\":[\"[\\\"cd_env1\\\"]\",\"[\\\"cd_env2\\\"]\"],\"s0_value\":[\"IN_PROGRESS\"],\"is_cd__8\":true,\"8_metadatafield1_val0\":[\"[\\\"cd_ype1\\\"]\",\"[\\\"cd_type2\\\"]\"],\"excl_job_statuses__8\":[\"Failed\"],\"8_metadatafield2_val0\":[\"[\\\"cd_service1\\\"]\"],\"jr.cicd_user_id__8__8\":[\"94d0a517-4c4b-4537-aa5d-5ca315df0017\"],\"s3_value\":[\"lbl_1\",\"lbl_c\"],\"metadatarepo_url_begins__8\":\"cd_repo%\"}", params);
    }

    @Test
    public void testGithubActionsCIStage() throws BadRequestException, JsonProcessingException {
        VelocityConfigDTO velocityConfigDTO = createVelocityDTOforCICDFilters(List.of(githubActionCIStageWithFilters()));
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().across(VelocityFilter.DISTINCT.values).calculation(VelocityFilter.CALCULATION.ticket_velocity)
                        .ratings(List.of(VelocityConfigDTO.Rating.GOOD, VelocityConfigDTO.Rating.SLOW)).limitToOnlyApplicableData(true)
                        .page(0).pageSize(10).build(),
                WORK_ITEMS_TYPE_JIRA, JIRA_ISSUES_FILTER, WORK_ITEM_FILTER_NULL, WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        String params = MAPPER.writeValueAsString(actual.getParams());
        Assert.assertEquals("SELECT * FROM (\n" +
                "  select u_id,integration_id,key,title,org,project,repo_id,greatest((s0 + 0) - 0, (s0 + 0) - s0) as s0,greatest((s1 + 0) - 0, (s1 + 0) - s1) as s1,greatest((s2 + 0) - 0, (s2 + 0) - s2) as s2,greatest((s3 + 0) - 0, (s3 + 0) - s3) as s3,greatest((s4 + 0) - 0, (s4 + 0) - s4) as s4,greatest((s5 + 0) - 0, (s5 + 0) - s5) as s5,greatest((s6 + 0) - 0, (s6 + 0) - s6) as s6,greatest((s7 + 0) - 0, (s7 + 0) - s7) as s7,(greatest(s0,0)+greatest(s1,0)+greatest(s2,0)+greatest(s3,0)+greatest(s4,0)+greatest(s5,0)+greatest(s6,0)+greatest(s7,0)) as t from ( \n" +
                "       select u_id,integration_id,key,title,org,project,repo_id,ABS(EXTRACT(EPOCH FROM (e0 - estart))) as s0,EXTRACT(EPOCH FROM (e1 - COALESCE(e0,estart))) as s1,EXTRACT(EPOCH FROM (e2 - COALESCE(e1,e0,estart))) as s2,EXTRACT(EPOCH FROM (e3 - COALESCE(e2,e1,e0,estart))) as s3,EXTRACT(EPOCH FROM (e4 - COALESCE(e3,e2,e1,e0,estart))) as s4,EXTRACT(EPOCH FROM (e5 - COALESCE(e4,e3,e2,e1,e0,estart))) as s5,EXTRACT(EPOCH FROM (e6 - COALESCE(e5,e4,e3,e2,e1,e0,estart))) as s6,EXTRACT(EPOCH FROM (e7 - COALESCE(e6,e5,e4,e3,e2,e1,e0,estart))) as s7 from ( \n" +
                "           select u_id,integration_id,key,title,org,project,repo_id,MIN(estart) as estart,MIN(e0) as e0,MIN(e1) as e1,MIN(e2) as e2,MIN(e3) as e3,MIN(e4) as e4,MIN(e5) as e5,MAX(e6) as e6,MIN(e7) as e7 from ( \n" +
                "               select im.id as u_id,im.integration_id as integration_id,im.key as key,im.summary as title,NULL as org,NULL as project,NULL as repo_id,to_timestamp(im.issue_created_at) as estart,CASE WHEN (ims.status in (:s0_value) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e0,pr.pr_created_at as e1,s2.label_added_at as e2,s3.label_added_at as e3,case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e4,case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e5,pr.pr_merged_at as e6,s7.end_time AS e7 \n" +
                "               from ${company}.jira_issues as im \n" +
                "               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n" +
                "               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n" +
                "               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n" +
                "               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n" +
                "               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n" +
                "               left join ${company}.scm_pullrequests as pr on ((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid)) \n" +
                "               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n" +
                "               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n" +
                "               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n" +
                "               left join ${company}.scm_pullrequest_labels as s2 on s2.scm_pullrequest_id = pr.id \n" +
                "               left join ${company}.scm_pullrequest_labels as s3 on s3.scm_pullrequest_id = pr.id AND s3.name in (:s3_value) \n" +
                "               left join ( select jr.id,jr.end_time from ${company}.cicd_job_runs as jr join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id join ${company}.cicd_instances as i on i.id = j.cicd_instance_id ) as s7 on s7.id = csm.cicd_job_run_id \n" +
                " \n" +
                "               where im.integration_id IN (:jira_integration_ids) AND ( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL)) AND ( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL)) \n" +
                "           ) as base \n" +
                "           group by u_id,integration_id,key,title,org,project,repo_id \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "     \n" +
                "   ORDER BY t DESC NULLS LAST,u_id DESC \n" +
                ") as histo\n" +
                "where (s0 <= 863999) OR (s0 > 2592001) OR (s1 <= 863999) OR (s1 > 2592001) OR (s2 <= 863999) OR (s2 > 2592001) OR (s3 <= 863999) OR (s3 > 2592001) OR (s4 <= 863999) OR (s4 > 2592001) OR (s5 <= 863999) OR (s5 > 2592001) OR (s6 <= 863999) OR (s6 > 2592001) OR (s7 <= 863999) OR (s7 > 2592001)\n" +
                "   LIMIT 10 OFFSET 0 ", sql);
        Assert.assertEquals("{\"s0_value\":[\"IN_PROGRESS\"],\"s3_value\":[\"lbl_1\",\"lbl_c\"]}", params);
    }

    public VelocityConfigDTO createVelocityDTOforCICDFilters(List<VelocityConfigDTO.Stage> postDeveopmentStages){
        return VelocityConfigDTO.builder()
                .preDevelopmentCustomStages(buildPreCustomStages())
                .fixedStages(buildFixedStages(true))
                .postDevelopmentCustomStages(postDeveopmentStages)
                .build();
    }

    public VelocityConfigDTO.Stage cicdStageWithFilters(){
        return VelocityConfigDTO.Stage.builder()
                .name("CI Stage").description("stage description").order(0).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.CICD_JOB_RUN).build())
                .filter(Map.of(
                        "cicd_user_ids", List.of("94d0a517-4c4b-4537-aa5d-5ca315df0017"),
                        "job_statuses", List.of("Failed", "Success"),
                        "exclude", Map.of("projects", List.of("project1"))
                ))
                .build();
    }

    public VelocityConfigDTO.Stage harnessCIStageWithFilters(){
        return VelocityConfigDTO.Stage.builder()
                .name("HarnessCI Stage").description("stage description").order(0).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.HARNESSCI_JOB_RUN).build())
                .filter(Map.of(
                        "services", List.of("service1"),
                        "environments", List.of("env1", "env2"),
                        "partial_match", new HashMap<>(Map.of("repositories", Map.of("$begins", "repo"))),
                        "deployment_types", List.of("type1", "type2"),
                        "branches", List.of("branch1", "branch2"),
                        "cicd_user_ids", List.of("94d0a517-4c4b-4537-aa5d-5ca315df0017"),
                        "exclude", Map.of("job_statuses", List.of("Failed"))
                ))
                .build();
    }

    public VelocityConfigDTO.Stage harnessCDStageWithFilters(){
        return VelocityConfigDTO.Stage.builder()
                .name("HarnessCD Stage").description("stage description").order(1).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.HARNESSCD_JOB_RUN).build())
                .filter(Map.of(
                        "services", List.of("cd_service1"),
                        "environments", List.of("cd_env1", "cd_env2"),
                        "partial_match", new HashMap<>(Map.of("repositories", Map.of("$begins", "cd_repo"))),
                        "deployment_types", List.of("cd_ype1", "cd_type2"),
                        "branches", List.of("cd_branch1", "cd_branch2"),
                        "cicd_user_ids", List.of("94d0a517-4c4b-4537-aa5d-5ca315df0017"),
                        "exclude", Map.of("job_statuses", List.of("Failed"))
                ))
                .build();
    }

    public VelocityConfigDTO.Stage githubActionCIStageWithFilters(){
        return VelocityConfigDTO.Stage.builder()
                .name("Github Actions Stage").description("stage description").order(1).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.GITHUB_ACTIONS_JOB_RUN).build())
                .filter(Map.of())
                .build();
    }

    @Test
    public void test() {
        String s = "( custom_fields @> :customfield0val0::jsonb)";
        boolean res = s.startsWith("(");
        Assert.assertTrue(res);

        s = "integration_id IN (:jira_integration_ids)";
        res = s.startsWith("(");
        Assert.assertFalse(res);
    }

    @Test
    public void testGenerateCoalescedValues() {
        Assert.assertEquals("COALESCE(e0,estart)", VelocityAggsQueryBuilder.generateCoalescedValuesForPrevOffset(0));
        Assert.assertEquals("COALESCE(e1,e0,estart)", VelocityAggsQueryBuilder.generateCoalescedValuesForPrevOffset(1));
        Assert.assertEquals("COALESCE(e2,e1,e0,estart)", VelocityAggsQueryBuilder.generateCoalescedValuesForPrevOffset(2));
        Assert.assertEquals("COALESCE(e7,e6,e5,e4,e3,e2,e1,e0,estart)", VelocityAggsQueryBuilder.generateCoalescedValuesForPrevOffset(7));
    }

    @Test
    public void multipleWorkitemExcludeLabelFilters() throws BadRequestException {
        VelocityConfigDTO velocityConfigDTO = createVelocityConfigDTO();
        VelocityAggsQueryBuilder bldr = new VelocityAggsQueryBuilder(jiraIssueService, scmAggService, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
        VelocityAggsQueryBuilder.Query actual = bldr.buildQuery("levelops", velocityConfigDTO,
                VelocityFilter.builder().calculation(VelocityFilter.CALCULATION.ticket_velocity).across(VelocityFilter.DISTINCT.values)
                        .page(1).pageSize(100).build(),
                WORK_ITEMS_TYPE_WI, JIRA_ISSUES_FILTER, WorkItemsFilter.builder()
                        .excludeLabels(List.of("Exclude", "Regression"))
                        .build(), WORKITEMS_MILESTONE_FILTER_EMPTY, SCM_PR_FILTER, SCM_COMMIT_FILTER, CICD_JOB_RUNS_FILTER, WORKITEM_CUSTOM_FIELDS, null);
        Assert.assertNotNull(actual);
        String sql = actual.getSql();
        var excludParams = (List<String>)(actual.getParams().get("im_excl_labels"));
        Assert.assertEquals(excludParams.size(), 2);
    }
}
