import { AZURE_TIME_FILTERS_KEYS } from "constants/filters";
import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import {
  leadTimeTrendTransformer,
  leadTimeTypeTransformer,
  statReportTransformer,
  bounceReportTransformer
} from "custom-hooks/helpers";
import { azureLeadTimeStatReportTransformer } from "custom-hooks/helpers/azureleadTimeSingleStatTransformer";
import { azureBacklogTransformerWrapper, azureTicketsReportChangeTransform } from "custom-hooks/helpers/helper";
import {
  azureResolutionTimeDataTransformer,
  azureSeriesDataTransformerWrapper,
  seriesDataTransformer,
  tableTransformer,
  timeAcrossStagesDataTransformer
} from "custom-hooks/helpers/seriesData.helper";
import { sprintStatReportTransformer } from "custom-hooks/helpers/sprintStatReporthelper";
import { azureStatReportTransformerWrapper } from "custom-hooks/helpers/statReport.helper";
import { azureTrendTransformer, trendReportTransformer } from "custom-hooks/helpers/trendReport.helper";
import {
  completedDateOptions,
  effortInvestmentTrendReportSampleInterval
} from "dashboard/graph-filters/components/Constants";
import { CustomTimeBasedTypes, modificationMappedValues } from "dashboard/graph-filters/components/helper";
import { buildTimeFiltersOptions, TimeConfigTypes } from "dashboard/helpers/buildTimeFilters";
import { sprintMetricStatCsvTransformer } from "dashboard/helpers/csv-transformers/sprintMetricStatCSVTransformer";
import { azureHygieneDrilldownTranformer } from "dashboard/helpers/drilldown-transformers/jiraDrilldownTransformer";
import { get } from "lodash";
import moment from "moment";
import {
  jiraAlignmentReport,
  jiraEffortInvestmentStat,
  jiraEffortInvestmentTrendReport
} from "reduxConfigs/actions/restapi";
import { sprintMetricsChartColors, sprintMetricsPercentageColors } from "shared-resources/charts/chart-themes";
import { staticPriorties } from "shared-resources/charts/jira-prioirty-chart/helper";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  sprintImpactTransformer,
  sprintMetricsPercentReportTransformer,
  sprintMetricsTrendTransformer
} from "transformers/reports/sprintMetricsPercentReportTransformer";
import { convertEpochToDate, DEFAULT_DATE_FORMAT, isvalidTimeStamp } from "utils/dateUtils";
import {
  azureIssueResolutionTimeReportStatTransformer,
  azureIssuesSingleStatReportTransformer
} from "../../../custom-hooks/helpers/issuesSingleStat.helper";
import { leadTimePhaseTransformer } from "../../../custom-hooks/helpers/leadTimeTransformer";
import { stageBounceDataTransformer } from "../../../custom-hooks/helpers/stageBounce.helper";
import { leadTimeCsvTransformer } from "../../helpers/csv-transformers/leadTimeCsvTransformer";
import {
  ChartContainerType,
  transformAzureIssuesResolutionTimeSingleStatReportPrevQuery,
  transformAzureIssuesSingleStatReportPrevQuery,
  transformIssuesReportPrevQuery,
  transformAzureLeadTimeStageReportPrevQuery
} from "../../helpers/helper";
import {
  hygieneWeightValidationHelper,
  issuesSingleStatValidationHelper,
  jiraBaValidationHelper
} from "../../helpers/widgetValidation.helper";
import {
  EIAlignmentReportCSVColumns,
  EIAlignmentReportCSVDataTransformer,
  EIAlignmentReportCSVFiltersTransformer,
  EIDynamicURITransformer,
  EIEngineerCSVDataTransformer,
  EIEngineerReportCSVColumns,
  EITrendReportCSVColumns,
  EITrendReportCSVDataTransformer
} from "../bussiness-alignment-applications/BACSVHelperTransformer";
import {
  ACTIVE_WORK_UNIT_FILTER_KEY,
  azureEITimeRangeDefMeta,
  DefaultKeyTypes,
  DISABLE_CATEGORY_SELECTION,
  DISABLE_XAXIS,
  INTERVAL_OPTIONS,
  REPORT_CSV_DOWNLOAD_CONFIG,
  RequiredFiltersType,
  REQUIRED_FILTERS_MAPPING,
  SHOW_EFFORT_UNIT_INSIDE_TAB,
  SHOW_PROFILE_INSIDE_TAB,
  SHOW_SAMPLE_INTERVAL_INSIDE_TAB,
  STORE_ACTION,
  SUPPORT_ACTIVE_WORK_UNIT_FILTERS,
  SUPPORT_TICKET_CATEGORIZATION_FILTERS,
  SUPPORT_TICKET_CATEGORIZATION_UNIT_FILTERS,
  SUPPORT_TREND_INTERVAL,
  TICKET_CATEGORIZATION_SCHEMES_KEY,
  TICKET_CATEGORIZATION_UNIT_FILTER_KEY,
  TIME_RANGE_DISPLAY_FORMAT_CONFIG,
  SUB_COLUMNS_TITLE,
  EXCLUDE_SUB_COLUMNS_FOR
} from "../bussiness-alignment-applications/constants";
import {
  effortInvestmentTrendChartOnClicked,
  effortInvestmentSingleStatBComTransformer,
  effortInvestmentTrendBComTransformer,
  getConditionalUriForFilterPreview
} from "../bussiness-alignment-applications/helper";
import { effortInvestmentTrendChartTooltipTransformer } from "../chartTooltipTransform/effortInvestmentTrendChartTooltip.transformer";
import {
  azureBacklogDrillDown,
  azureDrilldown,
  azureResponseTimeDrilldown,
  azureEffortInvestmentTrendReportDrilldown,
  azureIssueTimeAcrossStagesDrilldown,
  azureLeadTimeDrilldown,
  azureSprintMetricSingleStatDrilldown,
  azureSprintMetricTrendReportDrilldown,
  azureStatDrilldown
} from "../drilldown.constants";
import { EffortType, EffortUnitType, IntervalType } from "../enums/jira-ba-reports.enum";
import { WidgetFilterType } from "../enums/WidgetFilterType.enum";
import {
  ALLOW_KEY_FOR_STACKS,
  BAR_CHART_REF_LINE_STROKE,
  CSV_DRILLDOWN_TRANSFORMER,
  DEFAULT_METADATA,
  FILTER_KEY_MAPPING,
  HIDE_REPORT,
  IGNORE_FILTER_KEYS_CONFIG,
  jiraSprintIgnoreConfig,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  RANGE_FILTER_CHOICE,
  SHOW_AGGREGATIONS_TAB,
  SHOW_FILTERS_TAB,
  SHOW_METRICS_TAB,
  SHOW_SETTINGS_TAB,
  SHOW_WEIGHTS_TAB,
  STACKS_SHOW_TAB,
  STAT_TIME_BASED_FILTER,
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS
} from "../filter-key.mapping";
import {
  ALLOWED_WIDGET_DATA_SORTING,
  FILTER_NAME_MAPPING,
  issueManagementCommonFilterOptionsMapping,
  VALUE_SORT_KEY,
  WIDGET_DATA_SORT_FILTER_KEY,
  WIDGET_VALIDATION_FUNCTION
} from "../filter-name.mapping";
import {
  azureExcludeStatusFilter,
  azureHideStatusFilter,
  FILTER_WITH_INFO_MAPPING,
  leadTimeExcludeStageFilter
} from "../filterWithInfo.mapping";
import { hygieneDefaultSettings, leadTimeStatDefaultQuery, statDefaultQuery, WIDGET_MIN_HEIGHT } from "../helper";
import {
  azureLeadTimeSupportedFilters,
  issueManagementEffortInvestmentSupportedFilters,
  issueManagementSupportedFilters,
  leadTimeSingleStatAzureSupportedFilters
} from "../supported-filters.constant";
import { IssueVisualizationTypes } from "../typeConstants";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "../WidgetDataSortingFilter.constant";
import { effortInvestmentXAxisTitleTransformer } from "../xAxisTitleTransformers/EffortInvestmentTrend.xAxisTransformer";
import {
  ALLOW_ZERO_LABELS,
  CHART_DATA_TRANSFORMERS,
  CHART_TOOLTIP_RENDER_TRANSFORM,
  CHART_X_AXIS_TITLE_TRANSFORMER,
  CHART_X_AXIS_TRUNCATE_TITLE,
  COMPARE_X_AXIS_TIMESTAMP,
  FE_BASED_FILTERS,
  INFO_MESSAGES,
  LABEL_TO_TIMESTAMP,
  PREV_REPORT_TRANSFORMER,
  PREVIEW_DISABLED,
  REPORT_FILTERS_CONFIG,
  STACKS_FILTER_STATUS,
  IMPLICITY_INCLUDE_DRILLDOWN_FILTER,
  INCLUDE_INTERVAL_IN_PAYLOAD,
  HIDE_CUSTOM_FIELDS,
  MULTI_SERIES_REPORT_FILTERS_CONFIG,
  GET_GRAPH_FILTERS
} from "./names";
import { basicMappingType } from "../../dashboard-types/common-types";
import { azureIssuesChartTooltipTransformer, getTotalLabel } from "dashboard/reports/azure/issues-report/helper";
import { includeSolveTimeImplicitFilter } from "./constant";
import { defaultHygineTrendsFilters } from "../hygiene.constants";
import { IssueHopsSingleStatFiltersConfig } from "dashboard/reports/azure/hops-single-stat/filter.config";
import { ResolutionTimeTrendsReportFiltersConfig } from "dashboard/reports/azure/resolution-time-trend/filter.config";
import { IssueTimeAcrossFiltersConfig } from "dashboard/reports/azure/time-across-stages/filter.config";
import { BounceSingleStatReportFiltersConfig } from "dashboard/reports/azure/bounce-single-stat/filter.config";
import { ResolutionTimeSingleStatReportFiltersConfig } from "dashboard/reports/azure/resolution-time-single-stat/filter.config";
import { ResponseTimeReportTrendsFiltersConfig } from "dashboard/reports/azure/response-time-trend/filter.config";
import { IssueResponseTimeSingleStatFiltersConfig } from "dashboard/reports/azure/response-time-single-stat/filter.config";
import { IssueBacklogTrendReportFiltersConfig } from "dashboard/reports/azure/backlog-trend-report/filter.config";
import { SprintMetricTrendReportFiltersConfig } from "dashboard/reports/azure/sprint-metrics-trend/filter.config";
import { SprintMetricPercentageTrendReportFiltersConfig } from "dashboard/reports/azure/sprint-metric-percentage-trend/filter.config";
import { ResponseTimeReportFiltersConfig } from "dashboard/reports/azure/response-time-report/filter.config";
import { IssuesReportFiltersConfig } from "dashboard/reports/azure/issues-report/filter.config";
import { IssueSingleStatFiltersConfig } from "dashboard/reports/azure/issues-single-stat/filter.config";
import { IssueReportTrendsFiltersConfig } from "dashboard/reports/azure/issues-report-trend/filter.config";
import { ResolutionTimeReportFiltersConfig } from "dashboard/reports/azure/resolution-time/filter.config";
import { BounceReportFiltersConfig } from "dashboard/reports/azure/bounce-report/filter.config";
import { IssuesBounceReportTrendsFiltersConfig } from "dashboard/reports/azure/bounce-report-trends/filter.config";
import { IssueHopsReportFiltersConfig } from "dashboard/reports/azure/hops-report/filter.config";
import { IssueHopsReportTrendsFiltersConfig } from "dashboard/reports/azure/hops-report-trends/filter.config";
import { FirstAssigneeReportFiltersConfig } from "dashboard/reports/azure/first-assignee-report/filter.config";
import { CommitEffortInvestmentSingleStatFiltersConfig } from "dashboard/reports/jira/effort-investment-single-stat/filters.config";
import {
  AzureCommitEffortInvestmentSingleStatFiltersConfig,
  AzureEffortInvestmentSingleStatFiltersConfig
} from "dashboard/reports/azure/effort-investment-single-stat/filter.config";
import {
  AzureCommitEITrendReportFiltersConfig,
  AzureEITrendReportFiltersConfig
} from "dashboard/reports/azure/effort-investment-trend-report/filters.config";
import {
  AzureCommitEIByEngineerFiltersConfig,
  AzureEIByEngineerFiltersConfig
} from "dashboard/reports/azure/effort-investment-engineer-report/filters.config";
import { AzureEffortAlignmentReportFiltersConfig } from "dashboard/reports/azure/effort-alignment-report/filters.config";
import { IssueHygieneReportFiltersConfig } from "dashboard/reports/azure/hygiene-report/filter.config";
import { IssueHygieneReportTrendsFiltersConfig } from "dashboard/reports/azure/hygiene-report-trends/filter.config";
import { SprintMetricsSingleStatReportFiltersConfig } from "dashboard/reports/azure/sprint-metrics-single-stat-report/filter.config";
import { getXAxisLabel } from "shared-resources/charts/bar-chart/bar-chart.helper";
import { LeadTimeTrendReportFiltersConfig } from "dashboard/reports/azure/lead-time-trend-report/filter.config";
import { StageBounceReportFiltersConfig } from "dashboard/reports/azure/stage-bounce-report/filter.config";
import { SprintImpactOfUnestimatedTicketReportFiltersConfig } from "dashboard/reports/azure/sprint-impact-of-unestimated-tickets-report/filter.config";
import { LeadTimeSingleStatReportFiltersConfig } from "dashboard/reports/azure/lead-time-single-stat/filter.config";
import { LeadTimeByTypeReportFiltersConfig } from "dashboard/reports/azure/lead-time-by-type-report/filter.config";
import { LeadTimeByStageReportFiltersConfig } from "dashboard/reports/azure/lead-time-by-stage-report/filter.config";
import { show_value_on_bar } from "./constant";
import { azureCommonPrevQueryTansformer } from "dashboard/helpers/previous-query-transformers/azure/azureCommonPrevQuery.transformer";
import { AzureMultiSeriesTicketsReportFiltersConfig } from "dashboard/reports/multiseries-reports/azure/tickets-report/filter.config";
import { AzureMultiSeriesResolutionTimeReportFiltersConfig } from "dashboard/reports/multiseries-reports/azure/resolution-time-report/filter.config";
import { AzureMultiSeriesBacklogTrendReportFiltersConfig } from "dashboard/reports/multiseries-reports/azure/backlog-trend-report/filter-config";
import { StageBounceSingleStatReportFiltersConfig } from "dashboard/reports/azure/stage-bounce-single-stat/filters.config";
import {
  LEAD_TIME_STAGE_REPORT_DESCRIPTION,
  LEAD_TIME_BY_STAGE_REPORT_CHART_PROPS
} from "dashboard/reports/jira/lead-time-by-stage-report/constants";
import LeadTimeByStageFooter from "shared-resources/containers/server-paginated-table/components/Table-Footer/Lead-time-stage-footer";
import { getDrilldownCheckBox } from "./helper";
import { REPORT_KEY_IS_ENABLED } from "../../reports/constants";
import { mapFiltersBeforeCallIssueSingleStat } from "dashboard/reports/jira/tickets-counts-stat/helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

const azureResolutionTimeDefaultQuery = {
  metric: ["median_resolution_time", "number_of_tickets_closed"],
  workitem_resolved_at: {
    $gt: moment.utc().subtract(3, "weeks").startOf("week").unix().toString(),
    $lt: moment.utc().unix().toString()
  }
};

const chartProps = {
  barGap: 0,
  margin: { top: 20, right: 5, left: 5, bottom: 50 }
};

const azureLeadTimeDefaultQuery = {
  limit_to_only_applicable_data: false
};

const LEAD_TIME_STAGE_DEFAULT_QUERY = {
  ...azureLeadTimeDefaultQuery,
  ratings: ["good", "slow", "needs_attention"]
};

const AZURE_ACROSS_OPTION = [
  "project",
  "status",
  "priority",
  "assignee",
  "reporter",
  "workitem_type",
  "trend",
  "workitem_created_at",
  "workitem_updated_at",
  "workitem_resolved_at"
];

const TIME_ACROSS_STAGES_REPORT_OPTIONS = [
  "none",
  "project",
  "status",
  "priority",
  "assignee",
  "reporter",
  "workitem_type"
];

const ISSUE_TICKET_ACROSS_OPTION = ["project", "status", "priority", "assignee", "reporter", "workitem_type", "trend"];
const application: string = "azure_devops";
const AZURE_APPEND_ACROSS_OPTIONS = [
  { label: "Azure Teams", value: "teams" },
  { label: "Azure Areas", value: "code_area" }
];

const workitem_created_at = {
  type: WidgetFilterType.TIME_BASED_FILTERS,
  label: "WorkItem Created In",
  BE_key: "workitem_created_at",
  slicing_value_support: true,
  configTab: WIDGET_CONFIGURATION_KEYS.FILTERS,
  options: buildTimeFiltersOptions({ [TimeConfigTypes.DAYS]: { options: [7, 30, 90, 180, 365] } })
};

const workitem_updated_at = {
  type: WidgetFilterType.TIME_BASED_FILTERS,
  label: "WorkItem Updated In",
  BE_key: "workitem_updated_at",
  slicing_value_support: true,
  configTab: WIDGET_CONFIGURATION_KEYS.FILTERS,
  options: buildTimeFiltersOptions({ [TimeConfigTypes.DAYS]: { options: [7, 30, 90, 180, 365] } })
};

const workitem_resolved_at = {
  type: WidgetFilterType.TIME_BASED_FILTERS,
  label: "WorkItem resolved In",
  BE_key: "workitem_resolved_at",
  slicing_value_support: true,
  configTab: WIDGET_CONFIGURATION_KEYS.FILTERS,
  options: buildTimeFiltersOptions({ [TimeConfigTypes.DAYS]: { options: [7, 30, 90, 180, 365] } })
};

const ba_workitem_resolved_at = {
  ...workitem_resolved_at,
  label: "WorkItem resolved At"
};

const sprint_end_at = {
  type: WidgetFilterType.TIME_BASED_FILTERS,
  label: "Sprint End Date",
  BE_key: "completed_at",
  slicing_value_support: true,
  configTab: WIDGET_CONFIGURATION_KEYS.FILTERS,
  options: buildTimeFiltersOptions({ [TimeConfigTypes.DAYS]: { options: [7, 30, 90, 180, 365] } }),
  required: true
};

const azureBacklogTrendDefaultQuery = {
  across: "trend",
  interval: "week",
  [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.TIME_BASED]
};

const drillDownValuesToFiltersKeys = {
  status: "workitem_statuses",
  project: "workitem_projects",
  priority: "workitem_priorities",
  assignee: "workitem_assignees",
  reporter: "workitem_reporters",
  workitem_type: "workitem_types",
  story_points: "workitem_story_points"
};

const sprintMetricsTrendReport = {
  completed_at: {
    $gt: moment.utc().subtract(30, "days").startOf("day").unix().toString(),
    $lt: moment.utc().unix().toString()
  },
  interval: "week",
  metric: ["creep_done_points", "commit_done_points", "commit_not_done_points", "creep_not_done_points"]
};

const sprintMetricsPercentageReport = {
  completed_at: {
    $gt: moment.utc().subtract(30, "days").startOf("day").unix().toString(),
    $lt: moment.utc().unix().toString()
  },
  interval: "week",
  metric: ["commit_done_ratio", "creep_done_to_commit_ratio"]
};

export const workItemCreatedAt = {
  key: "workitem_created_at",
  label: "WORKITEM CREATED IN",
  dataKey: "workitem_created_at",
  dataValueType: "string"
};

export const workItemUpdatedAt = {
  key: "workitem_updated_at",
  label: "WORKITEM UPDATED IN",
  dataKey: "workitem_updated_at",
  dataValueType: "string"
};

export const workItemResolveddAt = {
  key: "workitem_resolved_at",
  label: "WORKITEM RESOLVED IN",
  dataKey: "workitem_resolved_at",
  dataValueType: "string"
};

const AZURE_STACKS_FILTERS = [
  "project",
  "status",
  "priority",
  "workitem_type",
  "status_category",
  "parent_workitem_id",
  "epic",
  "assignee",
  "ticket_category",
  "version",
  "fix_version",
  "reporter",
  "label",
  "story_points",
  "teams",
  "code_area"
];

const azureAcrossStagesDefaultQuery = {
  metric: "median_time",
  // [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED],
  workitem_resolved_at: {
    $gt: moment.utc().subtract(3, "weeks").startOf("week").unix().toString(),
    $lt: moment.utc().unix().toString()
  }
};

// just to add key in the filter
const issueSingleDefualtCreatedAt = {
  $lt: moment.utc().unix().toString(),
  $gt: moment.utc().unix().toString()
};

// this filter override absolute value
const issueSingleStatDefaultMeta = {
  [RANGE_FILTER_CHOICE]: {
    workitem_created_at: {
      type: "relative",
      relative: {
        last: {
          num: 30,
          unit: "days"
        },
        next: {
          unit: "today"
        }
      }
    }
  }
};
const idFilters = ["assignee", "reporter"];

// we need to pass these two filters in workitem_atteributes parent in filters
const attributeFilters = ["code_area", "teams"];

const stageBounceMetric = {
  type: WidgetFilterType.DROPDOWN_BASED_FILTERS,
  label: "Metric",
  BE_key: "metric",
  configTab: WIDGET_CONFIGURATION_KEYS.METRICS,
  defaultValue: "mean",
  options: [
    { value: "mean", label: "Mean Number of Times in stage" },
    { value: "median", label: "Median Number of Times in stage" },
    { value: "total_tickets", label: "Number of tickets" }
  ]
};

const onChartClickPayloadForId = (params: any) => {
  const { data, across } = params;
  const { activeLabel, activePayload } = data;
  let keyValue = activeLabel;
  if (across && (idFilters.includes(across) || attributeFilters.includes(across))) {
    keyValue = {
      id: get(activePayload, [0, "payload", "key"], "_UNASSIGNED_"),
      name: get(activePayload, [0, "payload", "name"], "_UNASSIGNED_")
    };
  } else if (across && AZURE_TIME_FILTERS_KEYS.includes(across)) {
    const newData = data?.activePayload?.[0]?.payload;
    return convertEpochToDate(newData.key, DEFAULT_DATE_FORMAT, true);
  }
  return keyValue;
};

const azureApiBasedFilterKeyMapping = {
  assignees: "assignee",
  reporters: "reporter",
  workitem_assignees: "workitem_assignee",
  workitem_reporters: "workitem_reporter",
  authors: "author",
  committers: "committer"
};

const azureApiBasedFilters = [
  "workitem_assignees",
  "workitem_reporters",
  "reporters",
  "assignees",
  "authors",
  "committers"
];

export const azureDashboards = {
  // azure_pipeline_jobs_runs_report: {
  //   name: "Azure Pipeline Runs Count Report",
  //   application: IntegrationTypes.AZURE,
  //   chart_type: ChartType?.BAR,
  //   chart_container: ChartContainerType.WIDGET_API_WRAPPER,
  //   uri: "azure_pipeline_runs",
  //   method: "list",
  //   filters: {},
  //   stack_filters: ["project", "pipeline", "result"],
  //   chart_props: {
  //     unit: "Jobs per pipeline",
  //     barProps: [
  //       {
  //         name: "total",
  //         dataKey: "total"
  //       }
  //     ],
  //     chartProps: chartProps
  //   },
  //   supported_filters: azurePiplineJobSupportedFilters,
  //   xaxis: true,
  //   drilldown: azurePipelineJobCountDrilldown,
  //   across: ["project", "pipeline", "result"],
  //   defaultAcross: "project",
  //   transformFunction: (data: any) => azureTransformer(data)
  // }
  azure_lead_time_trend_report: {
    [REPORT_KEY_IS_ENABLED]: false,
    name: "Issue Lead Time Trend Report",
    application: IntegrationTypes.AZURE,
    chart_type: ChartType?.AREA,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    chart_props: {
      unit: "Days",
      stackedArea: true,
      chartProps: {
        ...chartProps,
        margin: { top: 20, right: 5, left: 5, bottom: 20 }
      }
    },
    uri: "lead_time_report",
    method: "list",
    filters: {
      across: "trend",
      calculation: "ticket_velocity"
    },
    default_query: azureLeadTimeDefaultQuery,
    convertTo: "days",
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [FILTER_WITH_INFO_MAPPING]: [leadTimeExcludeStageFilter],
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [SHOW_SETTINGS_TAB]: true,
    [SHOW_METRICS_TAB]: true,
    [SHOW_AGGREGATIONS_TAB]: false,
    supported_filters: azureLeadTimeSupportedFilters,
    drilldown: azureLeadTimeDrilldown,
    [CSV_DRILLDOWN_TRANSFORMER]: leadTimeCsvTransformer,
    transformFunction: (data: any) => leadTimeTrendTransformer(data),
    [HIDE_REPORT]: true,
    [FE_BASED_FILTERS]: {
      workitem_created_at,
      workitem_updated_at,
      workitem_resolved_at
    },
    shouldJsonParseXAxis: () => true,
    [PREV_REPORT_TRANSFORMER]: (data: any) => azureCommonPrevQueryTansformer(data),
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: LeadTimeTrendReportFiltersConfig
  },
  azure_lead_time_by_stage_report: {
    name: "Issue Lead Time by Stage Report",
    application: IntegrationTypes.AZURE,
    chart_type: ChartType?.LEAD_TIME_PHASE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    description: LEAD_TIME_STAGE_REPORT_DESCRIPTION,
    chart_props: LEAD_TIME_BY_STAGE_REPORT_CHART_PROPS,
    dataKey: "duration",
    uri: "lead_time_report",
    method: "list",
    filters: {
      calculation: "ticket_velocity"
    },
    defaultAcross: "velocity",
    default_query: LEAD_TIME_STAGE_DEFAULT_QUERY,
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    supported_filters: azureLeadTimeSupportedFilters,
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [FILTER_WITH_INFO_MAPPING]: [leadTimeExcludeStageFilter],
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [SHOW_SETTINGS_TAB]: true,
    [SHOW_METRICS_TAB]: true,
    [SHOW_AGGREGATIONS_TAB]: false,
    [PREVIEW_DISABLED]: true,
    [WIDGET_MIN_HEIGHT]: "32rem",
    drilldown: azureLeadTimeDrilldown,
    [CSV_DRILLDOWN_TRANSFORMER]: leadTimeCsvTransformer,
    transformFunction: (data: any) => leadTimePhaseTransformer(data),
    [HIDE_REPORT]: true,
    [FE_BASED_FILTERS]: {
      workitem_created_at,
      workitem_updated_at,
      workitem_resolved_at
    },
    [PREV_REPORT_TRANSFORMER]: (data: any) => azureCommonPrevQueryTansformer(data),
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: LeadTimeByStageReportFiltersConfig,
    [GET_GRAPH_FILTERS]: (props: any) => {
      const { finalFilters, contextFilter } = props;
      return { ...finalFilters, filter: { ...finalFilters.filter, ...contextFilter } };
    },
    [PREV_REPORT_TRANSFORMER]: transformAzureLeadTimeStageReportPrevQuery,
    includeContextFilter: true,
    drilldownFooter: () => LeadTimeByStageFooter,
    drilldownCheckbox: getDrilldownCheckBox,
    drilldownMissingAndOtherRatings: true,
    drilldownTotalColCaseChange: true
  },
  azure_lead_time_by_type_report: {
    name: "Issue Lead Time by Type Report",
    application: IntegrationTypes.AZURE,
    chart_type: ChartType?.LEAD_TIME_TYPE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    // xaxis: true,  TODO: Add later, Out of scope for initial release
    chart_props: {
      unit: "Days",
      chartProps: chartProps
    },
    uri: "lead_time_report",
    method: "list",
    filters: {
      calculation: "ticket_velocity",
      stacks: ["issue_type"]
    },
    defaultAcross: "velocity",
    default_query: azureLeadTimeDefaultQuery,
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    supported_filters: azureLeadTimeSupportedFilters,
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [FILTER_WITH_INFO_MAPPING]: [leadTimeExcludeStageFilter],
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [SHOW_SETTINGS_TAB]: true,
    [SHOW_METRICS_TAB]: true,
    [SHOW_AGGREGATIONS_TAB]: false,
    [PREVIEW_DISABLED]: true,
    drilldown: azureLeadTimeDrilldown,
    [CSV_DRILLDOWN_TRANSFORMER]: leadTimeCsvTransformer,
    transformFunction: (data: any) => leadTimeTypeTransformer(data),
    [HIDE_REPORT]: true,
    [FE_BASED_FILTERS]: {
      workitem_created_at,
      workitem_updated_at,
      workitem_resolved_at
    },
    [PREV_REPORT_TRANSFORMER]: (data: any) => azureCommonPrevQueryTansformer(data),
    shouldJsonParseXAxis: () => true,
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: LeadTimeByTypeReportFiltersConfig
  },
  azure_lead_time_single_stat: {
    name: "Lead Time Single Stat",
    application: IntegrationTypes.AZURE,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "lead_time_report",
    method: "list",
    filters: {
      across: "velocity"
    },
    xaxis: false,
    chart_props: {
      unit: "Days"
    },
    default_query: leadTimeStatDefaultQuery,
    compareField: "mean",
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [FILTER_WITH_INFO_MAPPING]: [leadTimeExcludeStageFilter],
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [SHOW_SETTINGS_TAB]: true,
    // [FILTER_NAME_MAPPING]: azureIssueLeadTimeFilterOptionsMapping,
    supported_filters: leadTimeSingleStatAzureSupportedFilters,
    drilldown: {},
    transformFunction: (data: any) => azureLeadTimeStatReportTransformer(data),
    chart_click_enable: false,
    [FE_BASED_FILTERS]: {
      workitem_created_at,
      workitem_updated_at,
      workitem_resolved_at
    },
    [PREV_REPORT_TRANSFORMER]: (data: any) => azureCommonPrevQueryTansformer(data),
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: LeadTimeSingleStatReportFiltersConfig
  },
  azure_tickets_report: {
    name: "Issues Report",
    application: application,
    chart_type: ChartType?.BAR,
    defaultAcross: "assignee",
    across: ISSUE_TICKET_ACROSS_OPTION,
    appendAcrossOptions: [
      ...AZURE_APPEND_ACROSS_OPTIONS,
      { label: "Azure Iteration", value: "sprint" },
      { label: "Ticket Category", value: "ticket_category" }
    ],
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    stack_filters: AZURE_STACKS_FILTERS,
    chart_props: {
      barProps: [
        {
          name: "total_tickets",
          dataKey: "total_tickets",
          unit: "Tickets"
        }
      ],
      stacked: false,
      unit: "Tickets",
      sortBy: "total_tickets",
      chartProps: chartProps
    },
    uri: "issue_management_tickets_report",
    storyPointUri: "issue_management_story_point_report",
    method: "list",
    filters: {},
    default_query: {
      metric: ["ticket"],
      visualization: IssueVisualizationTypes.BAR_CHART
    },
    [STACKS_FILTER_STATUS]: (filters: any) => {
      return filters && filters.visualization && filters.visualization === IssueVisualizationTypes.DONUT_CHART;
    },
    [INFO_MESSAGES]: {
      stacks_disabled: "Stacks option is not applicable for Donut visualization"
    },
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [ALLOW_KEY_FOR_STACKS]: true,
    supported_filters: issueManagementSupportedFilters,
    drilldown: azureDrilldown,
    [STACKS_SHOW_TAB]: WIDGET_CONFIGURATION_KEYS.AGGREGATIONS,
    [SHOW_METRICS_TAB]: true,
    [SHOW_SETTINGS_TAB]: true,
    [HIDE_REPORT]: true,
    sortApiDataHandler: (params: any) => {
      const { across, apiData = [] } = params;
      if (["workitem_created_at", "workitem_resolved_at", "workitem_updated_at", "trend"].includes(across)) {
        return apiData.sort((a: any, b: any) => b.key - a.key);
      }
      return apiData;
    },
    xAxisLabelTransform: (params: any) => getXAxisLabel(params),
    onChartClickPayload: (params: { [key: string]: any }) => {
      const { data, across, visualization } = params;
      const { activeLabel, activePayload } = data;
      let keyValue = activeLabel;
      if (visualization && visualization === ChartType.DONUT) {
        keyValue = get(data, ["tooltipPayload", 0, "name"], "_UNASSIGNED_");

        if (idFilters.includes(across)) {
          keyValue = {
            id: get(data, ["key"], "_UNASSIGNED_"),
            name: get(data, ["name"], "_UNASSIGNED_")
          };
        }

        if (["teams", "code_area"].includes(across)) {
          const _data = get(data, ["tooltipPayload", 0, "payload", "payload"]);
          keyValue = { ..._data, id: _data.key };
        }

        return keyValue;
      } else {
        if (idFilters.includes(across)) {
          keyValue = {
            id: get(activePayload, [0, "payload", "key"], "_UNASSIGNED_"),
            name: get(activePayload, [0, "payload", "name"], "_UNASSIGNED_")
          };
        }

        if (["teams", "code_area"].includes(across)) {
          const _data = data?.activePayload?.[0]?.payload || {};
          keyValue = _data;
        }
      }
      return keyValue;
    },
    shouldReverseApiData: (params: any) => {
      const { interval, across } = params;
      let should = false;
      if (["workitem_created_at", "workitem_resolved_at", "workitem_updated_at", "trend"].includes(across)) {
        should = true;
      }

      return should;
    },
    transformFunction: (data: any) => azureTicketsReportChangeTransform(data),
    weekStartsOnMonday: true,
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [FE_BASED_FILTERS]: {
      workitem_created_at,
      workitem_updated_at,
      workitem_resolved_at,
      visualization: {
        type: WidgetFilterType.DROPDOWN_BASED_FILTERS,
        label: "Visualization",
        BE_key: "visualization",
        configTab: WIDGET_CONFIGURATION_KEYS.SETTINGS,
        options: [
          { label: "Bar Chart", value: IssueVisualizationTypes.BAR_CHART },
          { label: "Donut Chart", value: IssueVisualizationTypes.DONUT_CHART },
          { label: "Percentage Stacked Bar Chart", value: IssueVisualizationTypes.PERCENTAGE_STACKED_BAR_CHART },
          { label: "Line Chart", value: IssueVisualizationTypes.LINE_CHART }
        ],
        defaultValue: IssueVisualizationTypes.BAR_CHART,
        optionsTransformFn: (data: any) => {
          const { filters } = data;
          if (!filters?.stacks?.length || filters?.stacks[0] === undefined) {
            return [
              { label: "Bar Chart", value: IssueVisualizationTypes.BAR_CHART },
              { label: "Donut Chart", value: IssueVisualizationTypes.DONUT_CHART },
              { label: "Line Chart", value: IssueVisualizationTypes.LINE_CHART }
            ];
          }
          return [
            { label: "Bar Chart", value: IssueVisualizationTypes.BAR_CHART },
            { label: "Donut Chart", value: IssueVisualizationTypes.DONUT_CHART },
            { label: "Percentage Stacked Bar Chart", value: IssueVisualizationTypes.PERCENTAGE_STACKED_BAR_CHART },
            { label: "Multi-Line Chart", value: IssueVisualizationTypes.LINE_CHART }
          ];
        }
      },
      show_value_on_bar
    },
    valuesToFilters: drillDownValuesToFiltersKeys,
    [SUPPORT_TICKET_CATEGORIZATION_FILTERS]: true,
    [SHOW_PROFILE_INSIDE_TAB]: WIDGET_CONFIGURATION_KEYS.SETTINGS,
    [FILTER_KEY_MAPPING]: {
      ticket_categories: "workitem_ticket_categories",
      ticket_categorization_scheme: "workitem_ticket_categorization_scheme"
    },
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    getTotalLabel: getTotalLabel,
    [PREV_REPORT_TRANSFORMER]: (data: any) => transformIssuesReportPrevQuery(data),
    [CHART_DATA_TRANSFORMERS]: {
      [CHART_TOOLTIP_RENDER_TRANSFORM]: azureIssuesChartTooltipTransformer
    },
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [REPORT_FILTERS_CONFIG]: IssuesReportFiltersConfig,
    [MULTI_SERIES_REPORT_FILTERS_CONFIG]: AzureMultiSeriesTicketsReportFiltersConfig
  },
  azure_tickets_counts_stat: {
    name: "Issues Single Stat",
    application: application,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "issue_management_tickets_report",
    method: "list",
    filters: {},
    xaxis: false,
    chart_props: {
      unit: "Tickets"
    },
    defaultAcross: "workitem_created_at",
    default_query: {
      workitem_created_at: issueSingleDefualtCreatedAt
    },
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    compareField: "total_tickets",
    supported_filters: issueManagementSupportedFilters,
    drilldown: azureStatDrilldown,
    transformFunction: (data: any) => {
      const { apiData, widgetFilters } = data;
      const { across } = widgetFilters;
      const newApiData = get(apiData, ["0", across, "records"], []);
      return azureIssuesSingleStatReportTransformer({ ...data, apiData: newApiData });
    },
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [HIDE_REPORT]: true,
    [SHOW_SETTINGS_TAB]: true,
    [SHOW_METRICS_TAB]: false,
    [STAT_TIME_BASED_FILTER]: {
      options: [
        { value: "workitem_created_at", label: "Workitem Created" },
        { value: "workitem_resolved_at", label: "Workitem Resolved" },
        { value: "workitem_due_at", label: "Workitem Due" },
        { value: "workitem_updated_at", label: "Workitem Updated" }
      ],
      getFilterLabel: (data: any) => {
        const { filters } = data;
        return filters.across ? `${filters.across.replace("_at", "").replace("_", " ")} in` : "";
      },
      getFilterKey: (data: any) => {
        const { filters } = data;
        return filters.across || "";
      },
      defaultValue: "workitem_created_at"
    },
    [WIDGET_VALIDATION_FUNCTION]: issuesSingleStatValidationHelper,
    [DEFAULT_METADATA]: issueSingleStatDefaultMeta,
    [PREV_REPORT_TRANSFORMER]: (data: any) => transformAzureIssuesSingleStatReportPrevQuery(data),
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: IssueSingleStatFiltersConfig,
    mapFiltersForWidgetApi: mapFiltersBeforeCallIssueSingleStat
  },
  azure_tickets_report_trends: {
    name: "Issues Trend Report",
    application: application,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    composite: true,
    composite_transform: {
      total_tickets: "total_jira_tickets"
    },
    chart_props: {
      unit: "Tickets",
      chartProps: chartProps
    },
    uri: "issue_management_tickets_report",
    method: "list",
    filters: {
      across: "trend"
    },
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: issueManagementSupportedFilters,
    drilldown: azureDrilldown,
    transformFunction: (data: any) => azureTrendTransformer(data),
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [HIDE_REPORT]: true,
    [SHOW_SETTINGS_TAB]: true,
    [SHOW_METRICS_TAB]: false,
    [FE_BASED_FILTERS]: {
      workitem_created_at,
      workitem_updated_at,
      workitem_resolved_at
    },
    valuesToFilters: drillDownValuesToFiltersKeys,
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [PREV_REPORT_TRANSFORMER]: (data: any) => azureCommonPrevQueryTansformer(data),
    [REPORT_FILTERS_CONFIG]: IssueReportTrendsFiltersConfig
  },
  azure_time_across_stages: {
    name: "Issue Time Across Stages",
    application: application,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    defaultAcross: "none",
    across: TIME_ACROSS_STAGES_REPORT_OPTIONS,
    chart_props: {
      barProps: [
        {
          name: "Median Time In Status",
          dataKey: "median_time"
        },

        {
          name: "Average Time In Status",
          dataKey: "average_time"
        }
      ],
      stacked: false,
      unit: "Days",
      sortBy: "median_time",
      chartProps: chartProps
    },
    xAxisLabelTransform: (params: any) => {
      const { across, item = {} } = params;
      const { key, additional_key } = item;
      let newLabel = key || additional_key;
      if (idFilters.includes(across)) {
        newLabel = additional_key;
        return newLabel;
      }
      if (["priority"].includes(across)) {
        newLabel = get(staticPriorties, [key], key);
      }
      if (across === "code_area") {
        newLabel = key?.split("\\").pop() || key;
      }

      return newLabel;
    },
    uri: "issue_management_stage_time_report",
    method: "list",
    filters: {},
    dataKey: "median_time",
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    default_query: azureAcrossStagesDefaultQuery,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    appendAcrossOptions: [...AZURE_APPEND_ACROSS_OPTIONS, { label: "Azure Iteration", value: "sprint" }],
    supported_filters: issueManagementSupportedFilters,
    drilldown: azureIssueTimeAcrossStagesDrilldown,
    transformFunction: (data: any) => timeAcrossStagesDataTransformer(data),
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [FILTER_WITH_INFO_MAPPING]: [azureHideStatusFilter],
    [SHOW_SETTINGS_TAB]: true,
    [HIDE_REPORT]: true,
    [FE_BASED_FILTERS]: {
      workitem_created_at,
      workitem_updated_at,
      workitem_resolved_at,
      show_value_on_bar
    },
    valuesToFilters: drillDownValuesToFiltersKeys,
    onChartClickPayload: onChartClickPayloadForId,
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    [IMPLICITY_INCLUDE_DRILLDOWN_FILTER]: includeSolveTimeImplicitFilter,
    [REPORT_FILTERS_CONFIG]: IssueTimeAcrossFiltersConfig,
    [PREV_REPORT_TRANSFORMER]: (data: any) => azureCommonPrevQueryTansformer(data)
  },
  azure_hygiene_report: {
    name: "Issue Hygiene Report",
    application: application,
    chart_type: ChartType?.SCORE,
    chart_container: ChartContainerType.HYGIENE_API_WRAPPER,
    xaxis: true,
    chart_props: {
      unit: "tickets",
      barProps: [
        {
          name: "total_tickets",
          dataKey: "total_tickets"
        }
      ],
      stacked: false,
      sortBy: "total_tickets",
      chartProps: chartProps
    },
    uri: "issue_management_tickets_report",
    method: "list",
    filters: {},
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    defaultAcross: "project",
    hygiene_uri: "issue_management_tickets_report",
    drilldown: {
      ...azureDrilldown,
      drilldownTransformFunction: (data: any) => azureHygieneDrilldownTranformer(data)
    },
    default_query: hygieneDefaultSettings,
    supported_filters: issueManagementSupportedFilters,
    across: issueManagementSupportedFilters.values,
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [SHOW_METRICS_TAB]: false,
    [SHOW_AGGREGATIONS_TAB]: false,
    [SHOW_SETTINGS_TAB]: true,
    [HIDE_REPORT]: true,
    [FE_BASED_FILTERS]: {
      workitem_created_at,
      workitem_resolved_at
    },
    [WIDGET_VALIDATION_FUNCTION]: hygieneWeightValidationHelper,
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    [PREV_REPORT_TRANSFORMER]: (data: any) => azureCommonPrevQueryTansformer(data),
    [REPORT_FILTERS_CONFIG]: IssueHygieneReportFiltersConfig
  },
  azure_hygiene_report_trends: {
    name: "Issue Hygiene Trend Report",
    application: application,
    chart_type: ChartType?.HYGIENE_AREA_CHART,
    chart_container: ChartContainerType.HYGIENE_API_WRAPPER,
    xaxis: true,
    chart_props: {
      unit: "Score",
      chartProps: chartProps,
      areaProps: [],
      stackedArea: true
    },
    uri: "issue_management_list",
    method: "list",
    filters: defaultHygineTrendsFilters,
    default_query: {
      interval: "month"
    },
    hygiene_trend_uri: "issue_management_tickets_report",
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    drilldown: azureDrilldown,
    supported_filters: issueManagementSupportedFilters,
    across: issueManagementSupportedFilters.values,
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [HIDE_REPORT]: true,
    [SHOW_SETTINGS_TAB]: true,
    [SHOW_METRICS_TAB]: false,
    [SHOW_AGGREGATIONS_TAB]: false,
    [FE_BASED_FILTERS]: {
      workitem_created_at,
      workitem_resolved_at
    },
    [WIDGET_VALIDATION_FUNCTION]: hygieneWeightValidationHelper,
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    onChartClickPayload: (params: { [key: string]: any }) => {
      if (params?.visualization === ChartType?.HYGIENE_BAR_CHART) {
        return {
          id: get(params, ["data", "key"]),
          name: get(params, ["data", "name"]),
          hygiene: params?.hygiene
        };
      } else {
        return {
          id: get(params, ["data", "activePayload", 0, "payload", "key"]),
          name: get(params, ["data", "activePayload", 0, "payload", "name"]),
          hygiene: params?.hygiene
        };
      }
    },
    [LABEL_TO_TIMESTAMP]: false,
    [COMPARE_X_AXIS_TIMESTAMP]: true,
    [INCLUDE_INTERVAL_IN_PAYLOAD]: true,
    [REPORT_FILTERS_CONFIG]: IssueHygieneReportTrendsFiltersConfig,
    [PREV_REPORT_TRANSFORMER]: (data: any) => azureCommonPrevQueryTansformer(data)
  },
  azure_backlog_trend_report: {
    name: "Issue Backlog Trend Report",
    application: application,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    chart_props: {
      barProps: [
        {
          name: "median",
          dataKey: "median"
        }
      ],
      stacked: false,
      unit: "Days",
      chartProps: chartProps
    },
    uri: "issue_management_age_report",
    method: "list",
    filters: {
      across: "trend"
    },
    stack_filters: AZURE_STACKS_FILTERS,
    default_query: azureBacklogTrendDefaultQuery,
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    xAxisLabelTransform: (params: any) => getXAxisLabel(params),
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    supported_filters: issueManagementSupportedFilters,
    drilldown: azureBacklogDrillDown,
    shouldReverseApiData: () => false,
    transformFunction: (data: any) => azureBacklogTransformerWrapper(data),
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [STACKS_SHOW_TAB]: WIDGET_CONFIGURATION_KEYS.AGGREGATIONS,
    [SHOW_SETTINGS_TAB]: true,
    [HIDE_REPORT]: true,
    [FE_BASED_FILTERS]: {
      workitem_created_at,
      workitem_updated_at,
      workitem_resolved_at
    },
    valuesToFilters: drillDownValuesToFiltersKeys,
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    onChartClickPayload: (param: any) => {
      const timeStamp = get(param, ["data", "activePayload", 0, "payload", "key"], undefined);
      const label = get(param, ["data", "activeLabel"], undefined);
      return { id: timeStamp, name: label };
    },
    [REPORT_FILTERS_CONFIG]: IssueBacklogTrendReportFiltersConfig,
    [PREV_REPORT_TRANSFORMER]: (data: any) => azureCommonPrevQueryTansformer(data),
    [MULTI_SERIES_REPORT_FILTERS_CONFIG]: AzureMultiSeriesBacklogTrendReportFiltersConfig
  },
  azure_effort_investment_single_stat: {
    name: "Effort Investment Single Stat",
    application: application,
    chart_type: ChartType?.EFFORT_INVESTMENT_STAT,
    chart_container: ChartContainerType.BA_WIDGET_API_WRAPPER,
    uri: "azure_effort_investment_tickets",
    method: "list",
    filters: {},
    default_query: {
      workitem_resolved_at: {
        // required filters and default is last month
        $gt: moment.utc().subtract(4, "months").startOf("day").unix().toString(),
        $lt: moment.utc().unix().toString()
      }
    },
    xaxis: false,
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    supported_filters: issueManagementEffortInvestmentSupportedFilters,
    drilldown: azureStatDrilldown,
    [DEFAULT_METADATA]: azureEITimeRangeDefMeta,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [STORE_ACTION]: jiraEffortInvestmentStat,
    [SUPPORT_TICKET_CATEGORIZATION_UNIT_FILTERS]: true,
    [SUPPORT_TICKET_CATEGORIZATION_FILTERS]: true,
    [SHOW_EFFORT_UNIT_INSIDE_TAB]: WIDGET_CONFIGURATION_KEYS.SETTINGS,
    [SHOW_PROFILE_INSIDE_TAB]: WIDGET_CONFIGURATION_KEYS.SETTINGS,
    [DISABLE_CATEGORY_SELECTION]: true,
    [REQUIRED_FILTERS_MAPPING]: {
      [RequiredFiltersType.SCHEME_SELECTION]: true
    },
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    supported_widget_types: ["stats"],
    [HIDE_REPORT]: true,
    [SHOW_SETTINGS_TAB]: true,
    [DefaultKeyTypes.DEFAULT_DISPLAY_FORMAT_KEY]: "percentage",
    [DefaultKeyTypes.DEFAULT_EFFORT_UNIT]: "azure_effort_investment_tickets",
    [DefaultKeyTypes.DEFAULT_SCHEME_KEY]: true, // if true then default scheme is selected by default
    [FE_BASED_FILTERS]: {
      workitem_created_at,
      workitem_updated_at,
      ba_workitem_resolved_at
    },
    [PREV_REPORT_TRANSFORMER]: effortInvestmentSingleStatBComTransformer,
    transformFunction: (data: any) => tableTransformer(data),
    [WIDGET_VALIDATION_FUNCTION]: jiraBaValidationHelper,
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: (args: any) => {
      const { filters } = args;
      const uriUnit = get(filters, [TICKET_CATEGORIZATION_UNIT_FILTER_KEY], undefined);
      if ([EffortUnitType.AZURE_COMMIT_COUNT, EffortUnitType.COMMIT_COUNT].includes(uriUnit)) {
        return AzureCommitEffortInvestmentSingleStatFiltersConfig;
      }
      return AzureEffortInvestmentSingleStatFiltersConfig;
    },
    [HIDE_CUSTOM_FIELDS]: (args: any) => {
      const { filters } = args;
      const uriUnit = get(filters, [TICKET_CATEGORIZATION_UNIT_FILTER_KEY], undefined);
      if ([EffortUnitType.AZURE_COMMIT_COUNT, EffortUnitType.COMMIT_COUNT].includes(uriUnit)) {
        return true;
      }
      return false;
    }
  },
  azure_effort_investment_trend_report: {
    name: "Effort Investment Trend Report",
    application: application,
    chart_type: ChartType?.JIRA_EFFORT_ALLOCATION_CHART,
    chart_container: ChartContainerType.BA_WIDGET_API_WRAPPER,
    xaxis: true,
    uri: "azure_effort_investment_tickets",
    method: "list",
    filters: {
      across: "workitem_resolved_at"
    },
    chart_props: {
      chartProps: {
        barGap: 0,
        margin: { top: 20, right: 5, left: 5, bottom: 50 },
        className: "ba-bar-chart"
      },
      pieProps: {
        cx: "50%",
        innerRadius: 70
      },
      stacked: true,
      transformFn: (data: any) => {
        return (data as number).toFixed(1) + "%";
      },
      totalCountTransformFn: (data: any) => data + "%"
    },
    default_query: {
      workitem_resolved_at: {
        // required filters and default is last month
        $gt: moment.utc().subtract(4, "months").startOf("day").unix().toString(),
        $lt: moment.utc().unix().toString()
      },
      interval: IntervalType.BI_WEEK,
      [ACTIVE_WORK_UNIT_FILTER_KEY]: "active_azure_ei_ticket_count"
    },
    [TIME_RANGE_DISPLAY_FORMAT_CONFIG]: {
      week: "DD MMM YYYY",
      biweekly: "DD MMM YYYY",
      month: "MMM YYYY",
      [IntervalType.QUARTER]: "DD MMM YYYY"
    },
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    supported_filters: issueManagementEffortInvestmentSupportedFilters,
    drilldown: azureEffortInvestmentTrendReportDrilldown,
    shouldJsonParseXAxis: () => true,
    show_max: true,
    onChartClickPayload: effortInvestmentTrendChartOnClicked,
    [SUPPORT_ACTIVE_WORK_UNIT_FILTERS]: false,
    [DEFAULT_METADATA]: azureEITimeRangeDefMeta,
    [SHOW_EFFORT_UNIT_INSIDE_TAB]: WIDGET_CONFIGURATION_KEYS.SETTINGS,
    [SHOW_SAMPLE_INTERVAL_INSIDE_TAB]: WIDGET_CONFIGURATION_KEYS.SETTINGS,
    [SHOW_PROFILE_INSIDE_TAB]: WIDGET_CONFIGURATION_KEYS.SETTINGS,
    [SUPPORT_TREND_INTERVAL]: true,
    [INTERVAL_OPTIONS]: effortInvestmentTrendReportSampleInterval,
    [SHOW_SETTINGS_TAB]: true,
    [SHOW_METRICS_TAB]: false,
    [SHOW_AGGREGATIONS_TAB]: false,
    [STORE_ACTION]: jiraEffortInvestmentTrendReport,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PREVIEW_DISABLED]: true,
    [SUPPORT_TICKET_CATEGORIZATION_UNIT_FILTERS]: true,
    [SUPPORT_TICKET_CATEGORIZATION_FILTERS]: true,
    [REQUIRED_FILTERS_MAPPING]: {
      [RequiredFiltersType.SCHEME_SELECTION]: true
    },
    requiredFilters: [TICKET_CATEGORIZATION_SCHEMES_KEY, "interval"],
    [DISABLE_CATEGORY_SELECTION]: true,
    [DISABLE_XAXIS]: true,
    [DefaultKeyTypes.DEFAULT_SCHEME_KEY]: true,
    [DefaultKeyTypes.DEFAULT_EFFORT_UNIT]: "azure_effort_investment_tickets",
    [HIDE_REPORT]: true,
    transformFunction: (data: any) => tableTransformer(data),
    // * REFER To WidgetChartDataTransformerType for typings and more info
    [CHART_DATA_TRANSFORMERS]: {
      [CHART_X_AXIS_TITLE_TRANSFORMER]: effortInvestmentXAxisTitleTransformer,
      [CHART_X_AXIS_TRUNCATE_TITLE]: false,
      [CHART_TOOLTIP_RENDER_TRANSFORM]: effortInvestmentTrendChartTooltipTransformer,
      [ALLOW_ZERO_LABELS]: false
    },
    [FE_BASED_FILTERS]: {
      workitem_created_at,
      workitem_updated_at,
      ba_workitem_resolved_at
    },
    [REPORT_CSV_DOWNLOAD_CONFIG]: {
      widgetFiltersTransformer: EIAlignmentReportCSVFiltersTransformer,
      widgetDynamicURIGetFunc: EIDynamicURITransformer,
      widgetCSVColumnsGetFunc: EITrendReportCSVColumns,
      widgetCSVDataTransform: EITrendReportCSVDataTransformer
    },
    [PREV_REPORT_TRANSFORMER]: effortInvestmentTrendBComTransformer,
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [WIDGET_VALIDATION_FUNCTION]: jiraBaValidationHelper,
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: (args: any) => {
      const { filters } = args;
      const uriUnit = get(filters, [TICKET_CATEGORIZATION_UNIT_FILTER_KEY], undefined);
      if ([EffortUnitType.AZURE_COMMIT_COUNT, EffortUnitType.COMMIT_COUNT].includes(uriUnit)) {
        return AzureCommitEITrendReportFiltersConfig;
      }
      return AzureEITrendReportFiltersConfig;
    },
    [HIDE_CUSTOM_FIELDS]: (args: any) => {
      const { filters } = args;
      const uriUnit = get(filters, [TICKET_CATEGORIZATION_UNIT_FILTER_KEY], undefined);
      if ([EffortUnitType.AZURE_COMMIT_COUNT, EffortUnitType.COMMIT_COUNT].includes(uriUnit)) {
        return true;
      }
      return false;
    }
  },
  azure_sprint_metrics_single_stat: {
    name: "Sprint Metrics Single Stat",
    application: application,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.SPRINT_API_WRAPPER,
    uri: "issue_management_sprint_report",
    method: "list",
    filters: {
      include_workitem_ids: true
    },
    default_query: {
      agg_type: "average",
      completed_at: modificationMappedValues("last_month", completedDateOptions),
      metric: "avg_commit_to_done"
    },
    xaxis: false,
    chart_props: {
      unit: "%"
    },
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    columnWithInformation: true,
    columnsWithInfo: {
      additional_key: "Sprint name",
      key: "Sprint completion date",
      creep_story_points:
        "Number of story points added to the sprint after start. Doesn’t include points removed from the sprint.",
      delivered_creep_story_points: "Number of Creep points completed in the sprint. ",
      creep_completion: "Creep Points / Committed Points",
      committed_story_points: "Number of points committed at the beginning of the sprint.",
      sprint_creep: "Creep Points / Committed Points",
      delivered_story_points:
        "Number of points delivered at the end of a sprint (or on sprint completion date). Excludes points completed outside the sprint.",
      done_to_commit: "Done Points / Committed Points"
    },
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: issueManagementSupportedFilters,
    drilldown: {
      allowDrilldown: true,
      ...azureSprintMetricSingleStatDrilldown
    },
    [CSV_DRILLDOWN_TRANSFORMER]: sprintMetricStatCsvTransformer,
    [IGNORE_FILTER_KEYS_CONFIG]: jiraSprintIgnoreConfig,
    transformFunction: (data: any) => sprintStatReportTransformer(data),
    supported_widget_types: ["stats"],
    [SHOW_SETTINGS_TAB]: true,
    [HIDE_REPORT]: true,
    [FE_BASED_FILTERS]: {
      workitem_created_at,
      workitem_updated_at,
      sprint_end_at
    },
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: SprintMetricsSingleStatReportFiltersConfig,
    [PREV_REPORT_TRANSFORMER]: (data: any) => azureCommonPrevQueryTansformer(data)
  },
  azure_sprint_metrics_percentage_trend: {
    name: "Sprint Metrics Percentage Trend Report",
    application: application,
    chart_type: ChartType?.AREA,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "issue_management_sprint_report",
    method: "list",
    filters: {},
    xaxis: true,
    defaultAcross: "bi_week",
    show_max: false,
    [IGNORE_FILTER_KEYS_CONFIG]: jiraSprintIgnoreConfig,
    columnWithInformation: true,
    columnsWithInfo: {
      additional_key: "Sprint name",
      status: "Issue status at sprint close.",
      story_points: "Story points at sprint start and close."
    },
    chart_props: {
      customColors: sprintMetricsPercentageColors,
      unit: "Percentage",
      chartProps: chartProps,
      areaProps: [
        {
          name: "Creep to Commit",
          dataKey: "creep_to_commit_ratio",
          unit: "%",
          transformer: (data: any) => data + " %"
        },
        {
          name: "Delivered to Commit",
          dataKey: "done_to_commit_ratio",
          unit: "%",
          transformer: (data: any) => data + " %"
        },
        {
          name: "Creep Done to Commit",
          dataKey: "creep_done_to_commit_ratio",
          unit: "%",
          transformer: (data: any) => data + " %"
        },
        {
          name: "Creep Done",
          dataKey: "creep_done_ratio",
          unit: "%",
          transformer: (data: any) => data + " %"
        },
        {
          name: "Creep Missed",
          dataKey: "creep_missed_ratio",
          unit: "%",
          transformer: (data: any) => data + " %"
        },
        {
          name: "Commit Missed",
          dataKey: "commit_missed_ratio",
          unit: "%",
          transformer: (data: any) => data + " %"
        },
        {
          name: "Commit Done",
          dataKey: "commit_done_ratio",
          unit: "%",
          transformer: (data: any) => data + " %"
        }
      ],
      stackedArea: false,
      showGrid: true,
      showDots: true,
      fillOpacity: 0.2,
      legendType: "circle",
      areaType: "linear",
      showTotalOnTooltip: false
    },
    default_query: sprintMetricsPercentageReport,
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: issueManagementSupportedFilters,
    drilldown: azureSprintMetricTrendReportDrilldown,
    transformFunction: (data: any) => sprintMetricsPercentReportTransformer(data),
    [SHOW_SETTINGS_TAB]: true,
    [SHOW_FILTERS_TAB]: true,
    [SHOW_METRICS_TAB]: true,
    [SHOW_WEIGHTS_TAB]: false,
    [HIDE_REPORT]: true,
    [SHOW_AGGREGATIONS_TAB]: true,
    [FE_BASED_FILTERS]: {
      workitem_created_at,
      workitem_updated_at,
      sprint_end_at
    },
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: SprintMetricPercentageTrendReportFiltersConfig,
    [PREV_REPORT_TRANSFORMER]: (data: any) => azureCommonPrevQueryTansformer(data)
  },
  azure_sprint_metrics_trend: {
    name: "Sprint Metrics Trend Report",
    application: application,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "issue_management_sprint_report",
    method: "list",
    filters: {},
    xaxis: true,
    defaultAcross: "sprint",
    show_max: false,
    [IGNORE_FILTER_KEYS_CONFIG]: jiraSprintIgnoreConfig,
    columnWithInformation: true,
    columnsWithInfo: {
      additional_key: "Sprint name",
      status: "Issue status at sprint close.",
      story_points: "Story points at sprint start and close."
    },
    chart_props: {
      customColors: sprintMetricsChartColors,
      showNegativeValuesAsPositive: true,
      hideTotalInTooltip: true,
      barProps: [
        {
          name: "Commit done",
          dataKey: "commit_done_points",
          unit: "Points"
        },
        {
          name: "Commit missed",
          dataKey: "commit_not_done_points",
          unit: "Points"
        },
        {
          name: "Commit over done",
          dataKey: "commit_over_done_points",
          unit: "Points"
        },
        {
          name: "Creep done",
          dataKey: "creep_done_points",
          unit: "Points"
        },
        {
          name: "Creep missed",
          dataKey: "creep_not_done_points",
          unit: "Points"
        },
        {
          name: "Creep over done",
          dataKey: "creep_over_done_points",
          unit: "Points"
        }
      ],
      stacked: true,
      chartProps: {
        ...chartProps,
        stackOffset: "sign"
      },
      unit: "Points",
      config: {
        showXAxisTooltip: true
      }
    },
    default_query: sprintMetricsTrendReport,
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    compareField: "delivered_story_points",
    supported_filters: issueManagementSupportedFilters,
    drilldown: azureSprintMetricTrendReportDrilldown,
    transformFunction: (data: any) => sprintMetricsTrendTransformer(data),
    [SHOW_SETTINGS_TAB]: true,
    [SHOW_FILTERS_TAB]: true,
    [SHOW_METRICS_TAB]: true,
    [SHOW_WEIGHTS_TAB]: false,
    [SHOW_AGGREGATIONS_TAB]: true,
    [HIDE_REPORT]: true,
    [BAR_CHART_REF_LINE_STROKE]: "#4f4f4f",
    [FE_BASED_FILTERS]: {
      workitem_created_at,
      workitem_updated_at,
      sprint_end_at
    },
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: SprintMetricTrendReportFiltersConfig,
    [PREV_REPORT_TRANSFORMER]: (data: any) => azureCommonPrevQueryTansformer(data)
  },
  azure_sprint_impact_estimated_ticket_report: {
    name: "Sprint Impact of Unestimated Tickets Report",
    application: application,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "issue_management_sprint_report",
    method: "list",
    defaultAcross: "week",
    filters: {
      include_workitem_ids: true
    },
    default_query: {
      completed_at: modificationMappedValues("last_month", completedDateOptions)
    },
    xaxis: false,
    chart_props: {
      customColors: sprintMetricsChartColors,
      showNegativeValuesAsPositive: true,
      hideTotalInTooltip: true,
      stacked: true,
      hideGrid: true,
      chartProps: {
        ...chartProps,
        stackOffset: "sign",
        barProps: [
          {
            name: "Missed Points",
            dataKey: "missed_points",
            unit: "Points"
          }
        ],
        legendProps: {
          align: "left",
          iconType: "circle",
          verticalAlign: "bottom"
        },
        className: "sprint_impact_estimated_bar_chart"
      },
      unit: "Points"
    },
    // shouldJsonParseXAxis: () => true,
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: issueManagementSupportedFilters,
    drilldown: {},
    transformFunction: (data: any) => sprintImpactTransformer(data),
    [HIDE_REPORT]: true,
    [SHOW_SETTINGS_TAB]: true,
    [SHOW_METRICS_TAB]: false,
    [SHOW_AGGREGATIONS_TAB]: false,
    [FE_BASED_FILTERS]: {
      workitem_created_at,
      workitem_updated_at,
      sprint_end_at
    },
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: SprintImpactOfUnestimatedTicketReportFiltersConfig,
    [PREV_REPORT_TRANSFORMER]: (data: any) => azureCommonPrevQueryTansformer(data)
  },
  azure_response_time_report: {
    name: "Issue Response Time Report",
    application: application,
    defaultAcross: "assignee",
    across: AZURE_ACROSS_OPTION,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultFilterKey: "median",
    showExtraInfoOnToolTip: ["total_tickets"],
    xaxis: true,
    // default_query: {
    //   [WIDGET_DATA_SORT_FILTER_KEY]:
    //     widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED]
    // },
    appendAcrossOptions: [...AZURE_APPEND_ACROSS_OPTIONS, { label: "Azure Iteration", value: "sprint" }],
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [VALUE_SORT_KEY]: "response_time",
    chart_props: {
      barProps: [
        {
          name: "min",
          dataKey: "min"
        },
        {
          name: "median",
          dataKey: "median"
        },
        {
          name: "max",
          dataKey: "max"
        }
      ],
      stacked: false,
      unit: "Days",
      chartProps: chartProps,
      xAxisIgnoreSortKeys: ["priority"]
    },
    uri: "issue_management_response_time_report",
    method: "list",
    filters: {},
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: issueManagementSupportedFilters,
    convertTo: "days",
    drilldown: azureResponseTimeDrilldown,
    xAxisLabelTransform: (params: any) => {
      const { interval, across, item = {} } = params;
      const { key, additional_key } = item;
      let newLabel = key;
      if (idFilters.includes(across)) {
        newLabel = additional_key;
        return newLabel;
      }
      if (["priority"].includes(across)) {
        newLabel = get(staticPriorties, [key], key);
      }

      if (across === "code_area") {
        newLabel = key.split("\\").pop() || key;
      }

      return newLabel;
    },
    onChartClickPayload: (args: { data: basicMappingType<any>; across?: string }) => {
      const { data, across } = args;
      const { activeLabel, activePayload } = data;
      let keyValue = activeLabel;
      if (across && (idFilters.includes(across) || attributeFilters.includes(across))) {
        keyValue = {
          id: get(activePayload, [0, "payload", "key"], "_UNASSIGNED_"),
          name: get(activePayload, [0, "payload", "name"], "_UNASSIGNED_")
        };
      }
      return keyValue;
    },
    transformFunction: (data: any) => azureSeriesDataTransformerWrapper(data),
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [HIDE_REPORT]: true,
    [ALLOW_KEY_FOR_STACKS]: true,
    [SHOW_METRICS_TAB]: false,
    [SHOW_SETTINGS_TAB]: true,
    [FE_BASED_FILTERS]: {
      workitem_created_at,
      workitem_updated_at,
      workitem_resolved_at,
      show_value_on_bar
    },
    valuesToFilters: drillDownValuesToFiltersKeys,
    [REPORT_FILTERS_CONFIG]: ResponseTimeReportFiltersConfig,
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    [PREV_REPORT_TRANSFORMER]: (data: any) => azureCommonPrevQueryTansformer(data)
  },
  azure_response_time_counts_stat: {
    name: "Issue Response Time Single Stat",
    application: application,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "issue_management_response_time_report",
    method: "list",
    filters: {
      across: "trend"
    },
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    default_query: statDefaultQuery,
    xaxis: false,
    chart_props: {
      unit: "Days"
    },
    compareField: "median",
    supported_filters: issueManagementSupportedFilters,
    transformFunction: (data: any) => azureStatReportTransformerWrapper(data),
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [HIDE_REPORT]: true,
    [SHOW_METRICS_TAB]: false,
    [SHOW_SETTINGS_TAB]: true,
    [FE_BASED_FILTERS]: {
      workitem_created_at,
      workitem_updated_at,
      workitem_resolved_at
    },
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: IssueResponseTimeSingleStatFiltersConfig,
    [PREV_REPORT_TRANSFORMER]: (data: any) => azureCommonPrevQueryTansformer(data)
  },
  azure_response_time_report_trends: {
    name: "Issue Response Time Report Trends",
    application: application,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    composite: true,
    composite_transform: {
      min: "response_time_min",
      median: "response_time_median",
      max: "response_time_max"
    },
    chart_props: {
      unit: "Days",
      chartProps: chartProps
    },
    uri: "issue_management_response_time_report",
    method: "list",
    filters: {
      across: "trend"
    },
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [SHOW_SETTINGS_TAB]: true,
    default_query: {
      [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.TIME_BASED]
    },
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: issueManagementSupportedFilters,
    convertTo: "days",
    drilldown: azureResponseTimeDrilldown,
    transformFunction: (data: any) => azureTrendTransformer(data),
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [HIDE_REPORT]: true,
    [SHOW_METRICS_TAB]: false,
    [SHOW_AGGREGATIONS_TAB]: false,
    [FE_BASED_FILTERS]: {
      workitem_created_at,
      workitem_updated_at,
      workitem_resolved_at
    },
    valuesToFilters: drillDownValuesToFiltersKeys,
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: ResponseTimeReportTrendsFiltersConfig,
    [PREV_REPORT_TRANSFORMER]: (data: any) => azureCommonPrevQueryTansformer(data)
  },
  azure_resolution_time_report: {
    name: "Issue Resolution Time Report",
    application: application,
    chart_type: ChartType?.AZURE_RESOLUTION_TIME,
    across: ISSUE_TICKET_ACROSS_OPTION,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultAcross: "assignee",
    default_query: azureResolutionTimeDefaultQuery,
    appendAcrossOptions: [
      ...AZURE_APPEND_ACROSS_OPTIONS,
      { label: "Azure Iteration", value: "sprint" },
      { label: "Ticket Category", value: "ticket_category" },
      { label: "Azure Story Points", value: "story_points" }
    ],
    xaxis: true,
    chart_props: {
      barProps: [
        {
          name: "Median Resolution Time",
          dataKey: "median_resolution_time"
        },
        {
          name: "Number of Tickets",
          dataKey: "number_of_tickets_closed"
        }
      ],
      stacked: false,
      unit: "Days",
      chartProps: chartProps,
      xAxisIgnoreSortKeys: ["priority"]
    },
    tooltipMapping: { number_of_tickets_closed: "Number of Tickets" },
    uri: "issue_management_resolution_time_report",
    method: "list",
    filters: {},
    dataKey: ["median_resolution_time", "number_of_tickets_closed"],
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: issueManagementSupportedFilters,
    convertTo: "days",
    drilldown: azureDrilldown,
    xAxisLabelTransform: (params: any) => getXAxisLabel(params),
    onChartClickPayload: (args: { data: basicMappingType<any>; across?: string }) => {
      const { data, across } = args;
      const { activeLabel, activePayload } = data;
      let keyValue = activeLabel;
      if (across && (idFilters.includes(across) || attributeFilters.includes(across))) {
        keyValue = {
          id: get(activePayload, [0, "payload", "key"], "_UNASSIGNED_"),
          name: get(activePayload, [0, "payload", "name"], "_UNASSIGNED_")
        };
      }
      return keyValue;
    },
    transformFunction: (data: any) => azureResolutionTimeDataTransformer(data),
    weekStartsOnMonday: true,
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [VALUE_SORT_KEY]: "resolution_time",
    [SHOW_SETTINGS_TAB]: true,
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [HIDE_REPORT]: true,
    [FILTER_WITH_INFO_MAPPING]: [azureExcludeStatusFilter],
    [FE_BASED_FILTERS]: {
      workitem_created_at,
      workitem_updated_at,
      workitem_resolved_at
    },
    valuesToFilters: drillDownValuesToFiltersKeys,
    [SUPPORT_TICKET_CATEGORIZATION_FILTERS]: true,
    [SHOW_PROFILE_INSIDE_TAB]: WIDGET_CONFIGURATION_KEYS.SETTINGS,
    [FILTER_KEY_MAPPING]: {
      ticket_categories: "workitem_ticket_categories",
      ticket_categorization_scheme: "workitem_ticket_categorization_scheme"
    },
    [REPORT_FILTERS_CONFIG]: ResolutionTimeReportFiltersConfig,
    [MULTI_SERIES_REPORT_FILTERS_CONFIG]: AzureMultiSeriesResolutionTimeReportFiltersConfig,
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    [IMPLICITY_INCLUDE_DRILLDOWN_FILTER]: includeSolveTimeImplicitFilter,
    [PREV_REPORT_TRANSFORMER]: (data: any) => azureCommonPrevQueryTansformer(data)
  },
  azure_resolution_time_counts_stat: {
    name: "Issue Resolution Time Single Stat",
    application: application,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "issue_management_resolution_time_report",
    method: "list",
    filters: {},
    xaxis: false,
    chart_props: {
      unit: "Days"
    },
    defaultAcross: "workitem_created_at",
    default_query: { agg_type: "average", workitem_created_at: issueSingleDefualtCreatedAt },
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    compareField: "median",
    supported_filters: issueManagementSupportedFilters,
    transformFunction: (data: any) => azureIssueResolutionTimeReportStatTransformer(data),
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [FILTER_WITH_INFO_MAPPING]: [azureExcludeStatusFilter],
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [HIDE_REPORT]: true,
    [SHOW_SETTINGS_TAB]: true,
    [SHOW_METRICS_TAB]: false,
    [DEFAULT_METADATA]: issueSingleStatDefaultMeta,
    [PREV_REPORT_TRANSFORMER]: (data: any) => transformAzureIssuesResolutionTimeSingleStatReportPrevQuery(data),
    [STAT_TIME_BASED_FILTER]: {
      options: [
        { value: "workitem_created_at", label: "Workitem Created" },
        { value: "workitem_resolved_at", label: "Workitem Resolved" },
        { value: "workitem_updated_at", label: "Workitem Updated" }
      ],
      getFilterLabel: (data: any) => {
        const { filters } = data;
        return filters.across ? `${filters.across.replace("_at", "").replace("_", " ")} in` : "";
      },
      getFilterKey: (data: any) => {
        const { filters } = data;
        return filters.across || "";
      },
      defaultValue: "workitem_created_at"
    },
    hasStatUnit: (compareField: string) => true,
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: ResolutionTimeSingleStatReportFiltersConfig
  },
  azure_resolution_time_report_trends: {
    name: "Issues Resolution Time Trend Report",
    application: application,
    xaxis: false,
    composite: true,
    composite_transform: {
      min: "resolution_time_min",
      median: "resolution_time_median",
      max: "ressolution_time_max"
    },
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: {
      unit: "Days",
      chartProps: chartProps
    },
    uri: "issue_management_resolution_time_report",
    method: "list",
    filters: {
      across: "trend"
    },
    default_query: {
      [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.TIME_BASED]
    },
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: issueManagementSupportedFilters,
    convertTo: "days",
    drilldown: azureDrilldown,
    transformFunction: (data: any) => azureTrendTransformer(data),
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [FILTER_WITH_INFO_MAPPING]: [azureExcludeStatusFilter],
    [HIDE_REPORT]: true,
    [SHOW_AGGREGATIONS_TAB]: false,
    [SHOW_METRICS_TAB]: false,
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [SHOW_SETTINGS_TAB]: true,
    [FE_BASED_FILTERS]: {
      workitem_created_at,
      workitem_updated_at,
      workitem_resolved_at
    },
    valuesToFilters: drillDownValuesToFiltersKeys,
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    [IMPLICITY_INCLUDE_DRILLDOWN_FILTER]: includeSolveTimeImplicitFilter,
    [REPORT_FILTERS_CONFIG]: ResolutionTimeTrendsReportFiltersConfig,
    [PREV_REPORT_TRANSFORMER]: (data: any) => azureCommonPrevQueryTansformer(data)
  },
  azure_bounce_report: {
    name: "Issue Bounce Report",
    application: application,
    xaxis: true,
    defaultAcross: "assignee",
    across: AZURE_ACROSS_OPTION,
    chart_type: ChartType?.SCATTER,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: {
      yDataKey: "median",
      rangeY: ["min", "max"],
      unit: "Bounces",
      // When we do not want to sort the data for particular across value add across value in the array
      xAxisIgnoreSortKeys: ["priority"]
    },
    defaultSort: [{ id: "bounces", desc: true }],
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [VALUE_SORT_KEY]: "bounces",
    [SHOW_SETTINGS_TAB]: true,
    [HIDE_REPORT]: true,
    [FE_BASED_FILTERS]: {
      workitem_created_at,
      workitem_updated_at,
      workitem_resolved_at
    },
    default_query: {
      [WIDGET_DATA_SORT_FILTER_KEY]:
        widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED]
    },
    uri: "issue_management_bounce_report",
    method: "list",
    filters: {},
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: issueManagementSupportedFilters,
    drilldown: {
      ...azureDrilldown,
      drilldownVisibleColumn: ["workitem_id", "summary", "components", "bounces", "hops", "assignee"]
    },
    transformFunction: (data: any) => bounceReportTransformer(data),
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [REPORT_FILTERS_CONFIG]: BounceReportFiltersConfig,
    [SHOW_METRICS_TAB]: false,
    valuesToFilters: drillDownValuesToFiltersKeys,
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    xAxisLabelTransform: (params: any) => {
      const { item = {}, CustomFieldType, xAxisLabelKey, across } = params;
      const { key, additional_key } = item;
      let newLabel = key;

      if (idFilters.includes(across)) {
        newLabel = additional_key;
        return newLabel;
      }

      const isValidDate = isvalidTimeStamp(key);
      if (CustomFieldType && ((CustomTimeBasedTypes.includes(CustomFieldType) && isValidDate) || isValidDate)) {
        newLabel = moment(parseInt(key)).format("DD-MM-YYYY HH:mm:ss");
        return newLabel;
      }
      if (!newLabel) {
        newLabel = "UNRESOLVED";
      }
      return newLabel;
    },
    onChartClickPayload: (args: { data: basicMappingType<any>; across?: string }) => {
      const { data, across } = args;
      let activeLabel = data.key;
      if (across && across.includes("customfield_")) {
        return data.name;
      } else if (across && idFilters.includes(across)) {
        return {
          id: get(data, ["key"], "_UNASSIGNED_"),
          name: get(data, ["name"], "_UNASSIGNED_")
        };
      } else if (across && AZURE_TIME_FILTERS_KEYS.includes(across)) {
        const newData = data?.activePayload?.[0]?.payload;
        return convertEpochToDate(newData.key, DEFAULT_DATE_FORMAT, true);
      }
      return activeLabel;
    },
    [PREV_REPORT_TRANSFORMER]: (data: any) => azureCommonPrevQueryTansformer(data)
  },
  azure_bounce_report_trends: {
    name: "Issue Bounce Report Trends",
    application: application,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    chart_props: {
      unit: "Bounces",
      chartProps: chartProps
    },
    uri: "issue_management_bounce_report",
    composite: true,
    composite_transform: {
      min: "bounce_min",
      median: "bounce_median",
      max: "bounce_max"
    },
    method: "list",
    filters: {
      across: "trend"
    },
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [SHOW_SETTINGS_TAB]: true,
    default_query: {
      [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.TIME_BASED]
    },
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [HIDE_REPORT]: true,
    [FE_BASED_FILTERS]: {
      workitem_created_at,
      workitem_updated_at,
      workitem_resolved_at
    },
    supported_filters: issueManagementSupportedFilters,
    drilldown: azureDrilldown,
    transformFunction: (data: any) => trendReportTransformer(data),
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    valuesToFilters: drillDownValuesToFiltersKeys,
    [REPORT_FILTERS_CONFIG]: IssuesBounceReportTrendsFiltersConfig,
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    [PREV_REPORT_TRANSFORMER]: (data: any) => azureCommonPrevQueryTansformer(data)
  },
  azure_bounce_counts_stat: {
    name: "Issue Bounce Single Stat",
    application: application,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "issue_management_bounce_report",
    method: "list",
    filters: {
      across: "trend"
    },
    xaxis: false,
    chart_props: {
      unit: "Bounces"
    },
    default_query: statDefaultQuery,
    compareField: "median",
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: issueManagementSupportedFilters,
    drilldown: azureStatDrilldown,
    transformFunction: (data: any) => statReportTransformer(data),
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    supported_widget_types: ["stats"],
    xAxisLabelTransform: (params: any) => {
      const { interval, across, item = {} } = params;
      const { key, additional_key } = item;
      let newLabel = key;
      if (["priority"].includes(across)) {
        newLabel = get(staticPriorties, [key], key);
      }

      if (across === "code_area") {
        newLabel = key.split("\\").pop() || key;
      }

      return newLabel;
    },
    chart_click_enable: false,
    [HIDE_REPORT]: true,
    [FE_BASED_FILTERS]: {
      workitem_created_at,
      workitem_updated_at,
      workitem_resolved_at
    },
    valuesToFilters: drillDownValuesToFiltersKeys,
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: BounceSingleStatReportFiltersConfig,
    [PREV_REPORT_TRANSFORMER]: (data: any) => azureCommonPrevQueryTansformer(data)
  },
  azure_hops_report: {
    name: "Issue Hops Report",
    application: application,
    defaultAcross: "assignee",
    across: AZURE_ACROSS_OPTION,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultFilterKey: "median",
    showExtraInfoOnToolTip: ["total_tickets"],
    xaxis: true,
    default_query: {
      [WIDGET_DATA_SORT_FILTER_KEY]:
        widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED]
    },
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [VALUE_SORT_KEY]: "hops",
    defaultSort: [{ id: "hops", desc: true }],
    chart_props: {
      barProps: [
        {
          name: "min",
          dataKey: "min"
        },
        {
          name: "median",
          dataKey: "median"
        },
        {
          name: "max",
          dataKey: "max"
        }
      ],
      stacked: false,
      unit: "Hops",
      chartProps: chartProps,
      xAxisIgnoreSortKeys: ["priority"]
    },
    uri: "issue_management_hops_report",
    method: "list",
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [HIDE_REPORT]: true,
    [FE_BASED_FILTERS]: {
      workitem_created_at,
      workitem_updated_at,
      workitem_resolved_at,
      show_value_on_bar
    },
    supported_filters: issueManagementSupportedFilters,
    drilldown: azureDrilldown,
    transformFunction: (data: any) => seriesDataTransformer(data),
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    valuesToFilters: drillDownValuesToFiltersKeys,
    xAxisLabelTransform: (params: any) => {
      const { interval, across, item = {} } = params;
      const { key, additional_key } = item;
      let newLabel = key;

      if (idFilters.includes(across)) {
        newLabel = additional_key;
        return newLabel;
      }

      if (["priority"].includes(across)) {
        newLabel = get(staticPriorties, [key], key);
      }

      if (across === "code_area") {
        newLabel = key.split("\\").pop() || key;
      }

      return newLabel;
    },
    onChartClickPayload: onChartClickPayloadForId,
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: IssueHopsReportFiltersConfig,
    [PREV_REPORT_TRANSFORMER]: (data: any) => azureCommonPrevQueryTansformer(data)
  },
  azure_hops_report_trends: {
    name: "Issue Hops Report Trends",
    application: application,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    composite: true,
    composite_transform: {
      min: "hops_min",
      median: "hops_median",
      max: "hops_max"
    },
    xaxis: false,
    chart_props: {
      unit: "Hops",
      chartProps: chartProps
    },
    uri: "issue_management_hops_report",
    method: "list",
    filters: {
      across: "trend"
    },
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [SHOW_SETTINGS_TAB]: true,
    default_query: {
      [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.TIME_BASED]
    },
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [HIDE_REPORT]: true,
    [FE_BASED_FILTERS]: {
      workitem_created_at,
      workitem_updated_at,
      workitem_resolved_at
    },
    supported_filters: issueManagementSupportedFilters,
    drilldown: azureDrilldown,
    transformFunction: (data: any) => trendReportTransformer(data),
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    valuesToFilters: drillDownValuesToFiltersKeys,
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: IssueHopsReportTrendsFiltersConfig,
    [PREV_REPORT_TRANSFORMER]: (data: any) => azureCommonPrevQueryTansformer(data)
  },
  azure_hops_counts_stat: {
    name: "Issue Hops Single Stat",
    application: application,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "issue_management_hops_report",
    method: "list",
    filters: {
      across: "trend"
    },
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    xaxis: false,
    chart_props: {
      unit: "Hops"
    },
    default_query: statDefaultQuery,
    compareField: "median",
    supported_filters: issueManagementSupportedFilters,
    drilldown: azureDrilldown,
    transformFunction: (data: any) => statReportTransformer(data),
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    valuesToFilters: drillDownValuesToFiltersKeys,
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [FE_BASED_FILTERS]: {
      workitem_created_at,
      workitem_updated_at,
      workitem_resolved_at
    },
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: IssueHopsSingleStatFiltersConfig,
    [PREV_REPORT_TRANSFORMER]: (data: any) => azureCommonPrevQueryTansformer(data)
  },
  azure_first_assignee_report: {
    name: "Issue First Assignee Report",
    application: application,
    chart_type: ChartType?.BAR,
    defaultAcross: "assignee",
    across: AZURE_ACROSS_OPTION,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultFilterKey: "median",
    showExtraInfoOnToolTip: ["total_tickets"],
    xaxis: true,
    chart_props: {
      barProps: [
        {
          name: "min",
          dataKey: "min"
        },
        {
          name: "median",
          dataKey: "median"
        },
        {
          name: "max",
          dataKey: "max"
        }
      ],
      stacked: false,
      unit: "Days",
      chartProps: chartProps,
      xAxisIgnoreSortKeys: ["priority"]
    },
    uri: "issue_management_first_assignee_report",
    method: "list",
    filters: {},
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [VALUE_SORT_KEY]: "assign_to_resolve",
    [SHOW_SETTINGS_TAB]: true,
    default_query: {
      [WIDGET_DATA_SORT_FILTER_KEY]:
        widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED]
    },
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: issueManagementSupportedFilters,
    convertTo: "days",
    drilldown: azureDrilldown,
    transformFunction: (data: any) => seriesDataTransformer(data),
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [HIDE_REPORT]: true,
    [FE_BASED_FILTERS]: {
      workitem_created_at,
      workitem_updated_at,
      workitem_resolved_at,
      show_value_on_bar
    },
    xAxisLabelTransform: (params: any) => {
      const { interval, across, item = {} } = params;
      const { key, additional_key } = item;
      let newLabel = key;
      if (idFilters.includes(across)) {
        newLabel = additional_key;
        return newLabel;
      }
      if (["priority"].includes(across)) {
        newLabel = get(staticPriorties, [key], key);
      }

      if (across === "code_area") {
        newLabel = key.split("\\").pop() || key;
      }

      return newLabel;
    },
    onChartClickPayload: onChartClickPayloadForId,
    [REPORT_FILTERS_CONFIG]: FirstAssigneeReportFiltersConfig,
    valuesToFilters: drillDownValuesToFiltersKeys,
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    [PREV_REPORT_TRANSFORMER]: (data: any) => azureCommonPrevQueryTansformer(data)
  },
  azure_stage_bounce_report: {
    name: "Stage Bounce Report",
    application: application,
    xaxis: true,
    across: [...AZURE_ACROSS_OPTION, "stage"],
    defaultAcross: "stage",
    chart_type: ChartType?.STAGE_BOUNCE_CHART,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: {
      barProps: [
        {
          name: "total_tickets",
          dataKey: "total_tickets",
          unit: "Tickets"
        }
      ],
      stacked: false,
      unit: "Tickets",
      sortBy: "total_tickets",
      chartProps: chartProps
    },
    default_query: {
      [WIDGET_DATA_SORT_FILTER_KEY]:
        widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED],
      workitem_stages: ["Done"],
      metric: "mean"
    },
    uri: "issue_management_stage_bounce_report",
    method: "list",
    filters: {},
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    supported_filters: {
      ...issueManagementSupportedFilters,
      values: [...issueManagementSupportedFilters.values, "workitem_stage"]
    },
    stack_filters: ["project", "status", "priority", "assignee", "reporter", "workitem_type", "stage"],
    drilldown: azureDrilldown,
    transformFunction: (data: any) => stageBounceDataTransformer(data),
    valuesToFilters: { ...drillDownValuesToFiltersKeys, stage: "workitem_stages", workitem_stage: "workitem_stages" },
    tooltipMapping: {
      mean: "Mean Number of Times in stage",
      median: "Median Number of Times in stage",
      total_tickets: "Number of tickets"
    },
    requiredFilters: ["workitem_stage"],
    [SHOW_AGGREGATIONS_TAB]: true,
    [SHOW_METRICS_TAB]: true,
    [SHOW_SETTINGS_TAB]: true,
    [STACKS_SHOW_TAB]: WIDGET_CONFIGURATION_KEYS.AGGREGATIONS,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [WIDGET_VALIDATION_FUNCTION]: (payload: any) => {
      const { query = {} } = payload;
      let isValid = query?.workitem_stages && (query?.workitem_stages || []).length;
      if (isValid) {
        return true;
      } else if (!isValid && (query?.exclude?.workitem_stages || []).length) {
        return true;
      }
      return false;
    },
    getTotalKey: (params: any) => {
      const { metric } = params;
      return metric || "mean";
    },
    [STACKS_FILTER_STATUS]: (filters: any) => {
      return filters && filters.across && [...AZURE_TIME_FILTERS_KEYS, "stage"].includes(filters.across);
    },
    [INFO_MESSAGES]: {
      stacks_disabled: "Stacks option is not applicable"
    },
    [FE_BASED_FILTERS]: {
      workitem_resolved_at,
      stageBounceMetric
    },
    xAxisLabelTransform: (params: any) => getXAxisLabel(params),
    onChartClickPayload: (params: any) => {
      const { data, across, stage } = params;
      let value = data.activeLabel ?? data.name ?? "";
      if (AZURE_TIME_FILTERS_KEYS.includes(across)) {
        value = convertEpochToDate(data.key, DEFAULT_DATE_FORMAT, true);
      }

      if (["assignee", "reporter"].includes(across)) {
        value = {
          id: data.key,
          name: data?.name ?? data?.additional_key ?? data.key
        };
      }

      return {
        value,
        stage: stage ? [stage] : undefined
      };
    },
    [HIDE_REPORT]: true,
    shouldJsonParseXAxis: () => true,
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: StageBounceReportFiltersConfig,
    [PREV_REPORT_TRANSFORMER]: (data: any) => azureCommonPrevQueryTansformer(data)
  },
  azure_stage_bounce_single_stat: {
    name: "Stage Bounce Single Stat",
    application: application,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "issue_management_stage_bounce_report",
    method: "list",
    filters: {
      across: "trend"
    },
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    xaxis: false,
    chart_props: {
      unit: "Times"
    },
    default_query: { ...statDefaultQuery, workitem_stages: ["Done"], metric: "mean" },
    compareField: "mean",
    supported_filters: {
      ...issueManagementSupportedFilters,
      values: [...issueManagementSupportedFilters.values, "workitem_stage"]
    },
    requiredFilters: ["workitem_stage"],
    drilldown: {},
    transformFunction: (data: any) => statReportTransformer(data),
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    valuesToFilters: { ...drillDownValuesToFiltersKeys, stage: "workitem_stages", workitem_stage: "workitem_stages" },
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [WIDGET_VALIDATION_FUNCTION]: (payload: any) => {
      const { query = {} } = payload;
      let isValid = query?.workitem_stages && (query?.workitem_stages || []).length;
      if (isValid) {
        return true;
      } else if (!isValid && (query?.exclude?.workitem_stages || []).length) {
        return true;
      }
      return false;
    },
    [FE_BASED_FILTERS]: {
      workitem_created_at,
      workitem_updated_at,
      workitem_resolved_at,
      stageBounceMetric
    },
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: StageBounceSingleStatReportFiltersConfig
  },
  azure_effort_investment_engineer_report: {
    name: "Effort Investment By Engineer",
    application: application,
    chart_type: ChartType?.ENGINEER_TABLE,
    chart_container: ChartContainerType.BA_WIDGET_API_WRAPPER,
    xaxis: false,
    defaultAcross: "assignee",
    uri: "azure_effort_investment_tickets",
    method: "list",
    filters: {},
    chart_props: {},
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    supported_filters: issueManagementEffortInvestmentSupportedFilters,
    drilldown: {},
    requiredFilters: [TICKET_CATEGORIZATION_SCHEMES_KEY],
    [FE_BASED_FILTERS]: { ba_workitem_resolved_at, workitem_created_at, workitem_updated_at },
    [SUPPORT_ACTIVE_WORK_UNIT_FILTERS]: true,
    default_query: {
      workitem_resolved_at: {
        // required filters and default is last month
        $gt: moment.utc().subtract(4, "months").startOf("day").unix().toString(),
        $lt: moment.utc().unix().toString()
      },
      [TICKET_CATEGORIZATION_UNIT_FILTER_KEY]: "azure_effort_investment_tickets",
      [ACTIVE_WORK_UNIT_FILTER_KEY]: "active_azure_ei_ticket_count"
    },
    [DEFAULT_METADATA]: {
      ...azureEITimeRangeDefMeta,
      effort_type: EffortType.COMPLETED_EFFORT
    },
    [WIDGET_MIN_HEIGHT]: "36rem",
    [SHOW_EFFORT_UNIT_INSIDE_TAB]: WIDGET_CONFIGURATION_KEYS.SETTINGS,
    [SHOW_PROFILE_INSIDE_TAB]: WIDGET_CONFIGURATION_KEYS.SETTINGS,
    [SHOW_SETTINGS_TAB]: true,
    [SHOW_METRICS_TAB]: false,
    [SHOW_AGGREGATIONS_TAB]: false,
    [PREVIEW_DISABLED]: true,
    [SUPPORT_TICKET_CATEGORIZATION_UNIT_FILTERS]: true,
    [SUPPORT_TICKET_CATEGORIZATION_FILTERS]: true,
    [REQUIRED_FILTERS_MAPPING]: {
      [RequiredFiltersType.SCHEME_SELECTION]: true
    },
    [DISABLE_CATEGORY_SELECTION]: true,
    [DISABLE_XAXIS]: true,
    /**
     * Refer to @type ReportCSVDownloadConfig for more info on fields
     */
    [REPORT_CSV_DOWNLOAD_CONFIG]: {
      widgetFiltersTransformer: EIAlignmentReportCSVFiltersTransformer,
      widgetDynamicURIGetFunc: EIDynamicURITransformer,
      widgetCSVColumnsGetFunc: EIEngineerReportCSVColumns,
      widgetCSVDataTransform: EIEngineerCSVDataTransformer,
      [SUB_COLUMNS_TITLE]: ["Total", "Percentage"],
      [EXCLUDE_SUB_COLUMNS_FOR]: ["Engineer", "Remaining Allocation"]
    },
    [DefaultKeyTypes.DEFAULT_SCHEME_KEY]: true,
    [DefaultKeyTypes.DEFAULT_EFFORT_UNIT]: "azure_effort_investment_tickets",
    transformFunction: (data: any) => tableTransformer(data),
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    [WIDGET_VALIDATION_FUNCTION]: jiraBaValidationHelper,
    [REPORT_FILTERS_CONFIG]: (args: any) => {
      const { filters } = args;
      const uriUnit = get(filters, [TICKET_CATEGORIZATION_UNIT_FILTER_KEY], undefined);
      if ([EffortUnitType.AZURE_COMMIT_COUNT, EffortUnitType.COMMIT_COUNT].includes(uriUnit)) {
        return AzureCommitEIByEngineerFiltersConfig;
      }
      return AzureEIByEngineerFiltersConfig;
    },
    [HIDE_CUSTOM_FIELDS]: (args: any) => {
      const { filters } = args;
      const uriUnit = get(filters, [TICKET_CATEGORIZATION_UNIT_FILTER_KEY], undefined);
      if ([EffortUnitType.AZURE_COMMIT_COUNT, EffortUnitType.COMMIT_COUNT].includes(uriUnit)) {
        return true;
      }
      return false;
    },
    [PREV_REPORT_TRANSFORMER]: (data: any) => azureCommonPrevQueryTansformer(data)
  },
  azure_effort_alignment_report: {
    name: "Effort Alignment Report",
    application: application,
    chart_type: ChartType.ALIGNMENT_TABLE,
    chart_container: ChartContainerType.BA_WIDGET_API_WRAPPER,
    xaxis: true,
    defaultAcross: "ticket_category",
    uri: "active_azure_ei_ticket_count",
    method: "list",
    filters: {},
    chart_props: {},
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    supported_filters: issueManagementEffortInvestmentSupportedFilters,
    drilldown: {},
    [WIDGET_MIN_HEIGHT]: "28rem",
    requiredFilters: [TICKET_CATEGORIZATION_SCHEMES_KEY],
    [FE_BASED_FILTERS]: { ba_workitem_resolved_at },
    [DEFAULT_METADATA]: azureEITimeRangeDefMeta,
    default_query: {
      workitem_resolved_at: {
        // required filters and default is last month
        $gt: moment.utc().subtract(4, "months").startOf("day").unix().toString(),
        $lt: moment.utc().unix().toString()
      },
      [ACTIVE_WORK_UNIT_FILTER_KEY]: "active_azure_ei_ticket_count"
    },
    [SUPPORT_ACTIVE_WORK_UNIT_FILTERS]: true,
    [SHOW_PROFILE_INSIDE_TAB]: WIDGET_CONFIGURATION_KEYS.SETTINGS,
    [SHOW_SETTINGS_TAB]: true,
    [SHOW_METRICS_TAB]: false,
    [SHOW_AGGREGATIONS_TAB]: false,
    [STORE_ACTION]: jiraAlignmentReport,
    [PREVIEW_DISABLED]: true,
    [SUPPORT_TICKET_CATEGORIZATION_FILTERS]: true,
    [REQUIRED_FILTERS_MAPPING]: {
      [RequiredFiltersType.SCHEME_SELECTION]: true
    },
    [DISABLE_CATEGORY_SELECTION]: true,
    [DISABLE_XAXIS]: true,
    [DefaultKeyTypes.DEFAULT_SCHEME_KEY]: true,
    [DefaultKeyTypes.DEFAULT_EFFORT_UNIT]: EffortUnitType.AZURE_TICKETS_REPORT,
    /**
     * Refer to @type ReportCSVDownloadConfig for more info on fields
     */
    [REPORT_CSV_DOWNLOAD_CONFIG]: {
      widgetFiltersTransformer: EIAlignmentReportCSVFiltersTransformer,
      widgetDynamicURIGetFunc: EIDynamicURITransformer,
      widgetCSVColumnsGetFunc: EIAlignmentReportCSVColumns,
      widgetCSVDataTransform: EIAlignmentReportCSVDataTransformer
    },
    transformFunction: (data: any) => tableTransformer(data),
    [WIDGET_VALIDATION_FUNCTION]: jiraBaValidationHelper,
    [API_BASED_FILTER]: azureApiBasedFilters,
    [FIELD_KEY_FOR_FILTERS]: azureApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: AzureEffortAlignmentReportFiltersConfig,
    [PREV_REPORT_TRANSFORMER]: (data: any) => azureCommonPrevQueryTansformer(data)
  }
};
