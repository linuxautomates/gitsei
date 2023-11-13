package io.levelops.commons.databases.services.dev_productivity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.converters.CountQueryConverter;
import io.levelops.commons.databases.converters.UserDevProductivityReportConverters;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.dev_productivity.ReportIntervalType;
import io.levelops.commons.databases.models.database.dev_productivity.UserDevProductivityReport;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Timestamp;
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

import static io.levelops.commons.databases.converters.UserDevProductivityReportConverters.RESPONSE_IS_NOT_V1;

@Log4j2
@Service
public class UserDevProductivityReportV2DatabaseService extends DatabaseService<UserDevProductivityReport> {
    private static final Set<String> SORT_COLUMNS = Set.of("score", "start_time", "week_of_year", "year");
    private static final String DELETE_USER_PRODUCTIVITY_REPORT_SQL_FORMAT = "DELETE FROM %s.user_dev_productivity_reports_v2 WHERE id = :id";
    private static final String UPSERT_USER_PRODUCTIVITY_REPORT_SQL_FORMAT = "INSERT INTO %s.user_dev_productivity_reports_v2(org_user_id, org_user_ref_id, dev_productivity_profile_id, interval, start_time, end_time, week_of_year, year, score, report, incomplete, missing_features) " +
            "VALUES (:org_user_id, :org_user_ref_id, :dev_productivity_profile_id, :interval, :start_time, :end_time, :week_of_year, :year, :score, :report::jsonb, :incomplete, :missing_features) " +
            "ON CONFLICT(org_user_id, dev_productivity_profile_id, interval, week_of_year, year) DO UPDATE SET (org_user_ref_id, start_time, end_time, week_of_year, year, score, report, incomplete, missing_features, updated_at) = (EXCLUDED.org_user_ref_id, EXCLUDED.start_time, EXCLUDED.end_time, EXCLUDED.week_of_year, EXCLUDED.year, EXCLUDED.score,EXCLUDED.report, EXCLUDED.incomplete, EXCLUDED.missing_features, now())";
    private final NamedParameterJdbcTemplate template;
    private final PlatformTransactionManager transactionManager;
    private final ObjectMapper objectMapper;
    private final OrgUsersDatabaseService orgUsersDatabaseService;

    //region CSTOR
    public UserDevProductivityReportV2DatabaseService(DataSource dataSource, ObjectMapper objectMapper, OrgUsersDatabaseService orgUsersDatabaseService) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.transactionManager = new DataSourceTransactionManager(dataSource);
        this.objectMapper = objectMapper;
        this.orgUsersDatabaseService = orgUsersDatabaseService;
    }
    //endregion

    // region get references
    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(OrgUnitsDatabaseService.class, DevProductivityProfileDatabaseService.class, OrgUsersDatabaseService.class);
    }
    // endregion

    // region insert
    @Override
    public String insert(String company, UserDevProductivityReport t) throws SQLException {
        throw new NotImplementedException("insert not implemented!");
    }
    // endregion

    // region insert upsert common
    private void validateInput(String company, UserDevProductivityReport t) {
        Validate.notBlank(company, "Company cannot be null or empty!");

        Validate.notNull(t, "Input User Dev Productivity Report cannot be null!");
        Validate.notNull(t.getOrgUserId(),"Org User Id can not be null");
        Validate.notNull(t.getOrgUserRefId(),"Org User Ref Id can not be null");
        Validate.notNull(t.getDevProductivityProfileId(),"Dev Productivity Profile Id can not be null");
        Validate.notNull(t.getInterval(),"Interval can not be null");
        Validate.notNull(t.getStartTime(),"Start time can not be null");
        Validate.notNull(t.getEndTime(),"End time can not be null");
    }
    private MapSqlParameterSource constructParameterSourceForReport(UserDevProductivityReport t, final UUID existingId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("org_user_id", t.getOrgUserId());
        params.addValue("org_user_ref_id", t.getOrgUserRefId());
        params.addValue("dev_productivity_profile_id", t.getDevProductivityProfileId());
        params.addValue("interval", t.getInterval().toString());
        params.addValue("start_time", Timestamp.from(t.getStartTime()));
        params.addValue("end_time", Timestamp.from(t.getEndTime()));
        params.addValue("week_of_year",t.getWeekOfYear());
        params.addValue("year", t.getYear());
        params.addValue("score", t.getScore());
        try {
            params.addValue("report", objectMapper.writeValueAsString(t.getReport()));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize dev productivity report to JSON", e);
        }
        params.addValue("incomplete", BooleanUtils.isTrue(t.getIncomplete()));
        params.addValue("missing_features", CollectionUtils.emptyIfNull(t.getMissingFeatures()).toArray(new String[0]));
        return params;
    }
    // endregion

    // region upsert
    public String upsert(String company, UserDevProductivityReport t) throws SQLException {
        validateInput(company, t);
        TransactionDefinition txDef = new DefaultTransactionDefinition();
        TransactionStatus txStatus = transactionManager.getTransaction(txDef);
        try {
            String id = upsertReport(company, t);
            syncLatestFlag(company, t.getOrgUserId(), t.getOrgUserRefId(), t.getDevProductivityProfileId(), t.getInterval(), t.getWeekOfYear(), t.getYear(), t.getStartTime());
            transactionManager.commit(txStatus);
            return id;
        } catch (Exception e) {
            transactionManager.rollback(txStatus);
            throw e;
        }
    }
    private String upsertReport(String company, UserDevProductivityReport t) throws SQLException {
        MapSqlParameterSource params = constructParameterSourceForReport(t, null);
        String upsertReportSql = String.format(UPSERT_USER_PRODUCTIVITY_REPORT_SQL_FORMAT, company);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(upsertReportSql, params, keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            throw new SQLException("Failed to upsert dev productivity report record!! " + t.toString());
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }

    //dev_productivity_profile_id, interval
    private static final String SELECT_GET_ORG_USER_ID_FOR_LATEST_REPORT = "SELECT org_user_id FROM %s.user_dev_productivity_reports_v2 WHERE org_user_ref_id=:org_user_ref_id AND dev_productivity_profile_id=:dev_productivity_profile_id AND interval=:interval AND latest=true";
    private Optional<UUID> getOrgUserIdOfLatestReport(final String company, Integer reportOrgUserRefId, UUID devProductivityProfileId, ReportIntervalType interval) {
        String sql = String.format(SELECT_GET_ORG_USER_ID_FOR_LATEST_REPORT, company);
        Map<String, Object> params = new HashMap<>();
        params.put("org_user_ref_id", reportOrgUserRefId);
        params.put("dev_productivity_profile_id", devProductivityProfileId);
        params.put("interval", interval.toString());

        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        List<UUID> results = template.query(sql, params, (rs, rowNumber) -> (UUID) rs.getObject("org_user_id"));
        return results.size() > 0 ? Optional.of(results.get(0)) : Optional.empty();
    }

    private static final String SELECT_GET_START_TIME_FOR_LATEST_REPORT = "SELECT start_time FROM %s.user_dev_productivity_reports_v2 WHERE org_user_id=:org_user_id AND dev_productivity_profile_id=:dev_productivity_profile_id AND interval=:interval AND latest=true";
    private Optional<Instant> getStartTimeOfLatestReport(final String company, UUID reportOrgUserId, UUID devProductivityProfileId, ReportIntervalType interval) {
        String sql = String.format(SELECT_GET_START_TIME_FOR_LATEST_REPORT, company);
        Map<String, Object> params = new HashMap<>();
        params.put("org_user_id", reportOrgUserId);
        params.put("dev_productivity_profile_id", devProductivityProfileId);
        params.put("interval", interval.toString());

        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        List<Instant> results = template.query(sql, params, (rs, rowNumber) -> DateUtils.toInstant(rs.getTimestamp("start_time")));
        return results.size() > 0 ? Optional.of(results.get(0)) : Optional.empty();
    }

    private static final String UPDATE_REPORT_MARK_LATEST_FALSE = "UPDATE %s.user_dev_productivity_reports_v2 SET latest=false WHERE org_user_ref_id=:active_org_user_ref_id AND org_user_id!=:active_org_user_id AND dev_productivity_profile_id=:dev_productivity_profile_id AND interval=:interval AND latest=true AND week_of_year=:week_of_year AND year=:year";
    private int markReportAsNotLatest(final String company, UUID activeOrgUserId, Integer activeOrgUserRefId, UUID devProductivityProfileId, ReportIntervalType interval, Integer weekOfYear, Integer year) {
        String sql = String.format(UPDATE_REPORT_MARK_LATEST_FALSE, company);
        Map<String, Object> params = new HashMap<>();
        params.put("active_org_user_ref_id", activeOrgUserRefId);
        params.put("active_org_user_id", activeOrgUserId);
        params.put("dev_productivity_profile_id", devProductivityProfileId);
        params.put("interval", interval.toString());
        params.put("week_of_year",weekOfYear);
        params.put("year",year);

        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        int updatedRows = template.update(sql, params);
        log.debug("markReportAsNotLatest updatedRows = {}", updatedRows);
        return updatedRows;
    }

    private static final String UPDATE_ALL_REPORTS_MARK_LATEST_FALSE = "UPDATE %s.user_dev_productivity_reports_v2 SET latest=false WHERE org_user_ref_id=:active_org_user_ref_id AND org_user_id=:active_org_user_id AND dev_productivity_profile_id=:dev_productivity_profile_id AND interval=:interval AND latest=true";
    private int markAllLatestReportsAsNotLatest(final String company, UUID activeOrgUserId, Integer activeOrgUserRefId, UUID devProductivityProfileId, ReportIntervalType interval) {
        String sql = String.format(UPDATE_ALL_REPORTS_MARK_LATEST_FALSE, company);
        Map<String, Object> params = new HashMap<>();
        params.put("active_org_user_ref_id", activeOrgUserRefId);
        params.put("active_org_user_id", activeOrgUserId);
        params.put("dev_productivity_profile_id", devProductivityProfileId);
        params.put("interval", interval.toString());

        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        int updatedRows = template.update(sql, params);
        log.debug("markReportAsNotLatest updatedRows = {}", updatedRows);
        return updatedRows;
    }

    private static final String UPDATE_REPORT_MARK_LATEST_TRUE = "UPDATE %s.user_dev_productivity_reports_v2 SET latest=true WHERE org_user_ref_id=:active_org_user_ref_id AND org_user_id=:active_org_user_id AND dev_productivity_profile_id=:dev_productivity_profile_id AND interval=:interval AND week_of_year=:week_of_year AND year=:year";
    private int markReportAsLatest(final String company, UUID activeOrgUserId, Integer activeOrgUserRefId, UUID devProductivityProfileId, ReportIntervalType interval, Integer weekOfYear, Integer year) {
        String sql = String.format(UPDATE_REPORT_MARK_LATEST_TRUE, company);
        Map<String, Object> params = new HashMap<>();
        params.put("active_org_user_ref_id", activeOrgUserRefId);
        params.put("active_org_user_id", activeOrgUserId);
        params.put("dev_productivity_profile_id", devProductivityProfileId);
        params.put("interval", interval.toString());
        params.put("week_of_year",weekOfYear);
        params.put("year",year);

        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        int updatedRows = template.update(sql, params);
        log.debug("markReportAsLatest updatedRows = {}", updatedRows);
        return updatedRows;
    }

    private void syncLatestFlag(final String company, UUID reportOrgUserId, Integer reportOrgUserRefId, UUID devProductivityProfileId, ReportIntervalType interval, Integer weekOfYear, Integer year, Instant startTime) throws SQLException {
        DBOrgUser activeUser = orgUsersDatabaseService.get(company, reportOrgUserRefId)
                .orElseThrow(() -> new SQLException(String.format("Could not find active user for company %s Org User Ref Id %d!", company, reportOrgUserRefId)));
        Optional<UUID> optOrgUserIdOfLatestReport = getOrgUserIdOfLatestReport(company, reportOrgUserRefId, devProductivityProfileId, interval);
        boolean shouldUnMarkAllLatestReports = false;
        //If reportUser UUID equals activeUser UUID
        boolean shouldUnMarkCurrentLatestReport = (Objects.equals(reportOrgUserId, activeUser.getId())) && (optOrgUserIdOfLatestReport.isPresent()) && (!Objects.equals(optOrgUserIdOfLatestReport.get(), activeUser.getId()));
        boolean shouldMarkCurrentReportAsLatest = (optOrgUserIdOfLatestReport.isEmpty()) || ( (Objects.equals(reportOrgUserId, activeUser.getId())) && (!Objects.equals(optOrgUserIdOfLatestReport.get(), activeUser.getId())));
        if(optOrgUserIdOfLatestReport.isPresent() && (interval.equals(ReportIntervalType.LAST_WEEK) || interval.equals(ReportIntervalType.LAST_TWO_WEEKS))){
            Optional<Instant> latestStartTimeOpt = getStartTimeOfLatestReport(company, optOrgUserIdOfLatestReport.get(), devProductivityProfileId, interval);
            shouldMarkCurrentReportAsLatest = (latestStartTimeOpt.isEmpty() || (latestStartTimeOpt.isPresent() && !latestStartTimeOpt.get().isAfter(startTime)));
            shouldUnMarkAllLatestReports  = (latestStartTimeOpt.isPresent() && !latestStartTimeOpt.get().isAfter(startTime));
        }
        if(shouldUnMarkAllLatestReports){
            markAllLatestReportsAsNotLatest(company, reportOrgUserId, reportOrgUserRefId, devProductivityProfileId, interval);
        }
        else if(shouldUnMarkCurrentLatestReport) {
            markReportAsNotLatest(company, reportOrgUserId, reportOrgUserRefId, devProductivityProfileId, interval, weekOfYear, year);
        }
        if (shouldMarkCurrentReportAsLatest) {
            markReportAsLatest(company, reportOrgUserId, reportOrgUserRefId, devProductivityProfileId, interval, weekOfYear, year);
        }
    }
    // endregion

    // region update
    @Override
    public Boolean update(String company, UserDevProductivityReport t) throws SQLException {
        throw new NotImplementedException("update not implemented!");
    }
    // endregion

    // region get
    @Override
    public Optional<UserDevProductivityReport> get(String company, String id) throws SQLException {
        var results = getBatch(company, 0, 10, Collections.singletonList(UUID.fromString(id)), null, null, null, null, false, null, null).getRecords();
        return IterableUtils.getFirst(results);
    }
    // endregion

    // region list
    @Override
    public DbListResponse<UserDevProductivityReport> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return getBatch(company, pageNumber, pageSize, null, null, null, null, null,false, null, null);
    }

    private DbListResponse<UserDevProductivityReport> getBatch(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<UUID> orgUserIds, List<UUID> devProductivityProfileIds, List<ReportIntervalType> intervals, Map<String, SortingOrder> sort, boolean needRawResponse, List<UUID> absoluteOrgUserIds, Boolean latest) throws SQLException {
        List<String> criterias = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        boolean orgUsersJoinNeeded = false;
        if(CollectionUtils.isNotEmpty(ids)) {
            criterias.add("r.id in (:ids)");
            params.put("ids", ids);
        }
        if(CollectionUtils.isNotEmpty(orgUserIds)) {
            orgUsersJoinNeeded = true;
            criterias.add("u.id in (:org_user_ids)");
            params.put("org_user_ids", orgUserIds);
            if(BooleanUtils.isNotFalse(latest))
                criterias.add("r.latest = true");
        }
        if(CollectionUtils.isNotEmpty(devProductivityProfileIds)) {
            criterias.add("r.dev_productivity_profile_id in (:dev_productivity_profile_ids)");
            params.put("dev_productivity_profile_ids", devProductivityProfileIds);
        }
        if(CollectionUtils.isNotEmpty(intervals)) {
            criterias.add("r.interval in (:intervals)");
            params.put("intervals", intervals.stream().map(Objects::toString).collect(Collectors.toList()));
        }
        if(CollectionUtils.isNotEmpty(absoluteOrgUserIds)) {
            criterias.add("r.org_user_id in (:absolute_org_user_ids)");
            params.put("absolute_org_user_ids", absoluteOrgUserIds);
        }

        List<String> selects = new ArrayList<>();
        selects.add("r.*");
        String join = " ";
        if(orgUsersJoinNeeded) {
            selects.add("u.id as requested_org_user_id");
            join = join + " JOIN " + company + ".org_users AS u on r.org_user_ref_id = u.ref_id";
        } else {
            selects.add("r.org_user_id as requested_org_user_id");
        }
        String selectSqlBase = "SELECT " + String.join(",", selects) + " FROM " + company + ".user_dev_productivity_reports_v2 AS r " + join;

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

        log.debug("sql = " + selectSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        List<UserDevProductivityReport> results = template.query(selectSql, params, UserDevProductivityReportConverters.rowMapper(objectMapper,needRawResponse, RESPONSE_IS_NOT_V1));

        Integer totCount = 0;
        if (results.size() > 0) {
            totCount = results.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
            if (results.size() == pageSize) {
                totCount = template.query(countSql, params, CountQueryConverter.countMapper()).get(0);
            }
        }
        return DbListResponse.of(results, totCount);
    }
    public DbListResponse<UserDevProductivityReport> listByFilter(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<UUID> orgUserIds, List<UUID> devProductivityProfileIds, List<ReportIntervalType> intervals, Map<String, SortingOrder> sort, boolean needRawResponse, List<UUID> absoluteOrgUserIds) throws SQLException {
        return getBatch(company, pageNumber, pageSize, ids, orgUserIds, devProductivityProfileIds, intervals, sort, needRawResponse, absoluteOrgUserIds, null);
    }
    public DbListResponse<UserDevProductivityReport> listByFilter(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<UUID> orgUserIds, List<UUID> devProductivityProfileIds, List<ReportIntervalType> intervals, Map<String, SortingOrder> sort, boolean needRawResponse, List<UUID> absoluteOrgUserIds, Boolean latest) throws SQLException {
        return getBatch(company, pageNumber, pageSize, ids, orgUserIds, devProductivityProfileIds, intervals, sort, needRawResponse, absoluteOrgUserIds, latest);
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
                "CREATE TABLE IF NOT EXISTS " + company + ".user_dev_productivity_reports_v2\n" +
                        "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    org_user_id UUID NOT NULL REFERENCES " + company + ".org_users(id) ON DELETE CASCADE," +
                        "    org_user_ref_id INTEGER NOT NULL," +
                        "    dev_productivity_profile_id UUID  REFERENCES " + company + ".dev_productivity_profiles(id) ON DELETE CASCADE," +
                        "    \"interval\" VARCHAR NOT NULL,\n" +
                        "    start_time TIMESTAMPTZ NOT NULL,\n" +
                        "    end_time TIMESTAMPTZ NOT NULL,\n" +
                        "    week_of_year INTEGER NOT NULL DEFAULT -1,\n" +
                        "    year INTEGER NOT NULL DEFAULT -1,\n" +
                        "    latest BOOLEAN NOT NULL DEFAULT false,\n" +
                        "    score INTEGER,\n" +
                        "    report JSONB,\n" +
                        "    incomplete BOOLEAN NOT NULL DEFAULT false,\n" +
                        "    missing_features VARCHAR[] NOT NULL DEFAULT '{}'::VARCHAR[],\n" +
                        "    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),\n" +
                        "    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()\n" +
                        ")",
                "CREATE UNIQUE INDEX IF NOT EXISTS uniq_user_dev_productivity_reports_v2_org_user_id_dev_productivity_profile_id_interval_week_of_year_year_idx on " + company + ".user_dev_productivity_reports_v2 (org_user_id, dev_productivity_profile_id, interval, week_of_year, year)",
                "CREATE UNIQUE INDEX IF NOT EXISTS uniq_user_dev_productivity_reports_v2_org_user_ref_id_dev_productivity_profile_id_interval_week_of_year_year_latest_true_idx on " + company + ".user_dev_productivity_reports_v2 (org_user_ref_id, dev_productivity_profile_id, interval, week_of_year, year, latest) WHERE latest=true"
        );
        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
    // endregion
}
