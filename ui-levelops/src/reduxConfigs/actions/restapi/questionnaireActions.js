// questionnaires

import * as actions from "../actionTypes";
import { ASSESSMENT_TEMPLATE_POST } from "../actionTypes";
import { ASSESSMENT_TEMPLATE_GET } from "../actionTypes";

const uri = "questionnaires";
const notifyUri = "questionnaires_notify";

export const qsGet = (id, complete = null) => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: uri,
  function: "getQuestionnaire",
  method: "get",
  complete
});

export const qsList = filters => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: uri,
  function: "getQuestionnaires",
  method: "list"
});

export const qsSearch = filters => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: uri,
  function: "searchQuestionnaires",
  method: "search"
});

export const qsCreate = (policy, complete = null, id = "0") => ({
  type: actions.RESTAPI_WRITE,
  data: policy,
  uri: uri,
  function: "createQuestionnaire",
  method: "create",
  complete,
  id: id
});

export const qsUpdate = (id, questionnaire, complete = null) => ({
  type: actions.RESTAPI_WRITE,
  data: questionnaire,
  id: id,
  uri: uri,
  function: "updateQuestionnaire",
  method: "update",
  complete
});

export const qsDelete = id => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  uri: uri,
  function: "deleteQuestionnaire",
  method: "delete"
});

export const qsExport = id => ({
  type: actions.Q_EXPORT,
  id: id
});

export const qsImport = (data, id = "0") => ({
  type: actions.Q_IMPORT,
  data: data,
  id: id
});

export const qsNotify = data => ({
  type: actions.RESTAPI_CALL,
  data: data,
  id: "0",
  uri: notifyUri,
  method: "list"
});

export const templateCreateOrUpdate = (id = "0", data) => ({
  type: ASSESSMENT_TEMPLATE_POST,
  data: data,
  id
});

export const templateGet = id => ({
  type: ASSESSMENT_TEMPLATE_GET,
  data: id
});

export const qsBulkDelete = ids => ({
  type: actions.RESTAPI_WRITE,
  payload: ids,
  uri,
  method: "bulkDelete",
  id: "0"
});
