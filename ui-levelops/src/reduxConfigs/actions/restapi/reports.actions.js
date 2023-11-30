import * as actions from "../actionTypes";

const uri = "reports";

export const reportsList = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: uri,
  method: "list",
  complete: complete
});

export const reportsGet = (id, complete = null) => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: uri,
  method: "get",
  complete: complete
});
