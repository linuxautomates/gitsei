import { get } from "lodash";
import {
  CLEAR_SELECTED_WORKSPACE,
  SET_OU_HEADER_SELECTED_WORKSPACE,
  SET_SELECTED_WORKSPACE,
  SET_WORKSPACE_STATE,
  WORK_SPACE_API_CLEAR
} from "reduxConfigs/actions/actionTypes";
import LocalStoreService from "services/localStoreService";
import { WorkspaceActionType, WorkspaceHandlerType, WorkspaceInitialStateType } from "./workspaceTypes";

const _setWorkspaceState = (state: WorkspaceInitialStateType, action: WorkspaceActionType) => {
  if (action.method) {
    return {
      ...state,
      [action.method]: {
        ...get(state, [action.method], {}),
        [action.id]: action.payload
      }
    } as WorkspaceInitialStateType;
  }
  return state;
};

const _selectedWorkspace = (state: WorkspaceInitialStateType, action: WorkspaceActionType) => {
  /** NOTE : This is not the correct way of implementing this.
   * Will make changes in future with more concrete implementation. */
  const ls = new LocalStoreService();
  const workspaceId = get(action?.payload, ["id"]);
  if (workspaceId) {
    ls.setSelectedWorkspaceId(workspaceId);
  }
  return {
    ...state,
    [action.id]: action.payload
  } as WorkspaceInitialStateType;
};

const _clearSelectedWorkspace = (state: WorkspaceInitialStateType, action: WorkspaceActionType) => {
  const ls = new LocalStoreService();
  ls.setSelectedWorkspaceId('');
  return {
    ...state,
    [action.id]: undefined
  } as WorkspaceInitialStateType;
}

const _workspaceApiClear = (state: WorkspaceInitialStateType, action: WorkspaceActionType) => {
  if (action.method && action.id) {
    return {
      ...state,
      [action.method]: {
        ...get(state, [action.method], {}),
        [action.id]: {}
      }
    } as WorkspaceInitialStateType;
  } else if (action.id) {
    return {
      ...state,
      [action.id]: {}
    } as WorkspaceInitialStateType;
  }
  return state;
};

const _setOUHeaderSelectedWorkspace = (state: WorkspaceInitialStateType, action: WorkspaceActionType) => {
  return {
    ...state,
    [action.id]: action.payload
  } as WorkspaceInitialStateType;
};

export const workspaceHandler: Record<string, WorkspaceHandlerType> = {
  [SET_WORKSPACE_STATE]: _setWorkspaceState,
  [SET_SELECTED_WORKSPACE]: _selectedWorkspace,
  [WORK_SPACE_API_CLEAR]: _workspaceApiClear,
  [SET_OU_HEADER_SELECTED_WORKSPACE]: _setOUHeaderSelectedWorkspace,
  [CLEAR_SELECTED_WORKSPACE]: _clearSelectedWorkspace
};
