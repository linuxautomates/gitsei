package io.levelops.commons.databases.services;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.zendesk.DbZendeskField;
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
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Service
public class ZendeskFieldService extends DatabaseService<DbZendeskField>{

    private static final String ZENDESK_FIELDS = "zendesk_fields";

    private static final RowMapper<DbZendeskField> ZENDESK_FIELD_ROW_MAPPER = (rs, rowNumber) -> DbZendeskField.builder()
            .fieldId(rs.getLong("field_id"))
            .title(rs.getString("title"))
            .integrationId(rs.getString("integrationid"))
            .fieldType(rs.getString("fieldtype"))
            .description(rs.getString("description"))
            .createdAt(rs.getLong("createdat"))
            .build();

    private final NamedParameterJdbcTemplate template;

    @Autowired
    public ZendeskFieldService(DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public String insert(String company, DbZendeskField zendeskField) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public List<String> batchUpsert(String company, List<DbZendeskField> zendeskFields) throws SQLException {
        String SQL = "INSERT INTO " + company + "." + ZENDESK_FIELDS + "(field_id,title,integrationid,fieldtype,description)"
                + " VALUES(?,?,?,?,?) ON CONFLICT (integrationid,field_id)"
                + " DO UPDATE SET (title,fieldtype,description) ="
                + " (EXCLUDED.title,EXCLUDED.fieldtype,EXCLUDED.description)"
                + " RETURNING id";

        List<String> ids = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            for (DbZendeskField field : zendeskFields) {
                pstmt.setObject(1, field.getFieldId());
                pstmt.setString(2, field.getTitle());
                pstmt.setObject(3, NumberUtils.toInt(field.getIntegrationId()));
                pstmt.setString(4, field.getFieldType());
                if (StringUtils.isEmpty(field.getDescription())) {
                    pstmt.setNull(5, Types.VARCHAR);
                } else {
                    pstmt.setString(5, field.getDescription());
                }
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
    public Boolean update(String company, DbZendeskField field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DbZendeskField> get(String company, String param) {
        throw new UnsupportedOperationException();
    }

    public DbListResponse<DbZendeskField> listByFilter(String company,
                                                       List<String> integrationIds,
                                                       String partialTitle,
                                                       String title,
                                                       List<String> fieldId,
                                                       List<String> createdAt,
                                                       Integer pageNumber,
                                                       Integer pageSize)
            throws SQLException {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            conditions.add("integrationid IN (:integrationIds)");
            params.put("integrationIds", integrationIds.stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (StringUtils.isNotEmpty(title)) {
            conditions.add("title = :title");
            params.put("title", title);
        } else if (StringUtils.isNotEmpty(partialTitle)) {
            conditions.add("title ILIKE :title");
            params.put("title", partialTitle + "%");
        }
        if (CollectionUtils.isNotEmpty(fieldId)) {
            conditions.add("field_id IN (:fieldId)");
            params.put("fieldId", fieldId.stream().map(NumberUtils::toLong).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(createdAt)) {
            conditions.add("createdat IN (:createdat)");
            params.put("createdat", createdAt);
        }

        String criteria = conditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", conditions);
        String SQL = "SELECT field_id,title,integrationid,fieldtype,description,createdat"
                + " FROM " + company + "." + ZENDESK_FIELDS + " "
                + criteria
                + " LIMIT " + pageSize
                + " OFFSET " + (pageNumber * pageSize);

        log.info("sql = " + SQL); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbZendeskField> retval = template.query(SQL, params, ZENDESK_FIELD_ROW_MAPPER);

        String countSQL = "SELECT COUNT(*) FROM " + company + "." + ZENDESK_FIELDS + " " + criteria;
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
    public DbListResponse<DbZendeskField> list(String company, Integer pageNumber, Integer pageSize)
            throws SQLException {
        return listByFilter(company, null, null, null, null, null, pageNumber, pageSize);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + company + "." + ZENDESK_FIELDS + "(\n" +
                "    id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,\n" +
                "    field_id BIGINT NOT NULL,\n" +
                "    title VARCHAR NOT NULL,\n" +
                "    integrationid INTEGER NOT NULL REFERENCES " + company + ".integrations(id) ON DELETE CASCADE,\n" +
                "    fieldtype VARCHAR NOT NULL,\n" +
                "    description VARCHAR,\n" +
                "    createdat BIGINT DEFAULT extract(epoch from now()),\n" +
                "    UNIQUE (integrationid,field_id)" +
                ")";
        template.getJdbcTemplate().execute(sql);
        return true;
    }
}
