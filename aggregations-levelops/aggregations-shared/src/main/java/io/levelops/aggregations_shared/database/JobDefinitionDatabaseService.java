package io.levelops.aggregations_shared.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import io.levelops.aggregations_shared.database.converters.JobDefinitionConverters;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.aggregations_shared.database.models.DbJobDefinitionFilter;
import io.levelops.aggregations_shared.database.models.DbJobDefinitionUpdate;
import io.levelops.aggregations_shared.utils.DatabaseUtils;
import io.levelops.commons.etl.models.JobType;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.MapUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.aggregations_shared.database.EtlDatabaseConstants.ETL_SCHEMA;

@Log4j2
@Service
public class JobDefinitionDatabaseService implements DatabaseService<DbJobDefinition> {
    public static final String JOB_DEFINITION_TABLE = ETL_SCHEMA + ".job_definition";
    private static final int PAGE_SIZE = 25;
    private final ObjectMapper objectMapper;
    private final NamedParameterJdbcTemplate template;

    public JobDefinitionDatabaseService(ObjectMapper objectMapper, DataSource dataSource) {
        this.objectMapper = objectMapper;
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public NamedParameterJdbcTemplate getTemplate() {
        return this.template;
    }

    public UUID insert(DbJobDefinition dbJobDefinition) throws JsonProcessingException {
        Validate.notBlank(dbJobDefinition.getAggProcessorName(), "Agg processor name can not be null");
        Validate.notBlank(dbJobDefinition.getTenantId(), "Tenant id can not be null");
        Validate.notNull(dbJobDefinition.getJobType(), "Job type can not be null");
        if (Set.of(
                JobType.GENERIC_INTEGRATION_JOB,
                JobType.INGESTION_RESULT_PROCESSING_JOB
        ).contains(dbJobDefinition.getJobType())) {
            Validate.notBlank(dbJobDefinition.getIntegrationId(), "Integration ID can not be null");
        }

        if (dbJobDefinition.getJobType().equals(JobType.INGESTION_RESULT_PROCESSING_JOB)) {
            Validate.notBlank(dbJobDefinition.getIngestionTriggerId(), "Ingestion trigger id can not be null");
        }

        var keyHolder = new GeneratedKeyHolder();
        boolean shouldInsertId = Objects.nonNull(dbJobDefinition.getId());
        HashMap<String, Object> params = Maps.newHashMap();
        StringBuilder fields = new StringBuilder();
        StringBuilder values = new StringBuilder();
        if (shouldInsertId) {
            fields.append("id,");
            values.append(":id::uuid, ");
            params.put("id", dbJobDefinition.getId());
        }
        fields.append(" tenant_id, integration_id, integration_type, ingestion_trigger_id, job_type, is_active, " +
                "default_priority, attempt_max, retry_wait_time_minutes, timeout_in_minutes, frequency_in_minutes, " +
                "full_frequency_in_minutes, agg_processor_name, last_iteration_ts, metadata");
        values.append(":tenant_id, :integration_id, :integration_type, :ingestion_trigger_id, :job_type, :is_active," +
                ":default_priority, :attempt_max, :retry_wait_time_minutes, :timeout_in_minutes, :frequency_in_minutes, " +
                ":full_frequency_in_minutes, :agg_processor_name, :last_iteration_ts, :metadata::jsonb");

        String sql = "INSERT INTO " + JOB_DEFINITION_TABLE + "(" + fields + ") \n" +
                "VALUES(" + values + ") RETURNING id";
        params.put("tenant_id", dbJobDefinition.getTenantId());
        params.put("integration_id", dbJobDefinition.getIntegrationId());
        params.put("integration_type", dbJobDefinition.getIntegrationType());
        params.put("ingestion_trigger_id", dbJobDefinition.getIngestionTriggerId());
        params.put("job_type", dbJobDefinition.getJobType().toString());
        params.put("is_active", dbJobDefinition.getIsActive());
        params.put("default_priority", dbJobDefinition.getDefaultPriority().getPriority());
        params.put("attempt_max", dbJobDefinition.getAttemptMax());
        params.put("retry_wait_time_minutes", dbJobDefinition.getRetryWaitTimeInMinutes());
        params.put("timeout_in_minutes", dbJobDefinition.getTimeoutInMinutes());
        params.put("frequency_in_minutes", dbJobDefinition.getFrequencyInMinutes());
        params.put("full_frequency_in_minutes", dbJobDefinition.getFullFrequencyInMinutes());
        params.put("agg_processor_name", dbJobDefinition.getAggProcessorName());
        params.put("last_iteration_ts", DatabaseUtils.toTimestamp(dbJobDefinition.getLastIterationTs()));
        params.put("metadata", objectMapper.writeValueAsString(MapUtils.emptyIfNull(dbJobDefinition.getMetadata())));
        getTemplate().update(sql, new MapSqlParameterSource(params), keyHolder);

        return (UUID) keyHolder.getKeys().get("id");
    }

    public Optional<DbJobDefinition> get(UUID id) {
        return IterableUtils.getFirst(filter(0, 1, DbJobDefinitionFilter.builder()
                .ids(List.of(id))
                .build()).getRecords());
    }

    public DbListResponse<DbJobDefinition> filter(Integer pageNumber, Integer pageSize, DbJobDefinitionFilter filter) {
        int limit = MoreObjects.firstNonNull(pageSize, PAGE_SIZE);
        int skip = limit * MoreObjects.firstNonNull(pageNumber, 0);

        List<String> conditions = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();

        params.put("skip", skip);
        params.put("limit", limit);

        if (filter.getIsActive() != null) {
            conditions.add("is_active = :is_active");
            params.put("is_active", filter.getIsActive());
        }

        if (CollectionUtils.isNotEmpty(filter.getIds())) {
            conditions.add("id in (:ids)");
            params.put("ids", filter.getIds());
        }

        if (filter.getTenantIdIntegrationIdPair() != null) {
            conditions.add("tenant_id = :tenant_id");
            params.put("tenant_id", filter.getTenantIdIntegrationIdPair().getLeft());
            conditions.add("integration_id = :integration_id");
            params.put("integration_id", filter.getTenantIdIntegrationIdPair().getRight());
        }

        if (filter.getJobTypes() != null) {
            conditions.add("job_type in (:job_types)");
            params.put("job_types", filter.getJobTypes().stream().map(Enum::toString).collect(Collectors.toList()));
        }

        if (CollectionUtils.isNotEmpty(filter.getTenantIds())) {
            conditions.add("tenant_id in (:tenant_ids)");
            params.put("tenant_ids", filter.getTenantIds());
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String sql = "SELECT * FROM " + JOB_DEFINITION_TABLE +
                where +
                " ORDER BY id " +
                " OFFSET :skip " +
                " LIMIT :limit ";

        log.debug(sql);
        log.debug(params);

        List<DbJobDefinition> results = getTemplate().query(sql, params, JobDefinitionConverters.rowMapper(objectMapper));
        String countSql = "SELECT count(*) FROM " + JOB_DEFINITION_TABLE + where;
        Integer count = template.queryForObject(countSql, params, Integer.class);

        return DbListResponse.of(results, count);
    }

    public Stream<DbJobDefinition> stream(DbJobDefinitionFilter jobDefinitionFilter) {
        return PaginationUtils.stream(0, 1, page -> filter(page, PAGE_SIZE, jobDefinitionFilter).getRecords());
    }

    private void validateUpdateWhereClause(DbJobDefinitionUpdate.WhereClause whereClause) {
        Validate.notNull(whereClause, "JobDefinition where clause can not be null");
        if (whereClause.getId() == null && whereClause.getTenantId() == null) {
            throw new IllegalArgumentException("JobDefinition where clause must have either id or tenantId");
        }
    }

    public Boolean update(DbJobDefinitionUpdate update) throws JsonProcessingException {
        validateUpdateWhereClause(update.getWhereClause());

        List<String> updates = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        String whereClause;
        if (update.getWhereClause().getId() != null) {
            whereClause = "id = :id";
            params.put("id", update.getWhereClause().getId());
        } else if (update.getWhereClause().getTenantId() != null) {
            whereClause = "tenant_id = :tenantId";
            params.put("tenantId", update.getWhereClause().getTenantId());
            if (update.getWhereClause().getIntegrationId() != null) {
                whereClause += " AND integration_id = :integrationId";
                params.put("integrationId", update.getWhereClause().getIntegrationId());
            }
        } else {
            throw new IllegalArgumentException("JobDefinition where clause must have either id or tenantId");
        }

       getUpdateParams(update, updates, params);

        if (updates.isEmpty()) {
            return true;
        }

        String sql = "UPDATE " + JOB_DEFINITION_TABLE +
                " SET " + String.join(", ", updates) + " " +
                " WHERE " + whereClause;
        return template.update(sql, params) > 0;
    }

    private void getUpdateParams(
            DbJobDefinitionUpdate update,
            List<String> updates,
            Map<String, Object> params) throws JsonProcessingException {
        if (update.getIsActive() != null) {
            updates.add("is_active = :is_active");
            params.put("is_active", update.getIsActive());
        }

        if (update.getLastIterationTs() != null) {
            updates.add("last_iteration_ts = :last_iteration_ts");
            params.put("last_iteration_ts", Timestamp.from(update.getLastIterationTs()));
        }

        if (update.getMetadata() != null) {
            updates.add("metadata = :metadata::jsonb");
            params.put("metadata", objectMapper.writeValueAsString(MapUtils.emptyIfNull(update.getMetadata())));
        }
    }

    @Override
    public void ensureTableExistence() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + JOB_DEFINITION_TABLE + " (" +
                "        id                                 UUID DEFAULT uuid_generate_v4() PRIMARY KEY," +
                "        tenant_id                          VARCHAR(255)," +
                "        integration_id                     VARCHAR(255)," +
                "        integration_type                   VARCHAR(255)," +
                "        ingestion_trigger_id               VARCHAR(255)," +
                "        job_type                           VARCHAR(255) NOT NULL," +
                "        is_active                          BOOLEAN NOT NULL DEFAULT true," +
                "        default_priority                   INTEGER NOT NULL DEFAULT 2," +
                "        attempt_max                        INTEGER," +
                "        retry_wait_time_minutes            INTEGER," +
                "        timeout_in_minutes                 INTEGER," +
                "        frequency_in_minutes               INTEGER," +
                "        full_frequency_in_minutes          INTEGER," +
                "        agg_processor_name                 VARCHAR(255)," +
                "        last_iteration_ts                  TIMESTAMPTZ," +
                "        created_at                         TIMESTAMPTZ NOT NULL DEFAULT now()," +
                "        metadata                           JSONB" +
                ")";


        log.debug("sql={}", sql);
        getTemplate().getJdbcTemplate().execute(sql);

        List.of(
                "CREATE INDEX IF NOT EXISTS etl_job_definition_is_active_index ON " + JOB_DEFINITION_TABLE + "(is_active)",
                "CREATE INDEX IF NOT EXISTS etl_job_definition_tenant_id_index ON " + JOB_DEFINITION_TABLE + "(tenant_id)",
                "CREATE INDEX IF NOT EXISTS etl_job_definition_tenant_id_job_type_index ON " + JOB_DEFINITION_TABLE + "(tenant_id, job_type)"
        ).forEach(getTemplate().getJdbcTemplate()::execute);

        log.info("Ensured table existence: control_plane.jobs");
    }
}
