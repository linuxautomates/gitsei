package io.levelops.api.services;

import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.models.filters.DefaultListRequestUtils;
import io.levelops.commons.databases.services.dev_productivity.DevProductivityProfileDatabaseService;
import io.levelops.commons.databases.services.dev_productivity.services.DevProdTaskReschedulingService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.web.exceptions.BadRequestException;
import io.propelo.trellis_framework.client.TrellisAPIControllerClient;
import io.propelo.trellis_framework.client.exception.TrellisControllerClientException;
import io.propelo.trellis_framework.models.events.Event;
import io.propelo.trellis_framework.models.events.EventType;
import io.propelo.trellis_framework.models.jobs.JobStatus;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Service
public class DevProductivityProfileService {

    private  final DevProductivityProfileDatabaseService devProductivityProfileDatabaseService;
    private final DevProdTaskReschedulingService devProdTaskReschedulingService;
    private final TrellisAPIControllerClient trellisAPIControllerClient;

    @Autowired
    public DevProductivityProfileService(DevProductivityProfileDatabaseService devProductivityProfileDatabaseService, DevProdTaskReschedulingService devProdTaskReschedulingService, TrellisAPIControllerClient trellisAPIControllerClient){
        this.devProductivityProfileDatabaseService = devProductivityProfileDatabaseService;
        this.devProdTaskReschedulingService = devProdTaskReschedulingService;
        this.trellisAPIControllerClient = trellisAPIControllerClient;
    }

    private void validateSectionNames(final DevProductivityProfile profile) throws BadRequestException {
        Set<String> sectionNames = new HashSet<>();
        Set<String> duplicateSectionNames = CollectionUtils.emptyIfNull(profile.getSections()).stream()
                .map(DevProductivityProfile.Section::getName)
                .filter(n -> !sectionNames.add(n))
                .collect(Collectors.toSet());
        if(CollectionUtils.isNotEmpty(duplicateSectionNames)) {
            log.debug("duplicateSectionNames = {}", duplicateSectionNames);
            throw new BadRequestException("Dev productivity profile contains duplicate section names " + String.join(",", duplicateSectionNames));
        }
    }

    private void validateTicketCategoryDependency(final DevProductivityProfile profile) throws BadRequestException {
        if((profile.getEffortInvestmentProfileId() == null) || (CollectionUtils.isEmpty(profile.getSections()))) {
            return;
        }
        List<String> featuresWithoutTicketCategory = CollectionUtils.emptyIfNull(profile.getSections()).stream()
                .filter(s -> Boolean.TRUE.equals(s.getEnabled()) && CollectionUtils.isNotEmpty(s.getFeatures()) )
                .flatMap(s -> s.getFeatures().stream())
                .filter(f -> Boolean.TRUE.equals(f.getEnabled()) && f.getFeatureType().isTicketCategoryNeeded() && CollectionUtils.isEmpty(f.getTicketCategories()) )
                .map(DevProductivityProfile.Feature::getName)
                .collect(Collectors.toList());

        if(CollectionUtils.isNotEmpty(featuresWithoutTicketCategory)) {
            log.debug("duplicateSectionNames = {}", featuresWithoutTicketCategory);
            throw new BadRequestException("Dev productivity profile contains Investment profile but does not contain any categories " + String.join(",", featuresWithoutTicketCategory));
        }
    }

    private void validateSectionWeights(final DevProductivityProfile profile) throws BadRequestException {
        List<String> sectionsWithNullWeights = CollectionUtils.emptyIfNull(profile.getSections()).stream()
                .filter(s -> Boolean.TRUE.equals(s.getEnabled()) && Objects.isNull(s.getWeight()) )
                .map(DevProductivityProfile.Section::getName)
                .collect(Collectors.toList());

        if(CollectionUtils.isNotEmpty(sectionsWithNullWeights)) {
            log.debug("sectionsWithNullWeights = {}", sectionsWithNullWeights);
            throw new BadRequestException("Dev productivity profile contains sections with null weights " + String.join(",", sectionsWithNullWeights));
        }
    }


    //Internal for testing
    DevProductivityProfile validateAndCreateDevProductivityProfile(final DevProductivityProfile profile) throws BadRequestException {
        //Validate Section Names
        validateSectionNames(profile);

        //Validate Ticket Category Dependency
        validateTicketCategoryDependency(profile);

        //Validate Section Weights
        validateSectionWeights(profile);

       // validateAssociatedOuRefIds(profile);

        List<DevProductivityProfile.Section> sectionList = profile.getSections();
        int index = 0;
        for (DevProductivityProfile.Section section : profile.getSections()) {
            List<DevProductivityProfile.Feature> featureList = section.getFeatures();
            boolean isAllFeatureDisabled = true;
            for(DevProductivityProfile.Feature feature : featureList)
                if (feature.getEnabled()) {
                    isAllFeatureDisabled = false;
                    break;
                }
            if(isAllFeatureDisabled) {
                section = section.toBuilder().enabled(false).build();
                sectionList.set(index, section);
            }
            index++;
        }

        DevProductivityProfile.DevProductivityProfileBuilder bldr = DevProductivityProfile.builder()
                .name(profile.getName())
                .description(profile.getDescription())
                .effortInvestmentProfileId(profile.getEffortInvestmentProfileId())
                .settings(profile.getSettings())
                .sections(sectionList)
                .associatedOURefIds(profile.getAssociatedOURefIds());
        if(profile.getId() != null) {
            bldr.id(profile.getId());
        }
        DevProductivityProfile devProductivityProfile = bldr.build();
        return devProductivityProfile;
    }

    private void reSchdeuleUserDevProdReports(final String company, String devProdProfileId, DevProductivityProfile devProductivityProfile) {
        if(CollectionUtils.isEmpty(devProductivityProfile.getAssociatedOURefIds())) {
            log.info("Rescheduling User Dev Prod Reports for company {} skipper, no associated ous, trigger dev prod profile id {}", company, devProdProfileId);
            return;
        }
        log.info("Rescheduling User Dev Prod Reports for company {} starting, trigger dev prod profile id {}", company, devProdProfileId);
        boolean scheduled = devProdTaskReschedulingService.reScheduleUserDevProdReportsForOneTenant(company);
        log.info("Rescheduling User Dev Prod Reports for company {} completed success {}, trigger dev prod profile id {}", company, scheduled, devProdProfileId);
    }

    public String create(final String company, final DevProductivityProfile profile) throws SQLException, BadRequestException {
        DevProductivityProfile devProductivityProfile = validateAndCreateDevProductivityProfile(profile);
        try {
            String devProdProfileId = devProductivityProfileDatabaseService.insert(company, devProductivityProfile);
            reSchdeuleUserDevProdReports(company, devProdProfileId, devProductivityProfile);
            return devProdProfileId;
        } catch (DuplicateKeyException e) {
            log.error(e.getMessage());
            if(e.getMessage().contains("duplicate key value violates unique constraint")) {
                throw new BadRequestException("A default config already exists");
            } else {
                throw new BadRequestException(e);
            }
        }
    }

    public String update(final String company, final DevProductivityProfile profile, final Boolean persistV2Event) throws SQLException, BadRequestException {
        DevProductivityProfile newProfile = validateAndCreateDevProductivityProfile(profile);
        Boolean updateResult = null;
        try {
            DevProductivityProfile existingProfile = null;
            if(newProfile.getId() != null){
                existingProfile = devProductivityProfileDatabaseService.get(company,String.valueOf(newProfile.getId())).get();
            }
            updateResult = devProductivityProfileDatabaseService.update(company, newProfile);

            if(existingProfile != null && BooleanUtils.isTrue(persistV2Event)){
                log.info("Trellis persist V2 event = true, company {}, persistV2Event {}", company, persistV2Event);
                createDevProdProfileChangeEvent(company, existingProfile, newProfile);
            } else {
                log.info("Trellis persist V2 event = false, company {}, persistV2Event {}", company, persistV2Event);
            }
        } catch (DuplicateKeyException e) {
            log.error(e.getMessage());
            if(e.getMessage().contains("duplicate key value violates unique constraint")) {
                throw new BadRequestException("A default config already exists");
            } else {
                throw new BadRequestException(e);
            }
        }
        if(!Boolean.TRUE.equals(updateResult)) {
            throw new RuntimeException("For customer " + company + " profile id " + profile.getId().toString() + " failed to update config!");
        }
        //TODO : This step will be removed once the new trellis framework is stabilized
        reSchdeuleUserDevProdReports(company, profile.getId().toString(), newProfile);
        return profile.getId().toString();
    }

    private void createDevProdProfileChangeEvent(String company, DevProductivityProfile oldProfile, DevProductivityProfile newProfile) {
        try {
            trellisAPIControllerClient.createEvent(Event.builder()
                    .tenantId(company)
                    .eventType(EventType.DEV_PROD_PROFILE_CHANGE)
                    .status(JobStatus.SCHEDULED)
                    .data(Map.of("old_profile",oldProfile,"new_profile",newProfile))
                    .build());
        } catch (TrellisControllerClientException e) {
            log.error("Company {} Error creating event DEV_PROD_PROFILE_CHANGE event {} ",company, e);
        }
    }

    public Boolean makeDefault(final String company, final String id) throws SQLException {
        if(devProductivityProfileDatabaseService.upsertDefaultProfile(company, UUID.fromString(id)) != null)
            return true;
        return false;
    }

    public Boolean updateProfileOUMappings(final String company, final String profileId, final List<String> ouRefIds) throws SQLException {
        DevProductivityProfile existingProfile = devProductivityProfileDatabaseService.get(company, profileId).orElse(null);
        if(existingProfile != null){
            if(BooleanUtils.isTrue(devProductivityProfileDatabaseService.updateProfileOUMappings(company, UUID.fromString(profileId), ouRefIds))){
                DevProductivityProfile newProfile = devProductivityProfileDatabaseService.get(company,profileId).get();
                createDevProdProfileChangeEvent(company, existingProfile, newProfile);
                return true;
            }
        }
        return false;
    }

    public Boolean delete(final String company, final String id) throws SQLException {
        return devProductivityProfileDatabaseService.delete(company, id);
    }

    public Optional<DevProductivityProfile> get(final String company, final String id) throws SQLException {
        Optional<DevProductivityProfile> devProductivityProfileOptional = devProductivityProfileDatabaseService.get(company, id);
        if(devProductivityProfileOptional.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(devProductivityProfileOptional.get());
    }

    public Optional<DevProductivityProfile> getDefaultProfile(final String company) throws SQLException {
            return devProductivityProfileDatabaseService.getDefaultDevProductivityProfile(company);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public DbListResponse<DevProductivityProfile> listByFilter(String company, DefaultListRequest filter) throws SQLException, BadRequestException {
        List<UUID> ids = CollectionUtils.emptyIfNull(DefaultListRequestUtils.getListOrDefault(filter, "ids")).stream().filter(Objects::nonNull).map(UUID::fromString).collect(Collectors.toList());
        List<String> names = CollectionUtils.emptyIfNull(DefaultListRequestUtils.getListOrDefault(filter, "names")).stream().filter(Objects::nonNull).collect(Collectors.toList());
        List<Integer> ouRefIds = CollectionUtils.emptyIfNull(DefaultListRequestUtils.getListOrDefault(filter,"ou_ref_ids")).stream().filter(Objects::nonNull).map(Integer::valueOf).collect(Collectors.toList());
        Map partial = filter.getFilterValue("partial", Map.class).orElse(null);
        log.info("ids {}", ids);
        log.info("names {}", names);
        log.info("partial {}", partial);
        log.info("ou_ref_ids {}",ouRefIds);

        DbListResponse<DevProductivityProfile> dbListResponse = devProductivityProfileDatabaseService.listByFilter(company, filter.getPage(), filter.getPageSize(), ids, names,  partial, ouRefIds);
        log.info("dbListResponse totalCount {}, count {}", dbListResponse.getTotalCount(), dbListResponse.getCount());
        List<DevProductivityProfile> devProductivityProfiles = CollectionUtils.emptyIfNull(dbListResponse.getRecords()).stream()
                .collect(Collectors.toList());
        return DbListResponse.<DevProductivityProfile>builder()
                .records(devProductivityProfiles).totalCount(dbListResponse.getTotalCount())
                .build();
    }
}
