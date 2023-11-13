package io.levelops.commons.tenant_management.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.tenant_management.models.TaskStatus;
import io.levelops.commons.tenant_management.models.TaskTracker;
import io.levelops.commons.tenant_management.models.TaskType;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class TaskTrackersDBServiceTest  {
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    JdbcTemplate template;
    TaskTrackersDBService taskTrackersDatabaseService;

    @Before
    public void setUp() throws Exception {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();

        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(DatabaseService.SchemaType.FS_CONTROLLER_SCHEMA.getSchemaName());

        taskTrackersDatabaseService = new TaskTrackersDBService(dataSource, DefaultObjectMapper.get());
        template = new JdbcTemplate(dataSource);
        taskTrackersDatabaseService.ensureTableExistence(null);
    }

    @Test
    public void test() throws SQLException {
        TaskTracker globalTracker1 = TaskTracker.builder()
                .type(TaskType.TENANT_CONFIG_SYNC).frequency(90)
                .status(TaskStatus.UNASSIGNED).statusChangedAt(Instant.now())
                .build();
        UUID id1 = UUID.fromString(taskTrackersDatabaseService.insert(null, globalTracker1));
        assertThat(id1).isNotEqualTo(null);
        TaskTracker globalTracker1Read = taskTrackersDatabaseService.get(null, id1.toString()).orElse(null);
        assertThat(globalTracker1Read).isNotEqualTo(null);
        TaskTracker globalTracker1Updated = TaskTracker.builder().id(id1).frequency(30).status(TaskStatus.PENDING).build();
        taskTrackersDatabaseService.update(null, globalTracker1Updated);
        TaskTracker globalTracker1UpdatedRead = taskTrackersDatabaseService.get(null, id1.toString()).orElse(null);
        assertThat(globalTracker1UpdatedRead.getFrequency()).isEqualTo(30);
        var globalTracker2 = TaskTracker.builder()
                .type(TaskType.TENANT_INDEX_TYPE_CONFIG_SYNC).frequency(40)
                .status(TaskStatus.UNASSIGNED).statusChangedAt(Instant.now())
                .build();
        UUID id2 = UUID.fromString(taskTrackersDatabaseService.insert(null,  globalTracker2));
        DbListResponse<TaskTracker> list = taskTrackersDatabaseService.list(null,  0, 10);
        assertThat(list.getRecords().size()).isEqualTo(2);
        taskTrackersDatabaseService.delete(null, id2.toString());
        DbListResponse<TaskTracker> list1 = taskTrackersDatabaseService.list(null, 0, 10);
        assertThat(list1.getRecords().size()).isEqualTo(1);

        //Test Flow

        //Test insert safe
        TaskTracker gt3 = TaskTracker.builder()
                .type(TaskType.TENANT_INDEX_SNAPSHOT_SYNC).frequency(60).status(TaskStatus.UNASSIGNED)
                .build();
        UUID id3 = taskTrackersDatabaseService.insertSafe(gt3).orElseThrow();
        Assert.assertNotNull(id3);
        gt3 = gt3.toBuilder().id(id3).build();
        TaskTracker gt3Actual = taskTrackersDatabaseService.get(null, id3.toString()).get();
        verifyRecord(gt3, gt3Actual);
        assertThat(taskTrackersDatabaseService.list(null, 0, 10).getRecords().size()).isEqualTo(2);

        TaskTracker gt3Illegal =  gt3.toBuilder().status(TaskStatus.PENDING).build();
        Optional<UUID> opt = taskTrackersDatabaseService.insertSafe(gt3Illegal);
        Assert.assertTrue(opt.isEmpty());
        assertThat(taskTrackersDatabaseService.list(null, 0, 10).getRecords().size()).isEqualTo(2);


        //Test insert safe
        TaskTracker gt4 = TaskTracker.builder()
                .type(TaskType.TENANT_INDEX_SNAPSHOT_SCHEDULE).frequency(90).status(TaskStatus.UNASSIGNED)
                .build();
        UUID id4 = taskTrackersDatabaseService.insertSafe(gt4).orElseThrow();
        Assert.assertNotNull(id4);
        gt4 = gt4.toBuilder().id(id4).build();
        TaskTracker gt4Actual = taskTrackersDatabaseService.get(null, id4.toString()).get();
        verifyRecord(gt4, gt4Actual);
        assertThat(taskTrackersDatabaseService.list(null, 0, 10).getRecords().size()).isEqualTo(3);

        TaskTracker gt4Illegal =  gt4.toBuilder().status(TaskStatus.PENDING).build();
        opt = taskTrackersDatabaseService.insertSafe(gt4Illegal);
        Assert.assertTrue(opt.isEmpty());
        assertThat(taskTrackersDatabaseService.list(null, 0, 10).getRecords().size()).isEqualTo(3);

        //Test Update frequency
        gt3 = gt3.toBuilder().frequency(65).build();
        Assert.assertTrue(taskTrackersDatabaseService.updateFrequencyByType( gt3.getType(), gt3.getFrequency()));
        gt3Actual = taskTrackersDatabaseService.get(null, id3.toString()).get();
        verifyRecord(gt3, gt3Actual);
        assertThat(taskTrackersDatabaseService.list(null, 0, 10).getRecords().size()).isEqualTo(3);

        //Test Update Status by Type & Time In Secs
        Assert.assertFalse(taskTrackersDatabaseService.updateStatusByTypeAndTimeInSeconds(gt3.getType(), 1000, TaskStatus.PENDING));
        gt3Actual = taskTrackersDatabaseService.get(null, id3.toString()).get();
        verifyRecord(gt3, gt3Actual);
        assertThat(taskTrackersDatabaseService.list(null, 0, 10).getRecords().size()).isEqualTo(3);

        gt3 = gt3.toBuilder().status(TaskStatus.PENDING).build();
        Assert.assertTrue(taskTrackersDatabaseService.updateStatusByTypeAndTimeInSeconds(gt3.getType(), 0, TaskStatus.PENDING));
        gt3Actual = taskTrackersDatabaseService.get(null, id3.toString()).get();
        verifyRecord(gt3, gt3Actual);
        assertThat(taskTrackersDatabaseService.list(null, 0, 10).getRecords().size()).isEqualTo(3);

        //Test Update Status by Type
        gt3 = gt3.toBuilder().status(TaskStatus.SUCCESS).build();
        Assert.assertTrue(taskTrackersDatabaseService.updateStatusByType(gt3.getType(), TaskStatus.SUCCESS));
        gt3Actual = taskTrackersDatabaseService.get(null, id3.toString()).get();
        verifyRecord(gt3, gt3Actual);
        assertThat(taskTrackersDatabaseService.list(null, 0, 10).getRecords().size()).isEqualTo(3);

        gt3 = gt3.toBuilder().status(TaskStatus.PENDING).build();
        Assert.assertTrue(taskTrackersDatabaseService.updateStatusByType(gt3.getType(), TaskStatus.PENDING));
        gt3Actual = taskTrackersDatabaseService.get(null, id3.toString()).get();
        verifyRecord(gt3, gt3Actual);
        assertThat(taskTrackersDatabaseService.list(null, 0, 10).getRecords().size()).isEqualTo(3);

        //Test Update Status by Type & Status
        gt3 = gt3.toBuilder().status(TaskStatus.FAILURE).build();
        Assert.assertTrue(taskTrackersDatabaseService.updateStatusByTypeAndStatus(gt3.getType(),  TaskStatus.PENDING,TaskStatus.FAILURE));
        gt3Actual = taskTrackersDatabaseService.get(null, id3.toString()).get();
        verifyRecord(gt3, gt3Actual);
        assertThat(taskTrackersDatabaseService.list(null, 0, 10).getRecords().size()).isEqualTo(3);
    }

    private void verifyRecord(TaskTracker expected, TaskTracker actual) {
        Assert.assertEquals(expected.getId() , actual.getId());
        Assert.assertEquals(expected.getType() , actual.getType());
        Assert.assertEquals(expected.getFrequency().intValue() , actual.getFrequency().intValue());
        Assert.assertEquals(expected.getStatus() , actual.getStatus());
        Assert.assertNotNull(actual.getStatusChangedAt());
        Assert.assertNotNull(actual.getCreatedAt());
        Assert.assertNotNull(actual.getUpdatedAt());
    }

}