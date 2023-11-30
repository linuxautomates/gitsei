import * as actions from "../actionTypes";

const uri = "propel_reports";

export const propelReportsList = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: uri,
  method: "list"
});

export const propelReportsGet = (id, complete = null) => ({
  type: actions.RESTAPI_READ,
  id: id,
  complete: complete,
  uri: uri,
  method: "get"
});

export const propelReportsBulkDelete = ids => ({
  type: actions.RESTAPI_WRITE,
  id: "0",
  uri,
  method: "bulkDelete",
  payload: ids
});
