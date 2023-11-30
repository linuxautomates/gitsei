import * as actions from "../actionTypes";

const uri = "configs";

export const configsList = (filters, id = "0", complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: uri,
  method: "list",
  id: id,
  complete: complete
});

export const configsUpdate = (configs, id = 0, complete = null) => ({
  type: actions.RESTAPI_WRITE,
  data: configs,
  uri: uri,
  method: "update",
  id: id,
  complete: complete
});
