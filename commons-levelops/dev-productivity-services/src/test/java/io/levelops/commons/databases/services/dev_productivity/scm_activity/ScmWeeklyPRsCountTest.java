package io.levelops.commons.databases.services.dev_productivity.scm_activity;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.dev_productivity.IntegrationUserDetails;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmReview;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.*;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.models.IntegrationType;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.ScmQueryUtils.ARRAY_UNIQ;

public class ScmWeeklyPRsCountTest {

    private static final String company = "test";
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    private static IntegrationService integrationService;
    private static ScmAggService scmAggService;
    private static UserIdentityService userIdentityService;
    private static final ObjectMapper m = DefaultObjectMapper.get();

    private static String gitHubIntegrationId;
    private static IntegrationUserDetails integrationUserDetails;
    private static NamedParameterJdbcTemplate jdbcTemplate;

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

        UUID creatorId = UUID.randomUUID();
        UUID reviwerId = UUID.randomUUID();


        UUID prId1 = UUID.randomUUID();
        UUID prId2 = UUID.randomUUID();
        UUID prId3 = UUID.randomUUID();
        UUID prId4 = UUID.randomUUID();

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

        DbScmPullRequest pr1 = getPR(prId1.toString(), "common-levelops","10", creator, List.of(reviewer1, reviewer2), 1643673600l, 1643673600l);
        DbScmPullRequest pr2 = getPR(prId2.toString(), "devops-levelops","11", creator, List.of(reviewer3, reviewer4, reviewer5), 1643673600l, null);
        DbScmPullRequest pr3 = getPR(prId3.toString(), "common-levelops","12", creator, List.of(reviewer6, reviewer7, reviewer8), null, 1643673600l);
        DbScmPullRequest pr4 = getPR(prId4.toString(), "ingestion-levelops", "13", creator, List.of(reviewer9), null, 1643673600l);

        scmAggService.insert(company,pr1);
        scmAggService.insert(company,pr2);
        scmAggService.insert(company,pr3);
        scmAggService.insert(company,pr4);

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
                .prCreatedAt(1640995200l)
                .prMergedAt(mergedAt)
                .prClosedAt(closedAt)
                .prUpdatedAt(1640995200l)
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
    public void test() throws SQLException {

        String sql = "SELECT id from test.integration_users where cloud_id = 'ashish-propelo' ";
        UUID reviewerId = jdbcTemplate.queryForObject(sql, Map.of(), UUID.class);

        integrationUserDetails = IntegrationUserDetails.builder()
                .integrationUserId(reviewerId)
                .integrationType(IntegrationType.GITHUB)
                .integrationId(1)
                .cloudId("ashish-propelo")
                .displayName("ashish-propelo")
                .build();

        ScmPrFilter filter = ScmPrFilter.builder()
                .calculation(ScmPrFilter.CALCULATION.count)
                .aggInterval(AGG_INTERVAL.day_of_week)
                .integrationIds(List.of("1"))
                .across(ScmPrFilter.DISTINCT.commenter)
                .reviewerIds(List.of(String.valueOf(integrationUserDetails.getIntegrationUserId())))
                .prReviewedRange(ImmutablePair.of(1640908800l, 1643673600l))
                .build();


        List<DbAggregationResult> activity = scmAggService.groupByAndCalculatePrReviews(company, filter, null).getRecords();
        Assertions.assertThat(activity.size()).isEqualTo(4);
        Assertions.assertThat(activity.stream().mapToLong(r -> r.getCount()).sum()).isEqualTo(9);
        Assertions.assertThat(activity.stream().map(r -> r.getAdditionalKey()).collect(Collectors.toList())).containsExactlyInAnyOrder("Monday", "Tuesday", "Wednesday", "Friday");

        filter = ScmPrFilter.builder()
                .calculation(ScmPrFilter.CALCULATION.count)
                .aggInterval(AGG_INTERVAL.day_of_week)
                .integrationIds(List.of("1"))
                .across(ScmPrFilter.DISTINCT.repo_id)
                .reviewerIds(List.of(String.valueOf(integrationUserDetails.getIntegrationUserId())))
                .prReviewedRange(ImmutablePair.of(1640908800l, 1643673600l))
                .build();

        activity = scmAggService.groupByAndCalculatePrReviews(company, filter, null).getRecords();
        Assertions.assertThat(activity.size()).isEqualTo(7);
        Assertions.assertThat(activity.stream().mapToLong(r -> r.getCount()).sum()).isEqualTo(9);
        Assertions.assertThat(activity.stream().map(r -> r.getAdditionalKey()).collect(Collectors.toList())).containsExactlyInAnyOrder("Friday", "Monday", "Wednesday", "Monday", "Tuesday", "Wednesday", "Friday");
        Assertions.assertThat(activity.stream().map(r -> r.getKey()).collect(Collectors.toList())).containsExactlyInAnyOrder("common-levelops", "devops-levelops", "ingestion-levelops", "common-levelops", "common-levelops", "devops-levelops", "devops-levelops");


    }
}
