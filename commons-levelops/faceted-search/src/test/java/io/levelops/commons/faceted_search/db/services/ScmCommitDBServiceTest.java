package io.levelops.commons.faceted_search.db.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmFileCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFileCommitDetails;
import io.levelops.commons.databases.models.database.scm.DbScmTag;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.VCS_TYPE;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.faceted_search.db.models.ScmPROrCommitJiraWIMapping;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.ScmQueryUtils.ARRAY_UNIQ;

public class ScmCommitDBServiceTest {
    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static IntegrationService integrationService;
    private static ScmAggService scmAggService;
    private static UserIdentityService userIdentityService;
    private static String gitHubIntegrationId = "1" ;
    private static ScmCommitDBService scmCommitDBService;
    private static ScmPRDBService scmPRDBService;
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

        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        dataSource.getConnection().prepareStatement(ARRAY_UNIQ).execute();
        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        scmAggService.ensureTableExistence(company);
        scmPRDBService = new ScmPRDBService(dataSource, scmAggService);
        IntegrationService integrationService = new IntegrationService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.insert(company, Integration.builder().application("github").name("gh-integ")
                .status("enabled").id("1").build());

        now = Instant.now().getEpochSecond();

        DbScmCommit scmCommit1 = getScmCommits("934613bf40ceacc18ed59a787a745c49c18f71d9",  1629347837,123,6, 4, List.of("LEV-123"));
        DbScmCommit scmCommit2 = getScmCommits("934613bf40ceacc18ed59a787a745c49c18f71d8",  1629347906, 88,42, 9, List.of("LEV-456"));
        DbScmCommit scmCommit3 = getScmCommits("934613bf40ceacc18ed59a787a745c49c18f71d7",  1630685288,21,34, 1, List.of("LEV-789"));
        DbScmCommit scmCommit4 = getScmCommits("934613bf40ceacc18ed59a787a745c49c18f71d6",  1631559873,232,56, 7, List.of("LEV-910"));
        DbScmCommit scmCommit5 = getScmCommits("934613bf40ceacc18ed59a787a745c49c18f71d5",  1631774210,157,96, 6, List.of("LEV-111"));

        scmAggService.insertCommit(company,scmCommit1);
        scmAggService.insertCommit(company,scmCommit2);
        scmAggService.insertCommit(company,scmCommit3);
        scmAggService.insertCommit(company,scmCommit4);
        scmAggService.insertCommit(company,scmCommit5);
        DbScmTag tag1 = getTag("934613bf40ceacc18ed59a787a745c49c18f71d9","tag-1");
        DbScmTag tag2 = getTag("934613bf40ceacc18ed59a787a745c49c18f71d8","tag-2");

        scmAggService.insertTag(company, tag1);
        scmAggService.insertTag(company, tag2);

        DbScmFileCommit fc1 = getFileCommit("934613bf40ceacc18ed59a787a745c49c18f71d9");
        DbScmFileCommit fc2 = getFileCommit("934613bf40ceacc18ed59a787a745c49c18f71d7");

        DbScmFile sf1 = getScmFile(List.of("934613bf40ceacc18ed59a787a745c49c18f71d9"),List.of(fc1));
        DbScmFile sf2 = getScmFile(List.of("934613bf40ceacc18ed59a787a745c49c18f71d7"),List.of(fc2));

        scmAggService.insertFile(company, sf1);
        scmAggService.insertFile(company, sf2);
        scmCommitDBService = new ScmCommitDBService(dataSource, scmAggService, scmPRDBService);

    }

    private static DbScmFile getScmFile(List<String> commitshas, List<DbScmFileCommit> fc) {
        return DbScmFile.builder()
                .commitShas(commitshas)
                .fileCommits(fc)
                .createdAt(1629347837l)
                .numCommits(1l)
                .filename("1234")
                .integrationId("1")
                .project("test-project")
                .repoId("test-repo")
                .build();
    }

    private static DbScmFileCommit getFileCommit(String commitsha) {

        return DbScmFileCommit.builder()
                .addition(123)
                .deletion(6)
                .change(4)
                .fileId("1234")
                .commitSha(commitsha)
                .committedAt(1629347837l)
                .build();
    }

    private static DbScmTag getTag(String commitSha, String tag) {
        return DbScmTag.builder()
                .commitSha(commitSha)
                .integrationId("1")
                .project("test-project")
                .repo("test-repo")
                .tag(tag)
                .build();

    }

    private static DbScmCommit getScmCommits(String commitSha, long createdAt, int addition, int deletion, int changes, List<String> issueKey) {

        return DbScmCommit.builder()
                .integrationId("1")
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
                .issueKeys(issueKey)
                .build();
    }

    @Test
    public void testListByJiraIssuesOrWorkItems(){

        List<ScmPROrCommitJiraWIMapping> list = scmCommitDBService.getScmCommitJiraWIMappings(company, null, null, true);
        Assertions.assertThat(list.size()).isEqualTo(5);
        Assertions.assertThat(list.get(0).getWorkItemIds()).containsExactlyInAnyOrder("LEV-123");
        Assertions.assertThat(list.get(1).getWorkItemIds()).containsExactlyInAnyOrder("LEV-456");
        Assertions.assertThat(list.get(2).getWorkItemIds()).containsExactlyInAnyOrder("LEV-789");
        Assertions.assertThat(list.get(3).getWorkItemIds()).containsExactlyInAnyOrder("LEV-910");
        Assertions.assertThat(list.get(4).getWorkItemIds()).containsExactlyInAnyOrder("LEV-111");
        System.out.println(list);

        list = scmCommitDBService.getScmCommitJiraWIMappings(company, Set.of("LEV-123","LEV-789"), null, true);
        Assertions.assertThat(list.size()).isEqualTo(2);
        Assertions.assertThat(list.get(0).getWorkItemIds()).containsExactlyInAnyOrder("LEV-123");
        Assertions.assertThat(list.get(1).getWorkItemIds()).containsExactlyInAnyOrder("LEV-789");

        list = scmCommitDBService.getScmCommitJiraWIMappings(company, null, Set.of("934613bf40ceacc18ed59a787a745c49c18f71d8","934613bf40ceacc18ed59a787a745c49c18f71d6","934613bf40ceacc18ed59a787a745c49c18f71d5"), true);
        Assertions.assertThat(list.size()).isEqualTo(3);
        Assertions.assertThat(list.get(0).getWorkItemIds()).containsExactlyInAnyOrder("LEV-456");
        Assertions.assertThat(list.get(1).getWorkItemIds()).containsExactlyInAnyOrder("LEV-910");
        Assertions.assertThat(list.get(2).getWorkItemIds()).containsExactlyInAnyOrder("LEV-111");
    }

    @Test
    public void testListCommit(){
        ScmCommitFilter filter = ScmCommitFilter.builder()
                .build();

        List<DbScmCommit> list = scmCommitDBService.listCommit(company, filter, null, 0, 10);
        Assertions.assertThat(list.size()).isEqualTo(5);
        Assertions.assertThat(list.get(0).getCommitSha()).isEqualTo("934613bf40ceacc18ed59a787a745c49c18f71d9");
        Assertions.assertThat(list.get(1).getCommitSha()).isEqualTo("934613bf40ceacc18ed59a787a745c49c18f71d8");
        Assertions.assertThat(list.get(2).getCommitSha()).isEqualTo("934613bf40ceacc18ed59a787a745c49c18f71d7");
        Assertions.assertThat(list.get(3).getCommitSha()).isEqualTo("934613bf40ceacc18ed59a787a745c49c18f71d6");
        Assertions.assertThat(list.get(4).getCommitSha()).isEqualTo("934613bf40ceacc18ed59a787a745c49c18f71d5");

        filter = ScmCommitFilter.builder()
                .committedAtRange(ImmutablePair.of(1620328106l,1631540071l))
                .build();
        list = scmCommitDBService.listCommit(company, filter, null, 0, 10);
        Assertions.assertThat(list.size()).isEqualTo(3);
        Assertions.assertThat(list.get(0).getCommitSha()).isEqualTo("934613bf40ceacc18ed59a787a745c49c18f71d9");
        Assertions.assertThat(list.get(1).getCommitSha()).isEqualTo("934613bf40ceacc18ed59a787a745c49c18f71d8");
        Assertions.assertThat(list.get(2).getCommitSha()).isEqualTo("934613bf40ceacc18ed59a787a745c49c18f71d7");
    }

    @Test
    public void testTags(){
        ScmCommitFilter filter = ScmCommitFilter.builder()
                .build();
        List<DbScmCommit> list = scmCommitDBService.listCommit(company, filter, null, 0, 10);
        Map<String, List<DbScmTag>> tagMap = scmCommitDBService.getScmTags(company, list);
        Assertions.assertThat(tagMap.size()).isEqualTo(2);
        Assertions.assertThat(tagMap.containsKey("934613bf40ceacc18ed59a787a745c49c18f71d9")).isTrue();
        Assertions.assertThat(tagMap.containsKey("934613bf40ceacc18ed59a787a745c49c18f71d8")).isTrue();
        Assertions.assertThat(tagMap.containsKey("934613bf40ceacc18ed59a787a745c49c18f71d7")).isFalse();
    }

    @Test
    public void testFileCommits(){
        ScmCommitFilter filter = ScmCommitFilter.builder()
                .build();
        List<DbScmCommit> list = scmCommitDBService.listCommit(company, filter, null, 0, 10);
        Map<String, List<DbScmFileCommitDetails>> fileCommitMap = scmCommitDBService.getFileCommits(company, list);
        Assertions.assertThat(fileCommitMap.size()).isEqualTo(2);
        Assertions.assertThat(fileCommitMap.containsKey("934613bf40ceacc18ed59a787a745c49c18f71d9_1")).isTrue();
        Assertions.assertThat(fileCommitMap.containsKey("934613bf40ceacc18ed59a787a745c49c18f71d7_1")).isTrue();
        Assertions.assertThat(fileCommitMap.containsKey("934613bf40ceacc18ed59a787a745c49c18f71d6_1")).isFalse();
    }

    @Test
    public void testIntegrationUsers(){

        Map<String, DbScmUser> userMap = scmCommitDBService.getIntegrationUsers(company, null);
        List<DbScmUser> list = userMap.entrySet().stream().map( e -> e.getValue()).collect(Collectors.toList());
        Assertions.assertThat(list.size()).isEqualTo(1);
        Assertions.assertThat(list.get(0).getCloudId()).isEqualTo("ashish-levelops");
    }
}
