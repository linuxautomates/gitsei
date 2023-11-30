import * as actions from "../actionTypes";

const uri = "triage_grid_view_filters";

const triage_filter_uri = "triage_filters";

export const getTriageGridViewFilters = (id = "0") => ({
  type: actions.RESTAPI_READ,
  uri: uri,
  id: id,
  method: "get"
});

export const updateTriageGridViewFilters = (filters = {}, id = "0") => ({
  type: actions.RESTAPI_WRITE,
  data: filters,
  uri,
  id,
  method: "update"
});

export const getTriageFilter = (name: string) => ({
  type: actions.RESTAPI_READ,
  uri: triage_filter_uri,
  id: name,
  method: "get"
});

export const listTriageFilter = (filters = {}, id = "0") => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: triage_filter_uri,
  id,
  method: "list"
});

export const createTriageFilter = (data: { name: string; [key: string]: any }, id = "0") => ({
  type: actions.RESTAPI_WRITE,
  data,
  uri: triage_filter_uri,
  id,
  method: "create"
});

export const updateTriageFilter = (name: string, data: { [key: string]: any }) => ({
  type: actions.RESTAPI_WRITE,
  data,
  uri: triage_filter_uri,
  id: name,
  method: "update"
});

export const deleteTriageFilter = (name: string) => ({
  type: actions.RESTAPI_WRITE,
  uri: triage_filter_uri,
  id: name,
  method: "delete"
});

export const bulkDeleteTriageFilters = (names: string[], id = "0") => ({
  type: actions.RESTAPI_WRITE,
  payload: names,
  uri: triage_filter_uri,
  id,
  method: "bulkDelete"
});
