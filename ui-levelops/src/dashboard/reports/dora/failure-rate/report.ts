import {
  API_BASED_FILTER,
  CATEGORY,
  DEFAULT_METADATA,
  FIELD_KEY_FOR_FILTERS,
  GET_CUSTOMIZE_TITLE,
  IS_FRONTEND_REPORT,
  SHOW_SETTINGS_TAB
} from "dashboard/constants/filter-key.mapping";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { DoraFailureRateReportType } from "model/report/dora/failure-rate/failureRate.constants";
import {
  CHANGE_FAILURE_RATE_WIDGET_DESCRIPTION,
  DRILLDOWN_TOGGLE_CONFIG,
  FILTER_WARNING_LABEL,
  doraApiBasedFilterKeyMapping,
  doraIMApiBasedFilters,
  doraSupportedFilters
} from "../constants";
import { changeFailureRateDrilldown } from "dashboard/constants/drilldown.constants";
import { GET_GRAPH_FILTERS, PREV_REPORT_TRANSFORMER, REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import {
  getDoraProfileIntegrationId,
  getDoraProfileIntegrationType,
  getFilterConfig,
  getShowTitle,
  mapFiltersBeforeCall,
  getFilterKeysToHide,
  getDoraSingleStateValue,
  getDeployementProfileRoute,
} from "./helper";
import { CHART_TITLE } from "./constants";
import { doraFilterNameMapping, getConditionalUri, getDefaultMetadata, getDefaultQuery, getDoraGrapthFilters, getDoraProfileIntegrationApplication, getDrilldownTitle, getHideFilterButton, onChartClickPayload, prevReportTransformer } from "../helper";
import { getDoraReportsTitle } from "../doraReportTitle";
import { FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";

const chartProps = {
  barGap: 0,
  margin: { top: 20, right: 5, left: 5, bottom: 100 }
};

const failureRateReport: { change_failure_rate: DoraFailureRateReportType } = {
  change_failure_rate: {
    name: "Change Failure Rate",
    application: "any",
    chart_type: ChartType.DORA_COMBINED_BAR_CHART,
    chart_container: ChartContainerType.DORA_API_WRAPPER,
    drilldown: changeFailureRateDrilldown,
    uri: "",
    method: "list",
    [IS_FRONTEND_REPORT]: true,
    [CATEGORY]: "dora",
    [SHOW_SETTINGS_TAB]: true,
    description: CHANGE_FAILURE_RATE_WIDGET_DESCRIPTION,
    [REPORT_FILTERS_CONFIG]: getFilterConfig,
    [API_BASED_FILTER]: doraIMApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: doraApiBasedFilterKeyMapping,
    [GET_CUSTOMIZE_TITLE]: getDoraReportsTitle,
    supported_filters: doraSupportedFilters,
    conditionalUriMethod: getConditionalUri,
    defaultAcross: "velocity",
    chart_props: {
      unit: "Failures",
      chartTitle: CHART_TITLE,
      barProps: [
        {
          unit: "Failures",
          dataKey: "count",
          name: "count"
        }
      ],
      stacked: false,
      chartProps: chartProps
    },
    isAdvancedFilterSetting: true,
    filterWarningLabel: FILTER_WARNING_LABEL,
    mapFiltersBeforeCall: mapFiltersBeforeCall,
    getDoraProfileIntegrationType: getDoraProfileIntegrationType,
    getDrilldownTitle: getDrilldownTitle,
    onChartClickPayload: onChartClickPayload,
    getShowTitle: getShowTitle,
    hideFilterButton: getHideFilterButton,
    getFilterKeysToHide: getFilterKeysToHide,
    getDoraProfileIntegrationId: getDoraProfileIntegrationId,
    getDoraSingleStateValue: getDoraSingleStateValue,
    [DEFAULT_METADATA]: getDefaultMetadata,
    [PREV_REPORT_TRANSFORMER]: prevReportTransformer,
    default_query: getDefaultQuery,
    [GET_GRAPH_FILTERS]: getDoraGrapthFilters,
    getDoraProfileDeploymentRoute:getDeployementProfileRoute,
    getDoraProfileIntegrationApplication: getDoraProfileIntegrationApplication,
    [FILTER_NAME_MAPPING]: doraFilterNameMapping,
  }
};

export default failureRateReport;
