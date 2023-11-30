import { seriesDataTransformer } from "custom-hooks/helpers";
import { zendeskDrilldown } from "dashboard/constants/drilldown.constants";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ZendeskRequesterWaitTimeReportTypes } from "model/report/zendesk/zendesk-requester-wait-time/zendeskRequesterWaitTime.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { ZENDESK_REQUESTER_WAIT_TIME_CHART_PROPS } from "./constant";
import { ZendeskRequesterWaitTimeReportFiltersConfig } from "./filters.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const zendeskRequesterWaitTimeReport: { zendesk_requester_wait_time_report: ZendeskRequesterWaitTimeReportTypes } = {
  zendesk_requester_wait_time_report: {
    name: "Support Requester Wait Time Report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultFilterKey: "median",
    defaultAcross: "brand",
    chart_props: ZENDESK_REQUESTER_WAIT_TIME_CHART_PROPS,
    uri: "zendesk_requester_wait_time_report",
    method: "list",
    convertTo: "days",
    drilldown: zendeskDrilldown,
    transformFunction: data => seriesDataTransformer(data),
    report_filters_config: ZendeskRequesterWaitTimeReportFiltersConfig
  }
};

export default zendeskRequesterWaitTimeReport;
