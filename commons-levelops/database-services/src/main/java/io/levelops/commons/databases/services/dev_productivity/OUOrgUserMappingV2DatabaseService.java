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
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;


@Log4j2
@Service
public class OUOrgUserMappingV2DatabaseService extends DatabaseService<OuOrgUserMappings> {
    private final NamedParameterJdbcTemplate template;
    private final PlatformTransactionManager transactionManager;

    private static final String INSERT_NEW_OU_ORG_USER_ID_MAPPING_SQL_FORMAT = "INSERT INTO %s.ou_org_user_mappings_v2 (ou_ref_id,org_user_ref_id) "
            + "VALUES %s "
            + "ON CONFLICT(ou_ref_id,org_user_ref_id) DO NOTHING";

    // region CSTOR
    public OUOrgUserMappingV2DatabaseService(DataSource dataSource) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.transactionManager = new DataSourceTransactionManager(dataSource);
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
    // endregion

    // region Sync Flow
    private int batchInsertNew(String company, Integer ouRefId, List<Integer> orgUserRefIds) {
        Map<String, Object> params = new HashMap<>();
        List<String> values = new ArrayList<>();
        for(Integer orgUserRefId : orgUserRefIds){
            int i = values.size();
            params.putAll(Map.of(
                    "ou_ref_id_" + i, ouRefId,
                    "org_user_ref_id_" + i, orgUserRefId
            ));
            values.add(MessageFormat.format("(:ou_ref_id_{0}, :org_user_ref_id_{0})", String.valueOf(i)));
        }
        if (values.isEmpty()) {
            log.warn("Skipping bulk insert: no valid data provided");
            return 0;
        }
        String insertOUOrgUserIdMappingSQL = String.format(INSERT_NEW_OU_ORG_USER_ID_MAPPING_SQL_FORMAT, company, String.join(", ",values));
        return template.update(insertOUOrgUserIdMappingSQL, new MapSqlParameterSource(params));
    }
    private Integer deleteOuOrgUserMappings(String company, Integer ouRefId, List<Integer> orgUserRefIdsNotIn){
        String sql = "";
        HashMap<String, Object> params = new HashMap<>();
        params.put("ou_ref_id", ouRefId);

        if(CollectionUtils.isNotEmpty(orgUserRefIdsNotIn)) {
            sql = "DELETE FROM "+company+".ou_org_user_mappings_v2 WHERE ou_ref_id = :ou_ref_id AND org_user_ref_id NOT IN (:org_user_ref_ids)";
            params.put("org_user_ref_ids",orgUserRefIdsNotIn);
        } else {
            sql = "DELETE FROM "+company+".ou_org_user_mappings_v2 WHERE ou_ref_id = :ou_ref_id";
        }
        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        return template.update(sql, params);
    }
    public ImmutablePair<Integer, Integer> syncMappings(String company, Integer ouRefId, List<Integer> orgUserRefIds) throws SQLException {
        TransactionDefinition txDef = new DefaultTransactionDefinition();
        TransactionStatus txStatus = transactionManager.getTransaction(txDef);
        try {
            Integer newMappingsInserted = batchInsertNew(company, ouRefId, orgUserRefIds);
            Integer deletedMappingsCount = deleteOuOrgUserMappings(company, ouRefId, orgUserRefIds);
            transactionManager.commit(txStatus);
            return ImmutablePair.of(newMappingsInserted, deletedMappingsCount);
        } catch (Exception e) {
            transactionManager.rollback(txStatus);
            throw e;
        }
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
    private DbListResponse<OuOrgUserMappings> getBatch(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<Integer> ouRefIds, List<Integer> orgUserRefIds) {
        List<String> criterias = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        if(CollectionUtils.isNotEmpty(ids)) {
            criterias.add("id in (:ids)");
            params.put("ids", ids);
        }
        if(CollectionUtils.isNotEmpty(ouRefIds)) {
            criterias.add("ou_ref_id in (:ou_ref_ids)");
            params.put("ou_ref_ids", ouRefIds);
        }
        if(CollectionUtils.isNotEmpty(orgUserRefIds)) {
            criterias.add("org_user_ref_id in (:org_user_ref_ids)");
            params.put("org_user_ref_ids", orgUserRefIds);
        }

        String selectSqlBase = "SELECT * FROM " + company + ".ou_org_user_mappings_v2";
        String criteria = "";
        if(CollectionUtils.isNotEmpty(criterias)) {
            criteria = " WHERE " + String.join(" AND ", criterias);
        }
        String orderBy = " ORDER BY updated_at DESC ";

        String selectSql = selectSqlBase + criteria + orderBy + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSql = "SELECT COUNT(*) FROM (" +  selectSqlBase + criteria + ") AS counted";

        log.debug("sql = " + selectSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        List<OuOrgUserMappings> results = template.query(selectSql, params, new RowMapper<OuOrgUserMappings>() {
            @Override
            public OuOrgUserMappings mapRow(ResultSet rs, int rowNum) throws SQLException {
                return OuOrgUserMappings.builder()
                        .id((UUID) rs.getObject("id"))
                        .ouRefId(rs.getInt("ou_ref_id"))
                        .orgUserRefId(rs.getInt("org_user_ref_id"))
                        .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                        .updatedAt(DateUtils.toInstant(rs.getTimestamp("updated_at")))
                        .build();
            }
        });

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
    public DbListResponse<OuOrgUserMappings> listByFilter(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<Integer> ouRefIds, List<Integer> orgUserRefIds) throws SQLException {
        return getBatch(company, pageNumber, pageSize, ids, ouRefIds,orgUserRefIds);
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
                "CREATE TABLE IF NOT EXISTS " + company + ".ou_org_user_mappings_v2 (" +
                        "   id                              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "   ou_ref_id                       INTEGER NOT NULL," +
                        "   org_user_ref_id                 INTEGER NOT NULL," +
                        "   created_at                      TIMESTAMPTZ NOT NULL DEFAULT now(),\n" +
                        "   updated_at                      TIMESTAMPTZ NOT NULL DEFAULT now()\n" +
                        ")",
                "CREATE UNIQUE INDEX IF NOT EXISTS uniq_ou_org_user_mappings_v2_ou_ref_id_org_user_ref_id_idx on " + company + "." + "ou_org_user_mappings_v2 (ou_ref_id, org_user_ref_id)"
        );

        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
    // endregion
}
