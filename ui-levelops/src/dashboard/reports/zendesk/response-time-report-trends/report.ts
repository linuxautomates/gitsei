import { trendReportTransformer } from "custom-hooks/helpers";
import { zendeskDrilldown } from "dashboard/constants/drilldown.constants";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ZendeskResponseTimeTrendReportTypes } from "model/report/zendesk/zendesk-response-time-trend-report/zendeskResponseTimeTrendReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { BASE_ZENDESK_CHART_PROPS } from "../constant";
import { ZENDESK_RESPONSE_TIME_REPORT_TREND_COMPOSITE_TRANSFORM } from "./constant";
import { ZendeskResponseTimeReportTrendsFiltersConfig } from "./filters.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const zendeskResponseTimeTrendReport: { zendesk_response_time_report_trends: ZendeskResponseTimeTrendReportTypes } = {
  zendesk_response_time_report_trends: {
    name: "Support Response Time Trend Report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    composite: true,
    composite_transform: ZENDESK_RESPONSE_TIME_REPORT_TREND_COMPOSITE_TRANSFORM,
    chart_props: {
      unit: "Days",
      chartProps: BASE_ZENDESK_CHART_PROPS
    },
    uri: "zendesk_response_time_report",
    method: "list",
    convertTo: "days",
    drilldown: zendeskDrilldown,
    transformFunction: data => trendReportTransformer(data),
    report_filters_config: ZendeskResponseTimeReportTrendsFiltersConfig
  }
};

export default zendeskResponseTimeTrendReport;
