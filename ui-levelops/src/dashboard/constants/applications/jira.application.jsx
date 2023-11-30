import { GROUP_BY_TIME_FILTERS } from "constants/filters";
import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import {
  jiraResolutionTimeDataTransformer,
  seriesDataTransformer,
  statReportTransformer,
  tableTransformer,
  timeAcrossStagesDataTransformer,
  trendReportTransformer
} from "custom-hooks/helpers";
import { jiraBacklogTransformerWrapper, jiraTicketsReportChangeTransform } from "custom-hooks/helpers/helper";
import { JiraSalesforceNodeType, JiraZendeskNodeType } from "custom-hooks/helpers/sankey.helper";
import { sprintStatReportTransformer } from "custom-hooks/helpers/sprintStatReporthelper";
import { IssueVisualizationTypes } from "dashboard/constants/typeConstants";
import { CustomTimeBasedTypes, modificationMappedValues } from "dashboard/graph-filters/components/helper";
import { sprintMetricStatCsvTransformer } from "dashboard/helpers/csv-transformers/sprintMetricStatCSVTransformer";
import {
  jiraDrilldownTransformer,
  jiraHygieneDrilldownTranformer,
  jiraZendeskSalesforceDrilldownTransformer
} from "dashboard/helpers/drilldown-transformers";
import {
  AssigneeReportTableConfig,
  SalesforceJiraTableConfig,
  ZendeskJiraTableConfig
} from "dashboard/pages/dashboard-tickets/configs";
import { JiraSalesforceSupportEscalationReportFiltersConfig } from "dashboard/report-filters/cross-aggregation/jira-salesforce";
import { JiraZendeskSupportEscalationReportFiltersConfig } from "dashboard/report-filters/cross-aggregation/jira-zendesk";
import moment from "moment";
import React from "react";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { sprintMetricsDistributionTransformer } from "transformers/reports";
import { convertEpochToDate, isvalidTimeStamp } from "utils/dateUtils";
import { toTitleCase } from "utils/stringUtils";
import {
  bounceReportTransformer,
  leadTimePhaseTransformer,
  leadTimeTrendTransformer,
  leadTimeTypeTransformer
} from "../../../custom-hooks/helpers";
import {
  issuesSingleStatReportTransformer,
  jiraIssueResolutionTimeReportStatTransformer
} from "../../../custom-hooks/helpers/issuesSingleStat.helper";
import { sprintMetricsChartColors, sprintMetricsPercentageColors } from "../../../shared-resources/charts/chart-themes";
import { JiraIssueLink } from "../../../shared-resources/components/jira-issue-link/jira-issue-link-component";
import {
  sprintImpactTransformer,
  sprintMetricsPercentReportTransformer,
  sprintMetricsTrendTransformer
} from "../../../transformers/reports/sprintMetricsPercentReportTransformer";
import { DEFAULT_DATE_FORMAT } from "../../../utils/dateUtils";
import { completedDateOptions } from "../../graph-filters/components/Constants";
import { leadTimeCsvTransformer } from "../../helpers/csv-transformers/leadTimeCsvTransformer";
import { genericDrilldownTransformer } from "../../helpers/drilldown-transformers";
import {
  ChartContainerType,
  transformIssueBacklogTrendReportPrevQuery,
  transformIssueResolutionTimeSingleStatReportPrevQuery,
  transformIssuesSingleStatReportPrevQuery,
  transformIssuesReportPrevQuery,
  transformLeadTimeReportPrevQuery,
  transformSprintMetricsTrendReportPrevQuery
} from "../../helpers/helper";
import { hygieneWeightValidationHelper, issuesSingleStatValidationHelper } from "../../helpers/widgetValidation.helper";
import { sprintGoalReportColumns } from "../../pages/dashboard-tickets/configs/sprintSingleStatTableConfig";
import {
  SHOW_PROFILE_INSIDE_TAB,
  SUPPORT_TICKET_CATEGORIZATION_FILTERS
} from "../bussiness-alignment-applications/constants";
import {
  jiraBacklogDrillDown,
  jiraBounceReportDrilldown,
  jiraDrilldown,
  jiraResponseTimeTrendDrilldown,
  jiraResponseTimeReportDrilldown,
  jiraIssueTimeAcrossStagesDrilldown,
  jiraLeadTimeDrilldown,
  jiraStatDrilldown,
  sprintMetricSingleStatDrilldown,
  sprintMetricTrendReportDrilldown
} from "../drilldown.constants";
import { WidgetFilterType } from "../enums/WidgetFilterType.enum";
import { aggMetric, defaultFilter, percentile } from "../FE-BASED/jira.FEbased";
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
  PARTIAL_FILTER_MAPPING_KEY,
  IS_FRONTEND_REPORT
} from "../filter-key.mapping";
import {
  ALLOWED_WIDGET_DATA_SORTING,
  FILTER_NAME_MAPPING,
  issueLeadTimeFilterOptionsMapping,
  jiraCommonFilterOptionsMapping,
  jiraTimeAcrossFilterOptionsMapping,
  VALUE_SORT_KEY,
  WIDGET_DATA_SORT_FILTER_KEY,
  WIDGET_VALIDATION_FUNCTION
} from "../filter-name.mapping";
import {
  FILTER_WITH_INFO_MAPPING,
  jiraExcludeStatusFilter,
  jiraHideStatusFilter,
  leadTimeExcludeStageFilter
} from "../filterWithInfo.mapping";
import { CustomFieldMappingKey, hygieneDefaultSettings, statDefaultQuery, zendeskCustomFieldsMapping } from "../helper";
import { hygieneTypes, defaultHygineTrendsFilters } from "../hygiene.constants";
import {
  jiraSalesforceSupportedFilters,
  jiraSprintGoalSupportedFilters,
  jiraSupportedFilters,
  jiraZenDeskSupportedFilters,
  leadTimeJiraSupportedFilters
} from "../supported-filters.constant";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "../WidgetDataSortingFilter.constant";
import {
  COMPARE_X_AXIS_TIMESTAMP,
  FE_BASED_FILTERS,
  IMPLICITY_INCLUDE_DRILLDOWN_FILTER,
  INCLUDE_INTERVAL_IN_PAYLOAD,
  INFO_MESSAGES,
  LABEL_TO_TIMESTAMP,
  PREVIEW_DISABLED,
  PREV_REPORT_TRANSFORMER,
  REPORT_FILTERS_CONFIG,
  STACKS_FILTER_STATUS,
  TIME_FILTER_RANGE_CHOICE_MAPPER,
  HIDE_CUSTOM_FIELDS,
  MULTI_SERIES_REPORT_FILTERS_CONFIG
} from "./names";
import { API_BASED_FILTER, FIELD_KEY_FOR_FILTERS } from "../filter-key.mapping";
import { get } from "lodash";
import { JIRA_PARTIAL_FILTER_KEY_MAPPING } from "dashboard/reports/jira/constant";
import { stageBounceDataTransformer } from "../../../custom-hooks/helpers/stageBounce.helper";
import { JiraResolutionTimeReportFiltersConfig } from "dashboard/reports/jira/resolution-time-report/filters.config";
import { JiraBounceReportFiltersConfig } from "dashboard/reports/jira/bounce-report/filters.config";
import { JiraHopsReportFiltersConfig } from "dashboard/reports/jira/jira-hops-report/filters.config";
import { JiraResponseTimeReportFiltersConfig } from "dashboard/reports/jira/response-time-report/filters.config";
import { JiraFirstAssigneeReportFiltersConfig } from "dashboard/reports/jira/first-assignee-report/filters.config";
import { JiraIssuesReportFiltersConfig } from "dashboard/reports/jira/issues-report/filters.config";
import { JiraIssuesAssigneeTimeReportFiltersConfig } from "dashboard/reports/jira/issue-assignee-time-report/filters.config";
import { JiraIssuesBounceReportTrendsFiltersConfig } from "dashboard/reports/jira/bounce-reports-trends/filters.config";
import { JiraResponseTimeReportTrendsFiltersConfig } from "dashboard/reports/jira/response-time-report-trends/filters.config";
import { JiraResolutionTimeTrendReportFiltersConfig } from "dashboard/reports/jira/resolution-time-report-trends/filters.config";
import { JiraSprintGoalReportFiltersConfig } from "dashboard/reports/jira/sprint-goal-report/filters.config";
import { JiraIssueHygieneReportFiltersConfig } from "../../reports/jira/hygiene-report/filters.config";
import { JiraIssueBounceSingleStat } from "../../reports/jira/bounce-report-single-stat/filters.config";
import { JiraHopsReportTrendsFiltersConfig } from "../../reports/jira/hops-trend-report/filters.config";
import { JiraHopsReportSingleStatFiltersConfig } from "../../reports/jira/hops-count-stat/filters.config";
import { JiraResponseTimeSingleStatFiltersConfig } from "../../reports/jira/response-time-single-stat/filters.config";
import { JiraResolutionTimeSingleStatFiltersConfig } from "../../reports/jira/resolution-time-count-stat/filters.config";
import { JiraIssueHygieneReportTrendsFiltersConfig } from "../../reports/jira/hygiene-report-trends/filter.config";
import { JiraBacklogTrendReportFiltersConfig } from "../../reports/jira/jira-backlog-trend-report/filters.config";
import { JiraSprintMetricTrendReportFiltersConfig } from "../../reports/jira/sprint-metric-trend-report/filters.config";
import { JiraSprintMetricPercentageTrendReportFiltersConfig } from "../../reports/jira/sprint-metric-percentage-trend-report/filters.config";
import { JiraIssuesTrendReportTrendsFiltersConfig } from "../../reports/jira/tickets-report-trend/filters.config";
import { JiraSprintSingleStatReportFiltersConfig } from "../../reports/jira/sprint-metric-single-stat/filters.config";
import { JiraIssuesByFirstAssigneeFiltersConfig } from "../../reports/jira/jira-tickets-count-by-first-assignee/filters.config";
import { JiraIssuesSingleStatFiltersConfig } from "../../reports/jira/tickets-counts-stat/filters.config";
import { JiraTimeAcrossStagesReportFiltersConfig } from "../../reports/jira/jira-time-across-stages/filters.config";
import { includeSolveTimeImplicitFilter } from "./constant";
import { CUSTOM_FIELD_PREFIX } from "../constants";
import { getXAxisLabel } from "shared-resources/charts/bar-chart/bar-chart.helper";
import { JiraSprintImpactOfUnestimatedTicketReportFiltersConfig } from "dashboard/reports/jira/sprint-impact-of-unestimated-tickets-report/filter.config";
import { IssueLeadTimeTrendReportFiltersConfig } from "dashboard/reports/jira/lead-time-trend-report/filters.config";
import { JiraStageBounceReportFiltersConfig } from "dashboard/reports/jira/stage-bounce-report/filter.config";
import { JiraStageBounceSingleStatReportFiltersConfig } from "dashboard/reports/jira/stage-bounce-single-stat/filter.config";
import { JiraSprintDistributionRetrospectiveReportFiltersConfig } from "dashboard/reports/jira/sprint-distribution-retrospective-report/filter.config";
import { IssueLeadTimeByTypeReportFiltersConfig } from "dashboard/reports/jira/lead-time-by-type-report/filters.config";
import { IssueLeadTimeByStageReportFiltersConfig } from "dashboard/reports/jira/lead-time-by-stage-report/filters.config";
import { JiraMultiSeriesIssuesReportFiltersConfig } from "dashboard/reports/multiseries-reports/jira/tickets-report/filter.config";
import { JiraMultiSeriesBacklogTrendReportFiltersConfig } from "dashboard/reports/multiseries-reports/jira/backlog-trend-report/filter.config";
import { JiraMultiSeriesResolutionTimeReportFiltersConfig } from "dashboard/reports/multiseries-reports/jira/issue-resolution-time-report/filter.config";
import { LEAD_TIME_STAGE_REPORT_DESCRIPTION } from "dashboard/reports/jira/lead-time-by-stage-report/constants";

import momentDurationFormatSetup from "moment-duration-format";
import { REPORT_KEY_IS_ENABLED } from "../../reports/constants";
import { mapFiltersBeforeCallIssueSingleStat } from "dashboard/reports/jira/tickets-counts-stat/helper";
import { mapFiltersBeforeCallIssueReport } from "dashboard/reports/jira/issues-report/helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

momentDurationFormatSetup(moment);

const JIRA_TICKETS_APPEND_ACROSS_OPTIONS = [{ label: "Ticket", value: "parent" }];

const chartProps = {
  barGap: 0,
  margin: { top: 20, right: 5, left: 5, bottom: 50 }
};

const jiraBacklogTrendDefaultQuery = {
  interval: "week",
  [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.TIME_BASED],
  snapshot_range: {
    $gt: moment.utc().subtract(3, "weeks").startOf("week").unix().toString(),
    $lt: moment.utc().unix().toString()
  }
};

export const resolutionTimeDefaultQuery = {
  metric: ["median_resolution_time", "number_of_tickets_closed"],
  [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED],
  issue_resolved_at: {
    $gt: moment.utc().subtract(3, "weeks").startOf("week").unix().toString(),
    $lt: moment.utc().unix().toString()
  }
};

export const azureResolutionTimeDefaultQuery = {
  metric: ["median_resolution_time", "number_of_tickets_closed"],
  workitem_resolved_at: {
    $gt: moment.utc().subtract(3, "weeks").startOf("week").unix().toString(),
    $lt: moment.utc().unix().toString()
  }
};

const jiraAcrossStagesDefaultQuery = {
  metric: "median_time",
  // [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED],
  issue_resolved_at: {
    $gt: moment.utc().subtract(3, "weeks").startOf("week").unix().toString(),
    $lt: moment.utc().unix().toString()
  }
};

const sprintDefaultQuery = {
  agg_type: "average", // transformer use it for average calculation
  completed_at: {
    // required filters and default is last month
    $gt: moment.utc().subtract(30, "days").startOf("day").unix().toString(),
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
    jira_issue_created_at: {
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

const sprintDefaultMeta = {
  [RANGE_FILTER_CHOICE]: {
    completed_at: {
      type: "relative",
      relative: {
        last: {
          num: 4,
          unit: "weeks"
        },
        next: {
          unit: "today"
        }
      }
    }
  }
};

const jiraLeadTimeDefaultQuery = {
  limit_to_only_applicable_data: false
};

const sprintMetricsPercentageReport = {
  ...sprintDefaultQuery,
  interval: "week",
  metric: ["commit_done_ratio", "creep_done_to_commit_ratio"]
};

const sprintMetricsTrendReport = {
  ...sprintDefaultQuery,
  interval: "week",
  metric: ["creep_done_points", "commit_done_points", "commit_not_done_points", "creep_not_done_points"]
};

const jiraTicketReportQuery = {
  metric: "ticket",
  [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED],
  visualization: IssueVisualizationTypes.BAR_CHART
};

// hideOnFilterValueKeys key hides filter if value for particular filter is present
const sprint_end_date = {
  type: WidgetFilterType.TIME_BASED_FILTERS,
  label: "Sprint end date",
  BE_key: "completed_at",
  slicing_value_support: true,
  configTab: WIDGET_CONFIGURATION_KEYS.FILTERS,
  hideOnFilterValueKeys: ["state"]
};

const issue_resolved_at = {
  type: WidgetFilterType.TIME_BASED_FILTERS,
  label: "Issue Resolved In",
  BE_key: "issue_resolved_at",
  slicing_value_support: true,
  configTab: WIDGET_CONFIGURATION_KEYS.FILTERS
};
const idFilters = ["assignee", "reporter", "first_assignee"];

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

const onChartClickPayloadForId = params => {
  const { data, across } = params;
  const { activeLabel, activePayload } = data;
  let keyValue = activeLabel;
  if (idFilters.includes(across) || across.includes(CUSTOM_FIELD_PREFIX)) {
    keyValue = {
      id: get(activePayload, [0, "payload", "key"], "_UNASSIGNED_"),
      name: get(activePayload, [0, "payload", "name"], "_UNASSIGNED_")
    };
  }
  return keyValue;
};

const xAxisLabelTransformForCustomFields = params => {
  const { across, item = {}, CustomFieldType } = params;
  const { key, additional_key } = item;
  let newLabel = key;
  if (idFilters.includes(across)) {
    newLabel = additional_key;
    return newLabel;
  }
  const isValidDate = isvalidTimeStamp(newLabel);
  if ((CustomTimeBasedTypes.includes(CustomFieldType) && isValidDate) || isValidDate) {
    newLabel = moment(parseInt(key)).format("DD-MM-YYYY HH:mm:ss");
    return newLabel;
  }
  if (!newLabel) {
    newLabel = "UNRESOLVED";
  }
  return newLabel;
};

const jiraApiBasedFilterKeyMapping = {
  assignees: "assignee",
  reporters: "reporter",
  first_assignees: "first_assignee",
  jira_assignees: "jira_assignee",
  jira_reporters: "jira_reporter"
};

const issueFirstAssigneeReportImplicitFilter = {
  missing_fields: {
    first_assignee: false
  }
};

const getTotalLabelIssuesReport = data => {
  const { unit } = data;
  return unit === "Story Points" ? "Total sum of story points" : "Total number of tickets";
};

const leadTimeValuesToFiltersKeys = {
  jira_fix_version: "jira_fix_versions"
};

// @TODO: add docs file.
export const JiraDashboards = {
  bounce_report: {
    name: "Issue Bounce Report",
    application: IntegrationTypes.JIRA,
    xaxis: true,
    defaultAcross: "assignee",
    chart_type: ChartType?.SCATTER,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: {
      yDataKey: "median",
      rangeY: ["min", "max"],
      unit: "Bounces",
      // When we do not want to sort the data for particular across value add across value in the array
      xAxisIgnoreSortKeys: ["priority"],
      xAxisLabelKey: "additional_key"
    },
    defaultSort: [{ id: "bounces", desc: true }],
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [VALUE_SORT_KEY]: "bounces",
    [SHOW_SETTINGS_TAB]: true,
    default_query: {
      [WIDGET_DATA_SORT_FILTER_KEY]:
        widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED]
    },
    uri: "bounce_report",
    method: "list",
    filters: {},
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: jiraSupportedFilters,
    drilldown: {
      ...jiraBounceReportDrilldown,
      drilldownVisibleColumn: ["key", "summary", "component_list", "bounces", "hops", "assignee"]
    },
    transformFunction: data => bounceReportTransformer(data),
    [FILTER_NAME_MAPPING]: jiraCommonFilterOptionsMapping,
    [FE_BASED_FILTERS]: {
      issue_resolved_at
    },
    xAxisLabelTransform: xAxisLabelTransformForCustomFields,
    onChartClickPayload: ({ data, across }) => {
      let activeLabel = data.key;
      if (across && across.includes(CUSTOM_FIELD_PREFIX)) {
        return {
          id: get(data, ["key"], "_UNASSIGNED_"),
          name: get(data, ["name"], "_UNASSIGNED_")
        };
      } else if (across && ["issue_created", "issue_updated"].includes(across)) {
        return data?.activeLabel;
      } else if (across && idFilters.includes(across)) {
        return {
          id: get(data, ["key"], "_UNASSIGNED_"),
          name: get(data, ["name"], "_UNASSIGNED_")
        };
      }
      return activeLabel;
    },
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraBounceReportFiltersConfig
  },
  hops_report: {
    name: "Issue Hops Report",
    application: IntegrationTypes.JIRA,
    defaultAcross: "assignee",
    [SHOW_SETTINGS_TAB]: true,
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
    uri: "hops_report",
    method: "list",
    filters: {},
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: jiraSupportedFilters,
    drilldown: {
      ...jiraBounceReportDrilldown,
      defaultSort: [{ id: "hops", desc: true }]
    },
    transformFunction: data => seriesDataTransformer(data),
    [FILTER_NAME_MAPPING]: jiraCommonFilterOptionsMapping,
    [FE_BASED_FILTERS]: {
      issue_resolved_at
    },
    xAxisLabelTransform: xAxisLabelTransformForCustomFields,
    onChartClickPayload: onChartClickPayloadForId,
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraHopsReportFiltersConfig
  },
  response_time_report: {
    name: "Issue Response Time Report",
    application: IntegrationTypes.JIRA,
    defaultAcross: "assignee",
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
    [VALUE_SORT_KEY]: "response_time",
    [SHOW_SETTINGS_TAB]: true,
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
    uri: "response_time_report",
    method: "list",
    filters: {},
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: jiraSupportedFilters,
    convertTo: "days",
    drilldown: jiraResponseTimeReportDrilldown,
    transformFunction: data => seriesDataTransformer(data),
    [FILTER_NAME_MAPPING]: jiraCommonFilterOptionsMapping,
    [FE_BASED_FILTERS]: {
      issue_resolved_at
    },
    xAxisLabelTransform: xAxisLabelTransformForCustomFields,
    onChartClickPayload: onChartClickPayloadForId,
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraResponseTimeReportFiltersConfig,
    [IMPLICITY_INCLUDE_DRILLDOWN_FILTER]: includeSolveTimeImplicitFilter
  },
  first_assignee_report: {
    name: "Issue First Assignee Report",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.BAR,
    defaultAcross: "assignee",
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
    uri: "first_assignee_report",
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
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: jiraSupportedFilters,
    convertTo: "days",
    drilldown: jiraBounceReportDrilldown,
    transformFunction: data => seriesDataTransformer(data),
    [FILTER_NAME_MAPPING]: jiraCommonFilterOptionsMapping,
    [FE_BASED_FILTERS]: {
      issue_resolved_at
    },
    xAxisLabelTransform: xAxisLabelTransformForCustomFields,
    onChartClickPayload: onChartClickPayloadForId,
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [IMPLICITY_INCLUDE_DRILLDOWN_FILTER]: issueFirstAssigneeReportImplicitFilter,
    [REPORT_FILTERS_CONFIG]: JiraFirstAssigneeReportFiltersConfig
  },
  resolution_time_report: {
    name: "Issue Resolution Time Report",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultAcross: "assignee",
    default_query: resolutionTimeDefaultQuery,
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
      dataTruncatingValue: 2,
      stacked: false,
      unit: "Days",
      chartProps: chartProps,
      xAxisIgnoreSortKeys: ["priority"]
    },
    tooltipMapping: { number_of_tickets_closed: "Number of Tickets" },
    uri: "resolution_time_report",
    method: "list",
    filters: {},
    dataKey: ["median_resolution_time", "number_of_tickets_closed"],
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: jiraSupportedFilters,
    convertTo: "days",
    drilldown: jiraDrilldown,
    appendAcrossOptions: [{ label: "Ticket Category", value: "ticket_category" }],
    // shouldReverseApiData: params => {
    //   const { across } = params;
    //   let should = false;
    //   if (["issue_created", "issue_updated", "issue_resolved"].includes(across)) {
    //     should = true;
    //   }

    //   return should;
    // },
    xAxisLabelTransform: params => getXAxisLabel(params),
    transformFunction: data => jiraResolutionTimeDataTransformer(data),
    weekStartsOnMonday: true,
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: {
      issue_resolved_at: "jira_issue_resolved_at"
    },
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [VALUE_SORT_KEY]: "resolution_time",
    [SHOW_SETTINGS_TAB]: true,
    [FILTER_NAME_MAPPING]: jiraTimeAcrossFilterOptionsMapping,
    [FILTER_WITH_INFO_MAPPING]: [jiraExcludeStatusFilter],
    [SUPPORT_TICKET_CATEGORIZATION_FILTERS]: true,
    [SHOW_PROFILE_INSIDE_TAB]: WIDGET_CONFIGURATION_KEYS.SETTINGS,
    onChartClickPayload: onChartClickPayloadForId,
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraResolutionTimeReportFiltersConfig,
    [MULTI_SERIES_REPORT_FILTERS_CONFIG]: JiraMultiSeriesResolutionTimeReportFiltersConfig,
    [IMPLICITY_INCLUDE_DRILLDOWN_FILTER]: includeSolveTimeImplicitFilter
  },
  tickets_report: {
    name: "Issues Report",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.BAR,
    defaultAcross: "assignee",
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    stack_filters: [...jiraSupportedFilters.values, "ticket_category"],
    chart_props: {
      barProps: [
        {
          name: "total_tickets",
          dataKey: "total_tickets",
          unit: "Tickets"
        }
      ],
      pieProps: {
        cx: "50%",
        innerRadius: 70,
        outerRadius: 110
      },
      stacked: false,
      unit: "Tickets",
      sortBy: "total_tickets",
      chartProps: chartProps
    },
    uri: "tickets_report",
    storyPointUri: "story_point_report",
    method: "list",
    filters: {},
    default_query: jiraTicketReportQuery,
    appendAcrossOptions: [
      ...JIRA_TICKETS_APPEND_ACROSS_OPTIONS,
      { label: "Ticket Category", value: "ticket_category" }
    ],
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [ALLOW_KEY_FOR_STACKS]: true,
    supported_filters: jiraSupportedFilters,
    drilldown: jiraDrilldown,
    [STACKS_SHOW_TAB]: WIDGET_CONFIGURATION_KEYS.AGGREGATIONS,
    [SHOW_METRICS_TAB]: true,
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [VALUE_SORT_KEY]: "ticket_count",
    [SHOW_SETTINGS_TAB]: true,
    xAxisLabelTransform: params => getXAxisLabel(params),
    transformFunction: data => jiraTicketsReportChangeTransform(data),
    weekStartsOnMonday: true,
    [FILTER_NAME_MAPPING]: jiraCommonFilterOptionsMapping,
    [FE_BASED_FILTERS]: {
      issue_resolved_at,
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
        optionsTransformFn: data => {
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
      }
    },
    [STACKS_FILTER_STATUS]: filters => {
      return filters && filters.visualization && filters.visualization === IssueVisualizationTypes.DONUT_CHART;
    },
    [INFO_MESSAGES]: {
      stacks_disabled: "Stacks option is not applicable for Donut visualization"
    },
    [SUPPORT_TICKET_CATEGORIZATION_FILTERS]: true,
    [SHOW_PROFILE_INSIDE_TAB]: WIDGET_CONFIGURATION_KEYS.SETTINGS,
    ["filterOptionMap"]: {
      ticket_category: "Ticket Category"
    },
    onChartClickPayload: params => {
      const { data, across, visualization } = params;
      const { activeLabel, activePayload } = data;
      let keyValue = activeLabel;
      if (visualization && visualization === ChartType.DONUT) {
        keyValue = get(data, ["tooltipPayload", 0, "name"], "_UNASSIGNED_");

        if (idFilters.includes(across) || across.includes(CUSTOM_FIELD_PREFIX)) {
          keyValue = {
            id: get(data, ["key"], "_UNASSIGNED_"),
            name: get(data, ["name"], "_UNASSIGNED_")
          };
        }
        return keyValue;
      } else {
        if (idFilters.includes(across) || across.includes(CUSTOM_FIELD_PREFIX)) {
          keyValue = {
            id: get(activePayload, [0, "payload", "key"], "_UNASSIGNED_"),
            name: get(activePayload, [0, "payload", "name"], "_UNASSIGNED_")
          };
        }
      }
      return keyValue;
    },
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraIssuesReportFiltersConfig,
    [MULTI_SERIES_REPORT_FILTERS_CONFIG]: JiraMultiSeriesIssuesReportFiltersConfig,
    getTotalLabel: getTotalLabelIssuesReport,
    [PREV_REPORT_TRANSFORMER]: data => transformIssuesReportPrevQuery(data),
    mapFiltersBeforeCall: mapFiltersBeforeCallIssueReport,
  },
  assignee_time_report: {
    name: "Issue Assignee Time Report",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.TABLE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    chart_props: {
      size: "small",
      columns: [
        {
          title: "Assignee",
          key: "assignee",
          dataIndex: "assignee",
          width: "25%"
        },
        {
          title: "Issue",
          key: "key",
          dataIndex: "key",
          width: "25%",
          render: (item, record, index) => {
            const url = `ticket_details`.concat(
              `?key=${record.key}&integration_id=${record.integration_id}`
            );
            return <JiraIssueLink link={url} ticketKey={item} integrationUrl={record?.integration_url} />;
          }
        },
        {
          title: "Summary",
          key: "summary",
          dataIndex: "summary",
          ellipsis: true,
          width: "25%"
        },
        {
          title: "Total time",
          key: "total_time",
          dataIndex: "total_time",
          width: "25%",
          render: (item, record, index) => moment.duration(item, "seconds").format()
        }
      ],
      chartProps: {}
    },
    uri: "assignee_time_report",
    method: "list",
    filters: {},
    hidden_filters: {
      exclude: {
        assignees: ["_UNASSIGNED_"],
        time_assignees: ["_UNASSIGNED_"]
      }
    },
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: jiraSupportedFilters,
    drilldown: {
      title: "Jira Assignee Time",
      uri: "assignee_time_report",
      application: "jira_assignee_time_report",
      columns: AssigneeReportTableConfig,
      columnsWithInfo: ["bounces", "hops", "response_time", "resolution_time"],
      supported_filters: jiraSupportedFilters,
      drilldownTransformFunction: data => jiraDrilldownTransformer(data)
    },
    transformFunction: data => tableTransformer(data),
    [FILTER_NAME_MAPPING]: jiraCommonFilterOptionsMapping,
    [FE_BASED_FILTERS]: {
      issue_resolved_at
    },
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraIssuesAssigneeTimeReportFiltersConfig
  },
  hygiene_report: {
    name: "Issue Hygiene Report",
    application: IntegrationTypes.JIRA,
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
    uri: "hygiene_report",
    method: "list",
    filters: {},
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    defaultAcross: "project",
    hygiene_uri: "tickets_report",
    hygiene_trend_uri: "tickets_report",
    hygiene_types: hygieneTypes,
    drilldown: {
      ...jiraDrilldown,
      drilldownTransformFunction: data => jiraHygieneDrilldownTranformer(data)
    },
    default_query: hygieneDefaultSettings,
    supported_filters: jiraSupportedFilters,
    // [PREVIEW_DISABLED]: true,
    [FILTER_NAME_MAPPING]: jiraCommonFilterOptionsMapping,
    [SHOW_SETTINGS_TAB]: true,
    [SHOW_METRICS_TAB]: false,
    [SHOW_AGGREGATIONS_TAB]: false,
    [WIDGET_VALIDATION_FUNCTION]: hygieneWeightValidationHelper,
    [FE_BASED_FILTERS]: {
      issue_resolved_at
    },
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraIssueHygieneReportFiltersConfig
  },
  bounce_report_trends: {
    name: "Issue Bounce Report Trends",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    chart_props: {
      unit: "Bounces",
      chartProps: chartProps
    },
    uri: "bounce_report",
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
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [SHOW_SETTINGS_TAB]: true,
    default_query: {
      [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.TIME_BASED]
    },
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: jiraSupportedFilters,
    drilldown: jiraDrilldown,
    transformFunction: data => trendReportTransformer(data),
    [FILTER_NAME_MAPPING]: jiraCommonFilterOptionsMapping,
    [FE_BASED_FILTERS]: {
      issue_resolved_at
    },
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraIssuesBounceReportTrendsFiltersConfig
  },
  bounce_counts_stat: {
    name: "Issue Bounce Single Stat",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "bounce_report",
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
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: jiraSupportedFilters,
    drilldown: jiraStatDrilldown,
    transformFunction: data => statReportTransformer(data),
    [FILTER_NAME_MAPPING]: jiraCommonFilterOptionsMapping,
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [FE_BASED_FILTERS]: {
      issue_resolved_at
    },
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraIssueBounceSingleStat
  },
  hops_report_trends: {
    name: "Issue Hops Report Trends",
    application: IntegrationTypes.JIRA,
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
    uri: "hops_report",
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
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: jiraSupportedFilters,
    drilldown: {
      ...jiraDrilldown,
      defaultSort: [{ id: "hops", desc: true }]
    },
    transformFunction: data => trendReportTransformer(data),
    [FILTER_NAME_MAPPING]: jiraCommonFilterOptionsMapping,
    [FE_BASED_FILTERS]: {
      issue_resolved_at
    },
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraHopsReportTrendsFiltersConfig
  },
  hops_counts_stat: {
    name: "Issue Hops Single Stat",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "hops_report",
    method: "list",
    filters: {
      across: "trend"
    },
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    xaxis: false,
    chart_props: {
      unit: "Hops"
    },
    default_query: statDefaultQuery,
    compareField: "median",
    supported_filters: jiraSupportedFilters,
    drilldown: {
      ...jiraStatDrilldown,
      defaultSort: [{ id: "hops", desc: true }]
    },
    transformFunction: data => statReportTransformer(data),
    [FILTER_NAME_MAPPING]: jiraCommonFilterOptionsMapping,
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [FE_BASED_FILTERS]: {
      issue_resolved_at
    },
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraHopsReportSingleStatFiltersConfig
  },
  response_time_report_trends: {
    name: "Issue Response Time Report Trends",
    application: IntegrationTypes.JIRA,
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
    uri: "response_time_report",
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
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: jiraSupportedFilters,
    convertTo: "days",
    drilldown: jiraResponseTimeTrendDrilldown,
    transformFunction: data => trendReportTransformer(data),
    [FILTER_NAME_MAPPING]: jiraCommonFilterOptionsMapping,
    [FE_BASED_FILTERS]: {
      issue_resolved_at
    },
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraResponseTimeReportTrendsFiltersConfig,
    [IMPLICITY_INCLUDE_DRILLDOWN_FILTER]: includeSolveTimeImplicitFilter
  },
  response_time_counts_stat: {
    name: "Issue Response Time Single Stat",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "response_time_report",
    method: "list",
    filters: {
      across: "trend"
    },
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    default_query: statDefaultQuery,
    xaxis: false,
    chart_props: {
      unit: "Days"
    },
    compareField: "median",
    supported_filters: jiraSupportedFilters,
    drilldown: jiraStatDrilldown,
    transformFunction: data => statReportTransformer(data),
    [FILTER_NAME_MAPPING]: jiraCommonFilterOptionsMapping,
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [SHOW_SETTINGS_TAB]: true,
    [SHOW_METRICS_TAB]: false,
    [FE_BASED_FILTERS]: {
      issue_resolved_at
    },
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraResponseTimeSingleStatFiltersConfig
  },
  resolution_time_report_trends: {
    name: "Issues Resolution Time Trend Report",
    application: IntegrationTypes.JIRA,
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
    uri: "resolution_time_report",
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
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: jiraSupportedFilters,
    convertTo: "days",
    drilldown: jiraDrilldown,
    transformFunction: data => trendReportTransformer(data),
    [FILTER_NAME_MAPPING]: jiraTimeAcrossFilterOptionsMapping,
    [FILTER_WITH_INFO_MAPPING]: [jiraExcludeStatusFilter],
    [FE_BASED_FILTERS]: {
      issue_resolved_at
    },
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraResolutionTimeTrendReportFiltersConfig,
    [IMPLICITY_INCLUDE_DRILLDOWN_FILTER]: includeSolveTimeImplicitFilter
  },
  resolution_time_counts_stat: {
    name: "Issue Resolution Time Single Stat",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "resolution_time_report",
    method: "list",
    filters: {},
    xaxis: false,
    chart_props: {
      unit: "Days"
    },
    defaultAcross: "issue_created",
    default_query: { agg_type: "average", issue_created_at: issueSingleDefualtCreatedAt },
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    compareField: "median",
    supported_filters: jiraSupportedFilters,
    drilldown: jiraStatDrilldown,
    transformFunction: data => jiraIssueResolutionTimeReportStatTransformer(data),
    [FILTER_NAME_MAPPING]: jiraCommonFilterOptionsMapping,
    [FILTER_WITH_INFO_MAPPING]: [jiraExcludeStatusFilter],
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [SHOW_SETTINGS_TAB]: true,
    [STAT_TIME_BASED_FILTER]: {
      options: [
        { value: "issue_created", label: "Issue Created" },
        { value: "issue_resolved", label: "Issue Resolved" },
        { value: "issue_updated", label: "Issue Updated" }
      ],
      getFilterLabel: data => {
        const { filters } = data;
        return filters.across ? `${filters.across.replaceAll("_", " ")} in` : "";
      },
      getFilterKey: data => {
        const { filters } = data;
        return filters.across ? `${filters.across}_at` : "";
      },
      defaultValue: "issue_created"
    },
    [PREV_REPORT_TRANSFORMER]: data => transformIssueResolutionTimeSingleStatReportPrevQuery(data),
    [DEFAULT_METADATA]: issueSingleStatDefaultMeta,
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraResolutionTimeSingleStatFiltersConfig,
    hasStatUnit: compareField => true
  },
  tickets_report_trends: {
    name: "Issues Trend Report",
    application: IntegrationTypes.JIRA,
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
    uri: "tickets_report",
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
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: jiraSupportedFilters,
    drilldown: jiraDrilldown,
    transformFunction: data => trendReportTransformer(data),
    [FILTER_NAME_MAPPING]: jiraCommonFilterOptionsMapping,
    [SHOW_SETTINGS_TAB]: true,
    [FE_BASED_FILTERS]: {
      issue_resolved_at
    },
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraIssuesTrendReportTrendsFiltersConfig
  },
  tickets_counts_stat: {
    name: "Issues Single Stat",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "tickets_report",
    method: "list",
    filters: {},
    xaxis: false,
    chart_props: {
      unit: "Tickets"
    },
    defaultAcross: "issue_created",
    default_query: {
      issue_created_at: issueSingleDefualtCreatedAt
    },
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    compareField: "total_tickets",
    supported_filters: jiraSupportedFilters,
    drilldown: jiraStatDrilldown,
    transformFunction: data => issuesSingleStatReportTransformer(data),
    [FILTER_NAME_MAPPING]: jiraCommonFilterOptionsMapping,
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [SHOW_SETTINGS_TAB]: true,
    [WIDGET_VALIDATION_FUNCTION]: issuesSingleStatValidationHelper,
    [PREV_REPORT_TRANSFORMER]: data => transformIssuesSingleStatReportPrevQuery(data),
    [DEFAULT_METADATA]: issueSingleStatDefaultMeta,
    [STAT_TIME_BASED_FILTER]: {
      options: [
        { value: "issue_created", label: "Issue Created" },
        { value: "issue_resolved", label: "Issue Resolved" },
        { value: "issue_due", label: "Issue Due" },
        { value: "issue_updated", label: "Issue Updated" }
      ],
      getFilterLabel: data => {
        const { filters } = data;
        return filters.across ? `${filters.across.replaceAll("_", " ")} in` : "";
      },
      getFilterKey: data => {
        const { filters } = data;
        return filters.across ? `${filters.across}_at` : "";
      },
      defaultValue: "issue_created"
    },
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraIssuesSingleStatFiltersConfig,
    mapFiltersForWidgetApi: mapFiltersBeforeCallIssueSingleStat,
  },
  hygiene_report_trends: {
    name: "Issue Hygiene Trend Report",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.HYGIENE_AREA_CHART,
    chart_container: ChartContainerType.HYGIENE_API_WRAPPER,
    xaxis: false,
    chart_props: {
      unit: "Score",
      chartProps: chartProps,
      areaProps: [],
      stackedArea: true
    },
    uri: "hygiene_report",
    method: "list",
    filters: defaultHygineTrendsFilters,
    default_query: {
      interval: "month",
      visualization: "stacked_area"
    },
    hygiene_uri: "jira_tickets",
    hygiene_trend_uri: "tickets_report",
    hygiene_types: hygieneTypes,
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [SHOW_SETTINGS_TAB]: true,
    [SHOW_METRICS_TAB]: false,
    [SHOW_AGGREGATIONS_TAB]: false,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    drilldown: jiraDrilldown,
    supported_filters: jiraSupportedFilters,
    [FILTER_NAME_MAPPING]: jiraCommonFilterOptionsMapping,
    [WIDGET_VALIDATION_FUNCTION]: hygieneWeightValidationHelper,
    [FE_BASED_FILTERS]: {
      issue_resolved_at
    },
    [COMPARE_X_AXIS_TIMESTAMP]: true,
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraIssueHygieneReportTrendsFiltersConfig,
    onChartClickPayload: params => {
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
    [INCLUDE_INTERVAL_IN_PAYLOAD]: true
  },
  jira_zendesk_report: {
    name: "Support Escalation Report",
    application: "jirazendesk",
    chart_type: ChartType?.SANKEY,
    chart_container: ChartContainerType.SANKEY_API_WRAPPER,
    xaxis: false,
    chart_props: {
      unit: "",
      chartProps: chartProps,
      areaProps: [],
      stackedArea: true
    },
    drilldown: {
      application: "jirazendesk",
      uriForNodeTypes: {
        [JiraZendeskNodeType.JIRA]: "jira_zendesk_aggs_list_jira",
        [JiraZendeskNodeType.ZENDESK]: "zendesk_tickets",
        [JiraZendeskNodeType.ZENDESK_LIST]: "jira_zendesk_aggs_list_zendesk",
        [JiraZendeskNodeType.COMMIT]: "jira_zendesk_aggs_list_commit"
      },
      columnsForNodeTypes: {
        [JiraZendeskNodeType.ZENDESK]: ZendeskJiraTableConfig.zendesk,
        [JiraZendeskNodeType.ZENDESK_LIST]: ZendeskJiraTableConfig.zendesk_list,
        [JiraZendeskNodeType.JIRA]: ZendeskJiraTableConfig.jira,
        [JiraZendeskNodeType.COMMIT]: ZendeskJiraTableConfig.commit
      },
      drilldownTransformFunction: data => jiraZendeskSalesforceDrilldownTransformer(data)
    },
    uri: "jira_zendesk",
    method: "list",
    blockTimeFilterTransformation: params => {
      const { timeFilterName } = params;
      if (["jira_issue_created_at", "start_time"].includes(timeFilterName)) {
        return true;
      }

      return false;
    },
    supported_filters: jiraZenDeskSupportedFilters,
    [CustomFieldMappingKey.CUSTOM_FIELD_MAPPING_KEY]: zendeskCustomFieldsMapping,
    [FILTER_NAME_MAPPING]: jiraCommonFilterOptionsMapping,
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: {
      jira_issue_created_at: "jirazendesk_issue_created_at"
    },
    [REPORT_FILTERS_CONFIG]: JiraZendeskSupportEscalationReportFiltersConfig,
    [SHOW_SETTINGS_TAB]: true
  },
  jira_salesforce_report: {
    name: "Support Escalation Report",
    application: "jirasalesforce",
    chart_type: ChartType?.SANKEY,
    chart_container: ChartContainerType.SANKEY_API_WRAPPER,
    xaxis: false,
    chart_props: {
      unit: "",
      chartProps: chartProps,
      areaProps: [],
      stackedArea: true
    },
    drilldown: {
      application: "jirasalesforce",
      uriForNodeTypes: {
        [JiraSalesforceNodeType.JIRA]: "jira_salesforce_aggs_list_jira",
        [JiraSalesforceNodeType.SALESFORCE]: "salesforce_tickets",
        [JiraSalesforceNodeType.SALESFORCE_LIST]: "jira_salesforce_aggs_list_salesforce",
        [JiraSalesforceNodeType.COMMIT]: "jira_salesforce_aggs_list_commit"
      },
      columnsForNodeTypes: {
        [JiraSalesforceNodeType.SALESFORCE]: SalesforceJiraTableConfig.salesforce,
        [JiraSalesforceNodeType.SALESFORCE_LIST]: SalesforceJiraTableConfig.salesforce_list,
        [JiraSalesforceNodeType.JIRA]: SalesforceJiraTableConfig.jira,
        [JiraSalesforceNodeType.COMMIT]: SalesforceJiraTableConfig.commit
      },
      drilldownTransformFunction: data => jiraZendeskSalesforceDrilldownTransformer(data)
    },
    uri: "jira_salesforce",
    method: "list",
    supported_filters: jiraSalesforceSupportedFilters,
    [FILTER_NAME_MAPPING]: jiraCommonFilterOptionsMapping,
    [SHOW_SETTINGS_TAB]: true,
    [HIDE_REPORT]: true,
    [REPORT_FILTERS_CONFIG]: JiraSalesforceSupportEscalationReportFiltersConfig
  },
  jira_tickets_count_by_first_assignee: {
    name: "Issues By First Assignee",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
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
      chartProps: chartProps
    },
    uri: "tickets_report",
    method: "list",
    filters: {
      across: "first_assignee"
    },
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [VALUE_SORT_KEY]: "ticket_count",
    [SHOW_SETTINGS_TAB]: true,
    default_query: {
      [WIDGET_DATA_SORT_FILTER_KEY]:
        widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED]
    },
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: jiraSupportedFilters,
    drilldown: jiraDrilldown,
    transformFunction: data => jiraTicketsReportChangeTransform(data),
    [FILTER_NAME_MAPPING]: jiraCommonFilterOptionsMapping,
    [FE_BASED_FILTERS]: {
      issue_resolved_at
    },
    xAxisLabelTransform: params => {
      const { across, item = {} } = params;
      const { key, additional_key } = item;
      let newLabel = key;
      if (["first_assignee"].includes(across)) {
        newLabel = additional_key;
      }
      if (!newLabel) {
        newLabel = "UNRESOLVED";
      }
      return newLabel;
    },
    onChartClickPayload: params => {
      const { data, across } = params;
      const { activeLabel, activePayload } = data;
      let keyValue = activeLabel;
      if (idFilters.includes(across)) {
        keyValue = {
          id: get(activePayload, [0, "payload", "key"], "_UNASSIGNED_"),
          name: get(activePayload, [0, "payload", "name"], "_UNASSIGNED_")
        };
      }
      return keyValue;
    },
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraIssuesByFirstAssigneeFiltersConfig
  },
  jira_backlog_trend_report: {
    name: "Issue Backlog Trend Report",
    application: IntegrationTypes.JIRA,
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
      // sortBy: "median",
      chartProps: chartProps
    },
    uri: "backlog_report",
    method: "list",
    filters: {
      across: "trend"
    },
    stack_filters: jiraSupportedFilters.values,
    default_query: jiraBacklogTrendDefaultQuery,
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: jiraSupportedFilters,
    drilldown: jiraBacklogDrillDown,
    // shouldReverseApiData: () => true,
    transformFunction: data => jiraBacklogTransformerWrapper(data),
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [SHOW_SETTINGS_TAB]: true,
    [FILTER_NAME_MAPPING]: jiraCommonFilterOptionsMapping,
    [STACKS_SHOW_TAB]: WIDGET_CONFIGURATION_KEYS.AGGREGATIONS,
    [SHOW_SETTINGS_TAB]: true,
    xAxisLabelTransform: params => getXAxisLabel(params),
    [FE_BASED_FILTERS]: {
      issue_resolved_at
    },
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [PREV_REPORT_TRANSFORMER]: data => transformIssueBacklogTrendReportPrevQuery(data),
    [REPORT_FILTERS_CONFIG]: JiraBacklogTrendReportFiltersConfig,
    [MULTI_SERIES_REPORT_FILTERS_CONFIG]: JiraMultiSeriesBacklogTrendReportFiltersConfig,
    onChartClickPayload: param => {
      const timeStamp = get(param, ["data", "activePayload", 0, "payload", "key"], undefined);
      const label = get(param, ["data", "activeLabel"], undefined);
      return { id: timeStamp, name: label };
    }
  },
  jira_time_across_stages: {
    name: "Issue Time Across Stages",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    defaultAcross: "none",
    default_query: jiraAcrossStagesDefaultQuery,
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
    uri: "jira_time_across_stages_report",
    method: "list",
    filters: {},
    dataKey: "median_time",
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: jiraSupportedFilters,
    drilldown: jiraIssueTimeAcrossStagesDrilldown,
    transformFunction: data => timeAcrossStagesDataTransformer(data),
    [FILTER_NAME_MAPPING]: jiraTimeAcrossFilterOptionsMapping,
    [FILTER_WITH_INFO_MAPPING]: [jiraHideStatusFilter],
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: {
      issue_resolved_at: "jira_issue_resolved_at"
    },
    [SHOW_SETTINGS_TAB]: true,
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    xAxisLabelTransform: params => {
      const { across, item = {}, CustomFieldType } = params;
      const { key, additional_key } = item;
      let newLabel = key;
      const isValidDate = isvalidTimeStamp(newLabel);
      if (idFilters.includes(across)) {
        newLabel = additional_key;
        return newLabel;
      }
      if (
        (CustomFieldType && CustomTimeBasedTypes.includes(CustomFieldType) && isValidDate) ||
        (isValidDate && !GROUP_BY_TIME_FILTERS.includes(across))
      ) {
        newLabel = moment(parseInt(key)).format("DD-MM-YYYY HH:mm:ss");
        return newLabel;
      }
      if (!newLabel) {
        newLabel = "UNRESOLVED";
      }
      return newLabel;
    },
    onChartClickPayload: onChartClickPayloadForId,
    [REPORT_FILTERS_CONFIG]: JiraTimeAcrossStagesReportFiltersConfig,
    [IMPLICITY_INCLUDE_DRILLDOWN_FILTER]: includeSolveTimeImplicitFilter
  },
  sprint_metrics_single_stat: {
    name: "Sprint Metrics Single Stat",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.SPRINT_API_WRAPPER,
    uri: "jira_sprint_report",
    method: "list",
    filters: {
      include_issue_keys: true
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
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
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
    supported_filters: jiraSupportedFilters,
    drilldown: {
      allowDrilldown: true,
      ...sprintMetricSingleStatDrilldown
    },
    [CSV_DRILLDOWN_TRANSFORMER]: sprintMetricStatCsvTransformer,
    [IGNORE_FILTER_KEYS_CONFIG]: jiraSprintIgnoreConfig,
    transformFunction: data => sprintStatReportTransformer(data),
    supported_widget_types: ["stats"],
    [SHOW_SETTINGS_TAB]: true,
    [DEFAULT_METADATA]: sprintDefaultMeta,
    [FE_BASED_FILTERS]: {
      issue_resolved_at
    },
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraSprintSingleStatReportFiltersConfig
  },
  sprint_metrics_percentage_trend: {
    name: "Sprint Metrics Percentage Trend Report",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.AREA,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "jira_sprint_report",
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
          transformer: data => data + " %"
        },
        {
          name: "Delivered to Commit",
          dataKey: "done_to_commit_ratio",
          unit: "%",
          transformer: data => data + " %"
        },
        {
          name: "Creep Done to Commit",
          dataKey: "creep_done_to_commit_ratio",
          unit: "%",
          transformer: data => data + " %"
        },
        {
          name: "Creep Done",
          dataKey: "creep_done_ratio",
          unit: "%",
          transformer: data => data + " %"
        },
        {
          name: "Creep Missed",
          dataKey: "creep_missed_ratio",
          unit: "%",
          transformer: data => data + " %"
        },
        {
          name: "Commit Missed",
          dataKey: "commit_missed_ratio",
          unit: "%",
          transformer: data => data + " %"
        },
        {
          name: "Commit Done",
          dataKey: "commit_done_ratio",
          unit: "%",
          transformer: data => data + " %"
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
    onChartClickPayload: ({ data }) => {
      const filter = { sprint_name: data.activeLabel, sprint_id: data.activePayload?.[0]?.payload?.sprint_id };
      return filter;
    },
    default_query: sprintMetricsPercentageReport,
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: jiraSupportedFilters,
    drilldown: sprintMetricTrendReportDrilldown,
    transformFunction: data => sprintMetricsPercentReportTransformer(data),
    [SHOW_SETTINGS_TAB]: true,
    [SHOW_FILTERS_TAB]: true,
    [SHOW_METRICS_TAB]: true,
    [SHOW_WEIGHTS_TAB]: false,
    [SHOW_AGGREGATIONS_TAB]: true,
    [DEFAULT_METADATA]: sprintDefaultMeta,
    [FE_BASED_FILTERS]: {
      issue_resolved_at
    },
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraSprintMetricPercentageTrendReportFiltersConfig
  },
  sprint_metrics_trend: {
    name: "Sprint Metrics Trend Report",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "jira_sprint_report",
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
      xAxisProps: {
        interval: "preserveStartEnd"
      },
      config: {
        showXAxisTooltip: true
      }
    },
    default_query: sprintMetricsTrendReport,
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    compareField: "delivered_story_points",
    supported_filters: jiraSupportedFilters,
    drilldown: sprintMetricTrendReportDrilldown,
    transformFunction: data => sprintMetricsTrendTransformer(data),
    [SHOW_FILTERS_TAB]: true,
    [SHOW_METRICS_TAB]: true,
    [SHOW_SETTINGS_TAB]: true,
    [SHOW_WEIGHTS_TAB]: false,
    [SHOW_AGGREGATIONS_TAB]: true,
    [SHOW_SETTINGS_TAB]: true,
    [BAR_CHART_REF_LINE_STROKE]: "#4f4f4f",
    [DEFAULT_METADATA]: sprintDefaultMeta,
    [FE_BASED_FILTERS]: {
      issue_resolved_at
    },
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraSprintMetricTrendReportFiltersConfig,
    [PREV_REPORT_TRANSFORMER]: data => transformSprintMetricsTrendReportPrevQuery(data)
  },
  sprint_impact_estimated_ticket_report: {
    name: "Sprint Impact of Unestimated Tickets Report",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "jira_sprint_report",
    method: "list",
    filters: {
      include_issue_keys: true
    },
    defaultAcross: "week",
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
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: jiraSupportedFilters,
    drilldown: {},
    transformFunction: data => sprintImpactTransformer(data),
    [SHOW_SETTINGS_TAB]: true,
    [SHOW_METRICS_TAB]: false,
    [SHOW_AGGREGATIONS_TAB]: false,
    [DEFAULT_METADATA]: sprintDefaultMeta,
    [FE_BASED_FILTERS]: {
      issue_resolved_at
    },
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [IS_FRONTEND_REPORT]: true,
    [REPORT_FILTERS_CONFIG]: JiraSprintImpactOfUnestimatedTicketReportFiltersConfig
  },
  lead_time_trend_report: {
    [REPORT_KEY_IS_ENABLED]: false,
    name: "Issue Lead Time Trend Report",
    application: IntegrationTypes.JIRA,
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
    default_query: jiraLeadTimeDefaultQuery,
    convertTo: "days",
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [FILTER_NAME_MAPPING]: issueLeadTimeFilterOptionsMapping,
    [FILTER_WITH_INFO_MAPPING]: [leadTimeExcludeStageFilter],
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [SHOW_SETTINGS_TAB]: true,
    [SHOW_METRICS_TAB]: true,
    [SHOW_AGGREGATIONS_TAB]: false,
    supported_filters: leadTimeJiraSupportedFilters,
    drilldown: jiraLeadTimeDrilldown,
    [CSV_DRILLDOWN_TRANSFORMER]: leadTimeCsvTransformer,
    transformFunction: data => leadTimeTrendTransformer(data),
    shouldJsonParseXAxis: () => true,
    [API_BASED_FILTER]: ["jira_reporters", "jira_assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [PREV_REPORT_TRANSFORMER]: data => transformLeadTimeReportPrevQuery(data),
    valuesToFilters: leadTimeValuesToFiltersKeys,
    [REPORT_FILTERS_CONFIG]: IssueLeadTimeTrendReportFiltersConfig
  },
  lead_time_by_stage_report: {
    name: "Issue Lead Time by Stage Report",
    application: IntegrationTypes.JIRA,
    description: LEAD_TIME_STAGE_REPORT_DESCRIPTION,
    chart_type: ChartType?.LEAD_TIME_PHASE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: {
      unit: "Days",
      chartProps: chartProps
    },
    dataKey: "duration",
    uri: "lead_time_report",
    method: "list",
    filters: {
      calculation: "ticket_velocity"
    },
    defaultAcross: "velocity",
    default_query: jiraLeadTimeDefaultQuery,
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    supported_filters: leadTimeJiraSupportedFilters,
    [FILTER_NAME_MAPPING]: issueLeadTimeFilterOptionsMapping,
    [FILTER_WITH_INFO_MAPPING]: [leadTimeExcludeStageFilter],
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [SHOW_SETTINGS_TAB]: true,
    [SHOW_METRICS_TAB]: true,
    [SHOW_AGGREGATIONS_TAB]: false,
    [PREVIEW_DISABLED]: true,
    drilldown: jiraLeadTimeDrilldown,
    [CSV_DRILLDOWN_TRANSFORMER]: leadTimeCsvTransformer,
    transformFunction: data => leadTimePhaseTransformer(data),
    [API_BASED_FILTER]: ["jira_reporters", "jira_assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [PREV_REPORT_TRANSFORMER]: data => transformLeadTimeReportPrevQuery(data),
    valuesToFilters: leadTimeValuesToFiltersKeys,
    [REPORT_FILTERS_CONFIG]: IssueLeadTimeByStageReportFiltersConfig
  },
  lead_time_by_type_report: {
    name: "Issue Lead Time by Type Report",
    application: IntegrationTypes.JIRA,
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
    default_query: jiraLeadTimeDefaultQuery,
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    supported_filters: leadTimeJiraSupportedFilters,
    [FILTER_NAME_MAPPING]: issueLeadTimeFilterOptionsMapping,
    [FILTER_WITH_INFO_MAPPING]: [leadTimeExcludeStageFilter],
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [SHOW_SETTINGS_TAB]: true,
    [SHOW_METRICS_TAB]: true,
    [SHOW_AGGREGATIONS_TAB]: false,
    [PREVIEW_DISABLED]: true,
    drilldown: jiraLeadTimeDrilldown,
    [CSV_DRILLDOWN_TRANSFORMER]: leadTimeCsvTransformer,
    transformFunction: data => leadTimeTypeTransformer(data),
    shouldJsonParseXAxis: () => true,
    [API_BASED_FILTER]: ["jira_reporters", "jira_assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [PREV_REPORT_TRANSFORMER]: data => transformLeadTimeReportPrevQuery(data),
    valuesToFilters: leadTimeValuesToFiltersKeys,
    [REPORT_FILTERS_CONFIG]: IssueLeadTimeByTypeReportFiltersConfig
  },
  sprint_goal: {
    name: "Sprint Goal Report",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.TABLE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    chart_props: {
      size: "small",
      columns: sprintGoalReportColumns,
      chartProps: {}
    },
    uri: "jira_sprint_filters",
    method: "list",
    filters: {},
    default_query: {
      completed_at: modificationMappedValues("last_month", completedDateOptions)
    },
    supportExcludeFilters: false,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: jiraSprintGoalSupportedFilters,
    drilldown: {
      allowDrilldown: true,
      title: "Sprint Goals",
      uri: "jira_sprint_filters",
      application: "sprint_goal",
      columns: sprintGoalReportColumns,
      supported_filters: jiraSprintGoalSupportedFilters,
      drilldownTransformFunction: data => genericDrilldownTransformer(data)
    },
    transformFunction: data => tableTransformer(data),
    [FILTER_NAME_MAPPING]: jiraCommonFilterOptionsMapping,
    [SHOW_METRICS_TAB]: false,
    [DEFAULT_METADATA]: sprintDefaultMeta,
    [FE_BASED_FILTERS]: {
      sprint_end_date
    },
    [FILTER_KEY_MAPPING]: {
      sprint: "name"
    },
    [REPORT_FILTERS_CONFIG]: JiraSprintGoalReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  sprint_distribution_retrospective_report: {
    name: "Sprint Distribution Retrospective Report",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "sprint_distribution_report",
    xaxis: false,
    method: "list",
    filters: {},
    show_max: false,
    [IGNORE_FILTER_KEYS_CONFIG]: jiraSprintIgnoreConfig,
    columnWithInformation: true,
    columnsWithInfo: {
      additional_key: "Sprint name",
      status: "Issue status at sprint close.",
      story_points: "Story points at sprint start and close."
    },
    chart_props: {
      useCustomToolTipHeader: (data, key) => {
        const res = data.find(val => {
          return val.name === key;
        });
        return toTitleCase(res?.title) || "";
      },
      hideTotalInTooltip: true,
      stackOffset: "sign",
      unit: "Work",
      barProps: [
        {
          dataKey: "planned",
          fill: "#4197FF",
          name: "Planned",
          unit: "Work"
        },
        {
          dataKey: "unplanned",
          fill: "#FF4D4F",
          name: "Unplanned",
          unit: "Work"
        }
      ],
      stacked: true,
      chartProps: {
        ...chartProps
      }
    },
    doneStatusFilter: {
      valueKey: "distribution_stages" // making object as in future we can add default, labal etc as required
    },
    default_query: defaultFilter,
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [ALLOW_KEY_FOR_STACKS]: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: jiraSupportedFilters,
    drilldown: jiraDrilldown,
    transformFunction: data => sprintMetricsDistributionTransformer(data),
    onChartClickPayload: ({ data }) => {
      return { across: data?.activeLabel, keys: data?.activePayload?.[0]?.payload?.delivered_keys };
    },
    [SHOW_FILTERS_TAB]: true,
    [SHOW_METRICS_TAB]: true,
    [SHOW_SETTINGS_TAB]: true,
    [SHOW_WEIGHTS_TAB]: false,
    [SHOW_AGGREGATIONS_TAB]: true,
    [BAR_CHART_REF_LINE_STROKE]: "#4f4f4f",
    [DEFAULT_METADATA]: sprintDefaultMeta,
    [FE_BASED_FILTERS]: {
      sprint_end_date: { ...sprint_end_date, required: true },
      aggMetric,
      percentile
    },
    [REPORT_FILTERS_CONFIG]: JiraSprintDistributionRetrospectiveReportFiltersConfig
  },
  stage_bounce_report: {
    name: "Stage Bounce Report",
    application: IntegrationTypes.JIRA,
    xaxis: true,
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
      stages: ["DONE"],
      metric: "mean"
    },
    uri: "jira_stage_bounce_report",
    method: "list",
    filters: {},
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    supported_filters: {
      ...jiraSupportedFilters,
      values: [...jiraSupportedFilters.values, "stage"]
    },
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
    [SHOW_SETTINGS_TAB]: true,
    [STACKS_SHOW_TAB]: WIDGET_CONFIGURATION_KEYS.AGGREGATIONS,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [FILTER_NAME_MAPPING]: jiraCommonFilterOptionsMapping,
    [WIDGET_VALIDATION_FUNCTION]: payload => {
      const { query = {} } = payload;
      let isValid = query?.stages && (query?.stages || []).length;
      if (isValid) {
        return true;
      } else if (!isValid && (query?.exclude?.stages || []).length) {
        return true;
      }
      return false;
    },
    getTotalKey: params => {
      const { metric } = params;
      return metric || "mean";
    },
    [STACKS_FILTER_STATUS]: filters => {
      return filters && filters.across && filters.across === "stage";
    },
    [INFO_MESSAGES]: {
      stacks_disabled: "Stacks option is not applicable when x-Axis value is Stage"
    },
    [FE_BASED_FILTERS]: {
      issue_resolved_at,
      stageBounceMetric
    },
    xAxisLabelTransform: params => getXAxisLabel(params),
    onChartClickPayload: params => {
      const { data, across, stage } = params;
      let value = data.activeLabel ?? data.name ?? "";
      if (GROUP_BY_TIME_FILTERS.includes(across)) {
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
    shouldJsonParseXAxis: () => true,
    [REPORT_FILTERS_CONFIG]: JiraStageBounceReportFiltersConfig
  },
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
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    xaxis: false,
    chart_props: {
      unit: "Times"
    },
    default_query: {
      ...statDefaultQuery,
      stages: ["DONE"],
      metric: "mean"
    },
    compareField: "mean",
    supported_filters: {
      ...jiraSupportedFilters,
      values: [...jiraSupportedFilters.values, "stage"]
    },
    drilldown: {},
    transformFunction: data => statReportTransformer(data),
    [FILTER_NAME_MAPPING]: jiraCommonFilterOptionsMapping,
    valuesToFilters: { stage: "stages" },
    requiredFilters: ["stage"],
    [WIDGET_VALIDATION_FUNCTION]: payload => {
      const { query = {} } = payload;
      let isValid = query?.stages && (query?.stages || []).length;
      if (isValid) {
        return true;
      } else if (!isValid && (query?.exclude?.stages || []).length) {
        return true;
      }
      return false;
    },
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [FE_BASED_FILTERS]: {
      issue_resolved_at,
      stageBounceMetric
    },
    [REPORT_FILTERS_CONFIG]: JiraStageBounceSingleStatReportFiltersConfig
  }
};
