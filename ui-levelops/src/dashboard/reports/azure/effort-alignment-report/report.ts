import { tableTransformer } from "custom-hooks/helpers";
import {
  PREVIEW_DISABLED,
  PREV_REPORT_TRANSFORMER,
  REPORT_FILTERS_CONFIG
} from "dashboard/constants/applications/names";
import {
  DefaultKeyTypes,
  DISABLE_XAXIS,
  REPORT_CSV_DOWNLOAD_CONFIG,
  REQUIRED_FILTERS_MAPPING,
  STORE_ACTION,
  TICKET_CATEGORIZATION_SCHEMES_KEY
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { EffortUnitType } from "dashboard/constants/enums/jira-ba-reports.enum";
import { API_BASED_FILTER, DEFAULT_METADATA, FIELD_KEY_FOR_FILTERS } from "dashboard/constants/filter-key.mapping";
import { WIDGET_VALIDATION_FUNCTION } from "dashboard/constants/filter-name.mapping";
import { WIDGET_MIN_HEIGHT } from "dashboard/constants/helper";
import { issueManagementEffortInvestmentSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { azureCommonPrevQueryTansformer } from "dashboard/helpers/previous-query-transformers/azure/azureCommonPrevQuery.transformer";
import { jiraBaValidationHelper } from "dashboard/helpers/widgetValidation.helper";
import { AzureEffortAlignmentReportType } from "model/report/azure/effort-allignment-report/effort-allignment-report.model";
import { jiraAlignmentReport } from "reduxConfigs/actions/restapi/jiraBAProgressActions";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  ADO_APPLICATION,
  AZURE_API_BASED_FILTERS,
  AZURE_API_BASED_FILTER_KEY_MAPPING,
  AZURE_EI_TIME_RANGE_DEF_META,
  REPORT_LIST_METHOD
} from "../constant";
import {
  DEFAULT_ACROSS,
  DEFAULT_QUERY,
  MIN_WIDTH,
  REPORT_CSV_DEFAULT_CONFIG,
  REPORT_NAME,
  REQUIRED_FILTER_MAPPING,
  URI
} from "./constants";
import { AzureEffortAlignmentReportFiltersConfig } from "./filters.config";

const effortAlignmentReport: { azure_effort_alignment_report: AzureEffortAlignmentReportType } = {
  azure_effort_alignment_report: {
    name: REPORT_NAME,
    application: ADO_APPLICATION,
    chart_type: ChartType.ALIGNMENT_TABLE,
    chart_container: ChartContainerType.BA_WIDGET_API_WRAPPER,
    xaxis: true,
    defaultAcross: DEFAULT_ACROSS,
    uri: URI,
    method: REPORT_LIST_METHOD,
    filters: {},
    chart_props: {},
    supported_filters: issueManagementEffortInvestmentSupportedFilters,
    drilldown: {},
    [WIDGET_MIN_HEIGHT]: MIN_WIDTH,
    requiredFilters: [TICKET_CATEGORIZATION_SCHEMES_KEY],
    [DEFAULT_METADATA]: AZURE_EI_TIME_RANGE_DEF_META,
    default_query: DEFAULT_QUERY,
    [STORE_ACTION]: jiraAlignmentReport,
    [PREVIEW_DISABLED]: true,
    [REQUIRED_FILTERS_MAPPING]: REQUIRED_FILTER_MAPPING,
    [DISABLE_XAXIS]: true,
    [DefaultKeyTypes.DEFAULT_SCHEME_KEY]: true,
    [DefaultKeyTypes.DEFAULT_EFFORT_UNIT]: EffortUnitType.AZURE_TICKETS_REPORT,
    [REPORT_CSV_DOWNLOAD_CONFIG]: REPORT_CSV_DEFAULT_CONFIG,
    transformFunction: tableTransformer,
    [WIDGET_VALIDATION_FUNCTION]: jiraBaValidationHelper,
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [REPORT_FILTERS_CONFIG]: AzureEffortAlignmentReportFiltersConfig,
    [PREV_REPORT_TRANSFORMER]: azureCommonPrevQueryTansformer
  }
};
export default effortAlignmentReport;
