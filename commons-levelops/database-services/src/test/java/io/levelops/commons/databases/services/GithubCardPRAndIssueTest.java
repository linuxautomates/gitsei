package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.github.DbGithubCardTransition;
import io.levelops.commons.databases.models.database.github.DbGithubProject;
import io.levelops.commons.databases.models.database.github.DbGithubProjectCard;
import io.levelops.commons.databases.models.database.github.DbGithubProjectColumn;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.models.database.scm.DbScmIssue;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.GithubCardFilter;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.github.models.GithubProject;
import io.levelops.integrations.github.models.GithubProjectCard;
import io.levelops.integrations.github.models.GithubProjectColumn;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.github.models.GithubWebhookEvent;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static io.levelops.commons.databases.models.database.github.DbGithubCardTransition.fromGithubCardTransitionCreateEvent;
import static io.levelops.commons.databases.models.database.github.DbGithubCardTransition.fromGithubCardTransitionDeleteEvent;
import static io.levelops.commons.databases.models.database.github.DbGithubCardTransition.fromGithubCardTransitionMovedEvent;

public class GithubCardPRAndIssueTest {
    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    public static final String UNKNOWN = "UNKNOWN";
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static GithubAggService githubAggService;
    private static String gitHubIntegrationId;
    private static ProductsDatabaseService productsDatabaseService;
    private static ScmAggService scmAggService;
    private static Date currentTime;
    private static UserIdentityService userIdentityService;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        userIdentityService = new UserIdentityService(dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        IntegrationService integrationService = new IntegrationService(dataSource);
        productsDatabaseService = new ProductsDatabaseService(dataSource, DefaultObjectMapper.get());
        githubAggService = new GithubAggService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        productsDatabaseService.ensureTableExistence(company);
        gitHubIntegrationId = integrationService.insert(company, Integration.builder()
                .application("github")
                .name("github test")
                .status("enabled")
                .build());
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        githubAggService.ensureTableExistence(company);
        currentTime = new Date();
        List<DbRepository> repos = new ArrayList<>();

        String issuesInput = ResourceUtils.getResourceAsString("json/databases/github_issues_cards.json");
        PaginatedResponse<GithubRepository> issues = m.readValue(issuesInput,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        currentTime = new Date();
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

        String input = ResourceUtils.getResourceAsString("json/databases/github_projects_2.json");
        PaginatedResponse<GithubProject> projects = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubProject.class));
        projects.getResponse().getRecords().forEach(project -> {
            if (githubAggService.getProject(company, project.getId(), gitHubIntegrationId).isEmpty()) {
                DbGithubProject dbProject = DbGithubProject.fromProject(project, gitHubIntegrationId);
                String projectId = githubAggService.insert(company, dbProject);
                List<GithubProjectColumn> columns = project.getColumns();
                if (columns != null) {
                    columns.forEach(column -> {
                        if (githubAggService.getColumn(company, projectId, column.getId()).isEmpty()) {
                            DbGithubProjectColumn dbColumn = DbGithubProjectColumn.fromProjectColumn(column, projectId);
                            String columnId = githubAggService.insertColumn(company, dbColumn);
                            List<GithubProjectCard> cards = column.getCards();
                            if (cards != null) {
                                cards.forEach(card -> {
                                    if (githubAggService.getCard(company, columnId, card.getId()).isEmpty()) {
                                        DbGithubProjectCard dbCard = DbGithubProjectCard.fromProjectCard(card, columnId);
                                        githubAggService.insertCard(company, gitHubIntegrationId, dbCard);
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });


        String inputEvent = ResourceUtils.getResourceAsString("json/databases/github_webhook_events_2.json");
        List<GithubWebhookEvent> events = m.readValue(inputEvent, m.getTypeFactory()
                .constructCollectionType(List.class, GithubWebhookEvent.class));
        events.forEach(event ->
        {
            switch (event.getAction()) {
                case "created":
                    DbGithubCardTransition create = fromGithubCardTransitionCreateEvent(gitHubIntegrationId, event);
                    githubAggService.insertCardTransition(company, create);
                    break;
                case "moved":
                    DbGithubCardTransition to = fromGithubCardTransitionCreateEvent(gitHubIntegrationId, event);
                    DbGithubCardTransition from = fromGithubCardTransitionMovedEvent(gitHubIntegrationId, event);
                    githubAggService.insertCardTransition(company, to);
                    githubAggService.updateCardTransition(company, from);
                    break;
                case "deleted":
                    DbGithubCardTransition delete = fromGithubCardTransitionDeleteEvent(gitHubIntegrationId, event);
                    githubAggService.updateCardTransition(company, delete);
                    break;
            }
        });
    }

    @Test
    public void testPRAndIssueInsert() throws SQLException {
        Assertions.assertThat(githubAggService
                .groupByAndCalculateCards(company,
                        GithubCardFilter
                                .builder()
                                .across(GithubCardFilter.DISTINCT.column)
                                .repoIds(List.of("cog1/shiny-enigma"))
                                .build()).getTotalCount()).isEqualTo(3);
        Assertions.assertThat(githubAggService
                .groupByAndCalculateCards(company,
                        GithubCardFilter
                                .builder()
                                .across(GithubCardFilter.DISTINCT.column)
                                .repoIds(List.of("cog1/shiny"))
                                .build()).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(githubAggService
                .groupByAndCalculateCards(company,
                        GithubCardFilter
                                .builder()
                                .across(GithubCardFilter.DISTINCT.repo_id)
                                .repoIds(List.of("cog1/shiny-enigma"))
                                .build()).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(githubAggService
                .groupByAndCalculateCards(company,
                        GithubCardFilter
                                .builder()
                                .across(GithubCardFilter.DISTINCT.label)
                                .repoIds(List.of("cog1/shiny-enigma"))
                                .build()).getTotalCount()).isEqualTo(3);
        Assertions.assertThat(githubAggService
                .groupByAndCalculateCards(company,
                        GithubCardFilter
                                .builder()
                                .across(GithubCardFilter.DISTINCT.assignee)
                                .repoIds(List.of("cog1/shiny-enigma"))
                                .build()).getTotalCount()).isEqualTo(5);
        Assertions.assertThat(githubAggService
                .groupByAndCalculateCards(company,
                        GithubCardFilter
                                .builder()
                                .across(GithubCardFilter.DISTINCT.issue_closed)
                                .repoIds(List.of("cog1/shiny-enigma"))
                                .build()).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(githubAggService
                .groupByAndCalculateCards(company,
                        GithubCardFilter
                                .builder()
                                .across(GithubCardFilter.DISTINCT.issue_created)
                                .repoIds(List.of("cog1/shiny-enigma"))
                                .build()).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(githubAggService
                .groupByAndCalculateCards(company,
                        GithubCardFilter
                                .builder()
                                .across(GithubCardFilter.DISTINCT.issue_created)
                                .repoIds(List.of("cog1/shiny-enigma"))
                                .build()).getRecords().get(0).getAdditionalKey()).isEqualTo("23-7-2021");
        Assertions.assertThat(githubAggService
                .groupByAndCalculateCards(company,
                        GithubCardFilter
                                .builder()
                                .across(GithubCardFilter.DISTINCT.issue_created)
                                .repoIds(List.of("cog1/shiny-enigma"))
                                .aggInterval(AGG_INTERVAL.week)
                                .build()).getRecords().get(0).getAdditionalKey()).isEqualTo("29-2021");
        Assertions.assertThat(githubAggService
                .groupByAndCalculateCards(company,
                        GithubCardFilter
                                .builder()
                                .across(GithubCardFilter.DISTINCT.issue_created)
                                .repoIds(List.of("cog1/shiny-enigma"))
                                .aggInterval(AGG_INTERVAL.week)
                                .build()).getRecords().get(0).getCount()).isEqualTo(7);
        Assertions.assertThat(githubAggService
                .groupByAndCalculateCards(company,
                        GithubCardFilter
                                .builder()
                                .across(GithubCardFilter.DISTINCT.issue_created)
                                .labels(List.of("java"))
                                .aggInterval(AGG_INTERVAL.week)
                                .build()).getRecords().get(0).getAdditionalKey()).isEqualTo("29-2021");
        Assertions.assertThat(githubAggService
                .groupByAndCalculateCards(company,
                        GithubCardFilter
                                .builder()
                                .across(GithubCardFilter.DISTINCT.issue_created)
                                .labels(List.of("java"))
                                .aggInterval(AGG_INTERVAL.week)
                                .build()).getRecords().get(0).getCount()).isEqualTo(7);
        Assertions.assertThat(githubAggService
                .groupByAndCalculateCards(company,
                        GithubCardFilter
                                .builder()
                                .across(GithubCardFilter.DISTINCT.issue_created)
                                .labels(List.of("python"))
                                .aggInterval(AGG_INTERVAL.week)
                                .build()).getRecords().get(0).getCount()).isEqualTo(5);
        Assertions.assertThat(githubAggService
                .groupByAndCalculateCards(company,
                        GithubCardFilter
                                .builder()
                                .across(GithubCardFilter.DISTINCT.issue_created)
                                .labels(List.of("c++"))
                                .aggInterval(AGG_INTERVAL.week)
                                .build()).getRecords().get(0).getCount()).isEqualTo(2);
        Assertions.assertThat(githubAggService
                .groupByAndCalculateCards(company,
                        GithubCardFilter
                                .builder()
                                .across(GithubCardFilter.DISTINCT.assignee)
                                .assignees(List.of("modo"))
                                .aggInterval(AGG_INTERVAL.week)
                                .build()).getTotalCount()).isEqualTo(4);
        Assertions.assertThat(githubAggService
                .groupByAndCalculateCards(company,
                        GithubCardFilter
                                .builder()
                                .across(GithubCardFilter.DISTINCT.assignee)
                                .assignees(List.of("modo", "arpit"))
                                .aggInterval(AGG_INTERVAL.week)
                                .build()).getTotalCount()).isEqualTo(4);
        Assertions.assertThat(githubAggService
                .groupByAndCalculateCards(company,
                        GithubCardFilter
                                .builder()
                                .across(GithubCardFilter.DISTINCT.assignee)
                                .issueCreatedRange(ImmutablePair.of(1626928650L, 1627149650L))
                                .build()).getTotalCount()).isEqualTo(5);
        Assertions.assertThat(githubAggService
                .groupByAndCalculateCards(company,
                        GithubCardFilter
                                .builder()
                                .across(GithubCardFilter.DISTINCT.assignee)
                                .assignees(List.of("modo", "arpit"))
                                .aggInterval(AGG_INTERVAL.week)
                                .sort(Map.of("assignee", SortingOrder.DESC))
                                .build()).getTotalCount()).isEqualTo(4);


    }
}
