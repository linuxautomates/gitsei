package io.levelops.commons.helper.organization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.organization.DBOrgContentSection;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.ingestion.models.IntegrationType;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Log4j2
public class OrgUnitHelper {

    public static final boolean DO_NOT_USE_INTEGRATION_PREFIX = false;

    private final OrgUnitsDatabaseService unitsService;
    private final IntegrationService integrationService;

    public OrgUnitHelper(final OrgUnitsDatabaseService unitsService, final IntegrationService integrationService) {
        this.unitsService = unitsService;
        this.integrationService = integrationService;
    }

    public Set<Integer> insertNewOrgUnits(final String company, final Stream<DBOrgUnit> units) {
        var ids = new HashSet<Integer>();
        units.forEach(unit -> {
            try {
                var id = unitsService.insertForId(company, unit);
                ids.add(id.getRight());
                activateVersion(company, id.getLeft());
            } catch (SQLException e) {
                log.error("[{}] Unable to save unit: {}", company, unit, e);
            }
        });
        return ids;
    }

    public void updateUnits(final String company, final Stream<DBOrgUnit> units) {
        units.forEach(unit -> {
            try {
                Optional<DBOrgUnit> ouBeforeUpdation = unitsService.get(company, unit.getRefId(), true);
                String existingPath = StringUtils.EMPTY;
                UUID ouIdBeforeUpdation = null;
                UUID categoryId = null;
                if (ouBeforeUpdation.isPresent()) {
                    existingPath = ouBeforeUpdation.get().getPath();
                    ouIdBeforeUpdation = ouBeforeUpdation.get().getId();
                    categoryId = ouBeforeUpdation.get().getOuCategoryId();
                }
                // insert new version
                var ids = unitsService.insertForId(company, unit);
                Optional<DBOrgUnit> ouAfterUpdation = unitsService.get(company, ids.getLeft());
                if (ouAfterUpdation.isPresent()) {
                    boolean hasCategoryChanged = !ouAfterUpdation.get().getOuCategoryId().equals(categoryId);
                    unitsService.updatePathAndOuCategoryId(company, ouAfterUpdation.get().getPath(), existingPath, ouAfterUpdation.get().getRefId(), true,
                            ouAfterUpdation.get().getOuCategoryId(), hasCategoryChanged, ouAfterUpdation.get().getWorkspaceId());
                }
                if (ouIdBeforeUpdation != null) {
                    unitsService.insertOuDashboardMappings(company, ids, ouIdBeforeUpdation);
                }
                // activate new version
                activateVersion(company, ids.getLeft());
            } catch (SQLException e) {
                log.error("[{}]", company, e);
            }
        });
    }

    public void deleteUnits(final String company, Set<Integer> unitIdsToDelete) {
        unitIdsToDelete.forEach(refId -> {
            try {
                var unit = unitsService.get(company, refId, true);
                if (unit.isEmpty()) {
                    return;
                }
                DBOrgUnit dbOrgUnit = unit.get();
                Boolean updated = unitsService.update(company, dbOrgUnit.getId(), false);
                if (updated) {
                    unitsService.updatePathAndOuCategoryId(company, StringUtils.EMPTY, dbOrgUnit.getPath(), dbOrgUnit.getRefId(),
                            false, dbOrgUnit.getOuCategoryId(), false, dbOrgUnit.getWorkspaceId());
                }
            } catch (SQLException e) {
                log.error("[{}] ", company, e);
            }
        });
    }

    public void activateVersion(final String company, final Integer refId, final Integer version) throws SQLException {
        // get version
        activateVersion(company, unitsService.get(company, refId, version));
    }

    public void activateVersion(final String company, final UUID id) throws SQLException {
        // get new unit from the db
        activateVersion(company, unitsService.get(company, id));
    }

    public void activateVersion(final String company, final Optional<DBOrgUnit> newUnit) throws SQLException {
        if (newUnit.isEmpty()) {
            return;
        }
        // get current active version 
        var currentActiveUnit = unitsService.get(company, newUnit.get().getRefId());
        activateVersion(company, newUnit, currentActiveUnit);
    }

    public void activateVersion(final String company, final Optional<DBOrgUnit> newUnit, final Optional<DBOrgUnit> currentActiveUnit) throws SQLException {
        if (newUnit.isEmpty()) {
            log.info("[{}] unable to activate version '{}' for '{}. Not found in the db.", company);
            return;
        }
        if (currentActiveUnit.isPresent() && currentActiveUnit.get().getId() == newUnit.get().getId()) {
            return;
        }
        // activate new version
        unitsService.update(company, newUnit.get().getId(), true);
        // deactivate previous version
        if (currentActiveUnit.isPresent()) {
            unitsService.update(company, currentActiveUnit.get().getId(), false);
        }

    }

    /**
     * Retrieves the configuration for the OU specified in the request, if any, and returns a OUConfig object with all the OU settings on it and the request, modified or original, to reflect the OU filters applied.
     *
     * @param company
     * @param integrationType
     * @param request
     * @return
     * @throws SQLException
     */
    public OUConfiguration getOuConfigurationFromRequest(final String company, final IntegrationType integrationType, final DefaultListRequest request, final boolean useIntegrationPrefix) throws SQLException {
        return getOuConfigurationFromRequest(company, Set.of(integrationType), request, useIntegrationPrefix);
    }

    /**
     * Retrieves the configuration for the OU specified in the request, if any, and returns a OUConfig object with all the OU settings on it and the request, modified or original, to reflect the OU filters applied.
     *
     * @param company
     * @param integrationType
     * @param request
     * @return
     * @throws SQLException
     */
    public OUConfiguration getOuConfigurationFromRequest(final String company, final IntegrationType integrationType, final DefaultListRequest request) throws SQLException {
        return getOuConfigurationFromRequest(company, Set.of(integrationType), request);
    }

    /**
     * Retrieves the configuration for the OU specified in the request, if any, and returns a OUConfig object with all the OU settings on it and the request, modified or original, to reflect the OU filters applied.
     *
     * @param company
     * @param integrationTypes
     * @param request
     * @return
     * @throws SQLException
     */
    public OUConfiguration getOuConfigurationFromRequest(final String company, final Set<IntegrationType> integrationTypes, final DefaultListRequest request) throws SQLException {
        return getOuConfigurationFromRequest(company, integrationTypes, request, DO_NOT_USE_INTEGRATION_PREFIX);
    }

    /**
     * Retrieves the configuration for the OU specified in the request, if any, and returns a OUConfig object with all the OU settings on it and the request, modified or original, to reflect the OU filters applied.
     *
     * @param company
     * @param integrationTypes
     * @param request
     * @return
     * @throws SQLException
     */
    public OUConfiguration getOuConfigurationFromRequest(final String company, final Set<IntegrationType> integrationTypes, final DefaultListRequest request, final boolean useIntegrationPrefix) throws SQLException {
        // get OU
        if (request == null || CollectionUtils.isEmpty(request.getOuIds())) {
            return OUConfiguration.builder().request(request).build();
        }
        var refId = request.getOuIds().iterator().next();
        var optionalOu = unitsService.get(company, refId);
        if (optionalOu.isEmpty()) {
            return OUConfiguration.builder().request(request).build();
        }
        var ou = optionalOu.get();

        return getOuConfigurationFromDBOrgUnit(company, integrationTypes, request, ou, useIntegrationPrefix);
    }

    /**
     * Retrieves the configuration for the OU specified by the ouId (UUID), if any, and returns a OUConfig object with all the OU settings.
     * Since no integration type is passed in this method, all sections will be returned.
     *
     * @param company
     * @param ouId    UUID
     */
    public Optional<OUConfiguration> getOuConfiguration(final String company, final UUID ouId) throws SQLException {
        return getOuConfiguration(company, null, ouId);
    }

    public List<OUConfiguration> getOuConfigurationList(final String company, final Set<IntegrationType> integrationTypes, final List<UUID> ouIdList) throws SQLException {
        // get OU
        QueryFilter filter = QueryFilter.builder()
                .strictMatch("ou_id", ouIdList)
                .count(false)
                .build();
        var ouList = unitsService.filter(company, filter, 0, 100).getRecords();
        if (ouList.isEmpty()) {
            return List.of();
        }

        List<OUConfiguration> ouConfigurationList = Lists.newArrayList();
        ouList.forEach(ou -> {
            ouConfigurationList.add(getOuConfigurationFromDBOrgUnit(company, integrationTypes, null, ou, DO_NOT_USE_INTEGRATION_PREFIX));
        });

        return ouConfigurationList;
    }

    public Optional<OUConfiguration> getOuConfiguration(final String company, final Set<IntegrationType> integrationTypes, final UUID ouId) throws SQLException {
        // get OU
        var optionalOu = unitsService.get(company, ouId);
        if (optionalOu.isEmpty()) {
            return Optional.empty();
        }
        var ou = optionalOu.get();

        return Optional.of(getOuConfigurationFromDBOrgUnit(company, integrationTypes, null, ou, DO_NOT_USE_INTEGRATION_PREFIX));
    }

    /**
     *
     */
    @SuppressWarnings("unchecked")
    public OUConfiguration getOuConfigurationFromDBOrgUnit(final String company, final Set<IntegrationType> integrationTypes, final DefaultListRequest request, final DBOrgUnit ou, final boolean useIntegrationPrefix) {
        var sectionFilters = new HashMap<String, Object>();
        var staticUsers = new AtomicBoolean();
        var dynamicUsers = new AtomicBoolean();
        Set<Integer> integrationIds = new HashSet<Integer>();
        // get filters, dynamic users selections, static user flag and integration ids from all sections
        List<String> ouExclusions = new ArrayList<>();
        if (request != null && CollectionUtils.isNotEmpty(request.getOuExclusions())) {
            ouExclusions = request.getOuExclusions();
        }

        Map<String, Set<String>> ouUserFilterDesignation = Map.of();
        if (request != null && MapUtils.isNotEmpty(request.getOuUserFilterDesignation())) {
            ouUserFilterDesignation = request.getOuUserFilterDesignation();
        }

        processSections(ou.getSections(), integrationTypes, sectionFilters, dynamicUsers, staticUsers, integrationIds,
                ouExclusions, useIntegrationPrefix, ouUserFilterDesignation);
        // mix filters
        // filters from the ou will replace filters from the query
        var newFilters = new HashMap<String, Object>();
        if (request != null && request.getFilter() != null) {
            newFilters.putAll(request.getFilter());
        }

        var partialField = newFilters.containsKey("partial") ? "partial" : "partial_match";
        var sectionsPartialField = sectionFilters.containsKey("partial") ? "partial" : "partial_match";
        // filters not present in the OU but present in the widget will be passed on
        // filters present in both places will be overwritten by the OU ones
        // custom_fields and partial_match will be mixed in too. replace the ones in both places, use the widget ones when there are no OU ones
        var prefixConfig = new HashSet<String>();
        // in this case, we get all integration prefixes accordingly with the flag useIntegrationPrefix but, in some other cases we could have a mix.
        ou.getSections().stream()
                .filter(s -> useIntegrationPrefix)
                .filter(e -> e.getIntegrationType() != null)
                .filter(e -> StringUtils.isNotBlank(e.getIntegrationType().getRequestPrefix()))
                .forEach(e -> prefixConfig.add(e.getIntegrationType().getRequestPrefix()));
        log.info("[{}]: {}", useIntegrationPrefix, prefixConfig);

        processSpecialFilters(newFilters, sectionFilters, "exclude", "exclude", prefixConfig, ouExclusions);
        processSpecialFilters(newFilters, sectionFilters, "custom_fields", "custom_fields", prefixConfig, ouExclusions);

        processSpecialFilters(newFilters, sectionFilters, "workitem_custom_fields", "workitem_custom_fields", Collections.EMPTY_SET, ouExclusions);
        processSpecialFilters(newFilters, sectionFilters, "workitem_attributes", "workitem_attributes", Collections.EMPTY_SET, ouExclusions);
        // if the section filters have partial match for custom_fields we will tackle them differently.
        if (sectionFilters.containsKey(sectionsPartialField)) {
            // at this point newFilters contains all the request filters so,
            // we take the partial match to be used as the base filters

            // if we have prefixes we process all the prefixed custom_fields
            Set<String> prefixes = prefixConfig;
            if (CollectionUtils.isEmpty(prefixConfig)) {
                // if no prefixes then we use the regular custom_fields
                prefixes = Set.of("");
            }
            for (String p : prefixes) {
                var pKey = p + "custom_fields";
                if (((Map<String, Object>) sectionFilters.getOrDefault(sectionsPartialField, Map.<String, Object>of())).containsKey(pKey)) {
                    var tmpBase = new HashMap<>((Map<String, Object>) newFilters.getOrDefault(sectionsPartialField, Map.of()));
                    var tmpModifiers = (Map<String, Object>) sectionFilters.get(sectionsPartialField);
                    processSpecialFilters(tmpBase, tmpModifiers, pKey, pKey, Set.of(), ouExclusions);
                    newFilters.put(partialField, tmpBase);
                    sectionFilters.put(sectionsPartialField, tmpModifiers);
                }
            }
        }
        // we process now the partial field after the special filters inside have been taken care off.
        processSpecialFilters(newFilters, sectionFilters, partialField, sectionsPartialField, Set.of(), ouExclusions);
        newFilters.putAll(sectionFilters);
        // TODO: if there are OU time range filters, those won't be applied


        // create OU config
        var configBuilder = OUConfiguration.builder()
                .ouId(ou.getId())
                .ouRefId(ou.getRefId())
                .filters(newFilters)
                .request(request == null ? null : request.toBuilder().filter(newFilters).build())
                .ouExclusions(ouExclusions)
                .staticUsers(staticUsers.get())
                .dynamicUsers(dynamicUsers.get())
                .sections(ou.getSections().stream()
                        .filter(section -> section.getIntegrationType() == null || CollectionUtils.isEmpty(integrationTypes) || integrationTypes.contains(section.getIntegrationType()))
                        .collect(Collectors.toSet())
                );
        // if we didn't find any section with integration ids then keep the original request's integration ids
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            newFilters.put("integration_ids", integrationIds);
            configBuilder.integrationIds(integrationIds);
        }
        // if there are no integration ids form the original request but we have a type specified then get the integration ids from the configured integrations
        if ((newFilters.get("integration_ids") == null || (CollectionUtils.isEmpty((Collection) newFilters.get("integration_ids")))) && CollectionUtils.isNotEmpty(integrationTypes)) {
            // get integrations from the db
            try {
                var results = integrationService.listByFilter(company, null, integrationTypes.stream().map(i -> i.toString().toLowerCase()).collect(Collectors.toList()), null, null, null, 0, 100);
                integrationIds = results.getRecords().stream().map(i -> Integer.valueOf(i.getId())).collect(Collectors.toSet());
                log.info("[{}] Got integration ids from the integrations configured filtered by types '{}': {}", company, integrationTypes, integrationIds);
                newFilters.put("integration_ids", integrationIds);
                configBuilder.integrationIds(integrationIds);
            } catch (SQLException e1) {
                log.error("[{}] Unable to get the Integrations for the types {}", company, integrationTypes, e1);
            }
        }

        if (request != null && MapUtils.isNotEmpty(request.getOuUserFilterDesignation())) {
            var cicdFields = new HashSet<String>();
            var scmFields = new HashSet<String>();
            if (CollectionUtils.isNotEmpty(request.getOuUserFilterDesignation().get("jira"))) {
                configBuilder.jiraFields(getIntegrationFieldsFromFilter(request, "jira", ouExclusions));
            }
            if (CollectionUtils.isNotEmpty(request.getOuUserFilterDesignation().get("jenkins"))) {
                Set<String> jenkinsFields = getIntegrationFieldsFromFilter(request, "jenkins", ouExclusions);
                configBuilder.jenkinsFields(jenkinsFields);
                cicdFields.addAll(jenkinsFields);
            }
            if (CollectionUtils.isNotEmpty(request.getOuUserFilterDesignation().get("azure_pipelines"))) {
                Set<String> azurePipelinesFields = getIntegrationFieldsFromFilter(request, "azure_pipelines", ouExclusions);
                configBuilder.azurePipelinesFields(azurePipelinesFields);
                cicdFields.addAll(azurePipelinesFields);
            }
            if (CollectionUtils.isNotEmpty(request.getOuUserFilterDesignation().get("pagerduty"))) {
                configBuilder.pagerDutyFields(getIntegrationFieldsFromFilter(request, "pagerduty", ouExclusions));
            }
            if (CollectionUtils.isNotEmpty(request.getOuUserFilterDesignation().get("azure_devops"))) {
                Set<String> azureDevopsFields = getIntegrationFieldsFromFilter(request, "azure_devops", ouExclusions);
                configBuilder.adoFields(azureDevopsFields);
                scmFields.addAll(azureDevopsFields);
            }
            if (CollectionUtils.isNotEmpty(request.getOuUserFilterDesignation().get("github"))) {
                Set<String> githubFields = getIntegrationFieldsFromFilter(request, "github", ouExclusions);
                configBuilder.githubFields(githubFields);
                scmFields.addAll(githubFields);
            }
            if (CollectionUtils.isNotEmpty(request.getOuUserFilterDesignation().get("gitlab"))) {
                Set<String> gitlabFields = getIntegrationFieldsFromFilter(request, "gitlab", ouExclusions);
                configBuilder.gitlabFields(gitlabFields);
                scmFields.addAll(gitlabFields);
            }
            if (CollectionUtils.isNotEmpty(request.getOuUserFilterDesignation().get("helix"))) {
                Set<String> helixFields = getIntegrationFieldsFromFilter(request, "helix", ouExclusions);
                configBuilder.helixFields(helixFields);
                scmFields.addAll(helixFields);
            }
            if (CollectionUtils.isNotEmpty(request.getOuUserFilterDesignation().get("bitbucket"))) {
                Set<String> bitbucketFields = getIntegrationFieldsFromFilter(request, "bitbucket", ouExclusions);
                configBuilder.bitbucketFields(bitbucketFields);
                scmFields.addAll(bitbucketFields);
            }
            if (CollectionUtils.isNotEmpty(request.getOuUserFilterDesignation().get("bitbucket_server"))) {
                Set<String> bitbucketServerFields = getIntegrationFieldsFromFilter(request, "bitbucket_server", ouExclusions);
                configBuilder.bitbucketServerFields(bitbucketServerFields);
                scmFields.addAll(bitbucketServerFields);
            }
            // set the scm and cicd fields if any, if non collected, will use defaults
            if (scmFields.size() > 0) {
                configBuilder.scmFields(Set.copyOf(scmFields));
            }
            if (cicdFields.size() > 0) {
                configBuilder.cicdFields(Set.copyOf(cicdFields));
            }
        }
        var config = configBuilder.build();

        log.info("[{}] OU config: {}", company, config);
        return config;
    }

    private Set<String> getIntegrationFieldsFromFilter(DefaultListRequest request, String integrationType, List<String> ouExclusions) {

        Set<String> integrationFieldSet = request.getOuUserFilterDesignation()
                .get(integrationType)
                .stream()
                .filter(item -> !ouExclusions.contains(item))
                .collect(Collectors.toSet());

        if (integrationFieldSet.size() == 1 && integrationFieldSet.stream().findFirst().get().equalsIgnoreCase("none")) {
            return Collections.EMPTY_SET;
        }

        return integrationFieldSet;
    }

    private String getSprintField(Map<String, Set<String>> ouFilterDesignation) {
        Set<String> sprintMappingSet = ouFilterDesignation.get("sprint");
        if (CollectionUtils.isNotEmpty(sprintMappingSet)) {
            return sprintMappingSet.iterator().next();
        }
        return null;
    }


    /**
     * The base filters that match the filters in the sections will be overwritten.<br /><br />
     * The filters defined by the prefixed key (pKey) in the base filters will be overwritten by the new filters that combine the filters from the base and the filters from the modifier.<br /><br />
     * The filters defined by the key will be removed from the sectionFilters object.<br /><br />
     * <br /><br />
     * Examples:<br /><br />
     * <ol>
     *   <li>
     *     <b>key</b>: partial_match   <b>pKey</b>: partial_match<br /><br />
     *     request: <code>{"partial_match":{"project": "Hello", "status": "IN P", "name": "My p"}}</code><br /><br />
     *     sectionFilters:  <code>{"partial_match":{"project": "Hello", "manager": "BE man"}}</code><br /><br />
     *      <br /><br />
     *     new request filters: <code>{"partial_match":{"project": "Hello", "status": "IN P", "name": "My p", "manager": "BE man"}}</code><br /><br /><br /><br />
     *   </li>
     *   <li>
     *     <b>key</b>: partial_match   <b>pKey</b>: partial_match<br /><br />
     *     request: <code>{"partial_match":{"project": "Hello", "status": "IN P", "name": "My p"}}</code><br /><br />
     *     sectionFilters:  <code>{"partial_match":{"project": "Testing", "status": "CL", "manager": "BE man"}}</code><br /><br />
     *     <br /><br />
     *     new request filters: <code>{"partial_match":{"project": "Testing", "status": "CL", "name": "My p", "manager": "BE man"}}</code><br /><br /><br />
     *   </li>
     *   <li>
     *     <b>key</b>: partial_match   <b>pKey</b>: jira_partial_match<br /><br />
     *     request: <code>{"jira_partial_match":{"project": "Hello", "status": "IN P", "name": "My p"}}</code><br /><br />
     *     sectionFilters:  <code>{"partial_match":{"project": "Hello", "status": "IN P", "name": "My p", "manager": "BE man"}}</code><br /><br />
     *     <br /><br />
     *     new request filters: <code>{"jira_partial_match":{"project": "Hello", "status": "IN P", "name": "My p", "manager": "BE man"}}</code><br /><br /><br />
     *   </li>
     * </ol>
     *
     * @param baseFilters     filters to be modified
     * @param modifierFilters filters to be used in to override or to be added to the base filters
     * @param modifierKey     name of the field in the modifier filters
     * @param prefixes        prefixes to be used to look up and mix in values in the base filters
     */
    private void processSpecialFilters(Map<String, Object> baseFilters, Map<String, Object> modifierFilters, String baseKey, String modifierKey,
                                       final Set<String> prefixes, List<String> ouExclusions) {
        // if prefixes are present we will use them to look up fields in the request filters and then replace them with the filters from the ou section. e.g. request=jira_custom_fields, ouconfig=custom_fields
        var pkeys = CollectionUtils.isEmpty(prefixes)
                ? Set.of(baseKey)
                : prefixes.stream().map(p -> p + baseKey).collect(Collectors.toSet());

        pkeys.stream().forEach(pKey -> processSpecialFiltersKey(baseFilters, modifierFilters, pKey, baseKey.startsWith("partial") ? modifierKey : pKey, ouExclusions));
    }

    /**
     * The filters from the request, that match the filters in the sections will be overwritten.<br /><br />
     * The filters defined by the prefixed key (pKey) will be overwritten by the new filters that combine the filters from the request and the filters from the sections.<br /><br />
     * The filters defined by the key will be removed from the sectionFilters object.<br /><br />
     * <br /><br />
     * Examples:<br /><br />
     * <ol>
     *   <li>
     *     <b>key</b>: partial_match   <b>pKey</b>: partial_match<br /><br />
     *     request: <code>{"partial_match":{"project": "Hello", "status": "IN P", "name": "My p"}}</code><br /><br />
     *     sectionFilters:  <code>{"partial_match":{"project": "Hello", "manager": "BE man"}}</code><br /><br />
     *      <br /><br />
     *     new request filters: <code>{"partial_match":{"project": "Hello", "status": "IN P", "name": "My p", "manager": "BE man"}}</code><br /><br /><br /><br />
     *   </li>
     *   <li>
     *     <b>key</b>: partial_match   <b>pKey</b>: partial_match<br /><br />
     *     request: <code>{"partial_match":{"project": "Hello", "status": "IN P", "name": "My p"}}</code><br /><br />
     *     sectionFilters:  <code>{"partial_match":{"project": "Testing", "status": "CL", "manager": "BE man"}}</code><br /><br />
     *     <br /><br />
     *     new request filters: <code>{"partial_match":{"project": "Testing", "status": "CL", "name": "My p", "manager": "BE man"}}</code><br /><br /><br />
     *   </li>
     *   <li>
     *     <b>key</b>: partial_match   <b>pKey</b>: jira_partial_match<br /><br />
     *     request: <code>{"jira_partial_match":{"project": "Hello", "status": "IN P", "name": "My p"}}</code><br /><br />
     *     sectionFilters:  <code>{"partial_match":{"project": "Hello", "status": "IN P", "name": "My p", "manager": "BE man"}}</code><br /><br />
     *     <br /><br />
     *     new request filters: <code>{"jira_partial_match":{"project": "Hello", "status": "IN P", "name": "My p", "manager": "BE man"}}</code><br /><br /><br />
     *   </li>
     * </ol>
     *
     * @param baseFilters     filters to be modified
     * @param modifierFilters filters to be used in to override or to be added to the base filters
     * @param modifiersKey    name of the field in the modifier filters
     * @param baseKey         name of the field in the bas filters
     */
    @SuppressWarnings("unchecked")
    private void processSpecialFiltersKey(Map<String, Object> baseFilters, Map<String, Object> modifierFilters,
                                          String baseKey, String modifiersKey, List<String> ouExclusions) {
        // if there are no special filters for the key either in the ou filters or in the request (taking prefixes into consideration), then just return... nothing to do
        if (!baseFilters.keySet().contains(baseKey) && !modifierFilters.containsKey(modifiersKey)) {
            return;
        }
        // get the special filters from the request
        var baseSpecialFilters = (Map<String, Object>) baseFilters.remove(baseKey);
        var finalSpecialFields = new HashMap<String, Object>();
        // add the special filters from the request to the final filters map to function as base
        if (MapUtils.isNotEmpty(baseSpecialFilters)) {
            finalSpecialFields.putAll(baseSpecialFilters);
        }
        // get the special filters from the OU sections
        var modifierSpecialFilters = (Map<String, Object>) modifierFilters.remove(modifiersKey);
        // if OU has any special filters then add them now to replace any of the same name comming from the request
        // and to add any not present in the request
        if (MapUtils.isNotEmpty(modifierSpecialFilters)) {
            modifierSpecialFilters.forEach((key, value) -> {
                if (!ouExclusions.contains(key)) {
                    finalSpecialFields.put(key, value);
                }
            });
        }
        // finally place or replace the special filters in the new filters object
        baseFilters.put(baseKey, finalSpecialFields);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void processSections(
            final Set<DBOrgContentSection> sections,
            final Set<IntegrationType> integrationTypes,
            final Map<String, Object> combinedSectionsFilters,
            final AtomicBoolean dynamicUsersFlag,
            final AtomicBoolean staticUsersFlag,
            final Set<Integer> integrationIds,
            List<String> ouExclusions,
            final boolean useIntegrationPrefix,
            final Map<String, Set<String>> ouFilterDesignation) {
        sections.stream()
                .filter(section -> section.getIntegrationType() == null || CollectionUtils.isEmpty(integrationTypes) || integrationTypes.contains(section.getIntegrationType()))
                .forEach(section -> {
                    // all section types (default and integration)
                    // get dynamic users
                    if (MapUtils.isNotEmpty(section.getDynamicUsers())) {
                        dynamicUsersFlag.set(true);
                    }
                    // static users flag
                    if (CollectionUtils.isNotEmpty(section.getUsers())) {
                        staticUsersFlag.set(true);
                    }

                    // Integration filters - 'integration' type sections only
                    if (section.getIntegrationId() != null) {
                        integrationIds.add(section.getIntegrationId());
                    }

                    if (MapUtils.isEmpty(section.getIntegrationFilters())) {
                        return;
                    }
                    var sprintField = getSprintField(ouFilterDesignation);
                    var integrationPrefix = useIntegrationPrefix ? section.getIntegrationType().getRequestPrefix() : "";
                    var isFirstSection = combinedSectionsFilters.size() == 0;
                    processSection(section, combinedSectionsFilters, ouExclusions, sprintField, integrationPrefix, isFirstSection);
                });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void processSection(final DBOrgContentSection section,
                                final Map<String, Object> combinedSectionsFilters,
                                final List<String> ouExclusions,
                                final String sprintField,
                                final String integrationPrefix,
                                final Boolean isFirstSection) {
        section.getIntegrationFilters().entrySet().stream()
                .forEach(e -> {
                    if (!isFirstSection) {
                        if (!ouExclusions.contains(e.getKey()) && e.getValue() instanceof Collection) {
                            var base = new HashSet<>((Collection) e.getValue());
                            var prefixedKey = integrationPrefix + e.getKey();
                            var old = combinedSectionsFilters.get(prefixedKey);
                            if (old == null) {
                                combinedSectionsFilters.put(prefixedKey, base);
                                return;
                            }
                            if (old instanceof Collection) {
                                base.addAll((Collection) old);
                            } else {
                                base.add(old);
                            }
                            combinedSectionsFilters.put(prefixedKey, base);
                            return;
                        }
                    }
                    if ("partial".equals(e.getKey()) || "partial_match".equals(e.getKey())) {
                        var partial = (Map<String, Object>) e.getValue();
                        var prefixedPartial = new HashMap<String, Object>();
                        partial.entrySet().stream().forEach(ex -> {
                            if (!ouExclusions.contains(ex.getKey())) {
                                if ("sprint".equals(ex.getKey()) && sprintField != null) {
                                    prefixedPartial.put(integrationPrefix + sprintField, ex.getValue());
                                } else {
                                    prefixedPartial.put(integrationPrefix + ex.getKey(), ex.getValue());
                                }
                            }
                        });
                        combinedSectionsFilters.put(e.getKey(), prefixedPartial);
                        return;
                    }
                    if ("exclude".equals(e.getKey())) {
                        var excludeMap = (Map<String, Object>) e.getValue();
                        var excludeMapNew = new HashMap<String, Object>();
                        excludeMap.entrySet().stream().forEach(ex -> {
                            if (!ouExclusions.contains(ex.getKey())) {
                                if ("sprint".equals(ex.getKey()) && sprintField != null) {
                                    excludeMapNew.put(integrationPrefix + sprintField, ex.getValue());
                                } else {
                                    excludeMapNew.put(integrationPrefix + ex.getKey(), ex.getValue());
                                }
                            }
                        });
                        combinedSectionsFilters.put(e.getKey(), excludeMapNew);
                        return;
                    }
                    if (!ouExclusions.contains(e.getKey())) {
                        if ("sprint".equals(e.getKey()) && sprintField != null) {
                            combinedSectionsFilters.put(integrationPrefix + sprintField, e.getValue());
                        } else {
                            combinedSectionsFilters.put(integrationPrefix + e.getKey(), e.getValue());
                        }
                    }
                });
    }

    /**
     * Returns a query to select the users that belong to the OU configuration provided as long as it has a default section.
     * </ br></ br>
     * For sections with integrations use (company, ouConfig, params, types)
     *
     * @param company
     * @param ouConfig
     * @param params   the params map to be used for the variables, if any, resulting in this query
     * @return Query containing id, display_name, cloud_id and integration_id from the integraton_users table
     */
    public static String getSelectForCloudIdsByOuConfig(final String company, final OUConfiguration ouConfig, final Map<String, Object> params) {
        return getSelectForCloudIdsByOuConfig(company, ouConfig, params, Set.of());
    }

    /**
     * Returns a query to select the users that belong to the OU configuration provided.
     *
     * @param company
     * @param ouConfig
     * @param params   the params map to be used for the variables, if any, resulting in this query
     * @param type     the integration type to be used to generate the users select. If null or empty only the query for the default section, if any, will be return.
     * @return Query containing id, display_name, cloud_id and integration_id from the integraton_users table
     */
    public static String getSelectForCloudIdsByOuConfig(final String company, final OUConfiguration ouConfig, final Map<String, Object> params, final IntegrationType type) {
        return getSelectForCloudIdsByOuConfig(company, ouConfig, params, Set.of(type));
    }

    /**
     * Returns a query to select the users that belong to the OU configuration provided.
     *
     * @param company
     * @param ouConfig
     * @param params   the params map to be used for the variables, if any, resulting in this query
     * @param types    the integration types to be used to generate the users select. If null or empty only the query for the default section, if any, will be return.
     * @return Query containing id, display_name, cloud_id and integration_id from the integraton_users table
     */
    public static String getSelectForCloudIdsByOuConfig(final String company, final OUConfiguration ouConfig, final Map<String, Object> params, final Set<IntegrationType> types) {
        if (ouConfig == null || ouConfig.getOuRefId() == null || (!ouConfig.getStaticUsers() && !ouConfig.getDynamicUsers())) {
            return null;
        }

        // default section ids
        // the default section is only used if there is no section that matches the type requested
        // in this case, we will use all the ids from the original request minus the ids for the integration types that 
        // do have a definition in the OU
        var hasTypes = CollectionUtils.isNotEmpty(types);
        var defaultSectionIntegrationIds = new ArrayList<Integer>();
        Optional<List<Integer>> requestIntegrationIds = ouConfig.getRequest() == null ? Optional.empty() : ouConfig.getRequest().getFilterValueAsList("integration_ids")
                .map(l -> l.stream()
                        .map(i -> i instanceof String ? Integer.valueOf((String) i) : (Integer) i)
                        .collect(Collectors.toList())
                );
        if (ouConfig.getRequest() != null && requestIntegrationIds.isPresent() && CollectionUtils.isNotEmpty(requestIntegrationIds.get())) {
            defaultSectionIntegrationIds.addAll(requestIntegrationIds.get());
        }
        // if no integration types passed, use all the ids
        // if integration type passed then remove the integration ids for the types that are defined in the sections, if any
        if (hasTypes) {
            for (var s : ouConfig.getSections()) {
                if (s.getIntegrationType() != null && types.contains(s.getIntegrationType())) {
                    defaultSectionIntegrationIds.remove(s.getIntegrationId());
                }
            }
            ;
        }
        var unions = new ArrayList<String>();
        // if there are no integration ids, we will get all the cloud ids for the users
        // bind to integration ids if there are sections with integrations that match the requested types or if there are
        // integrations for default sections
        var integrationBind = (CollectionUtils.isNotEmpty(ouConfig.getIntegrationIds()) && hasTypes) || CollectionUtils.isNotEmpty(defaultSectionIntegrationIds)
                ? "    AND iu.integration_id IN (:ou_user_selection_integration_ids_{1}) \n"
                : "";
        Function<DBOrgContentSection, String> integrationIdCondition = s -> (s.getIntegrationId() != null || CollectionUtils.isNotEmpty(defaultSectionIntegrationIds) ? integrationBind : "");
        // get static users query
        var counter = new AtomicInteger(0);
        // if it is default section and there are no types defined or if it is not the default section and types are defined and the
        // integration type of the section matches any of the types requested
        Predicate<DBOrgContentSection> regularSectionFilter = s -> (s.getDefaultSection() && !hasTypes)
                || (!s.getDefaultSection() && hasTypes && s.getIntegrationType() != null && types.contains(s.getIntegrationType()));
        if (ouConfig.getStaticUsers()) {
            Consumer<DBOrgContentSection> staticUnionFunction = s -> {
                // integration id in the section can be null if this is the default section
                // var integrationIds = s.getIntegrationId() != null ? List.of(s.getIntegrationId()) : defaultIntegrations;
                var count = counter.incrementAndGet();
                var staticUsersSelect = MessageFormat.format("SELECT iu.id, iu.display_name, iu.cloud_id, iu.integration_id \n"
                                + "FROM {0}.integration_users iu, {0}.ou_content_sections o_s, {0}.org_users o_u, {0}.org_user_cloud_id_mapping o_m \n"
                                + "WHERE \n"
                                + "    o_u.versions @> ARRAY(SELECT version FROM {0}.org_version_counter WHERE type = :org_user_selection_version_type AND active = true) \n"
                                + "    AND o_u.ref_id = ANY(o_s.user_ref_ids) \n"
                                + "    AND o_m.org_user_id = o_u.id \n"
                                + "    AND o_m.integration_user_id = iu.id \n"
                                + integrationIdCondition.apply(s)
                                + "    AND o_s.id = :ou_user_selection_section_id_{1} \n",
                        company,
                        count);
                unions.add(staticUsersSelect);
                params.put("org_user_selection_version_type", "USER");
                params.put("ou_user_selection_integration_ids_" + count, s.getIntegrationId() != null ? s.getIntegrationId() : defaultSectionIntegrationIds);
                params.put("ou_user_selection_section_id_" + count, s.getId());
            };
            // all sections
            ouConfig.getSections().stream()
                    .filter(s -> CollectionUtils.isNotEmpty(s.getUsers()))
                    // if the section is the default section and no types are specified
                    // if the section matches the types
                    .filter(regularSectionFilter)
                    .forEach(staticUnionFunction);
            // if no other section matches the types or no types were defined, check for the default section
            if (unions.size() < 1) {
                ouConfig.getSections().stream()
                        // if the section is the default section
                        .filter(DBOrgContentSection::getDefaultSection)
                        .filter(s -> CollectionUtils.isNotEmpty(s.getUsers()))
                        .forEach(staticUnionFunction);
            }
        }
        // get dynamic select active version
        if (ouConfig.getDynamicUsers()) {
            Consumer<DBOrgContentSection> dynamicUnionFunction = s -> {
                // integration id in the section can be null if this is the default section
                var integrationIds = s.getIntegrationId() != null ? List.of(s.getIntegrationId()) : defaultSectionIntegrationIds;
                try {
                    var orgUserSelectionCondition = OrgUsersHelper.getOrgUsersSelectConditions(company, s.getDynamicUsers(), params);
                    var count = counter.incrementAndGet();
                    var union = MessageFormat.format("SELECT iu.id, iu.display_name, iu.cloud_id, iu.integration_id \n"
                                    + "FROM {0}.integration_users iu, {0}.ou_content_sections o_s, {0}.org_users o_u, {0}.org_user_cloud_id_mapping o_m \n"
                                    + "WHERE \n"
                                    + "    o_m.integration_user_id = iu.id \n"
                                    + "    AND o_m.org_user_id = o_u.id"
                                    + "  AND " + String.join(" AND ", orgUserSelectionCondition) + " \n"
                                    + integrationIdCondition.apply(s),
                            company,
                            count
                    );
                    params.put("ou_user_selection_integration_ids_" + count, integrationIds);
                    log.debug("[{}] union: {}", company, union);
                    unions.add(union);
                } catch (JsonProcessingException e) {
                    log.error("[{}] Unable to generate union statement for the users select query for the ou 'id={}, ref_id={}' section: {}", company, ouConfig.getOuId(), ouConfig.getOuRefId(), s, e);
                }
                // FilterConditionParser.parseCondition();
            };
            ouConfig.getSections().stream()
                    .filter(s -> MapUtils.isNotEmpty(s.getDynamicUsers()))
                    // if the section is the default section and no types are specified
                    // if the section matches the types
                    .filter(regularSectionFilter)
                    .forEach(dynamicUnionFunction);

            // if no other section matches the types or no types were defined, check for the default section
            if (unions.size() < 1) {
                ouConfig.getSections().stream()
                        // if the section is the default section
                        .filter(DBOrgContentSection::getDefaultSection)
                        .filter(s -> MapUtils.isNotEmpty(s.getDynamicUsers()))
                        .forEach(dynamicUnionFunction);
            }
        }
        return CollectionUtils.isNotEmpty(unions) ? String.join("UNION\n", unions) + " GROUP BY iu.id, iu.display_name, iu.cloud_id, iu.integration_id" : "";
    }

    public static boolean isOuConfigActive(OUConfiguration ouConfig) {
        return ouConfig != null && ouConfig.getOuId() != null;
    }

    public static boolean doesOUConfigHavePRApprover(OUConfiguration ouConfig) {
        return isOuConfigActive(ouConfig) && ouConfig.hasUsersSelection() && ouConfig.getScmFields().contains("approver")
                && !ouConfig.getOuExclusions().contains("approvers");
    }

    public static boolean doesOUConfigHaveSonarQubeAuthor(OUConfiguration ouConfig) {
        return isOuConfigActive(ouConfig) && ouConfig.hasUsersSelection() && ouConfig.getSonarqubeFields().contains("author")
                && !ouConfig.getOuExclusions().contains("authors");
    }

    public static boolean doesOUConfigHavePRCreator(OUConfiguration ouConfig) {
        return isOuConfigActive(ouConfig) && ouConfig.hasUsersSelection() && ouConfig.getScmFields().contains("author")
                && !ouConfig.getOuExclusions().contains("creators");
    }

    public static boolean doesOUConfigHavePRAssignee(OUConfiguration ouConfig) {
        return isOuConfigActive(ouConfig) && ouConfig.hasUsersSelection() && ouConfig.getScmFields().contains("assignee")
                && !ouConfig.getOuExclusions().contains("assignees");
    }

    public static boolean doesOUConfigHavePRReviewer(OUConfiguration ouConfig) {
        return isOuConfigActive(ouConfig) && ouConfig.hasUsersSelection() && ouConfig.getScmFields().contains("reviewer")
                && !ouConfig.getOuExclusions().contains("reviewers");
    }

    public static boolean doesOUConfigHaveJiraReporters(OUConfiguration ouConfig) {
        return isOuConfigActive(ouConfig) && ouConfig.hasUsersSelection() && ouConfig.getJiraFields().contains("reporter")
                && !ouConfig.getOuExclusions().contains("reporters");
    }

    public static boolean doesOUConfigHaveJiraAssignees(OUConfiguration ouConfig) {
        return isOuConfigActive(ouConfig) && ouConfig.hasUsersSelection() && ouConfig.getJiraFields().contains("assignee")
                && !ouConfig.getOuExclusions().contains("assignees");
    }

    public static boolean doesOUConfigHaveJiraFirstAssignees(OUConfiguration ouConfig) {
        return isOuConfigActive(ouConfig) && ouConfig.hasUsersSelection() && ouConfig.getJiraFields().contains("first_assignee")
                && !ouConfig.getOuExclusions().contains("first_assignees");
    }

    public static boolean doesOUConfigHaveWorkItemAssignees(OUConfiguration ouConfig) {
        return isOuConfigActive(ouConfig) && ouConfig.hasUsersSelection() && ouConfig.getAdoFields().contains("assignee")
                && !ouConfig.getOuExclusions().contains("workitem_assignees");
    }

    public static boolean doesOUConfigHaveWorkItemReporters(OUConfiguration ouConfig) {
        return isOuConfigActive(ouConfig) && ouConfig.hasUsersSelection() && ouConfig.getAdoFields().contains("reporter")
                && !ouConfig.getOuExclusions().contains("workitem_reporters");
    }

    public static boolean doesOUConfigHaveWorkItemFirstAssignees(OUConfiguration ouConfig) {
        return isOuConfigActive(ouConfig) && ouConfig.hasUsersSelection() && ouConfig.getAdoFields().contains("first_assignee")
                && !ouConfig.getOuExclusions().contains("workitem_first_assignees");
    }

    public static boolean doesOuConfigHaveCommitCommitters(OUConfiguration ouConfig) {
        return isOuConfigActive(ouConfig) && ouConfig.hasUsersSelection()
                && ouConfig.getScmFields().contains("committer") && !ouConfig.getOuExclusions().contains("committers");
    }

    public static boolean doesOuConfigHaveCommitAuthors(OUConfiguration ouConfig) {
        return isOuConfigActive(ouConfig) && ouConfig.hasUsersSelection()
                && ouConfig.getScmFields().contains("author") && !ouConfig.getOuExclusions().contains("authors");
    }

    public static boolean doesOuConfigHaveIssueCreators(OUConfiguration ouConfig) {
        return isOuConfigActive(ouConfig) && ouConfig.hasUsersSelection() && ouConfig.getScmFields().contains("author") &&
                !ouConfig.getOuExclusions().contains("creators");
    }

    public static boolean doesOuConfigHaveIssueAssignees(OUConfiguration ouConfig) {
        return isOuConfigActive(ouConfig) && ouConfig.hasUsersSelection() && ouConfig.getScmFields().contains("assignee")
                && !ouConfig.getOuExclusions().contains("assignees");
    }

    public static boolean doesOuConfigHaveReposAndCommittersAuthors(OUConfiguration ouConfig) {
        return OrgUnitHelper.isOuConfigActive(ouConfig) && ouConfig.hasUsersSelection() && ouConfig.getScmFields().contains("author")
                && !ouConfig.getOuExclusions().contains("authors");
    }

    public static boolean doesOuConfigHaveReposAndCommittersCreators(OUConfiguration ouConfig) {
        return OrgUnitHelper.isOuConfigActive(ouConfig) && ouConfig.hasUsersSelection() && ouConfig.getScmFields().contains("committer")
                && !ouConfig.getOuExclusions().contains("committers");
    }

    public static boolean doesOuConfigHaveCiCdUsers(OUConfiguration ouConfig) {
        return isOuConfigActive(ouConfig) && ouConfig.hasUsersSelection() && ouConfig.getCicdFields().contains("user")
                && !ouConfig.getOuExclusions().contains("cicd_user_ids");
    }

    public static boolean doesOuConfigHaveCiCdAuthors(OUConfiguration ouConfig) {
        return isOuConfigActive(ouConfig) && ouConfig.hasUsersSelection() && ouConfig.getCicdFields().contains("author")
                && !ouConfig.getOuExclusions().contains("authors");
    }

    public static boolean doesOuConfigHavePagerDutyUsers(OUConfiguration ouConfig) {
        return OrgUnitHelper.isOuConfigActive(ouConfig) && ouConfig.hasUsersSelection() && ouConfig.getPagerDutyFields().contains("user_id")
                && !ouConfig.getOuExclusions().contains("user_ids");
    }

    public static boolean doesOuConfigHaveGithubProjectCreators(OUConfiguration ouConfig) {
        return isOuConfigActive(ouConfig) && ouConfig.hasUsersSelection() && ouConfig.getScmFields().contains("project_creator")
                && !ouConfig.getOuExclusions().contains("project_creators");
    }

    public static boolean doesOuConfigHaveGithubCardCreators(OUConfiguration ouConfig) {
        return isOuConfigActive(ouConfig) && ouConfig.hasUsersSelection() && ouConfig.getScmFields().contains("card_creator")
                && !ouConfig.getOuExclusions().contains("card_creators");
    }

    public static boolean doesOuConfigHaveGithubAssignees(OUConfiguration ouConfig) {
        return isOuConfigActive(ouConfig) && ouConfig.hasUsersSelection() && ouConfig.getScmFields().contains("author")
                && !ouConfig.getOuExclusions().contains("assignees");
    }

    public static OUConfiguration newOUConfigForStacks(OUConfiguration ouConfig, String ouExclusion) {
        if (ouConfig == null) {
            return null;
        }
        List<String> ouExclusions = new ArrayList<>(ouConfig.getOuExclusions());
        ouExclusions.add(ouExclusion);
        return ouConfig.toBuilder().ouExclusions(ouExclusions).build();
    }

}
