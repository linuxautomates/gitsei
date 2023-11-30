// sso settings actions

import * as actions from "../actionTypes";
import { RestSamlconfig } from "../../../classes/RestSamlconfig";

const uri = "samlsso";

export const samlssoGet = (id = 0) => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: uri,
  function: "getSamlconfig",
  method: "get",
  validator: RestSamlconfig
});

export const samlssoUpdate = (id, samlsso) => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  data: samlsso,
  uri: uri,
  function: "updateSamlconfig",
  method: "update"
});

export const samlssoDelete = (id = 0) => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  uri: uri,
  function: "deleteSamlconfig",
  method: "delete"
});
