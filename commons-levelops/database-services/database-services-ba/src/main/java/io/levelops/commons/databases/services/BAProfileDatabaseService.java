package io.levelops.commons.databases.services;

import com.amazonaws.services.apigateway.model.Op;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.converters.BAProfileConverters;
import io.levelops.commons.databases.converters.CountQueryConverter;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.report_models.ba.BAProfile;
import io.levelops.web.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
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
import java.util.UUID;

@Log4j2
@Service
public class BAProfileDatabaseService extends DatabaseService<BAProfile> {
    private static final Boolean INSERT = false;
    private static final Boolean UPDATE = true;
    private static final String INSERT_FORMAT = "INSERT INTO %s.ba_profiles (name,description,default_profile,categories) VALUES (:name,:description,:default_profile,:categories::jsonb)";
    private static final String UPDATE_FORMAT = "UPDATE %s.ba_profiles SET name=:name, description=:description, default_profile=:default_profile, categories=:categories::jsonb, updated_at = now() WHERE id=:id";

    private static final String UNSET_DEFAULT_PROFILE_FORMAT = "UPDATE %s.ba_profiles SET default_profile=false, updated_at = now() WHERE default_profile=true AND id!=:id";
    private static final String SET_DEFAULT_PROFILE_FORMAT = "UPDATE %s.ba_profiles SET default_profile=true, updated_at = now() WHERE default_profile=false AND id=:id";
    private static final String DELETE_FORMAT = "DELETE FROM %s.ba_profiles WHERE id=:id";

    private final NamedParameterJdbcTemplate template;
    private final PlatformTransactionManager transactionManager;
    private final ObjectMapper objectMapper;

    // region CSTOR
    public BAProfileDatabaseService(DataSource dataSource, ObjectMapper objectMapper) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.transactionManager = new DataSourceTransactionManager(dataSource);
        this.objectMapper = objectMapper;
    }
    // endregion

    // region insert upsert common
    private void validateInput(String company, BAProfile t, boolean update) {
        Validate.notBlank(company, "Company cannot be null or empty!");
        Validate.notNull(t, "Input Profile cannot be null!");
        Validate.notBlank(t.getName(), "Profile name cannot be null or empty!");
        Validate.isTrue(CollectionUtils.isNotEmpty(t.getCategories()), "Profile Categories cannot be null or empty!");
        if (update) {
            Validate.notNull(t.getId(), "Input Profile Id cannot be null!");
        }
    }
    private MapSqlParameterSource constructParameterSource(BAProfile t, final UUID existingId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("name", t.getName());
        params.addValue("description", t.getDescription());
        params.addValue("default_profile", BooleanUtils.isTrue(t.getDefaultProfile()));
        try {
            params.addValue("categories", objectMapper.writeValueAsString(t.getCategories()));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize categories to JSON", e);
        }
        if (existingId != null) {
            params.addValue("id", existingId);
        }
        return params;
    }
    // endregion

    // region insert
    @Override
    public String insert(String company, BAProfile t) throws SQLException {
        validateInput(company, t, INSERT);
        MapSqlParameterSource params = constructParameterSource(t, null);
        String insertReportSql = String.format(INSERT_FORMAT, company);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(insertReportSql, params, keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            throw new SQLException("Failed to insert ba profile record!! " + t.toString());
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }
    // endregion

    // region update
    @Override
    public Boolean update(String company, BAProfile t) throws SQLException {
        validateInput(company, t, UPDATE);
        MapSqlParameterSource params = constructParameterSource(t, t.getId());
        String updateSql = String.format(UPDATE_FORMAT, company);

        int updatedRows = template.update(updateSql, params);
        if (updatedRows != 1) {
            throw new SQLException("Failed to update ba profile! company = " + company + ", id = " + t.getId() + ", updatedRows = " + updatedRows);
        }
        return true;
    }
    // endregion

    // region update
    public ImmutablePair<Integer, Integer> updateDefault(String company, UUID newDefaultProfileId) throws SQLException, NotFoundException {
        Optional<BAProfile> newDefault = get(company, newDefaultProfileId.toString());
        if(newDefault.isEmpty()) {
            throw new NotFoundException("Failed to update default ba profile! The new default profile does not exist! company = " + company + ", id = " +  newDefaultProfileId);
        }
        if(BooleanUtils.isTrue(newDefault.get().getDefaultProfile())) {
            return ImmutablePair.of(0, 0);
        }

        TransactionDefinition txDef = new DefaultTransactionDefinition();
        TransactionStatus txStatus = transactionManager.getTransaction(txDef);
        try {
            String unsetSql = String.format(UNSET_DEFAULT_PROFILE_FORMAT, company);
            int unsetRowsCount = template.update(unsetSql, Map.of("id", newDefaultProfileId));

            String setSql = String.format(SET_DEFAULT_PROFILE_FORMAT, company);
            int setRowsCount = template.update(setSql, Map.of("id", newDefaultProfileId));

            transactionManager.commit(txStatus);
            return ImmutablePair.of(unsetRowsCount, setRowsCount);
        } catch (Exception e) {
            transactionManager.rollback(txStatus);
            throw e;
        }
    }
    // endregion

    // region get and list
    @Override
    public Optional<BAProfile> get(String company, String id) throws SQLException {
        var results = getBatch(company, 0, 10, Collections.singletonList(UUID.fromString(id)), null, null).getRecords();
        return IterableUtils.getFirst(results);
    }
    @Override
    public DbListResponse<BAProfile> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return getBatch(company, pageNumber, pageSize, null, null, null);
    }
    public DbListResponse<BAProfile> listByFilter(String company, Integer pageNumber, Integer pageSize, final List<UUID> ids,
                                                  final List<String> names, final Boolean defaultConfig) {
        return getBatch(company, pageNumber, pageSize, ids, names, defaultConfig);
    }
    private DbListResponse<BAProfile> getBatch(String company, Integer pageNumber, Integer pageSize, final List<UUID> ids,
                                                       final List<String> names, final Boolean defaultProfile) {
        List<String> criterias = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (CollectionUtils.isNotEmpty(ids)) {
            criterias.add("id IN(:ids)");
            params.addValue("ids", ids);
        }
        if (CollectionUtils.isNotEmpty(names)) {
            criterias.add("name IN(:names)");
            params.addValue("names", names);
        }
        if (defaultProfile != null) {
            criterias.add("default_profile IN(:default_profile)");
            params.addValue("default_profile", defaultProfile);
        }

        String selectSqlBase = "SELECT * FROM " + company + ".ba_profiles";
        String criteria = "";
        if(CollectionUtils.isNotEmpty(criterias)) {
            criteria = " WHERE " + String.join(" AND ", criterias);
        }

        List<String> sortBy = new ArrayList<>();
        sortBy.add("updated_at DESC");
        String orderBy = " ORDER BY " + String.join(",", sortBy);

        String selectSql = selectSqlBase + criteria + orderBy + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSQL = "SELECT COUNT(*) FROM (" + selectSqlBase + ") AS counted";

        Integer totCount = 0;
        log.info("sql = " + selectSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<BAProfile> baProfiles = template.query(selectSql, params, BAProfileConverters.mapProfile(objectMapper));
        log.debug("baProfiles.size() = {}", baProfiles.size());
        if (baProfiles.size() > 0) {
            totCount = baProfiles.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
            if (baProfiles.size() == pageSize) {
                log.info("sql = " + countSQL); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
                log.info("params = {}", params);
                totCount = template.query(countSQL, params, CountQueryConverter.countMapper()).get(0);
            }
        }
        return DbListResponse.of(baProfiles, totCount);
    }
    // endregion

    // region delete
    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String deleteSql = String.format(DELETE_FORMAT, company);
        return template.update(deleteSql, Map.of("id", UUID.fromString(id))) > 0;
    }
    // endregion

    // region ensureTableExistence
    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList= List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".ba_profiles\n" +
                        "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    name VARCHAR NOT NULL,\n" +
                        "    description VARCHAR NOT NULL,\n" +
                        "    default_profile BOOLEAN NOT NULL DEFAULT false," +
                        "    categories JSONB NOT NULL DEFAULT '[]'::jsonb,\n" +
                        "    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),\n" +
                        "    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()\n" +
                        ")",
                // make sure there can only be one default profile:
                "CREATE UNIQUE INDEX IF NOT EXISTS ba_profiles_default_profile_index ON " + company + ".ba_profiles (default_profile) WHERE default_profile = true;",
                // make sure name is unique (case insensitive)
                "CREATE UNIQUE INDEX IF NOT EXISTS ba_profiles_name_index ON " + company + ".ba_profiles (UPPER(name));"
        );
        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
    // endregion
}
