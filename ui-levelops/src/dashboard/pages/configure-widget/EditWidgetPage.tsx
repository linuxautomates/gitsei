import React, { useCallback, useContext, useEffect, useState, useMemo } from "react";
import { RouteComponentProps, useHistory, useLocation, useParams } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { notification } from "antd";
import ConfigureWidgetWrapper from "./ConfigureWidgetWrapper";
import { RestWidget } from "../../../classes/RestDashboards";
import { useHeader } from "../../../custom-hooks/useHeader";
import { generateConfigureWidgetPageBreadcrumbs } from "../../../utils/dashboardUtils";
import { DashboardWidgetResolverContext } from "../context";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { dashboardsUpdateSelector, getDashboard, selectedDashboard } from "reduxConfigs/selectors/dashboardSelector";
import { WebRoutes } from "../../../routes/WebRoutes";
import { getWidget } from "reduxConfigs/selectors/widgetSelector";
import { newDashboardUpdate, restapiClear, setSelectedChildId } from "reduxConfigs/actions/restapi";
import { makeWidgetsReversible, widgetDelete, widgetUpdate } from "reduxConfigs/actions/restapi/widgetActions";
import { resetWidgetLibraryState } from "reduxConfigs/actions/widgetLibraryActions";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { getWidgetUri } from "shared-resources/containers/widget-api-wrapper/helper";
import { getRequiredField } from "reduxConfigs/selectors/requiredFieldSelector";
import { emitEvent } from "dataTracking/google-analytics";
import { AnalyticsCategoryType, WidgetFiltersActions } from "dataTracking/analytics.constants";
import widgetConstants from "dashboard/constants/widgetConstants";
import { get } from "lodash";
import { cachedIntegrationsListSelector } from "reduxConfigs/selectors/CachedIntegrationSelector";
import { workflowProfileDetailSelector } from "reduxConfigs/selectors/workflowProfileByOuSelector";
import queryString from "query-string";
import { updateWidgetFiltersForReport } from "utils/widgetUtils";
import { DashboardPathProps, ProjectPathProps } from "classes/routeInterface";
import { DORA_REPORTS } from "dashboard/constants/applications/names";
import { WorkflowIntegrationType } from "classes/RestWorkflowProfile";
import { CALCULATION_RELEASED_IN_KEY } from "configurations/pages/lead-time-profiles/containers/workflowDetails/components/constant";
import { IntegrationTypes } from "constants/IntegrationTypes";

interface EditWidgetPageProps {}

const CANCEL_ACTION_KEY = "action_cancel";
const DELETE_ACTION_KEY = "action_delete";
const SAVE_ACTION_KEY = "action_next";

const ACTION_BUTTONS = {
  [CANCEL_ACTION_KEY]: {
    type: "ghost",
    label: "Cancel",
    hasClicked: false
  },
  [DELETE_ACTION_KEY]: {
    type: "ghost",
    label: "Delete",
    hasClicked: false,
    progressLabel: "Deleting..."
  },
  [SAVE_ACTION_KEY]: {
    type: "primary",
    label: "Save widget",
    hasClicked: false,
    disabled: false,
    showProgress: false,
    progressLabel: "Saving...",
    tooltip: ""
  }
};

const EditWidgetPage: React.FC<EditWidgetPageProps> = () => {
  const [savingWidget, setSavingWidget] = useState(false);
  const [initialFilters, setInitialFilters] = useState({});

  const location = useLocation();
  const history = useHistory();
  const projectParams = useParams<ProjectPathProps>();

  const { dashboardId } = useContext(DashboardWidgetResolverContext);
  const dashboardParams = useParams<DashboardPathProps>();
  const { widgetId = "" } = dashboardParams;

  const dispatch = useDispatch();

  const dashboard = useParamSelector(getDashboard, { dashboard_id: dashboardId });
  const selecteddashboard = useSelector(selectedDashboard);
  const widget: RestWidget = useParamSelector(getWidget, { widget_id: widgetId });

  const integrationIds = useMemo(() => {
    return get(dashboard, ["query", "integration_ids"], []);
  }, [dashboard?.query]);
  const integrations = useParamSelector(cachedIntegrationsListSelector, { integration_ids: integrationIds });

  const queryParamOU = queryString.parse(location.search).OU as string;
  const workspaceOuProfilestate = useParamSelector(workflowProfileDetailSelector, { queryParamOU });

  const requiredField = useSelector(getRequiredField);
  const { loading: dashboardUpdating } = useParamSelector(dashboardsUpdateSelector, { dashboard_id: dashboardId });
  const { setupHeader, changeButtonState, onActionButtonClick } = useHeader(location.pathname);

  const handleSetHeader = useCallback(
    (
      dashboardId: string,
      dashboardName: string,
      widgetId: string,
      widgetName: string,
      validWidget: boolean,
      errorMessage: string
    ) => {
      ACTION_BUTTONS[SAVE_ACTION_KEY].disabled = !validWidget;
      ACTION_BUTTONS[SAVE_ACTION_KEY].tooltip = errorMessage ? errorMessage : "";
      const pageSetting = {
        title: "Edit Widget",
        action_buttons: ACTION_BUTTONS,
        bread_crumbs: generateConfigureWidgetPageBreadcrumbs(
          projectParams,
          dashboardId,
          dashboardName,
          widgetId,
          widgetName,
          location?.search
        ),
        showDivider: true,
        withBackButton: true,
        goBack: true
      };
      setupHeader(pageSetting);
    },
    []
  );

  const getDynamicURI = useCallback(() => {
    return getWidgetUri(widget?.type, getWidgetConstant(widget?.type, "uri"), widget?.query, widget?.metadata);
  }, [widget]);

  const redirectToDashboard = () =>
    history.replace({
      // @ts-ignore
      pathname: WebRoutes.dashboard.details(undefined, dashboardId),
      search: location?.search || undefined
    });

  useEffect(() => {
    setInitialFilters(widget?.metadata?.filter_tab_order || {});
    dispatch(makeWidgetsReversible([widgetId]));
    return () => {
      dispatch(resetWidgetLibraryState());
      //unsetting the previous widget while unmounting
      dispatch(setSelectedChildId({}));
    };
  }, []);

  useEffect(() => {
    if (widget?.hasError) {
      notification.error({ message: widget.errorMessage });
    }
    if (widget?.deleted) {
      redirectToDashboard();
    }
  }, [widget]);

  useEffect(() => {
    if (dashboardUpdating) {
      return;
    }
    if (savingWidget) {
      setSavingWidget(false);
      changeButtonState(SAVE_ACTION_KEY, {
        showProgress: false,
        disabled: false
      });
      // GA event WIDGET_FILTERS
      const initialFilterNames = Object.keys(initialFilters || {}) || [];
      const currentFilterNames = Object.keys(widget.metadata?.filter_tab_order || {}) || [];
      // tracking added filters
      currentFilterNames.length &&
        currentFilterNames.forEach(currentName => {
          if (!initialFilterNames.includes(currentName)) {
            emitEvent(
              AnalyticsCategoryType.WIDGET_FILTERS,
              WidgetFiltersActions.FILTER_ADDED,
              `${currentName}:${widget?.name || ""}`
            );
          }
        });
      // tracking deleted filters
      initialFilterNames.length &&
        initialFilterNames.forEach(initName => {
          if (!currentFilterNames.includes(initName)) {
            emitEvent(
              AnalyticsCategoryType.WIDGET_FILTERS,
              WidgetFiltersActions.FILTER_DELETED,
              `${initName}:${widget?.name || ""}`
            );
          }
        });

      redirectToDashboard();
    }
  }, [dashboardUpdating, changeButtonState]);

  useEffect(() => {
    onActionButtonClick((action: string) => {
      switch (action) {
        case CANCEL_ACTION_KEY:
          dispatch(restapiClear(getDynamicURI(), getWidgetConstant(widget?.type, "method"), `${widgetId}-preview`));
          history.goBack();
          return {
            hasClicked: false
          };
        case SAVE_ACTION_KEY:
          if (requiredField?.is_required_error_field) {
            notification.error({ message: requiredField.required_field_msg });
            return {
              hasClicked: false,
              disabled: false,
              showProgress: false
            };
          } else {
            const _dashboard = { ...dashboard?.json, query: { ...(selecteddashboard?.query || {}) } };
            setSavingWidget(true);
            dispatch(newDashboardUpdate(dashboardId, _dashboard, false, true));
            return {
              hasClicked: false,
              disabled: true,
              showProgress: true
            };
          }

        case DELETE_ACTION_KEY:
          dispatch(widgetDelete(widgetId));
          return {
            hasClicked: false,
            disabled: true,
            showProgress: true
          };
        default:
          return null;
      }
    });
  }, [onActionButtonClick]);

  useEffect(() => {
    const getReportCategoryName = get(widgetConstants, [widget?.type as string, "category"], undefined);
    if (getReportCategoryName === "dora") {
      const getdoraProfileType = get(
        widgetConstants,
        [widget?.type as string, "getDoraProfileIntegrationType"],
        undefined
      );
      let currentIntegrationType;
      if (getdoraProfileType) {
        currentIntegrationType = getdoraProfileType({
          integrations,
          workspaceOuProfilestate
        });
      }
      if (currentIntegrationType !== widget?.metadata.integration_type) {
        delete widget?.query["across"];
        if (Object.keys(widget?.query).length > 0) {
          widget.query = {};
          widget.metadata = {
            ...(widget.metadata || {}),
            integration_type: currentIntegrationType
          };
          const updatedWidget = updateWidgetFiltersForReport(
            widget as RestWidget,
            widget?.type,
            dashboard?.global_filters,
            dashboard,
            currentIntegrationType
          );

          dispatch(widgetUpdate(widgetId, updatedWidget.json));
        }
      } else if (DORA_REPORTS.DEPLOYMENT_FREQUENCY_REPORT === widget.type) {
        const getdoraProfileRoute = get(
          widgetConstants,
          [widget.type as string, "getDoraProfileDeploymentRoute"],
          undefined
        );
        const getdoraProfileApplication = get(
          widgetConstants,
          [widget.type as string, "getDoraProfileIntegrationApplication"],
          undefined
        );
        let doraProfileRoute;
        if (getdoraProfileRoute) {
          doraProfileRoute = getdoraProfileRoute({ workspaceOuProfilestate });
        }
        let doraProfileApplication;
        if (getdoraProfileApplication) {
          doraProfileApplication = getdoraProfileApplication({ workspaceOuProfilestate, reportType: widget.type });
        }
        delete widget?.query["across"];
        let filterWidgetQuery = Object.keys(widget?.query)?.filter(filter => filter !== "time_range");
        if (
          Object.keys(filterWidgetQuery).length > 0 &&
          currentIntegrationType === WorkflowIntegrationType.IM &&
          doraProfileApplication === IntegrationTypes.JIRA &&
          doraProfileRoute === CALCULATION_RELEASED_IN_KEY
        ) {
          widget.query = {};
          const updatedWidget = updateWidgetFiltersForReport(
            widget as RestWidget,
            widget?.type,
            dashboard?.global_filters,
            dashboard,
            currentIntegrationType
          );
          dispatch(widgetUpdate(widgetId, updatedWidget.json));
        }
      }
    }
  }, [integrations, workspaceOuProfilestate, widgetId]);

  return <ConfigureWidgetWrapper widgetId={widgetId} dashboardId={dashboardId} setHeader={handleSetHeader} />;
};

export default EditWidgetPage;
