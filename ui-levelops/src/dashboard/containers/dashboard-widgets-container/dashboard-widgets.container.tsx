import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Empty } from "antd";
import { useLocation } from "react-router-dom";
import queryString from "query-string";

import Loader from "components/Loader/Loader";
import { WidgetType } from "dashboard/helpers/helper";
import WidgetListContainer from "dashboard/pages/WidgetListContainer";
import { cloneDeep, get } from "lodash";
import {
  dashboardWidgetsSelector,
  isDashboardHasAccessSelector,
  selectedDashboard
} from "reduxConfigs/selectors/dashboardSelector";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { AntCol } from "shared-resources/components";
import "./dashboard-widgets.styles.scss";
import { DashboardWidgetsContainerConstants, WidgetslistContainerKeyType } from "./helper";
import { WidgetFilterContext, widgetOtherKeyDataContext } from "../../pages/context";
import { RBAC } from "constants/localStorageKeys";
import EmptyDashboard from "./empty-dashboard";
import { PERMISSION_MSG, PERMISSION_MSG_OU } from "constants/formWarnings";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { GLOBAL_SETTINGS_UUID } from "dashboard/constants/uuid.constants";
import { configsList } from "reduxConfigs/actions/restapi";
import { useDispatch, useSelector } from "react-redux";
import { sessionUserMangedOURefs } from "reduxConfigs/selectors/session_current_user.selector";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";

interface DashboardWidgetsContainerProps {
  dashboardId: any;
  widgetsLoading: boolean;
  updatedWidgetsMap: any;
  statsWidgetPage: number;
  graphWidgetPage: number;
}

const renderEmptyDashboard = (
  <div className="empty-dashboard">
    <Empty description={DashboardWidgetsContainerConstants.EMPTY_DESCRIPTION} />
  </div>
);

const renderLoader = (
  <div className="loader">
    <Loader />
  </div>
);

const DashboardWidgetsContainer: React.FC<DashboardWidgetsContainerProps> = (props: DashboardWidgetsContainerProps) => {
  const { dashboardId, updatedWidgetsMap, statsWidgetPage, graphWidgetPage, widgetsLoading } = props;

  const [widgetFilters, setWidgetFilters] = useState<any>({});
  const [widgetOtherKeyData, setWidgetOtherKeyData] = useState<any>({});
  const [drilldownIndex, setDrilldownIndex] = useState<number | undefined>(undefined);
  const [drilldownType, setDrilldownType] = useState<WidgetType>(WidgetType.GRAPH);
  const [drilldownWidgetId, setDrilldownWidgetId] = useState<string | undefined>(undefined);
  const dashboard = useSelector(selectedDashboard);
  const widgets = useParamSelector(dashboardWidgetsSelector, {
    dashboard_id: dashboardId
  });
  const location = useLocation();
  const OU = queryString.parse(location.search)?.OU as string;
  const allowedOus: Array<any> = useSelector(sessionUserMangedOURefs);
  const orgUnitEnhancementSupport = useHasEntitlements(Entitlement.ORG_UNIT_ENHANCEMENTS, EntitlementCheckType.AND);
  const newRBACPermission = window.isStandaloneApp ? getRBACPermission(PermeableMetrics.NEW_RBAC_PERMISSION) : true;
  const isNotAllowedOU: boolean = orgUnitEnhancementSupport
    ? allowedOus?.indexOf(Number(OU)) === -1 &&
      allowedOus?.length !== 0 &&
      allowedOus !== undefined &&
      newRBACPermission
    : false; // fallback to exiting logic if Entitlement or user account setting's in OU are not present

  const { view, edit } = useParamSelector(isDashboardHasAccessSelector, dashboard);
  const globalSettingsState = useParamSelector(getGenericRestAPISelector, {
    uri: "configs",
    method: "list",
    uuid: GLOBAL_SETTINGS_UUID
  });
  const dispatch = useDispatch();
  useEffect(() => {
    const data = get(globalSettingsState, "data", {});
    if (Object.keys(data).length <= 0) {
      // id is defined as number in configsList, which shouldn't be the case
      dispatch(configsList({}, GLOBAL_SETTINGS_UUID as any));
    }
  }, []);

  const addNewParent = useCallback(
    (id: string, newParent: any) => {
      const clonnedWidgetFilters = cloneDeep(widgetFilters);
      const parents =
        clonnedWidgetFilters[id] && clonnedWidgetFilters[id].localFilters
          ? clonnedWidgetFilters[id].localFilters.parents
          : [];
      clonnedWidgetFilters[id] = {
        localFilters: {
          parent_cicd_job_ids: [newParent[DashboardWidgetsContainerConstants.CICD_JOB_ID]],
          parents: [...parents, newParent]
        }
      };
      setWidgetFilters(clonnedWidgetFilters);
    },
    [widgetFilters]
  ); // eslint-disable-line react-hooks/exhaustive-deps

  const updateModuleForSalesforceReport = useCallback(
    (id: string, newModulePath: string) => {
      const clonnedWidgetFilters = cloneDeep(widgetFilters);
      clonnedWidgetFilters[id] = {
        localFilters: {
          ...clonnedWidgetFilters?.[id]?.localFilters,
          module: newModulePath
        }
      };
      setWidgetFilters(clonnedWidgetFilters);
    },
    [widgetFilters]
  ); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSetWidgetFilters = useCallback(
    (id: string, newFilters: Object) => {
      setWidgetFilters((prev: any) => {
        return {
          ...(prev || {}),
          [id]: newFilters
        };
      });
    },
    [widgetFilters]
  ); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSetWidgetOtherKeyData = useCallback(
    (id: string, newKey: Object) => {
      setWidgetOtherKeyData((prev: any) => {
        return {
          ...(prev || {}),
          [id]: newKey
        };
      });
    },
    [widgetOtherKeyData]
  ); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSetDrilldownIndex = useCallback((index: number | undefined) => {
    setDrilldownIndex(index);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSetDrilldownType = useCallback((type: WidgetType) => {
    setDrilldownType(type);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSetDrilldownWidgetId = useCallback((id: string | undefined) => {
    setDrilldownWidgetId(id);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const widgetFilterContextValue = useMemo(
    () => ({ filters: widgetFilters, setFilters: handleSetWidgetFilters }),
    [widgetFilters, handleSetWidgetFilters]
  );

  const widgetOtherKeyContextValue = useMemo(
    () => ({ otherKeyData: widgetOtherKeyData, setOtherKey: handleSetWidgetOtherKeyData }),
    [widgetOtherKeyData, handleSetWidgetOtherKeyData]
  );

  const oldAccess = getRBACPermission(PermeableMetrics.WIDGET_LIST_ACTION_BUTTONS);
  const showActionButton = window.isStandaloneApp ? oldAccess : true;

  return (
    <AntCol span={24}>
      {!view && !edit ? (
        <EmptyDashboard message={PERMISSION_MSG.replace("{EMAIL}", dashboard?.metadata?.rbac?.owner)} />
      ) : isNotAllowedOU ? (
        <EmptyDashboard message={PERMISSION_MSG_OU} />
      ) : (
        <>
          {!widgets?.length && renderEmptyDashboard}
          <WidgetFilterContext.Provider value={widgetFilterContextValue}>
            <widgetOtherKeyDataContext.Provider value={widgetOtherKeyContextValue}>
              <div className="dashboard-widgets-container">
                <WidgetListContainer
                  key={WidgetslistContainerKeyType.STATS_LIST}
                  type={WidgetType.STATS}
                  dashboardId={dashboardId}
                  query={dashboard?.query}
                  updatedWidgetsMap={updatedWidgetsMap}
                  addNewParent={addNewParent}
                  widgetPage={statsWidgetPage}
                  drilldownIndex={drilldownIndex}
                  setDrilldownIndex={handleSetDrilldownIndex}
                  drilldownType={drilldownType}
                  setDrilldownType={handleSetDrilldownType}
                  drilldownWidgetId={drilldownWidgetId}
                  setDrilldownWidgetId={handleSetDrilldownWidgetId}
                  showActionButtons={showActionButton}
                />
                <WidgetListContainer
                  key={WidgetslistContainerKeyType.GRAPH_LIST}
                  type={WidgetType.GRAPH}
                  dashboardId={dashboardId}
                  query={dashboard?.query}
                  updatedWidgetsMap={updatedWidgetsMap}
                  addNewParent={addNewParent}
                  updateModuleForSalesforceReport={updateModuleForSalesforceReport}
                  widgetPage={graphWidgetPage}
                  drilldownIndex={drilldownIndex}
                  setDrilldownIndex={handleSetDrilldownIndex}
                  drilldownType={drilldownType}
                  setDrilldownType={handleSetDrilldownType}
                  drilldownWidgetId={drilldownWidgetId}
                  setDrilldownWidgetId={handleSetDrilldownWidgetId}
                  showActionButtons={showActionButton}
                />
                {widgetsLoading && renderLoader}
              </div>
            </widgetOtherKeyDataContext.Provider>
          </WidgetFilterContext.Provider>
        </>
      )}
    </AntCol>
  );
};

export default DashboardWidgetsContainer;
