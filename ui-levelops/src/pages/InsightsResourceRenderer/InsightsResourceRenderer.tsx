import React, { ReactElement, useEffect, useState } from "react";
import { Layout, Text, Button } from "@harness/uicore";
import "./InsightsResourceRenderer.scss";
import ReduxStoreProvider from "reduxConfigs/ReduxStoreProvider";
import { RbacResourceRendererProps } from "@harness/microfrontends/dist/modules/20-rbac/factories/RbacFactory";
import useOpenApiClients from "custom-hooks/useOpenAPIClients";
import { buildQueryParam } from "helper/queryParamHelper";
import { Dashboard, useDashboardsListMutation } from "@harnessio/react-sei-service-client";
import { useWorkspace } from "custom-hooks/useWorkspace";

const InsightsResource = ({
  identifiers,
  onResourceSelectionChange, // this is required for delete
  resourceType,
  resourceScope
}: RbacResourceRendererProps): ReactElement => {
  const [dashboards, setDashboards] = useState<Dashboard[]>([]);
  const { accountIdentifier, projectIdentifier = "", orgIdentifier = "" } = resourceScope;
  const queryParams = buildQueryParam(accountIdentifier, orgIdentifier, projectIdentifier);

  const { mutate: getDashboards, isLoading, isError, data } = useDashboardsListMutation({});

  const { workspaceId, isFetching } = useWorkspace({
    accountId: accountIdentifier,
    projectIdentifier: projectIdentifier,
    orgIdentifier: orgIdentifier
  });

  useEffect(() => {
    if (!isLoading && !isError && data) {
      // @ts-ignore
      const { records } = data.content;
      if (records) {
        setDashboards(records);
      }
    }
  });

  useEffect(() => {
    if (!isFetching && workspaceId) {
      getDashboards({
        queryParams,
        body: {
          filter: {
            // @ts-ignore
            workspace_id: Number(workspaceId),
            has_rbac_access: true,
            ids: identifiers
          }
        }
      });
    }
  }, [isFetching, workspaceId, identifiers]);

  return (
    <Layout.Vertical padding={{ top: "large" }}>
      {dashboards.map((dashboard: Dashboard) => (
        <Layout.Horizontal padding="large" className={"insightResource"} key={dashboard.id} flex>
          <Text>{dashboard.name}</Text>
          <Button
            icon="main-trash"
            minimal
            onClick={() => {
              onResourceSelectionChange(resourceType, false, [dashboard.id || ""]);
            }}
          />
        </Layout.Horizontal>
      ))}
    </Layout.Vertical>
  );
};

const InsightsResourceRenderer = (props: RbacResourceRendererProps) => {
  window.isStandaloneApp = false;
  useOpenApiClients(() => {}, props.resourceScope.accountIdentifier);
  return (
    <ReduxStoreProvider>
      <InsightsResource {...props} />
    </ReduxStoreProvider>
  );
};

export default InsightsResourceRenderer;
