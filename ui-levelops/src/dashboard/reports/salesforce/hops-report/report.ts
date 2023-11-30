import { seriesDataTransformer } from "custom-hooks/helpers";
import { salesforceDrilldown } from "dashboard/constants/drilldown.constants";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SalesforceHopsReportType } from "model/report/salesforce/salesforce-hops-report/salesforceHopsReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { BASE_SALESFORCE_CHART_PROPS } from "../constant";
import { SalesforceHopsReportFiltersConfig } from "./filter.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const salesforceHopsReport: { salesforce_hops_report: SalesforceHopsReportType } = {
  salesforce_hops_report: {
    name: "Support Hops Report",
    application: IntegrationTypes.SALESFORCE,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultFilterKey: "median",
    defaultAcross: "status",
    chart_props: BASE_SALESFORCE_CHART_PROPS,
    uri: "salesforce_hops_report",
    method: "list",
    drilldown: salesforceDrilldown,
    transformFunction: data => seriesDataTransformer(data),
    HIDE_REPORT: true,
    report_filters_config: SalesforceHopsReportFiltersConfig
  }
};

export default salesforceHopsReport;
