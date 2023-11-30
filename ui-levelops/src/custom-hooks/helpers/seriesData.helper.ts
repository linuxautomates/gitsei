import { getStacksData } from "custom-hooks/helpers/helper";
import { getBullseyeDataKeyValue, getJiraDataKeyValue, getSCMDataKeyValue } from "dashboard/constants/helper";
import { ceil, cloneDeep, forEach, get, groupBy, isArray, map, mergeWith, unset } from "lodash";
import {
  CUSTOM_FIELD_NAME_STORY_POINTS,
  CUSTOM_STACK_FILTER_NAME_MAPPING,
  DEFAULT_MAX_RECORDS
} from "dashboard/constants/constants";
import widgetConstants, { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { ALLOWED_WIDGET_DATA_SORTING } from "dashboard/constants/filter-name.mapping";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { convertEpochToDate, convertTimeData, convertToDate, DateFormats } from "utils/dateUtils";
import { fixedNDecimalPlaces, timeDurationGenericTransform } from "./helper";
import { trendReportTransformer } from "./trendReport.helper";
import { SCM_PRS_TIME_FILTERS_KEYS } from "constants/filters";
import { JENKINS_REPORTS } from "dashboard/constants/applications/names";
import { SCMReworkVisualizationTypes } from "dashboard/constants/typeConstants";
import { idFilters } from "dashboard/reports/jira/commonJiraReports.constants";
import moment from "moment";
import { CustomTimeBasedTypes } from "dashboard/graph-filters/components/helper";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { convertToDays } from "utils/timeUtils";

const SONARQUBE_REPORTS = ["sonarqube_metrics_report", "sonarqube_code_complexity_report"];

export const genericSeriesDataTransformer = (
  apiData: any,
  records: any,
  sortBy: any,
  reportType: string,
  reverseCriteria: any = {},
  filters: any,
  supportedCustomFields?: any
) => {
  const getTotalKey = getWidgetConstant(reportType, ["getTotalKey"]);
  const across = get(filters, ["across"], undefined);
  const interval = get(filters, ["interval"], undefined);
  const metric = get(filters, ["metric"], undefined);
  const xAxisIgnoreSortKeys = get(widgetConstants, [reportType, "chart_props", "xAxisIgnoreSortKeys"], []);
  const xAxisLabelKey = get(widgetConstants, [reportType, "chart_props", "xAxisLabelKey"], []);
  let CustomFieldType: any = undefined;
  if (across?.includes("customfield_")) {
    CustomFieldType = supportedCustomFields?.find((item: any) => across === item?.field_key)?.field_type;
  }
  let seriesData =
    apiData && apiData.length
      ? apiData.map((item: any) => {
          let name = item.key;
          const xAxisLabelTransform = getWidgetConstant(reportType, ["xAxisLabelTransform"]);
          name = xAxisLabelTransform?.({ item, interval, across, CustomFieldType, xAxisLabelKey }) || name;
          let result = {
            ...item,
            name: name || ""
          };
          if (item.stacks) {
            const totalKey = getTotalKey?.({ metric }) || "count";
            const stackedData = getStacksData(item.stacks, filters, reportType, totalKey);

            result = {
              key: item.key,
              name: name || "",
              ...stackedData
            };

            if (["stage_bounce_report", "azure_stage_bounce_report"].includes(reportType)) {
              result = {
                ...result,
                stage: item?.stage || []
              };
            }
          }

          return result;
        })
      : [];

  const maxRecords = Math.min(records || DEFAULT_MAX_RECORDS, seriesData.length);

  const getShouldSliceFromEnd = getWidgetConstant(reportType, ["shouldSliceFromEnd"]);
  const shouldSliceFromEnd = getShouldSliceFromEnd?.({ reportType });
  let slice_start = seriesData?.length > 0 && shouldSliceFromEnd ? seriesData.length - maxRecords : 0;
  const slice_end =
    seriesData?.length > 0 && shouldSliceFromEnd ? seriesData.length : maxRecords || DEFAULT_MAX_RECORDS;
  if (slice_start < 0) slice_start = 0;

  let getSortFromConstant = getWidgetConstant(reportType, ["getSortKey"]);
  let _sortBy = getSortFromConstant?.(reverseCriteria) || sortBy;

  if (!xAxisIgnoreSortKeys.includes(across)) {
    const _sortOrder = getWidgetConstant(reportType, ["getSortOrder"]) || "desc";
    if (_sortOrder === "desc") {
      seriesData = seriesData.sort((a: any, b: any) => (b[_sortBy] || 0) - (a[_sortBy] || 0));
    } else {
      seriesData = seriesData.sort((a: any, b: any) => (a[_sortBy] || 0) - (b[_sortBy] || 0));
    }
  }

  seriesData = seriesData.slice(slice_start, slice_end);
  const getShouldReverseApiData = getWidgetConstant(reportType, ["shouldReverseApiData"]);
  const shouldReverseApiData = getShouldReverseApiData?.(reverseCriteria);
  if (shouldReverseApiData) {
    seriesData.reverse();
  }
  return seriesData;
};

export const azureSeriesDataTransformerWrapper = (data: any) => {
  const { widgetFilters, apiData } = data;
  const { across } = widgetFilters;
  const newApiData = get(apiData, ["0", across, "records"], []);
  return seriesDataTransformer({ ...data, apiData: newApiData });
};

export const seriesDataTransformer = (data: any) => {
  const {
    records,
    sortBy,
    reportType,
    metadata,
    filters: widgetFilters,
    isMultiTimeSeriesReport,
    supportedCustomFields
  } = data;
  let { apiData, timeFilterKeys } = data;
  const application = get(widgetConstants, [reportType, "application"], undefined);
  const convertTo = get(widgetConstants, [reportType, "convertTo"], undefined);
  const across = get(widgetFilters, ["across"], undefined);
  let stack_by = get(widgetFilters, ["stacks"], undefined);
  const interval = get(widgetFilters, ["interval"], undefined);
  const labels = get(widgetFilters, ["filter", "labels"], undefined);

  if (stack_by && Array.isArray(stack_by)) {
    stack_by = stack_by[0];
  }

  if (labels?.length && across === "label") {
    apiData = apiData?.filter((filters: any) => labels.includes(filters.key));
  }

  if (
    application === IntegrationTypes.JIRA &&
    across &&
    ["issue_created", "issue_updated", "issue_resolved"].includes(across)
  ) {
    return trendReportTransformer({ apiData, reportType, widgetFilters: data.widgetFilters, timeFilterKeys });
  }

  if (
    application === IntegrationTypes.AZURE &&
    reportType !== "azure_backlog_trend_report" &&
    across &&
    ["workitem_created_at", "workitem_updated_at", "workitem_resolved_at", "trend"].includes(across)
  ) {
    return trendReportTransformer({ apiData, reportType, widgetFilters: data.widgetFilters, timeFilterKeys });
  }

  if (reportType === "zendesk_tickets_report" && across && across === "ticket_created" && interval === "day") {
    return trendReportTransformer({ apiData, reportType, widgetFilters, metadata, timeFilterKeys });
  }

  if (application === IntegrationTypes.LEVELOPS && across && ["created", "updated"].includes(across)) {
    return trendReportTransformer({ apiData, reportType, timeFilterKeys });
  }

  if (["jira_salesforce_escalation_time_report", "jira_zendesk_escalation_time_report"].includes(reportType)) {
    return trendReportTransformer({ apiData, reportType, timeFilterKeys });
  }

  if (SONARQUBE_REPORTS.includes(reportType) && apiData) {
    const igonreKeys = ["max", "min", "median"];
    apiData = apiData.map((item: any) => {
      return Object.keys(item).reduce((acc: any, next: any) => {
        if (igonreKeys.includes(next)) {
          delete item[next];
          return { ...acc };
        }
        return { ...acc, [next]: item[next] };
      }, {});
    });
  }

  let seriesData = genericSeriesDataTransformer(
    apiData,
    records,
    sortBy,
    reportType,
    {},
    widgetFilters,
    supportedCustomFields
  );

  if (convertTo) {
    seriesData = convertTimeData(seriesData, convertTo);
  }

  return {
    data: seriesData
  };
};

export const backlogSeriesDataTransformer = (data: any) => {
  const { records, sortBy, reportType, metadata, widgetFilters, isMultiTimeSeriesReport, supportedCustomFields } = data;
  let { apiData, timeFilterKeys } = data;

  const application = get(widgetConstants, [reportType, "application"], undefined);
  const convertTo = get(widgetConstants, [reportType, "convertTo"], undefined);
  const across = get(widgetFilters, ["across"], undefined);
  let stack_by = get(widgetFilters, ["stacks"], undefined);
  const labels = get(widgetFilters, ["filter", "labels"], undefined);
  if (stack_by && Array.isArray(stack_by)) {
    stack_by = stack_by[0];
  }
  const stackValue =
    stack_by === "custom_field" ? get(widgetFilters, ["filter", "custom_stacks", 0], undefined) : stack_by;
  const customFieldType = supportedCustomFields?.find((field: any) => stackValue === field?.field_key)?.field_type;

  if (labels?.length && across === "label") {
    apiData = apiData?.filter((filters: any) => labels.includes(filters.key));
  }

  if (
    application === IntegrationTypes.JIRA &&
    across &&
    ["issue_created", "issue_updated", "issue_resolved"].includes(across)
  ) {
    return trendReportTransformer({ apiData, reportType, widgetFilters, timeFilterKeys });
  }

  const leftYAxis = get(metadata, "leftYAxis", "total_tickets");
  const rightYAxis = get(metadata, "rightYAxis", "median");
  const metrics = [leftYAxis, rightYAxis];
  apiData = (apiData || []).map((item: any) => {
    const { stacks } = item;
    const _item = Object.keys(item).reduce((acc: any, next: any) => {
      if (metrics.includes(next)) {
        return { ...acc, [next]: item[next] };
      }
      return acc;
    }, {});

    let stackData = {};
    if (stacks) {
      let stackedTicketsTotal = 0;
      let stackedTicketsOtherTotal = stacks.slice(10, stacks.length).reduce((acc: number, obj: any) => {
        acc = acc + obj[leftYAxis];
        return acc;
      }, 0);

      stackData = stacks
        .sort((a: any, b: any) => b[leftYAxis] - a[leftYAxis])
        .slice(0, 10)
        .reduce((acc: any, obj: any) => {
          // if key = "" then replace it with UNKNOWN
          if (idFilters.includes(stack_by)) {
            acc[obj.additional_key || "UNKNOWN"] = obj[leftYAxis];
            stackedTicketsTotal += obj[leftYAxis];
            return acc;
          }
          let newLabel = obj.key;
          if (CustomTimeBasedTypes.includes(customFieldType)) {
            newLabel = moment(parseInt(obj.key)).format("DD-MM-YYYY HH:mm:ss");
          }
          acc[newLabel || "UNKNOWN"] = obj[leftYAxis];
          stackedTicketsTotal += obj[leftYAxis];
          return acc;
        }, {});
      const missingTickets =
        stackedTicketsOtherTotal + Math.max(item[leftYAxis] - (stackedTicketsTotal + stackedTicketsOtherTotal), 0);
      const missingTicketsStackKey =
        (metadata?.[CUSTOM_STACK_FILTER_NAME_MAPPING] || "").toLowerCase() === CUSTOM_FIELD_NAME_STORY_POINTS
          ? "Unestimated"
          : "Other";
      if (missingTickets > 0) {
        stackData = {
          ...stackData,
          [missingTicketsStackKey]: missingTickets
        };
      }
    }

    const name = convertEpochToDate(item.key, DateFormats.DAY, true);
    const mappedItem: any = {
      ..._item,
      ...stackData,
      name,
      key: item.key,
      additional_key: item.additional_key
    };
    if (isMultiTimeSeriesReport) {
      mappedItem["timestamp"] = item.key;
    }
    return mappedItem;
  });

  let seriesData = genericSeriesDataTransformer(
    apiData,
    records,
    sortBy,
    reportType,
    {},
    widgetFilters,
    supportedCustomFields
  );

  if (convertTo) {
    seriesData = convertTimeData(seriesData, convertTo);
  }

  return {
    data: seriesData
  };
};

export const sonarQubeDuplicatiionBubbleChartTransformer = (data: any) => {
  let { apiData } = data;
  let seriesData = bubbleChartData(apiData);
  return {
    data: seriesData
  };
};

export const bubbleChartData = (apiData: any) => {
  return apiData && apiData.length
    ? apiData.map((item: any) => {
        return {
          name: item.key || "",
          yAxis: item.duplicated_density,
          xAxis: item.total,
          zAxis: item.duplicated_density * item.total
        };
      })
    : [];
};

export const tableTransformer = (data: any) => {
  const { apiData } = data;
  return { data: apiData };
};

export const scmIssueFirstResponseReport = (data: any) => {
  const { apiData, records, sortBy, reportType, widgetFilters } = data;

  let seriesData = genericSeriesDataTransformer(apiData, records, sortBy, reportType, {}, widgetFilters);

  seriesData = seriesData.map((item: any) => {
    const { count, sum, ...data } = item;
    return {
      min: convertToDays(item.min),
      median: convertToDays(item.median),
      max: convertToDays(item.max),
      average: convertToDays((item.count || 0) === 0 ? 0 : item.sum / item.count),
      name: item?.additional_key?.replace(/_/g, " ") || item.key.replace(/_/g, " "),
      key: item.key || item?.additional_key
    };
  });

  return {
    data: seriesData
  };
};

export const testrailsTransformer = (data: any) => {
  const { apiData, reportType, widgetFilters } = data;

  let _data = apiData || [];

  const stacks = get(widgetFilters, ["stacks"], undefined);

  if (stacks && stacks.length) {
    _data = _data.map((i: any) => {
      const { key, stacks: _stacks } = i;
      let stackedData = {};
      if (["testrails_tests_estimate_forecast_report", "testrails_tests_estimate_report"].includes(reportType)) {
        stackedData = _stacks.reduce((acc: any, i: any) => {
          return { ...acc, [`${i.key}-min`]: i.min, [`${i.key}-median`]: i.median, [`${i.key}-max`]: i.max };
        }, {});
      } else {
        stackedData = (_stacks || [])?.reduce((acc: any, i: any) => ({ ...acc, [i.key]: i.total_tests }), {});
      }
      return {
        name: key,
        ...stackedData
      };
    });
  } else {
    _data = _data.map((i: any) => {
      return {
        name: i.key,
        ...i
      };
    });
  }

  return { data: _data };
};

export const bullseyeDataTransformer = (data: any) => {
  const { apiData, records, sortBy, reportType, filters } = data;

  let seriesData = genericSeriesDataTransformer(apiData, records, sortBy, reportType, {}, filters);

  let dataKey = get(widgetConstants, [reportType, "dataKey"], undefined);
  if (filters?.filter?.metric) {
    dataKey = filters?.filter?.metric;
  }

  if (dataKey) {
    seriesData = (seriesData || []).map((item: any) => {
      if (isArray(dataKey)) {
        let newObj = {};
        forEach(dataKey, (key: string) => {
          newObj = {
            ...newObj,
            [key]: getBullseyeDataKeyValue(item, key)
          };
        });
        return { ...newObj, name: item.name || "" };
      } else {
        return {
          name: item.name || "",
          [dataKey]: getBullseyeDataKeyValue(item, dataKey)
        };
      }
    });
  }

  return { data: seriesData };
};

const getTransformedName = (current: string, payload: any) => {};

const metricAndGraphKeyMapping = {
  median_resolution_time: "median",
  number_of_tickets_closed: "total_tickets",
  "90th_percentile_resolution_time": "p90",
  average_resolution_time: "mean"
};

const getMappedAzureResolutionTimeData = (item: any, key: string | string[], stacks: string[]) => {
  const barKeys = ["median_resolution_time", "90th_percentile_resolution_time", "average_resolution_time"];
  let mappedData: any = { name: item.name || "", key: item.key || "" };
  const getStackData = (stacks: any[], currentkey: string) => {
    let stackedData: any = {};
    forEach(stacks, stack => {
      const stackDataKey = `${currentkey}^__${stack.additional_key || stack.key || stack.name || ""}^__${
        barKeys.includes(currentkey) ? "bar" : "line"
      }`;
      stackedData[stackDataKey] = barKeys.includes(currentkey)
        ? fixedNDecimalPlaces(stack[(metricAndGraphKeyMapping as any)[currentkey]] / 86400, 2)
        : stack[(metricAndGraphKeyMapping as any)[currentkey]];
      stackedData[currentkey] = barKeys.includes(currentkey)
        ? fixedNDecimalPlaces(stack[(metricAndGraphKeyMapping as any)[currentkey]] / 86400, 2)
        : stack[(metricAndGraphKeyMapping as any)[currentkey]];
    });
    return stackedData;
  };

  if (isArray(key)) {
    forEach(key, dataKey => {
      mappedData = {
        ...(mappedData || {}),
        stacked: !!((item.stacks || []).length > 0),
        stacks,
        ...getStackData(item.stacks || [], dataKey)
      };

      if (!mappedData.stacked) {
        const mappedDataKey = `${dataKey}^__${item.name}^__${barKeys.includes(dataKey) ? "bar" : "line"}`;
        mappedData = {
          ...(mappedData || {}),
          [mappedDataKey]: barKeys.includes(dataKey)
            ? fixedNDecimalPlaces(item[(metricAndGraphKeyMapping as any)[dataKey]] / 86400, 2)
            : item[(metricAndGraphKeyMapping as any)[dataKey]],
          [dataKey]: barKeys.includes(dataKey)
            ? fixedNDecimalPlaces(item[(metricAndGraphKeyMapping as any)[dataKey]] / 86400, 2)
            : item[(metricAndGraphKeyMapping as any)[dataKey]]
        };
      }
    });
  } else {
    mappedData = {
      ...(mappedData || {}),
      stacked: !!((item.stacks || []).length > 0),
      stacks,
      ...getStackData(item.stacks || [], key)
    };
    if (!mappedData.stacked) {
      const mappedDataKey = `${key}^${item.name}^${barKeys.includes(key) ? "bar" : "line"}`;
      mappedData = {
        ...(mappedData || {}),
        [mappedDataKey]: fixedNDecimalPlaces(item[(metricAndGraphKeyMapping as any)[key]] / 86400, 2),
        [key]: fixedNDecimalPlaces(item[(metricAndGraphKeyMapping as any)[key]] / 86400, 2)
      };
    }
  }

  return mappedData;
};

export const azureResolutionTimeDataTransformer = (data: any) => {
  const { records, filters, sortBy, reportType, isMultiTimeSeriesReport, timeFilterKeys } = data;
  const across = get(filters, ["across"], undefined);
  const graphType = get(filters, ["filter", "graph_type"], ChartType.BAR);
  const interval = get(filters, ["interval"], undefined);
  const labels = get(filters, ["filter", "labels"], undefined);
  let dataKey = get(widgetConstants, [reportType, "dataKey"], undefined);
  let apiData = get(data, ["apiData", 0, across, "records"], []);
  if (labels?.length && across === "label") {
    apiData = apiData?.filter((filters: any) => labels.includes(filters.key));
  }
  let seriesData = apiData.map((item: any) => {
    const newItem = cloneDeep(item);
    unset(newItem, "stacks");
    return newItem;
  });

  if (graphType !== ChartType.LINE) {
    seriesData = genericSeriesDataTransformer(seriesData, records, sortBy, reportType, { interval, across }, filters);
  }

  if (filters?.filter?.metric) {
    dataKey = filters?.filter.metric;
  }
  if (dataKey) {
    seriesData = (seriesData || []).map((item: any, i: number) => {
      let mappedItem: any = item;
      let name = item.additional_key || item.name;
      if (
        [
          "issue_created",
          "issue_updated",
          "issue_resolved",
          "workitem_created_at",
          "workitem_updated_at",
          "workitem_resolved_at",
          "trend"
        ].includes(across)
      ) {
        name = convertToDate(item.key);
        const xAxisLabelTransform = getWidgetConstant(reportType, ["xAxisLabelTransform"]);
        name = xAxisLabelTransform?.({ item, interval, across }) || name;
      }
      const xAxisLabelTransform = getWidgetConstant(reportType, ["xAxisLabelTransform"]);
      name = xAxisLabelTransform?.({ item, interval, across }) || name;
      mappedItem = { ...item, name: name || "", stacks: apiData?.[i]?.stacks || [], key: item?.key };
      mappedItem = getMappedAzureResolutionTimeData(mappedItem, dataKey, filters?.stacks || []);
      if (isMultiTimeSeriesReport) {
        mappedItem["timestamp"] = item?.key;
      }
      return mappedItem;
    });
  }

  const allowedWidgetDataSorting = get(widgetConstants, [reportType, ALLOWED_WIDGET_DATA_SORTING], false);
  const sortKey = get(filters, ["sort", 0, "id"], "");

  if (allowedWidgetDataSorting && (timeFilterKeys || []).includes(sortKey)) {
    const sortValue = get(filters, ["filter", "sort_xaxis"], "");
    sortValue.includes("old-latest") && seriesData.reverse();
  }

  return { data: seriesData };
};

export const jiraResolutionTimeDataTransformer = (data: any) => {
  const { records, filters, sortBy, reportType, isMultiTimeSeriesReport, timeFilterKeys, supportedCustomFields } = data;
  let { apiData } = data;
  const across = get(filters, ["across"], undefined);
  const graphType = get(filters, ["filter", "graph_type"], ChartType.BAR);
  const interval = get(filters, ["interval"], undefined);
  const labels = get(filters, ["filter", "labels"], undefined);
  const xAxisIgnoreSortKeys = get(widgetConstants, [reportType, "chart_props", "xAxisIgnoreSortKeys"], []);
  const getTotalKey = getWidgetConstant(reportType, ["getTotalKey"]);
  let dataKey = get(widgetConstants, [reportType, "dataKey"], undefined);
  let CustomFieldType: any = undefined;
  if (across?.includes("customfield_")) {
    CustomFieldType = supportedCustomFields?.find((item: any) => across === item?.field_key)?.field_type;
  }
  if (labels?.length && across === "label") {
    apiData = apiData?.filter((filters: any) => labels.includes(filters.key));
  }

  let seriesData = apiData;
  if (graphType !== ChartType.LINE) {
    const xAxisLabelKey = get(widgetConstants, [reportType, "chart_props", "xAxisLabelKey"], []);
    let CustomFieldType: any = undefined;
    if (across?.includes("customfield_")) {
      CustomFieldType = supportedCustomFields?.find((item: any) => across === item?.field_key)?.field_type;
    }
    seriesData =
      seriesData && seriesData.length
        ? seriesData.map((item: any) => {
            let name = item.key;
            const xAxisLabelTransform = getWidgetConstant(reportType, ["xAxisLabelTransform"]);
            name = xAxisLabelTransform?.({ item, interval, across, CustomFieldType, xAxisLabelKey }) || name;
            let result = {
              ...item,
              name: name || ""
            };
            if (item.stacks) {
              const totalKey = getTotalKey?.({ metric }) || "count";
              const stackedData = getStacksData(item.stacks, filters, reportType, totalKey);

              result = {
                key: item.key,
                name: name || "",
                ...stackedData
              };

              if (["stage_bounce_report", "azure_stage_bounce_report"].includes(reportType)) {
                result = {
                  ...result,
                  stage: item?.stage || []
                };
              }
            }

            return result;
          })
        : [];
  }
  const maxRecords = Math.min(records || DEFAULT_MAX_RECORDS, seriesData.length);

  const getShouldSliceFromEnd = getWidgetConstant(reportType, ["shouldSliceFromEnd"]);
  const shouldSliceFromEnd = getShouldSliceFromEnd?.({ reportType });
  let slice_start = seriesData?.length > 0 && shouldSliceFromEnd ? seriesData.length - maxRecords : 0;
  const slice_end =
    seriesData?.length > 0 && shouldSliceFromEnd ? seriesData.length : maxRecords || DEFAULT_MAX_RECORDS;
  if (slice_start < 0) slice_start = 0;
  seriesData = seriesData.slice(slice_start, slice_end);
  const metric = filters?.filter?.metric ?? filters?.metric;

  if (metric) {
    dataKey = metric;
  }
  if (dataKey) {
    const weekDateFormat = get(data, ["metadata", "weekdate_format"], undefined);
    seriesData = (seriesData || []).map((item: any) => {
      let name = item.key || item.name || item.additional_key;
      if (
        [
          "issue_created",
          "issue_updated",
          "issue_resolved",
          "workitem_created_at",
          "workitem_updated_at",
          "workitem_resolved_at"
        ].includes(across)
      ) {
        name = convertToDate(item.key);
        const xAxisLabelTransform = getWidgetConstant(reportType, ["xAxisLabelTransform"]);
        name = xAxisLabelTransform?.({ item, interval, across, CustomFieldType, weekDateFormat }) || name;
      }
      const xAxisLabelTransform = getWidgetConstant(reportType, ["xAxisLabelTransform"]);
      name = xAxisLabelTransform?.({ item, interval, across, CustomFieldType, weekDateFormat }) || name;
      let mappedItem: any = {};
      if (isArray(dataKey)) {
        let newObj = {};
        forEach(dataKey, (key: string) => {
          newObj = {
            ...newObj,
            [key]: getJiraDataKeyValue(item, key)
          };
        });
        mappedItem = { ...newObj, name: name || "", key: item?.key };
      } else {
        mappedItem = {
          name: name || "",
          key: item?.key,
          [dataKey]: getJiraDataKeyValue(item, dataKey)
        };
      }
      if (isMultiTimeSeriesReport) {
        mappedItem["timestamp"] = item.key;
      }
      return mappedItem;
    });
  }
  const allowedWidgetDataSorting = get(widgetConstants, [reportType, ALLOWED_WIDGET_DATA_SORTING], false);
  const sortKey = get(filters, ["sort", 0, "id"], "");
  const sortValue = get(filters, ["filter", "sort_xaxis"], "");
  const getShouldReverseApiData = getWidgetConstant(reportType, ["shouldReverseApiData"]);
  const shouldReverseApiData = getShouldReverseApiData?.({ interval, across });
  if (
    shouldReverseApiData ||
    (allowedWidgetDataSorting && timeFilterKeys.includes(sortKey) && sortValue && sortValue.includes("old-latest"))
  ) {
    seriesData.reverse();
  }
  return { data: seriesData };
};

export const timeAcrossStagesDataTransformer = (data: any) => {
  const { filters, reportType, supportedCustomFields } = data;
  let { apiData } = data;
  const across = get(filters, ["across"], undefined);
  const labels = get(filters, ["filter", "labels"], undefined);
  let CustomFieldType: any = undefined;
  if (supportedCustomFields && across?.includes("customfield_")) {
    CustomFieldType = supportedCustomFields?.find((item: any) => across === item?.field_key)?.field_type;
  }
  if (labels?.length && across === "label") {
    apiData = apiData?.filter((filters: any) => labels.includes(filters.key));
  }

  let dataKey = get(widgetConstants, [reportType, "dataKey"], undefined);
  if (filters?.filter?.metric) {
    dataKey = filters?.filter.metric;
  }

  apiData = (apiData || []).map((item: any) => {
    const updatedItem = Object.keys(item ?? {}).reduce((acc: any, next: string) => {
      if (across !== "none") {
        return next === "stage"
          ? {
              ...acc,
              [item[next]]: getJiraDataKeyValue(item, dataKey)
            }
          : acc;
      } else {
        return {
          ...acc,
          [dataKey]: getJiraDataKeyValue(item, dataKey)
        };
      }
    }, {});
    let name = item.key || item.additional_key;
    const xAxisLabelTransform = getWidgetConstant(reportType, ["xAxisLabelTransform"]);
    name = xAxisLabelTransform?.({ item, across, CustomFieldType }) || name;
    if (across === "none") {
      return {
        ...updatedItem,
        name: item?.stage,
        key: item?.stage
      };
    }
    return {
      ...updatedItem,
      name: name,
      key: item.key
    };
  });

  if (across && across !== "none") {
    const groupedItems = groupBy(apiData, "name");
    const result = map(groupedItems, item =>
      mergeWith({}, ...item, (obj: any, src: any) => (isArray(obj) ? obj.concat(src) : undefined))
    );
    let stackedData = {};
    let stackedTicketsTotal = 0;
    apiData = (result || []).map(item => {
      const stacks = Object.keys(item)
        .filter(key => !["name", "key"].includes(key))
        .map(key => ({
          key,
          count: parseFloat(item[key])
        }));

      if (stacks) {
        const sortKey = "count";
        let stackedTicketsOtherTotal = stacks.slice(10, stacks.length).reduce((acc: number, obj: any) => {
          acc = acc + obj[sortKey];
          return acc;
        }, 0);

        stackedData = stacks
          .sort((a: any, b: any) => b[sortKey] - a[sortKey])
          .slice(0, 10)
          .reduce((acc: any, obj: any) => {
            acc[obj.key] = obj[sortKey];
            stackedTicketsTotal += obj[sortKey];
            return acc;
          }, {});
        const missingTickets =
          stackedTicketsOtherTotal + Math.max(item[sortKey] - (stackedTicketsTotal + stackedTicketsOtherTotal), 0);
        if (missingTickets > 0) {
          stackedData = {
            ...stackedData,
            Other: missingTickets
          };
        }

        return {
          name: item.name,
          ...stackedData,
          key: item.key
        };
      }
      return item;
    });
  } else {
    apiData = apiData.sort((a: any, b: any) => {
      if (a.name < b.name) {
        return -1;
      }
      if (a.name > b.name) {
        return 1;
      }
      return 0;
    });
  }

  return { data: apiData };
};

export const azureTransformer = (data: any) => {
  let _data: any = timeDurationGenericTransform(data);
  const stacks = get(data.widgetFilters, ["stacks"], undefined);
  const { reportType } = data;
  let dataKey = "median";
  if (reportType === "azure_pipeline_jobs_runs_report") {
    dataKey = "total";
  }

  if (stacks && stacks.length) {
    _data.data = _data.data.map((i: any) => {
      const { name, stacks: _stacks } = i;
      let stackedData = _stacks.reduce((acc: any, i: any) => ({ ...acc, [i.key]: i[dataKey] }), {});
      return {
        name,
        ...stackedData
      };
    });
  }

  return _data;
};

export const SCMPRReportsTransformer = (data: any) => {
  const { records, sortBy, reportType, metadata, widgetFilters } = data;
  let { apiData } = data;
  const convertTo = get(widgetConstants, [reportType, "convertTo"], undefined);
  const across = get(widgetFilters, ["across"], undefined);
  const interval = get(widgetFilters, ["interval"], "week");
  const labels = get(widgetFilters, ["filter", "labels"], undefined);
  const stacks = get(widgetFilters, ["stacks"], undefined);

  let metric: string;
  let totalKey: string;

  if (reportType === JENKINS_REPORTS.SCM_PRS_REPORT) {
    metric = get(metadata, ["metrics"], "num_of_prs");
    totalKey = totalKey = metric === "num_of_prs" ? "count" : "pct_filtered_prs";
  } else if (reportType === JENKINS_REPORTS.SCM_CODING_DAYS_REPORT) {
    metric = get(widgetFilters, ["filter", "metrics"], "avg_coding_day_week");
    totalKey = metric.includes("avg_") ? "mean" : "median";
  } else {
    metric = get(widgetFilters, ["filter", "metrics"], "average_author_response_time");
    totalKey = metric.includes("average_") ? "mean" : "median";
  }

  if (labels?.length && across === "label") {
    apiData = apiData?.filter((filters: any) => labels.includes(filters.key));
  }

  let seriesData: any[] = [];

  if (apiData && apiData.length) {
    seriesData = apiData.map((item: any) => {
      let name = item.additional_key ?? item.key;
      let key = item.key ?? item.additional_key;
      let stackData = item?.stacks ?? [];

      if (SCM_PRS_TIME_FILTERS_KEYS.includes(across)) {
        const xAxisLabelTransform = getWidgetConstant(reportType, ["xAxisLabelTransform"]);
        name = xAxisLabelTransform?.({ item, interval, across });
      }

      let stackObj = {};
      let keyObj = { key: key };
      let metricObj = { [metric]: convertTo ? convertToDays(item?.[totalKey] || 0) : item?.[totalKey] || 0 };
      if (stacks && stacks.length) {
        delete metricObj[metric];

        stackObj = stackData?.reduce((acc: any, data: any, index: any) => {
          let stackKey = data.additional_key ?? data.key;
          acc[stackKey || "UNKNOWN"] = (acc[stackKey || "UNKNOWN"] || 0) + data.count;
          return acc;
        }, {});
      }

      return {
        ...keyObj,
        name: name,
        ...metricObj,
        ...stackObj
      };
    });
  }

  const maxRecords = Math.min(records || DEFAULT_MAX_RECORDS, seriesData.length);

  const getShouldSliceFromEnd = getWidgetConstant(reportType, ["shouldSliceFromEnd"]);
  const shouldSliceFromEnd = getShouldSliceFromEnd?.({ reportType });
  let slice_start = seriesData?.length > 0 && shouldSliceFromEnd ? seriesData.length - maxRecords : 0;
  const slice_end =
    seriesData?.length > 0 && shouldSliceFromEnd ? seriesData.length : maxRecords || DEFAULT_MAX_RECORDS;
  if (slice_start < 0) slice_start = 0;

  seriesData = seriesData.slice(slice_start, slice_end);

  if (stacks && stacks.length) {
    return {
      data: seriesData,
      all_data: apiData
    };
  }
  return {
    data: seriesData
  };
};

export const SCMReportsTransformer = (data: any) => {
  const { records, sortBy, reportType, metadata, widgetFilters } = data;
  let { apiData } = data;
  const convertTo = get(widgetConstants, [reportType, "convertTo"], undefined);
  const across = get(widgetFilters, ["across"], undefined);
  const interval = get(widgetFilters, ["interval"], undefined);
  const labels = get(widgetFilters, ["filter", "labels"], undefined);
  if (labels?.length && across === "label") {
    apiData = apiData?.filter((filters: any) => labels.includes(filters.key));
  }
  const getTotalKey = getWidgetConstant(reportType, ["getTotalKey"]);
  let seriesData =
    apiData && apiData.length
      ? apiData.map((item: any) => {
          const name = item?.additional_key || item.key;
          let result = {
            ...item,
            name: name || ""
          };

          if (item.stacks) {
            const totalKey = getTotalKey?.() || "count";
            const stackedData = getStacksData(item.stacks, widgetFilters, reportType, totalKey);

            result = {
              name: name || "",
              ...stackedData
            };
          }

          return result;
        })
      : [];

  const maxRecords = Math.min(records || DEFAULT_MAX_RECORDS, seriesData.length);

  const getShouldSliceFromEnd = getWidgetConstant(reportType, ["shouldSliceFromEnd"]);
  const shouldSliceFromEnd = getShouldSliceFromEnd?.({ reportType });
  let slice_start = seriesData?.length > 0 && shouldSliceFromEnd ? seriesData.length - maxRecords : 0;
  const slice_end =
    seriesData?.length > 0 && shouldSliceFromEnd ? seriesData.length : maxRecords || DEFAULT_MAX_RECORDS;
  if (slice_start < 0) slice_start = 0;

  seriesData = seriesData.slice(slice_start, slice_end);

  if (convertTo) {
    seriesData = convertTimeData(seriesData, convertTo);
  }

  return {
    data: seriesData
  };
};

export const scmaResolutionTimeDataTransformer = (data: any) => {
  const { records, filters, sortBy, reportType } = data;
  let { apiData } = data;
  const across = get(filters, ["across"], undefined);
  const stacks = get(filters, ["stacks"], undefined);
  const graphType = get(filters, ["filter", "graph_type"], ChartType.BAR);
  const interval = get(filters, ["interval"], undefined);
  const labels = get(filters, ["filter", "labels"], undefined);
  let dataKey = get(widgetConstants, [reportType, "dataKey"], undefined);

  if (labels?.length && across === "label") {
    apiData = apiData?.filter((filters: any) => labels.includes(filters.key));
  }

  let seriesData = apiData;

  if (graphType !== ChartType.LINE) {
    seriesData = genericSeriesDataTransformer(apiData, records, sortBy, reportType, { interval, across }, filters);
  }

  if (filters?.metric) {
    dataKey = filters?.metric;
  } else if (filters?.filter?.metric) {
    dataKey = filters?.filter.metric;
  }

  if (reportType === "scm_issues_time_across_stages_report" && stacks && stacks.length) {
    seriesData = (seriesData || []).map((item: any) => {
      let { key, name, additional_key, ...stackData } = item;
      name = additional_key || name;
      if (["issue_created", "issue_updated", "issue_resolved"].includes(across)) {
        name = convertToDate(key) || additional_key;
      }

      stackData = Object.keys(stackData).reduce((acc: any, next: any) => {
        return {
          ...acc,
          [next]: ceil(stackData[next] / 86400)
        };
      }, {});

      return {
        ...stackData,
        name: name || ""
      };
    });
    return { data: seriesData };
  }

  if (dataKey) {
    seriesData = (seriesData || []).map((item: any) => {
      let name = item.additional_key || item.name;
      if (["issue_created", "issue_updated", "issue_resolved"].includes(across)) {
        name = convertToDate(item.key) || item?.additional_key;
      }
      const xAxisLabelTransform = getWidgetConstant(reportType, "xAxisLabelTransform");
      if (xAxisLabelTransform) {
        name = xAxisLabelTransform({ item, across, interval });
      }
      if (isArray(dataKey)) {
        let newObj = {};
        forEach(dataKey, (key: string) => {
          newObj = {
            ...newObj,
            [key]: getSCMDataKeyValue(item, key)
          };
        });
        return { ...newObj, name: name || "" };
      } else {
        return {
          name: name || "",
          [dataKey]: getSCMDataKeyValue(item, dataKey)
        };
      }
    });
  }

  return { data: seriesData };
};

export const CodeVolVsDeployemntTransformer = (data: any) => {
  const { metadata, apiData } = data;
  const metrics = get(metadata, ["metrics"], "line_count");
  const chartData = apiData?.length
    ? apiData.map((item: any) => {
        let lineCount = item?.total_files_changed;
        if (metrics === "line_count") {
          lineCount = item?.total_lines_added + item?.total_lines_removed + item?.total_lines_changed;
        }
        return {
          name: convertToDate(item.key) || item?.additional_key,
          number_of_deployment: item?.deploy_job_runs_count || 0,
          volume_of_code_change: lineCount || 0,
          key: item.key
        };
      })
    : [];
  return { data: chartData };
};

export const scmReworkReportTransformer = (data: any) => {
  const { records, reportType, widgetFilters, metadata } = data;
  let { apiData } = data;
  const convertTo = get(widgetConstants, [reportType, "convertTo"], undefined);
  const across = get(widgetFilters, ["across"], undefined);
  const visualization = get(metadata, "visualization", SCMReworkVisualizationTypes.STACKED_BAR_CHART);
  const interval = get(widgetFilters, ["interval"], undefined);
  const isPercent = visualization === SCMReworkVisualizationTypes.PERCENTAGE_STACKED_BAR_CHART;
  const labels = get(widgetFilters, ["filter", "labels"], undefined);
  if (labels?.length && across === "label") {
    apiData = apiData?.filter((filters: any) => labels.includes(filters.key));
  }

  let seriesData =
    apiData && apiData.length
      ? apiData.map((item: any) => {
          let name = item.additional_key ?? item.key;
          let key = item.key ?? item.additional_key;

          if (["trend"].includes(across)) {
            const weekDateFormat = get(data, ["metadata", "weekdate_format"], undefined);
            const xAxisLabelTransform = getWidgetConstant(reportType, ["xAxisLabelTransform"]);
            name = xAxisLabelTransform?.({ item, interval, across, weekDateFormat });
          }

          let result: any = {
            key: key,
            name: name
          };

          if (!isPercent) {
            const stackData = (item.stacks || []).reduce((acc: any, next: any) => {
              return {
                ...acc,
                [next.key]: next.total_lines_changed
              };
            }, {});

            result = {
              ...result,
              ...stackData
            };
          } else {
            result = {
              ...result,
              new_lines: item.pct_new_lines,
              refactored_lines: item.pct_refactored_lines,
              legacy_refactored_lines: item.pct_legacy_refactored_lines
            };
          }

          return result;
        })
      : [];

  const maxRecords = Math.min(records || DEFAULT_MAX_RECORDS, seriesData.length);

  const getShouldSliceFromEnd = getWidgetConstant(reportType, ["shouldSliceFromEnd"]);
  const shouldSliceFromEnd = getShouldSliceFromEnd?.({ reportType });
  let slice_start = seriesData?.length > 0 && shouldSliceFromEnd ? seriesData.length - maxRecords : 0;
  const slice_end =
    seriesData?.length > 0 && shouldSliceFromEnd ? seriesData.length : maxRecords || DEFAULT_MAX_RECORDS;
  if (slice_start < 0) slice_start = 0;

  seriesData = seriesData.slice(slice_start, slice_end);

  if (convertTo) {
    seriesData = convertTimeData(seriesData, convertTo);
  }

  return {
    data: seriesData
  };
};

export const bounceReportTransformer = (data: any) => {
  const { records, sortBy, reportType, widgetFilters, filters, supportedCustomFields } = data;
  let { apiData, timeFilterKeys } = data;
  const application = get(widgetConstants, [reportType, "application"], undefined);
  const convertTo = get(widgetConstants, [reportType, "convertTo"], undefined);
  const across = get(widgetFilters, ["across"], undefined);
  const labels = get(widgetFilters, ["filter", "labels"], undefined);

  if (labels?.length && across === "label") {
    apiData = apiData?.filter((filters: any) => labels.includes(filters.key));
  }

  if (
    application === IntegrationTypes.JIRA &&
    across &&
    ["issue_created", "issue_updated", "issue_resolved"].includes(across)
  ) {
    return trendReportTransformer({ apiData, reportType, widgetFilters, timeFilterKeys });
  }

  if (
    application === IntegrationTypes.AZURE &&
    across &&
    ["workitem_created_at", "workitem_updated_at", "workitem_resolved_at", "trend"].includes(across)
  ) {
    return trendReportTransformer({ apiData, reportType, widgetFilters, timeFilterKeys });
  }

  let seriesData = genericSeriesDataTransformer(
    apiData,
    records,
    sortBy,
    reportType,
    {},
    filters,
    supportedCustomFields
  );

  if (convertTo) {
    seriesData = convertTimeData(seriesData, convertTo);
  }

  return {
    data: seriesData
  };
};
