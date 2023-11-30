import * as actions from "../actionTypes";

const uri = "product_aggs";

export const productAggsList = (filters, id = "0", complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: uri,
  id: id,
  method: "list",
  complete: complete
});

export const productAggsGet = (id, complete = null) => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: uri,
  method: "get",
  complete: complete
});
