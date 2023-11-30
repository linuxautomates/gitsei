import * as actions from "../actionTypes";

const uri = "dashboard_reports";

export const dashboardReportsCreate = item => ({
  type: actions.RESTAPI_READ,
  data: item,
  uri: uri,
  method: "create"
});

export const dashboardReportsDelete = id => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  uri: uri,
  method: "delete"
});

export const dashboardReportsUpdate = (id, item) => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  data: item,
  uri: uri,
  method: "update"
});

export const dashboardReportsGet = (id, complete = null) => ({
  type: actions.RESTAPI_READ,
  id: id,
  complete: complete,
  uri: uri,
  method: "get"
});

export const dashboardReportsList = (filters, id = "0") => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: uri,
  id: id,
  method: "list"
});

export const dashboardReportsUpload = (file, id = "0", data = {}) => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  file: file,
  data: data,
  uri: uri,
  method: "upload"
});
