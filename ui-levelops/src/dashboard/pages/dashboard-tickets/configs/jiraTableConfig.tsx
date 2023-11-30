import { assign, cloneDeep, get, isNumber, set, unset } from "lodash";
import React from "react";
import { baseColumnConfig } from "utils/base-table-config";
import { convertEpochToHumanizedForm } from "utils/timeUtils";
import {
  coloredTagsColumn,
  convertSecToDay,
  convertToReadableTimestamp,
  dateRangeFilterColumn,
  priorityColumn,
  statusColumn,
  timeColumn,
  timeDurationColumn,
  timeRangeFilterColumn,
  userColumn
} from "./common-table-columns";
import { JiraIssueLink } from "../../../../shared-resources/components/jira-issue-link/jira-issue-link-component";
import { hygieneTypes } from "../../../constants/hygiene.constants";
import { depencyAnalysisOptionBEKeys } from "dashboard/graph-filters/components/Constants";
import { AzureIssueLinkProps } from "../../../../shared-resources/components/azure-issue-link/azure-issue-link";
import { getAzureReportApiRecords } from "reduxConfigs/sagas/saga-helpers/BASprintReport.helper";

export const jira_response_time = {
  ...baseColumnConfig("Response Time", "response_time", { sorter: true }),
  render: (value: any, record: any, index: any) => {
    const totalDays = value && isNumber(value) ? Math.round(value / 86400) : 0;
    return `${totalDays} days`;
  }
};

export const azure_response_time = {
  ...baseColumnConfig("Response Time", "response_time"),
  render: (value: any, record: any, index: any) => {
    const totalDays = value && isNumber(value) ? Math.round(value / 86400) : 0;
    return `${totalDays} days`;
  }
};

export const jira_resolution_time = {
  ...baseColumnConfig("Resolution Time", "solve_time", { sorter: true }),
  render: (value: any, record: any, index: any) => {
    const totalDays = value && isNumber(value) ? Math.round(value / 86400) : 0;
    return `${totalDays} days`;
  }
};

export const azure_resolution_time = {
  ...baseColumnConfig("Resolution Time", "solve_time"),
  render: (value: any, record: any, index: any) => {
    const totalDays = value && isNumber(value) ? Math.round(value / 86400) : 0;
    return `${totalDays} days`;
  }
};

export const scm_issues_resolution_time = {
  ...baseColumnConfig("Issue Resolution Time", "scm_resolution_time", { sorter: true }),
  render: (value: any, record: any, index: any) => {
    const issue_closed_at = get(record, ["issue_closed_at"], 0);
    const issue_created_at = get(record, ["issue_created_at"], 0);
    const timestamp = issue_closed_at - issue_created_at;
    if (issue_closed_at === 0 || issue_created_at === 0) {
      return "NA";
    }
    return convertEpochToHumanizedForm("days", timestamp, true);
  }
};

export const azure_scm_issues_resolution_time = {
  ...baseColumnConfig("Issue Resolution Time", "scm_resolution_time"),
  render: (value: any, record: any, index: any) => {
    const workitem_closed_at = get(record, ["workitem_closed_at"], 0);
    const workitem_created_at = get(record, ["workitem_created_at"], 0);
    const timestamp = workitem_closed_at - workitem_created_at;
    if (workitem_closed_at === 0 || workitem_created_at === 0) {
      return "NA";
    }
    return convertEpochToHumanizedForm("time", timestamp);
  }
};

export const jiraKeyColumn = {
  ...baseColumnConfig("Issue", "key", { width: "6%" }),
  render: (value: any, record: any) => {
    const url = `ticket_details?key=${record.key}&integration_id=${record.integration_id}`;
    return <JiraIssueLink link={url} ticketKey={value} integrationUrl={record?.integration_url} />;
  }
};

export const jiraCommonTableColumns = [
  {
    ...baseColumnConfig("Hygiene", "id2", { hidden: true }),
    filterType: "multiSelect",
    filterField: "hygiene_types",
    options: hygieneTypes,
    filterLabel: "Hygiene"
  },
  {
    ...baseColumnConfig("Poor Description", "poor_description", { hidden: true }),
    filterType: "input",
    filterField: "poor_description",
    filterLabel: "Poor Description (number of chars)",
    hygiene_type: "POOR_DESCRIPTION"
  },
  {
    ...baseColumnConfig("Idle Length", "idle", { hidden: true }),
    filterType: "input",
    filterField: "idle",
    filterLabel: "Idle Length (days)",
    hygiene_type: "IDLE"
  },
  {
    ...baseColumnConfig("Tags", "tags_filter", { hidden: true }),
    filterType: "tags",
    filterField: "epics",
    filterLabel: "Epics"
  },
  baseColumnConfig("Custom", "custom_fields", { hidden: true }),
  baseColumnConfig("Fix Version", "fix_version", { hidden: true }),
  baseColumnConfig("Versions", "version", { hidden: true }),
  dateRangeFilterColumn("Issue Created At", "issue_created_at"),
  dateRangeFilterColumn("Issue Updated At", "issue_updated_at"),
  timeRangeFilterColumn("Points", "Story Point", "story_points"),
  timeRangeFilterColumn("Points", "Parent Story Point", "parent_story_points")
];

export const azureCommonTableColumns = [
  {
    ...baseColumnConfig("Workitem Hygiene", "id2", { hidden: true }),
    filterType: "multiSelect",
    filterField: "hygiene_types",
    options: hygieneTypes,
    filterLabel: "Hygiene"
  },
  {
    ...baseColumnConfig("Poor Description", "poor_description", { hidden: true }),
    filterType: "input",
    filterField: "poor_description",
    filterLabel: "Poor Description (number of chars)",
    hygiene_type: "POOR_DESCRIPTION"
  },
  {
    ...baseColumnConfig("Idle Length", "idle", { hidden: true }),
    filterType: "input",
    filterField: "idle",
    filterLabel: "Idle Length (days)",
    hygiene_type: "IDLE"
  },
  {
    ...baseColumnConfig("Tags", "tags_filter", { hidden: true }),
    filterType: "tags",
    filterField: "epics",
    filterLabel: "Epics"
  },
  baseColumnConfig("Workitem Project", "workitem_project", { hidden: true }),
  baseColumnConfig("Workitem Status", "workitem_status", { hidden: true }),
  baseColumnConfig("Workitem Priority", "workitem_priority", { hidden: true }),
  baseColumnConfig("Workitem Status Category", "workitem_status_category", { hidden: true }),
  baseColumnConfig("Parent Workitem Id", "workitem_parent_workitem_id", { hidden: true }),
  baseColumnConfig("Workitem Epic", "workitem_epic", { hidden: true }),
  baseColumnConfig("Workitem Assignee", "workitem_assignee", { hidden: true }),
  baseColumnConfig("Workitem Ticket Category", "workitem_ticket_category", { hidden: true }),
  baseColumnConfig("Workitem Version", "workitem_version", { hidden: true }),
  baseColumnConfig("Workitem Fix Version", "workitem_fix_version", { hidden: true }),
  baseColumnConfig("Workitem Reporter", "workitem_reporter", { hidden: true }),
  baseColumnConfig("Workitem Label", "workitem_label", { hidden: true }),
  baseColumnConfig("Custom", "custom_fields", { hidden: true }),
  baseColumnConfig("Fix Version", "fix_version", { hidden: true }),
  baseColumnConfig("Versions", "version", { hidden: true }),
  dateRangeFilterColumn("Workitem Created At", "workitem_created_at"),
  dateRangeFilterColumn("Workitem Updated At", "workitem_updated_at"),
  dateRangeFilterColumn("Workitem Resolved At", "workitem_resolved_at"),
  timeRangeFilterColumn("Points", "Story Point", "story_points"),
  timeRangeFilterColumn("Points", "Parent Story Point", "parent_story_points"),
  timeRangeFilterColumn("Points", "Workitem Story Point", "workitem_story_point")
];

export const AssigneeReportTableConfig = [
  userColumn(),
  jiraKeyColumn,
  baseColumnConfig("Summary", "summary"),
  timeDurationColumn("Total Time", "total_time", "seconds"),
  baseColumnConfig("Bounces", "bounces", { hidden: true }),
  baseColumnConfig("Reporter", "reporter", { hidden: true }),
  baseColumnConfig("Status", "status", { hidden: true }),
  baseColumnConfig("Status Category", "status_category", { hidden: true }),
  baseColumnConfig("Issue Type", "issue_type", { hidden: true }),
  baseColumnConfig("Project", "project", { hidden: true }),
  baseColumnConfig("Priority", "priority", { hidden: true }),
  baseColumnConfig("Labels", "labels", { hidden: true }),
  ...jiraCommonTableColumns
];

export const JiraTableConfig = [
  jiraKeyColumn,
  baseColumnConfig("Summary", "summary"),
  coloredTagsColumn("Components", "component_list"),
  baseColumnConfig("Epic Summary", "epic_summary"),
  baseColumnConfig("Bounces", "bounces", { sorter: true, align: "center", width: "6%" }),
  baseColumnConfig("Hops", "hops", { sorter: true, align: "center", width: "6%" }),
  userColumn("Reporter", "reporter"),
  userColumn(),
  statusColumn(),
  baseColumnConfig("Status Category", "status_category", { hidden: true }),
  baseColumnConfig("Issue Type", "issue_type", { width: "5%" }),
  baseColumnConfig("Project", "project", { width: "5%" }),
  priorityColumn(),
  {
    ...baseColumnConfig("Labels", "labels"),
    render: (item: any) => item.join(",")
  },
  {
    ...timeColumn("Created At", "issue_created_at"),
    // colSpan: 2,
    align: "left"
  },
  ...jiraCommonTableColumns
];

export const JiraResponseTimeTableConfig = [
  ...JiraTableConfig.slice(0, 3),
  jira_response_time,
  ...JiraTableConfig.slice(3)
];

const dependencyAnalysisColumnRender = {
  render: (value: string[], record: any) => {
    value = value || [];
    const sliceAbleLength = Math.min(value.length, 3);
    const firstThreeIssues = value.slice(0, sliceAbleLength);
    const leftOutTickets = (value.slice(sliceAbleLength) || []).length;
    return (
      <div className="flex">
        <div className="flex direction-column" style={{ height: "100%" }}>
          {(firstThreeIssues || []).map(issue => {
            const url = `ticket_details?key=${issue}&integration_id=${record.integration_id}`;
            return <JiraIssueLink link={url} ticketKey={issue} integrationUrl={record?.integration_url} />;
          })}
        </div>
        {leftOutTickets > 0 && (
          <div className="flex align-center justify-center pl-9">
            <p style={{ color: "var(--link-and-actions)", marginBottom: "0" }}>{`+ ${leftOutTickets}`}</p>
          </div>
        )}
      </div>
    );
  }
};

export const dependencyAnalysisDrilldownColumns = {
  [depencyAnalysisOptionBEKeys.BLOCKS]: {
    ...baseColumnConfig("Blocked Issues", "linked_issues"),
    ...dependencyAnalysisColumnRender
  },
  [depencyAnalysisOptionBEKeys.IS_BLOCKED_BY]: {
    ...baseColumnConfig("Blocked By Issues", "linked_issues"),
    ...dependencyAnalysisColumnRender
  },
  [depencyAnalysisOptionBEKeys.RELATED_TO]: {
    ...baseColumnConfig("Related Issues", "linked_issues"),
    ...dependencyAnalysisColumnRender
  }
};

export const azureWorkitemColumn = {
  ...baseColumnConfig("Workitem", "workitem_id", { width: "7%" }),
  render: (value: any, record: any, index: any) => {
    const url = `ticket_details?key=${record.workitem_id}&integration_id=${record.integration_id}`;
    return (
      <AzureIssueLinkProps
        link={url}
        workItemId={record?.workitem_id}
        integrationUrl={record?.integration_url}
        organization={record.attributes?.organization || ""}
        project={record.project || ""}
      />
    );
  }
};

export const azureUtilityDrilldownFilterColumn = [
  {
    title: "Azure Iteration",
    filterTitle: "Azure Iteration",
    key: "azure_iteration",
    dataIndex: "azure_iteration",
    filterType: "apiMultiSelect",
    filterField: "workitem_sprint_full_names",
    uri: "issue_management_sprint_filters",
    transformOptions: (records: any[]) => {
      return (records || []).map(record => ({
        ...(record || {}),
        name: `${record?.parent_sprint}\\${record?.name}`,
        id: `${record?.parent_sprint}\\${record?.name}`
      }));
    },
    hidden: true
  },
  {
    title: "Azure Teams",
    filterTitle: "Azure Teams",
    key: "azure_teams",
    dataIndex: "azure_teams",
    filterType: "apiMultiSelect",
    filterField: "teams",
    prefixPath: "workitem_attributes",
    uri: "issue_management_attributes_values",
    searchField: "teams",
    morePayload: {
      fields: ["teams"]
    },
    transformOptions: (records: any[]) => {
      return (getAzureReportApiRecords(records) || []).map((record: { key: string }) => ({
        teams: record?.key,
        id: record?.key
      }));
    },
    transformPayload: (payload: any) => {
      const nPayload = cloneDeep(payload);
      const partialFilter = get(nPayload, ["filter", "partial", "teams"]);
      if (partialFilter) {
        unset(nPayload, ["filter", "partial", "teams"]);
        set(nPayload, ["filter", "partial_match", "teams"], { $contains: partialFilter });
      }
      return nPayload;
    },
    hidden: true
  },
  {
    title: "Azure Areas",
    filterTitle: "Azure Areas",
    key: "azure_areas",
    dataIndex: "azure_areas",
    filterType: "apiMultiSelect",
    searchField: "code_area",
    filterField: "code_area",
    prefixPath: "workitem_attributes",
    morePayload: {
      fields: ["code_area"]
    },
    transformOptions: (records: any[]) => {
      return (getAzureReportApiRecords(records) || []).map((record: { key: string }) => ({
        code_area: record?.key,
        id: record?.key
      }));
    },
    transformPayload: (payload: any) => {
      const nPayload = cloneDeep(payload);
      const partialFilter = get(nPayload, ["filter", "partial", "code_area"]);
      if (partialFilter) {
        unset(nPayload, ["filter", "partial", "code_area"]);
        set(nPayload, ["filter", "partial_match", "code_area"], { $contains: partialFilter });
      }
      return nPayload;
    },
    uri: "issue_management_attributes_values",
    hidden: true
  }
];

export const azureTableConfig = [
  azureWorkitemColumn,
  baseColumnConfig("Summary", "summary"),
  coloredTagsColumn("Components", "components"),
  baseColumnConfig("Bounces", "bounces", { sorter: true, align: "center", width: "7%" }),
  baseColumnConfig("Hops", "hops", { sorter: true, align: "center", width: "7%" }),
  userColumn("Reporter", "reporter"),
  userColumn(),
  statusColumn(),
  baseColumnConfig("Workitem Type", "workitem_type"),
  baseColumnConfig("Project", "project"),
  baseColumnConfig("Severity", "severity"),
  priorityColumn("Priority", "priority", { width: "5%" }),
  {
    ...baseColumnConfig("Labels", "labels", { width: "7%" }),
    render: (item: any) => item.join(",")
  },
  {
    ...timeColumn("Workitem Created At", "workitem_created_at"),
    align: "left"
  },
  dateRangeFilterColumn("Workitem Created At", "workitem_created_at"),
  dateRangeFilterColumn("Workitem Updated At", "workitem_updated_at"),
  dateRangeFilterColumn("Workitem Resolved At", "workitem_resolved_at"),
  baseColumnConfig("Story Points", "story_point"),
  convertToReadableTimestamp("Average time spent working on Issues", "solve_time", "days"),
  timeRangeFilterColumn("Points", "Workitem Story Point", "workitem_story_point"),
  baseColumnConfig("Workitem Project", "workitem_project", { hidden: true }),
  baseColumnConfig("Workitem Status", "workitem_status", { hidden: true }),
  baseColumnConfig("Workitem Priority", "workitem_priority", { hidden: true }),
  baseColumnConfig("Workitem Status Category", "workitem_status_category", { hidden: true }),
  baseColumnConfig("Parent Workitem Id", "workitem_parent_workitem_id", { hidden: true }),
  baseColumnConfig("Workitem Epic", "workitem_epic", { hidden: true }),
  baseColumnConfig("Workitem Assignee", "workitem_assignee", { hidden: true }),
  baseColumnConfig("Workitem Ticket Category", "workitem_ticket_category", { hidden: true }),
  baseColumnConfig("Workitem Version", "workitem_version", { hidden: true }),
  baseColumnConfig("Workitem Fix Version", "workitem_fix_version", { hidden: true }),
  baseColumnConfig("Workitem Reporter", "workitem_reporter", { hidden: true }),
  baseColumnConfig("Workitem Label", "workitem_label", { hidden: true }),
  ...azureUtilityDrilldownFilterColumn
];

export const azureResponseTimeTableConfig = [
  ...azureTableConfig.slice(0, 3),
  azure_response_time,
  ...azureTableConfig.slice(3)
];

export const ticketTimeSpentColumnConfig: any = (user: string) => {
  return {
    ...baseColumnConfig("Ticket Time Spent (days)", "assignee_list", { align: "center", width: "15%" }),
    render: (item: any) => {
      const list = (item || []).filter((assigneeItem: any) => assigneeItem.assignee === user);
      let totalSeconds = 0;
      list.forEach((assignee: any) => {
        totalSeconds +=
          parseInt(assignee?.end_time?.toString() || Math.round(Date.now() / 1000)) -
          parseInt(assignee?.start_time?.toString() || 0);
      });
      return (totalSeconds / 86400).toFixed(2);
    }
  };
};

export const ticketStoryPointsColumnConfig = {
  ...baseColumnConfig("Ticket Story Points", "story_points", { align: "center", width: "15%" }),
  filterLabel: "Ticket Story Points",
  filterType: "multiSelect",
  filterField: "story_points",
  prefixPath: "custom_fields"
};

export const effortInvestmentTrendReportTableConfig = [
  { ...jiraKeyColumn, title: "Ticket Id", width: "25%" },
  baseColumnConfig("Ticket Category", "ticket_category", { width: "20%" }),
  baseColumnConfig("Ticket Project", "project", { align: "center", width: "15%" }),
  baseColumnConfig("Epic", "epic", { width: "15%" }),
  ticketStoryPointsColumnConfig
];

export const azureEffortInvestmentTrendReportTableConfig = [
  { ...azureWorkitemColumn, width: "25%" },
  baseColumnConfig("Ticket Category", "ticket_category", { width: "20%" }),
  baseColumnConfig("Ticket Project", "project", { align: "center", width: "15%" }),
  baseColumnConfig("Epic", "epic", { width: "15%" }),
  ...azureUtilityDrilldownFilterColumn
];

export const JiraBacklogReportColumns = [
  jiraKeyColumn,
  baseColumnConfig("Summary", "summary"),
  coloredTagsColumn("Components", "component_list"),
  baseColumnConfig("Bounces", "bounces", { sorter: true, align: "center", width: "7%" }),
  baseColumnConfig("Hops", "hops", { sorter: true, align: "center", width: "7%" }),
  userColumn("Reporter", "reporter"),
  userColumn(),
  statusColumn(),
  baseColumnConfig("Status Category", "status_category", { hidden: true }),
  baseColumnConfig("Issue Type", "issue_type"),
  baseColumnConfig("Project", "project"),
  priorityColumn(),
  {
    ...baseColumnConfig("Labels", "labels"),
    render: (item: any) => item.join(",")
  },
  ...jiraCommonTableColumns
];

export const AzureBacklogReportColumns = [
  azureWorkitemColumn,
  baseColumnConfig("Summary", "summary"),
  coloredTagsColumn("Components", "component_list"),
  baseColumnConfig("Bounces", "bounces", { sorter: true, align: "center", width: "7%" }),
  baseColumnConfig("Hops", "hops", { sorter: true, align: "center", width: "7%" }),
  userColumn("Reporter", "reporter"),
  userColumn(),
  statusColumn(),
  baseColumnConfig("Workitem Type", "workitem_type"),
  baseColumnConfig("Project", "project"),
  priorityColumn(),
  {
    ...baseColumnConfig("Labels", "labels"),
    render: (item: any) => item.join(",")
  },
  ...azureCommonTableColumns,
  ...azureUtilityDrilldownFilterColumn
];

export const JiraStatTableConfig = [
  { ...jiraKeyColumn, width: "auto" },
  { ...userColumn(), width: "auto" },
  { ...statusColumn(), width: "auto" },
  coloredTagsColumn("Components", "component_list", { hidden: true }),
  userColumn("Reporter", "reporter", "user", { hidden: true }),
  baseColumnConfig("Issue Type", "issue_type", { hidden: true }),
  baseColumnConfig("Project", "project", { hidden: true }),
  priorityColumn("", "", { hidden: true }),
  {
    ...baseColumnConfig("Labels", "labels", { hidden: true }),
    render: (item: any) => item.join(",")
  },
  {
    ...timeColumn("Created At", "issue_created_at", { hidden: true }),
    colSpan: 2,
    align: "left"
  },
  { ...timeColumn("Updated At", "issue_updated_at"), align: "left" },
  ...jiraCommonTableColumns
];

export const azureStatTableConfig = [
  baseColumnConfig("Workitem", "workitem_id"),
  userColumn(),
  statusColumn(),
  coloredTagsColumn("Components", "components"),
  userColumn("Reporter", "reporter"),
  baseColumnConfig("Workitem Type", "workitem_type"),
  baseColumnConfig("Project", "project"),
  priorityColumn(),
  {
    ...baseColumnConfig("Labels", "labels"),
    render: (item: any) => item.join(",")
  },
  {
    ...timeColumn("Created At", "workitem_created_at"),
    colSpan: 2,
    align: "left"
  },
  dateRangeFilterColumn("Workitem Created At", "workitem_created_at"),
  dateRangeFilterColumn("Workitem Updated At", "workitem_updated_at"),
  timeRangeFilterColumn("Points", "Story Point", "story_point"),
  ...azureUtilityDrilldownFilterColumn
];

export const JiraReleaseTableConfig = [
  jiraKeyColumn,
  baseColumnConfig("Summary", "summary"),
  convertSecToDay("Total Time", "velocity_stage_total_time", "days"),
  timeColumn("Created At", "issue_created_at"),
  priorityColumn(),
  userColumn(),
  coloredTagsColumn("Components", "component_list"),
  baseColumnConfig("Bounces", "bounces", { sorter: true, align: "center", width: "6%" }),
  baseColumnConfig("Hops", "hops", { sorter: true, align: "center", width: "6%" }),
  userColumn("Reporter", "reporter"),
  statusColumn(),
  baseColumnConfig("Issue Type", "issue_type", { width: "5%" }),
  baseColumnConfig("Project", "project", { width: "5%" }),
  {
    ...baseColumnConfig("Labels", "labels"),
    render: (item: any) => item.join(",")
  },
  ...jiraCommonTableColumns
];
