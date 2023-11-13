package io.levelops.commons.databases.services.dev_productivity.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.models.database.dev_productivity.FeatureResponse;
import io.levelops.commons.databases.models.database.dev_productivity.IntegrationUserDetails;
import io.levelops.commons.databases.models.database.dev_productivity.OrgUserDetails;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LOCPerMonthHandlerTest {

    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    private static final Integer SECTION_ORDER = 0;
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static ScmAggService scmAggService;
    private static UserIdentityService userIdentityService;
    private static String gitHubIntegrationId;
    private static String gitHubIntegrationId2;
    private static NumberOfCommitsAndLOCPerMonthHandler locPerMonthHandler;
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
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        repositoryService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);
        locPerMonthHandler = new NumberOfCommitsAndLOCPerMonthHandler(scmAggService, null, List.of("test"));
        currentTime = DateUtils.truncate(new Date(Instant.parse("2021-05-05T15:00:00-08:00").getEpochSecond()), Calendar.DATE);

        String input = ResourceUtils.getResourceAsString("json/databases/githubcommits.json");
        PaginatedResponse<GithubRepository> commits = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        List<DbRepository> repos = new ArrayList<>();
        insertCommits(commits, repos, gitHubIntegrationId);
        insertCommits(commits, repos, gitHubIntegrationId2);
        repositoryService.batchUpsert(company, repos);
    }

    private static void insertCommits(PaginatedResponse<GithubRepository> commits, List<DbRepository> repos, String integrationId) {
        commits.getResponse().getRecords().forEach(repo -> {
            repos.add(DbRepository.fromGithubRepository(repo, integrationId));
            repo.getEvents().stream()
                    .filter(ev -> "PushEvent".equals(ev.getType()))
                    .flatMap(ev -> ev.getCommits().stream())
                    .forEach(commit -> {
                        DbScmCommit tmp = DbScmCommit
                                .fromGithubCommit(commit, repo.getId(), integrationId,
                                        1632403905L, 0L);
                        if (scmAggService.getCommit(company, tmp.getCommitSha(), tmp.getRepoIds(),
                                tmp.getIntegrationId()).isEmpty()) {
                            try {
                                scmAggService.insertCommit(company, tmp);
                            } catch (SQLException e) {
                                throw new RuntimeStreamException(e);
                            }
                            DbScmFile.fromGithubCommit(
                                            commit, repo.getId(), integrationId, currentTime.toInstant().getEpochSecond())
                                    .forEach(scmFile -> scmAggService.insertFile(company, scmFile));
                        }
                    });
        });
    }

    @Test
    public void testLOCPerMonthHandler() throws SQLException, IOException {
        String userId1 = userIdentityService.getUser(company, gitHubIntegrationId, "meghana-levelops");
        DbScmUser user1 = userIdentityService.get(company, userId1).orElse(null);
        Assert.assertNotNull(user1);
        String userId2 = userIdentityService.getUser(company, gitHubIntegrationId2, "meghana-levelops");
        DbScmUser user2 = userIdentityService.get(company, userId2).orElse(null);
        Assert.assertNotNull(user2);
        Instant now = Instant.now();
        UUID orgUserId = UUID.randomUUID();
        FeatureResponse response = locPerMonthHandler.calculateFeature(company,SECTION_ORDER,
                DevProductivityProfile.Feature.builder()
                        .params(Map.of("fetch_committers_content", List.of("true")))
                        .featureType(DevProductivityProfile.FeatureType.LINES_OF_CODE_PER_MONTH)
                        .maxValue(100L).lowerLimitPercentage(25).upperLimitPercentage(75)
                        .build(), Map.of(),
                DevProductivityFilter.builder()
                        .timeRange(ImmutablePair.of(
                                1632144705L,
                                1640007033L))
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
        Assert.assertEquals(Double.valueOf("14173.5"), response.getMean());

        response = locPerMonthHandler.calculateFeature(company,SECTION_ORDER,
                DevProductivityProfile.Feature.builder()
                        .params(Map.of("fetch_committers_content", List.of("true")))
                        .featureType(DevProductivityProfile.FeatureType.LINES_OF_CODE_PER_MONTH)
                        .maxValue(100L).lowerLimitPercentage(25).upperLimitPercentage(75)
                        .build(), Map.of(),
                DevProductivityFilter.builder()
                        .timeRange(ImmutablePair.of(now.getEpochSecond(), now.getEpochSecond()))
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
        Assert.assertNull(response.getMean());

        response = locPerMonthHandler.calculateFeature(company,SECTION_ORDER,
                DevProductivityProfile.Feature.builder()
                        .params(Map.of("fetch_authors_content", List.of("true")))
                        .featureType(DevProductivityProfile.FeatureType.LINES_OF_CODE_PER_MONTH)
                        .maxValue(100L).lowerLimitPercentage(25).upperLimitPercentage(75)
                        .build(), Map.of(),
                DevProductivityFilter.builder()
                        .timeRange(ImmutablePair.of(
                                1632144705L,
                                1640007033L))
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
        Assert.assertEquals(Double.valueOf("14173.5"), response.getMean());

        response = locPerMonthHandler.calculateFeature(company,SECTION_ORDER,
                DevProductivityProfile.Feature.builder()
                        .params(Map.of("fetch_authors_content", List.of("true")))
                        .featureType(DevProductivityProfile.FeatureType.LINES_OF_CODE_PER_MONTH)
                        .maxValue(100L).lowerLimitPercentage(25).upperLimitPercentage(75)
                        .build(), Map.of(),
                DevProductivityFilter.builder()
                        .timeRange(ImmutablePair.of(now.getEpochSecond(), now.getEpochSecond()))
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
        Assert.assertNull(response.getMean());
    }
}
