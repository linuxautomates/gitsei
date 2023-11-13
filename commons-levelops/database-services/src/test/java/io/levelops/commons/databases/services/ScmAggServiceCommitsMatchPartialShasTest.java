package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.VCS_TYPE;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.models.IntegrationType;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;

public class ScmAggServiceCommitsMatchPartialShasTest{
    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static IntegrationService integrationService;
    private static UserIdentityService userIdentityService;
    private static ScmAggService scmAggService;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;

        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        integrationService = new IntegrationService(dataSource);
        userIdentityService = new UserIdentityService(dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        dataSource.getConnection().prepareStatement("update test.scm_files set filetype = COALESCE(substring(filename from '\\.([^\\.]*)$'),'NA');")
                .execute();
        dataSource.getConnection().prepareStatement(arrayUniq)
                .execute();
    }

    @Test
    public void test() throws SQLException {
        Integration integration = IntegrationUtils.createIntegration(integrationService, company, 0, IntegrationType.BITBUCKET.toString());
        Integer integrationId = Integer.parseInt(integration.getId());

        DbScmCommit commit1 = DbScmCommit.builder()
                .repoIds(List.of("somesecurity/some-agent"))
                .integrationId(integration.getId())
                .author("Viraj Ajgaonkar <viraj.ajgaonkar@somesecurity.com>")
                .committer("Viraj Ajgaonkar <viraj.ajgaonkar@somesecurity.com>")
                .commitSha("000d3f4122dcff7947af03e2a1f5bfb9d8c2f8b5")
                .commitUrl("https://bitbucket.org/contrastsecurity/java-agent/commits/000d3f4122dcff7947af03e2a1f5bfb9d8c2f8b5")
                .message("JAVA-1898 :wrench: Spring")
                .filesCt(3)
                .additions(113)
                .deletions(23)
                .ingestedAt(1616544000L)
                .committedAt(1616544000L)
                .changes(0)
                .project("contrastsecurity/java-agent")
                .vcsType(VCS_TYPE.GIT)
                .authorInfo(DbScmUser.builder().cloudId("12345").displayName("Viraj Ajgaonkar").originalDisplayName("Viraj Ajgaonkar").build())
                .committerInfo(DbScmUser.builder().cloudId("12345").displayName("Viraj Ajgaonkar").originalDisplayName("Viraj Ajgaonkar").build())
                .build();
        String id = scmAggService.insertCommit(company, commit1);
        commit1 = commit1.toBuilder().id(id).build();

        DbScmCommit commit2 = DbScmCommit.builder()
                .repoIds(List.of("somesecurity/some-teamserver-tfp"))
                .integrationId(integration.getId())
                .author("Harry Potter <harry.potter@somesecurity.com>")
                .committer("Harry Potter <harry.potter@somesecurity.com>")
                .commitSha("0010cacbf46e9f742a4410880dd5ac3148caa6aa")
                .commitUrl("https://bitbucket.org/contrastsecurity/contrast-teamserver-tfp/commits/0010cacbf46e9f742a4410880dd5ac3148caa6aa")
                .message("Merged in DEVOPS-7629 (pu")
                .filesCt(10)
                .additions(243)
                .deletions(89)
                .ingestedAt(1614643200L)
                .committedAt(1614643200L)
                .changes(0)
                .project("somesecurity/some-teamserver-tfp")
                .vcsType(VCS_TYPE.GIT)
                .authorInfo(DbScmUser.builder().cloudId("45678").displayName("Harry Potter").originalDisplayName("Harry Potter").build())
                .committerInfo(DbScmUser.builder().cloudId("45678").displayName("Harry Potter").originalDisplayName("Harry Potter").build())
                .build();
        id = scmAggService.insertCommit(company, commit2);
        commit2 = commit2.toBuilder().id(id).build();

        List<String> completeShas = scmAggService.findCommitShasMatchingPartialShas(company, integrationId, "000d3f4122dc");
        Assert.assertNotNull(completeShas);
        Assert.assertEquals(List.of("000d3f4122dcff7947af03e2a1f5bfb9d8c2f8b5"), completeShas);

        completeShas = scmAggService.findCommitShasMatchingPartialShas(company, integrationId, "00921d3dd86f");
        Assert.assertNotNull(completeShas);
        Assert.assertTrue(CollectionUtils.isEmpty(completeShas));

        Long time = System.currentTimeMillis();
        //PR with null merge sha
        DbScmPullRequest pr1 = DbScmPullRequest.builder()
                .repoIds(List.of("somesecurity/some-agent"))
                .integrationId(integration.getId())
                .creatorInfo(DbScmUser.builder().cloudId("creator-1").displayName("creator one").originalDisplayName("creator one").build())
                .creator("creator one").creatorId("creator.one@somesecurity.com")
                .project("project-1")
                .state("merged")
                .number("101")
                .mergeSha(null).commitShas(List.of())
                .sourceBranch("branch-1")
                .merged(true).assignees(List.of())
                .labels(List.of("1"))
                .title("title-1")
                .prCreatedAt(time).prUpdatedAt(time).prMergedAt(time).prClosedAt(time)
                .build();
        id = scmAggService.insert(company, pr1);
        pr1 = pr1.toBuilder().id(id).build();

        time = System.currentTimeMillis();
        //PR with interesting partial sha 1
        DbScmPullRequest pr2 = DbScmPullRequest.builder()
                .repoIds(List.of("somesecurity/some-agent"))
                .integrationId(integration.getId())
                .creatorInfo(DbScmUser.builder().cloudId("creator-2").displayName("creator two").originalDisplayName("creator two").build())
                .creator("creator two").creatorId("creator.two@somesecurity.com")
                .project("project-1")
                .state("merged")
                .number("102")
                .mergeSha("000d3f4122dc").commitShas(List.of("000d3f4122dc"))
                .sourceBranch("branch-2")
                .merged(true).assignees(List.of())
                .labels(List.of("2"))
                .title("title-2")
                .prCreatedAt(time).prUpdatedAt(time).prMergedAt(time).prClosedAt(time)
                .build();
        id = scmAggService.insert(company, pr2);
        pr2 = pr2.toBuilder().id(id).build();

        time = System.currentTimeMillis();
        //PR with interesting partial sha 2
        DbScmPullRequest pr3 = DbScmPullRequest.builder()
                .repoIds(List.of("somesecurity/some-agent"))
                .integrationId(integration.getId())
                .creatorInfo(DbScmUser.builder().cloudId("creator-3").displayName("creator three").originalDisplayName("creator three").build())
                .creator("creator three").creatorId("creator.three@somesecurity.com")
                .project("project-1")
                .state("merged")
                .number("103")
                .mergeSha("000d3f4122dc").commitShas(List.of("000d3f4122dc"))
                .sourceBranch("branch-3")
                .merged(true).assignees(List.of())
                .labels(List.of("3"))
                .title("title-3")
                .prCreatedAt(time).prUpdatedAt(time).prMergedAt(time).prClosedAt(time)
                .build();
        id = scmAggService.insert(company, pr3);
        pr3 = pr3.toBuilder().id(id).build();

        time = System.currentTimeMillis();
        //PR with interesting full sha
        DbScmPullRequest pr4 = DbScmPullRequest.builder()
                .repoIds(List.of("somesecurity/some-agent"))
                .integrationId(integration.getId())
                .creatorInfo(DbScmUser.builder().cloudId("creator-4").displayName("creator four").originalDisplayName("creator four").build())
                .creator("creator four").creatorId("creator.four@somesecurity.com")
                .project("project-1")
                .state("merged")
                .number("104")
                .mergeSha("000d3f4122dcff7947af03e2a1f5bfb9d8c2f8b5").commitShas(List.of("000d3f4122dcff7947af03e2a1f5bfb9d8c2f8b5"))
                .sourceBranch("branch-4")
                .merged(true).assignees(List.of())
                .labels(List.of("4"))
                .title("title-4")
                .prCreatedAt(time).prUpdatedAt(time).prMergedAt(time).prClosedAt(time)
                .build();
        id = scmAggService.insert(company, pr4);
        pr4 = pr4.toBuilder().id(id).build();

        time = System.currentTimeMillis();
        //PR with NOT interesting partial sha
        DbScmPullRequest pr5 = DbScmPullRequest.builder()
                .repoIds(List.of("somesecurity/some-teamserver-tfp"))
                .integrationId(integration.getId())
                .creatorInfo(DbScmUser.builder().cloudId("creator-5").displayName("creator five").originalDisplayName("creator five").build())
                .creator("creator five").creatorId("creator.five@somesecurity.com")
                .project("project-1")
                .state("merged")
                .number("105")
                .mergeSha("0010cacbf46e").commitShas(List.of("0010cacbf46e"))
                .sourceBranch("branch-5")
                .merged(true).assignees(List.of())
                .labels(List.of("5"))
                .title("title-5")
                .prCreatedAt(time).prUpdatedAt(time).prMergedAt(time).prClosedAt(time)
                .build();
        id = scmAggService.insert(company, pr5);
        pr5 = pr5.toBuilder().id(id).build();

        List<String> partialShas = scmAggService.findPartialShasInBitbucketPRs(company, integrationId, 100);
        Assert.assertNotNull(partialShas);
        Assert.assertEquals(2, partialShas.size());
        Assert.assertEquals(Set.of("000d3f4122dc", "0010cacbf46e"), partialShas.stream().collect(Collectors.toSet()));

        List<String> fullShas = scmAggService.findCommitShasMatchingPartialShas(company, integrationId, "000d3f4122dc");
        Assert.assertEquals(1, fullShas.size());

        int affectedRows = scmAggService.updatePartialShasInBitbucketPRs(company, integrationId, "000d3f4122dc", fullShas.get(0));
        Assert.assertEquals(2, affectedRows);

        partialShas = scmAggService.findPartialShasInBitbucketPRs(company, integrationId, 100);
        Assert.assertNotNull(partialShas);
        Assert.assertEquals(1, partialShas.size());
        Assert.assertEquals(Set.of("0010cacbf46e"), partialShas.stream().collect(Collectors.toSet()));

        fullShas = scmAggService.findCommitShasMatchingPartialShas(company, integrationId, "0010cacbf46e");
        Assert.assertEquals(1, fullShas.size());

        affectedRows = scmAggService.updatePartialShasInBitbucketPRs(company, integrationId, "0010cacbf46e", fullShas.get(0));
        Assert.assertEquals(1, affectedRows);
    }
}