import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { ChartContainerType } from "../../../helpers/helper";
import { issueManagementEffortInvestmentSupportedFilters } from "../../../constants/supported-filters.constant";
import { azureStatDrilldown } from "../../../constants/drilldown.constants";
import {
  API_BASED_FILTER,
  DEFAULT_METADATA,
  FIELD_KEY_FOR_FILTERS,
  HIDE_REPORT,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY
} from "../../../constants/filter-key.mapping";
import {
  azureEITimeRangeDefMeta,
  DefaultKeyTypes,
  REQUIRED_FILTERS_MAPPING,
  STORE_ACTION
} from "../../../constants/bussiness-alignment-applications/constants";
import {
  FILTER_NAME_MAPPING,
  issueManagementCommonFilterOptionsMapping,
  WIDGET_VALIDATION_FUNCTION
} from "../../../constants/filter-name.mapping";
import {
  HIDE_CUSTOM_FIELDS,
  PREV_REPORT_TRANSFORMER,
  REPORT_FILTERS_CONFIG
} from "../../../constants/applications/names";
import { effortInvestmentSingleStatBComTransformer } from "../../../constants/bussiness-alignment-applications/helper";
import { tableTransformer } from "../../../../custom-hooks/helpers";
import { jiraBaValidationHelper } from "../../../helpers/widgetValidation.helper";
import {
  ADO_APPLICATION,
  AZURE_API_BASED_FILTER_KEY_MAPPING,
  AZURE_API_BASED_FILTERS,
  REPORT_LIST_METHOD
} from "../constant";
import { jiraEffortInvestmentStat } from "reduxConfigs/actions/restapi";
import {
  DEFAULT_QUERY,
  REQUIRED_FILTERS,
  DISPLAY_FORMAT_KEY,
  EFFORT_UNIT,
  REPORT_NAME,
  SUPPORTED_WIDGET_TYPES,
  URI
} from "./constants";
import { AzureEffortInvestmentSingleStatType } from "../../../../model/report/azure/effort-investment-single-stat/effort-investment-single-stat.model";
import { getReportConfig, hideCustomFields } from "./helper";

const effortInvestmentSingleStatReport: {
  azure_effort_investment_single_stat: AzureEffortInvestmentSingleStatType;
} = {
  azure_effort_investment_single_stat: {
    name: REPORT_NAME,
    application: ADO_APPLICATION,
    chart_type: ChartType?.EFFORT_INVESTMENT_STAT,
    chart_container: ChartContainerType.BA_WIDGET_API_WRAPPER,
    uri: URI,
    method: REPORT_LIST_METHOD,
    filters: {},
    default_query: DEFAULT_QUERY,
    xaxis: false,
    supported_filters: issueManagementEffortInvestmentSupportedFilters,
    drilldown: azureStatDrilldown,
    supported_widget_types: SUPPORTED_WIDGET_TYPES,
    [DEFAULT_METADATA]: azureEITimeRangeDefMeta,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [STORE_ACTION]: jiraEffortInvestmentStat,
    [REQUIRED_FILTERS_MAPPING]: REQUIRED_FILTERS,
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [HIDE_REPORT]: true,
    [DefaultKeyTypes.DEFAULT_DISPLAY_FORMAT_KEY]: DISPLAY_FORMAT_KEY,
    [DefaultKeyTypes.DEFAULT_EFFORT_UNIT]: EFFORT_UNIT,
    [DefaultKeyTypes.DEFAULT_SCHEME_KEY]: true, // if true then default scheme is selected by default
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [PREV_REPORT_TRANSFORMER]: effortInvestmentSingleStatBComTransformer,
    [WIDGET_VALIDATION_FUNCTION]: jiraBaValidationHelper,
    transformFunction: tableTransformer,
    [REPORT_FILTERS_CONFIG]: getReportConfig,
    [HIDE_CUSTOM_FIELDS]: hideCustomFields
  }
};

export default effortInvestmentSingleStatReport;
