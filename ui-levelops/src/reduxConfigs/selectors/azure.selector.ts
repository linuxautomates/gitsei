import { get } from "lodash";
import { createSelector } from "reselect";
import { restapiState } from "./restapiSelector";
import { createParameterSelector } from "./selector";

const getId = createParameterSelector((params: any) => (params as any).id);

const _azureFilterValueMethodSelector = createSelector(restapiState, (data: any) => {
  return get(data, ["issue_management_attributes_values", "list"], { loading: true, error: false });
});

export const azureFilterValueIdSelector = createSelector(
  _azureFilterValueMethodSelector,
  getId,
  (data: any, id: string) => {
    return get(data, [id], { loading: true, error: false });
  }
);

const _azureFieldsListSelector = createSelector(restapiState, apis => {
  return get(apis, ["issue_management_workItem_Fields_list", "list"], {});
});

export const azureFieldsListSelector = createSelector(_azureFieldsListSelector, getId, (liststate: any, id: string) => {
  return get(liststate, [id, "data"], { loading: true, error: false });
});
