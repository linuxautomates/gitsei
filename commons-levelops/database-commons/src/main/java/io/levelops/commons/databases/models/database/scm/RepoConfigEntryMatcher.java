package io.levelops.commons.databases.models.database.scm;

import io.levelops.commons.databases.models.database.IntegrationConfig;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RepoConfigEntryMatcher {
    private static final boolean DEFAULT_NOT_CASE_SENSETIVE = false;
    private final Map<String, String> prefixToRepoIdMap;
    private final List<String> prefixSortedByLengthDesc;
    private final boolean caseSensitive;

    public RepoConfigEntryMatcher(final List<IntegrationConfig.RepoConfigEntry> configEntries, final boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
        if(CollectionUtils.isEmpty(configEntries)) {
            prefixToRepoIdMap = Map.of();
            prefixSortedByLengthDesc = List.of();
            return;
        }
        this.prefixSortedByLengthDesc = configEntries.stream()
                .filter(e -> (StringUtils.isNotBlank(e.getRepoId()) && StringUtils.isNotBlank(e.getPathPrefix())))
                .map(e -> (caseSensitive) ?e.getPathPrefix() : e.getPathPrefix().toLowerCase())
                .sorted((s1,s2)-> s2.length() - s1.length())
                .distinct()
                .collect(Collectors.toList());
        this.prefixToRepoIdMap = configEntries.stream()
                .filter(e -> (StringUtils.isNotBlank(e.getRepoId()) && StringUtils.isNotBlank(e.getPathPrefix())))
                .collect(Collectors.toMap(e -> (caseSensitive) ? e.getPathPrefix() :  e.getPathPrefix().toLowerCase(), e-> e.getRepoId(), (a,b)->a));
    }
    public RepoConfigEntryMatcher(final List<IntegrationConfig.RepoConfigEntry> configEntries) {
        this(configEntries, DEFAULT_NOT_CASE_SENSETIVE);
    }
    public String matchPrefix(String filePath) {
        if(CollectionUtils.isEmpty(prefixSortedByLengthDesc)) {
            return null;
        }
        if(StringUtils.isBlank(filePath)) {
            return null;
        }
        String effectiveFilePath = (caseSensitive) ? filePath : filePath.toLowerCase();
        for(String currentPrefix : prefixSortedByLengthDesc) {
            if(effectiveFilePath.startsWith(currentPrefix)) {
                return prefixToRepoIdMap.getOrDefault(currentPrefix, null);
            }
        }
        return null;
    }
}
