package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.StoredFilter;
import io.levelops.commons.databases.utils.TransactionCallback;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@Log4j2
public class StoredFiltersService extends DatabaseService<StoredFilter> {

    public static final Set<String> PARTIAL_MATCH_COLUMNS = Set.of("name", "description");
    public static String STORED_FILTERS_TABLE = "filters";

    private final NamedParameterJdbcTemplate template;

    @Autowired
    public StoredFiltersService(DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public String insert(String company, StoredFilter storedFilter) throws SQLException {
        return template.getJdbcOperations().execute(TransactionCallback.of(conn -> {
            String sql = "INSERT INTO " + company + "." + STORED_FILTERS_TABLE +
                    "(name, type, description, is_default, filter) VALUES(?,?,?,?,to_jsonb(?::jsonb)) ON CONFLICT (name, type)" +
                    " DO UPDATE SET (description, is_default, filter, updated_at) = (EXCLUDED.description,EXCLUDED.is_default," +
                    " EXCLUDED.filter, trunc(extract(epoch from now())))";
            log.info("StoredFiltersService: company {}, Attempting to insert filter : {}",
                    company, storedFilter.toString());
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, storedFilter.getName());
                pstmt.setString(2, storedFilter.getType());
                pstmt.setString(3, storedFilter.getDescription());
                pstmt.setBoolean(4, storedFilter.getIsDefault());
                pstmt.setObject(5, DefaultObjectMapper.get()
                        .writeValueAsString(MoreObjects.firstNonNull(storedFilter.getFilter(), Map.of())));
                if (storedFilter.getIsDefault()) {
                    removeDefault(company, conn);
                }
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    try (ResultSet rs = pstmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            return rs.getString(1);
                        }
                    }
                } else {
                    log.debug("Insert attempt failed for stored filter: {}", storedFilter.toString());
                }
            } catch (JsonProcessingException e) {
                log.warn("Failed to insert stored filter.", e);
                throw new SQLException("ERROR. Failed to insert stored filter.", e);
            }
            return null;
        }));
    }

    public void removeDefault(String company, Connection connection)
            throws SQLException {
        var update = "UPDATE " + company + "." + STORED_FILTERS_TABLE + " SET is_default = FALSE WHERE is_default = TRUE ";
        try (PreparedStatement updateStm = connection.prepareStatement(update)) {
            var count = updateStm.executeUpdate();
            if (count < 1) {
                log.warn("Unable to unset as default, maybe it was already updated just now...");
            }
        }
    }

    @Override
    public Boolean update(String company, StoredFilter t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<StoredFilter> get(String company, String name) throws SQLException {
        return Optional.empty();
    }

    public Optional<StoredFilter> get(String company, String type, String name) throws SQLException {
        Validate.notBlank(name, "Missing name.");
        String sql = "SELECT * FROM " + company + "." + STORED_FILTERS_TABLE
                + " WHERE name = :name AND type = :type ";
        Map<String, Object> params = new HashMap<>();
        params.put("name", name);
        params.put("type", type);
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<StoredFilter> data = template.query(sql, params, rowMapper());
        return data.stream().findFirst();
    }

    public static RowMapper<StoredFilter> rowMapper() {
        return (rs, rowNumber) -> {
            Map<String, Object> filter = Map.of();
            try {
                filter = DefaultObjectMapper.get().readValue(
                        MoreObjects.firstNonNull(rs.getString("filter"), "{}"), Map.class);
            } catch (JsonProcessingException e) {
                log.warn("error parsing stored filter for " + rs.getString("name"), e);
            }
            return StoredFilter.builder()
                    .id(rs.getString("id"))
                    .name(rs.getString("name"))
                    .type(rs.getString("type"))
                    .description(rs.getString("description"))
                    .isDefault(rs.getBoolean("is_default"))
                    .filter(filter)
                    .createdAt(rs.getLong("created_at"))
                    .updatedAt(rs.getLong("updated_at"))
                    .build();
        };
    }

    @Override
    public DbListResponse<StoredFilter> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public DbListResponse<StoredFilter> list(String company,
                                             List<String> types,
                                             List<String> names,
                                             Boolean isDefault,
                                             Map<String, Map<String, String>> partialMatch,
                                             Integer pageNumber,
                                             Integer pageSize) throws SQLException {
        List<String> criteria = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        if (CollectionUtils.isNotEmpty(names)) {
            criteria.add("name IN ( :names )");
            params.put("names", names);
        }
        if (CollectionUtils.isNotEmpty(types)) {
            criteria.add("type IN ( :types )");
            params.put("types", types);
        }
        if (Objects.nonNull(isDefault)) {
            criteria.add("is_default = ( :isDefault )");
            params.put("isDefault", isDefault);
        }
        if (MapUtils.isNotEmpty(partialMatch)) {
            createPartialMatchFilter(partialMatch, criteria, params);
        }
        params.put("offset", pageNumber * pageSize);
        params.put("limit", pageSize);
        String conditions = "";
        if (criteria.size() > 0)
            conditions = " WHERE " + StringUtils.join(criteria, " AND ");
        String sql = "SELECT * FROM " + company + "." + STORED_FILTERS_TABLE + conditions
                + " ORDER BY created_at DESC LIMIT :limit OFFSET :offset";
        String countSql = "SELECT COUNT(*) FROM " + company + "." + STORED_FILTERS_TABLE + conditions;
        List<StoredFilter> results = template.query(sql, params, rowMapper());
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }

    private void createPartialMatchFilter(Map<String, Map<String, String>> partialMatchMap,
                                          List<String> criteria,
                                          Map<String, Object> params) {
        for (Map.Entry<String, Map<String, String>> partialMatchEntry : partialMatchMap.entrySet()) {
            String key = partialMatchEntry.getKey();
            Map<String, String> value = partialMatchEntry.getValue();

            String begins = value.get("$begins");
            String ends = value.get("$ends");
            String contains = value.get("$contains");

            if (begins != null || ends != null || contains != null) {
                if (PARTIAL_MATCH_COLUMNS.contains(key)) {
                    createPartialMatchCondition(criteria, params, key, begins, ends, contains);
                }
            }
        }
    }

    private void createPartialMatchCondition(List<String> criteria, Map<String, Object> params,
                                             String key, String begins, String ends, String contains) {
        if (begins != null) {
            String beingsCondition = key + " SIMILAR TO :" + key + "_begins ";
            params.put(key + "_begins", begins + "%");
            criteria.add(beingsCondition);
        }

        if (ends != null) {
            String endsCondition = key + " SIMILAR TO :" + key + "_ends ";
            params.put(key + "_ends", "%" + ends);
            criteria.add(endsCondition);
        }

        if (contains != null) {
            String containsCondition = key + " SIMILAR TO :" + key + "_contains ";
            params.put(key + "_contains", "%" + contains + "%");
            criteria.add(containsCondition);
        }
    }

    @Override
    public Boolean delete(String company, String name) throws SQLException {
        return null;
    }

    public Boolean delete(String company, String type, String name) throws SQLException {
        String SQL = "DELETE FROM " + company + "." + STORED_FILTERS_TABLE + " WHERE name = ? AND type = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setObject(1, name);
            pstmt.setObject(2, type);
            if (pstmt.executeUpdate() > 0) {
                return true;
            }
        }
        return false;
    }

    public int bulkDelete(String company, String type, List<String> names) {
        if (CollectionUtils.isNotEmpty(names)) {
            Map<String, Object> params = new HashMap<>();
            params.put("names", names);
            params.put("types", type);
            String SQL = "DELETE FROM " + company + "." + STORED_FILTERS_TABLE + " WHERE name IN (:names) AND type in (:types)";
            return template.update(SQL, params);
        }
        return 0;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + company + "." + STORED_FILTERS_TABLE + "(\n " +
                "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                "    name VARCHAR NOT NULL,\n" +
                "    type VARCHAR NOT NULL DEFAULT '',\n" +
                "    description VARCHAR,\n" +
                "    is_default BOOLEAN,\n" +
                "    filter JSONB DEFAULT '{}'::jsonb,\n" +
                "    created_at BIGINT DEFAULT extract(epoch from now()),\n" +
                "    updated_at BIGINT DEFAULT extract(epoch from now()),\n" +
                "    UNIQUE (name, type)\n" +
                ")";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.execute();
            return true;
        }
    }
}
