import * as actions from "../actionTypes";

const uri = "jira_priorities";

export const jiraPrioritiesList = (filters, id = "0", complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: uri,
  id: id,
  method: "list"
});

export const jiraPrioritiesUpdate = (id, payload, complete = null) => ({
  type: actions.RESTAPI_WRITE,
  data: payload,
  complete: complete,
  uri: uri,
  id: id,
  method: "update"
});
