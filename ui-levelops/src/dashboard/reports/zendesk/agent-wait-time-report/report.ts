import { seriesDataTransformer } from "custom-hooks/helpers";
import { zendeskDrilldown } from "dashboard/constants/drilldown.constants";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ZendeskAgentWaitTimeReportTypes } from "model/report/zendesk/zendesk-agent-wait-time-report/zendeskAgentWaitTimeReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { ZENDESK_AGENT_WAIT_TIME_CHART_PROPS } from "./constant";
import { ZendeskAgentWaitTimeReportFiltersConfig } from "./filters.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const zendeskAgentWaitTimeReport: { zendesk_agent_wait_time_report: ZendeskAgentWaitTimeReportTypes } = {
  zendesk_agent_wait_time_report: {
    name: "Support Agent Wait Time Report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultFilterKey: "median",
    chart_props: ZENDESK_AGENT_WAIT_TIME_CHART_PROPS,
    uri: "zendesk_agent_wait_time_report",
    method: "list",
    defaultAcross: "brand",
    convertTo: "days",
    drilldown: zendeskDrilldown,
    transformFunction: data => seriesDataTransformer(data),
    report_filters_config: ZendeskAgentWaitTimeReportFiltersConfig
  }
};

export default zendeskAgentWaitTimeReport;
