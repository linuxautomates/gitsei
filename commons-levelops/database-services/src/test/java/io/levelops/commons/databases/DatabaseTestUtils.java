package io.levelops.commons.databases;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraStatusMetadata;
import io.levelops.commons.databases.models.database.jira.parsers.JiraIssueParser;
import io.levelops.commons.databases.models.database.organization.OrgUnitCategory;
import io.levelops.commons.databases.models.database.organization.OrgVersion;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.JiraFieldService;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.databases.services.JiraIssueStoryPointsDatabaseService;
import io.levelops.commons.databases.services.JiraProjectService;
import io.levelops.commons.databases.services.JiraStatusMetadataDatabaseService;
import io.levelops.commons.databases.services.ProductService;
import io.levelops.commons.databases.services.TagItemDBService;
import io.levelops.commons.databases.services.TagsService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.databases.services.dev_productivity.DevProductivityProfileDatabaseService;
import io.levelops.commons.databases.services.jira.JiraIssueAggService;
import io.levelops.commons.databases.services.jira.JiraIssuePrioritySlaService;
import io.levelops.commons.databases.services.jira.JiraIssueReadService;
import io.levelops.commons.databases.services.jira.JiraIssueSprintService;
import io.levelops.commons.databases.services.jira.JiraIssueStatusService;
import io.levelops.commons.databases.services.jira.JiraIssueUserService;
import io.levelops.commons.databases.services.jira.JiraIssueVersionService;
import io.levelops.commons.databases.services.jira.JiraIssueWriteService;
import io.levelops.commons.databases.services.jira.conditions.JiraConditionsBuilder;
import io.levelops.commons.databases.services.jira.conditions.JiraCustomFieldConditionsBuilder;
import io.levelops.commons.databases.services.jira.conditions.JiraFieldConditionsBuilder;
import io.levelops.commons.databases.services.jira.conditions.JiraPartialMatchConditionsBuilder;
import io.levelops.commons.databases.services.jira.utils.JiraFilterParser;
import io.levelops.commons.databases.services.jira.utils.JiraIssueQueryBuilder;
import io.levelops.commons.databases.services.organization.OrgUnitCategoryDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.databases.services.velocity.OrgProfileDatabaseService;
import io.levelops.commons.databases.services.velocity.VelocityConfigsDatabaseService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.jira.models.JiraIssue;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.Value;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.levelops.commons.databases.models.database.jira.DbJiraIssue.UNASSIGNED;
import static io.levelops.commons.databases.models.database.jira.DbJiraIssue.UNKNOWN;
import static io.levelops.commons.databases.services.ScmQueryUtils.ARRAY_UNIQ;

public class DatabaseTestUtils {

    public static DataSource setUpDataSource(SingleInstancePostgresRule pg, String company) throws SQLException, IOException {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        JdbcTemplate template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS " + company + " CASCADE; ",
                "CREATE SCHEMA " + company + " ; ",
                ARRAY_UNIQ
        ).forEach(template::execute);
        return dataSource;
    }

    @Value
    public static class JiraTestDbs {
        IntegrationService integrationService;
        UserIdentityService userIdentityService;
        JiraFieldService jiraFieldService;
        JiraProjectService jiraProjectService;
        JiraIssueService jiraIssueService;
        JiraStatusMetadataDatabaseService jiraStatusMetadataDatabaseService;

        JiraCustomFieldConditionsBuilder jiraCustomFieldConditionsBuilder;
        JiraFieldConditionsBuilder jiraFieldConditionsBuilder;
        JiraPartialMatchConditionsBuilder jiraPartialMatchConditionsBuilder;
        JiraConditionsBuilder jiraConditionsBuilder;
        JiraIssueQueryBuilder jiraIssueQueryBuilder;
        JiraIssueStoryPointsDatabaseService jiraIssueStoryPointsDatabaseService;

        IntegrationTrackingService integrationTrackingService;
        TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;
        JiraFilterParser jiraFilterParser;
    }

    public static JiraTestDbs setUpJiraServices(DataSource dataSource, String company) throws SQLException {
        IntegrationService integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);

        UserIdentityService userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);

        JiraFieldService jiraFieldService = new JiraFieldService(dataSource);
        jiraFieldService.ensureTableExistence(company);

        JiraProjectService jiraProjectService = new JiraProjectService(dataSource);
        jiraProjectService.ensureTableExistence(company);

        JiraStatusMetadataDatabaseService jiraStatusMetadataDatabaseService = new JiraStatusMetadataDatabaseService(dataSource);
        jiraStatusMetadataDatabaseService.ensureTableExistence(company);

        JiraCustomFieldConditionsBuilder customFieldConditionsBuilder = new JiraCustomFieldConditionsBuilder(dataSource, jiraFieldService, integrationService);
        JiraFieldConditionsBuilder fieldConditionsBuilder = new JiraFieldConditionsBuilder(jiraFieldService);
        JiraPartialMatchConditionsBuilder partialMatchConditionsBuilder = new JiraPartialMatchConditionsBuilder(dataSource, fieldConditionsBuilder, customFieldConditionsBuilder);
        JiraConditionsBuilder jiraConditionsBuilder = new JiraConditionsBuilder(dataSource, fieldConditionsBuilder, customFieldConditionsBuilder, partialMatchConditionsBuilder, true);
        JiraIssueQueryBuilder jiraIssueQueryBuilder = new JiraIssueQueryBuilder(jiraConditionsBuilder);

        JiraIssueAggService aggService = new JiraIssueAggService(dataSource, jiraProjectService, customFieldConditionsBuilder, jiraConditionsBuilder, jiraIssueQueryBuilder, jiraStatusMetadataDatabaseService, 0);
        JiraIssueSprintService sprintService = new JiraIssueSprintService(dataSource);
        JiraIssueUserService userService = new JiraIssueUserService(dataSource);
        JiraIssueVersionService versionService = new JiraIssueVersionService(dataSource);
        JiraIssuePrioritySlaService prioritySlaService = new JiraIssuePrioritySlaService(dataSource);
        JiraIssueStatusService statusService = new JiraIssueStatusService(dataSource, jiraConditionsBuilder);
        JiraIssueReadService readService = new JiraIssueReadService(dataSource, sprintService, statusService, jiraConditionsBuilder, jiraIssueQueryBuilder, jiraStatusMetadataDatabaseService);
        JiraIssueWriteService writeService = new JiraIssueWriteService(dataSource, DefaultObjectMapper.get(), aggService, readService);

        JiraIssueService jiraIssueService = new JiraIssueService(dataSource, writeService, readService, aggService, userService, sprintService, versionService, prioritySlaService, statusService);
        jiraIssueService.ensureTableExistence(company);
        JiraIssueStoryPointsDatabaseService jiraIssueStoryPointsDatabaseService = new JiraIssueStoryPointsDatabaseService(dataSource);

        IntegrationTrackingService integrationTrackingService = new IntegrationTrackingService(dataSource);
        integrationTrackingService.ensureTableExistence(company);
        TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService = new TicketCategorizationSchemeDatabaseService(dataSource, DefaultObjectMapper.get());
        ticketCategorizationSchemeDatabaseService.ensureTableExistence(company);
        JiraFilterParser jiraFilterParser = new JiraFilterParser(fieldConditionsBuilder, integrationService, integrationTrackingService, ticketCategorizationSchemeDatabaseService);

        return new JiraTestDbs(integrationService, userIdentityService, jiraFieldService, jiraProjectService, jiraIssueService, jiraStatusMetadataDatabaseService,
                customFieldConditionsBuilder, fieldConditionsBuilder, partialMatchConditionsBuilder, jiraConditionsBuilder, jiraIssueQueryBuilder, jiraIssueStoryPointsDatabaseService,
                integrationTrackingService, ticketCategorizationSchemeDatabaseService, jiraFilterParser);
    }

    public static DbJiraIssue populateDbJiraIssueUserIds(DataSource dataSource, String company, String integrationId, JiraIssue jiraIssue, DbJiraIssue dbJiraIssue) throws SQLException {
        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, company);
        UserIdentityService userIdentityService = jiraTestDbs.getUserIdentityService();
        userIdentityService.ensureTableExistence(company);

        String reporterId = null;
        String assigneeId = null;
        String firstAssigneeId = null;
        if (StringUtils.isNotEmpty(dbJiraIssue.getReporter())) {
            String reporter = UNKNOWN;
            if (jiraIssue.getFields().getReporter() != null) {
                reporter = ObjectUtils.firstNonNull(jiraIssue.getFields().getReporter().getAccountId(),
                        jiraIssue.getFields().getReporter().getName(), reporter);
            }
            reporterId = userIdentityService.getUser(company, integrationId, reporter);
        }
        if (StringUtils.isNotEmpty(dbJiraIssue.getAssignee())) {
            String assignee = UNASSIGNED;
            if (jiraIssue.getFields().getAssignee() != null) {
                assignee = ObjectUtils.firstNonNull(jiraIssue.getFields().getAssignee().getAccountId(),
                        jiraIssue.getFields().getAssignee().getName(), assignee);
            }
            assigneeId = userIdentityService.getUser(company, integrationId, assignee);
        }
        if (StringUtils.isNotEmpty(dbJiraIssue.getFirstAssignee())) {
            Optional<String> firstAssignee = userIdentityService.getUserByDisplayName(company, integrationId,
                    dbJiraIssue.getFirstAssignee());
            if (firstAssignee.isPresent()) {
                firstAssigneeId = firstAssignee.get();
            }
        }
        return dbJiraIssue.toBuilder()
                .reporterId(reporterId)
                .assigneeId(assigneeId)
                .firstAssigneeId(firstAssigneeId)
                .build();
    }

    public static String createIntegrationId(IntegrationService integrationService, String company, String app) throws SQLException {
        return integrationService.insert(company, Integration.builder()
                .application(app)
                .name(app + UUID.randomUUID())
                .status("enabled")
                .build());
    }

    public static void insertJiraIssues(JiraIssueService jiraIssueService, String resourcePath, String company,
                                        String integrationId,
                                        Date fetchTime,
                                        String epicLinkField,
                                        String storyPointsField,
                                        String sprintField,
                                        String sprintFieldName) throws IOException, SQLException {
        PaginatedResponse<JiraIssue> issues = ResourceUtils.getResourceAsObject(resourcePath, DefaultObjectMapper.get().getTypeFactory()
                .constructParametricType(PaginatedResponse.class, JiraIssue.class));
        for (JiraIssue issue : issues.getResponse().getRecords()) {
            JiraIssueParser.JiraParserConfig parserConfig = JiraIssueParser.JiraParserConfig.builder()
                    .epicLinkField(epicLinkField)
                    .storyPointsField(storyPointsField)
                    .sprintFieldName(sprintFieldName)
                    .sprintFieldKey(sprintField)
                    .build();
            jiraIssueService.insert(company, JiraIssueParser.parseJiraIssue(issue, integrationId, fetchTime, parserConfig));
        }
    }

    public static void setupStatusMetadata(String company, String integrationId, JiraStatusMetadataDatabaseService jiraStatusMetadataDatabaseService) throws IOException, SQLException {
        for (int i = 0; i < 5; i++) {
            DbJiraStatusMetadata statusMetadata = DbJiraStatusMetadata.builder()
                    .integrationId(integrationId)
                    .statusId("status-" + i)
                    .statusCategory("status-category-" + i)
                    .status("status-" + i)
                    .build();
            jiraStatusMetadataDatabaseService.insert(company, statusMetadata);
        }
    }

    @Value
    public static class OuTestDbs {
        OrgUnitsDatabaseService orgUnitsDatabaseService;
        OrgUnitHelper orgUnitHelper;

        OrgUnitCategory orgUnitCategory;
        UUID firstVersion;

        IntegrationService integrationService;
        UserIdentityService userIdentityService;
        OrgVersionsDatabaseService orgVersionsDatabaseService;
        OrgUsersDatabaseService orgUsersDatabaseService;
        UserService userService;
        DashboardWidgetService dashboardWidgetService;
        ProductService productService;
        TagsService tagsService;
        TagItemDBService tagItemDBService;
        OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService;
        OrgProfileDatabaseService orgProfileDatabaseService;
        CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
        CiCdJobsDatabaseService ciCdJobsDatabaseService;
        VelocityConfigsDatabaseService velocityConfigsDatabaseService;
        DevProductivityProfileDatabaseService devProductivityProfileDatabaseService;
    }

    public static OuTestDbs setUpOuTestDbs(DataSource dataSource, String company) throws SQLException {
        return setUpOuTestDbs(dataSource, company, true, true);
    }

    public static OuTestDbs setUpOuTestDbs(DataSource dataSource, String company, boolean bootstrapVersion, boolean bootstrapCategory) throws SQLException {
        ObjectMapper mapper = DefaultObjectMapper.get();

        IntegrationService integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);

        UserIdentityService userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);

        OrgVersionsDatabaseService orgVersionsDatabaseService = new OrgVersionsDatabaseService(dataSource);
        orgVersionsDatabaseService.ensureTableExistence(company);

        OrgUsersDatabaseService orgUsersDatabaseService = new OrgUsersDatabaseService(dataSource, mapper, orgVersionsDatabaseService, userIdentityService);
        orgUsersDatabaseService.ensureTableExistence(company);

        UserService userService = new UserService(dataSource, mapper);
        userService.ensureTableExistence(company);

        DashboardWidgetService dashboardWidgetService = new DashboardWidgetService(dataSource, mapper);
        dashboardWidgetService.ensureTableExistence(company);

        ProductService productService = new ProductService(dataSource);
        productService.ensureTableExistence(company);

        TagsService tagsService = new TagsService(dataSource);
        tagsService.ensureTableExistence(company);
        TagItemDBService tagItemDBService = new TagItemDBService(dataSource);
        tagItemDBService.ensureTableExistence(company);

        OrgProfileDatabaseService orgProfileDatabaseService = new OrgProfileDatabaseService(dataSource, mapper);
        orgProfileDatabaseService.ensureTableExistence(company);

        CiCdInstancesDatabaseService ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        CiCdJobsDatabaseService ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        VelocityConfigsDatabaseService velocityConfigsDatabaseService = new VelocityConfigsDatabaseService(dataSource, mapper, orgProfileDatabaseService);
        velocityConfigsDatabaseService.ensureTableExistence(company);
        DevProductivityProfileDatabaseService devProductivityProfileDatabaseService = new DevProductivityProfileDatabaseService(dataSource, mapper);
        devProductivityProfileDatabaseService.ensureTableExistence(company);

        OrgUnitsDatabaseService orgUnitsDatabaseService = new OrgUnitsDatabaseService(dataSource, mapper, tagItemDBService, orgUsersDatabaseService, orgVersionsDatabaseService, dashboardWidgetService);
        OrgUnitHelper orgUnitHelper = new OrgUnitHelper(orgUnitsDatabaseService, integrationService);

        OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService = new OrgUnitCategoryDatabaseService(dataSource, orgUnitHelper, mapper);
        orgUnitCategoryDatabaseService.ensureTableExistence(company);

        orgUnitsDatabaseService.ensureTableExistence(company);

        OrgUnitCategory orgUnitCategory = null;
        if (bootstrapCategory) {
            orgUnitCategory = OrgUnitCategory.builder()
                    .name("TEAM A")
                    .description("Sample team")
                    .isPredefined(true)
                    .build();
            String orgUnitCategoryId = orgUnitCategoryDatabaseService.insert(company, orgUnitCategory);
            orgUnitCategory = orgUnitCategory.toBuilder()
                    .id(UUID.fromString(orgUnitCategoryId))
                    .build();
        }

        UUID firstVersion = null;
        if (bootstrapVersion) {
            firstVersion = orgVersionsDatabaseService.insert(company, OrgVersion.OrgAssetType.USER);
            orgVersionsDatabaseService.update(company, firstVersion, true);
        }

        return new OuTestDbs(orgUnitsDatabaseService, orgUnitHelper, orgUnitCategory, firstVersion, integrationService, userIdentityService, orgVersionsDatabaseService, orgUsersDatabaseService, userService, dashboardWidgetService, productService, tagsService, tagItemDBService, orgUnitCategoryDatabaseService, orgProfileDatabaseService, ciCdInstancesDatabaseService, ciCdJobsDatabaseService, velocityConfigsDatabaseService, devProductivityProfileDatabaseService);
    }

}
