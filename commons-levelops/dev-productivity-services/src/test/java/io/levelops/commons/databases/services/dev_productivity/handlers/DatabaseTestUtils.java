package io.levelops.commons.databases.services.dev_productivity.handlers;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.parsers.JiraIssueParser;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.JiraFieldService;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.databases.services.JiraProjectService;
import io.levelops.commons.databases.services.JiraStatusMetadataDatabaseService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.services.UserIdentityService;
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

public class DatabaseTestUtils {
    public static DataSource setUpDataSource(SingleInstancePostgresRule pg, String company) throws SQLException, IOException {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        JdbcTemplate template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS " + company + " CASCADE; ",
                "CREATE SCHEMA " + company + " ; "
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
        TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;

        JiraCustomFieldConditionsBuilder jiraCustomFieldConditionsBuilder;
        JiraFieldConditionsBuilder jiraFieldConditionsBuilder;
        JiraPartialMatchConditionsBuilder jiraPartialMatchConditionsBuilder;
        JiraConditionsBuilder jiraConditionsBuilder;
        JiraIssueQueryBuilder jiraIssueQueryBuilder;
        JiraFilterParser jiraFilterParser;

    }

    public static JiraTestDbs setUpJiraServices(DataSource dataSource, String company) throws SQLException {
        IntegrationService integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);

        IntegrationTrackingService integrationTrackingService = new IntegrationTrackingService(dataSource);
        integrationTrackingService.ensureTableExistence(company);

        TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService = new TicketCategorizationSchemeDatabaseService(dataSource, DefaultObjectMapper.get());
        ticketCategorizationSchemeDatabaseService.ensureTableExistence(company);

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
        JiraFilterParser jiraFilterParser = new JiraFilterParser(fieldConditionsBuilder, integrationService, integrationTrackingService, ticketCategorizationSchemeDatabaseService);
        return new JiraTestDbs(integrationService, userIdentityService, jiraFieldService, jiraProjectService, jiraIssueService, ticketCategorizationSchemeDatabaseService, customFieldConditionsBuilder, fieldConditionsBuilder, partialMatchConditionsBuilder, jiraConditionsBuilder, jiraIssueQueryBuilder, jiraFilterParser);
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
            }//todo. name wsa null.
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
}
