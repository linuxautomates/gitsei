import { FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  API_BASED_FILTER,
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
  REPORT_FILTERS_CONFIG
} from "./../../../constants/applications/names";

import { LeadTimeForChangesSingleStatType } from "model/report/scm/lead-time-for-changes-single-stat/leadTimeForChangesSingleStat.constant";
import {
  REVERSE_SCM_COMMON_FILTER_KEY_MAPPING,
  SCM_COMMON_FILTER_KEY_MAPPING,
  SCM_FILTER_OPTIONS_MAPPING
} from "../constant";
import { SHOW_SINGLE_STAT_EXTRA_INFO } from "model/report/scm/baseSCMReports.constant";
import { githubPRSStatDrilldown } from "dashboard/constants/drilldown.constants";
import { supportedFilters, FAILURE_RATE_URI, FAILURE_RATE_DESCRIPTION, MESSAGE } from "../Failure-rate/constant";
import { SCMDoraFailureRateFilterConfig } from "./filter-config";
import StatsChartExtra from "shared-resources/charts/stats-chart/stats-chart-extra";
import { scmDoraFailureRateSingleStatTransformer } from "./helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

const scmDoraFailureRateSingleStat: { scm_dora_failure_rate: LeadTimeForChangesSingleStatType } = {
  scm_dora_failure_rate: {
    name: "Failure Rate",
    application: IntegrationTypes.GITHUB,
    description: FAILURE_RATE_DESCRIPTION,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: FAILURE_RATE_URI,
    method: "list",
    xaxis: false,
    chart_props: {},
    default_query: {
      metric: "resolve"
    },
    supported_filters: supportedFilters,
    transformFunction: scmDoraFailureRateSingleStatTransformer,
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [FILTER_NAME_MAPPING]: SCM_FILTER_OPTIONS_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: SCM_COMMON_FILTER_KEY_MAPPING,
    [API_BASED_FILTER]: ["creators"],
    [FIELD_KEY_FOR_FILTERS]: REVERSE_SCM_COMMON_FILTER_KEY_MAPPING,
    [REPORT_FILTERS_CONFIG]: SCMDoraFailureRateFilterConfig,
    [IS_FRONTEND_REPORT]: true,
    [SHOW_SINGLE_STAT_EXTRA_INFO]: (params: any) => {
      return StatsChartExtra({ ...params, desc: "This Month" });
    },
    show_max: false,
    filters: {},
    compareField: "count",
    drilldown: githubPRSStatDrilldown,
    [HIDE_CUSTOM_FIELDS]: true,
    [DEPRECATED_NOT_ALLOWED]: true,
    [DEPRECATED_MESSAGE]: MESSAGE
  }
};

export default scmDoraFailureRateSingleStat;
