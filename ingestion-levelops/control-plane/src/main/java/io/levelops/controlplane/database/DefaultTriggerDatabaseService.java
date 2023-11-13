package io.levelops.controlplane.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.models.DbTriggerConverters;
import io.levelops.controlplane.models.DbTriggerSettings;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Log4j2
@Service
public class DefaultTriggerDatabaseService implements TriggerDatabaseService {

    // TODO make db schema configurable (e.g. to add dev env)
    private static final String TRIGGERS_TABLE = "control_plane.triggers";
    private final ObjectMapper objectMapper;
    private final Function<Map<String, Object>, DbTrigger> dbTriggerParser;
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public DefaultTriggerDatabaseService(@Qualifier("controlPlaneJdbcTemplate") NamedParameterJdbcTemplate template,
                                         ObjectMapper objectMapper) {
        this.template = template;
        this.objectMapper = objectMapper;
        dbTriggerParser = DbTriggerConverters.getDbTriggerParser(objectMapper);
    }

    @Override
    public NamedParameterJdbcTemplate getTemplate() {
        return this.template;
    }

    // region GET
    public Stream<DbTrigger> streamTriggers(int pageSize) {
        return IntStream.iterate(0, i -> i + pageSize)
                .mapToObj(i -> getTriggers(i, pageSize))
                .takeWhile(CollectionUtils::isNotEmpty)
                .flatMap(Collection::stream);
    }

    // TODO replace with filter()
    public List<DbTrigger> getTriggers(int skip, int limit) {
        String sql = "SELECT * from control_plane.triggers " +
                " ORDER BY created_at DESC " +
                " OFFSET :skip " +
                " LIMIT :limit";
        return IterableUtils.parseIterable(getTemplate().queryForList(sql, Map.of(
                "skip", skip,
                "limit", limit)
        ), dbTriggerParser);
    }

    @Override
    public List<DbTrigger> filter(int skip, int limit, @Nullable String tenantId, @Nullable String integrationId, @Nullable String triggerType) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);

        if (Strings.isNotEmpty(tenantId)) {
            conditions.add("tenant_id = :tenant_id");
            params.put("tenant_id", tenantId);
        }
        if (Strings.isNotEmpty(integrationId)) {
            conditions.add("integration_id = :integration_id");
            params.put("integration_id", integrationId);
        }
        if (Strings.isNotEmpty(triggerType)) {
            conditions.add("type = :type");
            params.put("type", triggerType);
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String sql = "SELECT * from control_plane.triggers " +
                where +
                " ORDER BY created_at DESC " +
                " OFFSET :skip " +
                " LIMIT :limit";
        return IterableUtils.parseIterable(getTemplate().queryForList(sql, params), dbTriggerParser);
    }

    // TODO replace with filter()
    public List<DbTrigger> getTriggersByIntegration(IntegrationKey integrationKey) {
        String sql = "SELECT * from control_plane.triggers " +
                "WHERE integration_id = :integration_id " +
                "AND tenant_id = :tenant_id " +
                "ORDER BY created_at";
        return IterableUtils.parseIterable(getTemplate().queryForList(sql, Map.of(
                "integration_id", integrationKey.getIntegrationId(),
                "tenant_id", integrationKey.getTenantId())
        ), dbTriggerParser);
    }

    // TODO replace with filter()
    public List<DbTrigger> getTriggersByIntegrationAndType(IntegrationKey integrationKey, String triggerType) {
        String sql = "SELECT * from control_plane.triggers " +
                "WHERE integration_id = :integration_id " +
                "AND tenant_id = :tenant_id " +
                "AND type = :trigger_type " +
                "ORDER BY created_at";
        return IterableUtils.parseIterable(getTemplate().queryForList(sql, Map.of(
                "integration_id", integrationKey.getIntegrationId(),
                "tenant_id", integrationKey.getTenantId(),
                "trigger_type", triggerType)
        ), dbTriggerParser);
    }

    public Optional<DbTrigger> getTriggerById(String triggerId) {
        String sql = "SELECT * from control_plane.triggers " +
                "WHERE id = :id::uuid";
        List<Map<String, Object>> output = getTemplate().queryForList(sql, Map.of("id", triggerId));
        if (CollectionUtils.isEmpty(output)) {
            return Optional.empty();
        }
        return Optional.ofNullable(dbTriggerParser.apply(output.get(0)));
    }

    // endregion

    public void createTrigger(UUID id, String tenantId, String integrationId, Boolean reserved, String triggerType, int frequency, String metadataJson, @Nullable String callbackUrl, @Nullable DbTriggerSettings settings) throws JsonProcessingException {
        Validate.notNull(id, "id cannot be null.");
        Validate.notBlank(triggerType, "triggerType cannot be null or empty.");

        String sql = "INSERT INTO control_plane.triggers(id, tenant_id, integration_id, reserved, type, frequency, metadata, callback_url, settings) \n" +
                "VALUES(:id::uuid, :tenant_id, :integration_id, :reserved, :type, :frequency, to_json(:metadata::json), :callback_url, to_json(:settings::json))";

        HashMap<String, Object> params = Maps.newHashMap();
        params.put("id", id);
        params.put("tenant_id", tenantId);
        params.put("integration_id", integrationId);
        params.put("reserved", BooleanUtils.toBooleanDefaultIfNull(reserved, false));
        params.put("type", triggerType);
        params.put("frequency", frequency);
        params.put("metadata", metadataJson);
        params.put("callback_url", callbackUrl);
        params.put("settings", settings != null ? objectMapper.writeValueAsString(settings) : null);
        getTemplate().update(sql, params);
    }

    public boolean deleteTrigger(String triggerId) {
        Validate.notBlank(triggerId, "triggerId cannot be null or empty.");
        String sql = "DELETE FROM control_plane.triggers " +
                "WHERE id = :trigger_id::uuid";
        return getTemplate().update(sql, Map.of(
                "trigger_id", triggerId
        )) > 0;
    }

    public int deleteTriggers(String tenantId, String integrationId) {
        Validate.notBlank(tenantId, "tenantId cannot be null or empty.");
        Validate.notBlank(integrationId, "integrationId cannot be null or empty.");
        String sql = "DELETE FROM control_plane.triggers " +
                "WHERE tenant_id = :tenant_id AND integration_id = :integration_id";
        return getTemplate().update(sql, Map.of(
                "tenant_id", tenantId,
                "integration_id", integrationId)
        );
    }

    public boolean updateTriggerWithIteration(String triggerId, String iterationId, Instant iterationTs) {
        Validate.notBlank(triggerId, "triggerId cannot be null or empty.");
        Validate.notBlank(iterationId, "iterationId cannot be null or empty.");
        Validate.notNull(iterationTs, "iterationTs cannot be null.");
        String sql = "UPDATE control_plane.triggers SET " +
                "iteration_id = :iteration_id::uuid, " +
                "iteration_ts = :iteration_ts " +
                "WHERE id = :trigger_id::uuid";
        return getTemplate().update(sql, Map.of(
                "trigger_id", triggerId,
                "iteration_id", iterationId,
                "iteration_ts", DateUtils.toEpochSecond(iterationTs))
        ) > 0;
    }

    public boolean updateTriggerMetadata(String triggerId, Object metadata) throws JsonProcessingException {
        Validate.notBlank(triggerId, "triggerId cannot be null or empty.");
        Validate.notNull(metadata, "metadata cannot be null.");
        String sql = "UPDATE control_plane.triggers SET " +
                "metadata = to_json(:metadata::json) " +
                "WHERE id = :trigger_id::uuid";
        String metadataStr;
        if (metadata instanceof String) {
            metadataStr = (String) metadata;
        } else {
            metadataStr = objectMapper.writeValueAsString(metadata);
        }
        return getTemplate().update(sql, Map.of(
                "trigger_id", triggerId,
                "metadata", metadataStr)
        ) > 0;
    }

    public boolean updateTriggerFrequency(String triggerId, int frequency) {
        Validate.notBlank(triggerId, "triggerId cannot be null or empty.");
        String sql = "UPDATE control_plane.triggers SET " +
                "frequency = :frequency " +
                "WHERE id = :trigger_id::uuid";
        return getTemplate().update(sql, Map.of(
                "trigger_id", triggerId,
                "frequency", frequency)
        ) > 0;
    }

    // region --- setup ---

    @Override
    public void ensureTableExistence() {

        String sql = "CREATE TABLE IF NOT EXISTS control_plane.triggers (" +
                "        id              UUID PRIMARY KEY," +
                "        tenant_id       VARCHAR(255)," +
                "        integration_id  VARCHAR(255)," +
                "        reserved        BOOLEAN NOT NULL DEFAULT false," +
                "        type            VARCHAR(255) NOT NULL," +
                "        frequency       INTEGER NOT NULL DEFAULT 0," +
                "        metadata        JSONB," +
                "        settings        JSONB," +
                "        callback_url    VARCHAR(255)," +
                "        iteration_id    UUID," +
                "        iteration_ts    BIGINT," +
                "        created_at      BIGINT NOT NULL DEFAULT extract(epoch from now())" +
                ")";

        log.debug("sql={}", sql);
        getTemplate().getJdbcTemplate().execute(sql);

//        String sqlIndexCreation = "CREATE INDEX IF NOT EXISTS control_plane_triggers_type_index ON control_plane.jobs(tenant_id, integration_id, type)";
//        log.debug("sql={}", sqlIndexCreation);
//        getTemplate().getJdbcTemplate().execute(sqlIndexCreation);


        log.info("Ensured table existence: control_plane.triggers");
    }

    // endregion
}
