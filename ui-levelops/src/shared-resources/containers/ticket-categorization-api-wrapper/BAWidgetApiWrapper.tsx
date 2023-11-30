import React, { useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { RestWidget } from "classes/RestDashboards";
import Loader from "components/Loader/Loader";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { WidgetFilterContext, WidgetLoadingContext } from "dashboard/pages/context";
import { forEach, get, isEqual } from "lodash";
import { useDispatch } from "react-redux";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { getWidget } from "reduxConfigs/selectors/widgetSelector";
import ChartContainer from "../chart-container/chart-container.component";
import { widgetApiFilters } from "../widget-api-wrapper/helper";
import { DISPLAY_FORMAT_FILTER_KEY } from "dashboard/graph-filters/components/display-format-filter/helper";
import { reloadOnMetaDataChangeKeys, trimPreviewFromId } from "./helper";
import {
  activeEffortEIEngineerReport,
  AZURE_CUSTOM_FIELDS_LIST,
  completedEffortEIEngineerReport,
  JIRA_CUSTOM_FIELDS_LIST,
  ZENDESK_CUSTOM_FIELDS_LIST
} from "reduxConfigs/actions/restapi";
import { DefaultKeyTypes, URI_MAPPING } from "dashboard/constants/bussiness-alignment-applications/constants";
import {
  ACTIVE_WORK_UNIT_FILTER_KEY,
  STORE_ACTION,
  TICKET_CATEGORIZATION_UNIT_FILTER_KEY,
  uriUnitMapping
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { engineerTableEffortTypeToURIMapping } from "dashboard/constants/bussiness-alignment-applications/constants";
import { EffortType } from "dashboard/constants/enums/jira-ba-reports.enum";
import { activeWorkBasedReports } from "reduxConfigs/constants/effort-investment.constants";
import { useLocation } from "react-router-dom";
import queryString from "query-string";
import { EmptyApiErrorWidget } from "../../components";
import {
  ServerErrorSource,
  ERROR_CODE_RANGE_START,
  REQUEST_TIMEOUT_ERROR
} from "./../../helpers/server-error/constant";
import { getServerErrorDesc } from "./../../helpers/server-error/server-error-helper";
import { WidgetType } from "dashboard/helpers/helper";
import { OPEN_REPORTS_WITHOUT_DRILLDOWN } from "./constants";
import { cachedIntegrationsListSelector } from "reduxConfigs/selectors/CachedIntegrationSelector";
import { IntegrationTransformedCFTypes } from "configurations/configuration-types/integration-edit.types";
import { isDashboardTimerangeEnabled } from "helper/dashboard.helper";
import { issueContextTypes, logToBugsnag, severityTypes } from "bugsnag";

interface TicketCategorizationApiProps {
  id: string;
  globalFilters: any;
  filterApplyReload?: number;
  onChartClick?: (data: any, filters?: any) => void;
  hideLegend?: boolean;
  previewOnly?: boolean;
  jiraOrFilters: any;
  dashboardMetaData?: any;
  effortInvestmentTrendYaxis?: boolean;
}

const BAWidgetApiContainer: React.FC<TicketCategorizationApiProps> = (props: TicketCategorizationApiProps) => {
  const {
    globalFilters,
    id: widgetId,
    filterApplyReload,
    onChartClick,
    hideLegend,
    previewOnly,
    jiraOrFilters,
    dashboardMetaData,
    id,
    effortInvestmentTrendYaxis
  } = props;

  const { filters: contextFilters } = useContext(WidgetFilterContext);
  const { setWidgetLoading } = useContext(WidgetLoadingContext);

  const [apiData, setApiData] = useState<any>([]);
  const [reportHeaderData, setReportHeaderData] = useState<any[]>([]);
  const [apiLoading, setApiLoading] = useState<boolean>(false);

  const widget: RestWidget = useParamSelector(getWidget, { widget_id: trimPreviewFromId(widgetId) });
  const reportType = widget?.type;
  const widgetFilters = getWidgetConstant(reportType, "filters");
  const clickEnabled = getWidgetConstant(reportType, "chart_click_enable", true);
  const hiddenFilters = useMemo(() => {
    return getWidgetConstant(reportType, "hidden_filters", {});
  }, [reportType]);
  const chartType = getWidgetConstant(reportType, "chart_type");
  const application = getWidgetConstant(reportType, "application");
  const filters = widget?.query;
  const widgetMetaData = widget?.metadata;
  const maxRecords = widget?.max_records;
  const across = get(filters, ["across"], undefined);
  const location = useLocation();
  const queryParamOU = queryString.parse(location.search).OU as string;
  const effortInvestmentProfileFilter = useMemo(() => {
    return dashboardMetaData?.effort_investment_profile_filter;
  }, [dashboardMetaData?.effort_investment_profile_filter]);
  const integrationIds = useMemo(() => {
    return get(props.globalFilters, ["integration_ids"], []);
  }, [props.globalFilters?.integration_ids]);

  const integrations = useParamSelector(cachedIntegrationsListSelector, {
    integration_ids: props.globalFilters?.integration_ids
  });

  const integrationKey = useMemo(
    () => (integrationIds.length ? integrationIds.sort().join("_") : "0"),
    [integrationIds]
  );

  const azureFieldsSelector = useParamSelector(getGenericRestAPISelector, {
    uri: AZURE_CUSTOM_FIELDS_LIST,
    method: "list",
    uuid: integrationKey || 0
  });

  const jiraFieldsSelector = useParamSelector(getGenericRestAPISelector, {
    uri: JIRA_CUSTOM_FIELDS_LIST,
    method: "list",
    uuid: integrationKey || 0
  });

  const zendeskFieldsSelector = useParamSelector(getGenericRestAPISelector, {
    uri: ZENDESK_CUSTOM_FIELDS_LIST,
    method: "list",
    uuid: integrationKey || 0
  });

  const uri = useMemo(() => {
    let newUri = getWidgetConstant(reportType, "uri");
    const unitFilterValue = get(filters, [TICKET_CATEGORIZATION_UNIT_FILTER_KEY], undefined);
    const effortType = get(widgetMetaData, ["effort_type"], undefined);

    if (effortType && unitFilterValue) {
      if (effortType === EffortType.ACTIVE_EFFORT) {
        return get(filters, [ACTIVE_WORK_UNIT_FILTER_KEY]);
      } else {
        return get(engineerTableEffortTypeToURIMapping, [effortType, unitFilterValue]);
      }
    }

    if (unitFilterValue) {
      const URIMapping = getWidgetConstant(reportType, URI_MAPPING);
      if (URIMapping) {
        newUri = get(URIMapping, unitFilterValue, unitFilterValue);
      } else {
        newUri = get(uriUnitMapping, unitFilterValue, unitFilterValue);
      }
    }

    if (activeWorkBasedReports.includes(reportType)) {
      newUri = get(filters, [ACTIVE_WORK_UNIT_FILTER_KEY]);
    }

    return newUri;
  }, [reportType, filters, widgetMetaData]);

  const curMetaDataReloadObject = useMemo(() => {
    const newObj: any = {};
    forEach(reloadOnMetaDataChangeKeys, key => {
      newObj[key] = widgetMetaData[key];
    });
    return newObj;
  }, [widgetMetaData]);

  const supportedCustomFields = useMemo(() => {
    const applications = (integrations || []).map((item: any) => item.application);
    let customFields: IntegrationTransformedCFTypes[] = [];

    if (applications.includes("jira")) {
      customFields = [...customFields, ...get(jiraFieldsSelector, "data", [])];
    }

    if (applications.includes("azure_devops")) {
      customFields = [...customFields, ...get(azureFieldsSelector, "data", [])];
    }

    if (applications.includes("zendesk")) {
      customFields = [...customFields, ...get(zendeskFieldsSelector, "data", [])];
    }

    return customFields;
  }, [azureFieldsSelector, jiraFieldsSelector, zendeskFieldsSelector, integrations]);

  const apiMethod = getWidgetConstant(reportType, "method", "list");

  const apiState = useParamSelector(getGenericRestAPISelector, {
    uri,
    method: apiMethod,
    uuid: widgetId
  });

  const dispatch = useDispatch();

  const filtersRef = useRef<any>({});
  const reportRef = useRef<string>();
  const reloadOnPreviewEnableDisable = useRef<any>({});
  const globalFiltersRef = useRef<any>({});
  const globalTimeFiltersRef = useRef<any>({});
  const metaDataRef = useRef<any>({});

  const fetchData = () => {
    const effortType = get(widgetMetaData, ["effort_type"], undefined);
    let filters = getFilters();
    if (effortType) {
      if (effortType === EffortType.COMPLETED_EFFORT) {
        dispatch(completedEffortEIEngineerReport(widgetId, filters, uri, { application }));
      } else {
        dispatch(
          activeEffortEIEngineerReport(widgetId, filters, uri, {
            application
          })
        );
      }
    } else {
      const getApiAction = getWidgetConstant(reportType, STORE_ACTION);
      dispatch(
        getApiAction(uri, apiMethod, filters, widgetId, {
          dashboardMetaData,
          maxRecords,
          widgetMetaData,
          application,
          effortInvestmentTrendYaxis
        })
      );
    }
    setApiLoading(true);
  };

  useEffect(() => {
    filtersRef.current = getFilters(false);
    reportRef.current = reportType;
    reloadOnPreviewEnableDisable.current = filterApplyReload;
    globalFiltersRef.current = globalFilters;
    metaDataRef.current = curMetaDataReloadObject;
    globalTimeFiltersRef.current = dashboardMetaData.dashboard_time_range_filter;
    fetchData();
  }, []);

  useEffect(() => {
    let allFilters = getFilters(false);
    if (
      (!isEqual(allFilters, filtersRef.current) && !apiLoading) ||
      (globalTimeFiltersRef.current !== dashboardMetaData.dashboard_time_range_filter &&
        isDashboardTimerangeEnabled(dashboardMetaData || {}))
    ) {
      globalTimeFiltersRef.current = dashboardMetaData.dashboard_time_range_filter;
      filtersRef.current = allFilters;
      fetchData();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    filters,
    widgetFilters,
    hiddenFilters,
    effortInvestmentProfileFilter,
    dashboardMetaData,
    queryParamOU,
    apiLoading,
    effortInvestmentTrendYaxis
  ]);

  useEffect(() => {
    if (!isEqual(reportType, reportRef.current) && !apiLoading) {
      reportRef.current = reportType;
      fetchData();
    }
  }, [reportType]);

  useEffect(() => {
    if (!isEqual(curMetaDataReloadObject, metaDataRef.current) && !apiLoading) {
      metaDataRef.current = curMetaDataReloadObject;
      fetchData();
    }
  }, [curMetaDataReloadObject]);

  useEffect(() => {
    if (!isEqual(filterApplyReload, reloadOnPreviewEnableDisable.current) && !apiLoading) {
      reloadOnPreviewEnableDisable.current = filterApplyReload;
      fetchData();
    }
  }, [filterApplyReload]);

  useEffect(() => {
    if (apiLoading) {
      const loading = get(apiState, ["loading"], true);
      const error = get(apiState, ["error"], true);
      if (!loading && !error) {
        const data = get(apiState, ["data"], {});
        if (Object.keys(data).length > 0) {
          setApiData(get(data, ["records"], []));
          setReportHeaderData(get(data, ["reportHeaderInfoData"], []));
          setApiLoading(false);
        }
      } else if (!loading && error) {
        setApiLoading(false);
      }
    }
  }, [apiState]);

  useEffect(() => {
    setWidgetLoading(widgetId, apiLoading);
  }, [apiLoading]);

  const getFilters = (updateTimeFilters: boolean = true) => {
    return widgetApiFilters({
      widgetFilters,
      filters,
      hiddenFilters,
      globalFilters,
      reportType,
      contextFilters,
      updateTimeFilters,
      uri,
      widgetId,
      widgetMetaData,
      application,
      jiraOrFilters,
      supportedCustomFields,
      dashboardMetaData,
      availableIntegrations: integrations || [],
      queryParamOU,
      customFieldRecords: supportedCustomFields
    });
  };

  const handleChartClick = useCallback(
    (data: any) => {
      if (!clickEnabled) return;
      if (OPEN_REPORTS_WITHOUT_DRILLDOWN.includes(reportType as any)) {
        let filters = getFilters();
        onChartClick && onChartClick(data, filters);
      } else {
        onChartClick && onChartClick(data);
      }
    },
    [clickEnabled, getFilters]
  );

  const errorDesc = useMemo(
    () =>
      getServerErrorDesc(
        widget.widget_type === WidgetType.STATS ? ServerErrorSource.STAT_WIDGET : ServerErrorSource.WIDGET
      ),
    [widget.widget_type]
  );

  const chartProps = useMemo(() => {
    const reportChartProps = getWidgetConstant(reportType, "chart_props", {});
    return {
      onClick: handleChartClick,
      reportType,
      headerData: reportHeaderData,
      data: apiData,
      displayFormat:
        widgetMetaData?.[DISPLAY_FORMAT_FILTER_KEY] ||
        getWidgetConstant(reportType, DefaultKeyTypes.DEFAULT_DISPLAY_FORMAT_KEY),
      across,
      hideLegend,
      previewOnly,
      widgetId: id,
      ...reportChartProps,
      reload: fetchData
    };
  }, [
    widgetMetaData,
    reportType,
    apiData,
    reportHeaderData,
    across,
    hideLegend,
    handleChartClick,
    effortInvestmentTrendYaxis
  ]);

  if (apiLoading) return <Loader />;

  if (
    apiState?.error_code &&
    (apiState?.error_code >= ERROR_CODE_RANGE_START || apiState?.error_code === REQUEST_TIMEOUT_ERROR)
  ) {
    logToBugsnag("Failed to load BA widget", severityTypes.ERROR, issueContextTypes.WIDGETS, {
      error_code: apiState?.error_code,
      filters: filters,
      widgetId: id
    });

    return (
      <EmptyApiErrorWidget
        description={errorDesc}
        className={widget.widget_type === WidgetType.STATS ? "stats-error-container" : ""}
      />
    );
  }

  return <ChartContainer chartType={chartType} chartProps={chartProps as any} />;
};

export default BAWidgetApiContainer;
