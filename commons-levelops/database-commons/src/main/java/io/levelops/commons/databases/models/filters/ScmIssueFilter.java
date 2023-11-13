package io.levelops.commons.databases.models.filters;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.caching.CacheHashUtils;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.web.exceptions.BadRequestException;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOrDefault;
import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getTimeRange;
import static io.levelops.commons.caching.CacheHashUtils.hashData;
import static io.levelops.commons.caching.CacheHashUtils.hashDataMapOfMaps;
import static io.levelops.commons.caching.CacheHashUtils.hashDataMapOfStrings;

@Log4j2
@Value
@Builder(toBuilder = true)
public class ScmIssueFilter {
    DISTINCT across;
    CALCULATION calculation;
    List<EXTRA_CRITERIA> extraCriteria;
    AGG_INTERVAL aggInterval;
    List<String> repoIds;
    List<String> projects;
    List<String> creators;
    List<String> assignees;
    List<String> states;
    List<String> labels;
    List<String> integrationIds;
    ImmutablePair<Long, Long> issueCreatedRange;
    ImmutablePair<Long, Long> issueClosedRange;
    ImmutablePair<Long, Long> issueUpdatedRange;
    ImmutablePair<Long, Long> firstCommentAtRange;
    Map<String, Map<String, String>> partialMatch;
    Map<String, SortingOrder> sort;
    String title;
    Set<UUID> orgProductIds;

    List<String> excludeRepoIds;
    List<String> excludeProjects;
    List<String> excludeCreators;
    List<String> excludeAssignees;
    List<String> excludeStates;
    List<String> excludeLabels;

    public enum DISTINCT {
        repo_id,
        project,
        state,
        label,
        creator,
        assignee,
        //these are time based
        issue_created,
        issue_updated,
        issue_closed,
        first_comment;

        public static DISTINCT fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(DISTINCT.class, st);
        }
    }

    public enum CALCULATION {
        response_time,
        resolution_time,
        count; // just a count of rows

        public static CALCULATION fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(CALCULATION.class, st);
        }
    }

    //used to be called hygiene queries
    public enum EXTRA_CRITERIA {
        idle, // no action for 7 days
        no_response,
        no_assignees,
        no_labels,
        missed_response_time; // first comment took longer than 1 day

        public static EXTRA_CRITERIA fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(EXTRA_CRITERIA.class, st);
        }
    }


    public String generateCacheRawString() {
        StringBuilder dataToHash = new StringBuilder();
        if (across != null)
            dataToHash.append("across=").append(across);
        if (calculation != null)
            dataToHash.append(",calculation=").append(calculation);
        if (title != null)
            dataToHash.append(",title=").append(title);
        if (aggInterval!= null)
            dataToHash.append(",aggInterval=").append(aggInterval);
        if (CollectionUtils.isNotEmpty(extraCriteria)) {
            List<String> tempList = extraCriteria.stream().map(Enum::toString).sorted().collect(Collectors.toList());
            dataToHash.append(",extraCriteria=").append(String.join(",", tempList));
        }
        if(CollectionUtils.isNotEmpty(orgProductIds)) {
            ArrayList<String> tempList = orgProductIds.stream().sorted().map(UUID::toString)
                    .collect(Collectors.toCollection(ArrayList::new));
            dataToHash.append(",ordProductIds=").append(String.join(",", tempList));
        }
        hashData(dataToHash, "issueUpdatedRange", issueUpdatedRange);
        hashData(dataToHash, "issueCreatedRange", issueCreatedRange);
        hashData(dataToHash, "issueClosedRange", issueClosedRange);
        hashData(dataToHash, "firstCommentAtRange", firstCommentAtRange);
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
        if (CollectionUtils.isNotEmpty(creators)) {
            ArrayList<String> tempList = new ArrayList<>(creators);
            Collections.sort(tempList);
            dataToHash.append(",creators=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(assignees)) {
            ArrayList<String> tempList = new ArrayList<>(assignees);
            Collections.sort(tempList);
            dataToHash.append(",assignees=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(states)) {
            ArrayList<String> tempList = new ArrayList<>(states);
            Collections.sort(tempList);
            dataToHash.append(",states=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(labels)) {
            ArrayList<String> tempList = new ArrayList<>(labels);
            Collections.sort(tempList);
            dataToHash.append(",labels=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            ArrayList<String> tempList = new ArrayList<>(integrationIds);
            Collections.sort(tempList);
            dataToHash.append(",integrationIds=").append(String.join(",", tempList));
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
        if (CollectionUtils.isNotEmpty(excludeCreators)) {
            ArrayList<String> tempList = new ArrayList<>(excludeCreators);
            Collections.sort(tempList);
            dataToHash.append(",excludeCreators=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeAssignees)) {
            ArrayList<String> tempList = new ArrayList<>(excludeAssignees);
            Collections.sort(tempList);
            dataToHash.append(",excludeAssignees=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeLabels)) {
            ArrayList<String> tempList = new ArrayList<>(excludeLabels);
            Collections.sort(tempList);
            dataToHash.append(",excludeLabels=").append(String.join(",", tempList));
        }
        hashDataMapOfMaps(dataToHash, "partialMatch", partialMatch);
        if (MapUtils.isNotEmpty(sort))
            hashDataMapOfStrings(dataToHash, "sort", sort);

        if (CollectionUtils.isNotEmpty(excludeStates)) {
            ArrayList<String> tempList = new ArrayList<>(excludeStates);
            Collections.sort(tempList);
            dataToHash.append(",excludeStates=").append(String.join(",", tempList));
        }
        return dataToHash.toString();
    }

    public String generateCacheHash() {
        return CacheHashUtils.generateCacheHash(generateCacheRawString());
    }

    @SuppressWarnings("unchecked")
    public static ScmIssueFilter fromDefaultListRequest(DefaultListRequest filter, DISTINCT across,
                                                        CALCULATION calculation,
                                                        Map<String, Map<String, String>> partialMatchMap, Map<String, SortingOrder> sorting) throws BadRequestException {
        ImmutablePair<Long, Long> issueCreatedAtRange = getTimeRange(filter, "issue_created_at");
        ImmutablePair<Long, Long> issueClosedAtRange = getTimeRange(filter, "issue_closed_at");
        ImmutablePair<Long, Long> issueUpdatedRange = getTimeRange(filter, "issue_updated_at");
        ImmutablePair<Long, Long> firstCommentAtRange = getTimeRange(filter, "first_comment_at");
        List<String> orgProductIdsList = getListOrDefault(filter.getFilter(), "org_product_ids");
        Set<UUID> orgProductIdsSet = orgProductIdsList.stream().map(UUID::fromString).collect(Collectors.toSet());
        Map<String, Object> excludedFields = (Map<String, Object>) filter.getFilter()
                .getOrDefault("exclude", Map.of());
        Map<String, Object> filterExclude = MapUtils.emptyIfNull((Map<String, Object>) filter.getFilter().get("exclude"));
        List<String> excludeStates = (List<String>) filterExclude.getOrDefault("states", List.of());
        ScmIssueFilter.ScmIssueFilterBuilder bldr = ScmIssueFilter.builder()
                .issueCreatedRange(issueCreatedAtRange)
                .across(MoreObjects.firstNonNull(
                        ScmIssueFilter.DISTINCT.fromString(
                                filter.getAcross()),
                        ScmIssueFilter.DISTINCT.creator))
                .calculation(ScmIssueFilter.CALCULATION.count)
                .extraCriteria(MoreObjects.firstNonNull(
                        getListOrDefault(filter.getFilter(), "hygiene_types"),
                        List.of())
                        .stream()
                        .map(String::valueOf)
                        .map(ScmIssueFilter.EXTRA_CRITERIA::fromString)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .repoIds(getListOrDefault(filter.getFilter(), "repo_ids"))
                .projects(getListOrDefault(filter.getFilter(), "projects"))
                .creators(getListOrDefault(filter.getFilter(), "creators"))
                .assignees(getListOrDefault(filter.getFilter(), "assignees"))
                .labels(getListOrDefault(filter.getFilter(), "labels"))
                .states(getListOrDefault(filter.getFilter(), "states"))
                .integrationIds(getListOrDefault(filter.getFilter(), "integration_ids"))
                .excludeRepoIds(getListOrDefault(excludedFields, "repo_ids"))
                .excludeProjects(getListOrDefault(excludedFields, "projects"))
                .excludeCreators(getListOrDefault(excludedFields, "creators"))
                .excludeAssignees(getListOrDefault(excludedFields, "assignees"))
                .excludeStates(getListOrDefault(excludedFields, "states"))
                .excludeLabels(getListOrDefault(excludedFields, "labels"))
                .title((String) filter.getFilter().getOrDefault("title", null))
                .orgProductIds(orgProductIdsSet)
                .partialMatch(partialMatchMap)
                .excludeStates(excludeStates)
                .issueUpdatedRange(issueUpdatedRange)
                .issueClosedRange(issueClosedAtRange)
                .firstCommentAtRange(firstCommentAtRange)
                .aggInterval(MoreObjects.firstNonNull(
                        AGG_INTERVAL.fromString(filter.getAggInterval()), AGG_INTERVAL.day))
                .sort(sorting);
        if(across != null) {
            bldr.across(across);
        }
        if(calculation != null) {
            bldr.calculation(calculation);
        }
        ScmIssueFilter issueFilter = bldr.build();
        log.info("scmIssueFilter = {}", issueFilter);
        return issueFilter;

    }
}
