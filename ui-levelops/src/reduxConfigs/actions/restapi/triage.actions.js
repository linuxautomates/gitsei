import * as actions from "../actionTypes";

const uri = "triage";

export const triageDetailGet = id => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: uri,
  method: "get"
});

export const triageStagesGet = (id, filter) => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: uri,
  method: "idList",
  data: filter
});

export const triageRuleResultsGet = (id, filter) => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: uri,
  method: "list",
  data: filter
});

export const triageMatchingJobs = (workItem, id = "0") => ({
  type: actions.TRIAGE_MATCHING_JOGS,
  work_item: workItem,
  id
});

export const fetchTriageResultJobs = (filters, complete = null, id = "0") => ({
  type: actions.FETCH_JOB_RESULTS,
  filters,
  complete,
  id: id
});
