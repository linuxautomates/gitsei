package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.organization.DBOrgProduct;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmIssue;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmRepoAgg;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmReposFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.github.models.GithubRepository;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;
import static org.assertj.core.api.Assertions.assertThat;

public class ScmAggServiceFileTypeTest {

    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static ScmAggService scmAggService;
    private static String gitHubIntegrationId;
    private static GitRepositoryService repositoryService;
    private static UserIdentityService userIdentityService;
    private static TeamMembersDatabaseService teamMembersDatabaseService;
    private static ProductsDatabaseService productsDatabaseService;
    private static Date currentTime;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;

        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        IntegrationService integrationService = new IntegrationService(dataSource);
        userIdentityService = new UserIdentityService(dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        productsDatabaseService = new ProductsDatabaseService(dataSource, DefaultObjectMapper.get());
        repositoryService = new GitRepositoryService(dataSource);
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, DefaultObjectMapper.get());
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
        productsDatabaseService.ensureTableExistence(company);
        currentTime = DateUtils.truncate(new Date(Instant.parse("2021-05-05T15:00:00-08:00").getEpochSecond()), Calendar.DATE);
        String input = ResourceUtils.getResourceAsString("json/databases/githubprs.json");
        PaginatedResponse<GithubRepository> prs = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        List<DbRepository> repos = new ArrayList<>();
        prs.getResponse().getRecords().forEach(repo -> {
            repos.add(DbRepository.fromGithubRepository(repo, gitHubIntegrationId));
            repo.getPullRequests()
                    .forEach(review -> {
                        try {
                            DbScmPullRequest tmp = DbScmPullRequest
                                    .fromGithubPullRequest(review, repo.getId(), gitHubIntegrationId, null);
                            scmAggService.insert(company, tmp);
                        } catch (SQLException throwable) {
                            throwable.printStackTrace();
                        }
                    });
        });

        input = ResourceUtils.getResourceAsString("json/databases/github_issues.json");
        PaginatedResponse<GithubRepository> issues = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        issues.getResponse().getRecords().forEach(repo -> {
            repos.add(DbRepository.fromGithubRepository(repo, gitHubIntegrationId));
            repo.getIssues()
                    .forEach(issue -> {
                        DbScmIssue tmp = DbScmIssue
                                .fromGithubIssue(issue, repo.getId(), gitHubIntegrationId);
                        if (scmAggService.getIssue(company, tmp.getIssueId(), tmp.getRepoId(),
                                tmp.getIntegrationId()).isEmpty())
                            scmAggService.insertIssue(company, tmp);
                    });
        });

        input = ResourceUtils.getResourceAsString("json/databases/githubcommits.json");
        PaginatedResponse<GithubRepository> commits = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        commits.getResponse().getRecords().forEach(repo -> {
            repos.add(DbRepository.fromGithubRepository(repo, gitHubIntegrationId));
            repo.getEvents().stream()
                    .filter(ev -> "PushEvent".equals(ev.getType()))
                    .flatMap(ev -> ev.getCommits().stream())
                    .forEach(commit -> {
                        Long eventTime = currentTime.toInstant().getEpochSecond();
                        DbScmCommit dbScmCommit = DbScmCommit
                                .fromGithubCommit(commit, repo.getId(), gitHubIntegrationId, eventTime, 0L);
                        Optional<DbScmCommit> opt = scmAggService.getCommit(company, dbScmCommit.getCommitSha(), repo.getId(), gitHubIntegrationId);
                        if (opt.isEmpty()) {
                            List<DbScmFile> dbScmFiles = DbScmFile.fromGithubCommit(commit, repo.getId(), gitHubIntegrationId, eventTime);
                            try {
                                scmAggService.insert(company, dbScmCommit, dbScmFiles);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                            DbScmFile.fromGithubCommit(
                                    commit, repo.getId(), gitHubIntegrationId, currentTime.toInstant().getEpochSecond())
                                    .forEach(scmFile -> scmAggService.insertFile(company, scmFile));
                        }
                    });
        });
        repositoryService.batchUpsert(company, repos);
        dataSource.getConnection().prepareStatement("update test.scm_files set filetype = COALESCE(substring(filename from '\\.([^\\.]*)$'),'NA');")
                .execute();
        String arrayCatAgg = "CREATE AGGREGATE array_cat_agg(anyarray) (\n" +
                "  SFUNC=array_cat,\n" +
                "  STYPE=anyarray\n" +
                ");";
        dataSource.getConnection().prepareStatement(arrayCatAgg)
                .execute();
        dataSource.getConnection().prepareStatement(arrayUniq)
                .execute();
    }

    private Optional<String> userIdOf(String displayName) {
        return userIdentityService.getUserByDisplayName(company,gitHubIntegrationId, displayName);
    }

    @Test
    public void testCommitsGroupBy() {
        DbListResponse<DbAggregationResult> result = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmCommitFilter.DISTINCT.file_type)
                        .build(), null);
        Assertions.assertThat(result.getTotalCount()).isGreaterThan(0);
        Assertions.assertThat(result.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .contains("jsx", "js");
        result = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .repoIds(List.of("levelops/ui-levelops"))
                        .across(ScmCommitFilter.DISTINCT.file_type)
                        .build(), null);
        Assertions.assertThat(result.getTotalCount()).isGreaterThan(0);
        Assertions.assertThat(result.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .contains("jsx", "js");
        result = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .fileTypes(List.of("js", "jsx"))
                        .across(ScmCommitFilter.DISTINCT.file_type)
                        .build(), null);
        Assertions.assertThat(result.getTotalCount()).isGreaterThan(0);
        Assertions.assertThat(result.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .contains("js", "jsx");
        result = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .fileTypes(List.of("js"))
                        .across(ScmCommitFilter.DISTINCT.file_type)
                        .build(), null);
        Assertions.assertThat(result.getTotalCount()).isGreaterThan(0);
        Assertions.assertThat(result.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .contains("js");
        result = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .excludeFileTypes(List.of("js"))
                        .across(ScmCommitFilter.DISTINCT.file_type)
                        .build(), null);
        Assertions.assertThat(result.getTotalCount()).isGreaterThan(0);
        Assertions.assertThat(result.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .contains("jsx");

        result = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .excludeFileTypes(List.of("js", "tsx", "json", "ts", "svg", "png", "scss"))
                        .across(ScmCommitFilter.DISTINCT.file_type)
                        .build(), null);
        Assertions.assertThat(result.getTotalCount()).isGreaterThan(0);
        Assertions.assertThat(result.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .contains("jsx");
    }

    @Test
    public void testListFilesTypes() {
        DbListResponse<DbScmRepoAgg> result = scmAggService.listFileTypes(
                company, ScmReposFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .build(), Map.of(), null, 0, 1000);
        Assertions.assertThat(result.getTotalCount()).isGreaterThan(0);
        Assertions.assertThat(result.getRecords().stream().map(DbScmRepoAgg::getNumJiraIssues).collect(Collectors.toList()))
                .isNotEmpty();
        Assertions.assertThat(result.getRecords().stream().map(DbScmRepoAgg::getName).collect(Collectors.toList()))
                .contains("jsx", "js");
        result = scmAggService.listFileTypes(
                company, ScmReposFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .fileTypes(List.of("js", "jsx"))
                        .build(), Map.of(), null, 0, 1000);
        Assertions.assertThat(result.getTotalCount()).isGreaterThan(0);
        Assertions.assertThat(result.getRecords().stream().map(DbScmRepoAgg::getNumJiraIssues).collect(Collectors.toList()))
                .isNotEmpty();
        Assertions.assertThat(result.getRecords().stream().map(DbScmRepoAgg::getName).collect(Collectors.toList()))
                .contains("js", "jsx");
        result = scmAggService.listFileTypes(
                company, ScmReposFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .fileTypes(List.of("js"))
                        .build(), Map.of(), null, 0, 1000);
        Assertions.assertThat(result.getTotalCount()).isGreaterThan(0);
        Assertions.assertThat(result.getRecords().stream().map(DbScmRepoAgg::getNumJiraIssues).collect(Collectors.toList()))
                .isNotEmpty();
        Assertions.assertThat(result.getRecords().stream().map(DbScmRepoAgg::getName).collect(Collectors.toList()))
                .contains("js");
        result = scmAggService.listFileTypes(
                company, ScmReposFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .excludeFileTypes(List.of("js"))
                        .build(), Map.of(), null, 0, 1000);
        Assertions.assertThat(result.getTotalCount()).isGreaterThan(0);
        Assertions.assertThat(result.getRecords().stream().map(DbScmRepoAgg::getNumJiraIssues).collect(Collectors.toList()))
                .isNotEmpty();
        Assertions.assertThat(result.getRecords().stream().map(DbScmRepoAgg::getName).collect(Collectors.toList()))
                .contains("jsx");
        result = scmAggService.listFileTypes(
                company, ScmReposFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .excludeFileTypes(List.of("js", "jsx"))
                        .build(), Map.of(), null, 0, 1000);
        Assertions.assertThat(result.getTotalCount()).isEqualTo(0);
        Assertions.assertThat(result.getRecords().stream().map(DbScmRepoAgg::getNumJiraIssues).collect(Collectors.toList()))
                .isEmpty();
        Assertions.assertThat(result.getRecords().stream().map(DbScmRepoAgg::getName).collect(Collectors.toList()))
                .isEmpty();
        result = scmAggService.listFileTypes(
                company, ScmReposFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .excludeFileTypes(List.of("js", "jsx", "tsx"))
                        .build(), Map.of(), null, 0, 1000);
        Assertions.assertThat(result.getTotalCount()).isEqualTo(0);
        Assertions.assertThat(result.getRecords().stream().map(DbScmRepoAgg::getNumJiraIssues).collect(Collectors.toList()))
                .isEmpty();
        Assertions.assertThat(result.getRecords().stream().map(DbScmRepoAgg::getName).collect(Collectors.toList()))
                .isEmpty();

        DbListResponse<DbScmRepoAgg> expected = DbListResponse.of(List.of(
                DbScmRepoAgg.builder().name("ts").numCommits(19).numPrs(0).numAdditions(12048).numDeletions(2029)
                        .numChanges(14077).numJiraIssues(6).numWorkitems(22).build(),
                DbScmRepoAgg.builder().name("json").numCommits(15).numPrs(0).numAdditions(19603).numDeletions(3570)
                        .numChanges(23173).numJiraIssues(5).numWorkitems(38).build(),
                DbScmRepoAgg.builder().name("scss").numCommits(15).numPrs(0).numAdditions(9591).numDeletions(2517)
                        .numChanges(12108).numJiraIssues(9).numWorkitems(23).build(),
                DbScmRepoAgg.builder().name("svg").numCommits(4).numPrs(0).numAdditions(6386).numDeletions(1930)
                        .numChanges(8316).numJiraIssues(6).numWorkitems(18).build(),
                DbScmRepoAgg.builder().name("png").numCommits(2).numPrs(0).numAdditions(126).numDeletions(2)
                        .numChanges(128).numJiraIssues(0).numWorkitems(0).build()),
                5);
        assertThat(result.getRecords()).isEmpty();

        String userPiyush = userIdOf("piyushkantm").orElse("");
        String userWebFlow = userIdOf("web-flow").orElse("");
        result = scmAggService.listFileTypes(
                company, ScmReposFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .committers(List.of(userPiyush))
                        .excludeCommitters(List.of(userWebFlow))
                        .build(), Map.of(), null, 0, 1000);
        Assertions.assertThat(result.getTotalCount()).isGreaterThan(0);
        Assertions.assertThat(result.getRecords().stream().map(DbScmRepoAgg::getNumJiraIssues).collect(Collectors.toList()))
                .isNotEmpty();
        Assertions.assertThat(result.getRecords().stream().map(DbScmRepoAgg::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("jsx", "js");

        result = scmAggService.listFileTypes(
                company, ScmReposFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .authors(List.of(userPiyush))
                        .build(), Map.of(), null, 0, 1000);
        Assertions.assertThat(result.getTotalCount()).isGreaterThan(0);
        Assertions.assertThat(result.getRecords().stream().map(DbScmRepoAgg::getNumJiraIssues).collect(Collectors.toList()))
                .isNotEmpty();
        Assertions.assertThat(result.getRecords().stream().map(DbScmRepoAgg::getName).filter(Objects::nonNull).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("jsx", "js");
    }

    @Test
    public void testListFileTypesProductsTest() throws SQLException {
        DBOrgProduct productOne = ScmAggServiceFileTypeUtils.getProductWithNoIntegAndFilter();
        String uuid = productsDatabaseService.insert(company, productOne);
        DbListResponse<DbScmRepoAgg> result = scmAggService.listFileTypes(company, ScmReposFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .orgProductIds(Set.of(UUID.fromString(uuid)))
                        .build(), Map.of(), null, 0, 1000);
        Assertions.assertThat(result.getTotalCount()).isGreaterThan(0);
        Assertions.assertThat(result.getRecords().stream().map(DbScmRepoAgg::getNumJiraIssues).collect(Collectors.toList()))
                .isNotEmpty();
        Assertions.assertThat(result.getRecords().stream().map(DbScmRepoAgg::getName).collect(Collectors.toList()))
                .contains("jsx", "js");

        productOne = ScmAggServiceFileTypeUtils.getProductWithInteg();
        uuid = productsDatabaseService.insert(company, productOne);
        result = scmAggService.listFileTypes(company, ScmReposFilter.builder()
                .integrationIds(List.of(gitHubIntegrationId))
                .orgProductIds(Set.of(UUID.fromString(uuid)))
                .build(), Map.of(), null, 0, 1000);
        Assertions.assertThat(result.getTotalCount()).isGreaterThan(0);
        Assertions.assertThat(result.getRecords().stream().map(DbScmRepoAgg::getNumJiraIssues).collect(Collectors.toList()))
                .isNotEmpty();
        Assertions.assertThat(result.getRecords().stream().map(DbScmRepoAgg::getName).collect(Collectors.toList()))
                .contains("jsx", "js");
    }

    @Test
    public void testListCommits() {
        DbListResponse<DbScmCommit> result = scmAggService.listCommits(
                company,
                ScmCommitFilter.builder()
                        .fileTypes(List.of("js"))
                        .build(),
                Map.of(),
                null,
                0,
                10);
        Assertions.assertThat(result.getTotalCount()).isGreaterThan(0);
        result.getRecords().forEach(record -> {
            Assertions.assertThat(record.getFileTypes()).contains("js");
        });


        result = scmAggService.listCommits(
                company,
                ScmCommitFilter.builder()
                        .fileTypes(List.of("jsx"))
                        .build(),
                Map.of(),
                null,
                0,
                10);
        Assertions.assertThat(result.getTotalCount()).isGreaterThan(0);
        Assertions.assertThat(result.getTotalCount()).isGreaterThan(0);
        result.getRecords().forEach(record -> {
            Assertions.assertThat(record.getFileTypes()).contains("jsx");
        });


        result = scmAggService.listCommits(
                company,
                ScmCommitFilter.builder()
                        .fileTypes(List.of("json"))
                        .build(),
                Map.of(),
                null,
                0,
                10);
        Assertions.assertThat(result.getTotalCount()).isEqualTo(0);
        result.getRecords().forEach(record -> {
            Assertions.assertThat(record.getFileTypes()).contains("json");
        });


        result = scmAggService.listCommits(
                company,
                ScmCommitFilter.builder()
                        .excludeFileTypes(List.of("js"))
                        .build(),
                Map.of(),
                null,
                0,
                10);
        Assertions.assertThat(result.getTotalCount()).isGreaterThan(0);
        Assertions.assertThat(result.getTotalCount()).isGreaterThan(0);
    }
}
