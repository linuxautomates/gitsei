package io.levelops.commons.databases.services.dev_productivity;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.dev_productivity.GlobalTracker;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class GlobalTrackerDatabaseServiceTest {
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    String company = "test";
    JdbcTemplate template;
    GlobalTrackersDatabaseService globalTrackersDatabaseService;

    @Before
    public void setUp() throws Exception {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        globalTrackersDatabaseService = new GlobalTrackersDatabaseService(dataSource, DefaultObjectMapper.get());
        template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS test CASCADE; ",
                "CREATE SCHEMA test; "
        ).forEach(template::execute);
        System.out.println(template.queryForObject("SELECT current_database();", String.class));
        globalTrackersDatabaseService.ensureTableExistence(company);
    }

    @Test
    public void test() throws SQLException {
        GlobalTracker globalTracker1 = GlobalTracker.builder()
                .type("type1").frequency(90)
                .status("started").statusChangedAt(Instant.now())
                .build();
        UUID id1 = UUID.fromString(globalTrackersDatabaseService.insert(company, globalTracker1));
        assertThat(id1).isNotEqualTo(null);
        GlobalTracker globalTracker1Read = globalTrackersDatabaseService.get(company, id1.toString()).orElse(null);
        assertThat(globalTracker1Read).isNotEqualTo(null);
        GlobalTracker globalTracker1Updated = GlobalTracker.builder().id(id1).frequency(30).status("inprogress").build();
        globalTrackersDatabaseService.update(company, globalTracker1Updated);
        GlobalTracker globalTracker1UpdatedRead = globalTrackersDatabaseService.get(company, id1.toString()).orElse(null);
        assertThat(globalTracker1UpdatedRead.getFrequency()).isEqualTo(30);
        var globalTracker2 = GlobalTracker.builder()
                .type("type2").frequency(40)
                .status("started").statusChangedAt(Instant.now())
                .build();
        UUID id2 = UUID.fromString(globalTrackersDatabaseService.insert(company, globalTracker2));
        DbListResponse<GlobalTracker> list = globalTrackersDatabaseService.list(company, 0, 10);
        assertThat(list.getRecords().size()).isEqualTo(2);
        globalTrackersDatabaseService.delete(company, id2.toString());
        DbListResponse<GlobalTracker> list1 = globalTrackersDatabaseService.list(company, 0, 10);
        assertThat(list1.getRecords().size()).isEqualTo(1);

        //Test Flow

        //Test insert safe
        GlobalTracker gt3 = GlobalTracker.builder()
                .type("user_dev_productivity_reports").frequency(60).status("UNASSIGNED")
                .build();
        UUID id3 = globalTrackersDatabaseService.insertSafe(company, gt3).orElseThrow();
        Assert.assertNotNull(id3);
        gt3 = gt3.toBuilder().id(id3).build();
        GlobalTracker gt3Actual = globalTrackersDatabaseService.get(company, id3.toString()).get();
        verifyRecord(gt3, gt3Actual);
        assertThat(globalTrackersDatabaseService.list(company, 0, 10).getRecords().size()).isEqualTo(2);

        GlobalTracker gt3Illegal =  gt3.toBuilder().status("updated").build();
        Optional<UUID> opt = globalTrackersDatabaseService.insertSafe(company, gt3Illegal);
        Assert.assertTrue(opt.isEmpty());
        assertThat(globalTrackersDatabaseService.list(company, 0, 10).getRecords().size()).isEqualTo(2);


        //Test insert safe
        GlobalTracker gt4 = GlobalTracker.builder()
                .type("org_dev_productivity_reports_ab_test").frequency(90).status("UNASSIGNED")
                .build();
        UUID id4 = globalTrackersDatabaseService.insertSafe(company, gt4).orElseThrow();
        Assert.assertNotNull(id4);
        gt4 = gt4.toBuilder().id(id4).build();
        GlobalTracker gt4Actual = globalTrackersDatabaseService.get(company, id4.toString()).get();
        verifyRecord(gt4, gt4Actual);
        assertThat(globalTrackersDatabaseService.list(company, 0, 10).getRecords().size()).isEqualTo(3);

        GlobalTracker gt4Illegal =  gt4.toBuilder().status("updated").build();
        opt = globalTrackersDatabaseService.insertSafe(company, gt4Illegal);
        Assert.assertTrue(opt.isEmpty());
        assertThat(globalTrackersDatabaseService.list(company, 0, 10).getRecords().size()).isEqualTo(3);

        //Test Update frequency
        gt3 = gt3.toBuilder().frequency(65).build();
        Assert.assertTrue(globalTrackersDatabaseService.updateFrequencyByType(company, gt3.getType(), gt3.getFrequency()));
        gt3Actual = globalTrackersDatabaseService.get(company, id3.toString()).get();
        verifyRecord(gt3, gt3Actual);
        assertThat(globalTrackersDatabaseService.list(company, 0, 10).getRecords().size()).isEqualTo(3);

        //Test Update Status by Type & Time In Secs
        Assert.assertFalse(globalTrackersDatabaseService.updateStatusByTypeAndTimeInSeconds(company, gt3.getType(), 1000, "PENDING"));
        gt3Actual = globalTrackersDatabaseService.get(company, id3.toString()).get();
        verifyRecord(gt3, gt3Actual);
        assertThat(globalTrackersDatabaseService.list(company, 0, 10).getRecords().size()).isEqualTo(3);

        gt3 = gt3.toBuilder().status("PENDING").build();
        Assert.assertTrue(globalTrackersDatabaseService.updateStatusByTypeAndTimeInSeconds(company, gt3.getType(), 0, "PENDING"));
        gt3Actual = globalTrackersDatabaseService.get(company, id3.toString()).get();
        verifyRecord(gt3, gt3Actual);
        assertThat(globalTrackersDatabaseService.list(company, 0, 10).getRecords().size()).isEqualTo(3);

        //Test Update Status by Type
        gt3 = gt3.toBuilder().status("SUCCESS").build();
        Assert.assertTrue(globalTrackersDatabaseService.updateStatusByType(company, gt3.getType(), "SUCCESS"));
        gt3Actual = globalTrackersDatabaseService.get(company, id3.toString()).get();
        verifyRecord(gt3, gt3Actual);
        assertThat(globalTrackersDatabaseService.list(company, 0, 10).getRecords().size()).isEqualTo(3);

        gt3 = gt3.toBuilder().status("PENDING").build();
        Assert.assertTrue(globalTrackersDatabaseService.updateStatusByType(company, gt3.getType(), "PENDING"));
        gt3Actual = globalTrackersDatabaseService.get(company, id3.toString()).get();
        verifyRecord(gt3, gt3Actual);
        assertThat(globalTrackersDatabaseService.list(company, 0, 10).getRecords().size()).isEqualTo(3);

        //Test Update Status by Type & Status
        gt3 = gt3.toBuilder().status("FAILURE").build();
        Assert.assertTrue(globalTrackersDatabaseService.updateStatusByTypeAndStatus(company, gt3.getType(),  "PENDING","FAILURE"));
        gt3Actual = globalTrackersDatabaseService.get(company, id3.toString()).get();
        verifyRecord(gt3, gt3Actual);
        assertThat(globalTrackersDatabaseService.list(company, 0, 10).getRecords().size()).isEqualTo(3);
    }

    private void verifyRecord(GlobalTracker expected, GlobalTracker actual) {
        Assert.assertEquals(expected.getId() , actual.getId());
        Assert.assertEquals(expected.getType() , actual.getType());
        Assert.assertEquals(expected.getFrequency().intValue() , actual.getFrequency().intValue());
        Assert.assertEquals(expected.getStatus() , actual.getStatus());
        Assert.assertNotNull(actual.getStatusChangedAt());
        Assert.assertNotNull(actual.getCreatedAt());
        Assert.assertNotNull(actual.getUpdatedAt());
    }
}
