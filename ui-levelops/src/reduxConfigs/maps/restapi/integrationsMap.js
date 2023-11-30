import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapIntegrationsToProps = dispatch => {
  return {
    integrationsGet: id => dispatch(actionTypes.integrationsGet(id)),
    integrationsDelete: id => dispatch(actionTypes.integrationsDelete(id)),
    integrationsCreate: integration => dispatch(actionTypes.integrationsCreate(integration)),
    integrationsUpdate: (id, integration) => dispatch(actionTypes.integrationsUpdate(id, integration)),
    integrationsList: (filters, complete = null, id = "0") =>
      dispatch(actionTypes.integrationsList(filters, complete, id)),
    integrationsBulk: (filters, complete = null) => dispatch(actionTypes.integrationsBulk(filters, complete)),
    integrationsBulkDelete: ids => dispatch(actionTypes.integrationsBulkDelete(ids))
  };
};
