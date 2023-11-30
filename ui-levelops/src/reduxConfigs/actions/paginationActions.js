import {
  PAGINATION_CLEAR,
  PAGINATION_DATA,
  PAGINATION_DONE_LOADING,
  PAGINATION_ERROR,
  PAGINATION_FILTERS,
  PAGINATION_GET,
  PAGINATION_LOADING,
  PAGINATION_SET
} from "./actionTypes";

export const paginationSet = (uri, method, promiseToken) => ({
  type: PAGINATION_SET,
  method: method,
  uri: uri,
  promise_token: promiseToken
});

export const paginationGet = (
  uri,
  method,
  filters,
  id = "0",
  derive = true,
  deriveOnly = "all",
  complete = null,
  payload = {}
) => ({
  type: PAGINATION_GET,
  filters: filters,
  uri: uri,
  method: method,
  id: id,
  derive: derive,
  deriveOnly: deriveOnly,
  complete,
  payload
});

export const paginationLoading = () => ({ type: PAGINATION_LOADING });

export const paginationDoneLoading = () => ({ type: PAGINATION_DONE_LOADING });

export const paginationData = data => ({ type: PAGINATION_DATA, data: data });

export const paginationError = () => ({ type: PAGINATION_ERROR });

export const paginationFilter = filters => ({ type: PAGINATION_FILTERS, filter: filters });

export const paginationClear = () => ({ type: PAGINATION_CLEAR });
