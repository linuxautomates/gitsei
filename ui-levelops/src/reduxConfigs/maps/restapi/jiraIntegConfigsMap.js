import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapJiraIntegConfigsToProps = dispatch => {
  return {
    integrationConfigsList: (filters, complete = null) =>
      dispatch(actionTypes.integrationConfigsList(filters, complete)),
    integrationConfigsCreate: data => dispatch(actionTypes.integrationConfigsCreate(data))
  };
};
