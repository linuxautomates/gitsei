import { zendeskDrilldown } from "dashboard/constants/drilldown.constants";
import { hygieneDefaultSettings } from "dashboard/constants/helper";
import { ChartContainerType } from "dashboard/helpers/helper";
import { hygieneWeightValidationHelper } from "dashboard/helpers/widgetValidation.helper";
import { ZendeskHygieneReportTypes } from "model/report/zendesk/zendesk-hygiene-report/zendeskHygieneReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { zendeskSupportedFilters } from "../constant";
import { ZENDESK_HYGIENE_REPORT_CHART_PROPS, ZENDESK_HYGIENE_TYPES } from "./constants";
import { ZendeskHygieneReportFiltersConfig } from "./filters.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const zendeskHygieneReport: { zendesk_hygiene_report: ZendeskHygieneReportTypes } = {
  zendesk_hygiene_report: {
    name: "Support Hygiene Report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.SCORE,
    chart_container: ChartContainerType.HYGIENE_API_WRAPPER,
    chart_props: ZENDESK_HYGIENE_REPORT_CHART_PROPS,
    uri: "zendesk_hygiene_report",
    method: "list",
    hygiene_uri: "zendesk_tickets",
    hygiene_trend_uri: "zendesk_tickets_report",
    hygiene_types: ZENDESK_HYGIENE_TYPES,
    drilldown: zendeskDrilldown,
    preview_disabled: true,
    supported_filters: zendeskSupportedFilters,
    default_query: hygieneDefaultSettings,
    widget_validation_function: hygieneWeightValidationHelper,
    report_filters_config: ZendeskHygieneReportFiltersConfig
  }
};

export default zendeskHygieneReport;
