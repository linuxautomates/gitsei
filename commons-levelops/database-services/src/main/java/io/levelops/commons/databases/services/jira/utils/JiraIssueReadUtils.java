package io.levelops.commons.databases.services.jira.utils;

import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.database.jira.DbJiraPriority;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.JiraSprintFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.DbJiraSprintDistMetric;
import io.levelops.commons.databases.models.response.JiraSprintDistMetric;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.Query;
import io.levelops.ingestion.models.IntegrationType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.StringSubstitutor;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.JiraIssueService.ISSUES_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.JIRA_ISSUE_LINKS;
import static io.levelops.commons.databases.services.JiraIssueService.JIRA_ISSUE_SPRINTS;
import static io.levelops.commons.databases.services.JiraIssueService.JIRA_ISSUE_VERSIONS;
import static io.levelops.commons.databases.services.JiraIssueService.PRIORITIES_SLA_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.PRIORITIES_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.STATUSES_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.USERS_TABLE;

public class JiraIssueReadUtils {


    public static long getIngestedAtUpperBound(long ingestedAt, String aggInterval) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(TimeUnit.SECONDS.toMillis(ingestedAt));
        if (aggInterval.equals(AGG_INTERVAL.day.toString())) {
            cal.add(Calendar.DATE, 1);
        } else if (aggInterval.equals(AGG_INTERVAL.week.toString())) {
            cal.add(Calendar.DATE, 7);
        } else if (aggInterval.equals(AGG_INTERVAL.biweekly.toString())) {
            cal.add(Calendar.DATE, 14);
        } else if (aggInterval.equals(AGG_INTERVAL.month.toString())) {
            cal.add(Calendar.MONTH, 1);
        } else if (aggInterval.equals(AGG_INTERVAL.quarter.toString())) {
            cal.add(Calendar.MONTH, 3);
        } else if (aggInterval.equals(AGG_INTERVAL.year.toString())) {
            cal.add(Calendar.YEAR, 1);
        }
        return TimeUnit.MILLISECONDS.toSeconds(cal.getTimeInMillis());
    }

    public static JiraIssuesFilter.JiraIssuesFilterBuilder getFilterForTrendStack(JiraIssuesFilter.JiraIssuesFilterBuilder jiraIssuesFilterBuilder,
                                                                                  DbAggregationResult row, JiraIssuesFilter.DISTINCT across,
                                                                                  JiraIssuesFilter.DISTINCT stack, String aggInterval) throws SQLException {
        Calendar cal = Calendar.getInstance();
        BigDecimal bigDecimal = new BigDecimal(row.getKey());
        long startTimeInSeconds = bigDecimal.longValue();
        cal.setTimeInMillis(TimeUnit.SECONDS.toMillis(startTimeInSeconds));
        if (aggInterval.equals(AGG_INTERVAL.month.toString())) {
            cal.add(Calendar.MONTH, 1);
        } else if (aggInterval.equals(AGG_INTERVAL.day.toString())) {
            cal.add(Calendar.DATE, 1);
        } else if (aggInterval.equals(AGG_INTERVAL.year.toString())) {
            cal.add(Calendar.YEAR, 1);
        } else if (aggInterval.equals(AGG_INTERVAL.quarter.toString())) {
            cal.add(Calendar.MONTH, 3);
        } else {
            cal.add(Calendar.DATE, 7);
        }
        long endTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(cal.getTimeInMillis());

        ImmutablePair<Long, Long> timeRange = ImmutablePair.of(startTimeInSeconds, endTimeInSeconds);
        switch (across) {
            case issue_created:
                jiraIssuesFilterBuilder.issueCreatedRange(timeRange);
                break;
            case issue_due:
            case issue_due_relative:
                jiraIssuesFilterBuilder.issueDueRange(timeRange);
                break;
            case issue_updated:
                jiraIssuesFilterBuilder.issueUpdatedRange(timeRange);
                break;
            case issue_resolved:
                jiraIssuesFilterBuilder.issueResolutionRange(timeRange);
                break;
            case trend:
                jiraIssuesFilterBuilder
                        .ingestedAt(startTimeInSeconds)
                        .ingestedAtByIntegrationId(null) // we want to use the current stack's value only
                        .snapshotRange(null); // we can't use ingested_at if snapshot range is set
                break;
            default:
                throw new SQLException("This across option is not available trend. Provided across: " + across);
        }

        return jiraIssuesFilterBuilder.across(stack);
    }

    @NotNull
    public static String getPriorityOrderJoinForList(String company, String finalTableAlias) {
        return " LEFT JOIN (" +
                "   SELECT DISTINCT priority_order, integration_id, priority, project" +
                "   FROM " + company + "." + PRIORITIES_TABLE +
                "   WHERE integration_id IN (:jira_integration_ids)" +
                "   AND scheme = 'default'" +
                "   AND project = '_levelops_default_'" +
                " ) AS prior_order " +
                " ON " + finalTableAlias + ".integration_id = prior_order.integration_id" +
                " AND UPPER(" + finalTableAlias + ".priority) = UPPER(prior_order.priority) ";
    }

    @NotNull
    public static String getLinkedIssuesWhere(JiraIssuesFilter filter, Map<String, Object> params, Boolean needIngestedAtFilterForTrendLinkedIssues) {
        String linkedIssuesWhere = "";
        if (CollectionUtils.isNotEmpty(filter.getIntegrationIds())) {
            linkedIssuesWhere += " AND integration_id IN (:jira_linked_issues_integration_ids)";
            params.put("jira_linked_issues_integration_ids", filter.getIntegrationIds().stream()
                    .map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeIntegrationIds())) {
            linkedIssuesWhere += " AND integration_id NOT IN (:not_jira_linked_issues_integration_ids)";
            params.put("not_jira_linked_issues_integration_ids", filter.getExcludeIntegrationIds().stream()
                    .map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (filter.getIngestedAt() != null && needIngestedAtFilterForTrendLinkedIssues) {
            linkedIssuesWhere += " AND ingested_at = :jira_linked_issues_ingested_at";
            params.put("jira_linked_issues_ingested_at", filter.getIngestedAt());
        }
        return linkedIssuesWhere;
    }

    @NotNull
    public static String getLinkedIssuesWhereForJoinStmt(JiraIssuesFilter filter, Map<String, Object> params, Boolean needIngestedAtFilterForTrendLinkedIssues) {
        String linkedIssuesWhere = "";
        if (CollectionUtils.isNotEmpty(filter.getIntegrationIds())) {
            linkedIssuesWhere += " AND I.integration_id IN (:jira_linked_issues_integration_ids)";
            params.put("jira_linked_issues_integration_ids", filter.getIntegrationIds().stream()
                    .map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeIntegrationIds())) {
            linkedIssuesWhere += " AND I.integration_id NOT IN (:not_jira_linked_issues_integration_ids)";
            params.put("not_jira_linked_issues_integration_ids", filter.getExcludeIntegrationIds().stream()
                    .map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (filter.getIngestedAt() != null && needIngestedAtFilterForTrendLinkedIssues) {
            linkedIssuesWhere += " AND I.ingested_at = :jira_linked_issues_ingested_at";
            params.put("jira_linked_issues_ingested_at", filter.getIngestedAt());
        }
        return linkedIssuesWhere;
    }

    public static String getStackLinkedIssuesWhereForJoinStmt(JiraIssuesFilter originalFilter,JiraIssuesFilter filter, Map<String, Object> params, Boolean needIngestedAtFilterForTrendLinkedIssues,List<String> customCondition) {
        String linkedIssuesWhere = "";
        if (CollectionUtils.isNotEmpty(filter.getIntegrationIds())) {
            linkedIssuesWhere += " AND I.integration_id IN (:jira_linked_issues_integration_ids)";
            params.put("jira_linked_issues_integration_ids", filter.getIntegrationIds().stream()
                    .map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeIntegrationIds())) {
            linkedIssuesWhere += " AND I.integration_id NOT IN (:not_jira_linked_issues_integration_ids)";
            params.put("not_jira_linked_issues_integration_ids", filter.getExcludeIntegrationIds().stream()
                    .map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (filter.getIngestedAt() != null && needIngestedAtFilterForTrendLinkedIssues) {
            linkedIssuesWhere += " AND I.ingested_at = :jira_linked_issues_ingested_at";
            params.put("jira_linked_issues_ingested_at", filter.getIngestedAt());
        }
        if(originalFilter!=null) {
            if (originalFilter.getAcross() == JiraIssuesFilter.DISTINCT.project
                    && CollectionUtils.isNotEmpty(originalFilter.getProjects())) {
                if (CollectionUtils.isNotEmpty(filter.getProjects())) {
                    linkedIssuesWhere += " AND I.project IN (:jira_issue_project)";
                    params.put("jira_issue_project", filter.getProjects());
                } else {
                    linkedIssuesWhere += " AND I.project = '' ";
                }
            }
            if (originalFilter.getAcross() == JiraIssuesFilter.DISTINCT.issue_type
                    && CollectionUtils.isNotEmpty(originalFilter.getIssueTypes())) {
                if (CollectionUtils.isNotEmpty(filter.getIssueTypes())) {
                    linkedIssuesWhere += " AND I.issue_type IN (:jira_issue_issueType)";
                    params.put("jira_issue_issueType", filter.getIssueTypes());
                } else {
                    linkedIssuesWhere += " AND I.issue_type = '' ";
                }
            }
            if (originalFilter.getAcross() == JiraIssuesFilter.DISTINCT.priority
                    && CollectionUtils.isNotEmpty(originalFilter.getPriorities())) {
                if (CollectionUtils.isNotEmpty(filter.getPriorities())) {
                    linkedIssuesWhere += " AND I.priority IN (:jira_issue_priorities)";
                    params.put("jira_issue_priorities", filter.getPriorities());
                } else {
                    linkedIssuesWhere += " AND I.priority = '' ";
                }
            }
            if (originalFilter.getAcross() == JiraIssuesFilter.DISTINCT.label
                    && CollectionUtils.isNotEmpty(originalFilter.getLabels())) {
                if (CollectionUtils.isNotEmpty(filter.getLabels())) {
                    linkedIssuesWhere += " AND I.labels && ARRAY[ :jira_issue_labels ])";
                    params.put("jira_issue_labels", filter.getLabels());
                } else {
                    linkedIssuesWhere += " AND I.labels = '{}' ";
                }
            }
            if (originalFilter.getAcross() == JiraIssuesFilter.DISTINCT.component
                    && CollectionUtils.isNotEmpty(originalFilter.getComponents())) {
                if (CollectionUtils.isNotEmpty(filter.getComponents())) {
                    linkedIssuesWhere += " AND I.components && ARRAY[ :jira_issue_components ])";
                    params.put("jira_issue_components", filter.getComponents());
                } else {
                    linkedIssuesWhere += " AND I.components = '{}' ";
                }
            }
            if (originalFilter.getAcross() == JiraIssuesFilter.DISTINCT.reporter
                    && CollectionUtils.isNotEmpty(originalFilter.getReporters())) {
                if (CollectionUtils.isNotEmpty(filter.getReporters())) {
                    linkedIssuesWhere += " AND I.reporter_id::text IN ( :jira_issue_reporters )";
                    params.put("jira_issue_reporters", filter.getReporters());
                } else {
                    linkedIssuesWhere += " AND I.reporter_id::text = '' ";
                }
            }
            if (originalFilter.getAcross() == JiraIssuesFilter.DISTINCT.status
                    && CollectionUtils.isNotEmpty(originalFilter.getStatuses())) {
                if (CollectionUtils.isNotEmpty(filter.getStatuses())) {
                    linkedIssuesWhere += " AND I.status IN ( :jira_issue_statuses )";
                    params.put("jira_issue_statuses", filter.getStatuses());
                } else {
                    linkedIssuesWhere += " AND I.status = '' ";
                }
            }
            if (originalFilter.getAcross() == JiraIssuesFilter.DISTINCT.epic
                    && CollectionUtils.isNotEmpty(originalFilter.getEpics())) {
                if (CollectionUtils.isNotEmpty(filter.getEpics())) {
                    linkedIssuesWhere += " AND I.epic IN ( :jira_issue_epics )";
                    params.put("jira_issue_epics", filter.getEpics());
                } else {
                    linkedIssuesWhere += " AND I.epic = '' ";
                }
            }
            if (originalFilter.getAcross() == JiraIssuesFilter.DISTINCT.parent
                    && CollectionUtils.isNotEmpty(originalFilter.getParentKeys())) {
                if (CollectionUtils.isNotEmpty(filter.getParentKeys())) {
                    linkedIssuesWhere += " AND I.parent_key IN ( :jira_issue_parent_keys )";
                    params.put("jira_parent_keys", filter.getParentKeys());
                } else {
                    linkedIssuesWhere += " AND I.parent_key = '' ";
                }
            }
            if (originalFilter.getAcross() == JiraIssuesFilter.DISTINCT.first_assignee
                    && CollectionUtils.isNotEmpty(originalFilter.getFirstAssignees())) {
                if (CollectionUtils.isNotEmpty(filter.getFirstAssignees())) {
                    linkedIssuesWhere += " AND I.first_assignee_id::text IN ( :jira_issue_first_assignees )";
                    params.put("jira_issue_first_assignees", filter.getFirstAssignees());
                } else {
                    linkedIssuesWhere += " AND I.first_assignee_id::text = '' ";
                }
            }
            if (originalFilter.getAcross() == JiraIssuesFilter.DISTINCT.resolution
                    && CollectionUtils.isNotEmpty(originalFilter.getResolutions())) {
                if (CollectionUtils.isNotEmpty(filter.getResolutions())) {
                    linkedIssuesWhere += " AND I.resolution IN ( :jira_issue_resolutions )";
                    params.put("jira_issue_resolutions", filter.getResolutions());
                } else {
                    linkedIssuesWhere += " AND I.resolution = '' ";
                }
            }
            if (originalFilter.getAcross() == JiraIssuesFilter.DISTINCT.status_category
                    && CollectionUtils.isNotEmpty(originalFilter.getStatusCategories())) {
                if (CollectionUtils.isNotEmpty(filter.getStatusCategories())) {
                    linkedIssuesWhere += " AND I.status_category IN ( :jira_issue_status_categories )";
                    params.put("jira_issue_status_categories", filter.getStatusCategories());
                } else {
                    linkedIssuesWhere += " AND I.status_category = '' ";
                }
            }
            if (originalFilter.getAcross() == JiraIssuesFilter.DISTINCT.version
                    && CollectionUtils.isNotEmpty(originalFilter.getVersions())) {
                if (CollectionUtils.isNotEmpty(filter.getVersions())) {
                    linkedIssuesWhere += " AND I.versions && ARRAY[ :jira_issue_versions ]";
                    params.put("jira_issue_versions", filter.getVersions());
                } else {
                    linkedIssuesWhere += " AND I.versions = '{}' ";
                }
            }
            if (originalFilter.getAcross() == JiraIssuesFilter.DISTINCT.assignee
                    && CollectionUtils.isNotEmpty(originalFilter.getAssignees())) {
                if (CollectionUtils.isNotEmpty(filter.getAssignees())) {
                    linkedIssuesWhere += " AND I.assignee_id::text IN ( :jira_issue_assignees )";
                    params.put("jira_issue_assignees", filter.getAssignees());
                } else {
                    linkedIssuesWhere += " AND I.assignee_id::text = '' ";
                }
            }
            if (filter.getCustomFields() != null && !filter.getCustomFields().isEmpty() && CollectionUtils.isNotEmpty(customCondition)) {
                for (String condition : customCondition) {
                    linkedIssuesWhere += " AND " + condition;
                }
            } else if ((filter.getCustomFields() == null || filter.getCustomFields().isEmpty() || CollectionUtils.isEmpty(customCondition)) && (originalFilter.getCustomFields() != null && !originalFilter.getCustomFields().isEmpty())) {
                String keysAsString = String.join(", ", originalFilter.getCustomFields().keySet());
                ;
                linkedIssuesWhere += " AND not I.custom_fields ? '" + keysAsString + "'";
            }
            if (originalFilter.getAcross() == JiraIssuesFilter.DISTINCT.issue_created
                    && originalFilter.getIssueCreatedRange() != null) {
                if (filter.getIssueCreatedRange() != null) {
                    if (filter.getIssueCreatedRange().getLeft() != null) {
                        linkedIssuesWhere += " AND I.issue_created_at >  :jira_issue_created_start ";
                        params.put("jira_issue_created_start", filter.getIssueCreatedRange().getLeft());
                    } else {
                        linkedIssuesWhere += " AND I.issue_created_at is null ";
                    }

                    if (filter.getIssueCreatedRange().getRight() != null) {
                        linkedIssuesWhere += " AND I.issue_created_at <  :jira_issue_created_end ";
                        params.put("jira_issue_created_end", filter.getIssueCreatedRange().getRight());
                    } else {
                        linkedIssuesWhere += " AND I.issue_created_at is null ";
                    }
                } else {
                    linkedIssuesWhere += " AND I.issue_created_at is null ";
                }
            }
            if (originalFilter.getAcross() == JiraIssuesFilter.DISTINCT.issue_updated
                    && originalFilter.getIssueUpdatedRange() != null) {
                if (filter.getIssueUpdatedRange() != null) {
                    if (filter.getIssueUpdatedRange().getLeft() != null) {
                        linkedIssuesWhere += " AND I.issue_updated_at >  :jira_issue_updated_start ";
                        params.put("jira_issue_updated_start", filter.getIssueUpdatedRange().getLeft());
                    } else {
                        linkedIssuesWhere += " AND I.issue_updated_at is null ";
                    }

                    if (filter.getIssueUpdatedRange().getRight() != null) {
                        linkedIssuesWhere += " AND I.issue_updated_at <  :jira_issue_updated_end ";
                        params.put("jira_issue_updated_end", filter.getIssueUpdatedRange().getRight());
                    } else {
                        linkedIssuesWhere += " AND I.issue_updated_at is null ";
                    }
                } else {
                    linkedIssuesWhere += " AND I.issue_updated_at is null ";
                }
            }
            if (originalFilter.getAcross() == JiraIssuesFilter.DISTINCT.issue_due
                    && originalFilter.getIssueDueRange() != null) {
                if (filter.getIssueDueRange() != null) {
                    if (filter.getIssueDueRange().getLeft() != null) {
                        linkedIssuesWhere += " AND I.issue_due_at >  :jira_issue_due_start ";
                        params.put("jira_issue_due_start", filter.getIssueDueRange().getLeft());
                    } else {
                        linkedIssuesWhere += " AND I.issue_due_at is null ";
                    }

                    if (filter.getIssueDueRange().getRight() != null) {
                        linkedIssuesWhere += " AND I.issue_due_at <  :jira_issue_due_end ";
                        params.put("jira_issue_due_end", filter.getIssueDueRange().getRight());
                    } else {
                        linkedIssuesWhere += " AND I.issue_due_at is null ";
                    }
                } else {
                    linkedIssuesWhere += " AND I.issue_due_at is null ";
                }
            }
            if (originalFilter.getAcross() == JiraIssuesFilter.DISTINCT.issue_resolved
                    && originalFilter.getIssueResolutionRange() != null) {
                if (filter.getIssueResolutionRange() != null) {
                    if (filter.getIssueResolutionRange().getLeft() != null) {
                        linkedIssuesWhere += " AND I.issue_resolved_at >  :jira_issue_resolved_start ";
                        params.put("jira_issue_resolved_start", filter.getIssueResolutionRange().getLeft());
                    } else {
                        linkedIssuesWhere += " AND I.issue_resolved_at is null ";
                    }
                    if (filter.getIssueResolutionRange().getRight() != null) {
                        linkedIssuesWhere += " AND I.issue_resolved_at <  :jira_issue_resolved_end ";
                        params.put("jira_issue_resolved_end", filter.getIssueResolutionRange().getRight());
                    } else {
                        linkedIssuesWhere += " AND I.issue_resolved_at is null ";
                    }
                } else {
                    linkedIssuesWhere += " AND I.issue_resolved_at is null ";
                }
            }
            if (originalFilter.getAcross() == JiraIssuesFilter.DISTINCT.fix_version
                    && CollectionUtils.isNotEmpty(originalFilter.getFixVersions())) {
                if (CollectionUtils.isNotEmpty(filter.getFixVersions())) {
                    linkedIssuesWhere += " AND I.fix_versions && ARRAY[ :jira_issue_fix_versions ]";
                    params.put("jira_issue_fix_versions", filter.getFixVersions());
                } else {
                    linkedIssuesWhere += " AND I.fix_versions = '{}' ";
                }
            }
        }
        return linkedIssuesWhere;
    }

    public static String getSprintJoinStmt(String company, JiraIssuesFilter filter, String joinType, String sprintWhere, String issuesTableAlias) {
        String sprintLimitStatement = "";
        if (filter.getSprintCount() != null && filter.getSprintCount() > 0) {
            sprintLimitStatement = " order by end_date desc limit " + filter.getSprintCount();
        }
        return " " + joinType + " (Select integration_id as integ_id,sprint_id,name as sprint,state as" +
                " sprint_state,start_date as sprint_creation_date" + " FROM " + company + "." + JIRA_ISSUE_SPRINTS
                + sprintWhere
                + sprintLimitStatement + " ) sprints ON" +
                " sprints.integ_id=" + issuesTableAlias + ".integration_id AND sprints.sprint_id=ANY(" + issuesTableAlias + ".sprint_ids)";
    }

    public static String getVersionsJoinStmt(String company, String column, String joinType, String versionWhere, String issuesTableAlias) {
        return getVersionsJoinStmt(company, column, joinType, versionWhere, issuesTableAlias, false);
    }

    public static String getVersionsJoinStmt(String company, String column, String joinType, String versionWhere, String issuesTableAlias, boolean needRelease) {
        String versionOrderStatement = "";
        String endTimeSelectStatement = "";
        if (!needRelease) {
            versionOrderStatement = " ORDER BY " + column + "_end_date DESC";
            endTimeSelectStatement = ",end_date AS " + column + "_end_date";
        } else {
            endTimeSelectStatement = " ,extract(epoch from (end_date)) AS release_end_time";
        }
        return " " + joinType + " (SELECT integration_id AS integ_id,"
                + " name AS " + column + endTimeSelectStatement
                + " FROM " + company + "." + JIRA_ISSUE_VERSIONS
                + versionWhere
                + (needRelease ? " AND released=true" : "")
                + versionOrderStatement
                + ") versions ON versions.integ_id=" + issuesTableAlias + ".integration_id AND "
                + "versions." + column + "=ANY(" + issuesTableAlias + "." + column + "s)";
    }

    public static String getVersionTableJoinStmtForReleaseStage(String company, String column, String joinType, String versionWhere, String issuesTableAlias, boolean isClickedOnReleaseStage) {
        versionWhere += " AND released ";
        String endTimeSelectStatement = " ,extract(epoch from (end_date)) AS release_end_time";
        return " "
                + joinType + " (SELECT integration_id AS integ_id,"
                + " name AS " + column + endTimeSelectStatement
                + " FROM " + company + "." + JIRA_ISSUE_VERSIONS
                + versionWhere
                + ") versions ON versions.integ_id=" + issuesTableAlias + ".integration_id AND "
                + "versions." + column + "=ANY(" + issuesTableAlias + "." + column + "s)"
                + (!isClickedOnReleaseStage ? " AND issues.key in (:issue_keys_for_release)" : " " );
    }

    public static String getSprintAuxTable(String company, JiraIssuesFilter filter, String sprintWhere) {
        String sprintLimitStatement = "";
        if (filter.getSprintCount() != null && filter.getSprintCount() > 0) {
            sprintLimitStatement = " order by end_date desc limit " + filter.getSprintCount();
        }

        return "WITH spr_dates AS (select COALESCE(start_date, 99999999999) as start_date, sprint_id, name " +
                "from " + company + "." + JIRA_ISSUE_SPRINTS + sprintWhere + sprintLimitStatement + " )";
    }

    public static boolean isLinkedIssuesRequired(JiraIssuesFilter filter) {
        return (filter.getLinks() != null && CollectionUtils.isNotEmpty(filter.getLinks())) ||
                (filter.getExcludeLinks() != null && CollectionUtils.isNotEmpty(filter.getExcludeLinks()));
    }

    public static boolean isReleaseStageExcluded(Map<String, List<String>> velocityStageStatusesMap, JiraIssuesFilter filter) {
        if (velocityStageStatusesMap == null) {
            return false;
        }
        String releaseStage = velocityStageStatusesMap.entrySet().stream()
                .filter(entry -> entry.getValue().contains("$$RELEASE$$"))
                .map(entry -> entry.getKey()).collect(Collectors.joining());

        return CollectionUtils.emptyIfNull(velocityStageStatusesMap.values()).stream().flatMap(Collection::stream).anyMatch((value) -> {
            return value.equalsIgnoreCase("$$RELEASE$$");
        }) && !filter.getExcludeVelocityStages().contains(releaseStage);
    }

    @NotNull
    public static String getLinksTableJoinStmt(String company, String jiraIssueLinkWhere, String linkedIssuesWhere) {
        return " FROM " + company + "." + JIRA_ISSUE_LINKS + " L INNER JOIN ISSUES ON ISSUES.KEY = L.FROM_ISSUE_KEY " +
                jiraIssueLinkWhere + " INNER JOIN " + company + "." + ISSUES_TABLE + " I ON I.KEY = L.TO_ISSUE_KEY " + linkedIssuesWhere;
    }

    @NotNull
    public static String getLinksTableJoinStmt(String company, String linkWhere) {
        return " INNER JOIN (SELECT from_issue_key, to_issue_key, integration_id as intg_id, relation FROM "
                + company + "." + JIRA_ISSUE_LINKS + linkWhere
                + ") links ON issues.key = links.from_issue_key AND issues.integration_id = links.intg_id";
    }

    @NotNull
    public static String getIssuesAssigneeTableJoinStmt(String company) {
        return "" +
                "        join (\n" +
                "          select a.integration_id historical_assignee_integration_id, a.issue_key, assignee as historical_assignee from\n" +
                company + ".jira_issue_assignees as a\n" +
                "        ) as historical_assignees\n" +
                "        on issues.integration_id = historical_assignees.historical_assignee_integration_id and\n" +
                "           issues.key = historical_assignees.issue_key";
    }

    @NotNull
    public static Map<String, Integer> getStringIntegerMap(List<DbJiraPriority> jiraPriorityList) {
        return jiraPriorityList.stream()
                .collect(Collectors.toMap(
                        DbJiraPriority::getName,
                        DbJiraPriority::getOrder,
                        (existingOrder, replacementOrder) -> existingOrder));
    }

    public static boolean isSprintTblJoinRequired(JiraIssuesFilter filter) {
        if (CollectionUtils.size(filter.getSprintIds()) > 0 || CollectionUtils.size(filter.getSprintNames()) > 0 ||
                (filter.getAcross() != null && filter.getAcross() == JiraIssuesFilter.DISTINCT.sprint) ||
                CollectionUtils.size(filter.getSprintStates()) > 0 || CollectionUtils.size(filter.getExcludeSprintIds()) > 0 ||
                CollectionUtils.size(filter.getExcludeSprintNames()) > 0 || CollectionUtils.size(filter.getExcludeSprintStates()) > 0 ||
                (filter.getPartialMatch() != null && MapUtils.size(filter.getPartialMatch().get("sprint_name")) > 0) ||
                (filter.getPartialMatch() != null && MapUtils.size(filter.getPartialMatch().get("sprint_full_name")) > 0) ||
                (filter.getSprintCount() != null && filter.getSprintCount() > 0)) {
            return true;
        } else {
            return filter.getOrFilter() != null && (CollectionUtils.size(filter.getOrFilter().getSprintIds()) > 0 ||
                    CollectionUtils.size(filter.getOrFilter().getSprintNames()) > 0 || CollectionUtils.size(filter.getOrFilter().getSprintStates()) > 0 ||
                    (filter.getOrFilter().getPartialMatch() != null && MapUtils.size(filter.getOrFilter().getPartialMatch().get("sprint_name")) > 0) ||
                    (filter.getOrFilter().getPartialMatch() != null && MapUtils.size(filter.getOrFilter().getPartialMatch().get("sprint_full_name")) > 0));
        }
    }

    public static String getSelectStmtForArray(String column, String columnAlias, Map<String, Object> params,
                                               JiraIssuesFilter filter, List<String> filters, String partialFieldName) {
        String selectDistinctString = "UNNEST(" + column + ") AS " + columnAlias;
        List<String> filterValues = new ArrayList<>();
        if (BooleanUtils.isTrue(filter.getFilterAcrossValues())) {
            if (CollectionUtils.isNotEmpty(filters)) {
                filterValues.addAll(filters);
            }
            Map<String, Map<String, String>> partialMatchMap = filter.getPartialMatch();
            if (partialMatchMap != null && partialMatchMap.containsKey(partialFieldName)) {
                filterValues.addAll(getPartialValues(partialMatchMap.get(partialFieldName)));
            }
            if (CollectionUtils.isNotEmpty(filterValues)) {
                selectDistinctString = "UNNEST(array_intersect(" + column + ", ARRAY[ :across_filter_values ])) AS "
                        + columnAlias;
                params.put("across_filter_values", filterValues);
            }

        }
        return selectDistinctString;
    }

    public static List<String> getPartialValues(Map<String, String> partialFilter) {
        List<String> partialFilterValues = new ArrayList<>();
        if (partialFilter != null) {
            String begins = partialFilter.get("$begins");
            String ends = partialFilter.get("$ends");
            String contains = partialFilter.get("$contains");

            if (begins != null) {
                partialFilterValues.add(begins + "%");
            }

            if (ends != null) {
                partialFilterValues.add("%" + ends);
            }

            if (contains != null) {
                partialFilterValues.add("%" + contains + "%");
            }
        }
        return partialFilterValues;
    }

    @NotNull
    public static String getSlaTimeJoinStmt(String company, String issuesTableAlias) {
        return " LEFT OUTER JOIN ("
                + " SELECT solve_sla,resp_sla,project as proj,task_type as ttype,priority as prio,"
                + "integration_id as integid FROM " + company + "." + PRIORITIES_SLA_TABLE + " )"
                + " AS p ON p.proj = " + issuesTableAlias + ".project "
                + " AND p.prio = " + issuesTableAlias + ".priority"
                + " AND p.integid = " + issuesTableAlias + ".integration_id"
                + " AND p.ttype = " + issuesTableAlias + ".issue_type";
    }

    @NotNull
    public static String getParentSPjoin(String company) {
        return " INNER JOIN " +
                "( SELECT key AS parent_key, story_points AS parent_story_points, ingested_at as parent_ingested_at " +
                " FROM " + company + "." + ISSUES_TABLE + " " +
                " WHERE story_points IS NOT NULL " +
                " AND integration_id IN (:jira_integration_ids) " +
                " ) parent_issues ON epic = parent_issues.parent_key AND ingested_at = parent_issues.parent_ingested_at";
    }

    @NotNull
    public static String getSprintMappingsJoin(String company, String sprintMappingSprintWhere, String sprintMappingsWhere, String sprintOrderBy, String issuesTableAlias) {
        return " INNER JOIN (SELECT " +
                "   sm.sprint_id AS sprint_mapping_sprint_id, " +
                "   sm.issue_key AS sprint_mapping_issue_key, " +
                "   sm.integration_id AS sprint_mapping_integration_id, " +
                "   smpj.name AS sprint_mapping_name, " +
                "   smpj.goal AS sprint_mapping_goal, " +
                "   smpj.completed_at AS sprint_mapping_completed_at, " +
                "   smpj.start_date AS sprint_mapping_start_date, " +
                "   row_to_json(sm) AS sprint_mapping_json " +
                " FROM " + company + ".jira_issue_sprint_mappings as sm" +
                " INNER JOIN (SELECT integration_id, sprint_id, start_date, completed_at, name, goal " +
                "   FROM " + company + ".jira_issue_sprints as smp " +
                sprintMappingSprintWhere + sprintOrderBy +
                " ) as smpj" +
                " ON sm.integration_id = smpj.integration_id AND sm.sprint_id = smpj.sprint_id " +
                sprintMappingsWhere +
                ") AS smj " +
                " ON " + issuesTableAlias + ".integration_id = smj.sprint_mapping_integration_id " +
                " AND " + issuesTableAlias + ".key = smj.sprint_mapping_issue_key ";
    }

    @NotNull
    public static String getPriorityOrderJoinForReports(String company, String issuesTableAlias) {
        return " LEFT JOIN (" +
                "   SELECT DISTINCT priority_order, integration_id as prior_order_integration_id, priority as prior_order_priority" +
                "   FROM " + company + "." + PRIORITIES_TABLE +
                "   WHERE scheme = 'default' AND project = '_levelops_default_'" +
                " ) AS prior_order " +
                " ON " + issuesTableAlias + ".integration_id = prior_order.prior_order_integration_id" +
                " AND UPPER(" + issuesTableAlias + ".priority) = UPPER(prior_order.prior_order_priority) ";
    }

    @NotNull
    public static String getSlaTimeColumns(String company, long currentTime, String issuesTableAlias) {
        return ",(COALESCE(first_comment_at," + currentTime + ")-issue_created_at) AS resp_time"
                + ", greatest((COALESCE(issue_resolved_at," + currentTime + ") - issue_created_at - COALESCE("
                + " (SELECT SUM(end_time - start_time) AS exclude_time FROM " + company + "." + STATUSES_TABLE + " WHERE "
                + " integration_id IN (:jira_integration_ids) AND status IN (:not_stages) "
                + " AND issue_key=" + issuesTableAlias + ".key), 0)), 0) AS solve_time";
    }

    @NotNull
    public static String getSlaTimeJoin(String company) {
        return " ) AS issco LEFT OUTER JOIN ("
                + " SELECT solve_sla,resp_sla,project AS proj,task_type AS ttype,priority AS prio,"
                + "integration_id AS integid FROM " + company + "." + PRIORITIES_SLA_TABLE + " )"
                + " AS p ON p.proj = issco.project AND p.prio = issco.priority AND p.integid = issco.integration_id"
                + " AND p.ttype = issco.issue_type";
    }

    @NotNull
    public static String getUserTableJoin(String company, String usersWhere, String issuesTableAlias) {
        return " INNER JOIN ( SELECT ju.display_name,ju.active,ju.integ_id FROM " + company + "."
                + USERS_TABLE + " ju" + usersWhere + " ) AS u ON u.display_name = " + issuesTableAlias + ".assignee AND"
                + " u.integ_id = " + issuesTableAlias + ".integration_id";
    }

    @NotNull
    public static String getAssigneeArrayJoin(String company, String assigneesWhere, String issuesTableAlias) {
        return " INNER JOIN " +
                " (SELECT issue_key as iaj_issue_key, integration_id as iaj_integration_id, assignee as assignee_item" +
                "  FROM " + company + ".jira_issue_assignees AS ia" +
                assigneesWhere +
                " ) AS issue_assignees_join " +
                " ON issue_assignees_join.iaj_issue_key = " + issuesTableAlias + ".key " +
                " AND issue_assignees_join.iaj_integration_id = " + issuesTableAlias + ".integration_id";
    }

    @NotNull
    public static String getStatusTableJoinForStages(String company, String statusWhere, String issuesTableAlias) {
        return " INNER JOIN ( SELECT integration_id as integ_id, issue_key,end_time-start_time as time_spent," +
                "status as state from " + company + "." + STATUSES_TABLE + statusWhere +
                " ) s ON s.integ_id=" + issuesTableAlias + ".integration_id AND s.issue_key=" + issuesTableAlias + ".key ";
    }

    public static String getStatusTableJoinForRelease(String company, String statusWhere, String issuesTableAlias, List<String> previousStateForRelease, String preferRelease) {
        String stateBeforeRelease = "";
        if (previousStateForRelease.size() > 0) {
            String previousStateValue = previousStateForRelease.stream().map(str -> String.format("'%s'", str)).collect(Collectors.joining(","));
            stateBeforeRelease = " WHERE status IN (" + previousStateValue + ")";
        }
        String releaseEndTime = preferRelease + "(release_end_time)";
        return " UNION SELECT id, %s GREATEST(" + releaseEndTime + " - MAX(last_stage_start_time),0) as total_time_spent FROM issues " +
                "INNER JOIN ( SELECT integration_id as integ_id, issue_key, status as state, start_time AS last_stage_start_time from " + company + "." + STATUSES_TABLE +
                statusWhere + stateBeforeRelease + " ) s ON s.integ_id=" + issuesTableAlias + ".integration_id AND s.issue_key=" + issuesTableAlias + ".key ";
    }

    public static String getStatusTableJoinForReleaseDrillDown(String company, String statusWhere, String issuesTableAlias, List<String> previousStateForRelease) {
        String stateBeforeRelease = "";
        if (CollectionUtils.isNotEmpty(previousStateForRelease)) {
            String previousStateValue = previousStateForRelease.stream().map(str -> String.format("'%s'", str)).collect(Collectors.joining(","));
            stateBeforeRelease = " WHERE status IN (" + previousStateValue + ")";
        }
        return "INNER JOIN ( SELECT integration_id as integ_id, issue_key, status as state, max(start_time) as last_stage_start_time from " + company + "." + STATUSES_TABLE +
                statusWhere + stateBeforeRelease + " group by integration_id, issue_key, status) s ON s.integ_id=" + issuesTableAlias + ".integration_id AND s.issue_key=" + issuesTableAlias + ".key ";
    }


    public static String getStageTableJoinForBounce(String company, String statusWhere, boolean isList) {
        String statusGroupBy = "";
        String statusAlias = "";
        if (!isList) {
            statusGroupBy = ",status ";
            statusAlias = statusGroupBy + "as bounce_status";
        }
        return " INNER JOIN ( SELECT integration_id as integ_id, issue_key " + statusAlias + ", count(*) as count" +
                " from " + company + "." + STATUSES_TABLE + statusWhere +
                " GROUP  BY issue_key,integration_id" + statusGroupBy + " ) q ON q.integ_id=issues.integration_id AND q.issue_key= issues.key ";
    }

    public static String getStatusTableTransitJoin(String company, String issuesTableAlias) {
        return " INNER JOIN " +
                "(SELECT t1.issue_key, t1.integration_id AS status_integ_id, to_time - from_time AS state_transition_time" +
                " FROM " +
                "  (SELECT issue_key, integration_id, max(start_time) to_time " +
                "   FROM " + company + "." + STATUSES_TABLE +
                "   WHERE status = :to_status " +
                "   GROUP BY issue_key, integration_id" +
                "  ) t1 " +
                "  INNER JOIN " +
                "  (SELECT issue_key, integration_id, min(start_time) from_time" +
                "   FROM " + company + "." + STATUSES_TABLE +
                "   WHERE status = :from_status GROUP BY issue_key, integration_id" +
                "  ) t2 " +
                "  ON t1.issue_key = t2.issue_key AND t1.integration_id = t2.integration_id " +
                "  WHERE from_time < to_time" +
                ") statuses ON " + issuesTableAlias + ".key=statuses.issue_key AND " + issuesTableAlias + ".integration_id=statuses.status_integ_id";
    }

    public static ArrayList<JiraSprintDistMetric> computeAndGetPercentileData(List<Integer> completionPercentiles,
                                                                              JiraSprintFilter.CALCULATION calculation,
                                                                              String sprintId, List<DbJiraSprintDistMetric> sprintMetrics) {
        ArrayList<JiraSprintDistMetric> percentileResults = new ArrayList<>();
        for (Integer completionPercentile : completionPercentiles) {
            JiraSprintDistMetric.JiraSprintDistMetricBuilder sprintMetricsBuilder
                    = JiraSprintDistMetric.builder().key(String.valueOf(completionPercentile)).sprint(sprintId);
            HashSet<String> deliveredKeys = new HashSet<>();
            int planned = 0;
            int unplanned = 0;
            for (DbJiraSprintDistMetric sprintMetric : sprintMetrics) {
                deliveredKeys.add(sprintMetric.getKey());
                if (sprintMetric.getPlanned()) {
                    if (calculation == JiraSprintFilter.CALCULATION.sprint_story_points_report) {
                        planned += sprintMetric.getStoryPoints();
                    } else {
                        planned++;
                    }
                } else {
                    if (calculation == JiraSprintFilter.CALCULATION.sprint_story_points_report) {
                        unplanned += sprintMetric.getStoryPoints();
                    } else {
                        unplanned++;
                    }
                }
                if (sprintMetric.getPercentile() >= completionPercentile) {
                    sprintMetricsBuilder.deliveredStoryPoints(sprintMetric.getDeliveredStoryPoints());
                    sprintMetricsBuilder.deliveredKeys(deliveredKeys);
                    sprintMetricsBuilder.planned(planned);
                    sprintMetricsBuilder.unplanned(unplanned);
                    sprintMetricsBuilder.totalKeys(sprintMetrics.size());
                    sprintMetricsBuilder.totalTimeTaken(sprintMetric.getTotalTimeTaken());
                    percentileResults.add(sprintMetricsBuilder.build());
                    break;
                }
            }
        }
        return percentileResults;
    }

    public static boolean needParentIssueTypeJoin(JiraIssuesFilter filter) {
        return false; // SEI-2264 disabling for now
//        return CollectionUtils.isNotEmpty(filter.getParentIssueTypes()) ||
//                CollectionUtils.isNotEmpty(filter.getExcludeParentIssueTypes()) ||
//                (filter.getOrFilter() != null && CollectionUtils.isNotEmpty(filter.getOrFilter().getParentIssueTypes())) ||
//                ListUtils.emptyIfNull(filter.getTicketCategorizationFilters()).stream()
//                        .map(JiraIssuesFilter.TicketCategorizationFilter::getFilter)
//                        .filter(Objects::nonNull)
//                        .anyMatch(JiraIssueReadUtils::needParentIssueTypeJoin);
    }

    public static String getParentIssueTypeJoinStmt(String company, JiraIssuesFilter filter, String issuesTableAlias) {
        if (!needParentIssueTypeJoin(filter)) {
            return "";
        }

        Query parentIssuesQuery = new Query.QueryBuilder()
                .select(
                        Query.selectField("integration_id", "parent_integration_id"),
                        Query.selectField("key", "parent_issue_key"),
                        Query.selectField("ingested_at", "parent_ingested_at"),
                        Query.selectField("issue_type", "parent_issue_type"))
                .from(Query.fromField(company + ".jira_issues", null))
                .build();

        return StringSubstitutor.replace(" " +
                        " LEFT JOIN (" + parentIssuesQuery.toSql() + ") AS parent_workitems\n" +
                        " ON parent_integration_id = ${issuesTableAlias}integration_id " +
                        " AND parent_ingested_at = ${issuesTableAlias}ingested_at " +
                        " AND parent_issue_key = ${issuesTableAlias}parent_key\n",
                Map.of(
                        "issuesTableAlias", StringUtils.isNotBlank(issuesTableAlias) ? issuesTableAlias + "." : ""
                ));
    }
}
