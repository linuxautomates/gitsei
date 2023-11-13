package io.levelops.commons.services.velocity_productivity.services;

import io.levelops.commons.databases.models.database.velocity.OrgProfile;
import io.levelops.commons.databases.models.database.velocity.VelocityConfig;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.DefaultListRequestUtils;
import io.levelops.commons.databases.services.velocity.OrgProfileDatabaseService;
import io.levelops.commons.databases.services.velocity.VelocityConfigsDatabaseService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.ingestion.models.IntegrationFamilyType;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.exceptions.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Service
public class VelocityConfigsService {
    private static final String INTEGRATION_TYPE_SCM = "SCM";
    private static final String INTEGRATION_TYPE_IM = "IM";
    private static final String INTEGRATION_TYPE_CICD = "CICD";

    private static final Long LOWER_LIMIT = TimeUnit.DAYS.toSeconds(10);
    private static final Long UPPER_LIMIT = TimeUnit.DAYS.toSeconds(30);
    private static final EnumSet<IntegrationType> ISSUE_MANAGEMENT_INTEGRATION_TYPES = IntegrationType.getIssueManagementIntegrationTypes();

    private final VelocityConfigsDatabaseService velocityConfigsDatabaseService;

    private final OrgProfileDatabaseService orgProfileDatabaseService;

    @Autowired
    public VelocityConfigsService(VelocityConfigsDatabaseService velocityConfigsDatabaseService, OrgProfileDatabaseService orgProfileDatabaseService) {
        this.velocityConfigsDatabaseService = velocityConfigsDatabaseService;
        this.orgProfileDatabaseService = orgProfileDatabaseService;
    }

    public void validateDuplicateCICDJobsByProfile(List<UUID> uuidList) throws BadRequestException {
        Set<UUID> set = uuidList.stream().collect(Collectors.toSet());
        if (set.size() < uuidList.size()) {
            throw new BadRequestException("A default config already exists");
        }
    }

    private List<UUID> getCiCdJobIdsForDeploymentFrequencyOrChangeFailureRate(VelocityConfigDTO.Event events) {
        List<UUID> cicdJobIds = null;
        if (events.getType().equals(VelocityConfigDTO.EventType.CICD_JOB_RUN)) {
            cicdJobIds = events.getValues().stream().map(UUID::fromString)
                    .collect(Collectors.toList());
        }
        return cicdJobIds;
    }

    private List<UUID> getCiCdJobIds(List<VelocityConfigDTO.Stage> stages) throws BadRequestException {
        List<UUID> cicdJobIds = stages.stream()
                .map(s -> s.getEvent())
                .filter(Objects::nonNull)
                .filter(e -> VelocityConfigDTO.EventType.CICD_JOB_RUN.equals(e.getType()))
                .map(e -> e.getValues())
                .flatMap(Collection::stream)
                .map(UUID::fromString)
                .collect(Collectors.toList());
        validateDuplicateCICDJobsByProfile(cicdJobIds);
        return cicdJobIds;
    }

    private VelocityConfig createVelocityConfig(final VelocityConfigDTO configDto, List<UUID> cicdJobIds) {
        Set<UUID> set = new HashSet<>(cicdJobIds);
        cicdJobIds.clear();
        cicdJobIds.addAll(set);
        VelocityConfig.VelocityConfigBuilder bldr = VelocityConfig.builder()
                .name(configDto.getName())
                .defaultConfig(Boolean.TRUE.equals(configDto.getDefaultConfig()))
                .config(configDto)
                .cicdJobIds(cicdJobIds)
                .isNew(configDto.getIsNew());
        if (configDto.getId() != null) {
            bldr.id(configDto.getId());
        }
        VelocityConfig velocityConfig = bldr.build();
        return velocityConfig;
    }

    private List<VelocityConfigDTO.Stage> validateVelocityConfigDTO(final VelocityConfigDTO configDto) throws BadRequestException {
        List<VelocityConfigDTO.Stage> stages = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(configDto.getPreDevelopmentCustomStages())) {
            stages.addAll(configDto.getPreDevelopmentCustomStages());
        }
        if (CollectionUtils.isNotEmpty(configDto.getFixedStages())) {
            stages.addAll(configDto.getFixedStages());
        }
        if (CollectionUtils.isNotEmpty(configDto.getPostDevelopmentCustomStages())) {
            stages.addAll(configDto.getPostDevelopmentCustomStages());
        }

        Set<String> stageNames = new HashSet<>();
        Set<String> duplicateStageNames = stages.stream()
                .map(VelocityConfigDTO.Stage::getName)
                .filter(n -> !stageNames.add(n))
                .collect(Collectors.toSet());
        if (CollectionUtils.isNotEmpty(duplicateStageNames)) {
            log.debug("duplicateStageNames = {}", duplicateStageNames);
            throw new BadRequestException("Velocity Config contains duplicate stage names " + String.join(",", duplicateStageNames));
        }

        return stages;
    }

    private VelocityConfig validateAndCreateVelocityConfig(final VelocityConfigDTO configDto) throws BadRequestException {
        if (configDto.getIsNew() != null && configDto.getIsNew()) {
            return validateAndCreateNewVelocityConfig(configDto);
        }
        List<VelocityConfigDTO.Stage> stages = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(configDto.getPreDevelopmentCustomStages())) {
            stages.addAll(configDto.getPreDevelopmentCustomStages());
        }
        if (CollectionUtils.isNotEmpty(configDto.getFixedStages())) {
            stages.addAll(configDto.getFixedStages());
        }
        if (CollectionUtils.isNotEmpty(configDto.getPostDevelopmentCustomStages())) {
            stages.addAll(configDto.getPostDevelopmentCustomStages());
        }

        Set<String> stageNames = new HashSet<>();
        Set<String> duplicateStageNames = stages.stream()
                .map(VelocityConfigDTO.Stage::getName)
                .filter(n -> !stageNames.add(n))
                .collect(Collectors.toSet());
        if (CollectionUtils.isNotEmpty(duplicateStageNames)) {
            log.debug("duplicateStageNames = {}", duplicateStageNames);
            throw new BadRequestException("Velocity Config contains duplicate stage names " + String.join(",", duplicateStageNames));
        }

        Set<IntegrationType> invalidIntegrationTypes = CollectionUtils.emptyIfNull(configDto.getIssueManagementIntegrations()).stream()
                .filter(i -> !ISSUE_MANAGEMENT_INTEGRATION_TYPES.contains(i))
                .collect(Collectors.toSet());
        if (CollectionUtils.isNotEmpty(invalidIntegrationTypes)) {
            log.debug("invalidIntegrationTypes = {}", invalidIntegrationTypes);
            throw new BadRequestException("Velocity Config contains invalid issue management integration types " + String.join(",", invalidIntegrationTypes.stream().map(Objects::toString).collect(Collectors.toList())) + ". Supported are: " + String.join(",", ISSUE_MANAGEMENT_INTEGRATION_TYPES.stream().map(Objects::toString).collect(Collectors.toList())));
        }

        List<UUID> cicdJobIds = stages.stream()
                .map(s -> s.getEvent())
                .filter(Objects::nonNull)
                .filter(e -> VelocityConfigDTO.EventType.CICD_JOB_RUN.equals(e.getType()))
                .map(e -> e.getValues())
                .flatMap(Collection::stream)
                .map(UUID::fromString)
                .collect(Collectors.toList());
        VelocityConfig.VelocityConfigBuilder bldr = VelocityConfig.builder()
                .name(configDto.getName())
                .defaultConfig(Boolean.TRUE.equals(configDto.getDefaultConfig()))
                .config(configDto)
                .cicdJobIds(cicdJobIds);
        if (configDto.getId() != null) {
            bldr.id(configDto.getId());
        }
        VelocityConfig velocityConfig = bldr.build();
        return velocityConfig;
    }


    private VelocityConfigDTO convertLeadTimeForChangeTOVelocityConfigDTO(VelocityConfigDTO.LeadTimeForChange leadTimeForChange) {
        VelocityConfigDTO velocityConfigDTONew = VelocityConfigDTO.builder()
                .preDevelopmentCustomStages(Optional.ofNullable(leadTimeForChange.getPreDevelopmentCustomStages())
                        .orElse(new ArrayList<>()))
                .postDevelopmentCustomStages(Optional.ofNullable(leadTimeForChange.getPostDevelopmentCustomStages())
                        .orElse(new ArrayList<>()))
                .fixedStages(leadTimeForChange.getFixedStages()).build();
        return velocityConfigDTONew;
    }

    // This method is created as a part of PROP-1755 jira. Workflow profile introduced with 4 sub type
    // (Lead Time For changes, Deployment Frequency, Mean Time To Restore, Change Failure Rate)
    public VelocityConfig validateAndCreateNewVelocityConfig(final VelocityConfigDTO config) throws BadRequestException {
        VelocityConfigDTO.LeadTimeForChange leadTimeForChange = config.getLeadTimeForChanges();
        VelocityConfigDTO.LeadTimeForChange meanTimeToRestore = config.getMeanTimeToRestore();
        VelocityConfigDTO leadTimeForChangeNew = convertLeadTimeForChangeTOVelocityConfigDTO(leadTimeForChange);
        VelocityConfigDTO meanTimeToRestoreNew = convertLeadTimeForChangeTOVelocityConfigDTO(meanTimeToRestore);
        List<VelocityConfigDTO.Stage> leadTimeStages = validateVelocityConfigDTO(leadTimeForChangeNew);
        List<VelocityConfigDTO.Stage> meanTimeStages = validateVelocityConfigDTO(meanTimeToRestoreNew);
        List<UUID> cicdJobs = getCiCdJobIds(leadTimeStages);
        cicdJobs.addAll(getCiCdJobIds(meanTimeStages));
        getCiCdJobIdsForDeploymentFrequencyOrChangeFailureRate(config, cicdJobs);
        return createVelocityConfig(config, cicdJobs);
    }

    private void getCiCdJobIdsForDeploymentFrequencyOrChangeFailureRate(final VelocityConfigDTO config, List<UUID> cicdJobs) {
        if (config.getDeploymentFrequency() != null
                && config.getDeploymentFrequency().getVelocityConfigFilters() != null
                && config.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency() != null
                && config.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getEvent() != null) {
            cicdJobs.addAll(getCiCdJobIdsForDeploymentFrequencyOrChangeFailureRate(config.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getEvent()));
        }
        if (config.getChangeFailureRate() != null && config.getChangeFailureRate().getVelocityConfigFilters() != null) {
            if (config.getChangeFailureRate().getVelocityConfigFilters().getTotalDeployment() != null
                    && config.getChangeFailureRate().getVelocityConfigFilters().getTotalDeployment().getEvent() != null) {
                cicdJobs.addAll(getCiCdJobIdsForDeploymentFrequencyOrChangeFailureRate(config.getChangeFailureRate().getVelocityConfigFilters().getTotalDeployment().getEvent()));
            }
            if (config.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment() != null
                    && config.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getEvent() != null) {
                cicdJobs.addAll(getCiCdJobIdsForDeploymentFrequencyOrChangeFailureRate(config.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getEvent()));
            }
        }
    }

    public String create(final String company, final VelocityConfigDTO config) throws SQLException, BadRequestException {
        VelocityConfig velocityConfig = validateAndCreateVelocityConfig(config);
        try {
            String configId = velocityConfigsDatabaseService.insert(company, velocityConfig);
            return configId;
        } catch (DuplicateKeyException e) {
            log.error(e.getMessage());
            if (e.getMessage().contains("duplicate key value violates unique constraint")) {
                throw new BadRequestException("A default config already exists");
            } else {
                throw new BadRequestException(e);
            }
        }
    }

    public String update(final String company, final VelocityConfigDTO config) throws SQLException, BadRequestException {
        VelocityConfig velocityConfig = validateAndCreateVelocityConfig(config);
        Boolean updateResult = null;
        try {
            updateResult = velocityConfigsDatabaseService.update(company, velocityConfig);
        } catch (DuplicateKeyException e) {
            log.error(e.getMessage());
            if (e.getMessage().contains("duplicate key value violates unique constraint")) {
                throw new BadRequestException("A default config already exists");
            } else {
                throw new BadRequestException(e);
            }
        }
        if (!Boolean.TRUE.equals(updateResult)) {
            throw new RuntimeException("For customer " + company + " config id " + config.getId().toString() + " failed to update config!");
        }
        return config.getId().toString();
    }

    public void setDefault(final String company, final String id) throws SQLException {
        velocityConfigsDatabaseService.setDefault(company, UUID.fromString(id));
    }

    @Transactional
    public Boolean delete(final String company, final String id) throws SQLException {
        orgProfileDatabaseService.deleteByProfileId(company, UUID.fromString(id));
        return velocityConfigsDatabaseService.delete(company, id);
    }

    private VelocityConfigDTO enrichVelocityConfigDTO(final VelocityConfig velocityConfig) {
        if (velocityConfig == null) {
            return null;
        }
        VelocityConfigDTO enrichedDto = velocityConfig.getConfig();
        VelocityConfigDTO.VelocityConfigDTOBuilder bldr = enrichedDto.toBuilder()
                .id(velocityConfig.getId())
                .defaultConfig(velocityConfig.getDefaultConfig())
                .isNew(velocityConfig.getIsNew())
                .createdAt(velocityConfig.getCreatedAt())
                .updatedAt(velocityConfig.getUpdatedAt());
        if (MapUtils.isNotEmpty(velocityConfig.getCicdJobIdNameMappings())) {
            bldr.cicdJobIdNameMappings(velocityConfig.getCicdJobIdNameMappings());
        }
        EnumSet<IntegrationFamilyType> integrationFamilies = EnumSet.noneOf(IntegrationFamilyType.class);
        Stream.of(
                        CollectionUtils.emptyIfNull(velocityConfig.getConfig().getPreDevelopmentCustomStages()),
                        CollectionUtils.emptyIfNull(velocityConfig.getConfig().getFixedStages()),
                        CollectionUtils.emptyIfNull(velocityConfig.getConfig().getPostDevelopmentCustomStages())
                ).flatMap(x -> x.stream())
                .peek(s -> log.debug("Stage Name = " + s.getName()))
                .map(VelocityConfigDTO.Stage::getEvent)
                .filter(Objects::nonNull)
                .map(VelocityConfigDTO.Event::getType)
                .peek(et -> log.debug("Event Type = {}, Integration families {} ", et.name(), et.integrationFamilies()))
                .filter(Objects::nonNull)
                .forEach(et -> integrationFamilies.addAll(et.integrationFamilies()));
        if (CollectionUtils.isNotEmpty(integrationFamilies)) {
            log.debug("integrationFamilies = {}", integrationFamilies);
            bldr.integrationFamilies(integrationFamilies);
        }

        enrichedDto = bldr.build();
        enrichedDto = deploymentFrequencyConversionAdapter(enrichedDto);
        enrichedDto = failureRateConversionAdapter(enrichedDto);

        log.debug("velocityConfig {}, enrichedDto {}", velocityConfig, enrichedDto);
        return enrichedDto;
    }

    public Optional<VelocityConfigDTO> get(final String company, final String id) throws SQLException {
        Optional<VelocityConfig> velocityConfigOptional = velocityConfigsDatabaseService.get(company, id);
        if (velocityConfigOptional.isEmpty()) {
            return Optional.empty();
        }

        VelocityConfigDTO velocityConfig = enrichVelocityConfigDTO(velocityConfigOptional.get());

        if (velocityConfigOptional.get().getIsNew() != null && velocityConfigOptional.get().getIsNew()) {
            Optional<List<String>> associatedOURefIds = orgProfileDatabaseService.getByProfileId(
                    company, id, OrgProfile.ProfileType.WORKFLOW
            );
            if (associatedOURefIds.isPresent()) {
                List<String> assocOURefIds = associatedOURefIds.get();
                velocityConfig = velocityConfig.toBuilder().associatedOURefIds(assocOURefIds).build();
            }
        }

        return Optional.ofNullable(velocityConfig);

    }

    public Optional<VelocityConfigDTO> getByOuRefId(final String company, final Integer ouRefId) throws SQLException {
        Optional<VelocityConfig> velocityConfigOptional = velocityConfigsDatabaseService.getByOuRefId(company, ouRefId);
        if (velocityConfigOptional.isEmpty()) {
            return Optional.empty();
        }
        Optional<List<String>> associatedOURefIds = orgProfileDatabaseService.getByProfileId(
                company, velocityConfigOptional.get().getId().toString(), OrgProfile.ProfileType.WORKFLOW
        );

        VelocityConfigDTO velocityConfigDTO = enrichVelocityConfigDTO(velocityConfigOptional.get());
        if (associatedOURefIds.isPresent()) {
            List<String> assocOURefIds = associatedOURefIds.get();
            velocityConfigDTO = velocityConfigDTO.toBuilder().associatedOURefIds(assocOURefIds).build();
        }

        return Optional.ofNullable(velocityConfigDTO);
    }

    public Optional<VelocityConfigDTO> getDefaultConfig(final String company) {
        DbListResponse<VelocityConfig> dbListResponse = velocityConfigsDatabaseService.listByFilter(company, 0, 1, null, null, Boolean.TRUE, null);
        Optional<VelocityConfigDTO> velocityConfigDTO = CollectionUtils.emptyIfNull(dbListResponse.getRecords()).stream()
                .map(config -> enrichVelocityConfigDTO(config))
                .findFirst();
        return velocityConfigDTO;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public DbListResponse<VelocityConfigDTO> listByFilter(String company, DefaultListRequest filter) throws SQLException, BadRequestException {
        List<UUID> ids = CollectionUtils.emptyIfNull(DefaultListRequestUtils.getListOrDefault(filter, "ids")).stream().filter(Objects::nonNull).map(UUID::fromString).collect(Collectors.toList());
        List<String> names = CollectionUtils.emptyIfNull(DefaultListRequestUtils.getListOrDefault(filter, "names")).stream().filter(Objects::nonNull).collect(Collectors.toList());
        Boolean defaultConfig = filter.getFilterValue("default", Boolean.class).orElse(null);
        Map partial = filter.getFilterValue("partial", Map.class).orElse(null);
        log.debug("ids {}", ids);
        log.debug("names {}", names);
        log.debug("defaultConfig {}", defaultConfig);
        log.debug("partial {}", partial);

        DbListResponse<VelocityConfig> dbListResponse = velocityConfigsDatabaseService.listByFilter(company, filter.getPage(), filter.getPageSize(), ids, names, defaultConfig, partial);
        log.debug("dbListResponse totalCount {}, count {}", dbListResponse.getTotalCount(), dbListResponse.getCount());
        List<VelocityConfigDTO> velocityConfigDTOS = CollectionUtils.emptyIfNull(dbListResponse.getRecords()).stream()
                .map(config -> enrichVelocityConfigDTO(config))
                .collect(Collectors.toList());
        return DbListResponse.<VelocityConfigDTO>builder()
                .records(velocityConfigDTOS).totalCount(dbListResponse.getTotalCount())
                .build();
    }

    public VelocityConfigDTO getBaseConfigTemplate() {
        VelocityConfigDTO velocityConfigDTO = VelocityConfigDTO.builder()
                .defaultConfig(false)
                .preDevelopmentCustomStages(Collections.emptyList())
                .fixedStages(List.of(VelocityConfigDTO.Stage.builder()
                                .name("Lead time to First Commit").order(0).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                                .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.SCM_COMMIT_CREATED).build())
                                .build(),
                        VelocityConfigDTO.Stage.builder()
                                .name("PR Creation Time").order(1).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                                .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.SCM_PR_CREATED).build())
                                .build(),
                        VelocityConfigDTO.Stage.builder()
                                .name("Time to First Comment").order(2).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                                .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.SCM_PR_REVIEW_STARTED).build())
                                .build(),
                        VelocityConfigDTO.Stage.builder()
                                .name("Approval Time").order(3).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                                .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.SCM_PR_APPROVED).params(Map.of("last_approval", List.of("true"))).build())
                                .build(),
                        VelocityConfigDTO.Stage.builder()
                                .name("Merge Time").order(4).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                                .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.SCM_PR_MERGED).build())
                                .build()))
                .postDevelopmentCustomStages(Collections.emptyList())
                .issueManagementIntegrations(EnumSet.of(IntegrationType.JIRA))
                .build();
        return velocityConfigDTO;
    }

    private VelocityConfigDTO deploymentFrequencyConversionAdapter(VelocityConfigDTO velocityConfig) {
        if (velocityConfig.getIsNew() != null
                && velocityConfig.getIsNew()
                && velocityConfig.getDeploymentFrequency() != null) {
            String integrationType = velocityConfig.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getIntegrationType();
            VelocityConfigDTO.DeploymentFrequency deploymentFrequency = velocityConfig.getDeploymentFrequency();
            VelocityConfigDTO.VelocityConfigFilters deploymentFilters = deploymentFrequency.getVelocityConfigFilters();
            VelocityConfigDTO.FilterTypes filters = deploymentFilters.getDeploymentFrequency();

            if (INTEGRATION_TYPE_SCM.equals(integrationType)
                    && filters.getDeploymentCriteria() == null
                    && filters.getDeploymentRoute() == null) {

                VelocityConfigDTO.DeploymentCriteria deploymentCriteria = null;
                VelocityConfigDTO.DeploymentRoute deploymentRoute = null;
                Map<String, Map<String, List<String>>> scmFilters = filters.getScmFilters();
                switch (deploymentFrequency.getCalculationField()) {
                    case pr_closed_at:
                        deploymentCriteria = VelocityConfigDTO.DeploymentCriteria.pr_merged_closed;
                        deploymentRoute = VelocityConfigDTO.DeploymentRoute.pr;
                        break;
                    case pr_merged_at:
                        deploymentCriteria = VelocityConfigDTO.DeploymentCriteria.pr_merged;
                        deploymentRoute = VelocityConfigDTO.DeploymentRoute.pr;
                }
                scmFilters.remove("tags");
                scmFilters.remove("commit_branch");

                if (deploymentCriteria != null && deploymentRoute != null) {
                    velocityConfig = velocityConfig.toBuilder().deploymentFrequency(
                                    deploymentFrequency.toBuilder().integrationIds(List.of(velocityConfig.getDeploymentFrequency().getIntegrationId()))
                                            .velocityConfigFilters(deploymentFilters.toBuilder()
                                                    .deploymentFrequency(filters.toBuilder()
                                                            .deploymentCriteria(deploymentCriteria)
                                                            .deploymentRoute(deploymentRoute)
                                                            .calculationField(deploymentFrequency.getCalculationField())
                                                            .scmFilters(scmFilters)
                                                            .build())
                                                    .build())
                                            .integrationIds(List.of(deploymentFrequency.getIntegrationId()))
                                            .build())
                            .build();
                }
            } else if ((INTEGRATION_TYPE_CICD.equals(integrationType) || INTEGRATION_TYPE_IM.equals(integrationType))
                    && filters.getCalculationField() == null) {
                VelocityConfigDTO.CalculationField calculationField = deploymentFrequency.getCalculationField();

                velocityConfig = velocityConfig.toBuilder().deploymentFrequency(
                                deploymentFrequency.toBuilder()
                                        .integrationIds(List.of(deploymentFrequency.getIntegrationId()))
                                        .velocityConfigFilters(deploymentFilters.toBuilder()
                                                .deploymentFrequency(filters.toBuilder()
                                                        .calculationField(calculationField)
                                                        .build())
                                                .build())
                                        .build())
                        .build();
            }
        }
        return velocityConfig;
    }

    private VelocityConfigDTO failureRateConversionAdapter(VelocityConfigDTO velocityConfig) {
        if (velocityConfig.getIsNew() != null
                && velocityConfig.getIsNew()
                && velocityConfig.getChangeFailureRate() != null) {
            String failedIntegrationType = velocityConfig.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getIntegrationType();
            VelocityConfigDTO.ChangeFailureRate changeFailureRate = velocityConfig.getChangeFailureRate();
            VelocityConfigDTO.FilterTypes failedDeployment = changeFailureRate.getVelocityConfigFilters().getFailedDeployment();
            VelocityConfigDTO.VelocityConfigFilters failedDeploymentFilters = changeFailureRate.getVelocityConfigFilters();
            VelocityConfigDTO.FilterTypes filters = failedDeploymentFilters.getFailedDeployment();

            VelocityConfigDTO.DeploymentCriteria deploymentCriteria = null;
            VelocityConfigDTO.DeploymentRoute deploymentRoute = null;

            if (INTEGRATION_TYPE_SCM.equals(failedIntegrationType)
                    && failedDeployment.getDeploymentCriteria() == null
                    && failedDeployment.getDeploymentRoute() == null) {
                switch (changeFailureRate.getCalculationField()) {
                    case pr_closed_at:
                        deploymentCriteria = VelocityConfigDTO.DeploymentCriteria.pr_merged_closed;
                        deploymentRoute = VelocityConfigDTO.DeploymentRoute.pr;
                        break;
                    case pr_merged_at:
                        deploymentCriteria = VelocityConfigDTO.DeploymentCriteria.pr_merged;
                        deploymentRoute = VelocityConfigDTO.DeploymentRoute.pr;
                }
                Map<String, Map<String, List<String>>> failedDeploymentScmFilters = failedDeployment.getScmFilters();
                failedDeploymentScmFilters.remove("tags");
                failedDeploymentScmFilters.remove("commit_branch");

                if (deploymentCriteria != null) {
                    velocityConfig = velocityConfig.toBuilder().changeFailureRate(
                            changeFailureRate.toBuilder().integrationIds(List.of(velocityConfig.getChangeFailureRate().getIntegrationId()))
                                    .velocityConfigFilters(changeFailureRate.getVelocityConfigFilters().toBuilder()
                                            .failedDeployment(failedDeployment.toBuilder()
                                                    .deploymentCriteria(deploymentCriteria)
                                                    .deploymentRoute(deploymentRoute)
                                                    .calculationField(changeFailureRate.getCalculationField())
                                                    .scmFilters(failedDeploymentScmFilters)
                                                    .build()).build()).build()).build();

                    Boolean isAbsolute = velocityConfig.getChangeFailureRate().getIsAbsoulte();
                    if (isAbsolute != null && !isAbsolute) {

                        VelocityConfigDTO.FilterTypes totalDeployment = changeFailureRate.getVelocityConfigFilters().getTotalDeployment();
                        Map<String, Map<String, List<String>>> totalDeploymentScmFilters = totalDeployment.getScmFilters();
                        totalDeploymentScmFilters.remove("tags");
                        totalDeploymentScmFilters.remove("commit_branch");

                        velocityConfig = velocityConfig.toBuilder().changeFailureRate(
                                        changeFailureRate.toBuilder().integrationIds(List.of(velocityConfig.getChangeFailureRate().getIntegrationId()))
                                                .velocityConfigFilters(changeFailureRate.getVelocityConfigFilters().toBuilder()
                                                        .failedDeployment(failedDeployment.toBuilder()
                                                                .deploymentCriteria(deploymentCriteria)
                                                                .deploymentRoute(deploymentRoute)
                                                                .calculationField(changeFailureRate.getCalculationField())
                                                                .scmFilters(failedDeploymentScmFilters)
                                                                .build())
                                                        .totalDeployment(totalDeployment.toBuilder()
                                                                .deploymentCriteria(deploymentCriteria)
                                                                .deploymentRoute(deploymentRoute)
                                                                .calculationField(changeFailureRate.getCalculationField())
                                                                .scmFilters(totalDeploymentScmFilters)
                                                                .build())
                                                        .build())
                                                .integrationIds(List.of(changeFailureRate.getIntegrationId()))
                                                .build())
                                .build();
                    }
                }
            } else if ((INTEGRATION_TYPE_CICD.equals(failedIntegrationType)
                        || INTEGRATION_TYPE_IM.equals(failedIntegrationType))
                    && filters.getCalculationField() == null) {

                VelocityConfigDTO.CalculationField calculationField = changeFailureRate.getCalculationField();
                velocityConfig = velocityConfig.toBuilder()
                        .changeFailureRate(changeFailureRate.toBuilder()
                                .integrationIds(List.of(changeFailureRate.getIntegrationId()))
                                .velocityConfigFilters(
                                        failedDeploymentFilters.toBuilder()
                                                .failedDeployment(filters.toBuilder()
                                                        .calculationField(calculationField)
                                                        .build())
                                                .build())
                                .build())
                        .build();

                Boolean isAbsolute = velocityConfig.getChangeFailureRate().getIsAbsoulte();
                if (isAbsolute != null && !isAbsolute) {
                    VelocityConfigDTO.FilterTypes totalDeploymentFilters = failedDeploymentFilters.getTotalDeployment();

                    velocityConfig = velocityConfig.toBuilder()
                            .changeFailureRate(
                                    changeFailureRate.toBuilder()
                                            .integrationIds(List.of(changeFailureRate.getIntegrationId()))
                                            .velocityConfigFilters(changeFailureRate.getVelocityConfigFilters().toBuilder()
                                                    .failedDeployment(filters.toBuilder()
                                                            .calculationField(calculationField)
                                                            .build())
                                                    .totalDeployment(totalDeploymentFilters.toBuilder()
                                                            .calculationField(calculationField)
                                                            .build())
                                                    .build())
                                            .build())
                            .build();
                }
            }
        }
        return velocityConfig;
    }

    public VelocityConfigDTO adapterLeadTimeForChanges(VelocityConfigDTO velocityConfigDTO) {
        VelocityConfigDTO.LeadTimeForChange leadTimeForChange = velocityConfigDTO.getLeadTimeForChanges();
        return velocityConfigDTO.toBuilder()
                .preDevelopmentCustomStages(leadTimeForChange.getPreDevelopmentCustomStages())
                .fixedStages(leadTimeForChange.getFixedStages())
                .postDevelopmentCustomStages(leadTimeForChange.getPostDevelopmentCustomStages())
                .issueManagementIntegrations(leadTimeForChange.getIssueManagementIntegrations())
                .startingGenericEventTypes(leadTimeForChange.getStartingGenericEventTypes())
                .startingEventIsCommitCreated(leadTimeForChange.getStartingEventIsCommitCreated())
                .startingEventIsGenericEvent(leadTimeForChange.getStartingEventIsGenericEvent())
                .build();
    }

    public VelocityConfigDTO adapterMeanTimeToRestore(VelocityConfigDTO velocityConfigDTO) {
        VelocityConfigDTO.LeadTimeForChange meanTimeToRestore = velocityConfigDTO.getMeanTimeToRestore();
        return velocityConfigDTO.toBuilder()
                .preDevelopmentCustomStages(meanTimeToRestore.getPreDevelopmentCustomStages())
                .fixedStages(meanTimeToRestore.getFixedStages())
                .postDevelopmentCustomStages(meanTimeToRestore.getPostDevelopmentCustomStages())
                .issueManagementIntegrations(meanTimeToRestore.getIssueManagementIntegrations())
                .startingGenericEventTypes(meanTimeToRestore.getStartingGenericEventTypes())
                .startingEventIsCommitCreated(meanTimeToRestore.getStartingEventIsCommitCreated())
                .startingEventIsGenericEvent(meanTimeToRestore.getStartingEventIsGenericEvent())
                .build();
    }
}
