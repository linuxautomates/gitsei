import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapDashboardToProps = dispatch => {
  return {
    dashboardsList: (filters, id = 0) => dispatch(actionTypes.dashboardsList(filters, id)),
    dashboardsGet: item => dispatch(actionTypes.dashboardsGet(item)),
    dashboardsCreate: item => dispatch(actionTypes.dashboardsCreate(item)),
    dashboardsDelete: filters => dispatch(actionTypes.dashboardsDelete(filters)),
    dashboardSet: (id, data) => dispatch(actionTypes.dashboardSet(id, data)),
    dashboardDefault: id => dispatch(actionTypes.dashboardDefault(id)),
    dashboardsBulkDelete: ids => dispatch(actionTypes.dashboardsBulkDelete(ids)),
    newDashboardUpdate: (dashboardId, form) => {
      dispatch(actionTypes.newDashboardUpdate(dashboardId, form));
    }
  };
};
