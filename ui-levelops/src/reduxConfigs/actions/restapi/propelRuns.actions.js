import * as actions from "../actionTypes";

const uri = "propel_runs";

export const propelRunsList = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: uri,
  method: "list"
});

export const propelRunsGet = (id, complete = null) => ({
  type: actions.RESTAPI_READ,
  id: id,
  complete: complete,
  uri: uri,
  method: "get"
});

export const propelRunsBulkDelete = ids => ({
  type: actions.RESTAPI_WRITE,
  payload: ids,
  uri,
  method: "bulkDelete",
  id: "0"
});

export const propelRunRerun = id => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: "propels_run_rerun",
  method: "get"
});
