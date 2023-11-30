import { FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  API_BASED_FILTER,
  DEFAULT_METADATA,
  FIELD_KEY_FOR_FILTERS,
  IS_FRONTEND_REPORT,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_MAPPING_KEY
} from "../../../constants/filter-key.mapping";
import {
  DEPRECATED_MESSAGE,
  DEPRECATED_NOT_ALLOWED,
  HIDE_CUSTOM_FIELDS,
  PREV_REPORT_TRANSFORMER,
  REPORT_FILTERS_CONFIG,
  SIMPLIFY_VALUE
} from "./../../../constants/applications/names";
import {
  deploymentFrequencySingleStatChartProps,
  deploymentFrequencySingleStatDefaultQuery,
  DEPLOYMENT_FREQUENCY_DESCRIPTION,
  DefaultMetadata,
  MESSAGE
} from "./constant";
import { DeploymentFrequencySingleStatFiltersConfig } from "./filter.config";
import {
  GITHUB_PR_CLOSED_AT,
  GITHUB_PR_CREATED_AT,
  GITHUB_PR_MERGE_AT,
  GITHUB_PR_UPDATED_AT,
  REVERSE_SCM_COMMON_FILTER_KEY_MAPPING,
  scmDoraSupportedFilters,
  SCM_COMMON_FILTER_KEY_MAPPING,
  SCM_FILTER_OPTIONS_MAPPING
} from "../constant";
import { SHOW_SINGLE_STAT_EXTRA_INFO } from "model/report/scm/baseSCMReports.constant";
import { githubPRSStatDrilldown } from "dashboard/constants/drilldown.constants";
import StatsChartExtra from "shared-resources/charts/stats-chart/stats-chart-extra";
import { scmDoraDeploymentFrequencySingleStatTransformer } from "./transformer";
import { DeploymentFrequencySingleStat } from "model/report/scm/deployment-frequency-single-stat/deploymentFrequencySingleStat.constant";
import { transformReportPrevQuery } from "./helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

const STAT_EXTRA_DESC = "This Month";

const deploymentFrequencySingleStat: { deployment_frequency: DeploymentFrequencySingleStat } = {
  deployment_frequency: {
    name: "Deployment Frequency",
    application: IntegrationTypes.GITHUB,
    description: DEPLOYMENT_FREQUENCY_DESCRIPTION,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "scm_dora_deployment_frequency",
    method: "list",
    xaxis: false,
    chart_props: deploymentFrequencySingleStatChartProps,
    default_query: deploymentFrequencySingleStatDefaultQuery,
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    supported_filters: scmDoraSupportedFilters,
    transformFunction: data => {
      return scmDoraDeploymentFrequencySingleStatTransformer(data, "deployment_frequency");
    },
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [FILTER_NAME_MAPPING]: SCM_FILTER_OPTIONS_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: SCM_COMMON_FILTER_KEY_MAPPING,
    [API_BASED_FILTER]: ["creators"],
    [FIELD_KEY_FOR_FILTERS]: REVERSE_SCM_COMMON_FILTER_KEY_MAPPING,
    [REPORT_FILTERS_CONFIG]: DeploymentFrequencySingleStatFiltersConfig,
    [IS_FRONTEND_REPORT]: true,
    [SHOW_SINGLE_STAT_EXTRA_INFO]: (params: any) => {
      return StatsChartExtra({ ...params, desc: `in ${params?.timeRange ?? STAT_EXTRA_DESC}` });
    },
    show_max: false,
    filters: {},
    compareField: "count",
    widgetSettingsTimeRangeFilterSchema: [
      GITHUB_PR_CREATED_AT,
      GITHUB_PR_MERGE_AT,
      GITHUB_PR_CLOSED_AT,
      GITHUB_PR_UPDATED_AT
    ],
    drilldown: githubPRSStatDrilldown,
    [HIDE_CUSTOM_FIELDS]: true,
    [SIMPLIFY_VALUE]: true,
    [DEFAULT_METADATA]: DefaultMetadata,
    [DEPRECATED_NOT_ALLOWED]: true,
    [DEPRECATED_MESSAGE]: MESSAGE,
    [PREV_REPORT_TRANSFORMER]: transformReportPrevQuery
  }
};

export default deploymentFrequencySingleStat;
