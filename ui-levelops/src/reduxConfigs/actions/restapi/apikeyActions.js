import * as actions from "../actionTypes";

const uri = "apikeys";

export const apikeysCreate = item => ({
  type: actions.RESTAPI_WRITE,
  data: item,
  uri: uri,
  method: "create"
});

export const apikeysDelete = id => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  uri: uri,
  method: "delete"
});

export const apikeysList = filters => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: uri,
  method: "list"
});
