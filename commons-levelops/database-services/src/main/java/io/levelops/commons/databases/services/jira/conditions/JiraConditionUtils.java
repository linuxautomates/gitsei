package io.levelops.commons.databases.services.jira.conditions;

import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraStatus;
import io.levelops.commons.databases.models.database.jira.DbJiraStatusMetadata;
import io.levelops.commons.databases.models.database.jira.VelocityStageTime;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.response.VelocityStageResult;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class JiraConditionUtils {
    public static final String DONE_STATUS_CATEGORY = "DONE";

    public static String getRangeCondition(String key, Integer lt, Integer gt, Map<String, Object> params, String issueTblQualifier) {
        key = issueTblQualifier + key;
        String keyName = key.replaceAll("[^A-Za-z0-9_]", "");
        String condition = "int4range(:" + keyName + "_gt,:" + keyName + "_lt) @> length (" + key + ")";
        params.put(keyName + "_gt", gt);
        params.put(keyName + "_lt", lt);
        return condition;
    }

    @NotNull
    public static LinkedHashMap<String, List<String>> getStageStatusesMap(List<VelocityConfigDTO.Stage> devCustomStages) {
        return devCustomStages
                .stream()
                .filter(stage ->
                        stage.getEvent().getType().equals(VelocityConfigDTO.EventType.JIRA_STATUS)
                                || stage.getEvent().getType().equals(VelocityConfigDTO.EventType.JIRA_RELEASE)
                )
                .collect(Collectors.toMap(
                        VelocityConfigDTO.Stage::getName,
                        x -> x.getEvent().getType().equals(VelocityConfigDTO.EventType.JIRA_RELEASE) ? List.of("$$RELEASE$$") : x.getEvent().getValues(),
                        (oldKey, newKey) -> oldKey,
                        LinkedHashMap::new)
                );
    }

    @NotNull
    public static List<String> getAllVelocityStageStatuses(List<VelocityConfigDTO.Stage> stages) {
        return stages
                .stream()
                .map(VelocityConfigDTO.Stage::getEvent)
                .filter(event -> event.getType().equals(VelocityConfigDTO.EventType.JIRA_STATUS))
                .flatMap(event -> event.getValues().stream())
                .collect(Collectors.toList());
    }

    public static List<VelocityConfigDTO.Stage> getSortedDevStages(VelocityConfigDTO velocityConfigDTO) {
        List<VelocityConfigDTO.Stage> developmentCustomStages = new ArrayList<>();
        List<VelocityConfigDTO.Stage> preDevSortedStages;
        List<VelocityConfigDTO.Stage> postDevSortedStages;
        if (CollectionUtils.isNotEmpty(velocityConfigDTO.getPreDevelopmentCustomStages())) {
            preDevSortedStages = velocityConfigDTO.getPreDevelopmentCustomStages()
                    .stream()
                    .sorted(Comparator.comparing(VelocityConfigDTO.Stage::getOrder))
                    .collect(Collectors.toList());
            developmentCustomStages.addAll(preDevSortedStages);
        }
        if (CollectionUtils.isNotEmpty(velocityConfigDTO.getPostDevelopmentCustomStages())) {
            postDevSortedStages = velocityConfigDTO.getPostDevelopmentCustomStages()
                    .stream()
                    .sorted(Comparator.comparing(VelocityConfigDTO.Stage::getOrder))
                    .collect(Collectors.toList());
            developmentCustomStages.addAll(postDevSortedStages);
        }
        return developmentCustomStages;
    }

    public static List<DbJiraIssue> getDbJiraIssuesWithVelocityStages(JiraIssuesFilter jiraIssuesFilter,
                                                                      List<DbJiraIssue> results,
                                                                      VelocityConfigDTO velocityConfigDTO,
                                                                      List<DbJiraStatusMetadata.IntegStatusCategoryMetadata> finalStatusCategoryMetadataList,
                                                                      boolean useEs) {
        return getDbJiraIssuesWithVelocityStages(jiraIssuesFilter, results, velocityConfigDTO, finalStatusCategoryMetadataList, null, false, useEs);
    }
    public static List<DbJiraIssue> getDbJiraIssuesWithVelocityStages(JiraIssuesFilter jiraIssuesFilter,
                                                                      List<DbJiraIssue> results,
                                                                      VelocityConfigDTO velocityConfigDTO,
                                                                      List<DbJiraStatusMetadata.IntegStatusCategoryMetadata> finalStatusCategoryMetadataList,
                                                                      Map<String, DbJiraIssue> jiraReleaseMap,
                                                                      boolean needRelease,
                                                                      boolean useEs) {
        List<VelocityConfigDTO.Stage> developmentCustomStages = getSortedDevStages(velocityConfigDTO);
        Map<String, List<String>> velocityStageStatusesMap = JiraConditionUtils.getStageStatusesMap(developmentCustomStages);
        Map<String, VelocityConfigDTO.Stage> velocityStageNameStageMap = developmentCustomStages.stream().collect(Collectors.toMap(stage -> stage.getName(), stage -> stage));
        List<String> allVelocityStageStatuses = JiraConditionUtils.getAllVelocityStageStatuses(developmentCustomStages);
        List<DbJiraIssue> dbJiraIssues = results.stream().map(dbJiraIssue -> {
            List<DbJiraStatus> allDbJiraStatuses = dbJiraIssue.getStatuses(); // PROP-101 since we are already excluding Done statuses from Other using Ignore_Terminal_Stage - we want to use all statuses for the calculation
            DbJiraIssue.DbJiraIssueBuilder dbJiraIssueBuilder = dbJiraIssue.toBuilder();
            List<VelocityStageTime> velocityStages = new ArrayList<>();
            MutableLong totalSum = new MutableLong();
            MutableLong velocityStageTotalTime = new MutableLong();
            velocityStageStatusesMap.forEach((velocityStage, jiraStatusesForVelocityStage) -> {
                VelocityConfigDTO.Stage currStage = velocityStageNameStageMap.get(velocityStage);
                Long intervalSumForVelocityStage = getIntervalSumForVelocityStage(jiraStatusesForVelocityStage, allDbJiraStatuses);
                if (needRelease && jiraStatusesForVelocityStage.contains("$$RELEASE$$") && MapUtils.isNotEmpty(jiraReleaseMap)) {
                    var releaseDbJira = jiraReleaseMap.get(dbJiraIssue.getKey());
                    if (releaseDbJira != null) {
                        intervalSumForVelocityStage= releaseDbJira.getReleaseTime();
                    }
                }
                if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getVelocityStages()) && (jiraIssuesFilter.getVelocityStages().contains(velocityStage))) {
                    totalSum.add(intervalSumForVelocityStage);
                }
                if (CollectionUtils.isEmpty(jiraIssuesFilter.getExcludeVelocityStages()) || !jiraIssuesFilter.getExcludeVelocityStages().contains(velocityStage)) {
                    velocityStageTotalTime.add(intervalSumForVelocityStage);
                }
                //PROP-3441 : Add "velocity_stage_result" so that FE can render proper colours in drill-down
                velocityStages.add(VelocityStageTime.builder().stage(velocityStage).timeSpent(intervalSumForVelocityStage)
                        .velocityStageResult(VelocityStageResult.builder()
                                .lowerLimitValue(currStage.getLowerLimitValue())
                                .lowerLimitUnit(currStage.getLowerLimitUnit())
                                .upperLimitValue(currStage.getUpperLimitValue())
                                .upperLimitUnit(currStage.getUpperLimitUnit())
                                .rating(currStage.calculateRating(intervalSumForVelocityStage))
                                .build())
                        .build());
            });
            List<DbJiraStatus> nonTerminalDbJiraStatuses = getNonTerminalDbJiraStatuses(dbJiraIssue, finalStatusCategoryMetadataList);

            long intervalSumForOtherVelocityStage = 0L;
            if (CollectionUtils.isEmpty(jiraIssuesFilter.getExcludeVelocityStages()) || !jiraIssuesFilter.getExcludeVelocityStages().contains("Other")) {
                intervalSumForOtherVelocityStage = getIntervalSumForOtherVelocityStage(allVelocityStageStatuses, nonTerminalDbJiraStatuses);
            }
            //TODO : SA - Shoud we set "velocityStageResult" for "Other" stage as well ?
            velocityStages.add(VelocityStageTime.builder().stage("Other").timeSpent(intervalSumForOtherVelocityStage).build());
            DbJiraIssue.DbJiraIssueBuilder jiraIssueBuilder = dbJiraIssueBuilder.velocityStageTime((CollectionUtils.isNotEmpty(jiraIssuesFilter.getVelocityStages()) && jiraIssuesFilter.getVelocityStages().contains("Other")) ?
                    intervalSumForOtherVelocityStage + totalSum.getValue() : totalSum.getValue()).velocityStageTotalTime(velocityStageTotalTime.addAndGet(intervalSumForOtherVelocityStage)).velocityStages(velocityStages);
            return useEs ? jiraIssueBuilder.velocityStage(CollectionUtils.isNotEmpty(jiraIssuesFilter.getVelocityStages()) ?
                    jiraIssuesFilter.getVelocityStages().get(0) : null).build() : jiraIssueBuilder.build();
        }).collect(Collectors.toList());
        return dbJiraIssues;
    }

    public static List<DbJiraIssue> getDbJiraIssuesWithVelocityStagesForReleaseReport(JiraIssuesFilter jiraIssuesFilter,
                                                                                      List<DbJiraIssue> results,
                                                                                      VelocityConfigDTO velocityConfigDTO,
                                                                                      List<DbJiraStatusMetadata.IntegStatusCategoryMetadata> finalStatusCategoryMetadataList,
                                                                                      Map<String, DbJiraIssue> jiraReleaseMap,
                                                                                      boolean needRelease,
                                                                                      boolean useEs) {
        List<VelocityConfigDTO.Stage> developmentCustomStages = getSortedDevStages(velocityConfigDTO);
        Map<String, List<String>> velocityStageStatusesMap = JiraConditionUtils.getStageStatusesMap(developmentCustomStages);
        Map<String, VelocityConfigDTO.Stage> velocityStageNameStageMap = developmentCustomStages.stream().collect(Collectors.toMap(stage -> stage.getName(), stage -> stage));
        List<String> allVelocityStageStatuses = JiraConditionUtils.getAllVelocityStageStatuses(developmentCustomStages);
        List<DbJiraIssue> dbJiraIssues = results.stream().map(dbJiraIssue -> {
            List<DbJiraStatus> allDbJiraStatuses = dbJiraIssue.getStatuses(); // PROP-101 since we are already excluding Done statuses from Other using Ignore_Terminal_Stage - we want to use all statuses for the calculation
            DbJiraIssue.DbJiraIssueBuilder dbJiraIssueBuilder = dbJiraIssue.toBuilder();
            List<VelocityStageTime> velocityStages = new ArrayList<>();
            MutableLong totalSum = new MutableLong();
            MutableLong velocityStageTotalTime = new MutableLong();
            velocityStageStatusesMap.forEach((velocityStage, jiraStatusesForVelocityStage) -> {
                VelocityConfigDTO.Stage currStage = velocityStageNameStageMap.get(velocityStage);
                Long intervalSumForVelocityStage = getIntervalSumForVelocityStage(jiraStatusesForVelocityStage, allDbJiraStatuses);
                if (needRelease && jiraStatusesForVelocityStage.contains("$$RELEASE$$") && MapUtils.isNotEmpty(jiraReleaseMap)) {
                    var releaseDbJira = jiraReleaseMap.get(dbJiraIssue.getKey() + "_" + dbJiraIssue.getFixVersion());
                    if (releaseDbJira != null) {
                        intervalSumForVelocityStage = releaseDbJira.getReleaseTime();
                    }
                }
                if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getVelocityStages()) && (jiraIssuesFilter.getVelocityStages().contains(velocityStage))) {
                    totalSum.add(intervalSumForVelocityStage);
                }
                if (CollectionUtils.isEmpty(jiraIssuesFilter.getExcludeVelocityStages()) || !jiraIssuesFilter.getExcludeVelocityStages().contains(velocityStage)) {
                    velocityStageTotalTime.add(intervalSumForVelocityStage);
                }
                //PROP-3441 : Add "velocity_stage_result" so that FE can render proper colours in drill-down
                velocityStages.add(VelocityStageTime.builder().stage(velocityStage).timeSpent(intervalSumForVelocityStage)
                        .velocityStageResult(VelocityStageResult.builder()
                                .lowerLimitValue(currStage.getLowerLimitValue())
                                .lowerLimitUnit(currStage.getLowerLimitUnit())
                                .upperLimitValue(currStage.getUpperLimitValue())
                                .upperLimitUnit(currStage.getUpperLimitUnit())
                                .rating(currStage.calculateRating(intervalSumForVelocityStage))
                                .build())
                        .build());
            });
            List<DbJiraStatus> nonTerminalDbJiraStatuses = getNonTerminalDbJiraStatuses(dbJiraIssue, finalStatusCategoryMetadataList);

            long intervalSumForOtherVelocityStage = 0L;
            if (CollectionUtils.isEmpty(jiraIssuesFilter.getExcludeVelocityStages()) || !jiraIssuesFilter.getExcludeVelocityStages().contains("Other")) {
                intervalSumForOtherVelocityStage = getIntervalSumForOtherVelocityStage(allVelocityStageStatuses, nonTerminalDbJiraStatuses);
            }
            //TODO : SA - Shoud we set "velocityStageResult" for "Other" stage as well ?
            velocityStages.add(VelocityStageTime.builder().stage("Other").timeSpent(intervalSumForOtherVelocityStage).build());
            DbJiraIssue.DbJiraIssueBuilder jiraIssueBuilder = dbJiraIssueBuilder.velocityStageTime((CollectionUtils.isNotEmpty(jiraIssuesFilter.getVelocityStages()) && jiraIssuesFilter.getVelocityStages().contains("Other")) ?
                    intervalSumForOtherVelocityStage + totalSum.getValue() : totalSum.getValue()).velocityStageTotalTime(velocityStageTotalTime.addAndGet(intervalSumForOtherVelocityStage)).velocityStages(velocityStages);
            return useEs ? jiraIssueBuilder.velocityStage(CollectionUtils.isNotEmpty(jiraIssuesFilter.getVelocityStages()) ?
                    jiraIssuesFilter.getVelocityStages().get(0) : null).build() : jiraIssueBuilder.build();
        }).collect(Collectors.toList());
        return dbJiraIssues;
    }

    public static List<DbJiraStatus> getNonTerminalDbJiraStatuses(DbJiraIssue dbJiraIssue, List<DbJiraStatusMetadata.IntegStatusCategoryMetadata> integStatusCategoryMetadataList) {
        Optional<DbJiraStatusMetadata.IntegStatusCategoryMetadata> optIntegStatusCategoryMetadata = integStatusCategoryMetadataList.stream()
                .filter(metadata -> DONE_STATUS_CATEGORY.equalsIgnoreCase(metadata.getStatusCategory()))
                .filter(metadata -> dbJiraIssue.getIntegrationId().equals(metadata.getIntegrationId()))
                .findFirst();
        if (optIntegStatusCategoryMetadata.isPresent()) {
            DbJiraStatusMetadata.IntegStatusCategoryMetadata statusCategoryMetadata = optIntegStatusCategoryMetadata.get();
            return dbJiraIssue.getStatuses().stream()
                    .filter(dbJiraStatus -> !statusCategoryMetadata.getStatuses().contains(dbJiraStatus.getStatus()))
                    .collect(Collectors.toList());
        }
        return dbJiraIssue.getStatuses();
    }

    public static long getIntervalSumForVelocityStage(List<String> velocityStages, List<DbJiraStatus> statusesToConsiderInTheCalculation) {
        return statusesToConsiderInTheCalculation
                .stream()
                .filter(dbJiraStatus -> velocityStages.contains(dbJiraStatus.getStatus()))
                .map(dbJiraStatus -> dbJiraStatus.getEndTime() - dbJiraStatus.getStartTime())
                .mapToLong(Long::longValue).sum();
    }

    public static long getIntervalSumForOtherVelocityStage(List<String> allVelocityStageJiraStatuses, List<DbJiraStatus> nonTerminalDbJiraStatuses) {
        return nonTerminalDbJiraStatuses
                .stream()
                .filter(dbJiraStatus -> !allVelocityStageJiraStatuses.contains(dbJiraStatus.getStatus()))
                .map(dbJiraStatus -> dbJiraStatus.getEndTime() - dbJiraStatus.getStartTime())
                .mapToLong(Long::longValue).sum();
    }

}
