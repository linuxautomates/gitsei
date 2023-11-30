import { JIRA_SPRINT_FILTER_LIST, JIRA_SPRINT_REPORTS_LIST } from "../actionTypes";

const uri = "jira_sprint_report";

export const jiraSprintListReport = (data: { [key: string]: string | string[] }, id = "0") => ({
  type: JIRA_SPRINT_REPORTS_LIST,
  uri,
  method: "list",
  data,
  id
});

export const jiraSprintFilterList = (uri: string, method: string, data: any, id = "0") => ({
  type: JIRA_SPRINT_FILTER_LIST,
  uri,
  method: method,
  data,
  id
});
