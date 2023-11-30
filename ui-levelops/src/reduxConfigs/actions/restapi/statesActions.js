import * as actions from "../actionTypes";

const uri = "states";

export const statesList = (filter = {}) => ({
  type: actions.RESTAPI_READ,
  data: filter,
  uri,
  method: "list"
});

export const statesGet = id => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: uri,
  method: "get"
});

export const statesCreate = item => ({
  type: actions.RESTAPI_WRITE,
  data: item,
  uri: uri,
  method: "create"
});

export const statesUpdate = (id, item) => ({
  type: actions.RESTAPI_WRITE,
  id,
  data: item,
  uri: uri,
  method: "update"
});

export const statesDelete = id => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  uri: uri,
  method: "delete"
});
