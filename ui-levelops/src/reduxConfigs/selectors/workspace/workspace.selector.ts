import { get } from "lodash";
import { createSelector } from "reselect";
import { createParameterSelector } from "../selector";
import { SELECTED_OU_HEADER_WORKSPACE, SELECTED_WORKSPACE_ID } from "./constant";

export const workSpaceState = (state: any) => state.workSpaceReducer;

const getMETHOD = createParameterSelector((params: any) => params.method);
const getUUID = createParameterSelector((params: any) => params.uuid);

export const getGenericWorkSpaceMethodSelector = createSelector(
  workSpaceState,
  getMETHOD,
  (data: any, method: string) => {
    return get(data, [method || "list"], {});
  }
);

export const getGenericWorkSpaceUUIDSelector = createSelector(
  getGenericWorkSpaceMethodSelector,
  getUUID,
  (data: any, uuid: string) => {
    return get(data, [uuid || "0"], {});
  }
);

export const getSelectedWorkspace = createSelector(workSpaceState, data => get(data, [SELECTED_WORKSPACE_ID], {}));

export const getOUHeaderSelectedWorkspace = createSelector(workSpaceState, data => {
  let ouHeaderWorkspaceId = get(data, [SELECTED_OU_HEADER_WORKSPACE], undefined);
  if (typeof ouHeaderWorkspaceId !== "string") {
    ouHeaderWorkspaceId = undefined;
  }
  return ouHeaderWorkspaceId;
});
