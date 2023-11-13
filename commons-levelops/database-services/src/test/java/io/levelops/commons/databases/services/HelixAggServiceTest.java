package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.RepoConfigEntryMatcher;
import io.levelops.commons.databases.models.filters.ScmFilesFilter;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.helix_swarm.models.HelixSwarmReview;
import io.levelops.integrations.helixcore.models.HelixCoreChangeList;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
public class HelixAggServiceTest {

    private static final String company = "test";
    private final ObjectMapper m = DefaultObjectMapper.get();
    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private DataSource dataSource;
    private ScmAggService scmAggService;
    private UserIdentityService userIdentityService;
    private TeamMembersDatabaseService teamMembersDatabaseService;
    private RepoConfigEntryMatcher repoConfigEntryMatcher = new RepoConfigEntryMatcher(List.of(
            IntegrationConfig.RepoConfigEntry.builder().repoId("DummyProject").pathPrefix("//DummyProject/main").build(),
            IntegrationConfig.RepoConfigEntry.builder().repoId("sandbox").pathPrefix("//sandbox/main").build(),
            IntegrationConfig.RepoConfigEntry.builder().repoId("JamCode").pathPrefix("//JamCode/main").build()
    ));

    @Before
    public void setup() throws Exception {
        if (dataSource != null)
            return;

        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        userIdentityService = new UserIdentityService(dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, DefaultObjectMapper.get());
        IntegrationService integrationService = new IntegrationService(dataSource);

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        integrationService.insert(company, Integration.builder()
                .application("helix")
                .name("helix test")
                .status("enabled")
                .build());
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);
        List<IntegrationConfig.RepoConfigEntry> configEntries = List.of(IntegrationConfig.RepoConfigEntry.builder()
                        .repoId("dummy").pathPrefix("//DummyProjec").build(),
                IntegrationConfig.RepoConfigEntry.builder()
                        .repoId("sandbox").pathPrefix("//sandbox").build(),
                IntegrationConfig.RepoConfigEntry.builder()
                        .repoId("jam").pathPrefix("//JamCode").build());
        
        String input = ResourceUtils.getResourceAsString("json/databases/helix_changelist.json");
        PaginatedResponse<HelixCoreChangeList> helixChangelist = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, HelixCoreChangeList.class));
        helixChangelist.getResponse().getRecords().forEach(changeList -> {
            List<DbScmFile> dbScmFiles = DbScmFile.fromHelixCoreChangeList(changeList, "1", configEntries);
            Set<String> repoIds = dbScmFiles.stream().map(DbScmFile::getRepoId).collect(Collectors.toSet());
            DbScmCommit helixCoreCommit = DbScmCommit.fromHelixCoreChangeList(changeList, repoIds, "1");
            if (CollectionUtils.isNotEmpty(repoIds) && scmAggService.getCommit(company, helixCoreCommit.getCommitSha(),
                    helixCoreCommit.getRepoIds(), "1").isEmpty() && dbScmFiles.size() > 0) {
                try {
                    scmAggService.insertCommit(company, helixCoreCommit);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                dbScmFiles.forEach(dbScmFile -> scmAggService.insertFile(company, dbScmFile));
            }
        });
        
        Date ingestedAt = DateUtils.truncate(new Date(), Calendar.DATE);
        input = ResourceUtils.getResourceAsString("json/databases/helix_reviews.json");
        PaginatedResponse<HelixSwarmReview> helixReviews = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, HelixSwarmReview.class));
        helixReviews.getResponse().getRecords().forEach(review -> {
            Set<String> repoIdsSet = review.getVersions().stream()
                    .map(version -> repoConfigEntryMatcher.matchPrefix(version.getStream()))
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet());
            DbScmPullRequest dbScmPullRequest = DbScmPullRequest.fromHelixSwarmReview(review, repoIdsSet, "1");
            try {
                scmAggService.insert(company, dbScmPullRequest);
                List<String> repoIds = dbScmPullRequest.getRepoIds();
                ListUtils.emptyIfNull(review.getVersions()).stream()
                        .map(change -> DbScmCommit.fromHelixSwarmVersion(change, repoIds, "1", ingestedAt))
                        .forEach(dbScmCommit -> {
                            try {
                                scmAggService.insertCommit(company, dbScmCommit);
                            } catch (Exception e) {
                                log.error("setupHelixSwarmReviews: error inserting commit: " + dbScmCommit, e);
                            }
                        });
            } catch (SQLException e) {
                log.error("setupHelixSwarmReviews: error inserting review: " + review, e);
            }
        });
    }

    @Test
    public void testCommittedAt() throws SQLException {
        DbListResponse<DbScmFile> list = scmAggService.list(company, ScmFilesFilter.builder()
                        .build(), Map.of(),
                0, 50);
        Assertions.assertThat(list.getTotalCount()).isEqualTo(16);
        list = scmAggService.list(company, ScmFilesFilter.builder()
                        .commitEndTime(1617183427L)
                        .commitStartTime(1615665027L)
                        .build(), Map.of(),
                0, 50);
        Assertions.assertThat(list.getTotalCount()).isEqualTo(16);
        list = scmAggService.list(company, ScmFilesFilter.builder()
                        .commitEndTime(1617193427L)
                        .commitStartTime(1617183427L)
                        .build(), Map.of(),
                0, 50);
        Assertions.assertThat(list.getRecords().get(0).getNumCommits()).isEqualTo(0);
        list = scmAggService.list(company, ScmFilesFilter.builder()
                        .commitStartTime(1617183427L)
                        .build(), Map.of(),
                0, 50);
        Assertions.assertThat(list.getRecords().get(0).getNumCommits()).isEqualTo(0);
        list = scmAggService.list(company, ScmFilesFilter.builder()
                        .commitEndTime(1614665027L)
                        .build(), Map.of(),
                0, 50);
        Assertions.assertThat(list.getRecords().get(0).getNumCommits()).isEqualTo(0);
    }
}
