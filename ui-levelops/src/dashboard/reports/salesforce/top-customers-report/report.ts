import { tableTransformer } from "custom-hooks/helpers";
import { salesforceTopCustomerDrilldown } from "dashboard/constants/drilldown.constants";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SalesforceTopCustomerReportType } from "model/report/salesforce/salesforce-top-customer-report/salesforceTopCustomerReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { SALESFORCE_TOP_CUSTOMER_CHART_PROPS } from "./constant";
import { SalesforceTopCustomersReportFiltersConfig } from "./filter.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const salesforceTopCustomersReport: { salesforce_top_customers_report: SalesforceTopCustomerReportType } = {
  salesforce_top_customers_report: {
    name: "Support Top Customers Report",
    application: IntegrationTypes.SALESFORCE,
    chart_type: ChartType?.TABLE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultAcross: "status",
    chart_props: SALESFORCE_TOP_CUSTOMER_CHART_PROPS,
    uri: "salesforce_hygiene_report", //BACKEND CHANGES
    method: "list",
    drilldown: salesforceTopCustomerDrilldown,
    transformFunction: data => tableTransformer(data),
    HIDE_REPORT: true,
    report_filters_config: SalesforceTopCustomersReportFiltersConfig
  }
};

export default salesforceTopCustomersReport;
