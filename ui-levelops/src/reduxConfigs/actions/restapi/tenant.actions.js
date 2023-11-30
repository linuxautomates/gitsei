import * as actions from "../actionTypes";

const uri = "tenant_state";
//tenant state
export const tenantStateGet = (id = "0") => ({
  type: actions.TENANT_STATE,
  method: "get",
  uri: uri,
  id: 0
});
