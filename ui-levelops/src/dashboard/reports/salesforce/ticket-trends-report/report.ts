import { trendReportTransformer } from "custom-hooks/helpers";
import { salesforceDrilldown } from "dashboard/constants/drilldown.constants";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SalesforceTicketsTrendReportType } from "model/report/salesforce/salesforce-tickets-trend-report/salesforceTicketsTrendReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { BASE_SALESFORCE_CHART_PROPS } from "../constant";
import { SalesforceTicketsTrendReportFiltersConfig } from "./filter.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const salesforceTicketsTrendReport: { salesforce_tickets_report_trends: SalesforceTicketsTrendReportType } = {
  salesforce_tickets_report_trends: {
    name: "Support Ticket Trends Report",
    application: IntegrationTypes.SALESFORCE,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    composite: true,
    composite_transform: {
      total_tickets: "total_salesforce_tickets"
    },
    chart_props: {
      unit: "Cases",
      chartProps: BASE_SALESFORCE_CHART_PROPS
    },
    uri: "salesforce_hygiene_report",
    method: "list",
    filters: {
      across: "trend"
    },
    drilldown: salesforceDrilldown,
    transformFunction: data => trendReportTransformer(data),
    report_filters_config: SalesforceTicketsTrendReportFiltersConfig,
    HIDE_REPORT: true
  }
};

export default salesforceTicketsTrendReport;
