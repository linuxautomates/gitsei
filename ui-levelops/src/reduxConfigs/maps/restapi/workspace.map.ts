import {
  setSelectedWorkspace,
  setSessionSelectedWorkspace,
  workspaceApiClear,
  workSpaceGet
} from "reduxConfigs/actions/workspaceActions";

export const mapWorkspaceToProps = (dispatch: any) => {
  return {
    workspaceGet: (id: string) => dispatch(workSpaceGet(id)),
    setSessionWorkspace: (queryParamWorkspaceId: string) =>
      dispatch(setSessionSelectedWorkspace(queryParamWorkspaceId)),
    workspaceClear: (id: string) => dispatch(workspaceApiClear(id)),
    setSelectedWorkspace: (id: string, payload: any) => dispatch(setSelectedWorkspace(id, payload))
  };
};
