import { PREV_REPORT_TRANSFORMER, REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import {
  PARTIAL_FILTER_KEY,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  HIDE_REPORT,
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS
} from "dashboard/constants/filter-key.mapping";
import {
  FILTER_NAME_MAPPING,
  issueManagementCommonFilterOptionsMapping,
  WIDGET_VALIDATION_FUNCTION
} from "dashboard/constants/filter-name.mapping";
import { issueManagementSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { azureCommonPrevQueryTansformer } from "dashboard/helpers/previous-query-transformers/azure/azureCommonPrevQuery.transformer";
import { hygieneWeightValidationHelper } from "dashboard/helpers/widgetValidation.helper";
import { AzureIssueHygieneReportType } from "model/report/azure/hygiene-report/hygiene-report.model";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  ADO_APPLICATION,
  AZURE_API_BASED_FILTER_KEY_MAPPING,
  AZURE_API_BASED_FILTERS,
  REPORT_LIST_METHOD
} from "../constant";
import { azureCommonFilterTransformFunc } from "../helpers/commonFilterTransform.helper";
import {
  DEFAULT_ACROSS,
  DRILL_DOWN,
  HYGIENE_URI,
  CHART_PROPS,
  REPORT_NAME,
  URI,
  DEFAULT_QUERY,
  FILTER_CONFIG_BASED_PREVIEW_FILTERS
} from "./constants";
import { IssueHygieneReportFiltersConfig } from "./filter.config";

const hygieneReport: { azure_hygiene_report: AzureIssueHygieneReportType } = {
  azure_hygiene_report: {
    name: REPORT_NAME,
    application: ADO_APPLICATION,
    chart_type: ChartType?.SCORE,
    chart_container: ChartContainerType.HYGIENE_API_WRAPPER,
    xaxis: true,
    chart_props: CHART_PROPS,
    uri: URI,
    method: REPORT_LIST_METHOD,
    filters: {},
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    defaultAcross: DEFAULT_ACROSS,
    hygiene_uri: HYGIENE_URI,
    drilldown: DRILL_DOWN,
    default_query: DEFAULT_QUERY,
    supported_filters: issueManagementSupportedFilters,
    across: issueManagementSupportedFilters.values,
    widget_filter_transform: azureCommonFilterTransformFunc,
    filter_config_based_preview_filters: FILTER_CONFIG_BASED_PREVIEW_FILTERS,
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [HIDE_REPORT]: true,
    [WIDGET_VALIDATION_FUNCTION]: hygieneWeightValidationHelper,
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [PREV_REPORT_TRANSFORMER]: azureCommonPrevQueryTansformer,
    [REPORT_FILTERS_CONFIG]: IssueHygieneReportFiltersConfig
  }
};

export default hygieneReport;
