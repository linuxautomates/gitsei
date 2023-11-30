import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapJiraFieldsToProps = dispatch => {
  return {
    jiraFieldsList: (filters, complete = null) => dispatch(actionTypes.jiraFieldsList(filters, complete))
  };
};
