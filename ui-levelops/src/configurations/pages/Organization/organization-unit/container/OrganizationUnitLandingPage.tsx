import { Icon, Tabs } from "antd";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import queryString from "query-string";
import "../OrganizationUnit.styles.scss";
import OrganizationUnitPivotCreateModal from "../components/pivot-create-modal/OrganizationUnitPivotCreateModal";
import OrgUnitListContainer from "./org-unit-list-page/OrgUnitListContainer";
import { PivotType } from "configurations/configuration-types/OUTypes";
import { AntBadge, AntIcon, AntText } from "shared-resources/components";
import OrganizationUnitPivotUpdateModal from "../components/pivot-create-modal/OrganizationUnitPivotUpdateModal";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { useHistory, useLocation } from "react-router-dom";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";
import { useCollectionPermissions } from "custom-hooks/HarnessPermissions/useCollectionPermissions";
import { getIsStandaloneApp } from "helper/helper";
import { useHasConfigReadOnlyPermission } from "custom-hooks/HarnessPermissions/useHasConfigReadOnlyPermission";

const { TabPane } = Tabs;

interface OrganizationUnitLandingPageProps {
  tabs: Array<PivotType>;
  currentWorkspaceIsDemo: boolean;
  fetchPivotsList: () => void;
  isAdmin: boolean;
}

const OrganizationUnitLandingPage: React.FC<OrganizationUnitLandingPageProps> = ({
  tabs,
  fetchPivotsList,
  currentWorkspaceIsDemo,
  isAdmin
}) => {
  const history = useHistory();
  const location = useLocation();
  const [activekey, setActiveKey] = useState<string>("");
  const [pivotCreateModalVisible, setPivotCreateModalVisibility] = useState<boolean>(false);
  const [editablePivot, setEditablePivot] = useState<PivotType | undefined>(undefined);
  const ouEnhancementSupport = useHasEntitlements(Entitlement.ORG_UNIT_ENHANCEMENTS, EntitlementCheckType.AND);

  let { ou_category_tab, ou_workspace_id } = queryString.parse(location.search);

  const updateURL = (categoryId?: string) => {
    history.push({
      search: `?ou_workspace_id=${ou_workspace_id}&ou_category_tab=${categoryId}`
    });
  };

  useEffect(() => {
    if ((tabs ?? []).length && ou_category_tab) {
      const corsTab: PivotType | undefined = tabs.find(t => t.id === ou_category_tab);
      if (!corsTab) {
        ou_category_tab = null;
      }
    }
    if (!!ou_category_tab) {
      setActiveKey(ou_category_tab as string);
    } else {
      const nActiveKey = tabs.length ? tabs[0].id : "";
      setActiveKey(nActiveKey);
      updateURL(nActiveKey);
    }
  }, [tabs]);

  const onTabChangeHandler = (key: any) => {
    setActiveKey(key);
    updateURL(key);
  };

  const handlePivotEdit = (value?: PivotType) => {
    setEditablePivot(value);
  };

  const resetTabs = (pivotId?: string) => {
    if (pivotId) {
      updateURL(pivotId);
    }
    fetchPivotsList();
  };

  const [hasCreateAccess, hasEditAccess] = useCollectionPermissions();
  const oldReadOnly = getRBACPermission(PermeableMetrics.ORG_UNIT_READ_ONLY);
  const orgUnitReadOnly = window.isStandaloneApp ? oldReadOnly : !hasCreateAccess;
  const isConfigReadonly = useHasConfigReadOnlyPermission();
  const pivotUpdateAccess = window.isStandaloneApp
    ? getRBACPermission(PermeableMetrics.ORG_PIVOT_UPDATE)
    : !isConfigReadonly;
  const renderTabTitle = useCallback(
    (tab: PivotType) => (
      <div className="tab-title">
        <span className="mr-10">{tab.name}</span>
        <AntBadge
          style={{ backgroundColor: tab.enabled ? "var(--harness-blue)" : "var(--text-secondary-color)" }}
          count={tab.count_of_ous}
          overflowCount={tab.count_of_ous}
          showZero
          className="mr-10"
        />
        {activekey === tab.id && pivotUpdateAccess && !currentWorkspaceIsDemo && (
          <AntIcon type="setting" onClick={e => handlePivotEdit(tab)} />
        )}
      </div>
    ),
    [tabs, activekey, currentWorkspaceIsDemo, pivotUpdateAccess]
  );

  const renderExtraTabContent = useMemo(() => {
    const hasAdminAccess = getIsStandaloneApp() ? isAdmin : !isConfigReadonly;
    return (
      <div
        onClick={
          !currentWorkspaceIsDemo && !orgUnitReadOnly ? (e: any) => setPivotCreateModalVisibility(true) : () => {}
        }
        className={orgUnitReadOnly ? "" : "pointer"}>
        {hasAdminAccess && (
          <>
            {" "}
            | <span className="ml-10">Add custom</span>
            <Icon type={"plus"} className="ml-5" />
          </>
        )}
      </div>
    );
  }, [activekey, currentWorkspaceIsDemo, isConfigReadonly]);

  const disablePivotEnableSwitch = useMemo(
    () => !!editablePivot?.enabled && tabs?.filter(t => t.enabled).length === 1,
    [tabs, editablePivot]
  );

  return (
    <div className="organization-units-container">
      <Tabs
        size={"small"}
        activeKey={activekey}
        animated={false}
        onChange={onTabChangeHandler}
        className={tabs.length ? "" : "ant-tabs-container"}
        tabBarExtraContent={renderExtraTabContent}>
        {tabs.map(tab => (
          <TabPane key={tab.id} tab={renderTabTitle(tab)}>
            {editablePivot && (
              <OrganizationUnitPivotUpdateModal
                pivot={editablePivot}
                setVisibility={handlePivotEdit}
                resetTabs={resetTabs}
                disablePivotEnableSwitch={disablePivotEnableSwitch}
              />
            )}
            {!tab.enabled && (
              <div className="disabled-category-warning">
                <AntIcon type="warning" />
                <AntText>
                  This Collection has been disabled by an Administrator. Go to Collection category settings to re-enable
                  it.
                </AntText>
              </div>
            )}
            <OrgUnitListContainer
              activeTab={tab}
              resetTabs={resetTabs}
              currentWorkspaceIsDemo={currentWorkspaceIsDemo}
              ouEnhancementSupport={ouEnhancementSupport}
            />
          </TabPane>
        ))}
      </Tabs>
      {pivotCreateModalVisible ? (
        <OrganizationUnitPivotCreateModal resetTabs={resetTabs} setVisibility={setPivotCreateModalVisibility} />
      ) : (
        <></>
      )}
    </div>
  );
};

export default OrganizationUnitLandingPage;
