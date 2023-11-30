import * as actions from "../actionTypes";

const uri = "propels_nodes_evaluate";

export const evaluateNode = (filters: any, id: string = "0", complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri,
  method: "list",
  id,
  complete
});
