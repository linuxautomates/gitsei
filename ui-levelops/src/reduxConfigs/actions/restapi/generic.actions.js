import * as actions from "../actionTypes";

export const genericList = (
  uri,
  method,
  filters,
  complete = null,
  id = "0",
  setLoading = true,
  queryparams = {},
  isWidget = false,
  showNotfication = true
) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  id: id,
  uri: uri,
  method: method,
  complete: complete,
  set_loading: setLoading,
  queryparams: queryparams,
  isWidget: isWidget,
  showNotfication
});

export const genericGet = (uri, id, complete = null, setLoading = true) => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: uri,
  method: "get",
  complete: complete,
  set_loading: setLoading
});
