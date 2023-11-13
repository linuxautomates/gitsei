package io.levelops.commons.databases.services.dev_productivity.scm_activity;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.dev_productivity.IntegrationUserDetails;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmReview;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.VCS_TYPE;
import io.levelops.commons.databases.services.*;
import io.levelops.commons.databases.services.dev_productivity.engine.ScmActivitiesEngine;
import io.levelops.commons.databases.services.dev_productivity.filters.ScmActivityFilter;
import io.levelops.commons.databases.services.dev_productivity.models.ScmActivities;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.ScmQueryUtils.ARRAY_UNIQ;

public class ScmActivityEngineTest {

    private static final String company = "test";
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    private static IntegrationService integrationService;
    private static ScmAggService scmAggService;
    private static UserIdentityService userIdentityService;
    private static final ObjectMapper m = DefaultObjectMapper.get();

    private static String gitHubIntegrationId;
    private static IntegrationUserDetails integrationUserDetails;
    private static NamedParameterJdbcTemplate jdbcTemplate;
    public static ScmActivitiesEngine scmActivitiesEngine;

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;


    @BeforeClass
    public static void setup() throws SQLException, IOException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        dataSource.getConnection().prepareStatement(ARRAY_UNIQ).execute();
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
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        repositoryService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        scmActivitiesEngine = new ScmActivitiesEngine(scmAggService);

        UUID creatorId = UUID.randomUUID();
        UUID reviwerId = UUID.randomUUID();


        UUID prId1 = UUID.randomUUID();
        UUID prId2 = UUID.randomUUID();
        UUID prId3 = UUID.randomUUID();
        UUID prId4 = UUID.randomUUID();

        UUID prId5 = UUID.randomUUID();
        UUID prId6 = UUID.randomUUID();
        UUID prId7 = UUID.randomUUID();
        UUID prId8 = UUID.randomUUID();

        DbScmUser creator = DbScmUser.builder()
                .cloudId("ashish-levelops")
                .displayName("ashish-levelops")
                .originalDisplayName("ashish-levelops")
                .id(creatorId.toString())
                .integrationId(gitHubIntegrationId)
                .build();

        DbScmUser reviewerInfo = DbScmUser.builder()
                .cloudId("ashish-propelo")
                .displayName("ashish-propelo")
                .originalDisplayName("ashish-propelo")
                .id(reviwerId.toString())
                .integrationId(gitHubIntegrationId)
                .build();

        DbScmReview reviewer1 = getPrReviews(prId1.toString(), UUID.randomUUID().toString(), reviewerInfo, 1641168000l);
        DbScmReview reviewer2 = getPrReviews(prId1.toString(), UUID.randomUUID().toString(), reviewerInfo, 1641168000l);
        DbScmReview reviewer3 = getPrReviews(prId2.toString(), UUID.randomUUID().toString(), reviewerInfo, 1641168000l);
        DbScmReview reviewer4 = getPrReviews(prId2.toString(), UUID.randomUUID().toString(), reviewerInfo, 1641254400l);
        DbScmReview reviewer5 = getPrReviews(prId2.toString(), UUID.randomUUID().toString(), reviewerInfo, 1641340800l);
        DbScmReview reviewer6 = getPrReviews(prId3.toString(), UUID.randomUUID().toString(), reviewerInfo, 1641340800l);
        DbScmReview reviewer7 = getPrReviews(prId3.toString(), UUID.randomUUID().toString(), reviewerInfo, 1641513600l);
        DbScmReview reviewer8 = getPrReviews(prId3.toString(), UUID.randomUUID().toString(), reviewerInfo, 1641513600l);
        DbScmReview reviewer9 = getPrReviews(prId4.toString(), UUID.randomUUID().toString(), reviewerInfo, 1641513600l);

        DbScmPullRequest pr1 = getPR(prId1.toString(), "common-levelops","10", creator, List.of(reviewer1, reviewer2), 1641855200l, 1641942600l);
        DbScmPullRequest pr2 = getPR(prId2.toString(), "devops-levelops","11", creator, List.of(reviewer3, reviewer4, reviewer5), 1641511600l, null);
        DbScmPullRequest pr3 = getPR(prId3.toString(), "common-levelops","12", creator, List.of(reviewer6, reviewer7, reviewer8), null, 1642030000l);
        DbScmPullRequest pr4 = getPR(prId4.toString(), "ingestion-levelops", "13", creator, List.of(reviewer9), null, 1642114400l);

        DbScmPullRequest pr5 = getPR(prId5.toString(), "common-levelops","14", reviewerInfo, List.of(), 1642616800l, 1642617800l);
        DbScmPullRequest pr6 = getPR(prId6.toString(), "devops-levelops","15", reviewerInfo, List.of(), 1642711600l, null);
        DbScmPullRequest pr7 = getPR(prId7.toString(), "common-levelops","16", reviewerInfo, List.of(), null, 1643011600l);
        DbScmPullRequest pr8 = getPR(prId8.toString(), "ingestion-levelops", "17", reviewerInfo, List.of(), null, 1643011600l);

        scmAggService.insert(company,pr1);
        scmAggService.insert(company,pr2);
        scmAggService.insert(company,pr3);
        scmAggService.insert(company,pr4);
        scmAggService.insert(company,pr5);
        scmAggService.insert(company,pr6);
        scmAggService.insert(company,pr7);
        scmAggService.insert(company,pr8);

        DbScmCommit commit1 = getCommits(creator,1642616800l);
        DbScmCommit commit2 = getCommits(creator,1642030000l);
        DbScmCommit commit3 = getCommits(creator,1642711600l);
        DbScmCommit commit4 = getCommits(creator,1641513600l);
        DbScmCommit commit5 = getCommits(creator,1643011600l);

        scmAggService.insertCommit(company, commit1);
        scmAggService.insertCommit(company, commit2);
        scmAggService.insertCommit(company, commit3);
        scmAggService.insertCommit(company, commit4);
        scmAggService.insertCommit(company, commit5);

    }

    private static DbScmCommit getCommits(DbScmUser creator, long commitedAt) {

        return DbScmCommit.builder()
                .integrationId("1")
                .repoIds(List.of("common-levelops"))
                .project("common")
                .author(creator.getCloudId())
                .commitSha("dummy-commit-sha"+commitedAt)
                .committer(creator.getCloudId())
                .committerId(creator.getId())
                .filesCt(1)
                .additions(100)
                .deletions(20)
                .ingestedAt(Instant.now().getEpochSecond())
                .committedAt(commitedAt)
                .changes(120)
                .authorId(creator.getId())
                .committerId(creator.getId())
                .createdAt(commitedAt)
                .authorInfo(creator)
                .committerInfo(creator)
                .vcsType(VCS_TYPE.GIT)
                .build();
    }

    private static DbScmPullRequest getPR(String prId, String repo, String number, DbScmUser creator, List<DbScmReview> reviewer, Long mergedAt, Long closedAt) {

        return DbScmPullRequest.builder()
                .id(prId)
                .repoIds(List.of(repo))
                .integrationId(gitHubIntegrationId)
                .project("dummy-project")
                .title("dummy-title")
                .state("Open")
                .number(number)
                .creator(creator.getCloudId())
                .creatorInfo(creator)
                .creatorId(creator.getId())
                .merged(false)
                .labels(List.of("dummy-label"))
                .commitShas(List.of("dummy-commitsha"))
                .prCreatedAt(1641080600l)
                .prMergedAt(mergedAt)
                .prClosedAt(closedAt)
                .prUpdatedAt(1640995200l)
                .merged(mergedAt != null ? true : false)
                .reviews(reviewer)
                .build();
    }

    private static DbScmReview getPrReviews(String prId, String reviewId, DbScmUser reviewerInfo, long reviewedAt) {

        return DbScmReview.builder()
                .id(UUID.randomUUID().toString())
                .prId(prId)
                .state("COMMENTED")
                .reviewer("ashish-propelo")
                .reviewId(reviewId)
                .reviewerInfo(reviewerInfo)
                .reviewedAt(reviewedAt)
                .build();
    }

    @Test
    public void test() throws Exception {

        String sql = "SELECT id from test.integration_users ";
        List<UUID> userIds = jdbcTemplate.queryForList(sql, Map.of(), UUID.class);
        List<Integer> integrationIds = List.of(1);
        ImmutablePair<Long,Long> timeRange = ImmutablePair.of(1638316800l, 1643760000l);

        List<ScmActivities> activities = scmActivitiesEngine.calculateScmActivities(company, userIds, integrationIds, ScmActivityFilter.DISTINCT.integration_user, timeRange, 60l , true);

        List<String> userList = activities.stream().map(r -> r.getUserName()).collect(Collectors.toList());
        ScmActivities activity1 = activities.stream().filter(r -> r.getUserName().equals("ashish-levelops")).findAny().get();
        ScmActivities activity2 = activities.stream().filter(r -> r.getUserName().equals("ashish-propelo")).findAny().get();
        List<String> dayOfweek1 = activity1.getActivityDetails().stream().map(  r -> r.getDayOfWeek()).collect(Collectors.toList());
        List<String> dayOfweek2 = activity2.getActivityDetails().stream().map(  r -> r.getDayOfWeek()).collect(Collectors.toList());
        Assertions.assertThat(activities.size()).isEqualTo(2);
        Assertions.assertThat(userList).containsExactlyInAnyOrder("ashish-levelops", "ashish-propelo");
        Assertions.assertThat(activity1.getActivityDetails().size()).isEqualTo(6);
        Assertions.assertThat(activity1.getActivityDetails().size()).isEqualTo(6);
        Assertions.assertThat(dayOfweek1).containsExactlyInAnyOrder("Saturday", "Monday", "Tuesday", "Thursday", "Wednesday", "Friday");
        Assertions.assertThat(dayOfweek2).containsExactlyInAnyOrder("Saturday", "Monday", "Tuesday", "Thursday", "Wednesday", "Friday");

        activities = scmActivitiesEngine.calculateScmActivities(company, userIds, integrationIds, ScmActivityFilter.DISTINCT.repo_id, timeRange, 60l, true );
        userList = activities.stream().map(r -> r.getRepoId()).collect(Collectors.toList());
        activity1 = activities.stream().filter(r -> r.getRepoId().equals("common-levelops")).findAny().get();
        activity2 = activities.stream().filter(r -> r.getRepoId().equals("devops-levelops")).findAny().get();
        dayOfweek1 = activity1.getActivityDetails().stream().map(  r -> r.getDayOfWeek()).collect(Collectors.toList());
        dayOfweek2 = activity2.getActivityDetails().stream().map(  r -> r.getDayOfWeek()).collect(Collectors.toList());
        Assertions.assertThat(activities.size()).isEqualTo(3);
        Assertions.assertThat(userList).containsExactlyInAnyOrder("common-levelops", "devops-levelops", "ingestion-levelops");
        Assertions.assertThat(activity1.getActivityDetails().size()).isEqualTo(6);
        Assertions.assertThat(activity1.getActivityDetails().size()).isEqualTo(6);
        Assertions.assertThat(dayOfweek1).containsExactlyInAnyOrder("Saturday", "Monday", "Tuesday", "Thursday", "Wednesday", "Friday");
        Assertions.assertThat(dayOfweek2).containsExactlyInAnyOrder("Saturday", "Monday", "Tuesday", "Thursday", "Wednesday");

    }
}
