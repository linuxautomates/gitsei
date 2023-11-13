package io.levelops.commons.databases.services.dev_productivity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.converters.CountQueryConverter;
import io.levelops.commons.databases.converters.DevProductivityProfileConverters;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.utils.CriteriaUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ResourceUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.io.IOException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Service
public class DevProductivityProfileDatabaseService extends DatabaseService<DevProductivityProfile> {

    private static final Set<String> PARTIAL_MATCH_COLUMNS = Set.of("name");
    private static final Set<String> PARTIAL_MATCH_ARRAY_COLUMNS = Set.of();

    private static final String INSERT_PROFILE_SQL_FORMAT = "INSERT INTO %s.dev_productivity_profiles (name,description,is_predefined,ticket_categorization_scheme_id, settings) VALUES(:name,:description,:is_predefined,:ticket_categorization_scheme_id,:settings::jsonb) RETURNING id";
    private static final String INSERT_PROFILE_SECTION_SQL_FORMAT = "INSERT INTO %s.dev_productivity_sections (body,profile_id) VALUES(:body::jsonb,:profile_id) RETURNING id";
    private static final String INSERT_PROFILE_FEATURE_SQL_FORMAT = "INSERT INTO %s.dev_productivity_features (body,section_id) VALUES(:body::jsonb,:section_id) RETURNING id";
    private static final String UPDATE_PROFILE_SQL_FORMAT = "UPDATE %s.dev_productivity_profiles SET name = :name, description = :description, ticket_categorization_scheme_id = :ticket_categorization_scheme_id, settings = :settings::jsonb, updated_at = now() WHERE id = :id";
    private static final String DELETE_PROFILE_SQL_FORMAT = "DELETE FROM %s.dev_productivity_profiles WHERE id = :id";
    private static final String DELETE_SECTION_BY_PROFILE_ID_SQL_FORMAT = "DELETE FROM %s.dev_productivity_sections WHERE profile_id = :profile_id";
    private static final String DELETE_OU_MAPPING_BY_PROFILE_ID_SQL_FORMAT = "DELETE FROM %s.dev_productivity_profile_ou_mappings WHERE dev_productivity_profile_id = :profile_id";
    private static final String DELETE_OU_MAPPING_BY_OU_REF_ID_SQL_FORMAT = "DELETE FROM %s.dev_productivity_profile_ou_mappings WHERE ou_ref_id = :ou_ref_id";
    private static final String UPSERT_DEFAULT_PROFILE_SQL_FORMAT = "INSERT INTO %s.dev_productivity_profile_default (default_col,dev_productivity_profile_id) "
            + "VALUES (:default_col,:dev_productivity_profile_id) "
            + "ON CONFLICT(default_col) DO UPDATE SET dev_productivity_profile_id = EXCLUDED.dev_productivity_profile_id";
    private static final String INSERT_PROFILE_OU_MAPPING_SQL_FORMAT = "INSERT INTO %s.dev_productivity_profile_ou_mappings (ou_ref_id,dev_productivity_profile_id) "
            + "VALUES %s "
            + "ON CONFLICT(ou_ref_id) DO NOTHING";

    private final NamedParameterJdbcTemplate template;
    private final PlatformTransactionManager transactionManager;
    private final ObjectMapper objectMapper;
    private final Boolean devProdProfilesV2Enabled;

    @Autowired
    public DevProductivityProfileDatabaseService(DataSource dataSource, ObjectMapper objectMapper,
                                                 @Value("${DEV_PROD_PROFILES_V2_ENABLED:false}") Boolean devProdProfilesV2Enabled) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.transactionManager = new DataSourceTransactionManager(dataSource);
        this.objectMapper = objectMapper;
        this.devProdProfilesV2Enabled = BooleanUtils.isTrue(devProdProfilesV2Enabled);
    }


    public DevProductivityProfileDatabaseService(DataSource dataSource, ObjectMapper objectMapper) {
        this(dataSource, objectMapper, false);
    }


    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(TicketCategorizationSchemeDatabaseService.class);
    }

    @Override
    public String insert(String company, DevProductivityProfile t) throws SQLException {
        return insertProfile(company, t).toString();
    }

    private UUID insertProfile(String company, DevProductivityProfile t) throws SQLException {
        TransactionDefinition txDef = new DefaultTransactionDefinition();
        TransactionStatus txStatus = transactionManager.getTransaction(txDef);
        MapSqlParameterSource params = constructParameterSourceForProfile(t, null);
        String insertProfileSql = String.format(INSERT_PROFILE_SQL_FORMAT, company);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        UUID profileId = null;
        try {
            int updatedRows = template.update(insertProfileSql, params, keyHolder);
            if (updatedRows <= 0 || keyHolder.getKeys() == null) {
                throw new SQLException("Failed to insert dev productivity profile");
            }
            profileId = (UUID) keyHolder.getKeys().get("id");
            UUID finalProfileId = profileId;
            if (t.getSections() != null && CollectionUtils.isNotEmpty(t.getSections())) {
                for (DevProductivityProfile.Section section : t.getSections()) {
                    insertProfileSection(company, finalProfileId, section);
                }
            }
            if (CollectionUtils.isNotEmpty(t.getAssociatedOURefIds())) {
                    insertProfileOUMappingsBulk(company, finalProfileId, t.getAssociatedOURefIds());
            }
            transactionManager.commit(txStatus);
        } catch (Exception e) {
            transactionManager.rollback(txStatus);
            log.error("Error inserting dev productivity profile", e);
            throw new SQLException("could not insert dev productivity profile");
        }
        return profileId;
    }

    private UUID insertProfileSection(String company, UUID profileId, DevProductivityProfile.Section section) throws SQLException {
        //remove features from the body of the section before inserting
        DevProductivityProfile.Section sectionTrimmed = section.toBuilder().features(null).build();
        MapSqlParameterSource params = constructParameterSourceForSection(profileId, sectionTrimmed, null);
        String insertSectionSql = String.format(INSERT_PROFILE_SECTION_SQL_FORMAT, company);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(insertSectionSql, params, keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            throw new SQLException("Failed to insert dev productivity profile section");
        }
        UUID sectionId = (UUID) keyHolder.getKeys().get("id");
        if (section.getFeatures() != null && CollectionUtils.isNotEmpty(section.getFeatures())) {
            for (DevProductivityProfile.Feature feature : section.getFeatures()) {
                insertProfileFeature(company, sectionId, feature);
            }
        }
        return sectionId;
    }

    private UUID insertProfileFeature(String company, UUID sectionId, DevProductivityProfile.Feature feature) throws SQLException {
        MapSqlParameterSource params = constructParameterSourceForFeature(sectionId, feature, null);
        String insertFeatureSql = String.format(INSERT_PROFILE_FEATURE_SQL_FORMAT, company);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(insertFeatureSql, params, keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            throw new SQLException("Failed to insert dev productivity profile section");
        }
        UUID featureId = (UUID) keyHolder.getKeys().get("id");
        if (feature.getTicketCategories() != null && CollectionUtils.isNotEmpty(feature.getTicketCategories())) {
            for (UUID categoryId : feature.getTicketCategories()) {
                insertFeatureCategoryMapping(company, featureId, categoryId);
            }

        }
        return featureId;
    }

    private UUID insertFeatureCategoryMapping(String company, UUID featureId, UUID categoryId) throws SQLException {
        String sql = "INSERT INTO " + company + ".dev_productivity_feature_category_mappings (feature_id,category_id) VALUES(:feature_id,:category_id) RETURNING id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("feature_id", featureId);
        params.addValue("category_id", categoryId);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(sql, params, keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            throw new SQLException("Failed to insert feature category mapping");
        }
        return (UUID) keyHolder.getKeys().get("id");
    }

    public UUID upsertDefaultProfile(String company, UUID devProductivityProfileId) throws SQLException {
        String upsertDefaultProfileSQL = String.format(UPSERT_DEFAULT_PROFILE_SQL_FORMAT, company);
        template.update(upsertDefaultProfileSQL, new MapSqlParameterSource()
                .addValue("default_col", "Default")
                .addValue("dev_productivity_profile_id", devProductivityProfileId));
        return devProductivityProfileId;
    }

    private int insertProfileOUMappingsBulk(String company, UUID profileId, List<String> ouRefIds) {
        Map<String, Object> params = new HashMap<>();
        List<String> values = new ArrayList<>();
        for(String ouRefId : ouRefIds){
            int i = values.size();
            params.putAll(Map.of(
                    "ou_ref_id_" + i, Integer.valueOf(ouRefId),
                    "dev_productivity_profile_id_" + i, profileId
            ));
            values.add(MessageFormat.format("(:ou_ref_id_{0}, :dev_productivity_profile_id_{0})", i));
        }
        if (values.isEmpty()) {
            log.warn("Skipping bulk insert: no valid data provided");
            return 0;
        }
        String insertProfileOUMappingSQL = String.format(INSERT_PROFILE_OU_MAPPING_SQL_FORMAT, company, String.join(", ",values));
        return template.update(insertProfileOUMappingSQL, new MapSqlParameterSource(params));
    }

    public UUID createAndMakeDefault(String company, DevProductivityProfile profile) throws Exception {
        TransactionDefinition txDef = new DefaultTransactionDefinition();
        TransactionStatus txStatus = transactionManager.getTransaction(txDef);
        UUID id;
        try {
            id = insertProfile(company, profile);
            upsertDefaultProfile(company, id);
            transactionManager.commit(txStatus);
        } catch (Exception e) {
            transactionManager.rollback(txStatus);
            throw e;
        }
        return id;
    }

    @Override
    public Boolean update(String company, DevProductivityProfile t) throws SQLException {
        return updateProfile(company, t);
    }

    private Boolean updateProfile(String company, DevProductivityProfile t) throws SQLException {
        TransactionDefinition txDef = new DefaultTransactionDefinition();
        TransactionStatus txStatus = transactionManager.getTransaction(txDef);
        UUID profileId = t.getId();
        MapSqlParameterSource params = constructParameterSourceForProfile(t, profileId);
        String updateProfileSql = String.format(UPDATE_PROFILE_SQL_FORMAT, company);
        try {
            int updatedRows = template.update(updateProfileSql, params);
            if (updatedRows <= 0) {
                throw new SQLException("Could not update dev productivity profile " + profileId);
            }
            //drop all the existing sections in the profile
            deleteProfileSectionsByProfileId(company, profileId);
            for (DevProductivityProfile.Section section : t.getSections()) {
                insertProfileSection(company, profileId, section);
            }
            //drop OU mappings and insert fresh
            deleteProfileOUMappingsByProfileId(company,profileId);
            if(CollectionUtils.isNotEmpty(t.getAssociatedOURefIds())){
                insertProfileOUMappingsBulk(company,profileId,t.getAssociatedOURefIds());
            }
            transactionManager.commit(txStatus);
        } catch (Exception e) {
            transactionManager.rollback(txStatus);
            log.error("Failed to update dev productivity profile", e);
            throw new SQLException("Failed to update dev productivity profile");
        }
        return true;
    }

    public Boolean updateProfileOUMappings(String company, UUID profileId, List<String> assocaitedOURefIds) throws SQLException {
        TransactionDefinition txDef = new DefaultTransactionDefinition();
        TransactionStatus txStatus = transactionManager.getTransaction(txDef);

        try {
            //drop OU mappings and insert fresh
            deleteProfileOUMappingsByProfileId(company,profileId);
            if(CollectionUtils.isNotEmpty(assocaitedOURefIds)){
                insertProfileOUMappingsBulk(company,profileId,assocaitedOURefIds);
            }
            transactionManager.commit(txStatus);
        } catch (Exception e) {
            transactionManager.rollback(txStatus);
            log.error("Failed to update dev productivity profile", e);
            throw new SQLException("Failed to update dev productivity profile");
        }
        return true;
    }


    public Optional<DevProductivityProfile> getDefaultDevProductivityProfile(String company) throws SQLException {
        var results = listByFilter(company, 0, 100, null, null, null, null).getRecords();
        return results.stream().filter(DevProductivityProfile::getDefaultProfile).findFirst();
    }

    @Override
    public Optional<DevProductivityProfile> get(String company, String id) throws SQLException {
        var results = listByFilter(company, 0, 10, Collections.singletonList(UUID.fromString(id)), null, null, null).getRecords();
        return results.size() > 0 ? Optional.of(results.get(0)) : Optional.empty();
    }

    public DbListResponse<DevProductivityProfile> listByFilter(String company, Integer pageNumber, Integer pageSize, final List<UUID> ids,
                                                               final List<String> names, final Map<String, Map<String, String>> partialMatchMap, List<Integer> ouRefIds) {
        List<String> criterias = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource();

        parseCriterias(criterias, params, ids, names, partialMatchMap);
        String baseWhereClause = (CollectionUtils.isEmpty(criterias)) ? "" : " WHERE " + String.join(" AND ", criterias);
        String defaultMappingJoinClause = " LEFT JOIN " + company + ".dev_productivity_profile_default dpd ON dp.id = dpd.dev_productivity_profile_id ";
        String ouMappingJoinClause = " LEFT JOIN " + company + ".dev_productivity_profile_ou_mappings dpo ON dp.id = dpo.dev_productivity_profile_id ";
        String groupByClause = " GROUP BY dp.id, dpd.id";
        String orderByClause = " ORDER BY updated_at desc";
        String limitCaluse = " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String selectSqlBase = "SELECT dpd.id AS dev_productivity_default_mapping_id,array_remove(array_agg(ou_ref_id), NULL)::integer[] as ou_ref_ids,dp.* FROM " + company + ".dev_productivity_profiles as dp "
                + defaultMappingJoinClause
                + ouMappingJoinClause
                + baseWhereClause
                + groupByClause
                +orderByClause;

        String ouRefIdCondition = "";
        if(CollectionUtils.isNotEmpty(ouRefIds)){
            ouRefIdCondition = " WHERE ou_ref_ids && ARRAY[ :ouRefIds ]";
            params.addValue("ouRefIds",ouRefIds);
        }
        String selectSql = "WITH profiles as ("+selectSqlBase+")"+"\n"+
                "SELECT * FROM profiles"+ouRefIdCondition+limitCaluse;
        String countSQL = "SELECT COUNT(*) FROM (" + selectSqlBase + ") AS counted";

        Integer totCount = 0;
        log.info("sql = " + selectSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DevProductivityProfile> devProductivityprofiles = template.query(selectSql, params, DevProductivityProfileConverters.mapDevProductivityProfile(objectMapper));
        log.debug("devProductivityprofiles.size() = {}", devProductivityprofiles.size());
        if (devProductivityprofiles.size() > 0) {
            //fetch sections
            devProductivityprofiles = devProductivityprofiles.stream().map((profile) -> {
                String fetchSections = "SELECT * FROM " + company + ".dev_productivity_sections WHERE profile_id =:profile_id";
                MapSqlParameterSource params1 = new MapSqlParameterSource();
                params1.addValue("profile_id", profile.getId());
                List<DevProductivityProfile.Section> devProductivitySections = template.query(fetchSections, params1, DevProductivityProfileConverters.mapDevProductivitySection(objectMapper));
                if (devProductivitySections.size() > 0) {
                    //fetch features
                    devProductivitySections = devProductivitySections.stream().map((section) -> {
                        String fetchFeatures = "SELECT * FROM " + company + ".dev_productivity_features WHERE section_id =:section_id";
                        MapSqlParameterSource params2 = new MapSqlParameterSource();
                        params2.addValue("section_id", section.getId());
                        List<DevProductivityProfile.Feature> devProductivityFeatures = template.query(fetchFeatures, params2, DevProductivityProfileConverters.mapDevProductivityFeature(objectMapper));
                        Collections.sort(devProductivityFeatures, Comparator.comparingInt(DevProductivityProfile.Feature::getOrder));
                        return section.toBuilder().features(devProductivityFeatures).build();
                    }).collect(Collectors.toList());
                }
                return profile.toBuilder().sections(devProductivitySections)
                        .build();
            }).collect(Collectors.toList());
            totCount = devProductivityprofiles.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
            if (devProductivityprofiles.size() == pageSize) {
                log.info("sql = " + countSQL); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
                log.info("params = {}", params);
                totCount = template.query(countSQL, params, CountQueryConverter.countMapper()).get(0);
            }
        }
        return DbListResponse.of(devProductivityprofiles, totCount);
    }

    private void parseCriterias(final List<String> criterias, final MapSqlParameterSource params, final List<UUID> ids,
                                final List<String> names, final Map<String, Map<String, String>> partialMatchMap) {
        if (CollectionUtils.isNotEmpty(ids)) {
            criterias.add("dp.id IN(:ids)");
            params.addValue("ids", ids);
        }
        if (CollectionUtils.isNotEmpty(names)) {
            criterias.add("dp.name IN(:names)");
            params.addValue("names", names);
        }
        CriteriaUtils.addPartialMatchClause(partialMatchMap, criterias, null, params, PARTIAL_MATCH_COLUMNS, PARTIAL_MATCH_ARRAY_COLUMNS, StringUtils.EMPTY);
    }

    @Override
    public DbListResponse<DevProductivityProfile> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return listByFilter(company, pageNumber, pageSize, null, null, null, null);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        Optional<DevProductivityProfile> profile = get(company,id);
        if(profile.isPresent() && profile.get().getPredefinedProfile()) {
            throw new RuntimeException("Cannot delete a predefined trellis profile");
        }
        String deleteSql = String.format(DELETE_PROFILE_SQL_FORMAT, company);
        return template.update(deleteSql, Map.of("id", UUID.fromString(id))) > 0;
    }

    private Boolean deleteProfileSectionsByProfileId(String company, UUID profileId) {
        String deleteSql = String.format(DELETE_SECTION_BY_PROFILE_ID_SQL_FORMAT, company);
        return template.update(deleteSql, Map.of("profile_id", profileId)) > 0;
    }


    private Boolean deleteProfileOUMappingsByProfileId(String company, UUID profileId) {
        String deleteSql = String.format(DELETE_OU_MAPPING_BY_PROFILE_ID_SQL_FORMAT, company);
        return template.update(deleteSql, Map.of("profile_id", profileId)) > 0;
    }

    public Boolean deleteProfileOUMappingsByOuRefId(String company, Integer ouRefId) {
        String deleteSql = String.format(DELETE_OU_MAPPING_BY_OU_REF_ID_SQL_FORMAT, company);
        return template.update(deleteSql, Map.of("ou_ref_id", ouRefId)) > 0;
    }
  
    private MapSqlParameterSource constructParameterSourceForProfile(DevProductivityProfile t, final UUID existingId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("name", t.getName());
        params.addValue("description", t.getDescription());
        params.addValue("is_predefined",t.getPredefinedProfile());
        params.addValue("ticket_categorization_scheme_id", t.getEffortInvestmentProfileId());
        try {
            params.addValue("settings", objectMapper.writeValueAsString(t.getSettings()));
        } catch (JsonProcessingException e) {
            log.error("json parsing error", e);
            throw new IllegalArgumentException("Cannot serialize settings to JSON", e);
        }

        if (existingId != null) {
            params.addValue("id", existingId);
        }
        return params;
    }

    private MapSqlParameterSource constructParameterSourceForSection(UUID profileId, DevProductivityProfile.Section section, UUID existingId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("profile_id", profileId);
        try {
            params.addValue("body", objectMapper.writeValueAsString(section));
        } catch (JsonProcessingException e) {
            log.error("json parsing error", e);
            throw new IllegalArgumentException("Cannot serialize profile section to JSON", e);
        }
        if (existingId != null) {
            params.addValue("id", existingId);
        }
        return params;
    }

    private MapSqlParameterSource constructParameterSourceForFeature(UUID sectionId, DevProductivityProfile.Feature feature, UUID existingId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("section_id", sectionId);
        try {
            params.addValue("body", objectMapper.writeValueAsString(feature));
        } catch (JsonProcessingException e) {
            log.error("json parsing error", e);
            throw new IllegalArgumentException("Cannot serialize profile feature to JSON", e);
        }
        if (existingId != null) {
            params.addValue("id", existingId);
        }
        return params;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".dev_productivity_parent_profiles (" +
                        "  id                               UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        "  name                             VARCHAR NOT NULL," +
                        "  description                      VARCHAR," +
                        "  is_predefined                    BOOLEAN DEFAULT false," +
                        "  ticket_categorization_scheme_id  UUID REFERENCES " + company + ".ticket_categorization_schemes(id) ON DELETE RESTRICT," +
                        "  feature_ticket_categories_map         JSONB, \n" +
                        "  settings         JSONB NOT NULL DEFAULT '{}'::jsonb, \n" +
                        "  created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()," +
                        "  updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()" +
                        ")",
                "CREATE TABLE IF NOT EXISTS " + company + ".dev_productivity_profiles (" +
                        "  id                               UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        "  name                             VARCHAR NOT NULL," +
                        "  description                      VARCHAR," +
                        "  is_predefined                    BOOLEAN DEFAULT false," +
                        "  ticket_categorization_scheme_id  UUID REFERENCES " + company + ".ticket_categorization_schemes(id) ON DELETE RESTRICT," +
                        "  index                      INTEGER NOT NULL DEFAULT 1," +
                        "  enabled                    BOOLEAN DEFAULT true," +
                        "  settings         JSONB NOT NULL DEFAULT '{}'::jsonb, \n" +
                        "  matching_criteria         JSONB NOT NULL DEFAULT '[]'::jsonb, \n" +
                        "  created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()," +
                        "  updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()" +
                        ")",
                "CREATE TABLE IF NOT EXISTS " + company + ".dev_productivity_sections (" +
                        "  id               UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        "  body             JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "  profile_id       UUID NOT NULL REFERENCES " + company + ".dev_productivity_profiles(id) ON DELETE CASCADE," +
                        "  created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()," +
                        "  updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()" +
                        ")",
                "CREATE TABLE IF NOT EXISTS " + company + ".dev_productivity_features (" +
                        "  id               UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        "  body             JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "  section_id       UUID NOT NULL REFERENCES " + company + ".dev_productivity_sections(id) ON DELETE CASCADE," +
                        "  created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()," +
                        "  updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()" +
                        ")",
                "CREATE TABLE IF NOT EXISTS " + company + ".dev_productivity_feature_category_mappings (" +
                        "  id               UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        "  feature_id       UUID NOT NULL REFERENCES " + company + ".dev_productivity_features(id) ON DELETE CASCADE," +
                        "  category_id      UUID NOT NULL REFERENCES " + company + ".ticket_categorizations(id) ON DELETE RESTRICT" +
                        ")",
                "CREATE TABLE IF NOT EXISTS " + company + ".dev_productivity_profile_default(\n" +
                        "  id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "  default_col      VARCHAR NOT NULL DEFAULT \'Default\'," +
                        "  dev_productivity_profile_id UUID NOT NULL REFERENCES " + company + ".dev_productivity_profiles(id) ON DELETE CASCADE," +
                        "  UNIQUE(default_col)" +
                        ")",
                "CREATE TABLE IF NOT EXISTS " + company + ".dev_productivity_profile_ou_mappings(\n" +
                        "  id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "  ou_ref_id        INTEGER NOT NULL," +
                        "  dev_productivity_profile_id UUID NOT NULL REFERENCES " + company + ".dev_productivity_profiles(id) ON DELETE CASCADE," +
                        "  UNIQUE(ou_ref_id)" +
                        ")"
        );

        sqlList.forEach(template.getJdbcTemplate()::execute);
        if(!devProdProfilesV2Enabled)
            createPredefinedProfiles(company);
        return true;
    }

    private void createPredefinedProfiles(String company) {
        String resourceString = null;
        try {
            resourceString = ResourceUtils.getResourceAsString("db/default_data/dev_productivity/predefined_profiles.json", DevProductivityProfileDatabaseService.class.getClassLoader());
            List<DevProductivityProfile> profiles = objectMapper.readValue(resourceString,objectMapper.getTypeFactory().constructCollectionType(List.class,DevProductivityProfile.class));
            populatePreDefinedProfiles(company, profiles);
        } catch (IOException e) {
            log.warn("Failed to populate demo dashboard", e);
        }
    }

    private void populatePreDefinedProfiles(String company, List<DevProductivityProfile> profiles) {
        profiles.stream()
                .filter(p -> listByFilter(company,0,1,null,List.of(p.getName()),null, null).getTotalCount() == 0)
                .forEach(p -> {
                    try {
                        insertProfile(company,p);
                    } catch (SQLException e) {
                        log.error("Error while inserting pre-defined profile {} ",p.getName());
                    }
                });
    }
}
