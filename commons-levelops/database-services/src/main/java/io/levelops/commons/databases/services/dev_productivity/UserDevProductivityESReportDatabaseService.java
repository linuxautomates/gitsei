package io.levelops.commons.databases.services.dev_productivity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import io.levelops.commons.databases.converters.CountQueryConverter;
import io.levelops.commons.databases.converters.UserDevProductivityReportConverters;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.dev_productivity.ReportIntervalType;
import io.levelops.commons.databases.models.database.dev_productivity.UserDevProductivityReport;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Timestamp;
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

@Log4j2
@Service
public class UserDevProductivityESReportDatabaseService extends DatabaseService<UserDevProductivityReport> {
    private static final Set<String> SORT_COLUMNS = Set.of("score", "start_time");
    @VisibleForTesting
    protected boolean populateData = true;
    private static final String INSERT_USER_PRODUCTIVITY_REPORT_SQL_FORMAT = "INSERT INTO %s.user_dev_productivity_es_reports(org_user_id, org_user_ref_id, dev_productivity_profile_id, dev_productivity_profile_timestamp, interval, start_time, end_time, score, report) VALUES (:org_user_id, :org_user_ref_id, :dev_productivity_profile_id, :dev_productivity_profile_timestamp, :interval, :start_time, :end_time, :score, :report::jsonb) RETURNING id";
    private static final String DELETE_USER_PRODUCTIVITY_REPORT_SQL_FORMAT = "DELETE FROM %s.user_dev_productivity_es_reports WHERE id = :id";
    private static final String UPSERT_USER_PRODUCTIVITY_REPORT_SQL_FORMAT = "INSERT INTO %s.user_dev_productivity_es_reports(org_user_id, org_user_ref_id, dev_productivity_profile_id, dev_productivity_profile_timestamp, interval, start_time, end_time, score, report) " +
            "VALUES (:org_user_id, :org_user_ref_id, :dev_productivity_profile_id, :dev_productivity_profile_timestamp, :interval, :start_time, :end_time, :score, :report::jsonb) " +
            "ON CONFLICT(org_user_id, dev_productivity_profile_id, interval) DO UPDATE SET (org_user_ref_id, dev_productivity_profile_timestamp, start_time, end_time, score, report, updated_at) = (EXCLUDED.org_user_ref_id, EXCLUDED.dev_productivity_profile_timestamp, EXCLUDED.start_time, EXCLUDED.end_time, EXCLUDED.score,EXCLUDED.report, now())";
    private final NamedParameterJdbcTemplate template;
    private final ObjectMapper objectMapper;

    public UserDevProductivityESReportDatabaseService(DataSource dataSource, ObjectMapper objectMapper) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.objectMapper = objectMapper;
    }

    // region get references
    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(OrgUnitsDatabaseService.class, DevProductivityProfileDatabaseService.class);
    }
    // endregion

    // region insert
    @Override
    public String insert(String company, UserDevProductivityReport t) throws SQLException {
        return insertReport(company, t).toString();
    }
    private UUID insertReport(String company, UserDevProductivityReport t) throws SQLException {
        validateInput(company, t);
        MapSqlParameterSource params = constructParameterSourceForReport(t, null);
        String insertReportSql = String.format(INSERT_USER_PRODUCTIVITY_REPORT_SQL_FORMAT, company);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(insertReportSql, params, keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            throw new SQLException("Failed to insert dev productivity report record!! " + t.toString());
        }
        return (UUID) keyHolder.getKeys().get("id");
    }
    // endregion

    // region insert upsert common
    private void validateInput(String company, UserDevProductivityReport t) {
        Validate.notBlank(company, "Company cannot be null or empty!");

        Validate.notNull(t, "Input User Dev Productivity Report cannot be null!");
        Validate.notNull(t.getOrgUserId(),"Org User Id can not be null");
        Validate.notNull(t.getOrgUserRefId(),"Org User Ref Id can not be null");
        Validate.notNull(t.getDevProductivityProfileId(),"Dev Productivity Profile Id can not be null");
        Validate.notNull(t.getDevProductivityProfileTimestamp(),"Dev Productivity Profile Timestamp can not be null");
        Validate.notNull(t.getInterval(),"Interval can not be null");
        Validate.notNull(t.getStartTime(),"Start time can not be null");
        Validate.notNull(t.getEndTime(),"End time can not be null");
    }
    private MapSqlParameterSource constructParameterSourceForReport(UserDevProductivityReport t, final UUID existingId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("org_user_id", t.getOrgUserId());
        params.addValue("org_user_ref_id", t.getOrgUserRefId());
        params.addValue("dev_productivity_profile_id", t.getDevProductivityProfileId());
        params.addValue("dev_productivity_profile_timestamp", Timestamp.from(t.getDevProductivityProfileTimestamp()));
        params.addValue("interval", t.getInterval().toString());
        params.addValue("start_time", Timestamp.from(t.getStartTime()));
        params.addValue("end_time", Timestamp.from(t.getEndTime()));
        params.addValue("score", t.getScore());
        try {
            params.addValue("report", objectMapper.writeValueAsString(t.getReport()));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize dev productivity report to JSON", e);
        }
        return params;
    }
    // endregion

    // region upsert
    public String upsert(String company, UserDevProductivityReport t) throws SQLException {
        validateInput(company, t);
        MapSqlParameterSource params = constructParameterSourceForReport(t, null);
        String upsertReportSql = String.format(UPSERT_USER_PRODUCTIVITY_REPORT_SQL_FORMAT, company);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(upsertReportSql, params, keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            throw new SQLException("Failed to upsert dev productivity report record!! " + t.toString());
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }
    // endregion

    // region update
    @Override
    public Boolean update(String company, UserDevProductivityReport t) throws SQLException {
        return updateUserProdReport(company,t);
    }
    private Boolean updateUserProdReport(String company, UserDevProductivityReport t) throws SQLException {
        Validate.notBlank(t.getId().toString(), "id cannot be null or empty.");
        UUID reportId = t.getId();
        Boolean updated = false;
        List<String> updates = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        params.put("id", reportId);

        try {
            if (t.getOrgUserId() != null) {
                updates.add("org_user_id = :org_user_id");
                params.put("org_user_id", t.getOrgUserId());
            }
            if (t.getDevProductivityProfileId() != null) {
                updates.add("dev_productivity_profile_id = :dev_productivity_profile_id");
                params.put("dev_productivity_profile_id", t.getDevProductivityProfileId());
            }
            if (t.getInterval() != null) {
                updates.add("interval = :interval");
                params.put("interval", t.getInterval().toString());
            }
            if (t.getStartTime() != null) {
                updates.add("start_time = :start_time");
                params.put("start_time", Timestamp.from(t.getStartTime()));
            }
            if (t.getEndTime() != null) {
                updates.add("end_time = :end_time");
                params.put("end_time", Timestamp.from(t.getEndTime()));
            }
            if (t.getScore() != null) {
                updates.add("score = :score");
                params.put("score", t.getScore());
            }
            if (t.getReport() != null) {
                updates.add("report = :report::jsonb");
                params.put("report", objectMapper.writeValueAsString(t.getReport()));
            }

        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to serialize data", e);
        }

        if (updates.isEmpty()) {
            return true;
        }
        try{
            updates.add("updated_at = now()");
            String sql = "UPDATE " + company + ".user_dev_productivity_es_reports " +
                    " SET " + String.join(", ", updates) +
                    " WHERE id = :id::uuid ";

            updated =  template.update(sql, params) > 0;
        }catch(Exception e){
            log.error("Error updating dev productivity report", e);
            throw e;
        }
        return updated;
    }
    // endregion

    // region get
    @Override
    public Optional<UserDevProductivityReport> get(String company, String id) throws SQLException {
        var results = getBatch(company, 0, 10, Collections.singletonList(UUID.fromString(id)), null, null, null, null, false).getRecords();
        return results.size() > 0 ? Optional.of(results.get(0)) : Optional.empty();
    }
    // endregion

    // region list
    @Override
    public DbListResponse<UserDevProductivityReport> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return getBatch(company, pageNumber, pageSize, null, null, null, null, null,false);
    }

    private DbListResponse<UserDevProductivityReport> getBatch(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<UUID> orgUserIds, List<UUID> devProductivityProfileIds, List<ReportIntervalType> intervals, Map<String, SortingOrder> sort, boolean needRawResponse) throws SQLException {
        List<String> criterias = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        if(CollectionUtils.isNotEmpty(ids)) {
            criterias.add("id in (:ids)");
            params.put("ids", ids);
        }
        if(CollectionUtils.isNotEmpty(orgUserIds)) {
            criterias.add("org_user_id in (:org_user_ids)");
            params.put("org_user_ids", orgUserIds);
        }
        if(CollectionUtils.isNotEmpty(devProductivityProfileIds)) {
            criterias.add("dev_productivity_profile_id in (:dev_productivity_profile_ids)");
            params.put("dev_productivity_profile_ids", devProductivityProfileIds);
        }
        if(CollectionUtils.isNotEmpty(intervals)) {
            criterias.add("interval in (:intervals)");
            params.put("intervals", intervals.stream().map(Objects::toString).collect(Collectors.toList()));
        }

        String selectSqlBase = "SELECT * FROM " + company + ".user_dev_productivity_es_reports";
        String criteria = "";
        if(CollectionUtils.isNotEmpty(criterias)) {
            criteria = " WHERE " + String.join(" AND ", criterias);
        }

        List<String> sortBy = new ArrayList<>();
        if(MapUtils.isNotEmpty(sort)) {
            for(Map.Entry<String, SortingOrder> e : sort.entrySet()) {
                String sortColumn = e.getKey();
                SortingOrder sortingOrder = e.getValue();
                if(! SORT_COLUMNS.contains(sortColumn)) {
                    continue;
                }
                sortBy.add(String.format("%s %s NULLS LAST", sortColumn, sortingOrder));
            }
        }
        sortBy.add("updated_at DESC");
        String orderBy = " ORDER BY " + String.join(",", sortBy);

        String selectSql = selectSqlBase + criteria + orderBy + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSql = "SELECT COUNT(*) FROM (" +  selectSqlBase + criteria + ") AS counted";

        log.info("sql = " + selectSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<UserDevProductivityReport> results = template.query(selectSql, params, UserDevProductivityReportConverters.rowMapper(objectMapper,needRawResponse));

        Integer totCount = 0;
        if (results.size() > 0) {
            totCount = results.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
            if (results.size() == pageSize) {
                totCount = template.query(countSql, params, CountQueryConverter.countMapper()).get(0);
            }
        }
        return DbListResponse.of(results, totCount);
    }
    public DbListResponse<UserDevProductivityReport> listByFilter(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<UUID> orgUserIds, List<UUID> devProductivityProfileIds, List<ReportIntervalType> intervals, Map<String, SortingOrder> sort, boolean needRawResponse) throws SQLException {
        return getBatch(company, pageNumber, pageSize, ids, orgUserIds, devProductivityProfileIds, intervals, sort, needRawResponse);
    }
    // endregion

    // region delete
    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String deleteSql = String.format(DELETE_USER_PRODUCTIVITY_REPORT_SQL_FORMAT, company);
        return template.update(deleteSql, Map.of("id", UUID.fromString(id))) > 0;
    }
    // endregion

    //region ensureTableExistence
    /*
    https://dba.stackexchange.com/questions/58894/differences-between-match-full-match-simple-and-match-partial
    MATCH SIMPLE if one thing is NULL the constraint is simply ignored.
     */
    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList= List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".user_dev_productivity_es_reports\n" +
                        "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    org_user_id UUID NOT NULL REFERENCES " + company + ".org_users(id) ON DELETE CASCADE," +
                        "    org_user_ref_id INTEGER NOT NULL," +
                        "    dev_productivity_profile_id UUID  REFERENCES " + company + ".dev_productivity_profiles(id) ON DELETE CASCADE," +
                        "    dev_productivity_profile_timestamp TIMESTAMPTZ NOT NULL,\n" +
                        "    \"interval\" VARCHAR NOT NULL,\n" +
                        "    start_time TIMESTAMPTZ NOT NULL,\n" +
                        "    end_time TIMESTAMPTZ NOT NULL,\n" +
                        "    score INTEGER,\n" +
                        "    report JSONB,\n" +
                        "    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),\n" +
                        "    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()\n" +
                        ")",
                "CREATE UNIQUE INDEX IF NOT EXISTS uniq_user_dev_productivity_es_reports_org_user_id_dev_productivity_profile_id_interval_idx on " + company + ".user_dev_productivity_es_reports (org_user_id, dev_productivity_profile_id, interval)"
        );
        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
    // endregion
}
