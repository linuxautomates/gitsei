import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapDashboardReportsToProps = dispatch => {
  return {
    dashboardReportsCreate: item => dispatch(actionTypes.dashboardReportsCreate(item)),
    dashboardReportsDelete: id => dispatch(actionTypes.dashboardReportsDelete(id)),
    dashboardReportsUpdate: (id, item) => dispatch(actionTypes.dashboardReportsUpdate(id, item)),
    dashboardReportsGet: (id, complete = null) => dispatch(actionTypes.dashboardReportsGet(id, complete)),
    dashboardReportsList: (filters, id = "0") => dispatch(actionTypes.dashboardReportsList(filters, id)),
    dashboardReportsUpload: (file, id = "0", data = {}) => dispatch(actionTypes.dashboardReportsUpload(file, id, data))
  };
};
