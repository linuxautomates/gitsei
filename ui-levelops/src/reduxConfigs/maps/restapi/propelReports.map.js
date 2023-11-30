import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapPropelReportsToProps = dispatch => {
  return {
    propelReportsGet: (id, complete = null) => dispatch(actionTypes.propelReportsGet(id, complete)),
    propelReportsList: (filters, complete = null) => dispatch(actionTypes.propelReportsList(filters, complete)),
    propelReportsBulkDelete: ids => dispatch(actionTypes.propelReportsBulkDelete(ids))
  };
};
