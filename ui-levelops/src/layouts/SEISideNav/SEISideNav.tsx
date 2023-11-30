/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

import React, { useEffect, useMemo, useState } from "react";
import { useHistory } from "react-router-dom";
import { Color, Container, Layout, Tabs, Text } from "@harness/uicore";
import "./SEISideNav.scss";
import { ProjectPathProps } from "@harness/microfrontends/dist/modules/10-common/interfaces/RouteInterfaces";
import { useParentProvider } from "contexts/ParentProvider";
import { useAppStore } from "contexts/AppStoreContext";
import {
  getBaseUrl,
  getCollectionsPage,
  getContributersPage,
  getIntegrationMappingPage,
  getIntegrationPage,
  getInvestmentPage,
  getPropelsPage,
  getTablesPage,
  getTrellisPage,
  getWorkflowProfilePage
} from "constants/routePaths";
import { PermissionIdentifier, ResourceType } from "@harness/microfrontends";
import { useWorkspace } from "custom-hooks/useWorkspace";

const SEISideNav = (): React.ReactElement => {
  const [selectedTabId, setSelectedTabId] = useState<string>("project");
  const {
    components: { SidebarLink, ProjectSelector, NavExpandable, HarnessSideNav },
    routes
  } = useParentProvider();
  const {
    hooks: { usePermission }
  } = useParentProvider();

  const { updateAppStore, selectedOrg, selectedProject, accountInfo } = useAppStore();
  const history = useHistory();

  const params: ProjectPathProps = {
    accountId: accountInfo?.identifier || "",
    orgIdentifier: selectedOrg?.identifier || "",
    projectIdentifier: selectedProject?.identifier || ""
  };

  const { workspaceId, isFetching } = useWorkspace(params);

  const projectTabContent = useMemo(
    () => (
      <>
        <ProjectSelector
          onSelect={data => {
            updateAppStore({ selectedProject: data });
            history.push(getBaseUrl());
          }}
          // @ts-ignore
          fallbackAccountId={params.accountId}
        />
        {selectedProject && (
          <>
            <Container className={"group"}>
              {!isFetching && workspaceId && <SidebarLink label={"Insights"} to={`${getBaseUrl(params)}/dashboards`} />}
            </Container>
            <NavExpandable title={"Project Setup"} route={getBaseUrl(params)} defaultExpanded={true}>
              <Layout.Vertical spacing="small">
                <SidebarLink label={"SEI Integration Mapping"} to={getIntegrationMappingPage()} />
                <SidebarLink label={"SEI Collections"} to={getCollectionsPage(params)} />
                <SidebarLink to={routes.toAccessControl({ ...params, module: "sei" })} label={"Access Control"} />
              </Layout.Vertical>
            </NavExpandable>
          </>
        )}
      </>
    ),
    [history, params, selectedOrg, selectedProject, updateAppStore, workspaceId, isFetching, params.accountId]
  );

  const accountTabContent = useMemo(
    () => (
      <>
        <Layout.Vertical className={"group"}>
          <Text className={"groupHeader"} font="xsmall" margin={"small"} color={Color.WHITE}>
            Data settings
          </Text>
          <SidebarLink label={"SEI Integrations"} to={getIntegrationPage()} />
          <SidebarLink label={"Contributors"} to={getContributersPage()} />
        </Layout.Vertical>
        <Layout.Vertical className={"group"}>
          <Text className={"groupHeader"} font="xsmall" margin={"small"} color={Color.WHITE}>
            Profiles
          </Text>
          <SidebarLink label={"Workflow"} to={getWorkflowProfilePage()} />
          <SidebarLink label={"Investment"} to={getInvestmentPage()} />
          <SidebarLink label={"Trellis"} to={getTrellisPage()} />
        </Layout.Vertical>
        <Layout.Vertical className={"group"}>
          <Text className={"groupHeader"} font="xsmall" margin={"small"} color={Color.WHITE}>
            {"Advanced Features"}
          </Text>
          <SidebarLink label="Tables" to={getTablesPage()} />
          <SidebarLink label={"Propels"} to={getPropelsPage()} />
        </Layout.Vertical>
      </>
    ),
    [params]
  );

  useEffect(() => {
    let tab = "project";
    if (
      (history.location.pathname.includes("configuration") ||
        history.location.pathname.includes("tables") ||
        history.location.pathname.includes("propels")) &&
      !history.location.pathname.endsWith("configuration/organization")
    ) {
      tab = "account";
    }
    setSelectedTabId(tab);
  }, [history.location.pathname]);

  const [hasAccountAccess] = usePermission
    ? usePermission({
        resourceScope: {
          accountIdentifier: accountInfo?.identifier || ""
        },
        resource: {
          resourceType: ResourceType.SEI_CONFIGURATION_SETTINGS
        },
        permissions: [PermissionIdentifier.VIEW_SEI_CONFIGURATIONSETTINGS]
      })
    : [false];

  return (
    <HarnessSideNav subtitle="Software Engineering" title="Insights" icon="sei-main">
      <Layout.Vertical spacing="small" className={"seiSideNav"}>
        {hasAccountAccess ? (
          <Tabs
            id={"seiNavigationTabs"}
            selectedTabId={selectedTabId}
            tabList={[
              {
                id: "project",
                title: "Project",
                panel: projectTabContent
              },
              { id: "account", title: "Account", panel: accountTabContent }
            ]}
            onChange={nextTab => setSelectedTabId(nextTab as string)}
          />
        ) : (
          projectTabContent
        )}
      </Layout.Vertical>
    </HarnessSideNav>
  );
};

export default SEISideNav;
