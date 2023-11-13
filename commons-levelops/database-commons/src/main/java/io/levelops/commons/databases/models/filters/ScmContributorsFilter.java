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
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.caching.CacheHashUtils.hashData;

@Value
@Builder(toBuilder = true)
public class ScmContributorsFilter {
    DISTINCT across;
    List<String> authors;
    List<String> committers;
    List<String> repoIds;
    List<String> projects;
    List<String> integrationIds;
    Map<String, Map<String, String>> partialMatch;
    ImmutablePair<Long, Long> dataTimeRange;
    ImmutablePair<Long, Long> locRange;

    List<String> commitTitles;
    Set<UUID> orgProductIds;
    boolean includeIssues;
    List<String> excludeAuthors;
    List<String> excludeCommitters;
    List<String> excludeRepoIds;
    List<String> excludeProjects;
    Map<String, Map<String, String>> excludePartialMatch;
    ImmutablePair<Long, Long> excludeLocRange;

    List<String> excludeCommitTitles;

    public String generateCacheRawString() {
        StringBuilder dataToHash = new StringBuilder();
        if (across != null)
            dataToHash.append("across=").append(across);
        if (CollectionUtils.isNotEmpty(authors)) {
            ArrayList<String> tempList = new ArrayList<>(authors);
            Collections.sort(tempList);
            dataToHash.append(",authors=").append(String.join(",", tempList));
        }
        hashData(dataToHash, "includeIssues", includeIssues);
        if (CollectionUtils.isNotEmpty(committers)) {
            ArrayList<String> tempList = new ArrayList<>(committers);
            Collections.sort(tempList);
            dataToHash.append(",committers=").append(String.join(",", tempList));
        }
        if(CollectionUtils.isNotEmpty(orgProductIds)) {
            ArrayList<String> tempList = orgProductIds.stream().sorted().map(UUID::toString)
                    .collect(Collectors.toCollection(ArrayList::new));
            dataToHash.append(",ordProductIds=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(repoIds)) {
            ArrayList<String> tempList = new ArrayList<>(repoIds);
            Collections.sort(tempList);
            dataToHash.append(",repoIds=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(projects)) {
            ArrayList<String> tempList = new ArrayList<>(projects);
            Collections.sort(tempList);
            dataToHash.append(",projects=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            ArrayList<String> tempList = new ArrayList<>(integrationIds);
            Collections.sort(tempList);
            dataToHash.append(",integrationIds=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeAuthors)) {
            ArrayList<String> tempList = new ArrayList<>(excludeAuthors);
            Collections.sort(tempList);
            dataToHash.append(",excludeAuthors=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeCommitters)) {
            ArrayList<String> tempList = new ArrayList<>(excludeCommitters);
            Collections.sort(tempList);
            dataToHash.append(",excludeCommitters=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeRepoIds)) {
            ArrayList<String> tempList = new ArrayList<>(excludeRepoIds);
            Collections.sort(tempList);
            dataToHash.append(",excludeRepoIds=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeProjects)) {
            ArrayList<String> tempList = new ArrayList<>(excludeProjects);
            Collections.sort(tempList);
            dataToHash.append(",excludeProjects=").append(String.join(",", tempList));
        }
        if (dataTimeRange != null) {
            dataToHash.append(",dataTimeRange=");
            if (dataTimeRange.getLeft() != null)
                dataToHash.append(dataTimeRange.getLeft()).append("-");
            if (dataTimeRange.getRight() != null)
                dataToHash.append(dataTimeRange.getRight());
        }
        if (locRange != null) {
            dataToHash.append(",locRange=");
            if (locRange.getLeft() != null)
                dataToHash.append(locRange.getLeft()).append("-");
            if (locRange.getRight() != null)
                dataToHash.append(locRange.getRight());
        }
        if (excludeLocRange != null) {
            dataToHash.append(",excludeLocRange=");
            if (excludeLocRange.getLeft() != null)
                dataToHash.append(excludeLocRange.getLeft()).append("-");
            if (excludeLocRange.getRight() != null)
                dataToHash.append(excludeLocRange.getRight());
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
        if (MapUtils.isNotEmpty(excludePartialMatch)) {
            TreeSet<String> fields = new TreeSet<>(excludePartialMatch.keySet());
            dataToHash.append(",excludePartialMatch=(");
            for (String field : fields) {
                Map<String, String> innerMap = excludePartialMatch.get(field);
                TreeSet<String> innerFields = new TreeSet<>(innerMap.keySet());
                dataToHash.append("(");
                for (String innerField : innerFields) {
                    dataToHash.append(innerField).append("=").append(innerMap.get(innerField)).append(",");
                }
                dataToHash.append("),");
            }
            dataToHash.append(")");
        }
        return dataToHash.toString();
    }

    public String generateCacheHash() {
        return CacheHashUtils.generateCacheHash(generateCacheRawString());
    }

    public enum DISTINCT {
        committer,
        author;

        public static DISTINCT fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(DISTINCT.class, st);
        }
    }
}
