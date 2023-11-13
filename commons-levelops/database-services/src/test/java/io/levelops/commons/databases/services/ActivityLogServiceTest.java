package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.ActivityLog;
import io.levelops.commons.databases.models.filters.ActivityLogsFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class ActivityLogServiceTest {

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private ActivityLogService activityLogService;
    private IntegrationService integrationService;
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    private static final String COMPANY = "test";

    @Before
    public void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();

        activityLogService = new ActivityLogService(dataSource, MAPPER);
        JdbcTemplate template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS test CASCADE; ",
                "CREATE SCHEMA test; "
        ).forEach(template::execute);

        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(COMPANY);
        activityLogService.ensureTableExistence(COMPANY);

        ActivityLog activityLog1 = ActivityLog.builder()
                .targetItem("0c6efc2c-c375-42f6-861d-46a7faaf6a75")
                .email("abc@yahoo.com")
                .targetItemType(ActivityLog.TargetItemType.API_ACCESS_KEY)
                .body("Password User Login: 1.")
                .details(Collections.emptyMap())
                .action(ActivityLog.Action.CREATED)
                .build();
        ActivityLog activityLog2 = ActivityLog.builder()
                .targetItem("3a6c20ae-393c-4aee-aefa-8d7aad9f827f")
                .email("def@gmail.dev")
                .targetItemType(ActivityLog.TargetItemType.API_ACCESS_KEY)
                .body("Password User Deleted: 2.")
                .details(Collections.emptyMap())
                .action(ActivityLog.Action.DELETED)
                .build();
        ActivityLog activityLog3 = ActivityLog.builder()
                .targetItem("56139a73-3446-40db-a011-6fa293924b08")
                .email("ghi@gmail.com")
                .targetItemType(ActivityLog.TargetItemType.API_ACCESS_KEY)
                .body("Password User Success: 3.")
                .details(Collections.emptyMap())
                .action(ActivityLog.Action.SUCCESS)
                .build();

        activityLogService.insert(COMPANY, activityLog1);
        activityLogService.insert(COMPANY, activityLog2);
        activityLogService.insert(COMPANY, activityLog3);
    }


    @Test
    public void test() throws SQLException {

        List<ActivityLog> listActivityLog1 = activityLogService.list(COMPANY, 0, 100).getRecords();
        int sizeOfActivityLog1 = activityLogService.list(COMPANY, 0, 2).getCount();
        int totalCountOfActivityLog1 = activityLogService.list(COMPANY, 0, 100).getTotalCount();
        assertThat(listActivityLog1.get(0).getTargetItemType().toString()).isEqualTo("API_ACCESS_KEY");
        assertThat(sizeOfActivityLog1).isEqualTo(2);
        assertThat(totalCountOfActivityLog1).isEqualTo(3);

        List<String> emails = List.of("abc@yahoo.com");
        List<ActivityLog> listActivityLog2 = activityLogService.list(COMPANY, List.of(), emails, List.of(), null, 0, 100).getRecords();
        int sizeOfActivityLog2 = activityLogService.list(COMPANY, List.of(), emails, List.of(), null, 0, 1).getCount();
        int totalCountOfActivityLog2 = activityLogService.list(COMPANY, List.of(), emails, List.of(), null, 0, 100).getTotalCount();
        assertThat(listActivityLog2.get(0).getEmail()).isEqualTo("abc@yahoo.com");
        assertThat(sizeOfActivityLog2).isEqualTo(1);
        assertThat(totalCountOfActivityLog2).isEqualTo(1);

        List<String> actions = List.of("CREATED", "SUCCESS");
        List<ActivityLog> listActivityLog3 = activityLogService.list(COMPANY, List.of(), List.of(), actions, null, 0, 100).getRecords();
        int sizeOfActivityLog3 = activityLogService.list(COMPANY, List.of(), List.of(), actions, null, 0, 1).getCount();
        int totalCountOfActivityLog3 = activityLogService.list(COMPANY, List.of(), List.of(), actions, null, 0, 100).getTotalCount();
        assertThat(listActivityLog3.get(0).getAction().toString()).isEqualTo("CREATED");
        assertThat(sizeOfActivityLog3).isEqualTo(1);
        assertThat(totalCountOfActivityLog3).isEqualTo(2);

        List<ActivityLog> listActivityLog4 = activityLogService.list(COMPANY, List.of(), List.of(), List.of(),
                Map.of("email", Map.of("$begins", "ghi")), 0, 100).getRecords();
        assertThat(listActivityLog4.get(0).getAction().toString()).isEqualTo("SUCCESS");
        assertThat(listActivityLog4.size()).isEqualTo(1);

        List<ActivityLog> listActivityLog5 = activityLogService.list(COMPANY, List.of(), List.of(), List.of(),
                Map.of("email", Map.of("$ends", "dev")), 0, 100).getRecords();
        assertThat(listActivityLog5.get(0).getAction().toString()).isEqualTo("DELETED");
        assertThat(listActivityLog5.size()).isEqualTo(1);

        List<ActivityLog> listActivityLog6 = activityLogService.list(COMPANY, List.of(), List.of(), List.of(),
                Map.of("email", Map.of("$contains", "yahoo")), 0, 100).getRecords();
        assertThat(listActivityLog6.get(0).getAction().toString()).isEqualTo("CREATED");
        assertThat(listActivityLog6.size()).isEqualTo(1);

        ActivityLogsFilter activityLogsFilter1 = ActivityLogsFilter.builder().across(ActivityLogsFilter.DISTINCT.email).build();
        List<DbAggregationResult> listActivityLog7 = activityLogService.groupByAndCalculate(COMPANY, activityLogsFilter1).getRecords();
        assertThat(listActivityLog7.get(0).getKey()).isEqualTo("ghi@gmail.com");
        assertThat(listActivityLog7.get(0).getCount()).isEqualTo(1);
        assertThat(listActivityLog7.size()).isEqualTo(3);

        ActivityLogsFilter activityLogsFilter2 = ActivityLogsFilter.builder().across(ActivityLogsFilter.DISTINCT.targetitem).build();
        List<DbAggregationResult> listActivityLog8 = activityLogService.groupByAndCalculate(COMPANY, activityLogsFilter2).getRecords();
        assertThat(listActivityLog8.get(0).getKey()).isEqualTo("56139a73-3446-40db-a011-6fa293924b08");
        assertThat(listActivityLog8.get(0).getCount()).isEqualTo(1);
        assertThat(listActivityLog8.size()).isEqualTo(3);

        ActivityLogsFilter activityLogsFilter3 = ActivityLogsFilter.builder().emails(List.of("def@gmail.dev"))
                .across(ActivityLogsFilter.DISTINCT.email).build();
        List<DbAggregationResult> listActivityLog9 = activityLogService.groupByAndCalculate(COMPANY, activityLogsFilter3).getRecords();
        assertThat(listActivityLog9.get(0).getKey()).isEqualTo("def@gmail.dev");
        assertThat(listActivityLog9.get(0).getCount()).isEqualTo(1);
        assertThat(listActivityLog9.size()).isEqualTo(1);

        ActivityLogsFilter activityLogsFilter4 = ActivityLogsFilter.builder().emails(List.of("abc@gmail.dev"))
                .across(ActivityLogsFilter.DISTINCT.email).build();
        List<DbAggregationResult> listActivityLog10 = activityLogService.groupByAndCalculate(COMPANY, activityLogsFilter4).getRecords();
        assertThat(listActivityLog10).isEqualTo(List.of());
        assertThat(listActivityLog10.size()).isEqualTo(0);
    }

    private void setupForRecentLoginTest() throws SQLException {
        ActivityLog activityLog4 = ActivityLog.builder()
                .targetItem("56139a73-3446-40db-a011-6fa293924374")
                .email("sid1@propelo.ai")
                .targetItemType(ActivityLog.TargetItemType.USER_LOGIN)
                .body(String.format("%s User Login: %s.", "SSO", "56139a73-3446-40db-a011-6fa293924374"))
                .details(Collections.emptyMap())
                .action(ActivityLog.Action.SUCCESS)
                .build();

        ActivityLog activityLog5 = ActivityLog.builder()
                .targetItem("56139a73-3446-40db-a011-6fa293924375")
                .email("sid2@gmail.com")
                .targetItemType(ActivityLog.TargetItemType.USER_LOGIN)
                .body(String.format("%s User Login: %s.", "Password", "56139a73-3446-40db-a011-6fa293924375"))
                .details(Collections.emptyMap())
                .action(ActivityLog.Action.SUCCESS)
                .build();

        ActivityLog activityLog6 = ActivityLog.builder()
                .targetItem("UNKNOWN")
                .email("sid3@gmail.com")
                .targetItemType(ActivityLog.TargetItemType.USER_LOGIN)
                .body(String.format("%s User Login: %s.", "Password", "56139a73-3446-40db-a011-6fa293924376"))
                .details(Collections.emptyMap())
                .action(ActivityLog.Action.FAIL)
                .build();

        ActivityLog activityLog7 = ActivityLog.builder()
                .targetItem("56139a73-3446-40db-a011-6fa293924376")
                .email("sid4@super.com")
                .targetItemType(ActivityLog.TargetItemType.USER_LOGIN)
                .body(String.format("%s User Login: %s.", "Password", "56139a73-3446-40db-a011-6fa293924377"))
                .details(Collections.emptyMap())
                .action(ActivityLog.Action.SUCCESS)
                .build();

        activityLogService.insert(COMPANY, activityLog4);
        activityLogService.insert(COMPANY, activityLog5);
        activityLogService.insert(COMPANY, activityLog6);
        activityLogService.insert(COMPANY, activityLog7);
    }

    @Test
    public void testRecentLogins() throws SQLException {
        setupForRecentLoginTest();
        List<ActivityLog> results1 = activityLogService
                .listMostRecentLogins(COMPANY, List.of(ActivityLog.Action.SUCCESS), List.of("propelo")).getRecords();
        assertThat(results1.size()).isEqualTo(2);
        assertThat(results1.stream().map(ActivityLog::getEmail).collect(Collectors.toList()))
                .containsAll(List.of("sid2@gmail.com", "sid4@super.com"));

        List<ActivityLog> results2 = activityLogService
                .listMostRecentLogins(COMPANY, List.of(ActivityLog.Action.SUCCESS), List.of("rediff")).getRecords();
        assertThat(results2.size()).isEqualTo(3);
        assertThat(results2.stream().map(ActivityLog::getEmail).collect(Collectors.toList()))
                .containsAll(List.of("sid1@propelo.ai", "sid2@gmail.com", "sid4@super.com"));

        List<ActivityLog> results3 = activityLogService
                .listMostRecentLogins(COMPANY, List.of(ActivityLog.Action.SUCCESS), List.of()).getRecords();
        assertThat(results3.size()).isEqualTo(3);
        assertThat(results3.stream().map(ActivityLog::getEmail).collect(Collectors.toList()))
                .containsAll(List.of("sid1@propelo.ai", "sid2@gmail.com", "sid4@super.com"));

        List<ActivityLog> results4 = activityLogService
                .listMostRecentLogins(COMPANY, List.of(ActivityLog.Action.FAIL), List.of()).getRecords();
        assertThat(results4.size()).isEqualTo(1);
        assertThat(results4.get(0).getBody()).contains("56139a73-3446-40db-a011-6fa293924376");

        List<ActivityLog> results5 = activityLogService
                .listMostRecentLogins(COMPANY, List.of(ActivityLog.Action.SUCCESS), List.of("gmail", "super", "propelo")).getRecords();
        assertThat(results5.size()).isEqualTo(0);
    }
}