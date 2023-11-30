import { Spin } from "antd";
import { useWorkSpaceList } from "custom-hooks/workspace/useWorkSpaceList";
import React, { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { setSelectedWorkspace } from "reduxConfigs/actions/workspaceActions";
import { WorkspaceModel } from "reduxConfigs/reducers/workspace/workspaceTypes";
import { SELECTED_WORKSPACE_ID } from "reduxConfigs/selectors/workspace/constant";
import { getSelectedWorkspace } from "reduxConfigs/selectors/workspace/workspace.selector";
import Home from "./home/Home";
import LandingPage from "./landing-page/LandingPage";
import { useWorkspace } from "custom-hooks/useWorkspace";
import { useAppStore } from "contexts/AppStoreContext";
import GetStarted from "pages/GetStarted/GetStarted";
import { PageSpinner } from "@harness/uicore";
import { useLocation } from "react-router-dom";
import queryString from "query-string";
import { DASHBOARD_ID_KEY } from "constants/localStorageKeys";

const LandingPageContainer: React.FC = () => {
  const selectedWorkspace: WorkspaceModel = useSelector(getSelectedWorkspace);
  const { search } = useLocation();

  const { loading: workSpaceloading, workSpaceListData } = useWorkSpaceList();
  const dispatch = useDispatch();

  const { accountInfo, selectedProject } = useAppStore();
  const { identifier: accountId = "" } = accountInfo || {};
  const { identifier: projectIdentifier = "", orgIdentifier = "" } = selectedProject || {};

  const { isFetching, workspaceId } = useWorkspace({
    accountId,
    orgIdentifier,
    projectIdentifier
  });

  useEffect(() => {
    const { dashboard_id } = queryString.parse(search);
    const currentDashboard = localStorage.getItem(DASHBOARD_ID_KEY);
    if (dashboard_id && dashboard_id.toString() !== currentDashboard?.toString())
      localStorage.setItem(DASHBOARD_ID_KEY, dashboard_id as string);
  }, [search]);

  useEffect(() => {
    if (window.isStandaloneApp && !workSpaceloading && !Object.keys(selectedWorkspace).length && workSpaceListData) {
      if (workSpaceListData.length === 1) {
        dispatch(setSelectedWorkspace(SELECTED_WORKSPACE_ID, workSpaceListData[0]));
      }
    }
  }, [workSpaceListData, workSpaceloading]);

  if (window.isStandaloneApp) {
    if (workSpaceloading)
      return (
        <div className="flex align-center justify-center" style={{ width: "100%", height: "100%" }}>
          <Spin />
        </div>
      );

    return Object.keys(selectedWorkspace).length ? <LandingPage /> : <Home workspaces={workSpaceListData} />;
  }
  if (isFetching) return <PageSpinner />;
  if (workspaceId) return <LandingPage />;
  return <GetStarted />;
};

export default React.memo(LandingPageContainer);
