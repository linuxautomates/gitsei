import * as actions from "../actionTypes";

export const levelopsWidgets = (uri: string, method: string, filters: any, complete = null, id = "0") => ({
  type: actions.LEVELOPS_WIDGETS,
  data: filters,
  id,
  uri,
  method,
  complete
});
