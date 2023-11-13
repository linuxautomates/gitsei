package io.levelops.commons.databases.services.dev_productivity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import io.levelops.commons.databases.converters.CountQueryConverter;
import io.levelops.commons.databases.converters.IndustryDevProductivityReportsConverters;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.dev_productivity.IndustryDevProductivityReport;
import io.levelops.commons.databases.models.database.dev_productivity.ReportIntervalType;
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
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Service
public class IndustryDevProductivityReportDatabaseService extends DatabaseService<IndustryDevProductivityReport> {
    private static final Set<String> SORT_COLUMNS = Set.of("score");
    private static final String LEVELOPS_INVENTORY_SCHEMA = "_levelops";

    @VisibleForTesting
    protected boolean populateData = true;
    private static final String INSERT_INDUSTRY_PRODUCTIVITY_REPORT_SQL_FORMAT = "INSERT INTO %s.industry_dev_productivity_reports(interval, score, report) VALUES (:interval,:score, :report::jsonb) RETURNING id";
    private static final String DELETE_INDUSTRY_PRODUCTIVITY_REPORT_SQL_FORMAT = "DELETE FROM %s.industry_dev_productivity_reports WHERE id = :id";
    private static final String UPSERT_INDUSTRY_PRODUCTIVITY_REPORT_SQL_FORMAT = "INSERT INTO %s.industry_dev_productivity_reports( interval, score, report) " +
                                                                             "VALUES ( :interval, :score, :report::jsonb) " +
                                                                             "ON CONFLICT( interval) DO UPDATE SET ( score, report, updated_at) = (EXCLUDED.score,EXCLUDED.report, now())";
    private final NamedParameterJdbcTemplate template;
    private final ObjectMapper objectMapper;

    public IndustryDevProductivityReportDatabaseService(DataSource dataSource, ObjectMapper objectMapper) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isTenantSpecific() {
        return false;
    }

    @Override
    public SchemaType getSchemaType() {
        return SchemaType.LEVELOPS_INVENTORY_SCHEMA;
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(OrgUnitsDatabaseService.class, DevProductivityProfileDatabaseService.class);
    }

    // region insert upsert common
    private void validateInput(String company, IndustryDevProductivityReport t) {
        Validate.notBlank(company, "Company cannot be null or empty!");

        Validate.notNull(t, "Input Industry Dev Productivity Report cannot be null!");
        Validate.notNull(t.getInterval(),"Interval can not be null");
    }
    private MapSqlParameterSource constructParameterSourceForReport(IndustryDevProductivityReport t, final UUID existingId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("interval", t.getInterval().toString());
        params.addValue("score", t.getScore());
        try{
            params.addValue("report", objectMapper.writeValueAsString(t.getReport()));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize Industry report to JSON", e);
        }
        return params;
    }
    // endregion

    // region upsert
    public String upsert(String company, IndustryDevProductivityReport t) throws SQLException {
        validateInput(company, t);
        MapSqlParameterSource params = constructParameterSourceForReport(t, null);
        String upsertReportSql = String.format(UPSERT_INDUSTRY_PRODUCTIVITY_REPORT_SQL_FORMAT, LEVELOPS_INVENTORY_SCHEMA);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(upsertReportSql, params, keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            throw new SQLException("Failed to upsert industry productivity report record!! " + t.toString());
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }
    // endregion

    // region insert
    @Override
    public String insert(String company, IndustryDevProductivityReport t) throws SQLException {
        return insertReport(company, t).toString();
    }

    private UUID insertReport(String company, IndustryDevProductivityReport t) throws SQLException {
        MapSqlParameterSource params = constructParameterSourceForReport(t, null);
        String insertReportSql = String.format(INSERT_INDUSTRY_PRODUCTIVITY_REPORT_SQL_FORMAT, LEVELOPS_INVENTORY_SCHEMA);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        UUID reportId;
        try {
            int updatedRows = template.update(insertReportSql, params, keyHolder);
            if (updatedRows <= 0 || keyHolder.getKeys() == null) {
                throw new SQLException("Failed to insert industry dev productivity report");
            }
            reportId = (UUID) keyHolder.getKeys().get("id");
        } catch (Exception e) {
            log.error("Error inserting industry dev productivity report", e);
            throw new SQLException("could not insert industry dev productivity report");
        }
        return reportId;
    }
    // endregion

    // region update
    @Override
    public Boolean update(String company, IndustryDevProductivityReport t) throws SQLException {
        return updateUserProdReport(company,t);
    }

    private Boolean updateUserProdReport(String company, IndustryDevProductivityReport t) throws SQLException {
        //Validate.notBlank(t.getId(), "id cannot be null or empty.");
        UUID reportId = t.getId();
        Boolean updated = false;
        List<String> updates = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        params.put("id", reportId);

        boolean unsetCurrentDefaultScheme = false;
        try {
            if (t.getInterval() != null) {
                updates.add("interval = :interval");
                params.put("interval", t.getInterval().toString());
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
            String sql = "UPDATE " + LEVELOPS_INVENTORY_SCHEMA + ".industry_dev_productivity_reports " +
                    " SET " + String.join(", ", updates) +
                    " WHERE id = :id::uuid ";

            updated =  template.update(sql, params) > 0;
        }catch(Exception e){
            log.error("Error while updating the org report table industry_dev_productivity_reports"+e);
            throw e;
        }
        return updated;
    }

    @Override
    public Optional<IndustryDevProductivityReport> get(String company, String id) throws SQLException {
        var results = getBatch(LEVELOPS_INVENTORY_SCHEMA, 0, 10, Collections.singletonList(UUID.fromString(id)), null, null).getRecords();
        return results.size() > 0 ? Optional.of(results.get(0)) : Optional.empty();
    }

    @Override
    public DbListResponse<IndustryDevProductivityReport> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return getBatch(company, pageNumber, pageSize, null, null, null);
    }

    private DbListResponse<IndustryDevProductivityReport> getBatch(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<ReportIntervalType> intervals, Map<String, SortingOrder> sort) throws SQLException {
        List<String> criterias = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        if(CollectionUtils.isNotEmpty(ids)) {
            criterias.add("id in (:ids)");
            params.put("ids", ids);
        }
        if(CollectionUtils.isNotEmpty(intervals)) {
            criterias.add("interval in (:intervals)");
            params.put("intervals", intervals.stream().map(Objects::toString).collect(Collectors.toList()));
        }

        String selectSqlBase = "SELECT * FROM " + LEVELOPS_INVENTORY_SCHEMA + ".industry_dev_productivity_reports";
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
        List<IndustryDevProductivityReport> results = template.query(selectSql, params, IndustryDevProductivityReportsConverters.rowMapper(objectMapper));

        Integer totCount = 0;
        if (results.size() > 0) {
            totCount = results.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
            if (results.size() == pageSize) {
                log.info("sql = " + countSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
                log.info("params = {}", params);
                totCount = template.query(countSql, params, CountQueryConverter.countMapper()).get(0);
            }
        }
        log.info("results.size() = {}, totCount = {}", results.size(), totCount);
        return DbListResponse.of(results, totCount);
    }
    public DbListResponse<IndustryDevProductivityReport> listByFilter(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<ReportIntervalType> intervals, Map<String, SortingOrder> sort) throws SQLException {
        return getBatch(company, pageNumber, pageSize, ids, intervals, sort);
    }

    // endregion

    // region delete
    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String deleteSql = String.format(DELETE_INDUSTRY_PRODUCTIVITY_REPORT_SQL_FORMAT, LEVELOPS_INVENTORY_SCHEMA);
        return template.update(deleteSql, Map.of("id", UUID.fromString(id))) > 0;
    }
    // endregion


    @Override
    public Boolean ensureTableExistence(String schema) throws SQLException {
        ensureSchemaExistence(LEVELOPS_INVENTORY_SCHEMA);
        List<String> sqlList= List.of(
                "CREATE TABLE IF NOT EXISTS " + LEVELOPS_INVENTORY_SCHEMA + ".industry_dev_productivity_reports\n" +
                        "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    \"interval\" VARCHAR NOT NULL,\n" +
                        "    score integer,\n" +
                        "    report jsonb,\n" +
                        "    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),\n" +
                        "    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()\n" +
                        ")",
                "CREATE UNIQUE INDEX IF NOT EXISTS uniq_industry_dev_productivity_reports_interval_idx on " + LEVELOPS_INVENTORY_SCHEMA + ".industry_dev_productivity_reports(interval)"
        );
        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
}
