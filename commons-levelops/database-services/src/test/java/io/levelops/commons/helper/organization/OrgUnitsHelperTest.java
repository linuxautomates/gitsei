package io.levelops.commons.helper.organization;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Integration.Authentication;
import io.levelops.commons.databases.models.database.organization.DBOrgContentSection;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.models.database.organization.DBOrgUser.LoginId;
import io.levelops.commons.databases.models.database.organization.OrgUnitCategory;
import io.levelops.commons.databases.models.database.organization.OrgUserId;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ProductService;
import io.levelops.commons.databases.services.TagItemDBService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.databases.services.dev_productivity.DevProductivityProfileDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitCategoryDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.databases.services.velocity.OrgProfileDatabaseService;
import io.levelops.commons.databases.services.velocity.VelocityConfigsDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.ingestion.models.IntegrationType;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;

public class OrgUnitsHelperTest {

    private static final String company = "test";
    private static final ObjectMapper mapper = DefaultObjectMapper.get();

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;

    private static NamedParameterJdbcTemplate template;

    private static OrgUnitHelper unitsHelper;
    private static OrgVersionsDatabaseService versionsService;
    private static OrgUsersDatabaseService usersService;
    private static UserService userService;
    private static OrgUnitsDatabaseService unitsService;
    private static UserIdentityService userIdentityService;
    private static IntegrationService integrationService;
    private static TagItemDBService tagItemService;
    private static OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService;
    private static OrgUnitCategory orgGroup1;
    private static String orgGroupId1;

    private static OrgProfileDatabaseService ouProfileDbService;
    private static VelocityConfigsDatabaseService velocityConfigDbService;
    private static DevProductivityProfileDatabaseService devProductivityProfileDbService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private static TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        template = new NamedParameterJdbcTemplate(dataSource);
        List.<String>of(
                        "CREATE SCHEMA IF NOT EXISTS " + company,
                        "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";"
                )
                .forEach(template.getJdbcTemplate()::execute);
        dataSource.getConnection().prepareStatement(arrayUniq)
                .execute();
        versionsService = new OrgVersionsDatabaseService(dataSource);
        versionsService.ensureTableExistence(company);

        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);

        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);

        usersService = new OrgUsersDatabaseService(dataSource, mapper, versionsService, userIdentityService);
        usersService.ensureTableExistence(company);
        userService = new UserService(dataSource, mapper);
        userService.ensureTableExistence(company);
        DashboardWidgetService dashboardWidgetService = new DashboardWidgetService(dataSource, mapper);
        dashboardWidgetService.ensureTableExistence(company);
        new ProductService(dataSource).ensureTableExistence(company);
        orgUnitCategoryDatabaseService = new OrgUnitCategoryDatabaseService(dataSource, unitsHelper, mapper);
        orgUnitCategoryDatabaseService.ensureTableExistence(company);
        unitsService = new OrgUnitsDatabaseService(dataSource, mapper, tagItemService, usersService, versionsService, dashboardWidgetService);
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
        orgGroup1 = OrgUnitCategory.builder()
                .name("TEAM A")
                .description("Sample team")
                .isPredefined(true)
                .build();
        orgGroupId1 = orgUnitCategoryDatabaseService.insert(company, orgGroup1);
    }

    @Before
    public void cleanUp() {
        var statements = List.of(
                "DELETE FROM {0}.ou_content_sections;",
                "DELETE FROM {0}.org_user_cloud_id_mapping;",
                "DELETE FROM {0}.integrations;"
        );
        statements.stream()
                .map(st -> MessageFormat.format(st, company))
                .forEach(template.getJdbcOperations()::execute);
    }

    @Test
    public void test() throws SQLException {
        var integration1 = Integration.builder()
                .name("name")
                .application("test")
                .description("description")
                .authentication(Authentication.API_KEY)
                .metadata(Map.of())
                .satellite(false)
                .tags(List.of())
                .status("ok")
                .build();
        var integrationId1 = Integer.parseInt(integrationService.insert(company, integration1));
        unitsHelper.activateVersion(company, 1, 1);

        var orgUser1 = DBOrgUser.builder()
                .email("email1")
                .fullName("fullName1")
                .customFields(Map.of("test_name", "test1"))
                .ids(Set.of(LoginId.builder().cloudId("cloudId").username("cloudId").integrationType(integration1.getApplication()).integrationId(integrationId1).build()))
                .versions(Set.of(1))
                .build();
        var userId1 = usersService.upsert(company, orgUser1);

        var orgUser2 = DBOrgUser.builder()
                .email("email2")
                .fullName("fullName2")
                .customFields(Map.of("sample_name", "sample"))
                .ids(Set.of(LoginId.builder().cloudId("cloudId2").integrationId(integrationId1).build()))
                .versions(Set.of(2, 3))
                .build();
        var userId2 = usersService.upsert(company, orgUser2);

        var manager1 = OrgUserId.builder().id(userId1.getId()).refId(userId1.getRefId()).fullName(orgUser1.getFullName()).email(orgUser1.getEmail()).build();
        var manager2 = OrgUserId.builder().id(userId2.getId()).refId(userId2.getRefId()).fullName(orgUser2.getFullName()).email(orgUser2.getEmail()).build();
        var managers = Set.of(
                manager1,
                manager2
        );
        Stream<DBOrgUnit> units = List.<DBOrgUnit>of(DBOrgUnit.builder()
                .name("unit1")
                .description("My unit1")
                .active(false)
                .managers(managers)
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .sections(Set.of(DBOrgContentSection.builder().users(Set.of(1, 2)).build()))
                .build()).stream();
        var ids = unitsHelper.insertNewOrgUnits(company, units);

        var v1 = unitsService.get(company, ids.iterator().next()).get();

        Assertions.assertThat(v1.getVersions()).isEqualTo(Set.of(1));

        var tagIds = Set.of(1, 2);
        var unitUpdate = v1.toBuilder()
                .description("Updated!")
                .tags(Set.of(""))
                .tagIds(tagIds)
                .managers(Set.of(manager1))
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .sections(Set.of(DBOrgContentSection.builder().dynamicUsers(Map.of("location", "city")).users(Set.of(1, 2)).build()))
                .build();
        unitsHelper.updateUnits(company, Set.of(unitUpdate).stream());

        DBOrgUnit dbUnitUpdated = unitsService.get(company, unitUpdate.getRefId()).get();

        Assertions.assertThat(dbUnitUpdated.getDescription()).isEqualTo(unitUpdate.getDescription());
        Assertions.assertThat(dbUnitUpdated.getSections().stream().map(s -> s.toBuilder().id(null).build()).collect(Collectors.toSet())).containsExactlyInAnyOrderElementsOf(unitUpdate.getSections().stream().map(s -> s.toBuilder().id(null).build()).collect(Collectors.toSet()));
        Assertions.assertThat(dbUnitUpdated.getTagIds()).isEqualTo(tagIds);
    }

    @Test
    public void testOUConfig() throws SQLException {
        var integration1 = Integration.builder()
                .name("name")
                .application("jira")
                .description("description")
                .authentication(Authentication.API_KEY)
                .metadata(Map.of())
                .satellite(false)
                .tags(List.of())
                .status("ok")
                .build();
        var integrationId1 = Integer.parseInt(integrationService.insert(company, integration1));

        var orgUser1 = DBOrgUser.builder()
                .email("email1")
                .fullName("fullName1")
                .customFields(Map.of("test_name", "test1"))
                .ids(Set.of(LoginId.builder().cloudId("cloudId").username("cloudId").integrationType(integration1.getApplication()).integrationId(integrationId1).build()))
                .versions(Set.of(1))
                .build();
        var userId1 = usersService.upsert(company, orgUser1);

        var orgUser2 = DBOrgUser.builder()
                .email("email2")
                .fullName("fullName2")
                .customFields(Map.of("sample_name", "sample"))
                .ids(Set.of(LoginId.builder().cloudId("cloudId2").integrationId(integrationId1).build()))
                .versions(Set.of(2, 3))
                .build();
        var userId2 = usersService.upsert(company, orgUser2);

        var manager1 = OrgUserId.builder().id(userId1.getId()).refId(userId1.getRefId()).fullName(orgUser1.getFullName()).email(orgUser1.getEmail()).build();
        var manager2 = OrgUserId.builder().id(userId2.getId()).refId(userId2.getRefId()).fullName(orgUser2.getFullName()).email(orgUser2.getEmail()).build();
        var managers = Set.of(
                manager1,
                manager2
        );
        Stream<DBOrgUnit> units = List.<DBOrgUnit>of(
                DBOrgUnit.builder()
                        .name("unit1")
                        .description("My unit1")
                        .active(false)
                        .managers(managers)
                        .ouCategoryId(UUID.fromString(orgGroupId1))
                        .sections(Set.of(DBOrgContentSection.builder()
                                .integrationId(integrationId1)
                                .integrationFilters(Map.of(
                                        "projects", Set.of("FE"),
                                        "status", Set.of("ok"),
                                        "custom_fields", Map.of(
                                                "custom_field01", "value1"),
                                        "partial", Map.of(
                                                "product", "level",
                                                "custom_fields", Map.of(
                                                        "p_custom_filed_1000", "value"))
                                ))
                                .users(Set.of(1, 2))
                                .build()))
                        .build(),
                DBOrgUnit.builder()
                        .name("unit2")
                        .description("My unit2")
                        .active(false)
                        .managers(managers)
                        .ouCategoryId(UUID.fromString(orgGroupId1))
                        .sections(Set.of(DBOrgContentSection.builder()
                                .integrationId(integrationId1)
                                .integrationFilters(Map.of(
                                        "projects", Set.of("FE"),
                                        "status", Set.of("ok"),
                                        "custom_fields", Map.of(
                                                "custom_field01", "value1"),
                                        "partial_match", Map.of(
                                                "product", "level",
                                                "custom_fields", Map.of(
                                                        "p_custom_filed_1000", "value"))
                                ))
                                .users(Set.of(1, 2))
                                .build()))
                        .build(),
                DBOrgUnit.builder()
                        .name("unit3")
                        .description("My unit3")
                        .active(false)
                        .managers(managers)
                        .ouCategoryId(UUID.fromString(orgGroupId1))
                        .sections(Set.of(DBOrgContentSection.builder()
                                .integrationId(integrationId1)
                                .integrationFilters(Map.of(
                                        "projects", Set.of("FE"),
                                        "status", Set.of("ok"),
                                        "sprint", List.of("Sprint 1", "Sprint 3"),
                                        "exclude", Map.of(
                                                "sprint", List.of("Sprint 2")
                                        ),
                                        "partial_match", Map.of(
                                                "sprint", Map.of(
                                                        "$contains", "Sprint"))
                                ))
                                .users(Set.of(1, 2))
                                .build()))
                        .build(),
                DBOrgUnit.builder()
                        .name("unit4")
                        .description("My unit4")
                        .active(false)
                        .managers(managers)
                        .ouCategoryId(UUID.fromString(orgGroupId1))
                        .sections(Set.of(DBOrgContentSection.builder()
                                .integrationId(integrationId1)
                                .integrationFilters(Map.of(
                                        "projects", Set.of("FE"),
                                        "status", Set.of("ok"),
                                        "sprint", List.of("Sprint 1", "Sprint 3"),
                                        "custom_fields", Map.of(
                                                "custom_field01", "valueOu"),
                                        "exclude", Map.of(
                                                "projects", List.of("ou")
                                        ),
                                        "partial_match", Map.of(
                                                "sprint", Map.of(
                                                        "$contains", "ou"))
                                ))
                                .users(Set.of(1, 2))
                                .build()))
                        .build(),
                DBOrgUnit.builder()
                        .name("unit5")
                        .description("My unit5")
                        .active(false)
                        .managers(managers)
                        .ouCategoryId(UUID.fromString(orgGroupId1))
                        .sections(Set.of(DBOrgContentSection.builder()
                                .integrationId(integrationId1)
                                .integrationFilters(Map.of(
                                        "workitem_projects", List.of("FE"),
                                        "workitem_custom_fields", Map.of(
                                                "custom_field01", "valueOu"),
                                        "exclude", Map.of(
                                                "workitem_projects", List.of("ou")
                                        ),
                                        "workitem_attributes", Map.of(
                                                "team", Map.of(
                                                        "$contains", "ou"))
                                ))
                                .users(Set.of(1, 2))
                                .build()))
                        .build()).stream();
        var ids = unitsHelper.insertNewOrgUnits(company, units);
        var idIt = ids.iterator();
        var id1 = idIt.next();
        var id2 = idIt.next();
        var id3 = idIt.next();
        var id4 = idIt.next();
        var id5 = idIt.next();

        var integrationType = IntegrationType.JIRA;
        var request = DefaultListRequest.builder()
                .filter(Map.of(
                        "name", "AB",
                        "custom_fields", Map.of("custom_field01", "value_request", "custom_field02", "value2"),
                        "partial", Map.of("name", "LO")
                ))
                .ouIds(Set.of(id1))
                .ouUserFilterDesignation(Map.of("github", Set.of("committer")))
                .build();
        var config = unitsHelper.getOuConfigurationFromRequest(company, integrationType, request);

        Assertions.assertThat(config.getRequest().getFilter()).containsAllEntriesOf
                (Map.of(
                        "name", "AB",
                        "integration_ids", Set.of(integrationId1),
                        "projects", List.of("FE"),
                        "status", List.of("ok"),
                        "custom_fields", Map.of("custom_field01", "value1", "custom_field02", "value2"),
                        "partial", Map.of("product", "level", "name", "LO", "custom_fields", Map.of("p_custom_filed_1000", "value"))
                ));
        Assertions.assertThat(config.getScmFields()).containsExactlyElementsOf(Set.of("committer"));
        Assertions.assertThat(config.getGithubFields()).containsExactlyElementsOf(Set.of("committer"));

        integrationType = IntegrationType.JIRA;
        request = DefaultListRequest.builder()
                .filter(Map.of(
                        "status", List.of("Done"),
                        "sprint_report", List.of("Sprint 2"),
                        "custom_fields", Map.of(
                                "custom_field01", "valueFilter"),
                        "exclude", Map.of(
                                "projects", List.of("valueFilter")
                        ),
                        "partial_match", Map.of(
                                "sprint_report", Map.of(
                                        "$contains", "valueFilter"))
                ))
                .ouIds(Set.of(id4))
                .ouExclusions(List.of())
                .ouUserFilterDesignation(Map.of("sprint", Set.of("sprint_report")))
                .ouExclusions(List.of("status", "sprint", "projects", "custom_field01"))
                .build();
        config = unitsHelper.getOuConfigurationFromRequest(company, integrationType, request);

        Assertions.assertThat(config.getRequest().getFilter()).containsAllEntriesOf
                (Map.of(
                        "status", List.of("Done"),
                        "sprint_report", List.of("Sprint 2"),
                        "custom_fields", Map.of(
                                "custom_field01", "valueFilter"),
                        "exclude", Map.of(
                                "projects", List.of("valueFilter")
                        ),
                        "partial_match", Map.of(
                                "sprint_report", Map.of(
                                        "$contains", "valueFilter"))
                ));

        integrationType = IntegrationType.JIRA;
        request = DefaultListRequest.builder()
                .filter(Map.of(
                        "status", List.of("Done"),
                        "sprint_report", List.of("Sprint 2"),
                        "custom_fields", Map.of(
                                "custom_field01", "valueFilter"),
                        "exclude", Map.of(
                                "projects", List.of("valueFilter")
                        ),
                        "partial_match", Map.of(
                                "sprint_report", Map.of(
                                        "$contains", "valueFilter"))
                ))
                .ouIds(Set.of(id4))
                .ouExclusions(List.of())
                .ouUserFilterDesignation(Map.of("sprint", Set.of("sprint_report")))
                .ouExclusions(List.of("status", "custom_field01"))
                .build();
        config = unitsHelper.getOuConfigurationFromRequest(company, integrationType, request);

        Assertions.assertThat(config.getRequest().getFilter()).containsAllEntriesOf
                (Map.of(
                        "projects", List.of("FE"),
                        "status", List.of("Done"),
                        "sprint_report", List.of("Sprint 1", "Sprint 3"),
                        "custom_fields", Map.of(
                                "custom_field01", "valueFilter"),
                        "exclude", Map.of(
                                "projects", List.of("ou")
                        ),
                        "partial_match", Map.of(
                                "sprint_report", Map.of(
                                        "$contains", "ou"))
                ));

        request = DefaultListRequest.builder()
                .filter(Map.of(
                        "workitem_projects", List.of("test"),
                        "workitem_custom_fields", Map.of(
                                "custom_field01", "valueFilter"),
                        "exclude", Map.of(
                                "workitem_projects", List.of("filter"),
                                "workitem_statuses", List.of("todo")
                        ),
                        "workitem_attributes", Map.of(
                                "team", Map.of(
                                        "$contains", "filter"),
                                "code", Map.of(
                                        "$contains", "test"))
                ))
                .ouIds(Set.of(id5))
                .ouExclusions(List.of())
                .ouUserFilterDesignation(Map.of("sprint", Set.of("sprint_report")))
                .build();
        config = unitsHelper.getOuConfigurationFromRequest(company, integrationType, request);

        Assertions.assertThat(config.getRequest().getFilter()).containsAllEntriesOf
                (Map.of(
                        "workitem_projects", List.of("FE"),
                        "workitem_custom_fields", Map.of(
                                "custom_field01", "valueOu"),
                        "exclude", Map.of(
                                "workitem_projects", List.of("ou"),
                                "workitem_statuses", List.of("todo")
                        ),
                        "workitem_attributes", Map.of(
                                "team", Map.of(
                                        "$contains", "ou"),
                                "code", Map.of(
                                        "$contains", "test"))
                ));

        integrationType = IntegrationType.JIRA;
        request = DefaultListRequest.builder()
                .filter(Map.of(
                        "name", "AB",
                        "partial_match", Map.of("name", "LO")
                ))
                .ouIds(Set.of(id3))
                .ouUserFilterDesignation(Map.of("sprint", Set.of("customfield_10020")))
                .build();
        config = unitsHelper.getOuConfigurationFromRequest(company, integrationType, request);

        Assertions.assertThat(config.getRequest().getFilter()).containsAllEntriesOf
                (Map.of(
                        "name", "AB",
                        "integration_ids", Set.of(integrationId1),
                        "projects", List.of("FE"),
                        "status", List.of("ok"),
                        "customfield_10020", List.of("Sprint 1", "Sprint 3"),
                        "exclude", Map.of(
                                "customfield_10020", List.of("Sprint 2")
                        ),
                        "partial_match", Map.of("customfield_10020", Map.of("$contains", "Sprint"), "name", "LO")
                ));

        integrationType = IntegrationType.JIRA;
        request = DefaultListRequest.builder()
                .filter(Map.of(
                        "name", "AB",
                        "sprint_names", List.of("Sprint 2"),
                        "exclude", Map.of(
                                "sprint_names", List.of("Sprint 11"), "status", List.of("todo")
                        ),
                        "partial_match", Map.of("name", "LO")
                ))
                .ouIds(Set.of(id3))
                .ouUserFilterDesignation(Map.of("sprint", Set.of("sprint_names")))
                .build();
        config = unitsHelper.getOuConfigurationFromRequest(company, integrationType, request);

        Assertions.assertThat(config.getRequest().getFilter()).containsAllEntriesOf
                (Map.of(
                        "name", "AB",
                        "integration_ids", Set.of(integrationId1),
                        "projects", List.of("FE"),
                        "status", List.of("ok"),
                        "sprint_names", List.of("Sprint 1", "Sprint 3"),
                        "exclude", Map.of(
                                "sprint_names", List.of("Sprint 2"), "status", List.of("todo")
                        ),
                        "partial_match", Map.of("sprint_names", Map.of("$contains", "Sprint"), "name", "LO")
                ));

        integrationType = IntegrationType.JIRA;
        request = DefaultListRequest.builder()
                .filter(Map.of(
                        "name", "AB",
                        "sprint_report", List.of("Sprint 2"),
                        "exclude", Map.of(
                                "sprint_report", List.of("Sprint 11"), "status", List.of("todo")
                        ),
                        "partial_match", Map.of("name", "LO")
                ))
                .ouIds(Set.of(id3))
                .ouUserFilterDesignation(Map.of("sprint", Set.of("sprint_report")))
                .build();
        config = unitsHelper.getOuConfigurationFromRequest(company, integrationType, request);

        Assertions.assertThat(config.getRequest().getFilter()).containsAllEntriesOf
                (Map.of(
                        "name", "AB",
                        "integration_ids", Set.of(integrationId1),
                        "projects", List.of("FE"),
                        "status", List.of("ok"),
                        "sprint_report", List.of("Sprint 1", "Sprint 3"),
                        "exclude", Map.of(
                                "sprint_report", List.of("Sprint 2"), "status", List.of("todo")
                        ),
                        "partial_match", Map.of("sprint_report", Map.of("$contains", "Sprint"), "name", "LO")
                ));

        // Test mixing filters for custom_fields inside partial match
        var request2 = DefaultListRequest.builder()
                .filter(Map.of(
                        "name", "AB",
                        "custom_fields", Map.of("custom_field01", "value_request", "custom_field02", "value2"),
                        "partial_match", Map.of(
                                "name", "LO",
                                "custom_fields", Map.of("custom_field03", "hello")
                        )
                ))
                .ouIds(Set.of(id2))
                .build();
        var config2 = unitsHelper.getOuConfigurationFromRequest(company, integrationType, request2);
        Assertions.assertThat(config2).isNotNull();
        Assertions.assertThat(config2.getRequest().getFilter()).containsExactlyInAnyOrderEntriesOf
                (Map.of(
                        "name", "AB",
                        "integration_ids", Set.of(integrationId1),
                        "projects", List.of("FE"),
                        "status", List.of("ok"),
                        "custom_fields", Map.of("custom_field01", "value1", "custom_field02", "value2"),
                        "partial_match", Map.of(
                                "product", "level",
                                "name", "LO",
                                "custom_fields", Map.of(
                                        "p_custom_filed_1000", "value",
                                        "custom_field03", "hello"))
                ));

        // Test the special filters when a prefix needs to be used
        var request3 = DefaultListRequest.builder()
                .filter(Map.of(
                        "name", "AB",
                        "jira_custom_fields", Map.of("custom_field01", "value_request", "custom_field02", "value2"),
                        "partial", Map.of(
                                "name", "LO",
                                "jira_custom_fields", Map.of("custom_field03", "hello")
                        )
                ))
                .ouIds(Set.of(id1))
                .build();
        var config3 = unitsHelper.getOuConfigurationFromRequest(company, integrationType, request3, true);
        Assertions.assertThat(config3).isNotNull();
        Assertions.assertThat(config3.getRequest().getFilter()).containsExactlyInAnyOrderEntriesOf
                (Map.of(
                        "name", "AB",
                        "integration_ids", Set.of(integrationId1),
                        "jira_projects", List.of("FE"),
                        "jira_status", List.of("ok"),
                        "jira_custom_fields", Map.of("custom_field01", "value1", "custom_field02", "value2"),
                        "partial", Map.of("jira_product", "level", "name", "LO", "jira_custom_fields", Map.of("p_custom_filed_1000", "value", "custom_field03", "hello"))
                ));
    }

    @Test
    public void testOuConfigForPagerDuty() throws SQLException {
        var integration2 = Integration.builder()
                .name("name")
                .application("pagerduty")
                .description("description")
                .authentication(Authentication.API_KEY)
                .metadata(Map.of())
                .satellite(false)
                .tags(List.of())
                .status("ok")
                .build();
        var integrationId2 = Integer.parseInt(integrationService.insert(company, integration2));
        var orgUser1 = DBOrgUser.builder()
                .email("email2")
                .fullName("fullName2")
                .customFields(Map.of("test_name", "test2"))
                .ids(Set.of(LoginId.builder().cloudId("cloudId123").username("cloudId123").integrationType(integration2.getApplication()).integrationId(integrationId2).build()))
                .versions(Set.of(1))
                .build();
        var userId1 = usersService.upsert(company, orgUser1);
        var manager1 = OrgUserId.builder().id(userId1.getId()).refId(userId1.getRefId()).fullName(orgUser1.getFullName()).email(orgUser1.getEmail()).build();
        var managers = Set.of(
                manager1
        );
        Stream<DBOrgUnit> units = List.<DBOrgUnit>of(DBOrgUnit.builder()
                .name("unit4")
                .description("My unit4")
                .active(false)
                .managers(managers)
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(integrationId2)
                        .integrationFilters(Map.of(
                                "incident_urgencies", List.of("high")
                        ))
                        .users(Set.of(1, 2))
                        .build()))
                .build()).stream();
        var ids = unitsHelper.insertNewOrgUnits(company, units);
        var idIt = ids.iterator();
        var id1 = idIt.next();
        IntegrationType integrationType = IntegrationType.PAGERDUTY;
        var request4 = DefaultListRequest.builder()
                .filter(Map.of(
                        "incident_urgencies", List.of("low")
                ))
                .ouIds(Set.of(id1))
                .build();
        var config4 = unitsHelper.getOuConfigurationFromRequest(company, integrationType, request4);
        Assertions.assertThat(config4.getRequest().getFilter()).containsAllEntriesOf
                (Map.of(
                        "incident_urgencies", List.of("high")
                ));
    }

    @Test
    public void testGetSelectForCloudIdsByOuConfig() {
        var params = new HashMap<String, Object>();
        var ouConfig = OUConfiguration.builder()
                .ouId(UUID.fromString("f1d7d81a-87ab-4823-89cf-45dfdc7dae93"))
                .ouRefId(1)
                .staticUsers(true)
                .dynamicUsers(false)
                .integrationIds(Set.of(1))
                .request(DefaultListRequest.builder().filter(Map.of("integration_ids", List.of(1))).build())
                .sections(Set.of(DBOrgContentSection.builder()
                        .id(UUID.fromString("7c17553f-faaa-4cb8-8cdb-4e01c3a551f7"))
                        .integrationId(1)
                        .integrationType(IntegrationType.JIRA)
                        .users(Set.of(1))
                        .build()))
                .build();
        var ouUsersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params);
        Assertions.assertThat(ouUsersSelect).isEqualTo("");
        ouUsersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params, IntegrationType.JIRA);

        Assertions.assertThat(ouUsersSelect).isEqualTo(
                "SELECT iu.id, iu.display_name, iu.cloud_id, iu.integration_id \n"
                        + "FROM test.integration_users iu, test.ou_content_sections o_s, test.org_users o_u, test.org_user_cloud_id_mapping o_m \n"
                        + "WHERE \n"
                        + "    o_u.versions @> ARRAY(SELECT version FROM test.org_version_counter WHERE type = :org_user_selection_version_type AND active = true) \n"
                        + "    AND o_u.ref_id = ANY(o_s.user_ref_ids) \n"
                        + "    AND o_m.org_user_id = o_u.id \n"
                        + "    AND o_m.integration_user_id = iu.id \n"
                        + "    AND iu.integration_id IN (:ou_user_selection_integration_ids_1) \n"
                        + "    AND o_s.id = :ou_user_selection_section_id_1 \n"
                        + " GROUP BY iu.id, iu.display_name, iu.cloud_id, iu.integration_id");
        Assertions.assertThat(params).containsExactlyInAnyOrderEntriesOf(Map.of(
                "org_user_selection_version_type", "USER",
                "ou_user_selection_integration_ids_1", 1,
                "ou_user_selection_section_id_1", UUID.fromString("7c17553f-faaa-4cb8-8cdb-4e01c3a551f7")
        ));

        params = new HashMap<String, Object>();
        ouConfig = OUConfiguration.builder()
                .ouId(UUID.fromString("f1d7d81a-87ab-4823-89cf-45dfdc7dae93"))
                .ouRefId(1)
                .staticUsers(false)
                .dynamicUsers(true)
                .integrationIds(Set.of(1))
                .request(DefaultListRequest.builder().filter(Map.of("integration_ids", List.of("1"))).build())
                .sections(Set.of(DBOrgContentSection.builder()
                        .id(UUID.fromString("7c17553f-faaa-4cb8-8cdb-4e01c3a551f7"))
                        .integrationId(1)
                        .integrationType(IntegrationType.JIRA)
                        .dynamicUsers(Map.of(
                                "custom_field_location", "India",
                                "custom_field_designation", Map.of("$begins", "Senior")
                        ))
                        .build()))
                .build();
        ouUsersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params);
        Assertions.assertThat(ouUsersSelect).isEqualTo("");
        ouUsersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params, IntegrationType.JIRA);

        Assertions.assertThat(ouUsersSelect).isEqualTo(
                "SELECT iu.id, iu.display_name, iu.cloud_id, iu.integration_id \n"
                        + "FROM test.integration_users iu, test.ou_content_sections o_s, test.org_users o_u, test.org_user_cloud_id_mapping o_m \n"
                        + "WHERE \n"
                        + "    o_m.integration_user_id = iu.id \n"
                        + "    AND o_m.org_user_id = o_u.id  AND (o_u.custom_fields->>:org_user_condition_key_designation SIMILAR TO :o_u_c_o_u_custom_fields_org_user_condition_key_designation_begins)" +
                        " AND o_u.custom_fields->>:org_user_condition_key_location = :o_u_c_o_u_custom_fields_org_user_condition_key_location" +
                        " AND o_u.versions @> ARRAY(SELECT version FROM test.org_version_counter WHERE type = :org_user_selection_version_type AND active = true) \n"
                        + "    AND iu.integration_id IN (:ou_user_selection_integration_ids_1) \n"
                        + " GROUP BY iu.id, iu.display_name, iu.cloud_id, iu.integration_id");
        Assertions.assertThat(params).containsExactlyInAnyOrderEntriesOf(Map.of(
                "ou_user_selection_integration_ids_1", List.of(1),
                "org_user_selection_version_type", "USER",
                "org_user_condition_key_location", "location",
                "o_u_c_o_u_custom_fields_org_user_condition_key_location", "India",
                "org_user_condition_key_designation", "designation",
                "o_u_c_o_u_custom_fields_org_user_condition_key_designation_begins", "Senior%"
        ));

        params = new HashMap<String, Object>();
        ouConfig = OUConfiguration.builder()
                .ouId(UUID.fromString("f1d7d81a-87ab-4823-89cf-45dfdc7dae93"))
                .ouRefId(1)
                .staticUsers(true)
                .dynamicUsers(true)
                .integrationIds(Set.of(1, 2))
                .sections(Set.of(
                        DBOrgContentSection.builder()
                                .id(UUID.fromString("7c17553f-faaa-4cb8-8cdb-4e01c3a551f7"))
                                .integrationId(1)
                                .integrationType(IntegrationType.JIRA)
                                .dynamicUsers(Map.of(
                                        "custom_field_location", "India",
                                        "custom_field_designation", Map.of("$begins", "Senior")
                                ))
                                .build(),
                        DBOrgContentSection.builder()
                                .id(UUID.fromString("98e4933b-014d-4ac3-a350-89b77a7a291f"))
                                .integrationId(2)
                                .integrationType(IntegrationType.JIRA)
                                .users(Set.of(1))
                                .build()))
                .build();
        ouUsersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params);
        Assertions.assertThat(ouUsersSelect).isEqualTo("");
        ouUsersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params, IntegrationType.JIRA);

        Assertions.assertThat(ouUsersSelect).isEqualTo(
                "SELECT iu.id, iu.display_name, iu.cloud_id, iu.integration_id \n"
                        + "FROM test.integration_users iu, test.ou_content_sections o_s, test.org_users o_u, test.org_user_cloud_id_mapping o_m \n"
                        + "WHERE \n"
                        + "    o_u.versions @> ARRAY(SELECT version FROM test.org_version_counter WHERE type = :org_user_selection_version_type AND active = true) \n"
                        + "    AND o_u.ref_id = ANY(o_s.user_ref_ids) \n"
                        + "    AND o_m.org_user_id = o_u.id \n"
                        + "    AND o_m.integration_user_id = iu.id \n"
                        + "    AND iu.integration_id IN (:ou_user_selection_integration_ids_1) \n"
                        + "    AND o_s.id = :ou_user_selection_section_id_1 \n"

                        + "UNION\n"

                        + "SELECT iu.id, iu.display_name, iu.cloud_id, iu.integration_id \n"
                        + "FROM test.integration_users iu, test.ou_content_sections o_s, test.org_users o_u, test.org_user_cloud_id_mapping o_m \n"
                        + "WHERE \n"
                        + "    o_m.integration_user_id = iu.id \n"
                        + "    AND o_m.org_user_id = o_u.id  AND (o_u.custom_fields->>:org_user_condition_key_designation SIMILAR TO :o_u_c_o_u_custom_fields_org_user_condition_key_designation_begins)" +
                        " AND o_u.custom_fields->>:org_user_condition_key_location = :o_u_c_o_u_custom_fields_org_user_condition_key_location" +
                        " AND o_u.versions @> ARRAY(SELECT version FROM test.org_version_counter WHERE type = :org_user_selection_version_type AND active = true) \n"
                        + "    AND iu.integration_id IN (:ou_user_selection_integration_ids_2) \n"
                        + " GROUP BY iu.id, iu.display_name, iu.cloud_id, iu.integration_id");
        Assertions.assertThat(params).containsExactlyInAnyOrderEntriesOf(Map.of(
                "org_user_selection_version_type", "USER",
                "ou_user_selection_integration_ids_1", 2,
                "ou_user_selection_integration_ids_2", List.of(1),
                "ou_user_selection_section_id_1", UUID.fromString("98e4933b-014d-4ac3-a350-89b77a7a291f"),
                "org_user_condition_key_location", "location",
                "o_u_c_o_u_custom_fields_org_user_condition_key_location", "India",
                "org_user_condition_key_designation", "designation",
                "o_u_c_o_u_custom_fields_org_user_condition_key_designation_begins", "Senior%"
        ));
    }

    @Test
    public void testGetSelectForCloudIdsByOuConfigDefaultSection() {
        var params = new HashMap<String, Object>();
        var ouConfig = OUConfiguration.builder()
                .ouId(UUID.fromString("f1d7d81a-87ab-4823-89cf-45dfdc7dae93"))
                .ouRefId(1)
                .staticUsers(true)
                .dynamicUsers(false)
                .sections(Set.of(DBOrgContentSection.builder()
                        .id(UUID.fromString("7c17553f-faaa-4cb8-8cdb-4e01c3a551f7"))
                        .defaultSection(true)
                        .users(Set.of(1))
                        .build()))
                .build();
        var ouUsersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params);

        Assertions.assertThat(ouUsersSelect).isEqualTo(
                "SELECT iu.id, iu.display_name, iu.cloud_id, iu.integration_id \n"
                        + "FROM test.integration_users iu, test.ou_content_sections o_s, test.org_users o_u, test.org_user_cloud_id_mapping o_m \n"
                        + "WHERE \n"
                        + "    o_u.versions @> ARRAY(SELECT version FROM test.org_version_counter WHERE type = :org_user_selection_version_type AND active = true) \n"
                        + "    AND o_u.ref_id = ANY(o_s.user_ref_ids) \n"
                        + "    AND o_m.org_user_id = o_u.id \n"
                        + "    AND o_m.integration_user_id = iu.id \n"
                        // + "    AND iu.integration_id IN (:ou_user_selection_integration_ids_1) \n"
                        + "    AND o_s.id = :ou_user_selection_section_id_1 \n"
                        + " GROUP BY iu.id, iu.display_name, iu.cloud_id, iu.integration_id");
        Assertions.assertThat(params).containsExactlyInAnyOrderEntriesOf(Map.of(
                "org_user_selection_version_type", "USER",
                "ou_user_selection_integration_ids_1", List.of(),
                "ou_user_selection_section_id_1", UUID.fromString("7c17553f-faaa-4cb8-8cdb-4e01c3a551f7")
        ));

        params = new HashMap<String, Object>();
        ouConfig = OUConfiguration.builder()
                .ouId(UUID.fromString("f1d7d81a-87ab-4823-89cf-45dfdc7dae93"))
                .ouRefId(1)
                .staticUsers(false)
                .dynamicUsers(true)
                .sections(Set.of(DBOrgContentSection.builder()
                        .id(UUID.fromString("7c17553f-faaa-4cb8-8cdb-4e01c3a551f7"))
                        .defaultSection(true)
                        .dynamicUsers(Map.of(
                                "custom_field_location", "India",
                                "custom_field_designation", Map.of("$begins", "Senior")
                        ))
                        .build()))
                .build();
        ouUsersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params);

        Assertions.assertThat(ouUsersSelect).isEqualTo(
                "SELECT iu.id, iu.display_name, iu.cloud_id, iu.integration_id \n"
                        + "FROM test.integration_users iu, test.ou_content_sections o_s, test.org_users o_u, test.org_user_cloud_id_mapping o_m \n"
                        + "WHERE \n"
                        + "    o_m.integration_user_id = iu.id \n"
                        + "    AND o_m.org_user_id = o_u.id  AND (o_u.custom_fields->>:org_user_condition_key_designation SIMILAR TO :o_u_c_o_u_custom_fields_org_user_condition_key_designation_begins)" +
                        " AND o_u.custom_fields->>:org_user_condition_key_location = :o_u_c_o_u_custom_fields_org_user_condition_key_location" +
                        " AND o_u.versions @> ARRAY(SELECT version FROM test.org_version_counter WHERE type = :org_user_selection_version_type AND active = true) \n"
                        // + "    AND  \n"
                        + " GROUP BY iu.id, iu.display_name, iu.cloud_id, iu.integration_id");
        Assertions.assertThat(params).containsExactlyInAnyOrderEntriesOf(Map.of(
                "org_user_selection_version_type", "USER",
                "org_user_condition_key_location", "location",
                "o_u_c_o_u_custom_fields_org_user_condition_key_location", "India",
                "ou_user_selection_integration_ids_1", List.of(),
                "org_user_condition_key_designation", "designation",
                "o_u_c_o_u_custom_fields_org_user_condition_key_designation_begins", "Senior%"
        ));

        // Multiple sections with one default section
        // getSelectForCloudIdsByOuConfig with no integration type specified
        params = new HashMap<String, Object>();
        ouConfig = OUConfiguration.builder()
                .ouId(UUID.fromString("f1d7d81a-87ab-4823-89cf-45dfdc7dae93"))
                .ouRefId(1)
                .staticUsers(true)
                .dynamicUsers(true)
                .sections(Set.of(
                        DBOrgContentSection.builder()
                                .id(UUID.fromString("7c17553f-faaa-4cb8-8cdb-4e01c3a551f7"))
                                .defaultSection(true)
                                .dynamicUsers(Map.of(
                                        "custom_field_location", "India",
                                        "custom_field_designation", Map.of("$begins", "Senior")
                                ))
                                .build(),
                        DBOrgContentSection.builder()
                                .id(UUID.fromString("98e4933b-014d-4ac3-a350-89b77a7a291f"))
                                .integrationId(2)
                                .integrationType(IntegrationType.JIRA)
                                .users(Set.of(1))
                                .build()))
                .build();
        ouUsersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params);

        Assertions.assertThat(ouUsersSelect).isEqualTo(
                "SELECT iu.id, iu.display_name, iu.cloud_id, iu.integration_id \n"
                        + "FROM test.integration_users iu, test.ou_content_sections o_s, test.org_users o_u, test.org_user_cloud_id_mapping o_m \n"
                        + "WHERE \n"
                        + "    o_m.integration_user_id = iu.id \n"
                        + "    AND o_m.org_user_id = o_u.id  AND (o_u.custom_fields->>:org_user_condition_key_designation SIMILAR TO :o_u_c_o_u_custom_fields_org_user_condition_key_designation_begins) AND o_u.custom_fields->>:org_user_condition_key_location = :o_u_c_o_u_custom_fields_org_user_condition_key_location AND o_u.versions @> ARRAY(SELECT version FROM test.org_version_counter WHERE type = :org_user_selection_version_type AND active = true) \n"
                        // + "    AND \n"
                        + " GROUP BY iu.id, iu.display_name, iu.cloud_id, iu.integration_id");
        Assertions.assertThat(params).containsExactlyInAnyOrderEntriesOf(Map.of(
                "ou_user_selection_integration_ids_1", List.of(),
                "org_user_selection_version_type", "USER",
                "org_user_condition_key_location", "location",
                "o_u_c_o_u_custom_fields_org_user_condition_key_location", "India",
                "org_user_condition_key_designation", "designation",
                "o_u_c_o_u_custom_fields_org_user_condition_key_designation_begins", "Senior%"
        ));

        params = new HashMap<String, Object>();
        ouConfig = OUConfiguration.builder()
                .ouId(UUID.fromString("f1d7d81a-87ab-4823-89cf-45dfdc7dae93"))
                .ouRefId(1)
                .staticUsers(true)
                .dynamicUsers(true)
                .integrationIds(Set.of(2))
                .sections(Set.of(
                        DBOrgContentSection.builder()
                                .id(UUID.fromString("7c17553f-faaa-4cb8-8cdb-4e01c3a551f7"))
                                .defaultSection(true)
                                .dynamicUsers(Map.of(
                                        "custom_field_location", "India",
                                        "custom_field_designation", Map.of("$begins", "Senior")
                                ))
                                .build(),
                        DBOrgContentSection.builder()
                                .id(UUID.fromString("98e4933b-014d-4ac3-a350-89b77a7a291f"))
                                .integrationId(2)
                                .integrationType(IntegrationType.JIRA)
                                .users(Set.of(1))
                                .build()))
                .build();
        ouUsersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params);

        Assertions.assertThat(ouUsersSelect).isEqualTo(
                // "SELECT iu.id, iu.display_name, iu.cloud_id, iu.integration_id \n"
                // + "FROM test.integration_users iu, test.ou_content_sections o_s, test.org_users o_u, test.org_user_cloud_id_mapping o_m \n"
                // + "WHERE \n"
                // + "    o_u.versions @> ARRAY(SELECT version FROM test.org_version_counter WHERE type = :org_user_selection_version_type AND active = true) \n"
                // + "    AND o_u.ref_id = ANY(o_s.user_ref_ids) \n"
                // + "    AND o_m.org_user_id = o_u.id \n"
                // + "    AND o_m.integration_user_id = iu.id \n"
                // + "    AND iu.integration_id IN (:ou_user_selection_integration_ids_1) \n"
                // + "    AND o_s.id = :ou_user_selection_section_id_1 \n"

                // + "UNION\n"

                "SELECT iu.id, iu.display_name, iu.cloud_id, iu.integration_id \n"
                        + "FROM test.integration_users iu, test.ou_content_sections o_s, test.org_users o_u, test.org_user_cloud_id_mapping o_m \n"
                        + "WHERE \n"
                        + "    o_m.integration_user_id = iu.id \n"
                        + "    AND o_m.org_user_id = o_u.id  AND (o_u.custom_fields->>:org_user_condition_key_designation SIMILAR TO :o_u_c_o_u_custom_fields_org_user_condition_key_designation_begins)" +
                        " AND o_u.custom_fields->>:org_user_condition_key_location = :o_u_c_o_u_custom_fields_org_user_condition_key_location" +
                        " AND o_u.versions @> ARRAY(SELECT version FROM test.org_version_counter WHERE type = :org_user_selection_version_type AND active = true) \n"
                        // + "    AND o_m.integration_user_id = iu.id \n"
                        + " GROUP BY iu.id, iu.display_name, iu.cloud_id, iu.integration_id");
        Assertions.assertThat(params).containsExactlyInAnyOrderEntriesOf(Map.of(
                "org_user_selection_version_type", "USER",
                // "ou_user_selection_integration_ids_1", 2,
                // "ou_user_selection_section_id_1", UUID.fromString("98e4933b-014d-4ac3-a350-89b77a7a291f"),
                "ou_user_selection_integration_ids_1", List.of(),
                "org_user_condition_key_location", "location",
                "o_u_c_o_u_custom_fields_org_user_condition_key_location", "India",
                "org_user_condition_key_designation", "designation",
                "o_u_c_o_u_custom_fields_org_user_condition_key_designation_begins", "Senior%"
        ));
    }

    @Test
    public void testOUConfigWithoutIntegrationIds() throws SQLException {
        var integration1 = Integration.builder()
                .name("nameJira1")
                .application("jira")
                .description("description")
                .authentication(Authentication.API_KEY)
                .metadata(Map.of())
                .satellite(false)
                .tags(List.of())
                .status("ok")
                .build();
        var integrationId1 = Integer.parseInt(integrationService.insert(company, integration1));

        var orgUser1 = DBOrgUser.builder()
                .email("email1a")
                .fullName("fullName1")
                .customFields(Map.of("test_name", "test1"))
                .ids(Set.of(LoginId.builder().cloudId("cloudId").username("cloudId").integrationType(integration1.getApplication()).integrationId(integrationId1).build()))
                .versions(Set.of(1))
                .build();
        var userId1 = usersService.upsert(company, orgUser1);

        var orgUser2 = DBOrgUser.builder()
                .email("email2a")
                .fullName("fullName2")
                .customFields(Map.of("sample_name", "sample"))
                .ids(Set.of(LoginId.builder().cloudId("cloudId2").integrationId(integrationId1).build()))
                .versions(Set.of(2, 3))
                .build();
        var userId2 = usersService.upsert(company, orgUser2);

        var manager1 = OrgUserId.builder().id(userId1.getId()).refId(userId1.getRefId()).fullName(orgUser1.getFullName()).email(orgUser1.getEmail()).build();
        var manager2 = OrgUserId.builder().id(userId2.getId()).refId(userId2.getRefId()).fullName(orgUser2.getFullName()).email(orgUser2.getEmail()).build();
        var managers = Set.of(
                manager1,
                manager2
        );
        Stream<DBOrgUnit> units = List.<DBOrgUnit>of(
                DBOrgUnit.builder()
                        .name("unit1a")
                        .description("My unit1a")
                        .active(false)
                        .managers(managers)
                        .sections(Set.of(DBOrgContentSection.builder()
                                .integrationId(integrationId1)
                                .integrationFilters(Map.of(
                                        "projects", Set.of("FE"),
                                        "status", Set.of("ok"),
                                        "custom_fields", Map.of(
                                                "custom_field01", "value1"),
                                        "partial", Map.of(
                                                "product", "level",
                                                "custom_fields", Map.of(
                                                        "p_custom_filed_1000", "value"))
                                ))
                                .users(Set.of(1, 2))
                                .build()))
                        .build()).stream();
        var ids = unitsHelper.insertNewOrgUnits(company, units);

        Optional<DBOrgUnit> dbOrgUnit = unitsService.get(company, ids.iterator().next(), false);
        var ou = dbOrgUnit.orElse(null);
        if(ou != null) {
            var config = unitsHelper.getOuConfiguration(company, Set.of(IntegrationType.JIRA), ou.getId()).get();
            Assertions.assertThat(config.getIntegrationIds()).containsExactlyInAnyOrder(integrationId1);
        }

    }
}
