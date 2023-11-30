import React, { useContext, useEffect, useMemo, useState } from "react";
import {
  ConfigTableWidgetWrapper,
  HygieneRestApiWrapper,
  ProductAggRestApiWrapper,
  SankeyRestApiWrapper,
  BAWidgetApiWrapper,
  WidgetApiWrapper,
  SprintSingleStatApiWrapper,
  DevProductivityAPIWrapper,
  DoraAPIWrapper
} from "shared-resources/containers";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import widgetConstants from "dashboard/constants/widgetConstants";
import { get, isEqual } from "lodash";
import { ChartContainerType } from "../helpers/helper";
import { FileReports } from "../constants/helper";
import { sprintStatReports } from "dashboard/graph-filters/components/sprintFilters.constant";
import {
  DashboardColorSchemaContext,
  WidgetDrilldownHandlerContext,
  WidgetLoadingContext
} from "dashboard/pages/context";
import {
  getGenericRestAPISelector,
  getGenericUUIDSelector,
  useParamSelector
} from "reduxConfigs/selectors/genericRestApiSelector";
import { GLOBAL_SETTINGS_UUID } from "dashboard/constants/uuid.constants";
import { LEVELOPS_REPORTS } from "../reports/levelops/constant";
import {
  AZURE_CUSTOM_FIELDS_LIST,
  JIRA_CUSTOM_FIELDS_LIST,
  setSelectedEntity,
  ZENDESK_CUSTOM_FIELDS_LIST
} from "reduxConfigs/actions/restapi";
import Loader from "components/Loader/Loader";
import { RestDashboard } from "classes/RestDashboards";
import { useDispatch, useSelector } from "react-redux";
import { customDataLoadingSelector, selectedDashboard } from "reduxConfigs/selectors/dashboardSelector";
import {
  cachedIntegrationsListSelector,
  cachedIntegrationsLoadingAndError
} from "reduxConfigs/selectors/CachedIntegrationSelector";
import { useLocation, useParams } from "react-router-dom";
import TableWidgetApiWrapper from "shared-resources/containers/table-api-container/TableWidgetApiWrapper";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";

interface DashboardGraphsContainerProps {
  widgetId: string;
  reportType: string;
  applicationType: string;
  uri: string;
  method: string;
  globalFilters: any;
  localFilters: any;
  widgetFilters: any;
  chartType: ChartType;
  chartProps: any;
  reload: boolean;
  setReload?: (reload: boolean) => void;
  weights?: any;
  maxRecords?: any;
  chartClickEnable?: boolean;
  onChartClick?: (data: any, application: string, filters?: any, stackFilters?: string[]) => void;
  setAggNameOptions?: (value: Array<any>) => void;
  aggNameOptions?: Array<string>;
  graphType: string;
  children?: Array<any>;
  customHygienes?: Array<string>;
  widgetMetaData?: any;
  filterApplyReload?: number;
  hiddenFilters: any;
  childrenMaxRecords?: any[];
  previewOnly?: boolean;
  jiraOrFilters?: { [key: string]: any };
  hideLegend?: boolean;
  dashboardMetaData?: any;
  dashboardId?: string;
}

const DashboardGraphsContainer: React.FC<DashboardGraphsContainerProps> = (props: DashboardGraphsContainerProps) => {
  const {
    maxRecords,
    chartClickEnable,
    onChartClick,
    widgetFilters,
    weights,
    setReload,
    reportType,
    widgetId,
    globalFilters,
    uri,
    method,
    localFilters,
    chartType,
    chartProps,
    reload,
    children,
    graphType,
    widgetMetaData,
    hiddenFilters,
    childrenMaxRecords,
    previewOnly,
    jiraOrFilters,
    hideLegend,
    dashboardMetaData,
    dashboardId
  } = props;
  const dispatch = useDispatch();
  const location = useLocation();
  const [widgetFilter, setWidgetFilter] = useState<any>(undefined);
  const { isDrilldownOpen, setDrilldown, drilldownWidgetId } = useContext(WidgetDrilldownHandlerContext);
  const { isThisWidgetLoading } = useContext(WidgetLoadingContext);
  const [integrationLoading, setIntegrationLoading] = useState(false);
  const [jiraFieldsLoading, setJiraFieldsLoading] = useState<boolean>(false);
  const [azureFieldsLoading, setAzureFieldsLoading] = useState<boolean>(false);
  const [zendeskFieldsLoading, setZendeskFieldsLoading] = useState<boolean>(false);
  const params: any = useParams();

  const dashboard: RestDashboard | null = useSelector(selectedDashboard);
  const customDataLoading = useSelector(customDataLoadingSelector);
  const integrationIds = useMemo(() => {
    return get(dashboard, ["query", "integration_ids"], []);
  }, [dashboard?.query]);

  const integrationsLoadingState = useSelector(cachedIntegrationsLoadingAndError);
  const integrations = useParamSelector(cachedIntegrationsListSelector, { integration_ids: integrationIds });

  // ENTITLEMENT FOR  USE IN EFFORT INVESTMENT PROFILE FOR Y AXIS LABLE
  const effortInvestmentTrendYaxis = useHasEntitlements(
    Entitlement.EFFORT_INVESTMENT_TREND_REPORT_YAXIS,
    EntitlementCheckType.AND
  );

  const integrationData = useParamSelector(getGenericUUIDSelector, {
    uri: "integrations",
    method: "list",
    uuid: "integrations_custom_ou_field_data"
  });

  useEffect(() => {
    const loading = get(integrationData, ["loading"], false);
    if (loading) {
      setIntegrationLoading(true);
      setJiraFieldsLoading(true);
      setAzureFieldsLoading(true);
      setZendeskFieldsLoading(true);
    }
  }, [integrationData]);

  const integrationKey = useMemo(
    () => (integrationIds.length ? integrationIds.sort().join("_") : "0"),
    [integrationIds]
  );
  const _globalFilters = useMemo(() => {
    return { integration_ids: integrationIds };
  }, [integrationIds]);

  const jiraFieldsSelector = useParamSelector(getGenericRestAPISelector, {
    uri: JIRA_CUSTOM_FIELDS_LIST,
    method: "list",
    uuid: integrationKey || 0
  });

  const azureFieldsSelector = useParamSelector(getGenericRestAPISelector, {
    uri: AZURE_CUSTOM_FIELDS_LIST,
    method: "list",
    uuid: integrationKey || 0
  });

  const zendeskFieldsSelector = useParamSelector(getGenericRestAPISelector, {
    uri: ZENDESK_CUSTOM_FIELDS_LIST,
    method: "list",
    uuid: integrationKey || 0
  });

  useEffect(() => {
    const loading = get(integrationsLoadingState, "loading", true);
    const error = get(integrationsLoadingState, "error", true);
    if (!loading && !error) {
      const applications = integrations.map((item: any) => item.application);
      if (applications.includes("jira")) {
        const loading = get(jiraFieldsSelector, "loading", true);
        if (loading !== jiraFieldsLoading) {
          if (customDataLoading) {
            dispatch(setSelectedEntity("custom-data-loading", { loading: false }));
          }
          setJiraFieldsLoading(loading);
        }
      } else if (applications?.length && !applications.includes("jira")) {
        setJiraFieldsLoading(false);
      }

      if (applications.includes("azure_devops")) {
        const loading = get(azureFieldsSelector, "loading", true);
        if (loading !== azureFieldsLoading) {
          if (customDataLoading) {
            dispatch(setSelectedEntity("custom-data-loading", { loading: false }));
          }
          setAzureFieldsLoading(loading);
        }
      } else if (applications?.length && !applications.includes("azure_devops")) {
        setAzureFieldsLoading(false);
      }

      if (applications.includes("zendesk")) {
        const loading = get(zendeskFieldsSelector, "loading", true);
        if (loading !== zendeskFieldsLoading) {
          if (customDataLoading) {
            dispatch(setSelectedEntity("custom-data-loading", { loading: false }));
          }
          setZendeskFieldsLoading(loading);
        }
      } else if (applications?.length && !applications.includes("zendesk")) {
        setZendeskFieldsLoading(false);
      }
      if (
        !applications?.includes("jira") &&
        !applications?.includes("zendesk") &&
        !applications?.includes("azure_devops")
      ) {
        setIntegrationLoading(false);
      }
      if (!(jiraFieldsLoading && azureFieldsLoading && zendeskFieldsLoading && integrationLoading)) {
        if (customDataLoading) {
          dispatch(setSelectedEntity("custom-data-loading", { loading: false }));
        }
      }
      setIntegrationLoading(false);
    }
  }, [
    jiraFieldsSelector,
    zendeskFieldsSelector,
    azureFieldsSelector,
    integrations,
    integrationsLoadingState,
    integrationLoading,
    jiraFieldsLoading,
    azureFieldsLoading,
    zendeskFieldsLoading
  ]);

  const filterKey = useMemo(() => (reportType === "scm_jira_files_report" ? "scm_module" : "module"), [reportType]);

  const application = useMemo(
    () => get(widgetConstants, [reportType, "drilldown", "application"], undefined),
    [reportType]
  );

  const container = useMemo(() => get(widgetConstants, [reportType, "chart_container"], undefined), [reportType]);

  const validReport = useMemo(
    () => (graphType?.includes("composite") ? (children || []).length > 0 : reportType !== undefined),
    [graphType, reportType]
  );

  const widgetReport = useMemo(
    () => (graphType?.includes("composite") ? true : container === ChartContainerType.WIDGET_API_WRAPPER),
    [container, graphType]
  );

  const updatedJiraOrFilters = useMemo(() => {
    if (get(widgetMetaData, "disable_or_filters", false)) {
      return {};
    }
    return jiraOrFilters;
  }, [widgetMetaData, jiraOrFilters]);

  useEffect(() => {
    if (isThisWidgetLoading && isDrilldownOpen && drilldownWidgetId === widgetId) {
      setDrilldown(undefined);
    }
  }, [isThisWidgetLoading, drilldownWidgetId, isDrilldownOpen, widgetId]);

  const handleChartClick = (data: any, filters?: any, stackFilters?: string[]) => {
    if (
      [
        FileReports.JIRA_SALESFORCE_FILES_REPORT,
        FileReports.JIRA_ZENDESK_FILES_REPORT,
        FileReports.SCM_FILES_REPORT,
        FileReports.SCM_JIRA_FILES_REPORT
      ].includes(reportType as any) &&
      typeof data === "object" &&
      data?.type === "change_directory"
    ) {
      setWidgetFilter({
        [filterKey]: data?.[filterKey] || "",
        repo_id: data?.repo_id
      });
    } else {
      onChartClick?.(data, application, filters, stackFilters);
    }
  };

  const metaData = useMemo(() => ({ width: 24 }), []);

  useEffect(() => {
    return () => {
      setWidgetFilter(undefined);
    };
  }, []);

  useEffect(() => {
    if (
      [
        FileReports.JIRA_SALESFORCE_FILES_REPORT,
        FileReports.JIRA_ZENDESK_FILES_REPORT,
        FileReports.SCM_FILES_REPORT,
        FileReports.SCM_JIRA_FILES_REPORT
      ].includes(reportType as any) &&
      !isEqual(get(localFilters, [filterKey], undefined), get(widgetFilter, [filterKey], undefined))
    ) {
      setWidgetFilter({
        [filterKey]: get(localFilters, [filterKey], undefined)
      });
    }
  }, [localFilters]);

  const filters = useMemo(() => {
    if (
      [
        FileReports.JIRA_SALESFORCE_FILES_REPORT,
        FileReports.JIRA_ZENDESK_FILES_REPORT,
        FileReports.SCM_FILES_REPORT,
        FileReports.SCM_JIRA_FILES_REPORT
      ].includes(reportType as any) &&
      widgetFilter
    ) {
      return {
        ...localFilters,
        [filterKey]: widgetFilter?.[filterKey],
        repo_id: widgetFilter?.repo_id
      };
    }
    return localFilters;
  }, [reportType, widgetFilter, localFilters, filterKey]);

  const globalSettingsState = useParamSelector(getGenericRestAPISelector, {
    uri: "configs",
    method: "list",
    uuid: GLOBAL_SETTINGS_UUID
  });

  const colorSchema = useMemo(() => {
    const globalColorSchema = globalSettingsState?.data?.records.find(
      (item: any) => item.name === "DASHBOARD_COLOR_SCHEME"
    );
    const scheme = globalColorSchema
      ? typeof globalColorSchema?.value === "string"
        ? JSON.parse(globalColorSchema?.value)
        : globalColorSchema?.value
      : [];
    return scheme.reduce((acc: Record<string, string>, item: any) => {
      acc = { ...acc, [`${item.key?.toLowerCase() ?? ""}`]: item.value };
      return acc;
    }, {});
  }, [globalSettingsState]);

  // dashboard variable should have latest state/queryparams dashboard id to avoid multiple refresh while swicthing dashboard
  if (
    integrationLoading ||
    jiraFieldsLoading ||
    azureFieldsLoading ||
    zendeskFieldsLoading ||
    customDataLoading ||
    (dashboard?.id !== params.dashboardId && !widgetId.includes("preview"))
  ) {
    return <Loader />;
  }
  return (
    <DashboardColorSchemaContext.Provider value={{ colorSchema }}>
      {validReport && widgetReport && (
        <WidgetApiWrapper
          id={widgetId}
          reportType={reportType}
          globalFilters={_globalFilters}
          uri={uri}
          method={method}
          metadata={metaData}
          filters={filters}
          chartType={chartType}
          widgetFilters={widgetFilters}
          chartProps={chartProps}
          reload={reload}
          setReload={setReload}
          children={children}
          graphType={graphType}
          maxRecords={maxRecords}
          childrenMaxRecords={childrenMaxRecords || []}
          widgetMetaData={widgetMetaData}
          filterApplyReload={props.filterApplyReload}
          hiddenFilters={hiddenFilters}
          chartClickEnable={chartClickEnable}
          onChartClick={handleChartClick}
          jiraOrFilters={updatedJiraOrFilters}
          previewOnly={previewOnly}
          hideLegend={hideLegend}
          dashboardMetaData={dashboardMetaData}
        />
      )}
      {container === ChartContainerType.HYGIENE_API_WRAPPER && (
        <HygieneRestApiWrapper
          id={widgetId}
          globalFilters={_globalFilters}
          uri={uri}
          method={method}
          metadata={metaData}
          filters={localFilters}
          hiddenFilters={hiddenFilters}
          weights={weights}
          widgetFilters={widgetFilters}
          chartType={chartType}
          chartProps={chartProps}
          reload={reload}
          setReload={setReload}
          reportType={reportType}
          chartClickEnable={chartClickEnable}
          widgetMetaData={widgetMetaData}
          customHygienes={props.customHygienes}
          filterApplyReload={props.filterApplyReload}
          onChartClick={handleChartClick}
          previewOnly={previewOnly}
          hideLegend={hideLegend}
          jiraOrFilters={updatedJiraOrFilters}
          application={props.applicationType}
          dashboardMetaData={dashboardMetaData}
        />
      )}
      {container === ChartContainerType.SANKEY_API_WRAPPER && (
        <SankeyRestApiWrapper
          id={widgetId}
          globalFilters={_globalFilters}
          uri={uri}
          method={method}
          metadata={metaData}
          filters={localFilters}
          hiddenFilters={hiddenFilters}
          weights={weights}
          widgetFilters={widgetFilters}
          widgetMetaData={widgetMetaData}
          chartType={chartType}
          chartProps={chartProps}
          reload={reload}
          setReload={setReload}
          reportType={reportType}
          chartClickEnable={chartClickEnable}
          filterApplyReload={props.filterApplyReload}
          onChartClick={handleChartClick}
          previewOnly={previewOnly}
          hideLegend={hideLegend}
          jiraOrFilters={updatedJiraOrFilters}
          application={props.applicationType}
          dashboardMetaData={dashboardMetaData}
        />
      )}
      {container === ChartContainerType.PRODUCTS_AGGS_API_WRAPPER && (
        <ProductAggRestApiWrapper
          id={widgetId}
          globalFilters={_globalFilters}
          widgetFilters={widgetFilters}
          uri={uri}
          method={method}
          metadata={metaData}
          filters={localFilters}
          hiddenFilters={hiddenFilters}
          widgetMetaData={widgetMetaData}
          chartType={chartType}
          chartProps={chartProps}
          reload={reload}
          setReload={setReload}
          reportType={reportType}
          hideLegend={hideLegend}
          dashboardMetaData={dashboardMetaData}
        />
      )}
      {container === ChartContainerType.BA_WIDGET_API_WRAPPER && (
        <BAWidgetApiWrapper
          id={widgetId}
          globalFilters={_globalFilters}
          filterApplyReload={props.filterApplyReload}
          onChartClick={handleChartClick}
          hideLegend={hideLegend}
          previewOnly={previewOnly}
          jiraOrFilters={updatedJiraOrFilters}
          dashboardMetaData={dashboardMetaData}
          effortInvestmentTrendYaxis={effortInvestmentTrendYaxis}
        />
      )}
      {container === ChartContainerType.CONFIG_TABLE_API_WRAPPER && (
        <ConfigTableWidgetWrapper
          id={props.widgetId}
          widgetMetaData={props.widgetMetaData}
          localFilters={props.localFilters}
          maxRecords={props.maxRecords}
          chartProps={props.chartProps}
          hasClickEvents={!!onChartClick}
          onChartClick={handleChartClick}
          hideLegend={hideLegend}
          dashboardMetaData={dashboardMetaData}
          reportType={reportType}
        />
      )}
      {sprintStatReports.includes(reportType as any) && (
        <SprintSingleStatApiWrapper
          id={props.widgetId}
          globalFilters={_globalFilters}
          filterApplyReload={props.filterApplyReload}
          onChartClick={handleChartClick}
          chartClickEnable={chartClickEnable}
          application={props.applicationType}
          dashboardMetaData={dashboardMetaData}
        />
      )}
      {container === ChartContainerType.TABLE_WIDGET_API_WRAPPER && (
        <TableWidgetApiWrapper
          widgetId={widgetId}
          reportType={reportType}
          widgetMetaData={widgetMetaData}
          dashboardMetaData={dashboardMetaData}
          filters={filters}
        />
      )}
      {container === ChartContainerType.DEV_PROD_WRAPPER && (
        <DevProductivityAPIWrapper
          id={props.widgetId}
          globalFilters={_globalFilters}
          filterApplyReload={props.filterApplyReload}
          onChartClick={handleChartClick}
          chartClickEnable={chartClickEnable}
          application={props.applicationType}
          dashboardMetaData={dashboardMetaData}
        />
      )}
      {container === ChartContainerType.DORA_API_WRAPPER && (
        <DoraAPIWrapper
          id={props.widgetId}
          globalFilters={_globalFilters}
          filterApplyReload={props.filterApplyReload}
          onChartClick={handleChartClick}
          chartClickEnable={chartClickEnable}
          application={props.applicationType}
          dashboardMetaData={dashboardMetaData}
          chartType={chartType}
          widgetMetaData={widgetMetaData}
          dashboardId={dashboardId}
          uri={uri}
        />
      )}
    </DashboardColorSchemaContext.Provider>
  );
};

export default React.memo(DashboardGraphsContainer);
