package io.levelops.commons.databases.services.dev_productivity.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.models.database.dev_productivity.FeatureBreakDown;
import io.levelops.commons.databases.models.database.dev_productivity.FeatureResponse;
import io.levelops.commons.databases.models.database.dev_productivity.IntegrationUserDetails;
import io.levelops.commons.databases.models.database.dev_productivity.OrgUserDetails;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.DevProductivityFilter;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.GitRepositoryService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.integrations.github.models.GithubRepository;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile.FeatureType.AVG_PR_CYCLE_TIME;
import static io.levelops.commons.databases.services.ScmQueryUtils.ARRAY_UNIQ;

public class AvgPRCycleTimeHandlerTest {

    private static final String company = "test";
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    private static final Integer SECTION_ORDER = 0;
    private static OrgVersionsDatabaseService versionsService;
    private static OrgUsersDatabaseService usersService;
    private static IntegrationService integrationService;
    private static DataSource dataSource;
    private static AvgPRCycleTimeHandler avgPRCycleTimeHandler;
    private static ScmAggService scmAggService;
    private static UserIdentityService userIdentityService;
    private static final ObjectMapper m = DefaultObjectMapper.get();
    private static Date currentTime;
    private static String gitHubIntegrationId;
    private static String gitHubIntegrationId2;



    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        dataSource.getConnection().prepareStatement(ARRAY_UNIQ)
                .execute();
        integrationService = new IntegrationService(dataSource);
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

        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, m);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        repositoryService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);

        String input = ResourceUtils.getResourceAsString("json/databases/githubprs.json");
        PaginatedResponse<GithubRepository> prs = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        currentTime = new Date();
        List<DbRepository> repos = new ArrayList<>();
        insertPrs(prs, repos, gitHubIntegrationId);
        insertPrs(prs, repos, gitHubIntegrationId2);
        repositoryService.batchUpsert(company, repos);

        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);

        versionsService = new OrgVersionsDatabaseService(dataSource);
        versionsService.ensureTableExistence(company);

        usersService = new OrgUsersDatabaseService(dataSource, mapper, versionsService, userIdentityService);
        usersService.ensureTableExistence(company);

        scmAggService = new ScmAggService(dataSource, userIdentityService);
        avgPRCycleTimeHandler = new AvgPRCycleTimeHandler(scmAggService, null, List.of("test"));
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
    public void testAvgPRCycleTime() throws SQLException, IOException {

        String userId1 = userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops");
        DbScmUser user1 = userIdentityService.get(company, userId1).orElse(null);
        Assert.assertNotNull(user1);
        String userId2 = userIdentityService.getUser(company, gitHubIntegrationId2, "viraj-levelops");
        DbScmUser user2 = userIdentityService.get(company, userId2).orElse(null);
        Assert.assertNotNull(user2);

        UUID orgUserId = UUID.randomUUID();

        OrgUserDetails orgUserDetails1 = OrgUserDetails.builder()
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

        DevProductivityProfile.Feature feature1 = DevProductivityProfile.Feature.builder()
                .featureType(AVG_PR_CYCLE_TIME).maxValue(100L).lowerLimitPercentage(25).upperLimitPercentage(75).build();
        DevProductivityFilter filter1 = DevProductivityFilter.builder().build();

        FeatureResponse featureResponse1 = avgPRCycleTimeHandler.calculateFeature(company, SECTION_ORDER, feature1, Map.of(), filter1, orgUserDetails1, Map.of(), null);

        Assert.assertNotNull(featureResponse1);
        Assert.assertEquals(Double.valueOf("16583.33"), featureResponse1.getMean());

        OrgUserDetails orgUserDetails2 = OrgUserDetails.builder()
                .orgUserId(orgUserId)
                .email("user@levelops.io")
                .fullName("User Name")
                .IntegrationUserDetailsList(List.of(IntegrationUserDetails.builder()
                                .cloudId(user1.getCloudId())
                                .integrationId(Integer.parseInt(gitHubIntegrationId))
                                .integrationType(IntegrationType.GITHUB)
                                .integrationUserId(UUID.fromString(user1.getId()))
                                .displayName(user1.getDisplayName())
                                .build()))
                .build();

        FeatureBreakDown breakDown = avgPRCycleTimeHandler.getBreakDown(company, feature1, Map.of(), filter1, orgUserDetails2, Map.of(), null,Map.of(), 0,100);
        Assert.assertNotNull(breakDown);
        Assertions.assertThat(breakDown.getRecords().size()).isEqualTo(15);

    }
}