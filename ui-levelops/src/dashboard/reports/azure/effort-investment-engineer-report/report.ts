import { tableTransformer } from "custom-hooks/helpers";
import {
  HIDE_CUSTOM_FIELDS,
  PREVIEW_DISABLED,
  PREV_REPORT_TRANSFORMER,
  REPORT_FILTERS_CONFIG
} from "dashboard/constants/applications/names";
import {
  DefaultKeyTypes,
  DISABLE_XAXIS,
  REPORT_CSV_DOWNLOAD_CONFIG,
  REQUIRED_FILTERS_MAPPING,
  TICKET_CATEGORIZATION_SCHEMES_KEY,
  WIDGET_MIN_HEIGHT
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { API_BASED_FILTER, DEFAULT_METADATA, FIELD_KEY_FOR_FILTERS } from "dashboard/constants/filter-key.mapping";
import { WIDGET_VALIDATION_FUNCTION } from "dashboard/constants/filter-name.mapping";
import { issueManagementEffortInvestmentSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { azureCommonPrevQueryTansformer } from "dashboard/helpers/previous-query-transformers/azure/azureCommonPrevQuery.transformer";
import { jiraBaValidationHelper } from "dashboard/helpers/widgetValidation.helper";
import { AzureEffortInvestmentEngineerReportType } from "model/report/azure/effort-investment-engineer-report/effort-investment-engineer-report.model";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  ADO_APPLICATION,
  AZURE_API_BASED_FILTERS,
  AZURE_API_BASED_FILTER_KEY_MAPPING,
  REPORT_LIST_METHOD
} from "../constant";
import {
  CSV_CONFIG,
  DEFAULT_ACROSS,
  DEFAULT_METADATA_VALUE,
  DEFAULT_QUERY,
  EFFORT_UNIT,
  MIN_WIDTH,
  REPORT_NAME,
  REQUIRED_FILTERS_MAPPING_VALUE,
  URI
} from "./constants";
import { hideCustomFields, reportFilterConfig } from "./helper";

const effortInvestmentEngineerReport: {
  azure_effort_investment_engineer_report: AzureEffortInvestmentEngineerReportType;
} = {
  azure_effort_investment_engineer_report: {
    name: REPORT_NAME,
    application: ADO_APPLICATION,
    chart_type: ChartType?.ENGINEER_TABLE,
    chart_container: ChartContainerType.BA_WIDGET_API_WRAPPER,
    xaxis: false,
    defaultAcross: DEFAULT_ACROSS,
    uri: URI,
    method: REPORT_LIST_METHOD,
    filters: {},
    chart_props: {},
    supported_filters: issueManagementEffortInvestmentSupportedFilters,
    drilldown: {},
    requiredFilters: [TICKET_CATEGORIZATION_SCHEMES_KEY],
    default_query: DEFAULT_QUERY,
    [DEFAULT_METADATA]: DEFAULT_METADATA_VALUE,
    [WIDGET_MIN_HEIGHT]: MIN_WIDTH,
    [PREVIEW_DISABLED]: true,
    [REQUIRED_FILTERS_MAPPING]: REQUIRED_FILTERS_MAPPING_VALUE,
    [DISABLE_XAXIS]: true,
    /**
     * Refer to @type ReportCSVDownloadConfig for more info on fields
     */
    [REPORT_CSV_DOWNLOAD_CONFIG]: CSV_CONFIG,
    [DefaultKeyTypes.DEFAULT_SCHEME_KEY]: true,
    [DefaultKeyTypes.DEFAULT_EFFORT_UNIT]: EFFORT_UNIT,
    transformFunction: tableTransformer,
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [WIDGET_VALIDATION_FUNCTION]: jiraBaValidationHelper,
    [REPORT_FILTERS_CONFIG]: reportFilterConfig,
    [HIDE_CUSTOM_FIELDS]: hideCustomFields,
    [PREV_REPORT_TRANSFORMER]: azureCommonPrevQueryTansformer
  }
};

export default effortInvestmentEngineerReport;
