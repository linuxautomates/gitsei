import { trendReportTransformer } from "custom-hooks/helpers";
import { zendeskDrilldown } from "dashboard/constants/drilldown.constants";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ZendeskBounceTrendReportTypes } from "model/report/zendesk/zendesk-bounce-trend-report/zendeskBounceTrendReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { BASE_ZENDESK_CHART_PROPS } from "../constant";
import { ZENDESK_BOUNCE_TREND_REPORT_COMPOSITE_TRANSFORM } from "./constant";
import { ZendeskBounceTrendsReportFiltersConfig } from "./filters.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const zendeskBounceTrendReport: { zendesk_bounce_report_trends: ZendeskBounceTrendReportTypes } = {
  zendesk_bounce_report_trends: {
    name: "Support Bounce Trend Report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    composite: true,
    composite_transform: ZENDESK_BOUNCE_TREND_REPORT_COMPOSITE_TRANSFORM,
    chart_props: {
      unit: "Bounces",
      chartProps: BASE_ZENDESK_CHART_PROPS
    },
    uri: "zendesk_bounce_report",
    method: "list",
    drilldown: zendeskDrilldown,
    transformFunction: data => trendReportTransformer(data),
    report_filters_config: ZendeskBounceTrendsReportFiltersConfig
  }
};

export default zendeskBounceTrendReport;
