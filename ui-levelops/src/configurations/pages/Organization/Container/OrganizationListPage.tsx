import { useAppStore } from "contexts/AppStoreContext";
import { useWorkspace } from "custom-hooks/useWorkspace";
import React, { useEffect, useState } from "react";
import OrganizationContainer from "./OrganizationContainer";
import { RouteComponentProps, useHistory } from "react-router-dom";
import { PageSpinner } from "@harness/uicore";
import GetStartedModal from "pages/GetStarted/GetStartedModal";
import { OrgUnitDto, useListUnits_2Mutation } from "@harnessio/react-sei-service-client";
import RBACNoAccessScreen from "components/RBACNoAccessScreen/RBACNoAccessScreen";
import { buildQueryParam } from "helper/queryParamHelper";
import queryString from "query-string";

const OrganizationListPage = (props: RouteComponentProps) => {
  const [loadingOrgs, setLoadingOrgs] = useState<boolean>(false);
  const [hasViewAccess, setHasViewAccess] = useState<boolean>(false);
  const [rbacOUs, setRbacOUs] = useState<number[] | undefined>();
  const history = useHistory();

  const { ou_workspace_id } = queryString.parse(location.search);
  const { selectedProject, accountInfo } = useAppStore();
  const { identifier: accountId = "" } = accountInfo || {};
  const { identifier: projectIdentifier = "", orgIdentifier = "" } = selectedProject || {};

  const { data, isLoading: loadingOUs, isError, mutate: fetchOrgs, error } = useListUnits_2Mutation();
  const queryParams = buildQueryParam(accountId, orgIdentifier, projectIdentifier);

  const { isFetching, workspaceId } = useWorkspace({
    accountId,
    orgIdentifier,
    projectIdentifier
  });

  useEffect(() => {
    if (!isFetching && workspaceId && !loadingOrgs) {
      setLoadingOrgs(true);
      fetchOrgs({
        // @ts-ignore
        queryParams,
        body: {
          // @ts-ignore
          filter: {
            workspace_id: Number(workspaceId)
          }
        }
      });
    }
  }, [workspaceId, isFetching, queryParams]);

  useEffect(() => {
    if (data) {
      const { records = [] } = data.content;
      const accessibleOU = records.filter((ou: OrgUnitDto) => Boolean(ou.access_response?.view));
      if (records.length !== accessibleOU.length) {
        setRbacOUs(accessibleOU.map(ou => Number(ou.id)));
      }
      if (records.length > 0) {
        setHasViewAccess(accessibleOU.length > 0);
      } else {
        setHasViewAccess(true);
      }
    }
  }, [data]);

  if (window.isStandaloneApp) {
    return <OrganizationContainer {...props} />;
  }
  if (workspaceId) {
    if (ou_workspace_id !== workspaceId) {
      history.push({
        search: `?ou_workspace_id=${workspaceId}`
      });
    }
    if (loadingOUs) {
      return <PageSpinner />;
    }
    if (hasViewAccess) {
      return <OrganizationContainer {...props} rbacOUs={rbacOUs} />;
    }
    return <RBACNoAccessScreen />;
  }
  if (isFetching) return <PageSpinner />;
  return <GetStartedModal />;
};

export default OrganizationListPage;
