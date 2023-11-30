import { trendReportTransformer } from "custom-hooks/helpers";
import { zendeskDrilldown } from "dashboard/constants/drilldown.constants";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ZendeskRepliesReportTrendTypes } from "model/report/zendesk/zendesk-replies-report-trend-report/zendeskRepliesReportTrendReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { BASE_ZENDESK_CHART_PROPS } from "../constant";
import { ZENDESK_REPLIES_REPORT_TREND_COMPOSITE_TRANSFORM } from "./constant";
import { ZendeskRepliesReportTrendsFiltersConfig } from "./filters.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const zendeskRepliesReportTrend: { zendesk_replies_report_trends: ZendeskRepliesReportTrendTypes } = {
  zendesk_replies_report_trends: {
    name: "Support Replies Trend Report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: {
      unit: "Days",
      chartProps: BASE_ZENDESK_CHART_PROPS
    },
    composite: true,
    composite_transform: ZENDESK_REPLIES_REPORT_TREND_COMPOSITE_TRANSFORM,
    uri: "zendesk_replies_report",
    method: "list",
    drilldown: zendeskDrilldown,
    transformFunction: data => trendReportTransformer(data),
    report_filters_config: ZendeskRepliesReportTrendsFiltersConfig
  }
};

export default zendeskRepliesReportTrend;
