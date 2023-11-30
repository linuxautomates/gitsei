import { seriesDataTransformer } from "custom-hooks/helpers";
import { salesforceDrilldown } from "dashboard/constants/drilldown.constants";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SalesforceTicketsReportType } from "model/report/salesforce/salesforce-tickets-report/salesforceTicketsReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { SALESFORCE_TICKETS_REPORT } from "./constant";
import { SalesforceTicketsReportFiltersConfig } from "./filter.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const salesforceTicketsReport: { salesforce_tickets_report: SalesforceTicketsReportType } = {
  salesforce_tickets_report: {
    name: "Support Tickets Report",
    application: IntegrationTypes.SALESFORCE,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultAcross: "status",
    chart_props: SALESFORCE_TICKETS_REPORT,
    uri: "salesforce_hygiene_report", //BACKEND CHANGES
    method: "list",
    drilldown: salesforceDrilldown,
    transformFunction: data => seriesDataTransformer(data),
    HIDE_REPORT: true,
    report_filters_config: SalesforceTicketsReportFiltersConfig
  }
};

export default salesforceTicketsReport;
