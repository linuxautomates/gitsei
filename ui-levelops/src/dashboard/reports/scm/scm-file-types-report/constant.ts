export const METRIC_OPTIONS = [
  { value: "num_commits", label: "Number of Commits" },
  { value: "num_prs", label: "Number of PRs" },
  { value: "num_changes", label: "Number of Lines Changed" },
  { value: "num_additions", label: "Number of Lines Added" },
  { value: "num_deletions", label: "Number of Lines Removed" }
];

export const SCM_FILES_TYPES_REPORT_CHART_PROPS = {
  size: "small",
  columns: [
    {
      title: "File Type",
      key: "name",
      dataIndex: "name",
      width: "10%",
      ellipsis: true
    },
    {
      title: "No. of Changes",
      key: "num_changes",
      dataIndex: "num_changes",
      width: "10%",
      ellipsis: true
    },
    {
      title: "No. of Commits",
      key: "num_commits",
      dataIndex: "num_commits",
      width: "10%",
      ellipsis: true,
      sorter: true
    },
    {
      title: "No. of PRs",
      key: "num_prs",
      dataIndex: "num_prs",
      width: "10%",
      ellipsis: true,
      sorter: true
    },
    {
      title: "No. of Issues",
      key: "num_jira_issues",
      dataIndex: "num_jira_issues",
      width: "10%",
      ellipsis: true,
      sorter: true
    }
  ],
  chartProps: {}
};

export const REPORT_FILTERS = {
  page_size: 10
};

export const SCM_FILE_TYPES_API_BASED_FILTERS = ["committers", "authors"];
