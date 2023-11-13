package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.github.DbGithubCardTransition;
import io.levelops.commons.databases.models.database.github.DbGithubProject;
import io.levelops.commons.databases.models.database.github.DbGithubProjectCard;
import io.levelops.commons.databases.models.database.github.DbGithubProjectCardWithIssue;
import io.levelops.commons.databases.models.database.github.DbGithubProjectColumn;
import io.levelops.commons.databases.models.database.organization.DBOrgContentSection;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.models.database.organization.OrgUnitCategory;
import io.levelops.commons.databases.models.database.organization.OrgUserId;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.models.database.scm.DbScmIssue;
import io.levelops.commons.databases.models.filters.GithubCardFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.dev_productivity.DevProductivityProfileDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitCategoryDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.databases.services.velocity.OrgProfileDatabaseService;
import io.levelops.commons.databases.services.velocity.VelocityConfigsDatabaseService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.integrations.github.models.GithubProject;
import io.levelops.integrations.github.models.GithubProjectCard;
import io.levelops.integrations.github.models.GithubProjectColumn;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.github.models.GithubWebhookEvent;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.database.github.DbGithubCardTransition.fromGithubCardTransitionCreateEvent;
import static io.levelops.commons.databases.models.database.github.DbGithubCardTransition.fromGithubCardTransitionDeleteEvent;
import static io.levelops.commons.databases.models.database.github.DbGithubCardTransition.fromGithubCardTransitionMovedEvent;
import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;

@Log4j2
public class GithubCardsOUTest {
    private static final String company = "test";
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static GithubAggService githubAggService;
    private static ScmAggService scmAggService;
    private static OrgVersionsDatabaseService versionsService;

    private static OrgUnitHelper unitsHelper;
    private static OrgUsersDatabaseService usersService;
    private static Date currentTime;
    private static UserIdentityService userIdentityService;
    private static OrgUnitsDatabaseService unitsService;
    private static TagItemDBService tagItemService;
    private static Integer ouRefId;
    private static DBOrgUnit unit1;
    private static DBOrgUnit unit2;
    private static OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService;
    private static OrgUnitCategory orgGroup1;
    private static String orgGroupId1;

    private static OrgProfileDatabaseService ouProfileDbService;
    private static VelocityConfigsDatabaseService velocityConfigDbService;
    private static DevProductivityProfileDatabaseService devProductivityProfileDbService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private static TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;

    @Before
    public void setup() throws Exception {
        if (dataSource != null)
            return;

        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";" +
                "CREATE SCHEMA test;").execute();
        dataSource.getConnection().prepareStatement(arrayUniq)
                .execute();
        IntegrationService integrationService = new IntegrationService(dataSource);
        userIdentityService = new UserIdentityService(dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        githubAggService = new GithubAggService(dataSource);
        unitsHelper = new OrgUnitHelper(unitsService, integrationService);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        githubAggService.ensureTableExistence(company);
        versionsService = new OrgVersionsDatabaseService(dataSource);
        versionsService.ensureTableExistence(company);

        usersService = new OrgUsersDatabaseService(dataSource, mapper, versionsService, userIdentityService);
        usersService.ensureTableExistence(company);
        new UserService(dataSource, mapper).ensureTableExistence(company);
        DashboardWidgetService dashboardWidgetService = new DashboardWidgetService(dataSource, mapper);
        dashboardWidgetService.ensureTableExistence(company);
        new ProductService(dataSource).ensureTableExistence(company);
        orgUnitCategoryDatabaseService = new OrgUnitCategoryDatabaseService(dataSource, unitsHelper, mapper);
        orgUnitCategoryDatabaseService.ensureTableExistence(company);
        unitsService = new OrgUnitsDatabaseService(dataSource, mapper, tagItemService, usersService, versionsService,
                dashboardWidgetService);
        unitsService.ensureTableExistence(company);

        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        ouProfileDbService = new OrgProfileDatabaseService(dataSource,mapper);
        ouProfileDbService.ensureTableExistence(company);
        velocityConfigDbService = new VelocityConfigsDatabaseService(dataSource,mapper,ouProfileDbService);
        velocityConfigDbService.ensureTableExistence(company);
        ticketCategorizationSchemeDatabaseService = new TicketCategorizationSchemeDatabaseService(dataSource, mapper);
        ticketCategorizationSchemeDatabaseService.ensureTableExistence(company);
        devProductivityProfileDbService = new DevProductivityProfileDatabaseService(dataSource,mapper);
        devProductivityProfileDbService.ensureTableExistence(company);

        unitsHelper = new OrgUnitHelper(unitsService, integrationService);
        var integration2 = Integration.builder()
                .id("1")
                .name("name")
                .application("github")
                .description("description")
                .authentication(Integration.Authentication.API_KEY)
                .metadata(Map.of())
                .satellite(false)
                .tags(List.of())
                .status("ok")
                .build();
        var integrationId2 = Integer.parseInt(integrationService.insert(company, integration2));


        List<DbRepository> repos = new ArrayList<>();

        String issuesInput = ResourceUtils.getResourceAsString("json/databases/github_issues_cards.json");
        PaginatedResponse<GithubRepository> issues = mapper.readValue(issuesInput,
                mapper.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        currentTime = new Date();
        issues.getResponse().getRecords().forEach(repo -> {
            repos.add(DbRepository.fromGithubRepository(repo, integration2.getId()));
            repo.getIssues()
                    .forEach(issue -> {
                        DbScmIssue tmp = DbScmIssue
                                .fromGithubIssue(issue, repo.getId(), integration2.getId());
                        if (scmAggService.getIssue(company, tmp.getIssueId(), tmp.getRepoId(),
                                tmp.getIntegrationId()).isEmpty())
                            scmAggService.insertIssue(company, tmp);
                    });
        });

        String input = ResourceUtils.getResourceAsString("json/databases/github_projects_2.json");
        PaginatedResponse<GithubProject> projects = mapper.readValue(input,
                mapper.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubProject.class));
        projects.getResponse().getRecords().forEach(project -> {
            if (githubAggService.getProject(company, project.getId(), integration2.getId()).isEmpty()) {
                DbGithubProject dbProject = DbGithubProject.fromProject(project, integration2.getId());
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
                                        githubAggService.insertCard(company, integration2.getId(), dbCard);
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });

        String inputEvent = ResourceUtils.getResourceAsString("json/databases/github_webhook_events_2.json");
        List<GithubWebhookEvent> events = mapper.readValue(inputEvent, mapper.getTypeFactory()
                .constructCollectionType(List.class, GithubWebhookEvent.class));
        events.forEach(event ->
        {
            switch (event.getAction()) {
                case "created":
                    DbGithubCardTransition create = fromGithubCardTransitionCreateEvent(integration2.getId(), event);
                    githubAggService.insertCardTransition(company, create);
                    break;
                case "moved":
                    DbGithubCardTransition to = fromGithubCardTransitionCreateEvent(integration2.getId(), event);
                    DbGithubCardTransition from = fromGithubCardTransitionMovedEvent(integration2.getId(), event);
                    githubAggService.insertCardTransition(company, to);
                    githubAggService.updateCardTransition(company, from);
                    break;
                case "deleted":
                    DbGithubCardTransition delete = fromGithubCardTransitionDeleteEvent(integration2.getId(), event);
                    githubAggService.updateCardTransition(company, delete);
                    break;
            }
        });
        var orgUser1 = DBOrgUser.builder()
                .email("email1")
                .fullName("fullName1")
                .customFields(Map.of("test_name", "test1"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("shri").username("cloudId").integrationType(integration2.getApplication())
                        .integrationId(Integer.parseInt(integration2.getId())).build()))
                .versions(Set.of(1))
                .build();
        var userId1 = usersService.upsert(company, orgUser1);

        var orgUser2 = DBOrgUser.builder()
                .email("email2")
                .fullName("fullName2")
                .customFields(Map.of("sample_name", "sample"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("modo").integrationId(Integer.parseInt(integration2.getId())).build()))
                .versions(Set.of(2, 3))
                .build();
        var userId2 = usersService.upsert(company, orgUser2);
        var orgUser3 = DBOrgUser.builder()
                .email("email3")
                .fullName("fullName3")
                .customFields(Map.of("test_name", "test1"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("bolon").username("cloudId").integrationType(integration2.getApplication())
                        .integrationId(Integer.parseInt(integration2.getId())).build()))
                .versions(Set.of(1))
                .build();
        var userId3 = usersService.upsert(company, orgUser3);
        var manager1 = OrgUserId.builder().id(userId1.getId()).refId(userId1.getRefId()).fullName(orgUser1.getFullName()).email(orgUser1.getEmail()).build();
        var manager2 = OrgUserId.builder().id(userId2.getId()).refId(userId2.getRefId()).fullName(orgUser2.getFullName()).email(orgUser2.getEmail()).build();
        var managers = Set.of(
                manager1,
                manager2
        );
        orgGroup1 = OrgUnitCategory.builder()
                .name("TEAM A")
                .description("Sample team")
                .isPredefined(true)
                .build();
        orgGroupId1 = orgUnitCategoryDatabaseService.insert(company, orgGroup1);
        unit1 = DBOrgUnit.builder()
                .name("unit4")
                .description("My unit4")
                .active(true)
                .versions(Set.of(1))
                .managers(managers)
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(integrationId2)
                        .integrationFilters(Map.of(
                        ))
                        .defaultSection(false)
                        .users(Set.of(1, 2, 3))
                        .build()))
                .refId(1)
                .build();
        var ids = unitsService.insertForId(company, unit1);
        unitsHelper.activateVersion(company,ids.getLeft());

        unit2 = DBOrgUnit.builder()
                .name("unit5")
                .description("My unit5")
                .active(true)
                .versions(Set.of(1))
                .managers(managers)
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(integrationId2)
                        .integrationFilters(Map.of(
                                "assignees", List.of("sample")
                        ))
                        .defaultSection(false)
                        .users(Set.of(1, 2, 3))
                        .build()))
                .refId(2)
                .build();
        ids = unitsService.insertForId(company, unit2);
        unitsHelper.activateVersion(company,ids.getLeft());

    }

    @Test
    public void testOuConfig() throws SQLException {
        Optional<DBOrgUnit> dbOrgUnit1 = unitsService.get(company, 1, true);
        DefaultListRequest defaultListRequest = DefaultListRequest.builder().filter(Map.of()).across("project").ouIds(Set.of(1)).build();
        OUConfiguration ouConfig = unitsHelper.getOuConfigurationFromDBOrgUnit(company, IntegrationType.getSCMIntegrationTypes(),
                defaultListRequest, dbOrgUnit1.orElseThrow(), false);
        defaultListRequest = ouConfig.getRequest();

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse = githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter.createGithubCardFilter(defaultListRequest).across(GithubCardFilter.DISTINCT.project).build(), ouConfig);
        Assertions.assertThat(dbAggregationResultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(dbAggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("Kanban Board 3");

        Optional<DBOrgUnit> dbOrgUnit2 = unitsService.get(company, 2, true);
        defaultListRequest = DefaultListRequest.builder().filter(Map.of()).across("project").ouIds(Set.of(2)).build();
        ouConfig = unitsHelper.getOuConfigurationFromDBOrgUnit(company, IntegrationType.getSCMIntegrationTypes(),
                defaultListRequest, dbOrgUnit2.orElseThrow(), false);
        defaultListRequest = ouConfig.getRequest();

        dbAggregationResultDbListResponse = githubAggService.groupByAndCalculateCards(company,
                GithubCardFilter.createGithubCardFilter(defaultListRequest).across(GithubCardFilter.DISTINCT.project).build(), ouConfig);
        Assertions.assertThat(dbAggregationResultDbListResponse.getTotalCount()).isEqualTo(1);

        dbOrgUnit2 = unitsService.get(company, 1, true);
        defaultListRequest = DefaultListRequest.builder().filter(Map.of()).across("project").ouIds(Set.of(1)).build();
        ouConfig = unitsHelper.getOuConfigurationFromDBOrgUnit(company, IntegrationType.getSCMIntegrationTypes(),
                defaultListRequest, dbOrgUnit2.orElseThrow(), false);
        defaultListRequest = ouConfig.getRequest();
        DbListResponse<DbGithubProjectCardWithIssue> list = githubAggService.list(company, GithubCardFilter.createGithubCardFilter(defaultListRequest).build(),
                ouConfig, Map.of(), 0, 1000);
        Assertions.assertThat(list.getTotalCount()).isEqualTo(3);

        dbOrgUnit2 = unitsService.get(company, 2, true);
        defaultListRequest = DefaultListRequest.builder().filter(Map.of()).across("project").
                ouIds(Set.of(2)).ouUserFilterDesignation(Map.of("github", Sets.newHashSet("user"))).
                ouExclusions(List.of("assignees")).build();
        ouConfig = unitsHelper.getOuConfigurationFromDBOrgUnit(company, IntegrationType.getSCMIntegrationTypes(),
                defaultListRequest, dbOrgUnit2.orElseThrow(), false);
        defaultListRequest = ouConfig.getRequest();
        list = githubAggService.list(company, GithubCardFilter.createGithubCardFilter(defaultListRequest).build(),
                ouConfig, Map.of(), 0, 1000);
        Assertions.assertThat(list.getTotalCount()).isEqualTo(3);

        dbOrgUnit2 = unitsService.get(company, 2, true);
        defaultListRequest = DefaultListRequest.builder().filter(Map.of()).across("project").
                ouIds(Set.of(2)).ouUserFilterDesignation(Map.of("github", Sets.newHashSet("ids", "user_ids"))).
                ouExclusions(List.of("ids")).build();
        Assertions.assertThat(defaultListRequest.getOuUserFilterDesignation().get("github").size()).isEqualTo(2);
        ouConfig = unitsHelper.getOuConfigurationFromDBOrgUnit(company, IntegrationType.getSCMIntegrationTypes(),
                defaultListRequest, dbOrgUnit2.orElseThrow(), false);
        defaultListRequest = ouConfig.getRequest();
        Assertions.assertThat(ouConfig.getGithubFields().size()).isEqualTo(1);
        list = githubAggService.list(company, GithubCardFilter.createGithubCardFilter(defaultListRequest).build(),
                ouConfig, Map.of(), 0, 1000);
        Assertions.assertThat(list.getTotalCount()).isEqualTo(0);
    }
}
