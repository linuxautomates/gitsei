import * as actions from "../actionTypes";

const uri = "plugins";
const csv_uri = "plugins_csv";

export const pluginsList = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: uri,
  method: "list",
  complete: complete
});

export const pluginsUpload = (id, file, complete = null) => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  file: file,
  uri: uri,
  method: "upload",
  complete: complete
});

export const pluginsCSVUpload = (file, data, complete = null) => ({
  type: actions.RESTAPI_WRITE,
  file: file,
  data: data,
  uri: csv_uri,
  method: "upload",
  complete: complete
});

export const pluginsTrigger = (id, data, complete = null) => ({
  type: actions.RESTAPI_READ,
  id,
  data,
  uri,
  method: "trigger",
  complete: complete
});

export const pluginsGet = (id, complete = null) => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: uri,
  method: "get",
  complete: complete
});
