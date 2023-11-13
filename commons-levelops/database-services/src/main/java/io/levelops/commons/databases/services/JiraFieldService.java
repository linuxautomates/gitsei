package io.levelops.commons.databases.services;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
@Service
public class JiraFieldService extends DatabaseService<DbJiraField> {

    private static final RowMapper<DbJiraField> JIRA_FIELD_ROW_MAPPER = (rs, rowNumber) -> DbJiraField.builder()
            .id(rs.getString("id"))
            .name(rs.getString("name"))
            .custom(rs.getBoolean("custom"))
            .fieldKey(rs.getString("fieldkey"))
            .fieldType(rs.getString("fieldtype"))
            .fieldItems(rs.getString("fielditems"))
            .integrationId(rs.getString("integrationid"))
            .createdAt(rs.getLong("createdat"))
            .build();

    private final NamedParameterJdbcTemplate template;

    @Autowired
    public JiraFieldService(DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public String insert(String company, DbJiraField jiraField) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public List<String> batchUpsert(String company, List<DbJiraField> jiraFields) throws SQLException {
        String SQL = "INSERT INTO " + company + ".jirafields(integrationid,name,fieldkey,fieldtype,fielditems,custom)"
                + " VALUES(?,?,?,?,?,?) ON CONFLICT (integrationid,fieldkey)"
                + " DO UPDATE SET (name,fieldtype,fielditems,custom) ="
                + " (EXCLUDED.name,EXCLUDED.fieldtype,EXCLUDED.fielditems,EXCLUDED.custom)"
                + " RETURNING id";

        List<String> ids = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            for (DbJiraField field : jiraFields) {
                pstmt.setInt(1, Integer.parseInt(field.getIntegrationId()));
                pstmt.setString(2, field.getName());
                pstmt.setString(3, field.getFieldKey());
                pstmt.setString(4, field.getFieldType());
                if (StringUtils.isEmpty(field.getFieldItems())) {
                    pstmt.setNull(5, Types.VARCHAR);
                } else {
                    pstmt.setString(5, field.getFieldItems());
                }
                pstmt.setBoolean(6, field.getCustom());
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
    public Boolean update(String company, DbJiraField field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DbJiraField> get(String company, String fieldId) {
        throw new UnsupportedOperationException();
    }

    public DbListResponse<DbJiraField> listByFilter(String company,
                                                    List<String> integrationIds,
                                                    Boolean isCustom,
                                                    String exactName,
                                                    String partialName,
                                                    List<String> fieldKeys,
                                                    Integer pageNumber,
                                                    Integer pageSize)
            throws SQLException {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            conditions.add("integrationid IN (:integrationIds)");
            params.put("integrationIds", integrationIds.stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (isCustom != null) {
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
            conditions.add("fieldkey IN (:fieldKeys)");
            params.put("fieldKeys", fieldKeys);
        }

        String criteria = conditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", conditions);
        String SQL = "SELECT id,name,integrationid,fieldkey,fieldtype,custom,fielditems,createdat"
                + " FROM " + company + ".jirafields "
                + criteria
                + " LIMIT " + pageSize
                + " OFFSET " + (pageNumber * pageSize);

        log.info("sql = " + SQL); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbJiraField> retval = template.query(SQL, params, JIRA_FIELD_ROW_MAPPER);

        String countSQL = "SELECT COUNT(*) FROM " + company + ".jirafields " + criteria;
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
    public DbListResponse<DbJiraField> list(String company, Integer pageNumber, Integer pageSize)
            throws SQLException {
        return listByFilter(company, null, null, null, null,
                null, pageNumber, pageSize);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + company + ".jirafields(\n" +
                "    id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,\n" +
                "    name VARCHAR NOT NULL,\n" +
                "    custom BOOLEAN NOT NULL,\n" +
                "    integrationid INTEGER NOT NULL REFERENCES " + company + ".integrations(id) ON DELETE CASCADE,\n" +
                "    fieldkey VARCHAR NOT NULL,\n" +
                "    fieldtype VARCHAR NOT NULL,\n" +
                "    fielditems VARCHAR,\n" +
                "    createdat BIGINT DEFAULT extract(epoch from now())\n" +
                ")";
        String sqlIndexCreation = "CREATE UNIQUE INDEX IF NOT EXISTS uniq_jirafields_compound_idx on "
                + company + ".jirafields (integrationid,fieldkey)";

        template.getJdbcTemplate().execute(sql);
        template.getJdbcTemplate().execute(sqlIndexCreation);
        return true;
    }
}