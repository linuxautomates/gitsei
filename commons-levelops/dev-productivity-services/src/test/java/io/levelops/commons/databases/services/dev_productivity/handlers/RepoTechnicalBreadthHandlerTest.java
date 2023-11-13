package io.levelops.commons.databases.services.dev_productivity.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.dev_productivity.*;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.database.scm.RepoConfigEntryMatcher;
import io.levelops.commons.databases.models.filters.DevProductivityFilter;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.GitRepositoryService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.helix_swarm.models.HelixSwarmReview;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.ScmQueryUtils.ARRAY_UNIQ;

public class RepoTechnicalBreadthHandlerTest {
    private static final String company = "test";
    private static final RepoConfigEntryMatcher repoConfigEntryMatcher = new RepoConfigEntryMatcher(List.of(
            IntegrationConfig.RepoConfigEntry.builder().repoId("DummyProject").pathPrefix("//DummyProject/main").build(),
            IntegrationConfig.RepoConfigEntry.builder().repoId("sandbox").pathPrefix("//sandbox/main").build(),
            IntegrationConfig.RepoConfigEntry.builder().repoId("JamCode").pathPrefix("//JamCode/main").build()
    ));
    private static final ObjectMapper m = DefaultObjectMapper.get();
    private static final Integer SECTION_ORDER = 0;

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static ScmAggService scmAggService;
    private static UserIdentityService userIdentityService;
    private static String gitHubIntegrationId;
    private static String gitHubIntegrationId2;
    private static String helixIntegrationId;
    private static RepoTechnicalBreadthHandler repoTechnicalBreadthHandler;
    private static Date currentTime;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        IntegrationService integrationService = new IntegrationService(dataSource);
        userIdentityService = new UserIdentityService(dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        GitRepositoryService repositoryService = new GitRepositoryService(dataSource);
        TeamMembersDatabaseService teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, m);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        gitHubIntegrationId = integrationService.insert(company, Integration.builder()
                .application("github")
                .name("github test")
                .status("enabled")
                .build());
        gitHubIntegrationId2 = integrationService.insert(company, Integration.builder()
                .application("github")
                .name("github test 2")
                .status("enabled")
                .build());
        helixIntegrationId = integrationService.insert(company, Integration.builder()
                .application("helix")
                .name("helix test")
                .status("enabled")
                .build());
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        repositoryService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);
        repoTechnicalBreadthHandler = new RepoTechnicalBreadthHandler(scmAggService, null, List.of("test"));
        currentTime = DateUtils.truncate(new Date(Instant.parse("2021-05-05T15:00:00-08:00").getEpochSecond()), Calendar.DATE);

        String input = ResourceUtils.getResourceAsString("json/databases/githubcommits.json");
        PaginatedResponse<GithubRepository> commits = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        List<DbRepository> repos = new ArrayList<>();
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
                                throw new RuntimeStreamException(e);
                            }
                            DbScmFile.fromGithubCommit(
                                    commit, repo.getId(), gitHubIntegrationId, currentTime.toInstant().getEpochSecond())
                                    .forEach(scmFile -> scmAggService.insertFile(company, scmFile));
                        }
                    });
        });
        commits.getResponse().getRecords().forEach(repo -> {
            repos.add(DbRepository.fromGithubRepository(repo, gitHubIntegrationId2));
            repo.getEvents().stream()
                    .filter(ev -> "PushEvent".equals(ev.getType()))
                    .flatMap(ev -> ev.getCommits().stream())
                    .forEach(commit -> {
                        DbScmCommit tmp = DbScmCommit
                                .fromGithubCommit(commit, repo.getId(), gitHubIntegrationId2,
                                        currentTime.toInstant().getEpochSecond(), 0L);
                        if (scmAggService.getCommit(company, tmp.getCommitSha(), tmp.getRepoIds(),
                                tmp.getIntegrationId()).isEmpty()) {
                            try {
                                scmAggService.insertCommit(company, tmp);
                            } catch (SQLException e) {
                                throw new RuntimeStreamException(e);
                            }
                            DbScmFile.fromGithubCommit(
                                    commit, repo.getId(), gitHubIntegrationId2, currentTime.toInstant().getEpochSecond())
                                    .forEach(scmFile -> scmAggService.insertFile(company, scmFile));
                        }
                    });
        });
        repositoryService.batchUpsert(company, repos);

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
                            throw new RuntimeStreamException(e);
                        }
                    });
        });

        String arrayCatAgg = "CREATE AGGREGATE array_cat_agg(anyarray) (\n" +
                "  SFUNC=array_cat,\n" +
                "  STYPE=anyarray\n" +
                ");";
        dataSource.getConnection().prepareStatement(arrayCatAgg)
                .execute();

        dataSource.getConnection().prepareStatement(ARRAY_UNIQ)
                .execute();
    }

    @Test
    public void testRepoBreadthCalculateFeature() throws SQLException, IOException {
        String userId1 = userIdentityService.getUser(company, gitHubIntegrationId, "meghana-levelops");
        DbScmUser user1 = userIdentityService.get(company, userId1).orElse(null);
        Assert.assertNotNull(user1);
        String userId2 = userIdentityService.getUser(company, gitHubIntegrationId2, "meghana-levelops");
        DbScmUser user2 = userIdentityService.get(company, userId2).orElse(null);
        Assert.assertNotNull(user2);
        Instant now = Instant.now();
        UUID orgUserId = UUID.randomUUID();

        DevProductivityProfile.Feature feature = DevProductivityProfile.Feature.builder()
                .params(Map.of("use_committers", List.of("true"), "use_timeline", List.of("true")))
                .featureType(DevProductivityProfile.FeatureType.REPO_BREADTH)
                .maxValue(100L).lowerLimitPercentage(25).upperLimitPercentage(75)
                .build();
        DevProductivityFilter devProductivityFilter = DevProductivityFilter.builder()
                        .timeRange(ImmutablePair.of(
                                Instant.EPOCH.getEpochSecond(),
                                now.getEpochSecond()))
                        .build();
        OrgUserDetails orgUserDetails = OrgUserDetails.builder()
                        .orgUserId(orgUserId)
                        .email("user@levelops.io")
                        .fullName("User Name")
                        .IntegrationUserDetailsList(List.of(IntegrationUserDetails.builder()
                                        .cloudId(user1.getCloudId())
                                        .integrationId(Integer.parseInt(gitHubIntegrationId))
                                        .integrationType(IntegrationType.GITHUB)
                                        .integrationUserId(UUID.fromString(user1.getId()))
                                        .displayName(user1.getDisplayName())
                                        .build(),
                                IntegrationUserDetails.builder()
                                        .cloudId(user2.getCloudId())
                                        .integrationId(Integer.parseInt(gitHubIntegrationId2))
                                        .integrationType(IntegrationType.GITHUB)
                                        .integrationUserId(UUID.fromString(user2.getId()))
                                        .displayName(user2.getDisplayName())
                                        .build()))
                        .build();

        FeatureResponse response = repoTechnicalBreadthHandler.calculateFeature(company, SECTION_ORDER, feature, Map.of(), devProductivityFilter, orgUserDetails, Map.of(), null);
        Assert.assertNotNull(response);
        Assert.assertEquals(Double.valueOf(1), response.getMean());

        DevProductivityProfile.Feature feature1 = DevProductivityProfile.Feature.builder()
                .params(Map.of("use_authors", List.of("true")))
                .featureType(DevProductivityProfile.FeatureType.REPO_BREADTH)
                .maxValue(100L).lowerLimitPercentage(25).upperLimitPercentage(75)
                .build();
        DevProductivityFilter devProductivityFilter1 = DevProductivityFilter.builder()
                        .timeRange(ImmutablePair.of(
                                Instant.EPOCH.getEpochSecond(),
                                now.getEpochSecond()))
                        .build();
        OrgUserDetails orgUserDetails1 = OrgUserDetails.builder()
                        .orgUserId(orgUserId)
                        .email("user@levelops.io")
                        .fullName("User Name")
                        .IntegrationUserDetailsList(List.of(IntegrationUserDetails.builder()
                                .cloudId(user1.getCloudId())
                                .integrationId(Integer.parseInt(helixIntegrationId))
                                .integrationType(IntegrationType.HELIX_CORE)
                                .integrationUserId(UUID.fromString(user1.getId()))
                                .displayName(user1.getDisplayName())
                                .build()))
                        .build();

        response = repoTechnicalBreadthHandler.calculateFeature(company, SECTION_ORDER, feature1, Map.of(), devProductivityFilter1, orgUserDetails1, Map.of(), null);
        Assert.assertNotNull(response);

        FeatureBreakDown breakDown = repoTechnicalBreadthHandler.getBreakDown(company, feature, Map.of(), devProductivityFilter, orgUserDetails, Map.of(), null, Map.of(), 0, 100);
        Assert.assertNotNull(breakDown);
        Assertions.assertThat(breakDown.getRecords().size()).isEqualTo(2);
    }

    @Test
    public void testTechBreadthCalculateFeature() throws SQLException, IOException {
        String userId1 = userIdentityService.getUser(company, gitHubIntegrationId, "meghana-levelops");
        DbScmUser user1 = userIdentityService.get(company, userId1).orElse(null);
        Assert.assertNotNull(user1);
        String userId2 = userIdentityService.getUser(company, gitHubIntegrationId2, "meghana-levelops");
        DbScmUser user2 = userIdentityService.get(company, userId2).orElse(null);
        Assert.assertNotNull(user2);
        Instant now = Instant.now();
        UUID orgUserId = UUID.randomUUID();

        FeatureResponse response = repoTechnicalBreadthHandler.calculateFeature(company, SECTION_ORDER,
                DevProductivityProfile.Feature.builder()
                        .params(Map.of("use_authors", List.of("true"),"use_timeline", List.of("true")))
                        .featureType(DevProductivityProfile.FeatureType.TECHNICAL_BREADTH)
                        .maxValue(100L).lowerLimitPercentage(25).upperLimitPercentage(75)
                        .build(), Map.of(),
                DevProductivityFilter.builder()
                        .timeRange(ImmutablePair.of(
                                Instant.EPOCH.getEpochSecond(),
                                now.getEpochSecond()))
                        .build(),
                OrgUserDetails.builder()
                        .orgUserId(orgUserId)
                        .email("user@levelops.io")
                        .fullName("User Name")
                        .IntegrationUserDetailsList(List.of(IntegrationUserDetails.builder()
                                        .cloudId(user1.getCloudId())
                                        .integrationId(Integer.parseInt(gitHubIntegrationId))
                                        .integrationType(IntegrationType.GITHUB)
                                        .integrationUserId(UUID.fromString(user1.getId()))
                                        .displayName(user1.getDisplayName())
                                        .build(),
                                IntegrationUserDetails.builder()
                                        .cloudId(user2.getCloudId())
                                        .integrationId(Integer.parseInt(gitHubIntegrationId2))
                                        .integrationType(IntegrationType.GITHUB)
                                        .integrationUserId(UUID.fromString(user2.getId()))
                                        .displayName(user2.getDisplayName())
                                        .build()))
                        .build(), Map.of(), null);
        Assert.assertNotNull(response);
        Assert.assertEquals(Double.valueOf(3), response.getMean());
    }
}
