package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.TriageRule;
import io.levelops.commons.databases.utils.TransactionCallback;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Log4j2
public class TriageRulesService extends DatabaseService<TriageRule> {

    private final NamedParameterJdbcTemplate template;

    @Autowired
    public TriageRulesService(DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    public static RowMapper<TriageRule> rowMapper() {
        return (rs, rowNumber) -> {
            Map obj = Map.of();
            try {
                obj = DefaultObjectMapper.get().readValue(
                        MoreObjects.firstNonNull(rs.getString("metadata"), "{}"), Map.class);
            } catch (JsonProcessingException e) {
                log.warn("error parsing triage row: ", e);
            }
            return TriageRule.builder()
                    .id(rs.getString("id"))
                    .name(rs.getString("name"))
                    .owner(rs.getString("owner"))
                    .metadata(obj)
                    .application(rs.getString("application"))
                    .description(rs.getString("description"))
                    .regexes(Arrays.asList((String[]) rs.getArray("regexes").getArray()))
                    .createdAt(rs.getLong("created_at"))
                    .build();
        };
    }

    @Override
    public String insert(String company, TriageRule rule) throws SQLException {

        return template.getJdbcOperations().execute(TransactionCallback.of(conn -> {

            String SQL = "INSERT INTO " + company + ".triage_rules(name,regexes,owner," +
                    "application,description,metadata) VALUES(?,?,?,?,?,to_json(?::json))";

            try (PreparedStatement pstmt = conn.prepareStatement(SQL,
                    Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setObject(1, rule.getName());
                pstmt.setObject(2,
                        conn.createArrayOf("varchar", rule.getRegexes().toArray()));
                pstmt.setObject(3, rule.getOwner());
                pstmt.setObject(4, rule.getApplication());
                pstmt.setObject(5, rule.getDescription());
                pstmt.setObject(6, DefaultObjectMapper.get()
                        .writeValueAsString(MoreObjects.firstNonNull(rule.getMetadata(), Map.of())));

                int affectedRows = pstmt.executeUpdate();
                // check the affected rows
                if (affectedRows > 0) {
                    // get the ID back
                    try (ResultSet rs = pstmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            return rs.getString(1);
                        }
                    }
                }
            } catch (JsonProcessingException e) {
                log.warn("failed to insert triage rule.", e);
            }
            throw new SQLException("ERROR. Failed to insert triage rule.");
        }));
    }

    @Override
    public Boolean update(String company, TriageRule rule) {
        return template.getJdbcOperations().execute(TransactionCallback.of(conn -> {

            String SQL = "UPDATE " + company + ".triage_rules SET name = ? , regexes = ? , owner = ?"
                    + " , application = ? , description = ? , metadata = to_json(?::json) WHERE id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(SQL)) {
                pstmt.setObject(1, rule.getName());
                pstmt.setObject(2,
                        conn.createArrayOf("varchar", rule.getRegexes().toArray()));
                pstmt.setObject(3, rule.getOwner());
                pstmt.setObject(4, rule.getApplication());
                pstmt.setObject(5, rule.getDescription());
                pstmt.setObject(6, DefaultObjectMapper.get()
                        .writeValueAsString(MoreObjects.firstNonNull(rule.getMetadata(), Map.of())));
                pstmt.setObject(7, UUID.fromString(rule.getId()));

                return pstmt.executeUpdate() > 0;
            } catch (JsonProcessingException e) {
                log.warn("failed to update triage rule.", e);
            }
            return false;
        }));
    }

    @Override
    public Optional<TriageRule> get(String company, String ruleId) throws SQLException {
        return list(company, List.of(ruleId), null, null, null, 0, 1)
                .getRecords()
                .stream()
                .findFirst();
    }

    @Override
    public DbListResponse<TriageRule> list(String company, Integer pageNumber, Integer pageSize)
            throws SQLException {
        return list(company, null, null, null, null, pageNumber, pageSize);
    }

    public DbListResponse<TriageRule> list(String company,
                                           List<String> ids,
                                           List<String> applications,
                                           List<String> owners,
                                           String name,
                                           Integer pageNumber,
                                           Integer pageSize) throws SQLException {
        List<String> criteria = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        if (StringUtils.isNotEmpty(name)) {
            criteria.add("name ILIKE :name");
            params.put("name", "%" + name + "%");
        }
        if (CollectionUtils.isNotEmpty(ids)) {
            criteria.add("id IN ( :ids )");
            params.put("ids", ids.stream().map(UUID::fromString).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(applications)) {
            criteria.add("application IN ( :applications )");
            params.put("applications", applications);
        }
        if (CollectionUtils.isNotEmpty(owners)) {
            criteria.add("owner IN ( :owners )");
            params.put("owners", owners);
        }
        if (CollectionUtils.isNotEmpty(ids)) {
            criteria.add("id IN ( :ids )");
            params.put("ids", ids.stream().map(UUID::fromString).collect(Collectors.toList()));
        }
        params.put("offset", pageNumber * pageSize);
        params.put("limit", pageSize);
        String conditions = "";
        if (criteria.size() > 0)
            conditions = " WHERE " + StringUtils.join(criteria, " AND ");
        String sql = "SELECT * FROM " + company + ".triage_rules" + conditions
                + " ORDER BY created_at DESC LIMIT :limit OFFSET :offset";
        String countSql = "SELECT COUNT(*) FROM " + company + ".triage_rules" + conditions;
        List<TriageRule> results = template.query(sql, params, rowMapper());
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String SQL = "DELETE FROM " + company + ".triage_rules WHERE id = ?::uuid";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setObject(1, UUID.fromString(id));
            if (pstmt.executeUpdate() > 0) {
                return true;
            }
        }
        return false;
    }

    public int bulkDelete(String company, List<String> rulesIds) {
        if (CollectionUtils.isNotEmpty(rulesIds)) {
            Map<String, Object> params = Map.of("ids", rulesIds.stream().map(UUID::fromString)
                    .collect(Collectors.toList()));
            String SQL = "DELETE FROM " + company + ".triage_rules WHERE id IN (:ids)";
            return template.update(SQL, params) ;
        }
        return 0;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + company + ".triage_rules(\n" +
                "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                "    name VARCHAR NOT NULL,\n" +
                "    application VARCHAR NOT NULL DEFAULT '',\n" +
                "    owner VARCHAR NOT NULL DEFAULT '',\n" +
                "    description VARCHAR NOT NULL DEFAULT '',\n" +
                "    metadata JSONB,\n" +
                "    regexes VARCHAR[] NOT NULL DEFAULT '{}'::VARCHAR[],\n" +
                "    created_at BIGINT DEFAULT extract(epoch from now())\n" +
                ")";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.execute();
            return true;
        }
    }
}
