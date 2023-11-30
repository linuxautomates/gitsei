import * as actions from "../actionTypes";

const uri = "workflows";

export const workflowsCreate = item => ({
  type: actions.RESTAPI_WRITE,
  data: item,
  uri: uri,
  function: "createWorkflow",
  method: "create"
});

export const workflowsDelete = id => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  uri: uri,
  function: "deleteWorkflow",
  method: "delete"
});

export const workflowsUdpate = (id, item) => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  data: item,
  uri: uri,
  function: "updateWorkflow",
  method: "update"
});

export const workflowsGet = id => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: uri,
  function: "getWorkflow",
  method: "get"
});

export const workflowsList = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: uri,
  function: "getWorkflows",
  method: "list"
});
