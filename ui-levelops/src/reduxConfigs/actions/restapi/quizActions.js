import * as actions from "../actionTypes";
import { RestQuiz } from "../../../classes/RestQuiz";

const uri = "quiz";

export const quizGet = id => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: uri,
  function: "getQuiz",
  method: "get",
  validator: RestQuiz
});

export const quizDelete = id => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  uri: uri,
  function: "deleteQuiz",
  method: "delete"
});

export const quizUpdate = (id, quiz) => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  data: quiz,
  uri: uri,
  function: "updateQuiz",
  method: "update"
});

export const quizCreate = quiz => ({
  type: actions.RESTAPI_WRITE,
  data: quiz,
  uri: uri,
  function: "createQuiz",
  method: "create"
});

export const quizList = (filters, id) => ({
  id: id,
  type: actions.RESTAPI_READ,
  data: filters,
  uri: "quiz",
  function: "getQuizzes",
  method: "list"
});

export const quizFileUpload = (id, file) => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  file: file,
  uri: uri,
  function: "fileUploadQuiz",
  method: "upload"
});

export const commsCreate = comms => ({
  type: actions.RESTAPI_WRITE,
  data: comms,
  uri: "comms",
  function: "createComms",
  method: "create"
});
