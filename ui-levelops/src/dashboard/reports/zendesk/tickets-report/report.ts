import { seriesDataTransformer } from "custom-hooks/helpers";
import { zendeskDrilldown } from "dashboard/constants/drilldown.constants";
import { ZendeskStacksReportsKey } from "dashboard/constants/helper";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ZendeskTicketsReportTypes } from "model/report/zendesk/zendesk-tickets-report/zendeskTicketsReport.constant";
import { getXAxisLabel } from "shared-resources/charts/bar-chart/bar-chart.helper";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { ZENDESK_TICKETS_REPORT_CHART_PROPS } from "./constants";
import { ZendeskTicketsReportFiltersConfig } from "./filters.config";
import { getZendeskTicketsReportSortKey, getZendeskTicketsReportSortOrder } from "./helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

const zendeskTicketsReport: { zendesk_tickets_report: ZendeskTicketsReportTypes } = {
  zendesk_tickets_report: {
    name: "Support Tickets Report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultAcross: "brand",
    chart_props: ZENDESK_TICKETS_REPORT_CHART_PROPS,
    uri: "zendesk_tickets_report",
    method: "list",
    drilldown: zendeskDrilldown,
    getTotalKey: () => "total_tickets",
    [ZendeskStacksReportsKey.ZENDESK_STACKED_KEY]: true,
    transformFunction: data => seriesDataTransformer(data),
    xAxisLabelTransform: params => getXAxisLabel(params),
    getSortKey: getZendeskTicketsReportSortKey,
    getSortOrder: getZendeskTicketsReportSortOrder,
    report_filters_config: ZendeskTicketsReportFiltersConfig
  }
};

export default zendeskTicketsReport;
