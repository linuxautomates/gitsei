import { statReportTransformer } from "custom-hooks/helpers";
import { REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY
} from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING, WIDGET_VALIDATION_FUNCTION } from "dashboard/constants/filter-name.mapping";
import { ChartContainerType } from "dashboard/helpers/helper";
import { AzureStageBounceSingleStatReportType } from "model/report/azure/stage-bounce-single-stat/stage-bounce-single-stat.model";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  ADO_APPLICATION,
  AZURE_API_BASED_FILTERS,
  AZURE_API_BASED_FILTER_KEY_MAPPING,
  COMMON_FILTER_OPTIONS_MAPPING,
  REPORT_LIST_METHOD
} from "../constant";
import { azureCommonFilterTransformFunc } from "../helpers/commonFilterTransform.helper";
import {
  CHART_PROPS,
  COMPARE_FIELD,
  DEFAULT_QUERY,
  FILTERS,
  REPORT_NAME,
  REQUIRED_FILTERS,
  SUPPORTED_FILTERS,
  SUPPORTED_WIDGET_TYPES,
  URI,
  VALUES_TO_FILTERS
} from "./constants";
import { StageBounceSingleStatReportFiltersConfig } from "./filters.config";
import { widgetValidationFunction } from "./helper";

const stageBounceSingleStat: { azure_stage_bounce_single_stat: AzureStageBounceSingleStatReportType } = {
  azure_stage_bounce_single_stat: {
    name: REPORT_NAME,
    application: ADO_APPLICATION,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: URI,
    method: REPORT_LIST_METHOD,
    filters: FILTERS,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    xaxis: false,
    chart_props: CHART_PROPS,
    default_query: DEFAULT_QUERY,
    compareField: COMPARE_FIELD,
    supported_filters: SUPPORTED_FILTERS,
    requiredFilters: REQUIRED_FILTERS,
    drilldown: {},
    transformFunction: statReportTransformer,
    [FILTER_NAME_MAPPING]: COMMON_FILTER_OPTIONS_MAPPING,
    valuesToFilters: VALUES_TO_FILTERS,
    supported_widget_types: SUPPORTED_WIDGET_TYPES,
    chart_click_enable: false,
    [WIDGET_VALIDATION_FUNCTION]: widgetValidationFunction,
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    prev_report_transformer: azureCommonFilterTransformFunc,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [REPORT_FILTERS_CONFIG]: StageBounceSingleStatReportFiltersConfig
  }
};

export default stageBounceSingleStat;
