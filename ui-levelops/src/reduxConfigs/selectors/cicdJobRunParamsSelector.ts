import { get } from "lodash";
import { CicdJobRunParamsState } from "reduxConfigs/reducers/cicdJobRunParamsReducer";
import { createSelector } from "reselect";
import { createParameterSelector } from "./selector";

const cicdJobRunParameterSelector = (state: any) => state.cicdJobRunParamsReducer;
const getId = createParameterSelector((params: any) => params.id);

export const cicdJobRunParamsStateSelector = createSelector(
  cicdJobRunParameterSelector,
  getId,
  (data: CicdJobRunParamsState, id: string) => get(data.cicdJobRunParams, [id], { isLoading: true, error: false })
);

export const cicdJobRunParamsSelector = createSelector(
  cicdJobRunParameterSelector,
  getId,
  (data: CicdJobRunParamsState, id: string) => get(data.cicdJobRunParams, [id, "data"], undefined)
);

export const cicdJobRunParamsLoading = createSelector(
  cicdJobRunParameterSelector,
  getId,
  (data: CicdJobRunParamsState, id: string) => get(data.cicdJobRunParams, [id, "isLoading"], true)
);
