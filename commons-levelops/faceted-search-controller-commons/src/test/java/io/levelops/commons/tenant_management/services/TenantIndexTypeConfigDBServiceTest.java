package io.levelops.commons.tenant_management.services;

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
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TenantIndexTypeConfigDBServiceTest  {
    private static ObjectMapper objectMapper = DefaultObjectMapper.get();
    private static TenantConfigDBService tenantConfigDBService;
    private static TenantIndexTypeConfigDBService tenantIndexTypeConfigDBService;
    private static TenantIndexSnapshotDBService tenantIndexSnapshotDBService;
    private static NamedParameterJdbcTemplate template;

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

        String tenantSelect = "SELECT * from _levelops_faceted_search_controller.tenant_configs";
        String tenantConfigSelect = "SELECT * from _levelops_faceted_search_controller.tenant_index_type_configs";
        String tenantSnapshotSelect = "SELECT * from _levelops_faceted_search_controller.tenant_index_snapshots";

        TenantConfig tenantConfig = TenantConfig.builder()
                .tenantId("foo")
                .enabled(true)
                .priority(1)
                .build();
        String id = tenantConfigDBService.insert(null, tenantConfig);
        Assertions.assertThat(id).isEqualTo("1");

        List<TenantConfig> tenantConfigList = tenantConfigDBService.list("foo", 0, 10).getRecords();
        Assertions.assertThat(tenantConfigList.size()).isEqualTo(1);

        TenantIndexTypeConfig tenantIndexTypeConfig = TenantIndexTypeConfig.builder()
                .tenantId("foo")
                .tenantConfigId(Long.valueOf(id))
                .indexType(IndexType.WORK_ITEMS)
                .frequencyInMins(IndexType.WORK_ITEMS.getDefaultRefreshTimeInMins())
                .priority(3)
                .build();
        String tenantIndexTypeConfigId = tenantIndexTypeConfigDBService.insert(null, tenantIndexTypeConfig);


        TenantIndexSnapshot tenantIndexSnapshot = TenantIndexSnapshot.builder()
                .indexTypeConfigId(UUID.fromString(tenantIndexTypeConfigId))
                .indexName("work_items_foo_1612137600")
                .indexExist(true)
                .priority(1)
                .status(JobStatus.UNASSIGNED)
                .ingestedAt(1612137600L)
                .lastRefreshedAt(Instant.now())
                .build();

        tenantIndexSnapshotDBService.insertTenantIndexSnapshot(tenantIndexSnapshot);

        List<Map<String, Object>> list = template.queryForList(tenantSnapshotSelect, Map.of());
        Assertions.assertThat(list.size()).isEqualTo(1);

        tenantIndexSnapshot = TenantIndexSnapshot.builder()
                .indexTypeConfigId(UUID.fromString(tenantIndexTypeConfigId))
                .indexName("work_items_foo_1612137600")
                .indexExist(true)
                .priority(3)
                .status(JobStatus.SCHEDULED)
                .ingestedAt(Instant.now().getEpochSecond())
                .lastRefreshedAt(Instant.now())
                .build();

        tenantIndexSnapshotDBService.insertTenantIndexSnapshot(tenantIndexSnapshot);

        tenantConfig = TenantConfig.builder()
                .id(1l)
                .priority(3)
                .build();

        List<TenantIndexTypeConfig> streamTenantIndexes = tenantIndexTypeConfigDBService.list(null, 0, 10).getRecords();
        Assertions.assertThat(streamTenantIndexes.size()).isEqualTo(1);

        list = template.queryForList(tenantSelect, Map.of());
        Assertions.assertThat(list.size()).isEqualTo(1);
        list = template.queryForList(tenantConfigSelect, Map.of());
        Assertions.assertThat(list.size()).isEqualTo(1);
        list = template.queryForList(tenantSnapshotSelect, Map.of());
        Assertions.assertThat(list.size()).isEqualTo(2);

    }

}