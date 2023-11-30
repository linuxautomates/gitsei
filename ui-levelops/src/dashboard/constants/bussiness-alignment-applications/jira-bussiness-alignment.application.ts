import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { tableTransformer } from "custom-hooks/helpers";
import { ChartContainerType } from "dashboard/helpers/helper";
import { JiraIssueProgressReport } from "dashboard/report-filters/jira/issue-progress-report-filter.config";
import { baApiFilters, jiraApiBasedFilterKeyMapping } from "dashboard/reports/jira/commonJiraReports.constants";
import { get } from "lodash";
import moment from "moment";
import {
  jiraAlignmentReport,
  jiraBAProgressReport,
  jiraEffortInvestmentStat,
  jiraEffortInvestmentTrendReport
} from "reduxConfigs/actions/restapi";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { effortInvestmentTrendReportSampleInterval } from "../../graph-filters/components/Constants";
import { jiraBaValidationHelper } from "../../helpers/widgetValidation.helper";
import {
  ALLOW_CHART_TOOLTIP_LABEL_TRANSFORM,
  ALLOW_ZERO_LABELS,
  CHART_DATA_TRANSFORMERS,
  CHART_TOOLTIP_RENDER_TRANSFORM,
  CHART_X_AXIS_TITLE_TRANSFORMER,
  CHART_X_AXIS_TRUNCATE_TITLE,
  FE_BASED_FILTERS,
  HIDE_CUSTOM_FIELDS,
  PREV_REPORT_TRANSFORMER
} from "../applications/names";
import { effortInvestmentTrendChartTooltipTransformer } from "../chartTooltipTransform/effortInvestmentTrendChartTooltip.transformer";
import {
  effortInvestmentEngineerReportDrilldown,
  effortInvestmentTrendReportDrilldown,
  jiraProgressReportDrilldown,
  jiraStatDrilldown
} from "../drilldown.constants";
import { EffortType, EffortUnitType, IntervalType } from "../enums/jira-ba-reports.enum";
import { progressReportEIUnit } from "../FE-BASED/ba.FEbased";
import {
  API_BASED_FILTER,
  DEFAULT_METADATA,
  FIELD_KEY_FOR_FILTERS,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_MAPPING_KEY,
  SHOW_AGGREGATIONS_TAB,
  SHOW_METRICS_TAB,
  SHOW_SETTINGS_TAB
} from "../filter-key.mapping";
import { JIRA_PARTIAL_FILTER_KEY_MAPPING } from "dashboard/reports/jira/constant";
import { WIDGET_VALIDATION_FUNCTION } from "../filter-name.mapping";
import { EISupportedFilters } from "../supported-filters.constant";
import { effortInvestmentXAxisTitleTransformer } from "../xAxisTitleTransformers/EffortInvestmentTrend.xAxisTransformer";
import { REPORT_FILTERS_CONFIG } from "./../applications/names";
import {
  EIAlignmentReportCSVColumns,
  EIAlignmentReportCSVDataTransformer,
  EIAlignmentReportCSVFiltersTransformer,
  EIDynamicURITransformer,
  EIEngineerCSVDataTransformer,
  EIEngineerReportCSVColumns,
  EITrendReportCSVColumns,
  EITrendReportCSVDataTransformer
} from "./BACSVHelperTransformer";
import {
  ACTIVE_WORK_UNIT_FILTER_KEY,
  baInProgressStatusFilter,
  BA_EFFORT_ATTRIBUTION_BE_KEY,
  ba_issue_resolved_at,
  DefaultKeyTypes,
  DISABLE_CATEGORY_SELECTION,
  DISABLE_XAXIS,
  effortAttributionFilter,
  EffortAttributionOptions,
  effortInvestmentTimeRangeDefMeta,
  filterAssigneeByStatusFilter,
  IGNORE_SUPPORTED_FILTERS_KEYS,
  INTERVAL_OPTIONS,
  jiraProgressMaxRecordOptions,
  MAX_RECORDS_LABEL,
  MAX_RECORDS_OPTIONS_KEY,
  REPORT_CSV_DOWNLOAD_CONFIG,
  RequiredFiltersType,
  REQUIRED_FILTERS_MAPPING,
  SHOW_EFFORT_UNIT_INSIDE_TAB,
  SHOW_PROFILE_INSIDE_TAB,
  SHOW_SAMPLE_INTERVAL_INSIDE_TAB,
  statusOfTheCompletedIssues,
  STORE_ACTION,
  SUPPORT_ACTIVE_WORK_UNIT_FILTERS,
  SUPPORT_CATEGORY_EPIC_ACROSS_FILTER,
  SUPPORT_TICKET_CATEGORIZATION_FILTERS,
  SUPPORT_TICKET_CATEGORIZATION_UNIT_FILTERS,
  SUPPORT_TREND_INTERVAL,
  TICKET_CATEGORIZATION_SCHEMES_KEY,
  TICKET_CATEGORIZATION_UNIT_FILTER_KEY,
  TIME_RANGE_DISPLAY_FORMAT_CONFIG,
  URI_MAPPING,
  WIDGET_MIN_HEIGHT,
  SUB_COLUMNS_TITLE,
  EXCLUDE_SUB_COLUMNS_FOR
} from "./constants";
import {
  effortInvestmentTrendChartOnClicked,
  effortInvestmentEngineerBComTransformer,
  effortInvestmentSingleStatBComTransformer,
  effortInvestmentTrendBComTransformer,
  getConditionalUriForFilterPreview,
  mapFiltersForWidgetApiIssueProgressReport,
  mapFiltersForWidgetApiIssueProgressReportDrilldown,
  getDrilldownTitleEffortInvestmentTrendReport
} from "./helper";
import { EffortAlignmentReportFiltersConfig } from "dashboard/reports/jira/effort-alignment-report/filters.config";
import {
  CommitEffortInvestmentByEngineerFiltersConfig,
  EffortInvestmentByEngineerFiltersConfig
} from "dashboard/reports/jira/effort-investment-by-engineer/filters.config";
import {
  CommitEffortInvestmentSingleStatFiltersConfig,
  EffortInvestmentSingleStatFiltersConfig
} from "dashboard/reports/jira/effort-investment-single-stat/filters.config";
import {
  CommitEffortInvestmentTrendReportFiltersConfig,
  EffortInvestmentTrendReportFiltersConfig
} from "dashboard/reports/jira/effort-investment-trend-report/filters.config";
import { hasStoredEntitlement } from "custom-hooks/helpers/entitlements.helper";
import { Entitlement } from "custom-hooks/constants";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const jiraBussinessAlignmentDashboard = {
  // progress_single_stat: {
  //   name: "Jira Progress Single Stat",
  //   application: IntegrationTypes.JIRA,
  //   chart_type: ChartType?.GRAPH_STAT,
  //   chart_container: ChartContainerType.BA_WIDGET_API_WRAPPER,
  //   uri: "tickets_report",
  //   method: "list",
  //   filters: {
  //     across: "trend"
  //   },
  //   xaxis: false,
  //   supportExcludeFilters: true,
  //   supportPartialStringFilters: true,
  //   supported_filters: jiraSupportedFilters,
  //   drilldown: jiraStatDrilldown,
  //   [DefaultKeyTypes.DEFAULT_DISPLAY_FORMAT_KEY]: "fraction",
  //   [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  //   [STORE_ACTION]: jiraBAProgressStat,
  //   [SUPPORT_TICKET_CATEGORIZATION_FILTERS]: true,
  //   [SUPPORT_DISPLAY_FORMAT_FILTERS]: true,
  //   supported_widget_types: ["stats"],
  //   category: "effort_investment",
  //   transformFunction: (data: any) => tableTransformer(data)
  // },
  effort_investment_single_stat: {
    name: "Effort Investment Single Stat",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.EFFORT_INVESTMENT_STAT,
    chart_container: ChartContainerType.BA_WIDGET_API_WRAPPER,
    uri: "effort_investment_tickets",
    method: "list",
    filters: {},
    default_query: {
      issue_resolved_at: {
        // required filters and default is last month
        $gt: moment.utc().subtract(4, "months").startOf("day").unix().toString(),
        $lt: moment.utc().unix().toString()
      },
      [TICKET_CATEGORIZATION_UNIT_FILTER_KEY]: "tickets_report",
      [BA_EFFORT_ATTRIBUTION_BE_KEY]: EffortAttributionOptions.CURRENT_ASSIGNEE
    },
    xaxis: false,
    chart_props: {},
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    supported_filters: EISupportedFilters,
    drilldown: jiraStatDrilldown,
    supported_widget_types: ["stats"],
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [DEFAULT_METADATA]: effortInvestmentTimeRangeDefMeta,
    [SUPPORT_TICKET_CATEGORIZATION_UNIT_FILTERS]: true,
    [SUPPORT_TICKET_CATEGORIZATION_FILTERS]: true,
    [REQUIRED_FILTERS_MAPPING]: {
      [RequiredFiltersType.SCHEME_SELECTION]: true
    },
    [IGNORE_SUPPORTED_FILTERS_KEYS]: ["status"],
    [SHOW_EFFORT_UNIT_INSIDE_TAB]: WIDGET_CONFIGURATION_KEYS.SETTINGS,
    [SHOW_PROFILE_INSIDE_TAB]: WIDGET_CONFIGURATION_KEYS.SETTINGS,
    [FE_BASED_FILTERS]: {
      ba_issue_resolved_at,
      effortAttributionFilter,
      filterAssigneeByStatusFilter,
      baInProgressStatusFilter,
      statusOfTheCompletedIssues
    },
    [DISABLE_CATEGORY_SELECTION]: true,
    [SHOW_SETTINGS_TAB]: true,
    [DefaultKeyTypes.DEFAULT_DISPLAY_FORMAT_KEY]: "percentage",
    [DefaultKeyTypes.DEFAULT_EFFORT_UNIT]: "tickets_report",
    [DefaultKeyTypes.DEFAULT_SCHEME_KEY]: true, // if true then default scheme is selected by default
    [PREV_REPORT_TRANSFORMER]: effortInvestmentSingleStatBComTransformer,
    [STORE_ACTION]: jiraEffortInvestmentStat,
    transformFunction: (data: any) => tableTransformer(data),
    [API_BASED_FILTER]: baApiFilters,
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: (args: any) => {
      const { filters } = args;
      const uriUnit = get(filters, [TICKET_CATEGORIZATION_UNIT_FILTER_KEY], undefined);
      if ([EffortUnitType.AZURE_COMMIT_COUNT, EffortUnitType.COMMIT_COUNT].includes(uriUnit)) {
        return CommitEffortInvestmentSingleStatFiltersConfig;
      }
      return EffortInvestmentSingleStatFiltersConfig;
    },
    [WIDGET_VALIDATION_FUNCTION]: jiraBaValidationHelper,
    [HIDE_CUSTOM_FIELDS]: (args: any) => {
      const { filters } = args;
      const uriUnit = get(filters, [TICKET_CATEGORIZATION_UNIT_FILTER_KEY], undefined);
      if ([EffortUnitType.AZURE_COMMIT_COUNT, EffortUnitType.COMMIT_COUNT].includes(uriUnit)) {
        return true;
      }
      return false;
    },
    conditionalUriMethod: getConditionalUriForFilterPreview
  },
  progress_single_report: {
    name: "Issue Progress Report",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.JIRA_PROGRESS_CHART,
    chart_container: ChartContainerType.BA_WIDGET_API_WRAPPER,
    xaxis: true,
    defaultAcross: "epic",
    uri: "story_point_report",
    method: "list",
    filters: {},
    appendAcrossOptions: [
      { label: "Effort Investment Category", value: "ticket_category" },
      { label: "Epics", value: "epic" }
    ],
    category: "effort_investment",
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    supported_filters: EISupportedFilters,
    drilldown: jiraProgressReportDrilldown,
    [FE_BASED_FILTERS]: { progressReportEIUnit },
    show_max: true,
    [MAX_RECORDS_OPTIONS_KEY]: jiraProgressMaxRecordOptions,
    [MAX_RECORDS_LABEL]: "Max Records",
    [WIDGET_MIN_HEIGHT]: "350px",
    [STORE_ACTION]: jiraBAProgressReport,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [SUPPORT_CATEGORY_EPIC_ACROSS_FILTER]: true,
    [DefaultKeyTypes.DEFAULT_EFFORT_UNIT]: "story_point_report",
    transformFunction: (data: any) => tableTransformer(data),
    [URI_MAPPING]: {
      tickets_report: "tickets_report",
      story_point_report: "story_point_report"
    },
    [REPORT_FILTERS_CONFIG]: JiraIssueProgressReport,
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    mapFiltersForWidgetApi: mapFiltersForWidgetApiIssueProgressReport,
    mapFiltersBeforeCall: mapFiltersForWidgetApiIssueProgressReportDrilldown
  },
  effort_investment_trend_report: {
    name: "Effort Investment Trend Report",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.JIRA_EFFORT_ALLOCATION_CHART,
    chart_container: ChartContainerType.BA_WIDGET_API_WRAPPER,
    xaxis: true,
    uri: "effort_investment_tickets",
    method: "list",
    filters: {
      across: "issue_resolved_at"
    },
    [DEFAULT_METADATA]: effortInvestmentTimeRangeDefMeta,
    [FE_BASED_FILTERS]: {
      ba_issue_resolved_at,
      effortAttributionFilter,
      filterAssigneeByStatusFilter,
      baInProgressStatusFilter,
      statusOfTheCompletedIssues
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
      issue_resolved_at: {
        // required filters and default is last month
        $gt: moment.utc().subtract(4, "months").startOf("day").unix().toString(),
        $lt: moment.utc().unix().toString()
      },
      [BA_EFFORT_ATTRIBUTION_BE_KEY]: EffortAttributionOptions.CURRENT_ASSIGNEE,
      interval: IntervalType.BI_WEEK,
      [TICKET_CATEGORIZATION_UNIT_FILTER_KEY]: "tickets_report",
      [ACTIVE_WORK_UNIT_FILTER_KEY]: "active_effort_investment_tickets"
    },
    onChartClickPayload: effortInvestmentTrendChartOnClicked,
    [SUPPORT_ACTIVE_WORK_UNIT_FILTERS]: false,
    [PREV_REPORT_TRANSFORMER]: effortInvestmentTrendBComTransformer,
    [TIME_RANGE_DISPLAY_FORMAT_CONFIG]: {
      week: "DD MMM YYYY",
      biweekly: "DD MMM YYYY",
      month: "MMM YYYY",
      [IntervalType.QUARTER]: "DD MMM YYYY"
    },
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    supported_filters: EISupportedFilters,
    drilldown: effortInvestmentTrendReportDrilldown,
    shouldJsonParseXAxis: () => true,
    show_max: true,
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
    [SUPPORT_TICKET_CATEGORIZATION_UNIT_FILTERS]: true,
    [SUPPORT_TICKET_CATEGORIZATION_FILTERS]: true,
    [REQUIRED_FILTERS_MAPPING]: {
      [RequiredFiltersType.SCHEME_SELECTION]: true
    },
    requiredFilters: [TICKET_CATEGORIZATION_SCHEMES_KEY, "interval"],
    [DISABLE_CATEGORY_SELECTION]: true,
    [DISABLE_XAXIS]: true,
    getDrilldownTitle: getDrilldownTitleEffortInvestmentTrendReport,
    [DefaultKeyTypes.DEFAULT_SCHEME_KEY]: true,
    [DefaultKeyTypes.DEFAULT_EFFORT_UNIT]: "tickets_report",
    transformFunction: (data: any) => tableTransformer(data),
    [IGNORE_SUPPORTED_FILTERS_KEYS]: ["status"],
    // * REFER To WidgetChartDataTransformerType for typings and more info
    [CHART_DATA_TRANSFORMERS]: {
      [CHART_X_AXIS_TITLE_TRANSFORMER]: effortInvestmentXAxisTitleTransformer,
      [CHART_X_AXIS_TRUNCATE_TITLE]: false,
      [ALLOW_CHART_TOOLTIP_LABEL_TRANSFORM]: false,
      [ALLOW_ZERO_LABELS]: false,
      [CHART_TOOLTIP_RENDER_TRANSFORM]: effortInvestmentTrendChartTooltipTransformer
    },
    [REPORT_CSV_DOWNLOAD_CONFIG]: {
      widgetFiltersTransformer: EIAlignmentReportCSVFiltersTransformer,
      widgetDynamicURIGetFunc: EIDynamicURITransformer,
      widgetCSVColumnsGetFunc: EITrendReportCSVColumns,
      widgetCSVDataTransform: EITrendReportCSVDataTransformer
    },
    [API_BASED_FILTER]: baApiFilters,
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: (args: any) => {
      const { filters } = args;
      const uriUnit = get(filters, [TICKET_CATEGORIZATION_UNIT_FILTER_KEY], undefined);
      if ([EffortUnitType.AZURE_COMMIT_COUNT, EffortUnitType.COMMIT_COUNT].includes(uriUnit)) {
        return CommitEffortInvestmentTrendReportFiltersConfig;
      }
      return EffortInvestmentTrendReportFiltersConfig;
    },
    [WIDGET_VALIDATION_FUNCTION]: jiraBaValidationHelper,
    conditionalUriMethod: getConditionalUriForFilterPreview
  },
  jira_effort_investment_engineer_report: {
    name: "Effort Investment By Engineer",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.ENGINEER_TABLE,
    chart_container: ChartContainerType.BA_WIDGET_API_WRAPPER,
    xaxis: false,
    defaultAcross: "assignee",
    uri: "effort_investment_tickets",
    method: "list",
    filters: {},
    chart_props: {},
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    supported_filters: EISupportedFilters,
    requiredFilters: [TICKET_CATEGORIZATION_SCHEMES_KEY],
    [FE_BASED_FILTERS]: {
      ba_issue_resolved_at,
      effortAttributionFilter,
      filterAssigneeByStatusFilter,
      baInProgressStatusFilter,
      statusOfTheCompletedIssues
    },
    [SUPPORT_ACTIVE_WORK_UNIT_FILTERS]: true,
    default_query: {
      issue_resolved_at: {
        // required filters and default is last month
        $gt: moment.utc().subtract(4, "months").startOf("day").unix().toString(),
        $lt: moment.utc().unix().toString()
      },
      [BA_EFFORT_ATTRIBUTION_BE_KEY]: EffortAttributionOptions.CURRENT_ASSIGNEE,
      [TICKET_CATEGORIZATION_UNIT_FILTER_KEY]: "tickets_report",
      [ACTIVE_WORK_UNIT_FILTER_KEY]: "active_effort_investment_tickets"
    },
    [DEFAULT_METADATA]: {
      ...effortInvestmentTimeRangeDefMeta,
      effort_type: EffortType.COMPLETED_EFFORT
    },
    [WIDGET_MIN_HEIGHT]: "36rem",
    [SHOW_EFFORT_UNIT_INSIDE_TAB]: WIDGET_CONFIGURATION_KEYS.SETTINGS,
    [SHOW_PROFILE_INSIDE_TAB]: WIDGET_CONFIGURATION_KEYS.SETTINGS,
    [SHOW_SETTINGS_TAB]: true,
    [SHOW_METRICS_TAB]: false,
    [SHOW_AGGREGATIONS_TAB]: false,
    [SUPPORT_TICKET_CATEGORIZATION_UNIT_FILTERS]: true,
    [SUPPORT_TICKET_CATEGORIZATION_FILTERS]: true,
    [REQUIRED_FILTERS_MAPPING]: {
      [RequiredFiltersType.SCHEME_SELECTION]: true
    },
    [DISABLE_CATEGORY_SELECTION]: true,
    [DISABLE_XAXIS]: true,
    [DefaultKeyTypes.DEFAULT_SCHEME_KEY]: true,
    [DefaultKeyTypes.DEFAULT_EFFORT_UNIT]: "tickets_report",
    [IGNORE_SUPPORTED_FILTERS_KEYS]: ["status"],
    [PREV_REPORT_TRANSFORMER]: effortInvestmentEngineerBComTransformer,
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
    transformFunction: (data: any) => tableTransformer(data),
    [WIDGET_VALIDATION_FUNCTION]: jiraBaValidationHelper,
    [API_BASED_FILTER]: baApiFilters,
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: (args: any) => {
      const { filters } = args;
      const uriUnit = get(filters, [TICKET_CATEGORIZATION_UNIT_FILTER_KEY], undefined);
      if ([EffortUnitType.AZURE_COMMIT_COUNT, EffortUnitType.COMMIT_COUNT].includes(uriUnit)) {
        return CommitEffortInvestmentByEngineerFiltersConfig;
      }
      return EffortInvestmentByEngineerFiltersConfig;
    },
    drilldown: effortInvestmentEngineerReportDrilldown,
    onChartClickPayload: (params: any) => {
      const { record } = params;
      const current_allocation = get(record, "current_allocation", false);
      return { name: record?.engineer, id: record?.engineer, additional_data: { current_allocation } };
    },
    conditionalUriMethod: getConditionalUriForFilterPreview
  },
  jira_effort_alignment_report: {
    name: "Effort Alignment Report",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.ALIGNMENT_TABLE,
    chart_container: ChartContainerType.BA_WIDGET_API_WRAPPER,
    xaxis: true,
    defaultAcross: "ticket_category",
    uri: "active_effort_investment_tickets",
    method: "list",
    filters: {},
    chart_props: {},
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    supported_filters: EISupportedFilters,
    drilldown: {},
    [WIDGET_MIN_HEIGHT]: "28rem",
    requiredFilters: [TICKET_CATEGORIZATION_SCHEMES_KEY],
    [DEFAULT_METADATA]: effortInvestmentTimeRangeDefMeta,
    default_query: {
      issue_resolved_at: {
        // required filters and default is last 4 month
        $gt: moment.utc().subtract(4, "months").startOf("day").unix().toString(),
        $lt: moment.utc().unix().toString()
      },
      [ACTIVE_WORK_UNIT_FILTER_KEY]: "active_effort_investment_tickets"
    },
    [FE_BASED_FILTERS]: { ba_issue_resolved_at },
    [SUPPORT_ACTIVE_WORK_UNIT_FILTERS]: true,
    [SHOW_PROFILE_INSIDE_TAB]: WIDGET_CONFIGURATION_KEYS.SETTINGS,
    [SHOW_SETTINGS_TAB]: true,
    [SHOW_METRICS_TAB]: false,
    [SHOW_AGGREGATIONS_TAB]: false,
    [STORE_ACTION]: jiraAlignmentReport,
    [SUPPORT_TICKET_CATEGORIZATION_FILTERS]: true,
    [REQUIRED_FILTERS_MAPPING]: {
      [RequiredFiltersType.SCHEME_SELECTION]: true
    },
    [DISABLE_CATEGORY_SELECTION]: true,
    [DISABLE_XAXIS]: true,
    [DefaultKeyTypes.DEFAULT_SCHEME_KEY]: true,
    [DefaultKeyTypes.DEFAULT_EFFORT_UNIT]: "tickets_report",
    [PREV_REPORT_TRANSFORMER]: effortInvestmentEngineerBComTransformer,
    [IGNORE_SUPPORTED_FILTERS_KEYS]: ["status"],
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
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: EffortAlignmentReportFiltersConfig
  }
  // epic_priority_trend_report: {
  //   name: "Epic Priority Trend Report",
  //   application: IntegrationTypes.JIRA,
  //   chart_type: ChartType?.JIRA_PRIORITY_CHART,
  //   chart_container: ChartContainerType.BA_WIDGET_API_WRAPPER,
  //   xaxis: true,
  //   uri: "jira_tickets",
  //   method: "list",
  //   filters: {
  //     sort: [{ id: "priority", desc: true }]
  //   },
  //   defaultAcross: "epic",
  //   default_query: {
  //     [BA_TIME_RANGE_FILTER_KEY]: modificationMappedValues("last_4_week", jiraEpicPriorityTimeRangeFilterOption)
  //   },
  //   supportExcludeFilters: true,
  //   supportPartialStringFilters: true,
  //   supported_filters: jiraSupportedFilters,
  //   drilldown: epicPriorityReportDrilldown,
  //   show_max: true,
  //   category: "effort_investment",
  //   [MAX_RECORDS_OPTIONS_KEY]: jiraProgressMaxRecordOptions,
  //   [MAX_RECORDS_LABEL]: "Max Records",
  //   [WIDGET_MIN_HEIGHT]: "25rem",
  //   [STORE_ACTION]: jiraEpicPriorityReport,
  //   [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  //   [SUPPORT_TIME_RANGE_FILTER]: true,
  //
  //   [DISABLE_XAXIS]: true,
  //   [BA_WIDGET_TIME_RANGE_FILTER_CONFIG]: {
  //     options: jiraEpicPriorityTimeRangeFilterOption,
  //     filterKey: BA_TIME_RANGE_FILTER_KEY,
  //     label: "Epic Priority Time Range"
  //   },
  //   transformFunction: (data: any) => tableTransformer(data)
  // },
  // jira_burndown_report: {
  //   name: "Jira Burn Down Report",
  //   application: IntegrationTypes.JIRA,
  //   chart_type: ChartType.JIRA_BURNDOWN_CHART,
  //   chart_container: ChartContainerType.BA_WIDGET_API_WRAPPER,
  //   xaxis: true,
  //   uri: "tickets_report",
  //   method: "list",
  //   filters: {},
  //   defaultAcross: "epic",
  //   default_query: {
  //     [BA_TIME_RANGE_FILTER_KEY]: modificationMappedValues("last_4_week", effortInvestmentTeamTimeRangeFilterOption)
  //   },
  //   appendAcrossOptions: [
  //     { label: "Effort Investment Category", value: "ticket_category" },
  //     { label: "Epics", value: "epic" }
  //   ],
  //   supportExcludeFilters: true,
  //   supportPartialStringFilters: true,
  //   supported_filters: jiraSupportedFilters,
  //   drilldown: jiraBurndownReportDrilldown,
  //   [WIDGET_MIN_HEIGHT]: "36rem",
  //   show_max: false,
  //   category: "effort_investment",
  //   [STORE_ACTION]: jiraBurnDownReport,
  //   [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  //
  //   [SUPPORT_CATEGORY_EPIC_ACROSS_FILTER]: true,
  //   [SUPPORT_TIME_RANGE_FILTER]: true,
  //   [BA_WIDGET_TIME_RANGE_FILTER_CONFIG]: {
  //     options: effortInvestmentTeamTimeRangeFilterOption,
  //     filterKey: BA_TIME_RANGE_FILTER_KEY,
  //     label: "Burndown Time Range"
  //   },
  //   transformFunction: (data: any) => tableTransformer(data)
  // },
  // effort_investment_team_report: {
  //   name: "Effort Investment by Team Report",
  //   application: IntegrationTypes.JIRA,
  //   chart_type: ChartType.EFFORT_INVESTMENT_TEAM_CHART,
  //   chart_container: ChartContainerType.BA_WIDGET_API_WRAPPER,
  //   xaxis: false,
  //   uri: "tickets_report",
  //   method: "list",
  //   filters: {},
  //   default_query: {
  //     [BA_TIME_RANGE_FILTER_KEY]: modificationMappedValues("last_4_month", effortInvestmentTeamTimeRangeFilterOption)
  //   },
  //   defaultAcross: "trend",
  //   supportExcludeFilters: true,
  //   supportPartialStringFilters: true,
  //   supported_filters: jiraSupportedFilters,
  //   drilldown: jiraDrilldown,
  //   [WIDGET_MIN_HEIGHT]: "38.5rem",
  //   category: "effort_investment",
  //   [STORE_ACTION]: effortInvestmentTeamReport,
  //   [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  //
  //   [SUPPORT_TICKET_CATEGORIZATION_UNIT_FILTERS]: true,
  //   [SUPPORT_TIME_RANGE_FILTER]: true,
  //   [DISABLE_XAXIS]: true,
  //   [DefaultKeyTypes.DEFAULT_EFFORT_UNIT]: "tickets_report",
  //   [DefaultKeyTypes.DEFAULT_SCHEME_KEY]: true,
  //   [SUPPORT_TICKET_CATEGORIZATION_FILTERS]: true,
  //   [DISABLE_CATEGORY_SELECTION]: true,
  //   [REQUIRED_FILTERS_MAPPING]: {
  //     [RequiredFiltersType.SCHEME_SELECTION]: true
  //   },
  //   [BA_WIDGET_TIME_RANGE_FILTER_CONFIG]: {
  //     options: effortInvestmentTeamTimeRangeFilterOption,
  //     filterKey: BA_TIME_RANGE_FILTER_KEY,
  //     label: "Effort Investment Time Range"
  //   },
  //   transformFunction: (data: any) => tableTransformer(data)
  // }
};
