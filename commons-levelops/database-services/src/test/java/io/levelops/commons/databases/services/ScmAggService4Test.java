package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmReview;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.filters.VCS_TYPE;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ScmAggService4Test {

    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static DataSource dataSource;
    private static ScmAggService scmAggService;
    private static String gitHubIntegrationId;
    private static UserIdentityService userIdentityService;
    final DbScmUser testScmUser = DbScmUser.builder()
            .integrationId(gitHubIntegrationId)
            .cloudId("viraj-levelops")
            .displayName("viraj-levelops")
            .originalDisplayName("viraj-levelops")
            .build();
    private static DbScmPullRequest pr1, pr2, pr3, pr4, pr5, pr6, pr7, pr8, pr9;
    private static String pr1Id;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;

        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        IntegrationService integrationService = new IntegrationService(dataSource);
        userIdentityService = new UserIdentityService(dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        gitHubIntegrationId = integrationService.insert(company, Integration.builder()
                .application("github")
                .name("github test")
                .status("enabled")
                .build());

        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);

        DbScmReview scmReview1 = DbScmReview.builder()
                .reviewerInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("1").displayName("review-levelops").originalDisplayName("review-levelops").build())
                .reviewId("543339289").reviewer("ivan-levelops")
                .state("APPROVED").reviewedAt(300L)
                .build();

        DbScmReview scmReview2 = DbScmReview.builder()
                .reviewerInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("2").displayName("viraj-levelops").originalDisplayName("viraj-levelops").build())
                .reviewId("543339290").reviewer("viraj-levelops")
                .state("COMMENTED").reviewedAt(600L)
                .build();

        DbScmReview scmReview3 = DbScmReview.builder()
                .reviewerInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("3").displayName("meghana-levelops").originalDisplayName("meghana-levelops").build())
                .reviewId("543339291").reviewer("meghana-levelops")
                .state("COMMENTED").reviewedAt(700L)
                .build();

        DbScmReview scmReview4 = DbScmReview.builder()
                .reviewerInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("4").displayName("ivan-levelops").originalDisplayName("ivan-levelops").build())
                .reviewId("543339292").reviewer("than-levelops")
                .state("COMMENTED").reviewedAt(800L)
                .build();

        DbScmReview scmReview5 = DbScmReview.builder()
                .reviewerInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("5").displayName("viraj-levelops").originalDisplayName("viraj-levelops").build())
                .reviewId("543339293").reviewer("than-levelops")
                .state("APPROVED").reviewedAt(1000L)
                .build();
        DbScmReview scmReview6 = DbScmReview.builder()
                .reviewerInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("5").displayName("viraj-levelops").originalDisplayName("viraj-levelops").build())
                .reviewId("543339294").reviewer("meghana-levelops")
                .state("APPROVED").reviewedAt(1000L)
                .build();

        DbScmCommit commit1 = DbScmCommit.builder()
                .repoIds(List.of("levelops/commons-levelops"))
                .project("levelops/commons-levelops")
                .vcsType(VCS_TYPE.GIT)
                .integrationId(gitHubIntegrationId)
                .author("viraj-levelops")
                .authorInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("viraj-levelops").displayName("viraj-levelops").build())
                .committer("viraj-levelops")
                .committerInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("viraj-levelops").displayName("viraj-levelops").build())
                .additions(20)
                .deletions(10)
                .changes(30)
                .filesCt(3)
                .fileTypes(List.of("java"))
                .commitSha("commitsha1")
                .directMerge(false)
                .ingestedAt(100l)
                .committedAt(100l)
                .build();

        DbScmCommit commit2 = DbScmCommit.builder()
                .repoIds(List.of("levelops/commons-levelops"))
                .project("levelops/commons-levelops")
                .vcsType(VCS_TYPE.GIT)
                .integrationId(gitHubIntegrationId)
                .author("viraj-levelops")
                .authorInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("viraj-levelops").displayName("viraj-levelops").build())
                .committer("viraj-levelops")
                .committerInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("viraj-levelops").displayName("viraj-levelops").build())
                .additions(80)
                .deletions(10)
                .changes(90)
                .filesCt(8)
                .fileTypes(List.of("java"))
                .commitSha("commitsha2")
                .directMerge(false)
                .ingestedAt(100l)
                .committedAt(100l)
                .build();

        scmAggService.insert(company, commit1, List.of());
        scmAggService.insert(company, commit2, List.of());

        pr1 = DbScmPullRequest.builder()
                .repoIds(List.of("levelops/commons-levelops"))
                .project("levelops/commons-levelops")
                .number("164")
                .integrationId(gitHubIntegrationId)
                .creator("viraj-levelops").mergeSha("cd4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .creatorInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("viraj-levelops").displayName("viraj-levelops").build())
                .title("[LEV-1983] ADD: headers to the stellite response sent for the resp call").sourceBranch("lev-1983").state("open").merged(false)
                .assignees(List.of("viraj-levelops")).commitShas(List.of("934613bf40ceacc18ed59a787a745c49c18f71d9", "commitsha1")).labels(Collections.emptyList())
                .prCreatedAt(100L)
                .prUpdatedAt(100L)
                .prMergedAt(1500L)
                .prClosedAt(1500L)
                .reviews(List.of(scmReview1))
                .build();

        pr2 = DbScmPullRequest.builder() //Unapproved  assignee = commenters and approvers = null
                .repoIds(List.of("levelops/commons-levelops"))
                .project("levelops/commons-levelops")
                .number("165")
                .integrationId(gitHubIntegrationId)
                .creator("viraj-levelops").mergeSha("cd4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .creatorInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("viraj-levelops").displayName("viraj-levelops").originalDisplayName("viraj-levelops").build())
                .title("[LEV-1983] ADD: headers to the stellite response sent for the resp call").sourceBranch("lev-1983").state("open").merged(false)
                .assignees(List.of("viraj-levelops")).commitShas(List.of("934613bf40ceacc18ed59a787a745c49c18f71d9", "commitsha1")).labels(Collections.emptyList())
                .prCreatedAt(100L)
                .reviews(List.of(scmReview2))
                .prUpdatedAt(100L)
                .prMergedAt(1500L)
                .prClosedAt(1500L)
                .build();

        pr3 = DbScmPullRequest.builder() //Unapproved  creators = any(commenters) and approvers = null
                .repoIds(List.of("levelops/repo-levelops"))
                .project("levelops/repo-levelops")
                .number("166")
                .integrationId(gitHubIntegrationId)
                .creator("meghana-levelops").mergeSha("cd4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .creatorInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("meghana-levelops").displayName("meghana-levelops").originalDisplayName("meghana-levelops").build())
                .title("[LEV-1983] ADD: headers to the stellite response sent for the resp call").sourceBranch("lev-1983").state("open").merged(false)
                .assignees(List.of("viraj-levelops"))
                .assigneesInfo(List.of(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("viraj-levelops").displayName("viraj-levelops").originalDisplayName("viraj-levelops").build()))
                .commitShas(List.of("934613bf40ceacc18ed59a787a745c49c18f71d9", "commitsha1")).labels(Collections.emptyList())
                .prCreatedAt(100L)
                .reviews(List.of(scmReview3, scmReview2))
                .prUpdatedAt(100L)
                .prMergedAt(1500L)
                .prClosedAt(1500L)
                .build();

        pr4 = DbScmPullRequest.builder() //Self-approved  creators = any(approvers) and commenters = null
                .repoIds(List.of("levelops/repo-levelops"))
                .project("levelops/repo-levelops")
                .number("167")
                .integrationId(gitHubIntegrationId)
                .creator("ivan-levelops").mergeSha("cd4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .creatorInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("ivan-levelops").displayName("ivan-levelops").originalDisplayName("ivan-levelops").build())
                .title("[LEV-1983] ADD: headers to the stellite response sent for the resp call").sourceBranch("lev-1983").state("open").merged(false)
                .assignees(List.of("viraj-levelops"))
                .assigneesInfo(List.of(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("viraj-levelops").displayName("viraj-levelops").originalDisplayName("viraj-levelops").build()))
                .commitShas(List.of("934613bf40ceacc18ed59a787a745c49c18f71d9", "commitsha2")).labels(Collections.emptyList())
                .prCreatedAt(100L)
                .reviews(List.of(scmReview1))
                .prUpdatedAt(100L)
                .prMergedAt(1500L)
                .prClosedAt(1500L)
                .build();

        pr5 = DbScmPullRequest.builder() //Self-approved  creators = any(approvers) and  creators = any(commenters)
                .repoIds(List.of("levelops/repo-levelops"))
                .project("levelops/repo-levelops")
                .number("168")
                .integrationId(gitHubIntegrationId)
                .creator("ivan-levelops").mergeSha("cd4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .creatorInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("ivan-levelops").displayName("ivan-levelops").originalDisplayName("ivan-levelops").build())
                .title("[LEV-1983] ADD: headers to the stellite response sent for the resp call").sourceBranch("lev-1983").state("open").merged(false)
                .assignees(List.of("viraj-levelops"))
                .assigneesInfo(List.of(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("viraj-levelops").displayName("viraj-levelops").originalDisplayName("viraj-levelops").build()))
                .commitShas(List.of("934613bf40ceacc18ed59a787a745c49c18f71d9", "commitsha2")).labels(Collections.emptyList())
                .prCreatedAt(100L)
                .reviews(List.of(scmReview1, scmReview4))
                .prUpdatedAt(100L)
                .prMergedAt(1500L)
                .prClosedAt(1500L)
                .build();

        pr6 = DbScmPullRequest.builder() //Self-approved with review creators = any(approvers) and  assignees && commenters == 't'
                .repoIds(List.of("levelops/repo-levelops"))
                .project("levelops/repo-levelops")
                .number("169")
                .integrationId(gitHubIntegrationId)
                .creator("ivan-levelops").mergeSha("cd4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .creatorInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("ivan-levelops").displayName("ivan-levelops").originalDisplayName("ivan-levelops").build())
                .title("[LEV-1983] ADD: headers to the stellite response sent for the resp call")
                .sourceBranch("lev-1983")
                .state("open")
                .merged(false)
                .assignees(List.of("ivan-levelops", "than-levelops"))
                .assigneesInfo(List.of(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("ivan-levelops").displayName("ivan-levelops").originalDisplayName("ivan-levelops").build(),
                        DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("than-levelops").displayName("than-levelops").originalDisplayName("than-levelops").build()))
                .commitShas(List.of("934613bf40ceacc18ed59a787a745c49c18f71d9", "commitsha2"))
                .labels(Collections.emptyList())
                .prCreatedAt(100L)
                .reviews(List.of(scmReview1, scmReview4))
                .prUpdatedAt(100L)
                .prMergedAt(1500L)
                .prClosedAt(1500L)
                .build();

        pr7 = DbScmPullRequest.builder() //Assigned peer approved with review assignees = approvers and commenters is null
                .repoIds(List.of("levelops/repo-levelops"))
                .project("levelops/repo-levelops")
                .number("170")
                .integrationId(gitHubIntegrationId)
                .creator("ivan-levelops").mergeSha("cd4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .creatorInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("ivan-levelops").displayName("ivan-levelops").originalDisplayName("ivan-levelops").build())
                .title("[LEV-1983] ADD: headers to the stellite response sent for the resp call").sourceBranch("lev-1983").state("open").merged(false)
                .assignees(List.of("than-levelops"))
                .assigneesInfo(List.of(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("than-levelops").displayName("than-levelops").originalDisplayName("than-levelops").build()))
                .commitShas(List.of("934613bf40ceacc18ed59a787a745c49c18f71d9", "commitsha1", "commitsha2")).labels(Collections.emptyList())
                .prCreatedAt(100L)
                .reviews(List.of(scmReview5))
                .prUpdatedAt(100L)
                .prMergedAt(1500L)
                .prClosedAt(1500L)
                .build();

        pr8 = DbScmPullRequest.builder() //Unassigned peer approved with review
                .repoIds(List.of("levelops/repo-levelops"))
                .project("levelops/repo-levelops")
                .number("170")
                .integrationId(gitHubIntegrationId)
                .creator("ivan-levelops").mergeSha("cd4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .creatorInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("ivan-levelops").displayName("ivan-levelops").originalDisplayName("ivan-levelops").build())
                .title("[LEV-1983] ADD: headers to the stellite response sent for the resp call").sourceBranch("lev-1983").state("open").merged(false)
                .assignees(List.of("than-levelops"))
                .assigneesInfo(List.of(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("than-levelops").displayName("than-levelops").originalDisplayName("than-levelops").build()))
                .commitShas(List.of("934613bf40ceacc18ed59a787a745c49c18f71d9", "commitsha1", "commitsha2")).labels(Collections.emptyList())
                .prCreatedAt(100L)
                .reviews(List.of(scmReview6))
                .prUpdatedAt(100L)
                .prMergedAt(1500L)
                .prClosedAt(1500L)
                .build();

        pr1Id = scmAggService.insert(company, pr1);
        scmAggService.insert(company, pr2);
        scmAggService.insert(company, pr3);
        scmAggService.insert(company, pr4);
        scmAggService.insert(company, pr5);
        scmAggService.insert(company, pr6);
        scmAggService.insert(company, pr7);
        scmAggService.insert(company, pr8);
        dataSource.getConnection().prepareStatement(arrayUniq)
                .execute();
    }

    @Test
    public void testUpdateMetadata() throws SQLException {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("pr_link", "samplehttplink");
        scmAggService.updateScmMetadata(company, pr1Id, metadata);
        Optional<DbScmPullRequest> pr = scmAggService.getPr(company, pr1.getNumber(), pr1.getRepoIds(), pr1.getIntegrationId());
        Assertions.assertThat(pr).isNotEmpty();
        Assertions.assertThat(pr.get().getPrLink()).isEqualTo("samplehttplink");
    }

    @Test
    public void testCollaborationState() throws SQLException {

        DbListResponse<DbAggregationResult> resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.collab_state)
                        .codeChangeUnit("lines")
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(resultDbListResponse.getRecords()
                .stream().map(DbAggregationResult::getKey).collect(Collectors.toList())).containsExactlyInAnyOrder("unassigned-peer-approved",
                "self-approved", "unapproved", "self-approved-with-review");

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.collab_state)
                        .codeChangeUnit("lines")
                        .collabStates(List.of("unapproved"))
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords()
                .stream().map(DbAggregationResult::getKey).collect(Collectors.toList())).containsExactlyInAnyOrder("unapproved");

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.collab_state)
                        .codeChangeUnit("lines")
                        .codeChangeSizeConfig(Map.of())
                        .excludeCollabStates(List.of("unapproved"))
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(resultDbListResponse.getRecords()
                .stream().map(DbAggregationResult::getKey).collect(Collectors.toList())).containsExactlyInAnyOrder("unassigned-peer-approved",
                "self-approved", "self-approved-with-review");

        DbListResponse<DbScmPullRequest> listResponse = scmAggService.list(company, ScmPrFilter.builder()
                .codeChangeUnit("lines")
                .collabStates(List.of("unapproved"))
                .codeChangeSizeConfig(Map.of())
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(listResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(listResponse.getRecords().stream().map(DbScmPullRequest::getNumber).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("165", "166");


    }

    @Test
    public void testCollaborationStateStacks() throws SQLException {
        DbListResponse<DbAggregationResult> resultDbListResponse = scmAggService.stackedPrsGroupBy(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.repo_id)
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), List.of(ScmPrFilter.DISTINCT.collab_state), null, true);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(resultDbListResponse.getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .size()).isBetween(1, 5);
        assertThat(resultDbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).containsExactlyInAnyOrder("levelops/commons-levelops", "levelops/repo-levelops");
        assertThat(resultDbListResponse
                .getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey).collect(Collectors.toList())
        ).containsAnyOf("unassigned-peer-approved",
                "self-approved", "unapproved", "self-approved-with-review");

        resultDbListResponse = scmAggService.stackedPrsGroupBy(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.project)
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), List.of(ScmPrFilter.DISTINCT.collab_state), null, true);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(resultDbListResponse.getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .size()).isBetween(1, 5);
        assertThat(resultDbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).containsExactlyInAnyOrder("levelops/commons-levelops", "levelops/repo-levelops");
        assertThat(resultDbListResponse
                .getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey).collect(Collectors.toList())
        ).containsAnyOf("unassigned-peer-approved",
                "self-approved", "unapproved", "self-approved-with-review");

        resultDbListResponse = scmAggService.stackedPrsGroupBy(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.creator)
                        .codeChangeSizeConfig(Map.of())
                        .collabStates(List.of("unapproved"))
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), List.of(ScmPrFilter.DISTINCT.collab_state), null, true);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(resultDbListResponse.getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .size()).isEqualTo(1);
        assertThat(resultDbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getAdditionalKey)
                .collect(Collectors.toList())).containsExactlyInAnyOrder("viraj-levelops", "meghana-levelops");
        assertThat(resultDbListResponse
                .getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey).collect(Collectors.toList())
        ).containsExactlyInAnyOrder("unapproved");

        resultDbListResponse = scmAggService.stackedPrsGroupBy(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.project)
                        .calculation(ScmPrFilter.CALCULATION.author_response_time)
                        .codeChangeSizeConfig(Map.of("small", "100", "medium", "1000"))
                        .codeChangeUnit("lines")
                        .commentDensitySizeConfig(Map.of("shallow", "2000", "good", "5300"))
                        .sort(Map.of(ScmPrSorting.MEDIAN_AUTHOR_RESPONSE_TIME, SortingOrder.ASC))
                        .build(), List.of(ScmPrFilter.DISTINCT.project), null, false);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(resultDbListResponse.getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .size()).isEqualTo(1);
        assertThat(resultDbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getMedian)
                .collect(Collectors.toList())).containsExactly(0L, 500L);

        resultDbListResponse = scmAggService.stackedPrsGroupBy(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.project)
                        .calculation(ScmPrFilter.CALCULATION.author_response_time)
                        .codeChangeSizeConfig(Map.of("small", "100",  "medium", "1000"))
                        .codeChangeUnit("lines")
                        .commentDensitySizeConfig(Map.of("shallow", "2000", "good", "5300"))
                        .sort(Map.of(ScmPrSorting.MEDIAN_AUTHOR_RESPONSE_TIME, SortingOrder.DESC))
                        .build(), List.of(ScmPrFilter.DISTINCT.project), null, false);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(resultDbListResponse.getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .size()).isEqualTo(1);
        assertThat(resultDbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getMedian)
                .collect(Collectors.toList())).containsExactly(500L, 0L);

        resultDbListResponse = scmAggService.stackedPrsGroupBy(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.project)
                        .calculation(ScmPrFilter.CALCULATION.author_response_time)
                        .codeChangeSizeConfig(Map.of("small", "100","medium", "1000"))
                        .codeChangeUnit("lines")
                        .commentDensitySizeConfig(Map.of("shallow", "2000", "good", "5300"))
                        .sort(Map.of(ScmPrSorting.MEAN_AUTHOR_RESPONSE_TIME, SortingOrder.ASC))
                        .build(), List.of(ScmPrFilter.DISTINCT.project), null, false);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(resultDbListResponse.getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .size()).isEqualTo(1);
        assertThat(resultDbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getMean)
                .collect(Collectors.toList())).containsExactly(400.0d, 600.0d);

        resultDbListResponse = scmAggService.stackedPrsGroupBy(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.project)
                        .calculation(ScmPrFilter.CALCULATION.author_response_time)
                        .codeChangeSizeConfig(Map.of("small", "100", "medium", "1000"))
                        .codeChangeUnit("lines")
                        .commentDensitySizeConfig(Map.of("shallow", "2000", "good", "5300"))
                        .sort(Map.of(ScmPrSorting.MEAN_AUTHOR_RESPONSE_TIME, SortingOrder.DESC))
                        .build(), List.of(ScmPrFilter.DISTINCT.project), null, false);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(resultDbListResponse.getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .size()).isEqualTo(1);
        assertThat(resultDbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getMean)
                .collect(Collectors.toList())).containsExactly(600.0d, 400.0d);

//        TODO: Fixe branch filter
//        Branch filter not working as sorting override condition.
//        io/levelops/commons/databases/services/ScmAggService.java ::groupByAndCalculatePrs

//        resultDbListResponse = scmAggService.stackedPrsGroupBy(
//                company, ScmPrFilter.builder()
//                        .across(ScmPrFilter.DISTINCT.branch)
//                        .calculation(ScmPrFilter.CALCULATION.author_response_time)
//                        .codeChangeSizeConfig(Map.of("small", "100", "medium", "1000"))
//                        .codeChangeUnit("lines")
//                        .commentDensitySizeConfig(Map.of("shallow", "2000", "good", "5300"))
//                        .sort(Map.of(ScmPrFilter.DISTINCT.branch.toString(), SortingOrder.DESC))
//                        .build(), List.of(ScmPrFilter.DISTINCT.project), null, false);
//        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
    }

    @Test
    public void testCodeChange() throws SQLException {

        ScmPrFilter filter = ScmPrFilter.builder()
                .codeChanges(List.of("small"))
                .codeChangeSizeConfig(Map.of("small", "50", "medium", "100"))
                .codeChangeUnit("lines")
                .build();

        DbListResponse<DbScmPullRequest> res = scmAggService.list(company, filter, Map.of(), null, 0, 10);

        Assert.assertNotNull(res);
        List<DbScmPullRequest> result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(3);

        filter = ScmPrFilter.builder()
                .codeChanges(List.of("medium"))
                .codeChangeSizeConfig(Map.of("small", "50", "medium", "100"))
                .codeChangeUnit("lines")
                .build();

        res = scmAggService.list(company, filter, Map.of(), null, 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(3);

        filter = ScmPrFilter.builder()
                .codeChanges(List.of("large"))
                .codeChangeSizeConfig(Map.of("small", "50", "medium", "100"))
                .codeChangeUnit("lines")
                .build();

        res = scmAggService.list(company, filter, Map.of(), null, 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(1);

        filter = ScmPrFilter.builder()
                .excludeCodeChanges(List.of("small"))
                .codeChangeSizeConfig(Map.of("small", "50", "medium", "100"))
                .codeChangeUnit("lines")
                .build();

        res = scmAggService.list(company, filter, Map.of(), null, 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(4);

        filter = ScmPrFilter.builder()
                .codeChanges(List.of("small"))
                .codeChangeSizeConfig(Map.of("small", "5", "medium", "10"))
                .codeChangeUnit("files")
                .build();

        res = scmAggService.list(company, filter, Map.of(), null, 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(3);

        filter = ScmPrFilter.builder()
                .codeChanges(List.of("medium"))
                .codeChangeSizeConfig(Map.of("small", "5", "medium", "10"))
                .codeChangeUnit("files")
                .build();

        res = scmAggService.list(company, filter, Map.of(), null, 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(3);

        filter = ScmPrFilter.builder()
                .codeChanges(List.of("large"))
                .codeChangeSizeConfig(Map.of("small", "5", "medium", "10"))
                .codeChangeUnit("files")
                .build();

        res = scmAggService.list(company, filter, Map.of(), null, 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(1);

        filter = ScmPrFilter.builder()
                .excludeCodeChanges(List.of("small"))
                .codeChangeSizeConfig(Map.of("small", "5", "medium", "10"))
                .codeChangeUnit("files")
                .build();

        res = scmAggService.list(company, filter, Map.of(), null, 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(4);
    }

    @Test
    public void testValues() throws SQLException {
        DbListResponse<DbAggregationResult> resultDbListResponse = scmAggService.stackedPrsGroupBy(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.repo_id)
                        .codeChangeSizeConfig(Map.of("small", "100", "medium", "1000"))
                        .codeChangeUnit("lines")
                        .commentDensitySizeConfig(Map.of("shallow", "2000", "good", "5300"))
                        .sort(Map.of())
                        .build(), List.of(ScmPrFilter.DISTINCT.repo_id), null, true);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(resultDbListResponse.getRecords().get(0).getKey()).isEqualTo("levelops/repo-levelops");
        Assertions.assertThat(resultDbListResponse.getRecords().get(0).getCount()).isEqualTo(5);
        Assertions.assertThat(resultDbListResponse.getRecords().get(1).getKey()).isEqualTo("levelops/commons-levelops");
        Assertions.assertThat(resultDbListResponse.getRecords().get(1).getCount()).isEqualTo(2);

        resultDbListResponse = scmAggService.stackedPrsGroupBy(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.repo_id)
                        .codeChangeSizeConfig(Map.of("small", "100", "medium", "1000"))
                        .codeChangeUnit("lines")
                        .commentDensitySizeConfig(Map.of("shallow", "2000", "good", "5300"))
                        .sort(Map.of(ScmPrFilter.DISTINCT.repo_id.toString(), SortingOrder.DESC))
                        .build(), List.of(ScmPrFilter.DISTINCT.repo_id), null, true);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(resultDbListResponse.getRecords().get(0).getKey()).isEqualTo("levelops/repo-levelops");
        Assertions.assertThat(resultDbListResponse.getRecords().get(0).getCount()).isEqualTo(5);
        Assertions.assertThat(resultDbListResponse.getRecords().get(1).getKey()).isEqualTo("levelops/commons-levelops");
        Assertions.assertThat(resultDbListResponse.getRecords().get(1).getCount()).isEqualTo(2);

        resultDbListResponse = scmAggService.stackedPrsGroupBy(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.repo_id)
                        .codeChangeSizeConfig(Map.of("small", "100", "medium", "1000"))
                        .codeChangeUnit("lines")
                        .commentDensitySizeConfig(Map.of("shallow", "2000", "good", "5300"))
                        .sort(Map.of(ScmPrFilter.DISTINCT.repo_id.toString(), SortingOrder.ASC))
                        .build(), List.of(ScmPrFilter.DISTINCT.repo_id), null, true);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(resultDbListResponse.getRecords().get(0).getKey()).isEqualTo("levelops/commons-levelops");
        Assertions.assertThat(resultDbListResponse.getRecords().get(0).getCount()).isEqualTo(2);
        Assertions.assertThat(resultDbListResponse.getRecords().get(1).getKey()).isEqualTo("levelops/repo-levelops");
        Assertions.assertThat(resultDbListResponse.getRecords().get(1).getCount()).isEqualTo(5);
    }

//    @Test
    public void testList() throws SQLException {
        ScmPrFilter scmPrFilter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.repo_id)
                .codeChangeSizeConfig(Map.of("small", "100", "medium", "1000"))
                .codeChangeUnit("lines")
                .commentDensitySizeConfig(Map.of("shallow", "2000", "good", "5300"))
                .build();
        DbListResponse<DbScmPullRequest> resultDbListResponse = scmAggService.list(company, scmPrFilter, (Map.of("creator", SortingOrder.ASC)), null, 0, 10);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(7);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbScmPullRequest::getCreator).collect(Collectors.toList())).isSortedAccordingTo(String::compareTo);

        scmPrFilter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.repo_id)
                .codeChangeSizeConfig(Map.of("small", "100", "medium", "1000"))
                .codeChangeUnit("lines")
                .commentDensitySizeConfig(Map.of("shallow", "2000", "good", "5300"))
                .build();
        resultDbListResponse = scmAggService.list(company, scmPrFilter, (Map.of("creator", SortingOrder.DESC)), null, 0, 10);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(7);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbScmPullRequest::getCreator).collect(Collectors.toList())).isSortedAccordingTo(Comparator.reverseOrder());

        scmPrFilter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.repo_id)
                .codeChangeSizeConfig(Map.of("small", "100", "medium", "1000"))
                .codeChangeUnit("lines")
                .commentDensitySizeConfig(Map.of("shallow", "2000", "good", "5300"))
                .build();
        resultDbListResponse = scmAggService.list(company, scmPrFilter, (Map.of("pr_created_at", SortingOrder.ASC)), null, 0, 10);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(7);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbScmPullRequest::getCreatedAt).collect(Collectors.toList())).isSortedAccordingTo(Long::compare);

        scmPrFilter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.repo_id)
                .codeChangeSizeConfig(Map.of("small", "100", "medium", "1000"))
                .codeChangeUnit("lines")
                .commentDensitySizeConfig(Map.of("shallow", "2000", "good", "5300"))
                .build();
        resultDbListResponse = scmAggService.list(company, scmPrFilter, (Map.of("pr_created_at", SortingOrder.DESC)), null, 0, 10);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(7);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbScmPullRequest::getCreatedAt).collect(Collectors.toList())).isSortedAccordingTo(Comparator.reverseOrder());

        scmPrFilter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.repo_id)
                .codeChangeSizeConfig(Map.of("small", "100", "medium", "1000"))
                .codeChangeUnit("lines")
                .commentDensitySizeConfig(Map.of("shallow", "2000", "good", "5300"))
                .build();
        resultDbListResponse = scmAggService.list(company, scmPrFilter, Map.of(), null, 0, 10);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(7);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbScmPullRequest::getPrUpdatedAt).collect(Collectors.toList())).isSortedAccordingTo(Long::compare);
    }

    @Test
    public void testDrilldownAggregateCount() throws SQLException {
        DbListResponse<DbAggregationResult> resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.collab_state)
                        .codeChangeUnit("lines")
                        .codeChangeSizeConfig(Map.of())
                        .collabStates(List.of("unapproved"))
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getRecords()).isNotEmpty();
        DbListResponse<DbScmPullRequest> listResponse = scmAggService.list(company, ScmPrFilter.builder()
                .codeChangeUnit("lines")
                .collabStates(List.of("unapproved"))
                .codeChangeSizeConfig(Map.of())
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(listResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(Long.valueOf(listResponse.getTotalCount())).isEqualTo(resultDbListResponse.getRecords()
                .stream().map(DbAggregationResult::getCount).collect(Collectors.toList()).get(0));

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.collab_state)
                        .codeChangeUnit("lines")
                        .codeChangeSizeConfig(Map.of())
                        .collabStates(List.of("unassigned-peer-approved"))
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), null);
        Assertions.assertThat(resultDbListResponse.getRecords()).isNotEmpty();
        listResponse = scmAggService.list(company, ScmPrFilter.builder()
                .codeChangeUnit("lines")
                .collabStates(List.of("unassigned-peer-approved"))
                .codeChangeSizeConfig(Map.of())
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(listResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(Long.valueOf(listResponse.getTotalCount())).isEqualTo(resultDbListResponse.getRecords()
                .stream().map(DbAggregationResult::getCount).collect(Collectors.toList()).get(0));

        resultDbListResponse = scmAggService.groupByAndCalculatePrs(company, ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.collab_state)
                .codeChangeUnit("lines")
                .codeChangeSizeConfig(Map.of())
                .collabStates(List.of("self-approved-with-review"))
                .commentDensitySizeConfig(Map.of())
                .sort(Map.of())
                .build(), null);
        Assertions.assertThat(resultDbListResponse.getRecords()).isNotEmpty();
        listResponse = scmAggService.list(company, ScmPrFilter.builder()
                .codeChangeUnit("lines")
                .collabStates(List.of("self-approved-with-review"))
                .codeChangeSizeConfig(Map.of())
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(listResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(Long.valueOf(listResponse.getTotalCount())).isEqualTo(resultDbListResponse.getRecords()
                .stream().map(DbAggregationResult::getCount).collect(Collectors.toList()).get(0));

    }

    @Test
    public void testUpdatePrProject() throws SQLException {
        Optional<DbScmPullRequest> optPr = scmAggService.getPr(company, "164", "levelops/commons-levelops", gitHubIntegrationId);
        org.junit.jupiter.api.Assertions.assertTrue(optPr.isPresent());
        DbScmPullRequest dbScmPullRequest = optPr.get();
        scmAggService.updatePrProject(company, UUID.fromString(dbScmPullRequest.getId()), dbScmPullRequest.toBuilder().project("levelops").build());
        optPr = scmAggService.getPr(company, "164", "levelops/commons-levelops", gitHubIntegrationId);
        org.junit.jupiter.api.Assertions.assertTrue(optPr.isPresent());
        Assertions.assertThat(optPr.get().getProject()).isEqualTo("levelops");
    }


    @Test
    public void testUpdatePrAssignee() throws SQLException {
        Optional<DbScmPullRequest> optPr = scmAggService.getPr(company, "166", "levelops/repo-levelops", gitHubIntegrationId);
        assertTrue(optPr.isPresent());
        DbScmPullRequest dbScmPullRequest = optPr.get();
        Assertions.assertThat(dbScmPullRequest.getAssignees().size()).isEqualTo(1);
        Assertions.assertThat(dbScmPullRequest.getAssigneeIds().size()).isEqualTo(1);

        dbScmPullRequest = dbScmPullRequest.toBuilder()
                .creator("meghana-levelops")
                .creatorInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("meghana-levelops").displayName("meghana-levelops").originalDisplayName("meghana-levelops").build())
                .assigneesInfo(List.of(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("viraj-levelops").displayName("viraj-levelops").originalDisplayName("viraj-levelops").build(),
                        DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("ashish-levelops").displayName("ashish-levelops").originalDisplayName("ashish-levelops").build()))
                .build();

        scmAggService.insert(company, dbScmPullRequest);

        optPr = scmAggService.getPr(company, "166", "levelops/repo-levelops", gitHubIntegrationId);
        assertTrue(optPr.isPresent());
        dbScmPullRequest = optPr.get();
        Assertions.assertThat(dbScmPullRequest.getAssignees().size()).isEqualTo(2);
        Assertions.assertThat(dbScmPullRequest.getAssigneeIds().size()).isEqualTo(2);
    }
}
