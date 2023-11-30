import * as actions from "../actionTypes";

export const jiraZendeskGet = (uri, filters, id) => {
  return {
    type: actions.JIRA_ZENDESK,
    data: filters,
    uri: `${uri}_combined`,
    method: "list",
    id: id || "0"
  };
};
