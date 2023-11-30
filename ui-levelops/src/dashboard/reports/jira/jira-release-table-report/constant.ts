import { chartProps } from "dashboard/reports/commonReports.constants";
import moment from "moment";

export const JIRA_RELEASE_TABLE_REPORT_CHART_PROPS = {
  unit: "Days",
  showStaticLegends: true,
  chartProps
};

export const JIRA_RELEASE_TABLE_REPORT_API_BASED_FILTERS = ["reporters", "assignees"];

export const JIRA_RELEASE_TABLE_REPORT_DEFAULT_QUERY = {
  issue_resolved_at: {
    $gt: moment.utc().subtract(3, "weeks").startOf("week").unix().toString(),
    $lt: moment.utc().unix().toString()
  },
  released_in: {
    $gt: moment.utc().subtract(3, "weeks").startOf("week").unix().toString(),
    $lt: moment.utc().unix().toString()
  }
};

export const JIRA_RELEASE_TABLE_REPORT_DESCRIPTION = "Visualize the releases occurring across various projects using the Jira releases report.";

export const CUSTOM_FIELD_KEY = "custom_fields";
