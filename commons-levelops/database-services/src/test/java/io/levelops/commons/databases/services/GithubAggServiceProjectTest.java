package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.github.DbGithubProject;
import io.levelops.commons.databases.models.database.github.DbGithubProjectCard;
import io.levelops.commons.databases.models.database.github.DbGithubProjectColumn;
import io.levelops.commons.databases.models.database.organization.DBOrgProduct;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.models.database.scm.DbScmIssue;
import io.levelops.commons.databases.models.filters.GithubCardFilter;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.databases.services.scm.ScmProductServiceUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.github.models.GithubProject;
import io.levelops.integrations.github.models.GithubProjectCard;
import io.levelops.integrations.github.models.GithubProjectColumn;
import io.levelops.integrations.github.models.GithubRepository;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class GithubAggServiceProjectTest {

    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    public static final String UNKNOWN = "UNKNOWN";
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static GithubAggService githubAggService;
    private static ScmAggService scmAggService;
    private static String gitHubIntegrationId;
    private static ProductsDatabaseService productsDatabaseService;
    private static UserIdentityService userIdentityService;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        IntegrationService integrationService = new IntegrationService(dataSource);
        productsDatabaseService = new ProductsDatabaseService(dataSource, DefaultObjectMapper.get());
        githubAggService = new GithubAggService(dataSource);
        userIdentityService = new UserIdentityService(dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
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
    }

    @Test
    public void test() throws IOException, SQLException {
        String issuesInput = ResourceUtils.getResourceAsString("json/databases/github_issues_cards.json");
        PaginatedResponse<GithubRepository> issues = m.readValue(issuesInput,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        List<DbRepository> repos = new ArrayList<>();
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
        projects.getResponse().getRecords().forEach(this::insertProject);
        testList();
    }

    private void testList() throws SQLException {
        Assertions.assertThat(githubAggService.list(
                company,
                GithubCardFilter.builder().build(),
                Map.of(), 0, 1000)
                .getTotalCount())
                .isEqualTo(0);

        Assertions.assertThat(githubAggService.list(
                company,
                GithubCardFilter.builder()
                        .projectCreators(List.of("modo27"))
                        .build(),
                Map.of(), 0, 1000)
                .getTotalCount())
                .isEqualTo(0);

        Assertions.assertThat(githubAggService.list(
                company,
                GithubCardFilter.builder()
                        .projectCreators(List.of(UNKNOWN))
                        .build(),
                Map.of(), 0, 1000)
                .getTotalCount())
                .isEqualTo(0);

        Assertions.assertThat(githubAggService.list(
                company,
                GithubCardFilter.builder()
                        .organizations(List.of("cog1"))
                        .build(),
                Map.of(), 0, 1000)
                .getTotalCount())
                .isEqualTo(0);

        Assertions.assertThat(githubAggService.list(
                company,
                GithubCardFilter.builder()
                        .organizations(List.of(UNKNOWN))
                        .build(),
                Map.of(), 0, 1000)
                .getTotalCount())
                .isEqualTo(0);

        Assertions.assertThat(githubAggService.list(
                company,
                GithubCardFilter.builder()
                        .projectStates(List.of("open"))
                        .build(),
                Map.of(), 0, 1000)
                .getTotalCount())
                .isEqualTo(0);

        Assertions.assertThat(githubAggService.list(
                company,
                GithubCardFilter.builder()
                        .projectStates(List.of(UNKNOWN))
                        .build(),
                Map.of(), 0, 1000)
                .getTotalCount())
                .isEqualTo(0);

        Assertions.assertThat(githubAggService.list(
                company,
                GithubCardFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .columns(List.of("To do"))
                        .build(),
                Map.of(), 0, 1000)
                .getTotalCount())
                .isEqualTo(0);

        Assertions.assertThat(githubAggService.list(
                company,
                GithubCardFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .columns(List.of("In progress"))
                        .build(),
                Map.of(), 0, 1000)
                .getTotalCount())
                .isEqualTo(0);

        Assertions.assertThat(githubAggService.list(
                company,
                GithubCardFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .columns(List.of(UNKNOWN))
                        .build(),
                Map.of(), 0, 1000)
                .getTotalCount())
                .isEqualTo(0);

        Assertions.assertThat(githubAggService.list(
                company,
                GithubCardFilter.builder()
                        .cardIds(List.of("65527424", "61249133"))
                        .build(),
                Map.of(), 0, 1000)
                .getTotalCount())
                .isEqualTo(0);

        Assertions.assertThat(githubAggService.list(
                company,
                GithubCardFilter.builder()
                        .cardIds(List.of("12345678"))
                        .build(),
                Map.of(), 0, 1000)
                .getTotalCount())
                .isEqualTo(0);

        Assertions.assertThat(githubAggService.list(
                company,
                GithubCardFilter.builder()
                        .cardCreators(List.of("modo27"))
                        .build(),
                Map.of(), 0, 1000)
                .getTotalCount())
                .isEqualTo(0);

        Assertions.assertThat(githubAggService.list(
                company,
                GithubCardFilter.builder()
                        .cardCreators(List.of(UNKNOWN))
                        .build(),
                Map.of(), 0, 1000)
                .getTotalCount())
                .isEqualTo(0);

        Assertions.assertThat(githubAggService.list(
                company,
                GithubCardFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .build(),
                Map.of(), 0, 1000)
                .getTotalCount())
                .isEqualTo(0);

        Assertions.assertThat(githubAggService.list(
                company,
                GithubCardFilter.builder()
                        .integrationIds(List.of("0"))
                        .build(),
                Map.of(), 0, 1000)
                .getTotalCount())
                .isEqualTo(0);

        Assertions.assertThat(githubAggService.list(
                company,
                GithubCardFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .excludeColumns(List.of("To do"))
                        .build(),
                Map.of(), 0, 1000)
                .getTotalCount())
                .isEqualTo(0);

        Assertions.assertThat(githubAggService.list(
                company,
                GithubCardFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .excludeColumns(List.of("To do", "In progress"))
                        .build(),
                Map.of(), 0, 1000)
                .getTotalCount())
                .isEqualTo(0);

        Assertions.assertThat(githubAggService.list(
                company,
                GithubCardFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .privateProject(true)
                        .build(),
                Map.of(), 0, 1000)
                .getTotalCount())
                .isEqualTo(0);

        Assertions.assertThat(githubAggService.list(
                company,
                GithubCardFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .privateProject(false)
                        .build(),
                Map.of(), 0, 1000)
                .getTotalCount())
                .isEqualTo(0);

        Assertions.assertThat(githubAggService.list(
                company,
                GithubCardFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .archivedCard(true)
                        .build(),
                Map.of(), 0, 1000)
                .getTotalCount())
                .isEqualTo(0);

        Assertions.assertThat(githubAggService.list(
                company,
                GithubCardFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .archivedCard(false)
                        .build(),
                Map.of(), 0, 1000)
                .getTotalCount())
                .isEqualTo(0);

        Assertions.assertThat(githubAggService.list(
                company,
                GithubCardFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .organizations(List.of("cog1"))
                        .projectCreators(List.of("modo27"))
                        .projects(List.of("Kanban Board 3"))
                        .columns(List.of("To do"))
                        .privateProject(true)
                        .archivedCard(false)
                        .build(),
                Map.of(), 0, 1000)
                .getTotalCount())
                .isEqualTo(0);
        DBOrgProduct productOne = ScmProductServiceUtils.getProductWithNoIntegAndFilter();
        String uuid = productsDatabaseService.insert(company, productOne);
        Assertions.assertThat(githubAggService.list(
                company,
                GithubCardFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .organizations(List.of("cog1"))
                        .orgProductIds(Set.of(UUID.fromString(uuid)))
                        .projectCreators(List.of("modo27"))
                        .projects(List.of("Kanban Board 3"))
                        .columns(List.of("To do"))
                        .privateProject(true)
                        .archivedCard(false)
                        .build(),
                Map.of(), 0, 1000)
                .getTotalCount())
                .isEqualTo(0);
        productOne = ScmProductServiceUtils.getProductWithIntegAndFilter();
        uuid = productsDatabaseService.insert(company, productOne);
        Assertions.assertThat(githubAggService.list(
                company,
                GithubCardFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .orgProductIds(Set.of(UUID.fromString(uuid)))
                        .build(),
                Map.of(), 0, 1000)
                .getTotalCount())
                .isEqualTo(9);
    }

    private void insertProject(GithubProject project) {
        if (githubAggService.getProject(company, project.getId(), gitHubIntegrationId).isEmpty()) {
            DbGithubProject dbProject = DbGithubProject.fromProject(project, gitHubIntegrationId);
            String projectId = githubAggService.insert(company, dbProject);
            Assert.assertNotNull(projectId);
            Optional<DbGithubProject> insertedProject = githubAggService.getProject(company, project.getId(), gitHubIntegrationId);
            Assert.assertTrue(insertedProject.isPresent());
            validateProjects(dbProject, insertedProject.get());
            insertColumns(project, projectId);
        }
    }

    private void validateProjects(DbGithubProject dbProject, DbGithubProject project) {
        Assert.assertEquals(dbProject.getProjectId(), project.getProjectId());
        Assert.assertEquals(dbProject.getProject(), project.getProject());
        Assert.assertEquals(dbProject.getIntegrationId(), project.getIntegrationId());
        Assert.assertEquals(dbProject.getOrganization(), project.getOrganization());
        Assert.assertEquals(dbProject.getDescription(), project.getDescription());
        Assert.assertEquals(dbProject.getState(), project.getState());
        Assert.assertEquals(dbProject.getCreator(), project.getCreator());
        Assert.assertEquals(dbProject.getIsPrivate(), project.getIsPrivate());
        Assert.assertEquals(dbProject.getProjectCreatedAt(), project.getProjectCreatedAt());
        Assert.assertEquals(dbProject.getProjectUpdatedAt(), project.getProjectUpdatedAt());
    }

    private void insertColumns(GithubProject project, String projectId) {
        List<GithubProjectColumn> columns = project.getColumns();
        if (columns != null) {
            columns.forEach(column -> insertColumn(column, projectId));
        }
    }

    private void insertColumn(GithubProjectColumn column, String projectId) {
        if (githubAggService.getColumn(company, projectId, column.getId()).isEmpty()) {
            DbGithubProjectColumn dbColumn = DbGithubProjectColumn.fromProjectColumn(column, projectId);
            String columnId = githubAggService.insertColumn(company, dbColumn);
            Assert.assertNotNull(columnId);
            Optional<DbGithubProjectColumn> insertedColumn = githubAggService.getColumn(company, projectId, column.getId());
            Assert.assertTrue(insertedColumn.isPresent());
            validateColumns(dbColumn, insertedColumn.get());
            insertCards(column, columnId);
        }
    }

    private void validateColumns(DbGithubProjectColumn dbColumn, DbGithubProjectColumn column) {
        Assert.assertEquals(dbColumn.getProjectId(), column.getProjectId());
        Assert.assertEquals(dbColumn.getColumnId(), column.getColumnId());
        Assert.assertEquals(dbColumn.getName(), column.getName());
        Assert.assertEquals(dbColumn.getColumnCreatedAt(), column.getColumnCreatedAt());
        Assert.assertEquals(dbColumn.getColumnUpdatedAt(), column.getColumnUpdatedAt());
    }

    private void insertCards(GithubProjectColumn column, String columnId) {
        List<GithubProjectCard> cards = column.getCards();
        if (cards != null) {
            cards.forEach(card -> {
                if (githubAggService.getCard(company, columnId, card.getId()).isEmpty()) {
                    DbGithubProjectCard dbCard = DbGithubProjectCard.fromProjectCard(card, columnId);
                    String cardId = githubAggService.insertCard(company, gitHubIntegrationId, dbCard);
                    Assert.assertNotNull(cardId);
                    Optional<DbGithubProjectCard> insertedCard = githubAggService.getCard(company, columnId, card.getId());
                    Assert.assertTrue(insertedCard.isPresent());
                    validateCards(dbCard, insertedCard.get());
                }
            });
        }
    }

    private void validateCards(DbGithubProjectCard dbCard, DbGithubProjectCard card) {
        Assert.assertEquals(dbCard.getCurrentColumnId(), card.getCurrentColumnId());
        Assert.assertEquals(dbCard.getCardId(), card.getCardId());
        Assert.assertEquals(dbCard.getArchived(), card.getArchived());
        Assert.assertEquals(dbCard.getCreator(), card.getCreator());
        Assert.assertEquals(dbCard.getContentUrl(), card.getContentUrl());
        Assert.assertEquals(dbCard.getCardCreatedAt(), card.getCardCreatedAt());
        Assert.assertEquals(dbCard.getCardUpdatedAt(), card.getCardUpdatedAt());
    }

}
