import { get } from "lodash";
import { createSelector } from "reselect";
import { restapiState } from "./restapiSelector";

export const getObjectsSelector = createSelector(restapiState, data => {
  return get(data, ["objects"], {});
});

export const getObjectsListSelector = createSelector(getObjectsSelector, data => {
  return get(data, ["list"], {});
});
