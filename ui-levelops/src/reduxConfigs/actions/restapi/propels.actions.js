import * as actions from "../actionTypes";

const uri = "propels";

export const prepelsCreate = (item, id = "0") => ({
  type: actions.RESTAPI_WRITE,
  data: item,
  uri: uri,
  method: "create",
  id: id
});

export const prepelsDelete = id => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  uri: uri,
  method: "delete"
});

export const prepelsUpdate = (id, item) => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  data: item,
  uri: uri,
  method: "update"
});

export const prepelsGet = (id, complete = null) => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: uri,
  method: "get",
  complete: complete
});

export const propelsList = (filters, id = "0", complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: uri,
  method: "list",
  id: id
});

export const prepelsBulk = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: uri,
  method: "bulk"
});

export const prepelsBulkDelete = ids => ({
  type: actions.RESTAPI_WRITE,
  payload: ids,
  uri,
  method: "bulkDelete",
  id: "0"
});
