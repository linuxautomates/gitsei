/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

import React, { useEffect, useState } from "react";
import { Container, PageSpinner, PageError } from "@harness/uicore";
import OrgTreeView from "shared-resources/components/OrgTreeView/OrgTreeView";
import { OrgUnitDto, useListUnits_2Mutation } from "@harnessio/react-sei-service-client";
import { useWorkspace } from "custom-hooks/useWorkspace";
import ReduxStoreProvider from "reduxConfigs/ReduxStoreProvider";
import { RbacResourceModalProps } from "@harness/microfrontends/dist/modules/20-rbac/factories/RbacFactory";
import useOpenApiClients from "custom-hooks/useOpenAPIClients";
import { buildQueryParam } from "helper/queryParamHelper";

const CollectionResource = ({ searchTerm, onSelectChange, selectedData, resourceScope }: RbacResourceModalProps) => {
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
            workspace_id: Number(workspaceId)
          }
        }
      });
    }
  }, [workspaceId, isFetching, queryParams]);

  useEffect(() => {
    if (data) {
      // @ts-ignore
      setOrgList(data.content.records || []);
    }
  }, [data]);

  return (
    <Container padding="medium">
      {isLoading && <PageSpinner />}
      {isError && <PageError />}
      {orgList && (
        <OrgTreeView
          selectedData={selectedData}
          onSelectChange={onSelectChange}
          searchTerm={searchTerm}
          collections={orgList}
        />
      )}
    </Container>
  );
};

const CollectionResourceModalBody = (props: RbacResourceModalProps) => {
  window.isStandaloneApp = false;
  useOpenApiClients(() => {}, props.resourceScope.accountIdentifier);
  return (
    <ReduxStoreProvider>
      <CollectionResource {...props} />
    </ReduxStoreProvider>
  );
};

export default CollectionResourceModalBody;
