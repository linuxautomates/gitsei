package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.converters.gerrit.GerritPullRequestConverters;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.gerrit.models.ProjectInfo;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;

public class GerritAggServiceTest {

    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static UserIdentityService userIdentityService;
    private static TeamMembersDatabaseService teamMembersDatabaseService;
    private static ScmAggService scmAggService;

    private static Date currentTime;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;

        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        userIdentityService = new UserIdentityService(dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);

        IntegrationService integrationService = new IntegrationService(dataSource);
        GerritRepositoryService repositoryService = new GerritRepositoryService(dataSource);
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, m);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        integrationService.insert(company, Integration.builder()
                .application("gerrit")
                .name("gerrit test")
                .status("enabled")
                .build());
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        repositoryService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);
        currentTime = DateUtils.truncate(new Date(), Calendar.DATE);

        String input = ResourceUtils.getResourceAsString("json/databases/gerrit_prs.json");
        PaginatedResponse<ProjectInfo> prs = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, ProjectInfo.class));
        currentTime = new Date();
        List<DbRepository> repos = new ArrayList<>();
        prs.getResponse().getRecords().forEach(repo -> {
            repos.add(DbRepository.fromGerritProject(repo, "1"));
            repo.getChanges()
                    .forEach(review -> {
                        try {
                            DbScmPullRequest tmp = GerritPullRequestConverters
                                    .parsePullRequest("1", review);
                            scmAggService.insert(company, tmp);
                        } catch (SQLException throwable) {
                            throwable.printStackTrace();
                        }
                    });
        });
        repositoryService.batchUpsert(company, repos).size();
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
    }
}
