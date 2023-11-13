package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.database.sonarqube.DbSonarQubeProject;
import io.levelops.commons.databases.models.database.sonarqube.DbSonarQubePullRequest;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.SonarQubePrFilter;
import io.levelops.commons.databases.models.filters.VCS_TYPE;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class SonarQubeAggServiceTest {

    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static SonarQubeProjectService sonarQubeProjectService;
    private static ScmAggService scmAggService;
    private static SonarQubeAggService sonarQubeAggService;
    private static UserIdentityService userIdentityService;
    private static String gitHubIntegrationId;
    final static DbScmUser testScmUser = DbScmUser.builder()
            .integrationId(gitHubIntegrationId)
            .cloudId("ashish-levelops")
            .displayName("ashish-levelops")
            .originalDisplayName("ashish-levelops")
            .build();

    private static long now;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;

        dataSource = DatabaseTestUtils.setUpDataSource(pg, company);
        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, company);
        userIdentityService = jiraTestDbs.getUserIdentityService();
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        sonarQubeProjectService = new SonarQubeProjectService(dataSource);

        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        sonarQubeProjectService.ensureTableExistence(company);
        IntegrationService integrationService = new IntegrationService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        integrationService.insert(company, Integration.builder().application("sonarqube").name("sq-integ")
                .status("enabled").id("1").build());
        integrationService.insert(company, Integration.builder().application("github").name("github-integ")
                .status("enabled").id("2").build());

        List<DbSonarQubePullRequest> sonarPrList = List.of(
                 getSonarPR("1", "test-branch", 0l, 0l,3l),
                 getSonarPR("2", "test-branch", 0l, 0l,3l),
                 getSonarPR("3", "test-branch", 3l, 0l,7l),
                 getSonarPR("4", "test-branch", 5l, 2l,0l),
                 getSonarPR("5", "test-branch", 1l, 0l,5l));

        DbSonarQubeProject project = DbSonarQubeProject.builder()
                .integrationId("1")
                .organization("test")
                .key("test-key")
                .name("test-name")
                .pullRequests(sonarPrList)
                .build();

        sonarQubeProjectService.insert(company, project);

        now = Instant.now().getEpochSecond();
        DbScmPullRequest pr1  = getScmPR("1", 1629347837,List.of("934613bf40ceacc18ed59a787a745c49c18f71d9") );
        DbScmPullRequest pr2  = getScmPR("2", 1629347906,List.of("934613bf40ceacc18ed59a787a745c49c18f71d8") );
        DbScmPullRequest pr3  = getScmPR("3", 1630685288,List.of("934613bf40ceacc18ed59a787a745c49c18f71d7") );
        DbScmPullRequest pr4  = getScmPR("4", 1631559873,List.of("934613bf40ceacc18ed59a787a745c49c18f71d6") );
        DbScmPullRequest pr5  = getScmPR("5", 1631774210,List.of("934613bf40ceacc18ed59a787a745c49c18f71d5") );
        String prId = scmAggService.insert(company, pr1);
        prId = scmAggService.insert(company, pr2);
        prId = scmAggService.insert(company, pr3);
        prId = scmAggService.insert(company, pr4);
        prId = scmAggService.insert(company, pr5);

        DbScmCommit scmCommit1 = getScmCoomits("934613bf40ceacc18ed59a787a745c49c18f71d9",  1629347837,123,6, 4);
        DbScmCommit scmCommit2 = getScmCoomits("934613bf40ceacc18ed59a787a745c49c18f71d8",  1629347906, 88,42, 9);
        DbScmCommit scmCommit3 = getScmCoomits("934613bf40ceacc18ed59a787a745c49c18f71d7",  1630685288,21,34, 1);
        DbScmCommit scmCommit4 = getScmCoomits("934613bf40ceacc18ed59a787a745c49c18f71d6",  1631559873,232,56, 7);
        DbScmCommit scmCommit5 = getScmCoomits("934613bf40ceacc18ed59a787a745c49c18f71d5",  1631774210,157,96, 6);

        scmAggService.insertCommit(company,scmCommit1);
        scmAggService.insertCommit(company,scmCommit2);
        scmAggService.insertCommit(company,scmCommit3);
        scmAggService.insertCommit(company,scmCommit4);
        scmAggService.insertCommit(company,scmCommit5);

        sonarQubeAggService = new SonarQubeAggService(dataSource);

    }

    private static DbScmCommit getScmCoomits(String commitSha, long createdAt, int addition, int deletion, int changes) {

        return DbScmCommit.builder()
                .integrationId("2")
                .repoIds(List.of("levelops/ingestion-levelops", "levelops/integrations-levelops"))
                .project("test_project")
                .authorInfo(testScmUser)
                .committerInfo(testScmUser)
                .commitSha(commitSha)
                .author("ashish-levelops")
                .committedAt(createdAt)
                .committer("ashish-levelops")
                .filesCt(1)
                .additions(addition)
                .deletions(deletion)
                .changes(changes)
                .ingestedAt(now)
                .vcsType(VCS_TYPE.GIT)
                .createdAt(createdAt)
                .build();
    }

    private static DbSonarQubePullRequest getSonarPR(String key, String branch, long bugs, long vulnerabilities, long codeSmell) {
        return DbSonarQubePullRequest.builder()
                .key(key)
                .title("test-title")
                .branch(branch)
                .bugs(bugs)
                .vulnerabilities(vulnerabilities)
                .codeSmells(codeSmell)
                .build();
    }

    private static DbScmPullRequest getScmPR(String number, long createdAt, List<String> commitShas) {
        return DbScmPullRequest.builder()
                .repoIds(List.of("levelops/ingestion-levelops", "levelops/integrations-levelops")).integrationId("2")
                .project("test-project")
                .sourceBranch("test-branch")
                .number(number)
                .creator("ashish-levelops").mergeSha("ad4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .creatorInfo(testScmUser)
                .title("LEV-1983").state("open").merged(false)
                .assignees(List.of("ashish-levelops")).commitShas(commitShas).labels(Collections.emptyList())
                .prCreatedAt(createdAt).prUpdatedAt(createdAt)
                .createdAt(createdAt)
                .build();
    }

    @Test
    public void test() throws SQLException {

        DbListResponse<DbAggregationResult> response = sonarQubeAggService.groupByAndCalculatePrs(company, SonarQubePrFilter.builder()
                .integrationIds(List.of("1","2"))
                .across(SonarQubePrFilter.DISTINCT.pr_created)
                .calculation(SonarQubePrFilter.CALCULATION.count)
                .aggInterval(AGG_INTERVAL.month)
                .creators(List.of("ashish-levelops"))
                .prCreatedRange(ImmutablePair.of(1634816584l ,1919834498L))
                .build());
        Assert.assertNotNull(response);
        Assertions.assertThat(response.getCount()).isEqualTo(2);
        Assert.assertNotNull(response);
        Assertions.assertThat(response.getCount()).isEqualTo(2);
        Assertions.assertThat(response.getRecords().get(0).getBugs()).isEqualTo(0);
        Assertions.assertThat(response.getRecords().get(0).getVulnerabilities()).isEqualTo(0);
        Assertions.assertThat(response.getRecords().get(0).getCodeSmells()).isEqualTo(6);
        Assertions.assertThat(response.getRecords().get(1).getBugs()).isEqualTo(9);
        Assertions.assertThat(response.getRecords().get(1).getVulnerabilities()).isEqualTo(2);
        Assertions.assertThat(response.getRecords().get(1).getCodeSmells()).isEqualTo(12);

        response = sonarQubeAggService.groupByAndCalculatePrs(company, SonarQubePrFilter.builder()
                .integrationIds(List.of("1","2"))
                .across(SonarQubePrFilter.DISTINCT.committed_at)
                 .calculation(SonarQubePrFilter.CALCULATION.count)
                .aggInterval(AGG_INTERVAL.month)
                .committer(List.of("ashish-levelops"))
                .commitCreatedRange(ImmutablePair.of(1634816584l ,1919834498L))
                .build());
        Assert.assertNotNull(response);
        Assertions.assertThat(response.getCount()).isEqualTo(2);
        Assertions.assertThat(response.getRecords().get(0).getBugs()).isEqualTo(0);
        Assertions.assertThat(response.getRecords().get(0).getVulnerabilities()).isEqualTo(0);
        Assertions.assertThat(response.getRecords().get(0).getCodeSmells()).isEqualTo(6);
        Assertions.assertThat(response.getRecords().get(1).getBugs()).isEqualTo(9);
        Assertions.assertThat(response.getRecords().get(1).getVulnerabilities()).isEqualTo(2);
        Assertions.assertThat(response.getRecords().get(1).getCodeSmells()).isEqualTo(12);

        response = sonarQubeAggService.groupByAndCalculatePrs(company, SonarQubePrFilter.builder()
                .integrationIds(List.of("1","2"))
                .across(SonarQubePrFilter.DISTINCT.committed_at)
                .calculation(SonarQubePrFilter.CALCULATION.count)
                .aggInterval(AGG_INTERVAL.week)
                .committer(List.of("ashish-levelops"))
                .commitCreatedRange(ImmutablePair.of(1634816584l ,1919834498L))
                .build());
        Assert.assertNotNull(response);
        Assertions.assertThat(response.getCount()).isEqualTo(3);
        Assertions.assertThat(response.getRecords().get(0).getBugs()).isEqualTo(0);
        Assertions.assertThat(response.getRecords().get(0).getVulnerabilities()).isEqualTo(0);
        Assertions.assertThat(response.getRecords().get(0).getCodeSmells()).isEqualTo(6);
        Assertions.assertThat(response.getRecords().get(1).getBugs()).isEqualTo(3);
        Assertions.assertThat(response.getRecords().get(1).getVulnerabilities()).isEqualTo(0);
        Assertions.assertThat(response.getRecords().get(1).getCodeSmells()).isEqualTo(7);
        Assertions.assertThat(response.getRecords().get(2).getBugs()).isEqualTo(6);
        Assertions.assertThat(response.getRecords().get(2).getVulnerabilities()).isEqualTo(2);
        Assertions.assertThat(response.getRecords().get(2).getCodeSmells()).isEqualTo(5);

        response = sonarQubeAggService.groupByAndCalculatePrs(company, SonarQubePrFilter.builder()
                .integrationIds(List.of("1","2"))
                .across(SonarQubePrFilter.DISTINCT.committed_at)
                .calculation(SonarQubePrFilter.CALCULATION.count)
                .aggInterval(AGG_INTERVAL.day)
                .committer(List.of("ashish-levelops"))
                .commitCreatedRange(ImmutablePair.of(1634816584l ,1919834498L))
                .build());
        Assert.assertNotNull(response);
        Assertions.assertThat(response.getCount()).isEqualTo(4);
        Assertions.assertThat(response.getRecords().get(0).getBugs()).isEqualTo(0);
        Assertions.assertThat(response.getRecords().get(0).getVulnerabilities()).isEqualTo(0);
        Assertions.assertThat(response.getRecords().get(0).getCodeSmells()).isEqualTo(6);
        Assertions.assertThat(response.getRecords().get(1).getBugs()).isEqualTo(3);
        Assertions.assertThat(response.getRecords().get(1).getVulnerabilities()).isEqualTo(0);
        Assertions.assertThat(response.getRecords().get(1).getCodeSmells()).isEqualTo(7);
        Assertions.assertThat(response.getRecords().get(2).getBugs()).isEqualTo(5);
        Assertions.assertThat(response.getRecords().get(2).getVulnerabilities()).isEqualTo(2);
        Assertions.assertThat(response.getRecords().get(2).getCodeSmells()).isEqualTo(0);
        Assertions.assertThat(response.getRecords().get(3).getBugs()).isEqualTo(1);
        Assertions.assertThat(response.getRecords().get(3).getVulnerabilities()).isEqualTo(0);
        Assertions.assertThat(response.getRecords().get(3).getCodeSmells()).isEqualTo(5);
    }


}
