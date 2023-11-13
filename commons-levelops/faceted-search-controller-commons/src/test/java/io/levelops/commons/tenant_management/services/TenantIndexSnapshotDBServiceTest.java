package io.levelops.commons.tenant_management.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.faceted_search.models.IndexType;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.tenant_management.models.JobStatus;
import io.levelops.commons.tenant_management.models.Offsets;
import io.levelops.commons.tenant_management.models.TenantConfig;
import io.levelops.commons.tenant_management.models.TenantIndexSnapshot;
import io.levelops.commons.tenant_management.models.TenantIndexTypeConfig;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TenantIndexSnapshotDBServiceTest {
    private static ObjectMapper objectMapper = DefaultObjectMapper.get();
    private static TenantConfigDBService tenantConfigDBService;
    private static TenantIndexTypeConfigDBService tenantIndexTypeConfigDBService;
    private static TenantIndexSnapshotDBService tenantIndexSnapshotDBService;
    private static NamedParameterJdbcTemplate template;
    private static final String company = "foo";
    private static final String COMPANY_2 = "test";

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
        tenantIndexSnapshotDBService = new TenantIndexSnapshotDBService(dataSource, objectMapper);

        tenantConfigDBService.ensureTableExistence(null);
        tenantIndexTypeConfigDBService.ensureTableExistence(null);
        tenantIndexSnapshotDBService.ensureTableExistence(null);

        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Test
    public void test() throws SQLException {
        TenantConfig tenantConfig = TenantConfig.builder()
                .tenantId(company)
                .enabled(true)
                .priority(1)
                .build();

        String id = tenantConfigDBService.insert(null, tenantConfig);

        TenantIndexTypeConfig tenantIndexTypeConfig = TenantIndexTypeConfig.builder()
                .tenantConfigId(Long.valueOf(id))
                .indexType(IndexType.WORK_ITEMS)
                .priority(1)
                .frequencyInMins(60L)
                .build();

        String indexConfigId = tenantIndexTypeConfigDBService.insert(null, tenantIndexTypeConfig);

        TenantIndexSnapshot tenantIndexSnapshot = TenantIndexSnapshot.builder()
                .indexTypeConfigId(UUID.fromString(indexConfigId))
                .indexName("work_items_foo_1612137600")
                .indexExist(false)
                .priority(1)
                .status(JobStatus.UNASSIGNED)
                .ingestedAt(1612137600L)
                .build();

        UUID snapshotId = UUID.fromString(tenantIndexSnapshotDBService.insertTenantIndexSnapshot(tenantIndexSnapshot));

        final Instant now = Instant.now();
        List<Long> l = IntStream.range(1,20).mapToLong(i -> now.minus(i, ChronoUnit.DAYS).getEpochSecond()).boxed().collect(Collectors.toList());
        Iterator<Long> it = l.listIterator();
        Instant lastHeartbeat = null;

        Offsets emptyOffsets = Offsets.builder().build();

        tenantIndexSnapshotDBService.updateIndexSnapshotStatus(snapshotId, JobStatus.SCHEDULED, null);
        TenantIndexSnapshot t = tenantIndexSnapshotDBService.get(null, snapshotId.toString()).get();
        Assertions.assertThat(t.getIndexName()).isEqualTo("work_items_foo_1612137600");
        Assertions.assertThat(t.getPriority()).isEqualTo(1);
        Assertions.assertThat(t.getStatus()).isEqualTo(JobStatus.SCHEDULED);
        Assertions.assertThat(t.getFailedAttemptsCount()).isEqualTo(0);
        Assertions.assertThat(t.getIndexExist()).isEqualTo(false);
        Assertions.assertThat(t.getLastRefreshedAt()).isNull();
        Assertions.assertThat(t.getLatestOffsets()).isEqualTo(emptyOffsets);
        Assertions.assertThat(t.getHeartbeat()).isNull();

        Offsets offsets1 = Offsets.builder().latestJiraIssueUpdatedAts(Map.of(1,it.next(),2,it.next())).latestWiUpdatedAts(Map.of(4,it.next(),5,it.next())).latestScmCommittedAts(Map.of(6,it.next())).latestScmPRUpdatedAts(Map.of(7,it.next())).build();
        Assertions.assertThat(tenantIndexSnapshotDBService.updateIndexSnapshotLatestOffsets(snapshotId, offsets1)).isEqualTo(0);
        t = tenantIndexSnapshotDBService.get(null, snapshotId.toString()).get();
        Assertions.assertThat(t.getIndexName()).isEqualTo("work_items_foo_1612137600");
        Assertions.assertThat(t.getPriority()).isEqualTo(1);
        Assertions.assertThat(t.getStatus()).isEqualTo(JobStatus.SCHEDULED);
        Assertions.assertThat(t.getFailedAttemptsCount()).isEqualTo(0);
        Assertions.assertThat(t.getIndexExist()).isEqualTo(false);
        Assertions.assertThat(t.getLastRefreshedAt()).isNull();
        Assertions.assertThat(t.getLatestOffsets()).isEqualTo(emptyOffsets);
        Assertions.assertThat(t.getHeartbeat()).isNull();

        tenantIndexSnapshotDBService.updateIndexSnapshotStatus(snapshotId, JobStatus.PENDING, null);
        t = tenantIndexSnapshotDBService.get(null, snapshotId.toString()).get();
        Assertions.assertThat(t.getIndexName()).isEqualTo("work_items_foo_1612137600");
        Assertions.assertThat(t.getPriority()).isEqualTo(1);
        Assertions.assertThat(t.getStatus()).isEqualTo(JobStatus.PENDING);
        Assertions.assertThat(t.getFailedAttemptsCount()).isEqualTo(0);
        Assertions.assertThat(t.getIndexExist()).isEqualTo(false);
        Assertions.assertThat(t.getLastRefreshedAt()).isNull();
        Assertions.assertThat(t.getLatestOffsets()).isEqualTo(emptyOffsets);
        Assertions.assertThat(t.getHeartbeat()).isNull();

        Assertions.assertThat(tenantIndexSnapshotDBService.updateIndexSnapshotLatestOffsets(snapshotId, offsets1)).isEqualTo(1);
        t = tenantIndexSnapshotDBService.get(null, snapshotId.toString()).get();
        Assertions.assertThat(t.getIndexName()).isEqualTo("work_items_foo_1612137600");
        Assertions.assertThat(t.getPriority()).isEqualTo(1);
        Assertions.assertThat(t.getStatus()).isEqualTo(JobStatus.PENDING);
        Assertions.assertThat(t.getFailedAttemptsCount()).isEqualTo(0);
        Assertions.assertThat(t.getIndexExist()).isEqualTo(false);
        Assertions.assertThat(t.getLastRefreshedAt()).isNull();
        Assertions.assertThat(t.getLatestOffsets()).isEqualTo(offsets1);
        Assertions.assertThat(t.getHeartbeat()).isNotNull();
        lastHeartbeat = t.getHeartbeat();

        tenantIndexSnapshotDBService.updateIndexSnapshotStatus(snapshotId, JobStatus.FAILURE, null);
        t = tenantIndexSnapshotDBService.get(null, snapshotId.toString()).get();
        Assertions.assertThat(t.getIndexName()).isEqualTo("work_items_foo_1612137600");
        Assertions.assertThat(t.getPriority()).isEqualTo(1);
        Assertions.assertThat(t.getStatus()).isEqualTo(JobStatus.FAILURE);
        Assertions.assertThat(t.getFailedAttemptsCount()).isEqualTo(1);
        Assertions.assertThat(t.getIndexExist()).isEqualTo(false);
        Assertions.assertThat(t.getLastRefreshedAt()).isNull();
        Assertions.assertThat(t.getLatestOffsets()).isEqualTo(offsets1);
        Assertions.assertThat(t.getHeartbeat()).isNotNull();
        Assertions.assertThat(t.getHeartbeat()).isEqualTo(lastHeartbeat);

        Offsets offsets2 = Offsets.builder().latestJiraIssueUpdatedAts(Map.of(1,it.next(),2,it.next())).latestWiUpdatedAts(Map.of(4,it.next(),5,it.next())).latestScmCommittedAts(Map.of(6,it.next())).latestScmPRUpdatedAts(Map.of(7,it.next())).build();

        Assertions.assertThat(tenantIndexSnapshotDBService.updateIndexSnapshotLatestOffsets(snapshotId, offsets2)).isEqualTo(0);
        t = tenantIndexSnapshotDBService.get(null, snapshotId.toString()).get();
        Assertions.assertThat(t.getIndexName()).isEqualTo("work_items_foo_1612137600");
        Assertions.assertThat(t.getPriority()).isEqualTo(1);
        Assertions.assertThat(t.getStatus()).isEqualTo(JobStatus.FAILURE);
        Assertions.assertThat(t.getFailedAttemptsCount()).isEqualTo(1);
        Assertions.assertThat(t.getIndexExist()).isEqualTo(false);
        Assertions.assertThat(t.getLastRefreshedAt()).isNull();
        Assertions.assertThat(t.getLatestOffsets()).isEqualTo(offsets1);
        Assertions.assertThat(t.getHeartbeat()).isNotNull();
        Assertions.assertThat(t.getHeartbeat()).isEqualTo(lastHeartbeat);

        tenantIndexSnapshotDBService.updateIndexSnapshotStatus(snapshotId, JobStatus.FAILURE, null);
        t = tenantIndexSnapshotDBService.get(null, snapshotId.toString()).get();
        Assertions.assertThat(t.getIndexName()).isEqualTo("work_items_foo_1612137600");
        Assertions.assertThat(t.getPriority()).isEqualTo(1);
        Assertions.assertThat(t.getStatus()).isEqualTo(JobStatus.FAILURE);
        Assertions.assertThat(t.getFailedAttemptsCount()).isEqualTo(2);
        Assertions.assertThat(t.getIndexExist()).isEqualTo(false);
        Assertions.assertThat(t.getLastRefreshedAt()).isNull();
        Assertions.assertThat(t.getLatestOffsets()).isEqualTo(offsets1);
        Assertions.assertThat(t.getHeartbeat()).isNotNull();
        Assertions.assertThat(t.getHeartbeat()).isEqualTo(lastHeartbeat);

        Assertions.assertThat(tenantIndexSnapshotDBService.updateIndexSnapshotLatestOffsets(snapshotId, offsets2)).isEqualTo(0);
        t = tenantIndexSnapshotDBService.get(null, snapshotId.toString()).get();
        Assertions.assertThat(t.getIndexName()).isEqualTo("work_items_foo_1612137600");
        Assertions.assertThat(t.getPriority()).isEqualTo(1);
        Assertions.assertThat(t.getStatus()).isEqualTo(JobStatus.FAILURE);
        Assertions.assertThat(t.getFailedAttemptsCount()).isEqualTo(2);
        Assertions.assertThat(t.getIndexExist()).isEqualTo(false);
        Assertions.assertThat(t.getLastRefreshedAt()).isNull();
        Assertions.assertThat(t.getLatestOffsets()).isEqualTo(offsets1);
        Assertions.assertThat(t.getHeartbeat()).isNotNull();
        Assertions.assertThat(t.getHeartbeat()).isEqualTo(lastHeartbeat);

        tenantIndexSnapshotDBService.updateIndexSnapshotStatus(snapshotId, JobStatus.SUCCESS, now);
        t = tenantIndexSnapshotDBService.get(null, snapshotId.toString()).get();
        Assertions.assertThat(t.getIndexName()).isEqualTo("work_items_foo_1612137600");
        Assertions.assertThat(t.getPriority()).isEqualTo(1);
        Assertions.assertThat(t.getStatus()).isEqualTo(JobStatus.SUCCESS);
        Assertions.assertThat(t.getFailedAttemptsCount()).isEqualTo(0);
        Assertions.assertThat(t.getIndexExist()).isEqualTo(true);
        Assertions.assertThat(t.getLastRefreshStartedAt()).isNotNull();
        Assertions.assertThat(t.getLastRefreshStartedAt()).isEqualTo(now);
        Assertions.assertThat(t.getLastRefreshedAt()).isNotNull();
        Assertions.assertThat(t.getLatestOffsets()).isEqualTo(offsets1);
        Assertions.assertThat(t.getHeartbeat()).isNotNull();
        Assertions.assertThat(t.getHeartbeat()).isEqualTo(lastHeartbeat);

        Assertions.assertThat(tenantIndexSnapshotDBService.updateIndexSnapshotLatestOffsets(snapshotId, offsets2)).isEqualTo(0);
        t = tenantIndexSnapshotDBService.get(null, snapshotId.toString()).get();
        Assertions.assertThat(t.getIndexName()).isEqualTo("work_items_foo_1612137600");
        Assertions.assertThat(t.getPriority()).isEqualTo(1);
        Assertions.assertThat(t.getStatus()).isEqualTo(JobStatus.SUCCESS);
        Assertions.assertThat(t.getFailedAttemptsCount()).isEqualTo(0);
        Assertions.assertThat(t.getIndexExist()).isEqualTo(true);
        Assertions.assertThat(t.getLastRefreshStartedAt()).isNotNull();
        Assertions.assertThat(t.getLastRefreshStartedAt()).isEqualTo(now);
        Assertions.assertThat(t.getLastRefreshedAt()).isNotNull();
        Assertions.assertThat(t.getLatestOffsets()).isEqualTo(offsets1);
        Assertions.assertThat(t.getHeartbeat()).isNotNull();
        Assertions.assertThat(t.getHeartbeat()).isEqualTo(lastHeartbeat);

        tenantIndexSnapshotDBService.updateIndexSnapshotStatus(snapshotId, JobStatus.PENDING, null);
        t = tenantIndexSnapshotDBService.get(null, snapshotId.toString()).get();
        Assertions.assertThat(t.getIndexName()).isEqualTo("work_items_foo_1612137600");
        Assertions.assertThat(t.getPriority()).isEqualTo(1);
        Assertions.assertThat(t.getStatus()).isEqualTo(JobStatus.PENDING);
        Assertions.assertThat(t.getFailedAttemptsCount()).isEqualTo(0);
        Assertions.assertThat(t.getIndexExist()).isEqualTo(true);
        Assertions.assertThat(t.getLastRefreshStartedAt()).isNotNull();
        Assertions.assertThat(t.getLastRefreshedAt()).isNotNull();
        Assertions.assertThat(t.getLatestOffsets()).isEqualTo(offsets1);
        Assertions.assertThat(t.getHeartbeat()).isNotNull();
        Assertions.assertThat(t.getHeartbeat()).isEqualTo(lastHeartbeat);

        Assertions.assertThat(tenantIndexSnapshotDBService.updateIndexSnapshotLatestOffsets(snapshotId, offsets2)).isEqualTo(1);
        t = tenantIndexSnapshotDBService.get(null, snapshotId.toString()).get();
        Assertions.assertThat(t.getIndexName()).isEqualTo("work_items_foo_1612137600");
        Assertions.assertThat(t.getPriority()).isEqualTo(1);
        Assertions.assertThat(t.getStatus()).isEqualTo(JobStatus.PENDING);
        Assertions.assertThat(t.getFailedAttemptsCount()).isEqualTo(0);
        Assertions.assertThat(t.getIndexExist()).isEqualTo(true);
        Assertions.assertThat(t.getLastRefreshStartedAt()).isNotNull();
        Assertions.assertThat(t.getLastRefreshedAt()).isNotNull();
        Assertions.assertThat(t.getLatestOffsets()).isEqualTo(offsets2);
        Assertions.assertThat(t.getHeartbeat()).isNotNull();
        Assertions.assertThat(t.getHeartbeat()).isAfter(lastHeartbeat);

        List<TenantIndexSnapshot> records = tenantIndexSnapshotDBService.listByFilter(0, 100, null, null, null, null, null, null).getRecords();
        Assert.assertEquals(1, records.size());

        Assert.assertTrue(tenantIndexSnapshotDBService.delete(null, snapshotId.toString()));
        records = tenantIndexSnapshotDBService.listByFilter(0, 100, null, null, null, null, null, null).getRecords();
        Assert.assertEquals(0, records.size());
    }

    private List<Long> stringToLongList(String input) {
        return Arrays.asList(input.split(",")).stream()
                .map(s -> Long.valueOf(s))
                .collect(Collectors.toList());
    }

    @Test
    public void test2() throws SQLException {
        TenantConfig tenantConfig = TenantConfig.builder()
                .tenantId(COMPANY_2)
                .enabled(true)
                .priority(1)
                .build();

        String id = tenantConfigDBService.insert(null, tenantConfig);

        TenantIndexTypeConfig tenantIndexTypeConfig = TenantIndexTypeConfig.builder()
                .tenantConfigId(Long.valueOf(id))
                .indexType(IndexType.WORK_ITEMS)
                .priority(1)
                .frequencyInMins(60L)
                .build();

        String indexConfigId = tenantIndexTypeConfigDBService.insert(null, tenantIndexTypeConfig);
        final TenantIndexTypeConfig ic = tenantIndexTypeConfig.toBuilder().id(UUID.fromString(indexConfigId)).build();

        Instant current = Instant.ofEpochSecond(1659312000);
        //Instant current = startingIngestedAt;
        List<Long> ingestedAtsT0 = new ArrayList<>();
        while(current.getEpochSecond() >= 1612137600) {
            ingestedAtsT0.add(current.getEpochSecond());
            current = current.minus(1, ChronoUnit.DAYS);
        }
        List<TenantIndexSnapshot> snapshots = ingestedAtsT0.stream()
                .map(i -> TenantIndexSnapshot.builder()
                        .indexTypeConfigId(ic.getId())
                        .indexName(ic.getIndexType().getPartitionedIndexName(COMPANY_2, i))
                        .status(JobStatus.UNASSIGNED)
                        .priority(ic.getPriority())
                        .ingestedAt(i)
                        .build())
                .collect(Collectors.toList());

        tenantIndexSnapshotDBService.batchUpsert(snapshots);

        List<TenantIndexSnapshot> records  = tenantIndexSnapshotDBService.listByFilter(0, 1000, null, null, null, null, false, null).getRecords();
        Assert.assertEquals(ingestedAtsT0.size(), records.size());
        records = tenantIndexSnapshotDBService.listByFilter(0, 1000, null, null, null, null, true, null).getRecords();
        Assert.assertEquals(0, records.size());

        String expectedIngestedAtsString = "1659312000,1659225600,1659139200,1659052800,1658966400,1658880000,1658793600,1658707200,1658620800,1658534400,1658448000,1658361600,1658275200,1658188800,1658102400,1658016000,1657929600,1657843200,1657756800,1657670400,1657584000,1657497600,1657411200,1657324800,1657238400,1657152000,1657065600,1656979200,1656892800,1656806400,1656720000,1656633600,1656547200,1656460800,1656374400,1656288000,1656201600,1656115200,1656028800,1655942400,1655856000,1655769600,1655683200,1655596800,1655510400,1655424000,1655337600,1655251200,1655164800,1655078400,1654992000,1654905600,1654819200,1654732800,1654646400,1654560000,1654473600,1654387200,1654300800,1654214400,1654128000,1654041600,1653955200,1653868800,1653782400,1653696000,1653609600,1653523200,1653436800,1653350400,1653264000,1653177600,1653091200,1653004800,1652918400,1652832000,1652745600,1652659200,1652572800,1652486400,1652400000,1652313600,1652227200,1652140800,1652054400,1651968000,1651881600,1651795200,1651708800,1651622400,1651536000,1651363200,1648771200,1646092800,1643673600,1640995200,1638316800,1635724800,1633046400,1630454400,1627776000,1625097600,1622505600,1619827200,1617235200,1614556800,1612137600";
        List<Long> ingestedAtsT1 = stringToLongList(expectedIngestedAtsString);

        tenantIndexSnapshotDBService.markSnapshotsAsDeleted(ic.getId(), ingestedAtsT1);
        records = tenantIndexSnapshotDBService.listByFilter(0, 1000, null, null, null, null, false, null).getRecords();
        Assert.assertEquals(ingestedAtsT1.size(), records.size());
        records = tenantIndexSnapshotDBService.listByFilter(0, 1000, null, null, null, null, true, null).getRecords();
        Assert.assertEquals(ingestedAtsT0.size() - ingestedAtsT1.size(), records.size());

        //Set All snapshots to deleted
        int updatedCount = template.update("UPDATE _levelops_faceted_search_controller.tenant_index_snapshots SET marked_for_deletion = true, marked_for_deletion_at = now(), updated_at = now()", Map.of());
        Assert.assertEquals(ingestedAtsT0.size(), updatedCount);
        records = tenantIndexSnapshotDBService.listByFilter(0, 1000, null, null, null, null, false, null).getRecords();
        Assert.assertEquals(0, records.size());
        records = tenantIndexSnapshotDBService.listByFilter(0, 1000, null, null, null, null, true, null).getRecords();
        Assert.assertEquals(ingestedAtsT0.size(), records.size());

        tenantIndexSnapshotDBService.markSnapshotsAsNotDeleted(ic.getId(), ingestedAtsT1);
        records  = tenantIndexSnapshotDBService.listByFilter(0, 1000, null, null, null, null, false, null).getRecords();
        Assert.assertEquals(ingestedAtsT1.size(), records.size());
        records = tenantIndexSnapshotDBService.listByFilter(0, 1000, null, null, null, null, true, null).getRecords();
        Assert.assertEquals(ingestedAtsT0.size() - ingestedAtsT1.size(), records.size());

        Instant now = Instant.now();
        records  = tenantIndexSnapshotDBService.listByFilter(0, 1000, null, null, null, null, false, now).getRecords();
        Assert.assertEquals(0, records.size());
        records = tenantIndexSnapshotDBService.listByFilter(0, 1000, null, null, null, null, true, now).getRecords();
        Assert.assertEquals(ingestedAtsT0.size() - ingestedAtsT1.size(), records.size());

        Instant yesterday = now.minus(1, ChronoUnit.DAYS);
        records  = tenantIndexSnapshotDBService.listByFilter(0, 1000, null, null, null, null, false, yesterday).getRecords();
        Assert.assertEquals(0, records.size());
        records = tenantIndexSnapshotDBService.listByFilter(0, 1000, null, null, null, null, true, yesterday).getRecords();
        Assert.assertEquals(0, records.size());


    }
}