import * as actions from "../actionTypes";

const uri = "propel_trigger_templates";

export const propelTriggerTemplatesList = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: uri,
  method: "list"
});

export const propelTriggerTemplatesGet = (id, complete = null) => ({
  type: actions.RESTAPI_READ,
  id: id,
  complete: complete,
  uri: uri,
  method: "get"
});
