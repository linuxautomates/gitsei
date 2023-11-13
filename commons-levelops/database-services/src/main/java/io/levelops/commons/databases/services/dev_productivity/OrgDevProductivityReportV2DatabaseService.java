package io.levelops.commons.databases.services.dev_productivity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import io.levelops.commons.databases.converters.CountQueryConverter;
import io.levelops.commons.databases.converters.OrgDevProductivityReportsConverters;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.dev_productivity.OrgDevProductivityReport;
import io.levelops.commons.databases.models.database.dev_productivity.ReportIntervalType;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.ObjectUtils;
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

@Log4j2
@Service
public class OrgDevProductivityReportV2DatabaseService extends DatabaseService<OrgDevProductivityReport> {
    private static final Set<String> SORT_COLUMNS = Set.of("score", "start_time", "week_of_year", "year");

    private static final String DELETE_ORG_PRODUCTIVITY_REPORT_SQL_FORMAT = "DELETE FROM %s.org_dev_productivity_reports_v2 WHERE id = :id";
    private static final String UPSERT_ORG_PRODUCTIVITY_REPORT_SQL_FORMAT = "INSERT INTO %s.org_dev_productivity_reports_v2(ou_id, ou_ref_id, dev_productivity_profile_id, interval, start_time, end_time, week_of_year, year, score, report, missing_user_reports_count, stale_user_reports_count) " +
            "VALUES (:ou_id, :ou_ref_id, :dev_productivity_profile_id, :interval, :start_time, :end_time, :week_of_year, :year, :score, :report::jsonb, :missing_user_reports_count, :stale_user_reports_count) " +
            "ON CONFLICT(ou_id, dev_productivity_profile_id, interval, week_of_year, year) DO UPDATE SET (ou_ref_id, start_time, end_time, week_of_year, year, score, report, missing_user_reports_count, stale_user_reports_count, updated_at) = (EXCLUDED.ou_ref_id, EXCLUDED.start_time, EXCLUDED.end_time, EXCLUDED.week_of_year, EXCLUDED.year, EXCLUDED.score,EXCLUDED.report,EXCLUDED.missing_user_reports_count,EXCLUDED.stale_user_reports_count, now())";
    private final NamedParameterJdbcTemplate template;
    private final PlatformTransactionManager transactionManager;
    private final ObjectMapper objectMapper;
    private final OrgUnitsDatabaseService orgUnitsDatabaseService;

    public OrgDevProductivityReportV2DatabaseService(DataSource dataSource, ObjectMapper objectMapper, OrgUnitsDatabaseService orgUnitsDatabaseService) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.transactionManager = new DataSourceTransactionManager(dataSource);
        this.objectMapper = objectMapper;
        this.orgUnitsDatabaseService = orgUnitsDatabaseService;
    }

    // region get references
    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(OrgUnitsDatabaseService.class, DevProductivityProfileDatabaseService.class);
    }
    // endregion

    // region insert upsert common
    private void validateInput(String company, OrgDevProductivityReport t) {
        Validate.notBlank(company, "Company cannot be null or empty!");

        Validate.notNull(t, "Input User Dev Productivity Report cannot be null!");
        Validate.notNull(t.getOuID(),"OU Id can not be null");
        Validate.notNull(t.getOuRefId(),"OU Ref Id can not be null");
        Validate.notNull(t.getDevProductivityProfileId(),"Dev Productivity Profile Id can not be null");
        Validate.notNull(t.getInterval(),"Interval can not be null");
        Validate.notNull(t.getStartTime(),"Start time can not be null");
        Validate.notNull(t.getEndTime(),"End time can not be null");
    }
    private MapSqlParameterSource constructParameterSourceForReport(OrgDevProductivityReport t, final UUID existingId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ou_id", t.getOuID());
        params.addValue("ou_ref_id", t.getOuRefId());
        params.addValue("dev_productivity_profile_id", t.getDevProductivityProfileId());
        params.addValue("interval", t.getInterval().toString());
        params.addValue("start_time", Timestamp.from(t.getStartTime()));
        params.addValue("end_time", Timestamp.from(t.getEndTime()));
        params.addValue("week_of_year",t.getWeekOfYear());
        params.addValue("year", t.getYear());
        params.addValue("score", t.getScore());
        params.addValue("missing_user_reports_count", ObjectUtils.firstNonNull(t.getMissingUserReportsCount(), 0));
        params.addValue("stale_user_reports_count", ObjectUtils.firstNonNull(t.getStaleUserReportsCount(), 0));
        try{
            params.addValue("report", objectMapper.writeValueAsString(t.getReport()));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize profile report to JSON", e);
        }
        return params;
    }
    // endregion

    // region upsert
    public String upsert(String company, OrgDevProductivityReport t) throws SQLException {
        validateInput(company, t);
        TransactionDefinition txDef = new DefaultTransactionDefinition();
        TransactionStatus txStatus = transactionManager.getTransaction(txDef);
        try {
            String id = upsertReport(company, t);
            syncLatestFlag(company, t.getOuID(), t.getOuRefId(), t.getDevProductivityProfileId(), t.getInterval(), t.getWeekOfYear(), t.getYear(), t.getStartTime());
            transactionManager.commit(txStatus);
            return id;
        } catch (Exception e) {
            transactionManager.rollback(txStatus);
            throw e;
        }
    }

    private String upsertReport(String company, OrgDevProductivityReport t) throws SQLException {
        MapSqlParameterSource params = constructParameterSourceForReport(t, null);
        String upsertReportSql = String.format(UPSERT_ORG_PRODUCTIVITY_REPORT_SQL_FORMAT, company);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(upsertReportSql, params, keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            throw new SQLException("Failed to upsert dev productivity report record!! " + t.toString());
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }

    private static final String SELECT_GET_OU_ID_FOR_LATEST_REPORT = "SELECT ou_id FROM %s.org_dev_productivity_reports_v2 WHERE ou_ref_id=:ou_ref_id AND dev_productivity_profile_id=:dev_productivity_profile_id AND interval=:interval AND latest=true";
    private Optional<UUID> getOUIdOfLatestReport(final String company, Integer reportOURefId, UUID devProductivityProfileId, ReportIntervalType interval) {
        String sql = String.format(SELECT_GET_OU_ID_FOR_LATEST_REPORT, company);
        Map<String, Object> params = new HashMap<>();
        params.put("ou_ref_id", reportOURefId);
        params.put("dev_productivity_profile_id", devProductivityProfileId);
        params.put("interval", interval.toString());

        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        List<UUID> results = template.query(sql, params, (rs, rowNumber) -> (UUID) rs.getObject("ou_id"));
        return results.size() > 0 ? Optional.of(results.get(0)) : Optional.empty();
    }

    private static final String SELECT_GET_START_TIME_FOR_LATEST_REPORT = "SELECT start_time FROM %s.org_dev_productivity_reports_v2 WHERE ou_ref_id=:ou_ref_id AND dev_productivity_profile_id=:dev_productivity_profile_id AND interval=:interval";
    private Optional<Instant> getStartTimeOfLatestReport(final String company, Integer reportOURefId, UUID devProductivityProfileId, ReportIntervalType interval) {
        String sql = String.format(SELECT_GET_START_TIME_FOR_LATEST_REPORT, company);
        Map<String, Object> params = new HashMap<>();
        params.put("ou_ref_id", reportOURefId);
        params.put("dev_productivity_profile_id", devProductivityProfileId);
        params.put("interval", interval.toString());

        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        List<Instant> results = template.query(sql, params, (rs, rowNumber) -> DateUtils.toInstant(rs.getTimestamp("start_time")));
        return results.size() > 0 ? Optional.of(results.get(0)) : Optional.empty();
    }

    private static final String UPDATE_ALL_REPORTS_MARK_LATEST_FALSE = "UPDATE %s.org_dev_productivity_reports_v2 SET latest=false WHERE ou_ref_id=:ou_ref_id  AND dev_productivity_profile_id=:dev_productivity_profile_id AND interval=:interval AND latest=true";
    private int markAllLatestReportsAsNotLatest(final String company, Integer ouRefId, UUID devProductivityProfileId, ReportIntervalType interval) {
        String sql = String.format(UPDATE_ALL_REPORTS_MARK_LATEST_FALSE, company);
        Map<String, Object> params = new HashMap<>();
        params.put("ou_ref_id", ouRefId);
        params.put("dev_productivity_profile_id", devProductivityProfileId);
        params.put("interval", interval.toString());

        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        int updatedRows = template.update(sql, params);
        log.debug("markReportAsNotLatest updatedRows = {}", updatedRows);
        return updatedRows;
    }

    private static final String UPDATE_REPORT_MARK_LATEST_FALSE = "UPDATE %s.org_dev_productivity_reports_v2 SET latest=false WHERE ou_ref_id=:active_ou_ref_id AND ou_id!=:active_ou_id AND dev_productivity_profile_id=:dev_productivity_profile_id AND interval=:interval AND latest=true AND week_of_year=:week_of_year AND year=:year";
    private int markReportAsNotLatest(final String company, UUID activeOUId, Integer activeOURefId, UUID devProductivityProfileId, ReportIntervalType interval, Integer weekOfYear, Integer year) {
        String sql = String.format(UPDATE_REPORT_MARK_LATEST_FALSE, company);
        Map<String, Object> params = new HashMap<>();
        params.put("active_ou_ref_id", activeOURefId);
        params.put("active_ou_id", activeOUId);
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
    private static final String UPDATE_REPORT_MARK_LATEST_TRUE = "UPDATE %s.org_dev_productivity_reports_v2 SET latest=true WHERE ou_ref_id=:active_ou_ref_id AND ou_id=:active_ou_id AND dev_productivity_profile_id=:dev_productivity_profile_id AND interval=:interval AND week_of_year=:week_of_year AND year=:year";
    private int markReportAsLatest(final String company, UUID activeOUId, Integer activeOURefId, UUID devProductivityProfileId, ReportIntervalType interval, Integer weekOfYear, Integer year) {
        String sql = String.format(UPDATE_REPORT_MARK_LATEST_TRUE, company);
        Map<String, Object> params = new HashMap<>();
        params.put("active_ou_ref_id", activeOURefId);
        params.put("active_ou_id", activeOUId);
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

    private void syncLatestFlag(final String company, UUID reportOUId, Integer reportOURefId, UUID devProductivityProfileId, ReportIntervalType interval, Integer weekOfYear, Integer year, Instant startTime) throws SQLException {
        DBOrgUnit activeOU = orgUnitsDatabaseService.get(company, reportOURefId, true)
                .orElseThrow(() -> new SQLException(String.format("Could not find active ou for company %s OU Ref Id %d!", company, reportOURefId)));
        Optional<UUID> optOUIdOfLatestReport = getOUIdOfLatestReport(company, reportOURefId, devProductivityProfileId, interval);
        boolean shouldUnMarkAllLatestReports = false;
        //If reportUser UUID equals activeUser UUID
        boolean shouldUnMarkCurrentLatestReport = (Objects.equals(reportOUId, activeOU.getId())) && (optOUIdOfLatestReport.isPresent()) && (!Objects.equals(optOUIdOfLatestReport.get(), activeOU.getId()));
        boolean shouldMarkCurrentReportAsLatest = (optOUIdOfLatestReport.isEmpty()) || ( (Objects.equals(reportOUId, activeOU.getId())) && (!Objects.equals(optOUIdOfLatestReport.get(), activeOU.getId())));
        if(optOUIdOfLatestReport.isPresent() && (interval.equals(ReportIntervalType.LAST_WEEK) || interval.equals(ReportIntervalType.LAST_TWO_WEEKS))){
            Optional<Instant> latestStartTimeOpt = getStartTimeOfLatestReport(company, reportOURefId, devProductivityProfileId, interval);
            shouldMarkCurrentReportAsLatest = (latestStartTimeOpt.isEmpty() || (latestStartTimeOpt.isPresent() && !latestStartTimeOpt.get().isAfter(startTime)));
            shouldUnMarkAllLatestReports  = (latestStartTimeOpt.isPresent() && !latestStartTimeOpt.get().isAfter(startTime));
        }
        if(shouldUnMarkAllLatestReports){
            markAllLatestReportsAsNotLatest(company, reportOURefId, devProductivityProfileId, interval);
        } else if(shouldUnMarkCurrentLatestReport) {
            markReportAsNotLatest(company, reportOUId, reportOURefId, devProductivityProfileId, interval, weekOfYear, year);
        }
        if (shouldMarkCurrentReportAsLatest) {
            markReportAsLatest(company, reportOUId, reportOURefId, devProductivityProfileId, interval, weekOfYear, year);
        }
    }

    // endregion

    // region insert
    @Override
    public String insert(String company, OrgDevProductivityReport t) throws SQLException {
        throw new NotImplementedException("insert not implemented!");
    }
    // endregion

    // region update
    @Override
    public Boolean update(String company, OrgDevProductivityReport t) throws SQLException {
        throw new NotImplementedException("update not implemented!");
    }
    // endregion

    // region get
    @Override
    public Optional<OrgDevProductivityReport> get(String company, String id) throws SQLException {
        var results = getBatch(company, 0, 10, Collections.singletonList(UUID.fromString(id)), null, null, null, null,null, false, null).getRecords();
        return results.size() > 0 ? Optional.of(results.get(0)) : Optional.empty();
    }
    // endregion

    // region list
    @Override
    public DbListResponse<OrgDevProductivityReport> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return getBatch(company, pageNumber, pageSize, null, null, null, null, null, null, false, null);
    }

    private DbListResponse<OrgDevProductivityReport> getBatch(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<UUID> ouIds, List<UUID> devProductivityProfileIds, List<ReportIntervalType> intervals, List<Integer> ouRefIds, Map<String, SortingOrder> sort, boolean needRawReport, Boolean latest) throws SQLException {
        List<String> criterias = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        boolean ousJoinNeeded = false;
        if(CollectionUtils.isNotEmpty(ids)) {
            criterias.add("r.id in (:ids)");
            params.put("ids", ids);
        }
        if(CollectionUtils.isNotEmpty(ouIds)) {
            ousJoinNeeded = true;
            criterias.add("o.id in (:ou_ids)");
            params.put("ou_ids", ouIds);
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
        if(CollectionUtils.isNotEmpty(ouRefIds)) {
            criterias.add("r.ou_ref_id in (:ou_ref_ids)");
            params.put("ou_ref_ids", ouRefIds);
            criterias.add("r.latest = true");
        }
        List<String> selects = new ArrayList<>();
        selects.add("r.*");
        String join = " ";
        if(ousJoinNeeded) {
            selects.add("o.id as requested_ou_id");
            join = join + " JOIN " + company + ".ous as o on r.ou_ref_id=o.ref_id";
        } else {
            selects.add("r.ou_id as requested_ou_id");
        }

        String selectSqlBase = "SELECT " + String.join(",", selects) + " FROM " + company + ".org_dev_productivity_reports_v2 AS r JOIN " + company + ".dev_productivity_profile_ou_mappings AS m ON m.ou_ref_id=r.ou_ref_id AND m.dev_productivity_profile_id=r.dev_productivity_profile_id " + join;
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
        sortBy.add("r.updated_at DESC");
        String orderBy = " ORDER BY " + String.join(",", sortBy);

        String selectSql = selectSqlBase + criteria + orderBy + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSql = "SELECT COUNT(*) FROM (" +  selectSqlBase + criteria + ") AS counted";

        log.debug("sql = " + selectSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        List<OrgDevProductivityReport> results = template.query(selectSql, params, OrgDevProductivityReportsConverters.rowMapper(objectMapper, needRawReport, OrgDevProductivityReportsConverters.RESPONSE_IS_NOT_V1));

        Integer totCount = 0;
        if (results.size() > 0) {
            totCount = results.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
            if (results.size() == pageSize) {
                log.debug("sql = " + countSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
                log.debug("params = {}", params);
                totCount = template.query(countSql, params, CountQueryConverter.countMapper()).get(0);
            }
        }
        log.debug("results.size() = {}, totCount = {}", results.size(), totCount);
        return DbListResponse.of(results, totCount);
    }

    public DbListResponse<OrgDevProductivityReport> listByFilter(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<UUID> ouIds, List<UUID> devProductivityProfileIds, List<ReportIntervalType> intervals, List<Integer> ouRefIds, Map<String, SortingOrder> sort, boolean needRawReport) throws SQLException {
        return getBatch(company, pageNumber, pageSize, ids, ouIds, devProductivityProfileIds, intervals, ouRefIds, sort, needRawReport, null);
    }

    public DbListResponse<OrgDevProductivityReport> listByFilter(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<UUID> ouIds, List<UUID> devProductivityProfileIds, List<ReportIntervalType> intervals, List<Integer> ouRefIds, Map<String, SortingOrder> sort, boolean needRawReport, Boolean latest) throws SQLException {
        return getBatch(company, pageNumber, pageSize, ids, ouIds, devProductivityProfileIds, intervals, ouRefIds, sort, needRawReport, latest);
    }

    // endregion

    // region delete
    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String deleteSql = String.format(DELETE_ORG_PRODUCTIVITY_REPORT_SQL_FORMAT, company);
        return template.update(deleteSql, Map.of("id", UUID.fromString(id))) > 0;
    }
    // endregion

    /*
    https://dba.stackexchange.com/questions/58894/differences-between-match-full-match-simple-and-match-partial
    MATCH SIMPLE if one thing is NULL the constraint is simply ignored.
     */
    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList= List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".org_dev_productivity_reports_v2\n" +
                        "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    ou_id uuid NOT NULL REFERENCES " + company + ".ous(id) ON DELETE CASCADE," +
                        "    ou_ref_id INTEGER NOT NULL," +
                        "    dev_productivity_profile_id UUID  REFERENCES " + company + ".dev_productivity_profiles(id) ON DELETE CASCADE," +
                        "    \"interval\" VARCHAR NOT NULL,\n" +
                        "    start_time TIMESTAMPTZ NOT NULL,\n" +
                        "    end_time TIMESTAMPTZ NOT NULL,\n" +
                        "    week_of_year INTEGER NOT NULL DEFAULT -1,\n" +
                        "    year INTEGER NOT NULL DEFAULT -1,\n" +
                        "    latest BOOLEAN NOT NULL DEFAULT false,\n" +
                        "    score integer,\n" +
                        "    report jsonb,\n" +
                        "    missing_user_reports_count INTEGER NOT NULL DEFAULT 0,\n" +
                        "    stale_user_reports_count INTEGER NOT NULL DEFAULT 0,\n" +
                        "    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),\n" +
                        "    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()\n" +
                        ")",
                "CREATE UNIQUE INDEX IF NOT EXISTS uniq_org_dev_productivity_reports_v2_ou_id_dev_productivity_profile_id_interval_week_of_year_year_idx on " + company + ".org_dev_productivity_reports_v2 (ou_id, dev_productivity_profile_id, interval, week_of_year, year)",
                "CREATE UNIQUE INDEX IF NOT EXISTS uniq_org_dev_productivity_reports_v2_ou_ref_id_dev_productivity_profile_id_interval_week_of_year_year_latest_true_idx on " + company + ".org_dev_productivity_reports_v2 (ou_ref_id, dev_productivity_profile_id, interval,latest, week_of_year, year) WHERE latest=true"
        );
        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
}
