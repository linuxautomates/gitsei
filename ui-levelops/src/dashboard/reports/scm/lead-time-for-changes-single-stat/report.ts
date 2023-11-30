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
import {
  leadTimeForChangesSingleStatChartProps,
  leadTimeForChangesSingleStatDefaultQuery,
  LEAD_TIME_CHANGES_DESCRIPTION
} from "./constant";
import { LeadTimeForChangesSingleStatFiltersConfig } from "./filter.config";
import { LeadTimeForChangesSingleStatType } from "model/report/scm/lead-time-for-changes-single-stat/leadTimeForChangesSingleStat.constant";
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
import { scmDoraLeadTimeForChangesSingleStatTransformer } from "./transformer";
import { IntegrationTypes } from "constants/IntegrationTypes";

const STAT_EXTRA_DESC = "Changes This Month";

const leadTimeForChangesSingleStat: { lead_time_for_change: LeadTimeForChangesSingleStatType } = {
  lead_time_for_change: {
    name: "Lead Time For Change",
    application: IntegrationTypes.GITHUB,
    description: LEAD_TIME_CHANGES_DESCRIPTION,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "scm_dora_lead_time",
    method: "list",
    xaxis: false,
    chart_props: leadTimeForChangesSingleStatChartProps,
    default_query: leadTimeForChangesSingleStatDefaultQuery,
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    supported_filters: scmDoraSupportedFilters,
    transformFunction: data => {
      return scmDoraLeadTimeForChangesSingleStatTransformer(data, "lead_time");
    },
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [FILTER_NAME_MAPPING]: SCM_FILTER_OPTIONS_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: SCM_COMMON_FILTER_KEY_MAPPING,
    [API_BASED_FILTER]: ["creators"],
    [FIELD_KEY_FOR_FILTERS]: REVERSE_SCM_COMMON_FILTER_KEY_MAPPING,
    [REPORT_FILTERS_CONFIG]: LeadTimeForChangesSingleStatFiltersConfig,
    [IS_FRONTEND_REPORT]: true,
    [SHOW_SINGLE_STAT_EXTRA_INFO]: (params: any) => {
      return StatsChartExtra({ ...params, desc: STAT_EXTRA_DESC });
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
    [HIDE_CUSTOM_FIELDS]: true
  }
};

export default leadTimeForChangesSingleStat;
