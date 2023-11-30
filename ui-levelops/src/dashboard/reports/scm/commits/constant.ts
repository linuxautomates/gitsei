import { IssueVisualizationTypes } from "dashboard/constants/typeConstants";
import { BASE_SCM_CHART_PROPS } from "../constant";

export const ACROSS_OPTIONS = [
  { label: "AUTHOR", value: "author" },
  { label: "By Day of Week", value: "trend_day" },
  { label: "Code Change Size", value: "code_change" },
  { label: "Committed in Month", value: "trend_month" },
  { label: "Committed in Quarter", value: "trend_quarter" },
  { label: "Committed in Week", value: "trend_week" },
  { label: "COMMITTER", value: "committer" },
  { label: "FILE TYPE", value: "file_type" },
  { label: "PROJECT", value: "project" },
  { label: "REPO ID", value: "repo_id" },
  { label: "technology", value: "technology" }
];

export const VISUALIZATION_OPTIONS = [
  { label: "Pie Chart", value: IssueVisualizationTypes.PIE_CHART },
  { label: "Bar Chart", value: IssueVisualizationTypes.BAR_CHART },
  { label: "Line Chart", value: IssueVisualizationTypes.LINE_CHART },
  { label: "Smooth Area Chart", value: IssueVisualizationTypes.AREA_CHART }
];

export const SCM_COMMITS_BY_REPO_DESCRIPTION =
  "Cumulative count of commits for each of the repositories for the selected duration";
export const SCM_COMMITS_REPORT_CHART_PROPS = {
  unit: "Counts",
  barProps: [
    {
      name: "count",
      dataKey: "count",
      unit: "count"
    }
  ],
  stacked: false,
  stackedArea: false,
  chartProps: BASE_SCM_CHART_PROPS
};

export const SCM_COMMIT_DEFAULT_QUERY = {
  visualization: IssueVisualizationTypes.PIE_CHART,
  code_change_size_unit: "lines"
};

export const SCM_COMMITS_API_BASED_FILTERS = ["creators", "committers", "authors", "assignees", "reviewers"];
