import { createSelector } from "reselect";

const unSavedModal = (state: any) => state.unSavedChangesReducer;

export const unSavedChangesSelector = createSelector(unSavedModal, data => data);
