package io.levelops.commons.databases.models.filters;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.caching.CacheHashUtils;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.web.exceptions.BadRequestException;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getExcludedRange;
import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOrDefault;
import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getTimeRange;
import static io.levelops.commons.databases.models.filters.VCS_TYPE.parseFromFilter;

@Log4j2
@Value
@Builder(toBuilder = true)
public class ScmCommitFilter {
    DISTINCT across;
    Integer acrossLimit;
    CALCULATION calculation;
    List<String> commitShas;
    List<String> repoIds;
    List<String> projects;
    List<String> committers;
    List<String> technologies;
    List<String> authors;
    List<String> integrationIds;
    List<String> commitBranches;
    List<String> daysOfWeek;
    List<VCS_TYPE> vcsTypes;
    ImmutablePair<Long, Long> committedAtRange;
    ImmutablePair<Long, Long> commitPushedAtRange;
    ImmutablePair<Long, Long> createdAtRange;
    Map<String, Map<String, String>> partialMatch;
    Map<String, SortingOrder> sort;
    Set<UUID> orgProductIds;
    AGG_INTERVAL aggInterval;
    List<String> fileTypes;
    List<String> codeCategory;
    String codeChangeUnit;
    Map<String, String> codeChangeSizeConfig;
    Map<String, String> codeChangeSize;
    List<String> codeChanges;
    Long legacyCodeConfig;
    Boolean ignoreFilesJoin;
    ImmutablePair<Long, Long> daysCountRange;
    ImmutablePair<Long, Long> locRange;
    Boolean returnHasIssueKeys;
    List<UUID> ids;
    List<String> commitTitles;


    List<String> excludeCommitShas;
    List<String> excludeRepoIds;
    List<String> excludeProjects;
    List<String> excludeCommitters;
    List<String> excludeCommitBranches;
    List<String> excludeCommitTitles;
    List<String> excludeAuthors;
    List<String> excludeFileTypes;
    List<String> excludeTechnologies;
    List<String> excludeDaysOfWeek;
    Map<String, Map<String, String>> excludePartialMatch;
    ImmutablePair<Long, Long> excludeLocRange;
    Boolean isApplyOuOnVelocityReport;

    public enum DISTINCT {
        committer,
        author,
        commit_branch,
        repo_id,
        project,
        technology,
        vcs_type,
        file_type,
        code_change,
        code_category,
        //this is time based
        trend;

        public static DISTINCT fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(DISTINCT.class, st);
        }
    }

    public enum CALCULATION {
        commit_count,
        commit_days,
        commit_count_only,
        count; // just a count of rows

        public static CALCULATION fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(CALCULATION.class, st);
        }
    }

    public String generateCacheRawString() {
        StringBuilder dataToHash = new StringBuilder();
        if (across != null)
            dataToHash.append("across=").append(across);
        if (calculation != null)
            dataToHash.append(",calculation=").append(calculation);
        if (legacyCodeConfig != null)
            dataToHash.append(",legacyCodeConfig=").append(legacyCodeConfig);
        if (codeChangeUnit != null)
            dataToHash.append(",codeChangeUnit=").append(codeChangeUnit);
        if (daysCountRange != null) {
            dataToHash.append(",daysCountRange=");
            if (daysCountRange.getLeft() != null)
                dataToHash.append(daysCountRange.getLeft()).append("-");
            if (daysCountRange.getRight() != null)
                dataToHash.append(daysCountRange.getRight());
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
        if (CollectionUtils.isNotEmpty(commitShas)) {
            ArrayList<String> tempList = new ArrayList<>(commitShas);
            Collections.sort(tempList);
            dataToHash.append(",commitShas=").append(String.join(",", tempList));
        }
        if (MapUtils.isNotEmpty(codeChangeSizeConfig)) {
            TreeSet<String> fields = new TreeSet<>(codeChangeSizeConfig.keySet());
            dataToHash.append(",codeChangeSizeConfig=(");
            for (String field : fields) {
                dataToHash.append(field.toLowerCase(Locale.ROOT)).append("=")
                        .append(codeChangeSizeConfig.get(field).toLowerCase(Locale.ROOT));
            }
            dataToHash.append(")");
        }
        if (MapUtils.isNotEmpty(codeChangeSize)) {
            TreeSet<String> fields = new TreeSet<>(codeChangeSize.keySet());
            dataToHash.append(",codeChangeSize=(");
            for (String field : fields) {
                dataToHash.append(field.toLowerCase(Locale.ROOT)).append("=")
                        .append(codeChangeSize.get(field).toLowerCase(Locale.ROOT));
            }
            dataToHash.append(")");
        }
        if (CollectionUtils.isNotEmpty(orgProductIds)) {
            ArrayList<String> tempList = orgProductIds.stream().sorted().map(UUID::toString)
                    .collect(Collectors.toCollection(ArrayList::new));
            dataToHash.append(",ordProductIds=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(codeCategory)) {
            ArrayList<String> tempList = new ArrayList<>(codeCategory);
            Collections.sort(tempList);
            dataToHash.append(",codeCategory=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(technologies)) {
            ArrayList<String> tempList = new ArrayList<>(technologies);
            Collections.sort(tempList);
            dataToHash.append(",technologies=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeTechnologies)) {
            ArrayList<String> tempList = new ArrayList<>(excludeTechnologies);
            Collections.sort(tempList);
            dataToHash.append(",excludeTechnologies=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(codeChanges)) {
            ArrayList<String> tempList = new ArrayList<>(codeChanges);
            Collections.sort(tempList);
            dataToHash.append(",codeChanges=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(repoIds)) {
            ArrayList<String> tempList = new ArrayList<>(repoIds);
            Collections.sort(tempList);
            dataToHash.append(",repoIds=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(fileTypes)) {
            ArrayList<String> tempList = new ArrayList<>(fileTypes);
            Collections.sort(tempList);
            dataToHash.append(",fileTypes=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(fileTypes)) {
            ArrayList<String> tempList = new ArrayList<>(fileTypes);
            Collections.sort(tempList);
            dataToHash.append(",fileTypes=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(vcsTypes)) {
            ArrayList<VCS_TYPE> tempList = new ArrayList<>(vcsTypes);
            Collections.sort(tempList);
            String vcsTypesStr = tempList.stream()
                    .map(VCS_TYPE::name)
                    .collect(Collectors.joining(","));
            dataToHash.append(",vcsTypes=").append(vcsTypesStr);
        }
        if (CollectionUtils.isNotEmpty(projects)) {
            ArrayList<String> tempList = new ArrayList<>(projects);
            Collections.sort(tempList);
            dataToHash.append(",projects=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(daysOfWeek)) {
            ArrayList<String> tempList = new ArrayList<>(daysOfWeek);
            Collections.sort(tempList);
            dataToHash.append(",daysOfWeek=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(committers)) {
            ArrayList<String> tempList = new ArrayList<>(committers);
            Collections.sort(tempList);
            dataToHash.append(",committers=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(authors)) {
            ArrayList<String> tempList = new ArrayList<>(authors);
            Collections.sort(tempList);
            dataToHash.append(",authors=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            ArrayList<String> tempList = new ArrayList<>(integrationIds);
            Collections.sort(tempList);
            dataToHash.append(",integrationIds=").append(String.join(",", tempList));
        }
        if(CollectionUtils.isNotEmpty(commitBranches)){
            ArrayList<String> tempList = new ArrayList<>(commitBranches);
            Collections.sort(tempList);
            dataToHash.append(",commitBranches=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeCommitShas)) {
            ArrayList<String> tempList = new ArrayList<>(excludeCommitShas);
            Collections.sort(tempList);
            dataToHash.append(",excludeCommitShas=").append(String.join(",", tempList));
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
        if (CollectionUtils.isNotEmpty(excludeCommitters)) {
            ArrayList<String> tempList = new ArrayList<>(excludeCommitters);
            Collections.sort(tempList);
            dataToHash.append(",excludeCommitters=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeCommitBranches)) {
            ArrayList<String> tempList = new ArrayList<>(excludeCommitBranches);
            Collections.sort(tempList);
            dataToHash.append(",excludeCommitBranches=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeAuthors)) {
            ArrayList<String> tempList = new ArrayList<>(excludeAuthors);
            Collections.sort(tempList);
            dataToHash.append(",excludeAuthors=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeDaysOfWeek)) {
            ArrayList<String> tempList = new ArrayList<>(excludeDaysOfWeek);
            Collections.sort(tempList);
            dataToHash.append(",excludeDaysOfWeek=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeFileTypes)) {
            ArrayList<String> tempList = new ArrayList<>(excludeFileTypes);
            Collections.sort(tempList);
            dataToHash.append(",excludeFileTypes=").append(String.join(",", tempList));
        }
        if (aggInterval != null)
            dataToHash.append(",aggInterval=").append(aggInterval);
        if (committedAtRange != null) {
            dataToHash.append(",committedAtRange=");
            if (committedAtRange.getLeft() != null)
                dataToHash.append(committedAtRange.getLeft()).append("-");
            if (committedAtRange.getRight() != null)
                dataToHash.append(committedAtRange.getRight());
        }
        if (commitPushedAtRange != null) {
            dataToHash.append(",commitPushedAtRange=");
            if (commitPushedAtRange.getLeft() != null)
                dataToHash.append(commitPushedAtRange.getLeft()).append("-");
            if (commitPushedAtRange.getRight() != null)
                dataToHash.append(commitPushedAtRange.getRight());
        }
        if (createdAtRange != null) {
            dataToHash.append(",createdAtRange=");
            if (createdAtRange.getLeft() != null)
                dataToHash.append(createdAtRange.getLeft()).append("-");
            if (createdAtRange.getRight() != null)
                dataToHash.append(createdAtRange.getRight());
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
        if (MapUtils.isNotEmpty(sort)) {
            TreeSet<String> fields = new TreeSet<>(sort.keySet());
            dataToHash.append(",sort=(");
            for (String field : fields) {
                dataToHash.append(field.toLowerCase(Locale.ROOT)).append("=")
                        .append(sort.get(field).toString().toLowerCase(Locale.ROOT));
            }
            dataToHash.append(")");
        }
        if (ignoreFilesJoin != null) {
            dataToHash.append(",ignoreFilesJoin=").append(ignoreFilesJoin);
        }
        if (returnHasIssueKeys != null) {
            dataToHash.append(",returnHasIssueKeys=").append(returnHasIssueKeys);
        }
        if (CollectionUtils.isNotEmpty(ids)) {
            ArrayList<String> tempList = ids.stream().sorted().map(UUID::toString)
                    .collect(Collectors.toCollection(ArrayList::new));
            dataToHash.append(",ids=").append(String.join(",", tempList));
        }
        return dataToHash.toString();
    }

    public String generateCacheHash() {
        return CacheHashUtils.generateCacheHash(generateCacheRawString());
    }

    @SuppressWarnings("unchecked")
    public static ScmCommitFilter fromDefaultListRequest(DefaultListRequest filter, DISTINCT across,
                                                         CALCULATION calculation, Map<String, Map<String, String>> partialMatchMap) throws BadRequestException {
        Map<String, String> codeChangeConfigMap = MapUtils.emptyIfNull((Map<String, String>) filter.getFilter().get("code_change_size_config"));
        Map<String, String> codeChangeSizeMap = MapUtils.emptyIfNull((Map<String, String>) filter.getFilter().get("code_change_size"));
        Map<String, String> daysCountRange = filter.getFilterValue("days_count", Map.class)
                .orElse(Map.of());
        Map<String, String> locRange = filter.getFilterValue("loc", Map.class)
                .orElse(Map.of());
        final Long daysCountStart = daysCountRange.get("$gt") != null ? Long.valueOf(daysCountRange.get("$gt")) : null;
        final Long daysCountEnd = daysCountRange.get("$lt") != null ? Long.valueOf(daysCountRange.get("$lt")) : null;
        final Long locLowerRange = locRange.get("$gt") != null ? Long.valueOf(locRange.get("$gt")) : null;
        final Long locUpperRange = locRange.get("$lt") != null ? Long.valueOf(locRange.get("$lt")) : null;
        List<String> orgProductIdsList = getListOrDefault(filter.getFilter(), "org_product_ids");
        Set<UUID> orgProductIdsSet = orgProductIdsList.stream().map(UUID::fromString).collect(Collectors.toSet());
        Map<String, SortingOrder> sorting = SortingConverter.fromFilter(MoreObjects.firstNonNull(filter.getSort(), List.of()));
        Map<String, Object> excludedFields = (Map<String, Object>) filter.getFilter()
                .getOrDefault("exclude", Map.of());
        AGG_INTERVAL aggInterval = ((calculation != null) && calculation.equals(CALCULATION.commit_days) && filter.getAggInterval().equals("day")) ? AGG_INTERVAL.week :
                MoreObjects.firstNonNull(AGG_INTERVAL.fromString(filter.getAggInterval()), AGG_INTERVAL.day);
        ScmCommitFilter.ScmCommitFilterBuilder bldr = ScmCommitFilter.builder()
                .committedAtRange(getTimeRange(filter, "committed_at"))
                .commitPushedAtRange(getTimeRange(filter, "commit_pushed_at"))
                .createdAtRange(getTimeRange(filter, "created_at"))
                .across(across)
                .calculation(calculation)
                .aggInterval(aggInterval)
                .ignoreFilesJoin(!(Boolean) filter.getFilter().getOrDefault("include_metrics", false))
                .repoIds(getListOrDefault(filter.getFilter(), "repo_ids"))
                .vcsTypes(parseFromFilter(filter.getFilter()))
                .fileTypes(getListOrDefault(filter.getFilter(), "file_types"))
                .projects(getListOrDefault(filter.getFilter(), "projects"))
                .authors(getListOrDefault(filter.getFilter(), "authors"))
                .daysOfWeek(getListOrDefault(filter.getFilter(), "days_of_week"))
                .committers(getListOrDefault(filter.getFilter(), "committers"))
                .commitShas(getListOrDefault(filter.getFilter(), "commit_shas"))
                .integrationIds(getListOrDefault(filter.getFilter(), "integration_ids"))
                .commitBranches(getListOrDefault(filter.getFilter(), "commit_branches"))
                .codeChangeSizeConfig(codeChangeConfigMap)
                .technologies(getListOrDefault(filter.getFilter(), "technologies"))
                .excludeTechnologies(getListOrDefault(excludedFields, "technologies"))
                .codeChangeSize(codeChangeSizeMap)
                .codeChangeUnit(StringUtils.defaultIfEmpty((String) filter.getFilter().get("code_change_size_unit"), "lines"))
                .codeChanges(getListOrDefault(filter, "code_change_sizes"))
                .legacyCodeConfig(Long.valueOf((String) filter.getFilter().getOrDefault("legacy_update_interval_config",
                        String.valueOf(Instant.now().minus(60, ChronoUnit.DAYS).getEpochSecond()))))
                .daysCountRange(ImmutablePair.of(daysCountStart, daysCountEnd))
                .locRange(ImmutablePair.of(locLowerRange, locUpperRange))
                .excludeLocRange(getExcludedRange(filter, "", "loc"))
                .excludeRepoIds(getListOrDefault(excludedFields, "repo_ids"))
                .excludeFileTypes(getListOrDefault(excludedFields, "file_types"))
                .excludeProjects(getListOrDefault(excludedFields, "projects"))
                .excludeAuthors(getListOrDefault(excludedFields, "authors"))
                .excludeDaysOfWeek(getListOrDefault(excludedFields, "days_of_week"))
                .excludeCommitters(getListOrDefault(excludedFields, "committers"))
                .excludeCommitBranches(getListOrDefault(excludedFields,"commit_branches"))
                .excludeCommitShas(getListOrDefault(excludedFields, "commit_shas"))
                .isApplyOuOnVelocityReport(filter.getApplyOuOnVelocityReport())
                .partialMatch(partialMatchMap)
                .sort(sorting)
                .orgProductIds(orgProductIdsSet)
                .returnHasIssueKeys((Boolean) filter.getFilter().getOrDefault("return_has_issue_keys", false));

        if (across != null) {
            bldr.across(across);
        }
        if (calculation != null) {
            bldr.calculation(calculation);
        }
        ScmCommitFilter commitFilter = bldr.build();
        log.info("commitFilter = {}", commitFilter);
        return commitFilter;

    }
}
