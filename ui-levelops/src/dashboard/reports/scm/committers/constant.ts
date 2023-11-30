import { baseColumnConfig } from "utils/base-table-config";

export const METRIC_OPTIONS = [
  { value: "num_commits", label: "Number of Commits" },
  { value: "num_prs", label: "Number of PRs" },
  { value: "num_jira_issues", label: "Number of Issues" },
  { value: "num_changes", label: "Number of Lines Changed" },
  { value: "num_additions", label: "Number of Lines Added" },
  { value: "num_deletions", label: "Number of Lines Removed" },
  { value: "tech_breadth", label: "Tech Breadth" },
  { value: "repo_breadth", label: "Repo Breadth" },
  { value: "file_types", label: "File Extensions" }
];

export const SCM_COMMITTERS_REPORT_CHART_PROPS = {
  size: "small",
  columns: [
    baseColumnConfig("Committer", "name", { width: "10%", ellipsis: true }),
    baseColumnConfig("No. of Changes", "num_changes", { width: "10%", ellipsis: true }),
    baseColumnConfig("No. of Commits", "num_commits", { width: "10%", ellipsis: true, sorter: true }),
    baseColumnConfig("No. of PRs", "num_prs", { width: "10%", ellipsis: true, sorter: true }),
    baseColumnConfig("No. of Issues", "num_jira_issues", { width: "10%", ellipsis: true, sorter: true }),
    baseColumnConfig("Tech Breadth", "tech_breadth", { width: "10%", ellipsis: true, sorter: true }),
    baseColumnConfig("Repo Breadth", "repo_breadth", { width: "10%", ellipsis: true, sorter: true }),
    baseColumnConfig("File Extensions", "file_types", { width: "10%", ellipsis: true, sorter: true })
  ],
  chartProps: {}
};

export const REPORTS_FILTERS = {
  page_size: 10
};

export const SCM_COMMITTERS_API_BASED_FILTERS = ["committers", "authors"];
