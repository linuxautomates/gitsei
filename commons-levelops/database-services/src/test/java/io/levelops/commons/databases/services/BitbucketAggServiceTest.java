package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.converters.bitbucket.BitbucketCommitConverters;
import io.levelops.commons.databases.models.database.scm.converters.bitbucket.BitbucketPullRequestConverters;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmFilesFilter;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.bitbucket.models.BitbucketRepository;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;

public class BitbucketAggServiceTest {

    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static ScmAggService scmAggService;
    private static UserIdentityService userIdentityService;
    private static TeamMembersDatabaseService teamMembersDatabaseService;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;

        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        userIdentityService = new UserIdentityService(dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        IntegrationService integrationService = new IntegrationService(dataSource);
        BitbucketRepositoryService bitbucketRepositoryService = new BitbucketRepositoryService(dataSource, m);

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        integrationService.insert(company, Integration.builder()
                .application("bitbucket")
                .name("bitbucket test")
                .status("enabled")
                .build());
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        bitbucketRepositoryService.ensureTableExistence(company);
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, m);
        teamMembersDatabaseService.ensureTableExistence(company);
        String input = ResourceUtils.getResourceAsString("json/databases/bitbucket_prs.json");
        PaginatedResponse<BitbucketRepository> prs = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, BitbucketRepository.class));
        List<DbRepository> repos = new ArrayList<>();
        prs.getResponse().getRecords().forEach(repo -> {
            repos.add(DbRepository.fromBitbucketRepository(repo, "1"));
            repo.getPullRequests()
                    .forEach(review -> {
                        try {
                            DbScmPullRequest tmp = BitbucketPullRequestConverters
                                    .fromBitbucketPullRequest(review, repo.getFullName(), repo.getProject().getName(), "1", (review.getMergeCommit() != null) ? review.getMergeCommit().getHash(): null, null);
                            scmAggService.insert(company, tmp);
                        } catch (SQLException throwable) {
                            throwable.printStackTrace();
                        }
                    });
        });

        input = ResourceUtils.getResourceAsString("json/databases/bitbucket_commits.json");
        PaginatedResponse<BitbucketRepository> bitbucketCommits = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, BitbucketRepository.class));
        bitbucketCommits.getResponse().getRecords().forEach(repo -> {
            repos.add(DbRepository.fromBitbucketRepository(repo, "1"));
            repo.getCommits()
                    .forEach(commit -> {
                        long eventTime = commit.getDate().getTime();
                        DbScmCommit tmp = BitbucketCommitConverters.fromBitbucketCommit(commit, repo.getUuid(), repo.getProject().getName(), "1", eventTime);
                        if (scmAggService.getCommit(company, tmp.getCommitSha(), tmp.getRepoIds(),
                                tmp.getIntegrationId()).isEmpty()) {
                            try {
                                scmAggService.insertCommit(company, tmp);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                            DbScmFile.fromBitbucketCommit(
                                    commit, repo.getUuid(), "1", eventTime)
                                    .stream().filter(dbScmFile -> dbScmFile.getFilename() != null)
                                    .forEach(dbScmFile -> scmAggService.insertFile(company, dbScmFile));
                        }
                    });
        });
        bitbucketRepositoryService.batchUpsert(company, repos);
        dataSource.getConnection().prepareStatement(arrayUniq)
                .execute();
    }

    @Test
    public void test() throws SQLException, JsonProcessingException {

        for (ScmPrFilter.DISTINCT a : List.of(ScmPrFilter.DISTINCT.pr_updated,
                ScmPrFilter.DISTINCT.pr_merged,
                ScmPrFilter.DISTINCT.pr_created)) {
            System.out.println("hmm1: " + a + " data: " +
                    m.writeValueAsString(
                            scmAggService.groupByAndCalculatePrsDuration(
                                    company, ScmPrFilter.builder().across(a).build(), null)));
            System.out.println("hmm2: " + a + " data: " +
                    m.writeValueAsString(
                            scmAggService.groupByAndCalculatePrsDuration(
                                    company, ScmPrFilter.builder()
                                            .calculation(ScmPrFilter.CALCULATION.first_review_time)
                                            .across(a)
                                            .build(), null)));
            System.out.println("hmm3: " + a + " data: " +
                    m.writeValueAsString(
                            scmAggService.groupByAndCalculatePrsDuration(
                                    company, ScmPrFilter.builder()
                                            .calculation(ScmPrFilter.CALCULATION.first_review_to_merge_time)
                                            .across(a)
                                            .build(), null)));
        }

        Assertions.assertThat(
                scmAggService.groupByAndCalculatePrs(
                        company, ScmPrFilter.builder()
                                .integrationIds(List.of("1"))
                                .across(ScmPrFilter.DISTINCT.creator)
                                .build(), null).getTotalCount())
                .isEqualTo(1);

        Assertions.assertThat(
                        scmAggService.groupByAndCalculatePrs(
                                company, ScmPrFilter.builder()
                                        .integrationIds(List.of("1"))
                                        .across(ScmPrFilter.DISTINCT.approver)
                                        .build(), null).getTotalCount())
                .isEqualTo(1);

        DbListResponse<DbScmPullRequest> listResponse = scmAggService.list(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of("1"))
                        .across(ScmPrFilter.DISTINCT.approver)
                        .build(), Map.of(), null, 0, 10);
        Assertions.assertThat(listResponse.getRecords().get(0).getApprovers()).isEqualTo(List.of("Addepalli Venkata Manikyala Rao"));
        Assertions.assertThat(listResponse.getRecords().get(0).getCollabState()).isEqualTo("assigned-peer-approved");

        for (ScmFilesFilter.DISTINCT a : List.of(ScmFilesFilter.DISTINCT.values())) {
            System.out.println("files a: " + a + " data: " +
                    m.writeValueAsString(
                            scmAggService.groupByAndCalculateFiles(
                                    company, ScmFilesFilter.builder()
                                            .integrationIds(List.of("1"))
                                            .across(a)
                                            .build())));
        }
        Assertions.assertThat(scmAggService.list(
                company, ScmFilesFilter.builder().build(), Map.of(), 0, 1000)
                .getTotalCount())
                .isEqualTo(3);
        Assertions.assertThat(scmAggService.list(
                company, ScmFilesFilter.builder()
                        .integrationIds(List.of("1"))
                        .commitStartTime(1596111307L)
                        .commitEndTime(1606738507L)
                        .build(), Map.of(), 0, 1000)
                .getTotalCount())
                .isEqualTo(3);

        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(1);
        String user = userIdentityService.getUser(company, "1", "srinath.chandrashekhar@levelops.io");
        Assertions.assertThat(
                scmAggService.listCommits(
                        company,
                        ScmCommitFilter.builder()
                                .committers(List.of(user))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(2);
        Assertions.assertThat(
                scmAggService.listCommits(
                        company,
                        ScmCommitFilter.builder()
                                .committers(List.of("_UNKNOWN_"))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.listCommits(
                        company,
                        ScmCommitFilter.builder()
                                .authors(List.of(user))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(2);
    }

    @Test
    public void testCommittedAt() throws SQLException {
        DbListResponse<DbScmFile> list = scmAggService.list(company, ScmFilesFilter.builder()
                        .commitStartTime(1596111307L)
                        .commitEndTime(1606738507L)
                        .build(), Map.of(),
                0, 50);
        Assertions.assertThat(list.getTotalCount()).isEqualTo(3);
        list = scmAggService.list(company, ScmFilesFilter.builder()
                        .commitEndTime(1536111307L)
                        .build(), Map.of(),
                0, 50);
        Assertions.assertThat(list.getRecords().get(0).getNumCommits()).isEqualTo(0);
        list = scmAggService.list(company, ScmFilesFilter.builder()
                        .commitStartTime(1606738507L)
                        .build(), Map.of(),
                0, 50);
        Assertions.assertThat(list.getRecords().get(0).getNumCommits()).isEqualTo(0);
    }
}
