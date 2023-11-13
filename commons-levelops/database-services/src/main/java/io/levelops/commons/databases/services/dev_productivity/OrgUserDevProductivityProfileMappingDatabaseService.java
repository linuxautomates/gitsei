package io.levelops.commons.databases.services.dev_productivity;

import io.levelops.commons.databases.converters.CountQueryConverter;
import io.levelops.commons.databases.models.database.dev_productivity.OrgUserDevProdProfileMappings;
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
import java.util.*;
import java.util.stream.Collectors;


@Log4j2
@Service
public class OrgUserDevProductivityProfileMappingDatabaseService extends DatabaseService<OrgUserDevProdProfileMappings> {
    private final NamedParameterJdbcTemplate template;
    private final PlatformTransactionManager transactionManager;

    private static final String INSERT_NEW_ORG_USER_DEV_PROD_PROFILE_ID_MAPPING_SQL_FORMAT = "INSERT INTO %s.org_user_dev_productivity_profile_mappings (org_user_ref_id,dev_productivity_parent_profile_id,dev_productivity_profile_id) "
            + "VALUES %s "
            + "ON CONFLICT(org_user_ref_id,dev_productivity_parent_profile_id,dev_productivity_profile_id) DO NOTHING";

    // region CSTOR
    public OrgUserDevProductivityProfileMappingDatabaseService(DataSource dataSource) {
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
    public String insert(String company, OrgUserDevProdProfileMappings t) throws SQLException {
        throw new UnsupportedOperationException();
    }
    // endregion

    // region Sync Flow
    private int batchInsertNew(String company, List<OrgUserDevProdProfileMappings> mappings) {
        Map<String, Object> params = new HashMap<>();
        List<String> values = new ArrayList<>();
        for(OrgUserDevProdProfileMappings mapping : mappings){
            int i = values.size();
            params.putAll(Map.of(
                    "org_user_ref_id_" + i, mapping.getOrgUserRefId(),
                    "dev_productivity_parent_profile_id_" + i, mapping.getDevProductivityParentProfileId(),
                    "dev_productivity_profile_id_" + i, mapping.getDevProductivityProfileId()
            ));
            values.add(MessageFormat.format("(:org_user_ref_id_{0}, :dev_productivity_parent_profile_id_{0}, :dev_productivity_profile_id_{0})", String.valueOf(i)));
        }
        if (values.isEmpty()) {
            log.warn("Skipping bulk insert: no valid data provided");
            return 0;
        }
        String insertOrgUserProfileMappingSQL = String.format(INSERT_NEW_ORG_USER_DEV_PROD_PROFILE_ID_MAPPING_SQL_FORMAT, company, String.join(", ",values));
        return template.update(insertOrgUserProfileMappingSQL, new MapSqlParameterSource(params));
    }
    private Integer deleteOrgUserDevProdProfileMappings(String company, UUID parentProfileId, List<OrgUserDevProdProfileMappings> mappings){
        String sql = "";
        HashMap<String, Object> params = new HashMap<>();
        params.put("dev_productivity_parent_profile_id", parentProfileId);

        if(CollectionUtils.isNotEmpty(mappings)) {
            sql = "DELETE FROM "+company+".org_user_dev_productivity_profile_mappings WHERE dev_productivity_parent_profile_id = :dev_productivity_parent_profile_id AND (dev_productivity_profile_id NOT IN (:dev_productivity_profile_ids) OR org_user_ref_id NOT IN (:org_user_ref_ids))";
            params.put("dev_productivity_profile_ids",mappings.stream().map(OrgUserDevProdProfileMappings::getDevProductivityProfileId).collect(Collectors.toList()));
            params.put("org_user_ref_ids",mappings.stream().map(OrgUserDevProdProfileMappings::getOrgUserRefId).collect(Collectors.toList()));
        } else {
            sql = "DELETE FROM "+company+".org_user_dev_productivity_profile_mappings WHERE dev_productivity_parent_profile_id = :dev_productivity_parent_profile_id";
        }
        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        return template.update(sql, params);
    }
    public ImmutablePair<Integer, Integer> syncMappings(String company, UUID devProdParentProfileId, List<OrgUserDevProdProfileMappings> orgUserDevProdProfileMappings) throws SQLException {
        TransactionDefinition txDef = new DefaultTransactionDefinition();
        TransactionStatus txStatus = transactionManager.getTransaction(txDef);
        Integer newMappingsInserted = 0;
        Integer deletedMappingsCount = 0;
        try {
            Map<UUID,List<OrgUserDevProdProfileMappings>> parentProfileIdMappingsMap = devProdParentProfileId != null ? Map.of(devProdParentProfileId,CollectionUtils.emptyIfNull(orgUserDevProdProfileMappings).stream().collect(Collectors.toList()))
                    : CollectionUtils.emptyIfNull(orgUserDevProdProfileMappings).stream()
                    .collect(Collectors.groupingBy(OrgUserDevProdProfileMappings::getDevProductivityParentProfileId));
            for(UUID eachParentProfileId : parentProfileIdMappingsMap.keySet()){
                 newMappingsInserted += batchInsertNew(company, parentProfileIdMappingsMap.get(eachParentProfileId));
                 deletedMappingsCount += deleteOrgUserDevProdProfileMappings(company, eachParentProfileId, parentProfileIdMappingsMap.get(eachParentProfileId));
            }
            transactionManager.commit(txStatus);
        } catch (Exception e) {
            transactionManager.rollback(txStatus);
            throw e;
        }
        return ImmutablePair.of(newMappingsInserted, deletedMappingsCount);
    }
    // endregion

    // region Update
    @Override
    public Boolean update(String company, OrgUserDevProdProfileMappings t) throws SQLException {
        throw new UnsupportedOperationException();
    }
    // endregion

    // region Get
    @Override
    public Optional<OrgUserDevProdProfileMappings> get(String company, String param) throws SQLException {
        throw new UnsupportedOperationException();
    }
    // endregion

    // region List
    @Override
    public DbListResponse<OrgUserDevProdProfileMappings> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return null;
    }
    private DbListResponse<OrgUserDevProdProfileMappings> getBatch(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<Integer> orgUserRefIds, List<UUID> devProductivityParentProfileIds, List<UUID> devProductivityProfileIds) {
        List<String> criterias = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        if(CollectionUtils.isNotEmpty(ids)) {
            criterias.add("id in (:ids)");
            params.put("ids", ids);
        }
        if(CollectionUtils.isNotEmpty(orgUserRefIds)) {
            criterias.add("org_user_ref_id in (:org_user_ref_ids)");
            params.put("org_user_ref_ids", orgUserRefIds);
        }
        if(CollectionUtils.isNotEmpty(devProductivityParentProfileIds)) {
            criterias.add("dev_productivity_parent_profile_id in (:dev_productivity_parent_profile_ids)");
            params.put("dev_productivity_parent_profile_ids", devProductivityParentProfileIds);
        }
        if(CollectionUtils.isNotEmpty(devProductivityProfileIds)) {
            criterias.add("dev_productivity_profile_id in (:dev_productivity_profile_ids)");
            params.put("dev_productivity_profile_ids", devProductivityProfileIds);
        }
        String selectSqlBase = "SELECT * FROM " + company + ".org_user_dev_productivity_profile_mappings";
        String criteria = "";
        if(CollectionUtils.isNotEmpty(criterias)) {
            criteria = " WHERE " + String.join(" AND ", criterias);
        }
        String orderBy = " ORDER BY updated_at DESC ";

        String selectSql = selectSqlBase + criteria + orderBy + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSql = "SELECT COUNT(*) FROM (" +  selectSqlBase + criteria + ") AS counted";

        log.debug("sql = " + selectSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        List<OrgUserDevProdProfileMappings> results = template.query(selectSql, params, new RowMapper<OrgUserDevProdProfileMappings>() {
            @Override
            public OrgUserDevProdProfileMappings mapRow(ResultSet rs, int rowNum) throws SQLException {
                return OrgUserDevProdProfileMappings.builder()
                        .id((UUID) rs.getObject("id"))
                        .orgUserRefId(rs.getInt("org_user_ref_id"))
                        .devProductivityParentProfileId((UUID) rs.getObject("dev_productivity_parent_profile_id"))
                        .devProductivityProfileId((UUID) rs.getObject("dev_productivity_profile_id"))
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
    public DbListResponse<OrgUserDevProdProfileMappings> listByFilter(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<Integer> orgUserRefIds, List<UUID> devProductivityParentProfileIds, List<UUID> devProductivityProfileIds) throws SQLException {
        return getBatch(company, pageNumber, pageSize, ids, orgUserRefIds, devProductivityParentProfileIds, devProductivityProfileIds);
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
                "CREATE TABLE IF NOT EXISTS " + company + ".org_user_dev_productivity_profile_mappings (" +
                        "   id                                          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "   org_user_ref_id                             INTEGER NOT NULL," +
                        "   dev_productivity_parent_profile_id          UUID NOT NULL,"+
                        "   dev_productivity_profile_id                 UUID NOT NULL,"+
                        "   created_at                                  TIMESTAMPTZ NOT NULL DEFAULT now(),\n" +
                        "   updated_at                                  TIMESTAMPTZ NOT NULL DEFAULT now()\n" +
                        ")",
                "CREATE UNIQUE INDEX IF NOT EXISTS uniq_org_user_dev_prof_profile_mappings_org_user_ref_id_dev_prod_parent_profile_id_dev_prod_profile_id_idx on " + company + "." + "org_user_dev_productivity_profile_mappings (org_user_ref_id, dev_productivity_parent_profile_id, dev_productivity_profile_id)"
        );

        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
    // endregion
}
