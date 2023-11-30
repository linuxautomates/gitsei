import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapReportsToProps = dispatch => {
  return {
    reportsList: (filters, complete = null) => dispatch(actionTypes.reportsList(filters, complete)),
    reportsGet: (id, complete = null) => dispatch(actionTypes.reportsGet(id, complete))
  };
};
