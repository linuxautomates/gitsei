import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapJiraZendeskProps = dispatch => {
  return {
    jiraZendeskGet: (uri, filters, id = "0") => dispatch(actionTypes.jiraZendeskGet(uri, filters, id))
  };
};
