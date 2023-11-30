import { get } from "lodash";
import { WorkflowProfileState } from "reduxConfigs/reducers/workflowProfileReducer";
import { createSelector } from "reselect";
import { createParameterSelector } from "./selector";

const workflowProfileSelector = (state: any) => state.workflowProfileReducer;
const getWorkspaceId = createParameterSelector((params: any) => params.workspaceId);

export const workflowProfileListSelector = createSelector(
  workflowProfileSelector,
  (data: WorkflowProfileState) => data.profiles
);

export const workflowProfileDetailsSelector = createSelector(
  workflowProfileSelector,
  (data: WorkflowProfileState) => data.selectedProfile
);

export const workflowProfileSavingStatusSelector = createSelector(
  workflowProfileSelector,
  (data: WorkflowProfileState) => data.savingStatus
);

export const getOUsForWorkspace = createSelector(
  workflowProfileSelector,
  getWorkspaceId,
  (data: WorkflowProfileState, workspaceId: string) => get(data.workspaceOUList, [workspaceId], undefined)
);
