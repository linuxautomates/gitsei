import { seriesDataTransformer } from "custom-hooks/helpers";
import { zendeskDrilldown } from "dashboard/constants/drilldown.constants";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ZendeskBounceReportTypes } from "model/report/zendesk/zendesk-bounce-report/zendeskBounceReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { ZendeskBounceReportFiltersConfig } from "./filters.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const zendeskBounceReport: { zendesk_bounce_report: ZendeskBounceReportTypes } = {
  zendesk_bounce_report: {
    name: "Support Bounce Report",
    application: IntegrationTypes.ZENDESK,
    defaultAcross: "brand",
    chart_type: ChartType?.SCATTER,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: {
      yDataKey: "median",
      rangeY: ["min", "max"],
      unit: "Bounces"
    },
    uri: "zendesk_bounce_report",
    method: "list",
    drilldown: zendeskDrilldown,
    transformFunction: data => seriesDataTransformer(data),
    report_filters_config: ZendeskBounceReportFiltersConfig
  }
};

export default zendeskBounceReport;
