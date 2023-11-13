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

import java.util.List;
import java.util.Map;

import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOrDefault;
import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getTimeRange;
import static io.levelops.commons.caching.CacheHashUtils.hashData;
import static io.levelops.commons.caching.CacheHashUtils.hashDataMapOfStrings;
import static io.levelops.commons.caching.CacheHashUtils.hashDataUsingToString;

@Log4j2
@Value
@Builder(toBuilder = true)
public class CoveritySnapshotFilter {

    DISTINCT across;
    CALCULATION calculation;

    List<String> streamIds;
    List<String> snapshotIds;
    List<String> analysisHosts;
    List<String> analysisVersions;
    List<String> integrationIds;
    List<String> commitUsers;
    ImmutablePair<Long, Long> buildFailureCount;
    ImmutablePair<Long, Long> buildSuccessCount;
    ImmutablePair<Long, Long> snapshotCreatedAt;

    List<String> excludeSnapshotIds;
    List<String> excludeAnalysisHosts;
    List<String> excludeAnalysisVersions;
    List<String> excludeCommitUsers;
    Map<String, SortingOrder> sort;

    Integer page;
    Integer pageSize;

    public enum DISTINCT {
        analysis_version,
        analysis_host,
        snapshot_id,
        commit_user;

        public static CoveritySnapshotFilter.DISTINCT fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(CoveritySnapshotFilter.DISTINCT.class, st);
        }
    }

    public enum CALCULATION {
        analysis_time,
        count;

        public static CoveritySnapshotFilter.CALCULATION fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(CoveritySnapshotFilter.CALCULATION.class, st);
        }
    }

    public String generateCacheRawString() {
        StringBuilder dataToHash = new StringBuilder();
        hashDataUsingToString(dataToHash, "across", across);
        hashDataUsingToString(dataToHash, "calculation", calculation);
        hashData(dataToHash, "streamIds", streamIds);
        hashData(dataToHash, "snapshotIds", snapshotIds);
        hashData(dataToHash, "analysisHosts", analysisHosts);
        hashData(dataToHash, "analysisVersions", analysisVersions);
        hashData(dataToHash, "integrationIds", integrationIds);
        hashData(dataToHash, "commitUsers", commitUsers);
        hashData(dataToHash, "buildFailureCount", buildFailureCount);
        hashData(dataToHash, "buildSuccessCount", buildSuccessCount);
        hashData(dataToHash, "snapshotCreatedAt", snapshotCreatedAt);
        hashData(dataToHash, "excludeSnapshotIds", excludeSnapshotIds);
        hashData(dataToHash, "excludeAnalysisHosts", excludeAnalysisHosts);
        hashData(dataToHash, "excludeAnalysisVersions", excludeAnalysisVersions);
        hashData(dataToHash, "excludeCommitUsers", excludeCommitUsers);
        hashData(dataToHash, "page", page);
        hashData(dataToHash, "page_size", pageSize);
        if (MapUtils.isNotEmpty(sort))
            hashDataMapOfStrings(dataToHash, "sort", sort);

        return dataToHash.toString();
    }

    public String generateCacheHash() {
        return CacheHashUtils.generateCacheHash(generateCacheRawString());
    }

    public static CoveritySnapshotFilter fromDefaultListRequest(DefaultListRequest filter,
                                                              CoveritySnapshotFilter.DISTINCT across,
                                                              CoveritySnapshotFilter.CALCULATION calculation) throws BadRequestException {
        String prefix = "cov_snapshot_";
        Map<String, Object> excludedFields = (Map<String, Object>) filter.getFilter()
                .getOrDefault("exclude", Map.of());
        CoveritySnapshotFilter.CoveritySnapshotFilterBuilder snapshotBldr = CoveritySnapshotFilter.builder()
                .streamIds(getListOrDefault(filter, prefix + "stream_ids"))
                .snapshotIds(getListOrDefault(filter, prefix + "snapshot_ids"))
                .analysisHosts(getListOrDefault(filter, prefix + "analysis_hosts"))
                .analysisVersions(getListOrDefault(filter, prefix + "analysis_versions"))
                .integrationIds(getListOrDefault(filter, "integration_ids"))
                .commitUsers(getListOrDefault(filter, prefix + "commit_users"))
                .buildFailureCount(getTimeRange(filter, prefix + "build_failure_count"))
                .buildSuccessCount(getTimeRange(filter, prefix + "build_success_count"))
                .snapshotCreatedAt(getTimeRange(filter, prefix + "snapshot_created_at"))
                .excludeSnapshotIds(getListOrDefault(excludedFields, prefix + "snapshot_ids"))
                .excludeAnalysisHosts(getListOrDefault(excludedFields, prefix + "analysis_hosts"))
                .excludeAnalysisVersions(getListOrDefault(excludedFields, prefix + "analysis_versions"))
                .excludeCommitUsers(getListOrDefault(excludedFields, prefix + "commit_users"));

        if(across != null) {
            snapshotBldr.across(across);
        }
        if(calculation != null) {
            snapshotBldr.calculation(calculation);
        }
        CoveritySnapshotFilter snapshotFilter = snapshotBldr.build();
        log.info("snapshotFilter = {}", snapshotFilter);
        return snapshotFilter;
    }
}
