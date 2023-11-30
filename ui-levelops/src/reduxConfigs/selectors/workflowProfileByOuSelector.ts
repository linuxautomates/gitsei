import { get } from "lodash";
import { WorkflowProfileByOuState } from "reduxConfigs/reducers/workflowProfileByOuReducer";
import { createSelector } from "reselect";
import { createParameterSelector } from "./selector";

const workflowProfileSelector = (state: any) => state.workflowProfileByOuReducer;
const getWorkspaceOuId = createParameterSelector((params: any) => params.queryParamOU);

const workflowProfileOuListSelector = createSelector(workflowProfileSelector, (data: WorkflowProfileByOuState) => data);

export const workflowProfileDetailStateSelector = createSelector(
  workflowProfileSelector,
  getWorkspaceOuId,
  (data: WorkflowProfileByOuState, queryParamOU: string) =>
    get(data.workspaceOUProfile, [queryParamOU], { isLoading: true, error: false })
);

export const workflowProfileDetailSelector = createSelector(
  workflowProfileSelector,
  getWorkspaceOuId,
  (data: WorkflowProfileByOuState, queryParamOU: string) =>
    get(data.workspaceOUProfile, [queryParamOU, "data"], undefined)
);

export const workflowProfileDetailSelectorLoading = createSelector(
  workflowProfileSelector,
  getWorkspaceOuId,
  (data: WorkflowProfileByOuState, queryParamOU: string) =>
    get(data.workspaceOUProfile, [queryParamOU, "isLoading"], true)
);
