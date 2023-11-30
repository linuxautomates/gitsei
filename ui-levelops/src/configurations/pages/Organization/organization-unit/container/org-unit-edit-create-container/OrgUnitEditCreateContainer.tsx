import React, { useEffect, useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useLocation, useParams } from "react-router-dom";
import queryString from "query-string";
import { clearPageSettings, setPageButtonAction, setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { getGenericPageLocationSelector, getPageSettingsSelector } from "reduxConfigs/selectors/pagesettings.selector";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType, TOOLTIP_ACTION_NOT_ALLOWED } from "custom-hooks/constants";
import { RestOrganizationUnit } from "classes/RestOrganizationUnit";
import { orgUnitBasicInfoType, PivotType } from "configurations/configuration-types/OUTypes";
import { getBreadcumsForCreateOUPage } from "configurations/pages/ticket-categorization/helper/getBreadcrumsForCreateOUPage";
import OrgUnitBasicInfoComponent from "../../OrganizationUnitBasicInfoComponent";
import {
  OrganizationTabKey,
  ORG_UNIT_CONFIGURATION_TABS_CONFIG,
  ORG_UNIT_CONFIGURATION_TABS_CONFIG_NEW,
  PIVOT_LIST_ID,
  TRELLIS_TOOLTIP,
  TRELLIS_LABEL
} from "configurations/pages/Organization/Constants";
import OrganizationUnitConfigurationContainer from "../OrganizationUnitConfigurationContainer";
import "./orgUnitEditCreateContainer.styles.scss";
import OrgUnitDashboardConfigurationContainer from "../org-unit-dashboards-configuaration-container/OrgUnitDashboardConfigurationContainer";
import { getBreadcumsForEditOUPage } from "configurations/pages/ticket-categorization/helper/getBreadCrumbsForEditOUpage";
import { cloneDeep, get, isEqual } from "lodash";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { AntText } from "shared-resources/components";
import { ProjectPathProps } from "classes/routeInterface";
import { Icon, Switch, Tabs, Tooltip } from "antd";
import TrellisJobRoles from "components/trelis-job-roles/TrellisJobRoles";
import { OUAssociationWarningModal } from "components/trelis-job-roles/modals/OUAssociationWarningModal";
import { OUAssociationModal } from "components/trelis-job-roles/modals/OUAssociationModal";
import { validateTrellisProfile } from "components/trelis-job-roles/TrelisJobRoleHelper";
import { getDevProdCentralProfile, getDevProdParentProfileList } from "reduxConfigs/actions/devProdParentActions";
import { restapiLoading } from "reduxConfigs/actions/restapi";
import { getIsStandaloneApp } from "helper/helper";
import { useCollectionPermissions } from "custom-hooks/HarnessPermissions/useCollectionPermissions";

const { TabPane } = Tabs;

export interface OrgUnitEditCreateEditContainerProps {
  orgUnit: RestOrganizationUnit;
  handleOUChanges: (key: orgUnitBasicInfoType, value: any) => void;
  handleCancel: () => void;
  handleSave: (validationFlag?: any, trellisProfile?: any) => void;
}

const OrgUnitEditCreateContainer: React.FC<OrgUnitEditCreateEditContainerProps> = ({
  orgUnit,
  handleCancel,
  handleOUChanges,
  handleSave
}) => {
  const dispatch = useDispatch();
  const location = useLocation();
  const pageState = useSelector(getPageSettingsSelector);
  const entOrgUnits = useHasEntitlements(Entitlement.SETTING_ORG_UNITS);
  const entOrgUnitsCountExceed = useHasEntitlements(Entitlement.SETTING_ORG_UNITS_COUNT_5, EntitlementCheckType.AND);
  const { ou_category_tab, ou_workspace_id, tab } = queryString.parse(location.search);
  const [activeTab, setActiveKey] = useState<OrganizationTabKey>(
    tab ? (tab as OrganizationTabKey) : OrganizationTabKey.BASIC_INFO
  );

  const newTrellisProfile = useHasEntitlements(Entitlement.TRELLIS_BY_JOB_ROLES, EntitlementCheckType.AND);
  const [trellisProfile, setTrellisProfile] = useState<any>(undefined);
  const [showOUAssociationWarningModal, setShowOUAssociationWarningModal] = useState<boolean>(false);
  const [showApplyTrellisChangesToOrg, setShowApplyTrellisChangesToOrg] = useState<boolean>(false);
  const [trellisProfileIsEnabled, setTrellisProfileIsEnabled] = useState<boolean>(false);

  const [createAccess] = useCollectionPermissions();
  const isStandaloneApp = getIsStandaloneApp();
  const projectParams = useParams<ProjectPathProps>();
  const pivotsListState = useParamSelector(getGenericUUIDSelector, {
    uri: "pivots_list",
    method: "list",
    uuid: PIVOT_LIST_ID
  });
  const trellisProfileOu = useParamSelector(getGenericUUIDSelector, {
    uri: "trellis_profile_ou",
    method: "list",
    uuid: "current_ou_profile"
  });
  const trellisProfileCentralOu = useParamSelector(getGenericUUIDSelector, {
    uri: "trellis_profile_ou",
    method: "get",
    uuid: "current_ou_profile"
  });
  const pivotList = useMemo(() => get(pivotsListState, ["data", "records"], []), [pivotsListState]);

  const getPivot: PivotType = useMemo(() => {
    return pivotList.find((pivot: PivotType) => pivot.id === ou_category_tab);
  }, [pivotList, ou_category_tab]);

  const parentNodeRequired = useMemo(
    () => (!orgUnit.id && !orgUnit.parentId) || (!!orgUnit.id && !orgUnit.isParent && !orgUnit.parentId),
    [getPivot, orgUnit]
  );
  const pagePathnameState = useParamSelector(getGenericPageLocationSelector, {
    location: location?.pathname
  });

  useEffect(() => {
    if (newTrellisProfile) {
      if (!orgUnit.id) {
        dispatch(getDevProdCentralProfile({}, "current_ou_profile"));
      } else {
        dispatch(getDevProdParentProfileList({ filter: { ou_ref_ids: [orgUnit.id] } }, "current_ou_profile"));
      }
    }
  }, [orgUnit.id, newTrellisProfile]);

  useEffect(() => {
    const message = validateTrellisProfile(
      trellisProfile,
      orgUnit?.name,
      parentNodeRequired,
      !entOrgUnits || entOrgUnitsCountExceed,
      trellisProfileIsEnabled,
      orgUnit?.validName
    );
    const hasSaveAccess = isStandaloneApp || !!(orgUnit.id ? orgUnit.access_response?.edit : createAccess);
    const tooltip = hasSaveAccess ? message : "You do not have required permission";
    if (pagePathnameState?.action_buttons?.manage_save?.tooltip !== tooltip) {
      const isValid = !message && orgUnit.name && orgUnit.validName && !parentNodeRequired;
      const hasEntitlementAccess = entOrgUnits && !entOrgUnitsCountExceed;
      const settings = {
        title: orgUnit.id ? orgUnit.name || "Edit Collection" : "Create Collection",
        action_buttons: {
          manage_cancel: {
            type: "default",
            label: "Cancel",
            hasClicked: false
          },
          manage_save: {
            type: "primary",
            label: "Save",
            hasClicked: false,
            disabled: !isValid || !hasEntitlementAccess || !hasSaveAccess,
            tooltip
          }
        },
        bread_crumbs: orgUnit?.id
          ? getBreadcumsForEditOUPage(projectParams, ou_workspace_id as string, ou_category_tab as string)
          : getBreadcumsForCreateOUPage(projectParams, ou_workspace_id as string, ou_category_tab as string),
        bread_crumbs_position: "before",
        withBackButton: true,
        showDivider: true,
        headerClassName: `new-profiles-configure-page ${newTrellisProfile ? "new-trelis-container" : ""}`
      };
      dispatch(setPageSettings(location.pathname, settings));
    }
  }, [orgUnit, trellisProfile, parentNodeRequired, pagePathnameState, trellisProfileIsEnabled, newTrellisProfile]);

  useEffect(() => {
    if (orgUnit?.id && newTrellisProfile) {
      setTrellisProfile(cloneDeep(trellisProfileOu?.data?.records?.[0]));
      setTrellisProfileIsEnabled(trellisProfileOu?.data?.records?.[0]?.ou_trellis_enabled_map?.[orgUnit?.id || ""]);
    }
  }, [trellisProfileOu, orgUnit?.id, newTrellisProfile]);

  useEffect(() => {
    return () => {
      dispatch(clearPageSettings(location.pathname));
    };
  }, []);

  useEffect(() => {
    if (!orgUnit?.id && newTrellisProfile) {
      setTrellisProfile(cloneDeep(trellisProfileCentralOu?.data));
      setTrellisProfileIsEnabled(true);
    }
  }, [trellisProfileCentralOu, orgUnit?.id, newTrellisProfile]);

  const handleClickOnSave = (newProfile: any) => {
    newProfile.isTrellisEnabled = trellisProfileIsEnabled;
    // main handle call
    handleSave(undefined, newProfile);
  };
  const handlePopTrellisAssociationPopup = () => {
    if (newTrellisProfile) {
      const data = !orgUnit?.id ? trellisProfileCentralOu?.data : trellisProfileOu?.data?.records?.[0];
      if (isEqual(trellisProfile, data)) {
        trellisProfile.isTrellisEnabled = trellisProfileIsEnabled;
        handleSave(undefined, trellisProfile);
        dispatch(restapiLoading(true, "trellis_profile_ou", "update", "current_ou_profile"));
      } else {
        setShowOUAssociationWarningModal(!showOUAssociationWarningModal);
      }
    } else {
      // older flow
      handleSave();
    }
  };
  useEffect(() => {
    if (pageState && Object.keys(pageState).length > 0) {
      const page = pageState?.[location.pathname];
      if (page && page.hasOwnProperty("action_buttons")) {
        if (page?.action_buttons?.manage_save && page?.action_buttons?.manage_save?.hasClicked === true) {
          dispatch(setPageButtonAction(location.pathname, "manage_save", { hasClicked: false }));
          handlePopTrellisAssociationPopup();
        }
        if (page?.action_buttons?.manage_cancel && page?.action_buttons?.manage_cancel?.hasClicked === true) {
          dispatch(setPageButtonAction(location.pathname, "manage_cancel", { hasClicked: false }));
          handleCancel();
        }
      }
    }
  }, [pageState]);

  const disableDashboard = useMemo(() => {
    if (activeTab === OrganizationTabKey.OU_DASHBOARDS && !orgUnit?._parent_ref_id && !orgUnit?.isParent) {
      return true;
    }
    return false;
  }, [orgUnit, activeTab]);

  const handleChangePathName = (tabKey: OrganizationTabKey) => {
    setActiveKey(tabKey);
  };

  const renderContent = useMemo(() => {
    switch (activeTab) {
      case OrganizationTabKey.BASIC_INFO:
        return (
          <OrgUnitBasicInfoComponent
            draftOrgUnit={orgUnit}
            handleOUChanges={handleOUChanges}
            pivotList={pivotList}
            pivot={getPivot}
            parentNodeRequired={parentNodeRequired}
          />
        );
      case OrganizationTabKey.OU_DASHBOARDS:
        return <OrgUnitDashboardConfigurationContainer orgUnit={orgUnit} handleOUChanges={handleOUChanges} />;
      case OrganizationTabKey.OU_DEFINATIONS:
        return <OrganizationUnitConfigurationContainer draftOrgUnit={orgUnit} handleOrgUnitUpdate={handleOUChanges} />;
      case OrganizationTabKey.CONTRIBUTORS_ROLE_PROFILE:
        return (
          <TrellisJobRoles
            trellisProfile={trellisProfile}
            setTrellisProfile={setTrellisProfile}
            trellisProfileIsEnabled={trellisProfileIsEnabled}
            setTrellisProfileIsEnabled={() => setTrellisProfileIsEnabled(!trellisProfileIsEnabled)}
            orgUnitId={orgUnit?.id}
          />
        );
      default:
        return <></>;
    }
  }, [
    trellisProfileIsEnabled,
    activeTab,
    orgUnit,
    handleOUChanges,
    handleCancel,
    handleSave,
    setTrellisProfile,
    setTrellisProfileIsEnabled,
    pivotList,
    getPivot,
    trellisProfile
  ]);

  const trelisSwitch = useMemo(() => {
    return (
      <div className="flex align-center" style={{ columnGap: "5px" }}>
        <span>
          <Switch
            disabled={activeTab !== OrganizationTabKey.CONTRIBUTORS_ROLE_PROFILE}
            className={`${
              activeTab === OrganizationTabKey.CONTRIBUTORS_ROLE_PROFILE ? "enable-events" : "disable-events"
            }`}
            checked={trellisProfileIsEnabled}
            onChange={() => setTrellisProfileIsEnabled(!trellisProfileIsEnabled)}
          />
        </span>
        <span>{TRELLIS_LABEL}</span>
        <Tooltip title={TRELLIS_TOOLTIP}>
          <Icon type="info-circle" />
        </Tooltip>
      </div>
    );
  }, [trellisProfileIsEnabled, trellisProfile, activeTab, ORG_UNIT_CONFIGURATION_TABS_CONFIG_NEW, OrganizationTabKey]);

  const newTabs = useMemo(() => {
    return (
      <Tabs
        size="default"
        className="new-trelis-tab"
        defaultActiveKey={ORG_UNIT_CONFIGURATION_TABS_CONFIG_NEW?.[0]?.tab_key}
        onChange={(e: any) => setActiveKey(e)}>
        {ORG_UNIT_CONFIGURATION_TABS_CONFIG_NEW.map(tab => (
          <TabPane
            className="org-unit-edit-create-container"
            tab={tab.tab_key === OrganizationTabKey.CONTRIBUTORS_ROLE_PROFILE ? trelisSwitch : tab?.label}
            key={tab.tab_key}>
            <div className="content new-trelis">
              {!disableDashboard && renderContent}{" "}
              {disableDashboard && (
                <div className="user-warning m-t-10">
                  <AntText>Please select the Parent Node from Basic Info</AntText>
                </div>
              )}
            </div>
          </TabPane>
        ))}
      </Tabs>
    );
  }, [
    disableDashboard,
    trellisProfile,
    activeTab,
    renderContent,
    setActiveKey,
    ORG_UNIT_CONFIGURATION_TABS_CONFIG_NEW,
    orgUnit,
    trelisSwitch,
    trellisProfileIsEnabled
  ]);

  const handleApplyToOtherOrg = (orgs: Array<string>) => {
    const newProfile = cloneDeep(trellisProfile);
    newProfile.target_ou_ref_ids = orgs;
    handleClickOnSave(newProfile);
    dispatch(restapiLoading(true, "trellis_profile_ou", "update", "current_ou_profile"));
    setShowApplyTrellisChangesToOrg(!showApplyTrellisChangesToOrg);
  };
  if (newTrellisProfile) {
    return (
      <>
        {newTabs}{" "}
        {showOUAssociationWarningModal && (
          <OUAssociationWarningModal
            visible={showOUAssociationWarningModal}
            handleChangeToOtherOrg={() => {
              setShowOUAssociationWarningModal(!showOUAssociationWarningModal);
              setShowApplyTrellisChangesToOrg(!showApplyTrellisChangesToOrg);
            }}
            handleChangeToCurrentOrg={() => {
              setShowOUAssociationWarningModal(!showOUAssociationWarningModal);
              dispatch(restapiLoading(true, "trellis_profile_ou", "update", "current_ou_profile"));
              handleClickOnSave(cloneDeep(trellisProfile));
            }}
            onClose={() => setShowOUAssociationWarningModal(!showOUAssociationWarningModal)}
          />
        )}
        {showApplyTrellisChangesToOrg && (
          <OUAssociationModal
            handleApplyToOtherOrg={orgs => handleApplyToOtherOrg(orgs)}
            onClose={() => setShowApplyTrellisChangesToOrg(!showApplyTrellisChangesToOrg)}
            visible={showApplyTrellisChangesToOrg}
            ou_workspace_id={ou_workspace_id}
            currentOrgUnit={orgUnit?.id}
          />
        )}
      </>
    );
  } else {
    return (
      <div className="org-unit-edit-create-container">
        <div className="tabs">
          {ORG_UNIT_CONFIGURATION_TABS_CONFIG.map(tab => (
            <div
              className={`tab ${tab.tab_key === activeTab ? "active-tab" : ""}`}
              key={tab.tab_key}
              onClick={(e: any) => handleChangePathName(tab.tab_key)}>
              {tab.label}
            </div>
          ))}
        </div>
        {!disableDashboard && <div className="content">{renderContent}</div>}
        {disableDashboard && (
          <div className="user-warning">
            <AntText>Please select the Parent Node from Basic Info</AntText>
          </div>
        )}
      </div>
    );
  }
};

export default OrgUnitEditCreateContainer;
