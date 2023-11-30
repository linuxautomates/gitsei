import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { ChartContainerType } from "dashboard/helpers/helper";
import {
  ALLOWED_WIDGET_DATA_SORTING,
  FILTER_NAME_MAPPING,
  VALUE_SORT_KEY,
  WIDGET_DATA_SORT_FILTER_KEY
} from "../../../filter-name.mapping";
import { pagerdutyFilters } from "../../../supported-filters.constant";
import { FE_BASED_FILTERS, REPORT_FILTERS_CONFIG } from "../../names";
import { WidgetFilterType } from "../../../enums/WidgetFilterType.enum";
import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { get, unset } from "lodash";
import { DEFAULT_MAX_RECORDS } from "../../../constants";
import widgetConstants from '../../../widgetConstants'
import { getWidgetConstant } from "../../../widgetConstants";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";
import { genericDrilldownTransformer } from "../../../../helpers/drilldown-transformers";
import { baseColumnConfig } from "../../../../../utils/base-table-config";
import { statusColumn, timeColumn } from "../../../../pages/dashboard-tickets/configs/common-table-columns";
import { pagerdutyValuesToFilters } from "./constant";
import { getXAxisTimeLabel, isvalidTimeStamp } from "utils/dateUtils";
import { CustomTimeBasedTypes } from "dashboard/graph-filters/components/helper";
import { TIME_FILTERS_KEYS } from "constants/filters";
import moment from "moment";
import { PagerDutyTimeToResolveFiltersConfig } from "dashboard/reports/pagerduty/time-to-resolve/filters.config";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { convertToDays } from "utils/timeUtils";

const filterOptionKeyMapping = {
  user_id: "User (Engineer)",
  pd_service: "Service"
};

const tableConfig = [
  baseColumnConfig("Incident Id", "id"),
  baseColumnConfig("Summary", "summary"),
  statusColumn("Current Status", "status"),
  baseColumnConfig("Service", "service_name"),
  baseColumnConfig("Incident Urgency", "urgency"),
  timeColumn("Incident Created At", "created_at"),
  timeColumn("Incident Acknowledged At", "last_status_at"),
  timeColumn("Incident Resolved At", "incident_resolved_at")
];

const alertTableConfig = [
  baseColumnConfig("Alert Id", "id"),
  baseColumnConfig("Summary", "summary"),
  statusColumn("Current Status", "status"),
  baseColumnConfig("Incident Id", "incident_id"),
  baseColumnConfig("Service", "service_name"),
  baseColumnConfig("Alert Severity", "severity"),
  timeColumn("Alert Created At", "created_at"),
  timeColumn("Alert Acknowledged At", "last_status_at"),
  timeColumn("Alert Resolved At", "incident_resolved_at")
];

const issue_type = {
  type: WidgetFilterType.DROPDOWN_BASED_FILTERS,
  label: "Issue Type",
  BE_key: "issue_type",
  configTab: WIDGET_CONFIGURATION_KEYS.FILTERS,
  options: [
    { label: "Alert", value: "alert" },
    { label: "Incident", value: "incident" }
  ]
};

const incident_created_at = {
  type: WidgetFilterType.TIME_BASED_FILTERS,
  label: "Incident Created At",
  BE_key: "incident_created_at",
  slicing_value_support: true,
  configTab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

const incident_resolved_at = {
  type: WidgetFilterType.TIME_BASED_FILTERS,
  label: "Incident Resolved At",
  BE_key: "incident_resolved_at",
  slicing_value_support: true,
  configTab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

const alert_created_at = {
  type: WidgetFilterType.TIME_BASED_FILTERS,
  label: "Alert Created At",
  BE_key: "alert_created_at",
  slicing_value_support: true,
  configTab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

const alert_resolved_at = {
  type: WidgetFilterType.TIME_BASED_FILTERS,
  label: "Alert Resolved At",
  BE_key: "alert_resolved_at",
  slicing_value_support: true,
  configTab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

const ID_FILTERS = ["user_id", "pd_service"];

const xAxisLabelTransform = (props: Record<string, any>) => {
  const { interval, across, item = {}, CustomFieldType } = props;
  const { key, additional_key } = item;
  let newLabel = key;
  const isValidDate = isvalidTimeStamp(newLabel);
  if (ID_FILTERS.includes(across)) {
    newLabel = additional_key || key;
    return newLabel;
  }
  if (
    (CustomTimeBasedTypes.includes(CustomFieldType) && isValidDate) ||
    (isValidDate && !TIME_FILTERS_KEYS.includes(across))
  ) {
    newLabel = moment(parseInt(key)).format("DD-MM-YYYY HH:mm:ss");
    return newLabel;
  }

  if (TIME_FILTERS_KEYS.includes(across)) {
    newLabel = getXAxisTimeLabel({ interval, key });
  }

  if (!newLabel || newLabel === "NA") {
    newLabel = "UNRESOLVED";
  }
  return newLabel;
};

const transformerFn = (data: any) => {
  const { records, sortBy, reportType, metadata, filters: widgetFilters, isMultiTimeSeriesReport } = data;
  let { apiData } = data;
  const xAxisLabelTransform = getWidgetConstant(reportType, ["xAxisLabelTransform"]);
  const leftYAxis = get(metadata, "leftYAxis", "mean");
  const across = get(widgetFilters, ["across"], undefined);
  const interval = get(widgetFilters, ["interval"], "day");
  const xAxisIgnoreSortKeys = get(widgetConstants, [reportType, "chart_props", "xAxisIgnoreSortKeys"], []);
  apiData = (apiData || []).map((item: any) => {
    const { stacks } = item;

    let stackData = {};
    if (stacks) {
      let stackedTicketsTotal = 0;
      let stackedTicketsOtherTotal = stacks.slice(10, stacks.length).reduce((acc: number, obj: any) => {
        acc = acc + obj["count"];
        return acc;
      }, 0);

      stackData = stacks
        .sort((a: any, b: any) => b["count"] - a["count"])
        .slice(0, 10)
        .reduce((acc: any, obj: any) => {
          // if key = "" then replace it with UNKNOWN
          acc[xAxisLabelTransform?.({ item: obj, interval, across }) || "UNKNOWN"] = obj["count"];
          stackedTicketsTotal += obj["count"];
          return acc;
        }, {});
      const missingTickets =
        stackedTicketsOtherTotal + Math.max(item["count"] - (stackedTicketsTotal + stackedTicketsOtherTotal), 0);
      if (missingTickets > 0) {
        stackData = {
          ...stackData,
          Other: missingTickets
        };
      }
    }

    const weekDateFormat = get(metadata, ["weekdate_format"], undefined);
    const name = xAxisLabelTransform?.({ item, interval, weekDateFormat, across });

    const mappedItem: any = {
      count: item?.count,
      [leftYAxis]: convertToDays(item?.[leftYAxis] || 0),
      ...stackData,
      name,
      key: item.key
    };
    if (isMultiTimeSeriesReport) {
      mappedItem["timestamp"] = item.key;
    }
    return mappedItem;
  });

  // sorting X-axis
  const allowedWidgetDataSorting = get(widgetConstants, [reportType, ALLOWED_WIDGET_DATA_SORTING], false);
  if (allowedWidgetDataSorting) {
    const sortValue = get(widgetFilters, ["sort_xaxis"], "");
    sortValue.includes("old-latest") && apiData.reverse();
  }

  const maxRecords = Math.min(records || DEFAULT_MAX_RECORDS, apiData.length);

  const getShouldSliceFromEnd = getWidgetConstant(reportType, ["shouldSliceFromEnd"]);
  const shouldSliceFromEnd = getShouldSliceFromEnd?.({ reportType });
  let slice_start = apiData?.length > 0 && shouldSliceFromEnd ? apiData.length - maxRecords : 0;
  const slice_end = apiData?.length > 0 && shouldSliceFromEnd ? apiData.length : maxRecords || DEFAULT_MAX_RECORDS;
  if (slice_start < 0) slice_start = 0;

  apiData = apiData.slice(slice_start, slice_end);
  const getShouldReverseApiData = getWidgetConstant(reportType, ["shouldReverseApiData"]);
  const shouldReverseApiData = getShouldReverseApiData?.(sortBy);
  if (shouldReverseApiData) {
    apiData.reverse();
  }

  return {
    data: apiData
  };
};

const drilldownTransform = (data: any) => {
  const { drillDownProps, widget } = data;
  let { acrossValue, filters } = genericDrilldownTransformer(data);

  const x_axis = get(drillDownProps, ["x_axis"], "");
  const filterValue = typeof x_axis === "object" ? x_axis?.id : x_axis;

  if (["NA", "UNRESOLVED"].includes(filterValue)) {
    const widgetConstantFilterValue = get(
      widgetConstants,
      [widget?.type || "", "valuesToFilters", acrossValue],
      undefined
    );
    const filterKey = widgetConstantFilterValue || get(pagerdutyValuesToFilters, [acrossValue], acrossValue);
    unset(filters, ["filter", filterKey]);

    filters = {
      ...filters,
      filter: {
        ...filters?.filter,
        missing_fields: {
          [acrossValue]: true
        }
      }
    };
  }

  return { acrossValue, filters };
};

const pagerdutyTimeToResolve = {
  pagerduty_response_reports: {
    name: "PagerDuty Response Reports",
    application: IntegrationTypes.PAGERDUTY,
    chart_type: ChartType?.COMPOSITE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "pagerduty_resolution_time_report",
    acknowledgeUri: "pagerduty_response_time_report",
    method: "list",
    xaxis: true,
    across: [
      "status",
      "incident_priority",
      "alert_severity",
      "incident_created_at",
      "alert_created_at",
      "incident_resolved_at",
      "alert_resolved_at"
    ],
    appendAcrossOptions: [
      {
        label: "User (Engineer)",
        value: "user_id"
      },
      {
        label: "Service",
        value: "pd_service"
      }
    ],
    stack_filters: ["user_id", "pd_service", "incident_priority", "status", "alert_severity"],
    defaultAcross: "user_id",
    default_query: {
      [WIDGET_DATA_SORT_FILTER_KEY]:
        widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED],
      metric: "resolve",
      interval: "week"
    },
    chart_props: {
      unit: "Count",
      chartProps: {
        barGap: 0,
        margin: { top: 20, right: 5, left: 5, bottom: 50 }
      },
      barProps: [
        {
          name: "Number of Incidents or Alerts",
          dataKey: "count"
        }
      ],
      stacked: true,
      xAxisIgnoreSortKeys: ["alert_severity", "incident_priority"]
    },
    convertTo: "days",
    [FILTER_NAME_MAPPING]: filterOptionKeyMapping,
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [VALUE_SORT_KEY]: "user_id",
    defaultSort: [{ id: "user_id", desc: true }],
    tooltipMapping: { mean: "Mean Time", median: "Median Time", count: "Number of Incidents or Alerts" },
    supported_filters: pagerdutyFilters,
    valuesToFilters: pagerdutyValuesToFilters,
    transformFunction: (data: any) => transformerFn(data),
    drilldown: {
      title: "Pagerduty Report",
      uri: "pagerduty_incidents_aggs",
      alertUri: "pagerduty_alerts_aggs",
      application: "pagerduty_response_reports",
      columns: tableConfig,
      alertColumns: alertTableConfig,
      supported_filters: pagerdutyFilters,
      drilldownTransformFunction: (data: any) => drilldownTransform(data)
    },
    xAxisLabelTransform: xAxisLabelTransform,
    onChartClickPayload: (params: any) => {
      const { across, data } = params;
      const _data = data?.activePayload?.[0]?.payload || {};
      if (["user_id", "pd_service"].includes(across)) {
        return {
          name: data.activeLabel || "",
          id: _data.key || data.activeLabel
        };
      }

      return data.activeLabel || "";
    },
    [FE_BASED_FILTERS]: {
      issue_type,
      incident_created_at,
      incident_resolved_at,
      alert_created_at,
      alert_resolved_at
    },
    [REPORT_FILTERS_CONFIG]: PagerDutyTimeToResolveFiltersConfig
  }
};

export default pagerdutyTimeToResolve;
