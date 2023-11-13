package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.dev_productivity.UserDevProductivityReport;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.propelo.commons.generic_events.models.Component;
import io.propelo.commons.generic_events.models.GenericEventRequest;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class GenericEventDatabaseServiceTest {
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    String company = "test";
    JdbcTemplate template;
    private static GenericEventDatabaseService genericEventDatabaseService;

    @Before
    public void setUp() throws Exception {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS test CASCADE; ",
                "CREATE SCHEMA test; "
        ).forEach(template::execute);
        System.out.println(template.queryForObject("SELECT current_database();", String.class));

        genericEventDatabaseService = new GenericEventDatabaseService(dataSource);
        genericEventDatabaseService.ensureTableExistence(company);
    }

    private void verifyRecords(List<GenericEventRequest> a, List<GenericEventRequest> e) {
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if (CollectionUtils.isEmpty(a)) {
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<UUID, GenericEventRequest> actualMap = a.stream().collect(Collectors.toMap(GenericEventRequest::getId, x -> x));
        Map<UUID, GenericEventRequest> expectedMap = e.stream().collect(Collectors.toMap(GenericEventRequest::getId, x -> x));

        for (UUID key : actualMap.keySet()) {
            verifyRecord(actualMap.get(key), expectedMap.get(key));
        }
    }
    private void verifyRecord(GenericEventRequest expected, GenericEventRequest actual) {
        Assert.assertEquals(expected.getComponent(), actual.getComponent());
        Assert.assertEquals(expected.getKey(), actual.getKey());
        Assert.assertEquals(expected.getSecondaryKey(), actual.getSecondaryKey());
        Assert.assertEquals(expected.getEventType(), actual.getEventType());
        Assert.assertEquals(expected.getEventTime(), actual.getEventTime());
        Assert.assertNotNull(actual.getCreatedAt());
        Assert.assertNotNull(actual.getUpdatedAt());
    }

    private GenericEventRequest testInsert(GenericEventRequest expected) throws SQLException {
        String id = genericEventDatabaseService.insert(company, expected);
        Assert.assertNotNull(id);
        expected = expected.toBuilder().id(UUID.fromString(id)).build();

        Optional<GenericEventRequest> opt = genericEventDatabaseService.get(company, id);
        Assert.assertTrue(opt.isPresent());
        verifyRecord(expected, opt.get());

        return expected;
    }

    private void testList(List<GenericEventRequest> expected) throws SQLException {
        DbListResponse<GenericEventRequest> dbListResponse = genericEventDatabaseService.list(company, 0, 1000);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(expected.size(), dbListResponse.getTotalCount().intValue());
        verifyRecords(expected, dbListResponse.getRecords());
    }


    @Test
    public void test() throws SQLException {
        Instant startTime = Instant.now().minus(90, ChronoUnit.DAYS);

        List<GenericEventRequest> expected = new ArrayList<>();

        GenericEventRequest expected1 = GenericEventRequest.builder()
                .component(Component.JIRA).key("LEV-123").eventType("INCIDENT_OCCURED").eventTime(startTime.getEpochSecond())
                .build();
        expected1 = testInsert(expected1);
        expected.add(expected1);
        testList(expected);

        GenericEventRequest expected2 = GenericEventRequest.builder()
                .component(Component.SCM_PR).key("43").eventType("INCIDENT_OCCURED").eventTime(startTime.plus(1, ChronoUnit.DAYS).getEpochSecond())
                .build();
        expected2 = testInsert(expected2);
        expected.add(expected2);
        testList(expected);

        GenericEventRequest expected3 = GenericEventRequest.builder()
                .component(Component.SCM_COMMIT).key("cnbdjkcbdbdjkcjkdcj").eventType("INCIDENT_OCCURED").eventTime(startTime.plus(2, ChronoUnit.DAYS).getEpochSecond())
                .build();
        expected3 = testInsert(expected3);
        expected.add(expected3);
        testList(expected);

        GenericEventRequest expected4 = GenericEventRequest.builder()
                .component(Component.CICD_JOB_RUN).key("113").eventType("INCIDENT_OCCURED").eventTime(startTime.plus(3, ChronoUnit.DAYS).getEpochSecond())
                .build();
        expected4 = testInsert(expected4);
        expected.add(expected4);
        testList(expected);

        GenericEventRequest expected5 = GenericEventRequest.builder()
                .component(Component.WORK_ITEM).key("LEV-123").eventType("INCIDENT_OCCURED").eventTime(startTime.plus(4, ChronoUnit.DAYS).getEpochSecond())
                .build();
        expected5 = testInsert(expected5);
        expected.add(expected5);
        testList(expected);
    }
}