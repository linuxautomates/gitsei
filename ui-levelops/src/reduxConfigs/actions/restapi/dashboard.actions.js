import * as actions from "../actionTypes";

const uri = "dashboards";

export const dashboardsList = (filters, id, complete = "dashboard-list-complete") => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: uri,
  method: "list",
  id: id,
  complete
});

export const dashboardsGet = dashboardId => ({
  type: actions.LOAD_DASHBOARD,
  id: dashboardId
});

export const dashboardsCreate = item => ({
  type: actions.RESTAPI_WRITE,
  data: item,
  uri: uri,
  method: "create"
});

export const dashboardsDelete = dashboardId => ({
  type: actions.RESTAPI_WRITE,
  id: dashboardId,
  uri: uri,
  method: "delete"
});

export const dashboardsBulkDelete = ids => ({
  type: actions.RESTAPI_WRITE,
  id: "0",
  uri,
  method: "bulkDelete",
  payload: ids
});

export const dashboardSet = (id, data) => ({
  type: actions.DASHBOARD_SET,
  method: "get",
  uri: uri,
  id: id,
  data: data
});

export const dashboardDefault = (id = "0") => ({
  type: actions.DASHBOARD_DEFAULT,
  id: id,
  uri,
  method: "list"
});

export const newDashboardUpdate = (dashboardId, form, dashboardSetting = false, clearDashboard = false) => ({
  type: actions.DASHBOARD_UPDATE,
  dashboardId,
  form,
  dashboardSetting,
  clearDashboard
});

export const loadDashboardIntegrations = dashboardId => ({
  type: actions.LOAD_DASHBOARD_INTEGRATIONS,
  dashboardId
});

export const getDataUpdateDashboardWidgets = (complete, integrationIds) => ({
  type: actions.UPDATE_DASHBOARD_WIDGETS,
  complete: complete,
  integrationIds
});

export const demoDashboardUpdate = (dashboardId, widgetId, updatedWidgetData) => ({
  type: actions.DEMO_DASHBOARD_UPDATE,
  dashboardId,
  widgetId,
  data: updatedWidgetData
});
