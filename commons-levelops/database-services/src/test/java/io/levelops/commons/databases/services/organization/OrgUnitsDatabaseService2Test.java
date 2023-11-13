package io.levelops.commons.databases.services.organization;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Product;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.models.database.dashboard.Dashboard;
import io.levelops.commons.databases.models.database.github.DbGithubCardTransition;
import io.levelops.commons.databases.models.database.github.DbGithubProject;
import io.levelops.commons.databases.models.database.github.DbGithubProjectCard;
import io.levelops.commons.databases.models.database.github.DbGithubProjectColumn;
import io.levelops.commons.databases.models.database.organization.*;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.models.database.scm.DbScmIssue;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.GithubAggService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ProductService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.TagItemDBService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.databases.services.dev_productivity.DevProductivityProfileDatabaseService;
import io.levelops.commons.databases.services.velocity.OrgProfileDatabaseService;
import io.levelops.commons.databases.services.velocity.VelocityConfigsDatabaseService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
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
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.commons.databases.models.database.github.DbGithubCardTransition.fromGithubCardTransitionCreateEvent;
import static io.levelops.commons.databases.models.database.github.DbGithubCardTransition.fromGithubCardTransitionDeleteEvent;
import static io.levelops.commons.databases.models.database.github.DbGithubCardTransition.fromGithubCardTransitionMovedEvent;
import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;

public class OrgUnitsDatabaseService2Test {

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
    private static DBOrgUnit unit2, unit3, unit4, unit5, unit6, unit7, unit8;
    private static OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService;
    private static OrgUnitCategory orgGroup1;
    private static String orgGroupId1;
    private static Pair<UUID, Integer> ids, ids2, ids3, ids4, ids5, ids6, ids7;
    private static String dashBoardId1, dashBoardId2, dashBoardId3;

    private static OrgProfileDatabaseService ouProfileDbService;
    private static VelocityConfigsDatabaseService velocityConfigDbService;
    private static DevProductivityProfileDatabaseService devProductivityProfileDbService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private static TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;
    private static UserService propeloUserService;

    @BeforeClass
    public static void setup() throws Exception {
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
        propeloUserService = new UserService(dataSource,mapper);
        propeloUserService.ensureTableExistence(company);
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
        dashBoardId1 = dashboardWidgetService.insert(company, Dashboard.builder()
                .name("Dashboard 1")
                .firstName("DASH")
                .lastName("BOARD")
                .isDefault(true)
                .type("TYPE 1")
                .email("example@levelops.io")
                .build());

        dashBoardId2 = dashboardWidgetService.insert(company, Dashboard.builder()
                .name("Dashboard 2")
                .firstName("DASH1")
                .lastName("BOARD2")
                .isDefault(false)
                .type("TYPE 2")
                .email("example@levelops.io")
                .build());

        dashBoardId3 = dashboardWidgetService.insert(company, Dashboard.builder()
                .name("Dashboard 3")
                .firstName("DASH1")
                .lastName("BOARD2")
                .isDefault(false)
                .type("TYPE 3")
                .email("example@levelops.io")
                .build());
        unit1 = DBOrgUnit.builder()
                .name("unit4")
                .description("My unit4")
                .active(true)
                .managers(managers)
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .defaultDashboardId(Integer.parseInt(dashBoardId1))
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(integrationId2)
                        .integrationFilters(Map.of(
                        ))
                        .defaultSection(false)
                        .users(Set.of(1, 2, 3))
                        .build()))
                .build();
        ids = unitsService.insertForId(company, unit1);
        unitsHelper.activateVersion(company, ids.getLeft());
        unit4 = DBOrgUnit.builder()
                .name("unit7")
                .description("My uni7")
                .active(true)
                .managers(managers)
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .defaultDashboardId(Integer.parseInt(dashBoardId2))
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(integrationId2)
                        .integrationFilters(Map.of(
                        ))
                        .defaultSection(false)
                        .users(Set.of(1, 2, 3))
                        .build()))
                .build();
        ids2 = unitsService.insertForId(company, unit4);
        unitsHelper.activateVersion(company, ids2.getLeft());
        unit2 = DBOrgUnit.builder()
                .name("unit5")
                .description("My unit5")
                .active(true)
                .parentRefId(ids.getRight())
                .managers(managers)
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .defaultDashboardId(Integer.parseInt(dashBoardId3))
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(integrationId2)
                        .integrationFilters(Map.of(
                                "assignees", List.of("sample")
                        ))
                        .defaultSection(false)
                        .users(Set.of(1, 2, 3))
                        .build()))
                .build();
        ids3 = unitsService.insertForId(company, unit2);
        unitsHelper.activateVersion(company, ids3.getLeft());
        unit3 = DBOrgUnit.builder()
                .name("unit6")
                .description("My unit6")
                .active(true)
                .parentRefId(ids2.getRight())
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
                .build();
        ids4 = unitsService.insertForId(company, unit3);
        unitsHelper.activateVersion(company, ids4.getLeft());
        unit6 = DBOrgUnit.builder()
                .name("unit11")
                .description("My unit11")
                .active(true)
                .parentRefId(ids2.getRight())
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
                .build();
        ids5 = unitsService.insertForId(company, unit6);
        unitsHelper.activateVersion(company, ids5.getLeft());
        unit7 = DBOrgUnit.builder()
                .name("unit12")
                .description("My unit12")
                .active(true)
                .parentRefId(ids5.getRight())
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
                .build();
        ids6 = unitsService.insertForId(company, unit7);
        unitsHelper.activateVersion(company, ids6.getLeft());
        unit8 = DBOrgUnit.builder()
                .name("unit13")
                .description("My unit13")
                .active(true)
                .parentRefId(ids6.getRight())
                .managers(managers)
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .defaultDashboardId(Integer.parseInt(dashBoardId2))
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(integrationId2)
                        .integrationFilters(Map.of(
                                "assignees", List.of("sample")
                        ))
                        .defaultSection(false)
                        .users(Set.of(1, 2, 3))
                        .build()))
                .build();
        ids7 = unitsService.insertForId(company, unit8);
        unitsHelper.activateVersion(company, ids7.getLeft());
        String ouAdmin1 = propeloUserService.insert(company, User.builder()
                .firstName("first")
                .lastName("last")
                .email("first.last@email.com")
                .userType(RoleType.ORG_ADMIN_USER)
                        .bcryptPassword("")
                        .managedOURefIds(List.of(ids.getRight(),ids2.getRight()))
                        .passwordAuthEnabled(true)
                        .mfaEnabled(false)
                        .samlAuthEnabled(false)
                .build());
        String ouAdmin2 = propeloUserService.insert(company, User.builder()
                .firstName("firstt")
                .lastName("lastt")
                .email("firstt.lastt@email.com")
                .userType(RoleType.ORG_ADMIN_USER)
                        .bcryptPassword("")
                .managedOURefIds(List.of(ids.getRight(),ids3.getRight()))
                .passwordAuthEnabled(true)
                .mfaEnabled(false)
                .samlAuthEnabled(false).build());
        String publicUser1 = propeloUserService.insert(company, User.builder()
                .firstName("public")
                .lastName("user")
                .email("public.user@email.com")
                .userType(RoleType.PUBLIC_DASHBOARD)
                .bcryptPassword("")
                .managedOURefIds(List.of(ids.getRight(),ids3.getRight()))
                .passwordAuthEnabled(true)
                .mfaEnabled(false)
                .samlAuthEnabled(false).build());
        dataSource.getConnection().prepareStatement(arrayUniq)
                .execute();
    }

    @Test
    public void testDefaultDashboardIds() throws SQLException {
        DbListResponse<DBOrgUnit> dbOrgUnitDbListResponse = unitsService.filter(company, QueryFilter.builder()
                .strictMatch("default_dashboard_id", 2)
                .build(), 0, 1000);
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords().stream().map(DBOrgUnit::getDefaultDashboardId)
                .collect(Collectors.toList())).containsExactlyInAnyOrder(Integer.parseInt(dashBoardId1));

        dbOrgUnitDbListResponse = unitsService.filter(company, QueryFilter.builder()
                .strictMatch("default_dashboard_id", 3)
                .build(), 0, 1000);
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords().stream().map(DBOrgUnit::getDefaultDashboardId)
                .collect(Collectors.toList())).containsExactlyInAnyOrder(3, 3);

        dbOrgUnitDbListResponse = unitsService.filter(company, QueryFilter.builder()
                .strictMatch("default_dashboard_id", 4)
                .build(), 0, 1000);
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords().stream().map(DBOrgUnit::getDefaultDashboardId)
                .collect(Collectors.toList())).containsExactlyInAnyOrder(Integer.parseInt(dashBoardId3));
        dbOrgUnitDbListResponse = unitsService.filter(company, QueryFilter.builder()
                .strictMatch("default_dashboard_id", 2)
                .strictMatch("name", "unit4")
                .build(), 0, 1000);
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords().stream().map(DBOrgUnit::getDefaultDashboardId)
                .collect(Collectors.toList())).containsExactlyInAnyOrder(Integer.parseInt(dashBoardId1));
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords().stream().map(DBOrgUnit::getName)
                .collect(Collectors.toList())).containsExactlyInAnyOrder("unit4");
    }

    @Test
    public void test() throws SQLException {
        Optional<DBOrgUnit> dbOrgUnit = unitsService.get(company, 4, true);
        Assertions.assertThat(dbOrgUnit.isPresent()).isTrue();
        Assertions.assertThat(dbOrgUnit.get().getPath()).isEqualTo("/unit7/unit6");

        dbOrgUnit = unitsService.get(company, 3, true);
        Assertions.assertThat(dbOrgUnit.isPresent()).isTrue();
        Assertions.assertThat(dbOrgUnit.get().getPath()).isEqualTo("/unit4/unit5");

        unit5 = DBOrgUnit.builder()
                .name("unit10")
                .description("My unit10")
                .active(true)
                .parentRefId(ids2.getRight())
                .versions(Set.of(1))
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(1)
                        .integrationFilters(Map.of(
                                "assignees", List.of("sample")
                        ))
                        .defaultSection(false)
                        .users(Set.of(1, 2, 3))
                        .build()))
                .build();
        Pair<UUID, Integer> uuidIntegerPair = unitsService.insertForId(company, unit5);
        dbOrgUnit = unitsService.get(company, 8, true);
        Assertions.assertThat(dbOrgUnit.isPresent()).isFalse();
        // Assertions.assertThat(dbOrgUnit.get().getPath()).isEqualTo("/unit7/unit10");

        unitsHelper.deleteUnits(company, Set.of(uuidIntegerPair.getRight()));

        dbOrgUnit = unitsService.get(company, 5, true);
        Assertions.assertThat(dbOrgUnit.isPresent()).isTrue();
        Assertions.assertThat(dbOrgUnit.get().getPath()).isEqualTo("/unit7/unit11");

        dbOrgUnit = unitsService.get(company, 6, true);
        Assertions.assertThat(dbOrgUnit.isPresent()).isTrue();
        Assertions.assertThat(dbOrgUnit.get().getPath()).isEqualTo("/unit7/unit11/unit12");

        dbOrgUnit = unitsService.get(company, 7, true);
        Assertions.assertThat(dbOrgUnit.isPresent()).isTrue();
        Assertions.assertThat(dbOrgUnit.get().getPath()).isEqualTo("/unit7/unit11/unit12/unit13");

        unitsHelper.deleteUnits(company, Set.of(ids6.getRight()));
        dbOrgUnit = unitsService.get(company, 6, false);
        Assertions.assertThat(dbOrgUnit.isPresent()).isTrue();
        Assertions.assertThat(dbOrgUnit.get().getPath()).isEqualTo("/unit7/unit11/unit12");

        dbOrgUnit = unitsService.get(company, 7, true);
        Assertions.assertThat(dbOrgUnit.isPresent()).isTrue();
        Assertions.assertThat(dbOrgUnit.get().getPath()).isEqualTo("/unit7/unit11/unit12/unit13");
        unitsHelper.updateUnits(company, Stream.of(dbOrgUnit.get().toBuilder()
                .name("NEW NAME")
                .versions(Set.of(2))
                .build()));
        dbOrgUnit = unitsService.get(company, 7, true);
        Assertions.assertThat(dbOrgUnit.isPresent()).isTrue();
        Assertions.assertThat(dbOrgUnit.get().getPath()).isEqualTo("/NEW NAME");
        unitsHelper.activateVersion(company, 7, 1);
        dbOrgUnit = unitsService.get(company, 7, true);
        Assertions.assertThat(dbOrgUnit.get().getPath()).isEqualTo("/unit7/unit11/unit12/unit13");

        unitsHelper.updateUnits(company, Stream.of(dbOrgUnit.get().toBuilder()
                .name("NEW NAME")
                .versions(Set.of(3))
                .parentRefId(1)
                .build()));
        dbOrgUnit = unitsService.get(company, 7, true);
        Assertions.assertThat(dbOrgUnit.get().getPath()).isEqualTo("/unit4/NEW NAME");

        unitsHelper.updateUnits(company, Stream.of(dbOrgUnit.get().toBuilder()
                .name("NEW NAME")
                .versions(Set.of(3))
                .parentRefId(1)
                .build()));
        dbOrgUnit = unitsService.get(company, 7, true);
        Assertions.assertThat(dbOrgUnit.get().getPath()).isEqualTo("/unit4/NEW NAME");

        unitsHelper.updateUnits(company, Stream.of(dbOrgUnit.get().toBuilder()
                .name("NEW NAME (Test]")
                .versions(Set.of(3))
                .parentRefId(1)
                .build()));
        dbOrgUnit = unitsService.get(company, 7, true);
        Assertions.assertThat(dbOrgUnit.get().getPath()).isEqualTo("/unit4/NEW NAME (Test]");

        unitsHelper.updateUnits(company, Stream.of(dbOrgUnit.get().toBuilder()
                .name("NEW NAME [Test]")
                .versions(Set.of(3))
                .parentRefId(1)
                .build()));
        dbOrgUnit = unitsService.get(company, 7, true);
        Assertions.assertThat(dbOrgUnit.get().getPath()).isEqualTo("/unit4/NEW NAME [Test]");

    }

    @Test
    public void testCategoryAndNameChange() throws SQLException {
        ProductService productService = new ProductService(dataSource);
        Optional<Integer> productId = productService.insertForId(company, Product.builder()
                .name("Product A")
                .key("QWERTY")
                .createdAt(Instant.now().getEpochSecond())
                .description("This is a product")
                .build());
        OrgUnitCategory orgUnitCategory = OrgUnitCategory.builder()
                .name("TEAM A")
                .description("Sample team")
                .workspaceId(productId.get())
                .isPredefined(true)
                .build();
        String orgGroupId1 = orgUnitCategoryDatabaseService.insert(company, orgUnitCategory);

        orgUnitCategory = OrgUnitCategory.builder()
                .name("TEAM B")
                .description("Sample team")
                .workspaceId(productId.get())
                .isPredefined(true)
                .build();
        String orgGroupId2 = orgUnitCategoryDatabaseService.insert(company, orgUnitCategory);
        DBOrgUnit orgUnit = DBOrgUnit.builder()
                .name("All Teams")
                .description("Description")
                .active(true)
                .versions(Set.of(1))
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(1)
                        .integrationFilters(Map.of(
                                "assignees", List.of("sample")
                        ))
                        .defaultSection(false)
                        .users(Set.of(1, 2, 3))
                        .build()))
                .build();
        Pair<UUID, Integer> allTeamsPair = unitsService.insertForId(company, orgUnit);
        Integer allTeamsId = allTeamsPair.getRight();
        unitsHelper.activateVersion(company, allTeamsPair.getLeft());
        orgUnit = DBOrgUnit.builder()
                .name("All Sprints")
                .description("Description")
                .active(true)
                .versions(Set.of(1))
                .ouCategoryId(UUID.fromString(orgGroupId2))
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(1)
                        .integrationFilters(Map.of(
                                "assignees", List.of("sample")
                        ))
                        .defaultSection(false)
                        .users(Set.of(1, 2, 3))
                        .build()))
                .build();
        Pair<UUID, Integer> allSprintsPair = unitsService.insertForId(company, orgUnit);
        Integer allSprintsId = allSprintsPair.getRight();
        unitsHelper.activateVersion(company, allSprintsPair.getLeft());

        orgUnit = DBOrgUnit.builder()
                .name("level1")
                .description("Description")
                .active(true)
                .versions(Set.of(1))
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .parentRefId(allTeamsId)
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(1)
                        .integrationFilters(Map.of(
                                "assignees", List.of("sample")
                        ))
                        .defaultSection(false)
                        .users(Set.of(1, 2, 3))
                        .build()))
                .build();
        Pair<UUID, Integer> level1Pair = unitsService.insertForId(company, orgUnit);
        Integer level1Id = level1Pair.getRight();
        unitsHelper.activateVersion(company, level1Pair.getLeft());
        orgUnit = DBOrgUnit.builder()
                .name("level2")
                .description("Description")
                .active(true)
                .versions(Set.of(1))
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .parentRefId(level1Id)
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(1)
                        .integrationFilters(Map.of(
                                "assignees", List.of("sample")
                        ))
                        .defaultSection(false)
                        .users(Set.of(1, 2, 3))
                        .build()))
                .build();

        Pair<UUID, Integer> level2Pair = unitsService.insertForId(company, orgUnit);
        Integer level2Id = level2Pair.getRight();
        unitsHelper.activateVersion(company, level2Pair.getLeft());

        Optional<DBOrgUnit> dbOrgUnit = unitsService.get(company, level1Id);
        Assertions.assertThat(dbOrgUnit.isPresent()).isTrue();
        Assertions.assertThat(dbOrgUnit.get().getPath()).isEqualTo("/All Teams/level1");

        dbOrgUnit = unitsService.get(company, level2Id);
        Assertions.assertThat(dbOrgUnit.isPresent()).isTrue();
        Assertions.assertThat(dbOrgUnit.get().getPath()).isEqualTo("/All Teams/level1/level2");


        unitsHelper.updateUnits(company, Stream.of(DBOrgUnit.builder()
                .id(level1Pair.getLeft())
                .refId(level1Pair.getRight())
                .name("level1-updated")
                .description("Description")
                .active(true)
                .versions(Set.of(1))
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .parentRefId(allTeamsId)
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(1)
                        .integrationFilters(Map.of(
                                "assignees", List.of("sample")
                        ))
                        .defaultSection(false)
                        .users(Set.of(1, 2, 3))
                        .build()))
                .build()));

        dbOrgUnit = unitsService.get(company, level1Id);
        Assertions.assertThat(dbOrgUnit.isPresent()).isTrue();
        Assertions.assertThat(dbOrgUnit.get().getName()).isEqualTo("level1-updated");

        dbOrgUnit = unitsService.get(company, level2Id);
        Assertions.assertThat(dbOrgUnit.isPresent()).isTrue();
        Assertions.assertThat(dbOrgUnit.get().getPath()).isEqualTo("/All Teams/level1-updated/level2");

        unitsHelper.updateUnits(company, Stream.of(DBOrgUnit.builder()
                .id(level1Pair.getLeft())
                .refId(level1Pair.getRight())
                .name("level1-updated")
                .description("Description")
                .active(true)
                .versions(Set.of(1))
                .ouCategoryId(UUID.fromString(orgGroupId2))
                .parentRefId(allSprintsId)
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(1)
                        .integrationFilters(Map.of(
                                "assignees", List.of("sample")
                        ))
                        .defaultSection(false)
                        .users(Set.of(1, 2, 3))
                        .build()))
                .build()));

        dbOrgUnit = unitsService.get(company, level1Id);
        Assertions.assertThat(dbOrgUnit.isPresent()).isTrue();
        Assertions.assertThat(dbOrgUnit.get().getOuCategoryId()).isEqualTo(UUID.fromString(orgGroupId2));
        Assertions.assertThat(dbOrgUnit.get().getPath()).isEqualTo("/All Sprints/level1-updated");

        dbOrgUnit = unitsService.get(company, level2Id);
        Assertions.assertThat(dbOrgUnit.isPresent()).isTrue();
        Assertions.assertThat(dbOrgUnit.get().getOuCategoryId()).isEqualTo(UUID.fromString(orgGroupId2));
        Assertions.assertThat(dbOrgUnit.get().getPath()).isEqualTo("/All Sprints/level1-updated/level2");

        unitsService.delete(company, allTeamsPair.getLeft());
        unitsService.delete(company, allSprintsPair.getLeft());
        dbOrgUnit = unitsService.get(company, level1Id);
        unitsService.delete(company, dbOrgUnit.get().getId());

        dbOrgUnit = unitsService.get(company, level2Id);
        unitsService.delete(company, dbOrgUnit.get().getId());

    }

    @Test
    public void testDrilldown() throws SQLException {
        DbListResponse<DBOrgUnit> dbOrgUnitDbListResponse = unitsService.filter(company, QueryFilter.builder()
                .strictMatch("parent_ref_id", 1)
                .build(), 0, 1000);
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords().stream().map(DBOrgUnit::getPath).collect(Collectors.toList())).containsExactlyInAnyOrder("/unit4/unit5");
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords().stream().map(DBOrgUnit::getRefId).collect(Collectors.toList())).containsExactlyInAnyOrder(3);

        dbOrgUnitDbListResponse = unitsService.filter(company, QueryFilter.builder()
                .strictMatch("parent_ref_id", 5)
                .build(), 0, 1000);
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords().stream().map(DBOrgUnit::getPath)
                .collect(Collectors.toList())).containsExactlyInAnyOrder("/unit7/unit11/unit12");

        dbOrgUnitDbListResponse = unitsService.filter(company, QueryFilter.builder()
                .partialMatch("name", "uni")
                .build(), 0, 1000);
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(dbOrgUnitDbListResponse.getTotalCount()).isEqualTo(7);

        dbOrgUnitDbListResponse = unitsService.filter(company, QueryFilter.builder()
                .partialMatch("name", "7")
                .build(), 0, 1000);
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(dbOrgUnitDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords().stream().map(DBOrgUnit::getPath)
                .collect(Collectors.toList())).containsExactlyInAnyOrder("/unit7");

        dbOrgUnitDbListResponse = unitsService.filter(company, QueryFilter.builder()
                .strictMatch("parent_ref_id", 77)
                .build(), 0, 1000);
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords()).isEmpty();
        Assertions.assertThat(dbOrgUnitDbListResponse.getTotalCount()).isEqualTo(0);
    }

    @Test
    public void testRecursiveChildren() throws SQLException {
        Set<Integer> children = unitsService.getAllChildrenRefIdsRecursive(company, List.of(ids.getRight()));
        Assertions.assertThat(children).isNotNull();
        Assertions.assertThat(children.size()).isEqualTo(2);
        children = unitsService.getAllChildrenRefIdsRecursive(company, List.of(ids2.getRight()));
        Assertions.assertThat(children).isNotNull();
        Assertions.assertThat(children.size()).isEqualTo(5);
        children = unitsService.getAllChildrenRefIdsRecursive(company, List.of(ids.getRight(), ids2.getRight()));
        Assertions.assertThat(children).isNotNull();
        Assertions.assertThat(children.size()).isEqualTo(7);
        children = unitsService.getAllChildrenRefIdsRecursive(company, List.of(ids7.getRight()));
        Assertions.assertThat(children).isNotNull();
        Assertions.assertThat(children.size()).isEqualTo(1);
    }

    @Test
    public void testOUAdmins() throws SQLException {
        Optional<DBOrgUnit> dbOrgUnit = unitsService.get(company, ids.getLeft());
        Assertions.assertThat(dbOrgUnit.isPresent()).isTrue();
        Assertions.assertThat(dbOrgUnit.get().getAdmins()).isNotEmpty();
        Assertions.assertThat(dbOrgUnit.get().getAdmins().size()).isEqualTo(2);
        Assertions.assertThat(dbOrgUnit.get().getAdmins().stream()
                .map(PropeloUserId::getEmail).collect(Collectors.toList()))
                .containsAll(List.of("first.last@email.com","firstt.lastt@email.com"));
        Assertions.assertThat(dbOrgUnit.get().getAdmins().stream()
                        .map(PropeloUserId::getEmail).collect(Collectors.toList()))
                .doesNotContain("public.user@email.com");
    }
}
