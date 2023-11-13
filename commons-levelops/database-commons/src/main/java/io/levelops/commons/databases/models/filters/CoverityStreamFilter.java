package io.levelops.commons.databases.models.filters;

import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.caching.CacheHashUtils;
import io.levelops.commons.models.DefaultListRequest;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.EnumUtils;

import java.util.List;
import java.util.Map;

import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOrDefault;
import static io.levelops.commons.caching.CacheHashUtils.hashData;
import static io.levelops.commons.caching.CacheHashUtils.hashDataMapOfStrings;
import static io.levelops.commons.caching.CacheHashUtils.hashDataUsingToString;

@Log4j2
@Value
@Builder(toBuilder = true)
public class CoverityStreamFilter {

    DISTINCT across;
    CALCULATION calculation;

    List<String> names;
    List<String> languages;
    List<String> projects;
    List<String> triageStoreIds;
    List<String> integrationIds;

    List<String> excludeNames;
    List<String> excludeLanguages;
    List<String> excludeProjects;
    List<String> excludeTriageStoreIds;
    Map<String, SortingOrder> sort;

    Integer page;
    Integer pageSize;

    public enum DISTINCT {
        name,
        language,
        project,
        triageStoreId;

        public static CoverityStreamFilter.DISTINCT fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(CoverityStreamFilter.DISTINCT.class, st);
        }
    }

    public enum CALCULATION {
        count;

        public static CoverityStreamFilter.CALCULATION fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(CoverityStreamFilter.CALCULATION.class, st);
        }
    }

    public String generateCacheRawString() {
        StringBuilder dataToHash = new StringBuilder();
        hashDataUsingToString(dataToHash, "across", across);
        hashDataUsingToString(dataToHash, "calculation", calculation);
        hashData(dataToHash, "names", names);
        hashData(dataToHash, "languages", languages);
        hashData(dataToHash, "projects", projects);
        hashData(dataToHash, "triageStoreIds", triageStoreIds);
        hashData(dataToHash, "integrationIds", integrationIds);
        hashData(dataToHash, "excludeNames", excludeNames);
        hashData(dataToHash, "excludeLanguages", excludeLanguages);
        hashData(dataToHash, "excludeProjects", excludeProjects);
        hashData(dataToHash, "excludeTriageStoreIds", excludeTriageStoreIds);
        hashData(dataToHash, "page", page);
        hashData(dataToHash, "page_size", pageSize);
        if (MapUtils.isNotEmpty(sort))
            hashDataMapOfStrings(dataToHash, "sort", sort);

        return dataToHash.toString();
    }

    public String generateCacheHash() {
        return CacheHashUtils.generateCacheHash(generateCacheRawString());
    }

    public static CoverityStreamFilter fromDefaultListRequest(DefaultListRequest filter,
                                                              DISTINCT across,
                                                              CALCULATION calculation) {
        String prefix = "cov_stream_";
        Map<String, Object> excludedFields = (Map<String, Object>) filter.getFilter()
                .getOrDefault("exclude", Map.of());
        CoverityStreamFilter.CoverityStreamFilterBuilder streamBldr = CoverityStreamFilter.builder()
                .names(getListOrDefault(filter, prefix + "names"))
                .languages(getListOrDefault(filter, prefix + "languages"))
                .projects(getListOrDefault(filter, prefix + "projects"))
                .triageStoreIds(getListOrDefault(filter, prefix + "triage_store_ids"))
                .integrationIds(getListOrDefault(filter, "integration_ids"))
                .excludeNames(getListOrDefault(excludedFields, prefix + "names"))
                .excludeLanguages(getListOrDefault(excludedFields, prefix + "languages"))
                .excludeProjects(getListOrDefault(excludedFields, prefix + "projects"))
                .excludeTriageStoreIds(getListOrDefault(excludedFields, prefix + "triage_store_ids"));

        if (across != null) {
            streamBldr.across(across);
        }
        if(calculation != null) {
            streamBldr.calculation(calculation);
        }
        CoverityStreamFilter streamsFilter = streamBldr.build();
        log.info("streamsFilter = {}", streamsFilter);
        return streamsFilter;
    }
}
