import * as actions from "../actionTypes";
import { RestProduct } from "../../../classes/RestProduct";

const uri = "triage_rules";

export const triageRulesCreate = item => ({
  type: actions.RESTAPI_WRITE,
  data: item,
  uri: uri,
  method: "create"
});

export const triageRulesDelete = id => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  uri: uri,
  method: "delete"
});

export const triageRulesUdpate = (id, item) => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  data: item,
  uri: uri,
  function: "updateProduct",
  method: "update"
});

export const triageRulesGet = id => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: uri,
  function: "getProduct",
  method: "get",
  validator: RestProduct
});

export const triageRulesList = (filters, id = "0", complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: uri,
  id: id,
  method: "list"
});

export const triageRulesBulkDelete = ids => ({
  type: actions.RESTAPI_WRITE,
  payload: ids,
  uri,
  id: "0",
  method: "bulkDelete"
});
