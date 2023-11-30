import { trendReportTransformer } from "custom-hooks/helpers";
import { zendeskDrilldown } from "dashboard/constants/drilldown.constants";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ZendeskTicketTrendReportTypes } from "model/report/zendesk/zendesk-tickets-trend-report/zendeskTicketsTrendReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { BASE_ZENDESK_CHART_PROPS } from "../constant";
import { ZENDESK_TICKETS_TREND_REPORT_COMPOSITE_TRANSFORM } from "./constant";
import { ZendeskTicketsReportTrendsFiltersConfig } from "./filters.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const zendeskTicketsTrendReport: { zendesk_tickets_report_trends: ZendeskTicketTrendReportTypes } = {
  zendesk_tickets_report_trends: {
    name: "Support Tickets Trend Report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: {
      unit: "Tickets",
      chartProps: BASE_ZENDESK_CHART_PROPS
    },
    composite: true,
    composite_transform: ZENDESK_TICKETS_TREND_REPORT_COMPOSITE_TRANSFORM,
    uri: "zendesk_tickets_report",
    method: "list",
    drilldown: zendeskDrilldown,
    transformFunction: data => trendReportTransformer(data),
    report_filters_config: ZendeskTicketsReportTrendsFiltersConfig
  }
};

export default zendeskTicketsTrendReport;
