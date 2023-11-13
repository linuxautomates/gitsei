package io.levelops.commons.databases.services.dev_productivity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Sets;
import io.levelops.commons.databases.converters.CountQueryConverter;
import io.levelops.commons.databases.converters.DevProductivityProfileConverters;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityParentProfile;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.models.database.organization.DBOrgAccessUsers;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.utils.CriteriaUtils;
import io.levelops.commons.databases.utils.DevProductivityProfileUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.MapUtils;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.web.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
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
public class DevProductivityParentProfileDatabaseService extends DatabaseService<DevProductivityParentProfile> {

    private static final Set<String> PARTIAL_MATCH_COLUMNS = Set.of("name");
    private static final Set<String> PARTIAL_MATCH_ARRAY_COLUMNS = Set.of();

    private static final String INSERT_PARENT_PROFILE_SQL_FORMAT = "INSERT INTO %s.dev_productivity_parent_profiles (name,description,is_predefined,ticket_categorization_scheme_id, feature_ticket_categories_map, settings) VALUES(:name,:description,:is_predefined,:ticket_categorization_scheme_id,:feature_ticket_categories_map::jsonb,:settings::jsonb) RETURNING id";
    private static final String INSERT_PROFILE_SQL_FORMAT = "INSERT INTO %s.dev_productivity_profiles (name,description,index,enabled,ticket_categorization_scheme_id, settings, matching_criteria, parent_profile_id) VALUES(:name,:description,:index,:enabled,:ticket_categorization_scheme_id,:settings::jsonb,:matching_criteria::jsonb,:parent_profile_id) RETURNING id";
    private static final String INSERT_PROFILE_SECTION_SQL_FORMAT = "INSERT INTO %s.dev_productivity_sections (body,profile_id) VALUES(:body::jsonb,:profile_id) RETURNING id";
    private static final String INSERT_PROFILE_FEATURE_SQL_FORMAT = "INSERT INTO %s.dev_productivity_features (body,section_id) VALUES(:body::jsonb,:section_id) RETURNING id";
    private static final String UPDATE_PARENT_PROFILE_SQL_FORMAT = "UPDATE %s.dev_productivity_parent_profiles SET name = :name, description = :description, ticket_categorization_scheme_id = :ticket_categorization_scheme_id, feature_ticket_categories_map = :feature_ticket_categories_map::jsonb, settings = :settings::jsonb, updated_at = now() WHERE id = :id";
    private static final String UPDATE_PROFILE_SQL_FORMAT = "UPDATE %s.dev_productivity_profiles SET name = :name, description = :description, enabled = :enabled, updated_at = now() WHERE id = :id";
    private static final String DELETE_PARENT_PROFILE_SQL_FORMAT = "DELETE FROM %s.dev_productivity_parent_profiles WHERE id = :id";
    private static final String DELETE_PARENT_PROFILE_BY_OU_REF_ID_SQL_FORMAT = "DELETE FROM %s.dev_productivity_parent_profiles WHERE id IN (SELECT dev_productivity_parent_profile_id FROM %s.dev_productivity_parent_profile_ou_mappings"
            + " WHERE ou_ref_id IN (:ou_ref_ids))";
    private static final String DELETE_SUB_PROFILE_BY_PARENT_PROFILE_ID_SQL_FORMAT = "DELETE FROM %s.dev_productivity_profiles WHERE parent_profile_id = :parent_profile_id";
    private static final String DELETE_SECTION_BY_PROFILE_ID_SQL_FORMAT = "DELETE FROM %s.dev_productivity_sections WHERE profile_id = :profile_id";
    private static final String DELETE_OU_MAPPING_BY_PARENT_PROFILE_ID_SQL_FORMAT = "DELETE FROM %s.dev_productivity_parent_profile_ou_mappings WHERE dev_productivity_parent_profile_id = :parent_profile_id";
    private static final String DELETE_OU_MAPPING_BY_OU_REF_ID_SQL_FORMAT = "DELETE FROM %s.dev_productivity_parent_profile_ou_mappings WHERE ou_ref_id = :ou_ref_id";
    private static final String UPSERT_DEFAULT_PROFILE_SQL_FORMAT = "INSERT INTO %s.dev_productivity_parent_profile_default (default_col,dev_productivity_parent_profile_id) "
            + "VALUES (:default_col,:dev_productivity_parent_profile_id) "
            + "ON CONFLICT(default_col) DO UPDATE SET dev_productivity_parent_profile_id = EXCLUDED.dev_productivity_parent_profile_id";
    private static final String INSERT_PROFILE_OU_MAPPING_SQL_FORMAT = "INSERT INTO %s.dev_productivity_parent_profile_ou_mappings (ou_ref_id,dev_productivity_parent_profile_id) "
            + "VALUES %s "
            + "ON CONFLICT(ou_ref_id) DO UPDATE SET dev_productivity_parent_profile_id = EXCLUDED.dev_productivity_parent_profile_id, trellis_enabled = true";

    private final NamedParameterJdbcTemplate template;
    private final PlatformTransactionManager transactionManager;
    private final ObjectMapper objectMapper;
    private final Boolean devProdProfilesV2Enabled;

    @Autowired
    public DevProductivityParentProfileDatabaseService(DataSource dataSource, ObjectMapper objectMapper,
                                                       @Value("${DEV_PROD_PROFILES_V2_ENABLED:false}") Boolean devProdProfilesV2Enabled) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.transactionManager = new DataSourceTransactionManager(dataSource);
        this.objectMapper = objectMapper;
        this.devProdProfilesV2Enabled = BooleanUtils.isTrue(devProdProfilesV2Enabled);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(TicketCategorizationSchemeDatabaseService.class, DevProductivityProfileDatabaseService.class);
    }

    @Override
    public String insert(String company, DevProductivityParentProfile t) throws SQLException {
        return insertParentProfile(company, t).toString();
    }

    private UUID insertParentProfile(String company, DevProductivityParentProfile t) throws SQLException {
        t = populateTicketCategoriesToFeatures(t);
        TransactionDefinition txDef = new DefaultTransactionDefinition();
        TransactionStatus txStatus = transactionManager.getTransaction(txDef);
        MapSqlParameterSource params = constructParameterSourceForParentProfile(t, null);
        String insertParentProfileSql = String.format(INSERT_PARENT_PROFILE_SQL_FORMAT, company);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        UUID parentProfileId = null;
        try {
            int updatedRows = template.update(insertParentProfileSql, params, keyHolder);
            if (updatedRows <= 0 || keyHolder.getKeys() == null) {
                throw new SQLException("Failed to insert dev productivity parent profile");
            }
            parentProfileId = (UUID) keyHolder.getKeys().get("id");
            UUID finalParentProfileId = parentProfileId;
            if(CollectionUtils.isNotEmpty(t.getSubProfiles())){
                for(DevProductivityProfile profile : t.getSubProfiles()){
                    insertSubProfile(company, finalParentProfileId, profile);
                }
            }
            if (CollectionUtils.isNotEmpty(t.getAssociatedOURefIds())) {
                    insertProfileOUMappingsBulk(company, finalParentProfileId, t.getAssociatedOURefIds().stream().map(Integer::parseInt).collect(Collectors.toSet()));
            }
            transactionManager.commit(txStatus);
        } catch (Exception e) {
            transactionManager.rollback(txStatus);
            log.error("Error inserting dev productivity profile", e);
            throw new SQLException("could not insert dev productivity profile");
        }
        return parentProfileId;
    }

    public DevProductivityParentProfile populateTicketCategoriesToFeatures(final DevProductivityParentProfile t) {
        if(t.getEffortInvestmentProfileId() != null && !MapUtils.isEmpty(t.getFeatureTicketCategoriesMap())){
            return t.toBuilder().subProfiles(t.getSubProfiles().stream().map(sp -> sp.toBuilder().sections(sp.getSections().stream().map(s -> s.toBuilder().features(s.getFeatures().stream().map(f -> {
                if(t.getFeatureTicketCategoriesMap().containsKey(f.getFeatureType())){
                    return f.toBuilder().ticketCategories(t.getFeatureTicketCategoriesMap().get(f.getFeatureType())).build();
                }else {
                    return f;
                }
            }).collect(Collectors.toList())).build()).collect(Collectors.toList())).build()).collect(Collectors.toList())).build();
        }
        return t;
    }

    private UUID insertSubProfile(String company, UUID parentProfileId, DevProductivityProfile profile) throws SQLException {
        MapSqlParameterSource params = constructParameterSourceForSubProfile(parentProfileId, profile, null);
        String insertProfileSql = String.format(INSERT_PROFILE_SQL_FORMAT, company);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        UUID profileId = null;
        try {
            int updatedRows = template.update(insertProfileSql, params, keyHolder);
            if (updatedRows <= 0 || keyHolder.getKeys() == null) {
                throw new SQLException("Failed to insert dev productivity parent profile");
            }
            profileId = (UUID) keyHolder.getKeys().get("id");
            UUID finalProfileId = profileId;
            if (CollectionUtils.isNotEmpty(profile.getSections())) {
                for (DevProductivityProfile.Section section : profile.getSections()) {
                    insertProfileSection(company, finalProfileId, section);
                }
            }
        } catch (Exception e) {
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
                .addValue("dev_productivity_parent_profile_id", devProductivityProfileId));
        return devProductivityProfileId;
    }

    private static final String SET_TRELLIS_ENABLED_SQL_FORMAT = "UPDATE %s.dev_productivity_parent_profile_ou_mappings SET trellis_enabled =:trellis_enabled WHERE ou_ref_id IN (:ou_ref_ids)";
    public Boolean setTrellisEnabledOnOUMappingByOuRefId(String company, Set<Integer> ouRefIds, Boolean trellisEnabled) {
        Map<String, Object> params = new HashMap<>();
        params.put("ou_ref_ids",ouRefIds);
        params.put("trellis_enabled",trellisEnabled);
        String setTrellisEnabledSQL = String.format(SET_TRELLIS_ENABLED_SQL_FORMAT, company);
        return template.update(setTrellisEnabledSQL, new MapSqlParameterSource(params)) > 0;
    }

    private int insertProfileOUMappingsBulk(String company, UUID profileId, Set<Integer> ouRefIds) {
        Map<String, Object> params = new HashMap<>();
        List<String> values = new ArrayList<>();
        for(Integer ouRefId : ouRefIds){
            int i = values.size();
            params.putAll(Map.of(
                    "ou_ref_id_" + i, ouRefId,
                    "dev_productivity_parent_profile_id_" + i, profileId
            ));
            values.add(MessageFormat.format("(:ou_ref_id_{0}, :dev_productivity_parent_profile_id_{0})", i));
        }
        if (values.isEmpty()) {
            log.warn("Skipping bulk insert: no valid data provided");
            return 0;
        }
        String insertProfileOUMappingSQL = String.format(INSERT_PROFILE_OU_MAPPING_SQL_FORMAT, company, String.join(", ",values));
        return template.update(insertProfileOUMappingSQL, new MapSqlParameterSource(params));
    }

    private UUID createAndMakeDefault(String company, DevProductivityParentProfile profile) throws Exception {
        TransactionDefinition txDef = new DefaultTransactionDefinition();
        TransactionStatus txStatus = transactionManager.getTransaction(txDef);
        UUID id;
        try {
            id = insertParentProfile(company, profile);
            upsertDefaultProfile(company, id);
            transactionManager.commit(txStatus);
        } catch (Exception e) {
            transactionManager.rollback(txStatus);
            throw e;
        }
        return id;
    }

    @Override
    public Boolean update(String company, DevProductivityParentProfile t) throws SQLException{
        DevProductivityParentProfile existingProfile = get(company, String.valueOf(t.getId())).orElse(null);
        if(existingProfile == null){
            throw new SQLException("Trellis profile does not exist with id "+t.getId());
        }
        return updateDevProductivityParentProfile(company, existingProfile, t);
    }

    public Boolean copyParentProfile(String company, DevProductivityParentProfile t, List<String> targetOURefIds) throws SQLException, NotFoundException {
        DevProductivityParentProfile centralProfile = getDefaultDevProductivityProfile(company).orElse(null);
        if(centralProfile == null){
            throw new NotFoundException("central profile not found for company "+ company);
        }
        Set<String> targetOUs = new HashSet<>(ListUtils.emptyIfNull(targetOURefIds));
        targetOUs.addAll(t.getAssociatedOURefIds());
        //If the payload is matching with central profile, just associate all target OUs to central profile and stop
        if(DevProductivityProfileUtils.isParentProfileSameRecursive(centralProfile, t)){
            //Delete the existing profiles associated with the OUs
            deleteParentProfilesByOuRefIds(company, targetOUs.stream().map(Integer::parseInt).collect(Collectors.toList()));
            return updateProfileOUMappings(company, centralProfile.getId(), targetOUs);
        }
        List<DevProductivityParentProfile> existingProfiles = listByFilter(company, 0, 1000, null, null, null, targetOUs.stream().map(Integer::parseInt).collect(Collectors.toList())).getRecords();
        Map<Integer,DevProductivityParentProfile> existingProfileMap = existingProfiles.stream()
                .flatMap(p -> p.getAssociatedOURefIds().stream().map(Integer::parseInt))
                .collect(Collectors.toMap(
                        i -> i,
                        p -> existingProfiles.stream()
                                .filter(o -> o.getAssociatedOURefIds().contains(String.valueOf(p)))
                                .findFirst()
                                .orElse(null)
                ));
        TransactionDefinition txDef = new DefaultTransactionDefinition();
        TransactionStatus txStatus = transactionManager.getTransaction(txDef);
        try{

            //For each OU, fetch the associated profile
            //If a profile is already available, update the profile payload
            //If a profile is not available yet, create a new profile with the payload and associate it with OU
            for(String ouRefId : targetOUs){
                DevProductivityParentProfile existingProfile = existingProfileMap.get(Integer.parseInt(ouRefId));
                if (existingProfile != null && BooleanUtils.isNotTrue(existingProfile.getDefaultProfile())) {
                    updateDevProductivityParentProfile(company, existingProfile, t.toBuilder().associatedOURefIds(List.of(ouRefId)).build());
                } else {
                    insertParentProfile(company, t.toBuilder().associatedOURefIds(List.of(ouRefId)).build());
                }
            }
            transactionManager.commit(txStatus);
        }catch (Exception e) {
            transactionManager.rollback(txStatus);
            log.error("Failed to update dev productivity profile", e);
            throw new SQLException("Failed to update dev productivity profile");
        }
        return true;
    }

    private boolean updateDevProductivityParentProfile(final String company, DevProductivityParentProfile existingProfile, DevProductivityParentProfile newProfile) throws SQLException {
        UUID parentProfileId = existingProfile.getId();
        try{
            if(!DevProductivityProfileUtils.areTicketCategoriesSame(existingProfile.getFeatureTicketCategoriesMap(), newProfile.getFeatureTicketCategoriesMap())){
                newProfile = populateTicketCategoriesToFeatures(newProfile);
            }
            if(!DevProductivityProfileUtils.isParentProfileSame(existingProfile, newProfile)){
                MapSqlParameterSource params = constructParameterSourceForParentProfile(newProfile, parentProfileId);
                String updateProfileSql = String.format(UPDATE_PARENT_PROFILE_SQL_FORMAT, company);
                int updatedRows = template.update(updateProfileSql, params);
                if (updatedRows <= 0) {
                    throw new SQLException("Could not update dev productivity profile " + parentProfileId);
                }
            }
            for(DevProductivityProfile newSubProfile : newProfile.getSubProfiles()){
                DevProductivityProfile existingSubProfile = existingProfile.getSubProfiles().get(newSubProfile.getOrder());
                if(existingSubProfile == null){
                    insertSubProfile(company, parentProfileId, newSubProfile);
                } else if(!DevProductivityProfileUtils.isSubProfileSameRecursive(existingSubProfile, newSubProfile)){
                    updateDevProductivitySubProfile(company, existingSubProfile, newSubProfile);
                }
            }
            updateProfileOUMappings(company,parentProfileId,newProfile.getAssociatedOURefIds().stream().collect(Collectors.toSet()));
        } catch (Exception e) {
            log.error("Error updating dev productivity profile", e);
            throw new SQLException("could not insert dev productivity profile");
        }

        return true;
    }

    private Boolean updateDevProductivitySubProfile(String company, DevProductivityProfile existingSubProfile, DevProductivityProfile newSubProfile) throws SQLException {
        UUID profileId = existingSubProfile.getId();
        if(!DevProductivityProfileUtils.isSubProfileSame(existingSubProfile, newSubProfile)){
            MapSqlParameterSource params = constructParameterSourceForSubProfile(null, newSubProfile, profileId);
            String updateProfileSql = String.format(UPDATE_PROFILE_SQL_FORMAT, company);
            int updatedRows = template.update(updateProfileSql, params);
            if (updatedRows <= 0) {
                throw new SQLException("Could not update dev productivity profile " + profileId);
            }
        }
        //drop all the existing sections in the profile
        deleteProfileSectionsByProfileId(company, profileId);
        for (DevProductivityProfile.Section section : newSubProfile.getSections()) {
            insertProfileSection(company, profileId, section);
        }
        return true;
    }

    private Optional<DevProductivityParentProfile> getAssociatedProfileByOuRefId(String company, Integer ouRefId) {
        return  listByFilter(company,0, 1, null, null, null, List.of(ouRefId)).getRecords().stream().findFirst();
    }

   public Boolean updateProfileOUMappings(String company, UUID parentProfileId, Set<String> assocaitedOURefIds) throws SQLException {

        try {
            List<Integer> ouRefIds = assocaitedOURefIds.stream(). map(Integer::parseInt).collect(Collectors.toList());
            List<Integer> existingOuRefIds = getAssociatedOusByProfileId(company, parentProfileId, true);
            if(ListUtils.isEqualList(existingOuRefIds,ouRefIds))
                return true;
            Set<Integer> mappingsToBeDeleted = CollectionUtils.removeAll(existingOuRefIds, ouRefIds).stream().collect(Collectors.toSet());
            Set<Integer> mappingsToBeInserted = CollectionUtils.removeAll(ouRefIds, existingOuRefIds).stream().collect(Collectors.toSet());

            if(CollectionUtils.isNotEmpty(mappingsToBeDeleted)){
                deleteSpecificOUMappingsByProfileId(company,parentProfileId, mappingsToBeDeleted);
            }
            if(CollectionUtils.isNotEmpty(mappingsToBeInserted)){
                insertProfileOUMappingsBulk(company,parentProfileId,mappingsToBeInserted);
            }
        } catch (Exception e) {
            log.error("Failed to update dev productivity profile", e);
            throw new SQLException("Failed to update dev productivity profile");
        }
        return true;
    }


    private static final String GET_ASSOCIATED_OUS_BY_PROFILE_ID_SQL_FORMAT = "SELECT ou_ref_id FROM %s.dev_productivity_parent_profile_ou_mappings WHERE dev_productivity_parent_profile_id = :dev_productivity_parent_profile_id and trellis_enabled = :trellis_enabled";
    private List<Integer> getAssociatedOusByProfileId(String company, UUID profileId, Boolean trellisEnabled) {
        String getOusSql = String.format(GET_ASSOCIATED_OUS_BY_PROFILE_ID_SQL_FORMAT, company);
        return template.query(getOusSql, Map.of("dev_productivity_parent_profile_id", profileId, "trellis_enabled", trellisEnabled), new RowMapper<Integer>() {
            @Override
            public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getInt("ou_ref_id");
            }
        });
    }

    private Boolean deleteProfileOUMappingsByProfileId(String company, UUID parentProfileId) {
        String deleteSql = String.format(DELETE_OU_MAPPING_BY_PARENT_PROFILE_ID_SQL_FORMAT, company);
        return template.update(deleteSql, Map.of("parent_profile_id", parentProfileId)) > 0;
    }

    private static final String DELETE_SPECIFIC_OU_MAPPING_BY_PARENT_PROFILE_ID_SQL_FORMAT = "DELETE FROM %s.dev_productivity_parent_profile_ou_mappings WHERE dev_productivity_parent_profile_id = :parent_profile_id and ou_ref_id IN (:ou_ref_ids)";
    private Boolean deleteSpecificOUMappingsByProfileId(String company, UUID parentProfileId, Set<Integer> mappingsToBeDeleted) {
        String deleteSql = String.format(DELETE_OU_MAPPING_BY_PARENT_PROFILE_ID_SQL_FORMAT, company);
        return template.update(deleteSql, Map.of("parent_profile_id", parentProfileId, "ou_ref_ids",mappingsToBeDeleted)) > 0;
    }


    private static final String GET_DEFAULT_PROFILE_ID_SQL_FORMAT = "SELECT dev_productivity_parent_profile_id FROM %s.dev_productivity_parent_profile_default LIMIT 1";
    public Optional<DevProductivityParentProfile> getDefaultDevProductivityProfile(String company) throws SQLException {
        String sql = String.format(GET_DEFAULT_PROFILE_ID_SQL_FORMAT, company);
        UUID defaultProfileId = template.queryForObject(sql,Map.of(),UUID.class);
        var results = listByFilter(company, 0, 100, List.of(defaultProfileId), null, null, null).getRecords();
        return results.stream().filter(DevProductivityParentProfile::getDefaultProfile).findFirst();
    }

    @Override
    public Optional<DevProductivityParentProfile> get(String company, String id) throws SQLException {
        var results = listByFilter(company, 0, 10, Collections.singletonList(UUID.fromString(id)), null, null, null).getRecords();
        return results.size() > 0 ? Optional.of(results.get(0)) : Optional.empty();
    }

    public DbListResponse<DevProductivityParentProfile> listByFilter(String company, Integer pageNumber, Integer pageSize, final List<UUID> ids,
                                                               final List<String> names, final Map<String, Map<String, String>> partialMatchMap, List<Integer> ouRefIds) {
        List<String> criterias = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource();

        parseCriterias(criterias, params, ids, names, partialMatchMap);
        String baseWhereClause = (CollectionUtils.isEmpty(criterias)) ? "" : " WHERE " + String.join(" AND ", criterias);
        String defaultMappingJoinClause = " LEFT JOIN " + company + ".dev_productivity_parent_profile_default dpd ON dp.id = dpd.dev_productivity_parent_profile_id ";
        String ouMappingJoinClause = " LEFT JOIN " + company + ".dev_productivity_parent_profile_ou_mappings dpo ON dp.id = dpo.dev_productivity_parent_profile_id ";
        String groupByClause = " GROUP BY dp.id, dpd.id";
        String orderByClause = " ORDER BY updated_at desc";
        String limitClause = " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String selectSqlBase = "SELECT dpd.id AS dev_productivity_default_mapping_id,array_remove(array_agg(ou_ref_id), NULL)::integer[] as ou_ref_ids, jsonb_object_agg(coalesce(ou_ref_id,0), coalesce(trellis_enabled, false)) AS ou_trellis_enabled_map, dp.* FROM " + company + ".dev_productivity_parent_profiles as dp "
                + defaultMappingJoinClause
                + ouMappingJoinClause
                + baseWhereClause
                + groupByClause
                + orderByClause;

        String ouRefIdCondition = "";
        if(CollectionUtils.isNotEmpty(ouRefIds)){
            ouRefIdCondition = " WHERE ou_ref_ids && ARRAY[ :ouRefIds ]";
            params.addValue("ouRefIds",ouRefIds);
        }
        String selectSql = "WITH profiles as ("+selectSqlBase+")"+"\n"+
                "SELECT * FROM profiles"+ouRefIdCondition+limitClause;
        String countSQL = "SELECT COUNT(*) FROM (" + selectSqlBase + ") AS counted";

        Integer totCount = 0;
        log.info("sql = " + selectSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DevProductivityParentProfile> devProductivityParentProfiles = template.query(selectSql, params, DevProductivityProfileConverters.mapDevProductivityParentProfile(objectMapper));
        log.debug("devProductivityParentProfiles.size() = {}", devProductivityParentProfiles.size());
        if (devProductivityParentProfiles.size() > 0) {
            //fetch sub-profiles
            devProductivityParentProfiles = devProductivityParentProfiles.stream().map(
                    (parentPfofile) -> {
                        String fetchSubProfiles = "SELECT * FROM " + company +".dev_productivity_profiles WHERE parent_profile_id =:parent_profile_id order by index";
                        MapSqlParameterSource params1 = new MapSqlParameterSource();
                        params1.addValue("parent_profile_id", parentPfofile.getId());
                        List<DevProductivityProfile> devProductivityProfiles = template.query(fetchSubProfiles, params1, DevProductivityProfileConverters.mapDevProductivityProfile(objectMapper));
                        devProductivityProfiles = devProductivityProfiles.stream().map((profile) -> {
                            String fetchSections = "SELECT * FROM " + company + ".dev_productivity_sections WHERE profile_id =:profile_id";
                            MapSqlParameterSource params2 = new MapSqlParameterSource();
                            params2.addValue("profile_id", profile.getId());
                            List<DevProductivityProfile.Section> devProductivitySections = template.query(fetchSections, params2, DevProductivityProfileConverters.mapDevProductivitySection(objectMapper));
                            if (devProductivitySections.size() > 0) {
                                //fetch features
                                devProductivitySections = devProductivitySections.stream().map((section) -> {
                                    String fetchFeatures = "SELECT * FROM " + company + ".dev_productivity_features WHERE section_id =:section_id";
                                    MapSqlParameterSource params3 = new MapSqlParameterSource();
                                    params3.addValue("section_id", section.getId());
                                    List<DevProductivityProfile.Feature> devProductivityFeatures = template.query(fetchFeatures, params3, DevProductivityProfileConverters.mapDevProductivityFeature(objectMapper));
                                    Collections.sort(devProductivityFeatures, Comparator.comparingInt(DevProductivityProfile.Feature::getOrder));
                                    return section.toBuilder().features(devProductivityFeatures).build();
                                }).collect(Collectors.toList());
                            }
                            return profile.toBuilder().sections(devProductivitySections)
                                    .build();
                        }).collect(Collectors.toList());
                        return parentPfofile.toBuilder().subProfiles(devProductivityProfiles)
                                .build();
                    }).collect(Collectors.toList());

            totCount = devProductivityParentProfiles.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
            if (devProductivityParentProfiles.size() == pageSize) {
                log.info("sql = " + countSQL); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
                log.info("params = {}", params);
                totCount = template.query(countSQL, params, CountQueryConverter.countMapper()).get(0);
            }
        }
        return DbListResponse.of(devProductivityParentProfiles, totCount);
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
    public DbListResponse<DevProductivityParentProfile> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return listByFilter(company, pageNumber, pageSize, null, null, null, null);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        Optional<DevProductivityParentProfile> profile = get(company,id);
        if(profile.isPresent() && profile.get().getDefaultProfile()) {
            throw new RuntimeException("Cannot delete central trellis profile");
        }
        String deleteSql = String.format(DELETE_PARENT_PROFILE_SQL_FORMAT, company);
        return template.update(deleteSql, Map.of("id", UUID.fromString(id))) > 0;
    }

    public Boolean deleteParentProfilesByOuRefIds(String company, List<Integer> ouRefIds) throws SQLException {
        if(CollectionUtils.isEmpty(ouRefIds)){
            throw new SQLException("no ou ref ids provided to delete profile");
        }
        String deleteSql = String.format(DELETE_PARENT_PROFILE_BY_OU_REF_ID_SQL_FORMAT, company, company);
        return template.update(deleteSql, Map.of("ou_ref_ids", ouRefIds)) > 0;
    }

    private Boolean deleteSubProfilesByParentProfileId(String company, UUID parentProfileId) {
        String deleteSql = String.format(DELETE_SUB_PROFILE_BY_PARENT_PROFILE_ID_SQL_FORMAT, company);
        return template.update(deleteSql, Map.of("parent_profile_id", parentProfileId)) > 0;
    }

    private Boolean deleteProfileSectionsByProfileId(String company, UUID profileId) {
        String deleteSql = String.format(DELETE_SECTION_BY_PROFILE_ID_SQL_FORMAT, company);
        return template.update(deleteSql, Map.of("profile_id", profileId)) > 0;
    }


    private Boolean deleteProfileOUMappingsByParentProfileId(String company, UUID parentProfileId) {
        String deleteSql = String.format(DELETE_OU_MAPPING_BY_PARENT_PROFILE_ID_SQL_FORMAT, company);
        return template.update(deleteSql, Map.of("parent_profile_id", parentProfileId)) > 0;
    }

    public Boolean deleteProfileOUMappingsByOuRefId(String company, Integer ouRefId) {
        String deleteSql = String.format(DELETE_OU_MAPPING_BY_OU_REF_ID_SQL_FORMAT, company);
        return template.update(deleteSql, Map.of("ou_ref_id", ouRefId)) > 0;
    }
  
    private MapSqlParameterSource constructParameterSourceForParentProfile(DevProductivityParentProfile t, final UUID existingId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("name", t.getName());
        params.addValue("description", t.getDescription());
        params.addValue("is_predefined",t.getPredefinedProfile());
        params.addValue("ticket_categorization_scheme_id", t.getEffortInvestmentProfileId());
        try {
            params.addValue("feature_ticket_categories_map",objectMapper.writeValueAsString(t.getFeatureTicketCategoriesMap()));
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

    private MapSqlParameterSource constructParameterSourceForSubProfile(UUID parentProfileId, DevProductivityProfile t, final UUID existingId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("parent_profile_id", parentProfileId);
        params.addValue("name", t.getName());
        params.addValue("description", t.getDescription());
        params.addValue("index", t.getOrder());
        params.addValue("enabled", t.getEnabled());
        params.addValue("ticket_categorization_scheme_id",t.getEffortInvestmentProfileId());
        try {
            params.addValue("settings", objectMapper.writeValueAsString(t.getSettings()));
        } catch (JsonProcessingException e) {
            log.error("json parsing error", e);
            throw new IllegalArgumentException("Cannot serialize settings to JSON", e);
        }
        try {
            params.addValue("matching_criteria", objectMapper.writeValueAsString(t.getMatchingCriteria()));
        } catch (JsonProcessingException e) {
            log.error("json parsing error", e);
            throw new IllegalArgumentException("Cannot serialize matching_criteria to JSON", e);
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
        List<String> sqlList = new ArrayList<>();
        sqlList.addAll(List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".dev_productivity_parent_profile_default(\n" +
                        "  id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "  default_col      VARCHAR NOT NULL DEFAULT \'Default\'," +
                        "  dev_productivity_parent_profile_id UUID NOT NULL REFERENCES " + company + ".dev_productivity_parent_profiles(id) ON DELETE CASCADE," +
                        "  UNIQUE(default_col)" +
                        ")",
                "CREATE TABLE IF NOT EXISTS " + company + ".dev_productivity_parent_profile_ou_mappings(\n" +
                        "  id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "  ou_ref_id        INTEGER NOT NULL," +
                        "  trellis_enabled  BOOLEAN DEFAULT true," +
                        "  dev_productivity_parent_profile_id UUID NOT NULL REFERENCES " + company + ".dev_productivity_parent_profiles(id) ON DELETE CASCADE," +
                        "  UNIQUE(ou_ref_id)" +
                        ")"
        ));
        if(devProdProfilesV2Enabled){
            sqlList.add("ALTER TABLE IF EXISTS "  + company + ".dev_productivity_profiles ADD COLUMN IF NOT EXISTS parent_profile_id  UUID NOT NULL REFERENCES " + company + ".dev_productivity_parent_profiles(id) ON DELETE CASCADE");
        }

        sqlList.forEach(template.getJdbcTemplate()::execute);
        if(devProdProfilesV2Enabled)
            createCentralTrellisProfile(company);
        return true;
    }

    //Central profile is the only default profile in the tenant
    public void createCentralTrellisProfile(String company) {
        String resourceString = null;
        try {
            resourceString = ResourceUtils.getResourceAsString("db/default_data/dev_productivity/central_profile.json", DevProductivityParentProfileDatabaseService.class.getClassLoader());
            DevProductivityParentProfile centralProfile = objectMapper.readValue(resourceString,DevProductivityParentProfile.class);
            createAndMakeDefault(company, centralProfile);
        } catch (Exception e) {
            log.warn("Failed to populate central trellis profile", e);
        }
    }
}
