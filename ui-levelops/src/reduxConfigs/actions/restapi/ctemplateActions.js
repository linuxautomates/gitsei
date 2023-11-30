import * as actions from "../actionTypes";
import { RestCommTemplate } from "../../../classes/RestCommTemplate";

const uri = "ctemplates";

export const cTemplatesCreate = item => ({
  type: actions.RESTAPI_WRITE,
  data: item,
  uri: uri,
  function: "createCTemplate",
  method: "create"
});

export const cTemplatesDelete = id => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  uri: uri,
  function: "deleteCTemplate",
  method: "delete"
});

export const cTemplatesUdpate = (id, item) => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  data: item,
  uri: uri,
  function: "updateCTemplate",
  method: "update"
});

export const cTemplatesGet = id => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: uri,
  function: "getCTemplate",
  method: "get",
  validator: RestCommTemplate
});

export const cTemplatesList = filters => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: uri,
  function: "getCTemplates",
  method: "list"
});

export const cTemplatesBulkDelete = ids => ({
  type: actions.RESTAPI_WRITE,
  uri,
  method: "bulkDelete",
  id: "0",
  payload: ids
});
