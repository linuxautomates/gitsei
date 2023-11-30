import * as actions from "../actionTypes";

const uri = "workitem_priorities";

export const azurePrioritiesUpdate = (id: string, payload: any, complete: any = null) => ({
  type: actions.RESTAPI_WRITE,
  data: payload,
  complete: complete,
  uri: uri,
  id: id,
  method: "update"
});
