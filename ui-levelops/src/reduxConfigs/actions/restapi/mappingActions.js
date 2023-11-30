import * as actions from "../actionTypes";
import { RestMapping } from "../../../classes/RestProduct";

export const mappingsCreate = item => ({
  type: actions.RESTAPI_WRITE,
  data: item,
  uri: "mappings",
  function: "createMapping",
  method: "create"
});

export const mappingsDelete = id => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  uri: "mappings",
  function: "deleteMapping",
  method: "delete"
});

export const mappingsUdpate = (id, item) => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  data: item,
  uri: "mappings",
  function: "updateMapping",
  method: "update"
});

export const mappingsGet = id => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: "mappings",
  function: "getMapping",
  method: "get",
  validator: RestMapping
});

export const mappingsList = filters => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: "mappings",
  function: "getMappings",
  method: "list"
});
