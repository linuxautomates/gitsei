import { cicdJobParamsActions, workflowProfileActions } from "../actionTypes";

export const createWorkflowProfileAction = (item: any) => ({
  type: workflowProfileActions.WORKFLOW_PROFILE_CREATE,
  data: item
});

export const saveWorkflowProfileFailedAction = (error: any) => ({
  type: workflowProfileActions.WORKFLOW_PROFILE_SAVE_FAILED,
  error
});

export const saveWorkflowProfileSuccessfulAction = (id: string) => ({
  type: workflowProfileActions.WORKFLOW_PROFILE_SAVE_SUCCESSFUL,
  id
});

export const workflowProfileLoadSuccessfulAction = (data: any) => ({
  type: workflowProfileActions.WORKFLOW_PROFILE_LIST_LOAD_SUCCESSFUL,
  data
});

export const workflowProfileLoadFailedAction = (error: any) => ({
  type: workflowProfileActions.WORKFLOW_PROFILES_LIST_LOAD_FAILED,
  error
});

export const getWorkflowProfileAction = (id: string, basicStages: any) => ({
  type: workflowProfileActions.WORKFLOW_PROFILE_READ,
  id,
  basicStages
});

export const clearSavedWorkflowProfile = () => ({ type: workflowProfileActions.WORKFLOW_PROFILE_CLEAR });

export const updateWorkflowProfileAction = (id: string, data: any) => ({
  type: workflowProfileActions.WORKFLOW_PROFILE_UPDATE,
  id,
  data
});

export const getWorkflowProfileFilters = (uri: string, reportType: string, uuid: string) => ({
  type: workflowProfileActions.WORKFLOW_PROFILE_FILTERS,
  reportType,
  uri,
  method: "list",
  uuid
});

export const workflowProfileOuAssociationAction = (profileId: string, orgId: string, orgName: string) => ({
  type: workflowProfileActions.ASSOCIATE_OU_TO_PROFILE,
  profileId,
  orgId,
  orgName
});

export const getCICDJobParamsAction = (id: string, cicd_job_ids: string[]) => ({
  type: cicdJobParamsActions.GET_CICD_JOB_PARAMS,
  id,
  payload: { filter: { cicd_job_ids } }
});

export const cicdJobParamsSuccessAction = (id: string, data: Record<string, Array<string>>) => ({
  type: cicdJobParamsActions.GET_CICD_JOB_PARAMS_SUCCESS,
  id,
  data
});

export const cicdJobParamsFailedAction = (id: string, error: any) => ({
  type: cicdJobParamsActions.GET_CICD_JOB_PARAMS_FAIL,
  id,
  error
});

export const clearCICDJobParamsAction = (id: string) => ({
  type: cicdJobParamsActions.CLEAR_CICD_JOB_PARAMS,
  id
});
