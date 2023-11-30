import { uiActions } from "./actionTypes";

export const toggleFullscreenModalAction = (page, isOpen) => ({
  type: uiActions.TOGGLE_FULLSCREEN_MODAL,
  payload: { page, isOpen }
});
