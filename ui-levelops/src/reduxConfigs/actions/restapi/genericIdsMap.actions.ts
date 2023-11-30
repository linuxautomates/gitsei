import * as actions from "../actionTypes";

export const getIdsMap = (formName: string, filters: { [x: string]: Array<any> }) => ({
  type: actions.GENERIC_IDS_MAP,
  formName,
  filters
});

export const getStoredIdsMap = (formName: string, filters: { [x: string]: Array<any> }) => ({
  type: actions.GENERIC_STORED_IDS_MAP,
  formName,
  filters
});
