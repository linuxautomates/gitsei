package io.levelops.commons.databases.services.dev_productivity.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.models.database.dev_productivity.FeatureBreakDown;
import io.levelops.commons.databases.models.database.dev_productivity.FeatureResponse;
import io.levelops.commons.databases.models.database.dev_productivity.IntegrationUserDetails;
import io.levelops.commons.databases.models.database.dev_productivity.OrgUserDetails;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmIssue;
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
import java.util.Set;
import java.util.UUID;

import static io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile.FeatureType.AVG_CODING_DAYS_PER_WEEK;
import static io.levelops.commons.databases.services.ScmQueryUtils.ARRAY_UNIQ;
import static org.assertj.core.api.Assertions.assertThat;

public class AvgCodingDaysPerWeekHandlerTest {

    private static final String company = "test";
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    private static final Integer SECTION_ORDER = 0;
    private static OrgVersionsDatabaseService versionsService;
    private static OrgUsersDatabaseService usersService;
    private static IntegrationService integrationService;
    private static AvgCodingDaysPerWeekHandler avgCodingDaysPerWeekHandler;
    private static ScmAggService scmAggService;
    private static UserIdentityService userIdentityService;
    private static final ObjectMapper m = DefaultObjectMapper.get();
    private static Date currentTime;
    private static String integrationId;
    private static Integration integration1;

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;


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
        integration1 = Integration.builder()
                .application("github")
                .name("github test")
                .status("enabled")
                .build();
        integrationId = integrationService.insert(company, integration1);
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        repositoryService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);

        String input = ResourceUtils.getResourceAsString("json/databases/githubprs.json");
        PaginatedResponse<GithubRepository> prs = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        currentTime = new Date();
        List<DbRepository> repos = new ArrayList<>();

        input = ResourceUtils.getResourceAsString("json/databases/github_issues.json");
        PaginatedResponse<GithubRepository> issues = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        currentTime = new Date();
        issues.getResponse().getRecords().forEach(repo -> {
            repos.add(DbRepository.fromGithubRepository(repo, "1"));
            repo.getIssues()
                    .forEach(issue -> {
                        DbScmIssue tmp = DbScmIssue
                                .fromGithubIssue(issue, repo.getId(), String.valueOf(integrationId));
                        if (scmAggService.getIssue(company, tmp.getIssueId(), tmp.getRepoId(),
                                tmp.getIntegrationId()).isEmpty())
                            scmAggService.insertIssue(company, tmp);
                    });
        });

        input = ResourceUtils.getResourceAsString("json/databases/githubcommits.json");
        PaginatedResponse<GithubRepository> commits = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        currentTime = DateUtils.truncate(new Date(), Calendar.DATE);
        commits.getResponse().getRecords().forEach(repo -> {
            repos.add(DbRepository.fromGithubRepository(repo, String.valueOf(integrationId)));
            repo.getEvents().stream()
                    .filter(ev -> "PushEvent".equals(ev.getType()))
                    .flatMap(ev -> ev.getCommits().stream())
                    .forEach(commit -> {
                        DbScmCommit tmp = DbScmCommit
                                .fromGithubCommit(commit, repo.getId(), String.valueOf(integrationId),
                                        currentTime.toInstant().getEpochSecond(), 0L);
                        if (scmAggService.getCommit(company, tmp.getCommitSha(), tmp.getRepoIds(),
                                tmp.getIntegrationId()).isEmpty()) {
                            try {
                                scmAggService.insertCommit(company, tmp);
                            } catch (SQLException e) {
                                throw new RuntimeStreamException(e);
                            }
                            DbScmFile.fromGithubCommit(
                                    commit, repo.getId(), String.valueOf(integrationId), currentTime.toInstant().getEpochSecond())
                                    .forEach(scmFile -> scmAggService.insertFile(company, scmFile));
                        }
                    });
        });
        repositoryService.batchUpsert(company, repos).size();

        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);

        versionsService = new OrgVersionsDatabaseService(dataSource);
        versionsService.ensureTableExistence(company);

        usersService = new OrgUsersDatabaseService(dataSource, mapper, versionsService, userIdentityService);
        usersService.ensureTableExistence(company);

        scmAggService = new ScmAggService(dataSource, userIdentityService);
        avgCodingDaysPerWeekHandler = new AvgCodingDaysPerWeekHandler(scmAggService, null, List.of("test"));
    }

    @Test
    public void testAvgCodingDays() throws SQLException, IOException {

        var orgUser1 = DBOrgUser.builder()
                .email("email1")
                .fullName("meghana-levelops")
                .customFields(Map.of("test_name", "test1"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("meghana-levelops").username("meghana-levelops")
                        .integrationType(integration1.getApplication())
                        .integrationId(Integer.parseInt(integrationId)).build()))
                .versions(Set.of(1))
                .build();

        String userId1 = userIdentityService.getUser(company, String.valueOf(integrationId), "meghana-levelops");//id
        DbScmUser user1 = userIdentityService.get(company, userId1).orElse(null);


        IntegrationUserDetails integrationUserDetails1 = IntegrationUserDetails.builder()
                .integrationId(Integer.parseInt(integrationId))
                .integrationType(IntegrationType.GITHUB)
                .integrationUserId(UUID.fromString(user1.getId()))
                .cloudId(userId1)
                .displayName(userId1)
                .build();

        OrgUserDetails orgUserDetails1 = OrgUserDetails.builder()
                .orgUserId(orgUser1.getId())
                .fullName(orgUser1.getFullName())
                .email(orgUser1.getEmail())
                .IntegrationUserDetailsList(List.of(integrationUserDetails1)).build();


        DevProductivityProfile.Feature feature1 = DevProductivityProfile.Feature.builder()
                .featureType(AVG_CODING_DAYS_PER_WEEK).maxValue(100L).lowerLimitPercentage(25).upperLimitPercentage(75).build();
        ImmutablePair<Long, Long> committedRange = ImmutablePair.of(0L, Instant.now().toEpochMilli());

        DevProductivityFilter filter1 = DevProductivityFilter.builder()
                .timeRange(committedRange).build();

        FeatureResponse featureResponse1 = avgCodingDaysPerWeekHandler.calculateFeature(company, SECTION_ORDER, feature1, Map.of(), filter1, orgUserDetails1, Map.of(), null);

        Assert.assertNotNull(featureResponse1);
        assertThat(featureResponse1.getMean()).isEqualTo(0.0d);

        var orgUser2 = DBOrgUser.builder()
                .email("email1")
                .fullName("web-flow")
                .customFields(Map.of("test_name", "test1"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("web-flow").username("web-flow")
                        .integrationType(integration1.getApplication())
                        .integrationId(Integer.parseInt(integrationId)).build()))
                .versions(Set.of(1))
                .build();

        String userId2 = userIdentityService.getUser(company, String.valueOf(integrationId), "web-flow");
        DbScmUser user2 = userIdentityService.get(company, userId1).orElse(null);

        IntegrationUserDetails integrationUserDetails2 = IntegrationUserDetails.builder()
                .integrationId(Integer.parseInt(integrationId))
                .integrationType(IntegrationType.GITHUB)
                .integrationUserId(UUID.fromString(user2.getId()))
                .cloudId(userId2)
                .displayName(userId2)
                .build();

        OrgUserDetails orgUserDetails2 = OrgUserDetails.builder()
                .orgUserId(orgUser2.getId())
                .fullName(orgUser2.getFullName())
                .email(orgUser2.getEmail())
                .IntegrationUserDetailsList(List.of(integrationUserDetails2)).build();

        DevProductivityProfile.Feature feature2 = DevProductivityProfile.Feature.builder()
                .featureType(AVG_CODING_DAYS_PER_WEEK).maxValue(100L).lowerLimitPercentage(25).upperLimitPercentage(75).build();
        ImmutablePair<Long, Long> committedRange2 = ImmutablePair.of(Instant.now().toEpochMilli(), Instant.now().toEpochMilli());
        DevProductivityFilter filter2 = DevProductivityFilter.builder()
                .timeRange(committedRange2).build();

        FeatureResponse featureResponse2 = avgCodingDaysPerWeekHandler.calculateFeature(company, SECTION_ORDER, feature2, Map.of(), filter2, orgUserDetails2, Map.of(), null);
        Assert.assertNotNull(featureResponse2);
        assertThat(featureResponse2.getMean()).isEqualTo(0.0d);

        FeatureBreakDown breakDown = avgCodingDaysPerWeekHandler.getBreakDown(company,feature1, Map.of(), filter1,orgUserDetails1,Map.of(),null,Map.of(), 0,100);
        Assert.assertNotNull(breakDown);
        assertThat(breakDown.getRecords().size()).isEqualTo(100);
    }
}