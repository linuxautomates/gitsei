import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { BAEffortTypeSwitchConfig } from "dashboard/dashboard-types/BAReports.types";
import {
  FEBasedDropDownFilterConfig,
  FEBasedTimeFilterConfig
} from "dashboard/dashboard-types/FEBasedFilterConfig.type";
import { defaultMaxEntriesOptions } from "dashboard/graph-filters/components/Constants";
import { Dict } from "types/dict";
import { ISSUE_MANAGEMENT_REPORTS, JIRA_MANAGEMENT_TICKET_REPORT } from "../applications/names";
import { allignmentStatus, EffortType, EffortUnitType, jiraBAReportTypes } from "../enums/jira-ba-reports.enum";
import { WidgetFilterType } from "../enums/WidgetFilterType.enum";
import { RANGE_FILTER_CHOICE } from "../filter-key.mapping";
import { withBaseStaticUrl } from "helper/helper";

// BA Reports support keys
export const STORE_ACTION = "STORE_ACTION"; // used to call report api through saga
export const URI_MAPPING = "URI_MAPPING";
export const MAX_RECORDS_OPTIONS_KEY = "MAX_RECORDS_OPTIONS_KEY";
export const MAX_RECORDS_LABEL = "MAX_RECORDS_LABEL"; // label for max record filter
export const WIDGET_MIN_HEIGHT = "widget_height";
export const SUPPORT_TICKET_CATEGORIZATION_FILTERS = "SUPPORT_TICKET_CATEGORIZATION_FILTERS";
export const SUPPORT_TICKET_CATEGORIZATION_UNIT_FILTERS = "SUPPORT_TICKET_CATEGORIZATION_UNIT_FILTERS";
export const SUPPORT_ACTIVE_WORK_UNIT_FILTERS = "support_active_work_unit";
export const SUPPORT_DISPLAY_FORMAT_FILTERS = "SUPPORT_DISPLAY_FORMAT_FILTERS";
export const SUPPORT_TIME_RANGE_FILTER = "SUPPORT_EPIC_PRIORITY_TIME_RANGE_FILTER";
export const SUPPORT_TREND_INTERVAL = "SUPPORT_TREND_INTERVAL";
export const BA_WIDGET_TIME_RANGE_FILTER_CONFIG = "BA_WIDGET_TIME_RANGE_FILTER_CONFIG";
export const SUPPORT_CATEGORY_EPIC_ACROSS_FILTER = "SUPPORT_CATEGORY_EPIC_ACROSS_FILTER";
export const DISABLE_CATEGORY_SELECTION = "DISABLE_CATEGORY_SELECTION"; // key for disabling category selection in ticket categorization filter
export const REQUIRED_FILTERS_MAPPING = "REQUIRED_FILTERS_MAPPING"; // Mapping key to specify required filters
export const DISABLE_XAXIS = "DISABLE_XAXIS";
export const INTERVAL_OPTIONS = "INTERVAL_OPTIONS";
export const CATEGORY_SELECTION_MODE = "CATEGORY_SELECTION_MODE";
export const SHOW_SPRINT_LINE_CHART_DOT = "show_sprint_line_chart_dot";
export const TIME_RANGE_DISPLAY_FORMAT_CONFIG = "TIME_RANGE_DISPLAY_FORMAT_CONFIG";
export const SHOW_EFFORT_UNIT_INSIDE_TAB = "SHOW_EFFORT_UNIT_INSIDE_TAB";
export const SHOW_SAMPLE_INTERVAL_INSIDE_TAB = "SHOW_SAMPLE_INTERVAL_INSIDE_TAB";
export const SHOW_MAX_RECORDS_INSIDE_TAB = "SHOW_MAX_RECORDS_INSIDE_TAB";
export const SHOW_PROFILE_INSIDE_TAB = "SHOw_PROFILE_INSIDE_TAB";
export const TICKET_CATEGORIZATION_UNIT_FILTER_KEY = "uri_unit";
export const ACTIVE_WORK_UNIT_FILTER_KEY = "active_work_unit";
export const IGNORE_SUPPORTED_FILTERS_KEYS = "IGNORE_SUPPORTED_FILTERS_KEYS";
export const BA_EFFORT_ATTRIBUTION_BE_KEY = "ba_attribution_mode";
export const BA_HISTORICAL_ASSIGNEES_STATUS_BE_KEY = "ba_historical_assignees_statuses";
export const BA_IN_PROGRESS_STATUS_BE_KEY = "ba_in_progress_statuses";
export const BA_COMPLETED_WORK_STATUS_BE_KEY = "ba_completed_work_statuses";
export const WORKITEW_STATUS_CATEGORIES = "workitem_status_categories";
export const JIRA_STATUS_CATEGORIES = "status_categories";
/**
 * Support key for Report CSV Download
 */
export const REPORT_CSV_DOWNLOAD_CONFIG = "REPORT_CSV_DOWNLOAD_CONFIG";
export const SUB_COLUMNS_TITLE = "SUB_COLUMNS_TITLE";
export const EXCLUDE_SUB_COLUMNS_FOR = "EXCLUDE_SUB_COLUMNS_FOR";

// BA  Reports filter keys
export const TICKET_CATEGORIZATION_SCHEMES_KEY = "ticket_categorization_scheme"; // filter key for scheme selection filter
export const TICKET_CATEGORIZATION_SCHEME_CATEGORY_KEY = "ticket_categories"; // filter key for category selection filter
export const EPIC_FILTER_KEY = "epics"; //  filter key for epics selection when across is epic
export const EFFORT_INVESTMENT_STAT_TIME_FILTER_KEY = "time_period"; // temp time range filter key for effort investment single stat
export const BA_TIME_RANGE_FILTER_KEY = "ba_time_range"; // temp time range filter key

// List of Default keys used in BA reports
export enum DefaultKeyTypes {
  DEFAULT_DISPLAY_FORMAT_KEY = "default_display_format", // to specify default display format for BA reports
  DEFAULT_EFFORT_UNIT = "default_effort_unit", // to specify default option effort unit filter for BA reports
  DEFAULT_SCHEME_KEY = "default_scheme_key", // to specify whether to show default scheme as default or not
  DEFAULT_TIME_RANGE_FILTER = "default_time_range_filter", // to specify default value for time range filter for BA reports
  DEFAULT_ACTIVE_WORK_UNIT = "default_active_work_unit" // to specify default value for active work filter for BA reports
}

// List the filters that can be required for a report
export enum RequiredFiltersType {
  SCHEME_SELECTION = "SCHEME_SELECTION", // for ticket categorization filters
  CATEGORY_SELECTION = "CATEGORY_SELECTION" // for ticket categorization filters
}

// options for max records
export const jiraProgressMaxRecordOptions = [{ label: "5", value: 5 }, ...defaultMaxEntriesOptions];

// options for interval filter
export const jiraBAIntervalFilterOptions = [
  { label: "Week", value: "week" },
  { label: "Month", value: "month" }
];

export const jiraBAReports = [
  jiraBAReportTypes.JIRA_BURNDOWN_REPORT,
  jiraBAReportTypes.EFFORT_INVESTMENT_TREND_REPORT,
  jiraBAReportTypes.JIRA_PROGRESS_REPORT,
  jiraBAReportTypes.EPIC_PRIORITY_TREND_REPORT,
  jiraBAReportTypes.EFFORT_INVESTMENT_TEAM_REPORT,
  jiraBAReportTypes.JIRA_EFFORT_INVESTMENT_ENGINEER_REPORT,
  jiraBAReportTypes.JIRA_EI_ALIGNMENT_REPORT
];

export const azureBAReports = [
  ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_SINGLE_STAT_REPORT,
  ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_TREND_REPORT,
  ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_ENGINEER_REPORT,
  ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_ALIGNMENT_REPORT,
  ISSUE_MANAGEMENT_REPORTS.AZURE_ISSUES_PROGRESS_REPORT,
  ISSUE_MANAGEMENT_REPORTS.AZURE_PROGRAM_PROGRESS_REPORT
];

export const jiraBAStatReports = [
  jiraBAReportTypes.JIRA_PROGRESS_SINGLE_STAT,
  jiraBAReportTypes.EFFORT_INVESTMENT_SINGLE_STAT
];

// These includes reports that supports visualization of data on the basis of sprints
export const jiraBASprintReports = [
  jiraBAReportTypes.JIRA_BURNDOWN_REPORT,
  jiraBAReportTypes.EFFORT_INVESTMENT_TREND_REPORT,
  jiraBAReportTypes.EFFORT_INVESTMENT_TEAM_REPORT,
  jiraBAReportTypes.EPIC_PRIORITY_TREND_REPORT
];

export const extraReportWithTicketCategorizationFilter = [
  "resolution_time_report",
  "tickets_report",
  "azure_resolution_time_report",
  "azure_tickets_report"
];

// Mapping between effort type and URI to be used
export const engineerTableEffortTypeToURIMapping: BAEffortTypeSwitchConfig<string> = {
  [EffortType.COMPLETED_EFFORT]: {
    [EffortUnitType.TICKETS_REPORT]: "effort_investment_tickets",
    [EffortUnitType.STORY_POINT_REPORT]: "effort_investment_story_points",
    [EffortUnitType.COMMIT_COUNT]: "scm_jira_commits_count_ba",
    [EffortUnitType.TICKET_TIME_SPENT]: "effort_investment_time_spent",
    [EffortUnitType.AZURE_TICKETS_REPORT]: "azure_effort_investment_tickets",
    [EffortUnitType.AZURE_STORY_POINT_REPORT]: "azure_effort_investment_story_point",
    [EffortUnitType.AZURE_COMMIT_COUNT]: "azure_effort_investment_commit_count",
    [EffortUnitType.AZURE_TICKET_TIME_SPENT]: "azure_effort_investment_time_spent"
  },
  [EffortType.ACTIVE_EFFORT]: {
    [EffortUnitType.TICKETS_REPORT]: "active_effort_investment_tickets",
    [EffortUnitType.STORY_POINT_REPORT]: "active_effort_investment_story_points",
    [EffortUnitType.COMMIT_COUNT]: "",
    [EffortUnitType.TICKET_TIME_SPENT]: "",
    [EffortUnitType.AZURE_TICKETS_REPORT]: "active_azure_ei_ticket_count",
    [EffortUnitType.AZURE_STORY_POINT_REPORT]: "active_azure_ei_story_point",
    [EffortUnitType.AZURE_COMMIT_COUNT]: "",
    [EffortUnitType.AZURE_TICKET_TIME_SPENT]: ""
  }
};

export const effortTypeBAReports = [jiraBAReportTypes.JIRA_EFFORT_INVESTMENT_ENGINEER_REPORT];

export const alignmentStatusMapping: Dict<string, allignmentStatus> = {
  "3": allignmentStatus.GOOD,
  "2": allignmentStatus.ACCEPTABLE,
  "1": allignmentStatus.POOR,
  "0": allignmentStatus.POOR
};

export const alignmentToSVGMapping = (): { [x in allignmentStatus]: string } => ({
  [allignmentStatus.GOOD]: withBaseStaticUrl("static/AlignmentAssets/good.svg"),
  [allignmentStatus.ACCEPTABLE]: withBaseStaticUrl("static/AlignmentAssets/Fair.svg"),
  [allignmentStatus.POOR]: withBaseStaticUrl("static/AlignmentAssets/poor.svg")
});

export enum EffortAttributionOptions {
  CURRENT_ASSIGNEE = "current_assignee",
  CURRENT_ASSIGNEE_AND_PREV_ASSIGNEE = "current_and_previous_assignees"
}

/** issue resolved at filter config for BA reports */
export const ba_issue_resolved_at: FEBasedTimeFilterConfig = {
  type: WidgetFilterType.TIME_BASED_FILTERS,
  label: "Issue Resolved At",
  BE_key: "issue_resolved_at",
  slicing_value_support: false,
  configTab: WIDGET_CONFIGURATION_KEYS.FILTERS,
  required: true,
  options: []
};

export const effortAttributionOptions = [
  { label: "Only use current assignee", value: EffortAttributionOptions.CURRENT_ASSIGNEE },
  { label: "Use current and previous assignees", value: EffortAttributionOptions.CURRENT_ASSIGNEE_AND_PREV_ASSIGNEE }
];

export const effortAttributionFilter: FEBasedDropDownFilterConfig = {
  type: WidgetFilterType.DROPDOWN_BASED_FILTERS,
  label: "Effort attribution",
  BE_key: BA_EFFORT_ATTRIBUTION_BE_KEY,
  configTab: WIDGET_CONFIGURATION_KEYS.SETTINGS,
  required: false,
  defaultValue: EffortAttributionOptions.CURRENT_ASSIGNEE,
  options: effortAttributionOptions
};

export const filterAssigneeByStatusFilter: FEBasedDropDownFilterConfig = {
  type: WidgetFilterType.DROPDOWN_BASED_FILTERS,
  label: "Filter assignees by status",
  BE_key: BA_HISTORICAL_ASSIGNEES_STATUS_BE_KEY,
  filterInfo: "If empty, the effort will be attributed to all the assignees of a ticket",
  configTab: WIDGET_CONFIGURATION_KEYS.SETTINGS,
  required: false,
  select_mode: "multiple",
  options: []
};

export const baInProgressStatusFilter: FEBasedDropDownFilterConfig = {
  type: WidgetFilterType.DROPDOWN_BASED_FILTERS,
  label: "Statuses of in progress issues",
  BE_key: BA_IN_PROGRESS_STATUS_BE_KEY,
  filterInfo: 'By default, all statuses in the "In Progress" category will be used',
  configTab: WIDGET_CONFIGURATION_KEYS.SETTINGS,
  required: false,
  select_mode: "multiple",
  options: []
};

export const statusOfTheCompletedIssues: FEBasedDropDownFilterConfig = {
  type: WidgetFilterType.DROPDOWN_BASED_FILTERS,
  label: "Statuses of completed issues",
  BE_key: BA_COMPLETED_WORK_STATUS_BE_KEY,
  filterInfo: 'By default, all statuses in the "Done" category will be used',
  configTab: WIDGET_CONFIGURATION_KEYS.SETTINGS,
  required: false,
  select_mode: "multiple",
  options: []
};

export const BA_FE_BASED_FILETRS_WITH_STATUS_OPTIONS: string[] = [
  "filterAssigneeByStatusFilter",
  "baInProgressStatusFilter",
  "statusOfTheCompletedIssues"
];

export const effortInvestmentTimeRangeDefMeta = {
  [RANGE_FILTER_CHOICE]: {
    issue_resolved_at: {
      type: "relative",
      relative: {
        last: {
          num: 4,
          unit: "months"
        },
        next: {
          unit: "today"
        }
      }
    },
    committed_at: {
      type: "relative",
      relative: {
        last: {
          num: 4,
          unit: "months"
        },
        next: {
          unit: "today"
        }
      }
    }
  }
};

export const azureEITimeRangeDefMeta = {
  [RANGE_FILTER_CHOICE]: {
    workitem_resolved_at: {
      type: "relative",
      relative: {
        last: {
          num: 4,
          unit: "months"
        },
        next: {
          unit: "today"
        }
      }
    },
    committed_at: {
      type: "relative",
      relative: {
        last: {
          num: 4,
          unit: "months"
        },
        next: {
          unit: "today"
        }
      }
    }
  }
};

export const ticketCategorizationUnitFilterOptions = [
  {
    label: "Ticket Count",
    value: "tickets_report"
  },
  { label: "Story Point", value: "story_point_report" },
  { label: "Commit Count", value: "commit_count_fte" },
  { label: "Ticket Time Spent", value: "effort_investment_time_spent" }
];

export const azureEffortInvestmentUnitFilterOptions = [
  {
    label: "Ticket Count",
    value: "azure_effort_investment_tickets"
  },
  { label: "Story Point", value: "azure_effort_investment_story_point" },
  { label: "Commit Count", value: "azure_effort_investment_commit_count" },
  { label: "Ticket Time Spent", value: "azure_effort_investment_time_spent" }
];

export const jiraActiveWorkUnitOptions = [
  {
    label: "Ticket Count",
    value: "active_effort_investment_tickets"
  },
  { label: "Story Point", value: "active_effort_investment_story_points" }
];

export const azureActiveWorkUnitOptions = [
  {
    label: "Ticket Count",
    value: "active_azure_ei_ticket_count"
  },
  { label: "Story Point", value: "active_azure_ei_story_point" }
];

// new mapping for BA 2.0, so that we can support existing reports
export const uriUnitMapping = {
  tickets_report: "effort_investment_tickets",
  story_point_report: "effort_investment_story_points",
  commit_count_fte: "scm_jira_commits_count_ba",
  effort_investment_time_spent: "effort_investment_time_spent",
  azure_effort_investment_time_spent: "azure_effort_investment_time_spent",
  azure_effort_investment_tickets: "azure_effort_investment_tickets",
  azure_effort_investment_story_point: "azure_effort_investment_story_point",
  azure_effort_investment_commit_count: "azure_effort_investment_commit_count"
};

// reports supporting active work and having x_axis as an array
export const ACTIVE_WORK_DRILLDOWN_TITLE_REPORTS = [
  jiraBAReportTypes.EFFORT_INVESTMENT_TREND_REPORT,
  ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_TREND_REPORT
];

export const KEYS_TO_UNSET_WHEN_BA_WITH_COMMIT_COUNT = [
  "baInProgressStatusFilter",
  "statusOfTheCompletedIssues",
  "effortAttributionFilter",
  "filterAssigneeByStatusFilter",
  "workitem_updated_at",
  "ba_issue_resolved_at",
  "ba_workitem_resolved_at",
  "workitem_created_at"
];

/**
 * This is part of the payload for effort investment report payload
 * {
 *   ...
 *   "workitem_status_categories": [ ... ]
 *   ...
 * }
 */
export const WORKITEM_STATUS_CATEGORIES_ADO = ["Done", "Resolved", "Completed", "Closed"];
