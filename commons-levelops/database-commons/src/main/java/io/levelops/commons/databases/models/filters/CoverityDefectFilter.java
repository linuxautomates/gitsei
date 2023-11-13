package io.levelops.commons.databases.models.filters;

import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.caching.CacheHashUtils;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.web.exceptions.BadRequestException;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOrDefault;
import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getTimeRange;
import static io.levelops.commons.caching.CacheHashUtils.hashData;
import static io.levelops.commons.caching.CacheHashUtils.hashDataMapOfStrings;
import static io.levelops.commons.caching.CacheHashUtils.hashDataUsingToString;

@Log4j2
@Value
@Builder(toBuilder = true)
public class CoverityDefectFilter {

    DISTINCT across;
    CALCULATION calculation;
    String aggInterval;

    Long snapshotCreatedAt;
    Map<String, Long> snapshotCreatedAtByIntegrationId;
    List<String> cids;
    List<String> snapshotIds;
    List<String> checkerNames;
    List<String> componentNames;
    List<String> integrationIds;
    List<String> categories;
    List<String> impacts;
    List<String> kinds;
    List<String> types;
    List<String> domains;
    List<String> firstDetectedStreams;
    List<String> firstDetectedSnapshotIds;
    List<String> lastDetectedStreams;
    List<String> lastDetectedSnapshotIds;
    List<String> filePaths;
    List<String> functionNames;
    ImmutablePair<Long, Long> occurrenceCount;
    ImmutablePair<Long, Long> firstDetectedAt;
    ImmutablePair<Long, Long> lastDetectedAt;
    ImmutablePair<Long, Long> snapshotCreatedRange;

    List<String> excludeCheckerNames;
    List<String> excludeComponentNames;
    List<String> excludeCategories;
    List<String> excludeImpacts;
    List<String> excludeKinds;
    List<String> excludeTypes;
    List<String> excludeDomains;
    List<String> excludeCids;
    List<String> excludeFirstDetectedStreams;
    List<String> excludeFirstDetectedSnapshotIds;
    List<String> excludeLastDetectedStreams;
    List<String> excludeLastDetectedSnapshotIds;
    List<String> excludeFilePaths;
    List<String> excludeFunctionNames;
    Map<String, SortingOrder> sort;

    Integer page;
    Integer pageSize;

    public enum DISTINCT {
        impact,
        category,
        kind,
        first_detected,
        last_detected,
        checker_name,
        component_name,
        type,
        domain,
        file,
        function,
        snapshot_created,
        first_detected_stream,
        last_detected_stream;

        public static CoverityDefectFilter.DISTINCT fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(CoverityDefectFilter.DISTINCT.class, st);
        }
    }

    public enum CALCULATION {
        count;

        public static CoverityDefectFilter.CALCULATION fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(CoverityDefectFilter.CALCULATION.class, st);
        }
    }

    public String generateCacheRawString() {
        StringBuilder dataToHash = new StringBuilder();
        hashDataUsingToString(dataToHash, "across", across);
        hashDataUsingToString(dataToHash, "calculation", calculation);
        hashData(dataToHash, "cids", cids);
        hashData(dataToHash, "snapshotIds", snapshotIds);
        hashData(dataToHash, "checkerNames", checkerNames);
        hashData(dataToHash, "componentNames", componentNames);
        hashData(dataToHash, "integrationIds", integrationIds);
        hashData(dataToHash, "categories", categories);
        hashData(dataToHash, "impacts", impacts);
        hashData(dataToHash, "kinds", kinds);
        hashData(dataToHash, "types", types);
        hashData(dataToHash, "domains", domains);
        hashData(dataToHash, "filePaths", filePaths);
        hashData(dataToHash, "functionNames", functionNames);
        hashData(dataToHash, "firstDetectedStreams", firstDetectedStreams);
        hashData(dataToHash, "firstDetectedSnapshotIds", firstDetectedSnapshotIds);
        hashData(dataToHash, "lastDetectedStreams", lastDetectedStreams);
        hashData(dataToHash, "lastDetectedSnapshotIds", lastDetectedSnapshotIds);
        hashData(dataToHash, "occurenceCount", occurrenceCount);
        hashData(dataToHash, "firstDetectedAt", firstDetectedAt);
        hashData(dataToHash, "lastDetectedAt", lastDetectedAt);
        hashData(dataToHash, "snapshotCreatedRange", snapshotCreatedRange);
        hashData(dataToHash, "excludeCids", excludeCids);
        hashData(dataToHash, "excludeCheckerNames", excludeCheckerNames);
        hashData(dataToHash, "excludeComponentNames", excludeComponentNames);
        hashData(dataToHash, "excludeCategories", excludeCategories);
        hashData(dataToHash, "excludeImpacts", excludeImpacts);
        hashData(dataToHash, "excludeKinds", excludeKinds);
        hashData(dataToHash, "excludeTypes", excludeTypes);
        hashData(dataToHash, "excludeDomains", excludeDomains);
        hashData(dataToHash, "excludeFilePaths", excludeFilePaths);
        hashData(dataToHash, "excludeFunctionNames", excludeFunctionNames);
        hashData(dataToHash, "excludeFirstDetectedSnapshotIds", excludeFirstDetectedSnapshotIds);
        hashData(dataToHash, "excludeFirstDetectedStreams", excludeFirstDetectedStreams);
        hashData(dataToHash, "excludeLastDetectedSnapshotIds", excludeLastDetectedSnapshotIds);
        hashData(dataToHash, "excludeLastDetectedStreams", excludeLastDetectedStreams);
        hashDataUsingToString(dataToHash, "aggInterval", aggInterval);
        hashDataUsingToString(dataToHash, "snapshotCreatedAt", snapshotCreatedAt);
        hashDataMapOfStrings(dataToHash, "latestSnapshotCreatedAtByIntegrationId", snapshotCreatedAtByIntegrationId);
        hashData(dataToHash, "page", page);
        hashData(dataToHash, "page_size", pageSize);
        if (MapUtils.isNotEmpty(sort))
            hashDataMapOfStrings(dataToHash, "sort", sort);

        return dataToHash.toString();
    }

    public String generateCacheHash() {
        return CacheHashUtils.generateCacheHash(generateCacheRawString());
    }

    @SuppressWarnings("unchecked")
    public static CoverityDefectFilter fromDefaultListRequest(DefaultListRequest filter,
                                                              CoverityDefectFilter.DISTINCT across,
                                                              CoverityDefectFilter.CALCULATION calculation,
                                                              String aggInterval) throws SQLException, BadRequestException {
        String prefix = "cov_defect_";
        Map<String, Object> excludedFields = (Map<String, Object>) filter.getFilter()
                .getOrDefault("exclude", Map.of());
        CoverityDefectFilter.CoverityDefectFilterBuilder defectBldr = CoverityDefectFilter.builder()
                .aggInterval(aggInterval)
                .snapshotCreatedAt(Objects.nonNull(filter.getFilter().get(prefix + "snapshot_created_at"))
                        ? Long.parseLong(String.valueOf(filter.getFilter().get(prefix + "snapshot_created_at"))) : null)
                .categories(getListOrDefault(filter, prefix + "categories"))
                .cids(getListOrDefault(filter, prefix + "cids"))
                .snapshotIds(getListOrDefault(filter, prefix + "snapshot_ids"))
                .checkerNames(getListOrDefault(filter, prefix + "checker_names"))
                .componentNames(getListOrDefault(filter, prefix + "component_names"))
                .integrationIds(getListOrDefault(filter, "integration_ids"))
                .impacts(getListOrDefault(filter, prefix + "impacts"))
                .kinds(getListOrDefault(filter, prefix + "kinds"))
                .types(getListOrDefault(filter, prefix + "types"))
                .domains(getListOrDefault(filter, prefix + "domains"))
                .filePaths(getListOrDefault(filter, prefix + "file_paths"))
                .functionNames(getListOrDefault(filter, prefix + "function_names"))
                .firstDetectedStreams(getListOrDefault(filter, prefix + "first_detected_streams"))
                .firstDetectedSnapshotIds(getListOrDefault(filter, prefix + "first_detected_snapshot_ids"))
                .lastDetectedStreams(getListOrDefault(filter, prefix + "last_detected_streams"))
                .lastDetectedSnapshotIds(getListOrDefault(filter, prefix + "last_detected_snapshot_ids"))
                .occurrenceCount(getTimeRange(filter, prefix + "occurrence_count"))
                .firstDetectedAt(getTimeRange(filter, prefix + "first_detected_at"))
                .lastDetectedAt(getTimeRange(filter, prefix + "last_detected_at"))
                .snapshotCreatedRange(getTimeRange(filter, prefix + "snapshot_created_range"))
                .excludeCids(getListOrDefault(excludedFields, prefix + "cids"))
                .excludeCategories(getListOrDefault(excludedFields, prefix + "categories"))
                .excludeCheckerNames(getListOrDefault(excludedFields, prefix + "checker_names"))
                .excludeComponentNames(getListOrDefault(excludedFields, prefix + "component_names"))
                .excludeImpacts(getListOrDefault(excludedFields, prefix + "impacts"))
                .excludeKinds(getListOrDefault(excludedFields, prefix + "kinds"))
                .excludeTypes(getListOrDefault(excludedFields, prefix + "types"))
                .excludeDomains(getListOrDefault(excludedFields, prefix + "domains"))
                .excludeFilePaths(getListOrDefault(excludedFields, prefix + "file_paths"))
                .excludeFunctionNames(getListOrDefault(excludedFields, prefix + "function_names"))
                .excludeFirstDetectedStreams(getListOrDefault(excludedFields, prefix + "first_detected_streams"))
                .excludeFirstDetectedSnapshotIds(getListOrDefault(excludedFields, prefix + "first_detected_snapshot_ids"))
                .excludeLastDetectedStreams(getListOrDefault(excludedFields, prefix + "last_detected_streams"))
                .excludeLastDetectedSnapshotIds(getListOrDefault(excludedFields, prefix + "last_detected_snapshot_ids"));

        if (across != null) {
            defectBldr.across(across);
        }
        if (calculation != null) {
            defectBldr.calculation(calculation);
        }
        CoverityDefectFilter defectsFilter = defectBldr.build();
        log.info("defectsFilter = {}", defectsFilter);
        return defectsFilter;
    }
}
