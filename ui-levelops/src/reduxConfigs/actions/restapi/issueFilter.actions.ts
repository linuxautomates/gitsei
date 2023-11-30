import * as actions from "../actionTypes";

const issue_filter_uri = "issue_filters";

export const DEFAULT_FILTER_UUID = "default_issue_filters";

export const ISSUES_UUID = "issues_list";

export const getIssueFilter = (name: string) => ({
  type: actions.RESTAPI_READ,
  uri: issue_filter_uri,
  id: name,
  method: "get"
});

export const listIssueFilter = (filters = {}, id = "0") => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: issue_filter_uri,
  id,
  method: "list"
});

export const createIssueFilter = (data: { name: string; [key: string]: any }, id = "0") => ({
  type: actions.RESTAPI_WRITE,
  data,
  uri: issue_filter_uri,
  id,
  method: "create"
});

export const updateIssueFilter = (name: string, data: { [key: string]: any }) => ({
  type: actions.RESTAPI_WRITE,
  data,
  uri: issue_filter_uri,
  id: name,
  method: "update"
});

export const deleteIssueFilter = (name: string) => ({
  type: actions.RESTAPI_WRITE,
  uri: issue_filter_uri,
  id: name,
  method: "delete"
});

export const bulkDeleteIssueFilters = (names: string[], id = "0") => ({
  type: actions.RESTAPI_WRITE,
  payload: names,
  uri: issue_filter_uri,
  id,
  method: "bulkDelete"
});
