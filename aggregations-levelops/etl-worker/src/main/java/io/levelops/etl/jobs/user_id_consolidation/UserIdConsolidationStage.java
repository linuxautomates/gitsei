package io.levelops.etl.jobs.user_id_consolidation;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.aggregations_shared.database.JobDefinitionDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.aggregations_shared.database.models.DbJobDefinitionUpdate;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.TenantConfig;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.models.database.organization.DBOrgUserCloudIdMapping;
import io.levelops.commons.databases.models.database.organization.OrgVersion;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.UserIdentitiesFilter;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.TenantConfigService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.helper.organization.OrgUsersHelper;
import io.levelops.commons.helper.organization.OrgUsersLockService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.etl.job_framework.GenericJobProcessingStage;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.levelops.etl.jobs.user_id_consolidation.UserConsolidationGraphUtils.createGraphs;
import static io.levelops.etl.jobs.user_id_consolidation.UserConsolidationGraphUtils.getConnectedComponents;

@Service
@Log4j2
public class UserIdConsolidationStage implements GenericJobProcessingStage<UserIdConsolidationState> {
    private final String ENABLED_KEY = "AUTO_USER_ID_CONSOLIDATION_ENABLED";
    private final String LAST_AUTO_VERSION_METADATA_KEY = "LAST_AUTO_VERSION";
    private final UserIdentityService userIdentityService;
    private final OrgUsersDatabaseService orgUsersDatabaseService;
    private final OrgVersionsDatabaseService orgVersionsDatabaseService;
    private final IntegrationService integrationService;
    private final OrgUsersHelper orgUsersHelper;
    private final TenantConfigService tenantConfigService;
    private final JobDefinitionDatabaseService jobDefinitionDatabaseService;
    private final OrgUsersLockService orgUsersLockService;

    public UserIdConsolidationStage(
            UserIdentityService userIdentityService,
            OrgUsersDatabaseService orgUsersDatabaseService,
            OrgVersionsDatabaseService orgVersionsDatabaseService,
            IntegrationService integrationService,
            OrgUsersHelper orgUsersHelper,
            TenantConfigService tenantConfigService,
            JobDefinitionDatabaseService jobDefinitionDatabaseService,
            OrgUsersLockService orgUsersLockService
    ) {
        this.userIdentityService = userIdentityService;
        this.orgUsersDatabaseService = orgUsersDatabaseService;
        this.orgVersionsDatabaseService = orgVersionsDatabaseService;
        this.integrationService = integrationService;
        this.orgUsersHelper = orgUsersHelper;
        this.tenantConfigService = tenantConfigService;
        this.jobDefinitionDatabaseService = jobDefinitionDatabaseService;
        this.orgUsersLockService = orgUsersLockService;

    }

    @Override
    public void process(JobContext context, UserIdConsolidationState jobState) {
        if (!isJobEnabled(context.getTenantId())) {
            log.info("User ID Consolidation is disabled for tenant: {}", context.getTenantId());
            return;
        }

        // Used to determine if there have been any user driven changes while this job is running
        Optional<OrgVersion> latestUserVersion = getLatestVersion(context.getTenantId());
        log.info("Latest user version at start of job: {}", latestUserVersion.map(OrgVersion::getVersion).orElse(null));

        Map<String, DbScmUser> allIntegrationUsersWithEmails = userIdentityService
                .stream(context.getTenantId(), UserIdentitiesFilter.builder()
                        .emptyEmails(false)
                        .build())
                .filter(user -> user.getMappingStatus() == null || user.getMappingStatus() == DbScmUser.MappingStatus.AUTO)
                .collect(Collectors.toMap(DbScmUser::getId, user -> user));

        log.info("Found {} integration users with emails to consolidate", allIntegrationUsersWithEmails.size());
        Map<String, List<String>> emailToUserIds = new HashMap<>();
        Map<String, List<String>> emailGraph = new HashMap<>();
        createGraphs(allIntegrationUsersWithEmails.values(), emailToUserIds, emailGraph);
        List<Set<String>> connectedEmailGroups = getConnectedComponents(emailGraph);
        List<Set<DbScmUser>> connectedUserGroups = connectedEmailGroups.stream()
                .map(emails -> emails.stream()
                        .map(emailToUserIds::get)
                        .flatMap(List::stream)
                        .map(allIntegrationUsersWithEmails::get)
                        .collect(Collectors.toSet())
                )
                .collect(Collectors.toList());

        log.info("Found {} connected user groups", connectedUserGroups.size());

        List<DBOrgUser> orgUsersToUpdate = getOrgUsersToUpdate(context.getTenantId(), connectedUserGroups, latestUserVersion);

        log.info("Found {} org users to update", orgUsersToUpdate.size());
        try {
            updateUsers(context, orgUsersToUpdate, latestUserVersion);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected void updateUsers(JobContext context, List<DBOrgUser> orgUsersToUpdate, Optional<OrgVersion> startingLatestVersion) throws SQLException {
        if (orgUsersToUpdate.isEmpty()) {
            log.info("No users to update");
            return;
        }
        String company = context.getTenantId();
        Optional<Integer> lastRecordedAutoVersion = getLastAutoVersion(context.getJobInstanceId().getJobDefinitionId());
        Optional<OrgVersion> activeVersion = getActiveVersion(company);
        boolean shouldCreateNewVersion =
                lastRecordedAutoVersion.isEmpty() ||
                        activeVersion.isEmpty() ||
                        activeVersion.get().getVersion() != lastRecordedAutoVersion.get();

        throwIfLatestUserVersionHasChanged(startingLatestVersion, company);
        UUID versionId = null;
        OrgVersion newVersion = null;

        // Deduplicate the versions - if the previous version was also created by this job then we won't increment
        // the user version number
        if (shouldCreateNewVersion) {
            versionId = this.orgVersionsDatabaseService.insert(company, OrgVersion.OrgAssetType.USER);
            newVersion = this.orgVersionsDatabaseService.get(company, versionId).orElseThrow();
            log.info("Creating new version for user consolidation job. Old version: {}, New version: {}",
                    activeVersion.map(OrgVersion::getVersion).orElse(0), newVersion.getVersion());
        }

        HashSet<Integer> excludedRefIds = new HashSet<>();

        OrgVersion finalNewVersion = shouldCreateNewVersion ? newVersion : activeVersion.get();
        OrgVersion finalLatestVersion = shouldCreateNewVersion ? newVersion : startingLatestVersion.get();
        orgUsersToUpdate.forEach(orgUser -> {
            try {
                throwIfLatestUserVersionHasChanged(Optional.of(finalLatestVersion), company);
                excludedRefIds.add(orgUsersDatabaseService.upsertAuto(company, orgUser, Set.of(finalNewVersion.getVersion())).getRefId());
            } catch (SQLException e) {
                log.error("Failed to update org user: {}", orgUser, e);
            }
        });

        if (!orgUsersLockService.lock(company, 60)) {
            throw new RuntimeException("Failed to acquire lock for user consolidation job");
        }

        try {
            if (shouldCreateNewVersion) {
                orgUsersDatabaseService.upgradeUsersVersion(company, versionId, excludedRefIds);
                orgUsersHelper.activateVersion(company, versionId);
                setLastAutoVersion(context.getJobInstanceId().getJobDefinitionId(), newVersion.getVersion());
            }
        } finally {
            orgUsersLockService.unlock(company);
        }

    }

    private Optional<Integer> getLastAutoVersion(UUID jobDefinitionId) {
        return jobDefinitionDatabaseService.get(jobDefinitionId)
                .map(jobDefinition -> {
                    var metadata = jobDefinition.getMetadata();
                    Integer version = null;
                    if (metadata.containsKey(LAST_AUTO_VERSION_METADATA_KEY)) {
                        version = (Integer) metadata.get(LAST_AUTO_VERSION_METADATA_KEY);
                    }
                    return Optional.ofNullable(version);
                }).orElse(Optional.empty());
    }

    private void setLastAutoVersion(UUID jobDefinitionId, int version) {
        DbJobDefinition jobDefinition = jobDefinitionDatabaseService.get(jobDefinitionId).orElseThrow();
        var metadata = jobDefinition.getMetadata();
        metadata.put(LAST_AUTO_VERSION_METADATA_KEY, version);
        try {
            jobDefinitionDatabaseService.update(DbJobDefinitionUpdate.builder()
                    .whereClause(DbJobDefinitionUpdate.WhereClause.builder()
                            .id(jobDefinitionId)
                            .build())
                    .metadata(metadata)
                    .build());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the org users to update based on the connected user groups.
     *
     * @param connectedUserGroups   Groups of connected integration users, these are the integration users that need to be
     *                              mapped to the same org user
     * @param startingLatestVersion
     * @return List of Org Users to update. These OrgUsers have the correct mappings to the integration users.
     */
    protected List<DBOrgUser> getOrgUsersToUpdate(
            String company,
            List<Set<DbScmUser>> connectedUserGroups,
            Optional<OrgVersion> startingLatestVersion) {
        Optional<OrgVersion> version = getActiveVersion(company);
        Map<UUID, DBOrgUser> finalOrgUsersToUpdate = new HashMap<>();
        Set<UUID> processedOrgUserIds = new HashSet<>();
        connectedUserGroups
                .stream()
                .forEach(userGroup -> {
                    throwIfLatestUserVersionHasChanged(startingLatestVersion, company);
                    processMapping(
                            company,
                            version.isPresent() ? getCurrentMappings(userGroup, company, version.get()) : List.of(),
                            userGroup,
                            finalOrgUsersToUpdate,
                            processedOrgUserIds);
                });
        return new ArrayList<>(finalOrgUsersToUpdate.values());
    }

    /**
     * Process the mappings for a single group of integration users.
     * 1. Checks if the entire group is already mapped to the same org user - if so, skip - nothing to do
     * 2. If no org user is mapped - then we create the org user and map all the integration users to it
     * 3. If there are multiple org users mapped then we find the org user with most mappings and map
     * all the integration users to this org user
     * 4. If there are multiple org users associated with a user group it also removes the mapping from the rest of the
     * org users
     */
    private void processMapping(
            String company,
            List<UserMappingWithOrgUser> currentMappings,
            Set<DbScmUser> userGroup,
            Map<UUID, DBOrgUser> finalOrgUsersToUpdate,
            Set<UUID> processedOrgUsers) {
        Map<String, Integration> integrationsById = getAllIntegrations(company);
        Set<DBOrgUser> mappedOrgUsers = currentMappings.stream()
                .map(UserMappingWithOrgUser::getOrgUser)
                .collect(Collectors.toSet());

        // Check if the group is already mapped to the same org user - if so, skip - nothing to do
        if (mappedOrgUsers.size() == 1 && currentMappings.size() == userGroup.size() &&
                !processedOrgUsers.contains(mappedOrgUsers.stream().findFirst().get().getId())) {
            processedOrgUsers.add(mappedOrgUsers.stream().findFirst().get().getId());
            return;
        }

        // Find the org user that has the most integration users mapped to it
        Map<DBOrgUser, Integer> orgUserByCount = currentMappings
                .stream()
                .map(UserMappingWithOrgUser::getOrgUser)
                .collect(Collectors.toMap(user -> user, user -> 1, Integer::sum));

        Optional<DBOrgUser> orgUserToMap = orgUserByCount.entrySet()
                .stream()
                // This ensures that we don't overwrite a mapping that was already processed
                .filter(entry -> !processedOrgUsers.contains(entry.getKey().getId()))
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);

        DBOrgUser finalOrgUserToMap;
        // If no valid org user is mapped - then we create the org user and map all the integration users to it
        if (orgUserToMap.isEmpty()) {
            finalOrgUserToMap = DBOrgUser.builder()
                    .id(UUID.randomUUID()) // This id will be replaced, but we still set it so we can add it to the map
                    .email(getMostCommonEmail(userGroup))
                    .fullName(getMostCommonName(userGroup))
                    .ids(getLoginIds(userGroup, integrationsById))
                    .active(true)
                    .build();
        } else {
            if (finalOrgUsersToUpdate.containsKey(orgUserToMap.get().getId())) {
                finalOrgUserToMap = finalOrgUsersToUpdate.get(orgUserToMap.get().getId());
            } else {
                finalOrgUserToMap = orgUserToMap.get();
            }
        }
        finalOrgUserToMap.getIds().addAll(getLoginIds(userGroup, integrationsById));
        finalOrgUsersToUpdate.put(finalOrgUserToMap.getId(), finalOrgUserToMap);
        processedOrgUsers.add(finalOrgUserToMap.getId());

        // Remove the mapping from all other org users that were mapped previously, if any
        mappedOrgUsers.stream()
                .filter(orgUser -> !orgUser.getId().equals(finalOrgUserToMap.getId()))
                .forEach(orgUser -> {
                    DBOrgUser orgUserToRemoveMappingFrom;
                    orgUserToRemoveMappingFrom = finalOrgUsersToUpdate.getOrDefault(orgUser.getId(), orgUser);
                    orgUserToRemoveMappingFrom.getIds().removeAll(getLoginIds(userGroup, integrationsById));
                    finalOrgUsersToUpdate.put(orgUserToRemoveMappingFrom.getId(), orgUserToRemoveMappingFrom);
                });
    }

    private boolean hasLatestUserVersionChanged(Optional<OrgVersion> startingLatestVersion, String company) {
        Optional<OrgVersion> latestVersion = getLatestVersion(company);
        if (startingLatestVersion.isEmpty() && latestVersion.isEmpty()) {
            return false;
        }
        if (startingLatestVersion.isEmpty() || latestVersion.isEmpty()) {
            return true;
        }
        return !startingLatestVersion.get().getId().equals(latestVersion.get().getId());
    }

    private void throwIfLatestUserVersionHasChanged(Optional<OrgVersion> startingLatestVersion, String company) {
        if (hasLatestUserVersionChanged(startingLatestVersion, company)) {
            log.error("Latest user version has changed, bailing out of job");
            throw new RuntimeException("Latest user version has changed, bailing out of job");
        }
    }

    private Optional<OrgVersion> getActiveVersion(String company) {
        try {
            return orgVersionsDatabaseService.getActive(company, OrgVersion.OrgAssetType.USER);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<OrgVersion> getLatestVersion(String company) {
        try {
            return orgVersionsDatabaseService.getLatest(company, OrgVersion.OrgAssetType.USER);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Integration> getAllIntegrations(String company) {
        try {
            return integrationService.stream(company, null, false, List.of(), null, null, null, null, null, null)
                    .collect(Collectors.toMap(Integration::getId, Function.identity()));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<DBOrgUser.LoginId> getLoginIds(Set<DbScmUser> userGroup, Map<String, Integration> integrationsById) {
        return userGroup.stream().map(user -> DBOrgUser.LoginId.builder()
                        .cloudId(user.getCloudId())
                        .integrationId(Integer.parseInt(user.getIntegrationId()))
                        .username(user.getDisplayName())
                        .integrationType(integrationsById.get(user.getIntegrationId()).getApplication())
                        .build())
                .collect(Collectors.toSet());
    }

    private String getMostCommonName(Set<DbScmUser> userGroup) {
        return getMostCommon(userGroup, user -> List.of(user.getDisplayName()));
    }

    private String getMostCommonEmail(Set<DbScmUser> userGroup) {
        return getMostCommon(userGroup, DbScmUser::getEmails);
    }

    private String getMostCommon(Set<DbScmUser> userGroup, Function<DbScmUser, List<String>> mapper) {
        return userGroup.stream()
                .map(mapper)
                .flatMap(List::stream)
                .collect(Collectors.toMap(email -> email, email -> 1, Integer::sum))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new RuntimeException("No email found for user group " + userGroup));
    }

    /**
     * Gets the current mappings for the integration user group. Current mappings refers to mappings with org users
     * with the current version.
     */
    private List<UserMappingWithOrgUser> getCurrentMappings(Set<DbScmUser> connectedUserGroup, String company, OrgVersion currentVersion) {
        List<DBOrgUserCloudIdMapping> allMappings = orgUsersDatabaseService.streamOrgUserCloudIdMappings(
                company,
                OrgUsersDatabaseService.OrgUserCloudIdMappingFilter.builder()
                        .integrationUserIds(connectedUserGroup.stream()
                                .map(DbScmUser::getId)
                                .map(UUID::fromString)
                                .collect(Collectors.toList()))
                        .build()
        ).toList();

        Set<UUID> orgUserIds = allMappings.stream()
                .map(DBOrgUserCloudIdMapping::getOrgUserId)
                .collect(Collectors.toSet());
        var orgUsersById = orgUsersDatabaseService.stream(
                        company,
                        QueryFilter.builder()
                                .strictMatch("org_user_id", orgUserIds)
                                .build(),
                        100)
                .filter(orgUser -> orgUser.getVersions().contains(currentVersion.getVersion()))
                .collect(Collectors.toMap(DBOrgUser::getId, orgUser -> orgUser));
        List<UserMappingWithOrgUser> currentMappings = allMappings.stream()
                .filter(mapping -> orgUsersById.containsKey(mapping.getOrgUserId()))
                .map(mapping -> new UserMappingWithOrgUser(mapping, orgUsersById.get(mapping.getOrgUserId())))
                .collect(Collectors.toList());
        return currentMappings;
    }

    private boolean isJobEnabled(String company) {
        try {
            DbListResponse<TenantConfig> configs = tenantConfigService.listByFilter(company, ENABLED_KEY, 0, 1);
            if (configs.getCount() > 0) {
                return Boolean.parseBoolean(configs.getRecords().get(0).getValue());
            } else {
                // Switched off by default
                return false;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Value
    public static class UserMappingWithOrgUser {
        DBOrgUserCloudIdMapping cloudIdMapping;
        DBOrgUser orgUser;
    }

    @Override
    public String getName() {
        return "UserIdConsolidationStage";
    }

    @Override
    public void preStage(JobContext context, UserIdConsolidationState jobState) throws SQLException {

    }

    @Override
    public void postStage(JobContext context, UserIdConsolidationState jobState) throws SQLException {

    }

    @Override
    public boolean allowFailure() {
        return false;
    }
}
