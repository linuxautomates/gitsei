import * as actions from "../actionTypes";

const uri = "plugin_results";

export const pluginResultsList = (filters, id = "0", complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: uri,
  id: id,
  method: "list",
  complete: complete
});

export const pluginResultsDiff = (before, after, complete = null) => ({
  type: actions.RESTAPI_READ,
  before: before,
  after: after,
  uri: uri,
  method: "diff",
  complete: complete
});

export const pluginResultsGet = (id, complete = null) => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: uri,
  method: "get",
  complete: complete
});

export const pluginResultsUpdate = (id, data, complete = null) => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  data: data,
  uri: uri,
  method: "update",
  complete: complete
});

export const pluginResultsBulkDelete = ids => ({
  type: actions.RESTAPI_WRITE,
  id: "0",
  payload: ids,
  uri,
  method: "bulkDelete"
});
