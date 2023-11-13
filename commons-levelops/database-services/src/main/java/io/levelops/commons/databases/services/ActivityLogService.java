package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.ActivityLogConverters;
import io.levelops.commons.databases.models.database.ActivityLog;
import io.levelops.commons.databases.models.filters.ActivityLogsFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.database.ActivityLog.TargetItemType.USER_LOGIN;

@Log4j2
@Service
public class ActivityLogService extends DatabaseService<ActivityLog> {

    private static ObjectMapper objectMapper;
    private final NamedParameterJdbcTemplate template;
    private static final String ACTIVITY_LOGS_TABLE = "activitylogs";
    public static final Set<String> PARTIAL_MATCH_COLUMNS = Set.of("email");

    @Autowired
    public ActivityLogService(DataSource dataSource, ObjectMapper mapper) {
        super(dataSource);
        objectMapper = mapper;
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public String insert(String company, ActivityLog activityLog) throws SQLException {

        String SQL = "INSERT INTO " + company + "." + ACTIVITY_LOGS_TABLE + "(body,email,targetitem,action," +
                "details,itemtype) VALUES(?,?,?,?,to_json(?::json),?)";

        String retVal = null;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL,
                     Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, activityLog.getBody());
            pstmt.setString(2, activityLog.getEmail());
            pstmt.setString(3, activityLog.getTargetItem());
            pstmt.setString(4, String.valueOf(activityLog.getAction()));
            pstmt.setString(5,
                    objectMapper.writeValueAsString(activityLog.getDetails()));
            pstmt.setString(6,
                    String.valueOf(activityLog.getTargetItemType()));

            int affectedRows = pstmt.executeUpdate();
            // check the affected rows
            if (affectedRows > 0) {
                // get the ID back
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        retVal = rs.getString(1);
                    }
                }
            }
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to serialize acitivitylog details.", e);
        }
        return retVal;
    }

    @Override
    public Boolean update(String company, ActivityLog activityLog) {
        throw new UnsupportedOperationException("Activity Logs cannot be updated.");
    }

    @Override
    public Optional<ActivityLog> get(String company, String activityLogId) throws SQLException {
        String SQL = "SELECT id,body,email,targetitem,itemtype,action,details,createdat FROM "
                + company + "." + ACTIVITY_LOGS_TABLE + " WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setObject(1, UUID.fromString(activityLogId));
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(ActivityLog.builder()
                        .id(rs.getString("id"))
                        .body(rs.getString("body"))
                        .email(rs.getString("email"))
                        .targetItem(rs.getString("targetitem"))
                        .targetItemType(ActivityLog.TargetItemType.fromString(
                                rs.getString("itemtype")))
                        .createdAt(rs.getLong("createdat"))
                        .details(objectMapper.readValue(rs.getString("details"),
                                objectMapper.getTypeFactory().constructParametricType(Map.class,
                                        String.class, Object.class)))
                        .action(ActivityLog.Action.fromString(rs.getString("action")))
                        .build());
            }
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to parse details object.", e);
        }
        return Optional.empty();
    }

    @Override
    public DbListResponse<ActivityLog> list(String company, Integer pageNumber, Integer pageSize)
            throws SQLException {
        return list(company, null, null, null, null, pageNumber, pageSize);
    }

    public DbListResponse<ActivityLog> list(String company, List<String> targetItems, List<String> emails,
                                            List<String> actions, Map<String, Map<String, String>> partialMatch, Integer pageNumber, Integer pageSize)
            throws SQLException {
        Map<String, Object> params = new HashMap<>();
        Map<String, List<String>> conditions = createWhereClauseAndUpdateParams(params, targetItems, emails, actions, partialMatch);
        String whereClause = "";
        if (conditions.get(ACTIVITY_LOGS_TABLE).size() > 0) {
            whereClause = " WHERE " + String.join(" AND ", conditions.get(ACTIVITY_LOGS_TABLE));
        }
        String sql = "SELECT id,body,email,targetitem,itemtype,action,details,createdat FROM "
                + company + "." + ACTIVITY_LOGS_TABLE + " " + whereClause + " ORDER BY createdat DESC LIMIT "
                + pageSize + " OFFSET " + (pageNumber * pageSize);
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);

        List<ActivityLog> retval = template.query(sql, params, ActivityLogConverters.listRowMapper());

        String countSQL = "SELECT COUNT(*) FROM " + company + "." + ACTIVITY_LOGS_TABLE + " " + whereClause;
        Integer totCount = 0;
        if (retval.size() > 0) {
            totCount = retval.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
            if (retval.size() == pageSize) {
                totCount = MoreObjects.firstNonNull(template.queryForObject(countSQL, params, Integer.class), totCount);
            }
        }
        return DbListResponse.of(retval, totCount);
    }

    public DbListResponse<ActivityLog> listMostRecentLogins(String company, List<ActivityLog.Action> actions, List<String> emailsToExclude) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        conditions.add("itemtype in (:itemType)");
        params.put("itemType", USER_LOGIN.toString());

        if (CollectionUtils.isNotEmpty(actions)) {
            conditions.add("action IN (:actions)");
            params.put("actions", actions.stream().map(ActivityLog.Action::toString).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(emailsToExclude)) {
            conditions.add("email !~* (:emailsToExclude)");
            params.put("emailsToExclude", String.join("|", emailsToExclude));
        }

        String whereClause = " WHERE " + String.join(" AND ", conditions);

        String sql = "SELECT id,body,email,targetitem,itemtype,action,details,createdat FROM "
                + company + "." + ACTIVITY_LOGS_TABLE + " " + whereClause + " ORDER BY createdat DESC LIMIT 5";

        log.info("sql = " + sql);
        log.info("params = {}", params);

        List<ActivityLog> retval = template.query(sql, params, ActivityLogConverters.listRowMapper());
        return DbListResponse.of(retval, retval.size());
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculate(String company, ActivityLogsFilter filter) {
        ActivityLogsFilter.DISTINCT distinct = filter.getAcross();
        Validate.notNull(distinct, "Across must be present for group by query");
        Map<String, Object> params = new HashMap<>();
        Map<String, List<String>> conditions = createWhereClauseAndUpdateParams(params, filter.getTargetItems(),
                filter.getEmails(), filter.getActions(), filter.getPartialMatch());
        String whereClause = "";
        if (conditions.get(ACTIVITY_LOGS_TABLE).size() > 0) {
            whereClause = " WHERE " + String.join(" AND ", conditions.get(ACTIVITY_LOGS_TABLE));
        }
        String groupByKey;
        switch (distinct) {
            case email:
                groupByKey = " email ";
                break;
            case targetitem:
                groupByKey = " targetitem ";
                break;
            default:
                groupByKey = distinct.name();
        }
        String groupBySql = distinct.equals(ActivityLogsFilter.DISTINCT.none) ? "" : " GROUP BY " + groupByKey;
        String orderByClause = " ORDER BY ct desc";
        String limitString = "";
        String sql = "SELECT " + groupByKey + ", COUNT(id) as ct FROM " + company + "." + ACTIVITY_LOGS_TABLE
                + whereClause + groupBySql + orderByClause + limitString;
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        final List<DbAggregationResult> aggregationResults = template.query(sql, params, ActivityLogConverters.aggRowMapper(distinct.toString()));

        return DbListResponse.of(aggregationResults, aggregationResults.size());
    }

    private Map<String, List<String>> createWhereClauseAndUpdateParams(Map<String, Object> params, List<String> targetItems,
                                                                       List<String> emails, List<String> actions,
                                                                       Map<String, Map<String, String>> partialMatch) {
        List<String> conditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(targetItems)) {
            conditions.add("targetitem IN (:targetItems)");
            params.put("targetItems", targetItems);
        }
        if (CollectionUtils.isNotEmpty(emails)) {
            conditions.add("email IN (:emails)");
            params.put("emails", emails);
        }
        if (CollectionUtils.isNotEmpty(actions)) {
            conditions.add("action IN (:actions)");
            params.put("actions", actions);
        }
        if (MapUtils.isNotEmpty(partialMatch)) {
            createPartialMatchFilter(partialMatch, conditions, params);
        }
        return Map.of(ACTIVITY_LOGS_TABLE, conditions);
    }

    @Override
    public Boolean delete(String company, String id) {
        throw new UnsupportedOperationException("Activity Logs cannot be deleted.");
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
    public Boolean ensureTableExistence(String company) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + company + "." + ACTIVITY_LOGS_TABLE + "(\n" +
                "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                "    body VARCHAR NOT NULL,\n" +
                "    email VARCHAR NOT NULL,\n" +
                "    targetitem VARCHAR NOT NULL,\n" +
                "    itemtype VARCHAR NOT NULL,\n" +
                "    details JSONB NOT NULL DEFAULT '{}'::jsonb,\n" +
                "    action VARCHAR NOT NULL,\n" +
                "    createdat BIGINT DEFAULT extract(epoch from now())\n" +
                ")";
        String sqlIndexCreation = "CREATE INDEX IF NOT EXISTS activitylogs_targetitem_type_idx on "
                + company + "." + ACTIVITY_LOGS_TABLE + " (targetitem,itemtype)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             PreparedStatement indexPstmt = conn.prepareStatement(sqlIndexCreation)) {
            pstmt.execute();
            indexPstmt.execute();
            return true;
        }
    }
}
