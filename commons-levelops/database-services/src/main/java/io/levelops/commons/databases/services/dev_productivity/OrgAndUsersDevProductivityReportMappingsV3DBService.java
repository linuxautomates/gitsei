package io.levelops.commons.databases.services.dev_productivity;

import io.levelops.commons.databases.converters.CountQueryConverter;
import io.levelops.commons.databases.converters.OrgAndUsersDevProductivityReportMappingsConverters;
import io.levelops.commons.databases.models.database.dev_productivity.OrgAndUsersDevProductivityReportMappings;
import io.levelops.commons.databases.models.database.dev_productivity.ReportIntervalType;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class OrgAndUsersDevProductivityReportMappingsV3DBService extends DatabaseService<OrgAndUsersDevProductivityReportMappings> {
    private final NamedParameterJdbcTemplate template;

    private static final String UPSERT_SQL_FORMAT = "INSERT INTO %s.org_and_user_dev_prod_reports_mappings_v3(ou_id, dev_productivity_parent_profile_id, interval, org_user_ids) " +
            "VALUES (:ou_id, :dev_productivity_parent_profile_id, :interval, :org_user_ids) " +
            "ON CONFLICT(ou_id, dev_productivity_parent_profile_id, interval) DO UPDATE SET (org_user_ids, updated_at) = (EXCLUDED.org_user_ids, now()) RETURNING id";

    // region CSTOR
    public OrgAndUsersDevProductivityReportMappingsV3DBService(DataSource dataSource) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }
    // endregion

    // region get references
    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(OrgUnitsDatabaseService.class, DevProductivityProfileDatabaseService.class);
    }
    // endregion

    // region insert upsert common
    private void validateInput(String company, OrgAndUsersDevProductivityReportMappings t) {
        Validate.notBlank(company, "Company cannot be null or empty!");

        Validate.notNull(t, "Org and Users Dev Productivity Report Mappings cannot be null!");
        Validate.notNull(t.getOuID(),"OU Id can not be null");
        Validate.notNull(t.getDevProductivityParentProfileId(),"Dev Productivity Profile Id can not be null");
        Validate.notNull(t.getInterval(),"Interval can not be null");
    }
    private MapSqlParameterSource constructParameterSourceForReport(OrgAndUsersDevProductivityReportMappings t) {
        List<String> orgUserIds = CollectionUtils.emptyIfNull(t.getOrgUserIds()).stream().map(Objects::toString).collect(Collectors.toList());
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ou_id", t.getOuID());
        params.addValue("dev_productivity_parent_profile_id", t.getDevProductivityParentProfileId());
        params.addValue("interval", t.getInterval().toString());
        params.addValue("org_user_ids", orgUserIds.toArray(new String[0]));
        return params;
    }
    // endregion

    // region Upsert
    public String upsert(String company, OrgAndUsersDevProductivityReportMappings t) throws SQLException {
        validateInput(company, t);
        MapSqlParameterSource params = constructParameterSourceForReport(t);
        String upsertReportSql = String.format(UPSERT_SQL_FORMAT, company);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(upsertReportSql, params, keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            throw new SQLException("Failed to upsert org and users dev productivity report mapping!! " + t.toString());
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }
    // endregion

    // region Insert
    @Override
    public String insert(String company, OrgAndUsersDevProductivityReportMappings t) throws SQLException {
        throw new UnsupportedOperationException();
    }
    // endregion

    // region Update
    @Override
    public Boolean update(String company, OrgAndUsersDevProductivityReportMappings t) throws SQLException {
        throw new UnsupportedOperationException();
    }
    // endregion

    // region Get
    @Override
    public Optional<OrgAndUsersDevProductivityReportMappings> get(String company, String param) throws SQLException {
        throw new UnsupportedOperationException();
    }
    // endregion

    // region List
    @Override
    public DbListResponse<OrgAndUsersDevProductivityReportMappings> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return null;
    }
    private DbListResponse<OrgAndUsersDevProductivityReportMappings> getBatch(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<UUID> ouIds, List<UUID> devProductivityParentProfileIds, List<ReportIntervalType> intervals, List<Integer> ouRefIds) {
        List<String> criterias = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        if(CollectionUtils.isNotEmpty(ids)) {
            criterias.add("id in (:ids)");
            params.put("ids", ids);
        }
        if(CollectionUtils.isNotEmpty(ouIds)) {
            criterias.add("ou_id in (:ou_ids)");
            params.put("ou_ids", ouIds);
        }
        if(CollectionUtils.isNotEmpty(devProductivityParentProfileIds)) {
            criterias.add("dev_productivity_parent_profile_id in (:dev_productivity_parent_profile_ids)");
            params.put("dev_productivity_parent_profile_ids", devProductivityParentProfileIds);
        }
        if(CollectionUtils.isNotEmpty(intervals)) {
            criterias.add("interval in (:intervals)");
            params.put("intervals", intervals.stream().map(Objects::toString).collect(Collectors.toList()));
        }
        if(CollectionUtils.isNotEmpty(ouRefIds)) {
            criterias.add("ou_id in (select id from " + company + ".ous where active = true and ref_id in (:ou_ref_ids))");
            params.put("ou_ref_ids", ouRefIds);
        }

        String selectSqlBase = "SELECT * FROM " + company + ".org_and_user_dev_prod_reports_mappings_v3";
        String criteria = "";
        if(CollectionUtils.isNotEmpty(criterias)) {
            criteria = " WHERE " + String.join(" AND ", criterias);
        }
        String orderBy = " ORDER BY updated_at DESC ";

        String selectSql = selectSqlBase + criteria + orderBy + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSql = "SELECT COUNT(*) FROM (" +  selectSqlBase + criteria + ") AS counted";

        log.debug("sql = " + selectSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        List<OrgAndUsersDevProductivityReportMappings> results = template.query(selectSql, params, OrgAndUsersDevProductivityReportMappingsConverters.rowMapper());

        Integer totCount = 0;
        if (results.size() > 0) {
            totCount = results.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
            if (results.size() == pageSize) {
                log.debug("sql = " + countSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
                log.debug("params = {}", params);
                totCount = template.query(countSql, params, CountQueryConverter.countMapper()).get(0);
            }
        }
        return DbListResponse.of(results, totCount);
    }
    public DbListResponse<OrgAndUsersDevProductivityReportMappings> listByFilter(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<UUID> ouIds, List<UUID> devProductivityParentProfileIds, List<ReportIntervalType> intervals, List<Integer> ouRefIds) throws SQLException {
        return getBatch(company, pageNumber, pageSize, ids, ouIds, devProductivityParentProfileIds, intervals, ouRefIds);
    }
    // endregion

    // region Delete
    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException();
    }
    // endregion

    // region Ensure Table Existence
    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".org_and_user_dev_prod_reports_mappings_v3 (" +
                        "   id                              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "   ou_id                           UUID NOT NULL REFERENCES " + company + ".ous(id) ON DELETE CASCADE," +
                        "   dev_productivity_parent_profile_id     UUID  REFERENCES " + company + ".dev_productivity_parent_profiles(id) ON DELETE CASCADE,\n" +
                        "   interval                        VARCHAR NOT NULL,\n" +
                        "   org_user_ids                    VARCHAR[] NOT NULL DEFAULT '{}'::VARCHAR[],\n" +
                        "   created_at                      TIMESTAMPTZ NOT NULL DEFAULT now(),\n" +
                        "   updated_at                      TIMESTAMPTZ NOT NULL DEFAULT now()\n" +
                        ")",
                "CREATE UNIQUE INDEX IF NOT EXISTS uniq_org_and_user_dev_prod_reports_mappings_v3_ou_id_dev_productivity_parent_profile_id_interval_idx on " + company + "." + "org_and_user_dev_prod_reports_mappings_v3 (ou_id, dev_productivity_parent_profile_id, interval)"
        );

        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
    // endregion
}
