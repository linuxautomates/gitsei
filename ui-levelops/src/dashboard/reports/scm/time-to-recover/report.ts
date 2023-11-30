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
import { HIDE_CUSTOM_FIELDS, REPORT_FILTERS_CONFIG } from "./../../../constants/applications/names";

import { LeadTimeForChangesSingleStatType } from "model/report/scm/lead-time-for-changes-single-stat/leadTimeForChangesSingleStat.constant";
import {
  REVERSE_SCM_COMMON_FILTER_KEY_MAPPING,
  SCM_COMMON_FILTER_KEY_MAPPING,
  SCM_FILTER_OPTIONS_MAPPING
} from "../constant";
import { SHOW_SINGLE_STAT_EXTRA_INFO } from "model/report/scm/baseSCMReports.constant";
import { githubPRSStatDrilldown } from "dashboard/constants/drilldown.constants";

import StatsChartExtra from "shared-resources/charts/stats-chart/stats-chart-extra";
import { supportedFilters, TIME_TO_RECOVER_DESCRIPTION, TIME_TO_RECOVER_URI } from "./constant";
import { SCMDoraTimeToRecoverFilterConfig } from "./filter-config";
import { scmDoraTimeToRecoverSingleStatTransformer } from "./helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

const scmDoraTimeToRecoverSingleStat: { scm_dora_time_to_recover: LeadTimeForChangesSingleStatType } = {
  scm_dora_time_to_recover: {
    name: "Time To Restore Service",
    application: IntegrationTypes.GITHUB,
    description: TIME_TO_RECOVER_DESCRIPTION,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: TIME_TO_RECOVER_URI,
    method: "list",
    xaxis: false,
    chart_props: {},
    default_query: {
      metric: "resolve"
    },
    supported_filters: supportedFilters,
    transformFunction: data => scmDoraTimeToRecoverSingleStatTransformer(data, "recover_time"),
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [FILTER_NAME_MAPPING]: SCM_FILTER_OPTIONS_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: SCM_COMMON_FILTER_KEY_MAPPING,
    [API_BASED_FILTER]: ["creators"],
    [FIELD_KEY_FOR_FILTERS]: REVERSE_SCM_COMMON_FILTER_KEY_MAPPING,
    [REPORT_FILTERS_CONFIG]: SCMDoraTimeToRecoverFilterConfig,
    [IS_FRONTEND_REPORT]: true,
    [SHOW_SINGLE_STAT_EXTRA_INFO]: (params: any) => {
      return StatsChartExtra({ ...params, desc: "Changes This Month" });
    },
    show_max: false,
    filters: {},
    compareField: "count",
    drilldown: githubPRSStatDrilldown,
    [HIDE_CUSTOM_FIELDS]: true
  }
};

export default scmDoraTimeToRecoverSingleStat;
