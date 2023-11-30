import React, { useCallback, useMemo } from "react";
import { useHistory, useParams } from "react-router-dom";
import { AntButton, AntButtonGroup, AntIcon } from "shared-resources/components";
import { getDashboardsPage } from "constants/routePaths";
import { DashboardSearchInput } from "core/containers/header/dashboard-search-input";
import {
  DASHBOARDS_TITLE,
  DASHBOARD_LIST_COUNT,
  DASHBOARD_SEARCH_PLACEHOLDER,
  DEFAULT_DASHBOARD_KEY,
  MANAGE_DASHBOARDS_BUTTON_TITLE,
  SECURITY
} from "dashboard/constants/constants";
import { DashboardSearchDropdown } from "dashboard/pages/dashboard-drill-down-preview/components/dashboardSearchDropdown";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { get } from "lodash";
import { useDispatch } from "react-redux";
import { dashboardDefault, newDashboardUpdate } from "reduxConfigs/actions/restapi";
import { DashboardHeaderConfigType } from "dashboard/dashboard-types/Dashboard.types";
import { WebRoutes } from "routes/WebRoutes";
import { Dropdown } from "antd";
import ActionButtonMenu from "../dashboard-header/dashboard-actions/dashboard-configure-action-menu/ActionButtonMenu";
import { getAddDashboardActionMenuLabel } from "./helper";
import { setPageButtonAction } from "reduxConfigs/actions/pagesettings.actions";
import { AddDashboardActionMenuType } from "./constant";
import { useHasEntitlements } from "./../../../custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType } from "./../../../custom-hooks/constants";
import { ProjectPathProps } from "classes/routeInterface";
import { getIsStandaloneApp } from "helper/helper";

interface DashboardGeneralActionsHeaderProps {
  dashboardId: string;
  dashboardType: string;
  dashboardName: string;
}

const DashboardGeneralActionsHeader: React.FC<DashboardGeneralActionsHeaderProps> = ({
  dashboardId,
  dashboardType,
  dashboardName
}) => {
  const dashboardsListState = useParamSelector(getGenericRestAPISelector, {
    uri: "dashboards",
    method: "list",
    uuid: DASHBOARD_LIST_COUNT
  });
  const projectParams = useParams<ProjectPathProps>();
  const history = useHistory();
  const dispatch = useDispatch();
  const handleSetDefault = useCallback(
    (id: string) => {
      let dashDefault = false;
      if (dashboardId === id) {
        dashDefault = true;
      }
      dispatch(dashboardDefault(DEFAULT_DASHBOARD_KEY));
      dispatch(newDashboardUpdate(dashboardId, { default: dashDefault }));
    },
    [dashboardId]
  );
  const entDashboard = useHasEntitlements([Entitlement.DASHBOARDS, Entitlement.ALL_FEATURES], EntitlementCheckType.OR);

  const dashboardHeaderConfig: DashboardHeaderConfigType =
    dashboardType === SECURITY
      ? { dashCount: -1, dashboardTitle: dashboardName || DASHBOARDS_TITLE, style: { margin: "2rem 0rem" } }
      : {
          dashCount: get(dashboardsListState, ["data", "_metadata", "total_count"], 0),
          dashboardTitle: DASHBOARDS_TITLE
        };

  const handleManageDashboardButtonClick = useCallback(() => {
    history.push(WebRoutes.dashboard.list(projectParams));
  }, []);

  const addNotesButtonStyle = useMemo(() => ({ width: "170px" }), []);
  return (
    <div
      className={getIsStandaloneApp() ? "dashboard-view-page-header-container__upper" : ""}
      style={dashboardHeaderConfig.style}>
      <DashboardSearchDropdown
        setAsDefault={handleSetDefault}
        dashCount={dashboardHeaderConfig.dashCount}
        history={history}
        dashboardTitle={dashboardHeaderConfig.dashboardTitle}
        currentDashId={dashboardId || ""}
      />
      <div>
        {dashboardType !== SECURITY && (
          <DashboardSearchInput history={history} searchPlaceholder={DASHBOARD_SEARCH_PLACEHOLDER} />
        )}
        {entDashboard && (
          <AntButtonGroup className="ml-5">
            <AntButton
              type="secondary"
              onClick={() => {
                dispatch(setPageButtonAction(history.location.pathname, "create_dashboard", { hasClicked: true }));
                history.push(`${getDashboardsPage(projectParams)}/create`);
              }}>
              <span className="pl-5">Add Insight</span>
            </AntButton>
            {
              <Dropdown
                overlayClassName="dash-action-buttons"
                placement="bottomRight"
                overlayStyle={addNotesButtonStyle}
                overlay={
                  <ActionButtonMenu
                    handleMenuClick={handleManageDashboardButtonClick}
                    menuData={[
                      {
                        key: AddDashboardActionMenuType.MANAGE_DASHBOARD,
                        value: getAddDashboardActionMenuLabel(AddDashboardActionMenuType.MANAGE_DASHBOARD)
                      }
                    ]}
                  />
                }>
                <AntButton icon="down" />
              </Dropdown>
            }
          </AntButtonGroup>
        )}
      </div>
    </div>
  );
};

export default DashboardGeneralActionsHeader;
