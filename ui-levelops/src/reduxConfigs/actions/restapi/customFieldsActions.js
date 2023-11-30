import * as actions from "../actionTypes";

export const customFieldsList = filters => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: "custom_fields",
  function: "getCustomFields",
  method: "list"
});

export const customFieldsUpdate = (workItemId, fields) => ({
  type: actions.RESTAPI_WRITE,
  id: workItemId,
  data: fields,
  uri: "custom_fields",
  function: "updateCustomFields",
  method: "update"
});
