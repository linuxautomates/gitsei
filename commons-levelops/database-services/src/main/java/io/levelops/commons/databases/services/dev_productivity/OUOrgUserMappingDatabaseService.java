package io.levelops.commons.databases.services.dev_productivity;

import io.levelops.commons.databases.converters.CountQueryConverter;
import io.levelops.commons.databases.models.database.dev_productivity.OuOrgUserMappings;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.*;

@Log4j2
@Service
public class OUOrgUserMappingDatabaseService extends DatabaseService<OuOrgUserMappings> {
    private final NamedParameterJdbcTemplate template;

    private static final String UPSERT_OU_ORG_USER_ID_MAPPING_SQL_FORMAT = "INSERT INTO %s.ou_org_user_mappings (ou_id,org_user_id) "
            + "VALUES %s "
            + "ON CONFLICT(ou_id,org_user_id) DO UPDATE SET updated_at = now()";

    // region CSTOR
    public OUOrgUserMappingDatabaseService(DataSource dataSource) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }
    // endregion

    // region get references
    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(OrgUnitsDatabaseService.class, OrgUsersDatabaseService.class);
    }
    // endregion


    // region Insert
    @Override
    public String insert(String company, OuOrgUserMappings t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int upsertOUOrgUserMappingsBulk(String company, UUID ouId, List<UUID> orgUserIds) {
        Map<String, Object> params = new HashMap<>();
        List<String> values = new ArrayList<>();
        for(UUID orgUserId : orgUserIds){
            int i = values.size();
            params.putAll(Map.of(
                    "ou_id_" + i, ouId,
                    "org_user_id_" + i, orgUserId
            ));
            values.add(MessageFormat.format("(:ou_id_{0}, :org_user_id_{0})", String.valueOf(i)));
        }
        if (values.isEmpty()) {
            log.warn("Skipping bulk insert: no valid data provided");
            return 0;
        }
        String insertOUOrgUserIdMappingSQL = String.format(UPSERT_OU_ORG_USER_ID_MAPPING_SQL_FORMAT, company, String.join(", ",values));
        return template.update(insertOUOrgUserIdMappingSQL, new MapSqlParameterSource(params));
    }
    // endregion

    // region Update
    @Override
    public Boolean update(String company, OuOrgUserMappings t) throws SQLException {
        throw new UnsupportedOperationException();
    }
    // endregion

    // region Get
    @Override
    public Optional<OuOrgUserMappings> get(String company, String param) throws SQLException {
        throw new UnsupportedOperationException();
    }
    // endregion

    // region List
    @Override
    public DbListResponse<OuOrgUserMappings> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return null;
    }
    private DbListResponse<OuOrgUserMappings> getBatch(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<UUID> ouIds, List<UUID> orgUserIds) {
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
        if(CollectionUtils.isNotEmpty(orgUserIds)) {
            criterias.add("org_user_id in (:org_user_ids)");
            params.put("org_user_ids", orgUserIds);
        }

        String selectSqlBase = "SELECT * FROM " + company + ".ou_org_user_mappings";
        String criteria = "";
        if(CollectionUtils.isNotEmpty(criterias)) {
            criteria = " WHERE " + String.join(" AND ", criterias);
        }
        String orderBy = " ORDER BY updated_at DESC ";

        String selectSql = selectSqlBase + criteria + orderBy + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSql = "SELECT COUNT(*) FROM (" +  selectSqlBase + criteria + ") AS counted";

        log.info("sql = " + selectSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<OuOrgUserMappings> results = template.query(selectSql, params, new RowMapper<OuOrgUserMappings>() {
            @Override
            public OuOrgUserMappings mapRow(ResultSet rs, int rowNum) throws SQLException {
                return OuOrgUserMappings.builder()
                        .id((UUID) rs.getObject("id"))
                        .ouId((UUID) rs.getObject("ou_id"))
                        .orgUserId((UUID) rs.getObject("org_user_id"))
                        .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                        .updatedAt(DateUtils.toInstant(rs.getTimestamp("updated_at")))
                        .build();
            }
        });

        Integer totCount = 0;
        if (results.size() > 0) {
            totCount = results.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
            if (results.size() == pageSize) {
                log.info("sql = " + countSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
                log.info("params = {}", params);
                totCount = template.query(countSql, params, CountQueryConverter.countMapper()).get(0);
            }
        }
        return DbListResponse.of(results, totCount);
    }
    public DbListResponse<OuOrgUserMappings> listByFilter(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<UUID> ouIds, List<UUID> orgUserIds) throws SQLException {
        return getBatch(company, pageNumber, pageSize, ids, ouIds,orgUserIds);
    }
    // endregion

    // region Delete
    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Boolean deleteOuOrgUserMappings(String company, UUID ouId, List<UUID> orgUserIds){
        String deleteSql = "DELETE FROM "+company+".ou_org_user_mappings WHERE ou_id = :ou_id AND org_user_id IN (:org_user_ids)";
        return template.update(deleteSql, Map.of("ou_id", ouId, "org_user_ids",orgUserIds)) > 0;
    }
    // endregion

    // region Ensure Table Existence
    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".ou_org_user_mappings (" +
                        "   id                              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "   ou_id                           UUID NOT NULL REFERENCES " + company + ".ous(id) ON DELETE CASCADE," +
                        "   org_user_id                     UUID NOT NULL REFERENCES " + company + ".org_users(id) ON DELETE CASCADE,\n" +
                        "   created_at                      TIMESTAMPTZ NOT NULL DEFAULT now(),\n" +
                        "   updated_at                      TIMESTAMPTZ NOT NULL DEFAULT now()\n" +
                        ")",
                "CREATE UNIQUE INDEX IF NOT EXISTS uniq_ou_org_user_mappings_ou_id_org_user_id_idx on " + company + "." + "ou_org_user_mappings (ou_id, org_user_id)"
        );

        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
    // endregion
}
