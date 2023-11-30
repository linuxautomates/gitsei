import * as actions from "../actionTypes";

export const filesHead = id => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: "files",
  method: "head"
});

export const filesGet = (id, fileName = "download", download = true, filters = {}) => ({
  type: actions.RESTAPI_READ,
  id: id,
  file_name: fileName,
  uri: "files",
  function: "getFile",
  download: download,
  filters,
  method: "get"
});

export const filesDelete = id => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  uri: "files",
  method: "delete"
});
