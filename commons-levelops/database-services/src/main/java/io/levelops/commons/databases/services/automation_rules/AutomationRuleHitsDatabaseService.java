package io.levelops.commons.databases.services.automation_rules;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.database.ArrayWrapper;
import io.levelops.commons.database.DBUtils;
import io.levelops.commons.databases.models.database.automation_rules.AutomationRuleHit;
import io.levelops.commons.databases.models.database.automation_rules.ObjectType;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class AutomationRuleHitsDatabaseService extends DatabaseService<AutomationRuleHit> {
    private final NamedParameterJdbcTemplate template;
    private final ObjectMapper objectMapper;

    // region CSTOR
    @Autowired
    public AutomationRuleHitsDatabaseService(DataSource dataSource, ObjectMapper objectMapper) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.objectMapper = objectMapper;
    }
    // endregion

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(AutomationRulesDatabaseService.class);
    }

    private void validateInput(AutomationRuleHit t) {
        Validate.notNull(t, "Input Automation Rule Hit cannot be null!");
        Validate.notBlank(t.getObjectId(), "Object Id cannot be blank");
        Validate.notNull(t.getObjectType(), "Object Type cannot be null");
        Validate.notNull(t.getRuleId(), "Rule Id cannot be null");
    }
    // region Insert
    @Override
    public String insert(String company, AutomationRuleHit t) throws SQLException {
        validateInput(t);
        String sql = "INSERT INTO " + company + ".automation_rule_hits " +
                "(object_type, object_id, rule_id, count, hit_content, context)" +
                " VALUES (:object_type, :object_id, :rule_id::uuid, :count, :hit_content, :context::jsonb)";

        Map<String, Object> params;
        try {
            params = Map.of(
                    "object_type", t.getObjectType().toString(),
                    "object_id", t.getObjectId(),
                    "rule_id", t.getRuleId(),
                    "count", t.getCount(),
                    "hit_content", t.getHitContent(),
                    "context", objectMapper.writeValueAsString(t.getContext())
            );
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize context to JSON", e);
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(sql, new MapSqlParameterSource(params), keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            throw new SQLException("Failed to create automation rule hit! " + t.toString());
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }
    // endregion

    // region Insert
    public String upsert(String company, AutomationRuleHit t) throws SQLException {
        validateInput(t);
        String sql = "INSERT INTO " + company + ".automation_rule_hits " +
                "(object_type, object_id, rule_id, count, hit_content, context)" +
                " VALUES (:object_type, :object_id, :rule_id::uuid, :count, :hit_content, :context::jsonb)" +
                "ON CONFLICT(object_type,object_id,rule_id) DO UPDATE SET (count, hit_content, context, updated_at) = (EXCLUDED.count, EXCLUDED.hit_content, EXCLUDED.context,now())";

        Map<String, Object> params;
        try {
            params = Map.of(
                    "object_type", t.getObjectType().toString(),
                    "object_id", t.getObjectId(),
                    "rule_id", t.getRuleId(),
                    "count", t.getCount(),
                    "hit_content", t.getHitContent(),
                    "context", objectMapper.writeValueAsString(t.getContext())
            );
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize context to JSON", e);
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(sql, new MapSqlParameterSource(params), keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            throw new SQLException("Failed to create automation rule hit! " + t.toString());
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }
    // endregion

    // region Update
    @Override
    public Boolean update(String company, AutomationRuleHit t) throws SQLException {
        validateInput(t);
        String sql = "UPDATE " + company + ".automation_rule_hits " +
                "SET object_type = :object_type, object_id = :object_id, rule_id = :rule_id::uuid, count = :count, hit_content = :hit_content, context = :context::jsonb, updated_at = now() " +
                "WHERE id = :id::uuid";

        Map<String, Object> params;
        try {
            params = Map.of(
                    "object_type", t.getObjectType().toString(),
                    "object_id", t.getObjectId(),
                    "rule_id", t.getRuleId(),
                    "count", t.getCount(),
                    "hit_content", t.getHitContent(),
                    "context", objectMapper.writeValueAsString(t.getContext()),
                    "id", t.getId().toString()
            );
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize context to JSON", e);
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
    public DbListResponse<AutomationRuleHit> getBatch(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<ObjectType> objectTypes, List<String> objectIds,
                                                      List<ImmutablePair<ObjectType, String>> objectTypeIdPairs, List<UUID> ruleIds) throws SQLException {
        String selectSqlBase = "SELECT rh.*, r.name as rule_name FROM " + company + ".automation_rule_hits as rh " +
                "JOIN " + company + ".automation_rules as r on r.id = rh.rule_id ";

        String orderBy = " ORDER BY created_at DESC ";
        String criteria = " WHERE ";
        List<Object> values = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(ids)) {
            criteria = formatCriterea(criteria, values, "rh.id = ANY(?::uuid[]) ");
            values.add(new ArrayWrapper<>("uuid", ids));
        }

        if (CollectionUtils.isNotEmpty(objectTypes)) {
            criteria = formatCriterea(criteria, values, "rh.object_type = ANY(?) ");
            values.add(new ArrayWrapper<>("varchar", objectTypes.stream().distinct().map(x -> x.toString()).collect(Collectors.toList())));
        }
        if (CollectionUtils.isNotEmpty(objectIds)) {
            criteria = formatCriterea(criteria, values, "rh.object_id = ANY(?) ");
            values.add(new ArrayWrapper<>("varchar", objectIds));
        }

        if (CollectionUtils.isNotEmpty(objectTypeIdPairs)) {
            List<String> objectTypeIdsCriterea = new ArrayList<>();
            for(int i=0; i< objectTypeIdPairs.size(); i++){
                ImmutablePair<ObjectType, String> pair = objectTypeIdPairs.get(i);
                if(pair.getLeft() == null){
                    log.warn("object type is null skipping it!! {}", pair);
                    continue;
                }
                if(StringUtils.isBlank(pair.getRight())){
                    log.warn("object id is null or empty skipping it!! {}", pair);
                    continue;
                }
                objectTypeIdsCriterea.add("(rh.object_type = ? AND rh.object_id = ?)");
                values.add(pair.getLeft().toString());
                values.add(pair.getRight());
            }
            if(CollectionUtils.isNotEmpty(objectTypeIdsCriterea)) {
                criteria += "( " + String.join(" OR ", objectTypeIdsCriterea) + " )";
            }
        }

        if (CollectionUtils.isNotEmpty(ruleIds)) {
            criteria = formatCriterea(criteria, values, "rh.rule_id = ANY(?::uuid[]) ");
            values.add(new ArrayWrapper<>("uuid", ruleIds));
        }
        criteria = CollectionUtils.isEmpty(values) ? "" : criteria;

        String selectSql = selectSqlBase + criteria + orderBy + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSql = "SELECT COUNT(*) FROM (" +  selectSqlBase + criteria + ") AS counted";

        List<AutomationRuleHit> retval = new ArrayList<>();
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
                String objectId = rs.getString("object_id");
                ObjectType objectType = ObjectType.fromString(rs.getString("object_type"));
                UUID ruleId = (UUID) rs.getObject("rule_id");
                String ruleName = rs.getString("rule_name");
                Integer count = rs.getInt("count");
                String hitContent = rs.getString("hit_content");

                Map<String, Object> context = null;
                try {
                    context = objectMapper.readValue(rs.getString("context"),objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
                } catch (JsonProcessingException e) {
                    throw new SQLException("Failed to convert context to string.", e);
                }

                Instant createdAt = DateUtils.toInstant(rs.getTimestamp("created_at"));
                Instant updatedAt = DateUtils.toInstant(rs.getTimestamp("updated_at"));


                AutomationRuleHit ruleHit = AutomationRuleHit.builder()
                        .id(id)
                        .objectId(objectId).objectType(objectType)
                        .ruleId(ruleId).ruleName(ruleName)
                        .count(count).hitContent(hitContent).context(context)
                        .createdAt(createdAt).updatedAt(updatedAt)
                        .build();

                retval.add(ruleHit);
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
    public Optional<AutomationRuleHit> get(String company, String id) throws SQLException {
        var results = getBatch(company, 0, 10, Collections.singletonList(UUID.fromString(id)), null, null, null, null).getRecords();
        return results.size() > 0 ? Optional.of(results.get(0)) : Optional.empty();
    }
    // endregion

    // region List
    @Override
    public DbListResponse<AutomationRuleHit> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return getBatch(company, pageNumber, pageSize, null, null, null, null, null);
    }

    public DbListResponse<AutomationRuleHit> listByFilter(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<ObjectType> objectTypes, List<String> objectIds,
                                                          List<ImmutablePair<ObjectType, String>> objectTypeIdPairs, List<UUID> ruleIds) throws SQLException {
        return getBatch(company, pageNumber, pageSize, ids, objectTypes, objectIds, objectTypeIdPairs, ruleIds);
    }
    // endregion

    // region Delete
    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String sql = "DELETE FROM " + company + ".automation_rule_hits WHERE id = :id::uuid";
        return template.update(sql, Map.of(
                "id", id
        )) > 0;
    }
    // endregion

    // region Ensure Table Exists
    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".automation_rule_hits " +
                        "(" +
                        "   id           UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        "   object_id    TEXT NOT NULL," +
                        "   object_type  VARCHAR(64) NOT NULL," +
                        "   rule_id      UUID NOT NULL REFERENCES " + company + ".automation_rules(id) ON DELETE CASCADE," +
                        "   count        INTEGER NOT NULL," +
                        "   hit_content  VARCHAR NOT NULL,\n" +
                        "   context      JSONB NOT NULL DEFAULT '{}',\n" +
                        "   created_at   TIMESTAMPTZ NOT NULL DEFAULT now()," +
                        "   updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()" +
                        ")",
                "CREATE UNIQUE INDEX IF NOT EXISTS uniq_automation_rule_hits_object_type_object_id_rule_id_idx on " + company + ".automation_rule_hits (object_type, object_id, rule_id)",
                "CREATE INDEX IF NOT EXISTS automation_rule_hits_object_type_object_id_idx on " + company + "." + "automation_rule_hits (object_type, object_id)",
                "CREATE INDEX IF NOT EXISTS automation_rule_hits_rule_id_idx on " + company + "." + "automation_rule_hits (rule_id)"
        );

        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
    // endregion
}
