export const jiraZendeskReportBlockTimeFilterTransformation = (params: { timeFilterName: any }) => {
  const { timeFilterName } = params;
  return ["jira_issue_created_at", "start_time"].includes(timeFilterName);
};
