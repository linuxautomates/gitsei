import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useSelector, useDispatch } from "react-redux";
import { RouteComponentProps, withRouter, useHistory, useLocation, useParams } from "react-router-dom";
import { get } from "lodash";
import queryString from "query-string";
import "./DashboardHeader.scss";
import { DashboardActionMenuType, filterCount } from "./helper";
import DashboardActionButtons from "./dashboard-actions/DashboardActionButtons";
import {
  dashboardWidgetsSelector,
  isDashboardHasAccessSelector,
  selectedDashboard,
  _dashboardsListSelector
} from "reduxConfigs/selectors/dashboardSelector";
import LocalStoreService from "services/localStoreService";
import { getApplicationFilters, sanitizeGlobalMetaDataFilters } from "../dashboard-application-filters/helper";
import { WebRoutes } from "../../../routes/WebRoutes";
import { AntCol, AntRow, AntButton, AntBadge, Banner } from "shared-resources/components";
import FilterButton from "./filter-button/FilterButton";
import { DASHBOARDS_TITLE, DASHBOARD_LIST_COUNT, DEFAULT_DASHBOARD_KEY, SECURITY } from "dashboard/constants/constants";
import widgetConstants from "dashboard/constants/widgetConstants";
import { NO_LONGER_SUPPORTED_FILTER } from "dashboard/constants/applications/names";
import { dashboardDefault, newDashboardUpdate } from "reduxConfigs/actions/restapi";
import { DashboardHeaderConfigType, Metadata } from "dashboard/dashboard-types/Dashboard.types";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getGenericRestAPISelector, getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { isSelfOnboardingUser } from "reduxConfigs/selectors/session_current_user.selector";
import { INTEGRATION_MONITORING_KEY } from "../dashboard-settings-modal/constant";
import IntegrationMonitoringComponent from "../dashboard-view-page-secondary-header/integration-monitoring/IntegrationMonitoringComponent";
import { DashboardDatePicker } from "./DashboardDatePicker";
import { DROPDOWN_DASH, PREDEFINED_DASHBOARDS } from "configurations/pages/Organization/Constants";
import { OUDashboardList } from "configurations/configuration-types/OUTypes";
import { DashboradHeaderSearch } from "./DashboradHeaderSearch";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import DashboardCustomOU from "./DashboardCustomOU/dashboard-custom-ou-wrapper";
import { getSelectedOU } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { isDashboardTimerangeEnabled } from "helper/dashboard.helper";
import { ProjectPathProps } from "classes/routeInterface";
import { useDashboardPermissions } from "custom-hooks/HarnessPermissions/useDashboardPermissions";
import { getGenericPageLocationSelector } from "reduxConfigs/selectors/pagesettings.selector";
import { compareCurrentDate } from "utils/dateUtils";

interface DashboardHeaderProps extends RouteComponentProps {
  dashboardId: any;
  widgetsLoading: boolean;
  generatingReportProgress: boolean;
  handleExport: () => void;
  openSettings: (param: boolean) => void;
  handleShowApplicationFilterModal: (param?: boolean) => void;
  openNotesModal?: (visibility: boolean) => void;
  isParameterizedHeader: boolean;
  onFilerValueChange: (value: any, key: string) => void;
  queryparamOU?: string | undefined;
  metaData: Metadata;
  popOverVisible: boolean;
  setPopOverVisible: (value: boolean) => void;
}

const DashboardHeader: React.FC<DashboardHeaderProps> = (props: DashboardHeaderProps) => {
  const ls = new LocalStoreService();
  const {
    dashboardId,
    widgetsLoading,
    generatingReportProgress,
    handleExport,
    handleShowApplicationFilterModal,
    openSettings,
    openNotesModal,
    isParameterizedHeader,
    onFilerValueChange,
    queryparamOU
  } = props;

  const history = useHistory();
  const location = useLocation();
  const dispatch = useDispatch();
  const dashboardListState = useSelector(_dashboardsListSelector);
  const [isOpen, setIsOpen] = useState(false);
  const [reload, setReload] = useState(false);
  const [selectedOU, setSelectedOU] = useState<any>({});
  const projectParams = useParams<ProjectPathProps>();
  const dashboardsListState = useParamSelector(getGenericRestAPISelector, {
    uri: "dashboards",
    method: "list",
    uuid: DASHBOARD_LIST_COUNT
  });
  const pagePathnameBannerState = useParamSelector(getGenericPageLocationSelector, {
    location: `${location?.pathname}/banner`
  });
  const dashboard = useSelector(selectedDashboard);
  const OU = queryString.parse(location.search).OU as string;
  const { _metadata } = dashboard || {};
  const [showFiltersDropDown, setShowFiltersDropDown] = useState<boolean>(false);
  const integrationIds = get(dashboard, ["_query", "integration_ids"], []);
  const isTrialUser = useSelector(isSelfOnboardingUser);
  const showIntegrationMonitor = get(_metadata, [INTEGRATION_MONITORING_KEY], false);
  const userRole = useMemo(() => ls.getUserRbac()?.toString()?.toLowerCase() || "", []); // eslint-disable-line react-hooks/exhaustive-deps
  const widgets = useParamSelector(dashboardWidgetsSelector, {
    dashboard_id: dashboardId
  });
  const { edit } = useParamSelector(isDashboardHasAccessSelector, dashboard);
  const selectedOUState = useSelector(getSelectedOU);
  const filters = useMemo(() => {
    const allApplication = getApplicationFilters();
    const allApplicationKeys = Object.keys(allApplication);
    let globalFilters = get(dashboard, ["_metadata", "global_filters"], {});
    allApplicationKeys.forEach((item: any) => {
      const report = allApplication[item].report;
      const removeNoLongerSupportedFilter = get(widgetConstants, [report, NO_LONGER_SUPPORTED_FILTER], undefined);
      if (removeNoLongerSupportedFilter) {
        globalFilters = {
          ...globalFilters,
          [item]: removeNoLongerSupportedFilter(globalFilters[item])
        };
      }
    });
    return {
      jiraOrFilters: get(dashboard, ["_metadata", "jira_or_query"], {}),
      globalFilters: globalFilters
    };
  }, [dashboard]);

  useEffect(() => {
    const { loading: _loading, error } = get(dashboardListState, [DROPDOWN_DASH], { loading: true, error: true });
    if (_loading !== undefined && !_loading && error !== undefined && !error) {
      const list = get(dashboardListState, [DROPDOWN_DASH, "data", "records"], []);
      const allTopLevelDashboards: Array<OUDashboardList> = [];
      list.forEach((record: OUDashboardList) => {
        const name = record?.name?.toString().toLowerCase();
        if (PREDEFINED_DASHBOARDS.some(str => str.toLowerCase() === name)) {
          allTopLevelDashboards.push(record);
        }
      });
    }
  }, [dashboardListState]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (queryString.parse(window.location.href.split("?")[1]).editDashboardFilters) {
      handleShowApplicationFilterModal(true);
    }
  }, []);

  const tenant = useMemo(() => ls.getUserCompany(), []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleMenuClick = useCallback(
    (e: any) => {
      switch (e.key) {
        case DashboardActionMenuType.MODIFY_LAYOUT:
          props.history.push({
            pathname: WebRoutes.dashboard.widgets.widgetsRearrange(projectParams, dashboardId),
            search: location?.search
          });
          break;
        case DashboardActionMenuType.CONFIGURE_COLOR_SCHEME:
          props.history.push(WebRoutes.settings.global_settings("scrollTo=DASHBOARD_COLOR_SCHEME"));
          break;
        case DashboardActionMenuType.MANAGE_SETTINGS:
          openSettings(true);
          break;
        case DashboardActionMenuType.EXPORT:
          handleExport();
          break;
        case DashboardActionMenuType.ADD_NOTES:
          openNotesModal && openNotesModal(true);
          break;
        case DashboardActionMenuType.ADD_WIDGET:
          props.history.push({
            pathname: WebRoutes.dashboard.widgets.widgetsExplorer(projectParams, dashboard?.id),
            search: location?.search
          });
          break;
      }
    },
    [dashboardId, props.history, handleExport, dashboard, location?.search]
  ); // eslint-disable-line react-hooks/exhaustive-deps

  const handleGlobalFilterButtonClick = useCallback(() => {
    handleShowApplicationFilterModal(true);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const getFiltersCount = useMemo(() => {
    let count = 0;
    const allApplication = getApplicationFilters();
    const allApplicationKeys = Object.keys(allApplication);
    let _global_filters = sanitizeGlobalMetaDataFilters(dashboard?.global_filters);
    allApplicationKeys.forEach((item: any) => {
      const report = allApplication[item].report;
      const removeNoLongerSupportedFilter = get(widgetConstants, [report, NO_LONGER_SUPPORTED_FILTER], undefined);
      if (removeNoLongerSupportedFilter) {
        _global_filters = {
          ..._global_filters,
          [item]: removeNoLongerSupportedFilter(_global_filters[item])
        };
      }
    });
    let all_global_filters = Object.keys(getApplicationFilters());
    for (const [key, value] of Object.entries(_global_filters)) {
      // Added this logic for some already set invalid URI in filters
      if (all_global_filters.includes(key)) {
        // @ts-ignore
        count = count + filterCount(value);
      }
    }
    if (dashboard && Object.keys(dashboard?.jira_or_query || {}).length) {
      count = count + filterCount(dashboard.jira_or_query);
    }
    return count;
  }, [dashboard]);

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
  const handleClickChange = (visible: boolean) => {
    setIsOpen(visible);
  };
  const handleOUDashboardTour = useCallback(() => {
    setIsOpen(true);
  }, []);
  const handleClose = () => {
    setIsOpen(false);
  };
  const dashboardHeaderConfig: DashboardHeaderConfigType =
    dashboard?.type === SECURITY
      ? { dashCount: -1, dashboardTitle: dashboard?.name || DASHBOARDS_TITLE, style: { margin: "2rem 0rem" } }
      : {
          dashCount: get(dashboardsListState, ["data", "_metadata", "total_count"], 0),
          dashboardTitle: dashboard?.name
        };

  const oldAccess = getRBACPermission(PermeableMetrics.ADMIN_WIDGET_EXTRAS);
  const permissions = useDashboardPermissions();
  const hasEditAccess = window.isStandaloneApp ? oldAccess : permissions[1];

  const actions = useMemo(() => {
    return (
      <AntCol>
        <div className="flex justify-end">
          {(window.isStandaloneApp ? getRBACPermission(PermeableMetrics.DASHBOARD_HEADER_ACTIONS) : permissions[2]) && (
            <>
              <FilterButton
                handleGlobalFilterButtonClick={handleGlobalFilterButtonClick}
                setShowFiltersDropDown={setShowFiltersDropDown}
                showFiltersDropDown={showFiltersDropDown}
                integrationIds={integrationIds}
                filters={filters}
                filtersCount={getFiltersCount}
                disableActions={!hasEditAccess || !edit}
              />
              {showIntegrationMonitor && (
                <AntButton className="integration-status">
                  <IntegrationMonitoringComponent dashboard={dashboard as any} />
                  <AntBadge count={"!"} onClick />
                </AntButton>
              )}
              <DashboardActionButtons
                disableActions={generatingReportProgress || widgetsLoading || !edit}
                handleMenuClick={handleMenuClick}
                showDropdown={!!openNotesModal}
                widgetsCount={widgets.length}
              />
            </>
          )}
        </div>
      </AntCol>
    );
  }, [filters, showFiltersDropDown, generatingReportProgress || widgetsLoading || !edit, location.search]);

  const datePicker = useMemo(() => {
    if (!isDashboardTimerangeEnabled(props.metaData || {})) {
      return null;
    }
    return (
      <DashboardDatePicker
        popOverVisible={props.popOverVisible}
        metaData={props.metaData}
        onFilerValueChange={onFilerValueChange}
        setPopOverVisible={props.setPopOverVisible}
      />
    );
  }, [props.popOverVisible, props.metaData]);

  const getOUId = useCallback(() => {
    return selectedOUState?.ou_id;
  }, [selectedOUState]);
  
  const showBanner = useMemo(() => {
    return !pagePathnameBannerState?.hide && compareCurrentDate("12-31-2024");
  }, [pagePathnameBannerState]);

  return (
    <>
      {showBanner && <Banner />}

      <AntRow>{queryparamOU && <DashboardCustomOU dashboardId={dashboardId}></DashboardCustomOU>}</AntRow>
      <div className="dashboard-header-search-container">
        <div className="dashboard-search-div">
          <div className="dashboard-row">
            <DashboradHeaderSearch ouId={getOUId()} />
          </div>
        </div>
        <div className="flex align-center header-buttons">
          {datePicker}
          {actions}
        </div>
      </div>
    </>
  );
};

export default withRouter(DashboardHeader);
