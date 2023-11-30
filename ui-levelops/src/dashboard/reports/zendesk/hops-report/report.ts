import { seriesDataTransformer } from "custom-hooks/helpers";
import { zendeskDrilldown } from "dashboard/constants/drilldown.constants";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ZendeskHopsReportTypes } from "model/report/zendesk/zendesk-hops-report/zendeskHopsReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { ZENDESK_HOPS_CHART_PROPS } from "./constant";
import { ZendeskHopsReportFiltersConfig } from "./filters.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const zendeskHopsReport: { zendesk_hops_report: ZendeskHopsReportTypes } = {
  zendesk_hops_report: {
    name: "Support Hops report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultFilterKey: "median",
    chart_props: ZENDESK_HOPS_CHART_PROPS,
    uri: "zendesk_hops_report",
    method: "list",
    defaultAcross: "brand",
    drilldown: zendeskDrilldown,
    transformFunction: data => seriesDataTransformer(data),
    report_filters_config: ZendeskHopsReportFiltersConfig
  }
};

export default zendeskHopsReport;
