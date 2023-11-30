import { GENERIC_REST_API_SET, GENERIC_REST_API_LOADING, GENERIC_REST_API_ERROR } from "../actionTypes";

export const genericRestAPISet = (data: any, uri: string, method: string, id: string = "0") => ({
  type: GENERIC_REST_API_SET,
  data,
  uri,
  method,
  id
});

export const genericRestAPILoading = (data: any, uri: string, method: string, id: string = "0") => ({
  type: GENERIC_REST_API_LOADING,
  loading: data,
  uri,
  method,
  id
});

export const genericRestAPIError = (data: any, uri: string, method: string, id: string = "0") => ({
  type: GENERIC_REST_API_ERROR,
  error: data,
  uri,
  method,
  id
});
