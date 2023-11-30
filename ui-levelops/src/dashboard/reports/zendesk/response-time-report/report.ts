import { seriesDataTransformer } from "custom-hooks/helpers";
import { zendeskDrilldown } from "dashboard/constants/drilldown.constants";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ZendeskResponseTimeReportTypes } from "model/report/zendesk/zendesk-response-time-report/zendeskResponseTimeReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { ZENDESK_RESPONSE_TIME_CHART_PROPS } from "./constant";
import { ZendeskResponseTimeReportFiltersConfig } from "./filters.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const zendeskResponseTimeReport: { zendesk_response_time_report: ZendeskResponseTimeReportTypes } = {
  zendesk_response_time_report: {
    name: "Support Response Time Report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultFilterKey: "median",
    chart_props: ZENDESK_RESPONSE_TIME_CHART_PROPS,
    uri: "zendesk_response_time_report",
    method: "list",
    defaultAcross: "brand",
    convertTo: "days",
    drilldown: zendeskDrilldown,
    transformFunction: data => seriesDataTransformer(data),
    report_filters_config: ZendeskResponseTimeReportFiltersConfig
  }
};

export default zendeskResponseTimeReport;
