import { useDispatch, useSelector } from "react-redux";
import { getSelectedWorkspace } from "reduxConfigs/selectors/workspace/workspace.selector";
import { WorkspaceModel } from "reduxConfigs/reducers/workspace/workspaceTypes";
import { useGetWorkspaceFromHarnessProjectQuery } from "@harnessio/react-sei-service-client";
import { useState, useEffect } from "react";
import { clearSelectedWorkspace, setSelectedWorkspace } from "reduxConfigs/actions/workspaceActions";
import { SELECTED_WORKSPACE_ID } from "reduxConfigs/selectors/workspace/constant";
import { buildQueryParam } from "helper/queryParamHelper";
import { dashboardDefault } from "reduxConfigs/actions/restapi";
import { DEFAULT_DASHBOARD_KEY } from "dashboard/constants/constants";

interface useWorkspaceProps {
  accountId: string;
  orgIdentifier: string;
  projectIdentifier: string;
}

export const useWorkspace = (resource: useWorkspaceProps) => {
  const [fetchWorkspace, setFetchWorkspace] = useState<boolean>(false);
  const [workspaceId, setWorkspaceId] = useState<string | undefined>();
  const queryParams = buildQueryParam(resource.accountId, resource.orgIdentifier, resource.projectIdentifier);
  const selectedWorkspace: WorkspaceModel = useSelector(getSelectedWorkspace);
  const dispatch = useDispatch();

  const { data, isFetching, isError, isFetched } = useGetWorkspaceFromHarnessProjectQuery(
    {
      queryParams: {
        company: resource?.accountId || "",
        ...queryParams
      },
      orgIdentifier: resource?.orgIdentifier || "",
      projectIdentifier: resource?.projectIdentifier || ""
    },
    { enabled: fetchWorkspace }
  );

  const refetch = () => setFetchWorkspace(true);

  useEffect(() => {
    if (!fetchWorkspace && resource && !isError && !isFetched) {
      if (
        resource.projectIdentifier &&
        !(selectedWorkspace && selectedWorkspace.key?.toLowerCase() === resource.projectIdentifier.toLowerCase())
      ) {
        setFetchWorkspace(true);
        dispatch(clearSelectedWorkspace(SELECTED_WORKSPACE_ID));
      }
    }
  }, [selectedWorkspace, resource, isFetching, isFetched]);

  useEffect(() => {
    if (fetchWorkspace && !isFetching) {
      setFetchWorkspace(false);
      if (isFetched && !isError && data?.content.id && data?.content.key) {
        dispatch(setSelectedWorkspace(SELECTED_WORKSPACE_ID, (data.content ?? {}) as WorkspaceModel));
        setWorkspaceId(data.content.id);
        dispatch(dashboardDefault(DEFAULT_DASHBOARD_KEY));
      }
    }
  }, [fetchWorkspace, data, isFetching, isFetched]);

  useEffect(() => {
    if (
      selectedWorkspace &&
      resource &&
      selectedWorkspace.key?.toLowerCase() === resource.projectIdentifier.toLowerCase()
    ) {
      selectedWorkspace.id !== workspaceId && setWorkspaceId(selectedWorkspace.id);
    } else if (workspaceId) {
      setWorkspaceId(undefined);
      dispatch(setSelectedWorkspace(SELECTED_WORKSPACE_ID, -1));
    }
  }, [selectedWorkspace, resource]);

  return { selectedWorkspace, workspaceId, isFetching: isFetching || fetchWorkspace, refetch };
};
