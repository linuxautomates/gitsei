import * as actions from "../actionTypes";

const uri = "plugin_aggs";

export const pluginAggsList = (filters, id = "0", complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: uri,
  id: id,
  method: "list",
  complete: complete
});

export const pluginAggsGet = (id, complete = null) => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: uri,
  method: "get",
  complete: complete
});
