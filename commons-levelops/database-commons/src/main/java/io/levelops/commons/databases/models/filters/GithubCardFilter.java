package io.levelops.commons.databases.models.filters;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.caching.CacheHashUtils;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.models.DefaultListRequest;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOrDefault;

@Value
@Builder(toBuilder = true)
public class GithubCardFilter {
    DISTINCT across;
    CALCULATION calculation;
    AGG_INTERVAL aggInterval;
    List<String> projects;
    List<String> projectCreators;
    List<String> organizations;
    List<String> projectStates;
    List<String> columns;
    List<String> cardIds;
    List<String> cardCreators;
    List<String> currentColumns;
    List<String> integrationIds;
    List<String> labels;
    List<String> assignees;
    List<String> repoIds;
    Boolean privateProject;
    Boolean archivedCard;
    List<String> excludeColumns;
    Map<String, SortingOrder> sort;
    Set<UUID> orgProductIds;
    ImmutablePair<Long, Long> issueClosedRange;
    ImmutablePair<Long, Long> issueCreatedRange;

    public enum DISTINCT {
        project,
        organization,
        project_creator,
        card_creator,
        column,
        repo_id,
        label,
        assignee,
        issue_created,
        issue_closed;

        public static DISTINCT fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(DISTINCT.class, st);
        }
    }

    public enum CALCULATION {
        count,
        stage_times_report,
        resolution_time;

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
        if (aggInterval != null)
            dataToHash.append(",aggInterval=").append(aggInterval);
        if (CollectionUtils.isNotEmpty(projects)) {
            ArrayList<String> tempList = new ArrayList<>(projects);
            Collections.sort(tempList);
            dataToHash.append(",projects=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(projectCreators)) {
            ArrayList<String> tempList = new ArrayList<>(projectCreators);
            Collections.sort(tempList);
            dataToHash.append(",projectCreators=").append(String.join(",", tempList));
        }
        if(CollectionUtils.isNotEmpty(orgProductIds)) {
            ArrayList<String> tempList = orgProductIds.stream().sorted().map(UUID::toString)
                    .collect(Collectors.toCollection(ArrayList::new));
            dataToHash.append(",ordProductIds=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(organizations)) {
            ArrayList<String> tempList = new ArrayList<>(organizations);
            Collections.sort(tempList);
            dataToHash.append(",organizations=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(projectStates)) {
            ArrayList<String> tempList = new ArrayList<>(projectStates);
            Collections.sort(tempList);
            dataToHash.append(",projectStates=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(columns)) {
            ArrayList<String> tempList = new ArrayList<>(columns);
            Collections.sort(tempList);
            dataToHash.append(",columns=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(currentColumns)) {
            ArrayList<String> tempList = new ArrayList<>(currentColumns);
            Collections.sort(tempList);
            dataToHash.append(",currentColumns=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(cardIds)) {
            ArrayList<String> tempList = new ArrayList<>(cardIds);
            Collections.sort(tempList);
            dataToHash.append(",cardIds=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(cardCreators)) {
            ArrayList<String> tempList = new ArrayList<>(cardCreators);
            Collections.sort(tempList);
            dataToHash.append(",cardCreators=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            ArrayList<String> tempList = new ArrayList<>(integrationIds);
            Collections.sort(tempList);
            dataToHash.append(",integrationIds=").append(String.join(",", tempList));
        }
        if (privateProject != null)
            dataToHash.append(",privateProject=").append(privateProject);
        if (archivedCard != null)
            dataToHash.append(",archivedCard=").append(archivedCard);
        if (CollectionUtils.isNotEmpty(excludeColumns)) {
            ArrayList<String> tempList = new ArrayList<>(excludeColumns);
            Collections.sort(tempList);
            dataToHash.append(",excludeColumns=").append(String.join(",", tempList));
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
        if (CollectionUtils.isNotEmpty(labels)) {
            ArrayList<String> tempList = new ArrayList<>(labels);
            Collections.sort(tempList);
            dataToHash.append(",labels=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(repoIds)) {
            ArrayList<String> tempList = new ArrayList<>(repoIds);
            Collections.sort(tempList);
            dataToHash.append(",repos=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(assignees)) {
            ArrayList<String> tempList = new ArrayList<>(assignees);
            Collections.sort(tempList);
            dataToHash.append(",assigness=").append(String.join(",", tempList));
        }
        if (issueClosedRange != null) {
            dataToHash.append(",issueClosedRange=");
            if (issueClosedRange.getLeft() != null)
                dataToHash.append(issueClosedRange.getLeft()).append("-");
            if (issueClosedRange.getRight() != null)
                dataToHash.append(issueClosedRange.getRight());
        }
        if (issueCreatedRange != null) {
            dataToHash.append(",issueCreatedRange=");
            if (issueCreatedRange.getLeft() != null)
                dataToHash.append(issueCreatedRange.getLeft()).append("-");
            if (issueCreatedRange.getRight() != null)
                dataToHash.append(issueCreatedRange.getRight());
        }
        return dataToHash.toString();
    }

    public String generateCacheHash() {
        return CacheHashUtils.generateCacheHash(generateCacheRawString());
    }

    public static GithubCardFilter.GithubCardFilterBuilder createGithubCardFilter(DefaultListRequest filter) {
        Boolean isPrivateProject = filter.getFilterValue("private_project", Boolean.class).orElse(null);
        Boolean isArchivedCard = filter.getFilterValue("archived_card", Boolean.class).orElse(null);
        ImmutablePair<Long, Long> issueClosedAtRange = filter.getNumericRangeFilter("issue_closed_at");
        ImmutablePair<Long, Long> issueCreatedAtRange = filter.getNumericRangeFilter("issue_created_at");
        Map<String, Object> filterExclude = MapUtils.emptyIfNull((Map<String, Object>) filter.getFilter().get("exclude"));
        List<String> excludeColumns = (List<String>) filterExclude.getOrDefault("columns", List.of());
        List<String> orgProductIdsList = getListOrDefault(filter.getFilter(), "org_product_ids");
        Set<UUID> orgProductIdsSet = orgProductIdsList.stream().map(UUID::fromString).collect(Collectors.toSet());
        Map<String, SortingOrder> sort = SortingConverter.fromFilter(MoreObjects.firstNonNull(filter.getSort(), List.of()));
        return GithubCardFilter.builder()
                .projects(getListOrDefault(filter.getFilter(), "projects"))
                .projectCreators(getListOrDefault(filter.getFilter(), "project_creators"))
                .organizations(getListOrDefault(filter.getFilter(), "organizations"))
                .projectStates(getListOrDefault(filter.getFilter(), "project_states"))
                .privateProject(isPrivateProject)
                .orgProductIds(orgProductIdsSet)
                .columns(getListOrDefault(filter.getFilter(), "columns"))
                .cardIds(getListOrDefault(filter.getFilter(), "card_ids"))
                .cardCreators(getListOrDefault(filter.getFilter(), "card_creators"))
                .integrationIds(getListOrDefault(filter.getFilter(), "integration_ids"))
                .currentColumns(getListOrDefault(filter.getFilter(), "current_columns"))
                .assignees(getListOrDefault(filter.getFilter(), "assignees"))
                .labels(getListOrDefault(filter.getFilter(), "labels"))
                .repoIds(getListOrDefault(filter.getFilter(), "repo_ids"))
                .archivedCard(isArchivedCard)
                .sort(sort)
                .excludeColumns(excludeColumns)
                .issueClosedRange(issueClosedAtRange)
                .issueCreatedRange(issueCreatedAtRange);
    }

}
