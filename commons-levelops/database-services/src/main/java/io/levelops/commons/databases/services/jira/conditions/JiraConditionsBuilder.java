package io.levelops.commons.databases.services.jira.conditions;

import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.JiraOrFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.ingestion.models.IntegrationType;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.JiraIssueService.ASSIGNEES_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.FINAL_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.ISSUES_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.JIRA_ISSUE_LINKS;
import static io.levelops.commons.databases.services.JiraIssueService.JIRA_ISSUE_SPRINTS;
import static io.levelops.commons.databases.services.JiraIssueService.JIRA_ISSUE_VERSIONS;
import static io.levelops.commons.databases.services.JiraIssueService.STATUSES_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.USERS_TABLE;

@Log4j2
@Service
public class JiraConditionsBuilder {

    NamedParameterJdbcTemplate template;
    private final JiraFieldConditionsBuilder fieldConditionsBuilder;
    private final JiraCustomFieldConditionsBuilder customFieldConditionsBuilder;
    private final JiraPartialMatchConditionsBuilder partialMatchConditionsBuilder;
    private final boolean useIngestedAtByIntegration;

    @Autowired
    public JiraConditionsBuilder(DataSource dataSource,
                                 JiraFieldConditionsBuilder fieldConditionsBuilder,
                                 JiraCustomFieldConditionsBuilder customFieldConditionsBuilder,
                                 JiraPartialMatchConditionsBuilder partialMatchConditionsBuilder,
                                 @Nullable @Value("${jira.use_ingested_at_by_integration:true}") Boolean useIngestedAtByIntegration) {
        template = new NamedParameterJdbcTemplate(dataSource);
        this.fieldConditionsBuilder = fieldConditionsBuilder;
        this.customFieldConditionsBuilder = customFieldConditionsBuilder;
        this.partialMatchConditionsBuilder = partialMatchConditionsBuilder;
        this.useIngestedAtByIntegration = BooleanUtils.isTrue(useIngestedAtByIntegration);
    }

    public Map<String, List<String>> createWhereClauseAndUpdateParams(String company,
                                                                      Map<String, Object> params,
                                                                      JiraIssuesFilter filter,
                                                                      Long currentTime,
                                                                      Long ingestedAt,
                                                                      @Nullable OUConfiguration ouConfig) {
        return createWhereClauseAndUpdateParams(company, params, null, filter, currentTime, ingestedAt, "", ouConfig);
    }

    /**
     * @param issueTblQualifier including the "." - for example for table "t", pass "t."
     */
    public Map<String, List<String>> createWhereClauseAndUpdateParams(String company,
                                                                      Map<String, Object> params,
                                                                      String paramPrefix,
                                                                      JiraIssuesFilter filter,
                                                                      Long currentTime,
                                                                      Long ingestedAt,
                                                                      String issueTblQualifier,
                                                                      @Nullable OUConfiguration ouConfig) {
        paramPrefix = StringUtils.trimToEmpty(paramPrefix);
        Map<String, List<String>> andQueryConditions =
                createWhereClauseAndUpdateParams(company, params, paramPrefix, currentTime, filter.getExtraCriteria(),
                        filter.getKeys(), filter.getPriorities(), filter.getStatuses(), filter.getAssignees(),
                        filter.getIssueTypes(), filter.getIntegrationIds(), filter.getProjects(), filter.getComponents(),
                        filter.getReporters(), filter.getLabels(), filter.getFixVersions(), filter.getVersions(), filter.getStages(), filter.getVelocityStages(),
                        filter.getEpics(), filter.getParentKeys(), filter.getParentIssueTypes(), filter.getParentLabels(), filter.getFirstAssignees(), filter.getLinks(), filter.getCustomFields(),
                        filter.getHygieneCriteriaSpecs(), filter.getMissingFields(), filter.getIssueCreatedRange(), filter.getIssueDueRange(),
                        filter.getIssueUpdatedRange(), filter.getIssueResolutionRange(),
                        filter.getIssueReleasedRange(),filter.getParentStoryPoints(),
                        filter.getStoryPoints(), filter.getSummary(), filter.getFieldSize(), filter.getPartialMatch(),
                        filter.getAge(), filter.getSprintCount(), filter.getSprintIds(), filter.getSprintNames(), filter.getSprintFullNames(), filter.getSprintStates(),
                        filter.getResolutions(), filter.getStatusCategories(), filter.getTicketCategories(),
                        filter.getAssigneesDateRange(), filter.getExcludeStatusCategories(), filter.getExcludeResolutions(),
                        filter.getExcludeStages(), filter.getExcludeVelocityStages(), filter.getExcludeKeys(), filter.getExcludePriorities(),
                        filter.getExcludeStatuses(), filter.getExcludeAssignees(), filter.getExcludeReporters(),
                        filter.getExcludeIssueTypes(), filter.getExcludeFixVersions(), filter.getExcludeVersions(),
                        filter.getExcludeIntegrationIds(), filter.getExcludeProjects(), filter.getExcludeComponents(),
                        filter.getExcludeLabels(), filter.getExcludeEpics(), filter.getExcludeParentKeys(), filter.getExcludeParentIssueTypes(), filter.getExcludeParentLabels(), filter.getExcludeLinks(),
                        filter.getExcludeSprintIds(), filter.getExcludeSprintNames(), filter.getExcludeSprintFullNames(), filter.getExcludeSprintStates(),
                        filter.getExcludeCustomFields(), filter.getSnapshotRange(), filter.getFilterByLastSprint(), BooleanUtils.isNotFalse(filter.getIsActive()), (filter.getIgnoreOU() != null ? filter.getIgnoreOU() : false), issueTblQualifier, filter.getCustomStacks(), ouConfig, filter.getUnAssigned(), filter.getAssigneeDisplayNames(), filter.getHistoricalAssignees(), filter.getIds(),
                        filter.getIntegrationIdByIssueUpdatedRange());

        andQueryConditions.computeIfAbsent(ISSUES_TABLE, k -> new ArrayList<>());
        if (ingestedAt != null) {
            // NOTE: if ingested_at is null, we do not want to use useIngestedAtByIntegration either
            if (useIngestedAtByIntegration && MapUtils.isNotEmpty(filter.getIngestedAtByIntegrationId())) {
                List<String> ingestedAtConditions = new ArrayList<>();
                final String finalParamPrefix = paramPrefix;
                filter.getIngestedAtByIntegrationId().forEach((integrationId, latestIngestedAt) -> {
                    String param = StringUtils.trimToEmpty(finalParamPrefix) + "jira_ingested_at_" + integrationId;
                    ingestedAtConditions.add(String.format("(" + issueTblQualifier + "ingested_at = :%s AND " + issueTblQualifier + "integration_id = '%s') ", param, integrationId));
                    params.put(param, latestIngestedAt);
                });
                andQueryConditions.get(ISSUES_TABLE).add("(" + String.join(" OR ", ingestedAtConditions) + ")");
            } else {
                andQueryConditions.get(ISSUES_TABLE).add(issueTblQualifier + "ingested_at = :" + StringUtils.trimToEmpty(paramPrefix) + "jira_ingested_at");
                params.put(paramPrefix + "jira_ingested_at", ingestedAt);
            }
        }
        JiraOrFilter orFilter = filter.getOrFilter();
        if (orFilter != null) {
            Map<String, List<String>> orQueryConditions =
                    createWhereClauseAndUpdateParams(company, params, "or_" + paramPrefix, currentTime, orFilter.getExtraCriteria(),
                            orFilter.getKeys(), orFilter.getPriorities(), orFilter.getStatuses(), orFilter.getAssignees(),
                            orFilter.getIssueTypes(), null, orFilter.getProjects(), orFilter.getComponents(),
                            orFilter.getReporters(), orFilter.getLabels(), orFilter.getFixVersions(), orFilter.getVersions(), orFilter.getStages(), null,
                            orFilter.getEpics(), orFilter.getParentKeys(), orFilter.getParentIssueTypes(), null, null, orFilter.getLinks(), orFilter.getCustomFields(),
                            orFilter.getHygieneCriteriaSpecs(), orFilter.getMissingFields(), orFilter.getIssueCreatedRange(), orFilter.getIssueDueRange(),
                            orFilter.getIssueUpdatedRange(), orFilter.getIssueResolutionRange(), orFilter.getIssueReleasedRange(), orFilter.getParentStoryPoints(),
                            orFilter.getStoryPoints(), orFilter.getSummary(), orFilter.getFieldSize(), orFilter.getPartialMatch(),
                            orFilter.getAge(), orFilter.getSprintCount(), orFilter.getSprintIds(), orFilter.getSprintNames(), orFilter.getSprintFullNames(), orFilter.getSprintStates(),
                            orFilter.getResolutions(), orFilter.getStatusCategories(), null, null,
                            null, null, null, null, null,
                            null, null, null, null, null,
                            null, null, null, null, null,
                            null, null, null, null, null, null, null, null, null,
                            null, null, null, false, null, issueTblQualifier, filter.getCustomStacks(), ouConfig, null, null, null, null, null);
            //join the OR filter with the and filters
            for (Map.Entry<String, List<String>> x : orQueryConditions.entrySet()) {
                if (CollectionUtils.isNotEmpty(x.getValue())) {
                    String orCondition = " ( " + String.join(" OR ", x.getValue()) + " ) ";
                    //add the or condition into the AND condition list
                    andQueryConditions.computeIfAbsent(x.getKey(), k -> new ArrayList<>()).add(orCondition);
                }
            }
        }
        return andQueryConditions;
    }

    public Map<String, List<String>> createWhereClauseAndUpdateParams(String company,
                                                                      Map<String, Object> params,
                                                                      String paramPrefix,
                                                                      Long currentTime,
                                                                      List<JiraIssuesFilter.EXTRA_CRITERIA> extraCriteria,
                                                                      List<String> keys,
                                                                      List<String> priorities,
                                                                      List<String> statuses,
                                                                      List<String> assignees,
                                                                      List<String> issueTypes,
                                                                      List<String> integrationIds,
                                                                      List<String> projects,
                                                                      List<String> components,
                                                                      List<String> reporters,
                                                                      List<String> labels,
                                                                      List<String> fixVersions,
                                                                      List<String> versions,
                                                                      List<String> stages,
                                                                      List<String> velocityStages,
                                                                      List<String> epics,
                                                                      List<String> parentKeys,
                                                                      List<String> parentIssueTypes,
                                                                      List<String> parentLabels,
                                                                      List<String> firstAssignees,
                                                                      List<String> links,
                                                                      Map<String, Object> customFields,
                                                                      Map<JiraIssuesFilter.EXTRA_CRITERIA, Object> hygieneSpecs,
                                                                      Map<String, Boolean> missingFields,
                                                                      ImmutablePair<Long, Long> issueCreatedRange,
                                                                      ImmutablePair<Long, Long> issueDueRange,
                                                                      ImmutablePair<Long, Long> issueUpdatedRange,
                                                                      ImmutablePair<Long, Long> issueResolutionRange,
                                                                      ImmutablePair<Long, Long> issueReleasedRange,
                                                                      Map<String, String> parentStoryPoints,
                                                                      Map<String, String> storyPoints,
                                                                      String summary,
                                                                      Map<String, Map<String, String>> fieldSize,
                                                                      Map<String, Map<String, String>> partialMatch,
                                                                      ImmutablePair<Long, Long> age,
                                                                      Integer sprintCount,
                                                                      List<String> sprintIds,
                                                                      List<String> sprintNames,
                                                                      List<String> sprintFullNames,
                                                                      List<String> sprintStates,
                                                                      List<String> resolutions,
                                                                      List<String> statusCategories,
                                                                      List<String> ticketCategories,
                                                                      ImmutablePair<Long, Long> assigneesDateRange,
                                                                      List<String> excludeStatusCategories,
                                                                      List<String> excludeResolutions,
                                                                      List<String> excludeStages,
                                                                      List<String> excludeVelocityStages,
                                                                      List<String> excludeKeys,
                                                                      List<String> excludePriorities,
                                                                      List<String> excludeStatuses,
                                                                      List<String> excludeAssignees,
                                                                      List<String> excludeReporters,
                                                                      List<String> excludeIssueTypes,
                                                                      List<String> excludeFixVersions,
                                                                      List<String> excludeVersions,
                                                                      List<String> excludeIntegrationIds,
                                                                      List<String> excludeProjects,
                                                                      List<String> excludeComponents,
                                                                      List<String> excludeLabels,
                                                                      List<String> excludeEpics,
                                                                      List<String> excludeParentKeys,
                                                                      List<String> excludeParentIssueTypes,
                                                                      List<String> excludeParentLabels,
                                                                      List<String> excludeLinks,
                                                                      List<String> excludeSprintIds,
                                                                      List<String> excludeSprintNames,
                                                                      List<String> excludeSprintFullNames,
                                                                      List<String> excludeSprintStates,
                                                                      Map<String, Object> excludeCustomFields,
                                                                      ImmutablePair<Long, Long> snapshotRange,
                                                                      Boolean filterByLastSprint,
                                                                      Boolean isActive,
                                                                      String issueTblQualifier,
                                                                      List<String> customStacks,
                                                                      OUConfiguration ouConfig,
                                                                      Boolean unAssigned, List<String> assigneeDisplayNames,
                                                                      List<String> historicalAssignees, List<UUID> ids,
                                                                      Map<Integer, ImmutablePair<Long, Long>> integrationIdByIssueUpdatedRange) {

        return createWhereClauseAndUpdateParams(company, params, paramPrefix, currentTime, extraCriteria, keys, priorities, statuses, assignees, issueTypes, integrationIds, projects, components, reporters, labels, fixVersions,
                versions, stages, velocityStages, epics, parentKeys, parentIssueTypes, parentLabels, firstAssignees, links, customFields, hygieneSpecs, missingFields, issueCreatedRange, issueDueRange, issueUpdatedRange, issueResolutionRange,issueReleasedRange, parentStoryPoints, storyPoints, summary,
                fieldSize, partialMatch, age, sprintCount, sprintIds, sprintNames, sprintFullNames, sprintStates, resolutions, statusCategories, ticketCategories, assigneesDateRange, excludeStatusCategories, excludeResolutions, excludeStages, excludeVelocityStages, excludeKeys,
                excludePriorities, excludeStatuses, excludeAssignees, excludeReporters, excludeIssueTypes, excludeFixVersions, excludeVersions, excludeIntegrationIds, excludeProjects, excludeComponents, excludeLabels, excludeEpics, excludeParentKeys, excludeParentIssueTypes, excludeParentLabels,
                excludeLinks, excludeSprintIds, excludeSprintNames, excludeSprintFullNames, excludeSprintStates, excludeCustomFields, snapshotRange, filterByLastSprint, isActive, null, issueTblQualifier, customStacks, ouConfig, unAssigned, assigneeDisplayNames, historicalAssignees, ids,
                integrationIdByIssueUpdatedRange);
    }

    public Map<String, List<String>> createWhereClauseAndUpdateParams(String company,
                                                                      Map<String, Object> params,
                                                                      String paramPrefix,
                                                                      Long currentTime,
                                                                      List<JiraIssuesFilter.EXTRA_CRITERIA> extraCriteria,
                                                                      List<String> keys,
                                                                      List<String> priorities,
                                                                      List<String> statuses,
                                                                      List<String> assignees,
                                                                      List<String> issueTypes,
                                                                      List<String> integrationIds,
                                                                      List<String> projects,
                                                                      List<String> components,
                                                                      List<String> reporters,
                                                                      List<String> labels,
                                                                      List<String> fixVersions,
                                                                      List<String> versions,
                                                                      List<String> stages,
                                                                      List<String> velocityStages,
                                                                      List<String> epics,
                                                                      List<String> parentKeys,
                                                                      List<String> parentIssueTypes,
                                                                      List<String> parentLabels,
                                                                      List<String> firstAssignees,
                                                                      List<String> links,
                                                                      Map<String, Object> customFields,
                                                                      Map<JiraIssuesFilter.EXTRA_CRITERIA, Object> hygieneSpecs,
                                                                      Map<String, Boolean> missingFields,
                                                                      ImmutablePair<Long, Long> issueCreatedRange,
                                                                      ImmutablePair<Long, Long> issueDueRange,
                                                                      ImmutablePair<Long, Long> issueUpdatedRange,
                                                                      ImmutablePair<Long, Long> issueResolutionRange,
                                                                      ImmutablePair<Long, Long> issueReleasedRange,
                                                                      Map<String, String> parentStoryPoints,
                                                                      Map<String, String> storyPoints,
                                                                      String summary,
                                                                      Map<String, Map<String, String>> fieldSize,
                                                                      Map<String, Map<String, String>> partialMatch,
                                                                      ImmutablePair<Long, Long> age,
                                                                      Integer sprintCount,
                                                                      List<String> sprintIds,
                                                                      List<String> sprintNames,
                                                                      List<String> sprintFullNames,
                                                                      List<String> sprintStates,
                                                                      List<String> resolutions,
                                                                      List<String> statusCategories,
                                                                      List<String> ticketCategories,
                                                                      ImmutablePair<Long, Long> assigneesDateRange,
                                                                      List<String> excludeStatusCategories,
                                                                      List<String> excludeResolutions,
                                                                      List<String> excludeStages,
                                                                      List<String> excludeVelocityStages,
                                                                      List<String> excludeKeys,
                                                                      List<String> excludePriorities,
                                                                      List<String> excludeStatuses,
                                                                      List<String> excludeAssignees,
                                                                      List<String> excludeReporters,
                                                                      List<String> excludeIssueTypes,
                                                                      List<String> excludeFixVersions,
                                                                      List<String> excludeVersions,
                                                                      List<String> excludeIntegrationIds,
                                                                      List<String> excludeProjects,
                                                                      List<String> excludeComponents,
                                                                      List<String> excludeLabels,
                                                                      List<String> excludeEpics,
                                                                      List<String> excludeParentKeys,
                                                                      List<String> excludeParentIssueTypes,
                                                                      List<String> excludeParentLabels,
                                                                      List<String> excludeLinks,
                                                                      List<String> excludeSprintIds,
                                                                      List<String> excludeSprintNames,
                                                                      List<String> excludeSprintFullNames,
                                                                      List<String> excludeSprintStates,
                                                                      Map<String, Object> excludeCustomFields,
                                                                      ImmutablePair<Long, Long> snapshotRange,
                                                                      Boolean filterByLastSprint,
                                                                      Boolean isActive,
                                                                      Boolean ignoreOU,
                                                                      String issueTblQualifier,
                                                                      List<String> customStacks,
                                                                      @Nullable OUConfiguration ouConfig,
                                                                      Boolean unAssigned, List<String> assigneeDisplayNames,
                                                                      List<String> historicalAssignees, List<UUID> ids,
                                                                      Map<Integer, ImmutablePair<Long, Long>> integrationIdByIssueUpdatedRange) {
        paramPrefix = StringUtils.trimToEmpty(paramPrefix);
        if (CollectionUtils.isEmpty(extraCriteria)) {
            extraCriteria = List.of();
        }

        hygieneSpecs = MapUtils.emptyIfNull(hygieneSpecs);

        List<String> usersTblConditions = new ArrayList<>();
        List<String> issueTblConditions = new ArrayList<>();
        List<String> statusTblConditions = new ArrayList<>();
        List<String> finalTableConditions = new ArrayList<>();
        List<String> sprintTableConditions = new ArrayList<>();
        List<String> assigneesTableConditions = new ArrayList<>();
        List<String> versionTableConditions = new ArrayList<>();
        List<String> linkTableConditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(excludeSprintIds)) {
            sprintTableConditions.add("sprint_id NOT IN (:" + paramPrefix + "not_jira_sprint_ids)");
            params.put(paramPrefix + "not_jira_sprint_ids",
                    excludeSprintIds.stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(excludeSprintNames)) {
            sprintTableConditions.add("name NOT IN (:" + paramPrefix + "not_jira_sprint_names)");
            params.put(paramPrefix + "not_jira_sprint_names", excludeSprintNames);
        }
        if (CollectionUtils.isNotEmpty(excludeSprintFullNames)) {
            sprintTableConditions.add("name NOT IN (:" + paramPrefix + "not_jira_sprint_full_names)");
            params.put(paramPrefix + "not_jira_sprint_full_names", excludeSprintFullNames);
        }
        if (CollectionUtils.isNotEmpty(excludeSprintStates)) {
            sprintTableConditions.add("UPPER(state) NOT IN (:" + paramPrefix + "not_jira_sprint_states)");
            params.put(paramPrefix + "not_jira_sprint_states", excludeSprintStates.stream().map(StringUtils::upperCase).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(stages)) {
            statusTblConditions.add("UPPER(status) IN (:" + paramPrefix + "jira_stages)");
            params.put(paramPrefix + "jira_stages", stages.stream().map(StringUtils::upperCase).collect(Collectors.toList()));
            if (CollectionUtils.isNotEmpty(integrationIds)) {
                statusTblConditions.add("integration_id IN (:" + paramPrefix + "jira_integration_ids)");
                params.put(paramPrefix + "jira_integration_ids",
                        integrationIds.stream().map(NumberUtils::toInt).collect(Collectors.toList()));
            }
        }
        if (CollectionUtils.isNotEmpty(velocityStages) && !List.of("$$ALL_STAGES$$").equals(velocityStages)) {
            finalTableConditions.add("velocity_stage IN (:" + paramPrefix + "jira_velocity_stages)");
            params.put(paramPrefix + "jira_velocity_stages", velocityStages);
        }
        if (sprintCount != null && sprintCount > 0) {
            sprintTableConditions.add("end_date IS NOT NULL");
        }

        if (isActive != null) {
            issueTblConditions.add("(" + issueTblQualifier + "is_active = :" + paramPrefix + "is_active" +
                    // LEV-3861 in general is_active is not supposed to be null, but this is being used by join queries in other features
                    // where there may not be any issue, yielding null columns - in which case we want to ignore this filter
                    " OR " + issueTblQualifier + "is_active IS NULL)");
            params.put(paramPrefix + "is_active", isActive);
        }

        if (CollectionUtils.isNotEmpty(sprintIds)) {
            issueTblConditions.add(issueTblQualifier + "sprint_ids && ARRAY[ :" + paramPrefix + "sprint_ids ]");
            sprintTableConditions.add("sprint_id IN (:" + paramPrefix + "sprint_ids)");
            params.put(paramPrefix + "sprint_ids",
                    sprintIds.stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(sprintNames)) {
            sprintTableConditions.add("name IN (:" + paramPrefix + "sprint_names)");
            params.put(paramPrefix + "sprint_names", sprintNames);
        }
        if (CollectionUtils.isNotEmpty(sprintFullNames)) {
            sprintTableConditions.add("name IN (:" + paramPrefix + "sprint_full_names)");
            params.put(paramPrefix + "sprint_full_names", sprintFullNames);
        }
        if (CollectionUtils.isNotEmpty(sprintStates)) {
            sprintTableConditions.add("UPPER(state) IN (:" + paramPrefix + "sprint_states)");
            params.put(paramPrefix + "sprint_states", sprintStates.stream().map(StringUtils::upperCase).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            issueTblConditions.add(issueTblQualifier + "integration_id IN (:" + paramPrefix + "jira_integration_ids)");
            sprintTableConditions.add("integration_id IN (:" + paramPrefix + "jira_integration_ids)");
            versionTableConditions.add("integration_id IN (:" + paramPrefix + "jira_integration_ids)");
            linkTableConditions.add("integration_id IN (:" + paramPrefix + "jira_integration_ids)");
            params.put(paramPrefix + "jira_integration_ids",
                    integrationIds.stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(excludeKeys)) {
            issueTblConditions.add(issueTblQualifier + "key NOT IN (:" + paramPrefix + "not_jira_keys)");
            params.put(paramPrefix + "not_jira_keys", excludeKeys);
        }
        if (CollectionUtils.isNotEmpty(excludePriorities)) {
            issueTblConditions.add(issueTblQualifier + "priority NOT IN (:" + paramPrefix + "not_jira_priorities)");
            params.put(paramPrefix + "not_jira_priorities", excludePriorities);
        }
        if (CollectionUtils.isNotEmpty(excludeStatuses)) {
            issueTblConditions.add(issueTblQualifier + "status NOT IN (:" + paramPrefix + "not_jira_statuses)");
            params.put(paramPrefix + "not_jira_statuses", excludeStatuses);
        }
        if (CollectionUtils.isNotEmpty(excludeAssignees)) {
            issueTblConditions.add(issueTblQualifier + "assignee_id::text NOT IN (:" + paramPrefix + "not_jira_assignees)");
            params.put(paramPrefix + "not_jira_assignees", excludeAssignees);
        }
        if (CollectionUtils.isNotEmpty(excludeReporters)) {
            issueTblConditions.add(issueTblQualifier + "reporter_id::text NOT IN (:" + paramPrefix + "not_jira_reporters)");
            params.put(paramPrefix + "not_jira_reporters", excludeReporters);
        }
        if (CollectionUtils.isNotEmpty(excludeIssueTypes)) {
            issueTblConditions.add(issueTblQualifier + "issue_type NOT IN (:" + paramPrefix + "not_jira_issue_types)");
            params.put(paramPrefix + "not_jira_issue_types", excludeIssueTypes);
        }
        if (CollectionUtils.isNotEmpty(excludeFixVersions)) {
            issueTblConditions.add("NOT " + issueTblQualifier + "fix_versions && ARRAY[ :" + paramPrefix + "not_jira_fix_versions ]");
            versionTableConditions.add("name NOT IN (:" + paramPrefix + "not_jira_fix_versions)");
            params.put(paramPrefix + "not_jira_fix_versions", excludeFixVersions);
        }
        if (CollectionUtils.isNotEmpty(excludeVersions)) {
            issueTblConditions.add("NOT " + issueTblQualifier + "versions && ARRAY[ :" + paramPrefix + "not_jira_versions ]");
            versionTableConditions.add("name NOT IN (:" + paramPrefix + "not_jira_versions)");
            params.put(paramPrefix + "not_jira_versions", excludeVersions);
        }
        if (CollectionUtils.isNotEmpty(excludeIntegrationIds)) {
            issueTblConditions.add(issueTblQualifier + "integration_id NOT IN (:" + paramPrefix + "not_jira_integration_ids)");
            params.put(paramPrefix + "not_jira_integration_ids",
                    excludeIntegrationIds.stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(excludeProjects)) {
            issueTblConditions.add(issueTblQualifier + "project NOT IN (:" + paramPrefix + "not_jira_projects)");
            params.put(paramPrefix + "not_jira_projects", excludeProjects);
        }
        if (CollectionUtils.isNotEmpty(excludeComponents)
                && !extraCriteria.contains(JiraIssuesFilter.EXTRA_CRITERIA.no_components)) {
            issueTblConditions.add("NOT " + issueTblQualifier + "components && ARRAY[ :" + paramPrefix + "not_jira_components ]");
            params.put(paramPrefix + "not_jira_components", excludeComponents);
        }
        if (CollectionUtils.isNotEmpty(excludeLabels)) {
            issueTblConditions.add("NOT " + issueTblQualifier + "labels && ARRAY[ :" + paramPrefix + "not_jira_labels ]");
            params.put(paramPrefix + "not_jira_labels", excludeLabels);
        }
        if (CollectionUtils.isNotEmpty(excludeEpics)) {
            issueTblConditions.add(issueTblQualifier + "epic NOT IN (:" + paramPrefix + "not_jira_epics)");
            params.put(paramPrefix + "not_jira_epics", excludeEpics);
        }
        if (CollectionUtils.isNotEmpty(excludeParentKeys)) {
            issueTblConditions.add(issueTblQualifier + "parent_key NOT IN (:" + paramPrefix + "not_jira_parent_keys)");
            params.put(paramPrefix + "not_jira_parent_keys", excludeParentKeys);
        }
        if (CollectionUtils.isNotEmpty(excludeParentIssueTypes)) {
            issueTblConditions.add("parent_issue_type NOT IN (:" + paramPrefix + "not_jira_parent_issue_type)");
            params.put(paramPrefix + "not_jira_parent_issue_type", excludeParentIssueTypes);
        }
        if (CollectionUtils.isNotEmpty(excludeParentLabels)) {
            issueTblConditions.add("NOT " + issueTblQualifier + "parent_labels && ARRAY[ :" + paramPrefix + "not_jira_parent_labels ]");
            params.put(paramPrefix + "not_jira_parent_labels", excludeParentLabels);
        }
        if (CollectionUtils.isNotEmpty(excludeResolutions)) {
            issueTblConditions.add(issueTblQualifier + "resolution NOT IN (:" + paramPrefix + "not_jira_resolutions)");
            params.put(paramPrefix + "not_jira_resolutions", excludeResolutions);
        }
        if (CollectionUtils.isNotEmpty(excludeStatusCategories)) {
            issueTblConditions.add(issueTblQualifier + "status_category NOT IN (:" + paramPrefix + "not_jira_status_categories)");
            params.put(paramPrefix + "not_jira_status_categories", excludeStatusCategories);
        }
        if (MapUtils.isNotEmpty(excludeCustomFields)) {
            customFieldConditionsBuilder.createCustomFieldConditions(company, params, paramPrefix, integrationIds,
                    excludeCustomFields, issueTblConditions, false, issueTblQualifier, customStacks);
        }
        if (CollectionUtils.isNotEmpty(priorities)) {
            issueTblConditions.add(issueTblQualifier + "priority IN (:" + paramPrefix + "jira_priorities)");
            params.put(paramPrefix + "jira_priorities", priorities);
        }
        if (CollectionUtils.isNotEmpty(keys)) {
            issueTblConditions.add(issueTblQualifier + "key IN (:" + paramPrefix + "jira_keys)");
            params.put(paramPrefix + "jira_keys", keys);
        }
        if (CollectionUtils.isNotEmpty(statuses)) {
            issueTblConditions.add(issueTblQualifier + "status IN (:" + paramPrefix + "jira_statuses)");
            params.put(paramPrefix + "jira_statuses", statuses);
        }
        // OU: assignee
        // (note: please keep BaJiraAggsQueryBuilder.generateHistoricalAssigneesFilter up to date when making changes here)
        if (OrgUnitHelper.doesOUConfigHaveJiraAssignees(ouConfig) && BooleanUtils.isFalse(ignoreOU)) { // OU: assignee
            generateOuAssigneeCondition(company, ouConfig, params, issueTblQualifier).ifPresent(issueTblConditions::add);
        } else if (CollectionUtils.isNotEmpty(assignees)) {
            issueTblConditions.add(issueTblQualifier + "assignee_id::text IN (:" + paramPrefix + "jira_assignees)");
            params.put(paramPrefix + "jira_assignees", assignees);
        }
        if (CollectionUtils.isNotEmpty(assigneeDisplayNames)) {
            issueTblConditions.add(issueTblQualifier + "assignee IN (:" + paramPrefix + "jira_assignees_disp_name)");
            params.put(paramPrefix + "jira_assignees_disp_name", assigneeDisplayNames);
        }
        if (CollectionUtils.isNotEmpty(historicalAssignees)) {
            issueTblConditions.add(issueTblQualifier + "historical_assignee IN (:" + paramPrefix + "jira_historical_assignees_name)");
            params.put(paramPrefix + "jira_historical_assignees_name", historicalAssignees);
        }
        if (unAssigned != null && unAssigned) {
            issueTblConditions.add(issueTblQualifier + "assignee_id IS NULL");
        }
        if (CollectionUtils.isNotEmpty(links)) {
            linkTableConditions.add("relation IN (:" + paramPrefix + "links)");
            params.put(paramPrefix + "links", links);
        }
        if (filterByLastSprint != null && filterByLastSprint) {
            issueTblConditions.add("(SELECT max(coalesce(start_date, 99999999999)) FROM " + company + "." + JIRA_ISSUE_SPRINTS
                    + " spr WHERE spr.sprint_id = ANY(sprint_ids)) = ANY(ARRAY(SELECT start_date FROM spr_dates spr))");
        }
        if (CollectionUtils.isNotEmpty(reporters) || (OrgUnitHelper.isOuConfigActive(ouConfig) && ouConfig.getJiraFields().contains("reporter"))) { // OU: reporter
            if (OrgUnitHelper.doesOUConfigHaveJiraReporters(ouConfig) && BooleanUtils.isFalse(ignoreOU)) {
                var usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params, IntegrationType.JIRA);
                if (StringUtils.isNotBlank(usersSelect)) {
                    issueTblConditions.add(MessageFormat.format("{0} IN (SELECT display_name FROM ({1}) l)", issueTblQualifier + "reporter", usersSelect));
                }
            } else if (CollectionUtils.isNotEmpty(reporters)) {
                issueTblConditions.add(issueTblQualifier + "reporter_id::text IN (:" + paramPrefix + "jira_reporters)");
                params.put(paramPrefix + "jira_reporters", reporters);
            }
        }
        if (CollectionUtils.isNotEmpty(issueTypes)) {
            issueTblConditions.add(issueTblQualifier + "issue_type IN (:" + paramPrefix + "jira_issue_types)");
            params.put(paramPrefix + "jira_issue_types", issueTypes);
        }
        if (CollectionUtils.isNotEmpty(projects)) {
            issueTblConditions.add(issueTblQualifier + "project IN (:" + paramPrefix + "jira_projects)");
            params.put(paramPrefix + "jira_projects", projects);
        }
        if (CollectionUtils.isNotEmpty(epics)) {
            issueTblConditions.add(issueTblQualifier + "epic IN (:" + paramPrefix + "jira_epics)");
            params.put(paramPrefix + "jira_epics", epics);
        }
        if (CollectionUtils.isNotEmpty(parentKeys)) {
            issueTblConditions.add(issueTblQualifier + "parent_key IN (:" + paramPrefix + "jira_parent_keys)");
            params.put(paramPrefix + "jira_parent_keys", parentKeys);
        }
        if (CollectionUtils.isNotEmpty(parentIssueTypes)) {
            issueTblConditions.add("parent_issue_type IN (:" + paramPrefix + "jira_parent_issue_type)");
            params.put(paramPrefix + "jira_parent_issue_type", parentIssueTypes);
        }
        if (CollectionUtils.isNotEmpty(parentLabels)) {
            issueTblConditions.add(issueTblQualifier + "parent_labels && ARRAY[ :" + paramPrefix + "jira_parent_labels ]");
            params.put(paramPrefix + "jira_parent_labels", parentLabels);
        }
        if (CollectionUtils.isNotEmpty(resolutions)) {
            issueTblConditions.add(issueTblQualifier + "resolution IN (:" + paramPrefix + "jira_resolutions)");
            params.put(paramPrefix + "jira_resolutions", resolutions);
        }
        if (CollectionUtils.isNotEmpty(statusCategories)) {
            issueTblConditions.add(issueTblQualifier + "status_category IN (:" + paramPrefix + "jira_status_categories)");
            params.put(paramPrefix + "jira_status_categories", statusCategories);
        }
        if (CollectionUtils.isNotEmpty(excludeStages)) {
            statusTblConditions.add("status NOT IN (:" + paramPrefix + "not_stages)");
            params.put(paramPrefix + "not_stages", excludeStages.stream()
                    .map(StringUtils::upperCase).collect(Collectors.toList()));
            if (CollectionUtils.isNotEmpty(integrationIds)) {
                statusTblConditions.add("integration_id IN (:" + paramPrefix + "jira_integration_ids)");
                params.put(paramPrefix + "jira_integration_ids",
                        integrationIds.stream().map(NumberUtils::toInt).collect(Collectors.toList()));
            }
        }
        if (CollectionUtils.isNotEmpty(excludeVelocityStages) && !List.of("$$ALL_STAGES$$").equals(velocityStages)) {
            finalTableConditions.add(issueTblQualifier + "velocity_stage NOT IN (:" + paramPrefix + "not_jira_velocity_stages)");
            params.put(paramPrefix + "not_jira_velocity_stages", excludeVelocityStages);
        }
        if (CollectionUtils.isNotEmpty(excludeLinks)) {
            linkTableConditions.add("relation NOT IN (:" + paramPrefix + "not_links)");
            params.put(paramPrefix + "not_links", excludeLinks);
        }
        if (issueCreatedRange != null) {
            if (issueCreatedRange.getLeft() != null) {
                issueTblConditions.add(issueTblQualifier + "issue_created_at > :" + paramPrefix + "issue_created_start");
                params.put(paramPrefix + "issue_created_start", issueCreatedRange.getLeft());
            }
            if (issueCreatedRange.getRight() != null) {
                issueTblConditions.add(issueTblQualifier + "issue_created_at < :" + paramPrefix + "issue_created_end");
                params.put(paramPrefix + "issue_created_end", issueCreatedRange.getRight());
            }
        }
        if (issueUpdatedRange != null) {
            if (issueUpdatedRange.getLeft() != null) {
                issueTblConditions.add(issueTblQualifier + "issue_updated_at > :" + paramPrefix + "issue_updated_start");
                params.put(paramPrefix + "issue_updated_start", issueUpdatedRange.getLeft());
            }
            if (issueUpdatedRange.getRight() != null) {
                issueTblConditions.add(issueTblQualifier + "issue_updated_at < :" + paramPrefix + "issue_updated_end");
                params.put(paramPrefix + "issue_updated_end", issueUpdatedRange.getRight());
            }
        }
        if (MapUtils.isNotEmpty(integrationIdByIssueUpdatedRange)) {
            List<String> conditions = new ArrayList<>();
            List<Integer> integrationsWithTimeRange = new ArrayList<>();
            for(Map.Entry<Integer, ImmutablePair<Long, Long>> e: integrationIdByIssueUpdatedRange.entrySet()) {
                Integer integrationId = e.getKey();
                ImmutablePair<Long, Long> timeRange = e.getValue();
                if(timeRange == null) {
                    continue;
                }
                if ((timeRange.getLeft() != null) || (timeRange.getRight() != null)) {
                    integrationsWithTimeRange.add(integrationId);
                    List<String> innerConditions = new ArrayList<>();
                    innerConditions.add(issueTblQualifier + "integration_id = :" + paramPrefix + "jira_integration_ids_fs_"+ integrationId);
                    params.put(paramPrefix + "jira_integration_ids_fs_" + integrationId, integrationId);
                    if (timeRange.getLeft() != null) {
                        innerConditions.add(issueTblQualifier + "issue_updated_at > :" + paramPrefix + "issue_updated_start_fs_" + integrationId);
                        params.put(paramPrefix + "issue_updated_start_fs_" + integrationId, timeRange.getLeft());
                    }
                    if (timeRange.getRight() != null) {
                        innerConditions.add(issueTblQualifier + "issue_updated_at < :" + paramPrefix + "issue_updated_end_fs_" + integrationId);
                        params.put(paramPrefix + "issue_updated_end_fs_" + integrationId, timeRange.getRight());
                    }
                    conditions.add("( " + String.join(" AND ", innerConditions) + " )");
                }
            }
            if (CollectionUtils.isNotEmpty(integrationsWithTimeRange)) {
                conditions.add("( " + issueTblQualifier + "integration_id NOT IN (:" + paramPrefix + "jira_integration_ids_fs) )");
                params.put(paramPrefix + "jira_integration_ids_fs", integrationsWithTimeRange);
            }
            if(CollectionUtils.isNotEmpty(conditions)) {
                issueTblConditions.add("( " + String.join(" OR ", conditions) + " )");
            }
        }
        if (issueDueRange != null) {
            if (issueDueRange.getLeft() != null) {
                issueTblConditions.add(issueTblQualifier + "issue_due_at > :" + paramPrefix + "issue_due_start");
                params.put(paramPrefix + "issue_due_start", issueDueRange.getLeft());
            }
            if (issueDueRange.getRight() != null) {
                issueTblConditions.add(issueTblQualifier + "issue_due_at < :" + paramPrefix + "issue_due_end");
                params.put(paramPrefix + "issue_due_end", issueDueRange.getRight());
            }
        }
        if (issueResolutionRange != null) {
            if (issueResolutionRange.getLeft() != null) {
                issueTblConditions.add(issueTblQualifier + "issue_resolved_at > :" + paramPrefix + "issue_resolved_start");
                params.put(paramPrefix + "issue_resolved_start", issueResolutionRange.getLeft());
            }
            if (issueResolutionRange.getRight() != null) {
                issueTblConditions.add(issueTblQualifier + "issue_resolved_at < :" + paramPrefix + "issue_resolved_end");
                params.put(paramPrefix + "issue_resolved_end", issueResolutionRange.getRight());
            }
        }

        if (issueReleasedRange != null) {
            if (issueReleasedRange.getLeft() != null) {
                versionTableConditions.add( "end_date >= to_timestamp(:" + paramPrefix + "end_date_start)");
                params.put(paramPrefix + "end_date_start", issueReleasedRange.getLeft());
            }
            if (issueReleasedRange.getRight() != null) {
                versionTableConditions.add("end_date <= to_timestamp(:" + paramPrefix + "end_date_end)");
                params.put(paramPrefix + "end_date_end", issueReleasedRange.getRight());
            }
        }
        if (snapshotRange != null) {
            if (snapshotRange.getLeft() != null) {
                issueTblConditions.add(issueTblQualifier + "ingested_at > :" + paramPrefix + "snapshot_start");
                params.put(paramPrefix + "snapshot_start", snapshotRange.getLeft());
            }
            if (snapshotRange.getRight() != null) {
                issueTblConditions.add(issueTblQualifier + "ingested_at < :" + paramPrefix + "snapshot_end");
                params.put(paramPrefix + "snapshot_end", snapshotRange.getRight());
            }
        }
        if (assigneesDateRange != null) {
            if (assigneesDateRange.getLeft() != null) {
                // assignee_start >= lower_bound OR  assignee_end > lower_bound
                assigneesTableConditions.add("(start_time >= :" + paramPrefix + "assignees_start_time" +
                        " OR end_time > :" + paramPrefix + "assignees_start_time)");
                params.put(paramPrefix + "assignees_start_time", assigneesDateRange.getLeft());
            }
            if (assigneesDateRange.getRight() != null) {
                // assignee_start < upper_bound OR  assignee_end <=  upper_bound
                assigneesTableConditions.add("(start_time < :" + paramPrefix + "assignees_end_time" +
                        " OR end_time <= :" + paramPrefix + "assignees_end_time)");
                params.put(paramPrefix + "assignees_end_time", assigneesDateRange.getRight());
            }
        }
        if (age != null) {
            if (age.getLeft() != null) {
                issueTblConditions.add("(" + issueTblQualifier + "ingested_at-" + issueTblQualifier + "issue_created_at)/86400 > :" + paramPrefix + "age_start");
                params.put(paramPrefix + "age_start", age.getLeft());
            }
            if (age.getRight() != null) {
                issueTblConditions.add("(" + issueTblQualifier + "ingested_at-" + issueTblQualifier + "issue_created_at)/86400 < :" + paramPrefix + "age_end");
                params.put(paramPrefix + "age_end", age.getRight());
            }
        }
        if (CollectionUtils.isNotEmpty(firstAssignees)) { // OU: first assignee?
            issueTblConditions.add(issueTblQualifier + "first_assignee_id::text in (:" + paramPrefix + "first_assignees)");
            params.put(paramPrefix + "first_assignees", firstAssignees);
        }
        if (CollectionUtils.isNotEmpty(labels)) {
            issueTblConditions.add(issueTblQualifier + "labels && ARRAY[ :" + paramPrefix + "jira_labels ]");
            params.put(paramPrefix + "jira_labels", labels);
        }
        if (CollectionUtils.isNotEmpty(versions)) {
            issueTblConditions.add(issueTblQualifier + "versions && ARRAY[ :" + paramPrefix + "jira_versions ]");
            versionTableConditions.add("name IN (:" + paramPrefix + "jira_versions)");
            params.put(paramPrefix + "jira_versions", versions);
        }
        if (CollectionUtils.isNotEmpty(fixVersions)) {
            issueTblConditions.add(issueTblQualifier + "fix_versions && ARRAY[ :" + paramPrefix + "jira_fix_versions ]");
            versionTableConditions.add("name IN (:" + paramPrefix + "jira_fix_versions)");
            params.put(paramPrefix + "jira_fix_versions", fixVersions);
        }
        if (CollectionUtils.isNotEmpty(components)) {
            issueTblConditions.add(issueTblQualifier + "components && ARRAY[ :" + paramPrefix + "jira_components ]");
            params.put(paramPrefix + "jira_components", components);
        }
        if (StringUtils.isNotEmpty(summary)) {
            issueTblConditions.add(issueTblQualifier + "summary LIKE :" + paramPrefix + "jira_summary");
            params.put(paramPrefix + "jira_summary", "%" + summary + "%");
        }

        if (MapUtils.isNotEmpty(fieldSize)) {
            fieldConditionsBuilder.createFieldSizeFilter(fieldSize, issueTblConditions, company, integrationIds, params, issueTblQualifier);
        }

        if (MapUtils.isNotEmpty(partialMatch)) {
            partialMatchConditionsBuilder.createPartialMatchFilter(partialMatch, issueTblConditions, sprintTableConditions, params, company, integrationIds, issueTblQualifier);
        }

        if (MapUtils.isNotEmpty(storyPoints)) {
            String gt = storyPoints.get("$gt");
            if (gt != null) {
                issueTblConditions.add(issueTblQualifier + "story_points > :" + paramPrefix + "story_point_gt");
                params.put(paramPrefix + "story_point_gt", NumberUtils.toInt(gt));
            }
            String lt = storyPoints.get("$lt");
            if (lt != null) {
                issueTblConditions.add(issueTblQualifier + "story_points < :" + paramPrefix + "story_point_lt");
                params.put(paramPrefix + "story_point_lt", NumberUtils.toInt(lt));
            }
        }
        if (MapUtils.isNotEmpty(parentStoryPoints)) {
            String gt = parentStoryPoints.get("$gt");
            if (gt != null) {
                finalTableConditions.add("parent_story_points > :" + paramPrefix + "parent_story_points_gt");
                params.put(paramPrefix + "parent_story_points_gt", NumberUtils.toInt(gt));
            }
            String lt = parentStoryPoints.get("$lt");
            if (lt != null) {
                finalTableConditions.add("parent_story_points < :" + paramPrefix + "parent_story_points_lt");
                params.put(paramPrefix + "parent_story_points_lt", NumberUtils.toInt(lt));
            }
        }
        if (CollectionUtils.isNotEmpty(ticketCategories)) {
            finalTableConditions.add("ticket_category IN (:" + paramPrefix + "ticket_categories)");
            params.put(paramPrefix + "ticket_categories", ticketCategories);
        }
        if (CollectionUtils.isNotEmpty(ids)) {
            issueTblConditions.add(issueTblQualifier + "id::uuid IN (:" + paramPrefix + "ids)");
            params.put(paramPrefix + "ids", ids);
        }
        if (MapUtils.isNotEmpty(missingFields)) {
            Map<String, Boolean> missingCustomFields = new HashMap<>();
            Map<JiraIssuesFilter.MISSING_BUILTIN_FIELD, Boolean> missingBuiltinFields = new EnumMap<>(
                    JiraIssuesFilter.MISSING_BUILTIN_FIELD.class);
            missingFields.forEach((field, shouldBeMissing) -> {
                if (DbJiraField.CUSTOM_FIELD_KEY_PATTERN.matcher(field).matches()) {
                    missingCustomFields.put(field, shouldBeMissing);
                } else {
                    Optional.ofNullable(JiraIssuesFilter.MISSING_BUILTIN_FIELD.fromString(field))
                            .ifPresent(builtinField -> missingBuiltinFields.put(builtinField, shouldBeMissing));
                }
            });
            issueTblConditions.addAll(fieldConditionsBuilder.getMissingFieldsClause(missingBuiltinFields, missingCustomFields, params, issueTblQualifier));
        }
        if (MapUtils.isNotEmpty(customFields)) {
            customFieldConditionsBuilder.createCustomFieldConditions(company, params, paramPrefix, integrationIds, customFields, issueTblConditions, true, issueTblQualifier);
        }
        for (JiraIssuesFilter.EXTRA_CRITERIA hygieneType : extraCriteria) {
            switch (hygieneType) {
                case idle:
                    issueTblConditions.add(issueTblQualifier + "issue_updated_at < :" + paramPrefix + "jira_idletime");
                    params.put(paramPrefix + "jira_idletime", (currentTime - NumberUtils.toInt(
                            String.valueOf(hygieneSpecs.get(JiraIssuesFilter.EXTRA_CRITERIA.idle)), 30) * 86400L));
                    break;
                case no_assignee:
                    issueTblConditions.add(issueTblQualifier + "assignee = :" + paramPrefix + "jira_no_assignee");
                    params.put(paramPrefix + "jira_no_assignee", DbJiraIssue.UNASSIGNED);
                    break;
                case no_due_date:
                    issueTblConditions.add(issueTblQualifier + "issue_due_at IS NULL");
                    break;
                case poor_description:
                    issueTblConditions.add(issueTblQualifier + "desc_size < " + NumberUtils.toInt(String.valueOf(
                            hygieneSpecs.get(JiraIssuesFilter.EXTRA_CRITERIA.poor_description)), 10));
                    break;
                case no_components:
                    issueTblConditions.add(issueTblQualifier + "components = '{}'");
                    break;
                case missed_response_time:
                    finalTableConditions.add("resp_time > resp_sla");
                    break;
                case missed_resolution_time:
                    finalTableConditions.add("solve_time > solve_sla");
                    break;
                case inactive_assignees:
                    usersTblConditions.add("NOT active");
                    break;
            }
        }
        return new HashMap<>(
                Map.of(
                        ISSUES_TABLE, issueTblConditions,
                        USERS_TABLE, usersTblConditions,
                        FINAL_TABLE, finalTableConditions,
                        STATUSES_TABLE, statusTblConditions,
                        JIRA_ISSUE_SPRINTS, sprintTableConditions,
                        ASSIGNEES_TABLE, assigneesTableConditions,
                        JIRA_ISSUE_VERSIONS, versionTableConditions,
                        JIRA_ISSUE_LINKS, linkTableConditions));

    }

    /**
     * @param issueTblQualifier table alias with dot e.g. "issues."
     */
    public static Optional<String> generateOuAssigneeCondition(String company, OUConfiguration ouConfig, Map<String, Object> params, String issueTblQualifier) {
        String usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params, IntegrationType.JIRA);
        if (StringUtils.isBlank(usersSelect)) {
            return Optional.empty();
        }
        return Optional.of(MessageFormat.format("{0} IN (SELECT display_name FROM ({1}) l)", issueTblQualifier + "assignee", usersSelect));
    }

    public Long getMaxIngestedDate(String company) {
        List<Map<String, Object>> data = template.queryForList("SELECT MAX(ingested_at) FROM " + company
                + "." + ISSUES_TABLE, Map.of());
        return (Long) data.stream()
                .findFirst()
                .map(obj -> obj.getOrDefault("max", null))
                .orElse(null);
    }

}
