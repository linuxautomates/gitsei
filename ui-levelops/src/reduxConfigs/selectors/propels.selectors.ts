import { get } from "lodash";
import { createSelector } from "reselect";

import { restapiState } from "./restapiSelector";

export const getPropelNodesEvaluateSelector = createSelector(restapiState, (data: any) => {
  return get(data, ["propels_nodes_evaluate", "list"], {});
});

export const getPropelCreateSelector = (state: any) => {
  return get(state.restapiReducer, ["propels", "create"], {});
};

export const getPropelDeleteSelector = (state: any) => {
  return get(state.restapiReducer, ["propels", "delete"], {});
};

export const getPropelGetSelector = (state: any) => {
  return get(state.restapiReducer, ["propels", "get"], {});
};

export const getPropelUpdateSelector = (state: any) => {
  return get(state.restapiReducer, ["propels", "update"], {});
};
