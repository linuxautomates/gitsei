import { IssueVisualizationTypes } from "dashboard/constants/typeConstants";
import { BASE_SCM_CHART_PROPS } from "../constant";

export const ACROSS_OPTIONS = [
  { label: "Project", value: "project" },
  { label: "Repo ID", value: "repo_id" },
  { label: "Branch", value: "branch" },
  { label: "Author", value: "author" },
  { label: "Reviewer", value: "reviewer" },
  { label: "PR Closed By Week", value: "pr_closed_week" },
  { label: "PR Closed By Month", value: "pr_closed_month" },
  { label: "PR Closed By Quarter", value: "pr_closed_quarter" }
];

export const METRIC_OPTIONS = [
  { value: "average_author_response_time", label: "Average Author Response Time" },
  { value: "median_author_response_time", label: "Median Author Response Time" },
  { value: "average_reviewer_response_time", label: "Average Reviewer Response Time" },
  { value: "median_reviewer_response_time", label: "Median Reviewer Response Time" }
];

export const VISUALIZATION_OPTIONS = [
  { label: "Pie Chart", value: IssueVisualizationTypes.PIE_CHART },
  { label: "Bar Chart", value: IssueVisualizationTypes.BAR_CHART },
  { label: "Line Chart", value: IssueVisualizationTypes.LINE_CHART },
  { label: "Smooth Area Chart", value: IssueVisualizationTypes.AREA_CHART },
  { label: "Stacked Smooth Area Chart", value: IssueVisualizationTypes.STACKED_AREA_CHART }
];

export const SCM_PRS_RESPONSE_TIME_REPORT_CHART_PROPS = {
  unit: "Days",
  barProps: [
    {
      name: "Average Author Response Time",
      dataKey: "average_author_response_time",
      unit: "Days"
    }
  ],
  stacked: false,
  chartProps: BASE_SCM_CHART_PROPS
};

export const SCM_PRS_RESPONSE_TIME_STACK_FILTER = [
  "project",
  "repo_id",
  "branch",
  "author",
  "reviewer",
  "pr_closed_week",
  "pr_closed_month",
  "pr_closed_quarter"
];