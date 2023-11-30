import React, { useEffect, useMemo, useState } from "react";
import { RouteComponentProps } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { Route, Switch, useParams } from "react-router-dom";
import queryString from "query-string";
import Loader from "components/Loader/Loader";
import {
  dashboardsGet,
  revertWidgets,
  JIRA_CUSTOM_FIELDS_LIST,
  AZURE_CUSTOM_FIELDS_LIST,
  jiraCustomFilterFieldsList,
  azureCustomFilterFieldsList,
  ZENDESK_CUSTOM_FIELDS_LIST,
  zendeskCustomFilterFieldsList
} from "reduxConfigs/actions/restapi/index";
import { dashboardsGetSelector, getDashboard } from "reduxConfigs/selectors/dashboardSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import {
  LazyLoadedDashboardRearrange,
  LazyLoadedWidgetByThemePage,
  LazyLoadedWidgetCreatePage,
  LazyLoadedWidgetEditPage,
  LazyLoadedWidgetExplorerPage,
  LazyLoadedWidgetByCustomCategoryPage
} from "./index";
import { WebRoutes } from "../../routes/WebRoutes";
import { DashboardWidgetResolverContext } from "dashboard/pages/context";
import { selectedDashboardIntegrations } from "reduxConfigs/selectors/integrationSelector";
import { get } from "lodash";
import { getGenericRestAPISelector } from "reduxConfigs/selectors/genericRestApiSelector";
import {
  cachedIntegrationsLoadingAndError,
  cachedIntegrationsListSelector
} from "reduxConfigs/selectors/CachedIntegrationSelector";
import { getWorkflowProfileByOuAction } from "reduxConfigs/actions/restapi/workflowProfileByOuAction";
import { DashboardPathProps, ProjectPathProps } from "classes/routeInterface";

interface DashboardResolverProps extends RouteComponentProps {}

const DashboardWidgetsContainer: React.FC<DashboardResolverProps> = ({ location, match }) => {
  const dispatch = useDispatch();
  const [loading, setLoading] = useState(true);
  const [widgetId, setWidgetId] = useState<string | undefined>();
  const queryParamOU = queryString.parse(location.search).OU as string;
  const projectParams = useParams<ProjectPathProps>();
  const dashboardParams = useParams<DashboardPathProps>();
  const dashboardId = dashboardParams.dashboardId || "";
  if (widgetId === "modify-layout") {
    setWidgetId(undefined);
  }
  const [loadingIntegrations, setLoadingIntegrations] = useState<boolean>(true);
  const [jiraFieldsLoading, setJiraFieldsLoading] = useState<boolean>(false);
  const [azureFieldsLoading, setAzureFieldsLoading] = useState<boolean>(false);
  const [zendeskFieldsLoading, setZendeskFieldsLoading] = useState<boolean>(false);

  const { loading: dashboardLoading } = useParamSelector(dashboardsGetSelector, { dashboard_id: dashboardId });
  const dashboard = useParamSelector(getDashboard, { dashboard_id: dashboardId });
  const hasDashboard = !!dashboard?.id;
  const selectedDashboardIntegration = useSelector(selectedDashboardIntegrations);

  const integrationIds = useMemo(() => {
    return selectedDashboardIntegration.map((item: any) => item.id);
  }, [selectedDashboardIntegration]);

  const integrationKey = useMemo(
    () => (integrationIds.length ? integrationIds.sort().join("_") : "0"),
    [integrationIds]
  );
  const integrationsLoadingState = useSelector(cachedIntegrationsLoadingAndError);
  const integrations = useParamSelector(cachedIntegrationsListSelector, { integration_ids: integrationIds });

  const jiraFieldsState = useParamSelector(getGenericRestAPISelector, {
    uri: JIRA_CUSTOM_FIELDS_LIST,
    method: "list",
    uuid: integrationKey || 0
  });

  const azureFieldsState = useParamSelector(getGenericRestAPISelector, {
    uri: AZURE_CUSTOM_FIELDS_LIST,
    method: "list",
    uuid: integrationKey || 0
  });

  const zendeskFieldsState = useParamSelector(getGenericRestAPISelector, {
    uri: ZENDESK_CUSTOM_FIELDS_LIST,
    method: "list",
    uuid: integrationKey || 0
  });

  useEffect(() => {
    dispatch(getWorkflowProfileByOuAction(queryParamOU));
  }, [dashboardId, queryParamOU]);

  useEffect(() => {
    // Don't have dashboard load it.
    if (!hasDashboard) {
      setLoading(true);
      dispatch(dashboardsGet(dashboardId));
    }

    return () => {
      dispatch(revertWidgets(dashboardId));
    };
  }, []);

  useEffect(() => {
    if (!dashboardLoading && loading) {
      setLoading(false);
    }
  }, [dashboardLoading]);

  useEffect(() => {
    if (loadingIntegrations) {
      const loading = get(integrationsLoadingState, "loading", true);
      const error = get(integrationsLoadingState, "error", true);

      if (!loading && !error) {
        const data: any = integrations;
        const applications = data.map((integration: any) => integration.application);

        if (applications.includes("jira") && integrationIds.length) {
          const data = get(jiraFieldsState, ["data"], []);
          if (!data) {
            setJiraFieldsLoading(true);
            dispatch(jiraCustomFilterFieldsList({ integration_ids: integrationIds }, integrationKey));
          }
        }

        if (applications.includes("azure_devops") && integrationIds.length) {
          const data = get(azureFieldsState, ["data"], true);
          if (!data) {
            setAzureFieldsLoading(true);
            dispatch(azureCustomFilterFieldsList({ integration_ids: integrationIds }, integrationKey));
          }
        }

        if (applications.includes("zendesk")) {
          const data = get(zendeskFieldsState, ["data"], true);
          if (!data) {
            setZendeskFieldsLoading(true);
            dispatch(zendeskCustomFilterFieldsList({ integration_ids: integrationIds }, integrationKey));
          }
        }

        setLoadingIntegrations(false);
      }
    }
  }, [loadingIntegrations, integrationsLoadingState, integrationIds, integrations]);

  useEffect(() => {
    if (jiraFieldsLoading) {
      const loading = get(jiraFieldsState, "loading", true);
      const error = get(jiraFieldsState, "error", true);
      if (!loading && !error) {
        setJiraFieldsLoading(false);
      }
    }
  }, [jiraFieldsLoading, jiraFieldsState]);

  useEffect(() => {
    if (azureFieldsLoading) {
      const loading = get(azureFieldsState, "loading", true);
      const error = get(azureFieldsState, "error", true);
      if (!loading && !error) {
        setAzureFieldsLoading(false);
      }
    }
  }, [azureFieldsLoading, azureFieldsState]);

  useEffect(() => {
    if (zendeskFieldsLoading) {
      const loading = get(zendeskFieldsState, "loading", true);
      const error = get(zendeskFieldsState, "error", true);
      if (!loading && !error) {
        setZendeskFieldsLoading(false);
      }
    }
  }, [zendeskFieldsLoading, zendeskFieldsState]);

  const contextValue = useMemo(() => ({ dashboardId }), [dashboardId]);

  if (loading || loadingIntegrations || jiraFieldsLoading || azureFieldsLoading || zendeskFieldsLoading) {
    return <Loader />;
  }
  return (
    <DashboardWidgetResolverContext.Provider value={contextValue}>
      <Switch>
        <Route
          exact
          path={WebRoutes.dashboard.widgets.widgetsRearrange(projectParams)}
          component={LazyLoadedDashboardRearrange}
        />
        <Route
          exact
          path={WebRoutes.dashboard.widgets.widgetsExplorer(projectParams)}
          component={LazyLoadedWidgetExplorerPage}
        />
        <Route
          exact
          path={WebRoutes.dashboard.widgets.widgetsExploreByCategory(projectParams)}
          component={LazyLoadedWidgetByThemePage}
        />
        <Route
          exact
          path={WebRoutes.dashboard.widgets.widgetsExploreByCustomCategory(projectParams)}
          component={LazyLoadedWidgetByCustomCategoryPage}
        />
        <Route exact path={WebRoutes.dashboard.widgets.details(projectParams)} component={LazyLoadedWidgetEditPage} />
        <Route exact path={WebRoutes.dashboard.widgets.create(projectParams)} component={LazyLoadedWidgetCreatePage} />
      </Switch>
    </DashboardWidgetResolverContext.Provider>
  );
};

export default DashboardWidgetsContainer;
