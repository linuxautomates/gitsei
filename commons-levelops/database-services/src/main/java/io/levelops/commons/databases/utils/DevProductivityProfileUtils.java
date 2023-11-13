package io.levelops.commons.databases.utils;

import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityParentProfile;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.utils.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class DevProductivityProfileUtils {
    public static boolean isParentProfileSameRecursive(DevProductivityParentProfile p1, DevProductivityParentProfile p2){
        boolean isParentProfileSame =  isParentProfileSame(p1,p2);
        isParentProfileSame &= p1.getSubProfiles().size() == p2.getSubProfiles().size();
        Map<Integer, DevProductivityProfile> indexSubProfileMap1 = p1.getSubProfiles().stream().collect(Collectors.toMap(DevProductivityProfile::getOrder, x->x));
        Map<Integer, DevProductivityProfile> indexSubProfileMap2 = p2.getSubProfiles().stream().collect(Collectors.toMap(DevProductivityProfile::getOrder, x->x));
        isParentProfileSame &= MapUtils.emptyIfNull(indexSubProfileMap1).keySet().equals(MapUtils.emptyIfNull(indexSubProfileMap2).keySet());
        for(Integer index : indexSubProfileMap1.keySet()){
            isParentProfileSame &= isSubProfileSameRecursive(indexSubProfileMap1.get(index), indexSubProfileMap2.get(index));
        }
        return isParentProfileSame;
    }
    public static boolean isSubProfileSameRecursive(DevProductivityProfile p1, DevProductivityProfile p2){
        boolean isSubProfileSame = isSubProfileSame(p1, p2);
        isSubProfileSame &= p1.getSections().size() == p2.getSections().size();
        Map<Integer, DevProductivityProfile.Section> indexSectionsMap1 = p1.getSections().stream().collect(Collectors.toMap(DevProductivityProfile.Section::getOrder, x->x));
        Map<Integer, DevProductivityProfile.Section> indexSectionsMap2 = p2.getSections().stream().collect(Collectors.toMap(DevProductivityProfile.Section::getOrder, x->x));
        isSubProfileSame &= MapUtils.emptyIfNull(indexSectionsMap1).keySet().equals(MapUtils.emptyIfNull(indexSectionsMap2).keySet());
        for(Integer index : indexSectionsMap1.keySet()){
            isSubProfileSame &= isProfileSectionSameRecursive(indexSectionsMap1.get(index), indexSectionsMap2.get(index));
        }
        return isSubProfileSame;
    }

    private static boolean isProfileSectionSameRecursive(DevProductivityProfile.Section s1, DevProductivityProfile.Section s2) {
        boolean isSectionSame = isProfileSectionsSame(s1,s2);
        isSectionSame &= s1.getFeatures().size() == s2.getFeatures().size();
        Map<Integer, DevProductivityProfile.Feature> indexFeaturesMap1 = s1.getFeatures().stream().collect(Collectors.toMap(DevProductivityProfile.Feature::getOrder, x->x));
        Map<Integer, DevProductivityProfile.Feature> indexFeaturesMap2 = s2.getFeatures().stream().collect(Collectors.toMap(DevProductivityProfile.Feature::getOrder, x->x));
        isSectionSame &= MapUtils.emptyIfNull(indexFeaturesMap1).keySet().equals(MapUtils.emptyIfNull(indexFeaturesMap2).keySet());
        for(Integer index : indexFeaturesMap1.keySet()){
            isSectionSame &= isProfileFeatureSame(indexFeaturesMap1.get(index), indexFeaturesMap2.get(index));
        }
        return isSectionSame;

    }

    private static boolean isProfileFeatureSame(DevProductivityProfile.Feature f1, DevProductivityProfile.Feature f2) {
        if(f1 == null || f2 == null)
            return false;
        return StringUtils.compare(f1.getName(),f2.getName()) == 0 && StringUtils.compare(f1.getDescription(),f2.getDescription()) == 0
                && BooleanUtils.compare(f1.getEnabled(), f2.getEnabled()) == 0 && f1.getFeatureType().equals(f2.getFeatureType())
                && ListUtils.isEqualList(f1.getTicketCategories(), f2.getTicketCategories()) && StringUtils.compare(String.valueOf(f1.getMaxValue()),String.valueOf(f2.getMaxValue())) == 0
                && StringUtils.compare(String.valueOf(f1.getLowerLimitPercentage()), String.valueOf(f2.getLowerLimitPercentage())) == 0
                && StringUtils.compare(String.valueOf(f1.getUpperLimitPercentage()), String.valueOf(f2.getUpperLimitPercentage())) == 0;
    }


    public static boolean isParentProfileSame(DevProductivityParentProfile p1, DevProductivityParentProfile p2){
        if(p1 == null || p2 == null)
            return false;
        return StringUtils.compare(p1.getName(),p2.getName()) == 0 && StringUtils.compare(p1.getDescription(),p2.getDescription()) == 0  && StringUtils.compare(String.valueOf(p1.getEffortInvestmentProfileId()),String.valueOf(p2.getEffortInvestmentProfileId())) == 0
                && areMapsSame(p1.getSettings(), p2.getSettings()) && areTicketCategoriesSame(p1.getFeatureTicketCategoriesMap(), p2.getFeatureTicketCategoriesMap());
    }

    public static boolean areTicketCategoriesSame(Map<DevProductivityProfile.FeatureType, List<UUID>> featureTicketCategoriesMap1, Map<DevProductivityProfile.FeatureType, List<UUID>> featureTicketCategoriesMap2) {
        return (MapUtils.isEmpty(featureTicketCategoriesMap1) && MapUtils.isEmpty(featureTicketCategoriesMap2))
                || (MapUtils.emptyIfNull(featureTicketCategoriesMap1).size() == MapUtils.emptyIfNull(featureTicketCategoriesMap2).size()
                && MapUtils.emptyIfNull(featureTicketCategoriesMap2).entrySet()
                .stream().allMatch(e -> e.getValue().equals(featureTicketCategoriesMap1.get(e.getKey()))));
    }

    public static boolean isSubProfileSame(DevProductivityProfile p1, DevProductivityProfile p2){
        if(p1 == null || p2 == null)
            return false;
        return StringUtils.compare(p1.getName(),p2.getName()) == 0 && StringUtils.compare(p1.getDescription(),p2.getDescription()) == 0
                && BooleanUtils.compare(p1.getEnabled(),p2.getEnabled()) == 0 && isMacthingCriteriaSame(p1.getMatchingCriteria(), p2.getMatchingCriteria());
    }

    private static boolean isProfileSectionsSame(DevProductivityProfile.Section s1, DevProductivityProfile.Section s2) {
        if(s1 == null || s2 == null)
            return false;
        return StringUtils.compare(s1.getName(),s2.getName()) == 0 && StringUtils.compare(s1.getDescription(),s2.getDescription()) == 0
                && BooleanUtils.compare(s1.getEnabled(),s2.getEnabled()) == 0 && StringUtils.compare(String.valueOf(s1.getWeight()),String.valueOf(s2.getWeight())) == 0;
    }

    private static boolean areMapsSame(Map<String, Object> oldSettings, Map<String, Object> newSettings) {
        return (MapUtils.isEmpty(oldSettings) && MapUtils.isEmpty(newSettings))
                || (MapUtils.emptyIfNull(oldSettings).size() == MapUtils.emptyIfNull(newSettings).size()
                && MapUtils.emptyIfNull(newSettings).entrySet()
                .stream().allMatch(e -> e.getValue().equals(oldSettings.get(e.getKey()))));
    }

    private static boolean isMacthingCriteriaSame(List<DevProductivityProfile.MatchingCriteria> matchingCriteria1, List<DevProductivityProfile.MatchingCriteria> matchingCriteria2) {
        if(CollectionUtils.isEmpty(matchingCriteria1) && CollectionUtils.isEmpty(matchingCriteria2))
            return true;
        else if(CollectionUtils.isEmpty(matchingCriteria1) || CollectionUtils.isEmpty(matchingCriteria2))
            return false;
        Map<String, Object> criteriaMap1 = matchingCriteria1.stream().collect(Collectors.toMap(DevProductivityProfile.MatchingCriteria::getField, x -> x));
        Map<String, Object> criteriaMap2 = matchingCriteria1.stream().collect(Collectors.toMap(DevProductivityProfile.MatchingCriteria::getField, x -> x));
        return areMapsSame(criteriaMap1, criteriaMap2);
    }
}