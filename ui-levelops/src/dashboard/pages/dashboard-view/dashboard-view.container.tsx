import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { filter, get, isEqual } from "lodash";
import { useDispatch, useSelector } from "react-redux";
import { RouteComponentProps, useHistory, useLocation, useParams } from "react-router-dom";
import { notification } from "antd";
import moment from "moment";
import { v1 as uuid } from "uuid";
import Loader from "components/Loader/Loader";
import {
  DASHBOARD_PAGE_SIZE,
  GRAPH_WIDGET_HEIGHT,
  GRAPH_WIDGET_IN_ROW,
  STATS_WIDGET_HEIGHT,
  STATS_WIDGET_IN_ROW
} from "constants/dashboard";
import { COLLECTION_IDENTIFIER, DASHBOARD_ID_KEY } from "constants/localStorageKeys";
import { DashboardSettingsModal } from "dashboard/components";
import { DashboardApplicationFiltersModal } from "dashboard/containers";
import DashboardNotificationComponent from "dashboard/components/dashboard-notification/DashboardNotificationComponent";
import { WidgetType } from "dashboard/helpers/helper";
import { setPageButtonAction } from "reduxConfigs/actions/pagesettings.actions";
import {
  dashboardReportsCreate,
  dashboardReportsUpload,
  dashboardsList,
  restapiClear,
  setSelectedChildId,
  userProfileUpdate,
  setSelectedEntity,
  dashboardsGet
} from "reduxConfigs/actions/restapi";
import {
  _dashboardsUpdateSelector,
  dashboardWidgetsSelector,
  selectedDashboard
} from "reduxConfigs/selectors/dashboardSelector";
import {
  getGenericRestAPISelector,
  getGenericRestAPIStatusSelector,
  useParamSelector
} from "reduxConfigs/selectors/genericRestApiSelector";
import LocalStoreService from "services/localStoreService";
import { AntRow } from "shared-resources/components";
import { generateReport } from "utils/dashboardPdfUtils";
import { DashboardContainer } from "../index";
import "./dashboard-view.container.scss";
import { DASHBOARD_ROUTES, getBaseUrl } from "constants/routePaths";
import { UPLOAD_FAILED } from "constants/formWarnings";
import { DASHBOARD_LIST_COUNT } from "dashboard/constants/constants";
import widgetConstants from "dashboard/constants/widgetConstants";
import DashboardWidgetsContainer from "dashboard/containers/dashboard-widgets-container/dashboard-widgets.container";
import { SelectWidgetToBeCopiedContext, WidgetsLoadingContext, WidgetSvgContext } from "../context";
import { RestDashboard } from "../../../classes/RestDashboards";
import DashboardViewPageHeaderComponent from "dashboard/components/dashboard-view-page-header/DashboardViewPageHeaderComponent";
import { useWindowSize } from "../../../custom-hooks/useWindowSize";
import CopyDestinationDashboardDrawer from "dashboard/copy_destination_dashboard_drawer/CopyDestinationDashboardDrawer";
import { COPY_DESTINATION_DASHBOARD_NODE } from "dashboard/constants/filter-key.mapping";
import { DashboardNotesCreateModal } from "./dashboard-view-modals/dashboard-notes-create.modal";
import { integrationListSelector, _integrationListSelector } from "reduxConfigs/selectors/integrationSelector";
import { GLOBAL_SETTINGS_UUID } from "dashboard/constants/uuid.constants";
import { LEVELOPS_REPORTS } from "../../reports/levelops/constant";
import queryString from "query-string";
import { getDashboardBannerNotification } from "dashboard/components/dashboard-notification/helper";
import { DashboardNotificationType } from "dashboard/components/dashboard-notification/constant";
import { getTenantStateSelector } from "reduxConfigs/selectors/restapiSelector";
import { isSelfOnboardingUser, sessionUserState } from "reduxConfigs/selectors/session_current_user.selector";
import ScmIntegrationSetupSuccessComponent from "configurations/pages/self-onboarding/components/final-step-component/ScmIntegrationSetupSuccessComponent";
import DemoDashboardWrapper from "./Demo-Dashboard/Demo-Dashboard-Header/demo-dashboard-header-wrapper";
import DemoDashboardWidgetsContainer from "./Demo-Dashboard/Demo-Dashboard-Widgets/demo-widgets-container";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { RestUsers } from "classes/RestUsers";
import { getSelectedWorkspace } from "reduxConfigs/selectors/workspace/workspace.selector";
import { sessionCurrentUser } from "reduxConfigs/actions/sessionActions";
import { sanitizeObjectCompletely } from "utils/commonUtils";
import { getCachedIntegrations } from "reduxConfigs/actions/cachedIntegrationActions";
import { cachedIntegrationsListSelector } from "reduxConfigs/selectors/CachedIntegrationSelector";
import { Integration } from "model/entities/Integration";
import { useWorkSpaceList } from "custom-hooks/workspace/useWorkSpaceList";
import { getWorkflowProfileByOuAction } from "reduxConfigs/actions/restapi/workflowProfileByOuAction";
import { hasAccessFromHarness } from "helper/helper";
import { ProjectPathProps } from "classes/routeInterface";

interface DashboardViewContainerProps extends RouteComponentProps {
  statsWidgetPage: number;
  graphWidgetPage: number;
  setStatsWidgetPage: (page: number) => void;
  setGraphWidgetPage: (page: number) => void;
}

const DashboardViewContainer: React.FC<DashboardViewContainerProps> = (props: DashboardViewContainerProps) => {
  const ls = new LocalStoreService();

  const { dashboardId } = props.match.params as any;
  const location = useLocation();
  const history = useHistory();
  const OU = queryString.parse(location.search)?.OU as string;
  const { workspace_id } = queryString.parse(location.search);

  const { statsWidgetPage, graphWidgetPage, setStatsWidgetPage, setGraphWidgetPage } = props;
  const isTrialUser = useSelector(isSelfOnboardingUser);
  const dispatch = useDispatch();
  const dashboard: RestDashboard | null = useSelector(selectedDashboard);
  const tenantState = useSelector(state => getTenantStateSelector(state));
  const demoDashboardIntegrationState = useParamSelector(integrationListSelector, { integration_key: "0" });
  const tenantIntegrations = get(demoDashboardIntegrationState, ["data", "records"], []);
  const sessionCurrentUserData = useSelector(sessionUserState);
  const [width, height] = useWindowSize();
  const [showNotesPopup, setShowNotesPopup] = useState<boolean>(false);
  const integrationIds = useMemo(() => {
    return get(dashboard, ["query", "integration_ids"], []);
  }, [dashboard?.query?.integration_ids]);

  const { workSpaceListData } = useWorkSpaceList();

  const integrationKey = useMemo(
    () => (integrationIds.length ? integrationIds.sort().join("_") : "0"),
    [integrationIds]
  );
  const selectedWorkspace = useSelector(getSelectedWorkspace);
  const projectParams = useParams<ProjectPathProps>();

  const widgets = useParamSelector(dashboardWidgetsSelector, { dashboard_id: dashboardId }).filter((widget: any) => {
    const gType = widget.widget_type;
    const typeInConstant = get(widgetConstants, [widget.type], undefined);

    return gType.includes("composite")
      ? widget.children && widget.children.length > 0
      : widget.type &&
          widget.type !== "" &&
          (typeInConstant !== undefined ||
            [LEVELOPS_REPORTS.TABLE_REPORT, LEVELOPS_REPORTS.TABLE_STAT_REPORT].includes(widget.type));
  });
  const dashboardsListState = useParamSelector(getGenericRestAPISelector, {
    uri: "dashboards",
    method: "list",
    uuid: DASHBOARD_LIST_COUNT
  });

  const dashboardUpdateState = useSelector(_dashboardsUpdateSelector);

  const { loading: update_loading } = get(dashboardUpdateState, [dashboardId], { loading: false, error: false });

  const dashboardStatus = useParamSelector(getGenericRestAPIStatusSelector, {
    uri: "dashboards",
    method: "get",
    uuid: dashboardId
  });

  // const integrationsLoadingState = useSelector(cachedIntegrationsLoadingAndError);
  const integrations = useParamSelector(cachedIntegrationsListSelector, { integration_ids: integrationIds });

  const setHeaderNameRef = useRef<boolean>(false);
  const widgetsSVG = useRef<{ [x: string]: any }>({});
  const reportUploadingId = useRef<string | undefined>(undefined);

  const isWidgetsLoading = useCallback((loadingMap: any) => {
    return Object.values(loadingMap).reduce((carry, loading) => {
      return carry || loading;
    }, false);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const [widgetsLoadingDict, setWidgetsLoadingDict] = useState<any>({});
  const [allWidgetsLoaded, setAllWidgetsLoaded] = useState<boolean>(false);
  const [dashboardLoading, setDashboardLoading] = useState<boolean>(true);
  const [dashboardUpdating, setDashboardUpdating] = useState<boolean>(false);
  const [generatingReport, setGeneratingReport] = useState<boolean>(false);
  const [generatingReportProgress, setGeneratingReportProgress] = useState<boolean>(false);
  const [haveToTakeSnapshot, setHaveToTakeSnapshot] = useState<boolean>(false);
  const [settingsModalVisible, setSettingModalVisible] = useState<boolean>(false);
  const [globalFiltersModalVisible, setGlobalFiltersModalVisible] = useState<boolean>(false);
  const [updatedWidgetsMap, setUpdatedWidgetsMap] = useState<any>({});
  const [copyToWidgetId, setCopyToWidgetId] = useState<string | undefined>(undefined);
  const [loadingIntegrations, setLoadingIntegrations] = useState<boolean>(true);

  const dashboardReportsUploadingState = useParamSelector(getGenericRestAPISelector, {
    uri: "dashboard_reports",
    method: "upload",
    uuid: reportUploadingId.current
  });

  useEffect(() => {
    dispatch(getWorkflowProfileByOuAction(OU));
  }, [OU]);

  const addToLocalStorage = useMemo(
    () => (window.isStandaloneApp ? getRBACPermission(PermeableMetrics.ADD_DASHBOARD_ID_TO_LOCALSTORAGE) : true),
    []
  );

  const statsWidgetCount = useMemo(
    () => filter(widgets || [], (widget: any) => widget.widget_type === WidgetType.STATS).length,
    [dashboard, widgets]
  ); // eslint-disable-line react-hooks/exhaustive-deps

  const graphWidgetCount = useMemo(
    () => filter(widgets || [], (widget: any) => widget.widget_type !== WidgetType.STATS).length,
    [dashboard, widgets]
  ); // eslint-disable-line react-hooks/exhaustive-deps

  const totalWidgetsCount = useMemo(
    () => filter(widgets || [], (widget: any) => widget.hidden !== true).length,
    [dashboard, widgets]
  ); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    setWidgetsLoadingDict({});
    dispatch(dashboardsGet(dashboardId));
    setDashboardLoading(true);
    setHeaderNameRef.current = false;
  }, [dashboardId]);

  useEffect(() => {
    setDashboardUpdating(update_loading);
  }, [update_loading]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (!loadingIntegrations) {
      setLoadingIntegrations(true);
    }
  }, [integrationIds]);

  useEffect(() => {
    const { loading, error } = dashboardStatus || {};
    if (!dashboardLoading && !loading && !error && dashboard?.id) {
      if (loadingIntegrations) {
        dispatch(getCachedIntegrations("list", undefined, integrationIds));
      } else {
        loadingIntegrations && setLoadingIntegrations(false);
      }
    }
  }, [integrationIds, dashboardLoading, dashboardStatus, loadingIntegrations, integrationKey]);

  useEffect(() => {
    const loading = get(sessionCurrentUserData, ["loading"], true);
    const error = get(sessionCurrentUserData, ["error"], true);
    if (!loading && !error) {
      const sessionData = get(sessionCurrentUserData, ["data"], {});
      const last_login_url = location.pathname?.concat(location?.search);
      const previousUrl = get(sessionData, ["metadata", "last_login_url"], "");
      const sessionCurrentData = {
        ...sessionData,
        metadata: {
          ...sessionData?.metadata,
          last_login_url: last_login_url,
          selected_workspace: selectedWorkspace
        }
      };
      const OU = queryString.parse(location.search)?.OU;
      const workspace_id = queryString.parse(location.search)?.workspace_id;
      const isWorkspaceAvailable = workSpaceListData.find(workspace => workspace.id === workspace_id);

      if (
        !isEqual(last_login_url, previousUrl) &&
        OU &&
        Number(workspace_id) &&
        isWorkspaceAvailable &&
        workSpaceListData.length > 0
      ) {
        const restUser = new RestUsers({
          ...sessionData,
          metadata: {
            ...sessionData.metadata,
            last_login_url: last_login_url,
            selected_workspace: selectedWorkspace
          }
        });
        dispatch(sessionCurrentUser({ loading: false, error: false, data: sessionCurrentData }));
        dispatch(userProfileUpdate(restUser));
      }
    }
  }, [dashboardId, OU, location.search, workSpaceListData]);

  const memoizedStyle = useMemo(() => {
    let sideNavWidthOffset = hasAccessFromHarness() ? 352 : 154;
    const containerWidth = width - sideNavWidthOffset;

    if (containerWidth > 480) {
      return { width: containerWidth };
    } else {
      return { width: 480 };
    }
  }, [width]);

  useEffect(() => {
    setDashboardUpdating(update_loading);
  }, [update_loading]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const { loading, error } = dashboardStatus || {};
    if (!dashboardLoading && !loading && !error && dashboard?.id) {
      if (loadingIntegrations) {
        const data: Array<Integration> = integrations;
        if (data?.length) {
          dispatch(
            setSelectedEntity("selected-dashboard-integrations", {
              error: false,
              loading: false,
              loaded: true,
              records: data || []
            })
          );
        } else {
          dispatch(getCachedIntegrations("list", undefined, integrationIds));
        }
      } else {
        loadingIntegrations && setLoadingIntegrations(false);
      }
    }
  }, [integrationIds, dashboardLoading, dashboardStatus, loadingIntegrations, integrationKey]);

  useEffect(() => {
    if (allWidgetsLoaded && generatingReportProgress && !haveToTakeSnapshot) {
      setHaveToTakeSnapshot(true);
    }
  }, [generatingReportProgress, allWidgetsLoaded]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    OU && localStorage.setItem(COLLECTION_IDENTIFIER, OU);
  }, [OU]);
  useEffect(() => {
    dispatch(
      dashboardsList(
        {
          page_size: 1,
          filter: sanitizeObjectCompletely({
            workspace_id:
              !!workspace_id || !!selectedWorkspace?.id ? parseInt(workspace_id ?? selectedWorkspace?.id) : ""
          })
        },
        DASHBOARD_LIST_COUNT
      )
    );
    if ("newrelic" in window) {
      (window as any).newrelic.setCustomAttribute("DashboardId", dashboardId);
    }

    if (addToLocalStorage) {
      ls._setKey(DASHBOARD_ID_KEY, dashboardId);
    }

    return () => {
      dispatch(restapiClear("dashboard_reports", "upload", "-1"));
      dispatch(restapiClear("dashboard_reports", "create", "-1"));
      dispatch(restapiClear("dashboards", "list", DASHBOARD_LIST_COUNT));
      dispatch(setSelectedChildId({}, COPY_DESTINATION_DASHBOARD_NODE));
      dispatch(restapiClear("configs", "list", GLOBAL_SETTINGS_UUID));
      if (addToLocalStorage) {
        ls._setKey(DASHBOARD_ID_KEY, null);
      }
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const loading = get(dashboardStatus, "loading", true);
    const error = get(dashboardStatus, "error", true);
    if (dashboardLoading && dashboardsListState.loading === false && dashboardsListState.error === false) {
      if (!loading && !error && dashboard?.id) {
        if (addToLocalStorage && !dashboard?.public) {
          localStorage.setItem(DASHBOARD_ID_KEY, dashboardId as string);
        }

        const statsRow = Math.ceil(statsWidgetCount / STATS_WIDGET_IN_ROW);
        const defaultStatsRow = Math.ceil(DASHBOARD_PAGE_SIZE / STATS_WIDGET_IN_ROW);
        const defaultGraphRow = Math.ceil(DASHBOARD_PAGE_SIZE / GRAPH_WIDGET_IN_ROW);
        const defaultStatsRowHeight = defaultStatsRow * STATS_WIDGET_HEIGHT;
        const defaultGraphRowHeight = defaultGraphRow * GRAPH_WIDGET_HEIGHT;
        const containerHeight = window.innerHeight - 153;

        let statsHeight = 0;
        let statsPageNumber = 1;
        let graphPageNumber = 0;

        if (statsWidgetCount < DASHBOARD_PAGE_SIZE) {
          statsHeight = statsRow * STATS_WIDGET_HEIGHT;
        } else {
          statsPageNumber = Math.ceil(containerHeight / defaultStatsRowHeight);
          if (statsPageNumber * DASHBOARD_PAGE_SIZE > statsWidgetCount) {
            statsHeight = statsRow * STATS_WIDGET_HEIGHT;
          } else {
            statsHeight = defaultStatsRowHeight * statsPageNumber;
          }
        }

        if (statsHeight < containerHeight) {
          graphPageNumber = Math.ceil((containerHeight - statsHeight) / defaultGraphRowHeight);
        }

        if (!setHeaderNameRef.current) {
          const hasWidgets = widgets && !!widgets.length;

          if (dashboard.type !== "security" && !dashboard?.demo) {
            // don't show error for newly created dashboard
            if (!hasWidgets && moment.unix(dashboard?.created_at).add(1, "m").isBefore(moment())) {
              notification.error({
                message: "Insight does not have any widgets."
              });
            }
          }

          setHeaderNameRef.current = true;
          setDashboardLoading(false);
          setGraphWidgetPage(graphPageNumber);
          setStatsWidgetPage(statsPageNumber);
        }
      }

      if (error !== undefined && error === true && loading !== undefined && loading === false) {
        notification.error({
          message: "Requested Insight doesn't exist"
        });
        //if requested dashboard not found, redirect to home page instead of dashboardlist page
        const error_code = get(dashboardStatus, "error_code", 0);
        if (error_code === 404) {
          props.history.push(getBaseUrl());
        } else {
          props.history.push(`${getBaseUrl(projectParams)}${DASHBOARD_ROUTES._ROOT}`);
        }
      }
    }
  }, [dashboardStatus, dashboardsListState, dashboard, integrationKey, integrationIds]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (reportUploadingId.current) {
      const loading = get(dashboardReportsUploadingState, ["loading"], true);
      const error = get(dashboardReportsUploadingState, ["error"], true);

      if (!loading) {
        if (error) {
          notification.error({
            message: UPLOAD_FAILED
          });
        } else {
          const fileId = get(dashboardReportsUploadingState, ["data", "id"], "");
          if (fileId) {
            const reportObj = {
              dashboard_id: dashboardId,
              file_id: fileId,
              name: `${dashboard?.name} Report`,
              created_by: ls.getUserId() || ""
            };
            dispatch(dashboardReportsCreate(reportObj));
          }
        }
        reportUploadingId.current = undefined;
      }
    }
  }, [dashboardReportsUploadingState]); // eslint-disable-line react-hooks/exhaustive-deps

  const setWidgetSvg = useCallback(
    (id: string, svg: Object) => {
      if (generatingReport) {
        return;
      }
      widgetsSVG.current[id] = svg;
      if (Object.keys(widgetsSVG.current).length === totalWidgetsCount && haveToTakeSnapshot) {
        setGeneratingReport(true);
        if (!reportUploadingId.current) generatePDF();
      }
    },
    [generatingReport, widgetsSVG.current, totalWidgetsCount, haveToTakeSnapshot]
  ); // eslint-disable-line react-hooks/exhaustive-deps

  const generatePDF = useCallback(async () => {
    try {
      const blob = await generateReport(dashboard, widgets, widgetsSVG.current);
      const reportName = `${dashboard!.name} Report.pdf`;
      reportUploadingId.current = uuid();
      setGeneratingReport(false);
      setHaveToTakeSnapshot(false);
      setGeneratingReportProgress(false);
      widgetsSVG.current = {};
      dispatch(
        dashboardReportsUpload(blob, reportUploadingId.current, {
          dashboard_id: dashboard!.id,
          report_name: reportName
        })
      );
    } catch (e) {
      throw e;
    } finally {
      dispatch(
        setPageButtonAction(props.location.pathname, "print", {
          hasClicked: false,
          disabled: false,
          showProgress: false
        })
      );
    }
  }, [dashboard, widgetsSVG.current, props.location]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleExport = useCallback(() => {
    if (Object.keys(widgetsLoadingDict).length === totalWidgetsCount && allWidgetsLoaded) {
      // All widgets loaded!
      setHaveToTakeSnapshot(true);
      setGeneratingReportProgress(true);
      return;
    }
    setAllWidgetsLoaded(false);
    setGeneratingReportProgress(true);
    setStatsWidgetPage(statsWidgetCount);
    setGraphWidgetPage(graphWidgetCount);
  }, [graphWidgetCount, statsWidgetCount, allWidgetsLoaded, widgetsLoadingDict]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSettingsModalToggle = useCallback((visibility: boolean = false) => {
    setSettingModalVisible(visibility);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleNoteModalToggle = useCallback((visibility: boolean = false) => {
    setShowNotesPopup(visibility);
  }, []);

  const renderDashboardSettingsModal = useMemo(() => {
    return (
      <DashboardSettingsModal
        dashboardId={dashboardId || ""}
        visible={settingsModalVisible}
        toggleModal={handleSettingsModalToggle}
      />
    );
  }, [dashboardId, settingsModalVisible]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleGlobalFiltersModalToggle = useCallback((visibility: boolean = false) => {
    setGlobalFiltersModalVisible(visibility);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const dashboardBannerNotification = useMemo(() => {
    return getDashboardBannerNotification(
      DashboardNotificationType.ONBOARDING,
      dashboard,
      isTrialUser,
      tenantState,
      tenantIntegrations
    );
  }, [dashboard, tenantIntegrations]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleCloseCopyToWidgetDrawer = () => {
    setCopyToWidgetId(undefined);
  };
  const handleSetUpdatedWidgetsMap = useCallback((newUpdatedWidgetsMap: any = {}) => {
    setUpdatedWidgetsMap((prev: any) => {
      return {
        ...(prev || {}),
        ...newUpdatedWidgetsMap
      };
    });
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const renderGlobalFiltersModal = useMemo(() => {
    return (
      <DashboardApplicationFiltersModal
        dashboardId={dashboardId}
        toggleApplicationFilters={handleGlobalFiltersModalToggle}
        showApplicationModal={globalFiltersModalVisible}
      />
    );
  }, [dashboardId, globalFiltersModalVisible]); // eslint-disable-line react-hooks/exhaustive-deps

  const renderCopyDestinationDashboardsDrawer = useMemo(() => {
    return (
      <CopyDestinationDashboardDrawer
        showCopyDestinationDashboardsDrawer={copyToWidgetId}
        handleCopyDestinationDashboardsDrawer={handleCloseCopyToWidgetDrawer}
      />
    );
  }, [copyToWidgetId]);

  const memoizedGutter = useMemo(() => [0, 0], []); // eslint-disable-line react-hooks/exhaustive-deps

  const widgetSvgContextValue = useMemo(() => {
    return {
      haveToTakeSnapshot: haveToTakeSnapshot && allWidgetsLoaded,
      setSvg: setWidgetSvg
    };
  }, [haveToTakeSnapshot, setWidgetSvg, allWidgetsLoaded]);

  const widgetLoadingContextValue = useMemo(
    () => ({
      setWidgetsLoading: (widgetId: string, loading: boolean) => {
        if (widgetsLoadingDict[widgetId] === loading) {
          return;
        }
        widgetsLoadingDict[widgetId] = loading;
        setWidgetsLoadingDict({
          ...widgetsLoadingDict
        });
        const widgetsLoaded =
          !isWidgetsLoading(widgetsLoadingDict) && Object.keys(widgetsLoadingDict).length === totalWidgetsCount;
        setAllWidgetsLoaded(widgetsLoaded);
      },
      widgetsLoadingDict
    }),
    [totalWidgetsCount, widgetsLoadingDict]
  );

  const widgetToBeCopiedContextValue = useMemo(
    () => ({
      setWidgetToBeCopied: (widgetId: string) => {
        setCopyToWidgetId(widgetId);
      }
    }),
    []
  );

  const handleAddDashboard = useCallback(
    () => history.push(`${getBaseUrl(projectParams)}${DASHBOARD_ROUTES.CREATE}`),
    []
  );

  const dashboardNotesModal = useMemo(() => {
    return <DashboardNotesCreateModal visible={showNotesPopup} onClose={() => setShowNotesPopup(false)} />;
  }, [showNotesPopup]);

  const widgetsLoading = isWidgetsLoading(widgetsLoadingDict) as boolean;

  if (dashboardLoading) return <Loader />;

  if (dashboard?.demo) {
    return (
      <AntRow gutter={memoizedGutter} className="dashboard-view-container" style={memoizedStyle}>
        <AntRow gutter={memoizedGutter} className="dashboard-view-header-container" style={memoizedStyle}>
          <DashboardNotificationComponent data={dashboardBannerNotification} />
          <DemoDashboardWrapper queryparamOU={OU} dashboardName={dashboard?.name ?? ""} dashboardId={dashboardId} />
        </AntRow>
        <DemoDashboardWidgetsContainer dashboardId={dashboardId} dashboard={dashboard} />
      </AntRow>
    );
  }

  // Handling Security Dashboard.
  if (dashboard!.type === "security")
    return (
      <DashboardContainer securityDash={true}>
        <DashboardViewPageHeaderComponent
          dashboardId={dashboardId}
          widgetsLoading={widgetsLoading}
          generatingReportProgress={generatingReportProgress}
          handleExport={handleExport}
          openSettings={handleSettingsModalToggle}
          handleShowApplicationFilterModal={handleGlobalFiltersModalToggle}
          queryparamOU={OU}
        />
      </DashboardContainer>
    );

  return (
    <>
      <AntRow gutter={memoizedGutter} className="dashboard-view-container" style={memoizedStyle}>
        <AntRow gutter={memoizedGutter} className="dashboard-view-header-container" style={memoizedStyle}>
          <DashboardNotificationComponent data={dashboardBannerNotification} />
          <DashboardViewPageHeaderComponent
            dashboardId={dashboardId}
            widgetsLoading={widgetsLoading}
            generatingReportProgress={generatingReportProgress}
            handleExport={handleExport}
            openSettings={handleSettingsModalToggle}
            openNotesModal={handleNoteModalToggle}
            handleShowApplicationFilterModal={handleGlobalFiltersModalToggle}
            queryparamOU={OU}
          />
        </AntRow>
        <SelectWidgetToBeCopiedContext.Provider value={widgetToBeCopiedContextValue}>
          <WidgetSvgContext.Provider value={widgetSvgContextValue}>
            <WidgetsLoadingContext.Provider value={widgetLoadingContextValue}>
              <DashboardWidgetsContainer
                dashboardId={dashboardId}
                widgetsLoading={widgetsLoading}
                updatedWidgetsMap={updatedWidgetsMap}
                statsWidgetPage={statsWidgetPage}
                graphWidgetPage={graphWidgetPage}
              />
            </WidgetsLoadingContext.Provider>
          </WidgetSvgContext.Provider>
        </SelectWidgetToBeCopiedContext.Provider>
      </AntRow>
      {copyToWidgetId && renderCopyDestinationDashboardsDrawer}
      {renderGlobalFiltersModal}
      {settingsModalVisible && renderDashboardSettingsModal}
      {dashboardNotesModal}
      {<ScmIntegrationSetupSuccessComponent />}
      {/** uncomment this to revisit the dora metric modal */}
      {/* {<DefinationsConfigurationModalContainer />}*/}
    </>
  );
};

export default DashboardViewContainer;
