package io.levelops.commons.databases.models.filters;

import io.levelops.commons.caching.CacheHashUtils;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
public class SalesforceCaseFilter {
    SalesforceCaseFilter.DISTINCT across;
    SalesforceCaseFilter.CALCULATION calculation;
    List<SalesforceCaseFilter.EXTRA_CRITERIA> extraCriteria;
    List<String> caseIds;
    List<String> caseNumbers;
    List<String> priorities;
    List<String> statuses;
    List<String> contacts;
    List<String> types;
    List<String> integrationIds;
    List<String> accounts;
    Map<String, Object> age;
    AGG_INTERVAL aggInterval;

    @NonNull
    Long ingestedAt;
    ImmutablePair<Long, Long> SFCreatedRange;
    ImmutablePair<Long, Long> SFUpdatedRange;

    public enum DISTINCT {
        priority,
        account_name,
        status,
        contact,
        type,
        //time based
        trend,
        //no distinct (single query over which we are doing aggs. only supported for config table queries today)
        none;

        public static SalesforceCaseFilter.DISTINCT fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(SalesforceCaseFilter.DISTINCT.class, st);
        }
    }

    public enum CALCULATION {
        hops,
        bounces,
        resolution_time,
        case_count;

        public static CALCULATION fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(CALCULATION.class, st);
        }
    }

    public enum EXTRA_CRITERIA {
        idle,
        no_contact;

        public static SalesforceCaseFilter.EXTRA_CRITERIA fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(SalesforceCaseFilter.EXTRA_CRITERIA.class, st);
        }
    }

    public String generateCacheRawString() {
        StringBuilder dataToHash = new StringBuilder();
        if (across != null)
            dataToHash.append("across=").append(across.toString());
        if (calculation != null)
            dataToHash.append("calculation=").append(calculation.toString());
        if (CollectionUtils.isNotEmpty(extraCriteria)) {
            List<String> critStr = extraCriteria.stream().map(Enum::toString).sorted().collect(Collectors.toList());
            dataToHash.append(",extraCriteria=").append(String.join(",", critStr));
        }
        if (CollectionUtils.isNotEmpty(caseIds)) {
            ArrayList<String> tempList = new ArrayList<>(caseIds);
            Collections.sort(tempList);
            dataToHash.append(",caseIds=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(caseNumbers)) {
            ArrayList<String> tempList = new ArrayList<>(caseNumbers);
            Collections.sort(tempList);
            dataToHash.append(",caseNumbers=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(priorities)) {
            ArrayList<String> tempList = new ArrayList<>(priorities);
            Collections.sort(tempList);
            dataToHash.append(",priorities=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(statuses)) {
            ArrayList<String> tempList = new ArrayList<>(statuses);
            Collections.sort(tempList);
            dataToHash.append(",statuses=").append(String.join(",", tempList));
        }
        if (aggInterval != null)
            dataToHash.append(",aggInterval=").append(aggInterval);
        if (CollectionUtils.isNotEmpty(contacts)) {
            ArrayList<String> tempList = new ArrayList<>(contacts);
            Collections.sort(tempList);
            dataToHash.append(",contacts=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(types)) {
            ArrayList<String> tempList = new ArrayList<>(types);
            Collections.sort(tempList);
            dataToHash.append(",types=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            ArrayList<String> tempList = new ArrayList<>(integrationIds);
            Collections.sort(tempList);
            dataToHash.append(",integrationIds=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(accounts)) {
            ArrayList<String> tempList = new ArrayList<>(accounts);
            Collections.sort(tempList);
            dataToHash.append(",accounts=").append(String.join(",", tempList));
        }
        if (MapUtils.isNotEmpty(age)) {
            dataToHash.append(",age=");
            if (age.get("$gt") != null)
                dataToHash.append(age.get("$gt").toString()).append("-");
            if (age.get("$lt") != null)
                dataToHash.append(age.get("$lt").toString());
        }
        if (ingestedAt != null)
            dataToHash.append("ingestedAt=").append(ingestedAt);
        if (SFCreatedRange != null) {
            dataToHash.append(",SFCreatedRange=");
            if (SFCreatedRange.getLeft() != null)
                dataToHash.append(SFCreatedRange.getLeft()).append("-");
            if (SFCreatedRange.getRight() != null)
                dataToHash.append(SFCreatedRange.getRight());
        }
        if (SFUpdatedRange != null) {
            dataToHash.append(",SFUpdatedRange=");
            if (SFUpdatedRange.getLeft() != null)
                dataToHash.append(SFUpdatedRange.getLeft()).append("-");
            if (SFUpdatedRange.getRight() != null)
                dataToHash.append(SFUpdatedRange.getRight());
        }
        return dataToHash.toString();
    }

    public String generateCacheHash() {
        return CacheHashUtils.generateCacheHash(generateCacheRawString());
    }
}
