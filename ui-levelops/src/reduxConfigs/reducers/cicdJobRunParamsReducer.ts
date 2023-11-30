import { RestWorkflowProfile } from "classes/RestWorkflowProfile";
import { cloneDeep } from "lodash";
import { cicdJobParamsActions } from "reduxConfigs/actions/actionTypes";
import { createReducer } from "../createReducer";

export interface CicdJobRunParamsState {
  cicdJobRunParams: Record<string, { isLoading: boolean; data: RestWorkflowProfile; error?: any }>;
}

const INITIAL_STATE: CicdJobRunParamsState = {
  cicdJobRunParams: {}
};

const getCICDJobParams = (state: CicdJobRunParamsState, action: any): CicdJobRunParamsState => ({
  ...state,
  cicdJobRunParams: {
    ...state.cicdJobRunParams,
    [action.id]: {
      error: false,
      isLoading: true
    }
  }
});

const getCICDJobParamsSuccess = (state: CicdJobRunParamsState, action: any): CicdJobRunParamsState => {
  return {
    ...state,
    cicdJobRunParams: {
      ...state.cicdJobRunParams,
      [action.id]: {
        isLoading: false,
        data: action.data
      }
    }
  };
};

const getCICDJobParamsFailed = (state: CicdJobRunParamsState, action: any): CicdJobRunParamsState => ({
  ...state,
  cicdJobRunParams: {
    ...state.cicdJobRunParams,
    [action.id]: {
      isLoading: false,
      data: undefined,
      error: action.error
    }
  }
});

const clearCICDJobParams = (state: CicdJobRunParamsState, action: any): CicdJobRunParamsState => {
  const newState = cloneDeep(state);
  if (newState.cicdJobRunParams[action.id]) {
    delete newState.cicdJobRunParams[action.id];
  }
  return newState;
};

const cicdJobRunParamsReducer = createReducer(INITIAL_STATE, {
  [cicdJobParamsActions.GET_CICD_JOB_PARAMS]: getCICDJobParams,
  [cicdJobParamsActions.CLEAR_CICD_JOB_PARAMS]: clearCICDJobParams,
  [cicdJobParamsActions.GET_CICD_JOB_PARAMS_FAIL]: getCICDJobParamsFailed,
  [cicdJobParamsActions.GET_CICD_JOB_PARAMS_SUCCESS]: getCICDJobParamsSuccess
});

export default cicdJobRunParamsReducer;
