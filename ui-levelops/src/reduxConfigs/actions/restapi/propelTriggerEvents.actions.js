import * as actions from "../actionTypes";

const uri = "propel_trigger_events";

export const propelTriggerEventsList = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: uri,
  method: "list"
});

export const propelTriggerEventsGet = (id, complete = null) => ({
  type: actions.RESTAPI_READ,
  id: id,
  complete: complete,
  uri: uri,
  method: "get"
});
