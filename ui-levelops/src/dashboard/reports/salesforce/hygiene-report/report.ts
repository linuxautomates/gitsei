import { salesforceDrilldown } from "dashboard/constants/drilldown.constants";
import { hygieneDefaultSettings } from "dashboard/constants/helper";
import { ChartContainerType } from "dashboard/helpers/helper";
import { hygieneWeightValidationHelper } from "dashboard/helpers/widgetValidation.helper";
import { SalesforceHygieneReportType } from "model/report/salesforce/salesforce-hygiene-report/salesforceHygieneReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { SALESFORCE_HYGIENE_REPORT, SALESFORCE_HYGIENE_TYPES } from "./constant";
import { SalesforceHygieneReportFiltersConfig } from "./filter.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const salesforceHygieneReport: { salesforce_hygiene_report: SalesforceHygieneReportType } = {
  salesforce_hygiene_report: {
    name: "Support Hygiene Report",
    application: IntegrationTypes.SALESFORCE,
    chart_type: ChartType?.SCORE,
    chart_container: ChartContainerType.HYGIENE_API_WRAPPER,
    chart_props: SALESFORCE_HYGIENE_REPORT,
    uri: "salesforce_hygiene_report",
    method: "list",
    hygiene_uri: "salesforce_tickets",
    hygiene_trend_uri: "salesforce_tickets",
    hygiene_types: SALESFORCE_HYGIENE_TYPES,
    default_query: hygieneDefaultSettings,
    drilldown: salesforceDrilldown,
    preview_disabled: true,
    HIDE_REPORT: true,
    widget_validation_function: hygieneWeightValidationHelper,
    report_filters_config: SalesforceHygieneReportFiltersConfig
  }
};

export default salesforceHygieneReport;
