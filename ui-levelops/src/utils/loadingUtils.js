import { get } from "lodash";

const REST_METHODS = ["get", "delete", "create", "update"];

export function isLoading(restapiProps, type) {
  let loading = false;
  let error = false;
  REST_METHODS.forEach(method => {
    for (let id in restapiProps.rest_api[type][method]) {
      if (restapiProps.rest_api[type][method][id].hasOwnProperty("loading")) {
        loading = loading || restapiProps.rest_api[type][method][id].loading;
        error = error || restapiProps.rest_api[type][method][id].error;
        if (loading === false && error === false && method !== "get") {
          // because you want to do stuff with get values after loading is complete
          restapiProps.restapiClear(type, method, id);
        }
      }
    }
  });
  return { loading: loading, error: error };
}

export function getLoading(rest_api, apiName, apiMethod, id = "0") {
  if (!rest_api) {
    return true;
  }
  if (rest_api.hasOwnProperty(apiName) && rest_api[apiName].hasOwnProperty(apiMethod)) {
    if (rest_api[apiName][apiMethod].hasOwnProperty(id) && rest_api[apiName][apiMethod][id].hasOwnProperty("loading")) {
      return rest_api[apiName][apiMethod][id].loading;
    }
  }
  return true;
}

export function getError(rest_api, apiName, apiMethod, id = "0") {
  if (!rest_api) {
    return false;
  }

  return get(rest_api, [apiName, apiMethod, id, "error"], false);
  
}

export function getErrorCode(rest_api, apiName, apiMethod, id = "0") {
  return get(rest_api, [apiName, apiMethod, id, "error_code"], 0);
}

// At many places we are calling getError and getLoading together, so added this function.
export function loadingStatus(rest_api, apiName, apiMethod, id = "0") {
  return {
    loading: getLoading(rest_api, apiName, apiMethod, id),
    error: getError(rest_api, apiName, apiMethod, id)
  };
}

export function newGetLoading(props, apiMethod, id = "0") {
  if (props[apiMethod].hasOwnProperty(id) && props[apiMethod][id].hasOwnProperty("loading")) {
    return props[apiMethod][id].loading;
  }
  return true;
}

export function newGetError(props, apiMethod, id = "0") {
  if (props[apiMethod].hasOwnProperty(id) && props[apiMethod][id].hasOwnProperty("loading")) {
    return props[apiMethod][id].error;
  }
  return true;
}

export function getData(props, apiName, apiMethod, id = "0") {
  return get(props, [apiName, apiMethod, id, "data"], {});
}

export function getStateLoadingStatus(state, apiMethod, id = "0") {
  return {
    loading: get(state, [apiMethod, id, "loading"], true),
    error: get(state, [apiMethod, id, "error"], true)
  };
}
