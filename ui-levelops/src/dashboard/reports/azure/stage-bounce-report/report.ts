import { stageBounceDataTransformer } from "custom-hooks/helpers/stageBounce.helper";
import {
  INFO_MESSAGES,
  PREV_REPORT_TRANSFORMER,
  REPORT_FILTERS_CONFIG,
  STACKS_FILTER_STATUS
} from "dashboard/constants/applications/names";
import { azureDrilldown } from "dashboard/constants/drilldown.constants";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  HIDE_REPORT,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY
} from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING, WIDGET_VALIDATION_FUNCTION } from "dashboard/constants/filter-name.mapping";
import { ChartContainerType } from "dashboard/helpers/helper";
import { azureCommonPrevQueryTansformer } from "dashboard/helpers/previous-query-transformers/azure/azureCommonPrevQuery.transformer";
import { AzureStageBounceReportType } from "model/report/azure/stage-bounce-report/stage-bounce-report.model";
import { getXAxisLabel } from "shared-resources/charts/bar-chart/bar-chart.helper";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  ADO_APPLICATION,
  AZURE_API_BASED_FILTERS,
  AZURE_API_BASED_FILTER_KEY_MAPPING,
  COMMON_FILTER_OPTIONS_MAPPING,
  REPORT_LIST_METHOD
} from "../constant";
import {
  AZURE_ACROSS_OPTION,
  CHART_PROPS,
  DEFAULT_ACROSS,
  DEFAULT_QUERY,
  INFORMATION_MESSAGE,
  REPORT_NAME,
  REQUIRED_FILTERS,
  STACK_FILTERS,
  SUPPORTED_FILTERS,
  TOOLTIP_MAPPING,
  URI,
  VALUES_TO_FILTERS
} from "./constant";
import { StageBounceReportFiltersConfig } from "./filter.config";
import { getTotalKey, onChartClickPayload, stackFilterStatus, widgetValidationFunction } from "./helper";

const stageBounceReport: { azure_stage_bounce_report: AzureStageBounceReportType } = {
  azure_stage_bounce_report: {
    name: REPORT_NAME,
    application: ADO_APPLICATION,
    xaxis: true,
    across: AZURE_ACROSS_OPTION,
    defaultAcross: DEFAULT_ACROSS,
    chart_type: ChartType?.STAGE_BOUNCE_CHART,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: CHART_PROPS,
    default_query: DEFAULT_QUERY,
    uri: URI,
    method: REPORT_LIST_METHOD,
    filters: {},
    supported_filters: SUPPORTED_FILTERS,
    stack_filters: STACK_FILTERS,
    drilldown: azureDrilldown,
    transformFunction: stageBounceDataTransformer,
    valuesToFilters: VALUES_TO_FILTERS,
    tooltipMapping: TOOLTIP_MAPPING,
    requiredFilters: REQUIRED_FILTERS,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [FILTER_NAME_MAPPING]: COMMON_FILTER_OPTIONS_MAPPING,
    [WIDGET_VALIDATION_FUNCTION]: widgetValidationFunction,
    getTotalKey: getTotalKey,
    [STACKS_FILTER_STATUS]: stackFilterStatus,
    [INFO_MESSAGES]: INFORMATION_MESSAGE,
    xAxisLabelTransform: getXAxisLabel,
    onChartClickPayload: onChartClickPayload,
    [HIDE_REPORT]: true,
    shouldJsonParseXAxis: () => true,
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [REPORT_FILTERS_CONFIG]: StageBounceReportFiltersConfig,
    [PREV_REPORT_TRANSFORMER]: azureCommonPrevQueryTansformer
  }
};

export default stageBounceReport;
