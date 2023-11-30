import { seriesDataTransformer } from "custom-hooks/helpers";
import { salesforceDrilldown } from "dashboard/constants/drilldown.constants";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SalesforceResolutionTimeReportType } from "model/report/salesforce/salesforce-resolution-time-report/salesforceResolutionTimeReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { SALESFORCE_RESOLUTION_TIME_CHART_PROPS } from "./constant";
import { SalesforceResolutionTimeReportFiltersConfig } from "./filter.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const salesforceResolutionTimeReport: {
  salesforce_resolution_time_report: SalesforceResolutionTimeReportType;
} = {
  salesforce_resolution_time_report: {
    name: "Support Resolution Time Report",
    application: IntegrationTypes.SALESFORCE,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultFilterKey: "median",
    defaultAcross: "status",
    chart_props: SALESFORCE_RESOLUTION_TIME_CHART_PROPS,
    uri: "salesforce_resolution_time_report", //BACKEND CHANGES
    method: "list",
    convertTo: "days",
    drilldown: salesforceDrilldown,
    transformFunction: data => seriesDataTransformer(data),
    HIDE_REPORT: true,
    report_filters_config: SalesforceResolutionTimeReportFiltersConfig
  }
};

export default salesforceResolutionTimeReport;
