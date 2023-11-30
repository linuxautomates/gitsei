import * as actions from "../actionTypes";
import { RestWorkItem } from "../../../classes/RestWorkItem";

const uri = "workitem";

export const workItemCreate = item => ({
  type: actions.RESTAPI_READ,
  data: item,
  uri: uri,
  function: "createWorkItem",
  method: "create"
});

export const workItemCreateBlank = data => ({
  type: actions.RESTAPI_WRITE,
  data,
  uri,
  function: "createBlankWorkItem",
  method: "create"
});

export const workItemDelete = id => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  uri: uri,
  function: "deleteWorkItem",
  method: "delete"
});

export const workItemUdpate = (id, item) => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  data: item,
  uri: uri,
  function: "updateWorkItem",
  method: "update"
});

export const workItemGet = (id, complete = null) => ({
  type: actions.RESTAPI_READ,
  id: id,
  complete: complete,
  uri: uri,
  function: "getWorkItem",
  method: "get",
  validator: RestWorkItem
});

export const workItemList = (filters, id = "0") => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: uri,
  id: id,
  function: "getWorkItems",
  method: "list"
});

export const workItemPatch = (id, item) => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  data: item,
  uri: uri,
  method: "patch"
});

export const workItemUpload = (id, workitemId, file) => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  file: file,
  data: { workitemId: workitemId },
  uri: uri,
  method: "upload"
});

export const workItemBulkDelete = ids => ({
  type: actions.RESTAPI_WRITE,
  id: "0",
  payload: ids,
  uri: uri,
  method: "bulkDelete"
});
