package io.levelops.commons.databases.services.dev_productivity.utils;

import com.google.protobuf.MapEntry;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityResponse;
import io.levelops.commons.databases.models.database.dev_productivity.FeatureResponse;
import io.levelops.commons.databases.models.database.dev_productivity.SectionResponse;
import io.opencensus.trace.Tracestate;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
public class OrgDevProductivityUtils {
    public static Map<Integer, Map<Integer, List<FeatureResponse>>> buildPartialFeatureResponses(final List<DevProductivityResponse> orgUserResponses) {
        log.debug("orgUserResponses = {}", orgUserResponses);
        if(CollectionUtils.isEmpty(orgUserResponses)) {
            return Map.of();
        }

        Map<Integer, Map<Integer, List<FeatureResponse>>> resultsMap = new HashMap<>();
        for(DevProductivityResponse orgUserResponse : orgUserResponses) {
            if(CollectionUtils.isEmpty(orgUserResponse.getSectionResponses())) {
                continue;
            }
            for(SectionResponse sc : orgUserResponse.getSectionResponses()) {
                if(CollectionUtils.isEmpty(sc.getFeatureResponses())) {
                    continue;
                }
                for(FeatureResponse fr : sc.getFeatureResponses()) {
                    resultsMap.putIfAbsent(sc.getOrder(), new HashMap<>());
                    resultsMap.get(sc.getOrder()).putIfAbsent(fr.getOrder(), new ArrayList<>());
                    resultsMap.get(sc.getOrder()).get(fr.getOrder()).add(fr);
                }
            }
        }
        log.debug("resultsMap = {}", resultsMap);
        return resultsMap;
    }

    public static Map<Integer, Map<Integer, Map<Integer, List<FeatureResponse>>>> buildPartialFeatureResponsesByProfiles(List<DevProductivityResponse> orgUserResponses) {
        if(CollectionUtils.isEmpty(orgUserResponses)) {
            return Map.of();
        }
        Map<Integer,Map<Integer, Map<Integer, List<FeatureResponse>>>> resultsMap = orgUserResponses.stream()
                .collect(Collectors.groupingBy(DevProductivityResponse::getOrder))
                .entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> buildPartialFeatureResponses(e.getValue())));
        return resultsMap;
    }
}
