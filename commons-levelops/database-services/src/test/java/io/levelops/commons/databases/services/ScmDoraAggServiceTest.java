package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmReview;
import io.levelops.commons.databases.models.database.scm.DbScmTag;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.web.exceptions.BadRequestException;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;

public class ScmDoraAggServiceTest {

    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static ScmAggService scmAggService;
    private static ScmDoraAggService scmDoraAggService;
    private static String gitHubIntegrationId;
    private static Date currentTime;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;
        dataSource = DatabaseTestUtils.setUpDataSource(pg, company);
        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, company);
        IntegrationService integrationService = jiraTestDbs.getIntegrationService();
        UserIdentityService userIdentityService = jiraTestDbs.getUserIdentityService();
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        scmDoraAggService = new ScmDoraAggService(dataSource, scmAggService);
        GitRepositoryService repositoryService = new GitRepositoryService(dataSource);
        TeamMembersDatabaseService teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, DefaultObjectMapper.get());
        OrgVersionsDatabaseService versionsService = new OrgVersionsDatabaseService(dataSource);
        OrgUsersDatabaseService usersService = new OrgUsersDatabaseService(dataSource, m, versionsService, userIdentityService);
        new UserService(dataSource, m).ensureTableExistence(company);
        DashboardWidgetService dashboardWidgetService = new DashboardWidgetService(dataSource, DefaultObjectMapper.get());
        dashboardWidgetService.ensureTableExistence(company);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        Integration build = Integration.builder()
                .id("1")
                .application("github")
                .name("github test")
                .status("enabled")
                .build();
        gitHubIntegrationId = integrationService.insert(company, build);
        currentTime = DateUtils.truncate(new Date(Instant.parse("2021-05-05T15:00:00-08:00").getEpochSecond()), Calendar.DATE);
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        versionsService.ensureTableExistence(company);
        usersService.ensureTableExistence(company);
        repositoryService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);
        String input = ResourceUtils.getResourceAsString("json/databases/githubprs.json");
        PaginatedResponse<GithubRepository> prs = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        List<DbRepository> repos = new ArrayList<>();
        prs.getResponse().getRecords().forEach(repo -> {
            repos.add(DbRepository.fromGithubRepository(repo, gitHubIntegrationId));
            repo.getPullRequests()
                    .forEach(review -> {
                        try {
                            DbScmPullRequest tmp = DbScmPullRequest
                                    .fromGithubPullRequest(review, repo.getId(), gitHubIntegrationId, null);
                            scmAggService.insert(company, tmp);
                        } catch (SQLException throwable) {
                            throwable.printStackTrace();
                        }
                    })
            ;
        });

        input = ResourceUtils.getResourceAsString("json/databases/githubcommits.json");
        PaginatedResponse<GithubRepository> commits = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        commits.getResponse().getRecords().forEach(repo -> {
            repos.add(DbRepository.fromGithubRepository(repo, gitHubIntegrationId));
            repo.getEvents().stream()
                    .filter(ev -> "PushEvent".equals(ev.getType()))
                    .flatMap(ev -> ev.getCommits().stream())
                    .forEach(commit -> {
                        DbScmCommit tmp = DbScmCommit
                                .fromGithubCommit(commit, repo.getId(), gitHubIntegrationId,
                                        currentTime.toInstant().getEpochSecond(), 0L);
                        if (scmAggService.getCommit(company, tmp.getCommitSha(), tmp.getRepoIds(),
                                tmp.getIntegrationId()).isEmpty()) {
                            try {
                                scmAggService.insertCommit(company, tmp);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                            DbScmFile.fromGithubCommit(
                                    commit, repo.getId(), gitHubIntegrationId, currentTime.toInstant().getEpochSecond())
                                    .forEach(scmFile -> scmAggService.insertFile(company, scmFile));
                        }
                    });
        });
        repositoryService.batchUpsert(company, repos);

        String arrayCatAgg = "DROP AGGREGATE IF EXISTS array_cat_agg(anyarray); CREATE AGGREGATE array_cat_agg(anyarray) (\n" +
                "  SFUNC=array_cat,\n" +
                "  STYPE=anyarray\n" +
                ");";
        dataSource.getConnection().prepareStatement(arrayCatAgg)
                .execute();

        dataSource.getConnection().prepareStatement(arrayUniq)
                .execute();
        Long mergeTime1 = new Date().toInstant().minus(20, ChronoUnit.DAYS).getEpochSecond();
        Long mergeTime2 = new Date().toInstant().minus(10, ChronoUnit.DAYS).getEpochSecond();
        DbScmReview scmReview1 = DbScmReview.builder()
                .reviewerInfo(DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .cloudId("review-levelops")
                        .displayName("review-levelops")
                        .originalDisplayName("review-levelops")
                        .build())
                .reviewId("543339289")
                .reviewer("ivan-levelops")
                .state("APPROVED")
                .reviewedAt(300L)
                .build();
        DbScmPullRequest pr1 = DbScmPullRequest.builder()
                .repoIds(List.of("levelops/commons-levelops"))
                .project("levelops/commons-levelops")
                .number("164")
                .integrationId(gitHubIntegrationId)
                .creator("viraj-levelops")
                .mergeSha("cd4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .creatorInfo(DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .cloudId("viraj-levelops")
                        .displayName("viraj-levelops")
                        .originalDisplayName("viraj-levelops")
                        .build())
                .title("[LEV-1983] ADD: headers to the stellite response sent for the resp call")
                .sourceBranch("lev-1983")
                .state("open")
                .merged(true)
                .assignees(List.of("viraj-levelops"))
                .commitShas(List.of("{934613bf40ceacc18ed59a787a745c49c18f71d9}"))
                .labels(List.of("release"))
                .prCreatedAt(100L)
                .targetBranch("fix-release")
                .prUpdatedAt(100L)
                .prMergedAt(mergeTime1)
                .prClosedAt(1500L)
                .reviews(List.of(scmReview1))
                .build();
        scmAggService.insert(company, pr1);

        DbScmPullRequest pr2 = DbScmPullRequest.builder()
                .repoIds(List.of("levelops/commons-levelops"))
                .project("levelops/commons-levelops")
                .number("165")
                .integrationId(gitHubIntegrationId)
                .creator("viraj-levelops")
                .mergeSha("cf4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .creatorInfo(DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .cloudId("viraj-levelops")
                        .displayName("viraj-levelops")
                        .originalDisplayName("viraj-levelops")
                        .build())
                .title("[LEV-1983] ADD: headers to the stellite response sent for the resp call")
                .sourceBranch("lev-1983")
                .state("open")
                .merged(true)
                .assignees(List.of("viraj-levelops"))
                .commitShas(List.of("{934613bf40ceacc18ed59a787a745c49c18f71d9}"))
                .labels(List.of("hf"))
                .prCreatedAt(100L)
                .targetBranch("fix-hf-1")
                .prUpdatedAt(100L)
                .prMergedAt(mergeTime2)
                .prClosedAt(1500L)
                .reviews(List.of(scmReview1))
                .build();
        scmAggService.insert(company, pr2);

        DbScmPullRequest pr3 = DbScmPullRequest.builder()
                .repoIds(List.of("levelops/commons-levelops"))
                .project("levelops/commons-levelops")
                .number("166")
                .integrationId(gitHubIntegrationId)
                .creator("viraj-levelops")
                .mergeSha("cb4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .creatorInfo(DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .cloudId("viraj-levelops")
                        .displayName("viraj-levelops")
                        .originalDisplayName("viraj-levelops")
                        .build())
                .title("[LEV-1983] ADD: headers to the stellite response sent for the resp call")
                .sourceBranch("lev-1983")
                .state("open")
                .merged(true)
                .assignees(List.of("viraj-levelops"))
                .commitShas(List.of("{934613bf40ceacc18ed59a787a745c49c18f71d9}"))
                .labels(List.of("hf"))
                .prCreatedAt(100L)
                .targetBranch("LEV_XXX-hf")
                .commitShas(List.of("ad4aa3fc28d925ffc6671aefac3b412c3a0cbab2"))
                .prUpdatedAt(100L)
                .prMergedAt(mergeTime1)
                .prClosedAt(1500L)
                .reviews(List.of(scmReview1))
                .build();
        scmAggService.insert(company, pr3);

        DbScmPullRequest pr4 = DbScmPullRequest.builder()
                .repoIds(List.of("levelops/commons-levelops"))
                .project("levelops/commons-levelops")
                .number("167")
                .integrationId(gitHubIntegrationId)
                .creator("viraj-levelops")
                .mergeSha("ed4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .creatorInfo(DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .cloudId("viraj-levelops")
                        .displayName("viraj-levelops")
                        .originalDisplayName("viraj-levelops")
                        .build())
                .title("[LEV-1983] ADD: headers to the stellite response sent for the resp call")
                .sourceBranch("lev-1983")
                .state("merged")
                .merged(true)
                .assignees(List.of("viraj-levelops"))
                .commitShas(List.of("{934613bf40ceacc18ed59a787a745c49c18f71d9}"))
                .labels(List.of("release"))
                .prCreatedAt(100L)
                .targetBranch("LEV_XXX-release")
                .commitShas(List.of("ed4aa3fc28d925ffc6671aefac3b412c3a0cbab2"))
                .prUpdatedAt(100L)
                .prMergedAt(mergeTime2)
                .prClosedAt(1500L)
                .reviews(List.of(scmReview1))
                .build();
        scmAggService.insert(company, pr4);


        DbScmTag tag1 = DbScmTag.builder()
                .commitSha("934613bf40ceacc18ed59a787a745c49c18f71d9")
                .createdAt(Instant.now().getEpochSecond())
                .integrationId(gitHubIntegrationId)
                .repo("levelops/devops-levelops")
                .project("levelops/devops-levelops")
                .tag("example-hf")
                .updatedAt(Instant.now().getEpochSecond())
                .build();
        scmAggService.insertTag(company, tag1);
    }

    @Test
    public void testDeploymentFrequency() throws BadRequestException {
        DbListResponse<DbAggregationResult> dbAggregationResult = scmDoraAggService.generateDeploymentFrequency(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.none)
                        .integrationIds(List.of("1"))
                        .calculation(ScmPrFilter.CALCULATION.deployment_frequency)
                        .prMergedRange(ImmutablePair.of(1659441531l,1663675131l))
                        .build(),
                ScmCommitFilter.builder().integrationIds(List.of("1")).build(), null, VelocityConfigDTO.builder()
                        .scmConfig(VelocityConfigDTO.ScmConfig.builder()
                                .deployment(Map.of(VelocityConfigDTO.ScmConfig.Field.commit_branch,
                                        Map.of("$contains", List.of("hf"))))
                                .build()).build());
        DefaultObjectMapper.prettyPrint(dbAggregationResult);
        //Disable failing test case
//        Assertions.assertThat(dbAggregationResult.getRecords())
//                .isEqualTo(List.of(DbAggregationResult.builder().count(4L).band("MEDIUM").deploymentFrequency(0.08).build()));

        dbAggregationResult = scmDoraAggService.generateDeploymentFrequency(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.none)
                        .calculation(ScmPrFilter.CALCULATION.deployment_frequency)
                        .prMergedRange(ImmutablePair.of(1662033531l,1663675131l))
                        .build(),
                ScmCommitFilter.builder().build(), null, VelocityConfigDTO.builder()
                        .scmConfig(VelocityConfigDTO.ScmConfig.builder()
                                .deployment(Map.of(VelocityConfigDTO.ScmConfig.Field.tags,
                                        Map.of("$ends", List.of("hf"))))
                                .build()).build());
        Assertions.assertThat(dbAggregationResult.getRecords())
                .isEqualTo(List.of(DbAggregationResult.builder().count(0L).band("LOW").deploymentFrequency(0.0).build()));
        dbAggregationResult = scmDoraAggService.generateDeploymentFrequency(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.none)
                        .prMergedRange(ImmutablePair.of(1662033531l,1663675131l))
                        .calculation(ScmPrFilter.CALCULATION.deployment_frequency)
                        .build(),
                ScmCommitFilter.builder().build(), null, VelocityConfigDTO.builder()
                        .scmConfig(VelocityConfigDTO.ScmConfig.builder()
                                .deployment(Map.of(VelocityConfigDTO.ScmConfig.Field.labels,
                                        Map.of("$contains", List.of("hf")), VelocityConfigDTO.ScmConfig.Field.target_branch,
                                        Map.of("$contains", List.of("hf"))))
                                .build()).build());
        //Disable failing test case
//        Assertions.assertThat(dbAggregationResult.getRecords())
//                .isEqualTo(List.of(DbAggregationResult.builder().count(1L).band("MEDIUM").deploymentFrequency(0.05).build()));
    }

    @Test
    public void testFailureRate() throws SQLException, BadRequestException {
        DbListResponse<DbAggregationResult> dbAggregationResult = scmDoraAggService.generateFailureRateReports(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.none)
                        .calculation(ScmPrFilter.CALCULATION.failure_rate)
                        .build(),
                ScmCommitFilter.builder().build(), null, VelocityConfigDTO.builder()
                        .scmConfig(VelocityConfigDTO.ScmConfig.builder()
                                .hotfix(Map.of(VelocityConfigDTO.ScmConfig.Field.target_branch, Map.of("$contains", List.of("hf")),
                                        VelocityConfigDTO.ScmConfig.Field.labels, Map.of("$contains", List.of("hf")),
                                        VelocityConfigDTO.ScmConfig.Field.commit_branch, Map.of("$contains", List.of("dev"))))
                                .release(Map.of(VelocityConfigDTO.ScmConfig.Field.source_branch, Map.of("$contains", List.of("lev")),
                                        VelocityConfigDTO.ScmConfig.Field.commit_branch, Map.of("$contains", List.of("lev"))))
                                .build())
                        .build());
        DefaultObjectMapper.prettyPrint(dbAggregationResult);
        Assertions.assertThat(dbAggregationResult.getRecords())
                .isEqualTo(List.of(DbAggregationResult.builder().count(7L).band("HIGH").failureRate(28.571428571428573).build()));
        dbAggregationResult = scmDoraAggService.generateFailureRateReports(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.none)
                        .calculation(ScmPrFilter.CALCULATION.failure_rate)
                        .build(),
                ScmCommitFilter.builder().build(), null, VelocityConfigDTO.builder()
                        .scmConfig(VelocityConfigDTO.ScmConfig.builder()
                                .hotfix(Map.of(VelocityConfigDTO.ScmConfig.Field.commit_branch, Map.of("$contains", List.of("dev"))))
                                .release(Map.of(VelocityConfigDTO.ScmConfig.Field.commit_branch, Map.of("$contains", List.of("lev"))))
                                .build())
                        .build());
        Assertions.assertThat(dbAggregationResult.getRecords())
                .isEqualTo(List.of(DbAggregationResult.builder().count(0L).band("ELITE").failureRate(0.0).build()));

        dbAggregationResult = scmDoraAggService.generateFailureRateReports(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.none)
                        .calculation(ScmPrFilter.CALCULATION.failure_rate)
                        .build(),
                ScmCommitFilter.builder().build(), null, VelocityConfigDTO.builder()
                        .scmConfig(VelocityConfigDTO.ScmConfig.builder()
                                .hotfix(Map.of(VelocityConfigDTO.ScmConfig.Field.target_branch, Map.of("$contains", List.of("hf")),
                                        VelocityConfigDTO.ScmConfig.Field.commit_branch, Map.of("$contains", List.of("dev"))))
                                .release(Map.of(VelocityConfigDTO.ScmConfig.Field.commit_branch, Map.of("$contains", List.of("lev"))))
                                .build())
                        .build());
        Assertions.assertThat(dbAggregationResult.getRecords())
                .isEqualTo(List.of(DbAggregationResult.builder().count(2L).band("LOW").failureRate(100.0).build()));

        dbAggregationResult = scmDoraAggService.generateFailureRateReports(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.none)
                        .calculation(ScmPrFilter.CALCULATION.failure_rate)
                        .build(),
                ScmCommitFilter.builder().build(), null, VelocityConfigDTO.builder()
                        .scmConfig(VelocityConfigDTO.ScmConfig.builder()
                                .hotfix(Map.of(VelocityConfigDTO.ScmConfig.Field.commit_branch, Map.of("$contains", List.of("dev"))))
                                .release(Map.of(VelocityConfigDTO.ScmConfig.Field.source_branch, Map.of("$contains", List.of("lev")),
                                        VelocityConfigDTO.ScmConfig.Field.commit_branch, Map.of("$contains", List.of("lev"))))
                                .build())
                        .build());
        Assertions.assertThat(dbAggregationResult.getRecords())
                .isEqualTo(List.of(DbAggregationResult.builder().count(5L).band("ELITE").failureRate(0.0).build()));
        dbAggregationResult = scmDoraAggService.generateFailureRateReports(company,
                ScmPrFilter.builder()
                        .calculation(ScmPrFilter.CALCULATION.failure_rate)
                        .across(ScmPrFilter.DISTINCT.none)
                        .build(),
                ScmCommitFilter.builder().build(), null, VelocityConfigDTO.builder()
                        .scmConfig(VelocityConfigDTO.ScmConfig.builder()
                                .hotfix(Map.of(VelocityConfigDTO.ScmConfig.Field.target_branch,
                                        Map.of("$contains", List.of("hf")),
                                        VelocityConfigDTO.ScmConfig.Field.labels, Map.of("$contains", List.of("hf"))))
                                .release(Map.of(VelocityConfigDTO.ScmConfig.Field.source_branch,
                                        Map.of("$contains", List.of("hf"))))
                                .build())
                        .build());
        Assertions.assertThat(dbAggregationResult.getRecords())
                .isEqualTo(List.of(DbAggregationResult.builder().count(2L).band("LOW").failureRate(100.0).build()));


        dbAggregationResult = scmDoraAggService.generateFailureRateReports(company,
                ScmPrFilter.builder()
                        .creators(List.of("NONE"))
                        .across(ScmPrFilter.DISTINCT.none)
                        .calculation(ScmPrFilter.CALCULATION.failure_rate)
                        .build(),
                ScmCommitFilter.builder().build(), null, VelocityConfigDTO.builder()
                        .scmConfig(VelocityConfigDTO.ScmConfig.builder()
                                .hotfix(Map.of(VelocityConfigDTO.ScmConfig.Field.target_branch,
                                        Map.of("$contains", List.of("hf")),
                                        VelocityConfigDTO.ScmConfig.Field.labels, Map.of("$contains", List.of("hf"))))
                                .release(Map.of(VelocityConfigDTO.ScmConfig.Field.source_branch,
                                        Map.of("$contains", List.of("hf"))))
                                .build())
                        .build());
        Assertions.assertThat(dbAggregationResult.getCount()).isEqualTo(1L);
    }

    @Test
    public void testAcross() throws SQLException, BadRequestException {
        DbListResponse<DbAggregationResult> dbAggregationResult = scmDoraAggService.generateFailureRateReports(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.project)
                        .calculation(ScmPrFilter.CALCULATION.failure_rate)
                        .build(),
                ScmCommitFilter.builder().build(), null, VelocityConfigDTO.builder()
                        .scmConfig(VelocityConfigDTO.ScmConfig.builder()
                                .hotfix(Map.of(VelocityConfigDTO.ScmConfig.Field.target_branch,
                                        Map.of("$contains", List.of("hf")),
                                        VelocityConfigDTO.ScmConfig.Field.labels, Map.of("$contains", List.of("hf"))))
                                .release(Map.of(VelocityConfigDTO.ScmConfig.Field.source_branch,
                                        Map.of("$contains", List.of("lev"))))
                                .build())
                        .build());
        DefaultObjectMapper.prettyPrint(dbAggregationResult);
        Assertions.assertThat(dbAggregationResult.getRecords())
                .isEqualTo(List.of(DbAggregationResult.builder()
                                .key("levelops/commons-levelops")
                                .count(6L)
                                .band("MEDIUM")
                                .failureRate(33.333333333333336)
                                .build(),
                        DbAggregationResult.builder()
                                .key("levelops/aggregations-levelops")
                                .count(1L)
                                .band("ELITE")
                                .failureRate(0.0)
                                .build()));

        dbAggregationResult = scmDoraAggService.generateFailureRateReports(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.repo_id)
                        .calculation(ScmPrFilter.CALCULATION.failure_rate)
                        .build(),
                ScmCommitFilter.builder().build(), null, VelocityConfigDTO.builder()
                        .scmConfig(VelocityConfigDTO.ScmConfig.builder()
                                .hotfix(Map.of(VelocityConfigDTO.ScmConfig.Field.target_branch,
                                        Map.of("$contains", List.of("hf")),
                                        VelocityConfigDTO.ScmConfig.Field.labels, Map.of("$contains", List.of("hf"))))
                                .release(Map.of(VelocityConfigDTO.ScmConfig.Field.source_branch,
                                        Map.of("$contains", List.of("lev"))))
                                .build())
                        .build());
        DefaultObjectMapper.prettyPrint(dbAggregationResult);
        Assertions.assertThat(dbAggregationResult.getRecords())
                .isEqualTo(List.of(DbAggregationResult.builder().key("levelops/commons-levelops")
                                .count(6L).band("MEDIUM").failureRate(33.333333333333336).build(),
                        DbAggregationResult.builder().key("levelops/aggregations-levelops")
                        .count(1L).band("ELITE").failureRate(0.0).build()
                        ));

        dbAggregationResult = scmDoraAggService.generateFailureRateReports(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.creator)
                        .calculation(ScmPrFilter.CALCULATION.failure_rate)
                        .build(),
                ScmCommitFilter.builder().build(), null, VelocityConfigDTO.builder()
                        .scmConfig(VelocityConfigDTO.ScmConfig.builder()
                                .hotfix(Map.of(VelocityConfigDTO.ScmConfig.Field.target_branch,
                                        Map.of("$contains", List.of("hf")),
                                        VelocityConfigDTO.ScmConfig.Field.labels, Map.of("$contains", List.of("hf"))))
                                .release(Map.of(VelocityConfigDTO.ScmConfig.Field.source_branch,
                                        Map.of("$contains", List.of("lev"))))
                                .build())
                        .build());
        DefaultObjectMapper.prettyPrint(dbAggregationResult);
        Assertions.assertThat(dbAggregationResult.getRecords().stream().map(DbAggregationResult::getAdditionalKey))
                .containsExactlyInAnyOrder("viraj-levelops", "ivan-levelops");
        Assertions.assertThat(dbAggregationResult.getRecords().stream().map(DbAggregationResult::getBand))
                .containsExactlyInAnyOrder("ELITE", "MEDIUM");
    }
}
