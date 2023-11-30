import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapSignaturesToProps = dispatch => {
  return {
    signatureLogsList: filters => dispatch(actionTypes.signatureLogsList(filters)),
    signatureLogsGet: id => dispatch(actionTypes.signatureLogsGet(id)),
    signaturesList: (filters, complete = null) => dispatch(actionTypes.signaturesList(filters, complete))
  };
};
