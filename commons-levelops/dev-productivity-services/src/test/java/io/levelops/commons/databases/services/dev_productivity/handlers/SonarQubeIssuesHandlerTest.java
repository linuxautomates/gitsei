package io.levelops.commons.databases.services.dev_productivity.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.models.database.dev_productivity.FeatureBreakDown;
import io.levelops.commons.databases.models.database.dev_productivity.FeatureResponse;
import io.levelops.commons.databases.models.database.dev_productivity.IntegrationUserDetails;
import io.levelops.commons.databases.models.database.dev_productivity.OrgUserDetails;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.database.sonarqube.DbSonarQubeProject;
import io.levelops.commons.databases.models.database.sonarqube.DbSonarQubePullRequest;
import io.levelops.commons.databases.models.filters.DevProductivityFilter;
import io.levelops.commons.databases.models.filters.VCS_TYPE;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.SonarQubeAggService;
import io.levelops.commons.databases.services.SonarQubeProjectService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.models.IntegrationType;
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
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SonarQubeIssuesHandlerTest {

    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static SonarQubeProjectService sonarQubeProjectService;
    private static ScmAggService scmAggService;
    private static SonarQubeAggService sonarQubeAggService;
    private static UserIdentityService userIdentityService;
    private static String gitHubIntegrationId = "2";
    private static SonarQubeIssuesHandler sonarQubeIssuesHandler;
    private static final Integer SECTION_ORDER = 0;
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
        userIdentityService = new UserIdentityService(dataSource);
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
        sonarQubeIssuesHandler = new SonarQubeIssuesHandler(sonarQubeAggService);

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
    public void test() throws SQLException, IOException {

        String id = userIdentityService.getUser(company,gitHubIntegrationId,"ashish-levelops");

        DevProductivityFilter devProductivityFilter = DevProductivityFilter.builder()
                .timeRange(ImmutablePair.of(1634816584l,1919834498L))
                .build();

        OrgUserDetails orgUser = OrgUserDetails.builder()
                .orgUserId(UUID.randomUUID())
                .fullName("ashish")
                .IntegrationUserDetailsList(List.of(
                        IntegrationUserDetails.builder()
                                .integrationId(1).integrationType(IntegrationType.SONARQUBE)
                                .cloudId("ashish-levelops")
                                .build(),
                        IntegrationUserDetails.builder()
                                .integrationId(2).integrationType(IntegrationType.GITHUB)
                                .cloudId("ashish-levelops")
                                .integrationUserId(UUID.fromString(id))
                                .build()))
                .build();

        DevProductivityProfile.Feature feature = DevProductivityProfile.Feature.builder()
                .params(Map.of("aggInterval",List.of("month")))
                .maxValue(100L).lowerLimitPercentage(25).upperLimitPercentage(75)
                .featureType(DevProductivityProfile.FeatureType.SONAR_BUG_ISSUES_PER_HUNDERD_LINES_OF_CODE)
                .build();

        FeatureResponse response = sonarQubeIssuesHandler.calculateFeature(company, SECTION_ORDER, feature, Map.of(), devProductivityFilter,orgUser, null, null);
        Assert.assertNotNull(response);
        Assertions.assertThat(response.getMean()).isEqualTo(1.4);

        feature = DevProductivityProfile.Feature.builder()
                .params(Map.of("aggInterval",List.of("month")))
                .maxValue(100L).lowerLimitPercentage(25).upperLimitPercentage(75)
                .featureType(DevProductivityProfile.FeatureType.SONAR_VULNERABILITY_ISSUES_PER_HUNDERD_LINES_OF_CODE)
                .build();
        response = sonarQubeIssuesHandler.calculateFeature(company, SECTION_ORDER, feature, Map.of(),devProductivityFilter,orgUser, null, null);
        Assert.assertNotNull(response);
        Assertions.assertThat(response.getMean()).isEqualTo(0.3);

        feature = DevProductivityProfile.Feature.builder()
                .params(Map.of("aggInterval",List.of("month")))
                .maxValue(100L).lowerLimitPercentage(25).upperLimitPercentage(75)
                .featureType(DevProductivityProfile.FeatureType.SONAR_CODE_SMELLS_ISSUES_PER_HUNDERD_LINES_OF_CODE)
                .build();
        response = sonarQubeIssuesHandler.calculateFeature(company, SECTION_ORDER, feature, Map.of(), devProductivityFilter,orgUser, null, null);
        Assert.assertNotNull(response);
        Assertions.assertThat(response.getMean()).isEqualTo(2.9);

        FeatureBreakDown breakDown = sonarQubeIssuesHandler.getBreakDown(company, feature, Map.of(), devProductivityFilter, orgUser, null, null, Map.of(), 0, 100);
        Assert.assertNotNull(breakDown);
        Assertions.assertThat(breakDown.getRecords().size()).isEqualTo(5);
    }
}
