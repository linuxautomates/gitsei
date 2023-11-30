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
import { DoraDeploymentFrequencyReportType } from "model/report/dora/deployment-frequency/deploymentFrequency.constants";
import {
  DEPLOYMENT_FREQUENCY_DESCRIPTION,
  DRILLDOWN_TOGGLE_CONFIG,
  FILTER_WARNING_LABEL,
  doraApiBasedFilterKeyMapping,
  doraIMApiBasedFilters,
  doraSupportedFilters
} from "../constants";
import { deploymentFrequencyDrilldown } from "dashboard/constants/drilldown.constants";
import { GET_GRAPH_FILTERS, PREV_REPORT_TRANSFORMER, REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import {
  getDoraProfileIntegrationId,
  getDoraProfileIntegrationType,
  getFilterConfig,
  getShowTitle,
  mapFiltersBeforeCall,
  getFilterKeysToHide,
  getDeployementProfileRoute,
} from "./helper";
import { CHART_TITLE } from "./constants";
import { doraFilterNameMapping, getConditionalUri, getDefaultMetadata, getDefaultQuery, getDoraGrapthFilters, getDoraProfileIntegrationApplication, getDoraSingleStateValue, getDrilldownTitle, getHideFilterButton, onChartClickPayload, prevReportTransformer } from "../helper";
import { getDoraReportsTitle } from "../doraReportTitle";
import { FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";

const chartProps = {
  barGap: 0,
  margin: { top: 20, right: 5, left: 5, bottom: 50 }
};

const deploymentFrequencyReport: { deployment_frequency_report: DoraDeploymentFrequencyReportType } = {
  deployment_frequency_report: {
    name: "Deployment Frequency",
    application: "any",
    chart_type: ChartType.DORA_COMBINED_BAR_CHART,
    chart_container: ChartContainerType.DORA_API_WRAPPER,
    drilldown: deploymentFrequencyDrilldown,
    uri: "",
    method: "list",
    [IS_FRONTEND_REPORT]: true,
    [CATEGORY]: "dora",
    [SHOW_SETTINGS_TAB]: true,
    description: DEPLOYMENT_FREQUENCY_DESCRIPTION,
    isAdvancedFilterSetting: true,
    [REPORT_FILTERS_CONFIG]: getFilterConfig,
    [API_BASED_FILTER]: doraIMApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: doraApiBasedFilterKeyMapping,
    supported_filters: doraSupportedFilters,
    conditionalUriMethod: getConditionalUri,
    [GET_CUSTOMIZE_TITLE]: getDoraReportsTitle,
    defaultAcross: "velocity",
    chart_props: {
      unit: "Deployments",
      chartTitle: CHART_TITLE,
      barProps: [
        {
          unit: "Deployments",
          dataKey: "count",
          name: "count"
        }
      ],
      stacked: false,
      chartProps: chartProps
    },
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
    getDoraProfileDeploymentRoute: getDeployementProfileRoute,
    getDoraProfileIntegrationApplication: getDoraProfileIntegrationApplication,
    [FILTER_NAME_MAPPING]: doraFilterNameMapping,
  }
};

export default deploymentFrequencyReport;
