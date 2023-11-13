package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmContributorAgg;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.RepoConfigEntryMatcher;
import io.levelops.commons.databases.models.database.scm.converters.bitbucket.BitbucketCommitConverters;
import io.levelops.commons.databases.models.database.scm.converters.bitbucket.BitbucketPullRequestConverters;
import io.levelops.commons.databases.models.database.scm.converters.gerrit.GerritCommitConverters;
import io.levelops.commons.databases.models.database.scm.converters.gerrit.GerritFileConverters;
import io.levelops.commons.databases.models.database.scm.converters.gerrit.GerritPullRequestConverters;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmContributorsFilter;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.bitbucket.models.BitbucketRepository;
import io.levelops.integrations.gerrit.models.ProjectInfo;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.gitlab.models.GitlabProject;
import io.levelops.integrations.helix_swarm.models.HelixSwarmReview;
import io.levelops.integrations.helixcore.models.HelixCoreChangeList;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;

public class ScmAggServiceUserTest {
    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    private static final RepoConfigEntryMatcher repoConfigEntryMatcher = new RepoConfigEntryMatcher(List.of(
            IntegrationConfig.RepoConfigEntry.builder().repoId("DummyProject").pathPrefix("//DummyProject/main").build(),
            IntegrationConfig.RepoConfigEntry.builder().repoId("sandbox").pathPrefix("//sandbox/main").build(),
            IntegrationConfig.RepoConfigEntry.builder().repoId("JamCode").pathPrefix("//JamCode/main").build()
    ));
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static ScmAggService scmAggService;
    private static UserIdentityService userIdentityService;
    private static String gitHubIntegrationId;
    private static String bitbucketIntegrationId;
    private static String gerritIntegrationId;
    private static String helixIntegrationId;
    private static String gitlabIntegrationId;
    private static Date currentTime;
    private static TeamMembersDatabaseService teamMembersDatabaseService;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;

        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        IntegrationService integrationService = new IntegrationService(dataSource);
        userIdentityService = new UserIdentityService(dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, m);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        gitHubIntegrationId = integrationService.insert(company, Integration.builder()
                .application("github")
                .name("github test")
                .status("enabled")
                .build());
        bitbucketIntegrationId = integrationService.insert(company, Integration.builder()
                .application("bitbucket")
                .name("bitbucket test")
                .status("enabled")
                .build());
        gerritIntegrationId = integrationService.insert(company, Integration.builder()
                .application("gerrit")
                .name("gerrit test")
                .status("enabled")
                .build());
        helixIntegrationId = integrationService.insert(company, Integration.builder()
                .application("helix")
                .name("helix test")
                .status("enabled")
                .build());
        gitlabIntegrationId = integrationService.insert(company, Integration.builder()
                .application("gitlab")
                .name("gitlab test")
                .status("enabled")
                .build());
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);
        currentTime = DateUtils.truncate(new Date(), Calendar.DATE);

        String input = ResourceUtils.getResourceAsString("json/databases/githubprs.json");
        PaginatedResponse<GithubRepository> prs = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        prs.getResponse().getRecords().forEach(repo -> repo.getPullRequests()
                .forEach(review -> {
                    try {
                        DbScmPullRequest tmp = DbScmPullRequest
                                .fromGithubPullRequest(review, repo.getId(), gitHubIntegrationId, null);
                        scmAggService.insert(company, tmp);
                    } catch (SQLException throwable) {
                        throwable.printStackTrace();
                    }
                }));

        input = ResourceUtils.getResourceAsString("json/databases/githubcommits.json");
        PaginatedResponse<GithubRepository> commits = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        commits.getResponse().getRecords().forEach(repo -> repo.getEvents().stream()
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
                }));

        input = ResourceUtils.getResourceAsString("json/databases/bitbucket_prs_users.json");
        PaginatedResponse<BitbucketRepository> bitbucketPRs = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, BitbucketRepository.class));
        bitbucketPRs.getResponse().getRecords().forEach(repo -> repo.getPullRequests()
                .forEach(pr -> {
                    DbScmPullRequest bitbucketPr = BitbucketPullRequestConverters.fromBitbucketPullRequest(
                            pr, repo.getFullName(), repo.getProject().getName(), bitbucketIntegrationId, (pr.getMergeCommit() != null) ? pr.getMergeCommit().getHash(): null, null);
                    try {
                        DbScmPullRequest tmpPr = scmAggService.getPr(
                                company, bitbucketPr.getNumber(), repo.getUuid(), bitbucketIntegrationId)
                                .orElse(null);
                        if (tmpPr == null || tmpPr.getPrUpdatedAt() < bitbucketPr.getPrUpdatedAt())
                            scmAggService.insert(company, bitbucketPr);
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                }));

        input = ResourceUtils.getResourceAsString("json/databases/bitbucket_commits_users.json");
        PaginatedResponse<BitbucketRepository> bitbucketCommits = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, BitbucketRepository.class));
        bitbucketCommits.getResponse().getRecords().forEach(repo -> repo.getCommits()
                .forEach(commit -> {
                    Long eventTime = commit.getDate().getTime();
                    DbScmCommit gitCommit = BitbucketCommitConverters.fromBitbucketCommit(
                            commit, repo.getFullName(), repo.getProject().getName(), bitbucketIntegrationId, eventTime);
                    if (scmAggService.getCommit(
                            company, gitCommit.getCommitSha(), repo.getFullName(), bitbucketIntegrationId)
                            .isEmpty()) {
                        try {
                            scmAggService.insertCommit(company, gitCommit);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        DbScmFile.fromBitbucketCommit(
                                commit, repo.getFullName(), bitbucketIntegrationId, eventTime)
                                .forEach(scmFile -> scmAggService.insertFile(company, scmFile));
                    }
                }));

        input = ResourceUtils.getResourceAsString("json/databases/gerrit_prs_users.json");
        PaginatedResponse<ProjectInfo> gerrit = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, ProjectInfo.class));
        gerrit.getResponse().getRecords().forEach(projectInfo -> projectInfo.getChanges()
                .forEach(change -> {
                    DbScmPullRequest gitPr = GerritPullRequestConverters.parsePullRequest(gerritIntegrationId, change);
                    try {
                        DbScmPullRequest tmpPr = scmAggService.getPr(
                                company, gitPr.getNumber(), change.getProject(), gerritIntegrationId)
                                .orElse(null);
                        if (tmpPr == null || tmpPr.getPrUpdatedAt() < gitPr.getPrUpdatedAt())
                            scmAggService.insert(company, gitPr);
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                }));

        gerrit.getResponse().getRecords().forEach(projectInfo -> projectInfo.getChanges()
                .forEach(changeInfo -> changeInfo.getRevisions().forEach((key, value) -> {
                    DbScmCommit gitCommit = GerritCommitConverters.parseCommitByRevisionId(gerritIntegrationId, changeInfo, key);
                    if (scmAggService.getCommit(
                            company, gitCommit.getCommitSha(), changeInfo.getProject(), gerritIntegrationId)
                            .isEmpty()) {
                        try {
                            scmAggService.insertCommit(company, gitCommit);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        GerritFileConverters.parseCommitFiles(gerritIntegrationId, changeInfo, key)
                                .forEach(scmFile -> scmAggService.insertFile(company, scmFile));
                    }
                })));

        Date ingestedAt = DateUtils.truncate(new Date(), Calendar.DATE);
        input = ResourceUtils.getResourceAsString("json/databases/helix_reviews_users.json");
        PaginatedResponse<HelixSwarmReview> helixReviews = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, HelixSwarmReview.class));
        helixReviews.getResponse().getRecords().forEach(review -> {
            Set<String> repoIdsSet = review.getVersions().stream()
                    .map(version -> repoConfigEntryMatcher.matchPrefix(version.getStream()))
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet());
            DbScmPullRequest dbScmPullRequest = DbScmPullRequest.fromHelixSwarmReview(review, repoIdsSet, helixIntegrationId);
            try {
                scmAggService.insert(company, dbScmPullRequest);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            List<String> repoIds = dbScmPullRequest.getRepoIds();
            ListUtils.emptyIfNull(review.getVersions()).stream()
                    .map(change -> DbScmCommit.fromHelixSwarmVersion(change, repoIds, helixIntegrationId, ingestedAt))
                    .forEach(dbScmCommit -> {
                        try {
                            scmAggService.insertCommit(company, dbScmCommit);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    });
        });

        List<IntegrationConfig.RepoConfigEntry> configEntries = List.of(IntegrationConfig.RepoConfigEntry.builder()
                        .repoId("dummy").pathPrefix("//DummyProjec").build(),
                IntegrationConfig.RepoConfigEntry.builder()
                        .repoId("sandbox").pathPrefix("//sandbox").build(),
                IntegrationConfig.RepoConfigEntry.builder()
                        .repoId("jam").pathPrefix("//JamCode").build());

        input = ResourceUtils.getResourceAsString("json/databases/helix_changelist.json");
        PaginatedResponse<HelixCoreChangeList> helixChangelist = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, HelixCoreChangeList.class));
        helixChangelist.getResponse().getRecords().forEach(changeList -> {
            List<DbScmFile> dbScmFiles = DbScmFile.fromHelixCoreChangeList(changeList, helixIntegrationId, configEntries);
            Set<String> repoIds = dbScmFiles.stream().map(DbScmFile::getRepoId).collect(Collectors.toSet());
            DbScmCommit helixCoreCommit = DbScmCommit.fromHelixCoreChangeList(changeList, repoIds, helixIntegrationId);
            if (CollectionUtils.isNotEmpty(repoIds) && scmAggService.getCommit(company, helixCoreCommit.getCommitSha(),
                    helixCoreCommit.getRepoIds(), helixIntegrationId).isEmpty() && dbScmFiles.size() > 0) {
                try {
                    scmAggService.insertCommit(company, helixCoreCommit);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                dbScmFiles.forEach(dbScmFile -> scmAggService.insertFile(company, dbScmFile));
            }
        });

        input = ResourceUtils.getResourceAsString("json/databases/gitlab_prs_users.json");
        PaginatedResponse<GitlabProject> gitlabPRs = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GitlabProject.class));
        gitlabPRs.getResponse().getRecords().forEach(project -> project.getMergeRequests()
                .forEach(mergeRequest -> {
                    DbScmPullRequest gitlabMergeRequest = DbScmPullRequest.fromGitlabMergeRequest(
                            mergeRequest, project.getPathWithNamespace(), gitlabIntegrationId,null);
                    try {
                        if (scmAggService.getPr(company, gitlabMergeRequest.getId(),
                                project.getId(), gitlabIntegrationId)
                                .isEmpty()) {
                            scmAggService.insert(company, gitlabMergeRequest);
                        }
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                }));

        input = ResourceUtils.getResourceAsString("json/databases/gitlab_commits_users.json");
        PaginatedResponse<GitlabProject> gitlabCommits = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GitlabProject.class));
        gitlabCommits.getResponse().getRecords().forEach(project -> project.getCommits()
                .forEach(commit -> {
                    DbScmCommit gitlabCommit = DbScmCommit.fromGitlabCommit(
                            commit, project.getPathWithNamespace(), gitlabIntegrationId);
                    if (scmAggService.getCommit(
                            company, gitlabCommit.getId(), project.getPathWithNamespace(), gitlabIntegrationId)
                            .isEmpty()) {
                        try {
                            scmAggService.insertCommit(company, gitlabCommit);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        DbScmFile.fromGitlabCommit(
                                commit, project.getPathWithNamespace(), gitlabIntegrationId)
                                .forEach(scmFile -> scmAggService.insertFile(company, scmFile));
                    }
                }));

        String arrayCatAgg = "CREATE AGGREGATE array_cat_agg(anyarray) (\n" +
                "  SFUNC=array_cat,\n" +
                "  STYPE=anyarray\n" +
                ");";
        dataSource.getConnection().prepareStatement(arrayCatAgg)
                .execute();
        dataSource.getConnection().prepareStatement(arrayUniq)
                .execute();
    }

    @Test
    public void testUsers() throws SQLException {
        //github prs
        Assertions.assertThat(userIdentityService.getUser(company, gitHubIntegrationId, "ivan-levelops")).isNotNull();
        Assertions.assertThat(userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops")).isNotNull();
        String user = userIdentityService.getUser(company, gitHubIntegrationId, "harsh-levelops");
        Assertions.assertThat(user).isNull();
        Optional<DbScmPullRequest> githubPR = scmAggService.getPr(company, "24", List.of("levelops/aggregations-levelops"), gitHubIntegrationId);
        Assertions.assertThat(githubPR.get()).isNotNull();
//        Assertions.assertThat(githubPR.get().getCreatorId()).isEqualTo(user);

        //github commits
        Assertions.assertThat(userIdentityService.getUser(company, gitHubIntegrationId, "piyushkantm")).isNotNull();
        user = userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops");
        Assertions.assertThat(user).isNotNull();
        Optional<DbScmCommit> githubCommit = scmAggService.getCommit(company,
                "5a29b8b139929802ade0124650891181907948f4", "levelops/ui-levelops", gitHubIntegrationId);
        // FIXME
//        Assertions.assertThat(githubCommit.get()).isNotNull();
//        Assertions.assertThat(githubCommit.get().getAuthorId()).isEqualTo(user);
//        Assertions.assertThat(githubCommit.get().getCommitterId()).isEqualTo(user);

        //bitbucket prs
        Assertions.assertThat(userIdentityService.getUser(company, bitbucketIntegrationId, "{62eddd8d-e878-4ce5-8dda-e7fc0e7526fa}")).isNotNull();
        Assertions.assertThat(userIdentityService.getUser(company, bitbucketIntegrationId, "{7506f083-352a-473e-ba87-163d3500e497}")).isNotNull();
        user = userIdentityService.getUser(company, bitbucketIntegrationId, "{7506f083-352a-473e-ba87-163d3500e497}");
        Assertions.assertThat(user).isNotNull();
        Optional<DbScmPullRequest> bitbucketPR = scmAggService.getPr(company, "2", "levelops-test/commons", bitbucketIntegrationId);
        Assertions.assertThat(bitbucketPR.get()).isNotNull();
        Assertions.assertThat(bitbucketPR.get().getCreatorId()).isEqualTo(user);

        //bitbucket commits
        user = userIdentityService.getUser(company, bitbucketIntegrationId, "{62eddd8d-e878-4ce5-8dda-e7fc0e7526fa}");
        Assertions.assertThat(user).isNotNull();
        Optional<DbScmCommit> bitbucketCommit = scmAggService.getCommit(company, "904db27b806baca5ab8a2db0b9a8f07f0cc65019", "levelops-test/commons", bitbucketIntegrationId);
        Assertions.assertThat(bitbucketCommit.get()).isNotNull();
        Assertions.assertThat(bitbucketCommit.get().getAuthorId()).isEqualTo(user);
        Assertions.assertThat(bitbucketCommit.get().getCommitterId()).isEqualTo(user);

        //gerrit prs
        Assertions.assertThat(userIdentityService.getUser(company, gerritIntegrationId, "admin@example.com")).isNotNull();
        user = userIdentityService.getUser(company, gerritIntegrationId, "mohit@example.com");
        Assertions.assertThat(user).isNotNull();
        Optional<DbScmPullRequest> gerritPr = scmAggService.getPr(company, "8", "project1", gerritIntegrationId);
        Assertions.assertThat(gerritPr.get()).isNotNull();
        Assertions.assertThat(gerritPr.get().getCreator()).isEqualTo("Mohit");

        //gerrit commit
        user = userIdentityService.getUser(company, gerritIntegrationId, "admin@example.com");
        Optional<DbScmCommit> gerritCommit = scmAggService.getCommit(company,
                "88745411d35f7252c59271037658e73d7dddffb8", "project1", gerritIntegrationId);
        Assertions.assertThat(gerritCommit.get()).isNotNull();
        Assertions.assertThat(gerritCommit.get().getAuthorId()).isEqualTo(user);
        Assertions.assertThat(gerritCommit.get().getCommitterId()).isEqualTo(user);

        //helix prs
        Assertions.assertThat(userIdentityService.getUser(company, helixIntegrationId, "super")).isNotNull();
        Assertions.assertThat(userIdentityService.getUser(company, helixIntegrationId, "mohit")).isNotNull();
        user = userIdentityService.getUser(company, helixIntegrationId, "mohit");
        Assertions.assertThat(user).isNotNull();
        Optional<DbScmPullRequest> helixPR = scmAggService.getPr(company, "7", "JamCode", helixIntegrationId);
        Assertions.assertThat(helixPR.get()).isNotNull();
        Assertions.assertThat(helixPR.get().getCreatorId()).isEqualTo(user);

        //helix commits
        user = userIdentityService.getUser(company, helixIntegrationId, "swarm");
        Assertions.assertThat(user).isNotNull();
        Optional<DbScmCommit> helixCommit = scmAggService.getCommit(company, "960", "sandbox", helixIntegrationId);
        Assertions.assertThat(helixCommit.get()).isNotNull();
        Assertions.assertThat(helixCommit.get().getAuthorId()).isEqualTo(user);
        Assertions.assertThat(helixCommit.get().getCommitterId()).isEqualTo(user);

        //gitlab prs
        Assertions.assertThat(userIdentityService.getUser(company, gitlabIntegrationId, "modo2777")).isNotNull();
        Assertions.assertThat(userIdentityService.getUser(company, gitlabIntegrationId, "shrikesh")).isNotNull();
        user = userIdentityService.getUser(company, gitlabIntegrationId, "modo27");
        Assertions.assertThat(user).isNotNull();
        Optional<DbScmPullRequest> gitlabPR = scmAggService.getPr(company, "1", "modo27/simple-maven-dep", gitlabIntegrationId);
        Assertions.assertThat(gitlabPR.get()).isNotNull();
        Assertions.assertThat(gitlabPR.get().getCreatorId()).isEqualTo(user);

        //gitlab commits
        Optional<DbScmCommit> gitlabCommit = scmAggService.getCommit(company,
                "ee3acd53fa5df1a866cafd8e8402a9ff467a7b42", "modo2777/my-awesome-project", gitlabIntegrationId);
        Assertions.assertThat(gitlabCommit.get()).isNotNull();
        Assertions.assertThat(gitlabCommit.get().getAuthorId()).isEqualTo(user);
        Assertions.assertThat(gitlabCommit.get().getCommitterId()).isEqualTo(user);
    }

    @Test
    public void testUserAPIs() throws SQLException {
        String user = userIdentityService.getUser(company, "2", "{62eddd8d-e878-4ce5-8dda-e7fc0e7526fa}");
        DbListResponse<DbScmCommit> commitList = scmAggService.listCommits(company, ScmCommitFilter.builder()
                .integrationIds(List.of("2"))
                .authors(List.of(user))
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(commitList.getCount()).isEqualTo(1);
        commitList = scmAggService.listCommits(company, ScmCommitFilter.builder()
                .integrationIds(List.of("2"))
                .committers(List.of(user))
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(commitList.getCount()).isEqualTo(1);
        user = userIdentityService.getUser(company, "2", "{7506f083-352a-473e-ba87-163d3500e497}");
        commitList = scmAggService.listCommits(company, ScmCommitFilter.builder()
                .integrationIds(List.of("2"))
                .authors(List.of(user))
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(commitList.getCount()).isEqualTo(10);
        Assertions.assertThat(commitList.getRecords().get(0).getAuthor()).isEqualTo("Mohit Dokania");
        Assertions.assertThat(commitList.getRecords().get(0).getCommitter()).isEqualTo("Mohit Dokania");
        
        user = userIdentityService.getUser(company, "1", "viraj-levelops");
        DbListResponse<DbScmPullRequest> prList = scmAggService.list(company,
                ScmPrFilter.builder()
                        .integrationIds(List.of("1"))
                        .creators(List.of(user))
                        .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(prList.getCount()).isEqualTo(9);
        user = userIdentityService.getUser(company, "1", "ivan-levelops");
        prList = scmAggService.list(company,
                ScmPrFilter.builder()
                        .integrationIds(List.of("1"))
                        .creators(List.of(user))
                        .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(prList.getCount()).isEqualTo(1);
        Assertions.assertThat(prList.getRecords().get(0).getCreator()).isEqualTo("ivan-levelops");
        Assertions.assertThat(prList.getRecords().get(0).getCreator()).isEqualTo("ivan-levelops");

        user = userIdentityService.getUser(company, "5", "modo2777");
        String user2 = userIdentityService.getUser(company, "5", "modo27");
        DbListResponse<DbAggregationResult> prGroupByResponse = scmAggService.groupByAndCalculatePrs(company,
                ScmPrFilter.builder()
                        .integrationIds(List.of("5"))
                        .creators(List.of(user, user2))
                        .across(ScmPrFilter.DISTINCT.creator)
                        .build(), null);
        Assertions.assertThat(prGroupByResponse.getRecords().get(0).getCount()).isEqualTo(12);
        Assertions.assertThat(prGroupByResponse.getRecords().get(1).getCount()).isEqualTo(6);
        user = userIdentityService.getUser(company, "5", "shrikesh");
        prGroupByResponse = scmAggService.groupByAndCalculatePrs(company,
                ScmPrFilter.builder()
                        .integrationIds(List.of("5"))
                        .creators(List.of(user))
                        .across(ScmPrFilter.DISTINCT.creator)
                        .build(), null);
        Assertions.assertThat(prGroupByResponse.getRecords().get(0).getCount()).isEqualTo(1);

        user = userIdentityService.getUser(company, "4", "swarm");
        DbListResponse<DbAggregationResult> commitsGroupByResponse = scmAggService.groupByAndCalculateCommits(company,
                ScmCommitFilter.builder()
                        .integrationIds(List.of("4"))
                        .committers(List.of(user))
                        .across(ScmCommitFilter.DISTINCT.author)
                        .build(), null);
        Assertions.assertThat(commitsGroupByResponse.getRecords().get(0).getCount()).isEqualTo(2);
        commitsGroupByResponse = scmAggService.groupByAndCalculateCommits(company, ScmCommitFilter.builder()
                .integrationIds(List.of("4"))
                .authors(List.of(user))
                .across(ScmCommitFilter.DISTINCT.committer)
                .build(), null);
        Assertions.assertThat(commitsGroupByResponse.getRecords().get(0).getCount()).isEqualTo(2);
        user = userIdentityService.getUser(company, "4", "super");
        commitsGroupByResponse = scmAggService.groupByAndCalculateCommits(company, ScmCommitFilter.builder()
                .integrationIds(List.of("4"))
                .authors(List.of(user))
                .across(ScmCommitFilter.DISTINCT.author)
                .build(), null);
        Assertions.assertThat(commitsGroupByResponse.getRecords().get(0).getCount()).isEqualTo(16);
        commitsGroupByResponse = scmAggService.groupByAndCalculateCommits(company, ScmCommitFilter.builder()
                .integrationIds(List.of("4"))
                .committers(List.of(user))
                .across(ScmCommitFilter.DISTINCT.committer)
                .build(), null);
        Assertions.assertThat(commitsGroupByResponse.getRecords().get(0).getCount()).isEqualTo(16);

        DbListResponse<DbScmContributorAgg> contributorsList = scmAggService.list(company,
                ScmContributorsFilter.builder()
                        .integrationIds(List.of("2"))
                        .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(contributorsList.getCount()).isEqualTo(2);
    }
}
