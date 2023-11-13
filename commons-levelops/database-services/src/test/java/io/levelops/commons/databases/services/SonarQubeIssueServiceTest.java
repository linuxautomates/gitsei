package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.organization.DBOrgContentSection;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.models.database.organization.OrgUnitCategory;
import io.levelops.commons.databases.models.database.organization.OrgUserId;
import io.levelops.commons.databases.models.database.sonarqube.DbSonarQubeIssue;
import io.levelops.commons.databases.models.database.sonarqube.DbSonarQubeProject;
import io.levelops.commons.databases.models.filters.SonarQubeIssueFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.dev_productivity.DevProductivityProfileDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitCategoryDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.databases.services.velocity.OrgProfileDatabaseService;
import io.levelops.commons.databases.services.velocity.VelocityConfigsDatabaseService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.integrations.sonarqube.models.Issue;
import io.levelops.integrations.sonarqube.models.Project;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOrDefault;
import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;
import static org.assertj.core.api.Assertions.assertThat;

public class SonarQubeIssueServiceTest {
    private final static String COMPANY = "test";
    private final static String INTEGRATION_ID = "1";
    private final static ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static SonarQubeIssueService issueService;
    private static final Date currentTime = new Date();
    private static Long currentTimeTrunc;
    private static DataSource dataSource;
    private static final SimpleDateFormat sdf;
    private static OrgUnitsDatabaseService unitsService;
    private static OrgUnitHelper unitsHelper;
    private static OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService;
    private static OrgUnitCategory orgGroup1;
    private static String orgGroupId1;

    private static OrgProfileDatabaseService ouProfileDbService;
    private static VelocityConfigsDatabaseService velocityConfigDbService;
    private static DevProductivityProfileDatabaseService devProductivityProfileDbService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private static TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;

    private static TimeZone defaultVal = null;

    static {
        sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @BeforeClass
    public static void setup() throws SQLException, IOException, ParseException {
        if (dataSource != null)
            return;
        defaultVal = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")));
        currentTimeTrunc = DateUtils.truncate(currentTime, Calendar.DATE);
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        dataSource.getConnection().prepareStatement(arrayUniq)
                .execute();
        issueService = new SonarQubeIssueService(dataSource);
        SonarQubeProjectService projectService = new SonarQubeProjectService(dataSource);
        IntegrationService integrationService = new IntegrationService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService.ensureTableExistence(COMPANY);
        Integration integration = Integration.builder().application("sonarqube").name("sq-integ")
                .status("enabled").id("1").build();
        String integString = integrationService.insert(COMPANY, integration);
        projectService.ensureTableExistence(COMPANY);
        issueService.ensureTableExistence(COMPANY);
        TagItemDBService tagItemService = new TagItemDBService(dataSource);
        OrgVersionsDatabaseService versionsService = new OrgVersionsDatabaseService(dataSource);
        versionsService.ensureTableExistence(COMPANY);
        UserIdentityService userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(COMPANY);
        OrgUsersDatabaseService usersService = new OrgUsersDatabaseService(dataSource, m, versionsService, userIdentityService);
        usersService.ensureTableExistence(COMPANY);
        UserService userService = new UserService(dataSource, m);
        userService.ensureTableExistence(COMPANY);
        DashboardWidgetService dashboardWidgetService = new DashboardWidgetService(dataSource, DefaultObjectMapper.get());
        dashboardWidgetService.ensureTableExistence(COMPANY);
        new ProductService(dataSource).ensureTableExistence(COMPANY);
        orgUnitCategoryDatabaseService = new OrgUnitCategoryDatabaseService(dataSource, unitsHelper, m);
        orgUnitCategoryDatabaseService.ensureTableExistence(COMPANY);
        unitsService = new OrgUnitsDatabaseService(dataSource, m, tagItemService, usersService, versionsService,
                dashboardWidgetService);
        unitsService.ensureTableExistence(COMPANY);

        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdInstancesDatabaseService.ensureTableExistence(COMPANY);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        ciCdJobsDatabaseService.ensureTableExistence(COMPANY);
        ouProfileDbService = new OrgProfileDatabaseService(dataSource,m);
        ouProfileDbService.ensureTableExistence(COMPANY);
        velocityConfigDbService = new VelocityConfigsDatabaseService(dataSource,m,ouProfileDbService);
        velocityConfigDbService.ensureTableExistence(COMPANY);
        ticketCategorizationSchemeDatabaseService = new TicketCategorizationSchemeDatabaseService(dataSource, m);
        ticketCategorizationSchemeDatabaseService.ensureTableExistence(COMPANY);
        devProductivityProfileDbService = new DevProductivityProfileDatabaseService(dataSource,m);
        devProductivityProfileDbService.ensureTableExistence(COMPANY);

        unitsHelper = new OrgUnitHelper(unitsService, integrationService);
        String resourcePath = "json/databases/sonarqube_issues.json";
        List<DbSonarQubeProject> projects = new ArrayList<>(List.of());
        projects.addAll(readProjects(resourcePath));
        for (DbSonarQubeProject project : projects) {
            final Optional<String> idOpt = projectService.insertAndReturnId(COMPANY, project);
            if (idOpt.isEmpty()) {
                throw new RuntimeException("The project must exist: " + project);
            }
        }
        resourcePath = "json/databases/sonarqube_issues.json";
        List<DbSonarQubeIssue> issues = new ArrayList<>(List.of());
        issues.addAll(readIssues(resourcePath));
        for (DbSonarQubeIssue issue : issues) {
            final Optional<String> idOpt = issueService.insertAndReturnId(COMPANY, issue);
            if (idOpt.isEmpty()) {
                throw new RuntimeException("The issue must exist: " + issue);
            }
        }
        var orgUser1 = DBOrgUser.builder()
                .email("email1")
                .fullName("fullName1")
                .customFields(Map.of("test_name", "test1"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("srinath.chandrashekhar@levelops.io").username("cloudId").integrationType(integration.getApplication())
                        .integrationId(Integer.parseInt(integration.getId())).build()))
                .versions(Set.of(1))
                .build();
        var userId1 = usersService.upsert(COMPANY, orgUser1);

        var orgUser2 = DBOrgUser.builder()
                .email("email2")
                .fullName("fullName2")
                .customFields(Map.of("sample_name", "sample"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("harsh@levelops.io").integrationId(Integer.parseInt(integration.getId())).build()))
                .versions(Set.of(2, 3))
                .build();
        var userId2 = usersService.upsert(COMPANY, orgUser2);
        var orgUser3 = DBOrgUser.builder()
                .email("email3")
                .fullName("fullName3")
                .customFields(Map.of("test_name", "test1"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("harshjariwala@harshswkbookpro.lan").username("cloudId").integrationType(integration.getApplication())
                        .integrationId(Integer.parseInt(integration.getId())).build()))
                .versions(Set.of(1))
                .build();
        var userId3 = usersService.upsert(COMPANY, orgUser3);
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
        orgGroupId1 = orgUnitCategoryDatabaseService.insert(COMPANY, orgGroup1);
        DBOrgUnit unit1 = DBOrgUnit.builder()
                .name("unit4")
                .description("My unit4")
                .active(true)
                .versions(Set.of(1))
                .managers(managers)
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(Integer.parseInt(integString))
                        .integrationFilters(Map.of(
                                "projects", List.of("testing-01")
                        ))
                        .defaultSection(false)
                        .users(Set.of(1, 2))
                        .build()))
                .refId(1)
                .build();
        var ids = unitsService.insertForId(COMPANY, unit1);
        unitsHelper.activateVersion(COMPANY,ids.getLeft());

        DBOrgUnit unit2 = DBOrgUnit.builder()
                .name("unit5")
                .description("My unit5")
                .active(true)
                .versions(Set.of(1))
                .managers(managers)
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(Integer.parseInt(integString))
                        .integrationFilters(Map.of(
                        ))
                        .defaultSection(false)
                        .users(Set.of(1, 2, 3))
                        .build()))
                .refId(2)
                .build();
        ids = unitsService.insertForId(COMPANY, unit2);
        unitsHelper.activateVersion(COMPANY,ids.getLeft());
        DBOrgUnit unit3 = DBOrgUnit.builder()
                .name("unit6")
                .description("My unit6")
                .active(true)
                .versions(Set.of(5))
                .managers(managers)
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(Integer.parseInt(integString))
                        .integrationFilters(Map.of(
                        ))
                        .defaultSection(false)
                        .users(Set.of(1, 2))
                        .build()))
                .refId(3)
                .build();
        ids = unitsService.insertForId(COMPANY, unit3);
        unitsHelper.activateVersion(COMPANY,ids.getLeft());

        DBOrgUnit unit4 = DBOrgUnit.builder()
                .name("unit7")
                .description("My unit7")
                .active(true)
                .versions(Set.of(5))
                .managers(managers)
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(Integer.parseInt(integString))
                        .integrationFilters(Map.of(
                                "severities", List.of("MINOR"),
                                "types", List.of("CODE_SMELL"),
                                "organizations", List.of("default-organization")
                        ))
                        .defaultSection(false)
                        .users(Set.of(1, 2, 3))
                        .build()))
                .refId(4)
                .build();
        ids = unitsService.insertForId(COMPANY, unit4);
        unitsHelper.activateVersion(COMPANY,ids.getLeft());

        DBOrgUnit unit5 = DBOrgUnit.builder()
                .name("unit8")
                .description("My unit8")
                .active(true)
                .versions(Set.of(5))
                .managers(managers)
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(Integer.parseInt(integString))
                        .integrationFilters(Map.of(
                                "statuses", List.of("OPEN"),
                                "severities", List.of("CRITICAL")
                        ))
                        .defaultSection(false)
                        .users(Set.of(1, 2, 3))
                        .build()))
                .refId(5)
                .build();
        ids = unitsService.insertForId(COMPANY, unit5);
        unitsHelper.activateVersion(COMPANY,ids.getLeft());

        DBOrgUnit unit6 = DBOrgUnit.builder()
                .name("unit8")
                .description("My unit8")
                .active(true)
                .versions(Set.of(5))
                .managers(managers)
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(Integer.parseInt(integString))
                        .integrationFilters(Map.of(
                                "statuses", List.of("OPEN"),
                                "severities", List.of("CRITICAL")
                        ))
                        .defaultSection(false)
                        .users(Set.of(1))
                        .build()))
                .refId(6)
                .build();
        ids = unitsService.insertForId(COMPANY, unit6);
        unitsHelper.activateVersion(COMPANY,ids.getLeft());
    }

    @Before
    public void setupTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")));
    }

    @After
    public void clean() {
        TimeZone.setDefault(defaultVal);
    }

    @Test
    public void testSingleRecord() {
        Optional<DbSonarQubeIssue> issueOpt = issueService.get(COMPANY, "AXS0RZAGsszTasiJGEtm",
                "commons", currentTime, INTEGRATION_ID);
        if (issueOpt.isEmpty()) {
            throw new RuntimeException("Error. There should be atleast one issue.");
        }
        DbSonarQubeIssue issue = issueOpt.get();
        assertThat(issue.getComponent().
                equals("commons:database-services/src/main/java/io/levelops/commons/databases/services/JiraIssueService.java"))
                .isTrue();
        assertThat(issue.getAuthor()).isEqualTo("srinath.chandrashekhar@levelops.io");
        assertThat(issue.getEffort().equals("31")).isTrue();
        assertThat(issue.getDebt().equals("31")).isTrue();
        assertThat(issue.getTags()).isEmpty();
        assertThat(issue.getComments()).isNull();
        assertThat(sdf.format(issue.getCreationDate()).equals("2020-09-11T11:58:36+0000")).isTrue();
        assertThat(sdf.format(issue.getUpdationDate()).equals("2020-09-22T05:22:06+0000")).isTrue();
    }

    @Test
    public void testRecordLists() {
        List<DbSonarQubeIssue> issues = issueService.list(COMPANY, SonarQubeIssueFilter.builder()
                .ingestedAt(currentTimeTrunc).build(), Map.of(), 0, 1000, null).getRecords();
        assertThat(issues.size()).isEqualTo(18);
        issues = issueService.list(COMPANY, SonarQubeIssueFilter.builder().ingestedAt(currentTimeTrunc)
                        .projects(List.of("commons")).build()
                , Map.of(), 0, 1000, null).getRecords();
        assertThat(issues.size()).isEqualTo(7);
        issues = issueService.list(COMPANY, SonarQubeIssueFilter.builder().ingestedAt(currentTimeTrunc)
                        .projects(List.of("integrations"))
                        .severities(List.of("MINOR"))
                        .build()
                , Map.of(), 0, 1000, null).getRecords();
        assertThat(issues.size()).isEqualTo(0);
        issues = issueService.list(COMPANY, SonarQubeIssueFilter.builder().ingestedAt(currentTimeTrunc)
                .types(List.of("CODE_SMELL"))
                .severities(List.of("MINOR"))
                .build(), Map.of(), 0, 1000, null).getRecords();
        assertThat(issues.size()).isEqualTo(8);
        issues = issueService.list(COMPANY, SonarQubeIssueFilter.builder().ingestedAt(currentTimeTrunc)
                        .components(List.of("io", "java")).build(),
                Map.of(), 0, 1000, null).getRecords();
        assert (issues.stream().filter(x -> x.getComponent().contains("io")).count() == issues.size());
        issues = issueService.list(COMPANY, SonarQubeIssueFilter.builder().ingestedAt(currentTimeTrunc)
                .components(List.of("io", "java"))
                .severities(List.of("BLOCKER", "CRITICAL", "MAJOR"))
                .types(List.of("BUG", "VULNERABILITY", "CODE_SMELL"))
                .statuses(List.of("OPEN", "CONFIRMED", "REOPENED", "RESOLVED", "CLOSED"))
                .build(), Map.of(), 0, 1000, null).getRecords();
        assertThat(issues.size()).isEqualTo(10);
        issues = issueService.list(COMPANY, SonarQubeIssueFilter.builder().ingestedAt(currentTimeTrunc)
                .components(List.of("io", "java"))
                .statuses(List.of("';--"))
                .build(), Map.of(), 0, 1000, null).getRecords();
        assertThat(issues.size()).isEqualTo(0);

    }

    @Test
    public void testAggregations() throws SQLException {
        List<DbAggregationResult> aggs = issueService.groupByAndCalculate(COMPANY,
                SonarQubeIssueFilter.builder()
                        .distinct(SonarQubeIssueFilter.DISTINCT.project)
                        .calculation(SonarQubeIssueFilter.CALCULATION.issue_count)
                        .ingestedAt(currentTimeTrunc)
                        .build(), null, null).getRecords();
        assertThat(aggs.size()).isEqualTo(4);
        aggs = issueService.groupByAndCalculateForValues(COMPANY,
                SonarQubeIssueFilter.builder()
                        .distinct(SonarQubeIssueFilter.DISTINCT.project)
                        .calculation(SonarQubeIssueFilter.CALCULATION.issue_count)
                        .ingestedAt(currentTimeTrunc)
                        .build(), null, null).getRecords();
        assertThat(aggs.size()).isEqualTo(18);
        aggs = issueService.groupByAndCalculateForValues(COMPANY,
                SonarQubeIssueFilter.builder()
                        .distinct(SonarQubeIssueFilter.DISTINCT.severity)
                        .calculation(SonarQubeIssueFilter.CALCULATION.issue_count)
                        .ingestedAt(currentTimeTrunc)
                        .build(), null, null).getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = issueService.groupByAndCalculateForValues(COMPANY,
                SonarQubeIssueFilter
                        .builder().distinct(SonarQubeIssueFilter.DISTINCT.organization)
                        .ingestedAt(currentTimeTrunc)
                        .components(List.of("io", "java"))
                        .severities(List.of("BLOCKER", "CRITICAL", "MAJOR"))
                        .types(List.of("BUG", "VULNERABILITY", "CODE_SMELL"))
                        .calculation(SonarQubeIssueFilter.CALCULATION.issue_count)
                        .statuses(List.of("OPEN", "CONFIRMED", "REOPENED", "RESOLVED", "CLOSED"))
                        .build(), null, null).getRecords();
        assertThat(aggs.size() == 1).isTrue();
        assertThat(aggs.get(0).getTotalIssues()).isEqualTo(10L);
        aggs = issueService.groupByAndCalculateForValues(COMPANY,
                        SonarQubeIssueFilter.builder()
                                .distinct(SonarQubeIssueFilter.DISTINCT.tag)
                                .ingestedAt(currentTimeTrunc)
                                .calculation(SonarQubeIssueFilter.CALCULATION.issue_count)
                                .build(), null, null)
                .getRecords();
        assertThat(aggs.size()).isEqualTo(1);
        aggs = issueService.groupByAndCalculateForValues(COMPANY,
                SonarQubeIssueFilter
                        .builder().distinct(SonarQubeIssueFilter.DISTINCT.severity).ingestedAt(currentTimeTrunc)
                        .components(List.of("io"))
                        .severities(List.of("BLOCKER", "CRITICAL", "MAJOR"))
                        .types(List.of("BUG", "VULNERABILITY", "CODE_SMELL"))
                        .calculation(SonarQubeIssueFilter.CALCULATION.issue_count)
                        .statuses(List.of("OPEN", "CONFIRMED", "REOPENED", "RESOLVED", "CLOSED"))
                        .build(), null, null).getRecords();
        assertThat(aggs.size()).isEqualTo(2);
        aggs = issueService.groupByAndCalculate(COMPANY,
                SonarQubeIssueFilter.builder()
                        .distinct(SonarQubeIssueFilter.DISTINCT.project)
                        .calculation(SonarQubeIssueFilter.CALCULATION.issue_count)
                        .ingestedAt(currentTimeTrunc)
                        .partialMatch(Map.of("project", Map.of("$contains", "abc")))
                        .build(), null, null).getRecords();
        assertThat(aggs.size()).isEqualTo(0);
        aggs = issueService.groupByAndCalculate(COMPANY,
                SonarQubeIssueFilter.builder()
                        .distinct(SonarQubeIssueFilter.DISTINCT.project)
                        .calculation(SonarQubeIssueFilter.CALCULATION.issue_count)
                        .ingestedAt(currentTimeTrunc)
                        .partialMatch(Map.of("project", Map.of("$contains", "api")))
                        .build(), null, null).getRecords();
        assertThat(aggs.size()).isEqualTo(1);
        aggs = issueService.groupByAndCalculate(COMPANY,
                SonarQubeIssueFilter.builder()
                        .distinct(SonarQubeIssueFilter.DISTINCT.project)
                        .calculation(SonarQubeIssueFilter.CALCULATION.issue_count)
                        .ingestedAt(currentTimeTrunc)
                        .partialMatch(Map.of("project", Map.of("$begins", "comm")))
                        .build(), null, null).getRecords();
        assertThat(aggs.size()).isEqualTo(1);
        aggs = issueService.groupByAndCalculate(COMPANY,
                SonarQubeIssueFilter.builder()
                        .distinct(SonarQubeIssueFilter.DISTINCT.project)
                        .calculation(SonarQubeIssueFilter.CALCULATION.issue_count)
                        .ingestedAt(currentTimeTrunc)
                        .partialMatch(Map.of("project", Map.of("$ends", "api")))
                        .build(), null, null).getRecords();
        assertThat(aggs.size()).isEqualTo(1);
        aggs = issueService.groupByAndCalculate(COMPANY,
                SonarQubeIssueFilter.builder()
                        .distinct(SonarQubeIssueFilter.DISTINCT.project)
                        .calculation(SonarQubeIssueFilter.CALCULATION.issue_count)
                        .ingestedAt(currentTimeTrunc)
                        .partialMatch(Map.of("project", Map.of("$begins", "io.levelops")))
                        .build(), null, null).getRecords();
        assertThat(aggs.size()).isEqualTo(2);
        aggs = issueService.groupByAndCalculateForValues(COMPANY,
                SonarQubeIssueFilter.builder()
                        .distinct(SonarQubeIssueFilter.DISTINCT.project)
                        .calculation(SonarQubeIssueFilter.CALCULATION.issue_count)
                        .ingestedAt(currentTimeTrunc)
                        .build(), null, null).getRecords();
        assertThat(aggs.size()).isEqualTo(18);
        aggs = issueService.groupByAndCalculateForValues(COMPANY,
                SonarQubeIssueFilter.builder()
                        .distinct(SonarQubeIssueFilter.DISTINCT.severity)
                        .calculation(SonarQubeIssueFilter.CALCULATION.issue_count)
                        .ingestedAt(currentTimeTrunc)
                        .build(), null, null).getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = issueService.groupByAndCalculateForValues(COMPANY,
                SonarQubeIssueFilter
                        .builder().distinct(SonarQubeIssueFilter.DISTINCT.organization)
                        .ingestedAt(currentTimeTrunc)
                        .components(List.of("io", "java"))
                        .severities(List.of("BLOCKER", "CRITICAL", "MAJOR"))
                        .types(List.of("BUG", "VULNERABILITY", "CODE_SMELL"))
                        .calculation(SonarQubeIssueFilter.CALCULATION.issue_count)
                        .statuses(List.of("OPEN", "CONFIRMED", "REOPENED", "RESOLVED", "CLOSED"))
                        .build(), null, null).getRecords();
        assertThat(aggs.size() == 1).isTrue();
        assertThat(aggs.get(0).getTotalIssues()).isEqualTo(10L);
        aggs = issueService.groupByAndCalculateForValues(COMPANY,
                        SonarQubeIssueFilter.builder()
                                .distinct(SonarQubeIssueFilter.DISTINCT.tag)
                                .ingestedAt(currentTimeTrunc)
                                .calculation(SonarQubeIssueFilter.CALCULATION.issue_count)
                                .build(), null, null)
                .getRecords();
        assertThat(aggs.size()).isEqualTo(1);
        aggs = issueService.groupByAndCalculateForValues(COMPANY,
                SonarQubeIssueFilter
                        .builder().distinct(SonarQubeIssueFilter.DISTINCT.severity).ingestedAt(currentTimeTrunc)
                        .components(List.of("io"))
                        .severities(List.of("BLOCKER", "CRITICAL", "MAJOR"))
                        .types(List.of("BUG", "VULNERABILITY", "CODE_SMELL"))
                        .calculation(SonarQubeIssueFilter.CALCULATION.issue_count)
                        .statuses(List.of("OPEN", "CONFIRMED", "REOPENED", "RESOLVED", "CLOSED"))
                        .build(), null, null).getRecords();
        assertThat(aggs.size()).isEqualTo(2);
        aggs = issueService.groupByAndCalculate(COMPANY,
                SonarQubeIssueFilter.builder()
                        .distinct(SonarQubeIssueFilter.DISTINCT.severity)
                        .calculation(SonarQubeIssueFilter.CALCULATION.issue_count)
                        .ingestedAt(currentTimeTrunc)
                        .build(), null, null).getRecords();
        assertThat(aggs.size()).isEqualTo(3);
        aggs = issueService.groupByAndCalculate(COMPANY,
                SonarQubeIssueFilter.builder()
                        .distinct(SonarQubeIssueFilter.DISTINCT.trend)
                        .calculation(SonarQubeIssueFilter.CALCULATION.issue_count)
                        .ingestedAt(currentTimeTrunc)
                        .build(), null, null).getRecords();
        assertThat(aggs.size()).isEqualTo(1);
        assertThat(aggs.get(0).getKey()).isEqualTo(currentTimeTrunc.toString());
        assertThat(issueService.list(COMPANY,
                SonarQubeIssueFilter.builder()
                        .ingestedAt(currentTimeTrunc)
                        .tags(List.of("hello"))
                        .build(),
                Map.of(), 0, 1000, null).getRecords().size())
                .isGreaterThan(0);
        aggs = issueService.groupByAndCalculate(COMPANY,
                SonarQubeIssueFilter
                        .builder().distinct(SonarQubeIssueFilter.DISTINCT.organization)
                        .ingestedAt(currentTimeTrunc)
                        .components(List.of("io", "java"))
                        .severities(List.of("BLOCKER", "CRITICAL", "MAJOR"))
                        .types(List.of("BUG", "VULNERABILITY", "CODE_SMELL"))
                        .calculation(SonarQubeIssueFilter.CALCULATION.issue_count)
                        .statuses(List.of("OPEN", "CONFIRMED", "REOPENED", "RESOLVED", "CLOSED"))
                        .build(), null, null).getRecords();
        assertThat(aggs.size() == 1).isTrue();
        assertThat(aggs.get(0).getTotalIssues()).isEqualTo(10L);
        aggs = issueService.groupByAndCalculate(COMPANY,
                        SonarQubeIssueFilter.builder()
                                .distinct(SonarQubeIssueFilter.DISTINCT.tag)
                                .ingestedAt(currentTimeTrunc)
                                .calculation(SonarQubeIssueFilter.CALCULATION.issue_count)
                                .build(), null, null)
                .getRecords();
        assertThat(aggs.size()).isEqualTo(1);
        aggs = issueService.groupByAndCalculate(COMPANY,
                SonarQubeIssueFilter
                        .builder().distinct(SonarQubeIssueFilter.DISTINCT.severity).ingestedAt(currentTimeTrunc)
                        .components(List.of("io"))
                        .severities(List.of("BLOCKER", "CRITICAL", "MAJOR"))
                        .types(List.of("BUG", "VULNERABILITY", "CODE_SMELL"))
                        .calculation(SonarQubeIssueFilter.CALCULATION.issue_count)
                        .statuses(List.of("OPEN", "CONFIRMED", "REOPENED", "RESOLVED", "CLOSED"))
                        .build(), null, null).getRecords();
        assertThat(aggs.size()).isEqualTo(2);
        aggs = issueService.groupByAndCalculate(COMPANY,
                SonarQubeIssueFilter
                        .builder().distinct(SonarQubeIssueFilter.DISTINCT.project).ingestedAt(currentTimeTrunc)
                        .components(List.of("io"))
                        .severities(List.of("BLOCKER", "CRITICAL", "MAJOR"))
                        .types(List.of("BUG", "VULNERABILITY", "CODE_SMELL"))
                        .calculation(SonarQubeIssueFilter.CALCULATION.issue_count)
                        .projects(List.of("io.levelops.server_api:server-api", "testing-01"))
                        .statuses(List.of("OPEN", "CONFIRMED", "REOPENED", "RESOLVED", "CLOSED"))
                        .build(), null, null).getRecords();
        assertThat(aggs.size()).isEqualTo(1);

        aggs = issueService.groupByAndCalculate(COMPANY,
                SonarQubeIssueFilter
                        .builder().distinct(SonarQubeIssueFilter.DISTINCT.project).ingestedAt(currentTimeTrunc)
                        .components(List.of("io"))
                        .severities(List.of("BLOCKER", "CRITICAL", "MAJOR"))
                        .types(List.of("BUG", "VULNERABILITY", "CODE_SMELL"))
                        .calculation(SonarQubeIssueFilter.CALCULATION.effort)
                        .projects(List.of("io.levelops.server_api:server-api", "testing-01"))
                        .statuses(List.of("OPEN", "CONFIRMED", "REOPENED", "RESOLVED", "CLOSED"))
                        .build(), null, null).getRecords();

        long mini = aggs.stream().map(DbAggregationResult::getMin).reduce(Math::min).orElse(0L);
        long maxi = aggs.stream().map(DbAggregationResult::getMin).reduce(Math::max).orElse(0L);
        assertThat(mini).isEqualTo(5);
        assertThat(maxi).isEqualTo(5);

        mini = aggs.stream().map(DbAggregationResult::getMax).reduce(Math::min).orElse(0L);
        assertThat(mini).isEqualTo(5);
        maxi = aggs.stream().map(DbAggregationResult::getMax).reduce(Math::max).orElse(0L);
        assertThat(maxi).isEqualTo(5);

        aggs = issueService.groupByAndCalculate(COMPANY,
                SonarQubeIssueFilter
                        .builder().distinct(SonarQubeIssueFilter.DISTINCT.project).ingestedAt(currentTimeTrunc)
                        .components(List.of("io"))
                        .severities(List.of("BLOCKER", "CRITICAL", "MAJOR"))
                        .types(List.of("BUG", "VULNERABILITY", "CODE_SMELL"))
                        .calculation(SonarQubeIssueFilter.CALCULATION.effort)
                        .projects(List.of("io.levelops.server_api:server-api", "testing-01"))
                        .statuses(List.of("OPEN", "CONFIRMED", "REOPENED", "RESOLVED", "CLOSED"))
                        .build(), null, null).getRecords();

        mini = aggs.stream().map(DbAggregationResult::getMax).reduce(Math::min).orElse(0L);
        assertThat(mini).isEqualTo(5);
        maxi = aggs.stream().map(DbAggregationResult::getMax).reduce(Math::max).orElse(0L);
        assertThat(maxi).isEqualTo(5);

        mini = aggs.stream().map(DbAggregationResult::getMin).reduce(Math::min).orElse(0L);
        assertThat(mini).isEqualTo(5);
        maxi = aggs.stream().map(DbAggregationResult::getMin).reduce(Math::max).orElse(0L);
        assertThat(maxi).isEqualTo(5);
    }

    @Test
    public void testOuConfig() throws SQLException {

        Optional<DBOrgUnit> dbOrgUnit1 = unitsService.get(COMPANY, 1, true);
        DefaultListRequest defaultListRequest = DefaultListRequest.builder().filter(Map.of()).across("project").ouIds(Set.of(1)).build();
        OUConfiguration ouConfig = unitsHelper.getOuConfigurationFromDBOrgUnit(COMPANY, Set.of(IntegrationType.SONARQUBE),
                defaultListRequest, dbOrgUnit1.orElseThrow(), false);
        defaultListRequest = ouConfig.getRequest();
        List<DbSonarQubeIssue> issues = issueService.list(COMPANY, getSQIssueFilterBuilder(COMPANY, defaultListRequest, currentTimeTrunc)
                        .ingestedAt(currentTimeTrunc).build(),
                Map.of(), 0, 1000, null).getRecords();
        Assertions.assertThat(issues).isNotEmpty();
        Assertions.assertThat(issues.size()).isEqualTo(5);
        Assertions.assertThat(issues.stream().map(DbSonarQubeIssue::getProject).collect(Collectors.toList()))
                .containsAnyOf("testing-01");

        issues = issueService.list(COMPANY, SonarQubeIssueFilter.builder()
                        .ingestedAt(currentTimeTrunc).projects(List.of("testing-01")).build(), Map.of()
                , 0, 1000, null).getRecords();
        Assertions.assertThat(issues).isNotEmpty();
        Assertions.assertThat(issues.size()).isEqualTo(5);
        Assertions.assertThat(issues.stream().map(DbSonarQubeIssue::getProject).collect(Collectors.toList()))
                .containsAnyOf("testing-01");

        List<DbAggregationResult> aggs = issueService.groupByAndCalculate(COMPANY,
                SonarQubeIssueFilter.builder()
                        .distinct(SonarQubeIssueFilter.DISTINCT.project)
                        .calculation(SonarQubeIssueFilter.CALCULATION.issue_count)
                        .ingestedAt(currentTimeTrunc)
                        .build(), null, null).getRecords();
        assertThat(aggs.size()).isEqualTo(4);

        aggs = issueService.groupByAndCalculate(COMPANY,
                getSQIssueFilterBuilder(COMPANY, defaultListRequest, currentTimeTrunc)
                        .distinct(SonarQubeIssueFilter.DISTINCT.project)
                        .ingestedAt(currentTimeTrunc)
                        .build()
                , null, null).getRecords();
        assertThat(aggs.size()).isEqualTo(1);

        Optional<DBOrgUnit> dbOrgUnit2 = unitsService.get(COMPANY, 2, true);
        defaultListRequest = DefaultListRequest.builder().filter(Map.of()).across("project").ouIds(Set.of(2)).build();
        ouConfig = unitsHelper.getOuConfigurationFromDBOrgUnit(COMPANY, Set.of(IntegrationType.SONARQUBE),
                defaultListRequest, dbOrgUnit2.orElseThrow(), false);
        defaultListRequest = ouConfig.getRequest();
        issues = issueService.list(COMPANY, getSQIssueFilterBuilder(COMPANY, defaultListRequest, currentTimeTrunc)
                        .ingestedAt(currentTimeTrunc).build(),
                Map.of(), 0, 1000, ouConfig).getRecords();
        Assertions.assertThat(issues).isNotEmpty();
        Assertions.assertThat(issues.stream().map(DbSonarQubeIssue::getAuthor).collect(Collectors.toList()))
                .containsOnly("harshjariwala@harshswkbookpro.lan", "srinath.chandrashekhar@levelops.io", "harsh@levelops.io");

        defaultListRequest = DefaultListRequest.builder().filter(Map.of()).across("project").ouIds(Set.of(2)).build();
        ouConfig = unitsHelper.getOuConfigurationFromDBOrgUnit(COMPANY, Set.of(IntegrationType.SONARQUBE),
                defaultListRequest, dbOrgUnit2.orElseThrow(), false);
        defaultListRequest = ouConfig.getRequest();
        aggs = issueService.groupByAndCalculate(COMPANY,
                getSQIssueFilterBuilder(COMPANY, defaultListRequest, currentTimeTrunc)
                        .distinct(SonarQubeIssueFilter.DISTINCT.author)
                        .ingestedAt(currentTimeTrunc)
                        .build()
                , null, ouConfig).getRecords();
        Assertions.assertThat(aggs).isNotEmpty();
        Assertions.assertThat(aggs.stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsOnly("harshjariwala@harshswkbookpro.lan",
                        "srinath.chandrashekhar@levelops.io",
                        "harsh@levelops.io");
    }

    @Test
    public void testOuConfigTwo() throws SQLException {
        Optional<DBOrgUnit> dbOrgUnit2 = unitsService.get(COMPANY, 3, true);
        DefaultListRequest defaultListRequest = DefaultListRequest.builder().filter(Map.of()).across("project").ouIds(Set.of(3)).build();
        OUConfiguration ouConfig = unitsHelper.getOuConfigurationFromDBOrgUnit(COMPANY, Set.of(IntegrationType.SONARQUBE),
                defaultListRequest, dbOrgUnit2.orElseThrow(), false);
        defaultListRequest = ouConfig.getRequest();
        List<DbAggregationResult> aggs = issueService.groupByAndCalculate(COMPANY,
                getSQIssueFilterBuilder(COMPANY, defaultListRequest, currentTimeTrunc)
                        .distinct(SonarQubeIssueFilter.DISTINCT.author)
                        .ingestedAt(currentTimeTrunc)
                        .build()
                , null, ouConfig).getRecords();
        Assertions.assertThat(aggs).isNotEmpty();
        Assertions.assertThat(aggs.stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsOnly("srinath.chandrashekhar@levelops.io", "harsh@levelops.io");

        dbOrgUnit2 = unitsService.get(COMPANY, 4, true);
        defaultListRequest = DefaultListRequest.builder().filter(Map.of()).across("project").ouIds(Set.of(4)).build();
        ouConfig = unitsHelper.getOuConfigurationFromDBOrgUnit(COMPANY, Set.of(IntegrationType.SONARQUBE),
                defaultListRequest, dbOrgUnit2.orElseThrow(), false);
        defaultListRequest = ouConfig.getRequest();

        List<DbSonarQubeIssue> issues = issueService.list(COMPANY, getSQIssueFilterBuilder(COMPANY, defaultListRequest, currentTimeTrunc)
                        .ingestedAt(currentTimeTrunc).build(),
                Map.of(), 0, 1000, ouConfig).getRecords();
        Assertions.assertThat(issues).isNotEmpty();
        Assertions.assertThat(issues.size()).isEqualTo(5);
        Assertions.assertThat(issues.stream().map(DbSonarQubeIssue::getSeverity).collect(Collectors.toList()))
                .containsOnly("MINOR");
        Assertions.assertThat(issues.stream().map(DbSonarQubeIssue::getOrganization).collect(Collectors.toList()))
                .containsOnly("default-organization");
        Assertions.assertThat(issues.stream().map(DbSonarQubeIssue::getType).collect(Collectors.toList()))
                .containsOnly("CODE_SMELL");

        dbOrgUnit2 = unitsService.get(COMPANY, 5, true);
        defaultListRequest = DefaultListRequest.builder().filter(Map.of()).across("project").ouIds(Set.of(5)).build();
        ouConfig = unitsHelper.getOuConfigurationFromDBOrgUnit(COMPANY, Set.of(IntegrationType.SONARQUBE),
                defaultListRequest, dbOrgUnit2.orElseThrow(), false);
        defaultListRequest = ouConfig.getRequest();
        issues = issueService.list(COMPANY, getSQIssueFilterBuilder(COMPANY, defaultListRequest, currentTimeTrunc)
                        .ingestedAt(currentTimeTrunc).build(),
                Map.of(), 0, 1000, ouConfig).getRecords();
        Assertions.assertThat(issues).isNotEmpty();
        Assertions.assertThat(issues.size()).isEqualTo(6);
        Assertions.assertThat(issues.stream().map(DbSonarQubeIssue::getSeverity).collect(Collectors.toList()))
                .containsOnly("CRITICAL");
        Assertions.assertThat(issues.stream().map(DbSonarQubeIssue::getStatus).collect(Collectors.toList()))
                .containsOnly("OPEN");


        dbOrgUnit2 = unitsService.get(COMPANY, 6, true);
        defaultListRequest = DefaultListRequest.builder().filter(Map.of()).across("project").ouIds(Set.of(6)).build();
        ouConfig = unitsHelper.getOuConfigurationFromDBOrgUnit(COMPANY, Set.of(IntegrationType.SONARQUBE),
                defaultListRequest, dbOrgUnit2.orElseThrow(), false);
        defaultListRequest = ouConfig.getRequest();
        issues = issueService.list(COMPANY, getSQIssueFilterBuilder(COMPANY, defaultListRequest, currentTimeTrunc)
                        .ingestedAt(currentTimeTrunc).build(),
                Map.of(), 0, 1000, ouConfig).getRecords();
        Assertions.assertThat(issues).isNotEmpty();
        Assertions.assertThat(issues.size()).isEqualTo(4);
        Assertions.assertThat(issues.stream().map(DbSonarQubeIssue::getSeverity).collect(Collectors.toList()))
                .containsOnly("CRITICAL");
        Assertions.assertThat(issues.stream().map(DbSonarQubeIssue::getStatus).collect(Collectors.toList()))
                .containsOnly("OPEN");
        Assertions.assertThat(issues.stream().map(DbSonarQubeIssue::getAuthor).collect(Collectors.toList()))
                .containsOnly("srinath.chandrashekhar@levelops.io");
    }

    public static List<DbSonarQubeIssue> readIssues(String resourceName) throws IOException {
        String resourceIssue = ResourceUtils.getResourceAsString(resourceName);

        PaginatedResponse<Issue> issues = m.readValue(resourceIssue, m.getTypeFactory()
                .constructParametricType(PaginatedResponse.class, Issue.class));
        return issues.getResponse().getRecords().stream()
                .map(issue -> DbSonarQubeIssue.fromIssue(issue, INTEGRATION_ID, currentTime))
                .collect(Collectors.toList());
    }

    public static List<DbSonarQubeProject> readProjects(String resourceName) throws IOException {
        String resourceProject = ResourceUtils.getResourceAsString(resourceName);
        PaginatedResponse<Project> projects = m.readValue(resourceProject, m.getTypeFactory()
                .constructParametricType(PaginatedResponse.class, Project.class));
        return projects.getResponse().getRecords().stream()
                .map(project -> DbSonarQubeProject.fromComponent(project, INTEGRATION_ID, currentTime))
                .collect(Collectors.toList());
    }

    public static SonarQubeIssueFilter.SonarQubeIssueFilterBuilder getSQIssueFilterBuilder(
            String company,
            DefaultListRequest filter, Long currentTimeTrunc) {
        return SonarQubeIssueFilter.builder()
                .projects(getListOrDefault(filter.getFilter(), "projects"))
                .types(getListOrDefault(filter.getFilter(), "types"))
                .severities(getListOrDefault(filter.getFilter(), "severities"))
                .statuses(getListOrDefault(filter.getFilter(), "statuses"))
                .organizations(getListOrDefault(filter.getFilter(), "organizations"))
                .authors(getListOrDefault(filter.getFilter(), "authors"))
                .components(getListOrDefault(filter.getFilter(), "components"))
                .tags(getListOrDefault(filter.getFilter(), "tags"))
                .integrationIds(getListOrDefault(filter.getFilter(), "integration_ids"))
                .ingestedAt(currentTimeTrunc);

    }
}
