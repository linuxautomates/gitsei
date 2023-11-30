import { getGroupByRootFolderKey } from "configurable-dashboard/helpers/helper";
import {
  bullseyeDataTransformer,
  bullseyeTrendTransformer,
  jiraBacklogTransformerWrapper,
  jiraResolutionTimeDataTransformer,
  timeAcrossStagesDataTransformer
} from "custom-hooks/helpers";
import {
  azureResolutionTimeDataTransformer,
  scmaResolutionTimeDataTransformer
} from "custom-hooks/helpers/seriesData.helper";
import { customFieldFiltersSanitize } from "custom-hooks/helpers/zendeskCustomFieldsFiltersTransformer";
import { ACTIVE_SPRINT_TYPE_FILTER_KEY } from "dashboard/components/dashboard-application-filters/AddFiltersComponent/filterConstants";
import { LEVELOPS_MULTITIME_SERIES_REPORT } from "dashboard/constants/applications/multiTimeSeries.application";
import {
  azureIterationSupportableReports,
  ISSUE_MANAGEMENT_REPORTS,
  JENKINS_AZURE_REPORTS,
  JENKINS_REPORTS,
  JIRA_MANAGEMENT_TICKET_REPORT,
  JIRA_SPRINT_REPORTS,
  PAGERDUTY_REPORT,
  scmCicdReportTypes,
  BULLSEYE_REPORTS,
  ALL_VELOCITY_PROFILE_REPORTS,
  SIMPLIFY_VALUE,
  LEAD_TIME_MTTR_REPORTS,
  AZURE_SPRINT_REPORTS
} from "dashboard/constants/applications/names";
import { scmTableReportType } from "dashboard/constants/enums/scm-reports.enum";
import { GET_WIDGET_CHART_PROPS } from "dashboard/constants/filter-name.mapping";
import { FileReports, getSCMColumnsForMetrics, ReportsApplicationType } from "dashboard/constants/helper";
import { IssueVisualizationTypes, SCMVisualizationTypes } from "dashboard/constants/typeConstants";
import { GLOBAL_SETTINGS_UUID } from "dashboard/constants/uuid.constants";
import {
  APPLICATIONS_SUPPORTING_OU_FILTERS,
  JIRA_LEAD_TIME_REPORTS
} from "dashboard/pages/dashboard-drill-down-preview/helper-constants";
import {
  committerColumn,
  fileTypeColumn,
  repoColumn
} from "dashboard/pages/dashboard-tickets/configs/githubTableConfig";
import { cloneDeep, forEach, get, isEqual, uniqBy, unset } from "lodash";
import { default as React, ReactNode, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { useSelector } from "react-redux";
import {
  AZURE_CUSTOM_FIELDS_LIST,
  JIRA_CUSTOM_FIELDS_LIST,
  TESTRAILS_CUSTOM_FIELDS_LIST,
  ZENDESK_CUSTOM_FIELDS_LIST
} from "reduxConfigs/actions/restapi";
import { customTimeFilterKeysSelector } from "reduxConfigs/selectors/jira.selector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { SCM_PRS_REPORTS } from "shared-resources/charts/helper";
import { baseColumnConfig } from "utils/base-table-config";
import Loader from "components/Loader/Loader";
import { useApi, useDataTransformMany, useGlobalFilters } from "custom-hooks";
import widgetConstants, { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { DEFAULT_SCM_SETTINGS_OPTIONS } from "dashboard/constants/defaultFilterOptions";
import { updateIssueCreatedAndUpdatedFilters } from "dashboard/graph-filters/components/updateFilter.helper";
import {
  CacheWidgetPreview,
  WidgetFilterContext,
  WidgetIntervalContext,
  WidgetLoadingContext
} from "dashboard/pages/context";
import { getGenericRestAPISelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { sanitizeObject } from "utils/commonUtils";
import { EmptyWidget } from "../../components";
import { EmptyApiErrorWidget } from "../../components";
import ChartContainer from "../chart-container/chart-container.component";
import { ChartType } from "../chart-container/ChartType";
import {
  backlogTrendReportChartType,
  combineAllFilters,
  convertChildKeysToSiblingKeys,
  getBacklogChartProps,
  getProps,
  getSupportedApplications,
  getWidgetUri,
  mapWidgetMetadataForCompare,
  resolutionTimeReportChartType,
  sanitizeStages,
  updateTimeFiltersValue,
  widgetApiFilters
} from "./helper";
import EmptyWidgetPreview from "configurable-dashboard/components/widget-preview/custom-preview/EmptyWidgetPreview";
import { useLocation } from "react-router-dom";
import queryString from "query-string";
import { REPORT_REQUIRES_OU } from "constants/formWarnings";
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
import "./widgetApiWrapper.styles.scss";
import { EmptyWidgetPreviewArgsType } from "model/report/dev-productivity/baseDevProductivityReports.constants";
import { useMataDataTransformMany } from "custom-hooks/useDataTransformMany";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { sonarQubeMetricsOptions } from "dashboard/graph-filters/components/Constants";
import { issueContextTypes, logToBugsnag, severityTypes } from "bugsnag";
import { IssueManagementOptions } from "constants/issueManagementOptions";
import { allTimeFilterKeys } from "dashboard/graph-filters/components/helper";

interface WidgetApiContainerProps {
  id: string;
  reportType: string;
  globalFilters: any;
  uri: string;
  method: string;
  metadata: any;
  filters: any;
  chartType: ChartType;
  chartProps: any;
  reload: boolean;
  widgetFilters: any;
  maxRecords?: any;
  chartClickEnable?: boolean;
  widgetMetaData?: any;
  children?: Array<any>;
  graphType: string;
  onChartClick?: (data: any, filters?: any, stackFilters?: string[]) => void;
  setReload?: (reload: boolean) => void;
  filterApplyReload?: number;
  hiddenFilters: any;
  childrenMaxRecords?: any[];
  jiraOrFilters?: { [key: string]: any };
  previewOnly?: boolean;
  hideLegend?: boolean;
  dashboardMetaData?: any;
}

const WidgetApiContainer: React.FC<WidgetApiContainerProps> = (props: WidgetApiContainerProps) => {
  const { chartType, widgetFilters, hiddenFilters, maxRecords, dashboardMetaData, graphType } = props;
  const [fetchData, setFetchData] = useState<number>(0);
  const [localFilters, setLocalFilters] = useState<any>({});
  const [childrenMaxRecords, setChildrenMaxRecords] = useState<any>(props.childrenMaxRecords || []);
  const location = useLocation();
  const queryParamOU = queryString.parse(location.search).OU as string;
  const { setWidgetLoading } = useContext(WidgetLoadingContext);
  const { filters: contextFilters } = useContext(WidgetFilterContext);
  const { tempWidgetInterval } = useContext(WidgetIntervalContext);
  const globalFilters = useGlobalFilters(props.globalFilters);

  const integrationIds = useMemo(() => {
    return get(props.globalFilters, ["integration_ids"], []);
  }, [props.globalFilters]);

  const integrationKey = useMemo(
    () => (integrationIds.length ? integrationIds.sort().join("_") : "0"),
    [props.globalFilters]
  );
  const filters = Object.keys(props.filters || {}).length ? props.filters : {};
  const dashboardTimeFilterValue = dashboardMetaData?.dashboard_time_range_filter;
  const dashboardOuIds = useMemo(() => {
    return queryParamOU ? [queryParamOU] : [];
  }, [queryParamOU]);

  const excludeStatusState = useParamSelector(getGenericRestAPISelector, {
    uri: "jira_filter_values",
    method: "list",
    uuid: "exclude_status"
  });

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

  const testrailsFieldsSelector = useParamSelector(getGenericRestAPISelector, {
    uri: TESTRAILS_CUSTOM_FIELDS_LIST,
    method: "list",
    uuid: integrationKey || 0
  });

  const integrations = useParamSelector(cachedIntegrationsListSelector, {
    integration_ids: props.globalFilters?.integration_ids
  });

  const globalSettingsState = useParamSelector(getGenericRestAPISelector, {
    uri: "configs",
    method: "list",
    uuid: GLOBAL_SETTINGS_UUID
  });

  const widgetMetaToCompare = useMemo(() => {
    return mapWidgetMetadataForCompare(props.widgetMetaData);
  }, [props.widgetMetaData]);

  const filtersRef = useRef<any>({});
  const reportRef = useRef<string>();
  const maxRecordsRef = useRef<any>();
  const metadataRef = useRef<any>();
  const childFiltersRef = useRef<any>({});
  const filterApplyReloadRef = useRef<any>({});
  const globalIntegrationIdsRef = useRef<string[]>([]);
  const dashboardTimeFilterValueRef = useRef<any>({});
  const dashboardOuIdsRef = useRef<any>(dashboardOuIds);

  const uri = useMemo(() => (chartType === ChartType.STATS ? `${props.uri}-stat` : props.uri), [props.uri, chartType]);

  const filterKey = useMemo(
    () => (props.reportType === FileReports.SCM_JIRA_FILES_REPORT ? "scm_module" : "module"),
    [props.reportType]
  );

  const application = useMemo(
    () => get(widgetConstants, [props.reportType, "application"], undefined),
    [props.reportType]
  );

  const reloadChartDependencies = useMemo(() => {
    if (ALL_VELOCITY_PROFILE_REPORTS.includes(props.reportType as any)) {
      return [get(props.widgetMetaData, "metrics", "mean"), ...get(props.widgetMetaData, "hide_stages", [])];
    }
    if ([JIRA_MANAGEMENT_TICKET_REPORT.TICKET_ISSUE_SINGLE_STAT].includes(props.reportType as any)) {
      return get(props.widgetMetaData, "metrics", "total_tickets");
    }
    return undefined;
  }, [props.reportType, props.widgetMetaData]);

  const cacheWidgetPreview = useContext(CacheWidgetPreview);

  const fetchDataAgain = (forceRefresh = false) => {
    if (cacheWidgetPreview && !forceRefresh) {
      return;
    }
    setFetchData(prev => prev + 1);
  };

  const supportedCustomFields = useMemo(() => {
    const applications = (integrations || []).map((item: any) => item.application);
    let customFields: IntegrationTransformedCFTypes[] = [];
    if (applications.includes("jira") && props.globalFilters?.integration_ids?.length > 0) {
      customFields = [...customFields, ...get(jiraFieldsSelector, "data", [])];
    }

    if (applications.includes("azure_devops") && props.globalFilters?.integration_ids?.length > 0) {
      customFields = [...customFields, ...get(azureFieldsSelector, "data", [])];
    }

    if (applications.includes("zendesk") && props.globalFilters?.integration_ids?.length > 0) {
      customFields = [...customFields, ...get(zendeskFieldsSelector, "data", [])];
    }

    if (applications.includes("testrails") && props.globalFilters?.integration_ids?.length > 0) {
      customFields = [...customFields, ...get(testrailsFieldsSelector, "data", [])];
    }

    return customFields;
  }, [
    azureFieldsSelector,
    jiraFieldsSelector,
    zendeskFieldsSelector,
    testrailsFieldsSelector,
    integrations,
    props.globalFilters?.integration_ids
  ]);

  const scmGlobalSettings = useMemo(() => {
    const SCM_GLOBAL_SETTING = globalSettingsState?.data?.records.find(
      (item: any) => item.name === "SCM_GLOBAL_SETTINGS"
    );
    return SCM_GLOBAL_SETTING
      ? typeof SCM_GLOBAL_SETTING?.value === "string"
        ? JSON.parse(SCM_GLOBAL_SETTING?.value)
        : SCM_GLOBAL_SETTING?.value
      : DEFAULT_SCM_SETTINGS_OPTIONS;
  }, [globalSettingsState]);

  // This function prepares filters for API request for graph data.
  const getFilters = (updateTimeFilters: boolean = true) => {
    return widgetApiFilters({
      widgetFilters,
      filters,
      hiddenFilters,
      globalFilters: props.globalFilters,
      reportType: props.reportType,
      contextFilters,
      updateTimeFilters,
      application,
      jiraOrFilters: props.jiraOrFilters,
      maxRecords,
      filterKey,
      widgetId: props.id,
      widgetMetaData: props.widgetMetaData,
      uri: props.uri,
      updatedUri: getWidgetUri(props.reportType, props.uri, filters, props.widgetMetaData),
      excludeStatusState,
      supportedCustomFields,
      dashboardMetaData: props.dashboardMetaData,
      scmGlobalSettings,
      availableIntegrations: integrations || [],
      dashboardOuIdsRef,
      queryParamOU,
      customFieldRecords: supportedCustomFields,
      tempWidgetInterval: tempWidgetInterval?.[props?.id] || undefined
    });
  };

  const widgetQueryToCompare = useCallback(
    (updateTimeFilters: boolean = true) => {
      const updated = { ...(getFilters(updateTimeFilters) || {}) };
      const ignoreKeys = ["metric", "metrics", "across_limit"];
      forEach(ignoreKeys, key => {
        unset(updated, ["filter", key]);
        unset(updated, [key]);
      });
      return updated;
    },
    [getFilters]
  );

  useEffect(() => {
    if (
      filters?.[filterKey] !== undefined &&
      [
        FileReports.JIRA_SALESFORCE_FILES_REPORT,
        FileReports.JIRA_ZENDESK_FILES_REPORT,
        FileReports.SCM_FILES_REPORT,
        FileReports.SCM_JIRA_FILES_REPORT
      ].includes(props.reportType as any)
    ) {
      if (localFilters?.[filterKey] !== filters?.[filterKey]) {
        setLocalFilters({
          [filterKey]: filters?.[filterKey]
        });
        console.log("[fetchDataAgain 1]");
        fetchDataAgain();
      }
    }
  }, [filters]);

  const getFiltersByChild = (child: any, updateTimeFilters = true) => {
    const type = child?.type;
    const childWigetFilters = get(widgetConstants, [type, "filters"], {});
    const childHiddenFilters = get(widgetConstants, [type, "hidden_filters"], {});
    const childURI = get(widgetConstants, [type, "uri"], {});
    const application = get(widgetConstants, [type, "application"], undefined);

    const combinedFilters = combineAllFilters(
      childWigetFilters || {},
      cloneDeep(child.query || {}),
      childHiddenFilters || {}
    );

    let finalFilters: { [x: string]: any } = {
      filter: {
        ...combinedFilters,
        ...props.globalFilters
      }
    };

    if (JENKINS_AZURE_REPORTS.includes(props.reportType)) {
      finalFilters = {
        ...finalFilters,
        filter: {
          ...finalFilters.filter,
          cicd_integration_ids: finalFilters.filter?.integration_ids
        },
        cicd_integration_ids: finalFilters.filter?.integration_ids
      };
    }

    finalFilters = convertChildKeysToSiblingKeys(finalFilters, "filter", [
      "across",
      "stacks",
      "sort",
      "interval",
      "filter_across_values"
    ]);

    // this check is added to fix the bug in old reports that are still sending trend in across
    // @ts-ignore
    if (scmCicdReportTypes.includes(type) && finalFilters.across === "trend") {
      finalFilters["across"] = "job_end";
    }

    if (updateTimeFilters) {
      finalFilters = updateIssueCreatedAndUpdatedFilters(finalFilters, child.metadata || {}, type, childURI);
    }

    if (application === ReportsApplicationType.ZENDESK) {
      finalFilters = customFieldFiltersSanitize(finalFilters, true);
    }

    if (["jira", "githubjira"].includes(application) && Object.keys(props.jiraOrFilters || {}).length > 0) {
      const key = application === IntegrationTypes.JIRA ? "or" : "jira_or";
      finalFilters = {
        ...finalFilters,
        filter: {
          ...(finalFilters.filter || {}),
          [key]: props.jiraOrFilters
        }
      };
    }

    const ou_ids = queryParamOU ? [queryParamOU] : get(props.dashboardMetaData, "ou_ids", []);

    if (ou_ids.length && APPLICATIONS_SUPPORTING_OU_FILTERS.includes(application)) {
      let combinedOUFilters = {
        ...get(props.dashboardMetaData, "ou_user_filter_designation", {}),
        ...sanitizeObject(get(child.metadata, "ou_user_filter_designation", {}))
      };

      const supportedApplications = getSupportedApplications(type);

      Object.keys(combinedOUFilters).forEach((key: string) => {
        if (!supportedApplications.includes(key)) {
          delete combinedOUFilters?.[key];
        }
      });

      if (["jira", "azure_devops", "githubjira"].includes(application)) {
        let sprint: string | undefined = "";

        if (azureIterationSupportableReports.includes(type)) {
          sprint = "sprint_report";
        } else {
          const sprintCustomField = supportedCustomFields.find((item: any) =>
            (item.name || "").toLowerCase().includes("sprint")
          );
          if (sprintCustomField) {
            sprint = sprintCustomField?.field_key;
          }
        }
        combinedOUFilters = {
          ...combinedOUFilters,
          sprint: [sprint]
        };
      }
      if (
        combinedOUFilters?.hasOwnProperty("sprint") &&
        (!combinedOUFilters?.sprint || !combinedOUFilters?.sprint?.[0])
      ) {
        delete combinedOUFilters?.sprint;
      }

      finalFilters = {
        ...finalFilters,
        ou_ids,
        ou_user_filter_designation: Object.keys(combinedOUFilters).length ? combinedOUFilters : undefined
      };
    }

    if (
      application === IntegrationTypes.JIRA &&
      finalFilters?.filter?.[ACTIVE_SPRINT_TYPE_FILTER_KEY] &&
      !JIRA_LEAD_TIME_REPORTS.includes(type)
    ) {
      finalFilters = {
        ...finalFilters,
        filter: { ...finalFilters.filter, sprint_states: finalFilters?.filter?.[ACTIVE_SPRINT_TYPE_FILTER_KEY] }
      };
      unset(finalFilters, ["filter", ACTIVE_SPRINT_TYPE_FILTER_KEY]);
    }

    finalFilters.filter = sanitizeStages(excludeStatusState, props.reportType, finalFilters.filter);

    if (props.reportType === LEVELOPS_MULTITIME_SERIES_REPORT) {
      finalFilters = { ...(finalFilters || {}), sort: [{ id: finalFilters?.across, desc: true }] };
    }

    finalFilters = {
      ...(finalFilters || {}),
      filter: {
        ...finalFilters.filter,
        ...updateTimeFiltersValue(dashboardMetaData || {}, child?.metadata || {}, { ...finalFilters?.filter })
      }
    };
    // unset(finalFilters, ["filter", WIDGET_DATA_SORT_FILTER_KEY]);
    return finalFilters;
  };

  const apiCallArray = useMemo(() => {
    return props.graphType.includes("composite")
      ? (props.children || [])
          .filter(child => child !== undefined && child.type !== undefined && child.type !== "")
          .map(child => ({
            id: child?.id,
            apiName: (widgetConstants as any)[child.type]?.uri,
            apiStatUri: (widgetConstants as any)[child.type]?.uri,
            apiMethod: (widgetConstants as any)[child.type]?.method,
            filters: getFiltersByChild(child),
            maxRecords: child?.max_records,
            localFilters: getFiltersByChild(child),
            sortBy: (widgetConstants as any)[child.type]?.chart_props?.sortBy,
            reportType: child?.type,
            apiData: undefined,
            childName: child?.name,
            composite: true,
            metadata: props.widgetMetaData,
            childMetaData: child?.metadata,
            isMultiTimeSeriesReport: child.isMultiTimeSeriesReport,
            isWidget: (widgetConstants as any)[child.type]?.uri ? true : false,
            dashboardMetaData: props.dashboardMetaData,
            supportedCustomFields: supportedCustomFields
          }))
      : [
          {
            id: props.id,
            apiName: getWidgetUri(props.reportType, props.uri, filters, props.widgetMetaData),
            apiStatUri: uri,
            apiMethod: props.method,
            filters: getFilters(),
            maxRecords: props.maxRecords,
            localFilters: props.filters,
            sortBy: props.chartProps && props.chartProps.sortBy,
            reportType: props.reportType,
            apiData: undefined,
            composite: false,
            metadata: props.widgetMetaData,
            isWidget: getWidgetUri(props.reportType, props.uri, filters, props.widgetMetaData) ? true : false,
            dashboardMetaData: props.dashboardMetaData,
            supportedCustomFields: supportedCustomFields,
            onUnmountClearData: getWidgetConstant(props.reportType, "onUnmountClearData", false)
          }
        ];
  }, [props.graphType, props.children, localFilters, props.filters, props.widgetMetaData, contextFilters, getFilters]);

  const [apiLoading, apiData, apisMetaData, loaded, apiErrorCode] = useApi(apiCallArray, [fetchData]);

  const customeDateTimeKeysFields: Array<string> = useSelector(customTimeFilterKeysSelector);

  const timeFilterKeys = useMemo(() => {
    return allTimeFilterKeys.concat(customeDateTimeKeysFields || []);
  }, [customeDateTimeKeysFields]);

  const manyReportData = useDataTransformMany(
    apiCallArray,
    apiData,
    reloadChartDependencies,
    customeDateTimeKeysFields
  );

  const manyReportMetaData = useMataDataTransformMany(apiCallArray, apiData, apisMetaData);
  useEffect(() => {
    filtersRef.current = getFilters(false);
    reportRef.current = props.reportType;
    maxRecordsRef.current = props.maxRecords;
    metadataRef.current = widgetMetaToCompare;
    filterApplyReloadRef.current = props.filterApplyReload;
    dashboardTimeFilterValueRef.current = props.dashboardMetaData.dashboard_time_range_filter;
    globalIntegrationIdsRef.current = props?.globalFilters?.integration_ids; // optional chaning added for integration 500 and other errors
    dashboardOuIdsRef.current = props.dashboardMetaData.ou_ids;
    if (props.children?.length) {
      let child_filters: any = {};
      forEach(props.children, child => (child_filters[child.id] = getFiltersByChild(child, false)));
      childFiltersRef.current = child_filters;
    }
  }, []);

  useEffect(() => {
    setWidgetLoading(props.id, !loaded);
  }, [loaded]);

  useEffect(() => {
    if (!apiLoading && apiData && props.reload) {
      props.setReload && props.setReload(false);
    }
    if (!apiLoading && !isEqual(dashboardOuIdsRef.current, dashboardOuIds)) {
      dashboardOuIdsRef.current = dashboardOuIds;
    }
  }, [apiLoading, apiData]);

  useEffect(() => {
    const allFilters = getFilters(false);
    if (!isEqual(allFilters, filtersRef.current) && !apiLoading) {
      filtersRef.current = allFilters;
      globalIntegrationIdsRef.current = props.globalFilters.integration_ids;
      console.log("[fetchDataAgain 2]");
      fetchDataAgain();
    }
  }, [
    filters,
    widgetFilters,
    hiddenFilters,
    contextFilters,
    dashboardMetaData,
    queryParamOU,
    apiLoading,
    childrenMaxRecords,
    tempWidgetInterval?.[props?.id]
  ]);

  useEffect(() => {
    let changed = false;
    props?.children?.forEach((child: any) => {
      const childFilters = getFiltersByChild(child, false);
      const childKey: any = Object.keys(childFiltersRef.current).find((item: any) => item === child.id);
      const refrenseChildFilters = childFiltersRef.current[childKey];
      if (!isEqual(refrenseChildFilters, childFilters) && !apiLoading) {
        changed = true;
      }
    });

    if (changed) {
      let child_filters: any = {};
      forEach(props.children, child => (child_filters[child.id] = getFiltersByChild(child, false)));
      childFiltersRef.current = child_filters;
      console.log("[fetchDataAgain 3]");
      fetchDataAgain();
    }
  }, [props.children]);

  useEffect(() => {
    if (!isEqual(props.reportType, reportRef.current) && !apiLoading) {
      reportRef.current = props.reportType;
      console.log("[fetchDataAgain 4]");
      fetchDataAgain();
    }
  }, [props.reportType]);

  useEffect(() => {
    if (!isEqual(props.filterApplyReload, filterApplyReloadRef.current) && !apiLoading) {
      filterApplyReloadRef.current = props.filterApplyReload;
      console.log("[fetchDataAgain 5]");
      fetchDataAgain();
    }
  }, [props.filterApplyReload]);

  //TODO: Need to change the reloading logic for non-api filters / FE filters
  useEffect(() => {
    const maxRecordsChanged = !isEqual(maxRecords, maxRecordsRef.current);
    const widgetMetadataChanged = !isEqual(widgetMetaToCompare, metadataRef.current);
    const childrenMaxRecordsChanged = !isEqual(props.childrenMaxRecords, childrenMaxRecords);
    const integrationIdChanged = !isEqual(props?.globalFilters?.integration_ids, globalIntegrationIdsRef?.current); // inetrgartion list 500 error or other error(400 etc), optional chaining added
    const oldMetrics = get(metadataRef.current, "metrics", undefined);
    const newMetrics = get(props.widgetMetaData, "metrics", undefined);
    const oldDashboardTimeRange = get(metadataRef.current, ["dashBoard_time_keys"], {});
    const newDashboardTimeRange = get(props.widgetMetaData, ["dashBoard_time_keys"], {});
    const dashbaordTimeEnabledDisabledChanged = !isEqual(oldDashboardTimeRange, newDashboardTimeRange);
    const velocityMetricsChanged =
      ALL_VELOCITY_PROFILE_REPORTS.includes(props.reportType as any) && !isEqual(oldMetrics, newMetrics);
    if (
      (maxRecordsChanged ||
        childrenMaxRecordsChanged ||
        (!velocityMetricsChanged &&
          !integrationIdChanged &&
          !dashbaordTimeEnabledDisabledChanged &&
          widgetMetadataChanged)) &&
      !apiLoading
    ) {
      maxRecordsRef.current = maxRecords;
      metadataRef.current = widgetMetaToCompare;
      globalIntegrationIdsRef.current = props.globalFilters.integration_ids;
      setChildrenMaxRecords(props.childrenMaxRecords);
      console.log("[fetchDataAgain 6]");
      // fetchDataAgain(); commented as it's leading to race condition for calling same redux saga  with same action triggered by fetchDataAgain 2 useEffect. It now added as part of fetchDataAgain 2 useEffect dependency
    }
  }, [maxRecords, props.widgetMetaData, props.childrenMaxRecords]);

  useEffect(() => {
    const dashboardTimeFilterEnableKeys = Object.keys(props.widgetMetaData.dashBoard_time_keys || {}).filter(
      (item: any) => props?.widgetMetaData?.dashBoard_time_keys?.[item]?.["use_dashboard_time"]
    );
    if (
      !isEqual(dashboardTimeFilterValue, dashboardTimeFilterValueRef.current) &&
      dashboardTimeFilterEnableKeys.length
    ) {
      console.log("[fetchDataAgain 7]");
      fetchDataAgain();
    }
  }, [dashboardTimeFilterValue]);

  const getChartType = () => {
    const filters = getFilters();
    const application = get(widgetConstants, [props.reportType, "application"], undefined);

    if (["jira_backlog_trend_report", "azure_backlog_trend_report"].includes(props.reportType)) {
      return backlogTrendReportChartType(props.widgetMetaData);
    }

    if (["resolution_time_report", "scm_issues_time_resolution_report"].includes(props.reportType)) {
      return resolutionTimeReportChartType(filters?.filter || {});
    }

    if (
      [
        JIRA_SPRINT_REPORTS.SPRINT_METRICS_PERCENTAGE_TREND,
        ISSUE_MANAGEMENT_REPORTS.SPRINT_METRICS_PERCENTAGE_TREND
      ].includes(props.reportType as any)
    ) {
      const type = get(filters, ["filter", "visualization"], "stacked_area");
      if (type === "line") {
        return ChartType.LINE;
      }
      return ChartType.AREA;
    }

    if (props.reportType === "tickets_report" || props.reportType === "azure_tickets_report") {
      let visualization = get(filters, ["filter", "visualization"], IssueVisualizationTypes.BAR_CHART);
      switch (visualization) {
        case IssueVisualizationTypes.DONUT_CHART:
          return ChartType.DONUT;
        case IssueVisualizationTypes.LINE_CHART:
          return ChartType.LINE;
        default:
          return props.chartType;
      }
    }
    if (props.reportType === SCM_PRS_REPORTS[1]) {
      const type = get(filters, ["filter", "visualization"], IssueVisualizationTypes.PIE_CHART);
      if ([IssueVisualizationTypes.BAR_CHART, IssueVisualizationTypes.STACKED_BAR_CHART].includes(type)) {
        return ChartType.BAR;
      } else if (type === IssueVisualizationTypes.LINE_CHART) {
        return ChartType.LINE;
      } else if ([IssueVisualizationTypes.AREA_CHART, IssueVisualizationTypes.STACKED_AREA_CHART].includes(type)) {
        return ChartType.AREA;
      }
      return ChartType.CIRCLE;
    }

    if (
      [JENKINS_REPORTS.SCM_PRS_RESPONSE_TIME_REPORT, JENKINS_REPORTS.SCM_PRS_REPORT].includes(props.reportType as any)
    ) {
      const type = get(props.widgetMetaData || {}, "visualization", SCMVisualizationTypes.CIRCLE_CHART);

      switch (type) {
        case SCMVisualizationTypes.CIRCLE_CHART:
          return ChartType.CIRCLE;
        case SCMVisualizationTypes.BAR_CHART:
          return ChartType.BAR;
        case SCMVisualizationTypes.LINE_CHART:
          return ChartType.LINE;
        case SCMVisualizationTypes.AREA_CHART:
        case SCMVisualizationTypes.STACKED_AREA_CHART:
          return ChartType.AREA;
        default:
          return ChartType.CIRCLE;
      }
    }

    const convertChartType = get(widgetConstants, [props.reportType, "convertChartType"], undefined);
    if (convertChartType) {
      const type = get(props.widgetMetaData || {}, "visualization", SCMVisualizationTypes.BAR_CHART);
      return convertChartType({ type });
    }

    if (filters && filters.hasOwnProperty("across")) {
      if (["stage_bounce_report", "azure_stage_bounce_report"].includes(props.reportType)) {
        return ChartType.STAGE_BOUNCE_CHART;
      } else if (
        ["issue_created", "issue_updated", "issue_resolved"].includes(filters.across || "") &&
        application === IntegrationTypes.JIRA &&
        props.chartType !== ChartType.STATS
      ) {
        if ("tickets_report" === props.reportType && ["quarter", "month", "week", "day"].includes(filters.interval)) {
          return ChartType.BAR;
        }
        return ChartType.LINE;
      } else if (
        ["workitem_created_at", "workitem_updated_at", "workitem_resolved_at", "trend"].includes(
          filters.across || ""
        ) &&
        application === IntegrationTypes.AZURE &&
        [
          ISSUE_MANAGEMENT_REPORTS.RESPONSE_TIME_REPORT,
          ISSUE_MANAGEMENT_REPORTS.HOPS_REPORT,
          ISSUE_MANAGEMENT_REPORTS.FIRST_ASSIGNEE_REPORT,
          ISSUE_MANAGEMENT_REPORTS.BOUNCE_REPORT
        ].includes(props.reportType as any) &&
        props.chartType !== ChartType.STATS
      ) {
        return ChartType.LINE;
      } else if (
        ["created", "updated"].includes(filters.across || "") &&
        application === IntegrationTypes.LEVELOPS &&
        props.chartType !== ChartType.STATS
      ) {
        return ChartType.LINE;
      } else if (
        ["trend"].includes(filters.across || "") &&
        application === IntegrationTypes.SNYK &&
        props.chartType !== ChartType.STATS
      ) {
        return ChartType.LINE;
      } else if (
        filters.across &&
        filters.across === "ticket_created" &&
        application === IntegrationTypes.ZENDESK &&
        props.chartType !== ChartType.STATS
      ) {
        if ("zendesk_tickets_report" === props.reportType && ["month", "week"].includes(filters.interval)) {
          return ChartType.BAR;
        }
        return ChartType.LINE;
      } else return props.chartType;
    } else return props.chartType;
  };

  const handleChartClick = (data: any, stackFilters?: string[]) => {
    if (OPEN_REPORTS_WITHOUT_DRILLDOWN.includes(props.reportType as any)) {
      const filters = getFilters();
      props.onChartClick?.(data, filters, stackFilters);
    } else {
      props.onChartClick?.(data, undefined, stackFilters);
    }
  };

  const getFilteredData = useCallback(
    (apiData: any, rawData: any) => {
      const filters = getFilters();
      const metadata = props.widgetMetaData;
      const statUri = uri;
      const apiName = getWidgetUri(props.reportType, props.uri, filters, props.widgetMetaData);
      const sortBy = props.chartProps && props.chartProps?.sortBy;
      const dashMeta = props.dashboardMetaData;
      if (Object.values(BULLSEYE_REPORTS).includes(props.reportType as any)) {
        if (props.reportType.includes("trend")) {
          return bullseyeTrendTransformer({
            reportType: props.reportType,
            filters,
            apiData: rawData?.[props.id],
            records: props.maxRecords,
            timeFilterKeys,
            supportedCustomFields
          });
        }

        return bullseyeDataTransformer({
          reportType: props.reportType,
          filters,
          apiData: rawData?.[props.id],
          records: props.maxRecords,
          timeFilterKeys,
          supportedCustomFields
        });
      }

      if (["jira_backlog_trend_report"].includes(props.reportType)) {
        return jiraBacklogTransformerWrapper({
          reportType: props.reportType,
          filters: props.filters,
          apiData: rawData?.[props.id],
          metadata: props.widgetMetaData,
          records: props.maxRecords,
          supportedCustomFields,
          timeFilterKeys,
          widgetFilters: filters
        });
      }

      if (props.reportType === "resolution_time_report") {
        return jiraResolutionTimeDataTransformer({
          reportType: props.reportType,
          filters,
          apiData: rawData?.[props.id],
          records: props.maxRecords,
          supportedCustomFields,
          timeFilterKeys,
          metadata,
          statUri,
          uri: apiName,
          sortBy,
          dashMeta
        });
      }

      if ([ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT].includes(props.reportType as any)) {
        return azureResolutionTimeDataTransformer({
          reportType: props.reportType,
          filters,
          apiData: rawData?.[props.id],
          records: props.maxRecords,
          metadata: props.widgetMetaData,
          widgetFilters: props.filters,
          timeFilterKeys,
          supportedCustomFields,
          statUri,
          uri: apiName,
          sortBy,
          dashMeta
        });
      }

      if (["jira_time_across_stages", ISSUE_MANAGEMENT_REPORTS.TIME_ACROSS_STAGES].includes(props.reportType)) {
        return timeAcrossStagesDataTransformer({
          reportType: props.reportType,
          filters,
          apiData: rawData?.[props.id],
          records: props.maxRecords,
          timeFilterKeys,
          supportedCustomFields
        });
      }

      if (props.reportType === "scm_issues_time_resolution_report") {
        return scmaResolutionTimeDataTransformer({
          reportType: props.reportType,
          filters,
          apiData: rawData?.[props.id],
          records: props.maxRecords,
          timeFilterKeys,
          supportedCustomFields
        });
      }

      return apiData;
    },
    [
      filters,
      props.reportType,
      props.widgetMetaData,
      props.maxRecords,
      timeFilterKeys,
      uri,
      supportedCustomFields,
      props.uri,
      props.chartProps,
      props.filters
    ]
  );

  const mappedData = useMemo(
    () => getFilteredData(manyReportData, apiData),
    [manyReportData, apiData, filters, props.reportType, props.maxRecords, getFilteredData]
  );

  const hasTrendLikeData = useMemo(() => {
    if (props.graphType.includes("composite")) {
      const allTimeAcrossKeys: string[] = [];
      forEach(props.children || [], child => {
        const filters = getFiltersByChild(child);
        if (allTimeFilterKeys.includes(filters?.across || "")) {
          allTimeAcrossKeys.push(filters?.across || "");
        }
      });
      return allTimeAcrossKeys.length > 0;
    }
    const allFilters = getFilters();
    return allTimeFilterKeys.includes(allFilters?.across || "");
  }, [getFilters, props.graphType, props.children, getFiltersByChild]);

  /* 
    TODO : Will come back later for the optimizations , 
    because of optimizations the data is not updating
    I have tried applying more and more dependencies but nothing
   */
  const chartProps = () => {
    const filters = getFilters();

    let chartProps = getProps(
      props.reportType,
      props.chartProps,
      manyReportData,
      filters,
      props.widgetMetaData,
      props.dashboardMetaData
    );

    let units: any[] = [];

    if (props.reportType === "tickets_counts_stat") {
      const statmetric = get(props.widgetMetaData, ["metrics"], "total_tickets");
      const newUnit = statmetric === "total_tickets" ? "Tickets" : "Story Points";
      chartProps = {
        ...chartProps,
        unit: newUnit
      };
    }

    if (props.graphType && props.graphType.includes("composite")) {
      apiCallArray.forEach((child: any) => {
        const unit = get(widgetConstants, [child.reportType, "chart_props", "unit"], "");
        if (!units.includes(unit) && unit !== "") {
          units.push(unit);
        }
      });
    }

    if (props.reportType === "sonarqube_code_complexity_trend_report") {
      let metric = Array.isArray(props.filters?.metrics) ? props.filters?.metrics[0] : props.filters?.metrics;
      if (metric) {
        for (let _metric of sonarQubeMetricsOptions) {
          if (_metric.value === metric) units.push(_metric.label);
        }
      }
    }

    if (props.graphType && props.graphType.includes("composite") && props.children && props.children.length > 0) {
      chartProps = props.children
        .filter(child => child !== undefined)
        .reduce((acc: any, next: any) => {
          const props = get(widgetConstants, [next._type, "chart_props"], {});
          return {
            ...acc,
            ...props
          };
        }, {});

      // combining all children widget props and parent props
      chartProps = {
        ...chartProps,
        chartProps: {
          ...chartProps.chartProps,
          margin: {
            ...(chartProps.chartProps?.margin || {}),
            ...(props.chartProps?.chartProps?.margin || {})
          }
        }
      };
    }

    if (props.reportType === LEVELOPS_MULTITIME_SERIES_REPORT) {
      chartProps = {
        ...chartProps,
        unit: "Tickets"
      };
    }

    if (props.chartType == ChartType.STATS) {
      const simplifyValue = get(widgetConstants, [props.reportType, SIMPLIFY_VALUE], false);
      chartProps = {
        ...chartProps,
        simplifyValue
      };
    }

    if (["jira_backlog_trend_report", "azure_backlog_trend_report"].includes(props.reportType)) {
      const leftYAxis = get(props.widgetMetaData, "leftYAxis", "total_tickets");
      let leftYAxisUnit = "Tickets";
      if (leftYAxis === "total_story_points") {
        leftYAxisUnit = "Story Points";
      }
      units = [leftYAxisUnit, "Days"];
      chartProps = getBacklogChartProps(chartProps, props.widgetMetaData, filters, mappedData);
    }

    if (props.reportType === PAGERDUTY_REPORT.RESPONSE_REPORTS) {
      units = ["Days", "Count"];
    }

    let tooltipTitle = undefined;
    let defaultPath = undefined;

    if (
      [
        "resolution_time_report",
        "scm_issues_time_resolution_report",
        ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT
      ].includes(props.reportType)
    ) {
      const metric = filters?.filter?.metric || [];
      let isTicket = false;
      let isDay = false;

      if (["issue_created", "issue_updated", "issue_resolved"].includes(filters?.across)) {
        tooltipTitle = filters?.interval || "day";
      }

      forEach(metric, (item: string) => {
        if (item.includes("tickets")) isTicket = true;
        else isDay = true;
      });
      if (isDay) {
        units.push("Days");
      }
      if (isTicket) {
        units.push("Tickets");
      }

      if (!isDay && !isTicket) {
        units.push("Days", "Tickets");
      }
    }

    if (
      [
        FileReports.JIRA_SALESFORCE_FILES_REPORT,
        FileReports.JIRA_ZENDESK_FILES_REPORT,
        FileReports.SCM_FILES_REPORT,
        FileReports.SCM_JIRA_FILES_REPORT
      ].includes(props.reportType as any)
    ) {
      defaultPath = get(filters, ["filter", filterKey], "");
    }

    if (ALL_VELOCITY_PROFILE_REPORTS.includes(props.reportType as any)) {
      const hideStages = get(props.widgetMetaData, ["hide_stages"], undefined);
      const metrics = get(props.widgetMetaData, ["metrics"], "mean");
      chartProps = {
        ...chartProps,
        hideKeys: hideStages,
        dataKey: metrics
      };
    }

    if (LEAD_TIME_MTTR_REPORTS.includes(props.reportType as any)) {
      const hideStages = get(props.widgetMetaData, ["hide_stages"], undefined);
      const metrics = get(props.widgetMetaData, ["metrics"], "mean");
      chartProps = {
        ...chartProps,
        hideKeys: hideStages,
        dataKey: metrics
      };
    }

    if (
      ["scm_repos_report", "scm_committers_report", scmTableReportType.SCM_FILE_TYPES_REPORT].includes(props.reportType)
    ) {
      const metrics = get(props.widgetMetaData, "metrics", ["num_commits", "num_prs", "num_changes"]);
      let columns = getSCMColumnsForMetrics(metrics);
      if (columns.some((col: any) => ["num_jira_issues", "num_workitems"].includes(col.dataIndex))) {
        const newColumns = [
          ...columns,
          baseColumnConfig("Number of Workitems", "num_workitems", { sorter: true, align: "center" })
        ];
        const applications = (integrations || [])?.map((item: any) => item.application);
        if (applications.includes(IssueManagementOptions.JIRA) && applications.includes(IssueManagementOptions.AZURE)) {
          columns = newColumns;
        } else if (applications.includes(IssueManagementOptions.JIRA)) {
          newColumns.push(baseColumnConfig("Number of Issues", "num_jira_issues", { sorter: true, align: "center" }));
          columns = newColumns.filter((item: any) => item.dataIndex !== "num_workitems");
        } else if (applications.includes(IssueManagementOptions.AZURE)) {
          newColumns.push(baseColumnConfig("Number of Workitems", "num_workitems", { sorter: true, align: "center" }));
          columns = newColumns.filter((item: any) => item.dataIndex !== "num_jira_issues");
        } else
          columns = newColumns.filter((item: any) => !["num_jira_issues", "num_workitems"].includes(item.dataIndex));
      }

      if (!props.chartClickEnable && columns.length) {
        columns = columns.map((item: any) => {
          const newItem = { ...item };
          if (newItem?.sorter) {
            unset(newItem, ["sorter"]);
          }
          return newItem;
        });
      }
      chartProps = {
        ...chartProps,
        columns: uniqBy(
          [
            props.reportType === "scm_repos_report"
              ? repoColumn
              : props.reportType === "scm_committers_report"
              ? committerColumn
              : fileTypeColumn,
            ...columns
          ],
          "dataIndex"
        )
      };
    }
    const { reportType, widgetMetaData } = props;
    const getWidgetChartProps = get(widgetConstants, [reportType, GET_WIDGET_CHART_PROPS], undefined);

    if (getWidgetChartProps) {
      chartProps = { ...chartProps, ...getWidgetChartProps({ filters, metadata: widgetMetaData }) };
    }
    const getDynamicColumns: any = get(widgetConstants, [reportType, "getDynamicColumns"], undefined);
    if (getDynamicColumns) {
      const dynamicColumns = getDynamicColumns((apiData as any)?.[props?.id] || [], (contextFilters as any)[props.id]);
      chartProps = { ...chartProps, columns: [...chartProps?.columns, ...dynamicColumns] };
    }

    if ([...Object.values(JIRA_SPRINT_REPORTS), ...Object.values(AZURE_SPRINT_REPORTS)].includes(reportType as any)) {
      units = [get(filters, ["filter", "view_by"], "Points")];
      chartProps = {
        ...chartProps,
        unit: get(filters, ["filter", "view_by"], "Points")
      };
    }

    const widgetTableColumn: any = get(widgetConstants, [reportType, "widgetTableColumn"], undefined);
    if (widgetTableColumn && widgetTableColumn.length > 0) {
      chartProps = { ...chartProps, columns: [...widgetTableColumn] };
    }

    return {
      id: props.id,
      ...chartProps,
      xUnit: filters?.across || "",
      hasClickEvents: props.chartClickEnable,
      onClick: handleChartClick,
      units: units,
      showStaticLegends: get(widgetConstants, [reportType, "chart_props"], ""),
      reportType: props.reportType,
      tooltipTitle,
      defaultPath,
      ...mappedData,
      previewOnly: !!props.previewOnly,
      hideLegend: !!props.hideLegend,
      hasTrendLikeData,
      apisMetaData,
      apiData,
      widgetMetaData: props.widgetMetaData,
      metaData: manyReportMetaData,
      ou_id: queryParamOU
    };
  };

  const renderLoading = useMemo(() => {
    if (!apiLoading) {
      return null;
    }
    return <Loader />;
  }, [apiLoading]);

  const errorDesc = useMemo(
    () => getServerErrorDesc(graphType === WidgetType.STATS ? ServerErrorSource.STAT_WIDGET : ServerErrorSource.WIDGET),
    [graphType]
  );

  const renderData = useMemo(() => {
    if (apiLoading || !apiData) {
      return null;
    }

    const keysCount = Object.keys(apiData).length;
    const hasChildren = !!(props.children || []).length;
    const emptyWidgetPreview: (args: EmptyWidgetPreviewArgsType) => ReactNode = getWidgetConstant(
      props.reportType,
      "render_empty_widget_preview_func",
      undefined
    );

    if (apiErrorCode && !!emptyWidgetPreview) {
      return emptyWidgetPreview({ errorCode: apiErrorCode as number });
    }

    if (apiErrorCode && (apiErrorCode >= ERROR_CODE_RANGE_START || apiErrorCode === REQUEST_TIMEOUT_ERROR)) {
      logToBugsnag(
        `Failed to load ${props?.reportType ? props.reportType : "widget"}`,
        severityTypes.ERROR,
        issueContextTypes.WIDGETS,
        {
          error_code: apiErrorCode,
          filters: props?.filters || {},
          widgetId: props?.id
        }
      );

      return (
        <EmptyApiErrorWidget
          description={errorDesc}
          className={graphType === WidgetType.STATS ? "stats-error-container" : ""}
        />
      );
    }

    if (keysCount === 0) {
      return <EmptyWidget className={props.chartType === ChartType.STATS ? "empty-stat" : ""} />;
    }

    if (keysCount > 0) {
      if (!hasChildren) {
        if (
          [
            FileReports.JIRA_SALESFORCE_FILES_REPORT,
            FileReports.JIRA_ZENDESK_FILES_REPORT,
            FileReports.SCM_FILES_REPORT,
            FileReports.SCM_JIRA_FILES_REPORT
          ].includes(props.reportType as any) &&
          get(props.widgetMetaData, [getGroupByRootFolderKey(props.reportType)], false)
        ) {
          return <ChartContainer chartType={ChartType.GRID_VIEW} chartProps={chartProps()} />;
        }
        if (
          [
            FileReports.JIRA_SALESFORCE_FILES_REPORT,
            FileReports.JIRA_ZENDESK_FILES_REPORT,
            FileReports.SCM_FILES_REPORT,
            FileReports.SCM_JIRA_FILES_REPORT
          ].includes(props.reportType as any) &&
          !get(props.widgetMetaData, [getGroupByRootFolderKey(props.reportType)], false)
        ) {
          return <ChartContainer chartType={ChartType.TREEMAP} chartProps={chartProps()} />;
        }
        //@ts-ignore
        return <ChartContainer chartType={getChartType() as any} chartProps={chartProps()} />;
      }
      return (
        <ChartContainer
          chartType={
            props.reportType === LEVELOPS_MULTITIME_SERIES_REPORT ? ChartType.MULTI_TIME_SERIES : ChartType.COMPOSITE
          }
          chartProps={chartProps()}
        />
      );
    }
  }, [
    apiLoading,
    apiData,
    apiErrorCode,
    props.children,
    manyReportData,
    localFilters,
    filters,
    chartProps,
    props.reportType
  ]);

  return (
    <div className="widget-api-container">
      {renderLoading}
      {renderData}
    </div>
  );
};
export default WidgetApiContainer;
