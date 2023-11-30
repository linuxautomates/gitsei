import { trendReportTransformer } from "custom-hooks/helpers";
import { zendeskDrilldown } from "dashboard/constants/drilldown.constants";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ZendeskReopensTrendReportTypes } from "model/report/zendesk/zendesk-reopens-report-trends/zendeskReopensTrendReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { BASE_ZENDESK_CHART_PROPS } from "../constant";
import { ZENDESK_REOPENS_REPORT_COMPOSITE_TRANSFORM } from "./constant";
import { ZendeskTicketsReopensReportTrendsFiltersConfig } from "./filters.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const zendeskReopensTrendReport: { zendesk_reopens_report_trends: ZendeskReopensTrendReportTypes } = {
  zendesk_reopens_report_trends: {
    name: "Support Ticket Reopens Trend Report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: {
      unit: "Days",
      chartProps: BASE_ZENDESK_CHART_PROPS
    },
    composite: true,
    composite_transform: ZENDESK_REOPENS_REPORT_COMPOSITE_TRANSFORM,
    uri: "zendesk_reopens_report",
    method: "list",
    drilldown: zendeskDrilldown,
    transformFunction: data => trendReportTransformer(data),
    report_filters_config: ZendeskTicketsReopensReportTrendsFiltersConfig
  }
};

export default zendeskReopensTrendReport;
