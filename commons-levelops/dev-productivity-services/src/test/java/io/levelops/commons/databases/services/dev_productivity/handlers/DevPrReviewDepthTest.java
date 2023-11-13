package io.levelops.commons.databases.services.dev_productivity.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.TenantSCMSettings;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.models.database.dev_productivity.FeatureBreakDown;
import io.levelops.commons.databases.models.database.dev_productivity.FeatureResponse;
import io.levelops.commons.databases.models.database.dev_productivity.IntegrationUserDetails;
import io.levelops.commons.databases.models.database.dev_productivity.OrgUserDetails;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.DevProductivityFilter;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.GitRepositoryService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile.FeatureType.PRS_REVIEW_DEPTH;
import static io.levelops.commons.databases.services.ScmQueryUtils.ARRAY_UNIQ;

public class DevPrReviewDepthTest {

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
    private static DevPRReviewDepthHandler devPRReviewDepthHandler;


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
        devPRReviewDepthHandler = new DevPRReviewDepthHandler(scmAggService, null, List.of("test"));

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
                            scmAggService.insert(company, tmp);
                        } catch (SQLException throwable) {
                            throwable.printStackTrace();
                        }
                    });
        });
    }

    @Test
    public void testCalculateFeatures() throws SQLException, IOException {
        String userId1 = userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops");
        DbScmUser user1 = userIdentityService.get(company, userId1).orElse(null);
        Assert.assertNotNull(user1);

        FeatureResponse featureResponse = devPRReviewDepthHandler.calculateFeature(company, SECTION_ORDER, DevProductivityProfile.Feature.builder()
                        .name("Sample Feature")
                        .description("This is a sample description")
                        .order(1)
                        .featureType(PRS_REVIEW_DEPTH)
                        .params(Map.of("use_approver", List.of("false")))
                        .maxValue(100L).lowerLimitPercentage(25).upperLimitPercentage(75)
                        .build(), Map.of(), DevProductivityFilter.builder()
                        .timeRange(ImmutablePair.of(0L, 1665578101L))
                        .build(),
                OrgUserDetails.builder()
                        .orgUserId(UUID.fromString("51f1a9e0-8559-4e81-9397-87720a3848e0"))
                        .fullName("Ivan Levelops")
                        .IntegrationUserDetailsList(List.of(
                                IntegrationUserDetails.builder()
                                        .displayName("viraj-levelops")
                                        .integrationId(1)
                                        .integrationType(IntegrationType.GITHUB)
                                        .integrationUserId(UUID.fromString(user1.getId()))
                                        .cloudId("viraj-levelops").build()
                        ))
                        .build(), Map.of(), TenantSCMSettings.builder()
                        .commentDensitySmall(0)
                        .commentDensityMedium(1)
                        .build()
        );
        Assertions.assertThat(featureResponse).isNotNull();
        Assertions.assertThat(featureResponse.getMean()).isNotNull();
        DbListResponse<DbAggregationResult> resultDbListResponse = scmAggService.groupByAndCalculatePrs(company, ScmPrFilter.builder()
                .reviewers(List.of(userId1))
                .integrationIds(List.of(gitHubIntegrationId))
                .commentDensitySizeConfig(Map.of("shallow", "0", "good", "1"))
                .across(ScmPrFilter.DISTINCT.comment_density)
                .build(), null);
        Double actualMean = resultDbListResponse.getRecords().stream().mapToDouble(DbAggregationResult::getTotalComments).sum() /
                resultDbListResponse.getRecords().stream().mapToDouble(DbAggregationResult::getCount).sum();
        Assertions.assertThat(featureResponse.getMean().doubleValue()).isEqualTo(actualMean);

        featureResponse = devPRReviewDepthHandler.calculateFeature(company, SECTION_ORDER, DevProductivityProfile.Feature.builder()
                        .name("Sample Feature")
                        .description("This is a sample description")
                        .order(1)
                        .featureType(PRS_REVIEW_DEPTH)
                        .params(Map.of("use_approver", List.of("true")))
                        .maxValue(100L).lowerLimitPercentage(25).upperLimitPercentage(75)
                        .build(), Map.of(),  DevProductivityFilter.builder()
                        .timeRange(ImmutablePair.of(0L, 1665578101L))
                        .build(),
                OrgUserDetails.builder()
                        .orgUserId(UUID.fromString("51f1a9e0-8559-4e81-9397-87720a3848e0"))
                        .fullName("Ivan Levelops")
                        .IntegrationUserDetailsList(List.of(
                                IntegrationUserDetails.builder()
                                        .displayName("ivan-levelops")
                                        .integrationId(1)
                                        .integrationType(IntegrationType.GITHUB)
                                        .integrationUserId(UUID.fromString(user1.getId()))
                                        .cloudId("viraj-levelops").build()
                        ))
                        .build(),  Map.of(), TenantSCMSettings.builder()
                        .commentDensitySmall(0)
                        .commentDensityMedium(1)
                        .build()
        );

        Assertions.assertThat(featureResponse.getMean()).isNotNull();
        resultDbListResponse = scmAggService.groupByAndCalculatePrs(company, ScmPrFilter.builder()
                .approvers(List.of(userId1))
                .integrationIds(List.of(gitHubIntegrationId))
                .commentDensitySizeConfig(Map.of("shallow", "0", "good", "1"))
                .across(ScmPrFilter.DISTINCT.comment_density)
                .build(), null);
        actualMean = resultDbListResponse.getRecords().stream().mapToDouble(DbAggregationResult::getTotalComments).sum() /
                resultDbListResponse.getRecords().stream().mapToDouble(DbAggregationResult::getCount).sum();
        Assertions.assertThat(featureResponse.getMean().doubleValue()).isEqualTo(actualMean);

        DevProductivityProfile.Feature feature = DevProductivityProfile.Feature.builder()
                .name("Sample Feature")
                .description("This is a sample description")
                .order(1)
                .featureType(PRS_REVIEW_DEPTH)
                .params(Map.of("use_approver", List.of("true")))
                .maxValue(100L).lowerLimitPercentage(25).upperLimitPercentage(75)
                .build();
        DevProductivityFilter devProductivityFilter = DevProductivityFilter.builder()
                .timeRange(ImmutablePair.of(0L, 1665578101L))
                .build();
        OrgUserDetails orgUserDetails = OrgUserDetails.builder()
                        .orgUserId(UUID.fromString("51f1a9e0-8559-4e81-9397-87720a3848e0"))
                        .fullName("Ivan Levelops")
                        .IntegrationUserDetailsList(List.of(
                                IntegrationUserDetails.builder()
                                        .displayName("viraj-levelops")
                                        .integrationId(1)
                                        .integrationType(IntegrationType.GITHUB)
                                        .integrationUserId(UUID.fromString(user1.getId()))
                                        .cloudId("viraj-levelops").build()))
                        .build();

        featureResponse = devPRReviewDepthHandler.calculateFeature(company, SECTION_ORDER, feature, Map.of(), devProductivityFilter, orgUserDetails, Map.of(), TenantSCMSettings.builder()
                        .commentDensitySmall(null)
                        .commentDensityMedium(null)
                        .build()
        );
        Assertions.assertThat(featureResponse).isNotNull();
        Assertions.assertThat(featureResponse.getMean()).isNotNull();
        resultDbListResponse = scmAggService.groupByAndCalculatePrs(company, ScmPrFilter.builder()
                .approvers(List.of(userId1))
                .integrationIds(List.of(gitHubIntegrationId))
                .across(ScmPrFilter.DISTINCT.comment_density)
                .build(), null);
        actualMean = resultDbListResponse.getRecords().stream().mapToDouble(DbAggregationResult::getTotalComments).sum() /
                resultDbListResponse.getRecords().stream().mapToDouble(DbAggregationResult::getCount).sum();
        Assertions.assertThat(featureResponse.getMean().doubleValue()).isEqualTo(actualMean);

        FeatureBreakDown breakDown = devPRReviewDepthHandler.getBreakDown(company, feature, Map.of(), devProductivityFilter, orgUserDetails, Map.of(),TenantSCMSettings.builder().commentDensitySmall(null).commentDensityMedium(null).build(), Map.of(), 0, 100);
        Assertions.assertThat(breakDown).isNotNull();
        Assertions.assertThat(breakDown.getRecords().size()).isEqualTo(1);
    }
}
