import { seriesDataTransformer } from "custom-hooks/helpers";
import { zendeskDrilldown } from "dashboard/constants/drilldown.constants";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ZendeskReopensReportTypes } from "model/report/zendesk/zendesk-reopens-report/zendeskReopensReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { ZENDESK_REPORT_CHART_PROPS } from "./constant";
import { ZendeskTicketsReopensReportFiltersConfig } from "./filters.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const zendeskReopensReport: { zendesk_reopens_report: ZendeskReopensReportTypes } = {
  zendesk_reopens_report: {
    name: "Support Ticket Reopens Report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultFilterKey: "median",
    defaultAcross: "brand",
    chart_props: ZENDESK_REPORT_CHART_PROPS,
    uri: "zendesk_reopens_report",
    method: "list",
    drilldown: zendeskDrilldown,
    transformFunction: data => seriesDataTransformer(data),
    report_filters_config: ZendeskTicketsReopensReportFiltersConfig
  }
};

export default zendeskReopensReport;
