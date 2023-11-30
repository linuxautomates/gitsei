import * as actions from "../actionTypes";

const uri = "propel_node_categories";

export const propelNodeCategoriesGet = (id = "list", complete = null) => ({
  type: actions.RESTAPI_READ,
  id: id,
  complete: complete,
  uri: uri,
  method: "get"
});
