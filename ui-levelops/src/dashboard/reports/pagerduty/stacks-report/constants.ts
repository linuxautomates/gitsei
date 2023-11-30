import { pagerdutyServicesSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { genericDrilldownTransformer } from "dashboard/helpers/drilldown-transformers";
import { PagerdutyHotspotTableConfig } from "dashboard/pages/dashboard-tickets/configs";
import { chartProps } from "dashboard/reports/commonReports.constants";

export const pagerdutyStacksReportChartProps = {
  unit: "Jobs",
  chartProps: chartProps,
  barProps: [
    {
      name: "count",
      dataKey: "count"
    }
  ],
  stacked: false
};

export const pagerdutyStacksReportDrilldown = {
  title: "Pagerduty Report",
  uri: "services_report_list",
  application: "pagerduty_hotspot_report",
  columns: PagerdutyHotspotTableConfig,
  supported_filters: pagerdutyServicesSupportedFilters,
  drilldownTransformFunction: (data: any) => genericDrilldownTransformer(data)
};

export const pagerdutyStacksReportFilter = {
  type: ["pagerduty"]
};

export const STACK_FILTERS = [
  { label: "Incident Priority", value: "incident_priority" },
  { label: "Alert Severity", value: "alert_severity" }
];

export const ACROSS_OPTIONS = [
  { label: "Incident Priority", value: "incident_priority" },
  { label: "Alert Severity", value: "alert_severity" },
  { label: "Incident Urgency", value: "incident_urgency" }
];
