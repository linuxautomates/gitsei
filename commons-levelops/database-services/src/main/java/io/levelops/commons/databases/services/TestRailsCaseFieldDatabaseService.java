package io.levelops.commons.databases.services;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.DbTestRailsConverters;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsCaseField;
import io.levelops.commons.databases.models.filters.TestRailsCaseFieldFilter;
import io.levelops.commons.databases.utils.TransactionCallback;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ListUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.converters.DbTestRailsConverters.listRowMapperForCaseField;

@Service
@Log4j2
public class TestRailsCaseFieldDatabaseService extends DatabaseService<DbTestRailsCaseField>  {
    public static final String TESTRAILS_CASE_FIELDS = "testrails_case_fields";
    public static final String TESTRAILS_CASE_FIELD_TEMP_TABLE = "testrails_case_field_temp_table_";
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public TestRailsCaseFieldDatabaseService(DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public String insert(String company, DbTestRailsCaseField caseField) throws SQLException {
        return template.getJdbcTemplate().execute(TransactionCallback.of(conn -> {
            int integrationId = NumberUtils.toInt(caseField.getIntegrationId());
            String sql = "INSERT INTO " + company + "." + TESTRAILS_CASE_FIELDS +
                    " (integration_id, case_field_id, is_active, is_global, name, label, system_name, type, project_ids, created_at, updated_at)" +
                    " VALUES " +
                    " (?,?,?,?,?,?,?,?,?, 'now', 'now')" +
                    " ON CONFLICT (system_name, integration_id)" +
                    " DO UPDATE" +
                    " SET " +
                    "   case_field_id = EXCLUDED.case_field_id," +
                    "   is_active = EXCLUDED.is_active," +
                    "   is_global = EXCLUDED.is_global," +
                    "   name = EXCLUDED.name," +
                    "   label = EXCLUDED.label," +
                    "   type = EXCLUDED.type," +
                    "   project_ids = EXCLUDED.project_ids," +
                    "   updated_at = EXCLUDED.updated_at";
            String id = null;
            try (PreparedStatement caseFieldStmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
                int i = 1;
                caseFieldStmt.setObject(i++, integrationId);
                caseFieldStmt.setObject(i++, caseField.getCaseFieldId());
                caseFieldStmt.setObject(i++, caseField.getIsActive());
                caseFieldStmt.setObject(i++, caseField.getIsGlobal());
                caseFieldStmt.setObject(i++,caseField.getName());
                caseFieldStmt.setObject(i++, caseField.getLabel());
                caseFieldStmt.setObject(i++, caseField.getSystemName());
                caseFieldStmt.setObject(i++, caseField.getType());
                caseFieldStmt.setObject(i++, conn.createArrayOf("integer", ListUtils.emptyIfNull(caseField.getProjectIds()).toArray()));
                caseFieldStmt.executeUpdate();
                try (ResultSet rs = caseFieldStmt.getGeneratedKeys()) {
                    if (rs.next()){
                        id = rs.getString(1);
                    }
                }
            }
            return id;
        }));
    }

    @Override
    public Boolean update(String company, DbTestRailsCaseField t) throws SQLException {
        return null;
    }

    @Override
    public Optional<DbTestRailsCaseField> get(String company, String param) throws SQLException {
        return Optional.empty();
    }

    @Override
    public DbListResponse<DbTestRailsCaseField> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return listByFilter(company, TestRailsCaseFieldFilter.builder().build(), pageNumber, pageSize);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        return false;
    }

    public DbListResponse<DbTestRailsCaseField> listByFilter(String company,
                                                             TestRailsCaseFieldFilter filter,
                                                             Integer pageNumber,
                                                             Integer pageSize) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        List<String> integrationIds = filter.getIntegrationIds();
        List<String> systemNames = filter.getSystemNames();
        Boolean isActive = filter.getIsActive();
        Integer caseFieldId = filter.getCaseFieldId();
        Boolean needAssignedFieldsOnly = filter.getNeedAssignedFieldsOnly();
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            conditions.add("integration_id IN (:integrationIds)");
            params.put("integrationIds", integrationIds.stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (caseFieldId != null) {
            conditions.add("case_field_id IN (:caseFieldId)");
            params.put("caseFieldId", caseFieldId);
        }
        if(CollectionUtils.isNotEmpty(systemNames)){
            conditions.add("system_name IN (:systemNames)");
            params.put("systemNames", systemNames);
        }
        if(isActive != null) {
            conditions.add("is_active = :is_active");
            params.put("is_active", isActive);
        }
        if(needAssignedFieldsOnly != null && needAssignedFieldsOnly) {
            conditions.add("is_global is not null");
        }
        String criteria = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String SQL = "SELECT id,case_field_id,label,is_active,name,system_name,type,integration_id,is_global,project_ids"
                + " FROM " + company + "." + TESTRAILS_CASE_FIELDS
                + criteria
                + " LIMIT " + pageSize
                + " OFFSET " + (pageNumber * pageSize);
        log.info("sql = " + SQL);
        log.info("params = {}", params);
        List<DbTestRailsCaseField> caseFields = template.query(SQL, params, listRowMapperForCaseField());

        String countSQL = "SELECT COUNT(*) FROM " + company + "." + TESTRAILS_CASE_FIELDS + criteria;
        int totCount = 0;
        if (caseFields.size() > 0) {
            totCount = caseFields.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
            if (caseFields.size() == pageSize) {
                totCount = MoreObjects.firstNonNull(template.queryForObject(countSQL, params, Integer.class), totCount);
            }
        }
        return DbListResponse.of(caseFields, totCount);
    }

    public List<DbTestRailsCaseField> getByIds(String company, List<String> ids){
        String sql = "SELECT * FROM " + company + "." + TESTRAILS_CASE_FIELDS + " WHERE id IN (:ids)";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ids", ids.stream().map(UUID::fromString).collect(Collectors.toList()));
        return template.query(sql, params, DbTestRailsConverters.listRowMapperForCaseField());
    }

    public void cleanUpRecordsByIds(String company, List<String> ids){
        String sql = "DELETE FROM " + company + "." + TESTRAILS_CASE_FIELDS + " WHERE id IN (:ids)";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ids", ids.stream().map(UUID::fromString).collect(Collectors.toList()));
        template.update(sql, params);
    }

    public void createTempTable(String company, String integrationId, String currentTime) {
        String createSql = "CREATE TABLE IF NOT EXISTS " + company + "." + (TESTRAILS_CASE_FIELD_TEMP_TABLE + integrationId + "_" + currentTime) +
                " (id INTEGER NOT NULL)";
        template.getJdbcTemplate().execute(createSql);
        log.info("Successfully created temp table of casefield : " + company + "." + (TESTRAILS_CASE_FIELD_TEMP_TABLE + integrationId + "_" + currentTime));
    }

    public int insertIntoTempTable(String company, String integrationId, String currentTime, Integer id) {
        String insertSql = "INSERT INTO " + company + "." + (TESTRAILS_CASE_FIELD_TEMP_TABLE + integrationId + "_" + currentTime) +
                " VALUES(:id)";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        int affectedRows = template.update(insertSql, params);
        return affectedRows;
    }
    public void dropTempTable(String company, String integrationId, String currentTime) {
        String dropSql = "DROP TABLE IF EXISTS " + company + "." + (TESTRAILS_CASE_FIELD_TEMP_TABLE + integrationId + "_" + currentTime);
        template.getJdbcTemplate().execute(dropSql);
        log.debug("Successfully dropped temp table of casefield: " + company + "." + (TESTRAILS_CASE_FIELD_TEMP_TABLE + integrationId + "_" + currentTime));
    }
    public int deleteCaseFieldRecords(String company, String integrationId, String currentTime) {
        String deleteSql = "DELETE FROM " + company + "." + TESTRAILS_CASE_FIELDS +
                " using " + company + "." + TESTRAILS_CASE_FIELDS + " as cf" +
                " left join " + company + "." + (TESTRAILS_CASE_FIELD_TEMP_TABLE + integrationId + "_" + currentTime) +
                " as temp on temp.id = cf.case_field_id" +
                " WHERE cf.id = " + company + "." + TESTRAILS_CASE_FIELDS + "." + "id" +
                " AND cf.integration_id=:integration_id AND temp.id is null";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("integration_id", Integer.parseInt(integrationId));
        return template.update(deleteSql, params);
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        final List<String> ddlStmts = List.of(" CREATE TABLE IF NOT EXISTS " + company + "." + TESTRAILS_CASE_FIELDS +
                " (" +
                " id UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                " integration_id INTEGER NOT NULL REFERENCES "
                + company + ".integrations(id) ON DELETE CASCADE," +
                " case_field_id INTEGER NOT NULL," +
                " is_active BOOLEAN," +
                " is_global BOOLEAN," +
                " name VARCHAR," +
                " label VARCHAR NOT NULL," +
                " system_name VARCHAR NOT NULL," +
                " type VARCHAR NOT NULL," +
                " project_ids INTEGER[]," +
                " created_at TIMESTAMP WITH TIME ZONE NOT NULL," +
                " updated_at TIMESTAMP WITH TIME ZONE NOT NULL," +
                " UNIQUE(integration_id, system_name)" +
                ")");
        ddlStmts.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
}
