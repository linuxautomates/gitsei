import { BASE_SCM_CHART_PROPS } from "../constant";

export const ACROSS_OPTIONS = [
  { label: "Commit Week", value: "trend_week" },
  { label: "Commit Month", value: "trend_month" },
  { label: "Repo Id", value: "repo_id" },
  { label: "Committer", value: "committer" },
  { label: "Author", value: "author" },
  { label: "Project", value: "project" },
  { label: "File Type", value: "file_type" }
];

export const SCM_REWORK_DESCRIPTION =
  "Refactor the code i.e. rewrite or make modifications to the existing codebase. Legacy refactored lines are the lines of code which is older than 30 days or as defined in the settings. Refactored lines are the latest modifications made to the codebase which is lesser than 30 days.";

export const SCM_REWORK_REPORT_CHART_PROPS = {
  unit: "Lines of code",
  barProps: [
    {
      name: "Number of lines of code",
      dataKey: "total_lines_changed"
    }
  ],
  stacked: true,
  chartProps: BASE_SCM_CHART_PROPS
};

export const SCM_REWORK_API_BASED_FILTERS = ["committers", "authors"];
