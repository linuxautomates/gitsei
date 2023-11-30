import { tableTransformer } from "custom-hooks/helpers";
import { zendeskTopCustomerReportDrilldown } from "dashboard/constants/drilldown.constants";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ZendeskTopCustomerReportTypes } from "model/report/zendesk/zendesk-top-customer-report/zendeskTopCustomerReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { ZENDESK_TOP_CUSTOMER_REPORT_CHART_PROPS } from "./constant";
import { ZendeskTopCustomersReportFiltersConfig } from "./filters.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const zendeskTopCustomerReport: { zendesk_top_customers_report: ZendeskTopCustomerReportTypes } = {
  zendesk_top_customers_report: {
    name: "Support Top Customers Report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.TABLE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: ZENDESK_TOP_CUSTOMER_REPORT_CHART_PROPS,
    uri: "zendesk_tickets_report",
    method: "list",
    filters: {},
    defaultAcross: "brand",
    drilldown: zendeskTopCustomerReportDrilldown,
    transformFunction: data => tableTransformer(data),
    report_filters_config: ZendeskTopCustomersReportFiltersConfig
  }
};

export default zendeskTopCustomerReport;
