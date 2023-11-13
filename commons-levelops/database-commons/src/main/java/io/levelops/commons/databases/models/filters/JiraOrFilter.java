package io.levelops.commons.databases.models.filters;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

//This class is only a field in the JiraIssuesFilter class and cannot be used by itself.
//It is big enough to warrant its own file; thats all.
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraOrFilter.JiraOrFilterBuilder.class)
public class JiraOrFilter {
    List<String> keys;
    List<String> priorities;
    List<String> statuses;
    List<String> assignees;
    List<String> reporters;
    List<String> issueTypes;
    List<String> parentIssueTypes;
    List<String> fixVersions;
    List<String> versions;
    List<String> projects;
    List<String> components;
    List<String> labels;
    List<String> epics;
    List<String> parentKeys;
    List<String> stages;
    Integer sprintCount;
    List<String> sprintIds;
    List<String> sprintNames;
    List<String> sprintFullNames;
    List<String> sprintStates;
    List<String> resolutions;
    List<String> statusCategories;
    List<String> links;
    List<JiraIssuesFilter.EXTRA_CRITERIA> extraCriteria;
    ImmutablePair<Long, Long> issueCreatedRange;
    ImmutablePair<Long, Long> issueUpdatedRange;
    ImmutablePair<Long, Long> issueResolutionRange;
    ImmutablePair<Long, Long> issueReleasedRange;
    ImmutablePair<Long, Long> issueDueRange;
    ImmutablePair<Long, Long> age;
    Map<String, Object> customFields;
    Map<String, String> storyPoints;
    Map<String, String> parentStoryPoints;
    Map<String, Boolean> missingFields;
    String summary;
    Map<String, Map<String, String>> fieldSize;
    Map<String, Map<String, String>> partialMatch;
    Map<JiraIssuesFilter.EXTRA_CRITERIA, Object> hygieneCriteriaSpecs;

    public String generateCacheRawString() {
        StringBuilder dataToHash = new StringBuilder("orfilter=(");
        if (StringUtils.isNotEmpty(summary)) {
            dataToHash.append("summary=").append(summary);
        }
        if (sprintCount != null && sprintCount > 0) {
            dataToHash.append(",sprintCount=").append(sprintCount);
        }
        if (CollectionUtils.isNotEmpty(keys)) {
            ArrayList<String> tempList = new ArrayList<>(keys);
            Collections.sort(tempList);
            dataToHash.append(",keys=").append(String.join(",", tempList));
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
        if (CollectionUtils.isNotEmpty(assignees)) {
            ArrayList<String> tempList = new ArrayList<>(assignees);
            Collections.sort(tempList);
            dataToHash.append(",assignees=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(reporters)) {
            ArrayList<String> tempList = new ArrayList<>(reporters);
            Collections.sort(tempList);
            dataToHash.append(",reporters=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(issueTypes)) {
            ArrayList<String> tempList = new ArrayList<>(issueTypes);
            Collections.sort(tempList);
            dataToHash.append(",issueTypes=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(parentIssueTypes)) {
            ArrayList<String> tempList = new ArrayList<>(parentIssueTypes);
            Collections.sort(tempList);
            dataToHash.append(",parentIssueTypes=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(fixVersions)) {
            ArrayList<String> tempList = new ArrayList<>(fixVersions);
            Collections.sort(tempList);
            dataToHash.append(",fixVersions=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(versions)) {
            ArrayList<String> tempList = new ArrayList<>(versions);
            Collections.sort(tempList);
            dataToHash.append(",versions=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(stages)) {
            ArrayList<String> tempList = new ArrayList<>(stages);
            Collections.sort(tempList);
            dataToHash.append(",stages=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(projects)) {
            ArrayList<String> tempList = new ArrayList<>(projects);
            Collections.sort(tempList);
            dataToHash.append(",projects=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(components)) {
            ArrayList<String> tempList = new ArrayList<>(components);
            Collections.sort(tempList);
            dataToHash.append(",components=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(labels)) {
            ArrayList<String> tempList = new ArrayList<>(labels);
            Collections.sort(tempList);
            dataToHash.append(",labels=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(epics)) {
            ArrayList<String> tempList = new ArrayList<>(epics);
            Collections.sort(tempList);
            dataToHash.append(",epics=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(links)) {
            ArrayList<String> tempList = new ArrayList<>(links);
            Collections.sort(tempList);
            dataToHash.append(",links=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(sprintIds)) {
            ArrayList<String> tempList = new ArrayList<>(sprintIds);
            Collections.sort(tempList);
            dataToHash.append(",sprintIds=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(sprintNames)) {
            ArrayList<String> tempList = new ArrayList<>(sprintNames);
            Collections.sort(tempList);
            dataToHash.append(",sprintNames=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(sprintFullNames)) {
            ArrayList<String> tempList = new ArrayList<>(sprintFullNames);
            Collections.sort(tempList);
            dataToHash.append(",sprintFullNames=").append(String.join(",", tempList));
        }

        if (CollectionUtils.isNotEmpty(extraCriteria)) {
            List<String> critStr = extraCriteria.stream().map(Enum::toString).sorted().collect(Collectors.toList());
            dataToHash.append(",extraCriteria=").append(String.join(",", critStr));
        }
        if (CollectionUtils.isNotEmpty(sprintStates)) {
            ArrayList<String> tempList = new ArrayList<>(sprintStates);
            Collections.sort(tempList);
            dataToHash.append(",sprintStates=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(resolutions)) {
            ArrayList<String> tempList = new ArrayList<>(resolutions);
            Collections.sort(tempList);
            dataToHash.append(",resolutions=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(statusCategories)) {
            ArrayList<String> tempList = new ArrayList<>(statusCategories);
            Collections.sort(tempList);
            dataToHash.append(",statusCategories=").append(String.join(",", tempList));
        }

        if (issueCreatedRange != null) {
            dataToHash.append(",issueCreatedRange=");
            if (issueCreatedRange.getLeft() != null) {
                dataToHash.append(issueCreatedRange.getLeft()).append("-");
            }
            if (issueCreatedRange.getRight() != null) {
                dataToHash.append(issueCreatedRange.getRight());
            }
        }
        if (issueDueRange != null) {
            dataToHash.append(",issueDueRange=");
            if (issueDueRange.getLeft() != null) {
                dataToHash.append(issueDueRange.getLeft()).append("-");
            }
            if (issueDueRange.getRight() != null) {
                dataToHash.append(issueDueRange.getRight());
            }
        }
        if (issueUpdatedRange != null) {
            dataToHash.append(",issueUpdatedRange=");
            if (issueUpdatedRange.getLeft() != null) {
                dataToHash.append(issueUpdatedRange.getLeft()).append("-");
            }
            if (issueUpdatedRange.getRight() != null) {
                dataToHash.append(issueUpdatedRange.getRight());
            }
        }
        if (issueResolutionRange != null) {
            dataToHash.append(",issueResolutionRange=");
            if (issueResolutionRange.getLeft() != null) {
                dataToHash.append(issueResolutionRange.getLeft()).append("-");
            }
            if (issueResolutionRange.getRight() != null) {
                dataToHash.append(issueResolutionRange.getRight());
            }
        }

        if (issueReleasedRange != null) {
            dataToHash.append(",issueReleasedRange=");
            if (issueReleasedRange.getLeft() != null) {
                dataToHash.append(issueReleasedRange.getLeft()).append("-");
            }
            if (issueReleasedRange.getRight() != null) {
                dataToHash.append(issueReleasedRange.getRight());
            }
        }

        if (age != null) {
            dataToHash.append(",age=");
            if (age.getLeft() != null) {
                dataToHash.append(age.getLeft()).append("-");
            }
            if (age.getRight() != null) {
                dataToHash.append(age.getRight());
            }
        }
        if (MapUtils.isNotEmpty(customFields)) {
            hashDataMapOfStrings(dataToHash, "customfields", customFields);
        }
        if (MapUtils.isNotEmpty(storyPoints)) {
            TreeSet<String> fields = new TreeSet<>(storyPoints.keySet());
            dataToHash.append(",storyPoints=(");
            for (String field : fields) {
                String data = storyPoints.get(field);
                dataToHash.append(field).append("=").append(data).append(",");
            }
            dataToHash.append(")");
        }
        if (MapUtils.isNotEmpty(parentStoryPoints)) {
            TreeSet<String> fields = new TreeSet<>(parentStoryPoints.keySet());
            dataToHash.append(",parentStoryPoints=(");
            for (String field : fields) {
                String data = parentStoryPoints.get(field);
                dataToHash.append(field).append("=").append(data).append(",");
            }
            dataToHash.append(")");
        }
        if (MapUtils.isNotEmpty(missingFields)) {
            TreeSet<String> fields = new TreeSet<>(missingFields.keySet());
            dataToHash.append(",missingFields=(");
            for (String field : fields) {
                Boolean data = missingFields.get(field);
                dataToHash.append(field).append("=").append(data).append(",");
            }
            dataToHash.append(")");
        }
        if (MapUtils.isNotEmpty(hygieneCriteriaSpecs)) {
            TreeSet<String> fields = hygieneCriteriaSpecs.keySet().stream().map(Enum::toString).collect(Collectors.toCollection(TreeSet::new));
            dataToHash.append(",hygieneCriteriaSpecs=(");
            for (String field : fields) {
                Object data = hygieneCriteriaSpecs.get(JiraIssuesFilter.EXTRA_CRITERIA.fromString(field));
                dataToHash.append(field).append("=").append(NumberUtils.toInt(String.valueOf(data))).append(",");
            }
            dataToHash.append(")");
        }

        if (MapUtils.isNotEmpty(fieldSize)) {
            TreeSet<String> fields = new TreeSet<>(fieldSize.keySet());
            dataToHash.append(",fieldSize=(");
            for (String field : fields) {
                Map<String, String> innerMap = fieldSize.get(field);
                TreeSet<String> innerFields = new TreeSet<>(innerMap.keySet());
                dataToHash.append("(");
                for (String innerField : innerFields) {
                    dataToHash.append(innerField).append("=").append(innerMap.get(innerField)).append(",");
                }
                dataToHash.append("),");
            }
            dataToHash.append(")");
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

        return dataToHash.append(")").toString();
    }

    private void hashDataMapOfStrings(StringBuilder dataToHash, String fieldName, Map<String, ?> map) {
        if (!MapUtils.isNotEmpty(map)) {
            return;
        }
        TreeSet<String> fields = new TreeSet<>(map.keySet());
        dataToHash.append(",").append(fieldName).append("=(");
        for (String field : fields) {
            String data = String.valueOf(map.get(field));
            dataToHash.append(field).append("=").append(data).append(",");
        }
        dataToHash.append(")");
    }
}