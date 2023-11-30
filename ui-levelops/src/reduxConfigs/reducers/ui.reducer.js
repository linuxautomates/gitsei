import { fromJS } from "immutable";
import { uiActions } from "../actions/actionTypes";

const initialState = fromJS({
  fullscreenModal: {}
});

export const uiReducer = (state = initialState, action) => {
  switch (action.type) {
    case uiActions.TOGGLE_FULLSCREEN_MODAL:
      return state.setIn(["fullscreenModal", action.payload.page], action.payload.isOpen);
    default:
      return state;
  }
};
