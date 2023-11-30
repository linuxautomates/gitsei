// questions

import * as actions from "../actionTypes";
import { RestSection } from "../../../classes/RestQuestionnaire";

const uri = "sections";

export const sectionsGet = (id, complete = null) => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: uri,
  function: "getQuestion",
  method: "get",
  validator: RestSection,
  complete
});

export const sectionsList = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: uri,
  function: "getQuestions",
  method: "list",
  complete
});

export const sectionsSearch = filters => ({
  type: actions.RESTAPI_READ,
  data: filters
});

export const sectionsCreate = (question, id = "0", complete = null) => ({
  type: actions.RESTAPI_WRITE,
  data: question,
  uri: uri,
  id: id,
  function: "createQuestion",
  method: "create",
  complete
});

export const sectionsUpdate = (id, question, complete = null) => ({
  type: actions.RESTAPI_WRITE,
  data: question,
  id: id,
  uri: uri,
  function: "updateQuestion",
  method: "update",
  complete
});

export const sectionsDelete = (id, complete = null) => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  uri: uri,
  function: "deleteQuestion",
  method: "delete",
  complete
});
