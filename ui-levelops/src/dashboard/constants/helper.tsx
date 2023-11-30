import { Tooltip } from "antd";
import { ceil, get, isArray } from "lodash";
import moment from "moment";
import React from "react";
import { Dict } from "types/dict";
import { mapStringNumberKeysToNumber, stringToNumber } from "utils/commonUtils";
import { valuesToFilters } from "./constants";
import { scmTableReportType } from "./enums/scm-reports.enum";
import { WIDGET_DATA_SORT_FILTER_KEY } from "./filter-name.mapping";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "./WidgetDataSortingFilter.constant";
import { getXAxisLabel } from "shared-resources/charts/bar-chart/bar-chart.helper";
import { linesChangesColumn } from "dashboard/pages/dashboard-tickets/configs/common-table-columns";

export const apiPagerdutyWidgets = [
  "pagerduty_hotspot_report",
  "pagerduty_release_incidents",
  "pagerduty_ack_trend",
  "pagerduty_after_hours",
  "pagerduty_incident_report_trends"
];

export const jiraZendeskSalesforceWidgets = [
  "salesforce_time_across_stages",
  "zendesk_time_across_stages",
  "jira_zendesk_escalation_time_report",
  "jira_salesforce_escalation_time_report",
  "jira_salesforce_files_report",
  "jira_zendesk_files_report",
  "salesforce_c2f_trends",
  "zendesk_c2f_trends"
];

export const scmTableWidgets = ["scm_committers_report", "scm_repos_report", scmTableReportType.SCM_FILE_TYPES_REPORT];

export const getKeyForFilter = (filterName: string) => {
  const key = Object.keys(valuesToFilters).find(key => (valuesToFilters as any)[key] === filterName);
  return key ? key : filterName;
};

export const levelopsAcrossMap: { [x: string]: string } = {
  questionnaire_template_id: "questionnaire_template_ids",
  tags: "tag_ids",
  tag: "tag_ids",
  product: "product_ids",
  products: "product_ids",
  assignee: "assignee_user_ids",
  state: "status",
  reporters: "reporters"
};

export const statDefaultQuery = {
  time_period: 1,
  agg_type: "average"
};

export const hygieneDefaultSettings = {
  hideScore: false
};

export const getCodeCoverage = (data: any) => {
  const {
    functions_covered,
    total_functions,
    decisions_coverage,
    total_decisions,
    conditions_covered,
    total_conditions
  } = mapStringNumberKeysToNumber(data?.additional_counts?.bullseye_coverage_metrics || {});
  const total = total_functions + total_conditions + total_decisions;
  const covered = functions_covered + decisions_coverage + conditions_covered;
  if (isNaN(total) || isNaN(covered)) return 0;
  return (((total - covered) / total) * 100).toFixed(2);
};

export const getUncoveredCoverage = (data: any) => {
  const {
    functions_covered,
    total_functions,
    decisions_coverage,
    total_decisions,
    conditions_covered,
    total_conditions
  } = mapStringNumberKeysToNumber(data?.additional_counts?.bullseye_coverage_metrics || {});

  return {
    function_percentage_uncovered: (((total_functions - functions_covered) / total_functions) * 100).toFixed(2),
    condition_percentage_uncovered: (((total_conditions - conditions_covered) / total_conditions) * 100).toFixed(2),
    decision_percentage_uncovered: (((total_decisions - decisions_coverage) / total_decisions) * 100).toFixed(2)
  };
};

export const getBullseyeDataKeyValue = (item: any, key: string) => {
  let dataValue = get(item, ["additional_counts", "bullseye_coverage_metrics", key], 0);
  if (key === "coverage_percentage") {
    dataValue = getCodeCoverage(item);
  }
  if (key.includes("percentage_uncovered")) {
    dataValue = (getUncoveredCoverage(item) as any)[key];
  }
  return stringToNumber(dataValue);
};

export const getResolutionTime = (data: any) => {
  const { max, mean, median, min, p90 } = mapStringNumberKeysToNumber(data || {});

  return {
    min,
    max,
    median_resolution_time: median,
    average_resolution_time: mean,
    "90th_percentile_resolution_time": p90,
    median_time: median,
    average_time: mean,
    "90th_percentile_time": p90
  };
};

export const getJiraDataKeyValue = (item: any, key: string) => {
  if (key === "number_of_tickets_closed") {
    return get(item, ["total_tickets"], 0);
  } else {
    const value = (getResolutionTime(item) as any)[key];
    // converting it into days
    return value !== "No Data" ? parseFloat((value / 86400).toFixed(2)) : "No Data";
  }
};

export const getSCMDataKeyValue = (item: any, key: string) => {
  if (key === "number_of_tickets_closed") {
    return get(item, ["count"], 0);
  } else {
    const value = ceil((getResolutionTime(item) as any)[key] / (60 * 60 * 24)) as any;
    return value !== "No Data" ? value : "No Data";
  }
};

export const FILES_REPORT_ROOT_FOLDER_KEY = "root_folder_key";

export enum JiraSalesforceReports {
  JIRA_SALESFORCE_FILES_REPORT = "jira_salesforce_files_report",
  JIRA_SALESFORCE_ESCALATION_TIME_REPORT = "jira_salesforce_escalation_time_report",
  SALESFORCE_TIME_ACROSS_STAGES = "salesforce_time_across_stages",
  SALESFORCE_C2F_TREND = "salesforce_c2f_trends"
}

export enum JiraZendeskReports {
  JIRA_ZENDESK_FILES_REPORT = "jira_zendesk_files_report",
  JIRA_ZENDESK_ESCALATION_TIME_REPORT = "jira_zendesk_escalation_time_report",
  ZENDESK_TIME_ACROSS_STAGES = "zendesk_time_across_stages",
  ZENDESK_C2F_TREND = "zendesk_c2f_trends"
}

export enum FileReports {
  JIRA_SALESFORCE_FILES_REPORT = "jira_salesforce_files_report",
  JIRA_ZENDESK_FILES_REPORT = "jira_zendesk_files_report",
  SCM_FILES_REPORT = "scm_files_report",
  SCM_JIRA_FILES_REPORT = "scm_jira_files_report"
}

export enum FileReportRootURIs {
  JIRA_SALESFORCE_FILES_REPORT = "jira_salesforce_files_report",
  JIRA_ZENDESK_FILES_REPORT = "jira_zendesk_files_report",
  SCM_FILES_REPORT = "scm_files_root_folder_report",
  SCM_JIRA_FILES_REPORT = "scm_jira_files_root_folder_report"
}
export enum ZendeskStacksReportsKey {
  ZENDESK_STACKED_KEY = "zendesk_stacked_filters"
}

// Use this to add more applications
export enum ReportsApplicationType {
  ZENDESK = "zendesk",
  JIRA = "jira",
  JIRA_ZENDESK = "jirazendesk",
  BULLSEYE = "bullseye",
  SALESFORCE = "salesforce",
  GITHUB_JIRA = "githubjira",
  AZURE_DEVOPS = "azure_devops"
}

export enum CustomFieldMappingKey {
  CUSTOM_FIELD_MAPPING_KEY = "custom_field_mapping"
}

export const zendeskCustomFieldsMapping: Dict<string, string> = {
  across: "customAcross",
  custom_fields: "customFields",
  custom_stacks: "customStacks",
  exclude: "excludeCustomFields"
};

export const cicdTrendDefaultQuery = {
  interval: "month"
};

export const scmCicdDefaultQuery = {
  ...cicdTrendDefaultQuery,
  end_time: {
    $gt: moment.utc().subtract(6, "days").startOf("day").unix().toString(),
    $lt: moment.utc().unix().toString()
  },
  [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.TIME_BASED]
};

export const scmCodingDaysDefaultQuery = {
  interval: "week",
  committed_at: {
    $gt: moment.utc().subtract(6, "days").startOf("day").unix().toString(),
    $lt: moment.utc().unix().toString()
  },
  [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED]
};

export const scmCicdStatDefaultQuery = {
  time_period: 1,
  agg_type: "median"
};

export const jobCommitSingleStatDefaultQuery = {
  start_time: {
    $gt: moment.utc().subtract(1, "days").startOf("day").unix().toString(),
    $lt: moment.utc().unix().toString()
  },
  agg_type: "median"
};

export const cicdJobCountStatDefaultQuery = {
  ...scmCicdStatDefaultQuery,
  agg_type: "total"
};

export const scmCicdVolumeStatDefaultQuery = {
  ...scmCicdStatDefaultQuery,
  agg_type: "lines_changed"
};

export const scmCicdVolumeByFileStatDefaultQuery = {
  ...scmCicdStatDefaultQuery,
  agg_type: "files_changed"
};

export const leadTimeStatDefaultQuery = {
  calculation: "ticket_velocity"
};

export const shouldSliceFromEnd = (params: any) => {
  const { interval } = params;
  switch (interval) {
    case "day":
    case "week": {
      return true;
    }
    case "month":
    case "quarter":
      return false;
  }
  return false;
};

export const shouldReverseApiData = () => {
  return true;
};

export const xAxisLabelTransform = (params: any) => getXAxisLabel(params);

export type statTimeAndUnitDatatype = {
  time: number;
  unit: string;
  extraTime: number | undefined;
  extraUnit: string | undefined;
};

export const WIDGET_MIN_HEIGHT = "widget_height";

export const getSCMColumnsForMetrics = (metrics: string[]) => {
  let columns: any[] = [];
  metrics.forEach((metric: string) => {
    switch (metric) {
      case "num_commits":
        columns.push({
          title: <Tooltip title="Number of Commits">No. of Commits</Tooltip>,
          key: "num_commits",
          dataIndex: "num_commits",
          width: "10%",
          ellipsis: true,
          sorter: true
        });
        break;
      case "num_prs":
        columns.push({
          title: <Tooltip title="Number of PRs">No. of PRs</Tooltip>,
          key: "num_prs",
          dataIndex: "num_prs",
          width: "10%",
          ellipsis: true,
          sorter: true
        });
        break;
      case "num_jira_issues":
        columns.push({
          title: <Tooltip title="Number of Issues">No. of Issues</Tooltip>,
          key: "num_jira_issues",
          dataIndex: "num_jira_issues",
          width: "10%",
          ellipsis: true,
          sorter: true
        });
        break;
      case "num_workitems":
        columns.push({
          title: <Tooltip title="Number of Workitems">No. of Workitems</Tooltip>,
          key: "num_workitems",
          dataIndex: "num_workitems",
          width: "10%",
          ellipsis: true,
          sorter: true
        });
        break;
      case "num_changes":
        columns.push(linesChangesColumn);
        break;
      case "num_additions":
        columns.push({
          title: <Tooltip title="Number of Line Added">No. of Line Added</Tooltip>,
          key: "num_additions",
          dataIndex: "num_additions",
          width: "10%",
          ellipsis: true
        });
        break;
      case "num_deletions":
        columns.push({
          title: <Tooltip title="Number of Line Removed">No. of Line Removed</Tooltip>,
          key: "num_deletions",
          dataIndex: "num_deletions",
          width: "10%",
          ellipsis: true
        });
        break;
      case "tech_breadth":
        columns.push({
          title: <Tooltip title="Tech Breadth">Tech Breadth</Tooltip>,
          key: "tech_breadth",
          dataIndex: "tech_breadth",
          width: "10%",
          ellipsis: true,
          render: (value: any, record: any) => (isArray(value) ? value.join(", ") : value)
        });
        break;
      case "repo_breadth":
        columns.push({
          title: <Tooltip title="Repo Breadth">Repo Breadth</Tooltip>,
          key: "repo_breadth",
          dataIndex: "repo_breadth",
          width: "10%",
          ellipsis: true,
          render: (value: any, record: any) => (isArray(value) ? value.join(", ") : value)
        });
        break;
      case "file_types":
        columns.push({
          title: <Tooltip title="File Extensions">File Extensions</Tooltip>,
          key: "file_types",
          dataIndex: "file_types",
          width: "10%",
          ellipsis: true,
          render: (value: any, record: any) => (isArray(value) ? value.join(", ") : value)
        });
        break;
    }
  });
  return columns;
};
