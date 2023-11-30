import { RestWorkflowProfile } from "classes/RestWorkflowProfile";
import { workflowProfileByOuActions } from "reduxConfigs/actions/actionTypes";
import { createReducer } from "../createReducer";

export interface WorkflowProfileByOuState {
  workspaceOUProfile: Record<string, { isLoading: boolean; data: RestWorkflowProfile; error?: any }>;
}

const INITIAL_STATE: WorkflowProfileByOuState = {
  workspaceOUProfile: {}
};

const savingWorkflowByOuProfile = (state: WorkflowProfileByOuState, action: any): WorkflowProfileByOuState => ({
  ...state,
  workspaceOUProfile: {
    ...state.workspaceOUProfile,
    [action.id]: {
      ...state.workspaceOUProfile[action.id],
      isLoading: state?.workspaceOUProfile?.[action.id]?.data ? false : true
    }
  }
});

const saveWorkflowProfileByOuSuccessful = (state: WorkflowProfileByOuState, action: any): WorkflowProfileByOuState => {
  return {
    ...state,
    workspaceOUProfile: {
      ...state.workspaceOUProfile,
      [action.id]: {
        isLoading: false,
        data: action.data
      }
    }
  };
};

const saveWorkflowProfileByOuFailed = (state: WorkflowProfileByOuState, action: any): WorkflowProfileByOuState => ({
  ...state,
  workspaceOUProfile: {
    ...state.workspaceOUProfile,
    [action.id]: {
      isLoading: false,
      data: undefined,
      error: action.error
    }
  }
});

const reuseExistingWorkflowProfile = (state: WorkflowProfileByOuState, action: any): WorkflowProfileByOuState => ({
  ...state,
  workspaceOUProfile: {
    ...state.workspaceOUProfile,
    [action.id]: {
      data: state.workspaceOUProfile?.[action.id]?.data,
      isLoading: false
    }
  }
});

const clearSavedWorkflowProfileByOu = (state: WorkflowProfileByOuState, action: any): WorkflowProfileByOuState => ({
  ...state,
  workspaceOUProfile: {
    ...INITIAL_STATE.workspaceOUProfile
  }
});

const workflowProfileByOuReducer = createReducer(INITIAL_STATE, {
  [workflowProfileByOuActions.WORKFLOW_PROFILE_READ_BY_OU]: savingWorkflowByOuProfile,
  [workflowProfileByOuActions.WORKFLOW_PROFILE_READ_BY_OU_SUCCESSFUL]: saveWorkflowProfileByOuSuccessful,
  [workflowProfileByOuActions.WORKFLOW_PROFILE_READ_BY_OU_FAILED]: saveWorkflowProfileByOuFailed,
  [workflowProfileByOuActions.REUSE_EXISTING_WORKFLOW_PROFILE_BY_OU]: reuseExistingWorkflowProfile,
  [workflowProfileByOuActions.WORKFLOW_PROFILE_READ_BY_OU_CLEAR]: clearSavedWorkflowProfileByOu
});

export default workflowProfileByOuReducer;
