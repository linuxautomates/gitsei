import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { basicFiltersType } from "reduxConfigs/sagas/saga-types/jiraFiltersSaga.types";
import {
  EFFORT_INVESTMENT_SINGLE_STAT,
  EFFORT_INVESTMENT_TEAM_REPORT,
  JIRA_BURN_DOWN_REPORT,
  JIRA_EI_COMPLETED_ENGINEER_REPORT,
  JIRA_EI_ACTIVE_ENGINEER_REPORT,
  JIRA_EFFORT_INVESTMENT_STAT,
  JIRA_EFFORT_INVESTMENT_TREND_REPORT,
  JIRA_EPIC_PRIORITY_REPORT,
  JIRA_PROGRESS_REPORT,
  JIRA_PROGRESS_STAT,
  RESTAPI_READ,
  JIRA_EI_ALIGNMENT_REPORT,
  JIRA_RELEASE_TABLE_CSV_REPORT
} from "../actionTypes";

export const jiraBAProgressStat = (uri: string, method: string, filters: any, id = "0") => ({
  type: JIRA_PROGRESS_STAT,
  data: filters,
  id: id,
  uri: uri,
  method: method
});

export const effortInvestmentTeamReport = (uri: string, method: string, filters: any, id = "0") => ({
  type: EFFORT_INVESTMENT_TEAM_REPORT,
  data: filters,
  id: id,
  uri: uri,
  method: method
});

export const jiraEffortInvestmentStat = (uri: string, method: string, filters: any, id = "0", extra?: any) => ({
  type: JIRA_EFFORT_INVESTMENT_STAT,
  data: filters,
  id: id,
  uri: uri,
  method: method,
  extra
});

export const jiraBAProgressReport = (uri: string, method: string, filters: any, id = "0", extra?: any) => ({
  type: JIRA_PROGRESS_REPORT,
  data: filters,
  id: id,
  uri: uri,
  method: method,
  extra
});

export const jiraEpicPriorityReport = (uri: string, method: string, filters: any, id = "0", extra?: any) => ({
  type: JIRA_EPIC_PRIORITY_REPORT,
  data: filters,
  id: id,
  uri: uri,
  method: method,
  extra
});

export const jiraBurnDownReport = (uri: string, method: string, filters: any, id = "0", extra?: any) => ({
  type: JIRA_BURN_DOWN_REPORT,
  data: filters,
  id: id,
  uri: uri,
  method: method,
  extra
});

export const jiraEffortInvestmentTrendReport = (uri: string, method: string, filters: any, id = "0", extra?: any) => ({
  type: JIRA_EFFORT_INVESTMENT_TREND_REPORT,
  data: filters,
  id: id,
  uri: uri,
  method: method,
  extra,
  set_loading: true
});

export const epicPriorityList = (filters: any, id: string = "0", complete = null) => ({
  type: RESTAPI_READ,
  data: filters,
  complete,
  uri: "epic_priority_report",
  id,
  method: "list"
});

// for getting filter values for teams,organization or code_area
export const azureTeamsFilterValuesGet = (filters: { [x: string]: any; filter: any }, id: string = "0") => ({
  type: RESTAPI_READ,
  data: filters,
  uri: "issue_management_attributes_values",
  id,
  method: "list"
});

// BA 2.0 engineer table actions

/** For completed tickets report */

export const jiraAlignmentReport = (
  uri: string,
  method: string,
  filters: any,
  id = "0",
  extra?: basicMappingType<any>
) => ({
  type: JIRA_EI_ALIGNMENT_REPORT,
  data: filters,
  id: id,
  uri: uri,
  method: method,
  extra
});

/** For completed tickets report */

export const completedEffortEIEngineerReport = (
  id: string,
  filters: any,
  uri: string,
  extra?: basicMappingType<any>
) => ({
  type: JIRA_EI_COMPLETED_ENGINEER_REPORT,
  data: filters,
  id: id,
  uri: uri,
  method: "list",
  extra
});

/** For active work tickets report */

export const activeEffortEIEngineerReport = (id: string, filters: any, uri: string, extra?: basicMappingType<any>) => ({
  type: JIRA_EI_ACTIVE_ENGINEER_REPORT,
  data: filters,
  id: id,
  uri: uri,
  method: "list",
  extra
});

// export const jiraActiveTicketsEIEngineerReport = (id: string, filters: any) => ({
//   type: JIRA_EI_ACTIVE_ENGINEER_REPORT,
//   data: filters,
//   id: id,
//   uri: "active_effort_investment_tickets",
//   method: "list"
// });

// /** For active work story points report */
// export const jiraActiveStoryPointsEIEngineerReport = (id: string, filters: any) => ({
//   type: JIRA_EI_ACTIVE_ENGINEER_REPORT,
//   data: filters,
//   id: id,
//   uri: "active_effort_investment_story_points",
//   method: "list"
// });

export const jiraReleaseTableCsvReport = (id: string, filters: any, uri: string, extra?: basicMappingType<any>) => ({
  type: JIRA_RELEASE_TABLE_CSV_REPORT,
  data: filters,
  id: id,
  uri: uri,
  method: "list",
  extra
});