package io.levelops.commons.databases.services.velocity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.converters.CountQueryConverter;
import io.levelops.commons.databases.converters.VelocityConfigConverters;
import io.levelops.commons.databases.models.database.velocity.OrgProfile;
import io.levelops.commons.databases.models.database.velocity.VelocityConfig;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.databases.utils.CriteriaUtils;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.web.exceptions.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Log4j2
@Service
public class VelocityConfigsDatabaseService extends DatabaseService<VelocityConfig> {
    private static final Set<String> PARTIAL_MATCH_COLUMNS = Set.of("name");
    private static final Set<String> PARTIAL_MATCH_ARRAY_COLUMNS = Set.of();

    private static final String CHECK_EXISTING_SQL_FORMAT = "SELECT id from %s.cicd_job_config_changes where cicd_job_id = ? AND change_time = ? AND change_type = ?  AND cicd_user_id = ?";
    private static final String INSERT_CONFIG_SQL_FORMAT = "INSERT INTO %s.velocity_configs (name,default_config,config) VALUES(:name,:default_config,:config::jsonb) RETURNING id";

    private static final String INSERT_NEW_CONFIG_SQL_FORMAT = "INSERT INTO %s.velocity_configs (name,default_config,config,is_new) VALUES(:name,:default_config,:config::jsonb,true) RETURNING id";
    private static final String INSERT_CONFIG_MAPPING_SQL_FORMAT = "INSERT INTO %s.velocity_config_cicd_job_mappings (velocity_config_id,cicd_job_id) VALUES(:velocity_config_id,:cicd_job_id)";

    private static final String UPDATE_CONFIG_SQL_FORMAT = "UPDATE %s.velocity_configs SET name = :name, default_config = :default_config, config = :config::jsonb, updated_at = now() WHERE id = :id";

    private static final String UPDATE_CONFIG_UNSET_DEFAULT_SQL_FORMAT = "UPDATE %s.velocity_configs SET default_config = false, updated_at = now() WHERE default_config = true and id != :id";
    private static final String UPDATE_CONFIG_SET_DEFAULT_SQL_FORMAT = "UPDATE %s.velocity_configs SET default_config = true, updated_at = now() WHERE default_config = false and id = :id";

    private static final String DELETE_CONFIG_MAPPING_SQL_FORMAT = "DELETE FROM %s.velocity_config_cicd_job_mappings WHERE velocity_config_id = :velocity_config_id";
    private static final String DELETE_CONFIG_SQL_FORMAT = "DELETE FROM %s.velocity_configs WHERE id = :id";

    private final OrgProfileDatabaseService orgProfileDatabaseService;
    private final NamedParameterJdbcTemplate template;
    private final PlatformTransactionManager transactionManager;
    private final ObjectMapper objectMapper;

    private static final String VELOCITY_CONFIGS = "velocity_configs";
    private static final String OU_PROFILES = "ou_profiles";


    @Autowired
    public VelocityConfigsDatabaseService(DataSource dataSource, ObjectMapper objectMapper, OrgProfileDatabaseService orgProfileDatabaseService) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.transactionManager = new DataSourceTransactionManager(dataSource);
        this.objectMapper = objectMapper;
        this.orgProfileDatabaseService = orgProfileDatabaseService;
    }

    // region get references
    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(CiCdJobsDatabaseService.class);
    }
    // endregion

    /*
    This function does the following.
    1) Deletes all the existing mappings for the velocity config.
    2) Insert new config and cicd job mapping for every cicd job
     */
    private void upsertConfigMappins(final String company, UUID velocityConfigId, final List<UUID> cicdJobIds) {
        if (CollectionUtils.isEmpty(cicdJobIds)) {
            return;
        }
        String deleteSql = String.format(DELETE_CONFIG_MAPPING_SQL_FORMAT, company);
        template.update(deleteSql, Map.of("velocity_config_id", velocityConfigId));

        String insertConfigMappingSql = String.format(INSERT_CONFIG_MAPPING_SQL_FORMAT, company);
        for (UUID cicdJobId : cicdJobIds) {
            template.update(insertConfigMappingSql, Map.of("velocity_config_id", velocityConfigId, "cicd_job_id", cicdJobId));
        }
    }

    /*
    Insert Velocity Config
     */
    private UUID insertConfig(String company, VelocityConfig t) throws SQLException {
        MapSqlParameterSource params = constructParameterSource(t, null);
        String insertConfigSql;
        if(BooleanUtils.isTrue(t.getIsNew()))
            insertConfigSql = String.format( INSERT_NEW_CONFIG_SQL_FORMAT, company);
        else
            insertConfigSql = String.format( INSERT_CONFIG_SQL_FORMAT, company);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(insertConfigSql, params, keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            throw new SQLException("Failed to insert velocity config");
        }
        UUID id = (UUID) keyHolder.getKeys().get("id");
        return id;
    }

    @Override
    public String insert(String company, VelocityConfig t) throws SQLException {
        TransactionDefinition txDef = new DefaultTransactionDefinition();
        TransactionStatus txStatus = transactionManager.getTransaction(txDef);
        try {
            UUID id = insertConfig(company, t);
            upsertConfigMappins(company, id, t.getCicdJobIds());
            if(BooleanUtils.isTrue(t.getIsNew()))
                insertMappingWithOU(company, t.getConfig(), id.toString());
            transactionManager.commit(txStatus);
            return id.toString();
        } catch (Exception e) {
            transactionManager.rollback(txStatus);
            throw e;
        }
    }

    private MapSqlParameterSource constructParameterSource(VelocityConfig t, final UUID existingId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        try {
            params.addValue("name", t.getName());
            params.addValue("default_config", t.getDefaultConfig());
            params.addValue("config", objectMapper.writeValueAsString(t.getConfig()));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize velocity config", e);
        }
        if (existingId != null) {
            params.addValue("id", existingId);
        }
        return params;
    }

    private Boolean updateConfig(String company, VelocityConfig t) throws SQLException {
        MapSqlParameterSource params = constructParameterSource(t, t.getId());
        String updateConfigSql = String.format(UPDATE_CONFIG_SQL_FORMAT, company);
        int updatedRows = template.update(updateConfigSql, params);
        if (updatedRows != 1) {
            throw new SQLException("Failed to update velocity config, updatedRows = " + updatedRows);
        }
        return true;

    }

    @Override
    public Boolean update(String company, VelocityConfig t) throws SQLException {
        TransactionDefinition txDef = new DefaultTransactionDefinition();
        TransactionStatus txStatus = transactionManager.getTransaction(txDef);
        try {
            Boolean updateResult = updateConfig(company, t);
            upsertConfigMappins(company, t.getId(), t.getCicdJobIds());
            if(BooleanUtils.isTrue(t.getIsNew()))
                handleProfiles(company, t);
            transactionManager.commit(txStatus);
            return updateResult;
        } catch (Exception e) {
            transactionManager.rollback(txStatus);
            throw e;
        }
    }

    public void setDefault(final String company, final UUID id) throws SQLException {
        Validate.notBlank(company, "company cannot be blank");
        Validate.notNull(id, "id cannot be null");

        Map<String, Object> params = Map.of("id", id);
        TransactionDefinition txDef = new DefaultTransactionDefinition();
        TransactionStatus txStatus = transactionManager.getTransaction(txDef);
        try {
            String configUnsetDefaultSql = String.format(UPDATE_CONFIG_UNSET_DEFAULT_SQL_FORMAT, company);
            String configSetDefaultSql = String.format(UPDATE_CONFIG_SET_DEFAULT_SQL_FORMAT, company);


            log.info("sql = " + configUnsetDefaultSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            int unsetRowsCount = template.update(configUnsetDefaultSql, params);
            log.info("unsetRowsCount = {}", unsetRowsCount);


            log.info("sql = " + configSetDefaultSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            int setRowsCount = template.update(configSetDefaultSql, params);
            log.info("setRowsCount = {}", setRowsCount);
            if (setRowsCount < 1) {
                throw new SQLException("company " + company + " id " + id + " cannot set as default, config not found, setRowsCount = " + setRowsCount);
            } else if (setRowsCount > 1) {
                throw new SQLException("company " + company + " id " + id + " cannot set as default, multiple configs found, setRowsCount = " + setRowsCount);
            }
            transactionManager.commit(txStatus);
        } catch (Exception e) {
            transactionManager.rollback(txStatus);
            throw e;
        }
    }

    @Override
    public Optional<VelocityConfig> get(String company, String id) throws SQLException {
        var results = listByFilter(company, 0, 10, Collections.singletonList(UUID.fromString(id)), null, null, null).getRecords();
        return results.size() > 0 ? Optional.of(results.get(0)) : Optional.empty();
    }


    public Optional<VelocityConfig> getByOuRefId(String company, int ouRefId) {

        VelocityConfig res = null;
        try {
            String selectSqlBase = "SELECT vc.*, array_agg(json_build_object('cicd_job_id',m.cicd_job_id,'job_name', j.job_name)) as cicd_job_id_name FROM " + company + ".velocity_configs as vc "
                    + "left join " + company + ".velocity_config_cicd_job_mappings as m on m.velocity_config_id = vc.id "
                    + "left join " + company + ".cicd_jobs as j on j.id = m.cicd_job_id "
                    + "inner join " + company + ".ou_profiles as oup on oup.profile_id = vc.id and oup.ou_ref_id  = :ou_ref_id "
                    + " group by vc.id";

            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("ou_ref_id", ouRefId);
            List<VelocityConfig> results = template.query(selectSqlBase, params, VelocityConfigConverters.mapVelocityConfig(objectMapper));
            res = IterableUtils.getFirst(results).orElse(null);
            if(res == null){
                return Optional.empty();
            }
        } catch (DataAccessException e) {
            log.warn("Failed to get velocity config for ouRefId={}", ouRefId, e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("[{}] Error getting velocity config for ouRefId '{}'", company, ouRefId, e);
            throw e;
        }
        return Optional.of(res);
    }


    @Override
    public DbListResponse<VelocityConfig> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return listByFilter(company, pageNumber, pageSize, null, null, null, null);
    }

    private void parseCriterias(final List<String> criterias, final MapSqlParameterSource params, final List<UUID> ids,
                                final List<String> names, final Boolean defaultConfig, final Map<String, Map<String, String>> partialMatchMap) {
        if (CollectionUtils.isNotEmpty(ids)) {
            criterias.add("vc.id IN(:ids)");
            params.addValue("ids", ids);
        }
        if (CollectionUtils.isNotEmpty(names)) {
            criterias.add("vc.name IN(:names)");
            params.addValue("names", names);
        }
        if (defaultConfig != null) {
            criterias.add("vc.default_config IN(:default_config)");
            params.addValue("default_config", defaultConfig);
        }
        CriteriaUtils.addPartialMatchClause(partialMatchMap, criterias, null, params, PARTIAL_MATCH_COLUMNS, PARTIAL_MATCH_ARRAY_COLUMNS, StringUtils.EMPTY);
    }

    public DbListResponse<VelocityConfig> listByFilter(String company, Integer pageNumber, Integer pageSize, final List<UUID> ids,
                                                       final List<String> names, final Boolean defaultConfig, final Map<String, Map<String, String>> partialMatchMap) {
        List<String> criterias = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource();

        parseCriterias(criterias, params, ids, names, defaultConfig, partialMatchMap);
        String baseWhereClause = (CollectionUtils.isEmpty(criterias)) ? "" : " WHERE " + String.join(" AND ", criterias);
        String selectSqlBase = "SELECT vc.*, array_agg(json_build_object('cicd_job_id',m.cicd_job_id,'job_name', j.job_name)) as cicd_job_id_name FROM " + company + ".velocity_configs as vc "
                + "left join " + company + ".velocity_config_cicd_job_mappings as m on m.velocity_config_id = vc.id "
                + "left join " + company + ".cicd_jobs as j on j.id = m.cicd_job_id "
                + baseWhereClause
                + " group by vc.id";

        String selectSql = selectSqlBase + " ORDER BY updated_at desc" + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSQL = "SELECT COUNT(*) FROM (" + selectSqlBase + ") AS counted";

        Integer totCount = 0;
        log.info("sql = " + selectSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<VelocityConfig> velocityConfigs = template.query(selectSql, params, VelocityConfigConverters.mapVelocityConfig(objectMapper));
        log.debug("velocityConfigs.size() = {}", velocityConfigs.size());
        if (velocityConfigs.size() > 0) {
            totCount = velocityConfigs.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
            if (velocityConfigs.size() == pageSize) {
                log.info("sql = " + countSQL); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
                log.info("params = {}", params);
                totCount = template.query(countSQL, params, CountQueryConverter.countMapper()).get(0);
            }
        }
        return DbListResponse.of(velocityConfigs, totCount);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String deleteSql = String.format(DELETE_CONFIG_SQL_FORMAT, company);
        return template.update(deleteSql, Map.of("id", UUID.fromString(id))) > 0;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".velocity_configs (" +
                        "  id               UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        "  name             VARCHAR NOT NULL," +
                        "  default_config   BOOLEAN NOT NULL DEFAULT false," +
                        "  config           JSONB NOT NULL," +
                        "  is_new           BOOLEAN NOT NULL DEFAULT false," +
                        "  created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()," +
                        "  updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()" +
                        ")",
                //This gurantees that only one row has default_config = true
                "CREATE UNIQUE INDEX IF NOT EXISTS uniq_velocity_configs_default_config_only_1_idx on " + company + ".velocity_configs (default_config) WHERE default_config = true",
                "CREATE TABLE IF NOT EXISTS " + company + ".velocity_config_cicd_job_mappings(\n" +
                        "    velocity_config_id UUID NOT NULL REFERENCES " + company + ".velocity_configs(id) ON DELETE CASCADE,\n" +
                        "    cicd_job_id    UUID NOT NULL REFERENCES " + company + ".cicd_jobs(id) ON DELETE RESTRICT,\n" +
                        "    CONSTRAINT         uniq_velocity_config_id_cicd_job_id_idx UNIQUE(velocity_config_id, cicd_job_id)\n" +
                        ")"
        );

        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }

    private void insertMappingWithOU(String company, VelocityConfigDTO config, String configId) throws SQLException {
        List<OrgProfile> workflows = new ArrayList<>();
        ListUtils.emptyIfNull(config.getAssociatedOURefIds()).forEach(ouRefId -> {
            workflows.add(OrgProfile.builder()
                    .profileId(UUID.fromString(configId))
                    .profileType(OrgProfile.ProfileType.WORKFLOW)
                    .ouRefId(Integer.valueOf(ouRefId))
                    .build());
        });
        if (workflows.isEmpty()) {
            return;
        }
        orgProfileDatabaseService.insert(company, workflows);
    }

    private void handleProfiles(String company, VelocityConfig config) throws SQLException {
        List<String> newOUs = ListUtils.emptyIfNull(config.getConfig().getAssociatedOURefIds());
        orgProfileDatabaseService.deleteByProfileId(company, config.getId());
        if (CollectionUtils.isNotEmpty(newOUs)) {
            insertMappingWithOU(company, VelocityConfigDTO.builder().associatedOURefIds(newOUs).build(), config.getId().toString());
        }
    }
}
