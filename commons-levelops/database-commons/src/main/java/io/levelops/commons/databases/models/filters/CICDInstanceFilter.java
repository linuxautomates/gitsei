package io.levelops.commons.databases.models.filters;

import io.levelops.commons.caching.CacheHashUtils;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
public class CICDInstanceFilter {

    List<String> ids;
    List<String> names;
    List<String> integrationIds;
    List<CICD_TYPE> types;

    List<String> excludeIds;
    List<String> excludeNames;
    List<CICD_TYPE> excludeTypes;

    ImmutablePair<Long, Long> instanceCreatedRange;
    ImmutablePair<Long, Long> instanceUpdatedRange;
    Map<String, Map<String, String>> partialMatch;
    Map<String, Boolean> missingFields;
    CICDInstanceFilter.DISTINCT across;
    Integer acrossLimit;

    public enum DISTINCT {
        id,
        name,
        url,
        type,
        none;

        public static CICDInstanceFilter.DISTINCT fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(CICDInstanceFilter.DISTINCT.class, st);
        }
    }

    public enum MISSING_FIELD {
        integration_id,
        none;

        public static CICDInstanceFilter.MISSING_FIELD fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(CICDInstanceFilter.MISSING_FIELD.class, st);
        }
    }

    public String generateCacheRawString() {
        StringBuilder dataToHash = new StringBuilder();
        if (across != null) {
            dataToHash.append("across=").append(across);
        }
        if (CollectionUtils.isNotEmpty(names)) {
            ArrayList<String> tempList = new ArrayList<>(names);
            Collections.sort(tempList);
            dataToHash.append(",names=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeNames)) {
            ArrayList<String> tempList = new ArrayList<>(excludeNames);
            Collections.sort(tempList);
            dataToHash.append(",excludeNames=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(ids)) {
            ArrayList<String> tempList = new ArrayList<>(ids);
            Collections.sort(tempList);
            dataToHash.append(",ids=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeIds)) {
            ArrayList<String> tempList = new ArrayList<>(excludeIds);
            Collections.sort(tempList);
            dataToHash.append(",excludeIds=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(types)) {
            ArrayList<String> tempList = types.stream()
                    .map(CICD_TYPE::toString)
                    .collect(Collectors.toCollection(ArrayList::new));
            dataToHash.append(",types=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeTypes)) {
            ArrayList<String> tempList = excludeTypes.stream()
                    .map(CICD_TYPE::toString)
                    .collect(Collectors.toCollection(ArrayList::new));
            dataToHash.append(",excludeTypes=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            ArrayList<String> tempList = new ArrayList<>(integrationIds);
            Collections.sort(tempList);
            dataToHash.append(",integrationIds=").append(String.join(",", tempList));
        }
        if (instanceCreatedRange != null) {
            dataToHash.append(",instanceCreatedRange=");
            if (instanceCreatedRange.getLeft() != null)
                dataToHash.append(instanceCreatedRange.getLeft()).append("-");
            if (instanceCreatedRange.getRight() != null)
                dataToHash.append(instanceCreatedRange.getRight());
        }
        if (instanceUpdatedRange != null) {
            dataToHash.append(",instanceUpdatedRange=");
            if (instanceUpdatedRange.getLeft() != null)
                dataToHash.append(instanceUpdatedRange.getLeft()).append("-");
            if (instanceUpdatedRange.getRight() != null)
                dataToHash.append(instanceUpdatedRange.getRight());
        }
        if (MapUtils.isNotEmpty(partialMatch)) {
            TreeSet<String> fields = new TreeSet<>(partialMatch.keySet());
            dataToHash.append(",partialMatch=(");
            for (String field : fields) {
                Map<String, String> innerMap = partialMatch.get(field);
                TreeSet<String> innerFields = new TreeSet<>(innerMap.keySet());
                dataToHash.append("(");
                for (String innerField : innerFields) {
                    dataToHash.append(innerField).append("=").append(innerMap.get(innerField)).append(",");
                }
                dataToHash.append("),");
            }
            dataToHash.append(")");
        }
        if (MapUtils.isNotEmpty(missingFields)) {
            TreeSet<String> fields = new TreeSet<>(missingFields.keySet());
            dataToHash.append(",missingFields=(");
            for (String field : fields) {
                Boolean data = missingFields.get(field);
                dataToHash.append(field).append("=").append(data).append(",");
            }
            dataToHash.append(")");
        }
        return dataToHash.toString();
    }

    public String generateCacheHash() {
        return CacheHashUtils.generateCacheHash(generateCacheRawString());
    }
}
