import { get } from "lodash";
import { createSelector } from "reselect";
import { restapiState } from "./restapiSelector";

export const workItemState = createSelector(restapiState, state => {
  return get(state, ["workitem"], {});
});

export const workItemGetState = createSelector(workItemState, state => {
  return get(state, ["get"], {});
});
