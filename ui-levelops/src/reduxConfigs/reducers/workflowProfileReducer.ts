import { RestWorkflowProfile } from "classes/RestWorkflowProfile";
import { workflowProfileActions } from "reduxConfigs/actions/actionTypes";
import { createReducer } from "../createReducer";

interface listDataResponse {
  records: Array<RestWorkflowProfile>;
  count: number;
  _metadata: any;
}

export interface WorkflowProfilesListState {
  requireIntegrations: boolean;
  isLoading: boolean;
  error: any;
  data: listDataResponse | undefined;
}

export interface WorkflowDetailsState {
  isLoading: boolean;
  error: any;
  data: RestWorkflowProfile | undefined;
}

export interface WorkflowSavingState {
  isSaving: boolean;
  error: any;
  saveClicked: boolean;
  newId?: string;
}

export interface WorkflowProfileState {
  profiles: WorkflowProfilesListState;
  selectedProfile: WorkflowDetailsState;
  savingStatus: WorkflowSavingState;
  workspaceOUList: Record<string, { isloading: boolean; OUlist: Array<any> }>;
}

const INITIAL_STATE: WorkflowProfileState = {
  profiles: {
    requireIntegrations: false,
    isLoading: true,
    error: undefined,
    data: undefined
  },
  selectedProfile: {
    isLoading: false,
    error: undefined,
    data: undefined
  },
  savingStatus: {
    isSaving: false,
    error: undefined,
    saveClicked: false
  },
  workspaceOUList: {}
};

const savingWorkflowProfile = (state: WorkflowProfileState, action: any): WorkflowProfileState => ({
  ...state,
  savingStatus: {
    saveClicked: true,
    isSaving: true,
    error: undefined
  }
});

const saveWorkflowProfileSuccessful = (state: WorkflowProfileState, action: any): WorkflowProfileState => ({
  ...state,
  savingStatus: {
    saveClicked: true,
    isSaving: false,
    error: undefined,
    newId: action.id
  }
});

const saveWorkflowProfileFailed = (state: WorkflowProfileState, action: any): WorkflowProfileState => ({
  ...state,
  savingStatus: {
    saveClicked: true,
    isSaving: false,
    error: action.error,
    newId: undefined
  }
});

const readWorkflowProfileDetails = (state: WorkflowProfileState, action: any): WorkflowProfileState => ({
  ...state,
  selectedProfile: {
    ...state.selectedProfile,
    isLoading: false,
    data: undefined
  },
  savingStatus: {
    ...state.savingStatus,
    saveClicked: false
  }
});

const selectedWorkflowProfileLoadSuccessful = (state: WorkflowProfileState, action: any): WorkflowProfileState => ({
  ...state,
  selectedProfile: {
    isLoading: false,
    error: undefined,
    data: action.data
  }
});

const selectedWorkflowProfileLoadFailed = (state: WorkflowProfileState, action: any): WorkflowProfileState => ({
  ...state,
  selectedProfile: {
    isLoading: false,
    error: action.error,
    data: undefined
  }
});

const clearSavedWorkflowProfile = (state: WorkflowProfileState, action: any): WorkflowProfileState => ({
  ...state,
  selectedProfile: {
    ...INITIAL_STATE.selectedProfile
  },
  savingStatus: {
    ...INITIAL_STATE.savingStatus
  }
});

const workflowProfileReducer = createReducer(INITIAL_STATE, {
  [workflowProfileActions.WORKFLOW_PROFILE_UPDATE]: savingWorkflowProfile,
  [workflowProfileActions.WORKFLOW_PROFILE_CREATE]: savingWorkflowProfile,
  [workflowProfileActions.WORKFLOW_PROFILE_SAVE_SUCCESSFUL]: saveWorkflowProfileSuccessful,
  [workflowProfileActions.WORKFLOW_PROFILE_SAVE_FAILED]: saveWorkflowProfileFailed,
  [workflowProfileActions.WORKFLOW_PROFILE_READ]: readWorkflowProfileDetails,
  [workflowProfileActions.WORKFLOW_PROFILE_LIST_LOAD_SUCCESSFUL]: selectedWorkflowProfileLoadSuccessful,
  [workflowProfileActions.WORKFLOW_PROFILES_LIST_LOAD_FAILED]: selectedWorkflowProfileLoadFailed,
  [workflowProfileActions.WORKFLOW_PROFILE_CLEAR]: clearSavedWorkflowProfile
});

export default workflowProfileReducer;
