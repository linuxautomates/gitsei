package io.levelops.commons.tenant_management.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.faceted_search.models.IndexType;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.tenant_management.models.JobStatus;
import io.levelops.commons.tenant_management.models.TenantConfig;
import io.levelops.commons.tenant_management.models.TenantIndexSnapshot;
import io.levelops.commons.tenant_management.models.TenantIndexTypeConfig;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.mutable.MutableInt;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static io.levelops.commons.tenant_management.services.TenantIndexSnapshotDBService.TENANT_INDEX_SNAPSHOTS_FULL_NAME;

@Log4j2
public class TenantIndexSnapshotJobsDBServiceTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    private static NamedParameterJdbcTemplate template;
    private static TenantConfigDBService tenantConfigDBService;
    private static TenantIndexTypeConfigDBService tenantIndexTypeConfigDBService;
    private static TenantIndexSnapshotDBService tenantIndexSnapshotDBService;
    private static TenantIndexSnapshotJobsDBService tenantIndexSnapshotJobsDBService;

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    @BeforeClass
    public static void setup() throws SQLException {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(DatabaseService.SchemaType.FS_CONTROLLER_SCHEMA.getSchemaName());

        tenantConfigDBService = new TenantConfigDBService(dataSource);
        tenantIndexTypeConfigDBService = new TenantIndexTypeConfigDBService(dataSource);
        tenantIndexSnapshotDBService = new TenantIndexSnapshotDBService(dataSource, MAPPER);

        tenantConfigDBService.ensureTableExistence(null);
        tenantIndexTypeConfigDBService.ensureTableExistence(null);

        tenantIndexSnapshotDBService.ensureTableExistence(null);
        tenantIndexSnapshotDBService.ensureTableExistence(null);

        template = new NamedParameterJdbcTemplate(dataSource);
        tenantIndexSnapshotJobsDBService = new TenantIndexSnapshotJobsDBService(dataSource, tenantIndexSnapshotDBService);
    }

    private List<Long> getIngestedAts(IndexType i) {
        if (i.isSupportsPartitionedIndex()) {
            return List.of(1618444800l, 1618358400l, 1618272000l);
        } else {
            return List.of(9223372036854775807l);
        }
    }

    private List<Long> getIngestedAtsIntegrationTracker(IndexType i) {
        if (i.isSupportsPartitionedIndex()) {
            return List.of(1672963200l, 1673049600l, 1673136000l, 1673222400l, 1673308800l);
        } else {
            return List.of(9223372036854775807l);
        }
    }


    @Test
    public void test() throws SQLException, JsonProcessingException {
        Instant now = Instant.now();
        List<TenantConfig> tenantConfigs = TenantConfigTestUtils.createTenantConfigs(tenantConfigDBService, 3);

        List<TenantIndexTypeConfig> ics = new ArrayList<>();
        for(TenantConfig tc : tenantConfigs) {
            for(IndexType i : IndexType.values()) {
                TenantIndexTypeConfig ic = TenantIndexTypeConfig.builder()
                        .tenantId(tc.getTenantId())
                        .tenantConfigId(tc.getId())
                        .indexType(i)
                        .priority(tc.getPriority())
                        .frequencyInMins(i.getDefaultRefreshTimeInMins())
                        .build();
                log.info("ic = {}", ic);
                String id = tenantIndexTypeConfigDBService.insert(null, ic);
                ic = ic.toBuilder().id(UUID.fromString(id)).build();
                ics.add(ic);
            }
        }

        MutableInt snapshotsCount = new MutableInt();
        for(TenantIndexTypeConfig ic : ics) {
            List<Long> ingestedAts = getIngestedAts(ic.getIndexType());
            List<TenantIndexSnapshot> indexSnapshots = new ArrayList<>();
            for(Long i : ingestedAts) {
                TenantIndexSnapshot s = TenantIndexSnapshot.builder()
                        .indexTypeConfigId(ic.getId())
                        .ingestedAt(i)
                        .indexName(ic.getIndexType().getPartitionedIndexName(ic.getTenantId(), i))
                        .priority(ic.getPriority())
                        .status(JobStatus.UNASSIGNED)
                        .heartbeat(now)
                        .build();
                indexSnapshots.add(s);
            }
            tenantIndexSnapshotDBService.batchUpsert(indexSnapshots);
            snapshotsCount.add(indexSnapshots.size());
        }

        Integer scheduledCount = tenantIndexSnapshotJobsDBService.scheduleJobs();
        Assertions.assertThat(scheduledCount).isEqualTo(snapshotsCount.getValue());

        scheduledCount = tenantIndexSnapshotJobsDBService.scheduleJobs();
        Assertions.assertThat(scheduledCount).isEqualTo(0);

        List<TenantIndexSnapshot> snapshotsRead = new ArrayList<>();
        MutableInt snapshotsReadCount = new MutableInt();
        boolean fetch = true;
        while (fetch) {
            Optional<TenantIndexSnapshot> opt = tenantIndexSnapshotJobsDBService.assignJob();
            if(opt.isPresent()) {
                snapshotsReadCount.add(1);
                snapshotsRead.add(opt.get());
                Assertions.assertThat(opt.get().getStatus()).isEqualTo(JobStatus.PENDING);
                Assertions.assertThat(opt.get().getStatusUpdatedAt()).isNotNull();
                Assertions.assertThat(opt.get().getHeartbeat()).isNull();
            } else {
                fetch = false;
            }
        }
        Assertions.assertThat(snapshotsReadCount.getValue()).isEqualTo(snapshotsCount.getValue());
        System.out.println(MAPPER.writeValueAsString(snapshotsRead));

        TenantConfig tc4 = TenantConfigTestUtils.createTenantConfig(tenantConfigDBService, 3);
        List<TenantIndexTypeConfig> tc4ics = new ArrayList<>();
        for(IndexType i : IndexType.values()) {
            TenantIndexTypeConfig ic = TenantIndexTypeConfig.builder()
                    .tenantId(tc4.getTenantId())
                    .tenantConfigId(tc4.getId())
                    .indexType(i)
                    .priority(tc4.getPriority())
                    .frequencyInMins(i.getDefaultRefreshTimeInMins())
                    .build();
            log.info("ic = {}", ic);
            String id = tenantIndexTypeConfigDBService.insert(null, ic);
            ic = ic.toBuilder().id(UUID.fromString(id)).build();
            tc4ics.add(ic);
        }

        for(TenantIndexTypeConfig ic : tc4ics) {
            List<Long> ingestedAts = getIngestedAtsIntegrationTracker(ic.getIndexType());
            List<TenantIndexSnapshot> indexSnapshots = new ArrayList<>();
            for(int i=0; i< ingestedAts.size(); i++) {
                Long ingestedAt = ingestedAts.get(i);
                JobStatus status = JobStatus.SUCCESS;
                if(i==1) {
                    status = JobStatus.FAILURE;
                } else if (i ==2) {
                    status = JobStatus.UNASSIGNED;
                } else if (i ==3) {
                    status = JobStatus.PENDING;
                } else if (i ==4) {
                    status = JobStatus.SCHEDULED;
                }
                TenantIndexSnapshot s = TenantIndexSnapshot.builder()
                        .indexTypeConfigId(ic.getId())
                        .ingestedAt(ingestedAt)
                        .indexName(ic.getIndexType().getPartitionedIndexName(ic.getTenantId(), ingestedAt))
                        .priority(ic.getPriority())
                        .status(status)
                        .build();
                indexSnapshots.add(s);
            }
            tenantIndexSnapshotDBService.batchUpsert(indexSnapshots);
            snapshotsCount.add(indexSnapshots.size());
        }

        List<Long> ingestedAts = getIngestedAtsIntegrationTracker(IndexType.WORK_ITEMS);
        for(int i=0; i< ingestedAts.size(); i++ ) {
            if(i < 3) {
                Assert.assertEquals(1, tenantIndexSnapshotJobsDBService.scheduleJobsByTenantIdAndIngestedAt("foo-3", ingestedAts.get(i)));
            } else {
                Assert.assertEquals(0, tenantIndexSnapshotJobsDBService.scheduleJobsByTenantIdAndIngestedAt("foo-3", ingestedAts.get(i)));
            }
        }

        //At this point no jobs should need scheduling
        Assertions.assertThat(tenantIndexSnapshotJobsDBService.scheduleJobs()).isEqualTo(0);

        //Update less than pending stuck interval
        updateHeartBeatOfPendingJobs("work_items_", 118);

        //Pending jobs heartbeat is within acceptable limit, so no jobs will be scheduled
        Assertions.assertThat(tenantIndexSnapshotJobsDBService.scheduleJobs()).isEqualTo(0);

        //Update more than pending stuck interval
        int staleHeartbeatJobsCount = updateHeartBeatOfPendingJobs( "work_items_",121);
        Assertions.assertThat(tenantIndexSnapshotJobsDBService.scheduleJobs()).isEqualTo(staleHeartbeatJobsCount);

        //At this point no jobs should need scheduling
        Assertions.assertThat(tenantIndexSnapshotJobsDBService.scheduleJobs()).isEqualTo(0);

        System.out.println("Success");
    }

    //Helper function to test
    private int updateHeartBeatOfPendingJobs(String indexNamePrefix, int offsetInMinutes) {
        String update = "UPDATE " + TENANT_INDEX_SNAPSHOTS_FULL_NAME +  " SET heartbeat = now() - interval '" + offsetInMinutes + " minutes' where status = 'pending' and index_name like '" + indexNamePrefix + "%' ";
        return template.update(update, Map.of());
    }

}