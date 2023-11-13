package io.levelops.commons.databases.models.filters;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.caching.CacheHashUtils;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.models.DefaultListRequest;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Log4j2
@Value
@Builder(toBuilder = true)
public class UserIdentitiesFilter {

    List<String> cloudIds;
    List<String> integrationIds;
    ImmutablePair<Long, Long> usersCreatedRange;
    ImmutablePair<Long, Long> usersUpdatedRange;
    Map<String, Map<String, String>> partialMatch;
    Map<String, SortingOrder> sort;
    Boolean emptyEmails;
    DbScmUser.MappingStatus mappingStatus;

    public String generateCacheRawString() {
        StringBuilder dataToHash = new StringBuilder();
        if (CollectionUtils.isNotEmpty(cloudIds)) {
            ArrayList<String> tempList = new ArrayList<>(cloudIds);
            Collections.sort(tempList);
            dataToHash.append("cloudIds=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            ArrayList<String> tempList = new ArrayList<>(integrationIds);
            Collections.sort(tempList);
            dataToHash.append(",integrationIds=").append(String.join(",", tempList));
        }
        if (usersCreatedRange != null) {
            dataToHash.append(",usersCreatedRange=");
            if (usersCreatedRange.getLeft() != null)
                dataToHash.append(usersCreatedRange.getLeft()).append("-");
            if (usersCreatedRange.getRight() != null)
                dataToHash.append(usersCreatedRange.getRight());
        }
        if (usersUpdatedRange != null) {
            dataToHash.append(",usersUpdatedRange=");
            if (usersUpdatedRange.getLeft() != null)
                dataToHash.append(usersUpdatedRange.getLeft()).append("-");
            if (usersUpdatedRange.getRight() != null)
                dataToHash.append(usersUpdatedRange.getRight());
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
        if (MapUtils.isNotEmpty(sort)) {
            TreeSet<String> fields = new TreeSet<>(sort.keySet());
            dataToHash.append(",sort=(");
            for (String field : fields) {
                dataToHash.append(field.toLowerCase(Locale.ROOT)).append("=")
                        .append(sort.get(field).toString().toLowerCase(Locale.ROOT));
            }
            dataToHash.append(")");
        }
        return dataToHash.toString();
    }

    public String generateCacheHash() {
        return CacheHashUtils.generateCacheHash(generateCacheRawString());
    }


    public static UserIdentitiesFilter fromDefaultListRequest(String company, DefaultListRequest filter, Set<String> partialMapColumns) {
        Map<String, String> createdRange = filter.<String, String>getFilterValueAsMap("created_at").orElse(Map.of());
        final Long createdAtStart = createdRange.get("$gt") != null ? Long.valueOf(createdRange.get("$gt")) : null;
        final Long createdAtEnd = createdRange.get("$lt") != null ? Long.valueOf(createdRange.get("$lt")) : null;
        Map<String, String> updatedRange = filter.<String, String>getFilterValueAsMap("updated_at").orElse(Map.of());
        final Long updatedAtStart = updatedRange.get("$gt") != null ? Long.valueOf(updatedRange.get("$gt")) : null;
        final Long updatedAtEnd = updatedRange.get("$lt") != null ? Long.valueOf(updatedRange.get("$lt")) : null;
        Map<String, Map<String, String>> partialMatchMap
                = filter.<String, Map<String, String>>getFilterValueAsMap("partial_match").orElse(Map.of());
        validatePartialMatchFilter(company, partialMatchMap, partialMapColumns);
        Map<String, SortingOrder> sorting = SortingConverter.fromFilter(MoreObjects.firstNonNull(filter.getSort(), List.of()));
        return UserIdentitiesFilter.builder()
                .cloudIds(getListOrDefault(filter, "cloud_ids"))
                .integrationIds(getListOrDefault(filter, "integration_ids"))
                .usersCreatedRange(ImmutablePair.of(createdAtStart, createdAtEnd))
                .usersUpdatedRange(ImmutablePair.of(updatedAtStart, updatedAtEnd))
                .partialMatch(partialMatchMap)
                .sort(sorting)
                .emptyEmails(null)
                .build();
    }

    private static List<String> getListOrDefault(DefaultListRequest filter, String key) {
        try {
            // handles list with items that are not strings to begin with
            return filter.<Object>getFilterValueAsList(key).orElse(List.of()).stream().map(Object::toString).collect(Collectors.toList());
        } catch (ClassCastException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filter parameter: " + key);
        }
    }

    private static void validatePartialMatchFilter(String company, Map<String, Map<String, String>> partialMatchMap,
                                                   Set<String> partialMapColumns) {
        if (MapUtils.isEmpty(partialMatchMap)) {
            return;
        }
        ArrayList<String> partialMatchKeys = new ArrayList<>(partialMatchMap.keySet());
        List<String> invalidPartialMatchKeys = partialMatchKeys.stream()
                .filter(key -> !partialMapColumns.contains(key))
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(invalidPartialMatchKeys)) {
            log.warn("Company - " + company + ": " + String.join(",", invalidPartialMatchKeys)
                    + " are not valid fields for integration users partial match based filter");
        }
    }
}
