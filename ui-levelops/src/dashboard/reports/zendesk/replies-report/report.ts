import { seriesDataTransformer } from "custom-hooks/helpers";
import { zendeskDrilldown } from "dashboard/constants/drilldown.constants";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ZendeskRepliesReportTypes } from "model/report/zendesk/zendesk-replies-report/zendeskRepliesReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { ZENDESK_REPLIES_CHART_PROPS } from "./constant";
import { ZendeskRepliesReportFiltersConfig } from "./filters.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const zendeskRepliesReport: { zendesk_replies_report: ZendeskRepliesReportTypes } = {
  zendesk_replies_report: {
    name: "Support Replies Report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultFilterKey: "median",
    defaultAcross: "brand",
    chart_props: ZENDESK_REPLIES_CHART_PROPS,
    uri: "zendesk_replies_report",
    method: "list",
    filters: {},
    drilldown: zendeskDrilldown,
    transformFunction: data => seriesDataTransformer(data),
    report_filters_config: ZendeskRepliesReportFiltersConfig
  }
};

export default zendeskRepliesReport;
