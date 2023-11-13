package io.levelops.commons.databases.services.dev_productivity.services;

import io.levelops.commons.databases.models.database.dev_productivity.*;
import io.levelops.commons.databases.services.dev_productivity.engine.DevProductivityEngine;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


@Log4j2
@Service
public class DevProductivityResponseMergeService {
    private static final Boolean RESPONSE_HAS_CHANGED = true;
    private static final Boolean RESPONSE_HAS_NOT_CHANGED = false;

    private Map<Integer, Map<Integer, FeatureResponse>> extractFeatureResponseMap(DevProductivityResponse response) {
        if(response == null) {
            return Collections.EMPTY_MAP;
        }
        Map<Integer, Map<Integer, FeatureResponse>> featureResponseMap = new HashMap<>();
        for(SectionResponse sr : CollectionUtils.emptyIfNull(response.getSectionResponses())) {
            for(FeatureResponse fr : CollectionUtils.emptyIfNull(sr.getFeatureResponses())) {
                featureResponseMap.computeIfAbsent(sr.getOrder(),k -> new HashMap<>()).put(fr.getOrder(), fr);
            }
        }
        return featureResponseMap;
    }

    /**
     * This function merges old response & new response.
     * It does not merge, Fields used for Report Interval & Report Completeness.
     * @param devProductivityProfile
     * @param oldResponse
     * @param newResponse
     * @return
     */
    public DevProductivityResponse mergeDevProductivityResponses(DevProductivityProfile devProductivityProfile, DevProductivityResponse oldResponse, DevProductivityResponse newResponse, ReportIntervalType interval) {
        Map<Integer, Map<Integer, FeatureResponse>> existingFeatureResponses = extractFeatureResponseMap(oldResponse);
        Map<Integer, Map<Integer, FeatureResponse>> newFeatureResponses = extractFeatureResponseMap(newResponse);
        Map<Integer, Map<Integer, FeatureResponse>> mergedFeatureResponses = new HashMap<>();

        for(DevProductivityProfile.Section s : CollectionUtils.emptyIfNull(devProductivityProfile.getSections())) {
            for(DevProductivityProfile.Feature f : CollectionUtils.emptyIfNull(s.getFeatures())) {
                FeatureResponse oldFR = existingFeatureResponses.getOrDefault(s.getOrder(), Map.of()).getOrDefault(f.getOrder(), null);
                FeatureResponse newFR = newFeatureResponses.getOrDefault(s.getOrder(), Map.of()).getOrDefault(f.getOrder(), null);
                FeatureResponse mergedFR = ObjectUtils.firstNonNull(newFR, oldFR);
                if(mergedFR != null) {
                    //Populate "enabled" flag from feature to feature response
                    mergedFR = FeatureResponse.constructBuilder(s.getOrder(),f,mergedFR.getResult(), mergedFR.getCount(), mergedFR.getMean(), interval).build();
                    mergedFeatureResponses.computeIfAbsent(s.getOrder(),k -> new HashMap<>()).put(f.getOrder(), mergedFR);
                }
            }
        }

        DevProductivityResponse nonNullResponse = ObjectUtils.firstNonNull(newResponse, oldResponse);
        DevProductivityResponse mergedDevProductivityResponse = DevProductivityEngine.ResponseHelper.buildResponseFromFullFeatureResponses(devProductivityProfile, mergedFeatureResponses).toBuilder()
                .orgUserId(nonNullResponse.getOrgUserId()).fullName(nonNullResponse.getFullName()).email(nonNullResponse.getEmail()).order(nonNullResponse.getOrder())
                .build();

        return mergedDevProductivityResponse;
    }

    /**
     * Fields used for Report Interval & Report Completeness are NOT considered for this function
     * @param devProductivityProfile
     * @param oldResponse
     * @param newResponse
     * @return
     */
    public boolean devProductivityResponseHasChanged(DevProductivityProfile devProductivityProfile, DevProductivityResponse oldResponse, DevProductivityResponse newResponse) {
        Boolean nullCheck = customNullCheck(oldResponse, newResponse);
        if (Boolean.FALSE.equals(nullCheck)) {
            return RESPONSE_HAS_CHANGED;
        } else if (Boolean.TRUE.equals(nullCheck)) {
            return RESPONSE_HAS_NOT_CHANGED;
        }
        //If we reach here then both oldResponse & newResponse are NOT null
        if (oldResponse.getScore() != newResponse.getScore()) {
            return RESPONSE_HAS_CHANGED;
        }
        Map<Integer, Map<Integer, FeatureResponse>> existingFeatureResponses = extractFeatureResponseMap(oldResponse);
        Map<Integer, Map<Integer, FeatureResponse>> newFeatureResponses = extractFeatureResponseMap(newResponse);
        for(DevProductivityProfile.Section s : CollectionUtils.emptyIfNull(devProductivityProfile.getSections())) {
            for(DevProductivityProfile.Feature f : CollectionUtils.emptyIfNull(s.getFeatures())) {
                FeatureResponse oldFR = existingFeatureResponses.getOrDefault(s.getOrder(), Map.of()).getOrDefault(f.getOrder(), null);
                FeatureResponse newFR = newFeatureResponses.getOrDefault(s.getOrder(), Map.of()).getOrDefault(f.getOrder(), null);
                Boolean frNullCheck = customNullCheck(oldFR, newFR);
                if (Boolean.FALSE.equals(frNullCheck)) {
                    return RESPONSE_HAS_CHANGED;
                } else if (Boolean.TRUE.equals(frNullCheck)) {
                    continue;
                } else {
                    boolean frsAreEqual = oldFR.equals(newFR);
                    if(!frsAreEqual) {
                        return RESPONSE_HAS_CHANGED;
                    }
                }
            }
        }
        return RESPONSE_HAS_NOT_CHANGED;
    }

    /**
     * If both objects are null return true
     * If only one is null return false
     * If both are not null return null
     * @param o1
     * @param o2
     * @return
     */
    private Boolean customNullCheck(Object o1, Object o2){
        int count1 = (o1 == null) ? 1: 0;
        int count2 = (o2 == null) ? 1: 0;
        int count = count1 + count2;
        if(count == 2) {
            return Boolean.TRUE;
        } else if (count == 1) {
            return Boolean.FALSE;
        } else {
            return null;
        }
    }
}
