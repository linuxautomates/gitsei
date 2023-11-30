import { trendReportTransformer } from "custom-hooks/helpers";
import { zendeskDrilldown } from "dashboard/constants/drilldown.constants";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ZendeskRequesterWaitTimeTrendReportTypes } from "model/report/zendesk/zendesk-requester-wait-time-trend-report/zendeskWaitTimeTrendReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { BASE_ZENDESK_CHART_PROPS } from "../constant";
import { ZENDESK_REQUEST_WAIT_TIME_REPORT_COMPOSITE_TRANSFORM } from "./constant";
import { ZendeskRequesterWaitTimeReportTrendsFiltersConfig } from "./filters.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const zendeskRequesterWaitTimeTrendReport: {
  zendesk_requester_wait_time_report_trends: ZendeskRequesterWaitTimeTrendReportTypes;
} = {
  zendesk_requester_wait_time_report_trends: {
    name: "Support Requester Wait Time Trend Report",
    application: IntegrationTypes.ZENDESK,
    composite: true,
    composite_transform: ZENDESK_REQUEST_WAIT_TIME_REPORT_COMPOSITE_TRANSFORM,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: {
      unit: "Days",
      chartProps: BASE_ZENDESK_CHART_PROPS
    },
    uri: "zendesk_requester_wait_time_report",
    method: "list",
    convertTo: "days",
    drilldown: zendeskDrilldown,
    transformFunction: data => trendReportTransformer(data),
    report_filters_config: ZendeskRequesterWaitTimeReportTrendsFiltersConfig
  }
};

export default zendeskRequesterWaitTimeTrendReport;
