import * as actions from "../actionTypes";
import { RestProduct } from "../../../classes/RestProduct";

const uri = "products";

export const productsCreate = item => ({
  type: actions.RESTAPI_WRITE,
  data: item,
  uri: uri,
  function: "createProduct",
  method: "create"
});

export const productsDelete = id => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  uri: uri,
  function: "deleteProduct",
  method: "delete"
});

export const productsUpdate = (id, item) => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  data: item,
  uri: uri,
  function: "updateProduct",
  method: "update"
});

export const productsGet = id => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: uri,
  function: "getProduct",
  method: "get",
  validator: RestProduct
});

export const productsList = (filters, id = "0", complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: uri,
  id: id,
  function: "getProducts",
  method: "list"
});

export const productsBulk = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: uri,
  function: "getProducts",
  method: "bulk"
});

export const productsBulkDelete = ids => ({
  type: actions.RESTAPI_WRITE,
  payload: ids,
  uri,
  method: "bulkDelete",
  id: "0"
});
