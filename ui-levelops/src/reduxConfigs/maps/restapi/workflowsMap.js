import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapWorkflowsToProps = dispatch => {
  return {
    workflowsCreate: item => dispatch(actionTypes.workflowsCreate(item)),
    workflowsDelete: id => dispatch(actionTypes.workflowsDelete(id)),
    workflowsUpdate: (id, item) => dispatch(actionTypes.workflowsUdpate(id, item)),
    workflowsGet: id => dispatch(actionTypes.workflowsGet(id)),
    workflowsList: (filters, complete = null) => dispatch(actionTypes.workflowsList(filters, complete))
  };
};
