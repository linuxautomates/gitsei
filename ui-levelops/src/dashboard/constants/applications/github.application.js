import { scmaResolutionTimeDataTransformer, SCMReportsTransformer } from "custom-hooks/helpers/seriesData.helper";
import { genericDrilldownTransformer, githubFilesDrilldownTransformer } from "dashboard/helpers/drilldown-transformers";
import {
  GithubCommitsTableConfig,
  scmCommittersTableConfig,
  SCMFilesTableConfig,
  SCMJiraFilesTableConfig,
  scmReposTableConfig
} from "dashboard/pages/dashboard-tickets/configs";
import { scmFileTypeConfig } from "dashboard/pages/dashboard-tickets/configs/githubTableConfig";
import { get } from "lodash";
import { WIDGET_CONFIGURATION_KEYS } from "../../../constants/widgets";
import {
  leadTimePhaseTransformer,
  leadTimeTrendTransformer,
  scmFilesTransform,
  scmIssueFirstResponseReport,
  statReportTransformer,
  tableTransformer,
  trendReportTransformer,
  SCMReviewCollaborationTransformer
} from "../../../custom-hooks/helpers";
import {
  scmCommitsReportTransformer,
  scmCommitsStatReportTransformer
} from "../../../custom-hooks/helpers/scm-commits.helper";
import { SCMPRReportsTransformer, scmReworkReportTransformer } from "../../../custom-hooks/helpers/seriesData.helper";
import { ChartType } from "../../../shared-resources/containers/chart-container/ChartType";
import {
  ChartContainerType,
  transformAzureLeadTimeStageReportPrevQuery,
  transformSCMPrevQuery
} from "../../helpers/helper";
import { githubCommitsChartTooltipTransformer } from "../chartTooltipTransform/github-commitsChartTooltipTransformer";
import {
  githubCommitsStatDrilldown,
  githubIssuesDrilldown,
  githubIssuesStatDrilldown,
  githubPRSDrilldown,
  githubPRSFirstReviewTrendsDrilldown,
  githubPRSFirstReviewToMergeTrendsDrilldown,
  githubPRSStatDrilldown,
  scmIssueTimeAcrossStagesDrilldown,
  scmLeadTimeDrilldown,
  scmResolutionDrillDown,
  githubReviewCollaborationDrilldown,
  githubPRSMergeTrendDrilldown,
  githubIssuesFirstResponseReportTrendDrilldown
} from "../drilldown.constants";
import { scmCollabRadioBasedFilter, scmCommitsReportFEBased } from "../FE-BASED/github-commit.FEbased";
import { scmCodingDaysReportFEBased } from "../FE-BASED/github-coding.FEbased";
import { getXAxisLabel } from "shared-resources/charts/bar-chart/bar-chart.helper";
import { FilesReportFiltersConfig } from "dashboard/reports/scm/files-report/filter.config";

import {
  ALLOW_KEY_FOR_STACKS,
  API_BASED_FILTER,
  CSV_DRILLDOWN_TRANSFORMER,
  DISABLE_EXCLUDE_FILTER_MAPPING_KEY,
  DISABLE_PARTIAL_FILTER_MAPPING_KEY,
  FIELD_KEY_FOR_FILTERS,
  FILTERS_NOT_SUPPORTING_PARTIAL_FILTERS,
  IS_FRONTEND_REPORT,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  OU_EXCLUSION_CONFIG,
  PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_MAPPING_KEY,
  scmPartialFilterKeyMapping,
  SHOW_AGGREGATIONS_TAB,
  SHOW_METRICS_TAB,
  SHOW_SETTINGS_TAB,
  STACKS_SHOW_TAB
} from "../filter-key.mapping";
import {
  FILTER_NAME_MAPPING,
  GET_WIDGET_CHART_PROPS,
  jiraCommonFilterOptionsMapping,
  scmCommonFilterOptionsMapping,
  scmPrsFilterOptionsMapping,
  WIDGET_VALIDATION_FUNCTION,
  VALUE_SORT_KEY,
  ALLOWED_WIDGET_DATA_SORTING
} from "../filter-name.mapping";
import {
  FILTER_WITH_INFO_MAPPING,
  leadTimeExcludeStageFilter,
  sccIssueExcludeStatusFilter,
  scmExcludeStatusFilter
} from "../filterWithInfo.mapping";
import { leadTimeStatDefaultQuery, statDefaultQuery, WIDGET_MIN_HEIGHT, xAxisLabelTransform } from "../helper";
import {
  githubCommitsSupportedFilters,
  githubFilesSupportedFilters,
  githubIssuesSupportedFilters,
  githubJiraFilesSupportedFilters,
  githubPRsSupportedFilters,
  leadTimeCicdSupportedFilters,
  leadTimeSupportedFilters,
  scmIssuesTimeAcrossStagesSupportedFilters
} from "../supported-filters.constant";
import { IssueVisualizationTypes, ScmCommitsMetricTypes } from "../typeConstants";
import {
  CHART_DATA_TRANSFORMERS,
  CHART_TOOLTIP_RENDER_TRANSFORM,
  FE_BASED_FILTERS,
  PREVIEW_DISABLED,
  REPORT_FILTERS_CONFIG,
  TIME_FILTER_RANGE_CHOICE_MAPPER,
  GET_GRAPH_FILTERS,
  HIDE_CUSTOM_FIELDS,
  PREV_REPORT_TRANSFORMER,
  GET_VELOCITY_CONFIG
} from "./names";
import {
  scmIssueTrendTransformer,
  scmReworkDrilldownTransformer,
  scmDrilldownTranformerForIncludesFilter
} from "../../helpers/drilldown-transformers/githubDrilldownTransformer";
import { scmResolutionTimeTooltipMapping } from "dashboard/graph-filters/components/Constants";
import moment from "moment";
import { SCM_PRS_TIME_FILTERS_KEYS } from "constants/filters";
import { convertEpochToDate, DEFAULT_DATE_FORMAT } from "utils/dateUtils";
import { leadTimeCsvTransformer } from "../../helpers/csv-transformers/leadTimeCsvTransformer";
import { resolutionTimeGetGraphFilters } from "dashboard/reports/scm/issue-resolution-time/getGraphFilters";
import { scmCodingDaysDefaultQuery } from "../helper";
import { REVERSE_SCM_COMMON_FILTER_KEY_MAPPING } from "dashboard/reports/scm/constant";
import { GITHUB_ISSUES_FIRST_RESPONSE_REPORT_TRENDS_KEY_MAPPING, githubAppendAcrossOptions } from "./constant";

import { PrsSingleStatFiltersConfig } from "../../reports/scm/prs-single-stat/filter.config";
import { CommitsSingleStatFiltersConfig } from "../../reports/scm/commits-single-stat/filter.config";
import { PrsMergeTrendsFiltersConfig } from "../../reports/scm/prs-merge-trend/filter.config";
import { PrsFirstReviewTrendsFiltersConfig } from "../../reports/scm/prs-first-review-trend/filter.config";
import { PrsFirstReviewToMergeTrendsFiltersConfig } from "../../reports/scm/prs-first-review-to-merge/filter.config";
import { IssuesReportFiltersConfig } from "../../reports/scm/issues-report/filter.config";
import { IssuesReportTrendsFiltersConfig } from "../../reports/scm/issues-report-trends/filter.config";
import { IssuesCountSingleStatFiltersConfig } from "../../reports/scm/issues-count-single-stat/filter.config";
import { IssuesFirstResponseReportFiltersConfig } from "../../reports/scm/issues-first-response/filter.config";
import { IssuesFirstResponseSingleStatFiltersConfig } from "../../reports/scm/issues-first-response-single-stat/filter.config";
import { PrsMergeTrendSingleStatFiltersConfig } from "../../reports/scm/prs-merge-trend-single-stat/filter.config";
import { ReposReportFiltersConfig } from "../../reports/scm/repos/filter.config";
import { CommittersReportFiltersConfig } from "../../reports/scm/committers/filter.config";
import { IssuesResolutionTimeReportFiltersConfig } from "../../reports/scm/issue-resolution-time/filter.config";
import { PrLeadTimeTrendReportFiltersConfig } from "../../reports/scm/pr-lead-time-trend/filter.config";
import { PrLeadTimeByStageReportFiltersConfig } from "../../reports/scm/pr-lead-time-by-stage/filter.config";
import { CodingDaysReportFiltersConfig } from "../../reports/scm/coding-days/filter.config";
import { CodingDaysSingleStatReportFiltersConfig } from "../../reports/scm/coding-days-single-stat/filter.config";
import { PrsResponseTimeReportFiltersConfig } from "../../reports/scm/pr-response-time/filter.config";
import { CommitsReportFiltersConfig } from "../../reports/scm/commits/filter.config";
import { PrsReportFiltersConfig } from "../../reports/scm/pr-report/filter.config";
import { PrsFirstReviewToMergeTrendSingleStatFiltersConfig } from "../../reports/scm/pr-first-review-to-merge-trend-single-stat/filter.config";
import { PrsFirstReviewTrendSingleStatFiltersConfig } from "../../reports/scm/pr-first-review-trend-single-stat/filter.config";
import { show_value_on_bar } from "./constant";
import { getGraphFilters } from "custom-hooks/helpers/scm-prs.helper";
import { ScmJiraFilesFiltersConfig } from "dashboard/reports/scm/scm-jira-files-report/filter.config";
import { GithubFileTypeReportFiltersConfig } from "dashboard/reports/scm/scm-file-types-report/filter.config";
import { IssuesFirstResponseTrendReportFiltersConfig } from "dashboard/reports/scm/issues-first-response-trend/filters.config";
import { PrsResponseTimeSingleStatFiltersConfig } from "dashboard/reports/scm/prs-response-time-single-stat/filters.config";
import { SCMReviewCollabReportFiltersConfig } from "dashboard/reports/scm/scm-review-collaboration/filters.config";
import { SCMReworkReportFiltersConfig } from "dashboard/reports/scm/rework-report/filters.config";
import { SCMIssuesTimeAcrossStagesFiltersConfig } from "dashboard/reports/scm/issues-time-across-stages/filters.config";
import { SCM_REVIEW_COLLABORATION_DESCRIPTION } from "dashboard/reports/scm/scm-review-collaboration/constant";
import { SCM_CODING_DAYS_DESCRIPTION } from "dashboard/reports/scm/coding-days/constant";
import { SCM_REWORK_DESCRIPTION } from "dashboard/reports/scm/rework-report/constant";
import { SCM_COMMITS_BY_REPO_DESCRIPTION } from "dashboard/reports/scm/commits/constant";
import { SCM_PRS_REPORT_DESCRIPTION, SCM_PRS_STACK_FILTER } from "dashboard/reports/scm/pr-report/constant";
import { SCM_FILES_REPORT_DESCRIPTION } from "dashboard/reports/scm/files-report/constants";
import LeadTimeByStageFooter from "shared-resources/containers/server-paginated-table/components/Table-Footer/Lead-time-stage-footer";
import { formatTooltipValue } from "shared-resources/charts/helper";
import { getDrilldownCheckBox } from "./helper";
import { SCM_PRS_RESPONSE_TIME_STACK_FILTER } from "dashboard/reports/scm/pr-response-time/constant";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const scmTimeAcrossFilterOptionsMapping = {
  version: "Affects Version",
  status: "Current Status",
  current_column: "Current Status"
};
export const githubresolutionTimeDefaultQuery = {
  metric: ["median_resolution_time", "number_of_tickets_closed"]
};

export const githubTimeAcrossStagesDefaultQuery = {
  metric: "median_time"
};

const scmLeadTimeDefaultQuery = {
  limit_to_only_applicable_data: false
};

const scmLeadTimeStageDefaultQuery = {
  limit_to_only_applicable_data: false,
  ratings: ["good", "slow", "needs_attention"]
};

const chartProps = {
  barGap: 0,
  margin: { top: 20, right: 5, left: 5, bottom: 50 }
};

export const scmPrCreatedAt = {
  key: "pr_created_at",
  label: "PR CREATED IN",
  dataKey: "pr_created_at",
  dataValueType: "string"
};

const githubTimeFilter = {
  key: "time_range",
  label: "TIME",
  dataKey: "time_range",
  dataValueType: "string"
};

const scmResolutionTimeFilters = {
  key: "issue_closed_at",
  label: "Issue Closed Date",
  dataKey: "issue_closed_at",
  dataValueType: "string"
};

const scmIssueTimeAcrossStagesTimeFilters = [
  {
    key: "issue_closed_at",
    label: "Issue Closed In",
    dataKey: "issue_closed_at",
    dataValueType: "string"
  },
  { key: "issue_created_at", label: "Issue Created In", dataKey: "issue_created_at", dataValueType: "string" }
];

const githubPrCreatedAt = {
  key: "pr_created_at",
  label: "PR CREATED IN",
  dataKey: "pr_created_at",
  dataValueType: "string"
};

const githubPrClosedAt = {
  key: "pr_closed_at",
  label: "PR CLOSED TIME",
  dataKey: "pr_closed_at",
  dataValueType: "string"
};

const githubCommittedAt = {
  key: "committed_at",
  label: "COMMITTED IN",
  dataKey: "committed_at",
  dataValueType: "string"
};

const githubCreatedAt = {
  key: "issue_created_at",
  label: "Issue Created in",
  dataKey: "issue_created_at",
  dataValueType: "string"
};
const scmCommitsReportQuery = {
  visualization: IssueVisualizationTypes.PIE_CHART,
  code_change_size_unit: "lines"
};

export const scmLeadTimeFieldKeyMap = {
  creators: "creator",
  committers: "committer",
  authors: "author",
  assignees: "assignee",
  reviewers: "reviewer"
};

const githubPrMergedAt = {
  key: "pr_merged_at",
  label: "PR MERGED AT",
  dataKey: "pr_merged_at",
  dataValueType: "string"
};

const codingDaysApiBasedFilterKeyMapping = {
  authors: "author",
  committers: "committer"
};

export const GithubDashboards = {
  github_prs_report: {
    name: "SCM PRs Report",
    application: IntegrationTypes.GITHUB,
    description: SCM_PRS_REPORT_DESCRIPTION,
    chart_type: ChartType?.CIRCLE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    defaultAcross: "repo_id",
    appendAcrossOptions: githubAppendAcrossOptions,
    chart_props: {
      unit: "Count",
      barProps: [
        {
          name: "count",
          dataKey: "count",
          unit: "count"
        }
      ],
      stacked: false,
      chartProps: chartProps
    },
    uri: "github_prs_report",
    method: "list",
    filters: {},
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    weekStartsOnMonday: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: scmPartialFilterKeyMapping,
    [FILTER_NAME_MAPPING]: scmPrsFilterOptionsMapping,
    [DISABLE_PARTIAL_FILTER_MAPPING_KEY]: ["label"],
    supported_filters: githubPRsSupportedFilters,
    widgetSettingsTimeRangeFilterSchema: [githubPrCreatedAt, githubPrClosedAt],
    drilldown: githubPRSDrilldown,
    xAxisLabelTransform: xAxisLabelTransform,
    onChartClickPayload: params => {
      const { data, across } = params;
      if (params?.chart_type === ChartType.CIRCLE) {
        if (across && SCM_PRS_TIME_FILTERS_KEYS.includes(across)) {
          const key = data?.payload?.name;
          return key;
        } else {
          const name = data?.name ?? data?.additional_key ?? data.key;
          return { name: name, id: data.key };
        }
      }

      const newData = params?.data?.activePayload?.[0]?.payload;
      if (across && SCM_PRS_TIME_FILTERS_KEYS.includes(across)) {
        return newData?.name;
      }
      const name = newData?.name ?? newData?.key;
      const id = newData?.key;
      return { name, id };
    },
    transformFunction: data => SCMPRReportsTransformer(data),
    shouldSliceFromEnd: () => true,
    widgetSettingsTimeRangeFilterSchema: [githubPrCreatedAt, githubPrClosedAt, githubPrMergedAt],
    [REPORT_FILTERS_CONFIG]: PrsReportFiltersConfig,
    [API_BASED_FILTER]: ["committers", "authors", "creators"],
    [FIELD_KEY_FOR_FILTERS]: scmLeadTimeFieldKeyMap,
    [HIDE_CUSTOM_FIELDS]: true,
    stack_filters: SCM_PRS_STACK_FILTER,
    [ALLOW_KEY_FOR_STACKS]: true
  },
  github_commits_report: {
    name: "SCM Commits Report",
    application: IntegrationTypes.GITHUB,
    description: SCM_COMMITS_BY_REPO_DESCRIPTION,
    chart_type: ChartType?.CIRCLE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    defaultAcross: "repo_id",
    chart_props: {
      unit: "Counts",
      barProps: [
        {
          name: "count",
          dataKey: "count",
          unit: "count"
        }
      ],
      stacked: false,
      stackedArea: false,
      chartProps: chartProps
    },
    uri: "github_commits_report",
    method: "list",
    filters: {},
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: scmPartialFilterKeyMapping,
    [DISABLE_PARTIAL_FILTER_MAPPING_KEY]: ["file_type"],
    [FILTER_NAME_MAPPING]: scmCommonFilterOptionsMapping,
    supported_filters: {
      ...githubCommitsSupportedFilters,
      values: [...githubCommitsSupportedFilters.values, "file_type"]
    },
    widgetSettingsTimeRangeFilterSchema: [githubCommittedAt],
    drilldown: {
      title: "Github Commits Tickets",
      uri: "github_commits_tickets",
      application: "github_commits",
      columns: GithubCommitsTableConfig,
      supported_filters: {
        ...githubCommitsSupportedFilters,
        values: [...githubCommitsSupportedFilters.values, "file_type"]
      },
      drilldownTransformFunction: data => scmDrilldownTranformerForIncludesFilter(data)
    },
    onChartClickPayload: params => {
      const { data, across } = params;
      if (params?.chart_type === ChartType.CIRCLE) {
        if (across === "trend") {
          return get(data, ["payload", "name"]);
        }
        const name = data?.name ?? data.key;
        return { name: name, id: data.key };
      }
      if (across === "trend") {
        return get(data, ["activeLabel"]);
      }
      const newData = params?.data?.activePayload?.[0]?.payload;
      const name = newData?.name ?? newData?.key;
      const id = newData?.key;
      return { name, id };
    },
    transformFunction: data => scmCommitsReportTransformer(data),
    xAxisLabelTransform: xAxisLabelTransform,
    [SHOW_METRICS_TAB]: false,
    default_query: scmCommitsReportQuery,
    [FE_BASED_FILTERS]: scmCommitsReportFEBased,
    [GET_WIDGET_CHART_PROPS]: data => {
      const { filters = {} } = data;
      const metric = get(filters, ["filter", "metric"], [ScmCommitsMetricTypes.NO_OF_COMMITS]);
      const visualization = get(filters, ["filter", "visualization"], IssueVisualizationTypes.AREA_CHART);
      const barProps = metric.map(met => ({
        name: met,
        dataKey: met,
        unit: met
      }));
      if (visualization === IssueVisualizationTypes.STACKED_AREA_CHART) {
        return {
          barProps,
          stackedArea: true
        };
      } else if (visualization === IssueVisualizationTypes.STACKED_BAR_CHART) {
        return {
          barProps,
          stacked: true
        };
      }
      return {
        barProps
      };
    },
    [CHART_DATA_TRANSFORMERS]: {
      [CHART_TOOLTIP_RENDER_TRANSFORM]: githubCommitsChartTooltipTransformer
    },
    valuesToFilters: {
      code_change: "code_change_sizes"
    },
    [API_BASED_FILTER]: ["creators", "committers", "authors", "assignees", "reviewers"],
    [FIELD_KEY_FOR_FILTERS]: REVERSE_SCM_COMMON_FILTER_KEY_MAPPING,
    [REPORT_FILTERS_CONFIG]: CommitsReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true,
    [PREV_REPORT_TRANSFORMER]: transformSCMPrevQuery,
    weekStartsOnMonday: true
  },
  github_prs_single_stat: {
    name: "SCM PRs Single Stat",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    show_max: false,
    xaxis: true,
    across: ["pr_created", "pr_updated", "pr_merged", "pr_reviewed"],
    defaultAcross: "pr_created",
    chart_props: {
      unit: "PRs"
    },
    uri: "github_prs_report",
    method: "list",
    filters: {},
    default_query: statDefaultQuery,
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: scmPartialFilterKeyMapping,
    [FILTER_NAME_MAPPING]: scmPrsFilterOptionsMapping,
    compareField: "count",
    supported_filters: githubPRsSupportedFilters,
    widgetSettingsTimeRangeFilterSchema: [githubPrCreatedAt, githubPrClosedAt],
    drilldown: githubPRSStatDrilldown,
    transformFunction: data => statReportTransformer(data),
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [REPORT_FILTERS_CONFIG]: PrsSingleStatFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  github_commits_single_stat: {
    name: "SCM Commits Single Stat",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    show_max: false,
    xaxis: false,
    chart_props: {
      unit: "Commits"
    },
    uri: "github_commits_report",
    method: "list",
    filters: {
      across: "trend"
    },
    [SHOW_METRICS_TAB]: false,
    default_query: { ...statDefaultQuery, ...scmCommitsReportQuery },
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: scmPartialFilterKeyMapping,
    [FILTER_NAME_MAPPING]: scmCommonFilterOptionsMapping,
    compareField: "count",
    supported_filters: githubCommitsSupportedFilters,
    widgetSettingsTimeRangeFilterSchema: [githubCommittedAt],
    drilldown: githubCommitsStatDrilldown,
    transformFunction: data => scmCommitsStatReportTransformer(data),
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [REPORT_FILTERS_CONFIG]: CommitsSingleStatFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  github_prs_report_trends: {
    name: "SCM PRs Report Trends",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    transform: "count",
    composite: true,
    across: ["pr_created", "pr_updated", "pr_merged", "pr_reviewed"],
    defaultAcross: "pr_created",
    chart_props: {
      unit: "Count",
      chartProps: chartProps
    },
    uri: "github_prs_report",
    method: "list",
    filters: {},
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: scmPartialFilterKeyMapping,
    [FILTER_NAME_MAPPING]: scmPrsFilterOptionsMapping,
    [DISABLE_PARTIAL_FILTER_MAPPING_KEY]: ["label"],
    supported_filters: githubPRsSupportedFilters,
    widgetSettingsTimeRangeFilterSchema: [githubPrCreatedAt, githubPrClosedAt],
    drilldown: githubPRSDrilldown,
    transformFunction: data => trendReportTransformer(data),
    deprecated: true
  },
  github_prs_merge_trends: {
    name: "SCM PRs Merge Trends",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.AREA,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    transform: "median",
    composite: true,
    composite_transform: {
      pr_created: "merge_prs_created",
      pr_updated: "merge_prs_updated",
      pr_merged: "merge_prs_merged"
    },
    across: ["pr_created", "pr_updated", "pr_merged"],
    defaultAcross: "pr_created",
    chart_props: {
      areaProps: [
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
      unit: "Hours",
      //sortBy: "median",
      chartProps: chartProps
    },
    uri: "scm_prs_merge_trend",
    method: "list",
    filters: {},
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    [FILTERS_NOT_SUPPORTING_PARTIAL_FILTERS]: ["labels"],
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: scmPartialFilterKeyMapping,
    [FILTER_NAME_MAPPING]: scmPrsFilterOptionsMapping,
    [DISABLE_PARTIAL_FILTER_MAPPING_KEY]: ["label"],
    supported_filters: githubPRsSupportedFilters,
    widgetSettingsTimeRangeFilterSchema: [githubPrCreatedAt, githubPrClosedAt],
    convertTo: "hours",
    drilldown: githubPRSMergeTrendDrilldown,
    transformFunction: trendReportTransformer,
    [REPORT_FILTERS_CONFIG]: PrsMergeTrendsFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  github_prs_first_review_trends: {
    name: "SCM PRs First Review Trends",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.AREA,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    [FILTERS_NOT_SUPPORTING_PARTIAL_FILTERS]: ["labels"],
    xaxis: true,
    transform: "median",
    composite: true,
    composite_transform: {
      pr_created: "first_review_prs_created",
      pr_updated: "first_review_prs_updated",
      pr_merged: "first_review_prs_merged"
    },
    across: ["pr_created", "pr_updated", "pr_merged", "pr_closed"],
    defaultAcross: "pr_created",
    chart_props: {
      areaProps: [
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
      unit: "Hours",
      //sortBy: "median",
      chartProps: chartProps
    },
    uri: "scm_prs_first_review_trend",
    method: "list",
    filters: {},
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: scmPartialFilterKeyMapping,
    [FILTER_NAME_MAPPING]: scmPrsFilterOptionsMapping,
    [DISABLE_PARTIAL_FILTER_MAPPING_KEY]: ["label"],
    supported_filters: githubPRsSupportedFilters,
    widgetSettingsTimeRangeFilterSchema: [githubPrCreatedAt, githubPrClosedAt],
    convertTo: "hours",
    transformFunction: data => trendReportTransformer(data),
    drilldown: githubPRSFirstReviewTrendsDrilldown,
    [REPORT_FILTERS_CONFIG]: PrsFirstReviewTrendsFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  github_prs_first_review_to_merge_trends: {
    name: "SCM PRs First Review To Merge Trends",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.AREA,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    transform: "median",
    composite: true,
    [FILTERS_NOT_SUPPORTING_PARTIAL_FILTERS]: ["labels"],
    composite_transform: {
      pr_created: "first_review_to_merge_prs_created",
      pr_updated: "first_review_to_merge_prs_updated",
      pr_merged: "first_review_to_merge_prs_merged"
    },
    across: ["pr_created", "pr_updated", "pr_merged", "pr_closed"],
    defaultAcross: "pr_created",
    chart_props: {
      areaProps: [
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
      unit: "Hours",
      //sortBy: "median",
      chartProps: chartProps
    },
    uri: "scm_prs_first_review_to_merge_trend",
    method: "list",
    filters: {},
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: scmPartialFilterKeyMapping,
    [FILTER_NAME_MAPPING]: scmPrsFilterOptionsMapping,
    [DISABLE_PARTIAL_FILTER_MAPPING_KEY]: ["label"],
    supported_filters: githubPRsSupportedFilters,
    widgetSettingsTimeRangeFilterSchema: [githubPrCreatedAt, githubPrClosedAt],
    convertTo: "hours",
    transformFunction: data => trendReportTransformer(data),
    drilldown: githubPRSFirstReviewToMergeTrendsDrilldown,
    [REPORT_FILTERS_CONFIG]: PrsFirstReviewToMergeTrendsFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  scm_files_report: {
    name: "SCM Files Report",
    application: IntegrationTypes.GITHUB,
    description: SCM_FILES_REPORT_DESCRIPTION,
    chart_type: ChartType?.GRID_VIEW,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    uri: "scm_files_report",
    rootFolderURI: "scm_files_root_folder_report",
    method: "list",
    filters: {
      across: "repo_id"
    },
    defaultFilters: {
      module: ""
    },
    sort_options: ["num_commits", "changes", "deletions", "additions"],
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: scmPartialFilterKeyMapping,
    [FILTER_NAME_MAPPING]: scmCommonFilterOptionsMapping,
    supported_filters: githubFilesSupportedFilters,
    widgetSettingsTimeRangeFilterSchema: [githubCommittedAt],
    drilldown: {
      title: "Github Files",
      uri: "scm_files_report",
      application: "github_files",
      columns: SCMFilesTableConfig,
      supported_filters: githubFilesSupportedFilters,
      drilldownTransformFunction: data => githubFilesDrilldownTransformer(data)
    },
    transformFunction: data => scmFilesTransform(data),
    [REPORT_FILTERS_CONFIG]: FilesReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  github_issues_report: {
    name: "SCM Issues Report",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    defaultAcross: "repo_id",
    chart_props: {
      unit: "Counts",
      barProps: [
        {
          name: "count",
          dataKey: "count",
          unit: "count"
        }
      ],
      stacked: false,
      chartProps: chartProps
    },
    uri: "scm_issues_report",
    method: "list",
    filters: {},
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    [FILTERS_NOT_SUPPORTING_PARTIAL_FILTERS]: ["labels", "repo_ids", "assignees"],
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: scmPartialFilterKeyMapping,
    [FILTER_NAME_MAPPING]: scmCommonFilterOptionsMapping,
    [DISABLE_PARTIAL_FILTER_MAPPING_KEY]: ["label", "repo_id", "assignee"],
    supported_filters: githubIssuesSupportedFilters,
    widgetSettingsTimeRangeFilterSchema: [githubCreatedAt],
    drilldown: githubIssuesDrilldown,
    transformFunction: data => SCMReportsTransformer(data),
    [API_BASED_FILTER]: ["creators", "committers", "authors", "assignees", "reviewers"],
    [FIELD_KEY_FOR_FILTERS]: scmLeadTimeFieldKeyMap,
    [REPORT_FILTERS_CONFIG]: IssuesReportFiltersConfig,
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    },
    [HIDE_CUSTOM_FIELDS]: true,
    onChartClickPayload: params => {
      const { data, across } = params;
      const _data = data?.activePayload?.[0]?.payload || {};
      return {
        name: data.activeLabel || "",
        id: _data.key || data.activeLabel
      };
    }
  },
  github_issues_report_trends: {
    name: "SCM Issues Report Trends",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    [FILTERS_NOT_SUPPORTING_PARTIAL_FILTERS]: ["labels"],
    transform: "count",
    composite: true,
    across: ["issue_created", "issue_updated", "issue_closed", "first_comment"],
    defaultAcross: "issue_created",
    chart_props: {
      unit: "Count",
      chartProps: chartProps
    },
    uri: "scm_issues_report",
    method: "list",
    filters: {},
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: scmPartialFilterKeyMapping,
    [FILTER_NAME_MAPPING]: scmCommonFilterOptionsMapping,
    [DISABLE_PARTIAL_FILTER_MAPPING_KEY]: ["label"],
    supported_filters: githubIssuesSupportedFilters,
    widgetSettingsTimeRangeFilterSchema: [githubCreatedAt],
    drilldown: {
      ...githubIssuesDrilldown,
      drilldownTransformFunction: data => scmIssueTrendTransformer(data)
    },
    transformFunction: data => trendReportTransformer(data),
    [API_BASED_FILTER]: ["creators", "committers", "authors", "assignees", "reviewers"],
    [FIELD_KEY_FOR_FILTERS]: scmLeadTimeFieldKeyMap,
    onChartClickPayload: params => {
      const { data, across } = params;
      const _data = data?.activePayload?.[0]?.payload || {};
      return {
        name: data.activeLabel || "",
        id: _data.key || data.activeLabel
      };
    },
    [REPORT_FILTERS_CONFIG]: IssuesReportTrendsFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  github_issues_count_single_stat: {
    name: "SCM Issues Count Single Stat",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "scm_issues_report",
    method: "list",
    xaxis: true,
    across: ["issue_created", "issue_updated", "issue_closed", "first_comment"],
    defaultAcross: "issue_created",
    filters: {},
    chart_props: {
      unit: "issues"
    },
    default_query: statDefaultQuery,
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: scmPartialFilterKeyMapping,
    compareField: "count",
    supported_filters: githubIssuesSupportedFilters,
    widgetSettingsTimeRangeFilterSchema: [githubCreatedAt],
    drilldown: githubIssuesStatDrilldown,
    transformFunction: data => statReportTransformer(data),
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [API_BASED_FILTER]: ["creators", "committers", "authors", "assignees", "reviewers"],
    [FIELD_KEY_FOR_FILTERS]: scmLeadTimeFieldKeyMap,
    [REPORT_FILTERS_CONFIG]: IssuesCountSingleStatFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  github_issues_first_reponse_report: {
    name: "SCM Issues First Response Report",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    defaultAcross: "repo_id",
    [FILTERS_NOT_SUPPORTING_PARTIAL_FILTERS]: ["labels"],
    chart_props: {
      unit: "Days",
      barProps: [
        {
          name: "count",
          dataKey: "count",
          unit: "count"
        }
      ],
      stacked: false,
      chartProps: chartProps
    },
    uri: "scm_issues_first_response_report",
    method: "list",
    filters: {},
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: scmPartialFilterKeyMapping,
    [FILTER_NAME_MAPPING]: scmCommonFilterOptionsMapping,
    [DISABLE_PARTIAL_FILTER_MAPPING_KEY]: ["label"],
    supported_filters: githubIssuesSupportedFilters,
    widgetSettingsTimeRangeFilterSchema: [githubCreatedAt],
    drilldown: githubIssuesDrilldown,
    transformFunction: data => scmIssueFirstResponseReport(data),
    [API_BASED_FILTER]: ["creators", "committers", "authors", "assignees", "reviewers"],
    [FIELD_KEY_FOR_FILTERS]: scmLeadTimeFieldKeyMap,
    onChartClickPayload: params => {
      const { data, across } = params;
      const _data = data?.activePayload?.[0]?.payload || {};
      if (["creator"].includes(across)) {
        return {
          name: data.activeLabel || "",
          id: _data.key || data.activeLabel
        };
      }
      if (["label"].includes(across)) {
        return _data.key || data.activeLabel;
      }
      return data.activeLabel || "";
    },
    [REPORT_FILTERS_CONFIG]: IssuesFirstResponseReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  github_issues_first_response_report_trends: {
    name: "SCM Issues First Response Report Trends",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    transform: "median",
    composite: false,
    across: ["issue_created", "issue_updated", "issue_closed", "first_comment"],
    defaultAcross: "issue_created",
    chart_props: {
      unit: "Days",
      chartProps: chartProps
    },
    [FILTERS_NOT_SUPPORTING_PARTIAL_FILTERS]: ["labels"],
    uri: "scm_issues_first_response_report",
    method: "list",
    filters: {},
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: scmPartialFilterKeyMapping,
    [FILTER_NAME_MAPPING]: scmCommonFilterOptionsMapping,
    [DISABLE_PARTIAL_FILTER_MAPPING_KEY]: ["label"],
    supported_filters: githubIssuesSupportedFilters,
    widgetSettingsTimeRangeFilterSchema: [githubCreatedAt],
    convertTo: "days",
    drilldown: githubIssuesFirstResponseReportTrendDrilldown,
    transformFunction: data => trendReportTransformer(data),
    [IS_FRONTEND_REPORT]: true,
    [API_BASED_FILTER]: ["creators"],
    [FIELD_KEY_FOR_FILTERS]: GITHUB_ISSUES_FIRST_RESPONSE_REPORT_TRENDS_KEY_MAPPING,
    [REPORT_FILTERS_CONFIG]: IssuesFirstResponseTrendReportFiltersConfig
  },
  github_issues_first_response_count_single_stat: {
    name: "SCM Issues First Response Single Stat",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "scm_issues_first_response_report",
    method: "list",
    xaxis: true,
    across: ["issue_created", "issue_updated", "issue_closed", "first_comment"],
    defaultAcross: "issue_created",
    filters: {},
    chart_props: {
      unit: "hours"
    },
    default_query: statDefaultQuery,
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: scmPartialFilterKeyMapping,
    compareField: "sum",
    supported_filters: githubIssuesSupportedFilters,
    widgetSettingsTimeRangeFilterSchema: [githubCreatedAt],
    drilldown: githubIssuesStatDrilldown,
    transformFunction: data => statReportTransformer(data),
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [API_BASED_FILTER]: ["creators", "committers", "authors", "assignees", "reviewers"],
    [FIELD_KEY_FOR_FILTERS]: scmLeadTimeFieldKeyMap,
    [REPORT_FILTERS_CONFIG]: IssuesFirstResponseSingleStatFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  github_prs_merge_single_stat: {
    name: "SCM PRs Merge Trend Single Stat",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    show_max: false,
    xaxis: true,
    across: ["pr_created", "pr_updated", "pr_merged"],
    defaultAcross: "pr_created",
    chart_props: {},
    uri: "scm_prs_merge_trend",
    method: "list",
    filters: {},
    default_query: statDefaultQuery,
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: scmPartialFilterKeyMapping,
    [FILTER_NAME_MAPPING]: scmPrsFilterOptionsMapping,
    compareField: "sum",
    supported_filters: githubPRsSupportedFilters,
    widgetSettingsTimeRangeFilterSchema: [githubPrCreatedAt, githubPrClosedAt],
    drilldown: githubPRSStatDrilldown,
    transformFunction: data => statReportTransformer(data),
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [REPORT_FILTERS_CONFIG]: PrsMergeTrendSingleStatFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  github_prs_first_review_single_stat: {
    name: "SCM PRs First Review Trend Single Stat",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    show_max: false,
    xaxis: true,
    across: ["pr_created", "pr_updated", "pr_merged"],
    defaultAcross: "pr_created",
    chart_props: {},
    uri: "scm_prs_first_review_trend",
    method: "list",
    filters: {},
    default_query: statDefaultQuery,
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: scmPartialFilterKeyMapping,
    [FILTER_NAME_MAPPING]: scmPrsFilterOptionsMapping,
    compareField: "sum",
    supported_filters: githubPRsSupportedFilters,
    widgetSettingsTimeRangeFilterSchema: [githubPrCreatedAt, githubPrClosedAt],
    drilldown: githubPRSStatDrilldown,
    transformFunction: data => statReportTransformer(data),
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [REPORT_FILTERS_CONFIG]: PrsFirstReviewTrendSingleStatFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  github_prs_first_review_to_merge_single_stat: {
    name: "SCM PRs First Review To Merge Trend Single Stat",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    show_max: false,
    xaxis: true,
    across: ["pr_created", "pr_updated", "pr_merged"],
    defaultAcross: "pr_created",
    chart_props: {},
    uri: "scm_prs_first_review_to_merge_trend",
    method: "list",
    filters: {},
    default_query: statDefaultQuery,
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: scmPartialFilterKeyMapping,
    [FILTER_NAME_MAPPING]: scmPrsFilterOptionsMapping,
    compareField: "sum",
    supported_filters: githubPRsSupportedFilters,
    widgetSettingsTimeRangeFilterSchema: [githubPrCreatedAt, githubPrClosedAt],
    drilldown: githubPRSStatDrilldown,
    transformFunction: data => statReportTransformer(data),
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [REPORT_FILTERS_CONFIG]: PrsFirstReviewToMergeTrendSingleStatFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  scm_jira_files_report: {
    name: "Issue Hotspots Report",
    application: IntegrationTypes.GITHUBJIRA,
    chart_type: ChartType?.GRID_VIEW,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    uri: "scm_jira_files_report",
    rootFolderURI: "scm_jira_files_root_folder_report",
    method: "list",
    filters: {},
    defaultFilters: {
      scm_module: ""
    },
    supported_filters: githubJiraFilesSupportedFilters,
    supportExcludeFilters: true,
    [FILTER_NAME_MAPPING]: scmCommonFilterOptionsMapping,
    drilldown: {
      title: "Github/Jira Files",
      uri: "scm_jira_files_report",
      application: "github_jira_files",
      columns: SCMJiraFilesTableConfig,
      supported_filters: githubJiraFilesSupportedFilters,
      drilldownTransformFunction: data => githubFilesDrilldownTransformer(data)
    },
    transformFunction: data => scmFilesTransform(data),
    chart_click_enable: true,
    [REPORT_FILTERS_CONFIG]: ScmJiraFilesFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  scm_repos_report: {
    name: "SCM Repos Report",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.TABLE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    [FILTERS_NOT_SUPPORTING_PARTIAL_FILTERS]: ["repo_ids", "projects"],
    chart_props: {
      size: "small",
      columns: [
        {
          title: "Repo",
          key: "name",
          dataIndex: "name",
          width: "10%",
          ellipsis: true
        },
        {
          title: "No. of Line Changed",
          key: "num_changes",
          dataIndex: "num_changes",
          width: "10%",
          ellipsis: true
        },
        {
          title: "Number of Commits",
          key: "num_commits",
          dataIndex: "num_commits",
          width: "10%",
          ellipsis: true,
          sorter: true
        },
        {
          title: "Number of PRs",
          key: "num_prs",
          dataIndex: "num_prs",
          width: "10%",
          ellipsis: true,
          sorter: true
        },
        {
          title: "Number of Issues",
          key: "num_jira_issues",
          dataIndex: "num_jira_issues",
          width: "10%",
          ellipsis: true,
          sorter: true
        }
      ],
      chartProps: {}
    },
    uri: "scm_repos",
    method: "list",
    filters: {
      page_size: 10
    },
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: scmPartialFilterKeyMapping,
    [FILTER_NAME_MAPPING]: scmCommonFilterOptionsMapping,
    [DISABLE_PARTIAL_FILTER_MAPPING_KEY]: ["repo_id", "project"],
    supported_filters: githubCommitsSupportedFilters,
    widgetSettingsTimeRangeFilterSchema: [githubTimeFilter],
    drilldown: {
      title: "Github or Bitbucket Repos",
      uri: "scm_repos",
      application: "scm_repos",
      columns: scmReposTableConfig,
      supported_filters: githubCommitsSupportedFilters,
      drilldownTransformFunction: data => genericDrilldownTransformer(data)
    },
    transformFunction: data => tableTransformer(data),
    [SHOW_METRICS_TAB]: true,
    chart_click_enable: true,
    [API_BASED_FILTER]: ["committers", "authors"],
    [FIELD_KEY_FOR_FILTERS]: scmLeadTimeFieldKeyMap,
    [REPORT_FILTERS_CONFIG]: ReposReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  scm_committers_report: {
    name: "SCM Committers Report",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.TABLE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    [FILTERS_NOT_SUPPORTING_PARTIAL_FILTERS]: ["repo_ids", "projects"],
    xaxis: false,
    chart_props: {
      size: "small",
      columns: [
        {
          title: "Committer",
          key: "name",
          dataIndex: "name",
          width: "10%",
          ellipsis: true
        },
        {
          title: "No. of Changes",
          key: "num_changes",
          dataIndex: "num_changes",
          width: "10%",
          ellipsis: true
        },
        {
          title: "No. of Commits",
          key: "num_commits",
          dataIndex: "num_commits",
          width: "10%",
          ellipsis: true,
          sorter: true
        },
        {
          title: "No. of PRs",
          key: "num_prs",
          dataIndex: "num_prs",
          width: "10%",
          ellipsis: true,
          sorter: true
        },
        {
          title: "No. of Issues",
          key: "num_jira_issues",
          dataIndex: "num_jira_issues",
          width: "10%",
          ellipsis: true,
          sorter: true
        },
        {
          title: "Tech Breadth",
          key: "tech_breadth",
          dataIndex: "tech_breadth",
          width: "10%",
          ellipsis: true,
          sorter: true
        },
        {
          title: "Repo Breadth",
          key: "repo_breadth",
          dataIndex: "repo_breadth",
          width: "10%",
          ellipsis: true,
          sorter: true
        },
        {
          title: "File Extensions",
          key: "file_types",
          dataIndex: "file_types",
          width: "10%",
          ellipsis: true,
          sorter: true
        }
      ],
      chartProps: {}
    },
    uri: "scm_committers",
    method: "list",
    filters: {
      page_size: 10
    },
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: scmPartialFilterKeyMapping,
    [FILTER_NAME_MAPPING]: scmCommonFilterOptionsMapping,
    [DISABLE_PARTIAL_FILTER_MAPPING_KEY]: ["repo_id", "project"],
    supported_filters: githubCommitsSupportedFilters,
    widgetSettingsTimeRangeFilterSchema: [githubTimeFilter],
    drilldown: {
      title: "Github or Bitbucket Committers",
      uri: "scm_committers",
      application: "scm_committers",
      columns: scmCommittersTableConfig,
      supported_filters: githubCommitsSupportedFilters,
      drilldownTransformFunction: data => genericDrilldownTransformer(data)
    },
    transformFunction: data => tableTransformer(data),
    [SHOW_METRICS_TAB]: true,
    [API_BASED_FILTER]: ["committers", "authors"],
    [FIELD_KEY_FOR_FILTERS]: scmLeadTimeFieldKeyMap,
    [REPORT_FILTERS_CONFIG]: CommittersReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  scm_file_types_report: {
    name: "SCM File Types Report",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.TABLE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    chart_props: {
      size: "small",
      columns: [
        {
          title: "File Type",
          key: "name",
          dataIndex: "name",
          width: "10%",
          ellipsis: true
        },
        {
          title: "No. of Changes",
          key: "num_changes",
          dataIndex: "num_changes",
          width: "10%",
          ellipsis: true
        },
        {
          title: "No. of Commits",
          key: "num_commits",
          dataIndex: "num_commits",
          width: "10%",
          ellipsis: true,
          sorter: true
        },
        {
          title: "No. of PRs",
          key: "num_prs",
          dataIndex: "num_prs",
          width: "10%",
          ellipsis: true,
          sorter: true
        },
        {
          title: "No. of Issues",
          key: "num_jira_issues",
          dataIndex: "num_jira_issues",
          width: "10%",
          ellipsis: true,
          sorter: true
        }
      ],
      chartProps: {}
    },
    uri: "scm_file_types",
    method: "list",
    filters: {
      page_size: 10
    },
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: scmPartialFilterKeyMapping,
    [FILTER_NAME_MAPPING]: scmCommonFilterOptionsMapping,
    [DISABLE_PARTIAL_FILTER_MAPPING_KEY]: ["file_type"],
    supported_filters: {
      ...githubCommitsSupportedFilters,
      values: [...githubCommitsSupportedFilters.values, "file_type"]
    },
    widgetSettingsTimeRangeFilterSchema: [githubTimeFilter],
    drilldown: {
      title: "Github or Bitbucket File Types",
      uri: "scm_file_types",
      application: "scm_file_types",
      columns: scmFileTypeConfig,
      supported_filters: {
        ...githubCommitsSupportedFilters,
        values: [...githubCommitsSupportedFilters.values, "file_type"]
      },
      drilldownTransformFunction: data => genericDrilldownTransformer(data)
    },
    transformFunction: data => tableTransformer(data),
    [SHOW_METRICS_TAB]: true,
    [API_BASED_FILTER]: ["committers", "authors"],
    [FIELD_KEY_FOR_FILTERS]: scmLeadTimeFieldKeyMap,
    [FILTERS_NOT_SUPPORTING_PARTIAL_FILTERS]: ["file_types"],
    [REPORT_FILTERS_CONFIG]: GithubFileTypeReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  scm_issues_time_resolution_report: {
    name: "SCM Issues Resolution Time Report",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultAcross: "issue_created",
    default_query: githubresolutionTimeDefaultQuery,
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
      sortBy: "median_resolution_time",
      chartProps: chartProps
    },
    tooltipMapping: scmResolutionTimeTooltipMapping,
    uri: "scm_resolution_time_report",
    method: "list",
    filters: {},
    dataKey: ["median_resolution_time", "number_of_tickets_closed"],
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: githubIssuesSupportedFilters,
    drilldown: scmResolutionDrillDown,
    transformFunction: data => scmaResolutionTimeDataTransformer(data),
    weekStartsOnMonday: true,
    widgetSettingsTimeRangeFilterSchema: [scmResolutionTimeFilters],
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: {
      issue_resolved_at: "issue_resolved_at"
    },
    [FILTER_NAME_MAPPING]: scmTimeAcrossFilterOptionsMapping,
    [FILTER_WITH_INFO_MAPPING]: [scmExcludeStatusFilter],
    [SHOW_METRICS_TAB]: true,
    [API_BASED_FILTER]: ["creators", "committers", "authors", "assignees", "reviewers"],
    [FIELD_KEY_FOR_FILTERS]: scmLeadTimeFieldKeyMap,
    xAxisLabelTransform: getXAxisLabel,
    [GET_GRAPH_FILTERS]: resolutionTimeGetGraphFilters,
    [REPORT_FILTERS_CONFIG]: IssuesResolutionTimeReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  lead_time_single_stat: {
    name: "Lead Time Single Stat",
    application: IntegrationTypes.GITHUBJIRA,
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
    [FILTER_NAME_MAPPING]: jiraCommonFilterOptionsMapping,
    [FILTER_WITH_INFO_MAPPING]: [leadTimeExcludeStageFilter],
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: scmPartialFilterKeyMapping,
    [SHOW_SETTINGS_TAB]: true,
    widgetSettingsTimeRangeFilterSchema: [scmPrCreatedAt],
    supported_filters: leadTimeSupportedFilters,
    drilldown: {},
    transformFunction: data => statReportTransformer(data),
    chart_click_enable: false,
    [API_BASED_FILTER]: ["creators", "committers", "authors", "assignees", "reviewers"],
    [FIELD_KEY_FOR_FILTERS]: scmLeadTimeFieldKeyMap,
    [OU_EXCLUSION_CONFIG]: {
      prefixValue: "jira_",
      remove_normal_prefix: true,
      remove_exclude_prefix: true,
      remove_partial_prefix: true
    }
  },
  scm_pr_lead_time_trend_report: {
    name: "SCM PR Lead Time Trend Report",
    application: IntegrationTypes.GITHUB,
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
      calculation: "pr_velocity"
    },
    default_query: scmLeadTimeDefaultQuery,
    convertTo: "days",
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    shouldJsonParseXAxis: () => true,
    supported_filters: leadTimeCicdSupportedFilters,
    [FILTER_NAME_MAPPING]: jiraCommonFilterOptionsMapping,
    [FILTER_WITH_INFO_MAPPING]: [leadTimeExcludeStageFilter],
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [SHOW_SETTINGS_TAB]: true,
    [SHOW_METRICS_TAB]: true,
    [SHOW_AGGREGATIONS_TAB]: false,
    widgetSettingsTimeRangeFilterSchema: [scmPrCreatedAt, githubPrClosedAt],
    drilldown: scmLeadTimeDrilldown,
    transformFunction: data => leadTimeTrendTransformer(data),
    [API_BASED_FILTER]: ["creators", "committers", "authors", "assignees", "reviewers"],
    [FIELD_KEY_FOR_FILTERS]: scmLeadTimeFieldKeyMap,
    [REPORT_FILTERS_CONFIG]: PrLeadTimeTrendReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  scm_pr_lead_time_by_stage_report: {
    name: "SCM PR Lead Time by Stage Report",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.LEAD_TIME_PHASE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultAcross: "velocity",
    default_query: scmLeadTimeStageDefaultQuery,
    chart_props: {
      unit: "Days",
      chartProps: chartProps
    },
    dataKey: "duration",
    uri: "lead_time_report",
    method: "list",
    filters: {
      calculation: "pr_velocity"
    },
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    supported_filters: leadTimeCicdSupportedFilters,
    [FILTER_NAME_MAPPING]: jiraCommonFilterOptionsMapping,
    [FILTER_WITH_INFO_MAPPING]: [leadTimeExcludeStageFilter],
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    widgetSettingsTimeRangeFilterSchema: [scmPrCreatedAt, githubPrClosedAt],
    [SHOW_SETTINGS_TAB]: true,
    [SHOW_METRICS_TAB]: true,
    [SHOW_AGGREGATIONS_TAB]: false,
    [PREVIEW_DISABLED]: true,
    drilldown: scmLeadTimeDrilldown,
    [CSV_DRILLDOWN_TRANSFORMER]: leadTimeCsvTransformer,
    transformFunction: data => leadTimePhaseTransformer(data),
    [API_BASED_FILTER]: ["creators", "committers", "authors", "assignees", "reviewers"],
    [FIELD_KEY_FOR_FILTERS]: scmLeadTimeFieldKeyMap,
    [REPORT_FILTERS_CONFIG]: PrLeadTimeByStageReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true,
    [GET_VELOCITY_CONFIG]: true,
    [PREV_REPORT_TRANSFORMER]: transformAzureLeadTimeStageReportPrevQuery,
    includeContextFilter: true,
    drilldownFooter: () => LeadTimeByStageFooter,
    [GET_GRAPH_FILTERS]: props => {
      const { finalFilters, contextFilter } = props;
      return { ...finalFilters, filter: { ...finalFilters.filter, ...contextFilter } };
    },
    drilldownCheckbox: getDrilldownCheckBox
  },
  scm_issues_time_across_stages_report: {
    name: "SCM Issues Time Across Stages",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultAcross: "column",
    default_query: githubTimeAcrossStagesDefaultQuery,
    xaxis: true,
    chart_props: {
      barProps: [
        {
          name: "Median time in status",
          dataKey: "median_time"
        },
        {
          name: "Average time in stages",
          dataKey: "average_time"
        }
      ],
      stacked: false,
      unit: "Days",
      sortBy: "median_time",
      chartProps: chartProps
    },
    uri: "scm_issues_time_across_stages",
    method: "list",
    filters: {},
    dataKey: "median_time",
    supportPartialStringFilters: false,
    supportExcludeFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [DISABLE_EXCLUDE_FILTER_MAPPING_KEY]: ["project", "label", "repo_id", "assignee"],
    supported_filters: scmIssuesTimeAcrossStagesSupportedFilters,
    drilldown: scmIssueTimeAcrossStagesDrilldown,
    transformFunction: data => scmaResolutionTimeDataTransformer(data),
    weekStartsOnMonday: true,
    getTotalKey: params => {
      const { metric } = params;
      if (metric === "average_time") {
        return "mean";
      }
      return "median";
    },
    [FILTER_NAME_MAPPING]: { ...scmTimeAcrossFilterOptionsMapping, repo_id: "Repo" },
    [FILTER_WITH_INFO_MAPPING]: [sccIssueExcludeStatusFilter],
    [SHOW_METRICS_TAB]: true,
    [SHOW_SETTINGS_TAB]: true,
    [STACKS_SHOW_TAB]: WIDGET_CONFIGURATION_KEYS.AGGREGATIONS,
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    },
    widgetSettingsTimeRangeFilterSchema: scmIssueTimeAcrossStagesTimeFilters,
    [REPORT_FILTERS_CONFIG]: SCMIssuesTimeAcrossStagesFiltersConfig
  },
  github_coding_days_report: {
    name: "SCM Coding Days Report",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.BAR,
    description: SCM_CODING_DAYS_DESCRIPTION,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    defaultAcross: "repo_id",
    default_query: {
      interval: "week",
      committed_at: {
        $gt: moment.utc().subtract(6, "days").startOf("day").unix().toString(),
        $lt: moment.utc().unix().toString()
      }
    },
    chart_props: {
      unit: "Days",
      barProps: [
        {
          name: "Average Coding days per week",
          dataKey: "mean",
          unit: "Days"
        }
      ],
      stacked: false,
      barTopValueFormater: value => formatTooltipValue(value),
      chartProps: chartProps
    },
    uri: "github_coding_day",
    method: "list",
    filters: {
      sort: [
        {
          id: "commit_days",
          desc: true
        }
      ]
    },
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: scmPartialFilterKeyMapping,
    [DISABLE_PARTIAL_FILTER_MAPPING_KEY]: ["file_type"],
    [FILTER_NAME_MAPPING]: scmCommonFilterOptionsMapping,
    [API_BASED_FILTER]: ["authors", "committers"],
    [FIELD_KEY_FOR_FILTERS]: codingDaysApiBasedFilterKeyMapping,
    supported_filters: {
      ...githubCommitsSupportedFilters,
      values: [...githubCommitsSupportedFilters.values, "file_type"]
    },
    widgetSettingsTimeRangeFilterSchema: [githubCommittedAt],
    drilldown: {
      title: "Github Commits Tickets",
      uri: "github_commits_tickets",
      application: "github_commits",
      columns: GithubCommitsTableConfig,
      supported_filters: {
        ...githubCommitsSupportedFilters,
        values: [...githubCommitsSupportedFilters.values, "file_type"]
      },
      drilldownTransformFunction: data => scmDrilldownTranformerForIncludesFilter(data)
    },
    onChartClickPayload: params => {
      const { data, across } = params;
      const _data = data?.activePayload?.[0]?.payload || {};
      if (["author", "committer"].includes(across)) {
        return {
          name: data.activeLabel || "",
          id: _data.key || data.activeLabel
        };
      }
      return data.activeLabel || "";
    },
    transformFunction: data => SCMPRReportsTransformer(data),
    default_query: scmCodingDaysDefaultQuery,
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [VALUE_SORT_KEY]: "commit_days",
    [WIDGET_VALIDATION_FUNCTION]: payload => {
      const { query } = payload;
      const committed_at = get(query, ["committed_at"], undefined);
      return committed_at ? true : false;
    },
    [FE_BASED_FILTERS]: {
      ...scmCodingDaysReportFEBased,
      show_value_on_bar
    },
    [REPORT_FILTERS_CONFIG]: CodingDaysReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  github_coding_days_single_stat: {
    name: "SCM Coding Days Single Stat",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    show_max: false,
    xaxis: false,
    chart_props: {
      unit: "Days"
    },
    uri: "github_coding_day",
    method: "list",
    filters: {
      across: "committer"
    },
    default_query: {
      time_period: 1,
      agg_type: "total"
    },
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: scmPartialFilterKeyMapping,
    [FILTER_NAME_MAPPING]: scmCommonFilterOptionsMapping,
    compareField: "mean",
    supported_filters: githubCommitsSupportedFilters,
    widgetSettingsTimeRangeFilterSchema: [githubCommittedAt],
    drilldown: githubCommitsStatDrilldown,
    transformFunction: data => statReportTransformer(data),
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [REPORT_FILTERS_CONFIG]: CodingDaysSingleStatReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  github_prs_response_time_report: {
    name: "SCM PRs Response Time Report",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.CIRCLE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    defaultAcross: "repo_id",
    convertTo: "days",
    [FILTERS_NOT_SUPPORTING_PARTIAL_FILTERS]: ["labels"],
    chart_props: {
      unit: "Days",
      barProps: [
        {
          name: "Average Author Response Time",
          dataKey: "average_author_response_time",
          unit: "Days"
        }
      ],
      stacked: false,
      chartProps: chartProps
    },
    uri: "github_prs_author_response_time",
    reviewerUri: "github_prs_reviewer_response_time",
    method: "list",
    filters: {},
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [SHOW_METRICS_TAB]: true,
    [PARTIAL_FILTER_MAPPING_KEY]: scmPartialFilterKeyMapping,
    [FILTER_NAME_MAPPING]: scmPrsFilterOptionsMapping,
    [DISABLE_PARTIAL_FILTER_MAPPING_KEY]: ["label"],
    supported_filters: githubPRsSupportedFilters,
    drilldown: githubPRSDrilldown,
    widgetSettingsTimeRangeFilterSchema: [githubPrCreatedAt, githubPrClosedAt],
    transformFunction: data => SCMPRReportsTransformer(data),
    xAxisLabelTransform: xAxisLabelTransform,
    shouldSliceFromEnd: () => true,
    onChartClickPayload: params => {
      const { data, across, chart_type, visualization } = params;
      const chart = chart_type ?? visualization;
      if ([ChartType.BAR, ChartType.LINE, ChartType.AREA].includes(chart)) {
        const _data = data?.activePayload?.[0]?.payload || {};
        if (["author", "committer", "reviewer"].includes(across)) {
          return {
            name: data.activeLabel || "",
            id: _data.key || data.activeLabel
          };
        }
        return data.activeLabel || "";
      } else {
        if (["author", "committer", "reviewer"].includes(across)) {
          return {
            name: data.name || data.key || "",
            id: data.key || data.name || ""
          };
        }
        return data.name || "";
      }
    },
    [REPORT_FILTERS_CONFIG]: PrsResponseTimeReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true,
    weekStartsOnMonday: true,
    stack_filters: SCM_PRS_RESPONSE_TIME_STACK_FILTER,
    [ALLOW_KEY_FOR_STACKS]: true
  },
  github_prs_response_time_single_stat: {
    name: "SCM PRs Response Time Single Stat",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    show_max: false,
    xaxis: true,
    across: ["pr_created", "pr_updated", "pr_merged", "pr_reviewed"],
    defaultAcross: "pr_created",
    chart_props: {
      unit: "Days"
    },
    uri: "github_prs_author_response_time",
    reviewerUri: "github_prs_reviewer_response_time",
    method: "list",
    filters: {},
    default_query: statDefaultQuery,
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: scmPartialFilterKeyMapping,
    [FILTER_NAME_MAPPING]: scmPrsFilterOptionsMapping,
    compareField: "mean",
    supported_filters: githubPRsSupportedFilters,
    widgetSettingsTimeRangeFilterSchema: [githubPrCreatedAt, githubPrClosedAt],
    drilldown: githubPRSStatDrilldown,
    transformFunction: data => statReportTransformer(data),
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [REPORT_FILTERS_CONFIG]: PrsResponseTimeSingleStatFiltersConfig
  },
  scm_rework_report: {
    name: "SCM Rework Report",
    description: SCM_REWORK_DESCRIPTION,
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    defaultAcross: "author",
    chart_props: {
      unit: "Lines of code",
      barProps: [
        {
          name: "Number of lines of code",
          dataKey: "total_lines_changed"
        }
      ],
      stacked: true,
      chartProps: chartProps
    },
    uri: "scm_rework_report",
    method: "list",
    filters: {},
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: scmPartialFilterKeyMapping,
    [FILTER_NAME_MAPPING]: scmCommonFilterOptionsMapping,
    supported_filters: {
      ...githubCommitsSupportedFilters,
      values: [...githubCommitsSupportedFilters.values, "file_type"]
    },
    widgetSettingsTimeRangeFilterSchema: [githubCommittedAt],
    drilldown: {
      title: "Github Commits Tickets",
      uri: "github_commits_tickets",
      application: "github_commits",
      columns: GithubCommitsTableConfig,
      supported_filters: {
        ...githubCommitsSupportedFilters,
        values: [...githubCommitsSupportedFilters.values, "file_type"]
      },
      drilldownTransformFunction: data => scmDrilldownTranformerForIncludesFilter(data)
    },
    transformFunction: data => scmReworkReportTransformer(data),
    xAxisLabelTransform: xAxisLabelTransform,
    onChartClickPayload: params => {
      const { data, across } = params;
      const _data = data?.activePayload?.[0]?.payload || {};
      if (across === "trend") {
        return data.activeLabel;
      } else if (["author", "committer"].includes(across)) {
        return {
          name: data.activeLabel || "",
          id: _data.key || data.activeLabel
        };
      }
      return data.activeLabel || "";
    },
    [SHOW_METRICS_TAB]: false,
    [API_BASED_FILTER]: ["committers", "authors"],
    [FIELD_KEY_FOR_FILTERS]: scmLeadTimeFieldKeyMap,
    [PREV_REPORT_TRANSFORMER]: transformSCMPrevQuery,
    [REPORT_FILTERS_CONFIG]: SCMReworkReportFiltersConfig
  },
  review_collaboration_report: {
    name: "SCM Review Collaboration Report",
    description: SCM_REVIEW_COLLABORATION_DESCRIPTION,
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.REVIEW_SCM_SANKEY,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    chart_props: {
      unit: "Count",
      barProps: [
        {
          name: "count",
          dataKey: "count",
          unit: "count"
        }
      ],
      stacked: false,
      chartProps: chartProps
    },
    uri: "scm_review_collaboration_report",
    method: "list",
    filters: {},
    default_query: {
      missing_fields: {
        pr_merged: true
      }
    },
    supportPartialStringFilters: true,
    supportExcludeFilters: true,
    [WIDGET_MIN_HEIGHT]: "36rem",
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: scmPartialFilterKeyMapping,
    [FILTER_NAME_MAPPING]: scmPrsFilterOptionsMapping,
    [DISABLE_PARTIAL_FILTER_MAPPING_KEY]: ["label"],
    supported_filters: githubPRsSupportedFilters,
    widgetSettingsTimeRangeFilterSchema: [
      githubPrCreatedAt,
      githubPrClosedAt,
      { ...githubPrMergedAt, label: "PR MERGED TIME" }
    ],
    [SHOW_METRICS_TAB]: false,
    drilldown: githubReviewCollaborationDrilldown,
    transformFunction: data => SCMReviewCollaborationTransformer(data),
    [FE_BASED_FILTERS]: scmCollabRadioBasedFilter,
    shouldFocusOnDrilldown: true,
    [FILTERS_NOT_SUPPORTING_PARTIAL_FILTERS]: ["labels"],
    [API_BASED_FILTER]: ["committers", "authors", "creators"],
    [FIELD_KEY_FOR_FILTERS]: scmLeadTimeFieldKeyMap,
    includeMissingFieldsInPreview: true,
    [GET_GRAPH_FILTERS]: getGraphFilters,
    onUnmountClearData: true,
    [REPORT_FILTERS_CONFIG]: SCMReviewCollabReportFiltersConfig
  }
};
