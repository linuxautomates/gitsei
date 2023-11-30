import { get } from "lodash";
import { createSelector } from "reselect";
import { restapiState } from "./restapiSelector";

export const filesState = createSelector(restapiState, state => {
  return get(state, ["files"], {});
});

export const filesGetState = createSelector(filesState, state => {
  return get(state, ["get"], {});
});

export const filesHeadState = createSelector(filesState, state => {
  return get(state, ["head"], {});
});
