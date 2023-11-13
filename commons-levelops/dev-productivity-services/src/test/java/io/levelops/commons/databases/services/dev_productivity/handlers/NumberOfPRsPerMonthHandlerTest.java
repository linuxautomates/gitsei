package io.levelops.commons.databases.services.dev_productivity.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.dev_productivity.*;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.DevProductivityFilter;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.GitRepositoryService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.integrations.github.models.GithubRepository;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;

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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.levelops.commons.databases.services.ScmQueryUtils.ARRAY_UNIQ;


public class NumberOfPRsPerMonthHandlerTest {

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
    private static NumberOfPRsPerMonthHandler numberOfPRsPerMonthHandler;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        dataSource.getConnection().prepareStatement(ARRAY_UNIQ).execute();

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
        numberOfPRsPerMonthHandler = new NumberOfPRsPerMonthHandler(scmAggService, null, List.of("test"));

        String input = ResourceUtils.getResourceAsString("json/databases/githubprs.json");
        PaginatedResponse<GithubRepository> prs = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        List<DbRepository> repos = new ArrayList<>();
        insertPrs(prs, repos, gitHubIntegrationId);
        insertPrs(prs, repos, gitHubIntegrationId2);
        repositoryService.batchUpsert(company, repos);

    }

    private static void insertPrs(PaginatedResponse<GithubRepository> prs, List<DbRepository> repos, String integrationId) {
        prs.getResponse().getRecords().forEach(repo -> {
            repos.add(DbRepository.fromGithubRepository(repo, integrationId));
            repo.getPullRequests()
                    .forEach(pr -> {
                        try {
                            DbScmPullRequest tmp = DbScmPullRequest
                                    .fromGithubPullRequest(pr, repo.getId(), integrationId, null);
                            tmp = tmp.toBuilder().prMergedAt(1632403905L).build();
                            scmAggService.insert(company, tmp);
                        } catch (SQLException throwable) {
                            throwable.printStackTrace();
                        }
                    });
        });
    }

    @Test
    public void testNumberOfPRsPerMonthHandler() throws SQLException, IOException {
        String userId1 = userIdentityService.getUser(company, gitHubIntegrationId, "harsh-levelops");
        DbScmUser user1 = userIdentityService.get(company, userId1).orElse(null);
        Assert.assertNotNull(user1);
        String userId2 = userIdentityService.getUser(company, gitHubIntegrationId2, "harsh-levelops");
        DbScmUser user2 = userIdentityService.get(company, userId2).orElse(null);
        Assert.assertNotNull(user2);
        Instant now = Instant.now();
        UUID orgUserId = UUID.randomUUID();
        FeatureResponse response = numberOfPRsPerMonthHandler.calculateFeature(company,SECTION_ORDER,
                DevProductivityProfile.Feature.builder()
                        .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_PRS_PER_MONTH)
                        .maxValue(100L).lowerLimitPercentage(25).upperLimitPercentage(75)
                        .build(), Map.of(),
                DevProductivityFilter.builder()
                        .timeRange(ImmutablePair.of(
                                1632131529L,
                                1639993929L))
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
        Assert.assertEquals(Double.valueOf("3.5"), response.getMean());

        response = numberOfPRsPerMonthHandler.calculateFeature(company,SECTION_ORDER,
                DevProductivityProfile.Feature.builder()
                        .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_PRS_PER_MONTH)
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

        userId1 = userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops");
        user1 = userIdentityService.get(company, userId1).orElse(null);
        Assert.assertNotNull(user1);
        userId2 = userIdentityService.getUser(company, gitHubIntegrationId2, "viraj-levelops");
        user2 = userIdentityService.get(company, userId2).orElse(null);
        Assert.assertNotNull(user2);

        response = numberOfPRsPerMonthHandler.calculateFeature(company,SECTION_ORDER,
                DevProductivityProfile.Feature.builder()
                        .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_PRS_PER_MONTH)
                        .maxValue(100L).lowerLimitPercentage(25).upperLimitPercentage(75)
                        .build(), Map.of(),
                DevProductivityFilter.builder()
                        .timeRange(ImmutablePair.of(
                                1632131529L,
                                1639993929L))
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
        Assert.assertEquals(Double.valueOf("7.5"), response.getMean());

        response = numberOfPRsPerMonthHandler.calculateFeature(company,SECTION_ORDER,
                DevProductivityProfile.Feature.builder()
                        .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_PRS_PER_MONTH)
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

        response = numberOfPRsPerMonthHandler.calculateFeature(company,SECTION_ORDER,
                DevProductivityProfile.Feature.builder()
                        .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_PRS_PER_MONTH)
                        .maxValue(100L).lowerLimitPercentage(25).upperLimitPercentage(75)
                        .build(), Map.of(),
                DevProductivityFilter.builder()
                        .timeRange(ImmutablePair.of(
                                1632131529L,
                                1639993929L))
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
        Assert.assertEquals(Double.valueOf("7.5"), response.getMean());

        response = numberOfPRsPerMonthHandler.calculateFeature(company,SECTION_ORDER,
                DevProductivityProfile.Feature.builder()
                        .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_PRS_PER_MONTH)
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

        userId1 = userIdentityService.getUser(company, gitHubIntegrationId, "ivan-levelops");
        user1 = userIdentityService.get(company, userId1).orElse(null);
        Assert.assertNotNull(user1);
        userId2 = userIdentityService.getUser(company, gitHubIntegrationId2, "ivan-levelops");
        user2 = userIdentityService.get(company, userId2).orElse(null);
        Assert.assertNotNull(user2);

        response = numberOfPRsPerMonthHandler.calculateFeature(company,SECTION_ORDER,
                DevProductivityProfile.Feature.builder()
                        .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_PRS_PER_MONTH)
                        .maxValue(100L).lowerLimitPercentage(25).upperLimitPercentage(75)
                        .build(), Map.of(),
                DevProductivityFilter.builder()
                        .timeRange(ImmutablePair.of(
                                1632131529L,
                                1639993929L))
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
        Assert.assertEquals(Double.valueOf("1.0"), response.getMean());

        response = numberOfPRsPerMonthHandler.calculateFeature(company,SECTION_ORDER,
                DevProductivityProfile.Feature.builder()
                        .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_PRS_PER_MONTH)
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

        response = numberOfPRsPerMonthHandler.calculateFeature(company,SECTION_ORDER,
                DevProductivityProfile.Feature.builder()
                        .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_PRS_PER_MONTH)
                        .maxValue(100L).lowerLimitPercentage(25).upperLimitPercentage(75)
                        .build(), Map.of(),
                DevProductivityFilter.builder()
                        .timeRange(ImmutablePair.of(
                                1632131529L,
                                1639993929L))
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
        Assert.assertEquals(Double.valueOf("1.0"), response.getMean());

        response = numberOfPRsPerMonthHandler.calculateFeature(company,SECTION_ORDER,
                DevProductivityProfile.Feature.builder()
                        .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_PRS_PER_MONTH)
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

   @Test
   public void testReviewsPerMonth() throws SQLException, IOException {

       String userId1 = userIdentityService.getUser(company, gitHubIntegrationId, "maxime-levelops");
       DbScmUser user1 = userIdentityService.get(company, userId1).orElse(null);
       Assert.assertNotNull(user1);
       String userId2 = userIdentityService.getUser(company, gitHubIntegrationId2, "maxime-levelops");
       DbScmUser user2 = userIdentityService.get(company, userId2).orElse(null);
       Assert.assertNotNull(user2);
       Instant now = Instant.now();
       UUID orgUserId = UUID.randomUUID();
       DevProductivityProfile.Feature feature = DevProductivityProfile.Feature.builder()
               .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_PRS_COMMENTED_ON_PER_MONTH)
               .maxValue(10L).lowerLimitPercentage(25).upperLimitPercentage(75)
               .build();
       DevProductivityFilter devProductivityFilter = DevProductivityFilter.builder()
               .timeRange(ImmutablePair.of(
                       1632131529L,
                       1639993929L))
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
       FeatureResponse response = numberOfPRsPerMonthHandler.calculateFeature(company, SECTION_ORDER, feature, Map.of(), devProductivityFilter, orgUserDetails, Map.of(), null);
       Assert.assertNotNull(response);
       Assert.assertEquals(Double.valueOf("0.5"), response.getMean());

       FeatureBreakDown breakDown = numberOfPRsPerMonthHandler.getBreakDown(company, feature, Map.of(), devProductivityFilter, orgUserDetails, Map.of(), null, Map.of(), 0, 100);
       Assert.assertNotNull(breakDown);
       Assertions.assertThat(breakDown.getRecords().size()).isEqualTo(2);

       feature =   DevProductivityProfile.Feature.builder()
               .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_PRS_APPROVED_PER_MONTH)
               .maxValue(10L).lowerLimitPercentage(25).upperLimitPercentage(75)
               .build();

       response = numberOfPRsPerMonthHandler.calculateFeature(company, SECTION_ORDER, feature, Map.of(), devProductivityFilter, orgUserDetails, Map.of(), null);
       Assert.assertNotNull(response);
       Assert.assertEquals(Double.valueOf("0.5"), response.getMean());

       breakDown = numberOfPRsPerMonthHandler.getBreakDown(company, feature, Map.of(), devProductivityFilter, orgUserDetails, Map.of(), null, Map.of(), 0, 100);
       Assert.assertNotNull(breakDown);
       Assertions.assertThat(breakDown.getRecords().size()).isEqualTo(2);

       userId1 = userIdentityService.getUser(company, gitHubIntegrationId, "ivan-levelops");
       user1 = userIdentityService.get(company, userId1).orElse(null);
       Assert.assertNotNull(user1);
       userId2 = userIdentityService.getUser(company, gitHubIntegrationId2, "ivan-levelops");
       user2 = userIdentityService.get(company, userId2).orElse(null);
       Assert.assertNotNull(user2);

       feature =   DevProductivityProfile.Feature.builder()
               .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_PRS_COMMENTED_ON_PER_MONTH)
               .maxValue(10L).lowerLimitPercentage(25).upperLimitPercentage(75)
               .build();

       devProductivityFilter = DevProductivityFilter.builder()
               .timeRange(ImmutablePair.of(
                       1632131529L,
                       1639993929L))
               .build();
       orgUserDetails = OrgUserDetails.builder()
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

       response = numberOfPRsPerMonthHandler.calculateFeature(company, SECTION_ORDER, feature, Map.of(), devProductivityFilter, orgUserDetails, Map.of(), null);
       Assert.assertNotNull(response);
       Assert.assertNull(response.getMean());

       breakDown = numberOfPRsPerMonthHandler.getBreakDown(company, feature, Map.of(), devProductivityFilter, orgUserDetails, Map.of(), null, Map.of(), 0, 100);
       Assert.assertNotNull(breakDown);
       Assertions.assertThat(breakDown.getRecords().size()).isEqualTo(0);

       feature =   DevProductivityProfile.Feature.builder()
               .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_PRS_APPROVED_PER_MONTH)
               .maxValue(10L).lowerLimitPercentage(25).upperLimitPercentage(75)
               .build();

       response = numberOfPRsPerMonthHandler.calculateFeature(company, SECTION_ORDER, feature, Map.of(), devProductivityFilter, orgUserDetails, Map.of(), null);
       Assert.assertNotNull(response);
       Assert.assertEquals(Double.valueOf("2"), response.getMean());

       breakDown = numberOfPRsPerMonthHandler.getBreakDown(company, feature, Map.of(), devProductivityFilter, orgUserDetails, Map.of(), null, Map.of(), 0, 100);
       Assert.assertNotNull(breakDown);
       Assertions.assertThat(breakDown.getRecords().size()).isEqualTo(8);

   }
}
