import { get } from "lodash";
import { createSelector } from "reselect";
import { cachedRestApiState } from "./cachedRestapiSelector";
import { createParameterSelector } from "./selector";

export const tagsSelectorState = (state: any) => {
  return {
    list: get(state.restapiReducer, ["tags", "list"], {}),
    get: get(state.restapiReducer, ["tags", "get"], {}),
    getOrCreate: get(state.restapiReducer, ["tags", "getOrCreate"], {}),
    bulk: get(state.restapiReducer, ["tags", "bulk"], {})
  };
};

export const cachedTagsListSelector = createSelector(cachedRestApiState, data => get(data, ["tags", "list"], {}));
export const tagsSelector = createSelector(tagsSelectorState, data => data);

export const tagsGetOrCreateSelector = createSelector(tagsSelector, tagsState => {
  return get(tagsState, ["getOrCreate"], {});
});

export const tagsBulkSelector = createSelector(tagsSelector, tagsState => {
  return get(tagsState, ["bulk"], {});
});

const getId = createParameterSelector((params: any) => params.id);

export const getTagsListSelector = createSelector(tagsSelector, getId, (data: any, id: string) => {
  return get(data, ["list", id], { loading: true, error: false });
});
