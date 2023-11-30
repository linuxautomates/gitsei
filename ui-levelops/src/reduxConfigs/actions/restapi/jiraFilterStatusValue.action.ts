import * as actions from "../actionTypes";

const uri = "jira_filter_values";

export const jiraStatusFilterValues = (filters: any, id = "0", complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: uri,
  id,
  complete: complete,
  method: "list"
});
