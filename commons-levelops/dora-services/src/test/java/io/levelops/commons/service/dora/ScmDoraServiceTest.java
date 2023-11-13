package io.levelops.commons.service.dora;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmReview;
import io.levelops.commons.databases.models.database.scm.DbScmTag;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.dora.DoraResponseDTO;
import io.levelops.commons.databases.models.dora.DoraSingleStateDTO;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.GitRepositoryService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.web.exceptions.BadRequestException;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;

public class ScmDoraServiceTest {

    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static ScmAggService scmAggService;
    private static ScmDoraService scmDoraService;

    private static ScmDeployFrequencyFailureRateCalculation deploymentFrequency;
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
        scmDoraService = new ScmDoraService(dataSource, scmAggService, deploymentFrequency);

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

        input = ResourceUtils.getResourceAsString("json/dora/dora_merge_commits.json");
        PaginatedResponse<DbScmCommit> merge_commits = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, DbScmCommit.class));
        merge_commits.getResponse().getRecords().forEach(commit -> {
            try {
                scmAggService.insertCommit(company, commit);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

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
                .title("[dev] ADD: headers to the stellite response sent for the resp call")
                .sourceBranch("dev")
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
                .title("[dev] ADD: headers to the stellite response sent for the resp call")
                .sourceBranch("dev")
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
                .title("[hf] ADD: headers to the stellite response sent for the resp call")
                .sourceBranch("hf")
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
                .title("[hf] ADD: headers to the stellite response sent for the resp call")
                .sourceBranch("hf")
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
    public void testDeploymentFrequency() throws BadRequestException, IOException {

        String workFlowProfile =ResourceUtils.getResourceAsString("velocity/new_workflow_profile_scm.json");
        VelocityConfigDTO velocityConfigDTO = m.readValue(workFlowProfile, VelocityConfigDTO.class);
        Long begin = new Date().toInstant().minus(30, ChronoUnit.DAYS).getEpochSecond();
        Long end = new Date().toInstant().minus(1, ChronoUnit.DAYS).getEpochSecond();

        DoraResponseDTO doraResponseDTO = scmDoraService.calculateNewDeploymentFrequency(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.none)
                        .integrationIds(List.of("1"))
                        .calculation(ScmPrFilter.CALCULATION.deployment_frequency)
                        .prMergedRange(ImmutablePair.of(begin,end))
                        .build(),
                ScmCommitFilter.builder().integrationIds(List.of("1")).build(), null,
                velocityConfigDTO);

        DefaultObjectMapper.prettyPrint(doraResponseDTO);

        Assert.assertTrue(doraResponseDTO.getTimeSeries().getDay().size()>0);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getWeek().size()>0);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getMonth().size()>0);
        Assert.assertEquals(doraResponseDTO.getStats().getBand(), DoraSingleStateDTO.Band.MEDIUM);
    }

    @Test
    public void testFailureRate() throws SQLException, BadRequestException, IOException {

       String workFlowProfile =ResourceUtils.getResourceAsString("velocity/new_workflow_profile_scm.json");
       VelocityConfigDTO velocityConfigDTO = m.readValue(workFlowProfile, VelocityConfigDTO.class);
       Long begin = new Date().toInstant().minus(30, ChronoUnit.DAYS).getEpochSecond();
       Long end = new Date().toInstant().minus(1, ChronoUnit.DAYS).getEpochSecond();

       DoraResponseDTO doraResponseDTO = scmDoraService.calculateNewChangeFailureRate
               (company,
                       ScmPrFilter.builder()
                               .across(ScmPrFilter.DISTINCT.none)
                               .integrationIds(List.of("1"))
                               .calculation(ScmPrFilter.CALCULATION.deployment_frequency)
                               .prMergedRange(ImmutablePair.of(begin,end))
                               .build(),
                       ScmCommitFilter.builder().build(), null, velocityConfigDTO );

       DefaultObjectMapper.prettyPrint(doraResponseDTO);
       Assert.assertTrue(doraResponseDTO.getTimeSeries().getDay().size()>0);
       Assert.assertTrue(doraResponseDTO.getTimeSeries().getWeek().size()>0);
       Assert.assertTrue(doraResponseDTO.getTimeSeries().getMonth().size()>0);
       Assert.assertTrue(doraResponseDTO.getStats().getFailureRate()>45);
       Assert.assertEquals(doraResponseDTO.getStats().getBand(), DoraSingleStateDTO.Band.LOW);
   }

   @Test
   public void testFailureRateWithIsAbsoluteTrue() throws IOException, BadRequestException {

       String workFlowProfile =ResourceUtils.getResourceAsString("velocity/new_workflow_profile_scm.json");
       VelocityConfigDTO velocityConfigDTO = m.readValue(workFlowProfile, VelocityConfigDTO.class);
       velocityConfigDTO = velocityConfigDTO.toBuilder().changeFailureRate(velocityConfigDTO.getChangeFailureRate().toBuilder().isAbsoulte(true).build()).build();

       Long begin = new Date().toInstant().minus(30, ChronoUnit.DAYS).getEpochSecond();
       Long end = new Date().toInstant().minus(1, ChronoUnit.DAYS).getEpochSecond();

       DoraResponseDTO doraResponseDTO = scmDoraService.calculateNewChangeFailureRate
               (company,
                       ScmPrFilter.builder()
                               .across(ScmPrFilter.DISTINCT.none)
                               .integrationIds(List.of("1"))
                               .calculation(ScmPrFilter.CALCULATION.deployment_frequency)
                               .prMergedRange(ImmutablePair.of(begin,end))
                               .build(),
                       ScmCommitFilter.builder().build(), null, velocityConfigDTO );

       DefaultObjectMapper.prettyPrint(doraResponseDTO);
       Assert.assertTrue(doraResponseDTO.getTimeSeries().getDay().size() > 0);
       Assert.assertTrue(doraResponseDTO.getTimeSeries().getWeek().size()>0);
       Assert.assertTrue(doraResponseDTO.getTimeSeries().getMonth().size()>0);
       Assert.assertTrue(doraResponseDTO.getStats().getTotalDeployment()>0);
       Assert.assertNull(doraResponseDTO.getStats().getFailureRate());
   }

    @Test
    public void testGetDrillDownDataForDF() throws IOException, SQLException {

        String workFlowProfile = ResourceUtils.getResourceAsString("velocity/new_workflow_profile_scm.json");
        VelocityConfigDTO velocityConfigDTO = m.readValue(workFlowProfile, VelocityConfigDTO.class);

        DefaultListRequest defaultListRequest = DefaultListRequest.builder()
                .sort(List.of(Map.of("pr_merged_at", SortingOrder.DESC)))
                .page(0)
                .pageSize(10)
                .widget(scmDoraService.DEPLOYMENT_FREQUENCY)
                .build();

        Long begin = new Date().toInstant().minus(30, ChronoUnit.DAYS).getEpochSecond();
        Long end = new Date().toInstant().minus(1, ChronoUnit.DAYS).getEpochSecond();

        DbListResponse<DbScmPullRequest> scmPullRequests = scmDoraService.getDrillDownData(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.none)
                        .integrationIds(List.of("1"))
                        .calculation(ScmPrFilter.CALCULATION.deployment_frequency)
                        .prMergedRange(ImmutablePair.of(begin,end))
                        .build(),
                Map.of("pr_merged_at", SortingOrder.DESC),
                null,
                defaultListRequest.getPage(),
                defaultListRequest.getPageSize(),
                velocityConfigDTO,
                defaultListRequest);

        scmPullRequests.getRecords().forEach(DefaultObjectMapper::prettyPrint);
        Assert.assertEquals(2, (int)scmPullRequests.getCount());
        scmPullRequests.getRecords().forEach(pr -> {
            Assert.assertEquals("dev",pr.getSourceBranch());
            Assert.assertTrue(pr.getPrMergedAt() < end && pr.getPrMergedAt() > begin);
        });
    }

    @Test
    public void testGetDrillDownDataForCF() throws IOException, SQLException {

        String workFlowProfile = ResourceUtils.getResourceAsString("velocity/new_workflow_profile_scm.json");
        VelocityConfigDTO velocityConfigDTO = m.readValue(workFlowProfile, VelocityConfigDTO.class);

        DefaultListRequest defaultListRequest = DefaultListRequest.builder()
                .sort(List.of(Map.of("pr_merged_at", SortingOrder.DESC)))
                .page(0)
                .pageSize(10)
                .widget(scmDoraService.CHANGE_FAILURE_RATE)
                .build();

        Long begin = new Date().toInstant().minus(30, ChronoUnit.DAYS).getEpochSecond();
        Long end = new Date().toInstant().minus(1, ChronoUnit.DAYS).getEpochSecond();

        DbListResponse<DbScmPullRequest> scmPullRequests = scmDoraService.getDrillDownData(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.none)
                        .integrationIds(List.of("1"))
                        .calculation(ScmPrFilter.CALCULATION.failure_rate)
                        .prMergedRange(ImmutablePair.of(begin,end))
                        .build(),
                Map.of("pr_merged_at", SortingOrder.DESC),
                null,
                defaultListRequest.getPage(),
                defaultListRequest.getPageSize(),
                velocityConfigDTO,
                defaultListRequest);

        scmPullRequests.getRecords().forEach(DefaultObjectMapper::prettyPrint);
        Assert.assertEquals(3, (int)scmPullRequests.getCount());
        scmPullRequests.getRecords().forEach(pr -> {
            Assert.assertTrue(pr.getSourceBranch().equals("hf") || pr.getSourceBranch().equals("dev"));
            Assert.assertTrue(pr.getPrMergedAt() < end && pr.getPrMergedAt() > begin);
        });
    }

    @Test
    public void testGetScmCommitDrillDownDataForCFR() throws BadRequestException {
        DefaultListRequest originalRequest = DefaultListRequest.builder()
                .filter(new HashMap<>(Map.of("time_range", Map.of("$gt", "1664582400", "$lt", "1669852800"), "across", "velocity")))
                .page(0)
                .pageSize(10)
                .ouIds(Set.of(32110))
                .widget(scmDoraService.CHANGE_FAILURE_RATE)
                .widgetId("e3a13490-967b-11ed-b20d-ed5e0e447b12")
                .build();

        VelocityConfigDTO velocityConfigDTOForSCM = VelocityConfigDTO.builder()
                .associatedOURefIds(List.of("32110"))
                .changeFailureRate(VelocityConfigDTO.ChangeFailureRate.builder()
                        .integrationId(1)
                        .velocityConfigFilters(VelocityConfigDTO.VelocityConfigFilters.builder()
                                .failedDeployment(VelocityConfigDTO.FilterTypes.builder()
                                        .scmFilters(new HashMap<>(Map.of("commit_branch", Map.of("$begins", List.of("hotfix")))))
                                        .integrationType("SCM")
                                        .build())
                                .build())
                        .build())
                .deploymentFrequency(VelocityConfigDTO.DeploymentFrequency.builder().integrationId(1).build())
                .build();

        Map<String, SortingOrder> sorting = SortingConverter.fromFilter(MoreObjects.firstNonNull(originalRequest.getSort(), List.of()));
        ScmCommitFilter scmCommitFilter = ScmCommitFilter.fromDefaultListRequest(originalRequest, null, null, Map.of());

        DbListResponse<DbScmCommit> scmCommits = scmDoraService.getScmCommitDrillDownData(company,
                scmCommitFilter,
                null,
                velocityConfigDTOForSCM.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getScmFilters(),
                originalRequest.getPage(),
                originalRequest.getPageSize(),
                sorting);

        scmCommits.getRecords().forEach(DefaultObjectMapper::prettyPrint);
        Assert.assertEquals(6, (int) scmCommits.getCount());
        scmCommits.getRecords().forEach(commit -> {
            Assert.assertEquals("hotfix", commit.getBranch());
            Assert.assertTrue((commit.getCommitPushedAt() > 1664582400L) && (commit.getCommitPushedAt() < 1669852800L));
        });
    }

    @Test
    public void testGetScmCommitDrillDownDataForDF() throws BadRequestException {
        DefaultListRequest originalRequest = DefaultListRequest.builder()
                .filter(new HashMap<>(Map.of("time_range", Map.of("$gt", "1664582400", "$lt", "1669852800"), "across", "velocity")))
                .page(0)
                .pageSize(10)
                .ouIds(Set.of(32110))
                .widget(scmDoraService.DEPLOYMENT_FREQUENCY)
                .widgetId("e3a13490-967b-11ed-b20d-ed5e0e447b13")
                .build();

        VelocityConfigDTO velocityConfigDTOForSCM = VelocityConfigDTO.builder()
                .associatedOURefIds(List.of("32110"))
                .changeFailureRate(VelocityConfigDTO.ChangeFailureRate.builder().integrationId(1).build())
                .deploymentFrequency(VelocityConfigDTO.DeploymentFrequency.builder()
                        .integrationId(1)
                        .velocityConfigFilters(VelocityConfigDTO.VelocityConfigFilters.builder()
                                .deploymentFrequency(VelocityConfigDTO.FilterTypes.builder()
                                        .scmFilters(new HashMap<>(Map.of("commit_branch", Map.of("$begins", List.of("main")))))
                                        .integrationType("SCM")
                                        .build())
                                .build())
                        .build())
                .build();

        Map<String, SortingOrder> sorting = SortingConverter.fromFilter(MoreObjects.firstNonNull(originalRequest.getSort(), List.of()));
        ScmCommitFilter scmCommitFilter = ScmCommitFilter.fromDefaultListRequest(originalRequest, null, null, Map.of());

        DbListResponse<DbScmCommit> scmCommits = scmDoraService.getScmCommitDrillDownData(company,
                scmCommitFilter,
                null,
                velocityConfigDTOForSCM.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getScmFilters(),
                originalRequest.getPage(),
                originalRequest.getPageSize(),
                sorting);

        scmCommits.getRecords().forEach(DefaultObjectMapper::prettyPrint);
        Assert.assertEquals(3, (int) scmCommits.getCount());
        scmCommits.getRecords().forEach(commit -> {
            Assert.assertEquals("main", commit.getBranch());
            Assert.assertTrue((commit.getCommitPushedAt() > 1664582400L) && (commit.getCommitPushedAt() < 1669852800L));
        });
    }
}
