import { statReportTransformer } from "custom-hooks/helpers";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY
} from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING, WIDGET_VALIDATION_FUNCTION } from "../../../constants/filter-name.mapping";
import { FE_BASED_FILTERS, REPORT_FILTERS_CONFIG } from "../../../constants/applications/names";
import { issue_resolved_at, jiraApiBasedFilterKeyMapping } from "../commonJiraReports.constants";
import { StageBounceSingleStatType } from "../../../../model/report/jira/stage-bounce-single-stat/stageBounceSingleStat.constants";
import { stageBounceValidationFunc } from "./helper";
import {
  stageBounceChartProps,
  stageBounceDefaultQuery,
  stageBounceMetric,
  stageBounceSupportedFilters
} from "./constant";
import { JIRA_COMMON_FILTER_OPTION_MAPPING } from "../../commonReports.constants";
import { JiraStageBounceSingleStatReportFiltersConfig } from "./filter.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const stageBounceSingleStat: { stage_bounce_single_stat: StageBounceSingleStatType } = {
  stage_bounce_single_stat: {
    name: "Stage Bounce Single Stat",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "jira_stage_bounce_report",
    method: "list",
    filters: {
      across: "trend"
    },
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    xaxis: false,
    chart_props: stageBounceChartProps,
    default_query: stageBounceDefaultQuery,
    compareField: "mean",
    supported_filters: stageBounceSupportedFilters,
    drilldown: {},
    transformFunction: data => statReportTransformer(data),
    [FILTER_NAME_MAPPING]: JIRA_COMMON_FILTER_OPTION_MAPPING,
    valuesToFilters: { stage: "stages" },
    requiredFilters: ["stage"],
    [WIDGET_VALIDATION_FUNCTION]: stageBounceValidationFunc,
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [FE_BASED_FILTERS]: {
      issue_resolved_at,
      stageBounceMetric
    },
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [REPORT_FILTERS_CONFIG]: JiraStageBounceSingleStatReportFiltersConfig
  }
};

export default stageBounceSingleStat;
