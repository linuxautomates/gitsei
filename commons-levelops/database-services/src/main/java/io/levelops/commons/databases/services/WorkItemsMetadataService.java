package io.levelops.commons.databases.services;

import io.levelops.commons.databases.converters.CountQueryConverter;
import io.levelops.commons.databases.converters.DbIssueStatusMetadataConverters;
import io.levelops.commons.databases.issue_management.DbIssueStatusMetadata;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Log4j2
public class WorkItemsMetadataService extends DatabaseService<DbIssueStatusMetadata> {

    public static final String TABLE_NAME = "issue_mgmt_status_metadata";
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public WorkItemsMetadataService(DataSource dataSource) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    private static final String DELETE_SQL_FORMAT = "DELETE FROM %s." + TABLE_NAME + " WHERE id = ?";

    private static final String INSERT_SQL_FORMAT = "INSERT INTO %s.issue_mgmt_status_metadata (" +
            "integration_id, project_id, status, status_category, status_id)"
            + " VALUES(?,?,?,?,?)\n" +
            "ON CONFLICT(integration_id, project_id, status_id) " +
            "DO UPDATE SET (status_category, status) " +
            "= (EXCLUDED.status_category, EXCLUDED.status)\n" +
            "RETURNING id";

    @Override
    public String insert(String company, DbIssueStatusMetadata dbIssueStatusMetadata) throws SQLException {
        String insertSql = String.format(INSERT_SQL_FORMAT, company);
        UUID workItemMetadataId;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            pstmt.setObject(++i, NumberUtils.toInt(dbIssueStatusMetadata.getIntegrationId()));
            pstmt.setObject(++i, dbIssueStatusMetadata.getProjectId());
            pstmt.setObject(++i, dbIssueStatusMetadata.getStatus());
            pstmt.setObject(++i, dbIssueStatusMetadata.getStatusCategory());
            pstmt.setObject(++i, dbIssueStatusMetadata.getStatusId());
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    workItemMetadataId = (UUID) rs.getObject(1);
                    return workItemMetadataId.toString();
                }
            }
        }
        return "";
    }

    @Override
    public Boolean update(String company, DbIssueStatusMetadata t) throws SQLException {
        return null;
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public Optional<DbIssueStatusMetadata> get(String company, String id) throws SQLException {
        var results = listByFilter(company, 0, 10, Collections.singletonList(UUID.fromString(id)), null, null, null).getRecords();
        return results.size() > 0 ? Optional.of(results.get(0)) : Optional.empty();
    }

    @Override
    public DbListResponse<DbIssueStatusMetadata> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return listByFilter(company, pageNumber, pageSize, null, null, null, null);
    }

    private void parseCriterias(final List<String> criterias, final MapSqlParameterSource params, final List<UUID> ids,
                                final List<Integer> integrationIds, final List<String> projectIds, final List<String> statusIds) {
        if(CollectionUtils.isNotEmpty(ids)) {
            criterias.add("id IN(:ids)");
            params.addValue("ids", ids);
        }
        if(CollectionUtils.isNotEmpty(integrationIds)) {
            criterias.add("integration_id IN(:integration_ids)");
            params.addValue("integration_ids", integrationIds);
        }
        if(CollectionUtils.isNotEmpty(projectIds)) {
            criterias.add("project_id IN(:project_ids)");
            params.addValue("project_ids", projectIds);
        }
        if(CollectionUtils.isNotEmpty(statusIds)) {
            criterias.add("status_id IN(:status_ids)");
            params.addValue("status_ids", statusIds);
        }
    }

    public DbListResponse<DbIssueStatusMetadata> listByFilter(String company, Integer pageNumber, Integer pageSize, final List<UUID> ids, final List<Integer> integrationIds, final List<String> projectIds, final List<String> statusIds) throws SQLException {
        List<String> criterias = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource();

        parseCriterias(criterias, params, ids, integrationIds, projectIds, statusIds);
        String baseWhereClause = (CollectionUtils.isEmpty(criterias)) ? "" : " WHERE " + String.join(" AND ", criterias);
        String selectSqlBase = "SELECT * FROM " + company + "." + TABLE_NAME + " " + baseWhereClause;
        String selectSql = selectSqlBase + " ORDER BY status_id asc" + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSQL = "SELECT COUNT(*) FROM (" +  selectSqlBase + ") AS counted";

        Integer totCount = 0;
        log.info("sql = " + selectSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbIssueStatusMetadata> issueStatusMetadatas = template.query(selectSql, params, DbIssueStatusMetadataConverters.issueStatusMetadataRowMapper());
        log.info("issueStatusMetadatas.size() = {}", issueStatusMetadatas.size());
        if (issueStatusMetadatas.size() > 0) {
            totCount = issueStatusMetadatas.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
            if (issueStatusMetadatas.size() == pageSize) {
                log.info("sql = " + countSQL); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
                log.info("params = {}", params);
                totCount = template.query(countSQL, params, CountQueryConverter.countMapper()).get(0);
            }
        }
        return DbListResponse.of(issueStatusMetadatas, totCount);
    }

    public Optional<DbIssueStatusMetadata> getByStatus(String company, String integrationId, String status) {
        Validate.notBlank(integrationId, "integrationId cannot be null or empty.");
        Validate.notBlank(status, "status cannot be null or empty.");
        String sql = "SELECT * FROM " + company + "." + TABLE_NAME +
                " WHERE integration_id = :integration_id AND " +
                "       status = :status " +
                " LIMIT 1 ";
        try {
            List<DbIssueStatusMetadata> results = template.query(sql, Map.of(
                            "integration_id", Integer.valueOf(integrationId),
                            "status", StringUtils.trimToEmpty(status)),
                    DbIssueStatusMetadataConverters.issueStatusMetadataRowMapper());
            return IterableUtils.getFirst(results);
        } catch (DataAccessException e) {
            log.warn("Failed to get workitem status metadata for integrationId={}, status={}", integrationId, status, e);
            return Optional.empty();
        }
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        return null;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> ddl = List.of("CREATE TABLE IF NOT EXISTS {0}.issue_mgmt_status_metadata(\n" +
                        "    id                        UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    integration_id            INTEGER NOT NULL REFERENCES " + company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "    project_id                VARCHAR NOT NULL,\n" +
                        "    status                    VARCHAR,\n" +
                        "    status_category           VARCHAR,\n" +
                        "    status_id                 VARCHAR,\n" +
                        "    created_at                TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now() \n" +
                        ")",
                "CREATE UNIQUE INDEX IF NOT EXISTS issue_mgmt_metadata_integration_project_status_id_idx " +
                        "on {0}."+ TABLE_NAME +" (integration_id, project_id, status_id)"
        );
        ddl.stream()
                .map(statement -> MessageFormat.format(statement, company))
                .forEach(template.getJdbcTemplate()::execute);
        return true;
    }
}
