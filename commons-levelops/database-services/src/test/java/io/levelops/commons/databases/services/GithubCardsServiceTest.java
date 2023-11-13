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
import io.levelops.commons.databases.models.filters.GithubCardFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.github.models.GithubProject;
import io.levelops.integrations.github.models.GithubProjectCard;
import io.levelops.integrations.github.models.GithubProjectColumn;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.github.models.GithubWebhookEvent;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.database.github.DbGithubCardTransition.fromGithubCardTransitionCreateEvent;
import static io.levelops.commons.databases.models.database.github.DbGithubCardTransition.fromGithubCardTransitionDeleteEvent;
import static io.levelops.commons.databases.models.database.github.DbGithubCardTransition.fromGithubCardTransitionMovedEvent;

@Log4j2
public class GithubCardsServiceTest {
    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static GithubAggService githubAggService;
    private static ScmAggService scmAggService;
    private static String gitHubIntegrationId;
    private static Date currentTime;
    private static UserIdentityService userIdentityService;


    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;

        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        IntegrationService integrationService = new IntegrationService(dataSource);
        userIdentityService = new UserIdentityService(dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        githubAggService = new GithubAggService(dataSource);

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        gitHubIntegrationId = integrationService.insert(company, Integration.builder()
                .application("github")
                .name("github test")
                .status("enabled")
                .build());
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        githubAggService.ensureTableExistence(company);
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
    public void testStatusFilters() {
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.project)
                        .privateProject(true)
                        .build())
                .getRecords().size()).isEqualTo(1);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.project)
                        .privateProject(false)
                        .build())
                .getRecords().size()).isEqualTo(0);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.project)
                        .projects(List.of("Kanban Board 3"))
                        .build())
                .getRecords().size()).isEqualTo(1);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.project)
                        .projects(List.of("Kanban Board 3", "helloProj"))
                        .build())
                .getRecords().size()).isEqualTo(1);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.project)
                        .projects(List.of("UNKNOWN"))
                        .build())
                .getRecords().size()).isEqualTo(0);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.project)
                        .projectCreators(List.of("modo27"))
                        .build())
                .getRecords().size()).isEqualTo(1);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.project)
                        .projectCreators(List.of("UNKNOWN"))
                        .build())
                .getRecords().size()).isEqualTo(0);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.project)
                        .organizations(List.of("cog1"))
                        .build())
                .getRecords().size()).isEqualTo(1);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.project)
                        .organizations(List.of("UNKNOWN"))
                        .build())
                .getRecords().size()).isEqualTo(0);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.project)
                        .projectStates(List.of("open"))
                        .build())
                .getRecords().size()).isEqualTo(1);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.project)
                        .projectStates(List.of("closed"))
                        .build())
                .getRecords().size()).isEqualTo(0);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.project)
                        .columns(List.of("To do"))
                        .build())
                .getRecords().size()).isEqualTo(1);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.project)
                        .columns(List.of("WONT DO"))
                        .build())
                .getRecords().size()).isEqualTo(0);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.project)
                        .cardIds(List.of("65527424"))
                        .build())
                .getRecords().size()).isEqualTo(1);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.project)
                        .cardIds(List.of("61249999"))
                        .build())
                .getRecords().size()).isEqualTo(0);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.project)
                        .cardCreators(List.of("UNKNOWN"))
                        .build())
                .getRecords().size()).isEqualTo(0);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.project)
                        .cardCreators(List.of("modo27"))
                        .build())
                .getRecords().size()).isEqualTo(1);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.project)
                        .integrationIds(List.of(gitHubIntegrationId))
                        .build())
                .getRecords().size()).isEqualTo(1);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.project)
                        .integrationIds(List.of("999"))
                        .build())
                .getRecords().size()).isEqualTo(0);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.project)
                        .archivedCard(false)
                        .build())
                .getRecords().size()).isEqualTo(1);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.project)
                        .archivedCard(true)
                        .build())
                .getRecords().size()).isEqualTo(0);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.project)
                        .excludeColumns(List.of("closed"))
                        .build())
                .getRecords().size()).isEqualTo(1);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.project)
                        .excludeColumns(List.of("Done", "In progress", "To do"))
                        .build())
                .getRecords().size()).isEqualTo(0);
    }

    @Test
    public void testStatusAcross() {
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.project)
                        .build())
                .getRecords().size()).isEqualTo(1);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.card_creator)
                        .build())
                .getRecords().size()).isEqualTo(1);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.column)
                        .build())
                .getRecords().size()).isEqualTo(3);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.organization)
                        .build())
                .getRecords().size()).isEqualTo(1);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.project_creator)
                        .build())
                .getRecords().size()).isEqualTo(1);
    }

    @Test
    public void testCardResolutionTimeReport() {
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.card_creator)
                        .calculation(GithubCardFilter.CALCULATION.resolution_time)
                        .build())
                .getRecords().size()).isEqualTo(1);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.column)
                        .calculation(GithubCardFilter.CALCULATION.resolution_time)
                        .integrationIds(List.of(gitHubIntegrationId))
                        .excludeColumns(List.of("open"))
                        .build())
                .getRecords().size()).isEqualTo(3);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.column)
                        .calculation(GithubCardFilter.CALCULATION.resolution_time)
                        .integrationIds(List.of(gitHubIntegrationId))
                        .excludeColumns(List.of("WONT DO", "To do"))
                        .build())
                .getRecords().size()).isEqualTo(2);
    }

    @Test
    public void testCardTimeAcrossStagesReport() {
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.card_creator)
                        .calculation(GithubCardFilter.CALCULATION.stage_times_report)
                        .build())
                .getRecords().size()).isEqualTo(1);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.card_creator)
                        .calculation(GithubCardFilter.CALCULATION.stage_times_report)
                        .build())
                .getRecords().get(0).getKey()).isEqualTo("modo27");
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.card_creator)
                        .calculation(GithubCardFilter.CALCULATION.stage_times_report)
                        .build())
                .getRecords().get(0).getCount()).isEqualTo(3);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.column)
                        .calculation(GithubCardFilter.CALCULATION.stage_times_report)
                        .columns(List.of("To do"))
                        .build())
                .getRecords().size()).isEqualTo(1);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.column)
                        .calculation(GithubCardFilter.CALCULATION.stage_times_report)
                        .columns(List.of("To do"))
                        .build())
                .getRecords().get(0).getKey()).isEqualTo("To do");
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.column)
                        .calculation(GithubCardFilter.CALCULATION.stage_times_report)
                        .columns(List.of("To do"))
                        .build())
                .getRecords().get(0).getCount()).isEqualTo(2);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.column)
                        .calculation(GithubCardFilter.CALCULATION.stage_times_report)
                        .columns(List.of("To do"))
                        .build())
                .getRecords().get(0).getMean()).isEqualTo(2124.0);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.project)
                        .calculation(GithubCardFilter.CALCULATION.stage_times_report)
                        .columns(List.of("To do"))
                        .build())
                .getRecords().size()).isEqualTo(1);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.project_creator)
                        .calculation(GithubCardFilter.CALCULATION.stage_times_report)
                        .columns(List.of("To do"))
                        .build())
                .getRecords().size()).isEqualTo(1);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.column)
                        .calculation(GithubCardFilter.CALCULATION.stage_times_report)
                        .columns(List.of("In progress"))
                        .build())
                .getRecords().size()).isEqualTo(1);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.column)
                        .calculation(GithubCardFilter.CALCULATION.stage_times_report)
                        .integrationIds(List.of(gitHubIntegrationId))
                        .excludeColumns(List.of("To do"))
                        .build())
                .getRecords().size()).isEqualTo(2);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.column)
                        .calculation(GithubCardFilter.CALCULATION.stage_times_report)
                        .integrationIds(List.of(gitHubIntegrationId))
                        .currentColumns(List.of("To do"))
                        .build())
                .getRecords().size()).isEqualTo(1);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.column)
                        .calculation(GithubCardFilter.CALCULATION.stage_times_report)
                        .integrationIds(List.of(gitHubIntegrationId))
                        .currentColumns(List.of("Done"))
                        .build())
                .getRecords().size()).isEqualTo(1);
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.column)
                        .calculation(GithubCardFilter.CALCULATION.stage_times_report)
                        .integrationIds(List.of(gitHubIntegrationId))
                        .excludeColumns(List.of("WONT DO", "To do"))
                        .build())
                .getRecords().size()).isEqualTo(2);
    }

    @Test
    public void testSort() {
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.project)
                        .build()).getTotalCount()).isEqualTo(1);
        List<String> keys = githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.project)
                        .sort(Map.of("project", SortingOrder.DESC))
                        .build()).getRecords().stream().map(DbAggregationResult::getKey).map(String::toLowerCase).collect(Collectors.toList());
        Assertions.assertThat(keys).isSortedAccordingTo(Comparator.comparing(String::valueOf).reversed());
        Assertions.assertThat(githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.project)
                        .sort(Map.of("project", SortingOrder.DESC))
                        .build()).getTotalCount()).isEqualTo(1);
        keys = githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.project)
                        .sort(Map.of("project", SortingOrder.ASC))
                        .build()).getRecords().stream().map(DbAggregationResult::getKey).map(String::toLowerCase).collect(Collectors.toList());
        Assertions.assertThat(keys).isSortedAccordingTo(Comparator.comparing(String::valueOf));
    }

    @Test
    public void testStacks() throws SQLException {
        Assertions.assertThat(githubAggService.stackedGroupBy(company, GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.column)
                        .build(),
                List.of(GithubCardFilter.DISTINCT.column, GithubCardFilter.DISTINCT.card_creator,
                        GithubCardFilter.DISTINCT.project, GithubCardFilter.DISTINCT.project_creator,
                        GithubCardFilter.DISTINCT.organization), null).getTotalCount()).isEqualTo(3);
        Assertions.assertThat(githubAggService.stackedGroupBy(company, GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.column)
                        .build(),
                List.of(GithubCardFilter.DISTINCT.card_creator), null).getTotalCount()).isEqualTo(3);
        Assertions.assertThat(githubAggService.stackedGroupBy(company, GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.column)
                        .build(),
                List.of(GithubCardFilter.DISTINCT.card_creator), null).getRecords().get(0).getKey()).isEqualTo("In progress");
        Assertions.assertThat(githubAggService.stackedGroupBy(company, GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.column)
                        .build(),
                List.of(GithubCardFilter.DISTINCT.card_creator), null).getRecords().get(0).getStacks().get(0).getKey()).isEqualTo("modo27");
        Assertions.assertThat(githubAggService.stackedGroupBy(company, GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.column)
                        .build(),
                List.of(GithubCardFilter.DISTINCT.card_creator), null).getRecords().get(1).getKey()).isEqualTo("Done");
        Assertions.assertThat(githubAggService.stackedGroupBy(company, GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.column)
                        .build(),
                List.of(GithubCardFilter.DISTINCT.card_creator), null).getRecords().get(1).getStacks().get(0).getKey()).isEqualTo("modo27");
        Assertions.assertThat(githubAggService.stackedGroupBy(company, GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.issue_created)
                        .build(),
                List.of(GithubCardFilter.DISTINCT.column), null).getRecords().get(0).getStacks().get(0).getKey()).isEqualTo("In progress");
        Assertions.assertThat(githubAggService.stackedGroupBy(company, GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.issue_closed)
                        .build(),
                List.of(GithubCardFilter.DISTINCT.column),null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(githubAggService.stackedGroupBy(company, GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.issue_created)
                        .build(),
                List.of(GithubCardFilter.DISTINCT.card_creator),null).getRecords().get(0).getStacks().get(0).getKey()).isEqualTo("modo27");
        Assertions.assertThat(githubAggService.stackedGroupBy(company, GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.issue_closed)
                        .build(),
                List.of(GithubCardFilter.DISTINCT.card_creator),null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(githubAggService.stackedGroupBy(company, GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.issue_created)
                        .build(),
                List.of(GithubCardFilter.DISTINCT.assignee),null).getRecords().get(0).getStacks().get(0).getKey()).isEqualTo("modo");
        Assertions.assertThat(githubAggService.stackedGroupBy(company, GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.issue_closed)
                        .build(),
                List.of(GithubCardFilter.DISTINCT.assignee),null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(githubAggService.stackedGroupBy(company, GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.issue_created)
                        .build(),
                List.of(GithubCardFilter.DISTINCT.project),null).getRecords().get(0).getStacks().get(0).getKey()).isEqualTo("Kanban Board 3");
        Assertions.assertThat(DefaultObjectMapper.writeAsPrettyJson(githubAggService.stackedGroupBy(company, GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.issue_created)
                        .build(),
                List.of(GithubCardFilter.DISTINCT.project), null))).isNotNull();
        Assertions.assertThat(githubAggService.stackedGroupBy(company, GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.issue_closed)
                        .build(),
                List.of(GithubCardFilter.DISTINCT.project), null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(githubAggService.stackedGroupBy(company, GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.issue_created)
                        .build(),
                List.of(GithubCardFilter.DISTINCT.label), null).getRecords().get(0).getStacks().get(0).getKey()).isEqualTo("java");
    }


    @Test
    public void testPagination() throws SQLException {
        DbListResponse<DbAggregationResult> result = githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.assignee)
                        .build(), null, 0, 1);
        Assertions.assertThat(result.getCount()).isEqualTo(1);
        Assertions.assertThat(result.getTotalCount()).isEqualTo(5);
        result = githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter
                        .builder()
                        .across(GithubCardFilter.DISTINCT.assignee)
                        .build(), null, 1, 1);
        Assertions.assertThat(result.getCount()).isEqualTo(1);
        Assertions.assertThat(result.getTotalCount()).isEqualTo(5);
    }
}

