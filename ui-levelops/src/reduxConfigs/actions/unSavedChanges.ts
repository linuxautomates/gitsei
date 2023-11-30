import { UNSAVED_MODAL_CHANGES } from "./actionTypes";

export const setUnSavedChanges = (payload: any) => ({
  type: UNSAVED_MODAL_CHANGES,
  payload
});
