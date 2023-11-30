import { workflowProfileByOuActions } from "../actionTypes";

export const workflowProfileByOuLoadSuccessfulAction = (id: string, data: any) => ({
  type: workflowProfileByOuActions.WORKFLOW_PROFILE_READ_BY_OU_SUCCESSFUL,
  id,
  data
});

export const workflowProfileByOuLoadFailedAction = (id: string, error: any) => ({
  type: workflowProfileByOuActions.WORKFLOW_PROFILE_READ_BY_OU_FAILED,
  id,
  error
});

export const getWorkflowProfileByOuAction = (id: string) => ({
  type: workflowProfileByOuActions.WORKFLOW_PROFILE_READ_BY_OU,
  id
});

export const workProfileByOuAlreadyPresentAction = (id: string) => ({
  type: workflowProfileByOuActions.REUSE_EXISTING_WORKFLOW_PROFILE_BY_OU,
  id
});

export const workflowProfileClearAction = () => ({
  type: workflowProfileByOuActions.WORKFLOW_PROFILE_READ_BY_OU_CLEAR
});
