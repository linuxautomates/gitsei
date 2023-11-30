import * as actions from "../actionTypes";

const uri = "propel_node_templates";

export const propelNodeTemplatesList = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: uri,
  method: "list"
});

export const propelNodeTemplatesGet = (id, complete = null) => ({
  type: actions.RESTAPI_READ,
  id: id,
  complete: complete,
  uri: uri,
  method: "get"
});
