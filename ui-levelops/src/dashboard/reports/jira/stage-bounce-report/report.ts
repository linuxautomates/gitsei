import { StageBounceReportType } from "../../../../model/report/jira/stage-bounce-report/stageBounceReport.constants";
import { ChartContainerType } from "../../../helpers/helper";
import { FILTER_NAME_MAPPING, WIDGET_VALIDATION_FUNCTION } from "../../../constants/filter-name.mapping";
import { jiraSupportedFilters } from "../../../constants/supported-filters.constant";
import { jiraDrilldown } from "../../../constants/drilldown.constants";
import { stageBounceDataTransformer } from "../../../../custom-hooks/helpers/stageBounce.helper";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_MAPPING_KEY,
  SHOW_AGGREGATIONS_TAB,
  SHOW_METRICS_TAB,
  STACKS_SHOW_TAB
} from "../../../constants/filter-key.mapping";
import {
  FE_BASED_FILTERS,
  INFO_MESSAGES,
  REPORT_FILTERS_CONFIG,
  STACKS_FILTER_STATUS
} from "../../../constants/applications/names";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { issue_resolved_at, jiraApiBasedFilterKeyMapping } from "../commonJiraReports.constants";
import {
  stageBounceChartProps,
  stageBounceDefaultQuery,
  stageBounceMetric,
  stageBounceSupportedFilters
} from "./constant";
import {
  getStackStatus,
  getStageBounceTotalKey,
  stageBounceChartClickPayload,
  stageBounceValidationFunc,
  stageBounceXAxisLabelTransform
} from "./helper";
import { JIRA_COMMON_FILTER_OPTION_MAPPING } from "../../commonReports.constants";
import { JiraStageBounceReportFiltersConfig } from "./filter.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const stageBounceReport: { stage_bounce_report: StageBounceReportType } = {
  stage_bounce_report: {
    name: "Stage Bounce Report",
    application: IntegrationTypes.JIRA,
    xaxis: true,
    defaultAcross: "stage",
    chart_type: ChartType?.STAGE_BOUNCE_CHART,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: stageBounceChartProps,
    default_query: stageBounceDefaultQuery,
    uri: "jira_stage_bounce_report",
    method: "list",
    filters: {},
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    supported_filters: stageBounceSupportedFilters,
    stack_filters: [...jiraSupportedFilters.values, "stage"],
    drilldown: jiraDrilldown,
    transformFunction: data => stageBounceDataTransformer(data),
    valuesToFilters: { stage: "stages" },
    tooltipMapping: {
      mean: "Mean Number of Times in stage",
      median: "Median Number of Times in stage",
      total_tickets: "Number of tickets"
    },
    requiredFilters: ["stage"],
    [SHOW_AGGREGATIONS_TAB]: true,
    [SHOW_METRICS_TAB]: true,
    [STACKS_SHOW_TAB]: WIDGET_CONFIGURATION_KEYS.AGGREGATIONS,
    [FILTER_NAME_MAPPING]: JIRA_COMMON_FILTER_OPTION_MAPPING,
    [WIDGET_VALIDATION_FUNCTION]: stageBounceValidationFunc,
    getTotalKey: getStageBounceTotalKey,
    [STACKS_FILTER_STATUS]: getStackStatus,
    [INFO_MESSAGES]: {
      stacks_disabled: "Stacks option is not applicable when x-Axis value is Stage"
    },
    [FE_BASED_FILTERS]: {
      issue_resolved_at,
      stageBounceMetric
    },
    xAxisLabelTransform: stageBounceXAxisLabelTransform,
    onChartClickPayload: stageBounceChartClickPayload,
    shouldJsonParseXAxis: () => true,
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [REPORT_FILTERS_CONFIG]: JiraStageBounceReportFiltersConfig
  }
};

export default stageBounceReport;
