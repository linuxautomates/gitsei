import { createSelector } from "reselect";
import { useSelector } from "react-redux";
import { get } from "lodash";

import { restapiState } from "./restapiSelector";

export function useParamSelector(selector: any, ...params: any) {
  return useSelector((state: any) => selector(state, ...params));
}

function createParameterSelector(selector: any) {
  return (_: any, params: any) => selector(params);
}

const getURI = createParameterSelector((params: any) => params.uri);
const getMETHOD = createParameterSelector((params: any) => params.method);
const getUUID = createParameterSelector((params: any) => params.uuid);

// Usage:   const genericAPIState = useParamSelector(getGenericURISelector, { uri });
export const getGenericURISelector = createSelector(restapiState, getURI, (data: any, uri: string) => {
  return get(data, [uri], {});
});

// Usage:   const genericAPIState = useParamSelector(getGenericMethodSelector, { uri, method });
export const getGenericMethodSelector = createSelector(
  getGenericURISelector,
  getMETHOD,
  (data: any, method: string) => {
    return get(data, [method || "list"], {});
  }
);

// Usage:   const genericAPIState = useParamSelector(getGenericUUIDSelector, { uri, method, uuid });
export const getGenericUUIDSelector = createSelector(getGenericMethodSelector, getUUID, (data: any, uuid: string) => {
  return get(data, [uuid || "0"], {});
});

// Usage:   const genericAPIState = useParamSelector(getGenericRestAPISelector, { uri, method, uuid });
export const getGenericRestAPISelector = createSelector(getGenericUUIDSelector, (data: any) => {
  return data || { loading: true };
});

export const getGenericRestAPIStatusSelector = createSelector(getGenericRestAPISelector, (data: any) => {
  return {
    loading: data.loading,
    error: data.error,
    status: data.status,
    error_code: data.error_code
  };
});
