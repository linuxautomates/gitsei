import {
  CACHED_GENERIC_REST_API_CLEAR,
  CACHED_GENERIC_REST_API_ERROR,
  CACHED_GENERIC_REST_API_INVALIDATE,
  CACHED_GENERIC_REST_API_LOADING,
  CACHED_GENERIC_REST_API_SET,
  CACHED_GENERIC_REST_API_READ,
  CACHED_GENERIC_REST_API_APPEND
} from "./actionTypes";

type CachedStatePayloadType = {
  uri: string;
  method: string;
  id: string;
  data?: Record<any, any>;
  loading?: boolean;
  error?: boolean;
  expires_in?: number;
  storageType?: "append" | "set";
  forceLoad?: boolean;
  uniqueByKey?: string;
};

export interface CachedStateActionType {
  type:
    | typeof CACHED_GENERIC_REST_API_ERROR
    | typeof CACHED_GENERIC_REST_API_LOADING
    | typeof CACHED_GENERIC_REST_API_SET
    | typeof CACHED_GENERIC_REST_API_CLEAR
    | typeof CACHED_GENERIC_REST_API_INVALIDATE
    | typeof CACHED_GENERIC_REST_API_READ
    | typeof CACHED_GENERIC_REST_API_APPEND;
  payload: CachedStatePayloadType;
}

export const cachedGenericRestAPISet = (
  data: any,
  uri: string,
  method: string,
  id: string = "0",
  expires_in = 3600
) => ({
  type: CACHED_GENERIC_REST_API_SET,
  payload: {
    data,
    uri,
    method,
    id,
    expires_in
  }
});

export const cachedGenericRestAPIAppend = (
  data: any,
  uri: string,
  method: string,
  id: string = "0",
  expires_in = 3600,
  uniqueByKey?: string
) => ({
  type: CACHED_GENERIC_REST_API_APPEND,
  payload: {
    data,
    uri,
    method,
    id,
    expires_in,
    uniqueByKey
  }
});

export const cachedGenericRestAPILoading = (data: any, uri: string, method: string, id: string = "0") => ({
  type: CACHED_GENERIC_REST_API_LOADING,
  payload: {
    loading: data,
    uri,
    method,
    id
  }
});

export const cachedGenericRestAPIError = (data: any, uri: string, method: string, id: string = "0") => ({
  type: CACHED_GENERIC_REST_API_ERROR,
  payload: {
    error: data,
    uri,
    method,
    id
  }
});

export const cachedGenericRestAPIClear = (uri: string, method: string, id: string = "0") => ({
  type: CACHED_GENERIC_REST_API_CLEAR,
  payload: {
    uri,
    method,
    id
  }
});

export const cachedGenericRestAPIInvalidate = (uri: string, method: string, id: string = "0") => ({
  type: CACHED_GENERIC_REST_API_INVALIDATE,
  payload: {
    uri,
    method,
    id
  }
});

export const cachedGenericRestApiRead = (
  uri: string,
  method: string,
  id: string = "0",
  data: any,
  storageType = "set",
  forceLoad = false,
  uniqueByKey?: string
) => ({
  type: CACHED_GENERIC_REST_API_READ,
  payload: {
    uri,
    method,
    id,
    data,
    storageType,
    forceLoad,
    uniqueByKey
  }
});
