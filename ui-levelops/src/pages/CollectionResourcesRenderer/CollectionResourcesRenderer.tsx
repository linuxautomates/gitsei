import React, { ReactElement, useEffect, useState } from "react";
import { Layout, Text, Button } from "@harness/uicore";
import "./CollectionResourcesRenderer.scss";
import ReduxStoreProvider from "reduxConfigs/ReduxStoreProvider";
import { RbacResourceRendererProps } from "@harness/microfrontends/dist/modules/20-rbac/factories/RbacFactory";
import useOpenApiClients from "custom-hooks/useOpenAPIClients";
import { buildQueryParam } from "helper/queryParamHelper";
import { OrgUnitDto, useListUnits_2Mutation } from "@harnessio/react-sei-service-client";
import { useWorkspace } from "custom-hooks/useWorkspace";

const CollectionResources = ({
  identifiers,
  onResourceSelectionChange, // this is required for delete
  resourceType,
  resourceScope
}: RbacResourceRendererProps): ReactElement => {
  const [orgList, setOrgList] = useState<OrgUnitDto[]>([]);
  const [loadingOrgs, setLoadingOrgs] = useState<boolean>(false);
  const { accountIdentifier, projectIdentifier = "", orgIdentifier = "" } = resourceScope;

  const { data, isLoading, isError, mutate: fetchOrgs, error } = useListUnits_2Mutation();
  const queryParams = buildQueryParam(accountIdentifier, orgIdentifier, projectIdentifier);

  const { workspaceId, isFetching } = useWorkspace({
    accountId: accountIdentifier,
    projectIdentifier: projectIdentifier,
    orgIdentifier: orgIdentifier
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
            workspace_id: Number(workspaceId),
            ref_id: identifiers
          }
        }
      });
    }
  }, [workspaceId, isFetching, queryParams, identifiers]);

  useEffect(() => {
    if (data) {
      // @ts-ignore
      setOrgList(data.content.records || []);
    }
  }, [data]);
  return (
    <Layout.Vertical padding={{ top: "large" }}>
      {orgList.map((org: OrgUnitDto) => (
        <Layout.Horizontal padding="large" className={"collectionResource"} key={org.id} flex>
          <Text>{org.name}</Text>
          <Button
            icon="main-trash"
            minimal
            onClick={() => {
              onResourceSelectionChange(resourceType, false, [org.id || ""]);
            }}
          />
        </Layout.Horizontal>
      ))}
    </Layout.Vertical>
  );
};

const CollectionResourcesRenderer = (props: RbacResourceRendererProps) => {
  window.isStandaloneApp = false;
  useOpenApiClients(() => {}, props.resourceScope.accountIdentifier);
  return (
    <ReduxStoreProvider>
      <CollectionResources {...props} />
    </ReduxStoreProvider>
  );
};

export default CollectionResourcesRenderer;
