import { trendReportTransformer } from "custom-hooks/helpers";
import { zendeskDrilldown } from "dashboard/constants/drilldown.constants";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ZendeskResolutionTimeTrendReportTypes } from "model/report/zendesk/zendesk-resolution-time-trend-report/zendeskResolutionTimeTrendReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { BASE_ZENDESK_CHART_PROPS } from "../constant";
import { ZENDESK_RESOLUTION_TIME_REPORT_COMPOSITE_TRANSFORM } from "./constant";
import { ZendeskResolutionTimeReportTrendsFiltersConfig } from "./filters.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const zendeskResolutionTimeTrendReport: {
  zendesk_resolution_time_report_trends: ZendeskResolutionTimeTrendReportTypes;
} = {
  zendesk_resolution_time_report_trends: {
    name: "Support Resolution Time Trend Report",
    application: IntegrationTypes.ZENDESK,
    composite: true,
    composite_transform: ZENDESK_RESOLUTION_TIME_REPORT_COMPOSITE_TRANSFORM,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: {
      unit: "Days",
      chartProps: BASE_ZENDESK_CHART_PROPS
    },
    uri: "zendesk_resolution_time_report",
    method: "list",
    convertTo: "days",
    drilldown: zendeskDrilldown,
    transformFunction: data => trendReportTransformer(data),
    report_filters_config: ZendeskResolutionTimeReportTrendsFiltersConfig
  }
};

export default zendeskResolutionTimeTrendReport;
