package io.levelops.commons.faceted_search.db.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.dev_productivity.IntegrationUserDetails;
import io.levelops.commons.databases.models.database.scm.DbScmPRLabel;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmReview;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.GitRepositoryService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.faceted_search.db.models.ScmPROrCommitJiraWIMapping;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import java.time.Instant;

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
import java.util.Set;
import java.util.UUID;

import static io.levelops.commons.databases.services.ScmQueryUtils.ARRAY_UNIQ;

public class ScmPRDBServiceTest {

    private static final String company = "test";
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    private static IntegrationService integrationService;
    private static ScmAggService scmAggService;
    private static ScmPRDBService scmPRDBService;
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

        Instant i = Instant.ofEpochSecond(1641254400l, 0);
        DbScmPRLabel label1 = getDbScmPrLabel(prId1, i);
        DbScmPRLabel label2 = getDbScmPrLabel(prId2, i);
        DbScmPRLabel label3 = getDbScmPrLabel(prId3, i);
        DbScmPRLabel label4 = getDbScmPrLabel(prId4, i);

        DbScmPullRequest pr1 = getPR(prId1.toString(), "common-levelops","10", creator, List.of(reviewer1, reviewer2), 1639673600l,1643673600l, 1643673600l, label1, List.of("LEV-123"), 1643673600l);
        DbScmPullRequest pr2 = getPR(prId2.toString(), "devops-levelops","11", creator, List.of(reviewer3, reviewer4, reviewer5), 1640673600l,1643673600l, null, label2, List.of("LEV-456", "LEV-789"), 1643673600l);
        DbScmPullRequest pr3 = getPR(prId3.toString(), "common-levelops","12", creator, List.of(reviewer6, reviewer7, reviewer8), 1641673600l,null, 1643673600l, label3, List.of("LEV-910"), 1651662897l);
        DbScmPullRequest pr4 = getPR(prId4.toString(), "ingestion-levelops", "13", creator, List.of(reviewer9), 1642673600l,null, 1643673600l, label4, List.of(), 1651662897l);

        scmAggService.insert(company,pr1);
        scmAggService.insert(company,pr2);
        scmAggService.insert(company,pr3);
        scmAggService.insert(company,pr4);

        scmPRDBService = new ScmPRDBService(dataSource, scmAggService);
    }

    private static DbScmPRLabel getDbScmPrLabel(UUID prId, Instant i) {
        return DbScmPRLabel.builder()
                .scmPullRequestId(prId)
                .cloudId("dummy-id")
                .description("dummy-desc")
                .labelAddedAt(i)
                .name("dummy-label")
                .build();
    }

    private static DbScmPullRequest getPR(String prId, String repo, String number, DbScmUser creator, List<DbScmReview> reviewer, Long createdAt, Long mergedAt, Long closedAt, DbScmPRLabel label,
                                          List<String> issueKey, Long updatedAt) {
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
                .prCreatedAt(createdAt)
                .prMergedAt(mergedAt)
                .prClosedAt(closedAt)
                .prUpdatedAt(updatedAt)
                .reviews(reviewer)
                .prLabels(List.of(label))
                .issueKeys(issueKey)
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
    public void testGetScmPRJiraWIMappings(){

        List<ScmPROrCommitJiraWIMapping> list = scmPRDBService.getScmPRJiraWIMappings(company, null, null, null, true);
        Assertions.assertThat(list.size()).isEqualTo(3);
        list = scmPRDBService.getScmPRJiraWIMappings(company, Set.of("LEV-910"), null,  null,true);
        Assertions.assertThat(list.size()).isEqualTo(1);
        Assertions.assertThat(list.get(0).getWorkItemIds()).containsExactlyInAnyOrder("LEV-910");
        list = scmPRDBService.getScmPRJiraWIMappings(company, null, Set.of("11"), null,true);
        Assertions.assertThat(list.size()).isEqualTo(1);
        Assertions.assertThat(list.get(0).getWorkItemIds()).containsExactlyInAnyOrder("LEV-456","LEV-789");
        list = scmPRDBService.getScmPRJiraWIMappings(company, Set.of("LEV-123"), null, null,true);
        Assertions.assertThat(list.size()).isEqualTo(1);
        Assertions.assertThat(list.get(0).getWorkItemIds()).containsExactlyInAnyOrder("LEV-123");
    }

    @Test
    public void testListPrsAndLabels(){

        ScmPrFilter filter = ScmPrFilter.builder()
                .build();
        List<DbScmPullRequest> list = scmPRDBService.listPRsWithoutEnrichment(company, filter, 0,10);
        Assertions.assertThat(list.size()).isEqualTo(4);
        Assertions.assertThat(list.stream().map(p -> p.getNumber())).containsExactlyInAnyOrder("10","11","12", "13");
        Assertions.assertThat(list.get(0).getNumber()).isEqualTo("10");
        Assertions.assertThat(list.get(0).getRepoIds().get(0)).isEqualTo("common-levelops");
        Assertions.assertThat(list.get(1).getNumber()).isEqualTo("11");
        Assertions.assertThat(list.get(1).getRepoIds().get(0)).isEqualTo("devops-levelops");
        Assertions.assertThat(list.get(2).getNumber()).isEqualTo("12");
        Assertions.assertThat(list.get(2).getRepoIds().get(0)).isEqualTo("common-levelops");
        Assertions.assertThat(list.get(3).getNumber()).isEqualTo("13");
        Assertions.assertThat(list.get(3).getRepoIds().get(0)).isEqualTo("ingestion-levelops");
        Map<UUID, List<DbScmPRLabel>>  labelMap = scmPRDBService.listLabels(company, list);
        Assertions.assertThat(labelMap.size()).isEqualTo(4);
    }

    @Test
    public void test() throws IOException {

        String resource = ResourceUtils.getResourceAsString("test_resource/scm_prs.json");
        List<DbScmPullRequest> actual = mapper.readValue(resource,mapper.getTypeFactory().constructCollectionType(List.class, DbScmPullRequest.class));
        ScmPrFilter filter = ScmPrFilter.builder().build();
        List<DbScmPullRequest>  list = scmPRDBService.readFromDB(company, filter, 0, 1000, true, false);
        Assertions.assertThat(list).isNotNull();
        Assertions.assertThat(list.size()).isEqualTo(actual.size());
        Assertions.assertThat(list.get(0).getRepoIds()).isEqualTo(actual.get(0).getRepoIds());
        Assertions.assertThat(list.get(0).getNumber()).isEqualTo(actual.get(0).getNumber());
        Assertions.assertThat(list.get(1).getRepoIds()).isEqualTo(actual.get(1).getRepoIds());
        Assertions.assertThat(list.get(1).getNumber()).isEqualTo(actual.get(1).getNumber());
        Assertions.assertThat(list.get(2).getRepoIds()).isEqualTo(actual.get(2).getRepoIds());
        Assertions.assertThat(list.get(2).getNumber()).isEqualTo(actual.get(2).getNumber());
        Assertions.assertThat(list.get(3).getRepoIds()).isEqualTo(actual.get(3).getRepoIds());
        Assertions.assertThat(list.get(3).getNumber()).isEqualTo(actual.get(3).getNumber());
    }
}
