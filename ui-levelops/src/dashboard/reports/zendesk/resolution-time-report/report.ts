import { seriesDataTransformer } from "custom-hooks/helpers";
import { zendeskDrilldown } from "dashboard/constants/drilldown.constants";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ZendeskResolutionTimeReportTypes } from "model/report/zendesk/zendesk-resolution-time-report/zendeskResolutionTimeReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { ZENDESK_RESOLUTION_TIME_CHART_PROPS } from "./constant";
import { ZendeskResolutionTimeReportFiltersConfig } from "./filters.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const zendeskResolutionTimeReport: { zendesk_resolution_time_report: ZendeskResolutionTimeReportTypes } = {
  zendesk_resolution_time_report: {
    name: "Support Resolution Time Report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultFilterKey: "median",
    defaultAcross: "brand",
    chart_props: ZENDESK_RESOLUTION_TIME_CHART_PROPS,
    uri: "zendesk_resolution_time_report",
    method: "list",
    convertTo: "days",
    drilldown: zendeskDrilldown,
    transformFunction: data => seriesDataTransformer(data),
    report_filters_config: ZendeskResolutionTimeReportFiltersConfig
  }
};

export default zendeskResolutionTimeReport;
