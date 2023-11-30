import { trendReportTransformer } from "custom-hooks/helpers";
import { zendeskDrilldown } from "dashboard/constants/drilldown.constants";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ZendeskAgentWaitTimeTrendReportTypes } from "model/report/zendesk/zendesk-agent-wait-time-trend-report/zendeskAgentWaitTimeTrendReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { BASE_ZENDESK_CHART_PROPS } from "../constant";
import { ZENDESK_AGENT_WAIT_TIME_REPORT_TREND_COMPOSITE_TRANSFORM } from "./constant";
import { ZendeskAgentWaitTimeReportTrendsFiltersConfig } from "./filters.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const zendeskAgentWaitTimeTrendReport: {
  zendesk_agent_wait_time_report_trends: ZendeskAgentWaitTimeTrendReportTypes;
} = {
  zendesk_agent_wait_time_report_trends: {
    name: "Support Agent Wait Time Trend Report",
    application: IntegrationTypes.ZENDESK,
    composite: true,
    composite_transform: ZENDESK_AGENT_WAIT_TIME_REPORT_TREND_COMPOSITE_TRANSFORM,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: {
      unit: "Days",
      chartProps: BASE_ZENDESK_CHART_PROPS
    },
    uri: "zendesk_agent_wait_time_report",
    method: "list",
    convertTo: "days",
    drilldown: zendeskDrilldown,
    transformFunction: data => trendReportTransformer(data),
    report_filters_config: ZendeskAgentWaitTimeReportTrendsFiltersConfig
  }
};

export default zendeskAgentWaitTimeTrendReport;
