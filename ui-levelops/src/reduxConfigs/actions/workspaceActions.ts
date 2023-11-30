import { WorkspaceDataType, WorkspaceMethodType, WorkspaceModel } from "reduxConfigs/reducers/workspace/workspaceTypes";
import {
  SET_OU_HEADER_SELECTED_WORKSPACE,
  GET_WORKSPACE_CATEGORIES,
  SET_SELECTED_WORKSPACE,
  SET_SESSION_SELECTED_WORKSPACE,
  SET_WORKSPACE_STATE,
  WORKSPACE_READ,
  WORK_SPACE_API_CLEAR,
  CLEAR_SELECTED_WORKSPACE
} from "./actionTypes";

export const setWorkspaceState = (method: WorkspaceMethodType, id: string, payload: WorkspaceDataType) => ({
  type: SET_WORKSPACE_STATE,
  method,
  id,
  payload
});

export const setSelectedWorkspace = (id: string, payload: WorkspaceModel | number) => ({
  type: SET_SELECTED_WORKSPACE,
  id,
  payload
});

export const clearSelectedWorkspace = (id: string) => ({
  type: CLEAR_SELECTED_WORKSPACE,
  id
});

export const setOUHeaderSelectedWorkspace = (id: string, payload: string) => ({
  type: SET_OU_HEADER_SELECTED_WORKSPACE,
  id,
  payload
});

export const workspaceRead = (id: string, method: WorkspaceMethodType, data?: any) => ({
  type: WORKSPACE_READ,
  data,
  id,
  method
});

export const workspaceApiClear = (id: string, method?: WorkspaceMethodType) => ({
  type: WORK_SPACE_API_CLEAR,
  id,
  method
});

export const workSpaceGet = (id: string) => ({
  type: WORKSPACE_READ,
  id,
  method: "get"
});

export const setSessionSelectedWorkspace = (queryParamWorkspaceId: string) => ({
  type: SET_SESSION_SELECTED_WORKSPACE,
  queryParamWorkspaceId
});

export const getWorkspaceCategories = (workspaceId: string, dashboardId: string, id: string) => ({
  type: GET_WORKSPACE_CATEGORIES,
  id,
  dashboardId,
  workspaceId,
  method: "list"
});
