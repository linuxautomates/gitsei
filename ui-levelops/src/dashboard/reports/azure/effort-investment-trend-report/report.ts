import { tableTransformer } from "custom-hooks/helpers";
import {
  CHART_DATA_TRANSFORMERS,
  HIDE_CUSTOM_FIELDS,
  PREVIEW_DISABLED,
  PREV_REPORT_TRANSFORMER,
  REPORT_FILTERS_CONFIG
} from "dashboard/constants/applications/names";
import {
  DefaultKeyTypes,
  DISABLE_XAXIS,
  INTERVAL_OPTIONS,
  REPORT_CSV_DOWNLOAD_CONFIG,
  REQUIRED_FILTERS_MAPPING,
  STORE_ACTION,
  TICKET_CATEGORIZATION_SCHEMES_KEY,
  TIME_RANGE_DISPLAY_FORMAT_CONFIG
} from "dashboard/constants/bussiness-alignment-applications/constants";
import {
  effortInvestmentTrendBComTransformer,
  effortInvestmentTrendChartOnClicked
} from "dashboard/constants/bussiness-alignment-applications/helper";
import { azureEffortInvestmentTrendReportDrilldown } from "dashboard/constants/drilldown.constants";
import {
  API_BASED_FILTER,
  DEFAULT_METADATA,
  FIELD_KEY_FOR_FILTERS,
  HIDE_REPORT,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY
} from "dashboard/constants/filter-key.mapping";
import {
  FILTER_NAME_MAPPING,
  issueManagementCommonFilterOptionsMapping,
  WIDGET_VALIDATION_FUNCTION
} from "dashboard/constants/filter-name.mapping";
import { issueManagementEffortInvestmentSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { jiraBaValidationHelper } from "dashboard/helpers/widgetValidation.helper";
import { AzureEffortInvestmentTrendReportType } from "model/report/azure/effort-investment-trend-report/effort-investment-trend-report.model";
import { jiraEffortInvestmentTrendReport } from "reduxConfigs/actions/restapi";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  ADO_APPLICATION,
  AZURE_API_BASED_FILTERS,
  AZURE_API_BASED_FILTER_KEY_MAPPING,
  REPORT_LIST_METHOD
} from "../constant";
import {
  METADATA,
  CHART_DATA_TRANSFORMER_DEFAULT_VALUE,
  CHART_PROPS,
  CSV_DOWNLOAD_CONFIG,
  DEFAULT_QUERY,
  FILTERS,
  REPORT_NAME,
  REQUIRED_FILTER_MAPPING_VALUE,
  SAMPLE_INTERVAL,
  TIME_RANGE_FORMAT_CONFIG,
  URI
} from "./constant";
import { hideCustomFields, reportFilterConfig } from "./helper";

const effortInvestmentTrendReport: {
  azure_effort_investment_trend_report: AzureEffortInvestmentTrendReportType;
} = {
  azure_effort_investment_trend_report: {
    name: REPORT_NAME,
    application: ADO_APPLICATION,
    chart_type: ChartType?.JIRA_EFFORT_ALLOCATION_CHART,
    chart_container: ChartContainerType.BA_WIDGET_API_WRAPPER,
    xaxis: true,
    uri: URI,
    method: REPORT_LIST_METHOD,
    filters: FILTERS,
    chart_props: CHART_PROPS,
    default_query: DEFAULT_QUERY,
    [TIME_RANGE_DISPLAY_FORMAT_CONFIG]: TIME_RANGE_FORMAT_CONFIG,
    supported_filters: issueManagementEffortInvestmentSupportedFilters,
    drilldown: azureEffortInvestmentTrendReportDrilldown,
    shouldJsonParseXAxis: () => true,
    show_max: true,
    onChartClickPayload: effortInvestmentTrendChartOnClicked,
    [DEFAULT_METADATA]: METADATA,
    [INTERVAL_OPTIONS]: SAMPLE_INTERVAL,
    [STORE_ACTION]: jiraEffortInvestmentTrendReport,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PREVIEW_DISABLED]: true,
    [REQUIRED_FILTERS_MAPPING]: REQUIRED_FILTER_MAPPING_VALUE,
    requiredFilters: [TICKET_CATEGORIZATION_SCHEMES_KEY, "interval"],
    [DISABLE_XAXIS]: true,
    [DefaultKeyTypes.DEFAULT_SCHEME_KEY]: true,
    [DefaultKeyTypes.DEFAULT_EFFORT_UNIT]: "azure_effort_investment_tickets",
    [HIDE_REPORT]: true,
    transformFunction: tableTransformer,
    // * REFER To WidgetChartDataTransformerType for typings and more info
    [CHART_DATA_TRANSFORMERS]: CHART_DATA_TRANSFORMER_DEFAULT_VALUE,
    [REPORT_CSV_DOWNLOAD_CONFIG]: CSV_DOWNLOAD_CONFIG,
    [PREV_REPORT_TRANSFORMER]: effortInvestmentTrendBComTransformer,
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [WIDGET_VALIDATION_FUNCTION]: jiraBaValidationHelper,
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [REPORT_FILTERS_CONFIG]: reportFilterConfig,
    [HIDE_CUSTOM_FIELDS]: hideCustomFields
  }
};

export default effortInvestmentTrendReport;
