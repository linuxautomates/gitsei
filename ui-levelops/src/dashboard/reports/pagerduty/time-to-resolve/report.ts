import { pagerdutyFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  ALLOWED_WIDGET_DATA_SORTING,
  FILTER_NAME_MAPPING,
  VALUE_SORT_KEY
} from "dashboard/constants/filter-name.mapping";
import { FE_BASED_FILTERS } from "dashboard/constants/applications/names";
import { TimeToResolveReportType } from "model/report/pagerduty/time-to-resolve/timeToResolveReport.constant";
import {
  timeToResolveChartProps,
  timeToResolveDefaultQuery,
  timeToResolveDrilldown,
  timeToResolveFEBasedFilters,
  timeToResolveFilterOptionKeyMapping,
  timeToResolveValuesToFilters
} from "./constants";
import { timeToResolveTransformer } from "./transformers";
import { timeToResolveOnChartClickPayload, timeToResolveXAxisLabelTransform } from "./helpers";
import { IntegrationTypes } from "constants/IntegrationTypes";

const pagerdutyTimeToResolve: { pagerduty_response_reports: TimeToResolveReportType } = {
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
    default_query: timeToResolveDefaultQuery,
    chart_props: timeToResolveChartProps,
    convertTo: "days",
    [FILTER_NAME_MAPPING]: timeToResolveFilterOptionKeyMapping,
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [VALUE_SORT_KEY]: "user_id",
    defaultSort: [{ id: "user_id", desc: true }],
    tooltipMapping: { mean: "Mean Time", median: "Median Time", count: "Number of Incidents or Alerts" },
    supported_filters: pagerdutyFilters,
    valuesToFilters: timeToResolveValuesToFilters,
    transformFunction: (data: any) => timeToResolveTransformer(data),
    drilldown: timeToResolveDrilldown,
    xAxisLabelTransform: timeToResolveXAxisLabelTransform,
    onChartClickPayload: timeToResolveOnChartClickPayload,
    [FE_BASED_FILTERS]: timeToResolveFEBasedFilters
  }
};

export default pagerdutyTimeToResolve;
