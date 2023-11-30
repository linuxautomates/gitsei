import { get } from "lodash";
import { createSelector } from "reselect";
import { restapiState } from "./restapiSelector";
import { createParameterSelector } from "./selector";

const getId = createParameterSelector((params: any) => (params as any).id);

const _propelRunRerunSelector = createSelector(restapiState, (data: any) => {
  return get(data, ["propels_run_rerun"], { loading: true, error: false });
});

const _propelRunRerunGetSelector = createSelector(_propelRunRerunSelector, (data: any) => {
  return get(data, ["get"], { loading: true, error: false });
});

export const getPropelRunReRunSelector = createSelector(_propelRunRerunGetSelector, getId, (data: any, id: string) => {
  return get(data, [id], { loading: true, error: false });
});
