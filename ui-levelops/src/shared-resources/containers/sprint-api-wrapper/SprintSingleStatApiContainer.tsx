import React, { useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { RestWidget } from "classes/RestDashboards";
import Loader from "components/Loader/Loader";
import { WidgetFilterContext, WidgetLoadingContext } from "dashboard/pages/context";
import { get, isEqual } from "lodash";
import { useDispatch } from "react-redux";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { getWidget } from "reduxConfigs/selectors/widgetSelector";
import ChartContainer from "../chart-container/chart-container.component";
import { ChartType } from "../chart-container/ChartType";
import { widgetApiFilters } from "../widget-api-wrapper/helper";
import {
  filterExcludeKeysForNoReload,
  getApiCallEnableDisableObject,
  reloadOnMetaDataChangeKeys,
  trimPreviewFromId
} from "./sprintApiHelper";
import { jiraSprintListReport } from "reduxConfigs/actions/restapi/jiraSprintActions";
import { AZURE_SPRINT_REPORTS } from "../../../dashboard/constants/applications/names";
import {
  AZURE_CUSTOM_FIELDS_LIST,
  genericList,
  JIRA_CUSTOM_FIELDS_LIST,
  ZENDESK_CUSTOM_FIELDS_LIST
} from "reduxConfigs/actions/restapi";
import { useLocation } from "react-router-dom";
import queryString from "query-string";
import { cachedIntegrationsListSelector } from "reduxConfigs/selectors/CachedIntegrationSelector";
import { IntegrationTransformedCFTypes } from "configurations/configuration-types/integration-edit.types";
interface SprintSingleStatApiWrapperProps {
  id: string;
  globalFilters: any;
  filterApplyReload?: number;
  onChartClick?: (data: any) => void;
  chartClickEnable?: boolean;
  application: string;
  dashboardMetaData?: any;
}

const SprintSingleStatApiWrapper: React.FC<SprintSingleStatApiWrapperProps> = ({
  globalFilters,
  id: widgetId,
  filterApplyReload,
  onChartClick,
  chartClickEnable,
  application,
  dashboardMetaData
}) => {
  const { filters: contextFilters } = useContext(WidgetFilterContext);
  const { setWidgetLoading } = useContext(WidgetLoadingContext);

  const [apiData, setApiData] = useState<any>(undefined);
  const [apiLoading, setApiLoading] = useState<boolean>(false);
  const widget: RestWidget = useParamSelector(getWidget, { widget_id: trimPreviewFromId(widgetId) });
  const reportType = widget?.type;
  const widgetFilters = widget?.widgetFilters;
  const clickEnabled = widget?.isChartClickEnabled;
  const location = useLocation();
  const queryParamOU = queryString.parse(location.search).OU as string;
  const hiddenFilters = useMemo(() => {
    return widget?.hiddenFilters;
  }, [widget?.type]);
  const filters = widget?.query;
  const widgetMetaData = widget?.metadata;
  const uri = widget?.uri;
  const apiMethod = widget?.method;
  const curMetaDataReloadObject = useMemo(() => {
    getApiCallEnableDisableObject(widgetMetaData, reloadOnMetaDataChangeKeys);
  }, [widgetMetaData]);

  const reportChartData = useMemo(() => {
    const transformFunction = widget?.reportDataTransformationFunction;
    return transformFunction({ apiData, filters, widgetMetaData, reportType });
  }, [apiData, filters, widgetMetaData, reportType]);

  const apiLoadingState = useParamSelector(getGenericRestAPISelector, {
    uri,
    method: apiMethod,
    uuid: widgetId
  });
  const integrationIds = useMemo(() => {
    return get(globalFilters, ["integration_ids"], []);
  }, [globalFilters?.integration_ids]);

  const integrations = useParamSelector(cachedIntegrationsListSelector, {
    integration_ids: globalFilters?.integration_ids
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

  const dispatch = useDispatch();

  const filtersRef = useRef<any>({});
  const reportRef = useRef<string>();
  const reloadOnPreviewEnableDisable = useRef<any>({});
  const globalFiltersRef = useRef<any>({});
  const metaDataRef = useRef<any>({});

  const fetchData = () => {
    if (reportType === AZURE_SPRINT_REPORTS.SPRINT_METRICS_SINGLE_STAT) {
      dispatch(genericList("issue_management_sprint_report", apiMethod, getFilters(), null, widgetId));
    } else {
      dispatch(jiraSprintListReport(getFilters(), widgetId));
    }
    setApiLoading(true);
  };

  useEffect(() => {
    filtersRef.current = getFilters(false);
    reportRef.current = reportType;
    reloadOnPreviewEnableDisable.current = filterApplyReload;
    globalFiltersRef.current = globalFilters;
    metaDataRef.current = curMetaDataReloadObject;
    fetchData();
  }, []);

  useEffect(() => {
    const allFilters = getFilters(false);
    if (!isEqual(allFilters, filtersRef.current) && !apiLoading) {
      filtersRef.current = allFilters;
      fetchData();
    }
  }, [filters, widgetFilters, hiddenFilters, dashboardMetaData, queryParamOU, apiLoading]);

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
      const loading = get(apiLoadingState, ["loading"], true);
      const error = get(apiLoadingState, ["error"], true);
      if (!loading && !error) {
        const data = get(apiLoadingState, ["data"], {});
        if (Object.keys(data).length > 0) {
          setApiData(get(data, ["records"], []));
          setApiLoading(false);
        }
      } else if (!loading && error) {
        setApiLoading(false);
      }
    }
  }, [apiLoadingState]);

  useEffect(() => {
    setWidgetLoading(widgetId, apiLoading);
  }, [apiLoading]);

  const handleChartClick = useCallback(
    (data: any) => {
      if (!clickEnabled) return;
      onChartClick && onChartClick(data);
    },
    [clickEnabled]
  );

  const supportedCustomFields = useMemo(() => {
    const applications = (integrations || [])?.map((item: any) => item.application);
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
  }, [azureFieldsSelector, jiraFieldsSelector, zendeskFieldsSelector]);

  const getFilters = (updateTimeFilters: boolean = true) => {
    let newFilters = widgetApiFilters({
      widgetFilters,
      filters,
      hiddenFilters,
      globalFilters,
      application,
      reportType,
      contextFilters,
      updateTimeFilters,
      widgetMetaData,
      supportedCustomFields,
      dashboardMetaData,
      availableIntegrations: integrations || [],
      queryParamOU,
      customFieldRecords: supportedCustomFields
    });
    return getApiCallEnableDisableObject(newFilters, filterExcludeKeysForNoReload, true);
  };

  const chartProps = useMemo(() => {
    return {
      ...(reportChartData || {}),
      reportType,
      onClick: handleChartClick,
      hasClickEvents: chartClickEnable,
      id: widgetId
    };
  }, [reportChartData, reportType, widgetId]);

  if (apiLoading) return <Loader />;

  return <ChartContainer chartType={ChartType.STATS} chartProps={chartProps as any} />;
};

export default SprintSingleStatApiWrapper;
