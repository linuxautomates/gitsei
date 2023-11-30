import { UNSAVED_MODAL_CHANGES } from "../actions/actionTypes";
import { createReducer } from "../createReducer";

const INITIAL_STATE = {
  show_modal: false,
  dirty: false,
  onCancel: ""
};

const setUnSavedModalChanges = (state: any, { payload }: any) => {
  state = Object.assign(state, { ...payload });
  return {
    ...state,
    show_modal: payload.show_modal,
    dirty: payload.dirty,
    onCancel: payload.onCancel
  };
};

const unSavedChangesReducer = createReducer(INITIAL_STATE, {
  [UNSAVED_MODAL_CHANGES]: setUnSavedModalChanges
});

export default unSavedChangesReducer;
