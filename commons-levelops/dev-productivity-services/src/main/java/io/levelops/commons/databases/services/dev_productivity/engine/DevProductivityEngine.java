package io.levelops.commons.databases.services.dev_productivity.engine;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.TenantSCMSettings;
import io.levelops.commons.databases.models.database.dev_productivity.*;
import io.levelops.commons.databases.models.filters.DevProductivityFilter;
import io.levelops.commons.databases.services.dev_productivity.handlers.DevProductivityFeatureHandler;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Log4j2
@Service
public class DevProductivityEngine {
    private static final Long DEFAULT_TIMEOUT_IN_SECS = TimeUnit.MINUTES.toSeconds(5);

    private final Map<DevProductivityProfile.FeatureType, DevProductivityFeatureHandler> featureHandlers;

    @Autowired
    public DevProductivityEngine(Map<DevProductivityProfile.FeatureType, DevProductivityFeatureHandler> featureHandlers) {
        this.featureHandlers = featureHandlers;
    }

    private Future<FeatureResponse> processFeature(final ExecutorService executorService, final String company, final Integer sectionOrder, final DevProductivityProfile.Feature feature, final DevProductivityProfile devProductivityProfile, final DevProductivityFilter devProductivityFilter, final OrgUserDetails orgUserDetails, final Map<String, Long> latestIngestedAtByIntegrationId, final TenantSCMSettings tenantSCMSettings) {
        return executorService.submit(() -> {
            log.debug("featureHandlers = {}", featureHandlers);
            DevProductivityFeatureHandler featureHandler = featureHandlers.get(feature.getFeatureType());
            if(featureHandler == null) {
                log.error("featureType = {}, handler NOT found", feature.getFeatureType());
                return null;
            }
            Map<String, Object> settings = devProductivityProfile.getSettings() != null ? devProductivityProfile.getSettings() : Map.of();

            log.info("featureType = {}", feature.getFeatureType());

            FeatureResponse featureResponse = null;
            try {
                featureResponse = featureHandler.calculateFeature(company, sectionOrder, feature, settings, devProductivityFilter, orgUserDetails, latestIngestedAtByIntegrationId, tenantSCMSettings);
                log.debug("featureType = {}, featureResponse = {}", feature.getFeatureType(), featureResponse);
            } catch (Exception e) {
                log.error("Error calculating dev prod feature for company {}, orgUserId {}, email {}, featureType = {}", company, orgUserDetails.getOrgUserId(), orgUserDetails.getEmail(), feature.getFeatureType(), e);
                throw e;
            }
            return featureResponse;
        });
    }

    private List<Future<FeatureResponse>> processSection(final ExecutorService executorService, final String company, final DevProductivityProfile.Section section, final DevProductivityProfile devProductivityProfile, final DevProductivityFilter devProductivityFilter, final OrgUserDetails orgUserDetails, final Map<String, Long> latestIngestedAtByIntegrationId, final TenantSCMSettings tenantSCMSettings, final Set<DevProductivityProfile.FeatureType> selectFeatureTypes) {
        List<Future<FeatureResponse>> futures = CollectionUtils.emptyIfNull(section.getFeatures()).stream()
                //.filter(feature -> Boolean.TRUE.equals(feature.getEnabled()))
                .filter(feature -> shouldCalculateFeature(feature, selectFeatureTypes))
                .map(feature -> processFeature(executorService, company, section.getOrder(), feature, devProductivityProfile, devProductivityFilter, orgUserDetails, latestIngestedAtByIntegrationId, tenantSCMSettings))
                .collect(Collectors.toList());
        return futures;
    }

    public DevProductivityResponse calculateDevProductivity(final String company, final DevProductivityProfile devProductivityProfile, final DevProductivityFilter devProductivityFilter, final OrgUserDetails orgUserDetails, final Map<String, Long> latestIngestedAtByIntegrationId, final TenantSCMSettings tenantSCMSettings, final Long timeOutInSeconds) {
        return calculateDevProductivity(company, devProductivityProfile, devProductivityFilter, orgUserDetails, latestIngestedAtByIntegrationId, tenantSCMSettings, timeOutInSeconds, Collections.EMPTY_SET);
    }

    private boolean shouldCalculateFeature(final DevProductivityProfile.Feature feature, final Set<DevProductivityProfile.FeatureType> selectFeatureTypes) {
        if(CollectionUtils.isEmpty(selectFeatureTypes)) {
            return true;
        }
        return selectFeatureTypes.contains(feature.getFeatureType());
    }

    public DevProductivityResponse calculateDevProductivity(final String company, final DevProductivityProfile devProductivityProfile, final DevProductivityFilter devProductivityFilter, final OrgUserDetails orgUserDetails, final Map<String, Long> latestIngestedAtByIntegrationId, final TenantSCMSettings tenantSCMSettings, final Long timeOutInSeconds, final Set<DevProductivityProfile.FeatureType> selectFeatureTypes) {
        log.info("dev prod calculate starting  company {}, orgUserId {}, email {}", company, orgUserDetails.getOrgUserId(), orgUserDetails.getEmail());
        log.debug("company = {}, devProductivityProfile = {}, devProductivityFilter = {}, orgUserDetails = {}, latestIngestedAtByIntegrationId = {}, tenantSCMSettings = {}", company, devProductivityProfile, devProductivityFilter, orgUserDetails, latestIngestedAtByIntegrationId, tenantSCMSettings);

        final Set<DevProductivityProfile.FeatureType> selectFeatureTypesSanitized = SetUtils.emptyIfNull(selectFeatureTypes);

        int featuresCount = CollectionUtils.emptyIfNull(devProductivityProfile.getSections()).stream()
                .filter(Objects::nonNull)
                //.filter(section -> Boolean.TRUE.equals(section.getEnabled()))
                .flatMap(s -> CollectionUtils.emptyIfNull(s.getFeatures()).stream())
                .filter(Objects::nonNull)
                //.filter(f -> Boolean.TRUE.equals(f.getEnabled()))
                .filter(f -> shouldCalculateFeature(f, selectFeatureTypesSanitized))
                .collect(Collectors.toList()).size();
        log.info("featuresCount = {}", featuresCount);
        ExecutorService executorService = Executors.newFixedThreadPool(featuresCount);

        List<Future<FeatureResponse>> futures = CollectionUtils.emptyIfNull(devProductivityProfile.getSections()).stream()
                //.filter(section -> Boolean.TRUE.equals(section.getEnabled()))
                .map(section -> processSection(executorService, company, section, devProductivityProfile, devProductivityFilter, orgUserDetails, latestIngestedAtByIntegrationId, tenantSCMSettings, selectFeatureTypesSanitized))
                .filter(CollectionUtils::isNotEmpty)
                .flatMap(Collection::stream).collect(Collectors.toList());

        Long effectiveTimeOutInSecs = MoreObjects.firstNonNull(timeOutInSeconds, DEFAULT_TIMEOUT_IN_SECS);
        long expectedEndTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(effectiveTimeOutInSecs);
        log.info("effectiveTimeOutInSecs = {}, expectedEndTime = {}", effectiveTimeOutInSecs, expectedEndTime);

        final Map<Integer, Map<Integer, FeatureResponse>> featureResponsesMap = futures.stream()
                .map(f -> {
                    try {
                        return f.get(Math.max(0, expectedEndTime - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        log.error("feature calculation failed for company {}, orgUserId {}, email {}", company, orgUserDetails.getOrgUserId(), orgUserDetails.getEmail(), e);
                        f.cancel(true);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(f -> f.getSectionOrder(), Collectors.toMap(f -> f.getOrder(), f -> f, (r1,r2)-> r1)));

        DevProductivityResponse devProductivityResponse = ResponseHelper.buildResponseFromFullFeatureResponses(devProductivityProfile, featureResponsesMap).toBuilder()
                .orgUserId(orgUserDetails.getOrgUserId()).fullName(orgUserDetails.getFullName()).email(orgUserDetails.getEmail()).customFields(orgUserDetails.getCustomFields()).order(devProductivityProfile.getOrder())
                .build();
        log.debug("devProductivityResponse = {}", devProductivityResponse);
        executorService.shutdownNow();
       return devProductivityResponse;
    }

    public FeatureBreakDown getFeatureBreakDown(final String company, DevProductivityProfile.Feature feature, DevProductivityProfile devProductivityProfile, final DevProductivityFilter devProductivityFilter, final OrgUserDetails orgUserDetails, final Map<String, Long> latestIngestedAtByIntegrationId, final TenantSCMSettings tenantSCMSettings,
                                                UUID effortInvestmentProfileId, Map<String, SortingOrder> sortBy ,Integer pageNumber, Integer pageSize) {

        DevProductivityProfile.FeatureType featureType = feature.getFeatureType();
        log.info("dev prod calculate featureBreakDown starting  company {}, orgUserId {}, email {}, featureType = {}", company, orgUserDetails.getOrgUserId(), orgUserDetails.getEmail(), featureType);
        log.debug("company = {}, devProductivityFilter = {}, orgUserDetails = {}, latestIngestedAtByIntegrationId = {}, tenantSCMSettings = {}, featureType = {}", company,  devProductivityFilter, orgUserDetails, latestIngestedAtByIntegrationId, tenantSCMSettings, featureType);
        log.debug("featureHandlers = {}", featureHandlers);
        DevProductivityFeatureHandler featureHandler = featureHandlers.get(featureType);
        if(featureHandler == null) {
            log.error("featureType = {}, handler NOT found", featureType);
            return null;
        }
        try {
            Map<String, Object> settings = devProductivityProfile.getSettings() != null ? devProductivityProfile.getSettings() : Map.of();
            FeatureBreakDown featureResponse = featureHandler.getBreakDown(company, feature, settings, devProductivityFilter, orgUserDetails, latestIngestedAtByIntegrationId, tenantSCMSettings, sortBy, pageNumber, pageSize);
            log.info("featureType = {}", feature.getFeatureType());
            log.debug("featureType = {}, featureResponse = {}", featureType, featureResponse);
            return featureResponse;
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }



    public static class ResponseHelper {
        public static Integer calculateWeightedSectionScore(Integer sectionScoreUnWeighted, Integer sectionWeight, Integer profileWeightsTotal) {
            //If Section UnWeighted score is null -> Section Weighted Score is also null
            if(sectionScoreUnWeighted == null) {
                return null;
            }
            //If total profile weights is null or 0 -> Section Weighted Score is null
            if(profileWeightsTotal == null || profileWeightsTotal==0) {
                return null;
            }
            return (int)Math.round(((double)(sectionScoreUnWeighted * sectionWeight) / profileWeightsTotal));
        }
        private static SectionResponse processFullSectionResponse(final Map<Integer, Map<Integer, FeatureResponse>> featureResponsesMap, final DevProductivityProfile.Section section, final Integer profileWeightsTotal) {
            List<FeatureResponse> featureResponses = CollectionUtils.emptyIfNull(section.getFeatures()).stream()
                    //.filter(f -> Boolean.TRUE.equals(f.getEnabled()))
                    .map(feature -> featureResponsesMap.getOrDefault(section.getOrder(), Map.of()).getOrDefault(feature.getOrder(), null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            OptionalDouble scoreOptional = CollectionUtils.emptyIfNull(featureResponses).stream()
                    .filter(fr -> Boolean.TRUE.equals(fr.getEnabled()))
                    .filter(fr -> fr.getScore()!= null)
                    .mapToInt(FeatureResponse::getScore).average();
            Integer score = scoreOptional.isEmpty() ? null : ((int)Math.round(scoreOptional.getAsDouble()));
            Integer weightedScore = calculateWeightedSectionScore(score, section.getWeight(), profileWeightsTotal);
            SectionResponse sectionResponse = SectionResponse.builder()
                    .name(section.getName()).description(section.getDescription()).order(section.getOrder())
                    .featureResponses(featureResponses)
                    .score(score)
                    .weightedScore(weightedScore)
                    .enabled(section.getEnabled())
                    .build();
            return sectionResponse;
        }

        public static DevProductivityResponse buildResponseFromFullFeatureResponses(final DevProductivityProfile devProductivityProfile, final Map<Integer, Map<Integer, FeatureResponse>> featureResponsesMap){
            Integer profileWeightsTotal = DevProductivityProfile.getProfileWeightsTotal(devProductivityProfile);
            List<SectionResponse> sectionResponses = CollectionUtils.emptyIfNull(devProductivityProfile.getSections()).stream()
                   // .filter(section -> Boolean.TRUE.equals(section.getEnabled()))
                    .map(section -> processFullSectionResponse(featureResponsesMap, section, profileWeightsTotal))
                    .collect(Collectors.toList());
            Integer score = CollectionUtils.emptyIfNull(sectionResponses).stream().filter(sr -> Boolean.TRUE.equals(sr.getEnabled())).filter(sr -> sr.getWeightedScore()!= null).mapToInt(SectionResponse::getWeightedScore).sum();
            DevProductivityResponse devProductivityResponse = DevProductivityResponse.builder()
                    .sectionResponses(sectionResponses)
                    .score(score)
                    .build();
            return devProductivityResponse;
        }

        public static DevProductivityResponse buildResponseFromFullFeatureResponses(final DevProductivityParentProfile devProductivityParentProfile, final Map<Integer,Map<Integer, Map<Integer, FeatureResponse>>> devProdProfileFeatureResponsesMap){
            Map<Integer,Integer> profileWeightsBySubProfileIndex = devProductivityParentProfile.getSubProfiles().stream().collect(Collectors.toMap(DevProductivityProfile::getOrder, p -> DevProductivityProfile.getProfileWeightsTotal(p)));
            Map<Integer,List<SectionResponse>> sectionIdxSectionResponsesMap = CollectionUtils.emptyIfNull(devProductivityParentProfile.getSubProfiles()).stream()
                    .filter(subProfile -> subProfile.getEnabled())
                    .map(subProfile -> CollectionUtils.emptyIfNull(subProfile.getSections()).stream()
                            .map(section -> processFullSectionResponse(devProdProfileFeatureResponsesMap.get(subProfile.getOrder()), section, profileWeightsBySubProfileIndex.get(subProfile.getOrder())))
                            .collect(Collectors.toList())).flatMap(Collection::stream).collect(Collectors.groupingBy(SectionResponse::getOrder));
            List<DevProductivityProfile.Section> allSections = getAllSections(devProductivityParentProfile);
            List<SectionResponse> sectionResponses = CollectionUtils.emptyIfNull(allSections).stream()
                    .map(section -> mergeSectionResponsesAcrossSubProfiles(section, sectionIdxSectionResponsesMap))
                    .collect(Collectors.toList());
            Integer score = CollectionUtils.emptyIfNull(sectionResponses).stream().filter(sr -> Boolean.TRUE.equals(sr.getEnabled())).filter(sr -> sr.getWeightedScore()!= null).mapToInt(SectionResponse::getWeightedScore).sum();
            DevProductivityResponse devProductivityResponse = DevProductivityResponse.builder()
                    .sectionResponses(sectionResponses)
                    .score(score)
                    .build();
            return devProductivityResponse;
        }

        private  static SectionResponse mergeSectionResponsesAcrossSubProfiles(DevProductivityProfile.Section section, Map<Integer, List<SectionResponse>> sectionIdxSectionResponsesMap) {
            SectionResponse sectionResponse = SectionResponse.builder()
                    .name(section.getName()).description(section.getDescription()).order(section.getOrder()).build();
            List<SectionResponse> sectionResponsesToMerge = sectionIdxSectionResponsesMap.get(section.getOrder());
            List<FeatureResponse> featureResponses = sectionResponsesToMerge.stream().flatMap(sr -> sr.getFeatureResponses().stream()).collect(Collectors.toList());
            OptionalDouble scoreOptional = CollectionUtils.emptyIfNull(sectionResponsesToMerge).stream()
                    .filter(sr -> sr.getEnabled())
                    .mapToInt(SectionResponse::getScore).average();
            Integer score = scoreOptional.isEmpty() ? null : ((int)Math.round(scoreOptional.getAsDouble()));
            OptionalDouble weightedScoreOptional = CollectionUtils.emptyIfNull(sectionResponsesToMerge).stream()
                    .filter(sr -> sr.getEnabled())
                    .mapToInt(SectionResponse::getWeightedScore).average();
            Integer weightedScore = scoreOptional.isEmpty() ? null : ((int)Math.round(scoreOptional.getAsDouble()));
            Boolean enabled = CollectionUtils.emptyIfNull(sectionResponsesToMerge).stream().anyMatch(SectionResponse::getEnabled);

            sectionResponse = sectionResponse.toBuilder()
                    .score(score)
                    .weightedScore(weightedScore)
                    .enabled(enabled)
                    .featureResponses(featureResponses)
                    .build();

            return sectionResponse;
        }

        private static SectionResponse processPartialSectionResponse(final Map<Integer, Map<Integer, List<FeatureResponse>>> featuresResultMap, final DevProductivityProfile.Section section, final Integer profileWeightsTotal, final ReportIntervalType interval) {
            List<FeatureResponse> featureResponses = CollectionUtils.emptyIfNull(section.getFeatures()).stream()
                    //.filter(feature -> Boolean.TRUE.equals(feature.getEnabled()))
                    .map(feature -> {
                        List<FeatureResponse> featureResponsesList = CollectionUtils.emptyIfNull(featuresResultMap.getOrDefault(section.getOrder(), Map.of()).getOrDefault(feature.getOrder(), null))
                                .stream().filter(Objects::nonNull).collect(Collectors.toList());
                        int noOfOrgUsers = featureResponsesList.size();
                        Long totalResult = featureResponsesList.stream().map(FeatureResponse::getResult).filter(Objects::nonNull).mapToLong(Long::longValue).sum();
                        Long totalCount = featureResponsesList.stream().filter(Objects::nonNull).map(FeatureResponse::getCount).filter(Objects::nonNull).mapToLong(Long::longValue).sum();
                        Long avgResult = noOfOrgUsers == 0 ? null : Math.round(totalResult*1.0/noOfOrgUsers);
                        Long avgCount = noOfOrgUsers == 0 ? null : Math.round(totalCount*1.0/noOfOrgUsers);
                        FeatureResponse fr = FeatureResponse.constructBuilder(section.getOrder(), feature, avgResult, interval)
                                .count(avgCount)
                                .build();
                        return fr;
                    })
                    .collect(Collectors.toList());
            OptionalDouble scoreOptional = CollectionUtils.emptyIfNull(featureResponses).stream().filter(fr -> fr.getScore()!= null).filter(fr -> Boolean.TRUE.equals(fr.getEnabled())).mapToInt(FeatureResponse::getScore).average();
            Integer score = scoreOptional.isEmpty() ? null : ((int)Math.round(scoreOptional.getAsDouble()));
            Integer weightedScore = calculateWeightedSectionScore(score, section.getWeight(), profileWeightsTotal);

            SectionResponse sectionResponse = SectionResponse.builder()
                    .name(section.getName()).description(section.getDescription()).order(section.getOrder())
                    .featureResponses(featureResponses)
                    .score(score)
                    .weightedScore(weightedScore)
                    .enabled(section.getEnabled())
                    .build();
            return sectionResponse;
        }

        public static DevProductivityResponse buildResponseFromPartialFeatureResponses(final DevProductivityProfile devProductivityProfile, final Map<Integer, Map<Integer, List<FeatureResponse>>> featuresResultMap, final ReportIntervalType interval){
            Integer profileWeightsTotal = DevProductivityProfile.getProfileWeightsTotal(devProductivityProfile);

            List<SectionResponse> sectionResponses = CollectionUtils.emptyIfNull(devProductivityProfile.getSections()).stream()
                    //.filter(section -> Boolean.TRUE.equals(section.getEnabled()))
                    .map(section -> processPartialSectionResponse(featuresResultMap, section, profileWeightsTotal, interval))
                    .collect(Collectors.toList());
            Integer score = CollectionUtils.emptyIfNull(sectionResponses).stream().filter(sr -> Boolean.TRUE.equals(sr.getEnabled())).filter(sr -> sr.getWeightedScore()!= null).mapToInt(SectionResponse::getWeightedScore).sum();

            DevProductivityResponse devProductivityResponse = DevProductivityResponse.builder()
                    .sectionResponses(sectionResponses)
                    .score(score)
                    .build();
            return devProductivityResponse;
        }

        public static DevProductivityResponse buildResponseFromPartialFeatureResponses(final DevProductivityParentProfile devProductivityParentProfile, final Map<Integer, Map<Integer, Map<Integer, List<FeatureResponse>>>> devProdProfileFeatureResponsesMap, final ReportIntervalType interval){
            Map<Integer,Integer> profileWeightsBySubProfileIndex = devProductivityParentProfile.getSubProfiles().stream().collect(Collectors.toMap(DevProductivityProfile::getOrder, p -> DevProductivityProfile.getProfileWeightsTotal(p)));
            Map<Integer,List<SectionResponse>> sectionIdxSectionResponsesMap = CollectionUtils.emptyIfNull(devProductivityParentProfile.getSubProfiles()).stream()
                    .filter(subProfile -> BooleanUtils.isTrue(subProfile.getEnabled()))
                    .map(subProfile -> CollectionUtils.emptyIfNull(subProfile.getSections()).stream()
                            .map(section -> processPartialSectionResponse(devProdProfileFeatureResponsesMap.get(subProfile.getOrder()), section, profileWeightsBySubProfileIndex.get(subProfile.getOrder()), interval))
                            .collect(Collectors.toList())).flatMap(Collection::stream).collect(Collectors.groupingBy(SectionResponse::getOrder));
            List<DevProductivityProfile.Section> allSections = getAllSections(devProductivityParentProfile);
            List<SectionResponse> sectionResponses = allSections.stream()
                    .map(section -> mergeSectionResponsesAcrossSubProfiles(section, sectionIdxSectionResponsesMap))
                    .collect(Collectors.toList());
            Integer score = CollectionUtils.emptyIfNull(sectionResponses).stream().filter(sr -> Boolean.TRUE.equals(sr.getEnabled())).filter(sr -> sr.getWeightedScore()!= null).mapToInt(SectionResponse::getWeightedScore).sum();
            DevProductivityResponse devProductivityResponse = DevProductivityResponse.builder()
                    .sectionResponses(sectionResponses)
                    .score(score)
                    .build();

            return devProductivityResponse;
        }

        private static List<DevProductivityProfile.Section> getAllSections(DevProductivityParentProfile devProductivityParentProfile) {
            return devProductivityParentProfile.getSubProfiles().stream()
                    .flatMap(s -> s.getSections().stream())
                    .collect(Collectors.collectingAndThen(
                            Collectors.toMap(
                                    s -> s.getOrder(),
                                    s -> s,
                                    (existing, replacement) -> existing
                            ),
                            m -> new java.util.ArrayList<>(m.values())));
        }
    }
}