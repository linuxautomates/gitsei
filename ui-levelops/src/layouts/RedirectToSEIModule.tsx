import { getBaseUrl, getBaseUrlWhenNoScopeSelected } from "constants/routePaths";
import { useAppStore } from "contexts/AppStoreContext";
import React from "react";
import { Redirect } from "react-router-dom";
import { isNoScopeActive } from "./layout.utils";
import { NO_SCOPE } from "./layouts.constants";

const RedirectToSEIModule = (): React.ReactElement => {
  const { selectedProject, accountInfo } = useAppStore();
  let accountId = "";
  if (accountInfo) {
    accountId = accountInfo.identifier || "";
  }
  const { identifier: projectIdentifier = "", orgIdentifier = "" } = selectedProject || {};
  const searchParam = window.location.search;
  let redirectionPath = getBaseUrl();

  if (isNoScopeActive(searchParam) && accountId && orgIdentifier && projectIdentifier) {
    redirectionPath = getBaseUrlWhenNoScopeSelected({ accountId, orgIdentifier, projectIdentifier });
  }

  if (selectedProject) {
    return <Redirect to={`${redirectionPath}/dashboards`} />;
  } else if (isNoScopeActive(searchParam)) {
    return <Redirect to={`${getBaseUrl()}/home${NO_SCOPE}`} />;
  }
  return <Redirect to={`${getBaseUrl()}/home`} />;
};

export default RedirectToSEIModule;
