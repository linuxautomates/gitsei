import * as actions from "../actionTypes";

export const jiraSalesforceGet = (uri, filters, id) => {
  return {
    type: actions.JIRA_SALESFORCE,
    data: filters,
    uri: `${uri}_combined`,
    method: "list",
    id: id || "0"
  };
};
