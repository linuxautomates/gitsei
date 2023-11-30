import { pagerdutyServicesTransformer } from "custom-hooks/helpers";
import { pagerdutyServicesSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { StacksReportType } from "model/report/pagerduty/stacks-report/stacksReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  pagerdutyStacksReportChartProps,
  pagerdutyStacksReportDrilldown,
  pagerdutyStacksReportFilter
} from "./constants";
import { DEPRECATED_REPORT } from "dashboard/constants/applications/names";
import { IntegrationTypes } from "constants/IntegrationTypes";

const pagerdutyStacksReport: { pagerduty_hotspot_report: StacksReportType } = {
  pagerduty_hotspot_report: {
    name: "PagerDuty Stacks Report",
    application: IntegrationTypes.PAGERDUTY,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "services_report_aggregate",
    method: "list",
    filters: pagerdutyStacksReportFilter,
    defaultAcross: "incident_priority",
    chart_props: pagerdutyStacksReportChartProps,
    defaultStacks: ["incident_priority"],
    xaxis: true,
    across: ["incident_priority", "incident_urgency", "alert_severity"],
    stack_filters: ["incident_priority", "incident_urgency", "alert_severity"],
    drilldown: pagerdutyStacksReportDrilldown,
    supported_filters: pagerdutyServicesSupportedFilters,
    transformFunction: (data: any) => pagerdutyServicesTransformer(data),
    [DEPRECATED_REPORT]: true
  }
};

export default pagerdutyStacksReport;
