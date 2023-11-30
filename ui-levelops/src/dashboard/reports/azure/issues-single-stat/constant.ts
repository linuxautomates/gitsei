import { issueSingleDefualtCreatedAt } from "dashboard/reports/jira/commonJiraReports.constants";
import { FilterConfigBasedPreviewFilterConfigType } from "model/report/baseReport.constant";
import moment from "moment";

export const STAT_TIME_BASED_FILTER_CONFIG = {
  options: [
    { value: "workitem_created_at", label: "Workitem Created" },
    { value: "workitem_resolved_at", label: "Workitem Resolved" },
    { value: "workitem_due_at", label: "Workitem Due" },
    { value: "workitem_updated_at", label: "Workitem Updated" }
  ],
  getFilterLabel: (data: any) => {
    const { filters } = data;
    return filters.across ? `${filters.across.replace("_at", "").replace("_", " ")} in` : "";
  },
  getFilterKey: (data: any) => {
    const { filters } = data;
    return filters.across || "";
  },
  defaultValue: "workitem_created_at"
};

export const REPORT_NAME = "Issues Single Stat";
export const URI = "issue_management_tickets_report";
export const STORY_POINT_URI = "issue_management_story_point_report";
export const EFFORT_URI = "issue_management_effort_report";

export const CHART_PROPS = {
  unit: "Tickets"
};

export const DEFAULT_ACROSS = "workitem_created_at";
export const DEFAULT_QUERY = {
  metric: ["ticket"],
  workitem_created_at: {
    // required filters and default is last month
    $gt: moment.utc().subtract(1, "months").startOf("day").unix().toString(),
    $lt: moment.utc().unix().toString()
  }
};

export const COMPARE_FIELD = "total_tickets";
export const SUPPORTED_WIDGET_TYPES = ["stats"];

export const METRIC_OPTIONS = [
  { value: "ticket", label: "Number of tickets" },
  { value: "story_point", label: "Sum of story points" },
  { value: "effort", label: "Sum of effort" }
];
export const METRIC_URI_MAPPING = "METRIC_URI_MAPPING";

export const AZURE_METRIC: any = {
  ticket: { key: "total_tickets", unit: "Tickets" },
  story_point: { key: "total_story_points", unit: "Points" },
  effort: { key: "total_effort", unit: "Effort" }
};

export const METRIC_URI_MAPPING_LIST = {
  ticket: URI,
  story_point: STORY_POINT_URI,
  effort: EFFORT_URI
};

export const FILTER_CONFIG_BASED_PREVIEW_FILTERS: FilterConfigBasedPreviewFilterConfigType[] = [
  { filter_key: "workitem_feature", valueKey: "workitem_id", labelKey: "summary" },
  { filter_key: "workitem_user_story", valueKey: "workitem_id", labelKey: "summary" }
];
