package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmReview;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ScmAggServicePRResponseTest {

    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static DataSource dataSource;
    private static ScmAggService scmAggService;
    private static String gitHubIntegrationId;
    private static UserIdentityService userIdentityService;
    final DbScmUser testScmUser = DbScmUser.builder()
            .integrationId(gitHubIntegrationId)
            .cloudId("viraj-levelops")
            .displayName("viraj-levelops")
            .build();
    private static DbScmPullRequest pr1, pr2, pr3, pr4;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;

        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        IntegrationService integrationService = new IntegrationService(dataSource);
        userIdentityService = new UserIdentityService(dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        gitHubIntegrationId = integrationService.insert(company, Integration.builder()
                .application("github")
                .name("github test")
                .status("enabled")
                .build());

        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);

        DbScmReview scmReview1 = DbScmReview.builder()
                .reviewerInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("1").displayName("review-levelops").originalDisplayName("review-levelops").build())
                .reviewId("543339289").reviewer("review-levelops")
                .state("COMMENTED").reviewedAt(300L)
                .build();

        DbScmReview scmReview2 = DbScmReview.builder()
                .reviewerInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("2").displayName("viraj-levelops").originalDisplayName("viraj-levelops").build())
                .reviewId("543339289").reviewer("viraj-levelops")
                .state("COMMENTED").reviewedAt(600L)
                .build();

        DbScmReview scmReview3 = DbScmReview.builder()
                .reviewerInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("3").displayName("test-levelops").originalDisplayName("test-levelops").build())
                .reviewId("543339289").reviewer("test-levelops")
                .state("COMMENTED").reviewedAt(700L)
                .build();

        DbScmReview scmReview4 = DbScmReview.builder()
                .reviewerInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("4").displayName("test-levelops").originalDisplayName("test-levelops").build())
                .reviewId("543339289").reviewer("test-levelops")
                .state("COMMENTED").reviewedAt(800L)
                .build();

        DbScmReview scmReview5 = DbScmReview.builder()
                .reviewerInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("5").displayName("viraj-levelops").originalDisplayName("viraj-levelops").build())
                .reviewId("543339289").reviewer("viraj-levelops")
                .state("COMMENTED").reviewedAt(1000L)
                .build();

        pr1 = DbScmPullRequest.builder()
                .repoIds(List.of("levelops/commons-levelops"))
                .project("levelops/commons-levelops")
                .number("164")
                .integrationId(gitHubIntegrationId)
                .creator("viraj-levelops").mergeSha("cd4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .creatorInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("viraj-levelops").displayName("viraj-levelops").originalDisplayName("viraj-levelops").build())
                .title("[LEV-1983] ADD: headers to the stellite response sent for the resp call").sourceBranch("lev-1983").state("open").merged(false)
                .assignees(List.of("viraj-levelops")).commitShas(List.of("{934613bf40ceacc18ed59a787a745c49c18f71d9}")).labels(Collections.emptyList())
                .prCreatedAt(100L)
                .prUpdatedAt(100L)
                .prMergedAt(1500L)
                .prClosedAt(1500L)
                .reviews(List.of())
                .build();

        pr2 = pr1.toBuilder().number("165").reviews(List.of(scmReview1)).build();
        pr3 = pr1.toBuilder().number("166").reviews(List.of(scmReview1, scmReview2)).build();
        pr4 = pr1.toBuilder().number("167").reviews(List.of(scmReview1, scmReview5, scmReview3, scmReview2, scmReview4)).build();

        scmAggService.insert(company, pr1);
        scmAggService.insert(company, pr2);
        scmAggService.insert(company, pr3);
        scmAggService.insert(company, pr4);
        dataSource.getConnection().prepareStatement(arrayUniq)
                .execute();
    }

    @Test
    public void testAuthorResponseTime() {
        // case : no reviewer comment
        long authorResponseTime = scmAggService.computeAuthorResponseTime(pr1);
        assertThat(authorResponseTime).isEqualTo(0);

        // case - only comment by reviewer
        authorResponseTime = scmAggService.computeAuthorResponseTime(pr2);
        assertThat(authorResponseTime).isEqualTo(1200);

        // case - comments by reviewer and author
        authorResponseTime = scmAggService.computeAuthorResponseTime(pr3);
        assertThat(authorResponseTime).isEqualTo(300L);

        // case multiple comment by reviewer and author
        authorResponseTime = scmAggService.computeAuthorResponseTime(pr4);
        assertThat(authorResponseTime).isEqualTo(600L);
    }

    @Test
    public void testReviewerResponseTime() {
        // case : no reviewer comment
        long authorResponseTime = scmAggService.computeReviewerResponseTime(pr1);
        assertThat(authorResponseTime).isEqualTo(1400L);

        // case - only comment by reviewer
        authorResponseTime = scmAggService.computeReviewerResponseTime(pr2);
        assertThat(authorResponseTime).isEqualTo(200L);

        // case - comments by reviewer and author
        authorResponseTime = scmAggService.computeReviewerResponseTime(pr3);
        assertThat(authorResponseTime).isEqualTo(1100L);

        authorResponseTime = scmAggService.computeReviewerResponseTime(pr4);
        assertThat(authorResponseTime).isEqualTo(800L);
    }

    @Test
    public void testAuthorResponseTimeReport() throws SQLException {
        List<DbAggregationResult> records = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder().across(ScmPrFilter.DISTINCT.creator)
                        .calculation(ScmPrFilter.CALCULATION.author_response_time).build(), null).getRecords();
        System.out.println(records);
        assertThat(records.size()).isEqualTo(1);
        assertThat(records.get(0).getMin()).isEqualTo(0);
        assertThat(records.get(0).getMax()).isEqualTo(1200);
    }

    @Test
    public void testReviewerResponseTimeReport() throws SQLException {
        List<DbAggregationResult> records = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder().across(ScmPrFilter.DISTINCT.creator)
                        .calculation(ScmPrFilter.CALCULATION.reviewer_response_time).build(), null).getRecords();
        System.out.println(records);
        assertThat(records.size()).isEqualTo(1);
        assertThat(records.get(0).getMin()).isEqualTo(200);
        assertThat(records.get(0).getMax()).isEqualTo(1400);
    }
}