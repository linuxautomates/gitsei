import { trendReportTransformer } from "custom-hooks/helpers";
import { pagerdutyIncidentSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { IncidentReportTrendsType } from "model/report/pagerduty/incident-report-trends/incidentReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { pagerdutyIncidentReportTrendsChartProps, pagerdutyIncidentReportTrendsFilter } from "./constants";
import { DEPRECATED_REPORT } from "dashboard/constants/applications/names";
import { IntegrationTypes } from "constants/IntegrationTypes";

const pagerdutyIncidentReportTrends: { pagerduty_incident_report_trends: IncidentReportTrendsType } = {
  pagerduty_incident_report_trends: {
    name: "PagerDuty Incident Report Trends",
    application: IntegrationTypes.PAGERDUTY,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "pagerduty_incident_rates",
    method: "list",
    chart_props: pagerdutyIncidentReportTrendsChartProps,
    filters: pagerdutyIncidentReportTrendsFilter,
    xaxis: false,
    composite: true,
    composite_transform: {
      count: "pagerduty_incident_count"
    },
    supported_filters: pagerdutyIncidentSupportedFilters,
    transformFunction: data => trendReportTransformer(data),
    requiredFilters: ["pd_service"],
    singleSelectFilters: ["pd_service"],
    [DEPRECATED_REPORT]: true
  }
};

export default pagerdutyIncidentReportTrends;
