import * as actions from "../actionTypes";

// restapi actions
// mostly use id=0 for the create methods

export const restapiLoading = (
  action: boolean,
  uri: string,
  method: string,
  id: string | number = 0
) => ({
  type: actions.RESTAPI_LOADING,
  loading: action,
  uri: uri,
  method: method,
  id: id
});

export const restapiError = (error: any, uri: string, method: string, id = "0") => ({
  type: actions.RESTAPI_ERROR,
  error: error,
  uri: uri,
  method: method,
  id: id
});

export const restapiErrorCode = (code: string, uri: string, method: string, id = 0) => ({
  type: actions.RESTAPI_ERROR_CODE,
  error_code: code,
  uri: uri,
  method: method,
  id: id
});

export const setSelectedEntity = (uri: string, data: any) => ({
  type: actions.SET_SELECTED_ENTITY,
  data: data,
  uri: uri
});

export const setSelectedChildId = (data: any, uri?: string) => ({
  type: actions.SET_SELECTED_CHILD_ID,
  data,
  uri: uri || "selected_child_id"
});

export const setEntity = (data: any, uri: string, id: string | number = 0) => ({
  type: actions.SET_ENTITY,
  data: data,
  uri: uri,
  id: id
});

export const setEntities = (data: any, uri: string) => ({
  type: actions.SET_ENTITIES,
  data: data,
  uri: uri
});

export const deleteEntities = (entityIds: string[], uri: string) => ({
  type: actions.DELETE_ENTITIES,
  data: entityIds,
  uri: uri
});

export const restapiData = (data: any, uri: string, method: string, id: string | number = 0) => ({
  type: actions.RESTAPI_DATA,
  data: data,
  uri: uri,
  method: method,
  id: id
});

export const restResponseData = (type: string, data: any, uri: string, method: string, id: string | number = 0) => ({
  type,
  data,
  uri,
  method,
  id
});

export const restapiClear = (uri: string, method: string, id: string | number = 0) => ({
  type: actions.RESTAPI_CLEAR,
  uri: uri,
  method: method,
  id: id
});

export const restapiClearAll = () => ({
  type: actions.RESTAPI_CLEAR_ALL
});

export const setDemoWidgetFilters = (uri: string, widgetId: string, data: any) => ({
  type: actions.DEMO_WIGET_UPDATE,
  data: data,
  uri: uri,
  id: widgetId
});
