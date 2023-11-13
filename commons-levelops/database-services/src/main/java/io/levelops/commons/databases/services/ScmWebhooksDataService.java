package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.converters.DbWebhookDataConverters;
import io.levelops.commons.databases.models.database.scm.DbWebhookData;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class ScmWebhooksDataService extends DatabaseService<DbWebhookData>{

    private static final String SCM_WEBHOOKS_DATA_TABLE = "scm_webhooks_data";
    private static final ObjectMapper objectMapper = DefaultObjectMapper.get();
    private final NamedParameterJdbcTemplate template;

    private static final String INSERT_SQL_FORMAT = "INSERT INTO %s." + SCM_WEBHOOKS_DATA_TABLE + "(integration_id, webhook_id, job_id," +
            "webhook_event, status)"
            + " VALUES(?,?,?,to_json(?::jsonb),?)\n" +
            "ON CONFLICT(integration_id, webhook_id) " +
            "DO UPDATE SET (job_id, webhook_event, status) = " +
            "(EXCLUDED.job_id, EXCLUDED.webhook_event, EXCLUDED.status)\n" +
            "RETURNING id";

    @Autowired
    public ScmWebhooksDataService(DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public String insert(String company, DbWebhookData dbWebhookData) throws SQLException {
        String insertSql = String.format(INSERT_SQL_FORMAT, company);
        UUID dbWebhookDataId;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            pstmt.setObject(++i, dbWebhookData.getIntegrationId());
            pstmt.setObject(++i, UUID.fromString(dbWebhookData.getWebhookId()));
            pstmt.setObject(++i, StringUtils.isEmpty(dbWebhookData.getJobId()) ? null : UUID.fromString(dbWebhookData.getJobId()));
            String webhookEvent = "";
            try {
                webhookEvent = DefaultObjectMapper.get().writeValueAsString(dbWebhookData.getWebhookEvent());
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize attributes json. will store empty json.", e);
            }
            pstmt.setObject(++i, webhookEvent);
            pstmt.setObject(++i, dbWebhookData.getStatus());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows <= 0) {
                throw new SQLException("Failed to create DbWebhookData!");
            }
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new SQLException("Failed to create DbWebhookData!");
                }
                dbWebhookDataId = (UUID) rs.getObject(1);
                return dbWebhookDataId.toString();
            }
        }
    }

    @Override
    public Boolean update(String company, DbWebhookData t) {
        return null;
    }

    @Override
    public Optional<DbWebhookData> get(String company, String param) throws SQLException {
        return Optional.empty();
    }

    public List<DbWebhookData> getByNullJobId(String company, Integer pageNumber, Integer pageSize) {
        String sqlBody = company + "." + SCM_WEBHOOKS_DATA_TABLE + " WHERE job_id IS NULL";
        Map<String, Object> params = Map.of("skip", pageNumber * pageSize, "limit", pageSize);
        List<DbWebhookData> results = List.of();
        if (pageSize > 0) {
            String sql = "SELECT * FROM " + sqlBody;
            sql += " ORDER BY integration_id OFFSET :skip LIMIT :limit";
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            results = template.query(sql, params, DbWebhookDataConverters.webhookDataRowMapper(objectMapper));
        }
        log.info("params = {}", params);
        return results;
    }

    public void assignJobIds(String company, String integrationId, List<String> webhookIds, String jobId) {
        if (Objects.isNull(integrationId) || Objects.isNull(jobId) || CollectionUtils.isEmpty(webhookIds)) {
            log.warn("assignJobId: Invalid criteria provided. company: " + company + " integrationId: " + integrationId
                    + " jobId: " + jobId + " webhookId: " + webhookIds);
            return;
        }
        String sql = "UPDATE " + company + "." + SCM_WEBHOOKS_DATA_TABLE + " SET job_id = :job_id " +
                " WHERE job_id IS NULL and integration_id = :integration_id and webhook_id IN (:webhook_ids)";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("job_id", UUID.fromString(jobId));
        params.addValue("integration_id", NumberUtils.toInt(integrationId));
        params.addValue("webhook_ids",  webhookIds.stream().map(UUID::fromString).collect(Collectors.toList()));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        template.update(sql, params);
    }

    public void updateStatus(String company, String integrationId, List<String> webhookIds, String jobId, DbWebhookData.Status status) {
        if (Objects.isNull(integrationId) || Objects.isNull(jobId)
                || CollectionUtils.isEmpty(webhookIds)) {
            log.warn("updateStatus: Invalid criteria provided. company: " + company + " integrationId: " + integrationId
                    + " jobId: " + jobId + " webhookId: " + webhookIds + " status: " + status.toString());
            return;
        }
        String sql = "UPDATE " + company + "." + SCM_WEBHOOKS_DATA_TABLE + " SET status = :status " +
                " WHERE integration_id = :integration_id and webhook_id = :webhook_ids and job_id = :job_id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("status", status.toString());
        params.addValue("integration_id", NumberUtils.toInt(integrationId));
        params.addValue("webhook_ids", webhookIds.stream().map(UUID::fromString).collect(Collectors.toList()));
        params.addValue("job_id", UUID.fromString(jobId));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        template.update(sql, params);
    }

    public void updateStatus(String company, String integrationId, String jobId, DbWebhookData.Status status) {
        if (Objects.isNull(integrationId) || Objects.isNull(jobId)) {
            log.warn("updateStatus: Invalid criteria provided. company: " + company + " integrationId: " + integrationId
                    + " jobId: " + jobId + " status: " + status.toString());
            return;
        }
        String sql = "UPDATE " + company + "." + SCM_WEBHOOKS_DATA_TABLE + " SET status = :status" +
                " WHERE integration_id = :integration_id and job_id = :job_id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("status", status.toString());
        params.addValue("integration_id", NumberUtils.toInt(integrationId));
        params.addValue("job_id", UUID.fromString(jobId));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        template.update(sql, params);
    }

    public List<DbWebhookData> getWebhookEventData(String company, String integrationId, String jobId) {
        Validate.notBlank(jobId, "Missing jobId.");
        Validate.notBlank(integrationId, "Missing integrationId.");
        String sql = "SELECT * FROM " + company + "." + SCM_WEBHOOKS_DATA_TABLE
                + " WHERE job_id = :jobId AND integration_id = :integid";
        Map<String, Object> params = Map.of(
                "jobId", UUID.fromString(jobId), "integid", NumberUtils.toInt(integrationId));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        return template.query(sql, params, DbWebhookDataConverters.webhookDataRowMapper(objectMapper));
    }

    public List<String> getPendingJobIds(String company, Integer olderThanInMinutes, DbWebhookData.Status status, Integer pageNumber, Integer pageSize) {
        Validate.notBlank(company, "Missing customer");
        String sql = "SELECT DISTINCT job_id, created_at FROM " + company + "." + SCM_WEBHOOKS_DATA_TABLE
                + " WHERE job_id IS NOT NULL AND status = :status"
                + " AND (now() - created_at) > INTERVAL '" + olderThanInMinutes + " MINUTE'"
                + " ORDER BY created_at DESC OFFSET :skip LIMIT :limit";
        Map<String, Object> params = Map.of("status", status.toString(),
                "skip", pageNumber * pageSize,
                "limit", pageSize);
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        return template.query(sql, params, DbWebhookDataConverters.webhookJobIdRowMapper());
    }

    @Override
    public DbListResponse<DbWebhookData> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        String sqlBody = company + "." + SCM_WEBHOOKS_DATA_TABLE;
        Map<String, Object> params = Map.of("skip", pageNumber * pageSize, "limit", pageSize);
        List<DbWebhookData> results = List.of();
        if (pageSize > 0) {
            String sql = "SELECT * FROM " + sqlBody;
            sql += " OFFSET :skip LIMIT :limit";
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            results = template.query(sql, params, DbWebhookDataConverters.webhookDataRowMapper(objectMapper));
        }
        String countSql = "SELECT COUNT(*) FROM " + sqlBody;
        countSql += " OFFSET :skip LIMIT :limit";
        log.info("countSql = {}", countSql);
        log.info("params = {}", params);
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }

    @Override
    public Boolean delete(String company, String id) {
        return null;
    }

    public void updateCreatedAt(String company, String integrationId, String webhookId, String interval) {
        if (Objects.isNull(integrationId) || Objects.isNull(webhookId)) {
            log.warn("updateStatus: Invalid criteria provided. company: " + company + " integrationId: " + integrationId);
            return;
        }
        String sql = "UPDATE " + company + "." + SCM_WEBHOOKS_DATA_TABLE + " SET created_at = now() - INTERVAL '" + interval + "' DAY" +
                " WHERE integration_id = :integration_id and webhook_id = :webhook_id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("integration_id", NumberUtils.toInt(integrationId));
        params.addValue("webhook_id", UUID.fromString(webhookId));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        template.update(sql, params);
    }

    @Override
    public Boolean ensureTableExistence(String company) {
        List<String> ddl = List.of("CREATE TABLE IF NOT EXISTS " + company + "." + SCM_WEBHOOKS_DATA_TABLE + "(\n" +
                        "        id                    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "        integration_id        INTEGER NOT NULL REFERENCES " + company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "        webhook_id            UUID NOT NULL, \n" +
                        "        job_id                UUID, \n" +
                        "        webhook_event         JSONB NOT NULL DEFAULT '{}'::jsonb, \n" +
                        "        status                VARCHAR NOT NULL, \n" +
                        "        updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(), \n" +
                        "        created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now() \n" +
                        "    )",
                "CREATE INDEX IF NOT EXISTS scm_webhooks_data_integration_id_webhook_id_job_idx ON " + company + "." +
                        SCM_WEBHOOKS_DATA_TABLE + "(integration_id, webhook_id, COALESCE(job_id, '00000000-0000-0000-0000-000000000000'))",
                "CREATE UNIQUE INDEX IF NOT EXISTS scm_webhooks_data_integration_id_webhook_idx ON " + company + "." +
                        SCM_WEBHOOKS_DATA_TABLE + " (integration_id, webhook_id)");

        ddl.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
}