import { zendeskDrilldown } from "dashboard/constants/drilldown.constants";
import { ChartContainerType } from "dashboard/helpers/helper";
import { hygieneWeightValidationHelper } from "dashboard/helpers/widgetValidation.helper";
import { ZendeskHygieneTrendReportTypes } from "model/report/zendesk/zendesk-hygiene-trend-report/zendeskHygieneTrendReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { BASE_ZENDESK_CHART_PROPS } from "../constant";
import { ZENDESK_HYGIENE_TYPES } from "../hygiene-report/constants";
import { ZendeskHygieneReportTrendsFiltersConfig } from "./filters.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const zendeskHygieneTrendReport: { zendesk_hygiene_report_trends: ZendeskHygieneTrendReportTypes } = {
  zendesk_hygiene_report_trends: {
    name: "Support Hygiene Trend Report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.HYGIENE_API_WRAPPER,
    chart_props: {
      unit: "Score",
      chartProps: BASE_ZENDESK_CHART_PROPS
    },
    uri: "zendesk_hygiene_report",
    method: "list",
    filters: {
      across: "trend"
    },
    hygiene_uri: "zendesk_tickets",
    hygiene_trend_uri: "zendesk_tickets_report",
    hygiene_types: ZENDESK_HYGIENE_TYPES,
    drilldown: zendeskDrilldown,
    widget_validation_function: hygieneWeightValidationHelper,
    report_filters_config: ZendeskHygieneReportTrendsFiltersConfig
  }
};

export default zendeskHygieneTrendReport;
