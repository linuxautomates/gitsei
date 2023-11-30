import { get } from "lodash";
import { createSelector } from "reselect";

export const cachedRestApiState = (state: any) => state.cachedStateReducer;

const _genericCachedRestApiSelector = (state: any, props: any) => {
  if (props.uri) {
    const method = props.method || "list";
    const id = props.uuid || "0";
    return get(state.cachedStateReducer, [props.uri, method, id]);
  }
};

export const genericCachedRestApiSelector = createSelector(_genericCachedRestApiSelector, data => data ?? {});
