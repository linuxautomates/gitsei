import moment from "moment";
import {
  ISSUE_MANAGEMENT_REPORTS,
  JIRA_MANAGEMENT_TICKET_REPORT,
  JIRA_SPRINT_REPORTS,
  leadTimeReports
} from "dashboard/constants/applications/names";
import {
  AbsoluteTimeRange,
  RelativeTimeRangeDropDownPayload,
  RelativeTimeRangePayload
} from "../../../model/time/time-range";
import { RelativeTimeRangeUnits } from "../../../shared-resources/components/relative-time-range/constants";
import { IntervalType, IntervalTypeDisplay, jiraBAReportTypes } from "dashboard/constants/enums/jira-ba-reports.enum";

export const CreatedAtUpdateAtOptions = [
  {
    id: "last_week",
    label: "Last 7 days",
    mFactor: 7
  },
  {
    id: "last_month",
    label: "Last 30 days",
    mFactor: 30
  },
  {
    id: "last_quarter",
    label: "Last 90 days",
    mFactor: 90
  },
  {
    id: "last_6_months",
    label: "Last 180 days",
    mFactor: 180
  },
  {
    id: "last_year",
    label: "Last 365 days",
    mFactor: 365
  }
];

export const jiraEffortInvestmentTrendReportTimeRangeOptions = [
  {
    id: "last_4_week",
    label: "Last 4 weeks",
    mFactor: 7
  },
  {
    id: "last_4_month",
    label: "Last 4 months",
    mFactor: 30
  },
  {
    id: "last_4_quarter",
    label: "Last 4 quarters",
    mFactor: 120
  }
];

export const closedDateOptions = [
  {
    id: "last_7_days",
    label: "Last 7 Days",
    lowerLimit: 6,
    upperLimit: 7
  },
  {
    id: "last_4_weeks",
    label: "Last 4 Weeks",
    lowerLimit: 21,
    upperLimit: 28
  },
  {
    id: "last_4_months",
    label: "Last 4 Months",
    lowerLimit: 90,
    upperLimit: 123
  },
  {
    id: "last_4_quarters",
    label: "Last 4 Quarters",
    lowerLimit: 275,
    upperLimit: 366
  }
];

export const jiraEpicPriorityTimeRangeFilterOption = [
  {
    id: "last_4_week",
    label: "Last 4 weeks",
    mFactor: 7
  },
  {
    id: "last_4_month",
    label: "Last 4 months",
    mFactor: 30
  }
];

// These filters are used as time period options
export const jiraEffortInvestmentTimeRangeFilterOption = [
  {
    label: "Last 7 days",
    value: 7
  },
  {
    label: "Last 2 Weeks",
    value: 14
  },
  {
    label: "Last Month",
    value: 30
  },
  {
    label: "Last 3 Months",
    value: 90
  }
];

export const effortInvestmentTrendReportSampleInterval = [
  { label: IntervalTypeDisplay.WEEK, value: IntervalType.WEEK },
  { label: IntervalTypeDisplay.BI_WEEK, value: IntervalType.BI_WEEK },
  { label: IntervalTypeDisplay.MONTH, value: IntervalType.MONTH }
];
export const effortInvestmentTeamTimeRangeFilterOption = [
  {
    id: "last_4_week",
    label: "Last 4 weeks",
    mFactor: 7
  },
  {
    id: "last_8_week",
    label: "Last 8 weeks",
    mFactor: 14
  },
  {
    id: "last_4_month",
    label: "Last 4 months",
    mFactor: 30
  }
];

export const completedDateOptions = [
  {
    id: "last_week",
    label: "Last week",
    mFactor: 7
  },
  {
    id: "last_2_weeks",
    label: "Last 2 Weeks",
    mFactor: 14
  },
  {
    id: "last_month",
    label: "Last Month",
    mFactor: 30
  },
  {
    id: "last_3_months",
    label: "Last 3 Months",
    mFactor: 90
  }
];

export const timeBoundFilterKeys = [
  "issue_created_at",
  "issue_updated_at",
  "jira_issue_created_at",
  "jira_issue_updated_at",
  "pr_created_at",
  "pr_closed_at",
  "committed_at",
  "created_at",
  "salesforce_created_at",
  "salesforce_updated_at",
  "time_range",
  "ingested_at",
  "issue_resolved_at",
  "updated_at",
  "start_time",
  "created_after",
  "end_time",
  "completed_at",
  "disclosure_range",
  "publication_range",
  "snapshot_range",
  "jira_issue_resolved_at",
  "cicd_job_run_end_time",
  "pr_merged_at",
  "issue_due_at",
  "workitem_created_at",
  "workitem_updated_at",
  "workitem_resolved_at",
  "workitem_due_at",
  "incident_created_at",
  "incident_resolved_at",
  "alert_created_at",
  "alert_resolved_at",
  "resolution_time",
  "started_at",
  "planned_ended_at",
  "released_end_time"
];

export const allTimeFilterKeys = [
  "job_end",
  "trend",
  "issue_created",
  "issue_updated",
  "issue_resolved",
  "issue_due",
  ...timeBoundFilterKeys
];

export const sonarQubeMetricsOptions = [
  { value: "complexity", label: "Cyclomatic Complexity" },
  { value: "cognitive_complexity", label: "Cognitive Complexity" }
];

export const jiraResolutionTimeMetricsOptions = [
  { value: "median_resolution_time", label: "Median Resolution Time" },
  { value: "number_of_tickets_closed", label: "Number Of Tickets" },
  { value: "90th_percentile_resolution_time", label: "90th Percentile Resolution Time" },
  { value: "average_resolution_time", label: "Average Resolution Time" }
];

export const scmResolutionTimeMetricsOptions = [
  { value: "median_resolution_time", label: "Median Resolution Time" },
  { value: "number_of_tickets_closed", label: "Number Of Tickets" }
];
export const jiraSprintCommitToDoneMetricsOptions = [{ value: "avg_commit_to_done", label: "Ave. Done to Commit" }];

export const jiraSprintCreepMetricsOptions = [{ value: "avg_creep", label: "Ave. Sprint Creep" }];

export const jiraSprintCreepToDoneMetricsOptions = [{ value: "avg_creep_to_done", label: "Ave. Sprint Creep Done" }];

export const jiraSprintPercentageTrendMetricsOptions = [
  { value: "done_to_commit_ratio", label: "Delivered to Commit Ratio" },
  { value: "creep_to_commit_ratio", label: "Creep to Commit Ratio" },
  { value: "creep_done_to_commit_ratio", label: "Creep Done to Commit Ratio" },
  { value: "creep_done_ratio", label: "Creep Done Ratio" },
  { value: "creep_missed_ratio", label: "Creep Missed Ratio" },
  { value: "commit_missed_ratio", label: "Commit Missed Ratio" },
  { value: "commit_done_ratio", label: "Commit Done Ratio" }
];

export const jiraSprintTrendMetricsOptions = [
  { value: "commit_not_done_points", label: "Commit missed" },
  { value: "commit_done_points", label: "Commit done" },
  { value: "creep_done_points", label: "Creep done" },
  { value: "creep_not_done_points", label: "Creep missed" }
];

export const jiraSprintMetricOptions = {
  [JIRA_SPRINT_REPORTS.COMMIT_TO_DONE]: jiraSprintCommitToDoneMetricsOptions,
  [JIRA_SPRINT_REPORTS.SPRINT_CREEP]: jiraSprintCreepMetricsOptions,
  [JIRA_SPRINT_REPORTS.SPRINT_CREEP_DONE]: jiraSprintCreepToDoneMetricsOptions,
  [JIRA_SPRINT_REPORTS.SPRINT_METRICS_PERCENTAGE_TREND]: jiraSprintPercentageTrendMetricsOptions,
  [JIRA_SPRINT_REPORTS.SPRINT_METRICS_TREND]: jiraSprintTrendMetricsOptions,
  [JIRA_SPRINT_REPORTS.SPRINT_IMPACT]: jiraSprintCommitToDoneMetricsOptions
};

export const ticketReportMetricOptions = [
  { value: "ticket", label: "Number of tickets" },
  { value: "story_point", label: "Sum of story points" }
];

export const jiraResolutionTimeGroupByOptions = [
  { value: "week", label: "Last Closed Week" },
  { value: "month", label: "Last Closed Month" },
  { value: "quarter", label: "Last Closed Quarter" }
];

const CIRCLE_CHART = 1;
const LINE_CHART = 4;
const BAR_CHART = 3;

export const graphTypeOptions = [
  { value: BAR_CHART, label: "Bar" },
  { value: LINE_CHART, label: "Line" },
  { value: CIRCLE_CHART, label: "Pie Chart" }
];

export const sonarQubeCodeComplexityWigets = [
  "sonarqube_code_complexity_report",
  "sonarqube_code_complexity_trend_report"
];

export const backlogTrendReportOptions = [
  { value: "week", label: "Weekly on Monday" },
  { value: "month", label: "First day of the month" },
  { value: "quarter", label: "First day of the quarter" }
];

export const backlogLeftYAxisOptions = [
  { value: "total_tickets", label: "Number of tickets" },
  { value: "total_story_points", label: "Sum of Story Points" }
];

export const backlogRightYAxisOptions = [
  { value: "median", label: "Median Age of Tickets" },
  { value: "p90", label: "90th percentile Age of Tickets" },
  { value: "mean", label: "Average Age of Tickets" }
];

export const leadTimeCalculationOptions = [
  { value: "ticket_velocity", label: "Average Lead Time per Ticket" },
  { value: "pr_velocity", label: "Average Lead Time per PR" }
];
export const supportSystemOptions = [
  { value: "zendesk", label: "Zendesk" },
  { value: "salesforce", label: "Salesforce" }
];

export const scmMetricOptions = [
  { value: "num_commits", label: "Number of Commits" },
  { value: "num_prs", label: "Number of PRs" },
  { value: "num_jira_issues", label: "Number of Issues" },
  { value: "num_changes", label: "Number of Lines Changed" },
  { value: "num_additions", label: "Number of Lines Added" },
  { value: "num_deletions", label: "Number of Lines Removed" }
];

export const scmCommitterMetricOptions = [
  ...scmMetricOptions,
  { value: "tech_breadth", label: "Tech Breadth" },
  { value: "repo_breadth", label: "Repo Breadth" },
  { value: "file_types", label: "File Extensions" }
];

export const jiraTicketSingleStatMetricsOptions = [
  { value: "total_tickets", label: "Number of Issues" },
  { value: "total_story_points", label: "Sum Of Story Points" }
];

export const sonarQubeCodeDuplicationWidgets = [
  "sonarqube_code_duplication_report",
  "sonarqube_code_duplication_trend_report"
];

export const SPRINT_GRACE_OPTIONS = [
  { label: "30 minutes", value: 1800 },
  { label: "1 hour", value: 3600 },
  { label: "2 hours", value: 7200 },
  { label: "3 hours", value: 10800 },
  { label: "4 hours", value: 14400 },
  { label: "1 day", value: 86400 }
];

export const SPRINT_GRACE_OPTIONS_REPORTS = [
  "sprint_metrics_single_stat",
  "sprint_metrics_percentage_trend",
  "sprint_metrics_trend",
  "azure_sprint_metrics_single_stat",
  "azure_sprint_metrics_percentage_trend",
  "azure_sprint_metrics_trend"
];

export const SPRINT_GRACE_INFO =
  "Issues added to an active sprint within the grace period will be considered as committed tickets instead of unplanned creep.";

//removing github keys reason changes from LFE-569
export const rangeMap = {
  issue_created_at: "jira_issue_created_at",
  issue_updated_at: "jira_issue_updated_at",
  jira_issue_created_at: "jira_issue_created_at",
  jira_issue_updated_at: "jira_issue_updated_at",
  created_at: "jirazendesk_issue_created_at"
};

export const supportSystemToReportTypeMap: any = {
  salesforce_bounce_report: {
    salesforce: "salesforce_bounce_report",
    zendesk: "zendesk_bounce_report"
  },
  zendesk_bounce_report: {
    salesforce: "salesforce_bounce_report",
    zendesk: "zendesk_bounce_report"
  },
  salesforce_c2f_trends: {
    salesforce: "salesforce_c2f_trends",
    zendesk: "zendesk_c2f_trends"
  },
  zendesk_c2f_trends: {
    salesforce: "salesforce_c2f_trends",
    zendesk: "zendesk_c2f_trends"
  },
  jira_salesforce_report: {
    salesforce: "jira_salesforce_report",
    zendesk: "jira_zendesk_report"
  },
  jira_zendesk_report: {
    salesforce: "jira_salesforce_report",
    zendesk: "jira_zendesk_report"
  },
  salesforce_hops_report: {
    salesforce: "salesforce_hops_report",
    zendesk: "zendesk_hops_report"
  },
  zendesk_hops_report: {
    salesforce: "salesforce_hops_report",
    zendesk: "zendesk_hops_report"
  },
  jira_salesforce_files_report: {
    salesforce: "jira_salesforce_files_report",
    zendesk: "jira_zendesk_files_report"
  },
  jira_zendesk_files_report: {
    salesforce: "jira_salesforce_files_report",
    zendesk: "jira_zendesk_files_report"
  },
  salesforce_hygiene_report: {
    salesforce: "salesforce_hygiene_report",
    zendesk: "zendesk_hygiene_report"
  },
  zendesk_hygiene_report: {
    salesforce: "salesforce_hygiene_report",
    zendesk: "zendesk_hygiene_report"
  },
  salesforce_resolution_time_report: {
    salesforce: "salesforce_resolution_time_report",
    zendesk: "zendesk_resolution_time_report"
  },
  zendesk_resolution_time_report: {
    salesforce: "salesforce_resolution_time_report",
    zendesk: "zendesk_resolution_time_report"
  },
  jira_salesforce_escalation_time_report: {
    salesforce: "jira_salesforce_escalation_time_report",
    zendesk: "jira_zendesk_escalation_time_report"
  },
  jira_zendesk_escalation_time_report: {
    salesforce: "jira_salesforce_escalation_time_report",
    zendesk: "jira_zendesk_escalation_time_report"
  },
  salesforce_tickets_report: {
    salesforce: "salesforce_tickets_report",
    zendesk: "zendesk_tickets_report"
  },
  zendesk_tickets_report: {
    salesforce: "salesforce_tickets_report",
    zendesk: "zendesk_tickets_report"
  },
  salesforce_time_across_stages: {
    salesforce: "salesforce_time_across_stages",
    zendesk: "zendesk_time_across_stages"
  },
  zendesk_time_across_stages: {
    salesforce: "salesforce_time_across_stages",
    zendesk: "zendesk_time_across_stages"
  },
  salesforce_top_customers_report: {
    salesforce: "salesforce_top_customers_report",
    zendesk: "zendesk_top_customers_report"
  },
  zendesk_top_customers_report: {
    salesforce: "salesforce_top_customers_report",
    zendesk: "zendesk_top_customers_report"
  }
};

export const getStageDurationComputationOptions = (calculation: string) => {
  const isJiraReport = calculation === "ticket_velocity";
  return [
    {
      label: `Use all the filtered ${isJiraReport ? "tickets" : "PRs"}`,
      value: false
    },
    {
      label: `Exclude ${isJiraReport ? "tickets" : "PRs"} not in the workflow profile stage`,
      value: true
    }
  ];
};

export const timeFilterKey = (application: string, key: string, reportType?: string) => {
  switch (application) {
    case "jira":
    case "github":
    case "githubjira":
      if (
        leadTimeReports.includes(reportType as any) &&
        !["cicd_job_run_end_time", "pr_created_at", "pr_merged_at"].includes(key)
      ) {
        return `jira_${key}`;
      }
      return key;
    case "jirasalesforce":
      switch (reportType) {
        case "jira_salesforce_report":
          return `salesforce_${key}`;
        default:
          return `jira_${key}`;
      }
    case "jirazendesk":
      return `jira_${key}`;
    default:
      return key;
  }
};

export const modificationMappedValues = (value: any, options: any, stringify: boolean = true) => {
  const option = options.find((i: any) => i.id === value);
  if (option) {
    const now = moment().unix();
    const gt = now - option.mFactor * 86400;
    if (stringify) {
      return {
        $gt: gt.toString(),
        $lt: now.toString()
      };
    } else {
      return {
        $gt: gt,
        $lt: now
      };
    }
  }
  return {};
};

export const getMappedTimeRange = (value: any, stringify = true) => {
  const option = closedDateOptions.find((i: any) => i.id === value);
  if (option) {
    let gt: any;
    const lt = moment.utc().unix();
    switch (option.id) {
      case "last_7_days":
        gt = moment.utc().subtract(6, "days").startOf("day").unix();
        break;
      case "last_4_weeks":
        gt = moment.utc().startOf("week").subtract(3, "weeks").unix();
        break;
      case "last_4_months":
        gt = moment.utc().startOf("month").subtract(3, "month").unix();
        break;
      case "last_4_quarters":
        gt = moment.utc().subtract(3, "quarter").startOf("quarter").unix();
        break;
      default:
        return {};
    }
    if (stringify) {
      return {
        $gt: gt.toString(),
        $lt: lt.toString()
      };
    } else {
      return {
        $gt: gt,
        $lt: lt
      };
    }
  }
  return {};
};

export const getIdFromTimeRange = (value: { $gt: string; $lt: string }) => {
  if (!value) {
    return undefined;
  }
  const day = 86400;
  const diff = parseInt(value.$lt) - parseInt(value.$gt);
  const numOfDays = Math.round(diff / day);
  const option = closedDateOptions.find((i: any) => i.lowerLimit <= numOfDays && i.upperLimit >= numOfDays);
  return option ? option.id : undefined;
};

export const getModificationValue = (value: { $gt: string; $lt: string }, options: any) => {
  if (!value) {
    return undefined;
  }
  const day = 86400;
  const diff = parseInt(value.$lt) - parseInt(value.$gt);
  const numOfDays = Math.round(diff / day);
  const option = options.find((i: any) => i.mFactor === numOfDays);
  return option ? option.id : undefined;
};

export const updatedFilter = (
  value: { $gt: string; $lt: string },
  choice: string = "slicing",
  key?: string,
  options = closedDateOptions
) => {
  if (!value) {
    return undefined;
  }
  if (choice === "absolute") {
    return value;
  }

  // LEV-2114
  // The below code only works when $gt and $lt are defined. This is
  // not always the case, so require value to pass a condition.
  if (!!value && value.$gt && value.$lt) {
    if (key && ["issue_resolved_at", "end_time"].includes(key) && !!options.length) {
      const id = getIdFromTimeRange(value);
      return getMappedTimeRange(id);
    }

    const now = moment().unix();
    const diff = parseInt(value.$lt) - parseInt(value.$gt);

    return {
      $lt: now.toString(),
      $gt: (now - diff).toString()
    };
  }
};

// There are some things we don't want to allow in stacks
// options.
export interface StacksOptionsConstraintsOptions {
  filterAcross?: boolean;
  filterJobName?: boolean;
  filters: { [key: string]: any };
}

export const stringSortingComparator = (value1: any, value2: any) => {
  // sorting available options, alphabatically
  if (!!value1?.key && !!value2.key) {
    const key1 = value1?.key?.toLowerCase();
    const key2 = value2?.key?.toLowerCase();
    if (key1 < key2) return -1;
    if (key1 > key2) return 1;
    return 0;
  }
  // sorting available options, alphabatically
  const key1 = value1?.label?.toLowerCase();
  const key2 = value2?.label?.toLowerCase();
  if (key1 < key2) return -1;
  if (key1 > key2) return 1;
  return 0;
};

// Use this to sort filter options
export const getFiltersOptionsSortingComparater = (dataKey: string) => {
  return (value1: any, value2: any) => {
    const key1 = value1?.[dataKey]?.toLowerCase();
    const key2 = value2?.[dataKey]?.toLowerCase();
    if (key1 < key2) return -1;
    if (key1 > key2) return 1;
    return 0;
  };
};

export const getValueFromTimeRange = (data: RelativeTimeRangePayload, toString = false, partial = false) => {
  let $gt;
  let $lt;
  // returning undefined when no filter value
  // removing the isRequired dependency from the code because returning undefined when no filter is applied is same as isRequired
  if (
    (data?.last?.num === "" && data?.last?.unit !== "today") ||
    (data?.next?.num === "" && data?.next?.unit !== "today")
  ) {
    return undefined;
  }

  if (data.last) {
    // calculate $lt
    const isToday = data.last?.unit === RelativeTimeRangeUnits.TODAY;
    if (isToday) {
      $gt = moment.utc().startOf("day").unix();
    }
    if (!isToday && data.last?.num !== undefined) {
      $gt = moment
        .utc()
        .subtract(((parseInt(data.last?.num) || 1) - 1) as any, data.last.unit)
        .startOf(data.last.unit as any)
        .unix();
    }
  }

  if (data.next) {
    // calculate $gt
    const isToday = data.next?.unit === RelativeTimeRangeUnits.TODAY;
    if (isToday) {
      $lt = moment.utc().endOf("day").unix();
    }
    if (!isToday && data.next?.num !== undefined) {
      $lt = moment
        .utc()
        .add(data.next?.num as any, data.next?.unit)
        .endOf("day")
        .unix();
    } else if (!isToday) {
      $lt = moment.utc().endOf("day").unix();
    }
  } else if (!partial) {
    $lt = moment.utc().endOf("day").unix();
  }

  if ($gt && $lt) {
    return { $gt: toString ? $gt.toString() : $gt, $lt: toString ? $lt.toString() : $lt };
  }

  if (partial) {
    return { $gt: toString ? $gt?.toString() : $gt, $lt: toString ? $lt?.toString() : $lt };
  }

  return undefined;
};

export const getIssueManagementReportType = (reportType: string, issueManagementSystem: string) => {
  if (issueManagementSystem === "azure_devops") {
    switch (reportType) {
      case "lead_time_by_stage_report":
        return "azure_lead_time_by_stage_report";
      case "lead_time_trend_report":
        return "azure_lead_time_trend_report";
      case "lead_time_by_type_report":
        return "azure_lead_time_by_type_report";
      case "tickets_report":
        return "azure_tickets_report";
      case "tickets_counts_stat":
        return "azure_tickets_counts_stat";
      case "tickets_report_trends":
        return "azure_tickets_report_trends";
      case "jira_time_across_stages":
        return "azure_time_across_stages";
      case "hygiene_report":
        return "azure_hygiene_report";
      case "hygiene_report_trends":
        return "azure_hygiene_report_trends";
      case "jira_backlog_trend_report":
        return "azure_backlog_trend_report";
      case "effort_investment_single_stat":
        return "azure_effort_investment_single_stat";
      case "effort_investment_trend_report":
        return "azure_effort_investment_trend_report";
      case "lead_time_single_stat":
        return "azure_lead_time_single_stat";
      case "response_time_report":
        return "azure_response_time_report";
      case "resolution_time_report":
        return "azure_resolution_time_report";
      case "sprint_impact_estimated_ticket_report":
        return "azure_sprint_impact_estimated_ticket_report";
      case "response_time_counts_stat":
        return "azure_response_time_counts_stat";
      case "response_time_report_trends":
        return "azure_response_time_report_trends";
      case "sprint_metrics_single_stat":
        return "azure_sprint_metrics_single_stat";
      case "sprint_metrics_percentage_trend":
        return "azure_sprint_metrics_percentage_trend";
      case "sprint_metrics_trend":
        return "azure_sprint_metrics_trend";
      case "resolution_time_counts_stat":
        return "azure_resolution_time_counts_stat";
      case "resolution_time_report_trends":
        return "azure_resolution_time_report_trends";
      case "bounce_report":
        return "azure_bounce_report";
      case "bounce_report_trends":
        return "azure_bounce_report_trends";
      case "bounce_counts_stat":
        return "azure_bounce_counts_stat";
      case "hops_report":
        return "azure_hops_report";
      case "hops_report_trends":
        return "azure_hops_report_trends";
      case "hops_counts_stat":
        return "azure_hops_counts_stat";
      case "first_assignee_report":
        return "azure_first_assignee_report";
      case JIRA_MANAGEMENT_TICKET_REPORT.STAGE_BOUNCE_REPORT:
        return ISSUE_MANAGEMENT_REPORTS.STAGE_BOUNCE_REPORT;
      case JIRA_MANAGEMENT_TICKET_REPORT.STAGE_BOUNCE_SINGLE_STAT:
        return ISSUE_MANAGEMENT_REPORTS.STAGE_BOUNCE_SINGLE_STAT;
      case jiraBAReportTypes.JIRA_EFFORT_INVESTMENT_ENGINEER_REPORT:
        return ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_ENGINEER_REPORT;
      case jiraBAReportTypes.JIRA_EI_ALIGNMENT_REPORT:
        return ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_ALIGNMENT_REPORT;
      case jiraBAReportTypes.JIRA_PROGRESS_REPORT:
        return ISSUE_MANAGEMENT_REPORTS.AZURE_ISSUES_PROGRESS_REPORT;
    }
  }
  switch (reportType) {
    case "azure_lead_time_by_stage_report":
      return "lead_time_by_stage_report";
    case "azure_lead_time_trend_report":
      return "lead_time_trend_report";
    case "azure_lead_time_by_type_report":
      return "lead_time_by_type_report";
    case "azure_tickets_report":
      return "tickets_report";
    case "azure_tickets_counts_stat":
      return "tickets_counts_stat";
    case "azure_tickets_report_trends":
      return "tickets_report_trends";
    case "azure_time_across_stages":
      return "jira_time_across_stages";
    case "azure_hygiene_report":
      return "hygiene_report";
    case "azure_hygiene_report_trends":
      return "hygiene_report_trends";
    case "azure_backlog_trend_report":
      return "jira_backlog_trend_report";
    case "azure_effort_investment_trend_report":
      return "effort_investment_trend_report";
    case "azure_effort_investment_single_stat":
      return "effort_investment_single_stat";
    case "azure_lead_time_single_stat":
      return "lead_time_single_stat";
    case "azure_response_time_report":
      return "response_time_report";
    case "azure_resolution_time_report":
      return "resolution_time_report";
    case "azure_sprint_impact_estimated_ticket_report":
      return "sprint_impact_estimated_ticket_report";
    case "azure_response_time_counts_stat":
      return "response_time_counts_stat";
    case "azure_response_time_report_trends":
      return "response_time_report_trends";
    case "azure_sprint_metrics_single_stat":
      return "sprint_metrics_single_stat";
    case "azure_sprint_metrics_percentage_trend":
      return "sprint_metrics_percentage_trend";
    case "azure_sprint_metrics_trend":
      return "sprint_metrics_trend";
    case "azure_resolution_time_counts_stat":
      return "resolution_time_counts_stat";
    case "azure_resolution_time_report_trends":
      return "resolution_time_report_trends";
    case "azure_bounce_report":
      return "bounce_report";
    case "azure_bounce_report_trends":
      return "bounce_report_trends";
    case "azure_bounce_counts_stat":
      return "bounce_counts_stat";
    case "azure_hops_report":
      return "hops_report";
    case "azure_hops_report_trends":
      return "hops_report_trends";
    case "azure_hops_counts_stat":
      return "hops_counts_stat";
    case "azure_first_assignee_report":
      return "first_assignee_report";
    case ISSUE_MANAGEMENT_REPORTS.STAGE_BOUNCE_REPORT:
      return JIRA_MANAGEMENT_TICKET_REPORT.STAGE_BOUNCE_REPORT;
    case ISSUE_MANAGEMENT_REPORTS.STAGE_BOUNCE_SINGLE_STAT:
      return JIRA_MANAGEMENT_TICKET_REPORT.STAGE_BOUNCE_SINGLE_STAT;
    case ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_ENGINEER_REPORT:
      return jiraBAReportTypes.JIRA_EFFORT_INVESTMENT_ENGINEER_REPORT;
    case ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_ALIGNMENT_REPORT:
      return jiraBAReportTypes.JIRA_EI_ALIGNMENT_REPORT;
    case ISSUE_MANAGEMENT_REPORTS.AZURE_ISSUES_PROGRESS_REPORT:
      return jiraBAReportTypes.JIRA_PROGRESS_REPORT;
    default:
      return reportType;
  }
};

export const getRelativeValueFromTimeRange = (data?: AbsoluteTimeRange) => {
  if (!data) {
    return undefined;
  }

  const startOfDay = moment().utc().startOf("day").unix();
  const endOfDay = moment().utc().endOf("day").unix();
  const absoluteEnd = data?.$lt;
  const absoluteStart = data?.$gt;
  if (absoluteEnd && absoluteStart && absoluteEnd >= endOfDay && absoluteStart <= startOfDay) {
    const lastDays = Math.round((startOfDay - parseInt(absoluteStart.toString())) / 86400) + 1;
    const nextDays = Math.round((parseInt(absoluteEnd.toString()) - endOfDay) / 86400);

    return {
      next: {
        num: nextDays,
        unit: nextDays > 0 ? RelativeTimeRangeUnits.DAYS : RelativeTimeRangeUnits.TODAY
      },
      last: {
        num: lastDays,
        unit: lastDays > 0 ? RelativeTimeRangeUnits.DAYS : RelativeTimeRangeUnits.TODAY
      }
    };
  }

  return undefined;
};

export const getPartialRelativeValueFromTimeRange = (data?: AbsoluteTimeRange) => {
  if (!data) {
    return undefined;
  }

  const startOfDay = moment().utc().startOf("day").unix();
  const endOfDay = moment().utc().endOf("day").unix();
  const absoluteEnd = data?.$lt;
  const absoluteStart = data?.$gt;
  let next: RelativeTimeRangeDropDownPayload = { unit: RelativeTimeRangeUnits.DAYS };
  let last: RelativeTimeRangeDropDownPayload = { unit: RelativeTimeRangeUnits.DAYS };

  if (absoluteEnd) {
    const nextDays = Math.round((parseInt(absoluteEnd.toString()) - endOfDay) / 86400);
    next = {
      num: nextDays,
      unit: RelativeTimeRangeUnits.DAYS
    };
  }

  if (absoluteStart) {
    const lastDays = Math.round((startOfDay - parseInt(absoluteStart.toString())) / 86400);
    last = {
      num: lastDays,
      unit: RelativeTimeRangeUnits.DAYS
    };
  }

  return {
    next,
    last
  };
};

export const CustomTimeBasedTypes = ["date", "datetime", "dateTime"];

export const timeBasedFields = (item: any, list?: { key: string; type: string; name: string }[]) => {
  if (!list) {
    return false;
  }

  let key = Object.keys(item)[0];
  const label = key.split("@")[1];
  key = key.split("@")[0];
  if (key.includes("jira_")) {
    key = key.split("jira_")[1];
  }

  const allKeys = list.map(item => ({ key: item.key, name: item.name }));

  const index = allKeys.findIndex(_key => {
    if (label) {
      return _key.key === key && _key.name.toLowerCase() === label.toLowerCase();
    } else {
      return _key.key === key;
    }
  });

  if (index === -1) {
    return false;
  }

  return CustomTimeBasedTypes.includes(list[index].type);
};

export const DORA_REPORT_TO_KEY_MAPPING: Record<string, string> = {
  deployment_frequency_report: "deployment_frequency",
  leadTime_changes: "lead_time_for_changes",
  meanTime_restore: "mean_time_to_restore",
  change_failure_rate: "change_failure_rate"
};
