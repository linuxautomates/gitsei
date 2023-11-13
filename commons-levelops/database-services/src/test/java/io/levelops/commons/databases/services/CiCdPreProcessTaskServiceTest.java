package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.CiCDPreProcessTask;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.assertj.core.api.Assertions;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class CiCdPreProcessTaskServiceTest {

    private final String company = "_levelops_cicd_pre_process";

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private DataSource dataSource;
    private CiCdPreProcessTaskService ciCdPreProcessTaskService;

    @Before
    public void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        ciCdPreProcessTaskService = new CiCdPreProcessTaskService(dataSource);
        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        ciCdPreProcessTaskService.ensureTableExistence(company);

    }

    @Test
    public void testCiCdPreProcessTask() throws SQLException {
        CiCDPreProcessTask ciCDPreProcessTask = CiCDPreProcessTask.builder().tenantId(company).status("SUCCESS").metaData("metaData").attemptCount(1).build();
        String taskId = ciCdPreProcessTaskService.insert(company, ciCDPreProcessTask);
        Optional<CiCDPreProcessTask> processTask = ciCdPreProcessTaskService.get(company, taskId);
        Assertions.assertThat(processTask.get().getId().toString()).isEqualTo(taskId);
        Assertions.assertThat(processTask.get().getTenantId()).isEqualTo(company);
        Assertions.assertThat(processTask.get().getStatus()).isEqualTo("SUCCESS");
        Assertions.assertThat(processTask.get().getAttemptCount()).isEqualTo(1);
        Assertions.assertThat(processTask.get().getMetaData()).isEqualTo("metaData");
        DbListResponse<CiCDPreProcessTask> ciCdPreProcessList = ciCdPreProcessTaskService.list(company, 0, 20);
        DbListResponse<CiCDPreProcessTask> ciCDPreProcessTaskDbListResponse = ciCdPreProcessTaskService.listByFilter(0, 10, null, company);
        Assertions.assertThat(ciCDPreProcessTaskDbListResponse.getRecords().get(0).getMetaData()).isEqualTo(ciCDPreProcessTask.getMetaData());
        Assertions.assertThat(ciCdPreProcessList.getRecords().size()).isEqualTo(1);
        CiCDPreProcessTask ciCDPreProcessTask1 = CiCDPreProcessTask.builder().tenantId(company).status("SUCCESS").metaData("metaData").attemptCount(1).build();
        CiCDPreProcessTask ciCDPreProcessTask2 = CiCDPreProcessTask.builder().tenantId(company).status("FAILURE").metaData("metaData").attemptCount(2).build();
        ciCdPreProcessTaskService.batchInsert(List.of(ciCDPreProcessTask1, ciCDPreProcessTask2));
        DbListResponse<CiCDPreProcessTask> ciCdPreProcessTaskList = ciCdPreProcessTaskService.list(company, 0, 20);
        Assertions.assertThat(ciCdPreProcessTaskList.getRecords().size()).isEqualTo(3);
        List<UUID> ids = ciCdPreProcessTaskList.getRecords().stream().map(CiCDPreProcessTask::getId).collect(Collectors.toList());
        DbListResponse<CiCDPreProcessTask> ciCdPreProcessTaskByFilterList = ciCdPreProcessTaskService.listByFilter(0, 20, ids, company);
        Assertions.assertThat(ciCdPreProcessTaskByFilterList.getRecords().size()).isGreaterThanOrEqualTo(2);
        ciCdPreProcessTaskService.updateStatus(company, "FAILURE", UUID.fromString(taskId));
        Optional<CiCDPreProcessTask> processTasks = ciCdPreProcessTaskService.get(company, taskId);
        Assertions.assertThat(processTasks.get().getStatus()).isEqualTo("FAILURE");
        Assertions.assertThat(processTasks.get().getStatusChangedAt()).isNotNull();
        ciCdPreProcessTaskService.updateAttemptsCount(company, 2, UUID.fromString(taskId));
        Optional<CiCDPreProcessTask> updatedProcessTask = ciCdPreProcessTaskService.get(company, taskId);
        Assertions.assertThat(updatedProcessTask.get().getAttemptCount()).isEqualTo(2);
        ciCdPreProcessTaskService.updateStatusAndAttemptCount(company,"PENDING", UUID.fromString(taskId));
        Optional<CiCDPreProcessTask> finalTask = ciCdPreProcessTaskService.get(company, taskId);
        Assertions.assertThat(finalTask.get().getAttemptCount()).isEqualTo(3);

    }
}
