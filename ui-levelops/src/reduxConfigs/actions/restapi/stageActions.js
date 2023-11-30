import * as actions from "../actionTypes";

const uri = "stages";

export const stagesList = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: uri,
  function: "getStages",
  method: "list"
});

export const stagesUpdate = (id, stage) => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  data: stage,
  uri: uri,
  function: "updateStage",
  method: "update"
});
