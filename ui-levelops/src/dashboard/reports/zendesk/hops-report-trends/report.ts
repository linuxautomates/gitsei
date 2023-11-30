import { trendReportTransformer } from "custom-hooks/helpers";
import { zendeskDrilldown } from "dashboard/constants/drilldown.constants";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ZendeskHopsTrendReportTypes } from "model/report/zendesk/zendesk-hops-trend-report/zendeskHopsTrendReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { BASE_ZENDESK_CHART_PROPS } from "../constant";
import { ZENDESK_HOPS_TREND_REPORT_COMPOSITE_TRANSFORM } from "./constant";
import { ZendeskHopsReportTrendsFiltersConfig } from "./filters.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const zendeskHopsTrendReport: { zendesk_hops_report_trends: ZendeskHopsTrendReportTypes } = {
  zendesk_hops_report_trends: {
    name: "Support Hops Trend Report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    composite: true,
    composite_transform: ZENDESK_HOPS_TREND_REPORT_COMPOSITE_TRANSFORM,
    chart_props: {
      unit: "Hops",
      chartProps: BASE_ZENDESK_CHART_PROPS
    },
    uri: "zendesk_hops_report",
    method: "list",
    drilldown: zendeskDrilldown,
    transformFunction: data => trendReportTransformer(data),
    report_filters_config: ZendeskHopsReportTrendsFiltersConfig
  }
};

export default zendeskHopsTrendReport;
