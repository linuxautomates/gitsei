package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.database.ArrayWrapper;
import io.levelops.commons.database.DBUtils;
import io.levelops.commons.databases.converters.CICDInstanceConverters;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CiCdInstanceConfig;
import io.levelops.commons.databases.models.database.CiCdInstanceDetails;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.filters.CICDInstanceFilter;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.BulkUpdateResponse;
import io.levelops.commons.models.CICDInstanceIntegAssignment;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.UpdateResponse;
import io.levelops.commons.utils.UUIDUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Service
public class CiCdInstancesDatabaseService extends DatabaseService<CICDInstance> {
    private static final String INSERT_SQL_FORMAT = "INSERT INTO {0}.cicd_instances (id,name,url,last_hb_at,config_updated_at,integration_id,type,config) \n" +
            "VALUES(:id,:name,:url,:lastHbAt,:configUpdatedAt,:integrationId,:type,:config::jsonb) " 
            + "ON CONFLICT(id) DO UPDATE SET (name, url, last_hb_at, config_updated_at, updated_at, config) \n" +
            "= (EXCLUDED.name, EXCLUDED.url, EXCLUDED.last_hb_at, now(), now(), EXCLUDED.config)";
    
    private static final String UPSERT_SQL_FORMAT = "INSERT INTO {0}.cicd_instances (id,name,url,type,integration_id) "
                                                    + "VALUES (:id,:name,:url,:type,:integrationId) "
                                                    + "ON CONFLICT(id) DO UPDATE SET (name,url,updated_at) = (EXCLUDED.name, EXCLUDED.url, now())";
    
    private static final String DELETE_SQL_FORMAT = "DELETE FROM %s.cicd_instances WHERE id = ?";
    public static final Set<String> PARTIAL_MATCH_COLUMNS = Set.of("name");
    public static final Set<String> PARTIAL_MATCH_ARRAY_COLUMNS = Set.of();
    private static final Set<String> SORTABLE_COLUMNS = Set.of("created_at", "updated_at", "name");
    private static final String INSTANCES_TABLE = "cicd_instances";
    private static final int DEFAULT_PAGE_NUMBER = 0;
    private static final int DEFAULT_PAGE_SIZE = 25;

    private final NamedParameterJdbcTemplate template;
    private final IntegrationService integrationService;

    // region CSTOR
    @Autowired
    public CiCdInstancesDatabaseService(DataSource dataSource) {
        super(dataSource);
        integrationService = new IntegrationService(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
    }
    // endregion

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    // region insert
    @Override
    public String insert(String company, CICDInstance cicdInstance) throws SQLException {
        try {
            var configJson = "{}";
            if (cicdInstance.getConfig() != null){
                try {
                    configJson = DefaultObjectMapper.get().writeValueAsString(cicdInstance.getConfig());
                } catch (JsonProcessingException e) {
                    log.warn("Failed to serialize config json. will store empty json.", e);
                }
            }
            int affectedRows = template.update(
                MessageFormat.format(INSERT_SQL_FORMAT, company), 
                new MapSqlParameterSource()
                    .addValue("id", cicdInstance.getId())
                    .addValue("name", cicdInstance.getName())
                    .addValue("url", cicdInstance.getUrl())
                    .addValue("lastHbAt", Timestamp.from(cicdInstance.getLastHeartbeatAt() != null ? cicdInstance.getLastHeartbeatAt() : Instant.now() ))
                    .addValue("configUpdatedAt", Timestamp.from(Instant.now()))
                    .addValue("integrationId", StringUtils.isNotEmpty(cicdInstance.getIntegrationId()) ? NumberUtils.toInt(cicdInstance.getIntegrationId()) : null)
                    .addValue("type", cicdInstance.getType())
                    .addValue("config", configJson));
            // check the affected rows
            if (affectedRows <= 0) {
                throw new SQLException("Failed to create cicd instance!");
            }
            return cicdInstance.getId().toString();
        } catch (SQLException e) {
            log.error("[{}] Error inserting CicdInstance: {}", company, cicdInstance);
            throw new RuntimeException("Error processing details in the database..", e);
        }
    }
    // endregion

    // region update
    @Override
    public Boolean update(String company, CICDInstance t) throws SQLException {
        final String UPDATE_SQL_FORMAT = "UPDATE %s.cicd_instances SET  last_hb_at = ?, details = to_json(?::json), updated_at = ?," +
                "name = ?, url = ?  WHERE id = ? ";
        String updateSql = String.format(UPDATE_SQL_FORMAT, company);
        boolean success;
        Timestamp timestamp = Timestamp.from(t.getLastHeartbeatAt());
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setTimestamp(1, timestamp);
            pstmt.setObject(2, DefaultObjectMapper.get().writeValueAsString(t.getDetails()));
            pstmt.setObject(3, timestamp);
            pstmt.setObject(4, t.getName());
            pstmt.setObject(5, t.getUrl());
            pstmt.setObject(6, t.getId());
            int affectedRows = pstmt.executeUpdate();
            success = (affectedRows > 0);
            return success;
        } catch (JsonProcessingException e) {
            log.error("Error processing JSON for Heartbeat timestamp with instanceId {} for company {}", t.getId(), company);
            throw new RuntimeException("Error processing details in the database..", e);
        }
    }
    // endregion

    // region get and list commons
    private String formatCriterea(String criterea, List<Object> values, String newCriterea) {
        String result = criterea + ((values.size() == 0) ? "" : "AND ");
        result += newCriterea;
        return result;
    }

    public String upsert(String company, CICDInstance cicdInstance) {
        try {
            var affectedRows = template.update(
                MessageFormat.format(UPSERT_SQL_FORMAT, company),
                    new MapSqlParameterSource()
                            .addValue("id", cicdInstance.getId())
                            .addValue("name", cicdInstance.getName())
                            .addValue("type", cicdInstance.getType())
                            .addValue("url", cicdInstance.getUrl())
                            .addValue("integrationId", StringUtils.isNotEmpty(cicdInstance.getIntegrationId()) ? NumberUtils.toInt(cicdInstance.getIntegrationId()) : null));
            
            if (affectedRows <= 0) {
                throw new SQLException("Failed to create cicd instance!");
            }
            return cicdInstance.getId().toString();
        } catch (SQLException e) {
            log.error("[{}] Error upserting CicdInstance: {}", company, cicdInstance);
            throw new RuntimeException("Error processing details in the database..", e);
        }
    }

    private DbListResponse<CICDInstance> getBatch(String company, Integer pageNumber, Integer pageSize, List<UUID> ids) throws SQLException {
        String selectSqlBase = "SELECT * FROM " + company + ".cicd_instances";

        String orderBy = " ORDER BY created_at DESC ";
        String criteria = " WHERE ";
        List<Object> values = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(ids)) {
            criteria = formatCriterea(criteria, values, "id = ANY(?::uuid[]) ");
            values.add(new ArrayWrapper<>("uuid", ids));
        }
        criteria = CollectionUtils.isEmpty(values) ? "" : criteria;

        String selectSql = selectSqlBase + criteria + orderBy + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSql = "SELECT COUNT(*) FROM (" + selectSqlBase + criteria + ") AS counted";

        List<CICDInstance> retval = new ArrayList<>();
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
                String url = rs.getString("url");
                String integrationId = rs.getString("integration_id");
                String type = rs.getString("type");
                Instant lastHbAt = DateUtils.toInstant(rs.getTimestamp("last_hb_at"));
                Instant createdAt = DateUtils.toInstant(rs.getTimestamp("created_at"));
                Instant updatedAt = DateUtils.toInstant(rs.getTimestamp("updated_at"));
                CiCdInstanceConfig config = parseConfig(company, id, DefaultObjectMapper.get(), rs.getString("config"));
                CiCdInstanceDetails instDetails = parseInstDetails(company, id, DefaultObjectMapper.get(), rs.getString("details"));
                Instant configUpdatedAt = DateUtils.toInstant(rs.getTimestamp("config_updated_at"));
                CICDInstance aggregationRecord = CICDInstance.builder()
                        .id(id)
                        .name(name)
                        .url(url)
                        .integrationId(integrationId)
                        .type(type)
                        .config(config)
                        .details(instDetails)
                        .lastHeartbeatAt(lastHbAt)
                        .configUpdatedAt(configUpdatedAt)
                        .createdAt(createdAt)
                        .updatedAt(updatedAt)
                        .build();
                retval.add(aggregationRecord);
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
        }
        return DbListResponse.of(retval, totCount);
    }

    // endregion

    // region get
    @Override
    public Optional<CICDInstance> get(String company, String id) throws SQLException {
        var results = getBatch(company, 0, 10, Collections.singletonList(UUID.fromString(id))).getRecords();
        return results.size() > 0 ? Optional.of(results.get(0)) : Optional.empty();
    }
    // endregion

    // region list
    @Override
    public DbListResponse<CICDInstance> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return getBatch(company, pageNumber, pageSize, null);
    }

    public DbListResponse<CICDInstance> listByFilter(String company, Integer pageNumber, Integer pageSize, List<UUID> ids) throws SQLException {
        return getBatch(company, pageNumber, pageSize, ids);
    }

    public DbListResponse<CICDInstance> list(String company,
                                             CICDInstanceFilter filter,
                                             Map<String, SortingOrder> sortBy,
                                             Integer pageNumber,
                                             Integer pageSize) throws SQLException {
        sortBy = MapUtils.emptyIfNull(sortBy);
        pageNumber = MoreObjects.firstNonNull(pageNumber, DEFAULT_PAGE_NUMBER);
        pageSize = MoreObjects.firstNonNull(pageSize, DEFAULT_PAGE_SIZE);
        Map<String, Object> params = new HashMap<>();
        Map<String, List<String>> conditions = createWhereClauseAndUpdateParams(params, filter);
        params.put("offset", pageNumber * pageSize);
        params.put("limit", pageSize);
        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (SORTABLE_COLUMNS.contains(entry.getKey()))
                        return entry.getKey();
                    return "created_at";
                })
                .orElse("created_at");
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);
        String whereClause = "";
        if (conditions.get(INSTANCES_TABLE).size() > 0) {
            whereClause = " WHERE " + String.join(" AND ", conditions.get(INSTANCES_TABLE));
        }
        String limitClause = " OFFSET :offset LIMIT :limit";
        String orderByClause = " ORDER BY " + sortByKey + " " + sortOrder.name();
        String selectStatement = "SELECT * FROM  " + company + "." + INSTANCES_TABLE;
        String query = selectStatement + whereClause + orderByClause + limitClause;
        final List<CICDInstance> cicdInstances = template.query(query, params, CICDInstanceConverters.listRowMapper());
        String countQuery = "SELECT count(*) from " + company + "." + INSTANCES_TABLE + whereClause;
        final Integer count = template.queryForObject(countQuery, params, Integer.class);
        return DbListResponse.of(cicdInstances, count);
    }
    // endregion

    protected Map<String, List<String>> createWhereClauseAndUpdateParams(Map<String, Object> params,
                                                                         CICDInstanceFilter filter) {
        return createWhereClauseAndUpdateParams(params, filter.getIds(), filter.getNames(),
                filter.getIntegrationIds(), filter.getTypes(), filter.getExcludeNames(), filter.getExcludeIds(), filter.getExcludeTypes(),
                filter.getInstanceCreatedRange(), filter.getInstanceUpdatedRange(), filter.getPartialMatch(), filter.getMissingFields());
    }

    protected Map<String, List<String>> createWhereClauseAndUpdateParams(Map<String, Object> params,
                                                                         List<String> ids,
                                                                         List<String> names,
                                                                         List<String> integrationIds,
                                                                         List<CICD_TYPE> types,
                                                                         List<String> excludeNames,
                                                                         List<String> excludeIds,
                                                                         List<CICD_TYPE> excludeTypes,
                                                                         ImmutablePair<Long, Long> instanceCreatedRange,
                                                                         ImmutablePair<Long, Long> instanceUpdatedRange,
                                                                         Map<String, Map<String, String>> partialMatch,
                                                                         Map<String, Boolean> missingFields) {
        List<String> instanceTblConditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(ids)) {
            instanceTblConditions.add("id IN (:ids)");
            params.put("ids",
                    ids.stream().map(UUIDUtils::fromString).filter(Objects::nonNull).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(excludeIds)) {
            instanceTblConditions.add("id NOT IN (:excl_ids)");
            params.put("excl_ids",
                    excludeIds.stream().map(UUIDUtils::fromString).filter(Objects::nonNull).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(names)) {
            instanceTblConditions.add("name IN (:names)");
            params.put("names", names);
        }
        if (CollectionUtils.isNotEmpty(excludeNames)) {
            instanceTblConditions.add("name NOT IN (:excl_names)");
            params.put("excl_names", excludeNames);
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            instanceTblConditions.add("integration_id IN (:integration_ids)");
            params.put("integration_ids",
                    integrationIds.stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(types)) {
            instanceTblConditions.add("type IN (:types)");
            params.put("types", types.stream()
                    .map(CICD_TYPE::toString)
                    .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(excludeTypes)) {
            instanceTblConditions.add("type NOT IN (:excl_types)");
            params.put("excl_types", excludeTypes.stream()
                    .map(CICD_TYPE::toString)
                    .collect(Collectors.toList()));
        }
        if (instanceCreatedRange != null) {
            if (instanceCreatedRange.getLeft() != null) {
                instanceTblConditions.add("extract(epoch from created_at) > :created_start");
                params.put("created_start", instanceCreatedRange.getLeft());
            }
            if (instanceCreatedRange.getRight() != null) {
                instanceTblConditions.add("extract(epoch from created_at) < :created_end");
                params.put("created_end", instanceCreatedRange.getRight());
            }
        }
        if (instanceUpdatedRange != null) {
            if (instanceUpdatedRange.getLeft() != null) {
                instanceTblConditions.add("extract(epoch from updated_at) > :updated_start");
                params.put("updated_start", instanceUpdatedRange.getLeft());
            }
            if (instanceUpdatedRange.getRight() != null) {
                instanceTblConditions.add("extract(epoch from updated_at) < :updated_end");
                params.put("updated_end", instanceUpdatedRange.getRight());
            }
        }
        if (MapUtils.isNotEmpty(partialMatch)) {
            createPartialMatchFilter(partialMatch, instanceTblConditions);
        }
        if (MapUtils.isNotEmpty(missingFields)) {
            instanceTblConditions.addAll(missingFields.entrySet().stream()
                    .map(missingField -> {
                        final boolean shouldBeMissing = Boolean.TRUE.equals(missingField.getValue());
                        CICDInstanceFilter.MISSING_FIELD field = MoreObjects.firstNonNull(
                                CICDInstanceFilter.MISSING_FIELD.fromString(missingField.getKey()), CICDInstanceFilter.MISSING_FIELD.none);
                        String clause;
                        switch (field) {
                            case integration_id:
                                clause = shouldBeMissing ? " " + missingField.getKey() + " IS NULL " : " " + missingField.getKey() + " IS NOT NULL ";
                                break;
                            default:
                                clause = null;
                        }
                        return clause;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
        }
        return Map.of(INSTANCES_TABLE, instanceTblConditions);
    }

    private void createPartialMatchFilter(Map<String, Map<String, String>> partialMatchMap,
                                          List<String> instanceTblConditions) {
        for (Map.Entry<String, Map<String, String>> partialMatchEntry : partialMatchMap.entrySet()) {
            String key = partialMatchEntry.getKey();
            Map<String, String> value = partialMatchEntry.getValue();

            String begins = value.get("$begins");
            String ends = value.get("$ends");
            String contains = value.get("$contains");

            if (begins != null || ends != null || contains != null) {
                if (PARTIAL_MATCH_ARRAY_COLUMNS.contains(key)) {
                    createPartialMatchConditionArray(instanceTblConditions, key, begins, ends, contains);
                } else if (PARTIAL_MATCH_COLUMNS.contains(key)) {
                    createPartialMatchCondition(instanceTblConditions, key, begins, ends, contains);
                }
            }
        }
    }

    private void createPartialMatchConditionArray(List<String> instanceTblConditions, String key, String begins, String ends, String contains) {
        if (begins != null) {
            String beginsCondition = "exists (select 1 from unnest (" + key +
                    ") as k where k SIMILAR TO " + "'" + begins + "%')";
            instanceTblConditions.add(beginsCondition);
        }
        if (ends != null) {
            String endsCondition = "exists (select 1 from unnest (" + key +
                    ") as k where k SIMILAR TO " + "'%" + ends + "')";
            instanceTblConditions.add(endsCondition);
        }
        if (contains != null) {
            String containsCondition = "exists (select 1 from unnest (" + key +
                    ") as k where k SIMILAR TO " + "'%" + contains + "%')";
            instanceTblConditions.add(containsCondition);
        }
    }

    private void createPartialMatchCondition(List<String> instanceTblConditions, String key, String begins, String ends, String contains) {
        if (begins != null) {
            String beginsCondition = key + " SIMILAR TO " + "'" + begins + "%'";
            instanceTblConditions.add(beginsCondition);
        }
        if (ends != null) {
            String endsCondition = key + " SIMILAR TO " + "'%" + ends + "'";
            instanceTblConditions.add(endsCondition);
        }
        if (contains != null) {
            String containsCondition = key + " SIMILAR TO " + "'%" + contains + "%'";
            instanceTblConditions.add(containsCondition);
        }
    }

    // region delete
    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String deleteSql = String.format(DELETE_SQL_FORMAT, company);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.setObject(1, UUID.fromString(id));
            if (pstmt.executeUpdate() > 0) {
                return true;
            }
        }
        return false;
    }
    // endregion

    public CiCdInstanceConfig getConfig(String company, String instanceId) throws SQLException {
        if (!validateInstanceId(company, instanceId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "missing or invalid instance id provided");
        }
        String getSql = "SELECT config, config_updated_at FROM " + company + "." + INSTANCES_TABLE
                + " WHERE id = ?";
        UUID instUuid = UUID.fromString(instanceId);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(getSql)) {
            pstmt.setObject(1, instUuid);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return parseConfig(company, instUuid, DefaultObjectMapper.get(), rs.getString("config"));
            }
            return CiCdInstanceConfig.builder().build();
        }
    }

    public Boolean updateConfig(String company, CICDInstance instance, String instanceId) throws SQLException, JsonProcessingException {
        if (!validateInstanceId(company, instanceId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "missing or invalid instance id provided");
        }
        String updateSql = "UPDATE " + company + "." + INSTANCES_TABLE + " SET config = ?::jsonb, config_updated_at = now(),updated_at = now()  WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setObject(1, DefaultObjectMapper.get().writeValueAsString(instance.getConfig()));
            pstmt.setObject(2, UUID.fromString(instanceId));
            if (pstmt.executeUpdate() > 0) {
                return true;
            }
        }
        return false;
    }

    private boolean validateInstanceId(String company, String instanceId) throws SQLException {
        if (instanceId == null) return false;
        try {
            DbListResponse<CICDInstance> listResponse = list(company,
                    CICDInstanceFilter.builder().ids(List.of(instanceId)).build(),
                    null, null, null);
            return listResponse.getCount() == 1;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public BulkUpdateResponse assignIntegrationId(String company,
                                                  CICDInstanceIntegAssignment request) throws SQLException, JsonProcessingException {
        String integrationId = request.getIntegrationId();
        Set<String> addIds = request.getAddIds();
        Set<String> removeIds = request.getRemoveIds();
        if (StringUtils.isNotBlank(integrationId) && integrationService.listByFilter(company, null, null,
                null, List.of(Integer.parseInt(integrationId)),
                null, 0, 1).getCount() != 1) {
            return BulkUpdateResponse.createBulkUpdateResponse(
                    Stream.concat(addIds.stream(), removeIds.stream()),
                    false, "Invalid Integration Id");
        }
        List<UpdateResponse> updateResponses = new ArrayList<>();
        assignInstancesToIntegration(company, integrationId, addIds, updateResponses);
        unAssignInstancesFromIntegration(company, integrationId, removeIds, updateResponses);
        return BulkUpdateResponse.of(updateResponses);
    }

    private void assignInstancesToIntegration(String company, String integrationId, Set<String> addIds,
                                              List<UpdateResponse> updateResponses) throws SQLException {
        if (CollectionUtils.isEmpty(addIds)) {
            log.info("assignInstancesToIntegration: Add Ids should not be empty, " +
                    "company = {} and integrationId = {}", company, integrationId);
            return;
        }
        if (StringUtils.isBlank(integrationId)) {
            addIds.forEach(id -> updateResponses.add(
                    UpdateResponse.builder().id(id).success(false).error("Integration Id cannot be null or empty").build()
            ));
        } else {
            CICDInstanceFilter filter = CICDInstanceFilter.builder().ids(new ArrayList<>(addIds)).build();
            DbListResponse<CICDInstance> dbCICDInstances = list(company, filter, null, null, null);
            if (dbCICDInstances.getRecords().size() == addIds.size()) {
                batchUpdateInstancesInteg(company, addIds, integrationId, updateResponses);
            } else {
                Set<String> existingAddIds = dbCICDInstances.getRecords().stream()
                        .map(CICDInstance::getId)
                        .map(UUID::toString)
                        .collect(Collectors.toSet());
                addIds.forEach(id -> updateResponses.add(updateInstanceInteg(company, id, integrationId, existingAddIds)));
            }
        }
    }

    private void unAssignInstancesFromIntegration(String company, String integrationId, Set<String> removeIds,
                                                  List<UpdateResponse> updateResponses) throws SQLException {
        if (CollectionUtils.isEmpty(removeIds)) {
            log.info("unAssignInstancesFromIntegration: Remove Ids should not be empty, " +
                    "company = {} and integrationId = {}", company, integrationId);
            return;
        }
        CICDInstanceFilter.CICDInstanceFilterBuilder filterBuilder = CICDInstanceFilter.builder()
                .ids(new ArrayList<>(removeIds));
        if (StringUtils.isNotBlank(integrationId)) {
            filterBuilder.integrationIds(List.of(integrationId));
        }
        DbListResponse<CICDInstance> dbCICDInstances = list(company, filterBuilder.build(), null, null, null);
        if (dbCICDInstances.getRecords().size() == removeIds.size()) {
            batchUpdateInstancesInteg(company, removeIds, null, updateResponses);
        } else {
            Set<String> existingRemoveIds = dbCICDInstances.getRecords().stream()
                    .map(CICDInstance::getId)
                    .map(UUID::toString)
                    .collect(Collectors.toSet());
            removeIds.forEach(id -> updateResponses.add(updateInstanceInteg(company, id, null, existingRemoveIds)));
        }
    }

    private void batchUpdateInstancesInteg(String company, Set<String> ids,
                                           String integrationId, List<UpdateResponse> updateResponses) {
        try {
            updateIntegrationId(company, ids, integrationId);
            ids.forEach(id -> updateResponses.add(UpdateResponse.builder().id(id).success(true).build()));
        } catch (Exception e) {
            ids.forEach(id -> updateResponses.add(UpdateResponse.builder().id(id).success(false).error(e.getMessage()).build()));
        }
    }

    private UpdateResponse updateInstanceInteg(String company, String id, String integrationId, Set<String> existingIds) {
        try {
            if (existingIds.contains(id)) {
                updateIntegrationId(company, Set.of(id), integrationId);
                return UpdateResponse.builder().id(id).success(true).build();
            } else {
                return UpdateResponse.builder().id(id).success(false).error("Instance not found").build();
            }
        } catch (Exception e) {
            return UpdateResponse.builder().id(id).success(false).error(e.getMessage()).build();
        }
    }

    private void updateIntegrationId(String company, Set<String> ids, String integrationId) {
        String updateSql = "UPDATE " + company + "." + INSTANCES_TABLE +
                " SET integration_id = " + integrationId +
                " WHERE id IN (:ids)";
        Map<String, Object> params = new HashMap<>();
        params.put("ids",
                ids.stream()
                        .map(UUIDUtils::fromString)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
        template.update(updateSql, params);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculate(String company, CICDInstanceFilter filter) {
        CICDInstanceFilter.DISTINCT distinct = filter.getAcross();
        Validate.notNull(distinct, "Across must be present for group by query");
        Map<String, Object> params = new HashMap<>();
        Map<String, List<String>> conditions = createWhereClauseAndUpdateParams(params, filter);
        String whereClause = "";
        if (conditions.get(INSTANCES_TABLE).size() > 0) {
            whereClause = " WHERE " + String.join(" AND ", conditions.get(INSTANCES_TABLE));
        }
        String groupByKey;
        switch (distinct) {
            case id:
                groupByKey = " id ";
                break;
            case name:
                groupByKey = " name ";
                break;
            case url:
                groupByKey = " url ";
                break;
            case type:
                groupByKey = " type ";
                break;
            default:
                groupByKey = distinct.name();
        }
        String groupBySql = distinct.equals(CICDInstanceFilter.DISTINCT.none) ? "" : " GROUP BY " + groupByKey;
        String orderByClause = " ORDER BY ct desc";
        String limitString = "";
        Integer acrossLimit = filter.getAcrossLimit();
        if (acrossLimit != null && acrossLimit > 0) {
            limitString = " LIMIT " + acrossLimit;
        }
        String sql = "SELECT " + groupByKey + ", COUNT(id) as ct FROM " + company + "." + INSTANCES_TABLE
                + whereClause + groupBySql + orderByClause + limitString;
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        final List<DbAggregationResult> aggregationResults = template.query(sql, params,
                CICDInstanceConverters.aggRowMapper(distinct.toString()));
        return DbListResponse.of(aggregationResults, aggregationResults.size());
    }

    public CiCdInstanceDetails parseInstDetails(String company, UUID id, ObjectMapper objectMapper, String details) {
        try {
            if (details == null) return CiCdInstanceDetails.builder().build();
            return objectMapper.readValue(details, CiCdInstanceDetails.class);
        } catch (JsonProcessingException e) {
            log.warn("Error processing JSON for Heartbeat timestamp with " +
                    "instanceId for id " + id + " company " + company, e);
            return CiCdInstanceDetails.builder().build();
        }
    }

    private static CiCdInstanceConfig parseConfig(String company, UUID id, ObjectMapper objectMapper, String configString) {
        try {
            if (configString == null) return CiCdInstanceConfig.builder().build();
            return objectMapper.readValue(configString, objectMapper.getTypeFactory().constructType(CiCdInstanceConfig.class));
        } catch (JsonProcessingException e) {
            log.warn("Error processing JSON for config with " +
                    "instanceId for id " + id + " company " + company, e);
            return CiCdInstanceConfig.builder().build();
        }
    }

    // region ensureTableExistence
    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlStatements = new ArrayList<>();
        String sqlStatement = "CREATE TABLE IF NOT EXISTS " + company + ".cicd_instances(\n" +
                "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                "    name VARCHAR,\n" +
                "    url VARCHAR,\n" +
                "    integration_id INTEGER REFERENCES " + company + ".integrations(id) ON DELETE CASCADE," +
                "    type VARCHAR,\n" +
                "    config JSONB NOT NULL DEFAULT '{}'::jsonb,\n" +
                "    config_updated_at TIMESTAMP WITH TIME ZONE, \n" +
                "    details JSONB NOT NULL DEFAULT '{}'::jsonb,\n" +
                "    last_hb_at TIMESTAMP WITH TIME ZONE, \n" +
                "    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(), \n" +
                "    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now() \n" +
                ")";
        sqlStatements.add(sqlStatement);

        try (Connection conn = dataSource.getConnection()) {
            for (String currentSql : sqlStatements) {
                try (PreparedStatement pstmt = conn.prepareStatement(currentSql)) {
                    pstmt.execute();
                }
            }
            return true;
        }
    }
    // endregion
}

