package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmPRLabel;
import io.levelops.commons.databases.models.database.scm.DbScmPRLabelLite;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.VCS_TYPE;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.NumberUtils;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static io.levelops.commons.databases.services.ScmAggServiceTest.validatePrs;
import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;

@SuppressWarnings("unused")
public class ScmAggService2Test {
    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    private static final Long NOW = Instant.now().getEpochSecond();


    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static ScmAggService scmAggService;
    private static JiraIssueService jiraIssueService;
    private static ZendeskTicketService zendeskTicketService;
    private static ScmJiraZendeskService scmJiraZendeskService;
    private static String gitHubIntegrationId;
    private static ZendeskFieldService zendeskFieldService;
    private static UserIdentityService userIdentityService;
    private static TeamMembersDatabaseService teamMembersDatabaseService;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;

        dataSource =  DatabaseTestUtils.setUpDataSource(pg, company);
        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, company);
        jiraIssueService = jiraTestDbs.getJiraIssueService();

        JiraFieldService jiraFieldService = jiraTestDbs.getJiraFieldService();
        JiraProjectService jiraProjectService = jiraTestDbs.getJiraProjectService();
        IntegrationService integrationService = jiraTestDbs.getIntegrationService();

        userIdentityService = jiraTestDbs.getUserIdentityService();
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, m);
        zendeskTicketService = new ZendeskTicketService(dataSource, integrationService, zendeskFieldService);
        scmJiraZendeskService = new ScmJiraZendeskService(dataSource, scmAggService, jiraTestDbs.getJiraConditionsBuilder(), zendeskTicketService);

        GitRepositoryService repositoryService = new GitRepositoryService(dataSource);

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        gitHubIntegrationId = integrationService.insert(company, Integration.builder()
                .application("github")
                .name("github test")
                .status("enabled")
                .build());
        integrationService.insert(company, Integration.builder()
                .application("jira")
                .name("jira test")
                .status("enabled")
                .build());
        integrationService.insert(company, Integration.builder()
                .application("zendesk")
                .name("zendesk_test")
                .status("enabled")
                .build());
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);
        jiraIssueService.ensureTableExistence(company);
        zendeskTicketService.ensureTableExistence(company);
        repositoryService.ensureTableExistence(company);
        dataSource.getConnection().prepareStatement(arrayUniq)
                .execute();
    }

    private static DbScmPullRequest createPr(int i, List<String> repoIds) {
        Instant in = Instant.now();
        Long now = in.getEpochSecond();
        return DbScmPullRequest.builder()
                .repoIds(repoIds)
                .project(repoIds.stream().findFirst().orElse(""))
                .number(String.valueOf(i))
                .integrationId(gitHubIntegrationId)
                .creator("viraj" + i)
                .creatorInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("viraj" + i).displayName("viraj" + i).originalDisplayName("viraj" + i).build())
                .mergeSha("mergeSha" + i)
                .title("title" + i)
                .sourceBranch("main" + i)
                .state("merged")
                .merged(true)
                .assignees(List.of("user1", "user2"))
                .assigneesInfo(List.of(DbScmUser.builder().cloudId("user1").displayName("user1").originalDisplayName("user1").integrationId(gitHubIntegrationId).build(),
                        DbScmUser.builder().cloudId("user2").displayName("user2").originalDisplayName("user2").integrationId(gitHubIntegrationId).build()))
                .labels(List.of("lbl1", "lbl2"))
                .prLabels(List.of(DbScmPRLabel.builder().cloudId("c1").name("lbl1").labelAddedAt(in).build(), DbScmPRLabel.builder().cloudId("c2").name("tbl2").labelAddedAt(in).build()))
                .commitShas(List.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                .reviews(List.of())
                .prCreatedAt(now).prUpdatedAt(now).prMergedAt(now).prClosedAt(now)
                .createdAt(now)
                .build();
    }

    private static DbScmPullRequest createAndInsertPr(ScmAggService scmAggService, String company, int i, List<String> repoIds) throws SQLException {
        DbScmPullRequest pr = createPr(i, repoIds);
        String id = scmAggService.insert(company, pr);
        Assert.assertNotNull(id);
        pr = pr.toBuilder().id(id).build();
        return pr;
    }

    @Test
    public void testPrs() throws SQLException {
        DbScmPullRequest pr1 = createAndInsertPr(scmAggService, company, 0, List.of("dummy"));
        DbScmPullRequest pr2a = createAndInsertPr(scmAggService, company, 1, List.of("unknown"));
        DbScmPullRequest pr2b = createAndInsertPr(scmAggService, company, 1, List.of("repo1", "repo2"));
        DbScmPullRequest pr3a = createAndInsertPr(scmAggService, company, 2, List.of("unknown"));
        DbScmPullRequest pr3b = createAndInsertPr(scmAggService, company, 2, List.of("repo3"));
        DbScmPullRequest pr4 = createAndInsertPr(scmAggService, company, 0, List.of("sandbox"));

        List<DbScmPullRequest> expected = List.of(pr1, pr2a, pr2b, pr3a, pr3b, pr4);
        List<DbScmPullRequest> actual = scmAggService.list(company, 0, 1000).getRecords();
        validatePrs(expected, actual);

        scmAggService.delete(company, "1", "unknown", "unknown", gitHubIntegrationId);
        scmAggService.delete(company, "2", "unknown", "unknown", gitHubIntegrationId);

        expected = List.of(pr1, pr2b, pr3b, pr4);
        actual = scmAggService.list(company, 0, 1000).getRecords();
        validatePrs(expected, actual);
    }

    @Test
    public void testPrLabels() throws SQLException {
        Instant in = Instant.now();
        DbListResponse<DbScmPRLabel> listResponse = null;
        DbListResponse<DbScmPRLabelLite> listResponseLite = null;
        UUID prId1 = null;

        DbScmPullRequest pr1 = createAndInsertPr(scmAggService, company, 0, List.of("dummy"));
        DbScmPullRequest pr2 = createAndInsertPr(scmAggService, company, 1, List.of("repo1", "repo2"));
        DbScmPullRequest pr3 = createAndInsertPr(scmAggService, company, 2, List.of("repo3"));
        DbScmPullRequest pr4 = createAndInsertPr(scmAggService, company, 0, List.of("sandbox"));

        prId1 = UUID.fromString(pr1.getId());
        listResponse = scmAggService.listPRLabelsByFilter(company, 0, 100, null, List.of(prId1), null);
        Assert.assertEquals(2, listResponse.getRecords().size());
        listResponse = scmAggService.listPRLabelsByFilter(company, 0, 100, List.of(NumberUtils.toInteger(gitHubIntegrationId)), null, null);
        Assert.assertEquals(8, listResponse.getRecords().size());
        listResponse = scmAggService.listPRLabelsByFilter(company, 0, 100, null, null, Map.of("name", Map.of("$contains", "invalid")));
        Assert.assertEquals(0, listResponse.getRecords().size());
        listResponse = scmAggService.listPRLabelsByFilter(company, 0, 100, null, null, Map.of("name", Map.of("$contains", "bl")));
        Assert.assertEquals(8, listResponse.getRecords().size());
        listResponse = scmAggService.listPRLabelsByFilter(company, 0, 100, null, null, Map.of("name", Map.of("$begins", "lbl")));
        Assert.assertEquals(4, listResponse.getRecords().size());

        // test get prLabels based on company and prRowId
        List<DbScmPRLabel> prLabels = scmAggService.getPrLabels(company, pr1.getId());
        Assert.assertEquals(2, prLabels.size());
        prLabels = scmAggService.getPrLabels(company, pr2.getId());
        Assert.assertEquals(2, prLabels.size());
        prLabels = scmAggService.getPrLabels(company, pr3.getId());
        Assert.assertEquals(2, prLabels.size());

        listResponseLite = scmAggService.listUniquePRLabelsByFilter(company, 0, 100, null, List.of(prId1), null, Map.of("name", SortingOrder.ASC));
        Assert.assertEquals(2, listResponseLite.getRecords().size());
        listResponseLite = scmAggService.listUniquePRLabelsByFilter(company, 0, 100, List.of(NumberUtils.toInteger(gitHubIntegrationId)), null, null, Map.of("name", SortingOrder.ASC));
        Assert.assertEquals(2, listResponseLite.getRecords().size());
        listResponseLite = scmAggService.listUniquePRLabelsByFilter(company, 0, 100, null, null, Map.of("name", Map.of("$contains", "invalid")), Map.of("name", SortingOrder.ASC));
        Assert.assertEquals(0, listResponseLite.getRecords().size());
        listResponseLite = scmAggService.listUniquePRLabelsByFilter(company, 0, 100, null, null, Map.of("name", Map.of("$contains", "bl")), Map.of("name", SortingOrder.ASC));
        Assert.assertEquals(2, listResponseLite.getRecords().size());
        listResponseLite = scmAggService.listUniquePRLabelsByFilter(company, 0, 100, null, null, Map.of("name", Map.of("$contains", "lbl")), Map.of("name", SortingOrder.ASC));
        Assert.assertEquals(1, listResponseLite.getRecords().size());


        pr1 = pr1.toBuilder()
                .labels(List.of("lbl1"))
                .prLabels(List.of(DbScmPRLabel.builder().cloudId("c1").name("lbl1").labelAddedAt(in).build()))
                .build();
        scmAggService.insert(company, pr1);
        listResponse = scmAggService.listPRLabelsByFilter(company, 0, 100, null, List.of(prId1), null);
        Assert.assertEquals(1, listResponse.getRecords().size());
        listResponse = scmAggService.listPRLabelsByFilter(company, 0, 100, List.of(NumberUtils.toInteger(gitHubIntegrationId)), null, null);
        Assert.assertEquals(7, listResponse.getRecords().size());
        listResponse = scmAggService.listPRLabelsByFilter(company, 0, 100, null, null, Map.of("name", Map.of("$contains", "bl")));
        Assert.assertEquals(7, listResponse.getRecords().size());
        listResponse = scmAggService.listPRLabelsByFilter(company, 0, 100, null, null, Map.of("name", Map.of("$begins", "lbl")));
        Assert.assertEquals(4, listResponse.getRecords().size());

        listResponseLite = scmAggService.listUniquePRLabelsByFilter(company, 0, 100, null, List.of(prId1), null, Map.of("name", SortingOrder.ASC));
        Assert.assertEquals(1, listResponseLite.getRecords().size());
        listResponseLite = scmAggService.listUniquePRLabelsByFilter(company, 0, 100, List.of(NumberUtils.toInteger(gitHubIntegrationId)), null, null, Map.of("name", SortingOrder.ASC));
        Assert.assertEquals(2, listResponseLite.getRecords().size());
        listResponseLite = scmAggService.listUniquePRLabelsByFilter(company, 0, 100, null, null, Map.of("name", Map.of("$contains", "invalid")), Map.of("name", SortingOrder.ASC));
        Assert.assertEquals(0, listResponseLite.getRecords().size());
        listResponseLite = scmAggService.listUniquePRLabelsByFilter(company, 0, 100, null, null, Map.of("name", Map.of("$contains", "bl")), Map.of("name", SortingOrder.ASC));
        Assert.assertEquals(2, listResponseLite.getRecords().size());
        listResponseLite = scmAggService.listUniquePRLabelsByFilter(company, 0, 100, null, null, Map.of("name", Map.of("$contains", "lbl")), Map.of("name", SortingOrder.ASC));
        Assert.assertEquals(1, listResponseLite.getRecords().size());

        pr1 = pr1.toBuilder()
                .labels(List.of("lbl1"))
                .prLabels(List.of(DbScmPRLabel.builder().cloudId("c1").name("qbl1a").labelAddedAt(in).build()))
                .build();
        scmAggService.insert(company, pr1);
        listResponse = scmAggService.listPRLabelsByFilter(company, 0, 100, null, List.of(prId1), null);
        Assert.assertEquals(1, listResponse.getRecords().size());
        listResponse = scmAggService.listPRLabelsByFilter(company, 0, 100, List.of(NumberUtils.toInteger(gitHubIntegrationId)), null, null);
        Assert.assertEquals(7, listResponse.getRecords().size());
        listResponse = scmAggService.listPRLabelsByFilter(company, 0, 100, null, null, Map.of("name", Map.of("$contains", "bl")));
        Assert.assertEquals(7, listResponse.getRecords().size());
        listResponse = scmAggService.listPRLabelsByFilter(company, 0, 100, null, null, Map.of("name", Map.of("$begins", "lbl")));
        Assert.assertEquals(3, listResponse.getRecords().size());
        listResponse = scmAggService.listPRLabelsByFilter(company, 0, 100, null, null, Map.of("name", Map.of("$begins", "qbl")));
        Assert.assertEquals(1, listResponse.getRecords().size());

        listResponseLite = scmAggService.listUniquePRLabelsByFilter(company, 0, 100, null, List.of(prId1), null, Map.of("name", SortingOrder.ASC));
        Assert.assertEquals(1, listResponseLite.getRecords().size());
        listResponseLite = scmAggService.listUniquePRLabelsByFilter(company, 0, 100, List.of(NumberUtils.toInteger(gitHubIntegrationId)), null, null, Map.of("name", SortingOrder.ASC));
        Assert.assertEquals(3, listResponseLite.getRecords().size());
        listResponseLite = scmAggService.listUniquePRLabelsByFilter(company, 0, 100, null, null, Map.of("name", Map.of("$contains", "invalid")), Map.of("name", SortingOrder.ASC));
        Assert.assertEquals(0, listResponseLite.getRecords().size());
        listResponseLite = scmAggService.listUniquePRLabelsByFilter(company, 0, 100, null, null, Map.of("name", Map.of("$contains", "bl")), Map.of("name", SortingOrder.ASC));
        Assert.assertEquals(3, listResponseLite.getRecords().size());
        listResponseLite = scmAggService.listUniquePRLabelsByFilter(company, 0, 100, null, null, Map.of("name", Map.of("$contains", "lbl")), Map.of("name", SortingOrder.ASC));
        Assert.assertEquals(1, listResponseLite.getRecords().size());

        pr1 = pr1.toBuilder().labels(Collections.emptyList()).prLabels(Collections.emptyList()).build();
        scmAggService.insert(company, pr1);
        listResponse = scmAggService.listPRLabelsByFilter(company, 0, 100, null, List.of(prId1), null);
        Assert.assertEquals(0, listResponse.getRecords().size());
        listResponse = scmAggService.listPRLabelsByFilter(company, 0, 100, List.of(NumberUtils.toInteger(gitHubIntegrationId)), null, null);
        Assert.assertEquals(6, listResponse.getRecords().size());
        listResponse = scmAggService.listPRLabelsByFilter(company, 0, 100, null, null, Map.of("name", Map.of("$contains", "bl")));
        Assert.assertEquals(6, listResponse.getRecords().size());
        listResponse = scmAggService.listPRLabelsByFilter(company, 0, 100, null, null, Map.of("name", Map.of("$begins", "lbl")));
        Assert.assertEquals(3, listResponse.getRecords().size());
        listResponse = scmAggService.listPRLabelsByFilter(company, 0, 100, null, null, Map.of("name", Map.of("$begins", "qbl")));
        Assert.assertEquals(0, listResponse.getRecords().size());

        listResponseLite = scmAggService.listUniquePRLabelsByFilter(company, 0, 100, null, List.of(prId1), null, Map.of("name", SortingOrder.ASC));
        Assert.assertEquals(0, listResponseLite.getRecords().size());
        listResponseLite = scmAggService.listUniquePRLabelsByFilter(company, 0, 100, List.of(NumberUtils.toInteger(gitHubIntegrationId)), null, null, Map.of("name", SortingOrder.ASC));
        Assert.assertEquals(2, listResponseLite.getRecords().size());
        listResponseLite = scmAggService.listUniquePRLabelsByFilter(company, 0, 100, null, null, Map.of("name", Map.of("$contains", "invalid")), Map.of("name", SortingOrder.ASC));
        Assert.assertEquals(0, listResponseLite.getRecords().size());
        listResponseLite = scmAggService.listUniquePRLabelsByFilter(company, 0, 100, null, null, Map.of("name", Map.of("$contains", "bl")), Map.of("name", SortingOrder.ASC));
        Assert.assertEquals(2, listResponseLite.getRecords().size());
        listResponseLite = scmAggService.listUniquePRLabelsByFilter(company, 0, 100, null, null, Map.of("name", Map.of("$contains", "lbl")), Map.of("name", SortingOrder.ASC));
        Assert.assertEquals(1, listResponseLite.getRecords().size());

    }


    private static DbScmCommit createCommit(DbScmUser scmUser) {
        DbScmCommit commit = DbScmCommit.builder()
                .repoIds(List.of("levelops/ingestion-levelops", "levelops/integrations-levelops")).integrationId(gitHubIntegrationId)
                .project("levelops/ingestion-levelops")
                .committer("viraj-levelops").commitSha("ad4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .committerInfo(scmUser)
                .vcsType(VCS_TYPE.GIT)
                .commitUrl("url")
                .filesCt(1).additions(2).deletions(2).changes(0).author("viraj-levelops")
                .authorInfo(scmUser)
                .committedAt(NOW)
                .createdAt(NOW)
                .ingestedAt(NOW)
                .build();
        return commit;
    }

    private DbScmUser createScmUser() {
        DbScmUser testScmUser = DbScmUser.builder()
                .integrationId(gitHubIntegrationId)
                .cloudId("viraj-levelops")
                .displayName("viraj-levelops")
                .originalDisplayName("viraj-levelops")
                .build();
        return testScmUser;
    }

    @Test
    public void testCommits() throws SQLException {
        DbScmCommit commit = createCommit(createScmUser());
        String commitId = scmAggService.insertCommit(company, commit);
        Assert.assertTrue(StringUtils.isNotBlank(commitId));
        Assert.assertEquals(VCS_TYPE.GIT, commit.getVcsType());
        commit = commit.toBuilder().id(commitId).build();

        Optional<DbScmCommit> opt = scmAggService.getCommit(company, commit.getCommitSha(), commit.getRepoIds(), commit.getIntegrationId());
        Assert.assertNotNull(opt);
        Assert.assertTrue(opt.isPresent());

        int changesUpdated = commit.getChanges() + 10;
        Boolean success = scmAggService.updateCommitChangesCount(company, UUID.fromString(commit.getId()), changesUpdated);
        Assert.assertTrue(success);

        opt = scmAggService.getCommit(company, commit.getCommitSha(), commit.getRepoIds(), commit.getIntegrationId());
        Assert.assertNotNull(opt);
        Assert.assertTrue(opt.isPresent());
        Assert.assertEquals(changesUpdated, opt.get().getChanges().intValue());
        Assert.assertEquals(commit.getIngestedAt(), opt.get().getIngestedAt());

        Long newIngestedAt = commit.getIngestedAt() + 100000L;
        Assert.assertTrue(scmAggService.updateCommitIngestedAt(company, UUID.fromString(commitId), newIngestedAt));
        opt = scmAggService.getCommit(company, commit.getCommitSha(), commit.getRepoIds(), commit.getIntegrationId());
        Assert.assertNotNull(opt);
        Assert.assertTrue(opt.isPresent());
        Assert.assertEquals(newIngestedAt, opt.get().getIngestedAt());
    }
}
