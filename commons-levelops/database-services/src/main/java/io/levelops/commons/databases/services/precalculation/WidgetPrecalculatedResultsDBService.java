package io.levelops.commons.databases.services.precalculation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.converters.CountQueryConverter;
import io.levelops.commons.databases.converters.precalculation.WidgetPrecalculatedReportConverters;
import io.levelops.commons.databases.models.database.precalculation.WidgetPrecalculatedReport;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Log4j2
@Service
public class WidgetPrecalculatedResultsDBService extends DatabaseService<WidgetPrecalculatedReport> {
    private static final String UPSERT_WIDGET_PRECALCULATION_REPORT_SQL_FORMAT = "INSERT INTO %s.widget_precalculated_reports(widget_id, widget, list_request, ou_ref_id,  ou_id, report_sub_type, report, calculated_at, interval, start_time, end_time) " +
            "VALUES (:widget_id, :widget::jsonb, :list_request::jsonb, :ou_ref_id,  :ou_id, :report_sub_type, :report::jsonb, :calculated_at, :interval, :start_time, :end_time) " +
            "ON CONFLICT(widget_id, ou_ref_id, report_sub_type, interval) DO UPDATE SET (widget, list_request, ou_id, report_sub_type, report, calculated_at, start_time, end_time, updated_at) = (EXCLUDED.widget, EXCLUDED.list_request, EXCLUDED.ou_id, EXCLUDED.report_sub_type, EXCLUDED.report, EXCLUDED.calculated_at, EXCLUDED.start_time, EXCLUDED.end_time, now())";

    private final NamedParameterJdbcTemplate template;
    private final ObjectMapper objectMapper;

    public WidgetPrecalculatedResultsDBService(DataSource dataSource, ObjectMapper objectMapper) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.objectMapper = objectMapper;
    }

    // region Get References
    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(DashboardWidgetService.class, OrgUnitsDatabaseService.class);
    }
    // endregion

    // region Not Implemented
    @Override
    public String insert(String company, WidgetPrecalculatedReport t) throws SQLException {
        return null;
    }
    @Override
    public Boolean update(String company, WidgetPrecalculatedReport t) throws SQLException {
        return null;
    }
    @Override
    public Boolean delete(String company, String id) throws SQLException {
        return null;
    }
    // endregion

    //region Get and List
    @Override
    public Optional<WidgetPrecalculatedReport> get(String company, String id) throws SQLException {
        var results = getBatch(company, 0, 10, Collections.singletonList(UUID.fromString(id)), null, null, null, null, null).getRecords();
        return results.size() > 0 ? Optional.of(results.get(0)) : Optional.empty();
    }

    @Override
    public DbListResponse<WidgetPrecalculatedReport> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return getBatch(company, pageNumber, pageSize, null, null, null, null, null, null);
    }

    public DbListResponse<WidgetPrecalculatedReport> listByFilter(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<UUID> widgetIds, List<Integer> ouRefIds, List<String> reportSubTypes, List<String> intervals, String reportSubTypeStartsWith) throws SQLException {
        return getBatch(company, pageNumber, pageSize, ids, widgetIds, ouRefIds, reportSubTypes, intervals, reportSubTypeStartsWith);
    }

    public DbListResponse<WidgetPrecalculatedReport> getBatch(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<UUID> widgetIds, List<Integer> ouRefIds, List<String> reportSubTypes, List<String> intervals, String reportSubTypeStartsWith) throws SQLException {
        List<String> criterias = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        if(CollectionUtils.isNotEmpty(ids)) {
            criterias.add("id in (:ids)");
            params.put("ids", ids);
        }
        if(CollectionUtils.isNotEmpty(widgetIds)) {
            criterias.add("widget_id in (:widget_ids)");
            params.put("widget_ids", widgetIds);
        }
        if(CollectionUtils.isNotEmpty(ouRefIds)) {
            criterias.add("ou_ref_id in (:ou_ref_ids)");
            params.put("ou_ref_ids", ouRefIds);
        }
        if(CollectionUtils.isNotEmpty(reportSubTypes)) {
            criterias.add("report_sub_type in (:report_sub_types)");
            params.put("report_sub_types", reportSubTypes);
        }
        if(CollectionUtils.isNotEmpty(intervals)) {
            criterias.add("interval in (:intervals)");
            params.put("intervals", intervals);
        }
        if(StringUtils.isNotBlank(reportSubTypeStartsWith)) {
            criterias.add("report_sub_type LIKE :report_sub_type_starts_with");
            params.put("report_sub_type_starts_with", reportSubTypeStartsWith + "%");
        }

        String selectSqlBase = "SELECT * FROM " + company + ".widget_precalculated_reports";
        String criteria = "";
        if(CollectionUtils.isNotEmpty(criterias)) {
            criteria = " WHERE " + String.join(" AND ", criterias);
        }

        List<String> sortBy = new ArrayList<>();
        sortBy.add("updated_at DESC");
        String orderBy = " ORDER BY " + String.join(",", sortBy);

        String selectSql = selectSqlBase + criteria + orderBy + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSql = "SELECT COUNT(*) FROM (" +  selectSqlBase + criteria + ") AS counted";

        log.info("sql = " + selectSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<WidgetPrecalculatedReport> results = template.query(selectSql, params, WidgetPrecalculatedReportConverters.rowMapper(objectMapper));

        Integer totCount = 0;
        if (results.size() > 0) {
            totCount = results.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
            if (results.size() == pageSize) {
                totCount = template.query(countSql, params, CountQueryConverter.countMapper()).get(0);
            }
        }
        return DbListResponse.of(results, totCount);
    }
    // endregion



    // region insert upsert common
    private void validateInput(String company, WidgetPrecalculatedReport t) {
        Validate.notBlank(company, "Company cannot be null or empty!");
        Validate.notNull(t, "Input Widget Precalculated Report cannot be null!");

        Validate.notNull(t.getWidgetId(),"Widget Id can not be null");
        Validate.notNull(t.getWidget(),"Widget can not be null");

        Validate.notNull(t.getOuRefId(),"OU Ref Id can not be null");
        
        Validate.notNull(t.getReportSubType(),"Report Sub Type can not be null");
        Validate.notNull(t.getCalculatedAt(),"Report Calculated At can not be null");

        Validate.notNull(t.getInterval(),"Interval can not be null");
    }
    private MapSqlParameterSource constructParameterSourceForReport(WidgetPrecalculatedReport t) {
        MapSqlParameterSource params = new MapSqlParameterSource();

        params.addValue("widget_id", t.getWidgetId());
        try {
            params.addValue("widget", objectMapper.writeValueAsString(t.getWidget()));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize widget to JSON", e);
        }
        try {
            params.addValue("list_request", objectMapper.writeValueAsString(t.getListRequest()));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize list_request to JSON", e);
        }

        params.addValue("ou_ref_id", t.getOuRefId());
        params.addValue("ou_id", t.getOuID());

        params.addValue("report_sub_type", t.getReportSubType());
        try {
            params.addValue("report", objectMapper.writeValueAsString(t.getReport()));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize report to JSON", e);
        }
        params.addValue("calculated_at", Timestamp.from(t.getCalculatedAt()));

        params.addValue("interval", t.getInterval());
        params.addValue("start_time", (t.getStartTime() != null) ? Timestamp.from(t.getStartTime()) : null);
        params.addValue("end_time", (t.getEndTime() != null) ? Timestamp.from(t.getEndTime()) : null);

        return params;
    }
    // endregion

    //region Upsert
    public UUID upsert(String company, WidgetPrecalculatedReport t) throws SQLException {
        validateInput(company, t);
        MapSqlParameterSource params = constructParameterSourceForReport(t);
        String upsertReportSql = String.format(UPSERT_WIDGET_PRECALCULATION_REPORT_SQL_FORMAT, company);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(upsertReportSql, params, keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            throw new SQLException("Failed to upsert dev productivity report record!! " + t.toString());
        }
        UUID id = (UUID) keyHolder.getKeys().get("id");
        return id;
    }
    // endregion

    //region ensureTableExistence
    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList= List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".widget_precalculated_reports\n" +
                        "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +

                        "    widget_id UUID NOT NULL,\n" +
                        "    widget JSONB NOT NULL,\n" +
                        "    list_request JSONB,\n" +

                        "    ou_ref_id INTEGER NOT NULL," +
                        "    ou_id uuid REFERENCES " + company + ".ous(id) ON DELETE CASCADE," +

                        "    report_sub_type VARCHAR NOT NULL,\n" +
                        "    report JSONB,\n" +
                        "    calculated_at TIMESTAMPTZ NOT NULL,\n" +

                        "    \"interval\" VARCHAR NOT NULL,\n" +
                        "    start_time TIMESTAMPTZ,\n" +
                        "    end_time TIMESTAMPTZ,\n" +

                        "    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),\n" +
                        "    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()\n" +
                        ")",
                "CREATE UNIQUE INDEX IF NOT EXISTS uniq_widget_precalculated_reports_widget_id_ou_ref_id_report_sub_type_interval_idx on " + company + ".widget_precalculated_reports (widget_id, ou_ref_id, report_sub_type, interval)"
        );
        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
    // endregion
}
