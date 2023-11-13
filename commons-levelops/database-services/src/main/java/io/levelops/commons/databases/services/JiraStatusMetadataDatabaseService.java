package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.DbJiraIssueConverters;
import io.levelops.commons.databases.models.database.jira.DbJiraStatusMetadata;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
@Service
public class JiraStatusMetadataDatabaseService extends FilteredDatabaseService<DbJiraStatusMetadata, JiraStatusMetadataDatabaseService.JiraStatusMetadataFilter> {

    @Autowired
    public JiraStatusMetadataDatabaseService(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public String insert(String company, DbJiraStatusMetadata t) throws SQLException {
        Validate.notBlank(t.getIntegrationId(), "integrationId cannot be null or empty.");
        Validate.notBlank(t.getStatus(), "status cannot be null or empty.");

        String sql = "INSERT INTO " + company + ".jira_status_metadata " +
                "(integration_id, status, status_id, status_category)" +
                " VALUES " +
                "(:integration_id, :status, :status_id, :status_category)";

        Map<String, Object> params = Map.of(
                "integration_id", Integer.valueOf(t.getIntegrationId()),
                "status", StringUtils.upperCase(t.getStatus()),
                "status_id", StringUtils.defaultString(t.getStatusId()),
                "status_category", StringUtils.defaultString(StringUtils.upperCase(t.getStatusCategory()))
        );
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(sql, new MapSqlParameterSource(params), keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            return null;
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }

    public String upsert(String company, DbJiraStatusMetadata o) throws SQLException {
        String sql = "INSERT INTO " + company + ".jira_status_metadata " +
                " (integration_id, status, status_id, status_category)" +
                " VALUES " +
                " (:integration_id, :status, :status_id, :status_category)" +
                " ON CONFLICT (integration_id, status_id) " +
                " DO UPDATE SET" +
                "   status = EXCLUDED.status, " +
                "   status_category = EXCLUDED.status_category " +
                " RETURNING id";

        Map<String, Object> params = Map.of(
                "integration_id", Integer.valueOf(o.getIntegrationId()),
                "status", StringUtils.upperCase(o.getStatus()),
                "status_id", StringUtils.defaultString(o.getStatusId()),
                "status_category", StringUtils.defaultString(StringUtils.upperCase(o.getStatusCategory()))
        );
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(sql, new MapSqlParameterSource(params), keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            return null;
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }

    @Override
    public Boolean update(String company, DbJiraStatusMetadata t) throws SQLException {
        Validate.notBlank(t.getId(), "id cannot be null or empty.");

        List<String> updates = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        params.put("id", t.getId());

        // we will consider that {integration_id + status} is a secondary key, so we won't update these fields

        // -- status category
        if (t.getStatusCategory() != null) {
            updates.add("status_category = :status_category");
            params.put("status_category", StringUtils.upperCase(t.getStatusCategory()));
        }
        // -- status id
        if (t.getStatusId() != null) {
            updates.add("status_id = :status_id");
            params.put("status_id", t.getStatusId());
        }
        // -- status
        if (t.getStatus() != null) {
            updates.add("status = :status");
            params.put("status", t.getStatus());
        }

        if (updates.isEmpty()) {
            return true;
        }

        String sql = "UPDATE " + company + ".jira_status_metadata " +
                " SET " + String.join(", ", updates) +
                " WHERE id = :id::uuid ";

        return template.update(sql, params) > 0;
    }

    @Override
    public Optional<DbJiraStatusMetadata> get(String company, String id) throws SQLException {
        String sql = "SELECT * FROM " + company + ".jira_status_metadata " +
                " WHERE id = :id::uuid " +
                " LIMIT 1 ";
        try {
            List<DbJiraStatusMetadata> results = template.query(sql, Map.of("id", id),
                    DbJiraIssueConverters.statusMetadataRowMapper());
            return IterableUtils.getFirst(results);
        } catch (DataAccessException e) {
            log.warn("Failed to get jira status metadata for id={}", id, e);
            return Optional.empty();
        }
    }

    // Note: name is not unique!
    public Optional<DbJiraStatusMetadata> getByName(String company, String integrationId, String status) {
        Validate.notBlank(integrationId, "integrationId cannot be null or empty.");
        Validate.notBlank(status, "status cannot be null or empty.");
        String sql = "SELECT * FROM " + company + ".jira_status_metadata " +
                " WHERE integration_id = :integration_id AND " +
                "       status = :status " +
                " LIMIT 1 ";
        try {
            List<DbJiraStatusMetadata> results = template.query(sql, Map.of(
                    "integration_id", Integer.valueOf(integrationId),
                    "status", StringUtils.trimToEmpty(StringUtils.upperCase(status))),
                    DbJiraIssueConverters.statusMetadataRowMapper());
            return IterableUtils.getFirst(results);
        } catch (DataAccessException e) {
            log.warn("Failed to get jira status metadata for integrationId={}, name={}", integrationId, status, e);
            return Optional.empty();
        }
    }

    public List<DbJiraStatusMetadata.IntegStatusCategoryMetadata> getIntegStatusCategoryMetadata(String company, List<String> integrationIds) {
        String whereClause = "";
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            whereClause = " WHERE integration_id IN (:integration_ids) ";
        }
        String sql = "SELECT integration_id, status_category, array_agg(status) as statuses FROM " + company + ".jira_status_metadata "
                 + whereClause +
                " GROUP BY integration_id, status_category";
        try {
            return template.query(sql,
                    Map.of("integration_ids", integrationIds.stream().map(NumberUtils::toInt).collect(Collectors.toList())),
                    DbJiraIssueConverters.statusCategoryToStatusesRowMapper());
        } catch (DataAccessException e) {
            log.warn("Failed to get jira status metadata for integrationIds={}", integrationIds, e);
        }
        return List.of();
    }

    public Optional<DbJiraStatusMetadata> getByStatusId(String company, String integrationId, String statusId) {
        Validate.notBlank(integrationId, "integrationId cannot be null or empty.");
        Validate.notBlank(statusId, "statusId cannot be null or empty.");
        String sql = "SELECT * FROM " + company + ".jira_status_metadata " +
                " WHERE integration_id = :integration_id AND " +
                "       status_id = :status_id " +
                " LIMIT 1 ";
        try {
            List<DbJiraStatusMetadata> results = template.query(sql, Map.of(
                    "integration_id", Integer.valueOf(integrationId),
                    "status_id", StringUtils.trimToEmpty(statusId)),
                    DbJiraIssueConverters.statusMetadataRowMapper());
            return IterableUtils.getFirst(results);
        } catch (DataAccessException e) {
            log.warn("Failed to get jira status metadata for integrationId={}, statusId={}", integrationId, statusId, e);
            return Optional.empty();
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = JiraStatusMetadataFilter.JiraStatusMetadataFilterBuilder.class)
    public static class JiraStatusMetadataFilter {
        String integrationId;
        String statusId;
        String status;
        String partialStatus;
        String statusCategory;
    }

    @Override
    public DbListResponse<DbJiraStatusMetadata> filter(Integer pageNumber, Integer pageSize, String company, @Nullable JiraStatusMetadataFilter filter) {
        int limit = MoreObjects.firstNonNull(pageSize, DEFAULT_PAGE_SIZE);
        int skip = limit * MoreObjects.firstNonNull(pageNumber, 0);
        filter = filter != null ? filter : JiraStatusMetadataFilter.builder().build();

        List<String> conditions = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);

        // -- integration id
        if (StringUtils.isNotEmpty(filter.getIntegrationId())) {
            conditions.add("integration_id = :integration_id");
            params.put("integration_id", Integer.valueOf(filter.getIntegrationId()));
        }
        // -- status id
        if (StringUtils.isNotEmpty(filter.getStatusId())) {
            conditions.add("status_id = :status_id");
            params.put("status_id", filter.getStatusId());
        }
        // -- status
        if (StringUtils.isNotEmpty(filter.getStatus())) {
            conditions.add("status = :status");
            params.put("status", StringUtils.upperCase(filter.getStatus()));
        }
        // -- partial status
        else if (StringUtils.isNotEmpty(filter.getPartialStatus())) {
            conditions.add("status LIKE :status");
            params.put("status", "%" + StringUtils.trimToEmpty(StringUtils.upperCase(filter.getPartialStatus())) + "%");
        }
        // -- status_category
        if (StringUtils.isNotEmpty(filter.getStatusCategory())) {
            conditions.add("status_category = :status_category");
            params.put("status_category", StringUtils.upperCase(filter.getStatusCategory()));
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String sql = "SELECT * FROM " + company + ".jira_status_metadata " +
                where +
                " ORDER BY status ASC " +
                " OFFSET :skip " +
                " LIMIT :limit ";
        List<DbJiraStatusMetadata> results = template.query(sql, params, DbJiraIssueConverters.statusMetadataRowMapper());
        String countSql = "SELECT count(*) FROM " + company + ".jira_status_metadata " + where;
        Integer count = template.queryForObject(countSql, params, Integer.class);

        return DbListResponse.of(results, count);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String sql = "DELETE FROM " + company + ".jira_status_metadata " +
                " WHERE id = :id::uuid";
        return template.update(sql, Map.of(
                "id", id
        )) > 0;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".jira_status_metadata (" +
                        "    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        "    integration_id  INTEGER NOT NULL" +
                        "                       REFERENCES " + company + ".integrations(id)" +
                        "                       ON DELETE CASCADE," +
                        "    status          VARCHAR(64) NOT NULL," + // limit in jira is 60
                        "    status_category VARCHAR(64) NOT NULL," +
                        "    status_id       VARCHAR(32) NOT NULL," +
                        "    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()," +
                        "    UNIQUE (integration_id, status_id)" +
                        ")"
        );
        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }

}
