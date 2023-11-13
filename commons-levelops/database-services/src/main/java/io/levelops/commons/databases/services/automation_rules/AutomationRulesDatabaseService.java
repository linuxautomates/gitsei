package io.levelops.commons.databases.services.automation_rules;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.database.ArrayWrapper;
import io.levelops.commons.database.DBUtils;
import io.levelops.commons.databases.models.database.automation_rules.AutomationRule;
import io.levelops.commons.databases.models.database.automation_rules.Criterea;
import io.levelops.commons.databases.models.database.automation_rules.ObjectType;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class AutomationRulesDatabaseService extends DatabaseService<AutomationRule> {
    private final NamedParameterJdbcTemplate template;
    private final ObjectMapper objectMapper;

    // region CSTOR
    @Autowired
    public AutomationRulesDatabaseService(DataSource dataSource, ObjectMapper objectMapper) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.objectMapper = objectMapper;
    }
    // endregion

    private void validateInput(AutomationRule t) {
        Validate.notNull(t, "Input Automation Rule cannot be null!");
        Validate.notNull(t.getObjectType(), "Object Type cannot be null");
        CollectionUtils.emptyIfNull(t.getCritereas())
                .stream().filter(Objects::nonNull)
                .forEach(c -> { Validate.isTrue(CollectionUtils.isNotEmpty(c.getRegexes()), "Criterea cannot have empty regexes " + c.toString());});
    }
    // region Insert
    @Override
    public String insert(String company, AutomationRule t) throws SQLException {
        validateInput(t);
        String sql = "INSERT INTO " + company + ".automation_rules " +
                "(name, description, source, owner, object_type, critereas)" +
                " VALUES " +
                "(:name, :description, :source, :owner, :object_type, :critereas::jsonb)";

        Map<String, Object> params;
        try {
            params = Map.of(
                    "name", t.getName(),
                    "description", t.getDescription(),
                    "source", t.getSource(),
                    "owner", t.getOwner(),
                    "object_type", t.getObjectType().toString(),
                    "critereas", objectMapper.writeValueAsString(t.getCritereas())
            );
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize template to JSON", e);
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(sql, new MapSqlParameterSource(params), keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            throw new SQLException("Failed to create automation rule! " + t.toString());
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }
    // endregion

    // region Update
    @Override
    public Boolean update(String company, AutomationRule t) throws SQLException {
        validateInput(t);
        String sql = "UPDATE " + company + ".automation_rules " +
                "SET name = :name, description = :description, source = :source, owner = :owner, object_type = :object_type, critereas = :critereas::jsonb, updated_at = now() " +
                "WHERE id = :id::uuid";

        Map<String, Object> params;
        try {
            params = Map.of(
                    "name", t.getName(),
                    "description", t.getDescription(),
                    "source", t.getSource(),
                    "owner", t.getOwner(),
                    "object_type", t.getObjectType().toString(),
                    "critereas", objectMapper.writeValueAsString(t.getCritereas()),
                    "id", t.getId().toString()
            );
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize template to JSON", e);
        }
        int updatedRows = template.update(sql, new MapSqlParameterSource(params));
        return updatedRows > 0;
    }
    // endregion

    // region Get List Common
    private String formatCriterea(String criterea, List<Object> values, String newCriterea){
        String result = criterea + ((values.size() ==0) ? "" : "AND ");
        result += newCriterea;
        return result;
    }
    public DbListResponse<AutomationRule> getBatch(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<ObjectType> objectTypes, String namePartial) throws SQLException {
        String selectSqlBase = "SELECT * FROM " + company + ".automation_rules";

        String orderBy = " ORDER BY created_at DESC ";
        String criteria = " WHERE ";
        List<Object> values = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(ids)) {
            criteria = formatCriterea(criteria, values, "id = ANY(?::uuid[]) ");
            values.add(new ArrayWrapper<>("uuid", ids));
        }
        if (CollectionUtils.isNotEmpty(objectTypes)) {
            criteria = formatCriterea(criteria, values, "object_type = ANY(?::varchar[]) ");
            values.add(new ArrayWrapper<>("varchar", objectTypes.stream().map(ObjectType::toString).collect(Collectors.toList())));
        }
        if(StringUtils.isNotBlank(namePartial)) {
            criteria = formatCriterea(criteria, values, "name ILIKE ? ");
            values.add("%" + namePartial + "%");
        }
        criteria = CollectionUtils.isEmpty(values) ? "" : criteria;

        String selectSql = selectSqlBase + criteria + orderBy + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSql = "SELECT COUNT(*) FROM (" +  selectSqlBase + criteria + ") AS counted";

        List<AutomationRule> retval = new ArrayList<>();
        Integer totCount = 0;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSql);
             PreparedStatement pstmt2 = conn.prepareStatement(countSql)) {
            for (int i = 0; i < values.size(); i++) {
                Object obj = DBUtils.processArrayValues(conn, values.get(i));
                pstmt.setObject(i + 1, obj);
                pstmt2.setObject(i + 1, obj);
            }
            log.debug("Get or List Query = {}", pstmt);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                UUID id = (UUID) rs.getObject("id");
                String name = rs.getString("name");
                String description = rs.getString("description");
                String source = rs.getString("source");
                String owner = rs.getString("owner");
                ObjectType objectType = ObjectType.fromString(rs.getString("object_type"));
                List<Criterea> critereas = null;
                try {
                    critereas = objectMapper.readValue(rs.getString("critereas"), objectMapper.getTypeFactory().constructCollectionLikeType(List.class, Criterea.class));
                } catch (JsonProcessingException e) {
                    throw new SQLException("Failed to convert values to string.", e);
                }

                Instant createdAt = DateUtils.toInstant(rs.getTimestamp("created_at"));
                Instant updatedAt = DateUtils.toInstant(rs.getTimestamp("updated_at"));


                AutomationRule automationRule = AutomationRule.builder()
                        .id(id)
                        .name(name).description(description)
                        .owner(owner).source(source)
                        .objectType(objectType)
                        .critereas(critereas)
                        .createdAt(createdAt).updatedAt(updatedAt)
                        .build();

                retval.add(automationRule);
            }
            if (retval.size() > 0) {
                totCount = retval.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
                if (retval.size() == pageSize) {
                    rs = pstmt2.executeQuery();
                    if (rs.next()) {
                        totCount = rs.getInt("count");
                    }
                }
            }
        }
        return DbListResponse.of(retval, totCount);

    }
    // endregion

    // region Get
    @Override
    public Optional<AutomationRule> get(String company, String id) throws SQLException {
        var results = getBatch(company, 0, 10, Collections.singletonList(UUID.fromString(id)), null, null).getRecords();
        return results.size() > 0 ? Optional.of(results.get(0)) : Optional.empty();
    }
    // endregion

    // region List
    @Override
    public DbListResponse<AutomationRule> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return getBatch(company, pageNumber, pageSize, null, null, null);
    }

    public DbListResponse<AutomationRule> listByFilter(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<ObjectType> objectTypes, String namePartial) throws SQLException {
        return getBatch(company, pageNumber, pageSize, ids, objectTypes,namePartial);
    }
    // endregion

    // region Delete
    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String sql = "DELETE FROM " + company + ".automation_rules WHERE id = :id::uuid";
        return template.update(sql, Map.of(
                "id", id
        )) > 0;
    }

    public int bulkDelete(String company, List<String> ids) throws SQLException {
        if (CollectionUtils.isNotEmpty(ids)) {
            String sql = "DELETE FROM " + company + ".automation_rules WHERE id IN (:ids)";
            return template.update(sql, Map.of("ids", ids.stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toList())));
        }
        return 0;
    }
    // endregion

    // region Ensure Table Exists
    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".automation_rules " +
                        "(" +
                        "   id           UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        "   name         TEXT NOT NULL," +
                        "   description  TEXT NOT NULL DEFAULT ''," +
                        "   source       TEXT NOT NULL DEFAULT ''," +
                        "   owner        TEXT NOT NULL DEFAULT ''," +
                        "   object_type  VARCHAR(64) NOT NULL," +
                        "   critereas    JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "   created_at   TIMESTAMPTZ NOT NULL DEFAULT now()," +
                        "   updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()" +
                        ")",

                "CREATE INDEX IF NOT EXISTS automation_rules_object_type_idx on " + company + "." + "automation_rules (object_type)",
                "CREATE INDEX IF NOT EXISTS automation_rules_name_idx on " + company + "." + "automation_rules (name)"
        );

        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
    // endregion
}
