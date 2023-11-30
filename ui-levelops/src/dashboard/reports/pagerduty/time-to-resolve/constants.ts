import { baseColumnConfig } from "../../../../utils/base-table-config";
import { statusColumn, timeColumn } from "../../../pages/dashboard-tickets/configs/common-table-columns";
import { WidgetFilterType } from "../../../constants/enums/WidgetFilterType.enum";
import { WIDGET_CONFIGURATION_KEYS } from "../../../../constants/widgets";
import { WIDGET_DATA_SORT_FILTER_KEY } from "../../../constants/filter-name.mapping";
import { pagerdutyFilters } from "../../../constants/supported-filters.constant";
import { timeToResolveDrilldownTransform } from "./transformers";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "../../../constants/WidgetDataSortingFilter.constant";

export const timeToResolveFilterOptionKeyMapping = {
  user_id: "User (Engineer)",
  pd_service: "Service"
};

export const incidentTableConfig = [
  baseColumnConfig("Incident Id", "id"),
  baseColumnConfig("Summary", "summary"),
  statusColumn("Current Status", "status"),
  baseColumnConfig("Service", "service_name"),
  baseColumnConfig("Incident Urgency", "urgency"),
  timeColumn("Incident Created At", "created_at"),
  timeColumn("Incident Acknowledged At", "last_status_at"),
  timeColumn("Incident Resolved At", "incident_resolved_at")
];

export const alertTableConfig = [
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

export const timeToResolveValuesToFilters = {
  issue_type: "issue_type",
  alert_severity: "alert_severities",
  alert_status: "alert_statuses",
  incident_urgency: "incident_urgencies",
  incident_status: "incident_statuses",
  incident_priority: "incident_priorities",
  pd_service: "pd_service_ids",
  pd_service_id: "pd_service_ids",
  user_id: "user_ids"
};

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

export const timeToResolveFEBasedFilters = {
  issue_type,
  incident_created_at,
  incident_resolved_at,
  alert_created_at,
  alert_resolved_at
};

export const timeToResolveDrilldown = {
  title: "Pagerduty Report",
  uri: "pagerduty_incidents_aggs",
  alertUri: "pagerduty_alerts_aggs",
  application: "pagerduty_response_reports",
  columns: incidentTableConfig,
  alertColumns: alertTableConfig,
  supported_filters: pagerdutyFilters,
  drilldownTransformFunction: (data: any) => timeToResolveDrilldownTransform(data)
};

export const timeToResolveChartProps = {
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
};

export const timeToResolveDefaultQuery = {
  [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED],
  metric: "resolve",
  interval: "week"
};

export const ACROSS_OPTIONS = [
  {
    label: "User (Engineer)",
    value: "user_id"
  },
  {
    label: "Service",
    value: "pd_service"
  },
  {
    label: "Status",
    value: "status"
  },
  {
    label: "Incident Priority",
    value: "incident_priority"
  },
  {
    label: "Alert Severity",
    value: "alert_severity"
  },
  {
    label: "Incident Created At",
    value: "incident_created_at"
  },
  {
    label: "Alert Created At",
    value: "alert_created_at"
  },
  {
    label: "Incident Resolved At",
    value: "incident_resolved_at"
  },
  {
    label: "Alert Resolved At",
    value: "alert_resolved_at"
  }
];

export const STACK_OPTIONS = [
  {
    label: "Service",
    value: "pd_service"
  },
  {
    label: "Status",
    value: "status"
  },
  {
    label: "Incident Priority",
    value: "incident_priority"
  },
  {
    label: "Alert Severity",
    value: "alert_severity"
  }
];

export const PAGERDUTY_ACK_TREND_FILTER_KEY_MAPPING = {
  user_id: "user_ids",
  pd_service: "pd_service_id"
};
