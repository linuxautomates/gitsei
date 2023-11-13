package io.levelops.commons.databases.services;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.DbWorkItemFieldConverters;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Log4j2
public class WorkItemFieldsMetaService extends DatabaseService<DbWorkItemField> {
    public static final String TABLE_NAME = "issue_mgmt_workitems_fields_meta";
    private static final String INSERT_SQL_FORMAT = "INSERT INTO %s.issue_mgmt_workitems_fields_meta (custom, integration_id, " +
            "name, field_key, field_type, items_type)"
            + " VALUES(?,?,?,?,?,?)\n" +
            "ON CONFLICT(field_key, integration_id) " +
            "DO UPDATE SET (name, field_type, items_type) " +
            "= (EXCLUDED.name, EXCLUDED.field_type, EXCLUDED.items_type)\n" +
            "RETURNING id";
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public WorkItemFieldsMetaService(DataSource dataSource) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public String insert(String company, DbWorkItemField dbWorkItemField) throws SQLException {
        String insertSql = String.format(INSERT_SQL_FORMAT, company);
        UUID workItemHistoryJobId;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            pstmt.setObject(++i, dbWorkItemField.getCustom());
            pstmt.setObject(++i, NumberUtils.toInt(dbWorkItemField.getIntegrationId()));
            pstmt.setObject(++i, dbWorkItemField.getName());
            pstmt.setObject(++i, dbWorkItemField.getFieldKey());
            pstmt.setObject(++i, dbWorkItemField.getFieldType());
            pstmt.setObject(++i, dbWorkItemField.getItemsType());
            int affectedRows = pstmt.executeUpdate();
            // check the affected rows
            if (affectedRows <= 0) {
                throw new SQLException("Failed to create WorkItem job!");
            }
            // get the ID back
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new SQLException("Failed to create WorkItem job!");
                }
                workItemHistoryJobId = (UUID) rs.getObject(1);
                return workItemHistoryJobId.toString();
            }
        }
    }

    public List<String> batchUpsert(String company, List<DbWorkItemField> workItemFields) throws SQLException {
        String insertSql = String.format(INSERT_SQL_FORMAT, company);
        List<String> ids = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            for (DbWorkItemField field : workItemFields) {
                pstmt.setObject(1, field.getCustom());
                pstmt.setObject(2, NumberUtils.toInt(field.getIntegrationId()));
                pstmt.setObject(3, field.getName());
                pstmt.setObject(4, field.getFieldKey());
                pstmt.setObject(5, field.getFieldType());
                pstmt.setObject(6, field.getItemsType());
                pstmt.addBatch();
                pstmt.clearParameters();
                i++;
                if (i % 100 == 0) {
                    pstmt.executeBatch();
                    ResultSet rs = pstmt.getGeneratedKeys();
                    while (rs.next()) {
                        ids.add(rs.getString("id"));
                    }
                }
            }
            if (i % 100 != 0) {
                pstmt.executeBatch();
                ResultSet rs = pstmt.getGeneratedKeys();
                while (rs.next()) {
                    ids.add(rs.getString("id"));
                }
            }
        }
        return ids;
    }

    @Override
    public Boolean update(String company, DbWorkItemField t) throws SQLException {
        return null;
    }

    @Override
    public Optional<DbWorkItemField> get(String company, String param) throws SQLException {
        return Optional.empty();
    }

    @Override
    public DbListResponse<DbWorkItemField> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return null;
    }

    /**
     * It fetches the workItemFieldMeta data from the Db based on integrationIds and isCustom params
     *
     * @return List of DbWorkItemField
     * @throws SQLException
     */
    public DbListResponse<DbWorkItemField> list(String company, List<String> integrationIds, Boolean isCustom) throws SQLException {
        Validate.notEmpty(integrationIds, "Integration ids cannot be empty");
        Validate.notNull(isCustom, "isCustom param cannot be null");

        Map<String, Object> params = new HashMap<>();
        String sql = "SELECT * FROM " + company + "." + TABLE_NAME +
                " WHERE integration_id IN (:integrationIds) AND custom = :custom";
        params.put("integrationIds", integrationIds.stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        params.put("custom", isCustom);
        log.info("sql = " + sql);
        log.info("params: {}", params);

        List<DbWorkItemField> result = template.query(sql, params, DbWorkItemFieldConverters.workItemFieldRowMapper());
        return DbListResponse.of(result, result.size());
    }

    public DbListResponse<DbWorkItemField> listByFilter(String company,
                                                        List<String> integrationIds,
                                                        Boolean isCustom,
                                                        String exactName,
                                                        String partialName,
                                                        List<String> fieldKeys,
                                                        List<String> fieldTypes,
                                                        List<String> itemsTypes,
                                                        Integer pageNumber,
                                                        Integer pageSize) throws SQLException {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        if (CollectionUtils.isNotEmpty(integrationIds)) {
            conditions.add("integration_id IN (:integrationIds)");
            params.put("integrationIds", integrationIds.stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (Objects.nonNull(isCustom)) {
            conditions.add("custom = :custom");
            params.put("custom", isCustom);
        }
        if (StringUtils.isNotEmpty(exactName)) {
            conditions.add("name = :name");
            params.put("name", exactName);
        } else if (StringUtils.isNotEmpty(partialName)) {
            conditions.add("name ILIKE :name");
            params.put("name", partialName + "%");
        }
        if (CollectionUtils.isNotEmpty(fieldKeys)) {
            conditions.add("field_key IN (:fieldKeys)");
            params.put("fieldKeys", fieldKeys);
        }
        if (CollectionUtils.isNotEmpty(fieldTypes)) {
            conditions.add("field_type IN (:fieldTypes)");
            params.put("fieldTypes", fieldTypes);
        }
        if (CollectionUtils.isNotEmpty(itemsTypes)) {
            conditions.add("items_type IN (:itemsTypes)");
            params.put("itemsTypes", itemsTypes);
        }

        String criteria = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String sql = "SELECT * FROM " + company + "." + TABLE_NAME + criteria
                + " LIMIT " + pageSize
                + " OFFSET " + (pageNumber * pageSize);

        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbWorkItemField> retval = template.query(sql, params, DbWorkItemFieldConverters.workItemFieldRowMapper());

        String countSQL = "SELECT COUNT(*) FROM " + company + "." + TABLE_NAME + criteria;
        int totCount = 0;
        if (retval.size() > 0) {
            totCount = retval.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
            if (retval.size() == pageSize) {
                totCount = MoreObjects.firstNonNull(template.queryForObject(countSQL, params, Integer.class), totCount);
            }
        }
        return DbListResponse.of(retval, totCount);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        return null;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> ddl = List.of("CREATE TABLE IF NOT EXISTS {0}.issue_mgmt_workitems_fields_meta(\n" +
                        "    id                     UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    custom                 BOOLEAN,\n" +
                        "    integration_id         INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "    name                   VARCHAR NOT NULL,\n" +
                        "    field_key              VARCHAR NOT NULL,\n" +
                        "    field_type             VARCHAR NOT NULL,\n" +
                        "    items_type             VARCHAR,\n" +
                        "    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now() \n" +
                        ")",

                "CREATE UNIQUE INDEX IF NOT EXISTS issue_mgmt_workitems_fields_meta_field_key_integration_id_idx on {0}.issue_mgmt_workitems_fields_meta (field_key, integration_id)"
        );
        ddl.stream().map(statement -> MessageFormat.format(statement, company)).forEach(template.getJdbcTemplate()::execute);
        return true;
    }
}
