import {
  WORKBENCH_TAB_CLEAR,
  WORKBENCH_TAB_COUNTS,
  WORKBENCH_TAB_DATA,
  WORKBENCH_TAB_ERROR,
  WORKBENCH_TAB_LOADING
} from "./actionTypes";

export const workbenchTabCounts = filtersArray => ({ type: WORKBENCH_TAB_COUNTS, filters: filtersArray });

export const workbenchTabClear = () => ({ type: WORKBENCH_TAB_CLEAR });

export const workbenchTabLoading = loading => ({ type: WORKBENCH_TAB_LOADING, loading: loading });

export const workbenchTabError = error => ({ type: WORKBENCH_TAB_ERROR, error: error });

export const workbenchTabData = data => ({ type: WORKBENCH_TAB_DATA, data: data });
