// best practices

import * as actions from "../actionTypes";

const uri = "bestpractices";

export const bpsGet = (id, complete = null) => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: uri,
  function: "getBp",
  method: "get",
  complete
});

export const bpsList = (filters, id = 0, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: uri,
  function: "getBps",
  method: "list",
  id
});

export const bpsSearch = filters => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: uri,
  function: "searchBps",
  method: "search"
});

export const bpsCreate = (bp, id = 0, complete = null) => ({
  type: actions.RESTAPI_WRITE,
  data: bp,
  uri: uri,
  id,
  function: "createBps",
  method: "create",
  complete
});

export const bpsUpdate = (id, bp) => ({
  type: actions.RESTAPI_WRITE,
  data: bp,
  id: id,
  uri: uri,
  function: "updateBps",
  method: "update"
});

export const bpsDelete = id => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  uri: uri,
  function: "deleteBps",
  method: "delete"
});

export const bpsSend = bp => ({
  type: actions.RESTAPI_WRITE,
  data: bp,
  uri: uri,
  function: "sendBps",
  method: "send"
});

export const bpsFileUpload = (id, file) => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  file: file,
  uri: uri,
  method: "upload"
});

export const bpsBulkDelete = ids => ({
  type: actions.RESTAPI_WRITE,
  id: "0",
  uri,
  method: "bulkDelete",
  payload: ids
});
