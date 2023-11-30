import * as actions from "../actionTypes";

const uri = "plugin_labels";

export const pluginLabelsList = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: uri,
  method: "list",
  complete: complete
});

export const pluginLabelsValues = (id, filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  id: id,
  data: filters,
  uri: uri,
  method: "values",
  complete: complete
});
