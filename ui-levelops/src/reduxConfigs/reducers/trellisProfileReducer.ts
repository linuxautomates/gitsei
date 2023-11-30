import { RestTrellisScoreProfile } from "classes/RestTrellisProfile";
import { trellisProfileActions } from "reduxConfigs/actions/actionTypes";
import { createReducer } from "../createReducer";

interface listDataResponse {
  records: Array<RestTrellisScoreProfile>;
  count: number;
  _metadata: any;
}

export interface TrellisProfilesListState {
  requireIntegrations: boolean;
  isLoading: boolean;
  error: any;
  data: listDataResponse | undefined;
}

export interface TrellisDetailsState {
  isLoading: boolean;
  error: any;
  data: RestTrellisScoreProfile | undefined;
}

export interface TrellisSavingState {
  isSaving: boolean;
  error: any;
  saveClicked: boolean;
}

export interface TrellisProfileState {
  profiles: TrellisProfilesListState;
  selectedProfile: TrellisDetailsState;
  savingStatus: TrellisSavingState;
  workspaceOUList: Record<string, { isloading: boolean; OUlist: Array<any> }>;
}

const INITIAL_STATE: TrellisProfileState = {
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

const getTrellisProfileList = (state: TrellisProfileState, action: any): TrellisProfileState => ({
  ...state,
  profiles: {
    ...state.profiles,
    requireIntegrations: false,
    isLoading: true
  }
});

const setNoIntegrations = (state: TrellisProfileState, action: any): TrellisProfileState => ({
  ...state,
  profiles: {
    ...state.profiles,
    requireIntegrations: true,
    isLoading: false
  }
});

const trellisProfilesListLoadSuccessful = (state: TrellisProfileState, action: any): TrellisProfileState => ({
  ...state,
  profiles: {
    ...state.profiles,
    isLoading: false,
    error: undefined,
    data: action.data
  }
});

const trellisProfilesListLoadFailed = (state: TrellisProfileState, action: any): TrellisProfileState => ({
  ...state,
  profiles: {
    ...state.profiles,
    isLoading: false,
    error: action.error,
    data: undefined
  }
});

const readTrellisProfileDetails = (state: TrellisProfileState, action: any): TrellisProfileState => ({
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

const selectedTrellisProfileLoadSuccessful = (state: TrellisProfileState, action: any): TrellisProfileState => ({
  ...state,
  selectedProfile: {
    isLoading: false,
    error: undefined,
    data: action.data
  }
});

const selectedTrellisProfileLoadFailed = (state: TrellisProfileState, action: any): TrellisProfileState => ({
  ...state,
  selectedProfile: {
    isLoading: false,
    error: action.error,
    data: undefined
  }
});

const savingTrellisProfile = (state: TrellisProfileState, action: any): TrellisProfileState => ({
  ...state,
  savingStatus: {
    saveClicked: true,
    isSaving: true,
    error: undefined
  }
});

const saveTrellisProfileSuccessful = (state: TrellisProfileState, action: any): TrellisProfileState => ({
  ...state,
  savingStatus: {
    saveClicked: true,
    isSaving: false,
    error: undefined
  }
});

const saveTrellisProfileFailed = (state: TrellisProfileState, action: any): TrellisProfileState => ({
  ...state,
  savingStatus: {
    saveClicked: true,
    isSaving: false,
    error: action.error
  }
});

const clearSavedTrellisProfile = (state: TrellisProfileState, action: any): TrellisProfileState => ({
  ...state,
  selectedProfile: {
    ...INITIAL_STATE.selectedProfile
  },
  savingStatus: {
    ...INITIAL_STATE.savingStatus
  }
});

const reuseExistingList = (state: TrellisProfileState, action: any): TrellisProfileState => ({
  ...state,
  profiles: {
    ...state.profiles,
    isLoading: false
  }
});

const getWorkspaceOrgList = (state: TrellisProfileState, action: any): TrellisProfileState => ({
  ...state,
  workspaceOUList: {
    ...state.workspaceOUList,
    [action.workspaceId]: {
      isloading: true
    }
  }
});

const addWorkspaceOrgList = (state: TrellisProfileState, action: any): TrellisProfileState => ({
  ...state,
  workspaceOUList: {
    ...state.workspaceOUList,
    [action.workspaceId]: {
      isloading: false,
      OUlist: action.orgList
    }
  }
});

const trellisProfileReducer = createReducer(INITIAL_STATE, {
  [trellisProfileActions.GET_TRELLIS_PROFILES_LIST]: getTrellisProfileList,
  [trellisProfileActions.SET_NO_INTEGRATIONS]: setNoIntegrations,
  [trellisProfileActions.TRELLIS_PROFILES_LIST_LOAD_SUCCESSFUL]: trellisProfilesListLoadSuccessful,
  [trellisProfileActions.TRELLIS_PROFILES_LIST_LOAD_FAILED]: trellisProfilesListLoadFailed,
  [trellisProfileActions.TRELLIS_PROFILE_READ]: readTrellisProfileDetails,
  [trellisProfileActions.TRELLIS_PROFILE_LOAD_SUCCESSFUL]: selectedTrellisProfileLoadSuccessful,
  [trellisProfileActions.TRELLIS_PROFILE_LOAD_FAILED]: selectedTrellisProfileLoadFailed,
  [trellisProfileActions.TRELLIS_PROFILE_UPDATE]: savingTrellisProfile,
  [trellisProfileActions.TRELLIS_PROFILE_CREATE]: savingTrellisProfile,
  [trellisProfileActions.TRELLIS_PROFILE_PARTIAL_UPDATE]: savingTrellisProfile,
  [trellisProfileActions.TRELLIS_PROFILE_SAVE_SUCCESSFUL]: saveTrellisProfileSuccessful,
  [trellisProfileActions.TRELLIS_PROFILE_SAVE_FAILED]: saveTrellisProfileFailed,
  [trellisProfileActions.TRELLIS_PROFILE_CLEAR]: clearSavedTrellisProfile,
  [trellisProfileActions.REUSE_EXISTING_TRELLIS_PROFILE_LIST]: reuseExistingList,
  [trellisProfileActions.GET_WORKSPACE_OU_LIST]: getWorkspaceOrgList,
  [trellisProfileActions.SAVE_WORKSPACE_OU_LIST]: addWorkspaceOrgList
});

export default trellisProfileReducer;
